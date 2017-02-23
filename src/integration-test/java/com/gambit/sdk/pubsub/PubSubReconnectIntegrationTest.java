package com.gambit.sdk.pubsub;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import java.util.Collections;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import java.util.Random;

import com.gambit.sdk.pubsub.handlers.PubSubMessageHandler;

import org.json.JSONObject;

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;

public class PubSubReconnectIntegrationTest {

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

        initialSubscriptions.add("HEALTH");
        initialSubscriptions.add("MUSIC");
        initialSubscriptions.add("FOOD");
        initialSubscriptions.add("BINGO");
    }

    private static List<String> initialSubscriptions = Collections.synchronizedList(new ArrayList<>());
    private static CountDownLatch signal = new CountDownLatch(1);
    private static UUID sessionUuid = null;

    private void handleMessages(PubSubMessageRecord record) {
        // Don't care about the messages...
    }

    private void onReconnect() {
        PubSubOptions options = new PubSubOptions(testServer, null, null, sessionUuid);
        
        PubSubSDK.getInstance().connect(primaryPermissions, options)
            .thenComposeAsync((handle) -> {
                pubsubHandle = handle;

                return pubsubHandle.getSessionUuid();
            })
            .thenComposeAsync((uuid) -> {
                try {
                    assertEquals(
                        "Expect the collected Session ID to match the previous Session ID...",
                        sessionUuid.toString(),
                        uuid.toString()
                    );
                }
                catch(AssertionError e) {
                    isError = true;
                    errorMessage = "Session ID is different: " + e.getMessage();
                }

                return pubsubHandle.listSubscriptions();
            })
            .thenComposeAsync((subs) -> {
                Collections.sort(subs);
                Collections.sort(initialSubscriptions);

                try {
                    assertEquals(
                        "Expect to be subscribed to the original channels by this point...",
                        initialSubscriptions,
                        subs
                    );
                }
                catch(AssertionError e) {
                    isError = true;
                    errorMessage = "Channels were not resubscribed: " + e.getMessage();
                }

                return pubsubHandle.close();
            })
            .thenAcceptAsync((voided) -> {
                signal.countDown();
            })
            .exceptionally((error) -> {
                isError = true;
                errorMessage = "Error in onReconnect: " + error.getMessage();
                signal.countDown();
                return null;
            });
    }

    @Test
    public void testReconnectOnConnectionDrop() {
        try {
            PubSubOptions options = new PubSubOptions(testServer, null, null, null);
            PubSubSDK pubsubSDK = PubSubSDK.getInstance();

            pubsubSDK.connect(primaryPermissions, options)
                .thenComposeAsync((handle) -> {
                    pubsubHandle = handle;

                    pubsubHandle.onReconnect(this::onReconnect);

                    List<CompletableFuture<?>> subscribeFutures = Collections.synchronizedList(new ArrayList<>());

                    for(String channel : initialSubscriptions) {
                        subscribeFutures.add(pubsubHandle.subscribe(channel, this::handleMessages));
                    }

                    return CompletableFuture.allOf(subscribeFutures.toArray(new CompletableFuture<?>[0]));
                })
                .thenComposeAsync((voided) -> {
                    return pubsubHandle.getSessionUuid();
                })
                .thenAcceptAsync((uuid) -> {
                    sessionUuid = uuid;
                    pubsubHandle.dropConnection(new PubSubDropConnectionOptions(20));
                })
                .exceptionally((error) -> {
                    isError = true;
                    errorMessage = error.getMessage();
                    signal.countDown();
                    return null;
                });

            try {
                signal.await(5, TimeUnit.SECONDS);

                if(isError) {
                    fail("An error occurred: " + errorMessage);
                }
            }
            catch(InterruptedException e) {
                fail("INTERRUPTED WHILE WAITING FOR COUNTDOWN");
            }
        }
        catch(Throwable ex) {
            fail("There was an exception thrown: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}