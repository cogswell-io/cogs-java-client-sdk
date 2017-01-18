package com.gambit.sdk.pubsub;

import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.UUID;

import com.gabmit.sdk.pubsub.handlers.*;

// TODO: Create MessageHandler, ReconnectHandler, CloseHandler, ErrorHandler interfaces

public class PubSubHandle {
    public CompletableFuture<UUID> getSessionUuid() {
        return null;
    }

    public CompletableFuture<List<String>> subscribe(String channel, MessageHandler messageHandler) {
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

    public void onMessage(MessageHandler messageHandler) {

    }

    public void onReconnect(ReconnectHandler reconnectHandler) {

    }

    public void onClose(CloseHandler closeHandler) {

    }

    public void onError(ErrorHandler errorHandler) {

    }
}