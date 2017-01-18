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

import com.gambit.sdk.pubsub.*;
import javax.websocket.*;

class HelloCogs {
    public static void main(String args[]) {
        ////////////////////////////////////////////////////////

        String read = "dead";
        String write = "beef";
        String admin = "deadbeef";
        String ident = "600D";

        List<String> permissionKeys = new Vector<>();
        permissionKeys.add(String.format("R-%s-%s", ident, read));
        permissionKeys.add(String.format("W-%s-%s", ident, write));
        permissionKeys.add(String.format("A-%s-%s", ident, admin));

        JSONObject key = new JSONObject()
            .put("identity", ident)
            .put("read_key", read)
            .put("write_key", write)
            .put("admin_key", admin)
            .put("key_id", 1)
            .put("project_id", 1235)
            .put("customer_id", 1)
            .put("enabled", true);

        ///////////////////////////////////////////////////////

        TestKeyServer keyServer = new TestKeyServer("http://localhost:8778");
        PubSubSDK pubsub = PubSubSDK.getInstance();

        keyServer.createKey(key)
            .thenComposeAsync((result) -> {
                System.out.println("CREATED A KEY");
                System.out.println(result.getRawBody());
                return pubsub.connect(permissionKeys);
            })
            .thenComposeAsync((result) -> {
                System.out.println("CONNECTED TO PUB/SUB");
                return keyServer.deleteKey(ident);
            })
            .thenComposeAsync((result) -> {
                System.out.println("DELETED A KEY");
                return CompletableFuture.completedFuture(result); 
            })
            .exceptionally((err) -> {
                return null;
            });
        
        System.out.println("THERE IS ASYNCHRONOUS CODE IN THIS RUNNING PROGRAM...");
        ForkJoinPool.commonPool().awaitQuiescence(2, TimeUnit.MINUTES);
    }

    private static JSONObject getRandomProjectKey() {
        Random rand = new Random();

        String ident = Integer.toHexString(rand.nextInt());
        String read = Integer.toHexString(rand.nextInt());
        String write = Integer.toHexString(rand.nextInt());
        String admin = Integer.toHexString(rand.nextInt());

        JSONObject key = new JSONObject()
                .put("key_id", ident)
                .put("read_key", read)
                .put("write_key", write)
                .put("admin_key", admin)
                .put("project_id", ident)
                .put("customer_id", 1)
                .put("enabled", true);

        return key;
    }

    private static List<String> buildPermissionKeys(JSONObject key) {
        List<String> permissions = new Vector<>();

        String ident = key.getString("key_id");
        String read = key.getString("read_key");
        String write = key.getString("write_key");
        String admin = key.getString("admin_key");

        permissions.add(String.format("R-%s-%s", ident, read));
        permissions.add(String.format("W-%s-%s", ident, write));
        permissions.add(String.format("A-%s-%s", ident, admin));

        return permissions;
    }
}