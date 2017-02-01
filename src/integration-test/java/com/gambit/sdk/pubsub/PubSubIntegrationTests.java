package com.gambit.sdk.pubsub;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import java.util.Collections;
import java.util.Arrays;
import java.util.Vector;
import java.util.List;

import java.util.Random;

import com.gambit.sdk.pubsub.handlers.PubSubMessageHandler;

import org.json.JSONObject;

import static org.junit.Assert.*;
import org.junit.Test;

public class PubSubIntegrationTests {

    private static JSONObject getRandomProjectKey() {
        Random rand = new Random();

        String ident = Integer.toHexString(rand.nextInt());
        String read = Integer.toHexString(rand.nextInt());
        String write = Integer.toHexString(rand.nextInt());
        String admin = Integer.toHexString(rand.nextInt());

        if(ident.length() % 2 != 0) { ident += "f"; }
        if(read.length() % 2 != 0) { read += "f"; }
        if(write.length() % 2 != 0) { write += "f"; }
        if(admin.length() % 2 != 0) { admin += "f"; }

        JSONObject key = new JSONObject()
                .put("identity", ident)
                .put("read_key", read)
                .put("write_key", write)
                .put("admin_key", admin)
                .put("key_id", 1)
                .put("project_id", 1)
                .put("customer_id", 1)
                .put("enabled", true);

        return key;
    }

    private static List<String> buildPermissionKeys(JSONObject key) {
        List<String> permissions = new Vector<>();

        String ident = key.getString("identity");
        String read = key.getString("read_key");
        String write = key.getString("write_key");
        String admin = key.getString("admin_key");

        permissions.add(String.format("R-%s-%s", ident, read));
        permissions.add(String.format("W-%s-%s", ident, write));
        permissions.add(String.format("A-%s-%s", ident, admin));

        return permissions;
    }

    private static PubSubHandle pubsubHandle;
    private static PubSubHandle secondHandle;
    private static String errorMessage;
    private static boolean isError;

