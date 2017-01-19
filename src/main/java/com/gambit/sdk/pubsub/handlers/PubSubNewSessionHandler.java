package com.gabmit.sdk.pubsub.handlers;

import java.util.UUID;

public interface PubSubNewSessionHandler {
    void onNewSession(UUID uuid);
}