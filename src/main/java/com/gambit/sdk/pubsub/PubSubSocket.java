package com.gambit.sdk.pubsub;

import javax.websocket.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletableFuture;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import java.io.IOException;

import java.net.URI;

import org.json.JSONObject;
import org.json.JSONException;

public class PubSubSocket extends Endpoint implements MessageHandler.Whole<String>
{
    PubSubSocketConfigurator configurator;
    PubSubOptions options;

    RemoteEndpoint.Async server;
    Session session;

    Map<Integer, CompletableFuture<JSONObject>> promiseMap;
    //CompletableFuture<JSONObject> testFuture;

    public PubSubSocket(PubSubSocketConfigurator config, PubSubOptions options)
        throws DeploymentException, IOException
    {
        this.promiseMap = Collections.synchronizedMap(new HashMap<>());

        this.configurator = config;
        this.options = options;
        connect();
    }

    public void connect()
        throws DeploymentException, IOException
    {
        //////////////////////////////////////////////////////////////////////////////////////////////////////////

        ClientEndpointConfig config = ClientEndpointConfig.Builder.create().configurator(configurator).build();
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        if(container == null) {
            throw new RuntimeException("There was no socket container implementation found.");
        }
        else {
            session = container.connectToServer(this, config, URI.create(options.getUrl()));
            server = session.getAsyncRemote();
        }

        if(session == null) {
            throw new RuntimeException("Could not instantiate connection to server.");
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    CompletableFuture<JSONObject> sendMessage(int sequence, JSONObject json) {
        CompletableFuture<JSONObject> result = new CompletableFuture<>();
        promiseMap.put(sequence, result);
        
        server.sendText(json.toString(), (sendResult) -> {
            if(!sendResult.isOK()) {
                result.completeExceptionally(new Exception("Could not send JSON Object: " + json.toString()));
            }
        });

        return result;
    }

    ///////////////////// EXTENDING ENDPOINT AND IMPLEMENTING MESSAGE_HANDLER /////////////////////////////////// 

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        session.addMessageHandler(this);
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {

    }

    @Override
    public void onError(Session session, Throwable throwable) {

    }

    @Override
    public void onMessage(String message) {
        //System.out.println("GOT A MESSAGE FROM THE SERVER: ");
        //System.out.println(message);

        JSONObject json = new JSONObject(message);
        int seq = json.getInt("seq");

        CompletableFuture f = promiseMap.get(seq);
        f.complete(json);
    }
}