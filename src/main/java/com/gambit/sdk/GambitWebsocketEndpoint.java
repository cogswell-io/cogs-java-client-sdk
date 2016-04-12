package com.gambit.sdk;

import com.gambit.sdk.exceptions.CogsException;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@ClientEndpoint
public class GambitWebsocketEndpoint extends Endpoint {

    /**
     * The thread loop for all scheduled tasks
     */
    protected final ScheduledExecutorService mExecutorService = Executors.newSingleThreadScheduledExecutor();

    /**
     * Send PING binary frame every once in a while for a healthy relationship with the server
     */
    protected Runnable mPingRunnable = new Runnable() {
        @Override
        public void run() {
            sendPing();
        }
    };

    /**
     * The retry thread. Retries every minute, until the end of time...
     */
    protected Runnable mRetryRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning()) {
                init();
            }
            else if (mRetryScheduler != null) {
                mRetryScheduler.cancel(true);
            }
        }
    };

    /**
     * Indicate how many immediate retries could we make before switching to periodic retry
     */
    protected static final int MAX_FAST_RETRY = 5;

    /**
     * Indicate the PING/PONG cycle frequency
     */
    protected static final int PING_RATE_MINUTES = 2;

    /**
     * The URL to connect to
     */
    protected URI mEndpointUrl;

    /**
     * The authentication payload
     */
    protected String mPayload;

    /**
     * The authorization signature
     */
    protected String mSignature;

    /**
     * Message handler provided by the instance creator
     */
    protected MessageHandler.Whole<String> mMessageHandler;

    /**
     * PING handler provided by the instance creator
     */
    protected MessageHandler.Whole<PongMessage> mPingPongHandler;

    /**
     * The control point for the PING management thread
     */
    protected ScheduledFuture<?> mPingScheduler;

    /**
     * The control point for the retry sequencer
     */
    protected ScheduledFuture<?> mRetryScheduler;

    /**
     * The websocket session object
     */
    protected Session mSession = null;

    /**
     * The websocket container object
     */
    protected WebSocketContainer mContainer;

    /**
     * Connection retry counter
     */
    protected int mRetry = 0;

    /**
     * Indication whether the connection is established and well-working
     */
    protected boolean mIsRunning = false;

    /**
     * Create a websocket client endpoint instance
     * @param endpoint The server URL to connect to
     * @param payload The payload used for authorization
     * @param signature The signature used for proving the payload authenticity
     * @param message_handler The handler to be attached to the session object for receiving messages
     * @param ping_handler The handler to be attached to the session object for acknowledging PING/PONG packets
     */
    public GambitWebsocketEndpoint(URI endpoint, final String payload, final String signature, final MessageHandler.Whole<String> message_handler, final MessageHandler.Whole<PongMessage> ping_handler) {
        this.mEndpointUrl = endpoint;
        this.mPayload = payload;
        this.mSignature = signature;
        this.mMessageHandler = message_handler;
        this.mPingPongHandler = ping_handler;

        init();
    }

    /**
     * Initialize the websocket endpoint and attempt a connection establishment.
     */
    protected void init() {
        try {
            mContainer = ContainerProvider.getWebSocketContainer();

            ClientEndpointConfig.Configurator configurator = new ClientEndpointConfig.Configurator() {
                public void beforeRequest(Map<String, List<String>> headers) {
                    headers.put("Payload-HMAC", Collections.singletonList(mSignature));
                    headers.put("JSON-Base64", Collections.singletonList(mPayload));

                }
            };

            ClientEndpointConfig clientConfig = ClientEndpointConfig.Builder.create()
                    .configurator(configurator)
                    .build();

            mContainer.setDefaultMaxSessionIdleTimeout(300000); //5 minutes?
            mContainer.connectToServer(this, clientConfig, mEndpointUrl);
        } catch (Exception e) {
            throw new CogsException("Failed to initialize Cogs push WebSocket.", e);
        }
    }

    /**
     * Get the session object that controls the communication and holds all streams and handlers.
     * @return The session object.
     */
    public Session getSession() {
        return mSession;
    }

    /**
     * Triggers when the connection is established.
     * @param session The session object
     * @param config The configuration object
     */
    @Override
    public void onOpen(Session session, EndpointConfig config) {
        mSession = session;
        mRetry = 0; //reset error counter
        mIsRunning = true;

        mSession.addMessageHandler(mMessageHandler);
        mSession.addMessageHandler(mPingPongHandler);

        mPingScheduler = mExecutorService.scheduleAtFixedRate(mPingRunnable, PING_RATE_MINUTES, PING_RATE_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * Triggers when the connection is closed. Regardless of the reason.
     * @param session The session object
     * @param closeReason The {@link CloseReason}, which is almost never worth something
     */
    @Override
    public void onClose(Session session, CloseReason closeReason) {
        super.onClose(session, closeReason);

        if (mPingScheduler != null && !mPingScheduler.isCancelled()) {
            mPingScheduler.cancel(true); //stop ping
        }

        if (mRetryScheduler != null) {
            mRetryScheduler.cancel(true);
        }

        mIsRunning = false;

        if (!closeReason.getCloseCode().equals(CloseReason.CloseCodes.NORMAL_CLOSURE)) {
            mRetry++;

            if (mRetry <= MAX_FAST_RETRY) {
                init();
            }
            else {
                //fallback to periodic retry

                mRetryScheduler = mExecutorService.scheduleAtFixedRate(mRetryRunnable, 1, 1, TimeUnit.MINUTES);
            }
        }
    }

    /**
     * Triggers when the websocket encounters an error, typically during initialization.
     * @param session The session object
     * @param thr A handy stacktrace
     */
    @Override
    public void onError(Session session, Throwable thr) {
        super.onError(session, thr);

        mIsRunning = false;

        thr.printStackTrace(); //good to know
    }

    /**
     * Determine whether the session is open or not.
     * @return An indication whether the websocket is connected or not
     */
    public boolean isRunning() {
        return mIsRunning;
    }

    /**
     * Send PING frame
     */
    public void sendPing() {

        if (isRunning() && getSession() != null) {
            ByteBuffer buffer = ByteBuffer.allocate(1);
            buffer.put((byte) 0xFF);

            try {
                getSession().getBasicRemote().sendPing(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
