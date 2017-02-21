package com.gambit.sdk.pubsub;

import javax.websocket.*;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.io.IOException;

import java.nio.ByteBuffer;

import java.net.URI;

import org.json.JSONObject;
import org.json.JSONException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.Cache;

import com.gambit.sdk.pubsub.responses.successes.*;
import com.gambit.sdk.pubsub.responses.errors.*;

import com.gambit.sdk.pubsub.exceptions.*;
import com.gambit.sdk.pubsub.responses.*;
import com.gambit.sdk.pubsub.handlers.*;

import com.gambit.sdk.pubsub.utils.PubSubUtils;

/**
 * Wraps the logic of Java websockets by extending {@link javax.websocket.Endpoint} and implementing 
 * {@link javax.websocket.MessageHandler.Whole}. It also tracks and routes both incoming and outgoing 
 * message to and from Cogswell Pub/Sub.
 */
public class PubSubSocket extends Endpoint implements MessageHandler.Whole<String>
{
    /**
     * Creates and connects a PubSubSocket to Cogswell Pub/Sub using the given project keys and options.
     * 
     * @param projectKeys List of project keys to use for authenticating the connection to be establish.
     * @param options     {@link PubSubOptions} to use for the connection.
     * @return {@code CompletableFuture<PubSubSocket>} Completes with connected underlying PubSubSocket on success.
     */
    public static CompletableFuture<PubSubSocket> connectSocket(List<String> projectKeys, PubSubOptions options) {
        CompletableFuture<PubSubSocket> future = new CompletableFuture<>();
        
        try {
            PubSubSocket socket = new PubSubSocket(projectKeys, options);

            socket.connect()
                .thenAcceptAsync((voidReturn) -> {
                    future.complete(socket);
                })
                .exceptionally((error) -> {
                    future.completeExceptionally(error);
                    return null;
                });
        }
        catch(Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    /**
     * This is the shortest default delay that the socket will wait between reconnects
     * Current it is set to 5 seconds = 5 s * 1000 ms/s = 5000 ms.
     */
    private static final long DEFAULT_RECONNECT_DELAY = 5000L; // 5 seconds

    /**
     * This is the longest the socket will wait between reconnects before giving up.
     * Currently it is set to 2 minutes = 2 min * 60 s/min * 1000 ms/s = 120000 ms.
     */
    private static final long MAX_RECONNECT_DELAY = 120000L; // 2 minutes

    /**
     * 
     */
    private static final ByteBuffer pingData = ByteBuffer.allocate(0);

    /**
     * The project keys that were used to create this PubSubSocket
     */
    private List<String> projectKeys;

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
    private Session websocketSession;

    /**
     * The ping interval for which after every interval the socket should ping the server 
     */
    private AtomicLong pingInterval;

    /**
     * True when the socket should ping the server every interval given in pingInterval
     */
    private AtomicBoolean doPings;

    /**
     * Tracks whether this socket is actually connected to the Pub/Sub server
     */
    private AtomicBoolean isConnected;

    /**
     * Holds whether to actually reconnect (based on length of delay, and whether close was chosen)
     */
    private AtomicBoolean autoReconnect;

    /**
     * Holds the next delay to wait when an attempted reconnect fails
     */
    private AtomicLong autoReconnectDelay;

    /**
     * Holds the current session uuid from the Pub/Sub server
     */
    private UUID sessionUuid;

    /**
     * Holds whether the most recent session UUID meant that a new session was generated
     */
    private AtomicBoolean isNewSession;

    /**
     * Maps each outstanding request to the server by their sequence number 
     * with their associated {@link java.util.concurrent.CompletableFuture}
     */
    private Cache<Long, CompletableFuture<PubSubResponse>> outstanding;

    /**
     * Maps outstanding publish request sequence numbers to their error handlers
     */
    private Cache<Long, PubSubErrorResponseHandler> publishErrorHandlers;

    /**
     * Maps outstanding publish requests with the publish request objects
     */
    private Cache<Long, JSONObject> publishRequests;

    /**
     * Maps the channel subscriptions of this PubSubSocket with the specific message handlers given for those channels. 
     */
    private Map<String, PubSubMessageHandler> msgHandlers;

    /**
     * Handler called whenever server generates a new session for this connection
     */
    private PubSubNewSessionHandler newSessionHandler;

    /**
     * Handler called whenever this connection must reconnect for some reason
     */
    private PubSubReconnectHandler reconnectHandler;

    /**
     * Handler called whenever any raw string json message is received from the server
     */
    private PubSubRawRecordHandler rawRecordHandler;

    /**
     * Handler called as general message handler whenever published messages are received from server 
     */
    private PubSubMessageHandler generalMsgHandler;

    /**
     * General error handler for when error responses are received from the server.
     */
    private PubSubErrorResponseHandler errorResponseHandler;

    /**
     * Handler called whenever an error having to do with this connection is encountered
     */
    private PubSubErrorHandler errorHandler;

    /**
     * Handler called whenever this connection closes
     */
    private PubSubCloseHandler closeHandler;

    /**
     * Stores any exceptions collected when closing this socket
     */
    private IOException closeException;

    /**
     * Creates a minimal PubSubSocket, used for testing purposes
     */
    protected PubSubSocket() {
        this.outstanding = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();
        this.publishRequests = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();
        this.publishErrorHandlers = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();

        this.msgHandlers = Collections.synchronizedMap(new Hashtable<>());

        this.autoReconnectDelay = new AtomicLong(DEFAULT_RECONNECT_DELAY);
        this.autoReconnect = new AtomicBoolean(false);
        this.isConnected = new AtomicBoolean(false);

        this.doPings = new AtomicBoolean(false);
        this.pingInterval = new AtomicLong(15);

        this.options = PubSubOptions.DEFAULT_OPTIONS;
    }

    /**
     * Creates a minimal PubSubSocket, using the provided server as the connect to which to send messages
     * Used for testing purposes
     * @param server The server to which to send messages
     */
    protected PubSubSocket(RemoteEndpoint.Async server) {
        this();
        this.server = server;
    }

    /**
     * Creates a connection to the Pub/Sub with the given projectKeys and options
     * @param projectKeys The permissions keys requested for interacting with the Pub/Sub server
     * @param options The options requested for the connection represented by this PubSubSocket
     * @throws DeploymentException
     * @throws IOException
     */
    public PubSubSocket(List<String> projectKeys, PubSubOptions options)
        throws DeploymentException, IOException, PubSubException
    {
        this.projectKeys = projectKeys;
        this.options = options;

        this.publishErrorHandlers = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();
        this.publishRequests = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();
        this.outstanding = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();

        this.msgHandlers = Collections.synchronizedMap(new Hashtable<>());

        this.autoReconnectDelay = new AtomicLong(options.getConnectTimeout());
        this.autoReconnect = new AtomicBoolean(options.getAutoReconnect());
        this.isConnected = new AtomicBoolean(false);

        this.doPings = new AtomicBoolean(false);
        this.pingInterval = new AtomicLong(15);
    }

    /**
     * Closes the connection represented by this PubSubSocket
     */
    public void close()
    {
        autoReconnect.set(false);

        try {
            websocketSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Initiated a Standard Close"));
        }
        catch(IOException e) {
            closeException = e;
        }
    }

    /**
     * Sends the given request, represented by the {@link org.json.JSONObject}, to the server and maps the
     * eventual result to be stored in a {@link java.util.concurrent.CompletableFuture} with the sequence
     * number of the message.
     * @param sequence Sequence number of the message
     * @param json The request to send to the Pub/Sub server
     * @return {@code CompletableFuture<JSONObject>} future that will contain server response to given request
     */
    protected CompletableFuture<PubSubResponse> sendRequest(long sequence, JSONObject json) {
        CompletableFuture<PubSubResponse> result = new CompletableFuture<>();
        outstanding.put(sequence, result);

        server.sendText(json.toString(), (sendResult) -> {
            if(!sendResult.isOK()) {
                if(errorHandler != null) {
                    errorHandler.onError(sendResult.getException());
                }

                result.completeExceptionally(new Exception("Could not send JSON Object: " + json.toString()));
                outstanding.invalidate(sequence);
            }
        });

        return result;
    }

    /**
     * Sends the given request, represented by the {@link org.json.JSONObject}. Once the send completes
     * the callback {@link javax.websocket.SendHandler} is called. (Note: The callback is initiated for
     * sending the data only. It does NOT mean that anything was received for that send.)
     *
     * @param sequence Sequence number of the message
     * @param json The request to send to the Pub/Sub server
     * @param pubSubErrorHandler The handler that is called if some exception or error is thrown
     * @param handler The callback to initiate when sending is completed.
     */
    protected void sendPublish(long sequence, JSONObject json, PubSubErrorResponseHandler errorResponseHandler, SendHandler handler) {
        if(errorResponseHandler != null) {
            publishErrorHandlers.put(sequence, errorResponseHandler);
        }

        server.sendText(json.toString(), (sendResult) -> {
            if(!sendResult.isOK()) {
                if(errorHandler != null) {
                    errorHandler.onError(sendResult.getException());
                }

                handler.onResult(sendResult);
            }
            else {
                handler.onResult(sendResult);
            }
        });
    }

    /**
     * Sends the given request, represented by the {@link org.json.JSONObject}, to the server and maps the
     * eventual result to be stored in a {@link java.util.concurrent.CompletableFuture} with the sequence
     * number of the message. Once the send is completed, the callback {@link javax.websocket.SendHandler}
     * is called. (Note: the callback is initiated after success or failure to send, not after receiving.)
     * @param sequence Sequence number of the message
     * @param json The request to send to the Pub/Sub server
     * @param handler The callback to initiate when sending is completed.
     * @return {@code CompletableFuture<JSONObject>} future which will complete when ???
     */
    protected CompletableFuture<PubSubResponse> sendPublishWithAck(long sequence, JSONObject json, SendHandler handler) {
        CompletableFuture<PubSubResponse> result = new CompletableFuture<>();
        outstanding.put(sequence, result);
        
        server.sendText(json.toString(), (sendResult) -> {
            if(!sendResult.isOK()) {
                if(errorHandler != null) {
                    errorHandler.onError(sendResult.getException());
                }

                handler.onResult(sendResult);
            }
            else {
                handler.onResult(sendResult);
            }
        });

        return result;
    }

    /**
     * This method (used for testing purposes only) drops  underlying connection and reconnects with delay of msDelay milliseconds. 
     * @param msDelay Delay in milliseconds to wait before attempting a reconnect
     */
    protected void dropConnection(PubSubDropConnectionOptions dropOptions) {
        try {
            autoReconnectDelay.set(dropOptions.getAutoReconnectDelay());
            websocketSession.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Dropped Connection"));
        }
        catch(IOException e) {
            closeException = e;
        }
    }

    /**
     * Initiates the connection the the Pub/Sub server with the configuration for this PubSubSocket
     * @throws DeploymentException
     * @throws IOException
     */
    private CompletableFuture<Void> connect() {
        // Code Information: The connectToServer will block until it connects or throws an exception. Within Tyrus
        //                   there is an asyncConnectToServer method which returns a java.util.Future, but Futures
        //                   in java are blocking once get() is called on them.

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            PubSubSocketConfigurator configurator = new PubSubSocketConfigurator(projectKeys, sessionUuid);
            ClientEndpointConfig config = ClientEndpointConfig.Builder.create().configurator(configurator).build();
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();

            if(container != null) {
                try {
                    websocketSession = container.connectToServer(this, config, URI.create(options.getUrl()));
                    //server = websocketSession.getAsyncRemote();
                }
                catch(Exception e) {
                    throw new CompletionException(e);
                }
            }
            else {
                PubSubException e = new PubSubException("There was no socket container implementation found.");; 
                throw new CompletionException(e);
            }

            if(websocketSession == null) {
                PubSubException e = new PubSubException("Could not instantiate connection to server.");
                throw new CompletionException(e);
            }
        });

        return future;
    }

