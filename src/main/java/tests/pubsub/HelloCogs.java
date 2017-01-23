package tests.pubsub;

import java.util.*;
import java.text.*;
import java.util.concurrent.*;
import com.gambit.sdk.*;
import com.gambit.sdk.request.GambitRequestEvent;
import com.gambit.sdk.response.GambitResponseEvent;

import org.json.JSONObject;

import java.net.*;
import java.io.*;

import com.gambit.sdk.pubsub.handlers.*;
import com.gambit.sdk.pubsub.*;
import javax.websocket.*;

class HelloCogs {
    public static PubSubHandle pubsubHandle;

    public static void main(String args[]) {
        TestKeyServer keyServer = new TestKeyServer("http://localhost:8778");
        PubSubSDK pubsub = PubSubSDK.getInstance();

        JSONObject key = getRandomProjectKey();
        String ident = key.getString("identity");
        List<String> permissionKeys = buildPermissionKeys(key);

        CountDownLatch signal = new CountDownLatch(1);
        String testChan = "BASEBALL & SPORTS";

        keyServer.createKey(key)
            .thenComposeAsync((result) -> {
                System.out.println("AFTER CREATING KEY: " + result);
                return pubsub.connect(permissionKeys);
            })
            .thenComposeAsync((handle) -> {
                System.out.println("AFTER A CONNECTION: " + handle);
                pubsubHandle = handle;
                return pubsubHandle.subscribe(testChan, new PubSubMessageHandler() {
                    @Override
                    public void onMessage(PubSubMessageRecord record) {
                        System.out.println("ThIs WaS CaLleD: " + record.getMessage());
                    }
                });
            })
            .thenComposeAsync((chans) -> {
                System.out.println("AFTER SUBSCRIPTION: " + chans);
                pubsubHandle.publish(testChan, "Good Message");
                pubsubHandle.publish(testChan, "Some Message");
                return pubsubHandle.publish(testChan, "Good Grief");
            })
            .thenComposeAsync((sequence) -> {
                System.out.println("AFTER A PUBLISHING: " + sequence);
                return pubsubHandle.close();
            })
            .thenComposeAsync((result) -> {
                System.out.println("AFTER SOCK CLOSING: " + result);
                return keyServer.deleteKey(ident);
            })
            .thenAcceptAsync((result) -> {
                System.out.println("AFTER DELETING KEY: " + result);
                signal.countDown();
            })
            .exceptionally((error) -> {
                System.out.println("THERE WAS AN ERROR: " + error);
                return null;
            });

        try {
            signal.await();
        }
        catch(InterruptedException e) {
            System.out.println("INTERRUPTED WHILE WAITING FOR COUNTDOWN");
        }
    }

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
}