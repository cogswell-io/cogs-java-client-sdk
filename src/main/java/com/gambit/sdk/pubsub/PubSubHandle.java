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

import java.io.IOException;

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
        CompletableFuture<List<String>> outcome = new CompletableFuture<>();
        long seq = sequence.getAndIncrement();

        JSONObject request = new JSONObject()
            .put("seq", seq)
            .put("action", "unsubscribe")
            .put("channel", channel);

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
                outcome.completeExceptionally(error);
                return null;
            });

        return outcome;
    }

    public CompletableFuture<List<String>> listSubscriptions() {
        CompletableFuture outcome = new CompletableFuture<>();

        long seq = sequence.getAndIncrement();
        JSONObject request = new JSONObject()
            .put("seq", seq)
            .put("action", "subscriptions");

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
                outcome.completeExceptionally(error);
                return null;
            });

        return outcome;
    }

    public CompletableFuture<Long> publish(String channel, String message) {
        CompletableFuture<Long> outcome = new CompletableFuture<>();
        long seq = sequence.getAndIncrement();

        JSONObject publish = new JSONObject()
            .put("seq", seq)
            .put("action", "pub")
            .put("chan", channel)
            .put("msg", message);

        socket.sendMessage(seq, publish, (result) -> {
            if(result.isOK()) {
                outcome.complete(seq);
            }
            else {
                outcome.completeExceptionally(result.getException());
            }
        });

        return outcome;
    }

    public CompletableFuture<List<String>> close() {
        return unsubscribeAll()
            .whenCompleteAsync((res, err) -> {
                try {
                    socket.close();
                }
                catch(IOException e) {
                    System.err.println("Error while closing Websocket: " + e.getMessage());
                }
            });
    }

    public CompletableFuture<List<String>> unsubscribeAll() {
        CompletableFuture<List<String>> outcome = new CompletableFuture<>();
        long seq = sequence.getAndIncrement();

        JSONObject request = new JSONObject()
            .put("seq", seq)
            .put("action", "unsubscribe-all");

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
                outcome.completeExceptionally(error);
                return null;
            });

        return outcome;
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