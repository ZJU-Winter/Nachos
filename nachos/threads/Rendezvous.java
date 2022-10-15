package nachos.threads;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import nachos.machine.*;

/**
 * A <i>Rendezvous</i> allows threads to synchronously exchange values.
 */
public class Rendezvous {
    /**
     * Allocate a new Rendezvous.
     */
    public Rendezvous () {
        this.lock = new Lock();
        this.cv = new Condition(this.lock);
        this.first = new HashMap<>();
        this.second = new HashMap<>();
    }

    /**
     * Synchronously exchange a value with another thread.  The first
     * thread A (with value X) to exhange will block waiting for
     * another thread B (with value Y).  When thread B arrives, it
     * will unblock A and the threads will exchange values: value Y
     * will be returned to thread A, and value X will be returned to
     * thread B.
     *
     * Different integer tags are used as different, parallel
     * synchronization points (i.e., threads synchronizing at
     * different tags do not interact with each other).  The same tag
     * can also be used repeatedly for multiple exchanges.
     *
     * @param tag the synchronization tag.
     * @param value the integer to exchange.
     */

    public int exchange (int tag, int value) {
        lock.acquire();
        if (!first.containsKey(tag)) {
            first.put(tag, value);

            while (!second.containsKey(tag)) {
                cv.sleep();
            }
            int secondVal = second.get(tag);
            second.remove(tag);
            lock.release();
            return secondVal;
        }

        second.put(tag, value);
        cv.wake();
        int firstVal = first.get(tag);
        first.remove(tag);
        lock.release();
        return firstVal;
    }

    /* Test Cases for Rendezvous. */
    public static void rendezTest() {
        final Rendezvous r = new Rendezvous();
    
        KThread t1 = new KThread(new Runnable () {
            public void run() {
                int tag = 0;
                int send = -1;
    
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == 1, "Was expecting " + 1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
            });

        t1.setName("t1");
        KThread t2 = new KThread(new Runnable () {
            public void run() {
                int tag = 0;
                int send = 1;
    
                System.out.println ("Thread " + KThread.currentThread().getName() + " exchanging " + send);
                int recv = r.exchange (tag, send);
                Lib.assertTrue (recv == -1, "Was expecting " + -1 + " but received " + recv);
                System.out.println ("Thread " + KThread.currentThread().getName() + " received " + recv);
            }
            });
            t2.setName("t2");
        
            t1.fork();
            t2.fork();
            t1.join();
            t2.join();
            
        }
    
        public static void selfTest() {
            rendezTest();
            rendezTest();
        }

    private Condition cv;

    private Lock lock;

    private Map<Integer, Integer> first;

    private Map<Integer, Integer> second;
}