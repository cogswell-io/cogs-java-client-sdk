package com.gambit.sdk.pubsub.exceptions;

public class PubSubException extends Exception {
    public PubSubException() {
        super();
    }
    
    public PubSubException(String message) {
        super(message);
    }
}