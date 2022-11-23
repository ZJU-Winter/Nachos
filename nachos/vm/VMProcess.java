package nachos.vm;

import java.util.Arrays;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
	/**
	 * Allocate a new process.
	 */
	public VMProcess() {
		super();
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
		super.saveState();
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		super.restoreState();
	}

    /**
     * Initializes page tables for this process so that the executable can be
     * demand-paged.
     * 
     * @return <tt>true</tt> if successful.
     */
    @Override
    protected boolean loadSections() {        
        if (numPages > Machine.processor().getNumPhysPages()) {
            coff.close();
            Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tinsufficient physical memory");
            return false;
        }

        pageTable = new TranslationEntry[numPages];

        for (int s = 0; s < coff.getNumSections(); s += 1) {
            CoffSection section = coff.getSection(s);

            Lib.debug(dbgVM, "\tinitializing " + section.getName() + " section (" + section.getLength() + " pages)");

            for (int i = 0; i < section.getLength(); i += 1) {
                int vpn = section.getFirstVPN() + i;
                int ppn = VMKernel.allocate();

                pageTable[vpn] = new TranslationEntry(vpn, ppn, false, section.isReadOnly(), false, false);
                //Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tloaded a page, vpn " + vpn + ", ppn " + ppn);
            }
        }
        //load pages for the stack and args
        CoffSection lastSection = coff.getSection(coff.getNumSections() - 1);
        int nextVPN = lastSection.getFirstVPN() + lastSection.getLength();
        for (int i = 0; i <= stackPages; i += 1) {
            int vpn = nextVPN + i;
            int ppn = VMKernel.allocate();
            pageTable[vpn] = new TranslationEntry(vpn, ppn, false, false, false, false);
            //Lib.debug(dbgProcess, "PID[" + PID + "]:" + "\tloaded a page, vpn " + vpn + ", ppn " + ppn);
        }
        return true;
        
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
        super.unloadSections();
    }

	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @param offset the first byte to write in the array.
	 * @param length the number of bytes to transfer from virtual memory to the
	 * array.
	 * @return the number of bytes successfully transferred.
	 */
    @Override
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

        if (vaddr < 0 || vaddr >= numPages * pageSize) {
            return 0;
        }

		int amount = Math.min(length, numPages * pageSize - vaddr);

		return readVMWithPT(memory, vaddr, data, offset, amount);
	}

    private int readVMWithPT(byte[] memory, int vaddr, byte[] data, int offset, int amount) {
		int currentVa = vaddr;
		int totalRead = 0;
		while (currentVa < vaddr + amount) {
			int vpn = Processor.pageFromAddress(currentVa);
            if (!pageTable[vpn].valid) {
                Lib.debug(dbgVM, "PID[" + PID + "]:" + "\treadVMWithPT Page Fault on VPN " + vpn);
                handlePageFault(currentVa);
            }
			int ppn = pageTable[vpn].ppn;
			int addrOffset = Processor.offsetFromAddress(currentVa);
			int paddr = pageSize * ppn + addrOffset;
			int nextVa = pageSize * (vpn + 1);
            //Lib.debug(dbgProcess,  "PID[" + PID + "]:" + "\tvirtual address " + vaddr + ", physical address " + paddr + ", offset " + addrOffset);
			if (nextVa < vaddr + amount) { // reach the end of page
				int toRead = pageSize - addrOffset;
				System.arraycopy(memory, paddr, data, offset, toRead);
				offset += toRead;
				totalRead += toRead;
                //Lib.debug(dbgProcess,  "PID[" + PID + "]:" + "\tread from vpn " + vpn + " / ppn " + ppn + " to buffer " + toRead + " bytes");
			} else { // will not reach the end of page
                int toRead = vaddr + amount - currentVa;
				System.arraycopy(memory, paddr, data, offset, toRead);
				offset += toRead;
				totalRead += toRead;
                //Lib.debug(dbgProcess,  "PID[" + PID + "]:" + "\tread from vpn " + vpn + " / ppn " + ppn + " to buffer " + toRead + " bytes");
			}
			currentVa = nextVa;
		}
		return totalRead;
	}

    /**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @param offset the first byte to transfer from the array.
	 * @param length the number of bytes to transfer from the array to virtual
	 * memory.
	 * @return the number of bytes successfully transferred.
	 */
    @Override
    /* 
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		if (vaddr < 0 || vaddr >= numPages * pageSize) {
			return 0;
        }

		int amount = Math.min(length, numPages * pageSize - vaddr);

		return writeVMWithPT(data, offset, memory, vaddr, amount);
	}
    */

	private int writeVMWithPT(byte[] data, int offset, byte[] memory, int vaddr, int amount) {
		int currentVa = vaddr;
		int totalWrite = 0;
		while (currentVa < vaddr + amount) {
			int vpn = Processor.pageFromAddress(currentVa);
            if (!pageTable[vpn].valid) {
                Lib.debug(dbgVM, "PID[" + PID + "]:" + "\twriteVMWithPT Page Fault on VPN " + vpn);
                handlePageFault(currentVa);
            }
			int ppn = pageTable[vpn].ppn;
			int addrOffset = Processor.offsetFromAddress(currentVa);
			int paddr = pageSize * ppn + addrOffset;
			int nextVa = pageSize * (vpn + 1);
            //Lib.debug(dbgProcess,  "PID[" + PID + "]:" + "\tvirtual address " + vaddr + ", physical address " + paddr + ", offset " + addrOffset);
			if (nextVa < vaddr + amount) { // reach the end of page
				int toWrite = pageSize - addrOffset;
				System.arraycopy(data, offset, memory, paddr, toWrite);
				offset += toWrite;
				totalWrite += toWrite;
                //Lib.debug(dbgProcess,  "PID[" + PID + "]:" + "\twrite from vpn " + vpn + " / ppn " + ppn + " to buffer " + toWrite + " bytes");
			} else { // will not reach the end of page
                int toWrite = vaddr + amount - currentVa;
				System.arraycopy(data, offset, memory, paddr, toWrite);
				offset += toWrite;
				totalWrite += toWrite;
                //Lib.debug(dbgProcess,  "PID[" + PID + "]:" + "\twrite from vpn " + vpn + " / ppn " + ppn + " to buffer " + toWrite + " bytes");
			}
			currentVa = nextVa;
		}
		return totalWrite;
	}

    /**
     * 
     * @param vaddr the virtual address of page that is invalid.
     * @return -1 if load the page fails, return 0 if load the page successfully.
     */
    private int handlePageFault(int vaddr) {
        int vpn = Processor.pageFromAddress(vaddr);
        int ppn = pageTable[vpn].ppn;
        Lib.debug(dbgVM, "PID[" + PID + "]:" + "\tPage Fault on " + vaddr + " VPN: " + vpn + " PPN: " + ppn);
        for (int s = 0; s < coff.getNumSections(); s += 1) {
            CoffSection section = coff.getSection(s);
            if (section.getFirstVPN() <= vpn && section.getFirstVPN() + section.getLength() > vpn) {
                section.loadPage(vpn - section.getFirstVPN(), ppn);
                pageTable[vpn].valid = true;
                pageTable[vpn].used = true;
                Lib.debug(dbgVM, "PID[" + PID + "]:" + "\tLoad a page " + " VPN: " + vpn + " PPN: " + ppn);
                return 0;
            }
        }
        byte[] memory = Machine.processor().getMemory();
        Arrays.fill(memory, ppn * pageSize, (ppn + 1) * pageSize, (byte) 0);
        pageTable[vpn].valid = true;
        pageTable[vpn].used = true;
        Lib.debug(dbgVM, "PID[" + PID + "]:" + "\tLoad a page " + " VPN: " + vpn + " PPN: " + ppn);
        return 0;
    }

    /**
     * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
     * . The <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     * 
     * @param cause the user exception that occurred.
     */
    public void handleException(int cause) {
        Processor processor = Machine.processor();

        switch (cause) {
            case Processor.exceptionPageFault:
                int result = handlePageFault(processor.readRegister(Processor.regBadVAddr));
			    processor.writeRegister(Processor.regV0, result);
                break;
            default:
                super.handleException(cause);
                break;
        }
    }

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';
}
