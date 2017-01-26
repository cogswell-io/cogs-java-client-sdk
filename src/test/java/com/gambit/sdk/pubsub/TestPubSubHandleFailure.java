package com.gambit.sdk.pubsub;

import javax.websocket.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.UUID;

import org.json.JSONObject;

import static org.junit.Assert.*;
import org.junit.Test;

public class TestPubSubHandleFailure {

    private static boolean hasError = false;
    private static String errorMessage = "";

    @Test
    public void testGetSessionFailure() {
        PubSubHandle testHandle = new PubSubHandle(new TestPubSubSocketFailure());
        CountDownLatch signal = new CountDownLatch(1);

        testHandle.getSessionUuid()
            .thenAcceptAsync((uuid) -> {
                hasError = true;
                errorMessage = "There should not have been a UUID at all: " + uuid.toString(); 
                signal.countDown();
            })
            .exceptionally((error) -> {
                assertEquals(
                    "The message in the cause of the completion exception should be about the session.",
                    TestPubSubSocketFailure.ExceptionType.SESSION.toString(),
                    error.getCause().getMessage()
                );

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
    public void testSubscribeFailure() {
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
            assertEquals(
                "The message in the cause of the completion exception should be about the session.",
                TestPubSubSocketFailure.ExceptionType.SUBSCRIBE.toString(),
                error.getCause().getMessage()
            );
            
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

class TestPubSubSocketFailure extends PubSubSocket
{
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
        outcome.completeExceptionally(new Exception(ExceptionType.SEND.toString()));
        return outcome;
    }
}