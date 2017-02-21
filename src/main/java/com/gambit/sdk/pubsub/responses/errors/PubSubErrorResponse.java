package com.gambit.sdk.pubsub.responses.errors;

import java.util.Optional;

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
     * Holds the sequence number that precipitated the error response from the server.
     */
    private Long sequence;

    /**
     * Fills an error response with information about the error.
     *
     * @param seq Sequence number that precipitated the error, if any
     * @param code Code representing the error that occurred, if any
     * @param action Action that precipitated the error, if any
     * @param message Description of the error, if any
     * @param details Details about the error, if any
     */
    public PubSubErrorResponse(int code, Long seq, String action, String message, String details) {
        this.sequence = seq;
        this.code = code;
        this.action = action;
        this.details = details;
        this.message = message;
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
     * Provides the sequence number that precipitated the error response from the server.
     *
     * @return Optional<Long> Filled with the sequence number, if it exists
     */
    public Optional<Long> getSequence() {
        return Optional.ofNullable(sequence);
    }

    /**
     * Provides the details contained in the error response from the server.
     *
     * @return Optional<string> Filled with the details message, if it exists
     */
    public Optional<String> getDetails() {
        return Optional.ofNullable(details);
    }

    /**
     * Provides the action that precipitated the error response from the server.
     *
     * @return Optional<String> Filled with the action from the error message, if it exists
     */
    public Optional<String> getAction() {
        return Optional.ofNullable(action);
    }

    /**
     * Provides the message contained in the error response from the server.
     *
     * @return Optional<String> Filled with the message of the error, if it exists
     */
    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }
}