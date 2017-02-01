package com.gambit.sdk.pubsub;

import javax.websocket.*;

import java.io.IOException;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletableFuture;
import java.util.List;

/**
 * The main class that all SDK users will use to work with the Pub/Sub SDK. 
 */
public class PubSubSDK {
    /**
     * Singleton instance.
     */
    private static PubSubSDK instance;

    /**
     * Creates {@link PubSubSDK} instance if none exists, otherwise returns the existing instance.
     * @return PubSubSDK Instance used to work with the Pub/Sub SDK 
     */
    public static PubSubSDK getInstance() {
        if(instance == null) {
            instance = new PubSubSDK();
        }

        return instance;
    }

    /**
     * Singleton constructor
     */
    private PubSubSDK() {
        // No setup needed
    }

    /**
     * Creates a connection with the given project keys, and the defaults set for the {@link PubSubOptions}
     * @param projectKeys The list of requested keys for the connection to be established
     * @return CompletableFuture<PubSubHandle> future that will contain {@PubSubHandle} used for making SDK requests 
     */
    public CompletableFuture<PubSubHandle> connect(List<String> projectKeys) {
        return connect(projectKeys, new PubSubOptions());
    }

    /**
     * Creates a connection with the given project keys, and the given {@link PubSubOptions}
     * @param projectKeys The list of requested keys for the connection to be established
     * @param options The {@link PubSubOptions} to use for the connection to be established
     * @return CompletableFuture<PubSubHandle> future that will contain {@PubSubHandle} used for making SDK requests 
     */
    public CompletableFuture<PubSubHandle> connect(List<String> projectKeys, PubSubOptions options) {
        CompletableFuture<PubSubHandle> future = new CompletableFuture<>();

        PubSubSocket.connectSocket(projectKeys, options)
            .thenAcceptAsync((socket) -> {
                future.complete(new PubSubHandle(socket));
            })
            .exceptionally((error) -> {
                future.completeExceptionally(error);
                return null;
            });

        return future;
    }
}