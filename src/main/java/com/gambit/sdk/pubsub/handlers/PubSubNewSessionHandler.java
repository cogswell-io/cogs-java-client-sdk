package com.gambit.sdk.pubsub.handlers;

import java.util.UUID;

/**
 * Represents a handler function for when new Cogswell Pub/Sub sessions are generated. 
 */
 @FunctionalInterface
public interface PubSubNewSessionHandler {
    /**
     * Invoked when a new session is generated after a connection or reconnection to Cogswell Pub/Sub.
     * 
     * @param uuid Uuid of the new session that invoked the handler.
     */
    void onNewSession(UUID uuid);
}