package com.gambit.sdk.pubsub.responses.successes;

import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

public class PubSubSessionUuidResponse extends PubSubResponse {
    private UUID sessionUuid;

    public PubSubSessionUuidResponse(JSONObject response) throws JSONException {
        super(response);

        sessionUuid = UUID.fromString(response.getString("uuid"));
    }

    public UUID getSessionUuid() {
        return sessionUuid;
    }
}