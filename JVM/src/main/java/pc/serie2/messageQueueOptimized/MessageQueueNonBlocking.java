package pc.serie2.messageQueueOptimized;

import pc.utils.Timeouts;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MessageQueueNonBlocking<T> {

    public interface SendStatusNB {
        boolean isSent();
        boolean await(int timeout) throws InterruptedException;
    }

    private final Lock mon = new ReentrantLock();
    private final LinkedQueueNB<WaitingStatus> messages = new LinkedQueueNB<>();
    private final LinkedQueueNB<Receiver> receivers = new LinkedQueueNB<>();

    public SendStatusNB send(T sentMsg) {
        if(receivers.isNotEmpty()) {
            Receiver receiver = receivers.get();
            receiver.message = sentMsg;
            try {
                mon.lock();
                receiver.condition.signal();
            } finally {
                mon.unlock();
            }
            return new DeliveredStatus();
        } else {
            WaitingStatus mStatus = new WaitingStatus(sentMsg);
            messages.put(mStatus);
            return mStatus;
        }
    }

    public Optional<T> receive(int timeout) throws InterruptedException {
        //fast path
        if(messages.isNotEmpty()) {
            WaitingStatus mStatus = messages.get();
            mStatus.setAsSentAndSignal();
            return Optional.of(mStatus.message);
        }

        if(Timeouts.noWait(timeout))
            return Optional.empty();

        try {
            mon.lock();
            long targetTime = Timeouts.start(timeout);
            long remaining;
            Receiver receiver = new Receiver(mon.newCondition());
            receivers.put(receiver);
            while (true) {
                if(messages.isNotEmpty()) {
                    WaitingStatus mStatus = messages.get();
                    mStatus.setAsSentAndSignal();
                    return Optional.of(mStatus.message);
                }

                remaining = Timeouts.remaining(targetTime);
                if (Timeouts.isTimeout(remaining))
                    return Optional.empty();
                try {
                    receiver.condition.await(remaining, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    if (receiver.hasReceived())
                        return Optional.of(receiver.message);
                    throw e;
                }

                if (receiver.hasReceived())
                    return Optional.of(receiver.message);
            }
        } finally {
            mon.unlock();
        }
    }

    class DeliveredStatus implements SendStatusNB {
        @Override
        public boolean isSent() {
            return true;
        }

        @Override
        public boolean await(int timeout) throws InterruptedException {
            return true;
        }
    }

    class WaitingStatus implements SendStatusNB {
        private final Lock messageMon = new ReentrantLock();
        private final Condition condition = messageMon.newCondition();
        final T message;
        private final AtomicBoolean isSent;

        WaitingStatus(T message) {
            this.message = message;
            isSent = new AtomicBoolean(false);
        }

        void setAsSentAndSignal() {
            isSent.set(true);
            try {
                messageMon.lock();
                condition.signal();
            } finally {
                messageMon.unlock();
            }
        }

        @Override
        public boolean isSent() {
            return isSent.get();
        }

        @Override
        public boolean await(int timeout) throws InterruptedException {
            //fast path
            if(isSent.get())
                return true;

            if(Timeouts.noWait(timeout))
                return false;

            //wait to deliver
            try {
                messageMon.lock();
                long targetTime = Timeouts.start(timeout);
                long remaining;
                while (!isSent.get()) {
                    remaining = Timeouts.remaining(targetTime);
                    if (Timeouts.isTimeout(remaining))
                        return false;
                    try {
                        condition.await(remaining, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        if (isSent.get())
                            return true;
                        throw e;
                    }
                }
            } finally {
                messageMon.unlock();
            }

            return true;
        }
    }

    class Receiver {
        final Condition condition;
        T message;

        Receiver(Condition c) {
            condition = c;
            message = null;
        }

        boolean hasReceived() {
            return message != null;
        }
    }
}
