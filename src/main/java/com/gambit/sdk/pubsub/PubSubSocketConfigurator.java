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
     * Creates the {@link PubSubSocketConfigurator} given the list project keys for authentication
     *
     * @param projectKeys Project keys used to construct the payload for authentication
     */
    protected PubSubSocketConfigurator(List<String> projectKeys, UUID session) {
        try {
            PubSubAuth auth = new PubSubAuth(projectKeys, session);
            payload = auth.getPayload();
            payloadHmac = auth.getHmac();
        }
        catch(Exception e) {
            System.out.println(":: COULD NOT CONVERT THINGS: " + e.getMessage());
            e.printStackTrace();
        } 
    }

    /**
     * Adds the appropriate headers for connecting to Cogswell Pub/Sub.
     * The method is called before initial handshake when connecting to associated websocket.
     * 
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