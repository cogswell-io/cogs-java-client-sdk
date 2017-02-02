package com.gambit.sdk.pubsub.handlers;

import java.util.Optional;

public interface PubSubCloseHandler {
    void onClose(Throwable error);
}