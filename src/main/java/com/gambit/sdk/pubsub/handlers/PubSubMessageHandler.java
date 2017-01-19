package com.gabmit.sdk.pubsub.handlers;

public interface PubSubMessageHandler {
    void onMessage(String channel, String message);
}