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
        numPhysPages = Machine.processor().getNumPhysPages();
        invertedPageTable = new InvertedPageTableEntry[numPhysPages];
        swapLock = new Lock();
        pinLock = new Lock();
        allPinned = new Condition(pinLock);
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
        Lib.debug(dbgVM, "VMKernel: \tterminating VMKernel...");
        if (swapFile != null) {
            swapFile.close();
            if(ThreadedKernel.fileSystem.remove(swapFileName)) {
                Lib.debug(dbgVM, "VMKernel: \tremove swapfile successfully");
            }
        }
		super.terminate();
	}

    /**
     * Allocate a physical page and return the physical page number.
     * Evict a physical page using clock algorithm if the freePageList is empty.
     * @return the allocated page number.
     */
    public static int allocate(VMProcess process, int vpn) {
        lock.acquire();
        int pageNum;
        if (freePageList.isEmpty()) {
            pageNum = evict();
        } else {
            pageNum = freePageList.removeFirst();
        }
        invertedPageTable[pageNum] = new InvertedPageTableEntry(process, vpn);
        Lib.debug(dbgVM, "VMKernel: \tassign a physical page to PID[" + process.getPID() + "] vpn " + vpn + " ppn " + pageNum);
        lock.release();
        return pageNum;
    }

    /**
     * Evict a physical page and return the ecvicted physical page number.
     * Find the owner process and invalid the page table entry,
     * if the evicted page is dirty, write back to the dish and set the entry's ppn to the spn, set -1 otherwise.
     * @return the evicted physical page number.
     */
    private static int evict() {
        pinLock.acquire();
        Lib.debug(dbgVM, "VMKernel: \tinsufficient physical memory, evicting...");
        while (numPinned == numPhysPages) {
            Lib.debug(dbgVM, "VMKernel: \tall pages are pinned, waiting...");
            allPinned.wait();
        }
        int toEvict;
        while (true) {
            VMProcess process = invertedPageTable[victim].process;
            int vpn = invertedPageTable[victim].vpn;
            if (!process.isUsed(vpn) && !invertedPageTable[victim].pinned) {
                toEvict = victim;
                victim = (victim + 1) % numPhysPages;
                process.unsetValid(vpn);
                if (process.isDirty(vpn)) {
                    int spn = allocateSwapFilePage();
                    process.setPPN(vpn, spn);
                    writeToSwapFile(toEvict, spn);
                    Lib.debug(dbgVM, "VMKernel: \tevict ppn "+ toEvict + " and write it to disk spn " + spn);
                } else {
                    process.setPPN(vpn, -1);
                    Lib.debug(dbgVM, "VMKernel: \tevict ppn "+ toEvict + " no need to write back");
                }
                break;
            } else {
                victim = (victim + 1) % numPhysPages;
                process.unsetUsed(vpn);
            }
        }
        pinLock.release();
        return toEvict;
    }

    /**
     * Allocate a swap page number from swap file.
     * @return the allocated swap page number.
     */
    private static int allocateSwapFilePage() {
        swapLock.acquire();
        if (swapFile == null) {
            swapFile = ThreadedKernel.fileSystem.open(swapFileName, true);
            if (swapFile == null) {
                System.out.println("VMKernel: \tcreate the swap file failed");
                Lib.assertNotReached();
            }
        }
        if (swapFileFreePageList.isEmpty()) {
            growSwapFile();
        }
        int pageNum = swapFileFreePageList.removeFirst();
        swapLock.release();
        return pageNum;
    }

    /**
     * Deallocate a swapfile page, called by readFromSwapFile.
     * @param spn the page number to deallocate.
     */
    public static void deallocateSwapFilePage(int spn) {
        swapLock.acquire();
        swapFileFreePageList.addLast(spn);
        swapLock.release();
    }

    /**
     * Write one page from physical memory to swap file.
     * @param ppn the physical memory page number to read.
     * @param spn the swap page number of write.
     */
    private static void writeToSwapFile(int ppn, int spn) {
        byte[] memory = Machine.processor().getMemory();
        int written = swapFile.write(spn * pageSize, memory, ppn * pageSize, pageSize);
        if (written != pageSize) {
            Lib.debug(dbgVM, "VMKernel: \twrite to swap file less than " + pageSize + " bytes");
        }
    }

    /**
     * Read one page from swap to physical memory.
     * @param ppn the physical memory page number to write.
     * @param spn the swap page number to read.
     */
    public static void readFromSwapFile(int ppn, int spn) {
        byte[] memory = Machine.processor().getMemory();
        int read = swapFile.read(spn * pageSize, memory, ppn * pageSize, pageSize);
        if (read != pageSize) {
            Lib.debug(dbgVM, "VMKernel: \tread from swap file less than " + pageSize + " bytes");
        }
        deallocateSwapFilePage(spn);
    }

    /**
     * Expand the swap file, 8 pages a time.
     */
    private static void growSwapFile() {
        Lib.debug(dbgVM, "VMKernel: growing the swap file");
        for (int i = swapFileTotalPages; i < swapFileTotalPages + 8; i += 1) {
            swapFileFreePageList.addLast(i);
        }
        swapFileTotalPages += 8;
    }

    private static class InvertedPageTableEntry {
        VMProcess process;
        int vpn;
        boolean pinned;

        public InvertedPageTableEntry(VMProcess process, int vpn) {
            this.process = process;
            this.vpn = vpn;
            this.pinned = false;
        }
    }

    public static void pinPage(int ppn) {
        pinLock.acquire();
        invertedPageTable[ppn].pinned = true;
        numPinned += 1;
        pinLock.release();
    }

    public static void unpinPage(int ppn) {
        pinLock.acquire();
        invertedPageTable[ppn].pinned = false;
        numPinned -= 1;
        allPinned.wake();
        pinLock.release();
    }

    private static OpenFile swapFile = null;

    private static Deque<Integer> swapFileFreePageList = new ArrayDeque<>();

    private static int swapFileTotalPages = 0;

    private static Lock swapLock;

    private static InvertedPageTableEntry[] invertedPageTable;

    private static int victim = 0;

    private static Condition allPinned;

    private static Lock pinLock;

    private static int numPinned = 0;

    private static int numPhysPages;

	private static final int pageSize = Processor.pageSize;

	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;

	private static final char dbgVM = 'v';

    private static final String swapFileName = ".swapfile";
}
