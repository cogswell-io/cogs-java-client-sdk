package com.gambit.sdk.pubsub;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import java.util.Collections;
import java.util.Vector;
import java.util.List;

import java.util.Random;

import com.gambit.sdk.pubsub.handlers.PubSubMessageHandler;

import org.json.JSONObject;

import static org.junit.Assert.*;
import org.junit.Test;

public class PubSubIntegrationTests {

    private static JSONObject getRandomProjectKey() {
        Random rand = new Random();

        String ident = Integer.toHexString(rand.nextInt());
        String read = Integer.toHexString(rand.nextInt());
        String write = Integer.toHexString(rand.nextInt());
        String admin = Integer.toHexString(rand.nextInt());

        if(ident.length() % 2 != 0) { ident += "f"; }
        if(read.length() % 2 != 0) { read += "f"; }
        if(write.length() % 2 != 0) { write += "f"; }
        if(admin.length() % 2 != 0) { admin += "f"; }

        JSONObject key = new JSONObject()
                .put("identity", ident)
                .put("read_key", read)
                .put("write_key", write)
                .put("admin_key", admin)
                .put("key_id", 1)
                .put("project_id", 1)
                .put("customer_id", 1)
                .put("enabled", true);

        return key;
    }

    private static List<String> buildPermissionKeys(JSONObject key) {
        List<String> permissions = new Vector<>();

        String ident = key.getString("identity");
        String read = key.getString("read_key");
        String write = key.getString("write_key");
        String admin = key.getString("admin_key");

        permissions.add(String.format("R-%s-%s", ident, read));
        permissions.add(String.format("W-%s-%s", ident, write));
        permissions.add(String.format("A-%s-%s", ident, admin));

        return permissions;
    }

    private static PubSubHandle pubsubHandle;
    private static String errorMessage;
    private static boolean isError;

    @Test
    public void testSubscribeAndUnsubscribe() {
        KeyServer keyServer = new KeyServer("http://localhost:8778");
        PubSubSDK pubsubSDK = PubSubSDK.getInstance();

        JSONObject keys = getRandomProjectKey();
        String identity = keys.getString("identity");
        List<String> permissions = buildPermissionKeys(keys);

        CountDownLatch signal = new CountDownLatch(1);
        String channel = "BOOKS & MOVIES";

        keyServer.createKey(keys)
            .thenComposeAsync((result) -> {
                return pubsubSDK.connect(permissions);
            })
            .thenComposeAsync((handle) -> {
                pubsubHandle = handle;

                return pubsubHandle.subscribe(channel, (record) -> {
                    fail("There should be no records received in this test.");
                });
            })
            .thenComposeAsync((subscriptions) -> {
                assertEquals(
                    "There should be only one subscription: That to " + channel,
                    Collections.singletonList(channel),
                    subscriptions
                );

                return pubsubHandle.unsubscribe(channel);
            })
            .thenComposeAsync((subscriptions) -> {
                assertTrue(
                    "The list of subscriptions should be empty now...",
                    ((List<String>)subscriptions).isEmpty()
                );

                return keyServer.deleteKey(identity);
            })
            .thenAcceptAsync((result) -> {
                signal.countDown();
            })
            .exceptionally((error) -> {
                errorMessage = error.getMessage();
                isError = true;
                signal.countDown();
                
                return null;
            });

        try {
            signal.await();

            if(isError) {
                fail("There was an exception: " + errorMessage);
            }
        }
        catch(InterruptedException e) {
            fail("The test was unable to finish properly.");
        }
    }

    @Test
    public void testReceivingMessageForSubscription() {
        KeyServer keyServer = new KeyServer("http://localhost:8778");
        PubSubSDK pubsub = PubSubSDK.getInstance();

        JSONObject key = getRandomProjectKey();
        String ident = key.getString("identity");
        List<String> permissionKeys = buildPermissionKeys(key);

        CountDownLatch signal = new CountDownLatch(1);
        String testChan = "BOOKS & MOVIES";
        String testMsg = "Out Now: The Thriller of the Century. Find it near you!";

        keyServer.createKey(key)
            .thenComposeAsync((result) -> {
                return pubsub.connect(permissionKeys);
            })
            .thenComposeAsync((handle) -> {
                pubsubHandle = handle;
                
                return pubsubHandle.subscribe(testChan, (record) -> {
                    assertEquals(
                        "The channel published should be the one received.",
                        testChan, 
                        record.getChannel()
                    );

                    assertEquals(
                        "The message content received is that which was published.",
                        testMsg,
                        record.getMessage()
                    );

                    signal.countDown();
                });
            })
            .thenComposeAsync((sequence) -> {
                return pubsubHandle.publish(testChan, testMsg);
            })
            .exceptionally((error) -> {
                return null;
            });

        try {
            signal.await();
        }
        catch(InterruptedException e) {
            fail("INTERRUPTED WHILE WAITING FOR COUNTDOWN");
        }

        pubsubHandle.close()
            .thenComposeAsync((channels) -> {
                return keyServer.deleteKey(ident);
            })
            .exceptionally((error) -> {
                return null;
            });
    }
}