package pc.serie1.messageQueue;

import pc.utils.Timeouts;

import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MessageQueue<T> {

    private final Lock mon = new ReentrantLock();
    private final LinkedList<WaitingStatus> messages = new LinkedList<>();
    private final LinkedList<Receiver> receivers = new LinkedList<>();

    public SendStatus send(T sentMsg) {
        try {
            mon.lock();
            if(!receivers.isEmpty()) {
                Receiver receiver = receivers.poll();
                receiver.message = sentMsg;
                receiver.condition.signal();
                return new DeliveredStatus();
            } else {
                WaitingStatus mStatus = new WaitingStatus(sentMsg);
                messages.add(mStatus);
                return mStatus;
            }
        } finally {
            mon.unlock();
        }
    }

    public Optional<T> receive(int timeout) throws InterruptedException {
        try {
            mon.lock();

            //fast path
            if(!messages.isEmpty()) {
                WaitingStatus mStatus = messages.poll();
                mStatus.setAsSentAndSignal();
                return Optional.of(mStatus.message);
            }

            if(Timeouts.noWait(timeout))
                return Optional.empty();

            //wait to receive
            long targetTime = Timeouts.start(timeout);
            long remaining = Timeouts.remaining(targetTime);

            Receiver receiver = new Receiver(mon.newCondition());
            receivers.add(receiver);
            while (true) {
                try {
                    receiver.condition.await(remaining, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    if (receiver.hasReceived())
                        return Optional.of(receiver.message);
                    receivers.remove(receiver);
                    throw e;
                }

                if (receiver.hasReceived())
                    return Optional.of(receiver.message);

                remaining = Timeouts.remaining(targetTime);
                if (Timeouts.isTimeout(remaining)) {
                    receivers.remove(receiver);
                    return Optional.empty();
                }
            }
        } finally {
            mon.unlock();
        }
    }

    class DeliveredStatus implements SendStatus {
        @Override
        public boolean isSent() {
            return true;
        }

        @Override
        public boolean tryCancel() {
            return false;
        }

        @Override
        public boolean await(int timeout) throws InterruptedException {
            return true;
        }
    }

    class WaitingStatus implements SendStatus {
        private final Lock messageMon = new ReentrantLock();
        private final Condition condition = messageMon.newCondition();
        T message;
        private boolean isSent;
        private boolean canceled;

        WaitingStatus(T message) {
            this.message = message;
            isSent = false;
            canceled = false;
        }

        void setAsSentAndSignal() {
            try {
                messageMon.lock();
                isSent = true;
                condition.signal();
            } finally {
                messageMon.unlock();
            }
        }

        @Override
        public boolean isSent() {
            try {
                messageMon.lock();
                return isSent;
            } finally {
                messageMon.unlock();
            }
        }

        @Override
        public boolean tryCancel() {
            try {
                messageMon.lock();

                if(isSent)
                    return false;

                messages.remove(this);
                canceled = true;
                return true;
            } finally {
                messageMon.unlock();
            }
        }

        @Override
        public boolean await(int timeout) throws InterruptedException {
            try {
                messageMon.lock();

                //fast path
                if(isSent)
                    return true;

                if(Timeouts.noWait(timeout) || canceled)
                    return false;

                //wait to deliver
                long targetTime = Timeouts.start(timeout);
                long remaining = Timeouts.remaining(targetTime);
                while (true) {
                    try {
                        condition.await(remaining, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        if(isSent)
                            return true;
                        throw e;
                    }

                    if (isSent)
                        return true;

                    remaining = Timeouts.remaining(targetTime);
                    if (Timeouts.isTimeout(remaining))
                        return false;
                }
            } finally {
                messageMon.unlock();
            }
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
