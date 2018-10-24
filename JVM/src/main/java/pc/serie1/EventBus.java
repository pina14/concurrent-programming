package pc.serie1;

import java.util.function.Consumer;

public class EventBus {

    public EventBus(int maxPending) {

    }

    public <T> void subsribeEvent(Consumer<T> handler, Class type) {

    }

    public <E> void publishEvent(E message) {

    }

    public void shutdown() {

    }
}
