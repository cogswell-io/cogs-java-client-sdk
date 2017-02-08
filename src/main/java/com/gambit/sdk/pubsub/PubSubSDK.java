package com.gambit.sdk.pubsub;

import javax.websocket.*;

import java.io.IOException;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletableFuture;
import java.util.List;

/**
 * All initial connections made to Cogswell Pub/Sub are preformed through this class.
 * Thereafter, all operations are preformed through an instance of {@link PubSubHandle} 
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
     * Creates a connection with the given project keys, and defaults set for {@link PubSubOptions}.
     *
     * @param projectKeys List of project keys to use for authenticating the connection to establish.
     * @return {@code CompletableFuture<PubSubHandle>} Completes with a {@link PubSubHandle} used to make SDK requests. 
     */
    public CompletableFuture<PubSubHandle> connect(List<String> projectKeys) {
        return connect(projectKeys, PubSubOptions.DEFAULT_OPTIONS);
    }

    /**
     * Creates a connection with the given project keys, and the given {@link PubSubOptions}.
     *
     * @param projectKeys List of project keys to use for authenticating the connection to be establish.
     * @param options     {@link PubSubOptions} to use for the connection.
     * @return {@code CompletableFuture<PubSubHandle>} Completes with a {@link PubSubHandle} used to make SDK requests. 
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