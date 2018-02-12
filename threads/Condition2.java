package nachos.threads;

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
		waitQueue = ThreadedKernel.scheduler.newThreadQueue(false);
	}

	/**
	 * Atomically release the associated lock and go to sleep on this condition
	 * variable until another thread wakes it using <tt>wake()</tt>. The current
	 * thread must hold the associated lock. The thread will automatically
	 * reacquire the lock before <tt>sleep()</tt> returns.
	 */
	public void sleep() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		
		boolean intStatus = Machine.interrupt().disable();
		
		conditionLock.release();
		
		waitQueue.waitForAccess(KThread.currentThread());
		
		KThread.sleep();
		
		conditionLock.acquire();
		
		Machine.interrupt().restore(intStatus);
	}

	/**
	 * Wake up at most one thread sleeping on this condition variable. The
	 * current thread must hold the associated lock.
	 */
	public void wake() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		boolean intStatus = Machine.interrupt().disable();
		KThread nextToGo = waitQueue.nextThread();
		if(nextToGo != null)
			nextToGo.ready();
		Machine.interrupt().restore(intStatus);
	}

	/**
	 * Wake up all threads sleeping on this condition variable. The current
	 * thread must hold the associated lock.
	 */
	public void wakeAll() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		boolean intStatus = Machine.interrupt().disable();
		//No isEmpty() interface for ThreadQueue. Of course we can do some cast or add some codes. 
		//However not necessarily now.
		KThread threadToWake = waitQueue.nextThread();
		while(threadToWake != null) {
			threadToWake.ready();
			threadToWake = waitQueue.nextThread();
		}
		Machine.interrupt().restore(intStatus);
	}
	
	private static class Producer implements Runnable {
			Producer(Condition2 cond, Lock lock, int[] val) {
				this.cond = cond;
				this.lock = lock;
				this.val = val;
			}
				
			public void run() {
				for(int i=0;i<12;i++){
				lock.acquire();
				while(val[0]>=4){
					System.out.println("Buffer full.");
					cond.sleep();
				}
				val[0]++;
				System.out.println("Produced an item!");
				cond.wake();
				lock.release();
				}
			}

			private Condition2 cond;
			private Lock lock;
			private int[] val;
	}
	 
	private static class Consumer implements Runnable {
		Consumer(Condition2 cond, Lock lock, int[] val) {
			this.cond = cond;
			this.lock = lock;
			this.val = val;
		}
			
		public void run() {
			for(int i=0;i<12;i++){
			lock.acquire();
			while(val[0]==0){
				System.out.println("No items - sleep time.");
				cond.sleep();
			}
			val[0]--;
			System.out.println("Got an item!");
			cond.wake();
			lock.release();
			}
		}

		private Condition2 cond;
		private Lock lock;
		private int[] val;
	}

		    /**
		     * Test if this module is working.
		     */
	public static void selfTest() {
		Lock lock = new Lock();
		Condition2 cond = new Condition2(lock);
		int[] val = {0};

		new KThread(new Producer(cond,lock,val)).setName("prod").fork();
		new Consumer(cond,lock,val).run();
	}
	
	private Lock conditionLock;
	private ThreadQueue waitQueue;
}
