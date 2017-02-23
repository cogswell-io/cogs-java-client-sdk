package com.gambit.sdk.pubsub;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.gambit.sdk.pubsub.exceptions.PubSubErrorResponseException;
import com.gambit.sdk.pubsub.handlers.PubSubMessageHandler;
import com.gambit.sdk.pubsub.responses.errors.*;

import org.json.JSONObject;

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;

public class PubSubErrorResponseIntegrationTest {

    private static String testServer = "*";
    private static String readPermissions = "*";
    private static String writePermissions = "*";
    private static String adminPermissions = "*";

    private static PubSubHandle pubsubHandle;
    private static String errorMessage;
    private static boolean isError;

    private static CountDownLatch awaitingSubscribe = new CountDownLatch(1);
    private static CountDownLatch awaitingPublish = new CountDownLatch(1);

    @BeforeClass
    public static void setUpBeforeClass() {
        PubSubIntegrationTestsConfig config = PubSubIntegrationTestsConfig.getInstance();
        testServer = config.getHost();
        readPermissions = config.getPrimaryKeys().stream().filter(x -> (x.charAt(0) == 'R')).findFirst().get();
        writePermissions = config.getPrimaryKeys().stream().filter(x -> (x.charAt(0) == 'W')).findFirst().get();
        adminPermissions = config.getPrimaryKeys().stream().filter(x -> (x.charAt(0) == 'A')).findFirst().get();
    }

    @Before
    public void setupBeforeEach() {
        pubsubHandle = null;
        errorMessage = "";
        isError = false;
    }

    private void onErrorResponseForInvalidSubscribe(PubSubErrorResponse response) {
        assertTrue(
            "The Error Response should be an of type PubSubIncorrectPermissionsResponse...",
            response instanceof PubSubIncorrectPermissionsResponse
        );

        assertTrue("There should be an action in the error", response.getAction().isPresent());
        assertEquals("The error code should be 401 to mean permission is denied...", 401, response.getCode());
        assertEquals("The 'subscribe' action should have been rejected...", "subscribe", response.getAction().get());

        awaitingSubscribe.countDown();
    }

    private void onErrorResponseForInvalidPublish(PubSubErrorResponse response) {
        assertTrue(
            "The Error Response should be an of type PubSubIncorrectPermissionsResponse...",
            response instanceof PubSubIncorrectPermissionsResponse
        );

        assertTrue("There should be an action in the error", response.getAction().isPresent());
        assertEquals("The error code should be 401 to mean permission is denied...", 401, response.getCode());
        assertEquals("The 'pub' action should have been rejected...", "pub", response.getAction().get());

        awaitingPublish.countDown();
    }

    @Test
    public void testErrorResponseOnInvalidPublish() {
        PubSubOptions options = new PubSubOptions(testServer, null, null, null);

        List<String> subscribeKeys = Collections.synchronizedList(new LinkedList<>());
        subscribeKeys.add(readPermissions);
        
        PubSubSDK.getInstance().connect(subscribeKeys, options)
            .thenComposeAsync((handle) -> {
                pubsubHandle = handle;
                pubsubHandle.onErrorResponse(this::onErrorResponseForInvalidPublish);

                return pubsubHandle.publish("Some Channel", "This message is lost to the aether...");
            })
            .thenAcceptAsync((sequence) -> {
                // We don't need to do anything here
            })
            .exceptionally((error) -> {
                isError = true;
                errorMessage = "Error in trying to publish: " + error.getMessage();
                awaitingPublish.countDown();
                return null;
            });

        try {
            boolean completed  = awaitingPublish.await(5, TimeUnit.SECONDS);

            if(!completed) {
                fail("Test timed out before it could complete...");
            }

            if(isError) {
                fail(errorMessage);
            }
        }
        catch(InterruptedException e) {
            fail("Interrupted while waiting for the test to complete...");
        }
    }

    @Test
    public void testErrorResponseOnInvalidSubscribe() {
        PubSubOptions options = new PubSubOptions(testServer, null, null, null);

        List<String> publishKeys = Collections.synchronizedList(new LinkedList<>());
        publishKeys.add(writePermissions);

        CountDownLatch signal = new CountDownLatch(1);
        
        PubSubSDK.getInstance().connect(publishKeys, options)
            .thenComposeAsync((handle) -> {
                pubsubHandle = handle;
                pubsubHandle.onErrorResponse(this::onErrorResponseForInvalidSubscribe);

                return pubsubHandle.subscribe("Some Channel", (record) -> {});
            })
            .thenAcceptAsync((subscriptions) -> {
                isError = true;
                errorMessage = "Should not have been able to subscribe...";
                signal.countDown();
            })
            .exceptionally((error) -> {
                try {
                    assertTrue(
                        "Error result should be that of invalid permissions...",
                        error.getCause() instanceof PubSubErrorResponseException
                    );
                }
                catch(AssertionError e) {
                    isError = true;
                    errorMessage = "Incorrect error received: " + e.getMessage();
                }

                signal.countDown();
                return null;
            });

        try {
            boolean completed  = awaitingSubscribe.await(5, TimeUnit.SECONDS);

            if(!completed) {
                fail("Test timed out before it could complete...");
            }

            completed  = signal.await(5, TimeUnit.SECONDS);

            if(!completed) {
                fail("Test timed out before it could complete...");
            }

            if(isError) {
                fail(errorMessage);
            }
        }
        catch(InterruptedException e) {
            fail("Interrupted while waiting for the test to complete...");
        }
    }
}