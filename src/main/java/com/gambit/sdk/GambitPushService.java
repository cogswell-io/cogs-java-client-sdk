package com.gambit.sdk;

import com.gambit.sdk.message.GambitMessage;
import org.json.JSONObject;

import javax.websocket.CloseReason;
import javax.websocket.MessageHandler;
import javax.websocket.PongMessage;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.TimeZone;

public class GambitPushService {

    public static interface GambitMessageListener {
        /**
         * Gambit push message received
         * @param builder To identify the push service instance
         * @param message The actual message received
         */
        public void onGambitMessage(Builder builder, GambitMessage message);
    }

    public static class Builder {

        /**
         * Obtained through GambitToolsSDK
         */
        protected final String mClientSalt;

        /**
         * Obtained through GambitToolsSDK
         */
        protected final String mClientSecret;

        /**
         * Obtained through Gambit UI
         */
        protected final String mAccessKey;

        /**
         * Object containing namepace-specific fields. The names and types of
         * the supplied attributes must match those defined either within the
         * namespace, or defined as core attributes on the customer's account.
         */
        protected LinkedHashMap<String, Object> mAttributes;

        /**
         * The namespace for with which this request is associated. The
         * attributes must either be defined for the specified namespace, or
         * they must be core attributes defined by the customer owning this
         * namespace.
         */
        protected String mNamespace;

        /**
         * The topic description with which this push service (WebSocket) is associated.
         */
        protected String mTopicDescription;

        /**
         * Create push service builder with keys obtained through Gambit UI and
         * Gambit Tools SDK
         *
         * @param access_key The access key obtained from Gambit UI
         * @param client_salt The client salt hash obtained from GambitToolsSDK
         * @param client_secret The client secret hash obtained from GambitToolsSDK
         */
        public Builder(String access_key, String client_salt, String client_secret, String topicDescription) {
            mAccessKey = access_key;
            mClientSalt = client_salt;
            mClientSecret = client_secret;
            mTopicDescription = topicDescription;
        }

        /**
         * The namespace for with which this event is associated. The
         * attributes must either be defined for the specified namespace, or
         * they must be core attributes defined by the customer owning this
         * namespace.
         *
         * @param namespace The namespace for with which this request is associated.
         * @return The same instance
         */
        public Builder setNamespace(String namespace) {
            this.mNamespace = namespace;

            return this;
        }

        /**
         * The namespace for with which this request is associated. The
         * attributes must either be defined for the specified namespace, or
         * they must be core attributes defined by the customer owning this
         * namespace.
         *
         * @return The namespace for with which this request is associated.
         */
        public String getNamespace() {
            return mNamespace;
        }

        /**
         * Object containing namepace-specific fields. The names and types of
         * the supplied attributes must match those defined either within the
         * namespace, or defined as core attributes on the customer's account.
         *
         * @param attributes Obtained through Gambit UI or the Java Example App
         * @return The same instance
         */
        public Builder setAttributes(LinkedHashMap<String, Object> attributes) {
            this.mAttributes = attributes;

            return this;
        }

        /**
         * Object containing namepace-specific fields. The names and types of
         * the supplied attributes must match those defined either within the
         * namespace, or defined as core attributes on the customer's account.
         *
         * @return The namespace attributes added to this Builder instance
         */
        public LinkedHashMap<String, Object> getAttributes() {
            return mAttributes;
        }

        /**
         * Obtained through Gambit UI (public key)
         *
         * @return The access key obtained from Gambit UI
         */
        public String getAccessKey() {
            return mAccessKey;
        }

        /**
         * Obtained through Gambit Tools SDK
         *
         * @return The client salt hash
         */
        public String getClientSalt() {
            return mClientSalt;
        }

        /**
         * Obtained through Gambit Tools SDK
         *
         * @return The client secret hash
         */
        public String getClientSecret() {
            return mClientSecret;
        }

        /**
         * Supplies the topic description.
         *
         * @return the topic description
         */
        public String getTopicDescription() {
            return mTopicDescription;
        }

        /**
         * Build request object
         *
         * @return A push service instance
         * @throws java.lang.Exception if validation fails
         */
        public GambitPushService build() throws Exception {
            validate();

            return new GambitPushService(this);
        }

