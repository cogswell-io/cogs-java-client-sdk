package com.gabmit.sdk.pubsub.handlers;

import java.util.Optional;

public interface CloseHandler {
    void onClose(Optional<Throwable> error);
}