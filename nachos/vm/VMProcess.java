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

    /*
    private int readVMWithPT(byte[] memory, int vaddr, byte[] data, int offset, int amount) {
        super.readVMWithPT();
    }

    private int writeVMWithPT(byte[] data, int offset, byte[] memory, int vaddr, int amount) {
        super.writeVMWithPT();
    }
    */

    /**
     * 
     * @param vaddr the virtual address of page that is invalid.
     * @return -1 if load the page fails, return 0 if load the page successfully.
     */
    private int handlePageFault(int vaddr) {
        int vpn = Processor.pageFromAddress(vaddr);
        int ppn = pageTable[vpn].ppn;
        Lib.debug(dbgVM, "PID[" + PID + "]:" + "\tPage Fault on " + vaddr + " VPN: " + vpn + "PPN: " + ppn);
        for (int s = 0; s < coff.getNumSections(); s += 1) {
            CoffSection section = coff.getSection(s);
            if (section.getFirstVPN() <= vpn && section.getFirstVPN() + section.getLength() > vpn) {
                section.loadPage(vpn, ppn);
                pageTable[vpn].valid = true;
                pageTable[vpn].used = true;
                Lib.debug(dbgVM, "PID[" + PID + "]:" + "\tLoad a page " + " VPN: " + vpn + "PPN: " + ppn);
                return 0;
            }
        }
        byte[] memory = Machine.processor().getMemory();
        Arrays.fill(memory, ppn * pageSize, (ppn + 1) * pageSize, (byte) 0);
        pageTable[vpn].valid = true;
        pageTable[vpn].used = true;
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
