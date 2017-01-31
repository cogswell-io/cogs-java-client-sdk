package com.gambit.sdk.pubsub;

import javax.websocket.*;
import java.util.*;

/**
 * This class used to configure the programmatic endpoint (PubSubSocket) during
 * the initial handshake. It is used to modify the headers for authentication.
 */
public class PubSubSocketConfigurator extends ClientEndpointConfig.Configurator
{
    /**
     * The encoded payload for connecting to Pub/Sub 
     */
    private String payload;

    /**
     * The HMAC for connect to Pub/Sub
     */
    private String payloadHmac;

    /**
     * Create the {@link PubSubSocketConfigurator} given a list project keys
     * @param projectKeys The keys used for constructing the payload
     */
    public PubSubSocketConfigurator(List<String> projectKeys) {
        try {
            PubSubAuth auth = new PubSubAuth(projectKeys);
            payload = auth.getPayload();
            payloadHmac = auth.getHmac();
        }
        catch(Exception e) {
            System.out.println(":: COULD NOT CONVERT THINGS: " + e.getMessage());
            e.printStackTrace();
        } 
    }

    /**
     * Add the appropriate headers for connection to Pub/Sub. The method is
     * called before initial handshake when connecting to associated websocket. 
     * @param headers The HTML request headers to be modified
     */
    @Override
    public void beforeRequest(Map<String, List<String>> headers) {
        //headers.clear();
        //headers.put("Host", Collections.singletonList("gamqa-api.aviatainc.com"));
        headers.put("Payload", Collections.singletonList(payload));
        headers.put("PayloadHMAC", Collections.singletonList(payloadHmac));
    }
}