    /**
     * Attempts to reconnects a socket that has been dropped for any reason other than intentionally and cleanly disconnecting
     * @return CompletableFuture<Void> future that completes successfully when connected, with an error otherwise
     */
    private CompletableFuture<Void> reconnect() {
        return connect()
            .whenComplete((pubsubsocket, error) -> {
                if(reconnectHandler != null) {
                    reconnectHandler.onReconnect();
                }
            });
    }

    /**
     * Continuously attempts to reconnect to a socket after given time in milliseconds if the socket was not closed cleanly.
     * This method uses a backoff scheme whereby the time between the reconnect attempts increases up to MAX_RECONNECT_DELAY. 
     * @param msUntilNextRetry initial amount of time to wait before first reconnect attempt
     */
    private void reconnectRetry(final long msUntilNextRetry) {
        PubSubUtils.setTimeout(() -> {
            reconnect()
                .whenComplete((socket, error) -> { 
                    /* We Reconnected, We're done */ 
                })
                .exceptionally((error) -> {
                    long minimumDelay = Math.max(DEFAULT_RECONNECT_DELAY, msUntilNextRetry);
                    long nextDelay = Math.min(minimumDelay * 2, MAX_RECONNECT_DELAY);

                    reconnectRetry(nextDelay);
                    return null;
                });

        }, msUntilNextRetry);
    }

