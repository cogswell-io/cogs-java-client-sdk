package com.gambit.sdk.pubsub.responses.errors;

import com.gambit.sdk.pubsub.exceptions.PubSubException;

import org.json.JSONException;
import org.json.JSONObject;

public class PubSubIncorrectPermissionsResponse extends PubSubErrorResponse {

    public PubSubIncorrectPermissionsResponse(JSONObject response) throws JSONException, PubSubException {
        super(response);
    }
}