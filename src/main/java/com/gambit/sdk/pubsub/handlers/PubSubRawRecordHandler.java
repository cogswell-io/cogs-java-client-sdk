package com.gambit.sdk.pubsub.handlers;

/**
 * Represents a handler for when raw JSON-formatted records are received from Cogswell Pub/Sub.
 */
@FunctionalInterface
public interface PubSubRawRecordHandler {
    /**
     * Invoked when raw JSON-formatted record is received from Cogswell Pub/Sub.
     *
     * @param rawRecord Raw JSON-formatted record from Cogswell Pub/Sub.
     */
    void onRawRecord(String rawRecord);
}