package com.gambit.sdk.pubsub;

import java.util.UUID;

public class PubSubOptions {
    private String url;
    private boolean autoReconnect;
    private long connectTimeout;
    private UUID sessionUuid;

    public PubSubOptions() {
      // For Debugging
      this("ws://localhost:8888", false, 30000, null);
      
      //this("wss://api.cogswell.io/pubsub", true, 30000, null);
    }

    public PubSubOptions(String url) {
      this(url, true, 30000, null);
    }

    public PubSubOptions(String url, boolean autoReconnect) {
      this(url, autoReconnect, 30000, null);
    }

    public PubSubOptions(String url, boolean autoReconnect, long connectTimeout) {
      this(url, autoReconnect, connectTimeout, null);
    }

    public PubSubOptions(String url, boolean autoReconnect, long connectTimeout, UUID sessionUuid) {
      this.url = url;
      this.autoReconnect = autoReconnect;
      this.connectTimeout = connectTimeout;
      this.sessionUuid = sessionUuid;
    }

    public String getUrl() {
      return url;
    }

    public boolean getAutoReconnect() {
      return autoReconnect;
    }

    public long getConnectTimeout() {
      return connectTimeout;
    }

    public UUID getSessionUuid() {
      return sessionUuid;
    }
}