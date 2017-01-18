package com.gabmit.sdk.pubsub.handlers;

public interface MessageHandler {
    void onMessage(String channel, String message);
}