package com.gambit.sdk.pubsub;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import java.util.List;

import com.gambit.sdk.pubsub.handlers.PubSubMessageHandler;

import org.json.JSONObject;

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;

public class PubSubOnRawRecordIntegrationTest {

    private static String testServer = "*";
    private static List<String> primaryPermissions = null;
    private static List<String> secondaryPermissions = null;

    private static PubSubHandle pubsubHandle;
    private static String errorMessage;
    private static boolean isError;

    @BeforeClass
    public static void setUpBeforeClass() {
        PubSubIntegrationTestsConfig config = PubSubIntegrationTestsConfig.getInstance();
        testServer = config.getHost();
        primaryPermissions = config.getPrimaryKeys();
        secondaryPermissions = config.getSecondaryKeys();
    }

    @Before
    public void setupBeforeEach() {
        pubsubHandle = null;
        errorMessage = "";
        isError = false;
    }

    @Test
    public void testRawRecordHandler() {
        PubSubOptions options = new PubSubOptions(testServer, null, null, null);
        CountDownLatch signal = new CountDownLatch(1);
        AtomicInteger count = new AtomicInteger(0);
        
        PubSubSDK.getInstance().connect(primaryPermissions, options)
            .thenComposeAsync((handle) -> {
                pubsubHandle = handle;
                
                // Expect to be called once for each request for a total of five (5 + 2) times:
                //  initial connection (with autoReconnect)
                //  getSessionUuid, subscribe, publishWithAck, listSubscriptions, and unsubscribe
                //  receiving the published message
                pubsubHandle.onRawRecord(jsonRecord -> count.getAndIncrement());

                return pubsubHandle.getSessionUuid();
            })
            .thenComposeAsync(uuid -> { return pubsubHandle.subscribe("Test", (record) -> {}); })
            .thenComposeAsync(subs -> { return pubsubHandle.publishWithAck("Test", "I am sent..."); })
            .thenComposeAsync(uuid -> { return pubsubHandle.listSubscriptions(); })
            .thenComposeAsync(subs -> { return pubsubHandle.unsubscribe("Test"); })
            .thenComposeAsync(subs -> { return pubsubHandle.close(); })
            .thenAcceptAsync(voided -> { signal.countDown(); })
            .exceptionally((error) -> {
                isError = true;
                errorMessage = "Error in onReconnect: " + error.getMessage();
                signal.countDown();
                return null;
            });

        try {
            boolean completed  = signal.await(2, TimeUnit.SECONDS);

            if(!completed) {
                fail("Test timed out before it could complete...");
            }

            if(isError) {
                fail(errorMessage);
            }
            else {
                assertEquals("We must receive a response for each call...", 7, count.get());
            }
        }
        catch(InterruptedException e) {
            fail("Interrupted while waiting for the test to complete...");
        }
    }
}