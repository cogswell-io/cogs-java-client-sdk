package com.gambit.sdk.pubsub.responses.errors;

public class PubSubSubscriptionNotFoundResponse extends PubSubErrorResponse {

    public PubSubSubscriptionNotFoundResponse(Long seq) {
        super(404, seq, "unsubscribe", "Not Found", "You are not subscribed to the specified channel");
    }
}