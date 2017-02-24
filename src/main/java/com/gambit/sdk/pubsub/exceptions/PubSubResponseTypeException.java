package com.gambit.sdk.pubsub.exceptions;

import com.gambit.sdk.pubsub.responses.successes.PubSubResponse;

public class PubSubResponseTypeException extends PubSubException
{
    /**
     * The response that generated this PubSubResponseTypeException
     */
    private PubSubResponse response;

    /**
     * Creates this PubSubResponseTypeException with the given message and response.
     *
     * @param message Message to associate with this PubSubResponseTypeException.
     * @param response JSONObject that generated this PubSubResponseTypeException.
     */
    public PubSubResponseTypeException(String message, PubSubResponse response) {
        super(message);
        this.response = response;
    }

    /**
     * Returns the complete message information (with response) about the exception.
     * The format is the message followed by a colon and space followed by the name
     * of unexpected class received.
     *
     * @return String Message with info about the exception in format "message: name of invalid class". 
     */
    @Override
    public String getMessage() {
        return super.getMessage() + ": " + response.getClass().getName();
    }
}