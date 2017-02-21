package com.gambit.sdk.pubsub.responses.errors;

public class PubSubInvalidRequestResponse extends PubSubErrorResponse {
    
    private String badRequest;

    public PubSubInvalidRequestResponse(String details, String badRequest) {
        super(400, null, "invalid-request", "Invalid Request", details);
        this.badRequest = badRequest;
    }

    public String getBadRequest() {
        return badRequest;
    }
}