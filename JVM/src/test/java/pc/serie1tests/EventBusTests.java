package pc.serie1tests;

import org.junit.Assert;
import org.junit.Test;
import pc.Helper;
import pc.serie1.EventBus;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Rules:
 *  -> Bus has max events pending for each subscriber buffer;
 *
 *  -> subscribeEvent():
 *       o Subscribes one event with an 'handler' for when a message of that event type is published, that handler is executed.
 *       o The handler is executed by the same thread that called subscribeEvent().
 *       o This is a blocking method. It only returns in case of:
 *          - Shutdown(), but only returns after processing all pending events;
 *          - InterruptedException, this occurs in case the calling thread is interrupted;
 *
 *  -> pubishEvent():
 *      o Cases of success:
 *          - Puts the message in the buffer of all subscribers of this event and returns.
 *      o Cases of failure:
 *          - The event is discarded for each subscriber that has eventsInBuffer >= maxPending;
 *          - Throws IllegalStateException if the Bus is already shutdown.
 *
 *  -> Shutdown(): Blocks the calling thread until all pending events are processed.
 *
 */
public class EventBusTests {

    /****************** TEST maxPending *******************************/
    @Test
    public void test_maxPending() throws InterruptedException {
        int maxPending = 10;
        EventBus bus = new EventBus(maxPending);
        AtomicInteger eventsProcessed = new AtomicInteger(0);

        Helper producersHelper = new Helper();
        Helper consumersHelper = new Helper();

        consumersHelper.createAndStart(() -> {
            Consumer<String> stringHandler = s -> {
                try {
                    if(eventsProcessed.getAndIncrement() == 0)
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Assert.fail();
                }
            };
            bus.subscribeEvent(stringHandler, String.class);
        });

        //ensure subscription
        Thread.sleep(500);

        for (int i = 0; i < maxPending * 3; i++) {
            int message = i;
            producersHelper.createAndStart(() -> {
                try {
                    bus.publishEvent(""+message);
                } catch (IllegalStateException e) {
                    Assert.fail();
                }
            });
        }

        producersHelper.join();
        bus.shutdown();

        //Assert that the events processed == maxPending + 1.
        // Plus one because is the first event processed more the maxPending in buffer.
        Assert.assertEquals(maxPending + 1, eventsProcessed.get());
    }


    /****************** TEST subscribeEvent() *******************************/
    @Test
    public void test_return_subscription_in_shutdown() throws InterruptedException {
        int maxPending = 10;
        EventBus bus = new EventBus(maxPending);

        bus.shutdown();

        Consumer<String> stringHandler = s -> Assert.fail();
        bus.subscribeEvent(stringHandler, String.class);
    }

    @Test
    public void test_interrupt_consumer() throws InterruptedException {
        int maxPending = 10;
        EventBus bus = new EventBus(maxPending);
        AtomicBoolean exceptionOccurred = new AtomicBoolean(false);

        Helper consumersHelper = new Helper();
        consumersHelper.createAndStart(() -> {
            Consumer<String> stringHandler = s -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            };
            try {
                bus.subscribeEvent(stringHandler, String.class);
            }catch (InterruptedException e) {
                exceptionOccurred.set(true);
            }
        });

        //ensure subscriptions
        Thread.sleep(100);

        consumersHelper.interruptAndJoin();

        Assert.assertTrue(exceptionOccurred.get());
    }


    /****************** TEST publishEvent() *******************************/
    @Test
    public void test_delivers_all_events_from_multiple_publishers() throws InterruptedException {
        int numOfReps = 1000;
        EventBus bus = new EventBus(numOfReps);

        AtomicInteger stringEventCounter = new AtomicInteger(0);
        AtomicInteger integerEventCounter = new AtomicInteger(0);

        Helper consumersHelper = new Helper();
        Helper producersHelper = new Helper();

        consumersHelper.createAndStart(() -> {
            Consumer<String> stringHandler = s -> stringEventCounter.incrementAndGet();
            bus.subscribeEvent(stringHandler, String.class);
        });

        consumersHelper.createAndStart(() -> {
            Consumer<Integer> integerHandler = i -> integerEventCounter.incrementAndGet();
            bus.subscribeEvent(integerHandler, Integer.class);

        });

        //ensure subscriptions
        Thread.sleep(100);

        for (int i = 0; i < numOfReps; i++) {
            int message = i;
            producersHelper.createAndStart(() -> {
                try {
                    bus.publishEvent(""+message);
                } catch (IllegalStateException e) {
                    Assert.fail();
                }
            });
            producersHelper.createAndStart(() -> {
                try {
                    bus.publishEvent(""+message);
                } catch (IllegalStateException e) {
                    Assert.fail();
                }
            });
            producersHelper.createAndStart(() -> {
                try {
                    bus.publishEvent(message);
                } catch (IllegalStateException e) {
                    Assert.fail();
                }
            });
        }

        producersHelper.join();
        bus.shutdown();

        Assert.assertEquals(numOfReps*2, stringEventCounter.get());
        Assert.assertEquals(numOfReps, integerEventCounter.get());
    }

    @Test
    public void test_publisher_illegalStateException() throws InterruptedException {
        int numOfReps = 1000;
        EventBus bus = new EventBus(numOfReps);
        AtomicBoolean exceptionOccurred = new AtomicBoolean(false);

        bus.shutdown();

        try {
            bus.publishEvent("");
        } catch (IllegalStateException e) {
            exceptionOccurred.set(true);
        }

        Assert.assertTrue(exceptionOccurred.get());
    }

    /****************** TEST shutdown() *******************************/
    @Test
    public void test_executes_all_pending_events_before_shutdown() throws InterruptedException {
        int maxPending = 100;
        EventBus bus = new EventBus(maxPending);
        AtomicInteger eventsProcessed = new AtomicInteger(0);

        Helper producersHelper = new Helper();
        Helper consumersHelper = new Helper();

        consumersHelper.createAndStart(() -> {
            Consumer<String> stringHandler = s -> eventsProcessed.getAndIncrement();
            bus.subscribeEvent(stringHandler, String.class);
        });

        //ensure subscriptions
        Thread.sleep(100);

        for (int i = 0; i < maxPending; i++) {
            int message = i;
            producersHelper.createAndStart(() -> {
                try {
                    bus.publishEvent(""+message);
                } catch (IllegalStateException e) {
                    Assert.fail();
                }
            });
            producersHelper.createAndStart(() -> {
                try {
                    bus.publishEvent(""+message * -1);
                } catch (IllegalStateException e) {
                    Assert.fail();
                }
            });
        }

        producersHelper.join();
        bus.shutdown();

        //times 2 because there's 2 publishers for each iteration
        Assert.assertEquals(maxPending * 2, eventsProcessed.get());
    }
}
