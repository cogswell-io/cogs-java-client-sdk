package com.gambit.sdk.pubsub.responses.errors;

import com.gambit.sdk.pubsub.exceptions.PubSubException;

import org.json.JSONException;
import org.json.JSONObject;

public class PubSubInvalidFormatResponse extends PubSubErrorResponse {
    
    public PubSubInvalidFormatResponse(JSONObject response) throws JSONException, PubSubException {
        super(response);
    }
}