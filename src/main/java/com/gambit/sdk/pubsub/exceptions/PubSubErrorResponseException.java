package com.gambit.sdk.pubsub.exceptions;

import com.gambit.sdk.pubsub.responses.errors.PubSubErrorResponse;

public class PubSubErrorResponseException extends Throwable {

    private final PubSubErrorResponse errorResponse;

    public PubSubErrorResponseException(PubSubErrorResponse errorResponse) {
        super(errorResponse.getRawJson());
        this.errorResponse = errorResponse;
    }
}