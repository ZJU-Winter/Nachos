package nachos.threads;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 * 
 * <p>
 * You must implement this.
 * 
 * @see nachos.threads.Condition
 */
public class Condition2 {
	/**
	 * Allocate a new condition variable.
	 * 
	 * @param conditionLock the lock associated with this condition variable.
	 * The current thread must hold this lock whenever it uses <tt>sleep()</tt>,
	 * <tt>wake()</tt>, or <tt>wakeAll()</tt>.
	 */
	public Condition2(Lock conditionLock) {
		this.conditionLock = conditionLock;
        this.waitQueue = new ArrayDeque<>();
        //this.map = new HashMap<>();
	}

	/**
	 * Atomically release the associated lock and go to sleep on this condition
	 * variable until another thread wakes it using <tt>wake()</tt>. The current
	 * thread must hold the associated lock. The thread will automatically
	 * reacquire the lock before <tt>sleep()</tt> returns.
	 */
	public void sleep() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

		conditionLock.release();
        boolean intStatus = Machine.interrupt().disable();

        waitQueue.addLast(KThread.currentThread());
        //map.put(KThread.currentThread(), -1l);
        KThread.sleep();

		Machine.interrupt().restore(intStatus);

		conditionLock.acquire();
	}

	/**
	 * Wake up at most one thread sleeping on this condition variable. The
	 * current thread must hold the associated lock.
	 */
	public void wake() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        // /* remove stale threads from waitQueue, stale means it was awakened by a timer interrupt handler. */
        // while (!waitQueue.isEmpty() && map.get(waitQueue.peekFirst()) != -1 && map.get(waitQueue.peekFirst()) < Machine.timer().getTime()) {
        //     Lib.debug(dbgCondition, waitQueue.peekFirst().getName() + " is already wake up.");
        //     waitQueue.pollFirst();
        // }

        /* wake up first thread in the waitQueue, and cancle its interrupt. */
        if (!waitQueue.isEmpty()) {
            boolean intStatus = Machine.interrupt().disable();

            KThread toWake = waitQueue.pollFirst();
            if (ThreadedKernel.alarm.cancel(toWake)) {
                System.out.println("cancelled " + toWake.getName() + "'s intertupt.");
            } else if (toWake.getStatus() != 1){
                toWake.ready();
            }

		    Machine.interrupt().restore(intStatus);
        }
	}

	/**
	 * Wake up all threads sleeping on this condition variable. The current
	 * thread must hold the associated lock.
	 */
	public void wakeAll() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        while (!waitQueue.isEmpty()) {
            wake();
        }
	}

    /**
	 * Atomically release the associated lock and go to sleep on
	 * this condition variable until either (1) another thread
	 * wakes it using <tt>wake()</tt>, or (2) the specified
	 * <i>timeout</i> elapses.  The current thread must hold the
	 * associated lock.  The thread will automatically reacquire
	 * the lock before <tt>sleep()</tt> returns.
	 */
    public void sleepFor(long timeout) {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        conditionLock.release();
        waitQueue.addLast(KThread.currentThread());
        //map.put(KThread.currentThread(), Machine.timer().getTime() + timeout);

        ThreadedKernel.alarm.waitUntil(timeout);
        waitQueue.remove(KThread.currentThread());

        conditionLock.acquire();
	}

    /* Test Case 1: ping pong test. */
    private static class InterlockTest {
        private static Lock lock;
        private static Condition2 cv;

        private static class Interlocker implements Runnable {
            public void run () {
                lock.acquire();
                for (int i = 0; i < 10; i++) {
                    System.out.println(KThread.currentThread().getName());
                    cv.wake();
                    KThread.yield();
                    cv.sleep();
                }
                lock.release();
            }
        }

        public InterlockTest () {
            System.out.println("===== Start of Condition PingPong Test ======");
            lock = new Lock();
            cv = new Condition2(lock);

            KThread ping = new KThread(new Interlocker());
            ping.setName("ping");
            KThread pong = new KThread(new Interlocker());
            pong.setName("pong");

            ping.fork();
            pong.fork();

            ping.join();
            //for (int i = 0; i < 50; i++) { KThread.currentThread().yield(); }
        }
    }

    /* Test Case 2: producer and comsumer test. */
    public static void cvTest1() {
        System.out.println("===== Start of Condition Test1 ======");
        final Lock lock = new Lock();
        // final Condition empty = new Condition(lock);
        final Condition2 empty = new Condition2(lock);
        final LinkedList<Integer> list = new LinkedList<>();

        KThread consumer = new KThread( new Runnable () {
                public void run() {
                    lock.acquire();
                    while(list.isEmpty()){
                        empty.sleep();
                    }
                    Lib.assertTrue(list.size() == 5, "List should have 5 values.");
                    while(!list.isEmpty()) {
                        // context swith for the fun of it
                        KThread.yield();
                        System.out.println("Removed " + list.removeFirst());
                    }
                    lock.release();
                }
            });

        KThread producer = new KThread( new Runnable () {
                public void run() {
                    lock.acquire();
                    for (int i = 0; i < 5; i++) {
                        list.add(i);
                        System.out.println("Added " + i);
                        // context swith for the fun of it
                        KThread.yield();
                    }
                    empty.wake();
                    lock.release();
                }
            });

        consumer.setName("Consumer");
        producer.setName("Producer");
        consumer.fork();
        producer.fork();

        consumer.join();
        producer.join();
        //for (int i = 0; i < 50; i++) { KThread.currentThread().yield(); }
    }

    /* Test Case 1: simple sleepFor test. */
    private static void sleepForTest1 () {
        System.out.println("===== Start of sleepFor Test1 ======");
        Lock lock = new Lock();
        Condition2 cv = new Condition2(lock);
    
        lock.acquire();
        long t0 = Machine.timer().getTime();
        System.out.println (KThread.currentThread().getName() + " sleeping");
        // no other thread will wake us up, so we should time out
        cv.sleepFor(2000);
        long t1 = Machine.timer().getTime();
        System.out.println (KThread.currentThread().getName() +
                    " woke up, slept for " + (t1 - t0) + " ticks");
        lock.release();    
    }

    private static void sleepForTest2() {
        System.out.println("===== Start of sleepFor Test2 ======");
        Lock lock = new Lock();
        Condition2 cv = new Condition2(lock);

        KThread thread1 = new KThread(new Runnable() {
            public void run() {
                lock.acquire();
                long t0 = Machine.timer().getTime();
                System.out.println (KThread.currentThread().getName() + " sleeping");
                cv.sleepFor(20000);
                long t1 = Machine.timer().getTime();
                System.out.println(KThread.currentThread().getName() +
                            " woke up, slept for " + (t1 - t0) + " ticks");
                lock.release();
            }
        });

        KThread thread2 = new KThread(new Runnable() {
            public void run() {
                lock.acquire();
                if (ThreadedKernel.alarm.cancel(thread1)) {
                    System.out.println(KThread.currentThread().getName() + " cancelled " + thread1.getName() + "'s timer interrupt.");
                }
                lock.release();
            }
        });
        thread1.setName("thread1").fork();
        thread2.setName("thread2").fork();
        thread1.join();
    }

    private static void sleepForTest3() {
        System.out.println("===== Start of sleepFor Test3 ======");
        Lock lock = new Lock();
        Condition2 cv = new Condition2(lock);

        KThread thread1 = new KThread(new Runnable() {
            public void run() {
                lock.acquire();
                long t0 = Machine.timer().getTime();
                System.out.println (KThread.currentThread().getName() + " sleeping");
                cv.sleepFor(2000);
                long t1 = Machine.timer().getTime();
                System.out.println(KThread.currentThread().getName() +
                            " woke up, slept for " + (t1 - t0) + " ticks");
                lock.release();
            }
        });

        KThread thread2 = new KThread(new Runnable() {
            public void run() {
                lock.acquire();
                cv.wake();
                lock.release();
            }
        });
        thread1.setName("thread1").fork();
        thread2.setName("thread2").fork();
        thread1.join();
    }

    private static void sleepForTest4() {
        System.out.println("===== Start of sleepFor Test4 ======");
        Lock lock = new Lock();
        Condition2 cv = new Condition2(lock);

        KThread thread1 = new KThread(new Runnable() {
            public void run() {
                lock.acquire();
                long t0 = Machine.timer().getTime();
                System.out.println (KThread.currentThread().getName() + " sleeping");
                cv.sleepFor(2000);
                long t1 = Machine.timer().getTime();
                System.out.println(KThread.currentThread().getName() + 
                " woke up, slept for " + (t1 - t0) + " ticks");
                lock.release();
            }
        });

        KThread thread2 = new KThread(new Runnable() {
            public void run() {
                lock.acquire();
                /*
                long t0 = Machine.timer().getTime();
                System.out.println (KThread.currentThread().getName() + " sleeping");
                cv.sleepFor(5000);
                long t1 = Machine.timer().getTime();
                System.out.println(KThread.currentThread().getName() +
                " woke up, slept for " + (t1 - t0) + " ticks");
                 */
                ThreadedKernel.alarm.cancel(thread1);
                cv.wake();
                lock.release();
            }
        });
        thread1.setName("thread1").fork();
        thread2.setName("thread2").fork();
        thread1.join();
    }

    public static void selfTest() {
        Lib.debug(dbgCondition, "Enter Condition2.selfTest");
        if (Lib.test(dbgCondition)) {
            new InterlockTest();
            cvTest1();
            sleepForTest1();
            sleepForTest2();
            sleepForTest3();
            sleepForTest4();
        }
        Lib.debug(dbgCondition, "End Condition2.selfTest\n");
    }

    private static final char dbgCondition = 'o';

    private Lock conditionLock;

    private Deque<KThread> waitQueue; // a queue of waiting threads

    //private HashMap<KThread, Long> map; // keep track of wakeup time
}
