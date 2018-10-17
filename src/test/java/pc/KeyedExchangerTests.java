package pc;

import org.junit.Assert;
import org.junit.Test;
import pc.serie1.KeyedExchanger;

import java.util.Optional;

public class KeyedExchangerTests {

    @Test
    public void test_exchange_successful() throws InterruptedException {
        KeyedExchanger<Integer> exchanger = new KeyedExchanger<>();

        Helper h = new Helper();
        int numOfReps = 100;

        for (int i = 0, k = 100; i < numOfReps; i++, k++) {
            Integer message = i;
            int key = k;
            h.createAndStart(() -> {
                Optional res = exchanger.exchange(key, message, Integer.MAX_VALUE);
                if(res.isPresent())
                    Assert.assertEquals(res.get(), message + 1);
                else
                    Assert.assertFalse(true);
            });
        }

        for (int i = 1, k = 100; i < numOfReps + 1; i++, k++) {
            Integer message = i;
            int key = k;
            h.createAndStart(() -> {
                Optional res = exchanger.exchange(key, message, Integer.MAX_VALUE);
                if(res.isPresent())
                    Assert.assertEquals(res.get(), message -1);
                else
                    Assert.assertFalse(true);
            });
        }

        h.join();
    }

    @Test
    public void test_no_timeout() throws InterruptedException {
        KeyedExchanger<String> exchanger = new KeyedExchanger<>();
        String str1 = "a";
        String str2 = "b";

        Helper h = new Helper();

        h.createAndStart(() -> {
            Optional res = exchanger.exchange(1, str1, 0);
            Assert.assertFalse(res.isPresent());
        });

        h.createAndStart(() -> {
            Optional res = exchanger.exchange(1, str2, 0);
            Assert.assertFalse(res.isPresent());
        });

        h.join();
    }

    @Test
    public void test_3_threads_same_key() throws InterruptedException {
        KeyedExchanger<String> exchanger = new KeyedExchanger<>();
        String str1 = "a";
        String str2 = "b";

        Helper h = new Helper();

        h.createAndStart(() -> {
            Optional res = exchanger.exchange(1, str1, Integer.MAX_VALUE);
            if(res.isPresent())
                Assert.assertEquals(res.get(), str2);
            else
                Assert.assertFalse(true);
        });

        h.createAndStart(() -> exchanger.exchange(1, str2, 2000));
        h.createAndStart(() -> exchanger.exchange(1, str2, 2000));

        h.join();
    }

    @Test
    public void test_timeout() throws InterruptedException {
        KeyedExchanger<String> exchanger = new KeyedExchanger<>();
        String str1 = "a";

        Helper h = new Helper();

        h.createAndStart(() -> {
            Optional res = exchanger.exchange(1, str1, 1000);
            Assert.assertFalse(res.isPresent());
        });

        h.join();
    }

    @Test
    public void test_interrupt() throws InterruptedException {
        KeyedExchanger<String> exchanger = new KeyedExchanger<>();
        String str1 = "a";

        Helper h = new Helper();

        h.createAndStart(() -> {
            try {
                exchanger.exchange(1, str1, Integer.MAX_VALUE);
                Assert.assertTrue(false);
            } catch (InterruptedException e) {
                Assert.assertTrue(true);
            }
        });

        h.interruptAndJoin();
    }
}
