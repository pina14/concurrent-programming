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
        final Optional<T> myData;
        Optional<T> otherData;
        Condition cond;
        boolean wasMatched = false;

        DataHolder(T data, Condition cond) {
            myData = Optional.of(data);
            this.cond = cond;
        }

        Optional<T> setAndGet(T dataToSet) {
            otherData = Optional.of(dataToSet);
            wasMatched = true;
            return myData;
        }
    }

    private final Lock mon = new ReentrantLock();
    private final Map<Integer, DataHolder> keysMap = new HashMap<>();

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
            long targetTime = Timeouts.start(timeout);
            long remaining = Timeouts.remaining(targetTime);

            holder = new DataHolder(mydata, mon.newCondition());
            keysMap.put(ky, holder);
            while (true) {
                try {
                    holder.cond.await(remaining, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    if (holder.wasMatched)
                        return holder.otherData;
                    keysMap.remove(ky);
                    throw e;
                }
                if (holder.wasMatched)
                    return holder.otherData;

                remaining = Timeouts.remaining(targetTime);
                if (Timeouts.isTimeout(remaining)) {
                    keysMap.remove(ky);
                    return Optional.empty();
                }
            }
        } finally {
            mon.unlock();
        }
    }
}
