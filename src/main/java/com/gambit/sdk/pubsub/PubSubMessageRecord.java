package com.gambit.sdk.pubsub;

import java.time.Instant;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a Cogswell Pub/Sub message record holding published message content and associated information. 
 */
public class PubSubMessageRecord
{
    /**
     * The timestamp of the message represented by this PubSubMessageRecord.
     */
    private final Instant timestamp;

    /**
     * The channel to which the message represented by this PubSubMessageRecord was published.
     */
    private final String channel;

    /**
     * The content of the published message represented by this PubSubMessageRecord.
     */
    private final String message;

    /**
     * The UUID of the message represented by this PubSubMessageRecord
     */
    private final UUID id;

    /**
     * Creates the PubSubMessageRecord filled in with info from the message 
     * 
     * @param jsonObj The JSONObject representing the message record
     */
    public PubSubMessageRecord(JSONObject jsonObj) throws JSONException {
        message = jsonObj.getString("msg");
        channel = jsonObj.getString("chan");
        id = UUID.fromString(jsonObj.getString("id"));
        timestamp = Instant.parse(jsonObj.getString("time"));
    }

    /**
     * Returns the channel to which the message represented by this PubSubMessageRecord was published.
     *
     * @return String
     */
    public String getChannel() { 
        return channel; 
    }

    /**
     * Returns the content of the message represented by this PubSubMessageRecord that was published.
     *
     * @return String
     */
    public String getMessage() { 
        return message; 
    }

    /**
     * Returns the timestamp of the message represented by this PubSubMessageRecord.
     *
     * @return Instant
     */
    public Instant getTimestamp() { 
        return timestamp; 
    }

    /**
     * Returns the UUID of the message represented by this PubSubMessageRecord.
     *
     * @return UUID 
     */
    public UUID getId() { 
        return id; 
    }
}