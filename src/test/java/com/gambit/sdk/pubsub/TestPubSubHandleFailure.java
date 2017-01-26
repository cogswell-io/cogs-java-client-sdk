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

    //@Test
    public void testGetSessionFailure() {
        PubSubHandle testHandle = new PubSubHandle(new TestPubSubSocketFailure());
        CountDownLatch signal = new CountDownLatch(1);

        testHandle.getSessionUuid()
            .thenAcceptAsync((uuid) -> {
                hasError = true;
                errorMessage = "Should not have \"received\" any sort of UUID: " + uuid.toString();
                signal.countDown();
            })
            .exceptionally((error) -> {
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
    public TestPubSubSocketFailure() {
    }

    protected CompletableFuture<JSONObject> sendRequest(long sequence, JSONObject json) {
        CompletableFuture<JSONObject> outcome = new CompletableFuture<>();
        outcome.completeExceptionally(new Exception(""));
        return outcome;
    }

    protected CompletableFuture<JSONObject> sendPublish(long sequence, JSONObject json, SendHandler handler) {
        CompletableFuture<JSONObject> outcome = new CompletableFuture<>();
        outcome.completeExceptionally(new Exception(""));
        return outcome;
    }
}