        /**
         * Validate the builder integrity before proceeding with object creation
         * @throws Exception If anything crucial is missing
         */
        protected void validate() throws Exception {
            if (mAttributes == null || mAttributes.isEmpty()) {
                throw new Exception("Missing mandatory parameter of Builder: attributes");
            }

            if (mNamespace == null || mNamespace.isEmpty()) {
                throw new Exception("Missing mandatory parameter of Builder: namespace");
            }
        }
    }

    /**
     * Save the builder instance for later. We are going to send it to the {@link GambitMessageListener} to identify
     * this push service instance among the other potentially existing instances.
     */
    protected final Builder mBuilder;

    /**
     * The payload used to start the push service
     */
    protected String mPayload;

    /**
     * Obtained through GambitToolsSDK
     */
    protected String mSignature;

    /**
     * User land callback
     */
    protected GambitMessageListener mListener;

    /**
     * Websocket text message handler. Propagate all incoming messages to the attached listener and shove back a quick
     * ACK package. ACK packages are being sent only when the message is valid and there's a {@link GambitMessageListener}
     * attached. Otherwise we assume that the user will not be aware of this event and we give the server a second chance
     * to send it back later. (Although the server probably won't do that..)
     */
    protected MessageHandler.Whole<String> mMessageHandler = new MessageHandler.Whole<String>() {
        @Override
        public void onMessage(String message) {

            GambitMessage messageObject = new GambitMessage(message);

            if (mListener != null) {

                if (messageObject.isValid() && messageObject.getMessageId() != null) {
                    //send ACK only when the message is well formed, and the user land listener is attached

                    String ackPacket = getAckPacketForMessage(messageObject);

                    mEndpoint.getSession().getAsyncRemote().sendText(ackPacket);
                }

                mListener.onGambitMessage(mBuilder, messageObject);
            }
        }
    };

    /**
     * Handle PING/PONG transmissions if necessary. Useful for debugging.
     */
    protected MessageHandler.Whole<PongMessage> mPingPongHandler = new MessageHandler.Whole<PongMessage>() {
        @Override
        public void onMessage(PongMessage message) {
            //System.out.println("PONG received:" +message.toString());
            //for debugging and logging purposes
        }
    };

    /**
     * The websocket endpoint
     */
    protected GambitWebsocketEndpoint mEndpoint;

    /**
     * Construct the push service using it's own {@link Builder} object
     * @param builder The {@link Builder} object
     * @throws IOException When the API endpoint hostname is not configured.
     */
    public GambitPushService(Builder builder) throws IOException {

        mBuilder = builder;

        JSONObject payload = new JSONObject();

        payload.put("access_key", builder.getAccessKey());
        payload.put("client_salt", builder.getClientSalt());

        if (builder.getNamespace() != null) {
            payload.put("namespace", builder.getNamespace());
        }

        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
        df.setTimeZone(tz);
        String timestamp = df.format(new Date());

        payload.put("timestamp", timestamp);

        if (builder.getAttributes() != null) {
            payload.put("attributes", builder.getAttributes());
        }

        String payloadData = payload.toString();

        try {
            mSignature = GambitRequest.getHmac(payloadData, builder.getClientSecret());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mPayload = DatatypeConverter.printBase64Binary(payloadData.getBytes());

        start();
    }

    /**
     * Set callback listener
     * @param listener The {@link GambitMessageListener} to attach
     */
    public void setListener(GambitMessageListener listener) {
        mListener = listener;
    }

    /**
     * Build ACK package
     * @param messageObject The message that we have just received
     * @return The payload of the ACK package
     */
    protected String getAckPacketForMessage(GambitMessage messageObject) {
        JSONObject payload = new JSONObject();

        payload.put("event", "message-received");
        payload.put("message_id", messageObject.getMessageId());

        return payload.toString();
    }

    /**
     * Initializes the websocket service
     * @throws IOException When the endpoint hostname isn't configured
     */
    protected void start() throws IOException {

        String hostname = GambitSDKService.getInstance().getEndpointHostname();

        if (hostname == null) {
            throw new IOException("Invalid API endpoint.");
        }

        StringBuilder builder = new StringBuilder();
        builder.append("wss://"); //protocol
        builder.append(hostname);
        builder.append(":443"); //port
        builder.append("/push"); //websocket endpoint

        mEndpoint = new GambitWebsocketEndpoint(URI.create(builder.toString()), mPayload, mSignature, mMessageHandler, mPingPongHandler);
    }

    /**
     * Shutdown push service
     */
    public void shutdown() {
        try {
            if (mEndpoint.isRunning()) {
                mEndpoint.getSession().close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "shutting down"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
