package com.gambit.sdk.pubsub;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import java.util.List;
import java.util.UUID;

import javax.websocket.*;

import org.json.JSONObject;

import com.gabmit.sdk.pubsub.handlers.*;

public class PubSubHandle {
    private AtomicInteger sequence;
    private PubSubSocket socket;

    public PubSubHandle(PubSubSocket socket) {
        this.sequence = new AtomicInteger(0);
        this.socket = socket;
    }

    public CompletableFuture<UUID> getSessionUuid() {
        CompletableFuture promise = CompletableFuture.supplyAsync(() -> {
            int seq = sequence.getAndIncrement();

            JSONObject request = new JSONObject()
                .put("seq", seq)
                .put("action", "session-uuid");

            CompletableFuture<JSONObject> blah = socket.sendMessage(seq, request);

            CompletableFuture<UUID> done = blah.thenCompose((inside) -> {
                //System.out.println("THE \"RESULT\": " + inside.getString("uuid"));
                UUID uuid = UUID.fromString(inside.getString("uuid"));
                CompletableFuture<UUID> f = new CompletableFuture<>();
                f.complete(uuid);
                return f;
            });

            try {
                return done.get();
            }
            catch(Exception e) {
                throw new CompletionException(e);
            }
        });

        return promise;
    }

    public CompletableFuture<List<String>> subscribe(String channel, PubSubMessageHandler messageHandler) {
        return null;
    }

    public CompletableFuture<List<String>> unsubscribe(String channel) {
        return null;
    }

    public CompletableFuture<List<String>> listSubscriptions() {
        return null;
    }

    public long publish(String channel, String message) {
        return 0;
    }

    public CompletableFuture<List<String>> close() {
        return null;
    }

    public void onMessage(PubSubMessageHandler messageHandler) {

    }

    public void onReconnect(PubSubReconnectHandler reconnectHandler) {

    }

    public void onClose(PubSubCloseHandler closeHandler) {

    }

    public void onError(PubSubErrorHandler errorHandler) {

    }
}