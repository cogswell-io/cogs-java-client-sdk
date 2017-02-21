package com.gambit.sdk.pubsub.responses.successes;

import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

public class PubSubPublishAckResponse extends PubSubResponse {
    private UUID messageUuid;

    public PubSubPublishAckResponse(JSONObject response) throws JSONException {
        super(response);

        messageUuid = UUID.fromString(response.getString("id"));
    }

    public UUID getMessageId() {
        return messageUuid;
    }
}