package com.gambit.sdk.exceptions;

/**
 * Created by joel on 4/7/16.
 *
 * Runtime exception used to wrap exceptions in the example app and provide additional details.
 */
public class CogsException extends RuntimeException {
    public CogsException(String message) {
        super(message);
    }

    public CogsException(String message, Throwable cause) {
        super(message, cause);
    }
}