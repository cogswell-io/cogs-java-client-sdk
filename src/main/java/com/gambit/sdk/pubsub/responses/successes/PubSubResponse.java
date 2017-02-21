package com.gambit.sdk.pubsub.responses.successes;

import com.gambit.sdk.pubsub.exceptions.PubSubException;

import org.json.JSONException;
import org.json.JSONObject;

public class PubSubResponse {

    private Long seq;
    private String action;
    private String rawJson;

    private int code = 200;

    public static PubSubResponse create(JSONObject response) throws JSONException, PubSubException {
        switch(response.getString("action")) {
            case "session-uuid": 
                return new PubSubSessionUuidResponse(response);

            case "subscribe":
                return new PubSubSubscribeResponse(response);

            case "unsubscribe":
                return new PubSubUnsubscribeResponse(response);

            case "unsubscribe-all":
                return new PubSubUnsubscribeAllResponse(response);

            case "subscriptions":
                return new PubSubListSubscriptionsResponse(response);

            case "pub":
                return new PubSubPublishAckResponse(response);

            default:
                throw new PubSubException("Unknown/Unhandled Response from Server");
        }
    }

    public PubSubResponse(JSONObject response) throws JSONException {
        this.seq = response.getLong("seq");
        this.action = response.getString("action");
        this.rawJson = response.toString();
    }

    public long getSequence() {
        return seq;
    }

    public String getAction() {
        return action;
    }

    public int getCode() {
        return code;
    }

    public String getRawJson() {
        return rawJson;
    }
}