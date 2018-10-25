package pc.serie1;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class EventBus {
    private final int maxPending;
    private final Lock lock = new ReentrantLock();
    private final Condition finishedCondition = lock.newCondition();
    private final HashMap<Class, EventSubscribers> subscribers = new HashMap<>();
    private boolean shuttingDown = false;

    public EventBus(int maxPending) {
        this.maxPending = maxPending;
    }

    public <E> void subscribeEvent(Consumer<E> handler, Class eventType) throws InterruptedException {
        Subscriber subscriber = init(handler, eventType);
        if(subscriber == null)
            return;

        while (!shuttingDown || subscriber.hasEventsInBuffer()) {
            Object event = getEvent(subscriber);

            if(event != null)
                subscriber.processEvent(event);
        }

        signalIfLast(subscriber, eventType);
    }

    private void signalIfLast(Subscriber subscriber, Class eventType) {
        try {
            lock.lock();

            EventSubscribers eventSubscribers = subscribers.get(eventType);
            eventSubscribers.removeSubscriber(subscriber);
            if(eventSubscribers.noSubscribers()) {
                subscribers.remove(eventType);
                if(subscribers.isEmpty())
                    finishedCondition.signal();
            }
        } finally {
            lock.unlock();
        }
    }

    private Object getEvent(Subscriber subscriber) throws InterruptedException {
        try {
            lock.lock();

            if(subscriber.hasEventsInBuffer())
                return subscriber.getEvent();

            if(shuttingDown)
                return null;

            subscribers.get(subscriber.subscriptionType).waitEvent.await();

            return subscriber.getEvent();
        } finally {
            lock.unlock();
        }
    }

    private <E> Subscriber init(Consumer<E> handler, Class eventType) {
        try {
            lock.lock();

            if(shuttingDown)
                return null;

            EventSubscribers eventSubscribers = subscribers.get(eventType);
            if(eventSubscribers == null) {
                eventSubscribers = new EventSubscribers();
                subscribers.put(eventType, eventSubscribers);
            }

            Subscriber<E> subscriber = new Subscriber<>(handler, eventType);
            eventSubscribers.addSubscriber(subscriber);
            return subscriber;
        } finally {
            lock.unlock();
        }
    }

    public <E> void publishEvent(E message) throws IllegalStateException {
        try {
            lock.lock();

            if(shuttingDown)
                throw new IllegalStateException();
            if(subscribers.isEmpty())
                return;

            Class eventType = message.getClass();
            EventSubscribers<E> eventSubscribers = subscribers.get(eventType);
            if(eventSubscribers != null && eventSubscribers.hasSubscribers()) {
                eventSubscribers.addToAllSubscribers(message);
                eventSubscribers.waitEvent.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    public void shutdown() {
        try {
            lock.lock();

            shuttingDown = true;

            subscribers.values().forEach(eventSubscribers -> eventSubscribers.waitEvent.signalAll());

            while(true) {
                try {
                    finishedCondition.await();
                } catch (InterruptedException e) {
                    //ignore
                }

                if(subscribers.isEmpty())
                    return;
            }
        } finally {
            lock.unlock();
        }
    }

    private class EventSubscribers<E> {
        private final Condition waitEvent = lock.newCondition();
        private final LinkedList<Subscriber<E>> subscribers = new LinkedList<>();

        private boolean noSubscribers() {
            return subscribers.isEmpty();
        }

        private boolean hasSubscribers() {
            try {
                lock.lock();
                return !subscribers.isEmpty();
            } finally {
                lock.unlock();
            }
        }

        private void addToAllSubscribers(E message) {
            try {
                lock.lock();
                subscribers.forEach(s -> s.addEvent(message));
            } finally {
                lock.unlock();
            }
        }

        private void removeSubscriber(Subscriber subscriber) {
            try {
                lock.lock();
                subscribers.remove(subscriber);
            } finally {
                lock.unlock();
            }
        }

        private void addSubscriber(Subscriber<E> subscriber) {
            try {
                lock.lock();
                subscribers.add(subscriber);
            } finally {
                lock.unlock();
            }
        }
    }

    private class Subscriber<E> {
        private final Class subscriptionType;
        private LinkedList<E> eventsBuffer = new LinkedList<>();
        private final Consumer<E> handler;

        private Subscriber(Consumer<E> h, Class eventType) {
            handler = h;
            subscriptionType = eventType;
        }

        private void processEvent(Object event) {
            handler.accept((E) event);
        }

        private void addEvent(E message) {
            try {
                lock.lock();
                if (eventsBuffer.size() < maxPending)
                    eventsBuffer.add(message);
            }finally {
                lock.unlock();
            }
        }

        private E getEvent() {
            try {
                lock.lock();
                return eventsBuffer.poll();
            } finally {
                lock.unlock();
            }
        }

        private boolean hasEventsInBuffer() {
            try {
                lock.lock();
                return !eventsBuffer.isEmpty();
            } finally {
                lock.unlock();
            }
        }
    }
}
