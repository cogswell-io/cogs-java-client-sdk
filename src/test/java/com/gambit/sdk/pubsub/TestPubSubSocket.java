package com.gambit.sdk.pubsub;

import javax.websocket.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import java.util.Collections;

import com.gambit.sdk.pubsub.exceptions.*;
import com.gambit.sdk.pubsub.responses.successes.PubSubResponse;

import org.json.JSONObject;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class TestPubSubSocket
{
    public static boolean isError = false;
    public static String errorMessage = "";

    @Before
    public void setupBeforeEach() {
        isError = false;
        errorMessage = "";
    }

    @Test
    public void testOnMessageSuccessResponse() {
        try {
            RemoteEndpoint.Async mockServer = mock(RemoteEndpoint.Async.class);
            PubSubSocket socket = new PubSubSocket(mockServer);
            CountDownLatch signal = new CountDownLatch(1);
            final long sequence = 1000L; 

            JSONObject expectedJson = new JSONObject()
                    .put("seq", sequence)
                    .put("action", "subscribe")
                    .put("code", 200)
                    .put("channels", Collections.singletonList("Programming!"));

            PubSubResponse expectedResponse = PubSubResponse.create(expectedJson);

            doAnswer((invocation) -> {
                socket.onMessage(expectedJson.toString());
                return null;
            }).when(mockServer).sendText(anyString(), any());

            socket.sendRequest(sequence, new JSONObject())
                .thenAcceptAsync((response) -> {
                    try {
                        assertEquals(
                            "The response received should be the fake successful response.",
                            expectedResponse.getRawJson(),
                            response.getRawJson()
                        );
                    }
                    catch(AssertionError e) {
                        isError = true;
                        errorMessage = e.getMessage();
                    }

                    signal.countDown();
                })
                .exceptionally((error) -> {
                    isError = true;
                    errorMessage = error.getCause().getMessage();
                    signal.countDown();
                    return null;
                });

            if(!signal.await(2, TimeUnit.SECONDS)) {
                fail("Timed Out");
            }

            if(isError) {
                fail(errorMessage);
            }
        }
        catch(Throwable ex) {
            fail("There was an exception thrown: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Test
    public void testOnMessageInternalErrorResponse() {
        try {
            RemoteEndpoint.Async mockServer = mock(RemoteEndpoint.Async.class);
            PubSubSocket socket = new PubSubSocket(mockServer);
            CountDownLatch signal = new CountDownLatch(0);
            final long sequence = 1000L; 

            JSONObject expectedResponse = new JSONObject()
                    .put("seq", sequence)
                    .put("action", "subscribe")
                    .put("code", 500)
                    .put("message", "Internal Error")
                    .put("details", "The description of the server misbehaving...");

            doAnswer((invocation) -> {
                socket.onMessage(expectedResponse.toString());
                return null;
            }).when(mockServer).sendText(anyString(), any());

            socket.sendRequest(sequence, new JSONObject())
                .thenAcceptAsync((response) -> {
                    isError = true;
                    errorMessage = "Not Expecting a Response: " + response.toString();
                    signal.countDown();
                })
                .exceptionally((error) -> {
                    try {
                        assertTrue(
                            "The expected error type should be an PubSubInternalError",
                            error.getCause() instanceof PubSubException
                        );
                    }
                    catch(AssertionError e) {
                        isError = true;
                        errorMessage = e.getMessage();
                    }

                    signal.countDown();
                    return null;
                });

            signal.await();

            if(isError) {
                fail(errorMessage);
            }
        }
        catch(Throwable ex) {
            fail("There was an exception thrown: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Test
    public void testOnMessageInvalidFormatResponse() {
        try {
            RemoteEndpoint.Async mockServer = mock(RemoteEndpoint.Async.class);
            PubSubSocket socket = new PubSubSocket(mockServer);
            CountDownLatch signal = new CountDownLatch(0);
            final long sequence = 1000L;

            JSONObject expectedResponse = new JSONObject()
                    .put("seq", sequence)
                    .put("action", "unsubscribe-all")
                    .put("code", 400)
                    .put("message", "Invalid Format")
                    .put("details", "The description of how the format was wrong!");

            doAnswer((invocation) -> {
                socket.onMessage(expectedResponse.toString());
                return null;
            }).when(mockServer).sendText(anyString(), any());

            socket.sendRequest(sequence, new JSONObject())
                .thenAcceptAsync((response) -> {
                    isError = true;
                    errorMessage = "Not Expecting a Response: " + response.toString();
                    signal.countDown();
                })
                .exceptionally((error) -> {
                    try {
                        assertTrue(
                            "The expected error type should be an PubSubInternalError",
                            error.getCause() instanceof PubSubException
                        );
                    }
                    catch(AssertionError e) {
                        isError = true;
                        errorMessage = e.getMessage();
                    }

                    signal.countDown();
                    return null;
                });

            signal.await();

            if(isError) {
                fail(errorMessage);
            }
        }
        catch(Throwable ex) {
            fail("There was an exception thrown: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Test
    public void testOnMessageIncorrectPermissionsResponse() {
        try {
            RemoteEndpoint.Async mockServer = mock(RemoteEndpoint.Async.class);
            PubSubSocket socket = new PubSubSocket(mockServer);
            CountDownLatch signal = new CountDownLatch(0);
            final long sequence = 1000L;

            JSONObject expectedResponse = new JSONObject()
                    .put("seq", sequence)
                    .put("action", "subscribe")
                    .put("code", 401)
                    .put("message", "Not Authorized")
                    .put("details", "You do not have read permissions on this socket," 
                        + " and therefore cannot subscribe to channels.");

            doAnswer((invocation) -> {
                socket.onMessage(expectedResponse.toString());
                return null;
            }).when(mockServer).sendText(anyString(), any());

            socket.sendRequest(sequence, new JSONObject())
                .thenAcceptAsync((response) -> {
                    isError = true;
                    errorMessage = "Not Expecting a Response: " + response.toString();
                    signal.countDown();
                })
                .exceptionally((error) -> {
                    try {
                        assertTrue(
                            "The expected error type should be an PubSubInternalError",
                            error.getCause() instanceof PubSubException
                        );
                    }
                    catch(AssertionError e) {
                        isError = true;
                        errorMessage = e.getMessage();
                    }

                    signal.countDown();
                    return null;
                });

            signal.await();

            if(isError) {
                fail(errorMessage);
            }
        }
        catch(Throwable ex) {
            fail("There was an exception thrown: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Test
    public void testOnMessageSubscriptionNotFound() {
        try {
            RemoteEndpoint.Async mockServer = mock(RemoteEndpoint.Async.class);
            PubSubSocket socket = new PubSubSocket(mockServer);
            CountDownLatch signal = new CountDownLatch(0);
            final long sequence = 1000L;

            JSONObject expectedResponse = new JSONObject()
                    .put("seq", sequence)
                    .put("action", "subscribe")
                    .put("code", 404)
                    .put("message", "Not Found")
                    .put("details", "You are not subscribed to the specified channel.");

            doAnswer((invocation) -> {
                socket.onMessage(expectedResponse.toString());
                return null;
            }).when(mockServer).sendText(anyString(), any());

            socket.sendRequest(sequence, new JSONObject())
                .thenAcceptAsync((response) -> {
                    isError = true;
                    errorMessage = "Not Expecting a Response: " + response.toString();
                    signal.countDown();
                })
                .exceptionally((error) -> {
                    try {
                        assertTrue(
                            "The expected error type should be an PubSubInternalError",
                            error.getCause() instanceof PubSubException
                        );
                    }
                    catch(AssertionError e) {
                        isError = true;
                        errorMessage = e.getMessage();
                    }

                    signal.countDown();
                    return null;
                });

            signal.await();

            if(isError) {
                fail(errorMessage);
            }
        }
        catch(Throwable ex) {
            fail("There was an exception thrown: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    //The expectations when this occurs are not yet known
    //@Test
    public void testOnMessageInvalidRequestResponse() {
        try {
            RemoteEndpoint.Async mockServer = mock(RemoteEndpoint.Async.class);
            PubSubSocket socket = new PubSubSocket(mockServer);
            CountDownLatch signal = new CountDownLatch(0);
            final long sequence = 1000L;

            JSONObject expectedResponse = new JSONObject()
                    .put("action", "invalid-request")
                    .put("code", 400)
                    .put("message", "Invalid Request")
                    .put("details", "The description of how the format was wrong!");

            doAnswer((invocation) -> {
                socket.onMessage(expectedResponse.toString());
                return null;
            }).when(mockServer).sendText(anyString(), any());

            socket.sendRequest(sequence, new JSONObject())
                .thenAcceptAsync((response) -> {
                    isError = true;
                    errorMessage = "Not Expecting a Response: " + response.toString();
                    signal.countDown();
                })
                .exceptionally((error) -> {
                    try {
                        assertTrue(
                            "The expected error type should be an PubSubInternalError",
                            error.getCause() instanceof PubSubException
                        );
                    }
                    catch(AssertionError e) {
                        isError = true;
                        errorMessage = e.getMessage();
                    }

                    signal.countDown();
                    return null;
                });

            signal.await();

            if(isError) {
                fail(errorMessage);
            }
        }
        catch(Throwable ex) {
            fail("There was an exception thrown: " + ex.getMessage());
            ex.printStackTrace();
        }
        
    }
}