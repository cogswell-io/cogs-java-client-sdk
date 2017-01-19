package com.gabmit.sdk.pubsub.handlers;

import java.util.Optional;

public interface PubSubErrorHandler {
    void onError(Throwable error, Optional<Long> sequence, Optional<String> channel);
}