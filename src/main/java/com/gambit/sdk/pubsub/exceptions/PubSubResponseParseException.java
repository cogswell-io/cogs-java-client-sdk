package com.gambit.sdk.pubsub.exceptions;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Thrown when a response from the Cogswell Pub/Sub server could not be parsed.
 * The response that generated the exception is stored with the exception.
 */
public class PubSubResponseParseException extends PubSubException
{
    /**
     * Pub/Sub Server response that generated this exception
     */
    private JSONObject response;

    /**
     * Creates this PubSubResponseParseException with the given message and response.
     *
     * @param message Message to associate with this PubSubResponseParseException.
     * @param response JSONObject that generated this PubSubResponseParseException.
     */
    public PubSubResponseParseException(String message, JSONObject response) {
        super(message);
        this.response = response;
    }

    /**
     * Returns the complete message information (with response) about the exception.
     * The format is the message followed by a colon and space followed by the response as a string.
     *
     * @return String Message with info about the exception in format "message: response as string". 
     */
    @Override
    public String getMessage() {
        return super.getMessage() + ": " + response.toString();
    }
}