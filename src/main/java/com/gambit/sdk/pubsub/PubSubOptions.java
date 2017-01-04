package com.gambit.sdk.pubsub;

public class PubSubOptions {
    String url;
    boolean autoReconnect;
    long connectTimeout;

    public PubSubOptions() {
      this('wss://api.cogswell.io/pubsub', true, 30000);
    }

    public PubSubOptions(String url) {
      this(url, true, 30000);
    }

    public PubSubOptions(String url, boolean autoReconnect) {
      this(url, autoReconnect, 30000);
    }

    public PubSubOptions(String url, boolean autoReconnect, long connectTimout) {
      this.url = url;
      this.autoReconnect = autoReconnect;
      this.connectTimeout = connectTimeout;
    }
}