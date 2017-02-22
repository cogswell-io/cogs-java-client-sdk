package com.gambit.sdk.pubsub.responses.errors;

import com.gambit.sdk.pubsub.exceptions.PubSubException;

import org.json.JSONException;
import org.json.JSONObject;

public class PubSubInvalidRequestResponse extends PubSubErrorResponse {
    
    private String badRequest;

    public PubSubInvalidRequestResponse(JSONObject response) throws JSONException, PubSubException {
        super(response);
        this.badRequest = response.getString("bad_request");
    }

    public String getBadRequest() {
        return badRequest;
    }
}