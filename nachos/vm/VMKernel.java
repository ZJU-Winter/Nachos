package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

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
     * write back if the evicted page is dirty,
     * update page table entry's ppn to spn,
     * set -1 if write it back to the COFF
     * @return the evicted page number.
     */
    //TODO: Lock
    @Override
    public static int allocate() {
        return UserKernel.allocate();
    }

	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;

	private static final char dbgVM = 'v';
}
