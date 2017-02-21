package com.gambit.sdk.pubsub.responses.successes;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

public class PubSubUnsubscribeResponse extends PubSubResponse {
    private List<String> channels;

    public PubSubUnsubscribeResponse(JSONObject response) throws JSONException {
        super(response);

        JSONArray list = response.getJSONArray("channels");
        channels = Collections.synchronizedList(new LinkedList<>());

        for(int i = 0; i < list.length(); ++i) {
            channels.add(list.getString(i));
        }
    }

    public List<String> getChannels() {
        return channels;
    }
}