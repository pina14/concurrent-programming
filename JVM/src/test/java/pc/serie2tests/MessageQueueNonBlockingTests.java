package pc.serie2tests;

import org.junit.Assert;
import org.junit.Test;
import pc.Helper;
import pc.serie2.messageQueueOptimized.MessageQueueNonBlocking.SendStatusNB;
import pc.serie2.messageQueueOptimized.MessageQueueNonBlocking;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class MessageQueueNonBlockingTests {

    @Test
    public void test_successful_delivery() throws InterruptedException {
        MessageQueueNonBlocking<Integer> q = new MessageQueueNonBlocking<>();

        Helper h = new Helper();

        int numOfReps = 100;

        AtomicInteger counterSent = new AtomicInteger(0);
        AtomicInteger counterReceived = new AtomicInteger(0);

        for (int i = 0; i < numOfReps; i++) {
            Integer message = i;
            h.createAndStart(() -> {
                SendStatusNB status = q.send(message);
                boolean wasDelivered = status.await(Integer.MAX_VALUE);
                if(!wasDelivered)
                    Assert.assertFalse(true);
                else
                    counterSent.incrementAndGet();
            });
        }

        for (int i = 0; i < numOfReps; i++) {
            h.createAndStart(() -> {
                Optional message = q.receive(Integer.MAX_VALUE);
                if(!message.isPresent())
                    Assert.assertFalse(true);
                else
                    counterReceived.incrementAndGet();
            });
        }

        h.join();

        //Test all messages were marked as sent and all messages were received
        Assert.assertEquals(counterSent.intValue(), numOfReps);
        Assert.assertEquals(counterReceived.intValue(), numOfReps);
    }

    @Test
    public void test_call_receive_then_send() throws InterruptedException {
        MessageQueueNonBlocking<Integer> q = new MessageQueueNonBlocking<>();

        Helper h = new Helper();

        Integer m = 20;

        h.createAndStart(() -> {
            Optional message = q.receive(1000);
            if(!message.isPresent() || message.get() != m)
                Assert.assertFalse(true);
        });

        h.createAndStart(() -> {
            SendStatusNB status = q.send(m);
            boolean wasDelivered = status.await(1000);
            if(!wasDelivered || !status.isSent())
                Assert.assertFalse(true);
        });

        h.join();
    }

    @Test
    public void test_SendStatus_await_timeout() throws InterruptedException {
        MessageQueueNonBlocking<Integer> q = new MessageQueueNonBlocking<>();

        Helper h = new Helper();

        for (int i = 0; i < 10; i++) {
            Integer message = i;
            h.createAndStart(() -> {
                SendStatusNB status = q.send(message);
                boolean wasDelivered = status.await(2000);
                if(wasDelivered)
                    Assert.assertFalse(true);
            });
        }

        h.join();
    }

    @Test
    public void test_receive_timeout() throws InterruptedException {
        MessageQueueNonBlocking<Integer> q = new MessageQueueNonBlocking<>();

        Helper h = new Helper();

        for (int i = 0; i < 10; i++) {
            h.createAndStart(() -> {
                Optional message = q.receive(2000);
                if(message.isPresent())
                    Assert.assertFalse(true);
            });
        }

        h.join();
    }

    @Test
    public void test_isSent() throws InterruptedException {
        MessageQueueNonBlocking<Integer> q = new MessageQueueNonBlocking<>();

        Helper h = new Helper();

        int numOfReps = 100;

        for (int i = 0; i < numOfReps; i++) {
            Integer message = i;
            h.createAndStart(() -> {
                SendStatusNB status = q.send(message);
                boolean wasDelivered = status.await(Integer.MAX_VALUE);
                if(wasDelivered && !status.isSent())
                    Assert.assertFalse(true);
            });
        }

        for (int i = 0; i < numOfReps; i++) {
            h.createAndStart(() -> {
                Optional message = q.receive(Integer.MAX_VALUE);
                if(!message.isPresent())
                    Assert.assertFalse(true);
            });
        }

        h.join();
    }
}