    /**
     * Continuously pings the remote server with interval {@code msBetweenPings} between pings.
     * @param msBetweenPings Interval to wait between sending two pings to remote server 
     */
    private void pingRemoteRepeat(final long msBetweenPings) {
        new Thread(() -> {
            while(doPings.get()) {
                try {
                    if(server != null && isConnected.get()) {
                        server.sendPing(pingData);
                    }
                    Thread.sleep(msBetweenPings);
                }
                catch(Exception e) {
                    if(errorHandler != null) {
                        errorHandler.onError(e);
                    }
                }
            }
        }).start();
    }

    ///////////////////// EXTENDING ENDPOINT AND IMPLEMENTING MESSAGE_HANDLER ///////////////////// 

    /**
     * Called immediately after connection is established, but before server is created
     * @param session The session that has just been activated by this PubSubSocket
     * @param config The configuration used to establish this PubSubSocket
     */
    @Override
    public void onOpen(Session session, EndpointConfig config) {
        session.addMessageHandler(this);
        server = session.getAsyncRemote();

        isConnected.set(true);

        doPings.set(true);
        pingRemoteRepeat(pingInterval.get());
        
        autoReconnectDelay.set(DEFAULT_RECONNECT_DELAY);

        if(autoReconnect.get()) {
            (new PubSubHandle(this, -1L)).getSessionUuid()
                .thenAccept((uuid) -> {
                    if(sessionUuid == null || !sessionUuid.toString().equals(uuid.toString())) {
                        sessionUuid = uuid;

                        if(newSessionHandler != null) {
                            newSessionHandler.onNewSession(sessionUuid);
                        }
                    }
                })
                .exceptionally((error) -> {
                    isConnected.set(false);
                    return null;
                });
        }
    }

