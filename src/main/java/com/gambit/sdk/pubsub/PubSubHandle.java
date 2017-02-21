package com.gambit.sdk.pubsub;

import java.util.concurrent.atomic.AtomicLong;

import java.util.concurrent.CompletableFuture;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import java.io.IOException;

import org.json.JSONObject;
import org.json.JSONArray;

import com.gambit.sdk.pubsub.responses.successes.*;
import com.gambit.sdk.pubsub.exceptions.*;
import com.gambit.sdk.pubsub.handlers.*;

/**
 * Represents user endpoint to Cogswell Pub/Sub and provides methods to perform available Pub/Sub operations.
 */
public class PubSubHandle {
    private AtomicLong sequence;
    private PubSubSocket socket;

    /**
     * Creates an endpoint to Cogswell Pub/Sub using the given {@link PubSubSocket} as the underlying connection.
     *
     * @param socket {@link PubSubSocket} which contains the underlying connection to Cogswell Pub/Sub 
     */
    protected PubSubHandle(PubSubSocket socket) {
        this(socket, 0L);
    }

    /**
     * Creates an endpoint to Cogswell Pub/Sub using the given {@link PubSubSocket} as the underlying connection.
     *
     * @param socket {@link PubSubSocket} which contains the underlying connection to Cogswell Pub/Sub
     * @param firstSequenceNumber Provides the initial sequence number for stating to count calls (defaults to 0L)
     */
    protected PubSubHandle(PubSubSocket socket, long firstSequenceNumber) {
        this.sequence = new AtomicLong(firstSequenceNumber);
        this.socket = socket;
    }

    /**
     * This method (used for test purposes only) allows the handle to drop a socket connection without cleanly closing it.
     * @param dropOptions Options used to modify or fine-tune the results of dropping the underlying connection
     */
    protected void dropConnection(PubSubDropConnectionOptions dropOptions) {
        socket.dropConnection(dropOptions);
    }

    /**
     * Fetches UUID of current session, which enables caching if caching is enabled on the project. 
     * @return {@code CompletableFuture<UUID>} Completes with UUID of current session on success.   
     */
    public CompletableFuture<UUID> getSessionUuid() {
        CompletableFuture<UUID> outcome = new CompletableFuture<>();
        long seq = sequence.getAndIncrement();

        JSONObject request = new JSONObject()
            .put("seq", seq)
            .put("action", "session-uuid");

        socket.sendRequest(seq, request)
            .thenAcceptAsync((response) -> {
                if (response instanceof PubSubSessionUuidResponse) {
                    PubSubSessionUuidResponse uuidResponse = (PubSubSessionUuidResponse)(response);
                    outcome.complete(uuidResponse.getSessionUuid());
                }
                else {
                    outcome.completeExceptionally(new PubSubException("Invalid Response to Session UUID"));
                }
            })
            .exceptionally((error) -> {
                outcome.completeExceptionally(error);
                return null;
            });

        return outcome;
    }

    /**
     * Subscribes to {@code channel}, processing messages from {@code channel} using provided {@link PubSubMessageHandler}
     *
     * @param channel        Name of the channel to which to subscribe.
     * @param messageHandler Handler that receives message from {@code channel}. May NOT be null.
     * @return {@code CompletableFuture<List<String>>} Completes with list of all current subscriptions on success. 
     */
    public CompletableFuture<List<String>> subscribe(String channel, PubSubMessageHandler messageHandler) {
        CompletableFuture<List<String>> outcome = new CompletableFuture<>();
        long seq = sequence.getAndIncrement();

        JSONObject request = new JSONObject()
            .put("seq", seq)
            .put("action", "subscribe")
            .put("channel", channel);

        socket.addMessageHandler(channel, messageHandler);

        socket.sendRequest(seq, request)
            .thenAcceptAsync((response) -> {
                if (response instanceof PubSubSubscribeResponse) {
                    PubSubSubscribeResponse subResponse = (PubSubSubscribeResponse)(response);
                    outcome.complete(subResponse.getChannels());
                }
                else {
                    outcome.completeExceptionally(new PubSubException("Invalid Response to Subscribing"));
                }
            })
            .exceptionally((error) -> {
                socket.removeMessageHandler(channel);
                outcome.completeExceptionally(error);
                return null;
            });

        return outcome;
    }

