package pc.serie1;

import pc.utils.Timeouts;
import java.util.LinkedList;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SimpleThreadPoolExecutor {

    private Lock lock = new ReentrantLock();
    private final Condition waitTermination = lock.newCondition();

    private LinkedList<WorkUnit> workToExecute = new LinkedList<>();
    private LinkedList<WorkerThread> workersWaiting = new LinkedList<>();

    private final int maxPoolSize, keepAliveTime;
    private boolean shuttingDown = false;
    private int workersCounter = 0;
    private int workPendingCounter = 0;

    public SimpleThreadPoolExecutor(int maxPoolSize, int keepAliveTime) {
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
    }

    public boolean execute(Runnable command, int timeout) throws InterruptedException {
        try {
            lock.lock();

            //fast path
            if(shuttingDown)
                throw new RejectedExecutionException();

            if(!workersWaiting.isEmpty()) {
                WorkerThread worker = workersWaiting.pollFirst();
                WorkUnit work = new WorkUnit(command);
                worker.startWorkAndSignal(work);
                workPendingCounter++;
                return true;
            }

            if(workersCounter < maxPoolSize) {
                WorkUnit work = new WorkUnit(command);
                WorkerThread worker = new WorkerThread(work);
                worker.start();
                workersCounter++;
                workPendingCounter++;
                return true;
            }

            if(Timeouts.noWait(timeout))
                return false;

            //wait to execute
            long targetTime = Timeouts.start(timeout);
            long remaining = Timeouts.remaining(targetTime);

            WorkUnit work = new WorkUnit(command);
            workToExecute.add(work);
            workPendingCounter++;
            while (true) {
                try {
                    work.waitWorkerThread.await(remaining, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    if (work.isBeingExecuted)
                        return true;
                    workToExecute.remove(work);
                    workPendingCounter--;
                    throw e;
                }

                if (work.isBeingExecuted)
                    return true;

                remaining = Timeouts.remaining(targetTime);
                if (Timeouts.isTimeout(remaining)) {
                    workToExecute.remove(work);
                    workPendingCounter--;
                    return false;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void shutdown() {
        try {
            lock.lock();
            shuttingDown = true;
        }finally {
            lock.unlock();
        }
    }

    public boolean awaitTermination(int timeout) throws InterruptedException {
        try {
            lock.lock();

            //fast path
            if(workPendingCounter == 0)
                return true;

            if(Timeouts.noWait(timeout))
                return false;

            //wait work termination
            long targetTime = Timeouts.start(timeout);
            long remaining = Timeouts.remaining(targetTime);
            while (true) {
                try {
                    waitTermination.await(remaining, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    if (workPendingCounter == 0)
                        return true;
                    throw e;
                }

                if (workPendingCounter == 0)
                    return true;

                remaining = Timeouts.remaining(targetTime);
                if (Timeouts.isTimeout(remaining)) {
                    return false;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private class WorkUnit {
        private Condition waitWorkerThread = lock.newCondition();
        private Runnable work;
        private boolean isBeingExecuted = false;

        private WorkUnit(Runnable w) {
            work = w;
        }
    }


    private class WorkerThread extends Thread {
        private Condition waitWork = lock.newCondition();
        private WorkUnit workUnit;

        private WorkerThread(WorkUnit work) {
            workUnit = work;
        }

        @Override
        public void run() {
            do {
                workUnit.work.run();

                try {
                    lock.lock();
                    workPendingCounter--;
                    workUnit = null;
                } finally {
                    lock.unlock();
                }
            } while (fetchWork());
        }

        void startWorkAndSignal(WorkUnit work) {
            try {
                lock.lock();
                workUnit = work;
                waitWork.signal();
            } finally {
                lock.unlock();
            }
        }

        private boolean fetchWork() {
            try {
                lock.lock();

                //fast path
                if(!workToExecute.isEmpty()) {
                    workUnit = workToExecute.poll();
                    workUnit.isBeingExecuted = true;
                    workUnit.waitWorkerThread.signal();
                    return true;
                }

                if(shuttingDown) {
                    waitTermination.signal();
                    return false;
                }

                if(Timeouts.noWait(keepAliveTime))
                    return false;

                //wait to execute
                long targetTime = Timeouts.start(keepAliveTime);
                long remaining = Timeouts.remaining(targetTime);

                workersWaiting.add(this);
                while (true) {
                    try {
                        waitWork.await(remaining, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        //ignore exception
                        if (workUnit != null)
                            return true;
                        workersWaiting.remove(this);
                        return false;
                    }

                    if (workUnit != null)
                        return true;

                    remaining = Timeouts.remaining(targetTime);
                    if (Timeouts.isTimeout(remaining)) {
                        workersWaiting.remove(this);
                        return false;
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }

}
