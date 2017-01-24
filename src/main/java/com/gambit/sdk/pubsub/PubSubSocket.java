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

/**
 * PubSubSocket is used to wrap the logic of Java websockets by extending {@link javax.websocket.Endpoint}
 * and implementing {@link javax.websocket.MessageHandler.Whole}.
 */
public class PubSubSocket extends Endpoint implements MessageHandler.Whole<String>
{
    /**
     * The {@link PubSubSocketConfigurator} used when creating this PubSubSocket.
     */
    private PubSubSocketConfigurator configurator;

    /**
     * The {@link PubSubOptions} used when creating this PubSubSocket.
     */
    private PubSubOptions options;

    /**
     * The asynchronous connection used to send requests to the Pub/Sub server.
     */
    private RemoteEndpoint.Async server;

    /**
     * The {@link Session} that represents this PubSubSocket as a websocket Endpoint connection.
     */
    private Session session;

    /**
     * Maps each outstanding request to the server by their sequence number with their {@link CompletableFuture}.
     */
    private Map<Long, CompletableFuture<JSONObject>> outstanding;

    /**
     * Maps the channel subscriptions of this PubSubSocket with the specific message handlers given for those channels. 
     */
    private Map<String, PubSubMessageHandler> msgHandlers;

    /**
     * Creates a connection to the Pub/Sub server given a {@link PubSubSocketConfigurator} and {@link PubSubOptions}
     * @param config The configuration requested for the connection represented by this PubSubSocket
     * @param options The options requested for the connection represented by this PubSubSocket
     * @throws DeploymentException
     * @throws IOException
     */
    public PubSubSocket(PubSubSocketConfigurator config, PubSubOptions options)
        throws DeploymentException, IOException
    {
        this.msgHandlers = Collections.synchronizedMap(new HashMap<>());
        this.outstanding = Collections.synchronizedMap(new HashMap<>());
        this.configurator = config;
        this.options = options;
        connect();
    }

    /**
     * Associates a {@link PubSubMessageHandler} to call for message received from the given channel.
     * @param channel The channel with which to associate the given handler
     * @param handler The {@link PubSubMessageHandler} that will be called for message from the given channel.
     */
    public void addMessageHandler(String channel, PubSubMessageHandler handler) {
        msgHandlers.put(channel, handler);
    }

    /**
     * Closes the connection represented by this PubSubSocket
     * @throws IOException
     */
    public void close() 
        throws IOException 
    {
        session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Closing by Choice"));
    }

    /**
     * Sends the given request, represented by the {@link org.json.JSONObject}, to the server and maps the
     * eventual result to be stored in a {@link java.util.concurrent.CompletableFuture} with the sequence
     * number of the message.
     * @param sequence Sequence number of the message
     * @param json The request to send to the Pub/Sub server
     * @return CompletableFuture<JSONObject> future that will contain server response to given request
     */
    protected CompletableFuture<JSONObject> sendMessage(long sequence, JSONObject json) {
        CompletableFuture<JSONObject> result = new CompletableFuture<>();
        outstanding.put(sequence, result);
        
        server.sendText(json.toString(), (sendResult) -> {
            if(!sendResult.isOK()) {
                result.completeExceptionally(new Exception("Could not send JSON Object: " + json.toString()));
            }
        });

        return result;
    }

    /**
     * Sends the given request, represented by the {@link org.json.JSONObject}, to the server and maps the
     * eventual result to be stored in a {@link java.util.concurrent.CompletableFuture} with the sequence
     * number of the message. Once the send is completed, the callback {@link javax.websocket.SendHandler}
     * is called. (Note: the callback is initiated after success or failur to send, not after receiving.)
     * @param sequence Sequence number of the message
     * @param json The request to send to the Pub/Sub server
     * @param handler The callback to initiate when sending is completed.
     * @return CompletableFuture<JSONObject> future which will complete when ???
     */
    protected CompletableFuture<JSONObject> sendMessage(long sequence, JSONObject json, SendHandler handler) {
        CompletableFuture<JSONObject> result = new CompletableFuture<>();
        result.complete(json);

        server.sendText(json.toString(), handler);
        return result;
    }

    /**
     * Initiates the connection the the Pub/Sub server with the configuration for this PubSubSocket
     * @throws DeploymentException
     * @throws IOException
     */
    private void connect() 
        throws DeploymentException, IOException 
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

    ///////////////////// EXTENDING ENDPOINT AND IMPLEMENTING MESSAGE_HANDLER ///////////////////// 

    /**
     * Called immediately after establishing the connection represented by this PubSubSocket
     * @param session The session that has just been activated by this PubSubSocket
     * @param config The configuration used to establish this PubSubSocket
     */
    @Override
    public void onOpen(Session session, EndpointConfig config) {
        session.addMessageHandler(this);
    }

    /**
     * Called immediately before closing the connection represented by this PubSubSocket
     * @param session The session that is about to be closed by this PubSubSocket
     * @param closeReason The reason for closing this PubSubSocket 
     */
    @Override
    public void onClose(Session session, CloseReason closeReason) {
    }

    /**
     * Called whenever the connection represented by this PubSubSocket produces errors
     * @param session The session that has produced an error
     * @param throwable The error that was thrown involving the session
     */
    @Override
    public void onError(Session session, Throwable throwable) {
    }

    /**
     * Called when receiving messages from the remote endpoint (Pub/Sub server). 
     * The method proprogates Pub/Sub messages to appropriate channels when it receives them,
     * and completes outstanding futures when receiving response to other requests.
     * @param message The message received from the remote endpoint
     */
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