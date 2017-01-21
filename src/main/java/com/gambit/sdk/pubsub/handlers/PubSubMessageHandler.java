package com.gambit.sdk.pubsub.handlers;

import com.gambit.sdk.pubsub.PubSubMessageRecord;

@FunctionalInterface
public interface PubSubMessageHandler {
    void onMessage(PubSubMessageRecord record);
}