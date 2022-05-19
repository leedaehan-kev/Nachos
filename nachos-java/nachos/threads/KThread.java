package nachos.threads;

//----- import 문 -------
import nachos.machine.*;
import java.util.Vector;
/* 채우세요 */            
//-----------------------    

/**
 * A KThread is a thread that can be used to execute Nachos kernel code. Nachos
 * allows multiple threads to run concurrently.
 *
 * To create a new thread of execution, first declare a class that implements
 * the <tt>Runnable</tt> interface. That class then implements the <tt>run</tt>
 * method. An instance of the class can then be allocated, passed as an
 * argument when creating <tt>KThread</tt>, and forked. For example, a thread
 * that computes pi could be written as follows:
 *
 * <p><blockquote><pre>
 * class PiRun implements Runnable {
 *     public void run() {
 *     }
 * }
 * </pre></blockquote>
 * <p>The following code would then create a thread and start it running:
 *
 * <p><blockquote><pre>
 * PiRun p = new PiRun();
 * new KThread(p).fork();
 * </pre></blockquote>
 */
public class KThread {                                // Nachos 커널 쓰레드 구현
    /**
     * Get the current thread.
     *
     * @return	the current thread.
     */
    public static KThread currentThread() {           // 현재 실행중인 커널 쓰레드를 반환하는 메소드 정의
        Lib.assertTrue(currentThread != null);
	    return currentThread;
    }
    
    /**
     * Allocate a new <tt>KThread</tt>. If this is the first <tt>KThread</tt>,
     * create an idle thread as well.
     */
    public KThread() {
        if (currentThread!=null) {                              // 최초로 생성된 쓰레드가 아닌 경우 (힌트 : currentThread 객체 이용하여 작성)
            tcb = new TCB();                               // 생성하고자 하는 커널 쓰레드에 대한 TCB 할당

            /* 채우세요 */                                 // joined SET 초기화
            joined_thread = new Vector();
        }	     
        
        else {                                                              // 최초로 생성된 쓰레드인 경우
            readyQueue = ThreadedKernel.scheduler.newThreadQueue(false);    // Ready Queue 생성
            joined_thread = new Vector();                                   // joined SET 초기화
            readyQueue.acquire(this);	                                    // Ready Queue에 해당 쓰레드 위치
            currentThread = this;                                           // 최초로 생성한 쓰레드를 바로 실행토록스케쥴링(currentThread)
	        tcb = TCB.currentTCB();                                         // 최초의 쓰레드에 대한 TCB 생성
	        name = "main";                                                  // 최초로 생성된 쓰레드를 Main 쓰레드로 명명
	        restoreState();
            createIdleThread();
        }
    }

    /**
     * Allocate a new KThread.
     *
     * @param	target	the object whose <tt>run</tt> method is called.
     */
    public KThread(Runnable target) {
	this();
	this.target = target;
    }

    /**
     * Set the target of this thread.
     *
     * @param	target	the object whose <tt>run</tt> method is called.
     * @return	this thread.
     */
    public KThread setTarget(Runnable target) {
	Lib.assertTrue(status == statusNew);
	
	this.target = target;
	return this;
    }

    /**
     * Set the name of this thread. This name is used for debugging purposes
     * only.
     *
     * @param	name	the name to give to this thread.
     * @return	this thread.
     */
    public KThread setName(String name) {
	this.name = name;
	return this;
    }

    /**
     * Get the name of this thread. This name is used for debugging purposes
     * only.
     *
     * @return	the name given to this thread.
     */     
    public String getName() {
	return name;
    }

    /**
     * Get the full name of this thread. This includes its name along with its
     * numerical ID. This name is used for debugging purposes only.
     *
     * @return	the full name given to this thread.
     */
    public String toString() {
	return (name + " (#" + id + ")");
    }

    /**
     * Deterministically and consistently compare this thread to another
     * thread.
     */
    public int compareTo(Object o) {
	KThread thread = (KThread) o;

	if (id < thread.id)
	    return -1;
	else if (id > thread.id)
	    return 1;
	else
	    return 0;
    }

