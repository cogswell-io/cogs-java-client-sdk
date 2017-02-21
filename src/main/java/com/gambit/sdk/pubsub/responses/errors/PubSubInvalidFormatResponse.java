package com.gambit.sdk.pubsub.responses.errors;

public class PubSubInvalidFormatResponse extends PubSubErrorResponse {
    
    public PubSubInvalidFormatResponse(Long seq, String action, String details) {
        super(400, seq, action, "Invalid Format", details);
    }
}