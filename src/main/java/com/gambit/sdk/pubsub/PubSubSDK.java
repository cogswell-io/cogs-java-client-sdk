package com.gambit.sdk.pubsub;

import javax.websocket.*;

import java.io.IOException;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletableFuture;
import java.util.List;

public class PubSubSDK {
    private static PubSubSDK instance;

    private PubSubSDK() {
        // No setup needed
    }

    public static PubSubSDK getInstance() {
        if(instance == null) {
            instance = new PubSubSDK();
        }

        return instance;
    }

    public CompletableFuture<PubSubHandle> connect(List<String> projectKeys)
    {
        return connect(projectKeys, new PubSubOptions());
    }

    public CompletableFuture<PubSubHandle> connect(List<String> projectKeys, PubSubOptions options)
    {
        CompletableFuture promise = CompletableFuture.supplyAsync(() -> {
            try {
                PubSubSocketConfigurator configurator = new PubSubSocketConfigurator(projectKeys);
                PubSubOptions opts = (options == null) ? options : new PubSubOptions();

                PubSubSocket socket = new PubSubSocket(configurator, options);
                PubSubHandle handle = new PubSubHandle(socket);

                return handle;
            }
            catch(Exception e) {
                throw new CompletionException(e);
            }
        });

       return promise;
    }
}