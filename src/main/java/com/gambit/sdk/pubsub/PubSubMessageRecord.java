package com.gambit.sdk.pubsub;

import java.time.Instant;
import java.util.UUID;

/**
 * This class represents the information that should be passed on when receiving a Pub/Sub message
 */
public class PubSubMessageRecord
{
    /**
     * The timestamp of the Pub/Sub message
     */
    private final Instant timestamp;

    /**
     * The channel to which the Pub/Sub message is published
     */
    private final String channel;

    /**
     * The actual message stored in the Pub/Sub message to be published
     */
    private final String message;

    /**
     * The UUID of the Pub/Sub message 
     */
    private final UUID id;

    /**
     * Constructs the PubSubMessageRecord with the provided information
     * @param channel The channel to which the Pub/Sub message is meant to be published
     * @param message The actual message with the Pub/Sub message that should be published
     * @param timestamp The ISO_INSTANT formatted string representing the time the Pub/Sub message was published
     * @param id The UUID formatted string representing the UUID of the message to be published 
     */
    public PubSubMessageRecord(String channel, String message, String timestamp, String id)
    {
        this.channel = channel;
        this.message = message;

        this.timestamp = Instant.parse(timestamp);
        this.id = UUID.fromString(id);
    }

    /**
     * Returns the channel stored by this PubSubMessageRecord
     * @return String
     */
    public String getChannel() { return channel; }

    /**
     * Returns the message content from the original Pub/Sub message stored by this PubSubMessageRecord
     * @return String
     */
    public String getMessage() { return message; }

    /**
     * Returns the timestamp from the original Pub/Sub message stored by this PubSubMessageRecord
     * @return java.time.Instant
     */
    public Instant getTimestamp() { return timestamp; }

    /**
     * Returns the UUID of the original Pub/Sub message stored by this PubSubMessageRecord
     * @return java.util.UUID 
     */
    public UUID getId() { return id; }
}