    /**
     * Causes this thread to begin execution. The result is that two threads
     * are running concurrently: the current thread (which returns from the
     * call to the <tt>fork</tt> method) and the other thread (which executes
     * its target's <tt>run</tt> method).
     */
    public void fork() {                                  // 커널 쓰레드의 Fork 메소드 정의
        Lib.assertTrue(status == statusNew);
	    Lib.assertTrue(target != null);
	
	    Lib.debug(dbgThread,
		  "Forking thread: " + toString() + " Runnable: " + target);

	    boolean intStatus = Machine.interrupt().disable();

	    tcb.start(new Runnable() {
		    public void run() {
		        runThread();
		    }
	    });

	    ready();
	
	    Machine.interrupt().restore(intStatus);
    }

    private void runThread() {
	    begin();
	    target.run();
	    finish();
    }

    private void begin() {
	    Lib.debug(dbgThread, "Beginning thread: " + toString());
	
	    Lib.assertTrue(this == currentThread);

	    restoreState();

	    Machine.interrupt().enable();
    }

    /**
     * Finish the current thread and schedule it to be destroyed when it is
     * safe to do so. This method is automatically called when a thread's
     * <tt>run</tt> method returns, but it may also be called directly.
     *
     * The current thread cannot be immediately destroyed because its stack and
     * other execution state are still in use. Instead, this thread will be
     * destroyed automatically by the next thread to run, when it is safe to
     * delete this thread.
     */
    public static void finish() {
	    Lib.debug(dbgThread, "Finishing thread: " + currentThread.toString());
	
	    Machine.interrupt().disable();

	    Machine.autoGrader().finishingCurrentThread();

	    Lib.assertTrue(toBeDestroyed == null);
	    toBeDestroyed = currentThread;

	    currentThread.status = statusFinished;
	
	    sleep();
    }

    /**
     * Relinquish the CPU if any other thread is ready to run. If so, put the
     * current thread on the ready queue, so that it will eventually be
     * rescheuled.
     *
     * <p>
     * Returns immediately if no other thread is ready to run. Otherwise
     * returns when the current thread is chosen to run again by
     * <tt>readyQueue.nextThread()</tt>.
     *
     * <p>
     * Interrupts are disabled, so that the current thread can atomically add
     * itself to the ready queue and switch to the next thread. On return,
     * restores interrupts to the previous state, in case <tt>yield()</tt> was
     * called with interrupts disabled.
     */
    public static void yield() {
	    Lib.debug(dbgThread, "Yielding thread: " + currentThread.toString());
	
	    Lib.assertTrue(currentThread.status == statusRunning);
	
	    boolean intStatus = Machine.interrupt().disable();

	    currentThread.ready();

	    runNextThread();
	
	    Machine.interrupt().restore(intStatus);
    }

    /**
     * Relinquish the CPU, because the current thread has either finished or it
     * is blocked. This thread must be the current thread.
     *
     * <p>
     * If the current thread is blocked (on a synchronization primitive, i.e.
     * a <tt>Semaphore</tt>, <tt>Lock</tt>, or <tt>Condition</tt>), eventually
     * some thread will wake this thread up, putting it back on the ready queue
     * so that it can be rescheduled. Otherwise, <tt>finish()</tt> should have
     * scheduled this thread to be destroyed by the next thread to run.
     */
    public static void sleep() {
	    Lib.debug(dbgThread, "Sleeping thread: " + currentThread.toString());
	
	    Lib.assertTrue(Machine.interrupt().disabled());

	    if (currentThread.status != statusFinished)
	        currentThread.status = statusBlocked;

	    runNextThread();
    }

    /**
     * Moves this thread to the ready state and adds this to the scheduler's
     * ready queue.
     */
    public void ready() {
	    Lib.debug(dbgThread, "Ready thread: " + toString());
	
	    Lib.assertTrue(Machine.interrupt().disabled());
	    Lib.assertTrue(status != statusReady);
	
	    status = statusReady;
	    if (this != idleThread)
	        readyQueue.waitForAccess(this);
	
	    Machine.autoGrader().readyThread(this);
    }


    /**
     * Waits for this thread to finish. If this thread is already finished,
     * return immediately. This method must only be called once; the second
     * call is not guaranteed to return. This thread must not be the current
     * thread.
     */

