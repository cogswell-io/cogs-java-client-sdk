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

import com.gambit.sdk.pubsub.handlers.*;

public class PubSubSocket extends Endpoint implements MessageHandler.Whole<String>
{
    private PubSubSocketConfigurator configurator;
    private PubSubOptions options;

    private RemoteEndpoint.Async server;
    private Session session;

    private Map<Long, CompletableFuture<JSONObject>> outstanding;
    private Map<String, PubSubMessageHandler> msgHandlers;
    //CompletableFuture<JSONObject> testFuture;

    public PubSubSocket(PubSubSocketConfigurator config, PubSubOptions options)
        throws DeploymentException, IOException
    {
        this.msgHandlers = Collections.synchronizedMap(new HashMap<>());
        this.outstanding = Collections.synchronizedMap(new HashMap<>());
        this.configurator = config;
        this.options = options;
        connect();
    }

    public void connect() throws DeploymentException, IOException
    {
        // TODO: make async
        ClientEndpointConfig config = ClientEndpointConfig.Builder.create().configurator(configurator).build();
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        if(container != null) {
            session = container.connectToServer(this, config, URI.create(options.getUrl()));
            server = session.getAsyncRemote();
        }
        else {
            throw new RuntimeException("There was no socket container implementation found.");
        }

        if(session == null) {
            throw new RuntimeException("Could not instantiate connection to server.");
        }
    }

    public CompletableFuture<JSONObject> sendMessage(long sequence, JSONObject json)
    {
        CompletableFuture<JSONObject> result = new CompletableFuture<>();
        outstanding.put(sequence, result);
        
        server.sendText(json.toString(), (sendResult) -> {
            if(!sendResult.isOK()) {
                result.completeExceptionally(new Exception("Could not send JSON Object: " + json.toString()));
            }
        });

        return result;
    }

    public CompletableFuture<JSONObject> sendMessage(long sequence, JSONObject json, SendHandler handler) {
        CompletableFuture<JSONObject> result = new CompletableFuture<>();
        result.complete(json);

        server.sendText(json.toString(), handler);
        return result;
    }

    public void addMessageHandler(String channel, PubSubMessageHandler handler) {
        msgHandlers.put(channel, handler);
    }

    public void close() throws IOException {
        session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Closing by Choice"));
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
        JSONObject json = new JSONObject(message);
        //System.out.println("RECEIVED FROM SERVER: ");
        //System.out.println("\t" + message);

        if(json.getString("action").equals("msg")) {
            String id = json.getString("id");
            String msg = json.getString("msg");
            String time = json.getString("time");
            String chan = json.getString("chan");

            PubSubMessageRecord record = new PubSubMessageRecord(chan, msg, time, id);
            PubSubMessageHandler handler = msgHandlers.get(chan);
            handler.onMessage(record);
        }
        else {
            long seq = json.getLong("seq");
            outstanding.get(seq).complete(json);
        }
    }
}