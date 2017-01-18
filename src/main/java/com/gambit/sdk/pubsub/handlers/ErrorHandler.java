package com.gabmit.sdk.pubsub.handlers;

import java.util.Optional;

public interface ErrorHandler {
    void onError(Throwable error, Optional<Long> sequence, Optional<String> channel);
}