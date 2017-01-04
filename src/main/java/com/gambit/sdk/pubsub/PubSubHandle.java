package com.gambit.sdk.pubsub;

// TODO: Create MessageHandler, ReconnectHandler, CloseHandler, ErrorHandler interfaces

public class PubSubHandle {
    CompletableFuture<UUID> getClientUuid() {

    }

    CompletableFuture<List<String>> subscribe(String channel, MessageHandler messageHandler) {

    }

    CompletableFuture<List<String>> unsubscribe(String channel) {

    }

    CompletableFuture<List<String>> listSubscriptions() {

    }

    long publish(String channel, String message) {

    }

    CompletableFuture<List<String>> close() {

    }

    void onMessage(MessageHandler messageHandler) {

    }

    void onReconnect(ReconnectHandler reconnectHandler) {

    }

    void onClose(CloseHandler closeHandler) {

    }

    void onError(ErrorHandler errorHandler) {

    }
}