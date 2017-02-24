package com.gambit.sdk.pubsub.exceptions;

/**
 * Thrown when a  socket implementation is unusuable. The exception is not
 * differentiated from whether it exists or is unusuable for some other reason.
 */
public class PubSubSocketImplementationException extends PubSubException
{
    /**
     * Creates this PubSubSocketImplementationException with the given message.
     *
     * @param message Message to associate with this PubSubSocketImplementationException.
     */
    public PubSubSocketImplementationException(String message) {
        super(message);
    }
}