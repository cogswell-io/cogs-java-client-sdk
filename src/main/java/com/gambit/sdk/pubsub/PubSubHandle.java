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

/**
 * Users of the SDK receive an instance of this class when connecting to Pub/Sub.
 * All Pub/Sub operations available to users of the SDK are made through an instance of this class.
 */
public class PubSubHandle {
    private AtomicLong sequence;
    private PubSubSocket socket;

    /**
     * Construct a handle that uses the given {@link PubSubSocket} to connect to the Pub/Sub system
     */
    public PubSubHandle(PubSubSocket socket) {
        this.sequence = new AtomicLong(0);
        this.socket = socket;
    }

    /**
     * Request the UUID of the current session/connection with the Pub/Sub system.
     * @return CompletableFuture<UUID> Future that completes with Session UUID on success, and with error otherwise   
     */
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

    /**
     * Subscribe to the given channel, and process messages from channel with the given {@link PubSubMessageHandler}.
     * It is possible to send in null for the PubSubMessageHandler, which means the connection will subscribe to the
     * given channel, but any messages received from that channel are handled by the generic onRawRecord handler.
     * @param channel The name of the channel to which to subscribe
     * @param messageHandler The handler which will handle Pub/Sub messages received on the given channel
     * @return CompletableFuture<List<String>> Future completing with list of subscriptions on success, error otherwise 
     */
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

    /**
     * Unsubscribe to the given channel, thus stop receiving and handling messages from that channel as well.
     * @param channel THe name of the channel from which to unsubscribe
     * @return CompletableFuture<List<String>> Future completing with list of subscriptions on success, error otherwise
     */
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

    /**
     * Request a list of current subscriptions for the connection
     * @return CompletableFuture<List<String>> Future completing with list of subscriptions on success, error otherwise
     */
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

    /**
     * Publish the given message to the given channel. Note that the CompletableFuture returned by this method
     * indicates success in actually sending the message, but provides no information about whether the message
     * was received. This is unlike the other methods, which return futures with the results from the server.
     * @param channel The name of the channel on which to publish the given message.
     * @param message The actual content of the message to publish on the given channel.
     * @return CompletableFuture<Long> Future completed with sequence of message on successful send, error otherwise 
     */
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

    /**
     * Close the connection for good.
     * @return CompletableFuture<List<String>> Future completing with list of subscriptions on success, error otherwise
     */
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

    /**
     * Unsubscribe from all current channel subscriptions
     * @return CompletableFuture<List<String>> Future completing with list of all unsubscribed channels, error otherwise
     */
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

    /**
     * Register a handler for messages from any channel
     * @param messageHandler The {@link PubSubMessageHandler} that should be registered
     */
    public void onMessage(PubSubMessageHandler messageHandler) {
    }

    /**
     * Register a handler for reconnect events.
     * @param reconnectHandler The {@link PubSubReconnectHandler} that should be registered
     */
    public void onReconnect(PubSubReconnectHandler reconnectHandler) {
    }

    /**
     * Register a handler for close events
     * @param closeHandler The {@link PubSubCloseHandler} that should be registered
     */
    public void onClose(PubSubCloseHandler closeHandler) {
    }

    /**
     * Register a handler for errors
     * @param errorHandler The {@link PubSubErrorHandler} that should be registered
     */
    public void onError(PubSubErrorHandler errorHandler) {
    }
}