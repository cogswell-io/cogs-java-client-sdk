package com.gambit.sdk.pubsub.exceptions;

/**
 * Exception thrown when connection attempts fail.
 */
public class PubSubSocketConnectionException extends PubSubException {

    /**
     * Creates this PubSubSocketConnectionException with the given message.
     *
     * @param message Message to associate with this PubSubSocketConnectionException.
     */
    public PubSubSocketConnectionException(String message) {
        super(message);
    }
}