    /**
     * Called immediately before closing the connection represented by this PubSubSocket
     * @param session The session that is about to be closed by this PubSubSocket
     * @param closeReason The reason for closing this PubSubSocket 
     */
    @Override
    public void onClose(Session session, CloseReason closeReason) {
        server = null;
        doPings.set(false);
        isConnected.set(false);

        if(closeHandler != null) {
            closeHandler.onClose(closeException);
        }

        if(options.getAutoReconnect() == true) {
            reconnectRetry(autoReconnectDelay.get());
        }
    }

    /**
     * Called whenever the connection represented by this PubSubSocket produces errors
     * @param session The session that has produced an error
     * @param throwable The error that was thrown involving the session
     */
    @Override
    public void onError(Session session, Throwable throwable) {
        throwable.printStackTrace();

        if(errorHandler != null) {
            errorHandler.onError(throwable);
        }
    }

    /**
     * Called when receiving messages from the remote endpoint (Pub/Sub server). 
     * The method proprogates Pub/Sub messages to appropriate channels when it receives them,
     * and completes outstanding futures when receiving response to other requests.
     * @param message The message received from the remote endpoint
     */
    @Override
    public void onMessage(String message) {
        if(rawRecordHandler != null) {
            rawRecordHandler.onRawRecord(message);
        }

        // TODO: validate format of message received from server, if invalid call error

        Long seq = null;

        try {
            JSONObject json = new JSONObject(message);
            if(json.getString("action").equals("msg")) {
                String id = json.getString("id");
                String msg = json.getString("msg");
                String time = json.getString("time");
                String chan = json.getString("chan");

                PubSubMessageRecord record = new PubSubMessageRecord(chan, msg, time, id);
                PubSubMessageHandler handler = msgHandlers.get(chan);
                handler.onMessage(record);

                if(generalMsgHandler != null) {
                    generalMsgHandler.onMessage(record);
                }
            }
            else if(!json.has("seq")) {
                // This should never happen, but if it does, propogate to error handle if provided.
                if(errorHandler != null) {
                    errorHandler.onError(new PubSubException(json.toString()));
                }
            }
            else if(json.getInt("code") != 200) {
                seq = new Long(json.getLong("seq"));
                CompletableFuture<PubSubResponse> responseFuture = outstanding.getIfPresent(seq);

                if(responseFuture != null) {
                    responseFuture.completeExceptionally(new PubSubException(json.toString()));
                }

                PubSubErrorResponseHandler publishErrorResponseHandler = publishErrorHandlers.getIfPresent(seq);

                String action = json.getString("action");
                String details = json.getString("details");
                String errorMessage = json.getString("message");
                int code = json.getInt("code");

                PubSubErrorResponse errorResponse = new PubSubErrorResponse(code, seq, action, details, errorMessage);

                if(publishErrorResponseHandler != null) {
                    publishErrorResponseHandler.onErrorResponse(errorResponse);
                }

                if(errorResponseHandler != null) {
                    errorResponseHandler.onErrorResponse(errorResponse);
                }

                outstanding.invalidate(seq);
            }
            else {
                seq = new Long(json.getLong("seq"));
                CompletableFuture<PubSubResponse> responseFuture = outstanding.getIfPresent(seq);

                try {
                    if(responseFuture != null) {
                        responseFuture.complete(PubSubResponse.create(json));
                    }
                }
                catch(PubSubException ex) {
                    if(responseFuture != null) {
                        responseFuture.completeExceptionally(ex);
                    }

                    if(errorHandler != null) {
                        errorHandler.onError(new PubSubException(json.toString()));
                    }
                }
                
                outstanding.invalidate(seq);
            }
        }
        catch(JSONException e) {
            // Could not parse JSON
            if(seq != null) {
                CompletableFuture<PubSubResponse> responseFuture = outstanding.getIfPresent(seq);
                
                if(responseFuture != null) {
                    responseFuture.completeExceptionally(e);
                }
            }

            if(errorHandler != null) {
                errorHandler.onError(e);
            }
        }
    }

