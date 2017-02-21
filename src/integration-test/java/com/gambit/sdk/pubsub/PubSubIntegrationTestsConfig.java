package com.gambit.sdk.pubsub;

import java.io.InputStream;
import java.io.IOException;

import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

public class PubSubIntegrationTestsConfig {
    
    private static PubSubIntegrationTestsConfig instance;

    private String host;
    private List<String> mainKeys;
    private List<String> secondaryKeys;

    public static PubSubIntegrationTestsConfig getInstance() {
        if(instance == null) {
            instance = new PubSubIntegrationTestsConfig();
        }

        return instance;
    }

    private PubSubIntegrationTestsConfig() {
        try {
            InputStream configInputStream = getClass().getResourceAsStream("config.json");

            // Not filling in configInputStream
            String configString = new Scanner(configInputStream, "UTF-8").useDelimiter("\\A").next();

            JSONObject configJSON = new JSONObject(configString);

            if(configJSON.has("host")) {
                host = configJSON.getString("host");
            }
            else {
                host = "wss://api.cogswell.io/pubsub";
            }
            
            JSONObject main = null; 
            JSONObject secondary = null;

            if(configJSON.has("mainKeys")) {
                mainKeys = new ArrayList<>();
                main = configJSON.getJSONObject("mainKeys");
                buildKeys(mainKeys, main);
            }

            if(configJSON.has("secondaryKeys")) {
                secondaryKeys = new ArrayList<>();
                secondary = configJSON.getJSONObject("secondaryKeys");
                buildKeys(secondaryKeys, secondary);
            }
        }
        catch(JSONException e) {
            System.err.println("Error converting JSON config: " + e.getMessage());
        }
        catch(Exception e) {
            System.err.println("Error reading JSON config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void buildKeys(List<String> keys, JSONObject keyObject) throws JSONException {
        if(keyObject.has("readKey")) {
            keys.add(keyObject.getString("readKey"));
        }

        if(keyObject.has("writeKey")) {
            keys.add(keyObject.getString("writeKey"));
        }

        if(keyObject.has("adminKey")) {
            keys.add(keyObject.getString("adminKey"));
        }
    }

    public String getHost() {
        return host;
    }

    public List<String> getMainKeys() {
        return mainKeys;
    }

    public List<String> getSecondaryKeys() {
        return secondaryKeys;
    }
}