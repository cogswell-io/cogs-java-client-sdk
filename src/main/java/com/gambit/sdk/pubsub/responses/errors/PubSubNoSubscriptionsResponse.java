package com.gambit.sdk.pubsub.responses.errors;

public class PubSubNoSubscriptionsResponse extends PubSubErrorResponse {

    public PubSubNoSubscriptionsResponse(Long seq) {
        super(404, seq, "pub", "Not Found", "There are no subscribers to the specified channel, so the message could not be delivered");
    }
}