    @Test
    public void testSubscribeAndUnsubscribe() {
        try {
            KeyServer keyServer = new KeyServer("http://localhost:8778");
            PubSubSDK pubsubSDK = PubSubSDK.getInstance();

            JSONObject keys = getRandomProjectKey();

            String identity = keys.getString("identity");
            List<String> permissions = buildPermissionKeys(keys);

            CountDownLatch signal = new CountDownLatch(1);
            String channel = "BOOKS & MOVIES";

            keyServer.createKey(keys)
                .thenComposeAsync((result) -> {
                    return pubsubSDK.connect(permissions);
                })
                .thenComposeAsync((handle) -> {
                    pubsubHandle = handle;

                    return pubsubHandle.subscribe(channel, (record) -> {
                        fail("There should be no records received in this test.");
                    });
                })
                .thenComposeAsync((subscriptions) -> {
                    try {
                        assertEquals(
                            "There should be only one subscription: That to " + channel,
                            Collections.singletonList(channel),
                            subscriptions
                        );
                    }
                    catch(AssertionError e) {
                        isError = true;
                        errorMessage = e.getMessage();
                    }

                    return pubsubHandle.unsubscribe(channel);
                })
                .thenComposeAsync((subscriptions) -> {
                    try {
                        assertTrue(
                            "The list of subscriptions should be empty now...",
                            ((List<String>)subscriptions).isEmpty()
                        );
                    }
                    catch(AssertionError e) {
                        isError = true;
                        errorMessage = e.getMessage();
                    }

                    signal.countDown();

                    return keyServer.deleteKey(identity);
                })
                .thenAcceptAsync((result) -> {
                    signal.countDown();
                })
                .exceptionally((error) -> {
                    errorMessage = error.getMessage();
                    isError = true;
                    signal.countDown();
                    
                    return null;
                });

            try {
                signal.await();

                if(isError) {
                    fail("There was an exception: " + errorMessage);
                }
            }
            catch(InterruptedException e) {
                fail("The test was unable to finish properly.");
            }
        }
        catch(Throwable e) {
            fail("There was an exception thrown: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testReceiveMessageForSubscription() {
        try {
            KeyServer keyServer = new KeyServer("http://localhost:8778");
            PubSubSDK pubsub = PubSubSDK.getInstance();

            JSONObject key = getRandomProjectKey();
            String ident = key.getString("identity");
            List<String> permissionKeys = buildPermissionKeys(key);

            CountDownLatch signal = new CountDownLatch(1);
            String testChan = "BOOKS & MOVIES";
            String testMsg = "Out Now: The Thriller of the Century. Find it near you!";

            keyServer.createKey(key)
                .thenComposeAsync((result) -> {
                    return pubsub.connect(permissionKeys);
                })
                .thenComposeAsync((handle) -> {
                    pubsubHandle = handle;
                    
                    return pubsubHandle.subscribe(testChan, (record) -> {
                        try {
                            assertEquals(
                                "The channel published should be the one received.",
                                testChan, 
                                record.getChannel()
                            );

                            assertEquals(
                                "The message content received is that which was published.",
                                testMsg,
                                record.getMessage()
                            );
                        }
                        catch(AssertionError e) {
                            isError = true;
                            errorMessage = e.getMessage();
                        }

                        signal.countDown();
                    });
                })
                .thenComposeAsync((sequence) -> {
                    return pubsubHandle.publish(testChan, testMsg, null);
                })
                .exceptionally((error) -> {
                    return null;
                });

            try {
                signal.await();

                if(isError) {
                    fail("There was an exception: " + errorMessage);
                }
            }
            catch(InterruptedException e) {
                fail("INTERRUPTED WHILE WAITING FOR COUNTDOWN");
            }

            pubsubHandle.close()
                .thenComposeAsync((channels) -> {
                    return keyServer.deleteKey(ident);
                })
                .exceptionally((error) -> {
                    return null;
                });
        }
        catch(Throwable ex) {
            fail("There was an exception thrown: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Test
    public void testListingSubscriptions() {
        try {
            KeyServer keyServer = new KeyServer("http://localhost:8778");
            PubSubSDK pubsub = PubSubSDK.getInstance();

            JSONObject key = getRandomProjectKey();
            String ident = key.getString("identity");
            List<String> permissionKeys = buildPermissionKeys(key);

            CountDownLatch signal = new CountDownLatch(1);
            
            String[] testChans = { "BOOKS & MOVIES", "ARTS & CRAFTS", "SELF-IMPROVEMENT" };
            AtomicInteger index = new AtomicInteger(0);

            keyServer.createKey(key)
                .thenComposeAsync((result) -> {
                    return pubsub.connect(permissionKeys);
                })
                .thenComposeAsync((handle) -> {
                    pubsubHandle = handle;
                    
                    return pubsubHandle.subscribe(testChans[index.getAndIncrement()], (record) -> {});
                })
                .thenComposeAsync((subscriptions) -> {
                    return pubsubHandle.subscribe(testChans[index.getAndIncrement()], (record) -> {});
                })
                .thenComposeAsync((subscriptions) -> {
                    return pubsubHandle.subscribe(testChans[index.getAndIncrement()], (record) -> {});
                })
                .thenComposeAsync((subscriptions) -> {
                    return pubsubHandle.listSubscriptions();
                })
                .thenAcceptAsync((subscriptions) -> {
                    try {
                        assertEquals(
                            "The list of subscriptions returned should match the list that was subscribed.",
                            Arrays.asList(testChans),
                            subscriptions
                        );
                    }
                    catch(AssertionError e) {
                        isError = true;
                        errorMessage = e.getMessage();
                    }

                    signal.countDown();
                })
                .exceptionally((error) -> {
                    errorMessage = error.getMessage();
                    isError = true;
                    signal.countDown();

                    return null;
                });

            try {
                signal.await();

                if(isError) {
                    fail("There was an exception: " + errorMessage);
                }
            }
            catch(InterruptedException e) {
                fail("INTERRUPTED WHILE WAITING FOR COUNTDOWN");
            }

            pubsubHandle.close()
                .thenComposeAsync((channels) -> {
                    return keyServer.deleteKey(ident);
                })
                .exceptionally((error) -> {
                    return null;
                });
        }
        catch(Throwable ex) {
            fail("There was an exception thrown: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Test
    public void testNoReceiveOnUnsubscribedChannel() {
        try {
            KeyServer keyServer = new KeyServer("http://localhost:8778");
            PubSubSDK pubsub = PubSubSDK.getInstance();

            JSONObject key = getRandomProjectKey();
            String ident = key.getString("identity");
            List<String> permissionKeys = buildPermissionKeys(key);

            CountDownLatch signal = new CountDownLatch(1);
            String subscribeChan = "BOOKS & MOVIES";
            String subscribeMessage = "A good book made into a good movie is a good thing, but it doesn't happen often.";
            String publishChan = "SPORTS";
            String publishMessage = "The Super Bowl will not be as big an event this year... Or will it?";

            keyServer.createKey(key)
                .thenComposeAsync((result) -> {
                    return pubsub.connect(permissionKeys);
                })
                .thenComposeAsync((handle) -> {
                    pubsubHandle = handle;
                    
                    return pubsubHandle.subscribe(subscribeChan, (record) -> {
                        try {
                            assertEquals(
                                "The channel published should be the one received.",
                                subscribeChan, 
                                record.getChannel()
                            );

                            assertEquals(
                                "The message content received is that which was published.",
                                subscribeMessage,
                                record.getMessage()
                            );

                            assertTrue(
                                "The channel published should be the one to which was subscribed.",
                                record.getChannel().equals(subscribeChan)
                            );

                            assertTrue(
                                "The message content received should not have been published on another channel.",
                                !record.getMessage().equals(publishMessage)
                            );
                        }
                        catch(AssertionError e) {
                            isError = true;
                            errorMessage = e.getMessage();
                        }

                        signal.countDown();
                    });
                })
                .thenComposeAsync((chans) -> {
                    return pubsubHandle.publish(publishChan, publishMessage, null);
                })
                .thenAcceptAsync((sequence) -> {
                    pubsubHandle.publish(subscribeChan, subscribeMessage, null);
                })
                .exceptionally((error) -> {
                    return null;
                });

            try {
                signal.await();

                if(isError) {
                    fail("There was an exception: " + errorMessage);
                }
            }
            catch(InterruptedException e) {
                fail("INTERRUPTED WHILE WAITING FOR COUNTDOWN");
            }

            pubsubHandle.close()
                .thenComposeAsync((channels) -> {
                    return keyServer.deleteKey(ident);
                })
                .exceptionally((error) -> {
                    return null;
                });
        }
        catch(Throwable ex) {
            fail("There was an exception thrown: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Test
    public void testTwoHandlesReceiveSameMessage() {
        try {
            KeyServer keyServer = new KeyServer("http://localhost:8778");
            PubSubSDK pubsub = PubSubSDK.getInstance();

            JSONObject keyOne = getRandomProjectKey();
            String identOne = keyOne.getString("identity");
            List<String> permissionKeysOne = buildPermissionKeys(keyOne);

            JSONObject keyTwo = getRandomProjectKey();
            String identTwo = keyTwo.getString("identity");
            List<String> permissionKeysTwo = buildPermissionKeys(keyTwo);

            CountDownLatch signal = new CountDownLatch(2);
            String chan = "BOOKS & MOVIES";
            String message = "A good book made into a good movie is a good thing, but it doesn't happen often.";

            StringBuffer msgFromOne = new StringBuffer();
            StringBuffer msgFromTwo = new StringBuffer();

            keyServer.createKey(keyOne)
                .thenComposeAsync((result) -> {
                    return pubsub.connect(permissionKeysOne);
                })
                .thenComposeAsync((handle) -> {
                    pubsubHandle = handle;
                    return keyServer.createKey(keyTwo);
                })
                .thenComposeAsync((result) -> {
                    return pubsub.connect(permissionKeysTwo);
                })
                .thenComposeAsync((handle) -> {
                    secondHandle = handle;
                    
                    return pubsubHandle.subscribe(chan, (record) -> {
                        msgFromOne.append(record.getMessage());
                        signal.countDown();
                    });
                })
                .thenComposeAsync((subsOne) -> {
                    return secondHandle.subscribe(chan, (record) -> {
                        msgFromTwo.append(record.getMessage());
                        signal.countDown();
                    });
                })
                .thenAcceptAsync((sequence) -> {
                    pubsubHandle.publish(chan, message, null);
                })
                .exceptionally((error) -> {
                    return null;
                });

            try {
                signal.await();

                assertEquals(
                    "The two messages received on different sockets should be the same message.",
                    msgFromOne.toString(),
                    msgFromTwo.toString()
                );

                if(isError) {
                    fail("There was an exception: " + errorMessage);
                }
            }
            catch(InterruptedException e) {
                fail("INTERRUPTED WHILE WAITING FOR COUNTDOWN");
            }

            pubsubHandle.close()
                .thenComposeAsync((channels) -> {
                    return secondHandle.close();
                })
                .thenComposeAsync((channels) -> {
                    return keyServer.deleteKey(identOne);
                })
                .thenAcceptAsync((response) -> {
                    keyServer.deleteKey(identTwo);
                })
                .exceptionally((error) -> {
                    return null;
                });
        }
        catch(Throwable ex) {
            fail("There was an exception thrown: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Test
    public void testPublishWithAck() {
        try {
            KeyServer keyServer = new KeyServer("http://localhost:8778");
            PubSubSDK pubsub = PubSubSDK.getInstance();

            JSONObject key = getRandomProjectKey();
            String ident = key.getString("identity");
            List<String> permissionKeys = buildPermissionKeys(key);

            CountDownLatch signal = new CountDownLatch(2);
            String chan = "BOOKS & MOVIES";
            String message = "A good book made into a good movie is a good thing, but it doesn't happen often.";

            StringBuffer uuidFromMessage = new StringBuffer();
            StringBuffer uuidFromPublish = new StringBuffer();

            keyServer.createKey(key)
                .thenComposeAsync((result) -> {
                    return pubsub.connect(permissionKeys);
                })
                .thenComposeAsync((handle) -> {
                    pubsubHandle = handle;
                    
                    return pubsubHandle.subscribe(chan, (record) -> {
                        uuidFromMessage.append(record.getId().toString());
                        signal.countDown();
                    });
                })
                .thenComposeAsync((subs) -> {
                    return pubsubHandle.publishWithAck(chan, message, null);
                })
                .thenAcceptAsync((ack) -> {
                    uuidFromPublish.append(ack.toString());
                    signal.countDown();
                })
                .exceptionally((error) -> {
                    return null;
                });

            try {
                signal.await();

                assertEquals(
                    "The two messages received on different sockets should be the same message.",
                    uuidFromMessage.toString(),
                    uuidFromPublish.toString()
                );

                if(isError) {
                    fail("There was an exception: " + errorMessage);
                }
            }
            catch(InterruptedException e) {
                fail("INTERRUPTED WHILE WAITING FOR COUNTDOWN");
            }

            pubsubHandle.close()
                .thenAcceptAsync((channels) -> {
                    keyServer.deleteKey(ident);
                })
                .exceptionally((error) -> {
                    return null;
                });
        }
        catch(Throwable ex) {
            fail("There was an exception thrown: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}