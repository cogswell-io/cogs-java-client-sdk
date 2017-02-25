package com.gambit.sdk.pubsub;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import java.util.Collections;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Random;

import java.util.UUID;

import com.gambit.sdk.pubsub.PubSubHandle;
import com.gambit.sdk.pubsub.PubSubSDK;
import com.gambit.sdk.pubsub.handlers.*;
import com.gambit.sdk.pubsub.exceptions.*;
import com.gambit.sdk.pubsub.responses.successes.*;
import com.gambit.sdk.pubsub.responses.errors.*;

import org.json.JSONObject;

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;

public class PubSubFullSweepIntegrationTest {

    private static String testServer = "*";
    private static List<String> primaryPermissions = null;
    private static List<String> secondaryPermissions = null;

    private static PubSubHandle pubsubHandle = null;
    private static PubSubHandle secondHandle = null;
    private static String errorMessage = "";
    private static boolean isError = false;

    @Before
    public void setUpBeforeEach() {
        PubSubIntegrationTestsConfig config = PubSubIntegrationTestsConfig.getInstance();
        testServer = config.getHost();
        primaryPermissions = config.getPrimaryKeys();
        secondaryPermissions = config.getSecondaryKeys();

        testChannels = Collections.synchronizedMap(new HashMap<>());
        testChannels.put("health", "Health & Fitness");
        testChannels.put("books", "Books & Movies");
        testChannels.put("time", "Time Management");

        channels = Collections.synchronizedList(new ArrayList<>());
        channels.add(testChannels.get("health"));
        channels.add(testChannels.get("time"));

        bookMessages = Collections.synchronizedMap(new HashMap<>());
        bookMessages.put("desired", "The advertisement of the century, again...");
        bookMessages.put("filtered", "Have you read this great new book???");
    }

    //////////////////////////// DECLS USED FOR TEST ////////////////////////////

    private static PubSubResponse messageAck;
    private static UUID messageId;
    private static UUID sessionId;

    private static UUID healthMessageId;

    private static Map<String, String> testChannels;
    private static List<String> channels;

    private static Map<String, String> bookMessages;
    private static String healthMessage = "Feedback is a useful mechanism";

    private static CountDownLatch waitToFinish = new CountDownLatch(1);
    private static CountDownLatch firstHealthMessage = new CountDownLatch(1);
    private static CountDownLatch secondHealthMessage = new CountDownLatch(1);

    public void bookHandler(PubSubMessageRecord record) {
        assertEquals(
            "This handler should only see messages for books...",
            testChannels.get("books"),
            record.getChannel()
        );

        assertEquals(
            "This handler should only see the advertisement message...",
            bookMessages.get("desired"),
            record.getMessage()
        );
    }

    public void healthHandler(PubSubMessageRecord record) {
        assertEquals(
            "This handler should only see messages for health...",
            testChannels.get("health"),
            record.getChannel()
        );

        assertEquals(
            "This handler should only see the advertisement message...",
            healthMessage,
            record.getMessage()
        );

        messageId = record.getId();

        if(firstHealthMessage.getCount() != 0) {
            firstHealthMessage.countDown();
        }
        else {
            secondHealthMessage.countDown();
        }
    }

    public void timeHandler(PubSubMessageRecord record) {
        fail("Received Unexpected Message: " + record.getMessage());
    }

    //////////////////////////// HANDLER DEFINITIONS ////////////////////////////

