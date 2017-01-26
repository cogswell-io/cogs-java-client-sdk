package com.gambit.sdk.pubsub;

import javax.websocket.*;

import java.util.concurrent.atomic.AtomicInteger;

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
        PubSubHandle testHandle = new PubSubHandle(new TestPubSubSocketSuccess());
        CountDownLatch signal = new CountDownLatch(1);

        testHandle.getSessionUuid()
            .thenAcceptAsync((uuid) -> {
                assertEquals(
                    "The returned UUID should match the expected UUID",
                    TestPubSubSocketSuccess.FAKE_UUID,
                    uuid
                );

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

            if(hasError) {
                fail(errorMessage);
            }
        }
        catch(InterruptedException e) {
            fail("There was an error waiting for the test to finish: " + e.getMessage());
        }
    }

    @Test
    public void testSubscribeSuccessful() {
        PubSubHandle testHandle = new PubSubHandle(new TestPubSubSocketSuccess());
        CountDownLatch signal = new CountDownLatch(1);

        PubSubMessageHandler handler = new PubSubMessageHandler() {
            @Override
            public void onMessage(PubSubMessageRecord record) {}
        };

        String channel = "This is a test...";

        testHandle.subscribe(channel, handler)
            .thenAcceptAsync((subscriptions) -> {
                assertTrue(
                    "The channel list returned should contain the channel passed in.",
                    subscriptions.contains(channel)
                );

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

            if(hasError) {
                fail(errorMessage);
            }
        }
        catch(InterruptedException e) {
            fail("There was an error waiting for the test to finish: " + e.getMessage());
        }
    }

    @Test
    public void testUnsubscribeSuccessful() {
        PubSubHandle testHandle = new PubSubHandle(new TestPubSubSocketSuccess());
        CountDownLatch signal = new CountDownLatch(1);

        String channel = "This is a test..." + UUID.randomUUID().toString();

        testHandle.subscribe(channel, (record) -> {})
            .thenComposeAsync((subscriptions) -> {
                assertTrue(
                    "The channel list returned should contain the channel passed in.",
                    subscriptions.contains(channel)
                );

                return testHandle.unsubscribe(channel);
            })
            .thenAcceptAsync((subscriptions) -> {
                assertTrue(
                    "The channel list returned should NOT contain the channel which was unsubscribed.",
                    !subscriptions.contains(channel)
                );

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

            if(hasError) {
                fail(errorMessage);
            }
        }
        catch(InterruptedException e) {
            fail("There was an error waiting for the test to finish: " + e.getMessage());
        }
    }

    @Test
    public void testListSubscriptionsSuccessful() {
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
                assertTrue(
                    "The provided list should contain all of the original subscriptions.",
                    subscriptions.containsAll(Arrays.asList(channels))
                );

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

            if(hasError) {
                fail(errorMessage);
            }
        }
        catch(InterruptedException e) {
            fail("There was an error waiting for the test to finish: " + e.getMessage());
        }
    }

    @Test
    public void testPublishSuccessful() {
        PubSubHandle testHandle = new PubSubHandle(new TestPubSubSocketSuccess());
        CountDownLatch signal = new CountDownLatch(1);

        String channel = "COOKING";
        String message = "Next Week: We show you how to clean and cook with leeks.";

        testHandle.subscribe(channel, (record) -> {
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
        })
        .thenComposeAsync((subscriptions) -> {
            return testHandle.publish(channel, message);
        })
        .thenAcceptAsync((sequence) -> {
            // Do nothing
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

    @Test
    public void testUnsubscribeAllSuccessful() {
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
                assertTrue(
                    "The subscriptions list should be completely empty now.",
                    subscriptions.isEmpty()
                );

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

            if(hasError) {
                fail(errorMessage);
            }
        }
        catch(InterruptedException e) {
            fail("There was an error waiting for the test to finish: " + e.getMessage());
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
        outcome.complete(new JSONObject());

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

        return outcome;
    }
}