package com.gambit.sdk.pubsub.handlers;

public interface PubSubRawRecordHandler {
    void onRawRecord(String rawRecord);
}