    public void join() {                                              // join 을 호출한 KThread(커널 쓰레드) 종료될 때까지 대기시키는 메소드 정의
                                                                      // 즉, B 라는 KThread 에서, A.join()을 하는 경우, B Thread 는 A(Caller Thread) 가 종료할 때까지 대기  
	    Lib.debug(dbgThread, "Joining to thread: " + toString());
        Lib.assertTrue(this != currentThread);
 
        if((this!=currentThread) & (this.status==statusFinished)) return;                                     // Caller Thread가 이미 종료 상태인 경우, 대기 상태 진입 필요 없이 바로 리턴 
        
        //---------------------만약, join 해야할 쓰레드가 종료되지 않음 경우, 다음 단계로 진입-----------------------//
        /* 채우세요 */                                     // 인터럽트 끄기
	boolean intStatus = Machine.interrupt().disable();
        /* 채우세요 */                                     // 현재 실행 상태의 커널 쓰레드를 joined_set 에 저장 (FIFO 방식으로 저장)
	joined_thread.addElement((KThread)currentThread);
                                         
        currentThread.sleep();                             //  현재 실행 상태의 커널 쓰레드를 대기 상태로 전환
        Machine.interrupt().restore(intStatus);            // 인터럽트 켜기  
        //---------------------------------------------------------------------------------------------------------//
    }





    /**
     * Create the idle thread. Whenever there are no threads ready to be run,
     * and <tt>runNextThread()</tt> is called, it will run the idle thread. The
     * idle thread must never block, and it will only be allowed to run when
     * all other threads are blocked.
     *
     * <p>
     * Note that <tt>ready()</tt> never adds the idle thread to the ready set.
     */
    private static void createIdleThread() {
	    Lib.assertTrue(idleThread == null);
	
	    idleThread = new KThread(new Runnable() {
	        public void run() { while (true) yield(); }
	    });
	    idleThread.setName("idle");

	    Machine.autoGrader().setIdleThread(idleThread);
	
	    idleThread.fork();
    }
    
    /**
     * Determine the next thread to run, then dispatch the CPU to the thread
     * using <tt>run()</tt>.
     */
    private static void runNextThread() {
	    KThread nextThread = readyQueue.nextThread();
	    if (nextThread == null)
	        nextThread = idleThread;

	    nextThread.run();
    }

    /**
     * Dispatch the CPU to this thread. Save the state of the current thread,
     * switch to the new thread by calling <tt>TCB.contextSwitch()</tt>, and
     * load the state of the new thread. The new thread becomes the current
     * thread.
     *
     * <p>
     * If the new thread and the old thread are the same, this method must
     * still call <tt>saveState()</tt>, <tt>contextSwitch()</tt>, and
     * <tt>restoreState()</tt>.
     *
     * <p>
     * The state of the previously running thread must already have been
     * changed from running to blocked or ready (depending on whether the
     * thread is sleeping or yielding).
     *
     * @param	finishing	<tt>true</tt> if the current thread is
     *				finished, and should be destroyed by the new
     *				thread.
     */
    private void run() {
	    Lib.assertTrue(Machine.interrupt().disabled());

	    Machine.yield();

	    currentThread.saveState();

	    Lib.debug(dbgThread, "Switching from: " + currentThread.toString()
		  + " to: " + toString());

	    currentThread = this;

	    tcb.contextSwitch();

	    currentThread.restoreState();
    }

    /**
     * Prepare this thread to be run. Set <tt>status</tt> to
     * <tt>statusRunning</tt> and check <tt>toBeDestroyed</tt>.
     */
    protected void restoreState() {                                          // 해당 커널 쓰레드를 실행상태로 전환 (따라서, currentThread == this)시키는 메소드 정의
	    Lib.debug(dbgThread, "Running thread: " + currentThread.toString());
	
	    Lib.assertTrue(Machine.interrupt().disabled());
	    Lib.assertTrue(this == currentThread);
	    Lib.assertTrue(tcb == TCB.currentTCB());

	    Machine.autoGrader().runningThread(this);
	
	    /* 채우세요 */              // 현재 쓰레드의 상태를 실행 상태로 전환
	    status = statusRunning;

	    if (toBeDestroyed != null) {    // 종료되어야 할 커널 쓰레드가 있는지 확인
        
            while(!toBeDestroyed.joined_thread.isEmpty() && !(toBeDestroyed.getName().equals("main"))) {
                // 종료되어야할 커널 쓰레드는 자신을 기다리는 쓰레드(joined SET)들이 있는지 확인 
                // 있다면, 반드시, 해당 커널 쓰레드의 joined SET 에 있는 모든 쓰레드들을 wake up 해줘야 함
             
                KThread tmpThread;                                            
                tmpThread = (KThread)toBeDestroyed.joined_thread.get(0);     // joined SET 에서 Head 위치의 커널 쓰레드를 가져옴
                toBeDestroyed.joined_thread.remove(0);                       // joined SET 에서 Head 위치의 커널 쓰레드를 가져옴
                tmpThread.ready();                                           // joined SET 에서 Head 위치의 커널 쓰레드를 가져와 준비 상태로 전환
            	}
 
	        /* 채우세요 */                                     // 종료되어야 할 커널 쓰레드의 TCB 제거 (힌트 machine/TCB.java 참고)
		toBeDestroyed.tcb.destroy();
	        toBeDestroyed.tcb = null;
	        toBeDestroyed = null;
	    }
    }

