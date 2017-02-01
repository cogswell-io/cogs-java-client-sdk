package com.gambit.sdk.pubsub;

import javax.websocket.*;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import java.util.Collections;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Hashtable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.time.Instant;

import com.gambit.sdk.pubsub.handlers.PubSubMessageHandler;

import org.json.JSONObject;
import org.json.JSONArray;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class TestPubSubHandleSuccess {

    private static boolean hasError = false;
    private static String errorMessage = "";

    @Before
    public void setupBeforeEach() {
        hasError = false;
        errorMessage = "";
    }

    @Test
    public void testGetSessionSuccessful() {
        try {
            PubSubHandle testHandle = new PubSubHandle(new TestPubSubSocketSuccess());
            CountDownLatch signal = new CountDownLatch(1);

            testHandle.getSessionUuid()
                .thenAcceptAsync((uuid) -> {
                    try {
                        assertEquals(
                            "The returned UUID should match the expected UUID",
                            TestPubSubSocketSuccess.FAKE_UUID,
                            uuid
                        );

                        signal.countDown();
                    }
                    catch(AssertionError e) {
                        throw new CompletionException(e);
                    }
                })
                .exceptionally((error) -> {
                    hasError = true;
                    errorMessage = error.getCause().getMessage(); 
                    signal.countDown();

                    return null;
                });

            try {
                signal.await();

                if(hasError) {
                    fail(errorMessage);
                }
            }
            catch(InterruptedException e) {
                fail("There was an error waiting for the test to finish: " + e.getMessage());
            }
        }
        catch(Throwable e) {
            fail("There was an exception thrown: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testSubscribeSuccessful() {
        try {
            TestPubSubSocketSuccess socket = new TestPubSubSocketSuccess();
            PubSubHandle testHandle = new PubSubHandle(socket);
            CountDownLatch signal = new CountDownLatch(1);

            PubSubMessageHandler handler = new PubSubMessageHandler() {
                @Override
                public void onMessage(PubSubMessageRecord record) {}
            };

            String channel = "This is a test...";

            testHandle.subscribe(channel, handler)
                .thenAcceptAsync((subscriptions) -> {
                    try {
                        assertTrue(
                            "The channel list returned should contain the channel passed in.",
                            subscriptions.contains(channel)
                        );

                        assertSame(
                            "The message handler found in the PubSubSocket should be the one given.",
                            handler,
                            socket.getMessageHandler(channel)
                        );

                        signal.countDown();
                    }
                    catch(AssertionError e) {
                        throw new CompletionException(e);
                    }
                })
                .exceptionally((error) -> {
                    hasError = true;
                    errorMessage = error.getCause().getMessage();
                    signal.countDown();
                    return null;
                });

            try {
                signal.await();

                if(hasError) {
                    fail(errorMessage);
                }
            }
            catch(InterruptedException e) {
                fail("There was an error waiting for the test to finish: " + e.getMessage());
            }
        }
        catch(Throwable ex) {
            fail("There was an exception thrown: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Test
    public void testUnsubscribeSuccessful() {
        try {
            PubSubHandle testHandle = new PubSubHandle(new TestPubSubSocketSuccess());
            CountDownLatch signal = new CountDownLatch(1);

            String channel = "This is a test..." + UUID.randomUUID().toString();

            testHandle.subscribe(channel, (record) -> {})
                .thenComposeAsync((subscriptions) -> {
                    try {
                        assertTrue(
                            "The channel list returned should contain the channel passed in.",
                            subscriptions.contains(channel)
                        );
                    }
                    catch(AssertionError e) {
                        throw new CompletionException(e);
                    }

                    return testHandle.unsubscribe(channel);
                })
                .thenAcceptAsync((subscriptions) -> {
                    try {
                        assertTrue(
                            "The channel list returned should NOT contain the channel which was unsubscribed.",
                            !subscriptions.contains(channel)
                        );

                        signal.countDown();
                    }
                    catch(AssertionError e) {
                        throw new CompletionException(e);
                    }
                })
                .exceptionally((error) -> {
                    hasError = true;
                    errorMessage = error.getCause().getMessage();
                    signal.countDown();
                    return null;
                });

            try {
                signal.await();

                if(hasError) {
                    fail(errorMessage);
                }
            }
            catch(InterruptedException e) {
                fail("There was an error waiting for the test to finish: " + e.getMessage());
            }
        }
        catch(Throwable ex) {
            fail("There was an exception thrown: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Test
    public void testListSubscriptionsSuccessful() {
        try {
            PubSubHandle testHandle = new PubSubHandle(new TestPubSubSocketSuccess());
            CountDownLatch signal = new CountDownLatch(1);

            String[] channels = { "MOVIES & BOOKS", "DIY", "POSITIVE THINKING" };
            AtomicInteger index = new AtomicInteger(0);

            testHandle.subscribe(channels[index.getAndIncrement()], (record) -> {})
                .thenComposeAsync((subscriptions) -> {
                    return testHandle.subscribe(channels[index.getAndIncrement()], (record) -> {});
                })
                .thenComposeAsync((subscriptions) -> {
                    return testHandle.subscribe(channels[index.getAndIncrement()], (record) -> {});
                })
                .thenComposeAsync((subscriptions) -> {
                    return testHandle.listSubscriptions();
                })
                .thenAcceptAsync((subscriptions) -> {
                    try {
                        assertTrue(
                            "The provided list should contain all of the original subscriptions.",
                            subscriptions.containsAll(Arrays.asList(channels))
                        );

                        signal.countDown();
                    }
                    catch(AssertionError e) {
                        throw new CompletionException(e);
                    }
                })
                .exceptionally((error) -> {
                    hasError = true;
                    errorMessage = error.getCause().getMessage();
                    signal.countDown();
                    return null;
                });

            try {
                signal.await();

                if(hasError) {
                    fail(errorMessage);
                }
            }
            catch(InterruptedException e) {
                fail("There was an error waiting for the test to finish: " + e.getMessage());
            }
        }
        catch(Throwable ex) {
            fail("There was an exception thrown: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Test
    public void testPublishSuccessful() {
        try {
            PubSubHandle testHandle = new PubSubHandle(new TestPubSubSocketSuccess());
            CountDownLatch signal = new CountDownLatch(1);

            String channel = "COOKING";
            String message = "Next Week: We show you how to clean and cook with leeks.";

            testHandle.subscribe(channel, (record) -> {
                try {
                    assertEquals(
                        "The record message content should match the message that was actually published.",
                        message,
                        record.getMessage()
                    );

                    assertEquals(
                        "The record channel should match the channel on which the message was actually published.",
                        channel,
                        record.getChannel()
                    );

                    signal.countDown();
                }
                catch(AssertionError e) {
                    throw new CompletionException(e);
                }
            })
            .thenComposeAsync((subscriptions) -> {
                return testHandle.publish(channel, message, null);
            })
            .thenAcceptAsync((sequence) -> {
                // Reaching here is an okay thing.
            })
            .exceptionally((error) -> {
                hasError = true;
                errorMessage = "There was an error with completion: " + error.getMessage();
                signal.countDown();
                return null;
            });

            try {
                signal.await();

                if(hasError) {
                    fail(errorMessage);
                }
            }
            catch(InterruptedException e) {
                fail("There was an error waiting for the test to finish: " + e.getMessage());
            }
        }
        catch(Throwable ex) {
            fail("There was an exception thrown: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Test
    public void testPublishWithAckSuccessful() {
        try {
            PubSubHandle testHandle = new PubSubHandle(new TestPubSubSocketSuccess());
            CountDownLatch signal = new CountDownLatch(2);

            String channel = "COOKING";
            String message = "Next Week: We show you how to clean and cook with leeks.";
            StringBuffer publishAck = new StringBuffer();
            StringBuffer messageUuid = new StringBuffer();

            testHandle.subscribe(channel, (record) -> {
                try {
                    assertEquals(
                        "The record message content should match the message that was actually published.",
                        message,
                        record.getMessage()
                    );

                    assertEquals(
                        "The record channel should match the channel on which the message was actually published.",
                        channel,
                        record.getChannel()
                    );

                    messageUuid.append(record.getId().toString());

                    signal.countDown();
                }
                catch(AssertionError e) {
                    throw new CompletionException(e);
                }
            })
            .thenComposeAsync((subscriptions) -> {
                return testHandle.publishWithAck(channel, message, null);
            })
            .thenAcceptAsync((uuid) -> {
                publishAck.append(uuid.toString());
                signal.countDown();
            })
            .exceptionally((error) -> {
                hasError = true;
                errorMessage = "There was an error with completion: " + error.getMessage();
                signal.countDown();
                return null;
            });

            try {
                signal.await();

                assertEquals(
                    "The string-represented uuids from the message in publishing and receiving should be the same.",
                    publishAck.toString(),
                    messageUuid.toString()
                );

                if(hasError) {
                    fail(errorMessage);
                }
            }
            catch(InterruptedException e) {
                fail("There was an error waiting for the test to finish: " + e.getMessage());
            }
            catch(AssertionError err) {
                fail("The strings were not appropriate equal: " + err.getMessage());
            }
        }
        catch(Throwable ex) {
            fail("There was an exception thrown: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Test
    public void testUnsubscribeAllSuccessful() {
        try {
            PubSubHandle testHandle = new PubSubHandle(new TestPubSubSocketSuccess());
            CountDownLatch signal = new CountDownLatch(1);

            String[] channels = { "PEOPLE", "PLACES", "THINGS" };
            AtomicInteger index = new AtomicInteger(0);

            testHandle.subscribe(channels[index.getAndIncrement()], (record) -> {})
                .thenComposeAsync((subscriptions) -> {
                    return testHandle.subscribe(channels[index.getAndIncrement()], (record) -> {});
                })
                .thenComposeAsync((subscriptions) -> {
                    return testHandle.subscribe(channels[index.getAndIncrement()], (record) -> {});
                })
                .thenComposeAsync((subscriptions) -> {
                    return testHandle.unsubscribeAll();
                })
                .thenAcceptAsync((subscriptions) -> {
                    try {
                        assertTrue(
                            "The subscriptions list should be completely empty now.",
                            subscriptions.isEmpty()
                        );

                        signal.countDown();
                    }
                    catch(AssertionError e) {
                        throw new CompletionException(e);
                    }
                })
                .exceptionally((error) -> {
                    hasError = true;
                    errorMessage = "There was an error with completion: " + error.getMessage();
                    signal.countDown();
                    return null;
                });

            try {
                signal.await();

                if(hasError) {
                    fail(errorMessage);
                }
            }
            catch(InterruptedException e) {
                fail("There was an error waiting for the test to finish: " + e.getMessage());
            }
        }
        catch(Throwable ex) {
            fail("There was an exception thrown: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}

class TestPubSubSocketSuccess extends PubSubSocket
{
    public static final UUID FAKE_UUID = UUID.fromString("85b2c602-e329-11e6-bf01-fe55135034f3");
    public static List<String> subscriptions = Collections.synchronizedList(new LinkedList<>());
    public static Map<String, PubSubMessageHandler> handlers = Collections.synchronizedMap(new Hashtable<>());

    public TestPubSubSocketSuccess() {
        subscriptions.clear();
    }

    public void addMessageHandler(String channel, PubSubMessageHandler handler) {
        handlers.put(channel, handler);
    }

    public void removeMessageHandler(String channel) {
        handlers.remove(channel);
    }

    public PubSubMessageHandler getMessageHandler(String channel) {
        return handlers.get(channel);
    }

    protected CompletableFuture<JSONObject> sendRequest(long sequence, JSONObject json) {
        CompletableFuture<JSONObject> outcome = new CompletableFuture<>();
        String action = json.getString("action");
        
        JSONObject result = new JSONObject()
            .put("seq", sequence)
            .put("action", action)
            .put("code", 200);

        switch(action) {
            case "session-uuid": {
                result.put("uuid", FAKE_UUID.toString());
            }
            break;

            case "subscribe": {
                subscriptions.add(json.getString("channel"));
                result.put("channels", new JSONArray(subscriptions.toArray()) );
            }
            break;

            case "unsubscribe": {
                subscriptions.remove(json.getString("channel"));
                result.put("channels", new JSONArray(subscriptions.toArray()) );
            }
            break;

            case "subscriptions": {
                result.put("channels", new JSONArray(subscriptions.toArray()) );
            }
            break;

            case "unsubscribe-all": {
                subscriptions.clear();
                result.put("channels", new JSONArray(subscriptions.toArray()) );
            }
            break;

            default: {
            }
            break;
        }

        outcome.complete(result);
        return outcome;
    }

    protected CompletableFuture<JSONObject> sendPublish(long sequence, JSONObject json, SendHandler handler) {
        CompletableFuture<JSONObject> outcome = new CompletableFuture<>();

        String channel = json.getString("chan");
        String msg = json.getString("msg");

        JSONObject publishMessage = new JSONObject()
            .put("id", UUID.randomUUID().toString())
            .put("action", "msg")
            .put("time", Instant.now().toString())
            .put("chan", channel)
            .put("msg", msg);

        handlers.get(channel).onMessage(
            new PubSubMessageRecord(
                publishMessage.getString("chan"),
                publishMessage.getString("msg"),
                publishMessage.getString("time"),
                publishMessage.getString("id")
            )
        );

        outcome.complete(new JSONObject()
            .put("seq", sequence)
            .put("action", "pub")
            .put("code", 200)
            .put("id", publishMessage.getString("id"))
        );

        return outcome;
    }
}