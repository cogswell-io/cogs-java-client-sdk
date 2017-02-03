package com.gambit.sdk.pubsub.handlers;

/**
 * Represents a handler function for whenever {@link com.gambit.sdk.pubsub.PubSubSocket} re-established underlying connection. 
 */
 @FunctionalInterface
public interface PubSubReconnectHandler {
    /**
     * Invoked when the underlying socket of an instance of {@link com.gambit.sdk.pubsub.PubSubSocket} is re-established. 
     */
    void onReconnect();
}