    /**
     * Prepare this thread to give up the processor. Kernel threads do not
     * need to do anything here.
     */
    protected void saveState() {
	    Lib.assertTrue(Machine.interrupt().disabled());
	    Lib.assertTrue(this == currentThread);
    }

    private static class PingTest implements Runnable { // KThread join 을 제대로 구현했는지 확인하는 Test 코드입니다. 수정하지마세요.
	    PingTest(int which) {
	        this.which = which;
	    }
	
	    public void run() {
	        for (int i=0; i<5; i++) {
		        System.out.println("*** thread " + which + " looped "
                + i + " times");
                currentThread.yield();
	        }
	    }
        
        private int which;
    }

    /**
     * Tests whether this module is working.
     */
    public static void selfTest() {                      // KThread join 을 제대로 구현했는지 확인하는 Test 코드입니다. 수정하지마세요.
	    Lib.debug(dbgThread, "Enter KThread.selfTest");
	
	    new KThread(new PingTest(1)).setName("forked thread").fork();
	    new PingTest(0).run();

        KThread.joinTest1();          // this is a test code for join test
    }

    private static void joinTest1() {                    // KThread join 을 제대로 구현했는지 확인하는 Test 코드입니다. 수정하지마세요.
        KThread child1 = new KThread(new Runnable() {
            public void run() { System.out.println("I (heart) Nachos!"); }
        });      // for testing thread which has routine of printing "I (heart) Nachos!"

        child1.setName("child1").fork();    // create this thread and run it!!
        
        for(int i=0; i<5;i++){
            System.out.println("busy...");
            KThread.currentThread().yield();
        } 
        
        child1.join();                       // now testing thread(parent of child1) joins child1
        System.out.println("After Joining, child1 should be finished");
        System.out.println("is it? " + (child1.status==statusFinished));
        Lib.assertTrue((child1.status==statusFinished), "Expected child1 to be finished.");
    }

    private static final char dbgThread = 't';

    /**
     * Additional state used by schedulers.
     *
     * @see	nachos.threads.PriorityScheduler.ThreadState
     */
    public Object schedulingState = null;

    private static final int statusNew = 0;                // 커널 쓰레드 : 생성 상태 
    private static final int statusReady = 1;              // 커널 쓰레드 : 준비 상태 
    private static final int statusRunning = 2;            // 커널 쓰레드 : 실행 상태 
    private static final int statusBlocked = 3;            // 커널 쓰레드 : 대기 상태 
    private static final int statusFinished = 4;           // 커널 쓰레드의 종료 상태 

    /**
     * The status of this thread. A thread can either be new (not yet forked),
     * ready (on the ready queue but not running), running, or blocked (not
     * on the ready queue and not running).
     */

    private int status = statusNew;                      // 현재(this) 커널 쓰레드의 상태
    private String name = "(unnamed thread)";
    private Runnable target;
    private TCB tcb;                                     // 현재 실행 중인 커널 쓰레드의 TCB

    /**
     * Unique identifer for this thread. Used to deterministically compare
     * threads.
     */
    private int id = numCreated++;
    /** Number of times the KThread constructor was called. */
    private static int numCreated = 0;

    /* 채우세요 */                                       // Ready 상태의 쓰레드들에 대한 큐(ThreadQueue 클래스 이용) 정의
    private static ThreadQueue readyQueue = null; 
    private static KThread currentThread = null;         // 현재 스케쥴링된, 실행 상태의 커널 쓰레드(KThread)            
    private static KThread toBeDestroyed = null;
    private static KThread idleThread = null;
    private Vector joined_thread=null;                   // 해당 커널 쓰레드(this)가 끝날때까지 대기상태에 있는 커널 쓰레드들에 대한 큐(joined SET) 정의
}
