package nachos.threads;

import java.util.PriorityQueue;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
		waitingPQ = new PriorityQueue<ThreadWake>();
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
		boolean intStatus = Machine.interrupt().disable();
    	
	    	while (waitingPQ.peek() != null &&  Machine.timer().getTime() >= waitingPQ.peek().getWakeTime()) {
	    		waitingPQ.poll().getThreadToWake().ready();
	    	}
    	
    		Machine.interrupt().restore(intStatus);
		KThread.yield();
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		// for now, cheat just to get something working (busy waiting is bad)
		long wakeTime = Machine.timer().getTime() + x;
		ThreadWake toAdd = new ThreadWake(KThread.currentThread(), wakeTime);
		boolean intStatus = Machine.interrupt().disable();
		waitingPQ.add(toAdd);
		KThread.sleep();
		Machine.interrupt().restore(intStatus);
	}
	private PriorityQueue<ThreadWake> waitingPQ;
	
	private static class PingAlarmTest implements Runnable {
		PingAlarmTest(int which, Alarm alarm) {
			this.which = which;
			this.alarm = alarm;
			
		}
		Alarm alarm;

		public void run() {
			System.out.println("thread " + which + " started.");
			alarm.waitUntil(which);
			System.out.println("Current Time: " + Machine.timer().getTime());
			System.out.println("thread " + which + " ran.");
			
		}

		private int which;
	}
	
	public static void selfTest() {
		Alarm myAlarm = new Alarm();

		System.out.println("*** Entering Alarm self test");
		KThread thread1 = new KThread(new PingAlarmTest(1000,myAlarm));
		thread1.fork();

		KThread thread2 = new KThread(new PingAlarmTest(500,myAlarm));
		thread2.fork();

		new PingAlarmTest(2000,myAlarm).run();


		System.out.println("*** Exiting Alarm self test");
	}
		

}
