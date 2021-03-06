package com.gambit.sdk.pubsub.handlers;

import com.gambit.sdk.pubsub.responses.errors.PubSubErrorResponse;

/**
 * Represents a handler function for Cogswell Pub/Sub close events.
 */
@FunctionalInterface
public interface PubSubErrorResponseHandler {
    /**
     * Invoked as the initial call when shutting down an instance of {@link com.gambit.sdk.pubsub.PubSubSocket}.
     *
     * @param errorResponse Error response that caused the invocation of the handler.
     */
    void onErrorResponse(PubSubErrorResponse errorResponse);
}