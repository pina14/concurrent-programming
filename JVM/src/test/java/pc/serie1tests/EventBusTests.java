package pc.serie1tests;

import org.junit.Assert;
import org.junit.Test;
import pc.Helper;
import pc.serie1.EventBus;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class EventBusTests {

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

        //wait to subscribe
        Thread.sleep(500);

        for (int i = 0; i < numOfReps; i++) {
            int message = i;
            producersHelper.createAndStart(() -> {
                try {
                    bus.publishEvent(""+message);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            });
            producersHelper.createAndStart(() -> {
                try {
                    bus.publishEvent(""+message);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            });
            producersHelper.createAndStart(() -> {
                try {
                    bus.publishEvent(message);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            });
        }

        try {
            producersHelper.join();
            bus.shutdown();
            consumersHelper.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(numOfReps*2, stringEventCounter.get());
        Assert.assertEquals(numOfReps, integerEventCounter.get());
    }
}