    //////////////////////// HANDLERS THAT ARE PROVIDED BY A PUBSUBHANDLE ////////////////////////

    /**
     * Registers a handler to call whenever a new session is generated by the server
     * @param handler The handler to call
     */
    public void setNewSessionHandler(PubSubNewSessionHandler handler) {
        newSessionHandler = handler;
    }

    /**
     * Registers a handler that will be called any time the underlying socket must be reconnected
     * @param handler The handler to register for the reconnects
     */
    public void setReconnectHandler(PubSubReconnectHandler handler) {
        reconnectHandler = handler;
    }

    /**
     * Register a handler to call whenever a raw record (string json) is received from the server.
     * @param handler The handler to register 
     */
    public void setRawRecordHandler(PubSubRawRecordHandler handler) {
        rawRecordHandler = handler;
    }

    /**
     * Registers a handler to call if there are failures working with the underlying socket
     *
     * @param handler The handler to register
     */
    public void setErrorHandler(PubSubErrorHandler handler) {
        errorHandler = handler;
    }

    /**
     * Registers a handler to call if error response are received from the server.
     *
     * @param handler The handler to register
     */
    public void setErrorResponseHandler(PubSubErrorResponseHandler handler) {
        errorResponseHandler = handler;
    }

    /**
     * Register a handler to call whenever the underlying socket is actually closed.
     * @param handler The handler to register
     */
    public void setCloseHandler(PubSubCloseHandler handler) {
        closeHandler = handler;
    }

    /**
     * Registers a general handler that receives and handles message from all channels.
     * @param handler The handler to be registered 
     */
    public void setMessageHandler(PubSubMessageHandler handler) {
        generalMsgHandler = handler;
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
     * Disassociates the current {@link PubSubMessageHandler}, if any, with the given channel.
     * @param channel The channel from which to remove the handler
     */
    public void removeMessageHandler(String channel) {
        msgHandlers.remove(channel);
    }
}