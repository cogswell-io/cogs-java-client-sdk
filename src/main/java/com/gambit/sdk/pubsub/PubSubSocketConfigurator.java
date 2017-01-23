package com.gambit.sdk.pubsub;

import javax.websocket.*;
import java.util.*;

public class PubSubSocketConfigurator extends ClientEndpointConfig.Configurator
{
    private String payload;
    private String payloadHmac;

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

    @Override
    public void beforeRequest(Map<String, List<String>> headers) {
        headers.put("Payload", Collections.singletonList(payload));
        headers.put("PayloadHMAC", Collections.singletonList(payloadHmac));
    }
}