package com.gambit.sdk.pubsub.responses.errors;

public class PubSubIncorrectPermissionsResponse extends PubSubErrorResponse {

    public PubSubIncorrectPermissionsResponse(Long seq, String action, String details) {
        super(401, seq, action, "Not Authorized", details);
    }
}