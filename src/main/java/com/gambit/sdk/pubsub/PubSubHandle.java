package com.gambit.sdk.pubsub;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;

import java.util.Collections;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.websocket.*;

import org.json.JSONObject;
import org.json.JSONArray;

import com.gambit.sdk.pubsub.handlers.*;

public class PubSubHandle {

    private AtomicLong sequence;
    private PubSubSocket socket;

    public PubSubHandle(PubSubSocket socket) {
        this.sequence = new AtomicLong(0);
        this.socket = socket;
    }

    public CompletableFuture<UUID> getSessionUuid() {
        CompletableFuture<UUID> outcome = new CompletableFuture<>();
        long seq = sequence.getAndIncrement();

        JSONObject request = new JSONObject()
            .put("seq", seq)
            .put("action", "session-uuid");

        socket.sendMessage(seq, request)
            .thenAcceptAsync((json) -> {
                UUID uuid = UUID.fromString(json.getString("uuid"));
                outcome.complete(uuid);
            })
            .exceptionally((error) -> {
                outcome.completeExceptionally(error);
                return null;
            });

        return outcome;
    }

    public CompletableFuture<List<String>> subscribe(String channel, PubSubMessageHandler messageHandler) {
        CompletableFuture<List<String>> outcome = new CompletableFuture<>();

        long seq = sequence.getAndIncrement();

        JSONObject request = new JSONObject()
            .put("seq", seq)
            .put("action", "subscribe")
            .put("channel", channel);

        socket.addMessageHandler(channel, messageHandler);

        socket.sendMessage(seq, request)
            .thenAcceptAsync((json) -> {
                List<String> channels = Collections.synchronizedList(new LinkedList<>());
                JSONArray list = json.getJSONArray("channels");

                for(int i = 0; i < list.length(); ++i) {
                    channels.add(list.getString(i));
                }

                outcome.complete(channels);
            })
            .exceptionally((error) -> {
                //socket.removeMessageHandler(channel, messageHandler);
                outcome.completeExceptionally(error);
                return null;
            });

        return outcome;
    }

    public CompletableFuture<List<String>> unsubscribe(String channel) {
        return null;
    }

    public CompletableFuture<List<String>> listSubscriptions() {
        CompletableFuture promise = CompletableFuture.supplyAsync(() -> {
            long seq = sequence.getAndIncrement();
            JSONObject request = new JSONObject()
                .put("seq", seq)
                .put("action", "subscriptions");

            CompletableFuture<List<String>> result = socket.sendMessage(seq, request)
                .thenCompose((json) -> {
                    CompletableFuture<List<String>> future = new CompletableFuture<>();
                    List<String> channels = Collections.synchronizedList(new LinkedList<>());

                    JSONArray list = json.getJSONArray("channels");
                    for(int i = 0; i < list.length(); ++i) {
                        channels.add(list.getString(i));
                    }

                    future.complete(channels);
                    return future;
                });

            try {
                return result.get();
            }
            catch(Exception e) {
                throw new CompletionException(e);
            }
        });

        return promise;
    }

    public long publish(String channel, String message) {

        System.out.println("TRYING TO PUBLISH: \n\t" + message);
        long seq = sequence.getAndIncrement();

        JSONObject publish = new JSONObject()
            .put("seq", seq)
            .put("action", "pub")
            .put("chan", channel)
            .put("msg", message);

        socket.sendMessage(seq, publish); // returns CompletableFuture<JSONObject>
        return seq;
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