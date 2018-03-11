 package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A scheduler that chooses threads based on their priorities.
 * 
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the thread
 * that has been waiting longest.
 * 
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has the
 * potential to starve a thread if there's always a thread waiting with higher
 * priority.
 * 
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
	/**
	 * Allocate a new priority scheduler.
	 */
	public PriorityScheduler() {
	}

	/**
	 * Allocate a new priority thread queue.
	 * 
	 * @param transferPriority <tt>true</tt> if this queue should transfer
	 * priority from waiting threads to the owning thread.
	 * @return a new priority thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}

	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getPriority();
	}

	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getEffectivePriority();
	}

	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());

		Lib.assertTrue(priority >= priorityMinimum
				&& priority <= priorityMaximum);

		getThreadState(thread).setPriority(priority);
	}

	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();
		boolean ret = true;

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			ret = false;
		else
			setPriority(thread, priority + 1);

		Machine.interrupt().restore(intStatus);
		return ret;
	}

	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();
		boolean ret = true;

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			ret = false;
		else
			setPriority(thread, priority - 1);

		Machine.interrupt().restore(intStatus);
		return ret;
	}

	/**
	 * The default priority for a new thread. Do not change this value.
	 */
	public static final int priorityDefault = 1;

	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMinimum = 0;

	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMaximum = 7;

	/**
	 * Return the scheduling state of the specified thread.
	 * 
	 * @param thread the thread whose scheduling state to return.
	 * @return the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class PriorityQueue extends ThreadQueue {
		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).acquire(this);
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me
			if(currentOwner != null) {
	              getThreadState(currentOwner).ownedResources.remove(this);
	        }
			
			ThreadState threadState = pickNextThread();
			if (threadState != null){
				threadState.acquire(this);
				currentOwner = threadState.thread;
				return currentOwner;
			}
			else 
				return null;
		}
		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 * 
		 * @return the next thread that <tt>nextThread()</tt> would return.
		 */
    protected ThreadState pickNextThread() {
			// implement me
				int priorityTemp = -1;
				KThread nextThread = null;
				for (KThread thread : waitQueue){
					if (getThreadState(thread).getEffectivePriority() > priorityTemp){
						priorityTemp = getThreadState(thread).getEffectivePriority();
						nextThread = thread;
					}
					if (priorityTemp == priorityMaximum)
						break;
				}
				if (nextThread == null)
					return null;
				return getThreadState(nextThread);
		}

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me (if you want)
		}
		
		 public  boolean empty() {
	          return (this.waitQueue.size() == 0);
	     }

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;
		
		private KThread currentOwner = null;
		
		LinkedList<KThread> waitQueue = new LinkedList<KThread>();

	}

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue it's
	 * waiting for, if any.
	 * 
	 * @see nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState {
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 * 
		 * @param thread the thread this state belongs to.
		 */
		public ThreadState(KThread thread) {
			this.thread = thread;
			this.ownedResources = new LinkedList<PriorityQueue>();
			setPriority(priorityDefault);
		}

		/**
		 * Return the priority of the associated thread.
		 * 
		 * @return the priority of the associated thread.
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 * 
		 * @return the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {
			// implement me
			// Initialize effective priority to actual priority
	        int effectivePriority = this.priority;
	        for(PriorityQueue q : this.ownedResources) {
	            // Only transfer priority if this queue allows priority to be transferred
	            if(q.transferPriority && !q.empty()) {
	              if(effectivePriority < q.pickNextThread().getEffectivePriority()) {
	                effectivePriority = q.pickNextThread().getEffectivePriority();
	              }
	            }
	          }
	          return effectivePriority;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 * 
		 * @param priority the new priority.
		 */
		public void setPriority(int priority) {
			if (this.priority == priority)
				return;

			this.priority = priority;

			// implement me
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the resource
		 * guarded by <tt>waitQueue</tt>. This method is only called if the
		 * associated thread cannot immediately obtain access.
		 * 
		 * @param waitQueue the queue that the associated thread is now waiting
		 * on.
		 * 
		 * @see nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue waitQueue) {
			// implement me
			Lib.assertTrue(Machine.interrupt().disabled());
			waitQueue.waitQueue.add(thread);
		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 * 
		 * @see nachos.threads.ThreadQueue#acquire
		 * @see nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue waitQueue) {
			// implement me
			waitQueue.waitQueue.remove(thread);
			getThreadState(thread).ownedResources.add(waitQueue);
			waitQueue.currentOwner = thread;
			
			
		}
		
		/** The thread with which this object is associated. */
		protected KThread thread;

		/** The priority of the associated thread. */
		protected int priority;
		
		protected LinkedList<PriorityQueue> ownedResources;
		
	}
	 private static class PingTest implements Runnable  
	    {  
	        Lock a=null,b=null;  
	        int name;  
	        PingTest(Lock A,Lock B,int x)  
	        {  
	            a=A;b=B;name=x;  
	        }  
	        public void run() {  
	            System.out.println("Thread "+name+" starts.");  
	            if(b!=null)  
	            {  
	                System.out.println("Thread "+name+" waits for Lock b.");  
	                b.acquire();  
	                System.out.println("Thread "+name+" gets Lock b.");  
	            }  
	            if(a!=null)  
	            {  
	                System.out.println("Thread "+name+" waits for Lock a.");  
	                a.acquire();  
	                System.out.println("Thread "+name+" gets Lock a.");  
	            }  
	            KThread.yield();  
	            boolean intStatus = Machine.interrupt().disable();  
	            System.out.println("Thread "+name+" has priority "+ThreadedKernel.scheduler.getEffectivePriority()+".");  
	            Machine.interrupt().restore(intStatus);  
	            KThread.yield();  
	            if(b!=null) b.release();  
	            if(a!=null) a.release();  
	            System.out.println("Thread "+name+" finishs.");  
	              
	        }  
	    }  
	      
	    public static void selfTest()  
	    {  
	        Lock a=new Lock();  
	        Lock b=new Lock();  
	          
	        LinkedList<KThread> qq=new LinkedList<KThread>();  
	        for(int i=1;i<=5;i++)  
	        {  
	            KThread kk=new KThread(new PingTest(null,null,i));  
	            qq.add(kk);  
	            kk.setName("Thread-"+i).fork();  
	        }  
	        for(int i=6;i<=10;i++)  
	        {  
	            KThread kk=new KThread(new PingTest(a,null,i));  
	            qq.add(kk);  
	            kk.setName("Thread-"+i).fork();  
	        }  
	        for(int i=11;i<=15;i++)  
	        {  
	            KThread kk=new KThread(new PingTest(a,b,i));  
	            qq.add(kk);  
	            kk.setName("Thread-"+i).fork();  
	        }  
	        KThread.yield();  
	        Iterator it=qq.iterator();  
	        int pp=0;  
	        while(it.hasNext())  
	        {  
	            boolean intStatus = Machine.interrupt().disable();  
	            ThreadedKernel.scheduler.setPriority((KThread)it.next(),pp+1);  
	            Machine.interrupt().restore(intStatus);  
	            pp=(pp+1)%6+1;  
	        }  
	    }  

}
