package pc;

import org.junit.Assert;
import org.junit.Test;
import pc.serie1.KeyedExchanger;

import java.util.Optional;

public class Exercise1Tests {

    @Test
    public void test_exchange() throws InterruptedException {
        KeyedExchanger<String> exchanger = new KeyedExchanger<>();
        String str1 = "a";
        String str2 = "b";

        Helper h = new Helper();

        h.createAndStart(() -> {
            Optional res = exchanger.exchange(1, str1, 5000);
            if(res.isPresent())
                Assert.assertEquals(res.get(), str2);
            else
                Assert.assertFalse(true);
        });

        h.createAndStart(() -> {
            Optional res = exchanger.exchange(1, str2, 5000);
            if(res.isPresent())
                Assert.assertEquals(res.get(), str1);
            else
                Assert.assertFalse(true);
        });

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
    public void test_timeout() throws InterruptedException {
        KeyedExchanger<String> exchanger = new KeyedExchanger<>();
        String str1 = "a";

        Helper h = new Helper();

        h.createAndStart(() -> {
            Optional res = exchanger.exchange(1, str1, 5000);
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
                Optional res = exchanger.exchange(1, str1, 1000);
                Assert.assertTrue(false);
            } catch (InterruptedException e) {
                Assert.assertTrue(true);
            }
        });

        h.interruptAndJoin();
    }
}
