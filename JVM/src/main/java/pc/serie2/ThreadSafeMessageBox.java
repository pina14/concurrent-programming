package pc.serie2;

import java.util.concurrent.atomic.AtomicInteger;

public class ThreadSafeMessageBox<M> {

    private class MsgHolder {
        private final M msg;
        private AtomicInteger lives;

        private MsgHolder(M m, int l) {
            msg = m;
            lives = new AtomicInteger(l);
        }
    }

    private MsgHolder msgHolder = null;

    public void publish(M m, int lvs) {
        msgHolder = new MsgHolder(m, lvs);
    }

    public M tryConsume() {
        int lvs;
        do {
            if (msgHolder == null)
                return null;
            lvs = msgHolder.lives.get();
            if (lvs <= 0)
                return null;

        } while (msgHolder.lives.compareAndSet(lvs, lvs - 1) == false);

        return msgHolder.msg;
    }
}
