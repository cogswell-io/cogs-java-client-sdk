package com.gambit.sdk.pubsub;

import javax.xml.bind.DatatypeConverter;

import org.json.JSONObject;

import java.util.stream.*;
import java.util.*;

import java.time.*;
import java.time.format.*;

import java.security.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.*;
import java.io.*;

/**
 * Used to build the payload sent when trying to establish a websocket connection with Cogswell Pub/Sub
 */
public class PubSubAuth
{
    /**
     * The encoded payload.
     */
    private String payload;

    /**
     * The HMAC for the payload
     */
    private String payloadHmac;

    /**
     * Build payload using the given project keys. No session will be restored.
     *
     * @param keys The permission keys for the permissions requested for the connection
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     * @throws InvalidKeyException
     */
    protected PubSubAuth(List<String> keys)
        throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException  
    {
        this(keys, null);
    }

    /**
     * Build payload using the given keys that also requests the given session be restored.
     *
     * @param keys    The permission keys for the permissions requested for the connection.
     * @param session The uuid of the session that is being requested.
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     * @throws InvalidKeyException
     */
    protected PubSubAuth(List<String> keys, UUID session)
        throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException 
    {
        if(0 < keys.size() && keys.size() <= 3) {
            generatePayload(keys, session);
        }
        else {
            throw new IllegalArgumentException("There must be at least one key in the keys list.");
        }
    }

    /**
     * Gets the encoded payload created by this PubSubAuth.
     *
     * @return String
     */
    protected String getPayload() { 
        return payload; 
    }
    
    /**
     * Get the encoded payload hamc created by this PubSubAuth.
     * 
     * @return String
     */
    protected String getHmac() {
        return payloadHmac; 
    }

    /**
     * Generates an encoded payload using the provided project keys and session uuid.
     *
     * @param keys    The keys used to generate the payload with requested permissions
     * @param session The UUID of the session to restore, if there is one (null if there is not)
     */
    private void generatePayload(List<String> keys, UUID session)
        throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException
    {
        List<String[]> splitKeys = keys.stream()
            .map((k) -> { return k.split("-"); })
            .collect(Collectors.toList());

        String permissions = keys.stream()
            .map((k) -> { return k.substring(0, 1); })
            .collect(Collectors.joining());

        String identity = splitKeys.get(0)[1];

        String timestamp = ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT);

        JSONObject payload = new JSONObject()
            .put("identity", identity)
            .put("permissions", permissions)
            .put("security_timestamp", timestamp);

        if(session != null) {
            payload.put("session_uuid", session.toString());
        }

        byte[] utf8Payload = payload.toString().getBytes("UTF-8");
        String base64Payload = DatatypeConverter.printBase64Binary(utf8Payload);

        generateHmac(keys, utf8Payload);

        this.payload = base64Payload;
    }

    /**
     * Generates appropriate hmac using key identity as secret and payload as content.
     *
     * @param keys        The keys used in creating the request
     * @param utf8Payload The UTF-8 encoded payload used for the request
     */
    private void generateHmac(List<String> keys, byte[] utf8Payload)
        throws NoSuchAlgorithmException, InvalidKeyException //,  UnsupportedEncodingException 
    {
        List<ByteBuffer> permIdents = new Vector<>();

        // Coding Choice: Using for-each loop, instead of streams prevents issues with checked Exceptions in lambdas
        for(String key : keys) {
            String[] arr = key.split("-");
            String ident = arr[arr.length - 1];

            byte[]  hex = DatatypeConverter.parseHexBinary(ident);

            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            sha256Hmac.init(new SecretKeySpec(hex, "HmacSHA256"));
            permIdents.add(ByteBuffer.wrap(sha256Hmac.doFinal(utf8Payload)));
        }

        byte[] accum = permIdents.get(0).array();

        // Only try to accumulate the second key set if it exists
        if(permIdents.size() > 1) {
            accum = byteXor(accum, permIdents.get(1).array());
        }

        // Only try to accumulate the third key set if it exists.
        if(permIdents.size() > 2) {
            accum = byteXor(accum, permIdents.get(2).array());
        }

        this.payloadHmac = DatatypeConverter.printHexBinary(accum);
        this.payloadHmac = this.payloadHmac.toLowerCase();
    }

    /**
     * Utility function to Bitwise XOR a string of bytes. 
     * It is assumed the arrays {@code a} and {@code b} are the same length.
     *
     * @param a       The array of bytes to use as the left operand
     * @param b       The array of bytes to use as the right operand
     * @return byte[] The result of ({@code a} XOR {@code b}) as if they were one long bitstring.
     */
    private byte[] byteXor(byte[] a, byte[] b) {
        byte[] result = new byte[a.length];

        for(int i = 0; i < result.length; ++i) {
            result[i] = (byte)(a[i] ^ b[i]);
        }

        return result;
    }
}