package com.gambit.sdk.pubsub;

import java.util.UUID;

/**
 * Holds initialization options to use when first connect to Cogswell Pub/Sub
 */
public class PubSubOptions {

    /**
     * The url used for connecting to the Pub/Sub service
     */
    private final String url;

    /**
     * True if connection should auto-reconnect when dropped
     */
    private final boolean autoReconnect;

    /**
     * The amount of time, in milliseconds, when a connection attempt should timeout
     */
    private final long connectTimeout;

    /**
     * Holds UUID of the session to be restored if a session restore is requested.
     */
    private final UUID sessionUuid;

    /**
     * Initializes this PubSubOptions with all default values
     */
    private PubSubOptions() {
      this("wss://api.cogswell.io/pubsub", true, 30000L, null);
    }

    /**
     * Static instance of PubSubOptions that contains all default values.
     */
    public static final PubSubOptions DEFAULT_OPTIONS = new PubSubOptions();

    /**
     * Initializes this PubSubOptions with the given options, filling in null values with defaults.
     *
     * @param url            URL to which to connect (Deafult: "wss://api.cogswell.io/pubsub").
     * @param autoReconnect  True if connection should attempt to reconnect when disconnected (Default: true).
     * @param connectTimeout Time, in milliseconds, before connection should timeout (Default: 30000).
     * @param sessionUuid    UUID of session to restore, if requested (Default: null). 
     */
    public PubSubOptions(String url, Boolean autoReconnect, Long connectTimeout, UUID sessionUuid) {
      this.url = (url == null) ? "wss://api.cogswell.io/pubsub" : url;
      this.autoReconnect = (autoReconnect == null) ? false : autoReconnect;
      this.connectTimeout = (connectTimeout == null) ? 30000 : connectTimeout;
      this.sessionUuid = sessionUuid;
    }

    /**
     * Gets the url represented in this PubSubOptions for a connection.
     *
     * @return String The url represented in this PubSubOptions .
     */
    public String getUrl() {
      return url;
    }

    /**
     * Gets whether these options represent the request to auto-reconnect.
     *
     * @return boolean True if auto-reconnect was set.
     */
    public boolean getAutoReconnect() {
      return autoReconnect;
    }

    /**
     * Gets the time, in milliseconds, before a connection attempt should fail.
     * @return long Time, in milliseconds, before connection attempt should fail.
     */
    public long getConnectTimeout() {
      return connectTimeout;
    }

    /**
     * Gets the UUID of the session requested to be re-established using this PubSubOptions.
     * @return UUID UUID of session requested to be re-established.
     */
    public UUID getSessionUuid() {
      return sessionUuid;
    }
}