package com.gambit.sdk.pubsub;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import java.util.Collections;
import java.util.Arrays;
import java.util.Vector;
import java.util.List;

import java.util.Random;

import com.gambit.sdk.pubsub.handlers.PubSubMessageHandler;

import org.json.JSONObject;

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;

public class PubSubInteractionIntegrationTest {

    private static String testServer = "*";
    private static List<String> primaryPermissions = null;
    private static List<String> secondaryPermissions = null;

    private static PubSubHandle primaryHandle;
    private static PubSubHandle secondaryHandle;
    private static String errorMessage;
    private static boolean isError;

    @BeforeClass
    public static void setUpBeforeClass() {
        PubSubIntegrationTestsConfig config = PubSubIntegrationTestsConfig.getInstance();
        testServer = config.getHost();
        primaryPermissions = config.getMainKeys();
        secondaryPermissions = config.getSecondaryKeys();
    }

    @Before
    public void setupBeforeEach() {
        primaryHandle = null;
        secondaryHandle = null;
        errorMessage = "";
        isError = false;
    }

    private static CountDownLatch signal = new CountDownLatch(2);

    private static String books = "BOOKS";
    private static String movies = "MOVIES";
    private static String booksMessage = "A good book makes the cat sleep.";
    private static String moviesMessage = "A good movie makes the dog bark.";

    private void onBooksMessage(PubSubMessageRecord record) {
        try {
            assertEquals(
                "The only messages from books should be from BOOKS...",
                books,
                record.getChannel()
            );

            assertEquals(
                "The only message received on BOOKS should be the books message...",
                booksMessage,
                record.getMessage()
            );
        }
        catch(AssertionError e) {
            isError = true;
            errorMessage = "Error in BOOKS handler: " + e.getMessage();
        }

        signal.countDown();
    }

    private void onMoviesMessage(PubSubMessageRecord record) {
        try {
            assertEquals(
                "The only messages from movies should be from MOVIES...",
                movies,
                record.getChannel()
            );

            assertEquals(
                "The only message received on MOVIES should be the movies message...",
                moviesMessage,
                record.getMessage()
            );
        }
        catch(AssertionError e) {
            isError = true;
            errorMessage = "Error in MOVIES handler: " + e.getMessage();
        }

        signal.countDown();
    }

    @Test
    public void testInteractionOfTwoHandles() {
        try {
            PubSubOptions options = new PubSubOptions(testServer, null, null, null);
            PubSubSDK pubsubSDK = PubSubSDK.getInstance();

            StringBuffer msgFromOne = new StringBuffer();
            StringBuffer msgFromTwo = new StringBuffer();

            pubsubSDK.connect(primaryPermissions, options)
                .thenComposeAsync((handle) -> {
                    primaryHandle = handle;

                    return pubsubSDK.connect(secondaryPermissions, options);
                })
                .thenComposeAsync((handle) -> {
                    secondaryHandle = handle;
                    
                    return primaryHandle.subscribe(books, this::onBooksMessage);
                })
                .thenComposeAsync((subsOne) -> {
                    return secondaryHandle.subscribe(movies, this::onMoviesMessage);
                })
                .thenComposeAsync((subscriptions) -> {
                    return primaryHandle.publish(movies, moviesMessage, null);
                })
                .thenComposeAsync((sequence) -> {
                    return secondaryHandle.publish(books, booksMessage, null);
                })
                .thenAcceptAsync((sequence) -> {
                    // We don't care...
                })
                .exceptionally((error) -> {
                    return null;
                });

            try {
                signal.await();

                assertEquals(
                    "The two messages received on different sockets should be the same message.",
                    msgFromOne.toString(),
                    msgFromTwo.toString()
                );

                if(isError) {
                    fail("There was an exception: " + errorMessage);
                }
            }
            catch(InterruptedException e) {
                fail("INTERRUPTED WHILE WAITING FOR COUNTDOWN");
            }

            primaryHandle.close()
                .thenAcceptAsync((channels) -> {
                    secondaryHandle.close();
                })
                .exceptionally((error) -> {
                    return null;
                });
        }
        catch(Throwable ex) {
            fail("There was an exception thrown: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}