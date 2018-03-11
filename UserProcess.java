package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.util.LinkedList;

/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 * 
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 * 
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
	/**
	 * Allocate a new process.
	 */
	public UserProcess() {
		int numPhysPages = Machine.processor().getNumPhysPages();
		pageTable = new TranslationEntry[numPhysPages];
		for (int i = 0; i < numPhysPages; i++)
			pageTable[i] = new TranslationEntry(i, i, true, false, false, false);
	}

	/**
	 * Allocate and return a new process of the correct class. The class name is
	 * specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 * 
	 * @return a new process of the correct class.
	 */
	
	public static UserProcess newUserProcess() {
		return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
	}

	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
		if (!load(name, args))
			return false;

		new UThread(this).setName(name).fork();

		return true;
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		Machine.processor().setPageTable(pageTable);
	}

	/**
	 * Read a null-terminated string from this process's virtual memory. Read at
	 * most <tt>maxLength + 1</tt> bytes from the specified address, search for
	 * the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 * 
	 * @param vaddr the starting virtual address of the null-terminated string.
	 * @param maxLength the maximum number of characters in the string, not
	 * including the null terminator.
	 * @return the string read, or <tt>null</tt> if no null terminator was
	 * found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
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
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		// for now, just assume that virtual addresses equal physical addresses
		if (vaddr < 0 || vaddr >= memory.length)
			return 0;

		int amount = Math.min(length, memory.length - vaddr);
		System.arraycopy(memory, vaddr, data, offset, amount);

		return amount;
	}

	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory. Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
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
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		// for now, just assume that virtual addresses equal physical addresses
		if (vaddr < 0 || vaddr >= memory.length)
			return 0;

		int amount = Math.min(length, memory.length - vaddr);
		System.arraycopy(data, offset, memory, vaddr, amount);

		return amount;
	}

	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the executable was successfully loaded.
	 */
	private boolean load(String name, String[] args) {
		Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

		OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
		if (executable == null) {
			Lib.debug(dbgProcess, "\topen failed");
			return false;
		}

		try {
			coff = new Coff(executable);
		}
		catch (EOFException e) {
			executable.close();
			Lib.debug(dbgProcess, "\tcoff load failed");
			return false;
		}

		// make sure the sections are contiguous and start at page 0
		numPages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				Lib.debug(dbgProcess, "\tfragmented executable");
				return false;
			}
			numPages += section.getLength();
		}

		// make sure the argv array will fit in one page
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += 4 + argv[i].length + 1;
		}
		if (argsSize > pageSize) {
			coff.close();
			Lib.debug(dbgProcess, "\targuments too long");
			return false;
		}

		// program counter initially points at the program entry point
		initialPC = coff.getEntryPoint();

		// next comes the stack; stack pointer initially points to top of it
		numPages += stackPages;
		initialSP = numPages * pageSize;

		// and finally reserve 1 page for arguments
		numPages++;

		if (!loadSections())
			return false;

		// store arguments in last page
		int entryOffset = (numPages - 1) * pageSize;
		int stringOffset = entryOffset + args.length * 4;

		this.argc = args.length;
		this.argv = entryOffset;

		for (int i = 0; i < argv.length; i++) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
			stringOffset += argv[i].length;
			Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] { 0 }) == 1);
			stringOffset += 1;
		}

		return true;
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be run
	 * (this is the last step in process initialization that can fail).
	 * 
	 * @return <tt>true</tt> if the sections were successfully loaded.
	 */
	protected boolean loadSections() {
		if (numPages > Machine.processor().getNumPhysPages()) {
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			return false;
		}

		// load sections
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);

			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");

			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;

				// for now, just assume virtual addresses=physical addresses
				section.loadPage(i, vpn);
			}
		}

		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
	}

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of the
	 * stack, set the A0 and A1 registers to argc and argv, respectively, and
	 * initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i = 0; i < processor.numUserRegisters; i++)
			processor.writeRegister(i, 0);

		// initialize PC and SP according
		processor.writeRegister(Processor.regPC, initialPC);
		processor.writeRegister(Processor.regSP, initialSP);

		// initialize the first two argument registers to argc and argv
		processor.writeRegister(Processor.regA0, argc);
		processor.writeRegister(Processor.regA1, argv);
	}

	/**
	 * Handle the halt() system call.
	 */
	private int handleHalt() {

		Machine.halt();

		Lib.assertNotReached("Machine.halt() did not halt machine!");
		return 0;
	}
	
	
	/*Project 2 Task 1 
	 *Implement System Call
	 *Yishan */
	
	private int handleCreate(int a0) {
        // private int handleCreate() {  
    Lib.debug(dbgProcess, "[handleCreate] Start ");                           /*@BAA*/

    // a0 is address of filename 
    String filename = readVirtualMemoryString(a0, MAXSTRLEN);          /*@BAA*/

    Lib.debug(dbgProcess, "[handleCreate] filename: "+filename);                      /*@BAA*/

    // invoke open through stubFilesystem
    OpenFile retval  = UserKernel.fileSystem.open(filename, true);     /*@BAA*/

    if (retval == null) {                                              /*@BAA*/
        return -1;                                                     /*@BAA*/
    }                                                                  /*@BAA*/
    else {                                                             /*@BAA*/
        int fileHandle = findEmptyFileDescriptor();                    /*@BAA*/ 
        if (fileHandle < 0)                                            /*@BAA*/ 
            return -1;                                                 /*@BAA*/ 
        else {                                                         /*@BAA*/
            fds[fileHandle].filename = filename;                       /*@BAA*/
            Lib.debug(dbgProcess,                                      /*@BAA*/
                "[handleCreate] handle " + fileHandle);               /*@BAA*/
            fds[fileHandle].file = retval;                             /*@BAA*/
            return fileHandle;                                         /*@BAA*/
        }                                                              /*@BAA*/ 
    }                                                                  /*@BAA*/
}                                                                      /*@BAA*/

