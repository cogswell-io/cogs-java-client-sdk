package com.gambit.sdk.pubsub.responses.errors;

import com.gambit.sdk.pubsub.exceptions.*;

import java.util.Optional;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents the information contained in an error response received from the Pub/Sub server.
 */
public class PubSubErrorResponse {
    /**
     * Holds the code that was in the error response from the server.
     */
    private int code;

    /**
     * Holds the details that were in the error response from the server.
     */
    private String details;

    /**
     * Holds the action that precipitated the error response from the server.
     */
    private String action;

    /**
     * Holds the error message that was in the error response from the server.
     */
    private String message;

    /**
     * Holds the raw json that was used to fill out this error response
     */
    private String rawJson;

    /**
     * Holds the sequence number that precipitated the error response from the server.
     */
    private Long sequence;

    public static PubSubErrorResponse create(JSONObject response) throws PubSubException {
        try {
            if (!response.has("seq")) {
                return new PubSubInvalidRequestResponse(response);
            }

            switch(response.getInt("code")) {
                case 500:
                    return new PubSubErrorResponse(response);

                case 400:
                    return new PubSubInvalidFormatResponse(response);

                case 401:
                    return new PubSubIncorrectPermissionsResponse(response);

                case 404: {
                    switch(response.getString("action")) {
                        case "unsubscribe":
                            return new PubSubSubscriptionNotFoundResponse(response);

                        case "pub":
                            return new PubSubNoSubscriptionsResponse(response);

                        default:
                            throw new PubSubResponseParseException("Unknown Error Response from Server", response);
                    }
                }
                
                default:
                    throw new PubSubResponseParseException("Unknown Error Response from Server", response);
            }
        }
        catch(JSONException e) {
            throw new PubSubResponseParseException("Could Not Parse Error Response from Server", response);
        }
    }

    /**
     * Fills an error response with information about the error.
     *
     * @param response JSON object representing the error returned from the server
     */
    public PubSubErrorResponse(JSONObject response) throws JSONException {
        if(response.has("seq")) {
            sequence = response.getLong("seq");
        }

        if(response.has("details")) {
            details = response.getString("details");
        }

        if(response.has("action")) {
            action = response.getString("action");
        }

        if(response.has("message")) {
            message = response.getString("message");
        }

        code = response.getInt("code");
        rawJson = response.toString();
    }

    /**
     * Provides the code for the error response received from the server.
     *
     * @return int Numeric code of the error response
     */
    public int getCode() {
        return code;
    }

    /**
     * Provides a raw JSON string representing the error response
     *
     * @return String Raw JSON string representing the response
     */
    public String getRawJson() {
        return rawJson;
    }

    /**
     * Provides the sequence number that precipitated the error response from the server.
     *
     * @return {@code Optional<String>} Filled with the sequence number, if it exists
     */
    public Optional<Long> getSequence() {
        return Optional.ofNullable(sequence);
    }

    /**
     * Provides the details contained in the error response from the server.
     *
     * @return {@code Optional<String>} Filled with the details message, if it exists
     */
    public Optional<String> getDetails() {
        return Optional.ofNullable(details);
    }

    /**
     * Provides the action that precipitated the error response from the server.
     *
     * @return {@code Optional<String>} Filled with the action from the error message, if it exists
     */
    public Optional<String> getAction() {
        return Optional.ofNullable(action);
    }

    /**
     * Provides the message contained in the error response from the server.
     *
     * @return {@code Optional<String>} Filled with the message of the error, if it exists
     */
    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }
}