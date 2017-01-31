package com.gambit.sdk.pubsub;

import javax.websocket.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.Collections;
import java.util.LinkedList;
import java.util.UUID;
import java.util.List;

import org.json.JSONObject;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class TestPubSubHandleFailure {

    private static boolean hasError = false;
    private static String errorMessage = "";

    @Before
    public void setupBeforeEach() {
        hasError = false;
        errorMessage = "";
    }

    @Test
    public void testGetSessionFailure() {
        try {
            PubSubHandle testHandle = new PubSubHandle(new TestPubSubSocketFailure());
            CountDownLatch signal = new CountDownLatch(1);

            testHandle.getSessionUuid()
                .thenAcceptAsync((uuid) -> {
                    hasError = true;
                    errorMessage = "There should not have been a UUID at all: " + uuid.toString(); 
                    signal.countDown();
                })
                .exceptionally((error) -> {
                    try {
                        assertEquals(
                            "The message in the cause of the completion exception should be about the session.",
                            TestPubSubSocketFailure.ExceptionType.SESSION.toString(),
                            error.getCause().getMessage()
                        );
                    }
                    catch(AssertionError e) {
                        hasError = true;
                        errorMessage = e.getMessage();
                    }

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
    public void testSubscribeFailure() {
        try {
            PubSubHandle testHandle = new PubSubHandle(new TestPubSubSocketFailure());
            CountDownLatch signal = new CountDownLatch(1);

            String channel = "FITNESS";

            testHandle.subscribe(channel, (record) -> {
                hasError = true;
                errorMessage = "No message should have been published or received.";
                signal.countDown();
            })
            .thenAcceptAsync((subscriptions) -> {
                hasError = true;
                errorMessage = "There should have not been any subscriptions made: " + subscriptions.toString(); 
                signal.countDown();
            })
            .exceptionally((error) -> {
                try {
                    assertEquals(
                        "The message in the cause of the completion exception should be about the session.",
                        TestPubSubSocketFailure.ExceptionType.SUBSCRIBE.toString(),
                        error.getCause().getMessage()
                    );
                }
                catch(AssertionError e) {
                    hasError = true;
                    errorMessage = e.getMessage();
                }
                
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
    public void testUnsubscribeFailure() {
        try {
            PubSubHandle testHandle = new PubSubHandle(new TestPubSubSocketFailure());
            CountDownLatch signal = new CountDownLatch(1);

            String channel = "FITNESS";

            testHandle.unsubscribe(channel)
                .thenAcceptAsync((subscriptions) -> {
                    hasError = true;
                    errorMessage = "There should have not been any subscriptions made: " + subscriptions.toString(); 
                    signal.countDown();
                })
                .exceptionally((error) -> {
                    try {
                        assertEquals(
                            "The message in the cause of the completion exception should be about the session.",
                            TestPubSubSocketFailure.ExceptionType.UNSUBSCRIBE.toString(),
                            error.getCause().getMessage()
                        );
                    }
                    catch(AssertionError e) {
                        hasError = true;
                        errorMessage = e.getMessage();
                    }
                    
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
    public void testListSubscriptionsFailure() {
        try {
            PubSubHandle testHandle = new PubSubHandle(new TestPubSubSocketFailure());
            CountDownLatch signal = new CountDownLatch(1);

            testHandle.listSubscriptions()
                .thenAcceptAsync((subscriptions) -> {
                    hasError = true;
                    errorMessage = "There should have not been any subscriptions made: " + subscriptions.toString(); 
                    signal.countDown();
                })
                .exceptionally((error) -> {
                    try {
                        assertEquals(
                            "The message in the cause of the completion exception should be about the session.",
                            TestPubSubSocketFailure.ExceptionType.LIST_SUBSCRIPTIONS.toString(),
                            error.getCause().getMessage()
                        );
                    }
                    catch(AssertionError e) {
                        hasError = true;
                        errorMessage = e.getMessage();
                    }
                    
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
    public void testPublishFailure() {
        try {
            PubSubHandle testHandle = new PubSubHandle(new TestPubSubSocketFailure());
            CountDownLatch signal = new CountDownLatch(1);

            String channel = "Health & Cooking";
            String message = "\"What, do those go together?\" said the chef.";

            testHandle.publish(channel, message)
                .thenAcceptAsync((subscriptions) -> {
                    hasError = true;
                    errorMessage = "There should have not been any subscriptions made: " + subscriptions.toString(); 
                    signal.countDown();
                })
                .exceptionally((error) -> {
                    try {
                        assertEquals(
                            "The message in the cause of the completion exception should be about the session.",
                            TestPubSubSocketFailure.ExceptionType.SEND.toString(),
                            error.getCause().getMessage()
                        );
                    }
                    catch(AssertionError e) {
                        hasError = true;
                        errorMessage = e.getMessage();
                    }
                    
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
    public void testUnsubscribeAllFailure() {
        try {
            PubSubHandle testHandle = new PubSubHandle(new TestPubSubSocketFailure());
            CountDownLatch signal = new CountDownLatch(1);

            String channel = "FITNESS";

            testHandle.unsubscribeAll()
                .thenAcceptAsync((subscriptions) -> {
                    hasError = true;
                    errorMessage = "There should have not been any subscriptions made: " + subscriptions.toString(); 
                    signal.countDown();
                })
                .exceptionally((error) -> {
                    try {
                        assertEquals(
                            "The message in the cause of the completion exception should be about the session.",
                            TestPubSubSocketFailure.ExceptionType.UNSUBSCRIBE_ALL.toString(),
                            error.getCause().getMessage()
                        );
                    } catch(AssertionError e) {
                        hasError = true;
                        errorMessage = e.getMessage();
                    }
                    
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
}

class TestPubSubSocketFailure extends PubSubSocket
{
    public static List<String> subscriptions = Collections.synchronizedList(new LinkedList<>());

    public static enum ExceptionType {
        UNKNOWN("Something Unknown Happened"),
        SEND("Could not succesfully send anything"),
        SESSION("Could not properly retrieve session"), 
        SUBSCRIBE("Could not properly subscribe to a channel"), 
        UNSUBSCRIBE("Could not properly unsubscribe from a channel"), 
        UNSUBSCRIBE_ALL("Could not properly unsubscribe from all channels"),
        LIST_SUBSCRIPTIONS("Could not properly list all subscriptions");

        private final String description;

        ExceptionType(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return this.description;
        }
    }

    public TestPubSubSocketFailure() {
        subscriptions.add("PARTY PLANNING");
        subscriptions.add("DIY CONSTRUCTION");
    }

    protected CompletableFuture<JSONObject> sendRequest(long sequence, JSONObject json) {
        CompletableFuture<JSONObject> outcome = new CompletableFuture<>();
        String action = json.getString("action");

        Throwable ex = null;

        switch(action) {
            case "session-uuid": {
                ex = new Exception(ExceptionType.SESSION.toString());
            }
            break;

            case "subscribe": {
                ex = new Exception(ExceptionType.SUBSCRIBE.toString());
            }
            break;

            case "unsubscribe": {
                ex = new Exception(ExceptionType.UNSUBSCRIBE.toString());
            }
            break;

            case "unsubscribe-all": {
                ex = new Exception(ExceptionType.UNSUBSCRIBE_ALL.toString());
            }
            break;

            case "subscriptions": {
                ex = new Exception(ExceptionType.LIST_SUBSCRIPTIONS.toString());
            }
            break;

            default: {
                ex = new Exception(ExceptionType.UNKNOWN.toString());
            }
            break;
        }

        outcome.completeExceptionally(ex);
        return outcome;
    }

    protected CompletableFuture<JSONObject> sendPublish(long sequence, JSONObject json, SendHandler handler) {
        CompletableFuture<JSONObject> outcome = new CompletableFuture<>();
        Exception sendException = new Exception(ExceptionType.SEND.toString());

        outcome.completeExceptionally(sendException);
        handler.onResult(new SendResult(sendException));

        return outcome;
    }
}