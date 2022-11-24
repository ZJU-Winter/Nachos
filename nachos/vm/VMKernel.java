package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import java.util.Deque;
import java.util.ArrayDeque;
import java.util.List;
import java.util.ArrayList;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
	/**
	 * Allocate a new VM kernel.
	 */
	public VMKernel() {
		super();
	}

	/**
	 * Initialize this kernel.
	 */
	public void initialize(String[] args) {
		super.initialize(args);
	}

	/**
	 * Test this kernel.
	 */
	public void selfTest() {
		super.selfTest();
	}

	/**
	 * Start running user programs.
	 */
	public void run() {
		super.run();
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
    //TODO: remove and close swap file
	public void terminate() {
		super.terminate();
	}

    /**
     * Allocate a physical page and return the physical page number,
     * evict a physical page using clock algorithm if the freePageList is empty,
     * find the owner process and invalid the page table entry,
     * write back if the evicted page is dirty, assign spn tp entry's ppn, set -1 otherwise,
     * @return the evicted page number.
     */
    //TODO: Lock, update IPT
    public static int allocate(VMProcess process, int vpn) {
        return UserKernel.allocate();
    }


    /**
     * Allocate new pages to the swap file, 8 pages a time.
     */
    //TODO: Lock??
    private void growSwapFile() {
        Lib.debug(dbgVM, "VMKernel: growing swap file");
        for (int i = swapFileTotalPages; i < swapFileTotalPages + 8; i += 1) {
            swapFileFreePageList.addLast(i);
        }
        swapFileTotalPages += 8;
    }
    
    private class Pair {
        VMProcess process;
        int vpn;

        public Pair(VMProcess process, int vpn) {
            this.process = process;
            this.vpn = vpn;
        }
    }

	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;

	private static final char dbgVM = 'v';

    private static OpenFile swapFile = null;

    private static Deque<Integer> swapFileFreePageList = new ArrayDeque<>();

    private static List<Pair> invertedPageTable = new ArrayList<>();

    private static int swapFileTotalPages = 0;
}
