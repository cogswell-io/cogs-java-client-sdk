package com.gambit.sdk.pubsub;

import java.time.Instant;
import java.util.UUID;

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
     * Creates the PubSubMessageRecord filled in with the provided information.
     *
     * @param channel   Channel to which the message is published.
     * @param message   Content of the message that is published.
     * @param timestamp String representing time the message was published, formatted as ISO_INSTANT.
     * @param id        UUID formatted string representing the UUID of the message to be published. 
     */
    public PubSubMessageRecord(String channel, String message, String timestamp, String id) {
        this.channel = channel;
        this.message = message;

        this.timestamp = Instant.parse(timestamp);
        this.id = UUID.fromString(id);
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