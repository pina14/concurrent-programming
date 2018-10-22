package pc;

import org.junit.Assert;
import org.junit.Test;
import pc.serie1.SimpleThreadPoolExecutor;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Rules:
 *  -> Pool has fixed maxPoolSize;
 *  -> Threads have a keepAliveTime when in waiting state;
 *
 *  -> Execute():
 *       o Cases of success:
 *          - If there's available threads in the pool, the command is
 *          delivered to be executed;
 *          - If there's no threads available and the total number of threads
 *          is inferior to maxPoolSize, then a new Thread is created and the command
 *          is delivered to it;
 *          - If there's no threads available and the total number of threads
 *          is equal to maxPoolSize, then the caller thread enters in await state
 *          until it is notified that there´s conditions to execute the work.
 *       o Cases of failure:
 *          - If the pool is in shutdown mode, the method throws RejectedExecutionException;
 *          - If the thread is in waiting state longer than timeout, then return FALSE;
 *          - If the thread enters waiting state and it's interrupted, then throws
 *          InterruptedException.
 *
 *  -> Shutdown(): Puts the pool in shutdown and exits.
 *
 *  -> awaitTermination(): Allows pool to end the work already in execution or in wait to execute.
 *      o Cases of success:
 *          - Returns TRUE if the jobs are all finished.
 *      o Cases of failure:
 *          - Returns FALSE if there's timeout without finishing all the jobs;
 *          - If the thread enters waiting state and it's interrupted, then throws
 *          InterruptedException.
 *
 */
public class SimpleThreadPoolExecutorTests {


    /*********************************** execute() tests ***********************************/
    @Test
    public void test_execute_timeout() throws InterruptedException {
        int maxSize = 20;
        int keepAliveTime = 500;
        SimpleThreadPoolExecutor pool = new SimpleThreadPoolExecutor(maxSize, keepAliveTime);

        for (int i = 0; i < maxSize; i++) {
            boolean executed = pool.execute(() -> {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Assert.assertFalse(true);
                }
            }, 0);
            Assert.assertTrue(executed);
        }

        boolean executed = pool.execute(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Assert.assertFalse(true);
            }
        }, 0);

        Assert.assertFalse(executed);

        pool.shutdown();
        boolean allFinished = pool.awaitTermination(10000);

        Assert.assertTrue(allFinished);
    }

    @Test
    public void test_execute_RejectedExecutionException() throws InterruptedException {
        int maxSize = 20;
        int keepAliveTime = 500;
        SimpleThreadPoolExecutor pool = new SimpleThreadPoolExecutor(maxSize, keepAliveTime);

        pool.shutdown();

        AtomicBoolean wasRejectedExecutionException = new AtomicBoolean(false);
        try {
            pool.execute(() -> {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Assert.assertFalse(true);
                }
            }, 0);

            Assert.assertFalse(true);
        } catch (RejectedExecutionException e) {
            wasRejectedExecutionException.set(true);
        }

        Assert.assertTrue(wasRejectedExecutionException.get());
    }

    @Test
    public void test_execute_InterruptedException() throws InterruptedException {
        int maxSize = 1;
        int keepAliveTime = 500;
        SimpleThreadPoolExecutor pool = new SimpleThreadPoolExecutor(maxSize, keepAliveTime);

        pool.execute(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Assert.assertFalse(true);
            }
        }, 0);


        AtomicBoolean wasInterruptedException = new AtomicBoolean(false);
        Helper h = new Helper();

        h.createAndStart(() -> {
            try {
                pool.execute(() -> {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        Assert.assertFalse(true);
                    }
                }, 5000);
            } catch (InterruptedException e) {
                wasInterruptedException.set(true);
            }
        });

        h.interruptAndJoin();

        pool.shutdown();
        boolean allFinished = pool.awaitTermination(5000);

        Assert.assertTrue(allFinished);
        Assert.assertTrue(wasInterruptedException.get());
    }

    @Test
    public void test_create_more_than_max_size() throws InterruptedException {
        int maxSize = 20;
        int keepAliveTime = 500;
        SimpleThreadPoolExecutor pool = new SimpleThreadPoolExecutor(maxSize, keepAliveTime);

        for (int i = 0; i < maxSize * 2; i++) {
            boolean executed = pool.execute(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Assert.assertFalse(true);
                }
            }, 2000);
            Assert.assertTrue(executed);
        }

        pool.shutdown();
        boolean allFinished = pool.awaitTermination(5000);

        Assert.assertTrue(allFinished);
    }

    /*********************************** awaitTermination() tests ***********************************/
    @Test
    public void test_max_size_await_termination() throws InterruptedException {
        int maxSize = 20;
        int keepAliveTime = 500;
        SimpleThreadPoolExecutor pool = new SimpleThreadPoolExecutor(maxSize, keepAliveTime);

        for (int i = 0; i < maxSize; i++) {
            boolean executed = pool.execute(() -> {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Assert.assertFalse(true);
                }
            }, 0);
            Assert.assertTrue(executed);
        }

        pool.shutdown();
        boolean allFinished = pool.awaitTermination(5000);

        Assert.assertTrue(allFinished);
    }

    @Test
    public void test_awaitTermination_timeout() throws InterruptedException {
        int maxSize = 20;
        int keepAliveTime = 500;
        SimpleThreadPoolExecutor pool = new SimpleThreadPoolExecutor(maxSize, keepAliveTime);

        for (int i = 0; i < maxSize; i++) {
            boolean executed = pool.execute(() -> {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Assert.assertFalse(true);
                }
            }, 0);
            Assert.assertTrue(executed);
        }

        pool.shutdown();
        boolean allFinished = pool.awaitTermination(1000);

        Assert.assertFalse(allFinished);
    }

    @Test
    public void test_awaitTermination_InterruptedException() throws InterruptedException {
        int maxSize = 20;
        int keepAliveTime = 500;
        SimpleThreadPoolExecutor pool = new SimpleThreadPoolExecutor(maxSize, keepAliveTime);

        pool.execute(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Assert.assertFalse(true);
            }
        }, 0);

        AtomicBoolean wasInterruptedException = new AtomicBoolean(false);
        Helper h = new Helper();

        pool.shutdown();
        h.createAndStart(() -> {
            try {
            pool.awaitTermination(5000);
            } catch (InterruptedException e) {
                wasInterruptedException.set(true);
            }
        });

        h.interruptAndJoin();

        Assert.assertTrue(wasInterruptedException.get());
    }
}
