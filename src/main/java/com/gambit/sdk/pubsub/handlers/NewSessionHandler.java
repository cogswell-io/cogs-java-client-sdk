package com.gabmit.sdk.pubsub.handlers;

import java.util.UUID;

public interface NewSessionHandler {
    void onNewSession(UUID uuid);
}