    public void onReconnect() {
        pubsubHandle.getSessionUuid()
            .thenComposeAsync(uuid -> {
                try {
                    assertEquals(
                        "The previous session should match the restored session...",
                        sessionId.toString(),
                        uuid.toString()
                    );
                }
                catch(AssertionError e) {
                    isError = true;
                    errorMessage = "On Reconnect (Get Session UUID): " + e.getMessage();
                    waitToFinish.countDown();
                }

                return pubsubHandle.listSubscriptions();
            })
            .thenComposeAsync(subscriptions -> {
                try {
                    Collections.sort(subscriptions);
                    Collections.sort(channels);

                    assertEquals(
                        "The subscriptions after reconnect should be what they were previously...",
                        subscriptions,
                        channels
                    );
                }
                catch(AssertionError e) {
                    isError = true;
                    errorMessage = "On Reconnect (List Subscriptions): " + e.getMessage();
                    waitToFinish.countDown();
                }

                return pubsubHandle.publishWithAck(testChannels.get("health"), healthMessage);
            })
            .thenComposeAsync((uuid) -> {
                try {
                    System.out.println("UUID: " + uuid);
                    boolean completed = secondHealthMessage.await(2, TimeUnit.SECONDS);
                    
                    if(!completed) {
                        isError = true;
                        errorMessage = "Did not receive the published health message second time...";
                    }
                    else {
                        assertEquals(
                            "The published health message and the acknowledged message should be the same...",
                            healthMessageId.toString(),
                            uuid.toString()
                        );
                    }
                }
                catch(InterruptedException e) {
                    isError = true;
                    errorMessage = "Interrupted waiting for the published health message: " + e.getMessage();
                }
                catch(AssertionError ex) {
                    isError = true;
                    errorMessage = "Published healht message problem: " + ex.getMessage();
                }

                return pubsubHandle.close();
            })
            .thenAcceptAsync(voided -> {
                waitToFinish.countDown();
            })
            .exceptionally(error -> {
                isError = true;
                errorMessage = "On Reconnect (Exceptionally): " + error.getMessage();
                waitToFinish.countDown();
                return null;
            });
    }

    public void onMessage(PubSubMessageRecord record) {
        List<String> possibleMessages = Collections.synchronizedList(new ArrayList<>());
        possibleMessages.add(healthMessage);
        possibleMessages.add(bookMessages.get("desired"));
        possibleMessages.add(bookMessages.get("filtered"));

        Collection<String> possibleChannels = testChannels.values();

        assertTrue(
            "The received message should only be on of the expected messages...",
            possibleMessages.contains(record.getMessage())
        );

        assertTrue(
            "The channel found should be only one of the expected channels...",
            possibleChannels.contains(record.getChannel())
        );
    }

    public void onRawRecord(String jsonString) {
        assertNotNull("There should have been a raw record...", jsonString);
    }

    public void onNewSession(UUID uuid) {
        assertFalse(
            "The new session should not have the same UUID as a previous session...",
            sessionId.toString().equals(uuid.toString())
        );
    }

    public void onError(Throwable error) {
        fail("Client-Side Error: " + error.getMessage());
    }

    public void onErrorResponse(PubSubErrorResponse errorResponse) {
        fail("Server-Side Error: " + errorResponse.getMessage());
    }

    public void onClose(Throwable error) {
        assertNull("There should not have been an error upon closing the handle...", error);
    }

    ////////////////////////// CHAIN MADE IN TESTING ////////////////////////////

