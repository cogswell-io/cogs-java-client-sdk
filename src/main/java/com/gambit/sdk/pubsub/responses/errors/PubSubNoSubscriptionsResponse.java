package com.gambit.sdk.pubsub.responses.errors;

import com.gambit.sdk.pubsub.exceptions.PubSubException;

import org.json.JSONException;
import org.json.JSONObject;

public class PubSubNoSubscriptionsResponse extends PubSubErrorResponse {

    public PubSubNoSubscriptionsResponse(JSONObject response) throws JSONException, PubSubException {
        super(response);
    }
}