/**
 * Attempt to open the named file and return a file descriptor.
 *
 * Note that open() can only be used to open files on disk; open() will never
 * return a file descriptor referring to a stream.
 *
 * Returns the new file descriptor, or -1 if an error occurred.
 */
private int handleOpen(int a0) {
    Lib.debug(dbgProcess, "[UserProcess.handleOpen] Start");           /*@BAA*/

    Lib.debug(dbgProcess, "[UserProcess.handleOpen] a0: "+a0+"\n");    /*@BAA*/

    if (a0 < 0) {                                                      /*@BDA*/ 
        Lib.debug(dbgProcess,                                          /*@BDA*/
                "[UserProcess.handleOpen] a0: invalid address\n");     /*@BDA*/
        return -1;                                                     /*@BDA*/
    }                                                                  /*@BDA*/

    // a0 is address of filename 
    String filename = readVirtualMemoryString(a0, MAXSTRLEN);          /*@BAA*/

    Lib.debug(dbgProcess, "filename: "+filename);                      /*@BAA*/

    // invoke open through stubFilesystem, truncate flag is set to false
    OpenFile retval  = UserKernel.fileSystem.open(filename, false);    /*@BAA*/

    if (retval == null) {                                              /*@BAA*/
        Lib.debug(dbgProcess,                                          /*@BEA*/
                "[UserProcess.handleOpen] failed to open "+filename);  /*@BEA*/
        return -1;                                                     /*@BAA*/
    }                                                                  /*@BAA*/
    else {                                                             /*@BAA*/
        int fileHandle = findEmptyFileDescriptor();                    /*@BAA*/ 
        if (fileHandle < 0) {                                          /*@BAA*/ 
            Lib.debug(dbgProcess,                                      /*@BEA*/
         "[UserProcess.handleOpen] failed to find empty file handler");/*@BEA*/
            return -1;                                                 /*@BAA*/ 
        }                                                              /*@BEA*/
        else {                                                         /*@BAA*/
            fds[fileHandle].filename = filename;                       /*@BAA*/
            fds[fileHandle].file = retval;                             /*@BAA*/
            return fileHandle;                                         /*@BAA*/
        }                                                              /*@BAA*/ 
    }                                                                  /*@BAA*/
}                                                                      /*@BAA*/


