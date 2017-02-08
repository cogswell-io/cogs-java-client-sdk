package com.gambit.sdk.pubsub.utils;

/**
 * Created by gambit on 2/7/17.
 */
public class PubSubUtils {
    /**
     * Simple method to spin up new thread that calls provided Runnable no sooner than the given delay in ms.
     * @param runnable The runnable that will be called after the given delay
     * @param delay The time in milliseconds to wait before calling the given runnable
     * @throws InterruptedException
     */
    public static void setTimeout(Runnable runnable, long delay)
            throws InterruptedException
    {
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                runnable.run();
            }
            catch(InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
}
