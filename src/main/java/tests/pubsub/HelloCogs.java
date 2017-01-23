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
    public static PubSubHandle handle = null;

    public static void main(String args[]) {
        ////////////////////////////////////////////////////////

        String read = "dead";
        String write = "beef";
        String admin = "deadbeef";
        String ident = "600D";

        JSONObject key = new JSONObject()
            .put("identity", ident)
            .put("read_key", read)
            .put("write_key", write)
            .put("admin_key", admin)
            .put("key_id", 1)
            .put("project_id", 1235)
            .put("customer_id", 1)
            .put("enabled", true);//*/

        //JSONObject key = getRandomProjectKey();

        List<String> permissionKeys = buildPermissionKeys(key);

        ///////////////////////////////////////////////////////

        UUID session = UUID.fromString("8e404b70-dd96-11e6-aa81-194db86f61d4");

        TestKeyServer keyServer = new TestKeyServer("http://localhost:8778");
        //PubSubOptions opts = new PubSubOptions("ws://localhost:8888", false, 30000, session);
        PubSubSDK pubsub = PubSubSDK.getInstance();

        String chan = "A-GOOD-CHANNEL";

        CompletableFuture<String> end = new CompletableFuture<>();

        keyServer.createKey(key)
            .thenComposeAsync((result) -> {
                return pubsub.connect(permissionKeys);
            })
            .thenComposeAsync((h) -> {
                handle = h;

                return handle.subscribe(chan, new PubSubMessageHandler() {
                    @Override
                    public void onMessage(PubSubMessageRecord record) {
                        System.out.println("This was received from the handler...");
                        System.out.println(record.getMessage());
                    }
                });
            })
            .thenComposeAsync((chans) -> {
                System.out.println("CHANNELS: " + chans);
                return keyServer.deleteKey(ident);
            })
            .thenComposeAsync((response) -> {
                end.complete("DONE");
                return end;
            })
            .exceptionally((err) -> {
                System.out.println("THERE WAS SOME KIND OF EXCEPTION");
                System.out.println("Type: " + err.getMessage());
                return null;
            });

        try {
            ForkJoinPool.commonPool().awaitTermination(2, TimeUnit.MINUTES);
            Thread.sleep(2000);
        }
        catch(InterruptedException e) {
            System.out.println("INTERRUPTED WHILE WAITING");
        }//*/
    }

    private static JSONObject getRandomProjectKey() {
        Random rand = new Random();

        String ident = Integer.toHexString(rand.nextInt());
        String read = Integer.toHexString(rand.nextInt());
        String write = Integer.toHexString(rand.nextInt());
        String admin = Integer.toHexString(rand.nextInt());

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