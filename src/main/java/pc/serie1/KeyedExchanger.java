package pc.serie1;

import pc.utils.Timeouts;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class KeyedExchanger<T> {

    private class DataHolder {
        public final Optional<T> myData;
        public Optional<T> otherData;
        public Condition cond;
        public boolean wasMatched = false;

        public DataHolder(T data, Condition cond) {
            myData = Optional.of(data);
            this.cond = cond;
        }

        public Optional<T> setAndGet(T dataToSet) {
            otherData = Optional.of(dataToSet);
            wasMatched = true;
            return myData;
        }
    }

    private final Lock mon = new ReentrantLock();
    private final Map<Integer, DataHolder> keysMap = new HashMap<Integer, DataHolder>();

    public Optional<T> exchange(int ky, T mydata, int timeout) throws InterruptedException {
        try {
            mon.lock();
            DataHolder holder = keysMap.get(ky);

            //fast path
            if(holder != null) {
                Optional<T> ret = holder.setAndGet(mydata);
                holder.cond.signal();
                keysMap.remove(ky);
                return ret;
            }

            if (Timeouts.noWait(timeout))
                return Optional.empty();

            //wait to exchange
            long t = Timeouts.start(timeout);
            long remaining = Timeouts.remaining(t);

            holder = new DataHolder(mydata, mon.newCondition());
            keysMap.put(ky, holder);
            while (true) {
                try {
                    holder.cond.await(remaining, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    if (holder.wasMatched)
                        return holder.otherData;

                    throw e;
                }
                if (holder.wasMatched)
                    return holder.otherData;

                remaining = Timeouts.remaining(t);
                if (Timeouts.isTimeout(remaining))
                    return Optional.empty();
            }
        } finally {
            mon.unlock();
        }
    }
}