/**
 * Attempt to read up to count bytes into buffer from the file or stream
 * referred to by fileDescriptor.
 *
 * On success, the number of bytes read is returned. If the file descriptor
 * refers to a file on disk, the file position is advanced by this number.
 *
 * It is not necessarily an error if this number is smaller than the number of
 * bytes requested. If the file descriptor refers to a file on disk, this
 * indicates that the end of the file has been reached. If the file descriptor
 * refers to a stream, this indicates that the fewer bytes are actually
 * available right now than were requested, but more bytes may become available
 * in the future. Note that read() never waits for a stream to have more data;
 * it always returns as much as possible immediately.
 *
 * On error, -1 is returned, and the new file position is undefined. This can
 * happen if fileDescriptor is invalid, if part of the buffer is read-only or
 * invalid, or if a network stream has been terminated by the remote host and
 * no more data is available.
 */
private int handleRead(int a0, int a1, int a2) {                      /*@BAA*/
    Lib.debug(dbgProcess, "handleRead()");                            /*@BAA*/
     
    int handle = a0;                    /* a0 is file descriptor handle @BAA*/
    int vaddr = a1;                     /* a1 is buf address            @BAA*/
    int bufsize = a2;                   /* a2 is buf size               @BAA*/

    Lib.debug(dbgProcess, "handle: " + handle);                       /*@BAA*/
    Lib.debug(dbgProcess, "buf address: " + vaddr);                   /*@BAA*/
    Lib.debug(dbgProcess, "buf size: " + bufsize);                    /*@BAA*/

    // get data regarding to file descriptor
    if (handle < 0 || handle > MAXFD                                  /*@BAA*/
            || fds[handle].file == null)                              /*@BAA*/
        return -1;                                                    /*@BAA*/

    if (bufsize < 0) {                                                /*@BGA*/
        Lib.debug(dbgProcess, "[UserProcess.handleRead]"              /*@BGA*/
                  + " bufsize is a negative number");                 /*@BGA*/
        return  -1;                                                   /*@BGA*/
    }                                                                 /*@BGA*/
    else if (bufsize == 0) {                                          /*@BGA*/
        Lib.debug(dbgProcess, "[UserProcess.handleRead]"              /*@BGA*/
                  + " bufsize is zero");                              /*@BGA*/
        return 0;                                                     /*@BGA*/
    }                                                                 /*@BGA*/

    FileDescriptor fd = fds[handle];                                  /*@BAA*/
    byte[] buf = new byte[bufsize];                                   /*@BAA*/

    /* invoke read through classOpenFileWithPosition                        */
    /* rather than class StubFileSystem                                     */
    int readnum = fd.file.read(buf, 0, bufsize);                      /*@BHC*/

    if (readnum < 0) {                                                /*@BGA*/
        return -1;                                                    /*@BAA*/
    }                                                                 /*@BAA*/
    else {                                                            /*@BAA*/
        int writenum = writeVirtualMemory(vaddr, buf, 0, readnum);    /*@BGC*/

        if (writenum < 0) {                                           /*@BGA*/
            return -1;                                                /*@BGA*/
        }                                                             /*@BGA*/
        else {                                                        /*@BHA*/
        /*fd.position = fd.position + writenum;                         @BHD*/
           return writenum;                                           /*@BGC*/
        }                                                             /*@BHA*/
    }                                                                 /*@BAA*/
}                                                                     /*@BAA*/

