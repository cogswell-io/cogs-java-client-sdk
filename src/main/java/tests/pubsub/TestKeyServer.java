package tests.pubsub;

import java.util.concurrent.*;
import java.util.*;
import java.io.*;
import java.net.*;

import com.gambit.sdk.*;

import org.json.JSONObject;

public class TestKeyServer
{
    private String serverLoc;

    public TestKeyServer(String url) {
        serverLoc = url;
    }

    public CompletableFuture<GambitResponse> createKey(JSONObject key) {
        CompletableFuture promise = CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(serverLoc + "/project_key");
                String message = key.toString();
                return send("POST", url, message);
            }
            catch(Exception e) {
                throw new CompletionException(e);
            }
        });

        return promise;
    }

    public CompletableFuture<GambitResponse> deleteKey(String identity) {
        CompletableFuture<GambitResponse> promise 
            = CompletableFuture.supplyAsync(() -> {
                try {
                    URL url = new URL(serverLoc + "/project_key/" + identity);
                    return send("DELETE", url, null);
                }
                catch(Exception e) {
                    throw new CompletionException(e);
                }
            });

        return promise;
    }

    private GambitResponse send(String method, URL url, String message)
        throws IOException, ProtocolException, UnsupportedEncodingException 
    {
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestMethod(method);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        if(method.equals("POST")) {
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
            writer.write(message);
            writer.close();
        }

        int code = connection.getResponseCode();

        BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuffer jsonString = new StringBuffer();
        String line;

        while((line = input.readLine()) != null) {
            jsonString.append(line);
        }

        input.close();
        connection.disconnect();

        return new GambitResponse(jsonString.toString(), code);
    }
}