    /**
     * Unsubscribes from {@code channel} which stops receipt and handling of messages for {@code channel}.
     *
     * @param channel Name of the channel from which to unsubscribe.
     * @return {@code CompletableFuture<List<String>>} Completes with list of all remaining subscriptions on success.
     */
    public CompletableFuture<List<String>> unsubscribe(String channel) {
        CompletableFuture<List<String>> outcome = new CompletableFuture<>();
        long seq = sequence.getAndIncrement();

        JSONObject request = new JSONObject()
            .put("seq", seq)
            .put("action", "unsubscribe")
            .put("channel", channel);

        socket.sendRequest(seq, request)
            .thenAcceptAsync((response) -> {
                if(response instanceof PubSubUnsubscribeResponse) {
                    PubSubUnsubscribeResponse unsubResponse = (PubSubUnsubscribeResponse)(response);
                    outcome.complete(unsubResponse.getChannels());
                }
                else {
                    outcome.completeExceptionally(new PubSubException("Invalid Response to Unsubscribing"));
                }
            })
            .exceptionally((error) -> {
                outcome.completeExceptionally(error);
                return null;
            });

        return outcome;
    }
    
    /**
     * Unsubscribes from all channels. This stops receipt and handling of message from all channels.
     *
     * @return {@code CompletableFuture<List<String>>} Completes with list of channels that have been unsubscribed on success.
     */
    public CompletableFuture<List<String>> unsubscribeAll() {
        CompletableFuture<List<String>> outcome = new CompletableFuture<>();
        long seq = sequence.getAndIncrement();

        JSONObject request = new JSONObject()
            .put("seq", seq)
            .put("action", "unsubscribe-all");

        socket.sendRequest(seq, request)
            .thenAcceptAsync((response) -> {
                if (response instanceof PubSubUnsubscribeAllResponse) {
                    PubSubUnsubscribeAllResponse unsubAllResponse = (PubSubUnsubscribeAllResponse)(response);
                    outcome.complete(unsubAllResponse.getChannels());
                }
                else {
                    outcome.completeExceptionally(new PubSubException("Invalid Response to Unsubscribing from All Channels"));
                }
            })
            .exceptionally((error) -> {
                outcome.completeExceptionally(error);
                return null;
            });

        return outcome;
    }

    /**
     * Fetches list of all current subscriptions.
     *
     * @return {@code CompletableFuture<List<String>>} Completes with list of all current subscriptions on success.
     */
    public CompletableFuture<List<String>> listSubscriptions() {
        CompletableFuture<List<String>> outcome = new CompletableFuture<>();

        long seq = sequence.getAndIncrement();
        JSONObject request = new JSONObject()
            .put("seq", seq)
            .put("action", "subscriptions");

        socket.sendRequest(seq, request)
            .thenAcceptAsync((response) -> {
                if (response instanceof PubSubListSubscriptionsResponse) {
                    PubSubListSubscriptionsResponse listResponse = (PubSubListSubscriptionsResponse)(response);
                    outcome.complete(listResponse.getChannels());
                }
                else {
                    outcome.completeExceptionally(new PubSubException("Invalid Response to Listing Subscriptions"));
                }
            })
            .exceptionally((error) -> {
                outcome.completeExceptionally(error);
                return null;
            });

        return outcome;
    }