/**
 * Attempt to write up to count bytes from buffer to the file or stream
 * referred to by fileDescriptor. write() can return before the bytes are
 * actually flushed to the file or stream. A write to a stream can block,
 * however, if kernel queues are temporarily full.
 *
 * On success, the number of bytes written is returned (zero indicates nothing
 * was written), and the file position is advanced by this number. It IS an
 * error if this number is smaller than the number of bytes requested. For
 * disk files, this indicates that the disk is full. For streams, this
 * indicates the stream was terminated by the remote host before all the data
 * was transferred.
 *
 * On error, -1 is returned, and the new file position is undefined. This can
 * happen if fileDescriptor is invalid, if part of the buffer is invalid, or
 * if a network stream has already been terminated by the remote host.
 *
 * Syscall: 
 *       int write(int fileDescriptor, void *buffer, int count);
 *
 */
 private int handleWrite(int a0, int a1, int a2) {
    Lib.debug(dbgProcess, "handleWrite()");                           /*@BAA*/
     
    int handle = a0;                    /* a0 is file descriptor handle @BAA*/
    int vaddr = a1;                     /* a1 is buf address            @BAA*/
    int bufsize = a2;                   /* a2 is buf size               @BAA*/
    int retval;                                                       /*@BAA*/  

    Lib.debug(dbgProcess, "handle: " + handle);                       /*@BAA*/
    Lib.debug(dbgProcess, "buf address: " + vaddr);                   /*@BAA*/
    Lib.debug(dbgProcess, "buf size: " + bufsize);                    /*@BAA*/

    // get data regarding to file descriptor
    if (handle < 0 || handle > MAXFD                                  /*@BAA*/
            || fds[handle].file == null) {                            /*@BAA*/
        Lib.debug(dbgProcess, "[UserProcess.handleWrite]"             /*@BAA*/
                  + " file handle is invalid");                       /*@BAA*/
        return -1;                                                    /*@BAA*/
    }                                                                 /*@BAA*/

    FileDescriptor fd = fds[handle];                                  /*@BAA*/

    if (bufsize < 0) {                                                /*@BFA*/
        Lib.debug(dbgProcess, "[UserProcess.handleWrite]"             /*@BFA*/
                  + " bufsize is a negative number");                 /*@BFA*/
        return  -1;                                                   /*@BFA*/
    }                                                                 /*@BFA*/
    else if (bufsize == 0) {                                          /*@BFA*/
        Lib.debug(dbgProcess, "[UserProcess.handleWrite]"             /*@BFA*/
                  + " bufsize is zero");                              /*@BFA*/
        return 0;                                                     /*@BFA*/
    }                                                                 /*@BFA*/


    byte[] buf = new byte[bufsize];                                   /*@BAA*/  

    int bytesRead = readVirtualMemory(vaddr, buf);                    /*@BAA*/
    Lib.debug(dbgProcess, "vaddr: " + vaddr                           /*@BAA*/ 
                        + "\nbuf: "   + buf                           /*@BAA*/
                        + "\nbytesRead: "   + bytesRead);             /*@BAA*/

    if (bytesRead < 0) {                                              /*@BFA*/
        return -1;                                                    /*@BFA*/
    }                                                                 /*@BFA*/

    // invoke write through stubFilesystem                            /*@BAA*/
    retval = fd.file.write(buf, 0, bytesRead);                        /*@BHC*/

    if (retval < 0) {                                                 /*@BAA*/
        return -1;                                                    /*@BAA*/
    }                                                                 /*@BAA*/
    else {                                                            /*@BAA*/
        /* classOpenFileWithPostion will maintain a position                */
        /* fd.position = fd.position + retval;                        /*@BHD*/
        return retval;                                                /*@BAA*/
    }                                                                 /*@BAA*/
}

/**
 * Close a file descriptor, so that it no longer refers to any file or stream
 * and may be reused.
 *
 * If the file descriptor refers to a file, all data written to it by write()
 * will be flushed to disk before close() returns.
 * If the file descriptor refers to a stream, all data written to it by write()
 * will eventually be flushed (unless the stream is terminated remotely), but
 * not necessarily before close() returns.
 *
 * The resources associated with the file descriptor are released. If the
 * descriptor is the last reference to a disk file which has been removed using
 * unlink, the file is deleted (this detail is handled by the file system
 * implementation).
 *
 * Returns 0 on success, or -1 if an error occurred.
 */
