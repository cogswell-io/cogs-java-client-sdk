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

    public CompletableFuture<JSONObject> sendMessage(long sequence, JSONObject json) {
        CompletableFuture<JSONObject> result = new CompletableFuture<>();
        outstanding.put(sequence, result);
        
        server.sendText(json.toString(), (sendResult) -> {
            if(!sendResult.isOK()) {
                result.completeExceptionally(new Exception("Could not send JSON Object: " + json.toString()));
            }
        });

        System.out.println(outstanding.toString());
        return result/*.whenCompleteAsync((res, err) -> {
            if(res != null) System.out.println("RESULT: " + res.toString());
            if(err != null) System.out.println("ERROR: " + err.toString());
        })*/;
    }

    public void addMessageHandler(String channel, PubSubMessageHandler handler) {
        msgHandlers.put(channel, handler);
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
        System.out.println("RECEIVED FROM SERVER: ");
        //System.out.println("\t" + message);

        long seq = json.getLong("seq");
        CompletableFuture f;// = outstanding.get(seq);
        synchronized (outstanding) {
            f = outstanding.get(seq);
        }
        System.out.println("Got Sequence: " + seq);
        System.out.println("Got Future: " + f);

        /*if(json.getString("action").equals("msg")) {
            System.out.println("BLAH, BLAH, BLAH");

            String channel = json.getString("chan");
            String msg = json.getString("msg");
            String timestamp = json.getString("timestamp");
            String id = json.getString("id");

            PubSubMessageRecord record = new PubSubMessageRecord(channel, msg, timestamp, id);
            msgHandlers.get(channel).onMessage(record);
        }*/
        //else {
            f.complete(json);
        //}
    }
}