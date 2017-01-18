package com.gambit.sdk.pubsub;

import javax.websocket.*;
import java.util.*;

public class PubSubSocket extends Endpoint implements MessageHandler.Whole<String>
{
    public PubSubSocket() {
        System.out.println(":: PROG SOCKET CONFIG CONSTRUCTION");
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        session.addMessageHandler(this);
    }

    @Override
    public void onMessage(String message) {

    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {

    }

    @Override
    public void onError(Session session, Throwable throwable) {

    }
}