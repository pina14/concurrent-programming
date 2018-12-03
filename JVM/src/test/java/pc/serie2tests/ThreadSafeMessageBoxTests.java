package pc.serie2tests;

import org.junit.Assert;
import org.junit.Test;
import pc.Helper;
import pc.serie2.ThreadSafeMessageBox;

import java.util.concurrent.atomic.AtomicInteger;

public class ThreadSafeMessageBoxTests {

    @Test
    public void test_try_consume_equal_to_lives() throws InterruptedException {
        int lives = 100;
        ThreadSafeMessageBox<String> mBox = new ThreadSafeMessageBox<>();
        Helper h = new Helper();
        AtomicInteger counter = new AtomicInteger(0);

        mBox.publish("Test message", lives);

        for (int i = 0; i < lives; i++) {
            h.createAndStart(() -> {
                String msg = mBox.tryConsume();
                if(msg != null)
                    counter.incrementAndGet();
                });
        }

        h.join();
        Assert.assertEquals(lives, counter.get());
    }

    @Test
    public void test_try_consume_double_of_lives() throws InterruptedException {
        int lives = 100;
        ThreadSafeMessageBox<String> mBox = new ThreadSafeMessageBox<>();
        Helper h = new Helper();
        AtomicInteger counter = new AtomicInteger(0);

        mBox.publish("Test message", lives);

        for (int i = 0; i < lives * 2; i++) {
            h.createAndStart(() -> {
                String msg = mBox.tryConsume();
                if(msg != null)
                    counter.incrementAndGet();
            });
        }

        h.join();
        Assert.assertEquals(lives, counter.get());
    }

    @Test
    public void test_try_consume_half_of_lives() throws InterruptedException {
        int lives = 100;
        ThreadSafeMessageBox<String> mBox = new ThreadSafeMessageBox<>();
        Helper h = new Helper();
        AtomicInteger counter = new AtomicInteger(0);

        mBox.publish("Test message", lives);

        for (int i = 0; i < lives / 2; i++) {
            h.createAndStart(() -> {
                String msg = mBox.tryConsume();
                if(msg != null)
                    counter.incrementAndGet();
            });
        }

        h.join();
        Assert.assertEquals(lives / 2, counter.get());
    }

}
