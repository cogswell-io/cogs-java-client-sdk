package com.gambit.sdk.pubsub;

public class PubSubDropConnectionOptions {
    private final long autoReconnectDelay;

    public PubSubDropConnectionOptions(long autoReconnectDelay) {
        this.autoReconnectDelay = autoReconnectDelay;
    }

    public long getAutoReconnectDelay() {
        return autoReconnectDelay;
    }
}