private int handleClose(int a0) {                                     /*@BAA*/
    Lib.debug(dbgProcess, "handleClose()");                           /*@BAA*/
    
    int handle = a0;                                                  /*@BAA*/
    if (a0 < 0 || a0 >= MAXFD)                                        /*@BAA*/
        return -1;                                                    /*@BAA*/

    boolean retval = true;                                            /*@BAA*/

    FileDescriptor fd = fds[handle];                                  /*@BAA*/

    /*fd.position = 0;                                                  @BHD*/
    fd.file.close();                                                  /*@BAA*/
    fd.file = null;                                                   /*@BEA*/

    // remove this file if necessary                                  /*@BAA*/
    if (fd.toRemove) {                                                /*@BAA*/
        retval = UserKernel.fileSystem.remove(fd.filename);           /*@BAA*/
        fd.toRemove = false;                                          /*@BAA*/  
    }                                                                 /*@BAA*/

    fd.filename = "";                                                 /*@BAA*/

    return retval ? 0 : -1;                                           /*@BAA*/
}                                                                     /*@BAA*/

/**
 * Delete a file from the file system. If no processes have the file open, the
 * file is deleted immediately and the space it was using is made available for
 * reuse.
 *
 * If any processes still have the file open, the file will remain in existence
 * until the last file descriptor referring to it is closed. However, creat()
 * and open() will not be able to return new file descriptors for the file
 * until it is deleted.
 *
 * Returns 0 on success, or -1 if an error occurred.
 */
private int handleUnlink(int a0) {
    Lib.debug(dbgProcess, "handleUnlink()");

    boolean retval = true;

    // a0 is address of filename 
    String filename = readVirtualMemoryString(a0, MAXSTRLEN);         /*@BAA*/

    Lib.debug(dbgProcess, "filename: " + filename);                   /*@BAA*/

    int fileHandle = findFileDescriptorByName(filename);              /*@BAA*/ 
    if (fileHandle < 0) {                                             /*@BAA*/  
       /* invoke open through stubFilesystem, truncate flag is set to false
        * If no processes have the file open, the file is deleted immediately 
        * and the space it was using is made available for reuse.
        */
        retval = UserKernel.fileSystem.remove(filename);              /*@BAA*/
    }                                                                 /*@AA*/ 
    else {                                                            /*@BAA*/
        /* If any processes still have the file open, 
         * the file will remain in existence until the 
         * last file descriptor referring to it is closed.
         * However, creat() and open() will not be able to 
         * return new file descriptors for the file until 
         * it is deleted.
         */
        /* 
         * TODO: If any processes still have the file open, 
         * the file will remain in existence until the 
         * last file descriptor referring to it is closed.
         * 2/4/2014 HY
         */
        /* fds[fileHandle].toRemove = true;                             @BAA*/  
        handleClose(fileHandle);                                      /*@BEA*/
        retval = UserKernel.fileSystem.remove(filename);              /*@BEA*/
    }                                                                 /*@BAA*/

    return retval ? 0 : -1;                                           /*@BAA*/  
}
	

/* Find the first empty position in FD array               @BAA */
private int findEmptyFileDescriptor() {                 /* @BAA */
    for (int i = 0; i < MAXFD; i++) {                   /* @BAA */
        if (fds[i].file == null)                        /* @BAA */
            return i;                                   /* @BAA */
    }                                                   /* @BAA */

    return -1;                                          /* @BAA */
}                                                       /* @BAA */

/* Find the first empty position in FD array by filename   @BAA */
private int findFileDescriptorByName(String filename) { /* @BAA */
        
    for (int i = 0; i < MAXFD; i++) {                   /* @BAA */
        if (fds[i].filename.equals(filename))           /* @BEC */
            return i;                                   /* @BAA */
    }                                                   /* @BAA */

    return -1;                                          /* @BAA */
}                                                       /* @BAA */

/** The program being run by this process. */
protected Coff coff;

/** This process's page table. */
protected TranslationEntry[] pageTable;

/** The number of contiguous pages occupied by the program. */
protected int numPages;

/** The number of pages in the program's stack. */
protected final int stackPages = 8;

private int initialPC, initialSP;
private int argc, argv;

private static final int pageSize = Processor.pageSize;
private static final char dbgProcess = 'a';

/**
 * variables added by hy 1/18/2014 *
 */
