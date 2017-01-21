package com.gambit.sdk.pubsub.handlers;

import java.util.Optional;

public interface PubSubCloseHandler {
    void onClose(Optional<Throwable> error);
}