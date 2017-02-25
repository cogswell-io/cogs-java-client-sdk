package com.gambit.sdk.pubsub;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import java.util.Collections;
import java.util.Arrays;
import java.util.Vector;
import java.util.List;
import java.util.UUID;

import java.util.Random;

import com.gambit.sdk.pubsub.handlers.PubSubMessageHandler;

import org.json.JSONObject;

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

public class PubSubIntegrationTests {

    private static String testServer = "*";
    private static List<String> primaryPermissions = null;
    private static List<String> secondaryPermissions = null;

    private static PubSubHandle pubsubHandle;
    private static PubSubHandle secondHandle;
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
        secondHandle = null;
        errorMessage = "";
        isError = false;
    }

    @After
    public void tearDownAfterEach() {
        pubsubHandle = null;
        secondHandle = null;
        errorMessage = "";
        isError = false;
    }

    @Test
    public void testSubscribeAndUnsubscribe() {
        try {
            PubSubOptions options = new PubSubOptions(testServer, null, null, null);
            PubSubSDK pubsubSDK = PubSubSDK.getInstance();
            List<String> permissions = primaryPermissions;

            CountDownLatch signal = new CountDownLatch(1);
            String channel = "BOOKS & MOVIES";

            pubsubSDK.connect(permissions, options)
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
                .thenAcceptAsync((subscriptions) -> {
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
            PubSubOptions options = new PubSubOptions(testServer, null, null, null);
            PubSubSDK pubsubSDK = PubSubSDK.getInstance();
            List<String> permissions = primaryPermissions;

            CountDownLatch signal = new CountDownLatch(1);
            String testChan = "BOOKS & MOVIES";
            String testMsg = "Out Now: The Thriller of the Century. Find it near you!";

            pubsubSDK.connect(permissions, options)
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
            PubSubOptions options = new PubSubOptions(testServer, null, null, null);
            PubSubSDK pubsubSDK = PubSubSDK.getInstance();
            List<String> permissions = primaryPermissions;

            CountDownLatch signal = new CountDownLatch(1);
            
            String[] testChans = { "BOOKS & MOVIES", "ARTS & CRAFTS", "SELF-IMPROVEMENT" };
            AtomicInteger index = new AtomicInteger(0);

            pubsubSDK.connect(permissions, options)
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
            PubSubOptions options = new PubSubOptions(testServer, null, null, null);
            PubSubSDK pubsubSDK = PubSubSDK.getInstance();
            List<String> permissions = primaryPermissions;

            CountDownLatch signal = new CountDownLatch(1);
            String subscribeChan = "BOOKS & MOVIES";
            String subscribeMessage = "A good book made into a good movie is a good thing, but it doesn't happen often.";
            String publishChan = "SPORTS";
            String publishMessage = "The Super Bowl will not be as big an event this year... Or will it?";

            pubsubSDK.connect(permissions, options)
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
            PubSubOptions options = new PubSubOptions(testServer, null, null, null);
            PubSubSDK pubsubSDK = PubSubSDK.getInstance();
            List<String> permissionsOne = primaryPermissions;
            List<String> permissionsTwo = secondaryPermissions;

            CountDownLatch signal = new CountDownLatch(2);
            String chan = "BOOKS & MOVIES";
            String message = "A good book made into a good movie is a good thing, but it doesn't happen often.";

            StringBuffer msgFromOne = new StringBuffer();
            StringBuffer msgFromTwo = new StringBuffer();

            pubsubSDK.connect(permissionsOne, options)
                .thenComposeAsync((handle) -> {
                    pubsubHandle = handle;

                    return pubsubSDK.connect(permissionsTwo, options);
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
                .thenAcceptAsync((channels) -> {
                    secondHandle.close();
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
            PubSubOptions options = new PubSubOptions(testServer, null, null, null);
            PubSubSDK pubsubSDK = PubSubSDK.getInstance();
            List<String> permissions = primaryPermissions;
 
            CountDownLatch signal = new CountDownLatch(2);
            String chan = "BOOKS & MOVIES";
            String message = "A good book made into a good movie is a good thing, but it doesn't happen often.";

            StringBuffer uuidFromMessage = new StringBuffer();
            StringBuffer uuidFromPublish = new StringBuffer();

            pubsubSDK.connect(permissions, options)
                .thenComposeAsync((handle) -> {
                    pubsubHandle = handle;
                    
                    return pubsubHandle.subscribe(chan, (record) -> {
                        uuidFromMessage.append(record.getId().toString());
                        signal.countDown();
                    });
                })
                .thenComposeAsync((subs) -> {
                    return pubsubHandle.publishWithAck(chan, message);
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
    public void testGetSession() {
        try {
            PubSubOptions firstOptions = new PubSubOptions(testServer, null, null, null);
            PubSubSDK pubsubSDK = PubSubSDK.getInstance();

            AtomicReference<UUID> sessionId = new AtomicReference<>(UUID.randomUUID());
            CountDownLatch signal = new CountDownLatch(1);

            pubsubSDK.connect(primaryPermissions, firstOptions)
                .thenComposeAsync((handle) -> {
                    pubsubHandle = handle;

                    return pubsubHandle.getSessionUuid();
                })
                .thenComposeAsync((uuid) -> {
                    sessionId.set(uuid);

                    return pubsubHandle.close();
                })
                .thenComposeAsync((subs) -> {
                    PubSubOptions secondOptions = new PubSubOptions(testServer, null, null, sessionId.get());

                    return pubsubSDK.connect(primaryPermissions, secondOptions);
                })
                .thenComposeAsync((handle) -> {
                    pubsubHandle = handle;

                    return pubsubHandle.getSessionUuid();
                })
                .thenComposeAsync((uuid) -> {
                    try {
                        assertEquals(
                            "The session UUID upon connecting the second time should have been the same...",
                            sessionId.get().toString(),
                            uuid.toString()
                        );
                    }
                    catch(AssertionError e) {
                        isError = true;
                        errorMessage = "Sessions did not match: " + e.getMessage();
                    }

                    return pubsubHandle.close();
                })
                .thenAcceptAsync((voided) -> {
                    signal.countDown();
                })
                .exceptionally((error) -> {
                    isError = true;
                    errorMessage = error.getMessage();
                    signal.countDown();
                    return null;
                });

            try {
                signal.await();

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

    @Test
    public void testReconnectOnConnectionDrop() {
        try {
            PubSubOptions options = new PubSubOptions(testServer, null, null, null);
            PubSubSDK pubsubSDK = PubSubSDK.getInstance();
            List<String> permissions = primaryPermissions;

            AtomicBoolean wasCalled = new AtomicBoolean(false);
            CountDownLatch signal = new CountDownLatch(1);

            pubsubSDK.connect(permissions, options)
                .thenAcceptAsync((handle) -> {
                    pubsubHandle = handle;

                    pubsubHandle.onReconnect(() -> {
                        wasCalled.set(true);
                        signal.countDown();
                    });

                    pubsubHandle.dropConnection(new PubSubDropConnectionOptions(0));
                })
                .exceptionally((error) -> {
                    isError = true;
                    errorMessage = error.getMessage();
                    signal.countDown();
                    return null;
                });

            try {
                signal.await(5, TimeUnit.SECONDS);

                assertTrue(
                    "The reconnect handler should have been called.",
                    wasCalled.get()
                );

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

    @Test
    public void testSameSessionAfterDroppedConnection() {
        try {
            PubSubOptions options = new PubSubOptions(testServer, null, null, null);
            PubSubSDK pubsubSDK = PubSubSDK.getInstance();
            List<String> permissions = primaryPermissions;

            AtomicReference<String> uuidRef = new AtomicReference<>();
            AtomicInteger timesCalled = new AtomicInteger(0);
            CountDownLatch signal = new CountDownLatch(1);

            pubsubSDK.connect(permissions, options)
                .thenComposeAsync((handle) -> {
                    pubsubHandle = handle;

                    pubsubHandle.onReconnect(() -> {
                        pubsubHandle.getSessionUuid()
                            .thenAcceptAsync((uuid) -> {
                                try {
                                    assertEquals(
                                        "The reconnected session and the one received previously should match.",
                                        uuidRef.get().toString(),
                                        uuid.toString()
                                    );
                                }
                                catch(AssertionError e) {
                                    isError = true;
                                    errorMessage = e.getMessage();
                                }

                                signal.countDown();
                            })
                            .exceptionally((error) -> {
                                isError = true;
                                errorMessage = error.getMessage();
                                signal.countDown();
                                return null;
                            });
                    });

                    pubsubHandle.onNewSession((uuid) -> {
                        timesCalled.getAndIncrement();
                    });

                    return pubsubHandle.getSessionUuid();
                })
                .thenAcceptAsync((uuid) -> {
                    uuidRef.set(uuid.toString());

                    pubsubHandle.dropConnection(new PubSubDropConnectionOptions(0));
                })
                .exceptionally((error) -> {
                    isError = true;
                    errorMessage = error.getMessage();
                    signal.countDown();
                    return null;
                });

            try {
                signal.await(5, TimeUnit.SECONDS);

                assertTrue(
                    "The new session handler should not have been called.",
                    timesCalled.get() == 1L
                );

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

    @Test
    public void testCloseEventIsEmitted() {
        try {
            PubSubOptions options = new PubSubOptions(testServer, null, null, null);
            CountDownLatch signal = new CountDownLatch(1);

            PubSubSDK.getInstance().connect(primaryPermissions, options)
                .thenComposeAsync((handle) -> {
                    pubsubHandle = handle;

                    pubsubHandle.onClose(voided -> signal.countDown());

                    return pubsubHandle.close();
                })
                .thenAcceptAsync(voided -> { /* Do Nothing */ })
                .exceptionally((error) -> {
                    errorMessage = error.getMessage();
                    isError = true;
                    signal.countDown();
                    
                    return null;
                });

            try {
                boolean completed = signal.await(2, TimeUnit.SECONDS);

                if(!completed) {
                    fail("The test timed out before it could complete...");
                }

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
    public void testPublishAndReceiveAfterReconnect() {
        try {
            PubSubOptions options = new PubSubOptions(testServer, null, null, null);
            PubSubSDK pubsubSDK = PubSubSDK.getInstance();

            AtomicReference<UUID> messageId = new AtomicReference<>(UUID.randomUUID());
            AtomicReference<UUID> sessionId = new AtomicReference<>(UUID.randomUUID());
            CountDownLatch received = new CountDownLatch(1);
            CountDownLatch signal = new CountDownLatch(1);
            String channel = "Test Channel: Dead Beef";
            String message = "Test the Reconnect & Publish!!!";

            pubsubSDK.connect(primaryPermissions, options)
                .thenComposeAsync((handle) -> {
                    pubsubHandle = handle;

                    pubsubHandle.onReconnect(() -> {
                        pubsubHandle.publish(channel, message)
                            .thenAcceptAsync(seq -> {
                                try {
                                    boolean completed = received.await(2, TimeUnit.SECONDS);
                                    assertTrue("The test timed out. No message received.", completed);
                                }
                                catch(InterruptedException e) {
                                    isError = true;
                                    errorMessage = e.getMessage();
                                }
                                catch(AssertionError e) {
                                    isError = true;
                                    errorMessage = e.getMessage();
                                }

                                signal.countDown();
                            })
                            .exceptionally((error) -> {
                                isError = true;
                                errorMessage = error.getMessage();
                                signal.countDown();
                                return null;
                            });
                    });

                    return pubsubHandle.subscribe(channel, (record) -> received.countDown());
                })
                .thenComposeAsync((subscriptions) -> {
                    return pubsubHandle.getSessionUuid();
                })
                .thenAcceptAsync((uuid) -> {
                    sessionId.set(uuid);
                    pubsubHandle.dropConnection(new PubSubDropConnectionOptions(0));
                })
                .exceptionally((error) -> {
                    isError = true;
                    errorMessage = error.getMessage();
                    signal.countDown();
                    return null;
                });

            try {
                boolean completed = signal.await(5, TimeUnit.SECONDS);

                assertTrue("The test timed out before failing", completed);
                assertFalse("An error occurred: " + errorMessage, isError);
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