public class FileDescriptor {                                 /*@BAA*/
    public FileDescriptor() {                                 /*@BAA*/ 
    }                                                         /*@BAA*/
    private  String   filename = "";   // opened file name    /*@BAA*/
    private  OpenFile file = null;     // opened file object  /*@BAA*/
    /*private  int      position = 0;  // IO position           @BHA*/

    private  boolean  toRemove = false;// if need to remove   /*@BAA*/
                                       // this file           /*@BAA*/
}                                                             /*@BAA*/

/* maximum number of opened files per process                       */
public static final int MAXFD = 16;                           /*@BAA*/

/* standard input file descriptor                                   */
public static final int STDIN = 0;                            /*@BAA*/ 

/* standard output file descriptor                                  */
public static final int STDOUT = 1;                           /*@BAA*/

/* maximum length of strings passed as arguments to system calls    */
public static final int MAXSTRLEN = 256;                      /*@BAA*/  

/* pid of root process(first user process)                          */
public static final int ROOT = 1;                             /*@BCA*/  

/* file descriptors per process                                     */
private FileDescriptor fds[] = new FileDescriptor[MAXFD];     /*@BAA*/   

/* number of opened files                                           */
private int cntOpenedFiles = 0;                               /*@BAA*/


                             	
	
	private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2,
			syscallJoin = 3, syscallCreate = 4, syscallOpen = 5,
			syscallRead = 6, syscallWrite = 7, syscallClose = 8,
			syscallUnlink = 9;

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 * 
	 * <table>
	 * <tr>
	 * <td>syscall#</td>
	 * <td>syscall prototype</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td><tt>void halt();</tt></td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td><tt>void exit(int status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td><tt>int  exec(char *name, int argc, char **argv);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td><tt>int  join(int pid, int *status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>4</td>
	 * <td><tt>int  creat(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>5</td>
	 * <td><tt>int  open(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>6</td>
	 * <td><tt>int  read(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>7</td>
	 * <td><tt>int  write(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>8</td>
	 * <td><tt>int  close(int fd);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>9</td>
	 * <td><tt>int  unlink(char *name);</tt></td>
	 * </tr>
	 * </table>
	 * 
	 * @param syscall the syscall number.
	 * @param a0 the first syscall argument.
	 * @param a1 the second syscall argument.
	 * @param a2 the third syscall argument.
	 * @param a3 the fourth syscall argument.
	 * @return the value to be returned to the user.
	 */
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
		case syscallHalt:
			return handleHalt();
		case syscallCreate:
	        /* the first argument is filename              @BAA*/
		    return handleCreate(a0); 

	    case syscallOpen:
	        /* the first argument is filename              @BAA*/
		    return handleOpen(a0);   

	    case syscallRead:
	        /* the first argument is filename              @BAA*/
	        /* the second argument is buf address          @BAA*/
	        /* the third argument is buf size              @BAA*/
		    return handleRead(a0, a1, a2); 
	         
	    case syscallWrite:
	        /* the first argument is filename              @BAA*/
	        /* the second argument is buf address          @BAA*/
	        /* the third argument is buf size              @BAA*/
		    return handleWrite(a0, a1, a2);

	    case syscallClose:
	        /* the first argument is file handle           @BAA*/
		    return handleClose(a0);

	    case syscallUnlink:
	        /* the first argument is filename              @BAA*/
		    return handleUnlink(a0);                     /*@BAA*/
		   

		default:
			Lib.debug(dbgProcess, "Unknown syscall " + syscall);
			Lib.assertNotReached("Unknown system call!");
		}
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
		case Processor.exceptionSyscall:
			int result = handleSyscall(processor.readRegister(Processor.regV0),
					processor.readRegister(Processor.regA0),
					processor.readRegister(Processor.regA1),
					processor.readRegister(Processor.regA2),
					processor.readRegister(Processor.regA3));
			processor.writeRegister(Processor.regV0, result);
			processor.advancePC();
			break;

		default:
			Lib.debug(dbgProcess, "Unexpected exception: "
					+ Processor.exceptionNames[cause]);
			Lib.assertNotReached("Unexpected exception");
		}
	}

}
