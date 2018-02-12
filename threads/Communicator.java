package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator {
	/**
	 * Allocate a new communicator.
	 */
	public Communicator() {
		mutex = new Lock();
		speaker = new Condition2(mutex);
		listener = new Condition2(mutex);
		waiting = new Condition2(mutex);
		buff = null;
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 * 
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 * 
	 * @param word the integer to transfer.
	 */
	public void speak(int word) {
		mutex.acquire();
		
		while(buff!=null){
			speaker.sleep();
		}
		buff=word;
		listener.wake();
		waiting.sleep();
		mutex.release();
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen() {
		mutex.acquire();
		while(buff == null) {
			listener.sleep();
		}
		int res = buff.intValue();
		buff = null;
		/* wake order doesn't matter (?) */
		waiting.wake();
		speaker.wake();
		mutex.release();
		return res;
	}
	private Condition2 speaker;
	private Condition2 listener;
	private Condition2 waiting;
	private Lock mutex;
	private Integer buff;
	
	private static class Speak implements Runnable {
		Speak(Communicator com, String name) {
		    this.com = com;
		    this.name = name;
		}

		public void run() {
		   
		    for (int i = 0; i < 2; i++) {
			com.speak(i);
			System.out.println(name + " says " + i);
		    }
		    System.out.println(name + " is done");
		}

		private Communicator com;
		private String name;
	    }

	    private static class Listen implements Runnable {
		Listen(Communicator com, String name) {
		    this.com = com;
		    this.name = name;
		}

		public void run() {
			int i = 0;
			if(name == "D") i = -2;
		    for ( ; i < 1; i++) {
			int heard = com.listen();
			System.out.println(name + " hears " + heard);
		    }
		    
		    System.out.println(name + " is done");
		}

		private Communicator com;
		private String name;
	    }

	    public static void selfTest() {
		Communicator com1 = new Communicator();
		
		KThread thread1 = new KThread(new Speak(com1, "A"));
		KThread thread2 = new KThread(new Listen(com1, "B"));
		KThread thread3 = new KThread(new Speak(com1, "C"));
		thread1.fork();
		thread2.fork();
		thread3.fork();
		//once D is done then the other people get cut off because he is the main thread which is done
		new Listen(com1, "D").run();
	    }
}
