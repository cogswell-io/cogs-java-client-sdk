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

public class PubSubAuth
{
    public String payload;
    public String payloadHmac;

    public PubSubAuth(List<String> keys)
        throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException  
    {
        this(keys, null);
    }

    public PubSubAuth(List<String> keys, UUID session)
        throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException 
    {
        generatePayload(keys, session);
    }

    public String getPayload() { return payload; }
    public String getHmac() { return payloadHmac; }

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

    private void generateHmac(List<String> keys, byte[] utf8Payload)
        throws NoSuchAlgorithmException, InvalidKeyException //,  UnsupportedEncodingException 
    {
        List<ByteBuffer> permIdents = new Vector<>();

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

    private byte[] byteXor(byte[] a, byte[] b) {
        byte[] result = new byte[a.length];

        for(int i = 0; i < result.length; ++i) {
            result[i] = (byte)(a[i] ^ b[i]);
        }

        return result;
    }
}