    @Test
    public void fullsweepIntegrationTest() {
        try {
            PubSubOptions options = new PubSubOptions(testServer, true, null, null);
            PubSubSDK pubsubSDK = PubSubSDK.getInstance();
            List<String> primary = primaryPermissions;
            List<String> secondary = secondaryPermissions;

            String channel = "BOOKS & MOVIES";

            pubsubSDK.connect(primary, options)
                .thenComposeAsync((handle) -> {
                    pubsubHandle = handle;

                    pubsubHandle.onErrorResponse(this::onErrorResponse);
                    pubsubHandle.onError(this::onError);
                    pubsubHandle.onNewSession(this::onNewSession);
                    pubsubHandle.onReconnect(this::onReconnect);
                    pubsubHandle.onRawRecord(this::onRawRecord);
                    pubsubHandle.onMessage(this::onMessage);
                    pubsubHandle.onClose(this::onClose);

                    return pubsubHandle.subscribe(testChannels.get("health"), this::healthHandler);
                })
                .thenComposeAsync((subscriptions) -> {
                    return pubsubHandle.subscribe(testChannels.get("books"), this::bookHandler);
                })
                .thenComposeAsync((subscriptions) -> {
                    return pubsubHandle.subscribe(testChannels.get("time"), this::timeHandler);
                })
                .thenComposeAsync((subscriptions) -> {
                    List<String> expectedChannels = Collections.synchronizedList(new ArrayList<>(testChannels.values()));
                    Collections.sort(expectedChannels);
                    Collections.sort(subscriptions);

                    try {
                        assertEquals(
                            "The complete list of subscriptions should match the expected channels...",
                            expectedChannels,
                            subscriptions
                        );
                    }
                    catch(AssertionError e) {
                        isError = true;
                        errorMessage = "Main Chain (Subscriptions): " + e.getMessage();
                        waitToFinish.countDown();
                    }

                    return pubsubHandle.publish(testChannels.get("books"), bookMessages.get("desired"));
                })
                .thenComposeAsync((sequence) -> {
                    return pubsubHandle.unsubscribe(testChannels.get("books"));
                })
                .thenComposeAsync((subscriptions) -> {
                    Collections.sort(channels);
                    Collections.sort(subscriptions);

                    try {
                        assertEquals(
                            "The complete list of subscriptions should match the expected channels...",
                            channels,
                            subscriptions
                        );
                    }
                    catch(AssertionError e) {
                        isError = true;
                        errorMessage = "Main Chain (Subscriptions): " + e.getMessage();
                        waitToFinish.countDown();
                    }

                    return pubsubHandle.publish(testChannels.get("books"), bookMessages.get("filtered"));
                })
                .thenComposeAsync((sequence) -> {
                    return pubsubHandle.listSubscriptions();
                })
                .thenComposeAsync((subscriptions) -> {
                    Collections.sort(channels);
                    Collections.sort(subscriptions);

                    try {
                        assertEquals(
                            "The complete list of subscriptions should match the expected channels...",
                            channels,
                            subscriptions
                        );
                    }
                    catch(AssertionError e) {
                        isError = true;
                        errorMessage = "Main Chain (Subscriptions): " + e.getMessage();
                        waitToFinish.countDown();
                    }

                    System.out.println("Before Dropping: " + subscriptions);

                    return pubsubHandle.publishWithAck(testChannels.get("health"), healthMessage);
                })
                .thenComposeAsync((uuid) -> {
                    try {
                        boolean completed = firstHealthMessage.await(2, TimeUnit.SECONDS);
                        
                        if(!completed) {
                            isError = true;
                            errorMessage = "Did not receive the published health message...";
                        }
                        else {
                            assertEquals(
                                "The published health message and the acknowledged message should be the same...",
                                messageId.toString(),
                                uuid.toString()
                            );
                        }
                    }
                    catch(InterruptedException e) {
                        isError = true;
                        errorMessage = "Interrupted waiting for the published health message: " + e.getMessage();
                    }
                    catch(AssertionError ex) {
                        isError = true;
                        errorMessage = "Published healht message problem: " + ex.getMessage();
                    }

                    return pubsubHandle.getSessionUuid();
                })
                .thenAcceptAsync((uuid) -> {
                    sessionId = uuid;
                    pubsubHandle.dropConnection(new PubSubDropConnectionOptions(1));
                })
                .exceptionally((error) -> {
                    errorMessage = error.getMessage();
                    isError = true;
                    waitToFinish.countDown();
                    return null;
                });

            try {
                boolean completed = waitToFinish.await(5, TimeUnit.SECONDS);
                assertTrue("Test Timed Out...", completed);

                if(isError) {
                    fail("There was an exception: " + errorMessage);
                }
            }
            catch(InterruptedException e) {
                fail("The test was unable to finish properly.");
            }
        }
        catch(Throwable e) {
            ;
            fail("There was an exception thrown: " + e.getMessage());
            e.printStackTrace();
        }
    }
}