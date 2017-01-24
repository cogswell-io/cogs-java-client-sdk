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
 * This class is used to build the payload to be sent to the Pub/Sub server when initaitng a connection
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
     * Construct the payload information with the given keys. No session will be restored.
     * @param keys The keys of permission to be requested when initiating the connection
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingExceptio
     * @throws InvalidKeyException
     */
    public PubSubAuth(List<String> keys)
        throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException  
    {
        this(keys, null);
    }

    /**
     * Construct the payload information with the given keys and given session to be restored.
     * @param keys The keys of permission to be requested when initiating the connection
     * @param session The UUID of the session that should be restored
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingExceptio
     * @throws InvalidKeyException
     */
    public PubSubAuth(List<String> keys, UUID session)
        throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException 
    {
        generatePayload(keys, session);
    }

    /**
     * Get the encoded payload that was created by this PubSubAuth instance.
     * @return String
     */
    public String getPayload() { 
        return payload; 
    }
    
    /**
     * Get the encoded hmac for the payload that was created by this PubSubAuth instance
     * @return String
     */
    public String getHmac() {
        return payloadHmac; 
    }

    /**
     * Generate an encoded payload with the given keys and session
     * @param keys The keys used to generate the payload with requested permissions
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
            payload.put("uuid", session.toString());
        }

        byte[] utf8Payload = payload.toString().getBytes("UTF-8");
        String base64Payload = DatatypeConverter.printBase64Binary(utf8Payload);

        generateHmac(keys, utf8Payload);

        this.payload = base64Payload;
    }

    /**
     * Generate an appropriate hmac using the key identity as the secret and the payload as the content
     * @param keys The keys used in creating the request
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
        accum = byteXor(accum, permIdents.get(1).array());
        accum = byteXor(accum, permIdents.get(2).array());

        this.payloadHmac = DatatypeConverter.printHexBinary(accum);
        this.payloadHmac = this.payloadHmac.toLowerCase();
    }

    /**
     * Utility function to Bitwise XOR a string of bytes. 
     * It is assumed the arrays a and b are the same length.
     * @param a The array of bytes to use as the left operand
     * @param b The array of bytes to use as the right operand
     * @return byte[] The result of (a XOR b) as if they were one long bitstring.
     */
    private byte[] byteXor(byte[] a, byte[] b) {
        byte[] result = new byte[a.length];

        for(int i = 0; i < result.length; ++i) {
            result[i] = (byte)(a[i] ^ b[i]);
        }

        return result;
    }
}