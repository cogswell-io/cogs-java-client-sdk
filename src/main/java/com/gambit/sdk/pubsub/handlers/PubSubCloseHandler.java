package com.gabmit.sdk.pubsub.handlers;

import java.util.Optional;

public interface PubSubCloseHandler {
    void onClose(Optional<Throwable> error);
}