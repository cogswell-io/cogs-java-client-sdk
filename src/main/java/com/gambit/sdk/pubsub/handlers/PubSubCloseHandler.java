package com.gambit.sdk.pubsub.handlers;

import java.util.Optional;

/**
 * Represents a handler function for Cogswell Pub/Sub close events.
 */
@FunctionalInterface
public interface PubSubCloseHandler {
    /**
     * Invoked as the initial call when shutting down an instance of {@link com.gambit.sdk.pubsub.PubSubSocket}.
     *
     * @param error Error that caused the invocation of the handler.
     */
    void onClose(Throwable error);
}