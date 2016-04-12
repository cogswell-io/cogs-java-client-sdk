package com.gambit.sdk;

import com.gambit.sdk.request.GambitRequestEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The main class that all SDK users will use to work with the Gambit SDK.
 */
public class GambitSDKService {

    /**
     * Singleton instance
     */
    protected static GambitSDKService mInstance;

    /**
     * Thread loop
     */
    protected final ExecutorService mExecutor;

    /**
     * Gambit Push Services
     */
    protected HashMap<GambitPushService.Builder, GambitPushService> mPushInstances;

    /**
     * Gambit Push Message Listener
     */
    protected GambitPushService.GambitMessageListener mMessageListener;

    /**
     * Gambit API Endpoint Hostname
     */
    protected String mEndpointHostname = "api.cogswell.io";

    /**
     * Singleton constructor
     */
    protected GambitSDKService() throws RuntimeException {
        mExecutor = Executors.newCachedThreadPool();

        mPushInstances = new HashMap<>();
    }

    /**
     * Creates a {@link GambitSDKService} if none previously existed in the VM,
     * otherwise returns the existing {@link GambitSDKService} instance.
     * @return GambitSDKService
     */
    public static GambitSDKService getInstance() throws RuntimeException {
        if (mInstance == null) {
            mInstance = new GambitSDKService();
        }

        return mInstance;
    }

    /**
     * Finalize {@link GambitSDKService}  by shutting down all threads.
     */
    public void finish() {

        if (getExecutorService() != null && !getExecutorService().isShutdown()) {
            getExecutorService().shutdown();
        }

        if (mPushInstances != null && !mPushInstances.isEmpty()) {
            Iterator<GambitPushService.Builder> i = mPushInstances.keySet().iterator();

            while (i.hasNext()) {
                GambitPushService service = mPushInstances.get(i.next());

                service.shutdown();
            }
        }
    }

    /**
     * Get the main thread pool
     * @return executor service
     */
    protected ExecutorService getExecutorService() {
        return mExecutor;
    }

    /**
     * Set Gambit API Endpoint Hostname
     * @param hostname The endpoint hostname. Protocol is always secure.
     */
    public void setEndpointUrl(String hostname) {
        mEndpointHostname = hostname;
    }

    /**
     * Get Gambit API Endpoint Hostname
     * @return Gambit API Endpoint Hostname
     */
    public String getEndpointHostname() {
        return mEndpointHostname;
    }

    /**
     * Send Gambit Event data
     * @param builder Builder that configures the {@link GambitRequest} inheriting object
     * @return Promised object that inherits {@link GambitResponse}
     * @throws java.lang.Exception 
     */
    public Future<GambitResponse> sendGambitEvent(GambitRequestEvent.Builder builder) throws Exception {
        return mExecutor.submit(builder.build());
    }

    /**
     * Attach a {@link com.gambit.sdk.GambitPushService.GambitMessageListener} to all existing push messages instances.
     * @param listener The listener object that is going to receive all {@link com.gambit.sdk.message.GambitMessage}s
     */
    public void setGambitMessageListener(GambitPushService.GambitMessageListener listener) {
        mMessageListener = listener;

        applyGambitMessageListener();
    }

    /**
     * Make sure the {@link com.gambit.sdk.GambitPushService.GambitMessageListener} is attached to all existing
     * {@link GambitPushService} instances.
     */
    protected void applyGambitMessageListener() {
        if (mPushInstances != null && !mPushInstances.isEmpty()) {
            Iterator<GambitPushService.Builder> i = mPushInstances.keySet().iterator();

            while (i.hasNext()) {
                GambitPushService service = mPushInstances.get(i.next());

                service.setListener(mMessageListener);
            }
        }
    }

    /**
     * Start a new /push websocket instance. To identify the message source, the {@link com.gambit.sdk.GambitPushService.Builder}
     * instance is being passed along with the {@link com.gambit.sdk.message.GambitMessage} to the {@link com.gambit.sdk.GambitPushService.GambitMessageListener}
     * @param builder The {@link com.gambit.sdk.GambitPushService.Builder} object
     * @throws Exception When we fail to create a new instance
     */
    public GambitPushService startPushService(GambitPushService.Builder builder) throws Exception {
        GambitPushService service = builder.build();
        service.setListener(mMessageListener);
        mPushInstances.put(builder, service);
        return service;
    }

}
