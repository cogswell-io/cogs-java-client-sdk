package com.gambit.sdk.pubsub;

import javax.websocket.*;

import java.io.IOException;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletableFuture;
import java.util.List;

import java.net.URI;

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
                PubSubOptions opts = options;

                PubSubSocketConfigurator configurator = new PubSubSocketConfigurator(projectKeys);
                PubSubSocket sock = new PubSubSocket();

                ClientEndpointConfig config = ClientEndpointConfig.Builder.create().configurator(configurator).build();
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();

                if(container == null) {
                    throw new CompletionException(new Exception("There was no socket container implementation found."));
                }
                else {
                    System.out.println(":: BEFORE CONNECTION TO SOCKET");
                    container.connectToServer(sock, config, URI.create(opts.getUrl()));
                    System.out.println(":: AFTER CONNECTION TO SOCKET");
                }

                return null;
            }
            catch(Exception e) {
                throw new RuntimeException(e);
            }
        });

       return promise;
    }
}