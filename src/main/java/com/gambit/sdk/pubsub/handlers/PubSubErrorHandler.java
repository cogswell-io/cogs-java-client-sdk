package com.gambit.sdk.pubsub.handlers;

import java.util.Optional;

public interface PubSubErrorHandler {
    void onError(Throwable error, Long sequence, String channel);
}