package nachos.threads;


class ThreadWake implements Comparable<ThreadWake>{
	private KThread threadToWake;
	private long wakeTime;
	/**
	 * Constructor of ThreadWake -- thread with timer
	 */
	public ThreadWake(KThread kt, long time) {
		threadToWake = kt;
		wakeTime = time;
	}
	public KThread getThreadToWake() {
		return threadToWake;
	}
	public long getWakeTime() {
		return wakeTime;
	}
	@Override
	public int compareTo(ThreadWake anotherThreadWake) {
		return Long.compare(wakeTime, anotherThreadWake.wakeTime);
	}
	
}