    /**
     * Publishes {@code message} to {@code channel} without acknowledgement that the message was actually published.
     * Note: Completion of the returned CompletableFuture indicates success only in sending the message. 
     *       This method gives no information and no guarantees that the message was actually published.
     *
     * @param channel Name of the channel on which to publish the message.
     * @param message Content of the message to be publish on the given channel.
     * @param handler Error handler called if <em>sending</em> fails.
     * @return {@code CompletableFuture<Long>} Completes with sequence number of record sent on a successful send. 
     */
    public CompletableFuture<Long> publish(String channel, String message, PubSubErrorResponseHandler handler) {
        CompletableFuture<Long> outcome = new CompletableFuture<>();
        long seq = sequence.getAndIncrement();

        JSONObject publish = new JSONObject()
            .put("seq", seq)
            .put("action", "pub")
            .put("chan", channel)
            .put("msg", message)
            .put("ack", false);

        socket.sendPublish(seq, publish, handler, (result) -> {
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
     * Publishes {@code message} to {@code channel} without acknowledgement that the message was actually published.
     * Note: Completion of the returned CompletableFuture indicates success only in sending the message. 
     *       This method gives no information and no guarantees that the message was actually published.
     *
     * @param channel Name of the channel on which to publish the message.
     * @param message Content of the message to be publish on the given channel.
     * @param handler Error handler called if <em>sending</em> fails.
     * @return {@code CompletableFuture<Long>} Completes with sequence number of record sent on a successful send. 
     */
    public CompletableFuture<Long> publish(String channel, String message) {
        CompletableFuture<Long> outcome = new CompletableFuture<>();
        long seq = sequence.getAndIncrement();

        JSONObject publish = new JSONObject()
            .put("seq", seq)
            .put("action", "pub")
            .put("chan", channel)
            .put("msg", message)
            .put("ack", false);

        socket.sendPublish(seq, publish, null, (result) -> {
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
     * Publishes {@code message} to {@code channel} with acknowledgement that the message was actually published.
     *
     * @param channel Name of the channel on which to publish the message.
     * @param message Content of the message to be publish on the given channel.
     * @return {@code CompletableFuture<UUID>} Completes with UUID of published message on success. 
     */
    public CompletableFuture<UUID> publishWithAck(String channel, String message) {
        CompletableFuture<UUID> outcome = new CompletableFuture<>();
        long seq = sequence.getAndIncrement();

        JSONObject publish = new JSONObject()
            .put("seq", seq)
            .put("action", "pub")
            .put("chan", channel)
            .put("msg", message)
            .put("ack", true);

        socket.sendPublishWithAck(seq, publish, (sendResult) -> {
            if(!sendResult.isOK()) {
                outcome.completeExceptionally(sendResult.getException());
            }
        })
        .thenAcceptAsync((response) -> {
            if (response instanceof PubSubPublishAckResponse) {
                PubSubPublishAckResponse pubResponse = (PubSubPublishAckResponse)(response);
                outcome.complete(pubResponse.getMessageId());
            }
            else {
                outcome.completeExceptionally(new PubSubException("Invalid Response to Publishing"));
            }
        })
        .exceptionally((error) -> {
            outcome.completeExceptionally(error);
            return null;
        });

        return outcome;
    }

    /**
     * Closes the connection with Cogswell Pub/Sub and unsubscribes from all channels.
     *
     * @return {@code CompletableFuture<List<String>>} Completes with list of channels unsubscribed on success.
     */
    public CompletableFuture<Void> close() {
        return CompletableFuture.runAsync(socket::close);
    }

    /**
     * Registers a handler to process any published messages received from Cogswell Pub/Sub on any subscribed channels.
     *
     * @param messageHandler The {@link PubSubMessageHandler} that should be registered.
     */
    public void onMessage(PubSubMessageHandler messageHandler) {
        socket.setMessageHandler(messageHandler);
    }

    /**
     * Registers a handler that is called whenever the underlying connection is re-established.
     *
     * @param reconnectHandler The {@link PubSubReconnectHandler} that should be registered.
     */
    public void onReconnect(PubSubReconnectHandler reconnectHandler) {
        socket.setReconnectHandler(reconnectHandler);
    }

    /**
     * Registers a handler to process every raw record (as a JSON-formatted String) received from Cogswell Pub/Sub.
     *
     * @param rawRecordHandler The {@link PubSubRawRecordHandler} that should be registered.
     */
    public void onRawRecord(PubSubRawRecordHandler rawRecordHandler) {
        socket.setRawRecordHandler(rawRecordHandler);
    }

    /**
     * Registers a handler that is called immediately before the underlying connection to Cogswell Pub/Sub is closed. 
     *
     * @param closeHandler The {@link PubSubCloseHandler} that should be registered.
     */
    public void onClose(PubSubCloseHandler closeHandler) {
        socket.setCloseHandler(closeHandler);
    }

    /**
     * Registers a handler for whenever a client-side exception is thrown.
     *
     * @param errorHandler The {@link PubSubErrorHandler} that should be registered
     */
    public void onError(PubSubErrorHandler errorHandler) {
        socket.setErrorHandler(errorHandler);
    }

    /**
     * Registers a handler for whenever an error response is received from ther server.
     *
     * @param errorHandler The {@link PubSubErrorHandler} that should be registered
     */
    public void onErrorResponse(PubSubErrorResponseHandler errorResponseHandler) {
        socket.setErrorResponseHandler(errorResponseHandler);
    }

    /**
     * Registers a handler that is called whenever reconnecting the underlying connection to Cogswell Pub/Sub forces a new session
     *
     * @param newSessionHandler The {@link PubSubNewSessionHandler} that should be registered
     */
    public void onNewSession(PubSubNewSessionHandler newSessionHandler) {
        socket.setNewSessionHandler(newSessionHandler);
    }
}