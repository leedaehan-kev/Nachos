package nachos.threads;

import nachos.machine.*;

import java.util.LinkedList;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue; 

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {           // 우선순위 스케쥴러를 구현한 Class

	public PriorityScheduler() {}                            // 새로운 우선순위 스케쥴러를 생성

	/**
	 * Allocate a new priority thread queue.
	 *
	 * @param transferPriority 생성하고자 하는 Queue 로부터 우선순위 양도가 가능한지 여부를 나타냄
	 * @return 새로이 생성된 Priority Queue
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {    // 새로운 우선순위 스케쥴링 큐를 생성
		return new PriorityThreadQueue(transferPriority);
	}

    /**
	 * 주어진 쓰레드에 부여된 '기본 우선순위' 를 반환
	 *
	 * @param thread 
	 * '기본적으로 부여된 우선순위'를 파악하고자하는 쓰레드
	 * 
	 * @return 해당 쓰레드에 부여된 '기본 우선순위'
	 */
	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());
		return getThreadState(thread).getPriority();
	}

    /**
	 * 주어진 쓰레드의 실질적인 우선순위(Effective Priority, EP)를 반환
	 *
	 * @param thread 
	 * '실질적 우선순위'를 파악하고자하는 쓰레드
	 * 
	 * @return 해당 쓰레드의 '실질적 우선순위'
	 */
	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());
		return getThreadState(thread).getEffectivePriority(); // 해당 KThread 의 Scheduling 상태 정보를 통해, 실질적 우선순위 조회
	}

    /**
	 * 주어진 KThread 의 '기본 우선순위'(Effective Priority, EP)를 설정함과 동시에, 해당 KThread 의 EP 를 재계산
	 *
	 * @param thread 
	 * '기본적 우선순위' 를 설정하고자 하는 KThread
	 * @param priority
	 * 설정하고자 하는 우선순위 값
	 * @return Nonde
	 */
	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());

		Lib.assertTrue(priority >= priorityMinimum && priority <= priorityMaximum);

		getThreadState(thread).setPriority(priority);
	}

	/*  해당 Method 의 역할(인자, 반환값, 정의 목적)과 자세한 실행 로직(알고리즘)을 보고서에 적으시오. */
	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();                          

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			return false;

		setPriority(thread, priority+1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	/*  해당 Method 의 역할(인자, 반환값, 정의 목적)과 자세한 실행 로직(알고리즘)을 보고서에 적으시오. */
	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			return false;

		setPriority(thread, priority-1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	/**
	 * 새로이 생성된 KThread 에 부여되는 '기본 우선순위'의 Default 값
	 */
	public static final int priorityDefault = 1;
	/**
	 * 임의의 KThread 에 부여되는 최소(높은 우선순위 의미) '기본 우선순위' 값 
	 */
	public static final int priorityMinimum = 0;
	/**
	 * 임의의 KThread 에 부여되는 최소(낮은 우선순위 의미) '기본 우선순위' 값 
	 */
	public static final int priorityMaximum = 7;

	/**
	 * 특정 KThread의, Scheduling 상태를 반환 (PriorityScheduler 에 의해 호출)
	 *
	 * @param thread Scheduling 상태를 알고자하는 특정 KThread
	 * @return 특정 KThread의, Scheduling 상태
	 */
	protected ThreadState getThreadState(KThread thread) {

		if (thread.schedulingState == null)                        // 특정 KThread 의 Scheduling 상태 정보가 없는 경우
			thread.schedulingState = new ThreadState(thread);      // -> 해당 KThread에 대한 Scheduling 상태 정보 초기화

		return (ThreadState) thread.schedulingState;               // 해당 KThread 의 Scheduling 상태 정보 반환
	}
	
	
	// ------------- PriorityScheduler 구현의 완성도를 평가하기 위한 테스트 코드 입니다. ----------------
	public static void selfTest(){
		final Lock myLock = new Lock();
		final Condition2 myCond = new Condition2(myLock);
		
		KThread T1 = new KThread(new Runnable(){
			public void run(){
				System.out.println("T1 is forked at : "+Machine.timer().getTime());
            			// 우선순위가 작은 T1이 Lock을 쥔 채로 sleep
            			myLock.acquire();
                		System.out.println("T1: acquire호출 , Lock 소유중 : " +Machine.timer().getTime());
                		myCond.sleep();
                		System.out.println("T1 woke up : " +Machine.timer().getTime());
				myLock.release();
        			System.out.println("T1 is finished at : "+Machine.timer().getTime());
        	}}).setName("T1");
		
		
		KThread T2 = new KThread(new Runnable(){
			public void run(){
				System.out.println("T2 is forked at : "+Machine.timer().getTime());
				//T2가 Lock을 요청
            			myLock.acquire();
            			System.out.println("T2: acquire호출 , Lock 소유중 : "+Machine.timer().getTime());
                		myCond.wake();
                		System.out.println("T2 call wake() : " + +Machine.timer().getTime());
				myLock.release();
        			System.out.println("T2 is finished at : "+Machine.timer().getTime());
        	}}).setName("T2");
		
		
		T1.fork();
		//Lock을 쥐고있는 T1 스레드에 낮은 우선순위를 부여
		((ThreadState) T1.schedulingState).setPriority(3);
		
		T2.fork();
		T2.join();
		((ThreadState) T2.schedulingState).setPriority(5);
	}
	// --------------------------------------------------------------------------------------------------


	// --------------------- KThread들을 우선순위에 따라 정렬하기 위한 Queue ----------------------------
	protected class PriorityThreadQueue extends ThreadQueue {     

		PriorityThreadQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
		}

	/**
	 * 현재 상태의 priorityQueue 에 접근하고자 하는, KThread
	 *
	 * @param thread 현재 상태의 priorityQueue 에 접근하고자 하는, KThread
	 */
		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
		}
	/**
	 * 현재 상태의 priorityQueue 에 접근하고자 하는, KThread
	 *
	 * @param thread 현재 상태의 priorityQueue 에 접근하고자 하는, KThread
	 */
		public void acquire(KThread thread) {     
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).acquire(this);                       
		}

		/* 해당 Method 의 역할(인자, 반환값, 정의 목적)과 자세한 실행 로직을 보고서에 적으시오. */
		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			ThreadState threadState = this.pickNextThread();         // 종합적 우선순위가 가장 높은 KThread 의 Scheduling 상태 정보 조회

			priorityQueue.remove(threadState);                       // priorityQueue 에서, 해당 KThread(종합적 우선순위가 가장 높은 KThread) 의 Scheduling 상태 정보 제거
			
			if (transferPriority && threadState != null) {           
				this.dequeuedThread.removeQueue(this);               // 해당 KThread 의 Scheduling 상태 정보에서, 해당 KThread 가 제거된 priorityQueue 를 제거
				threadState.acquire(this);                           // 해당 KThread 가 현재의 Priority Queue 에 대한 접근권한을 획득 
				threadState.waiting = null;                          // 해당 KThread 가 Run 되도록 선택되었으므로, 기존에 준비 상태로 대기하고 있던 waiting Queue 에 대한 정보는 필요 없음 
				threadState.addQueue(this);
				/* 채우세요. */                                      // 현재 상태의 priorityQueue 를 Donation Queue 리스트에 append (라애에 정의된 addQueue 메소드 사용할 것)
			}

			this.dequeuedThread = threadState;                       // 가장 최근에, priorityQueue 에서 제거된 KThread 의 Scheduling 상태 정보를 설정  

			if (threadState == null){                                // 만약, priorityQueue 가 비어있었다면, 새로운 priorityQueue 를 생성
				this.priorityQueue = new PriorityQueue<ThreadState>();  
				return null;                                             
			}
			return threadState.thread;
		}

		/**
		 * priorityQueue 에서, 종합적 우선순위가 가장 높은 KThread 의 Scheduling 상태 정보를 반환 (다음으로 실행할 KThread)
		 * 이때 기존의 priorityQueue 의 상태를 변화시키지 않음
		 * 즉, 우선순위가 가장 높은 KThread 의 Scheduling 상태 정보를 가져오되, 이를 priortyQueue 에서 삭제하지는 않음
		 * @return 종합적 우선순위가 가장 높은 KThread 의 Scheduling 상태 정보
		 */
		protected ThreadState pickNextThread() {
			boolean intStatus = Machine.interrupt().disable();

			// 바로 이전의 priorityQueue 를, 우선순위에 기반하여 재정렬
			// 이는, priorityQueue 내의 KThread 들의 Scheduling 상태 정보들이 제대로 정렬됬는지 확인하기 위함
			this.priorityQueue = new PriorityQueue<ThreadState>(priorityQueue); 
			
			Machine.interrupt().restore(intStatus);

            // priorityQueue 의 가장 첫원소(종합 우선순위가 가장 높은 KThread 의 Scheduling 상태 정보)를 가져옴 
			// (이때, 가져오는 Scheduling 상태 정보는 priorityQueue 에서 삭제하지 않음, peek() 메소드 참조)
			return this.priorityQueue.peek(); 

		}

		/* 추상 메소드, 구현하실 필요 없습니다. */
		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me (if you want)
		}

		// KThread 들의 Scheduling 상태 정보들을, 우선순위에 따라 정렬한 Queue 정의
		protected PriorityQueue<ThreadState> priorityQueue = new PriorityQueue<ThreadState>();   

		protected ThreadState dequeuedThread = null;  
		// 가장 최근에 priorityQueue 에서 제거된 KThread 에 대한 Scheduling 상태 정보
		// 가장 최근에 priorityQueue 에서 제거된 KThread(가장 최근에 실행된 KThread)는 공유자원에 대한 Lock 을 소유하고 있다고 가정함

		public boolean transferPriority;
		/* 채우세요. */      // 우선순위 양도가 가능한지 여부를 나타내는 상태 변수(public boolean 변수) 정의
	}
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * KThread 의 스케쥴링 상태를 의미. KThread의 스케쥴링의 상태는 다음과 같은 정보들을 포함
	 * -> 해당 KThread 의 기본 우선순위 (Priority)
	 * -> 해당 KThread 의 실질적 우선순위 (EP, Effective Priority)
	 * ->  
	 any objects it owns, and the queue
	 * 
	 * it's waiting for, if any.
	 *
	 * @see nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState implements Comparable<ThreadState> {
		/**
		 * 특정 KThread 의 스케쥴링 상태 정보들을 초기화 
		 * @param thread 해당 스케쥴링 상태 정보들을 소유하고 있는 KThread
		 */
		public ThreadState(KThread thread) {
			this.thread = thread;                                   // 현재 Scheduling 상태 정보의 소유 KThread 지정                         
			//initialize the onQueue linkedlist
			this.onQueues = new LinkedList<PriorityThreadQueue>();
			this.age = Machine.timer().getTime();                   // 현재까지 경과된 시간을 구함 (Aging 시 사용)
			this.effectivePriority = priorityDefault;               // KThread 의 Scheduling 상태 정보 초기화 시, 최초의 실질적 우선순위(EP) 는 1
			this.waiting = null;

			setPriority(priorityDefault);                           // 현재 KThread 의 우선순위를, 기본값(1) 로 설정 (+ EP 계산)
		}

		/**
		 * 해당 KThread 에 부여된 우선순위를 반환 
		 *
		 * @return 해당 KThread 에 부여된 우선순위
		 */
		public int getPriority() {                                             
			return priority;
		}

		/**
		 * 특정 KThread 의 실질적 우선순위와, 현재 공유자원에 대한 Lock을 Holding 하고 있는 KThread 의 EP를 계산
         * @return None
		 */
		public void calcEffectivePriority() {
			int initialPriority = this.getPriority();                             // 현재 KThread의, '기본적 우선순위'를 가져옴 
			int maxEP = -1;                                                       // 현재 KThread의 가능한 실질적 우선순위의 최대치에 대한 정의
        
			if (onQueues.size() != 0){                                            // onQueues 에 저장된
				int size = onQueues.size();

				for(int i = 0; i < size; i++){                                    // 
					PriorityThreadQueue current = onQueues.get(i);
					ThreadState donator = current.pickNextThread();               // 현재 Priority Queue 에서, 종합적 우선순위가 가장 높은 KThread의 Scheduling 상태 정보를 조회
					if (donator != null){
						if ((donator.getEffectivePriority() > maxEP) && current.transferPriority)   
							maxEP = donator.getEffectivePriority();               // maxEP(실질적 우선순위의 최대치) 를, 양도자의 EP 로 설정
					}
				}
			}
			if (initialPriority > maxEP){                                         // 만일, '기본적 우선순위' 가, 양도자로부터 양도받을 수 있는 EP 보다 더 크다면, maxEP(실질적 우선순위의 최대치) 는 '기본적 우선순위로 설정'
				maxEP=initialPriority;
				/* 채우세요. */                        
			}
			this.effectivePriority = maxEP;                                       // 현재 KThread 의 EP 를, maxEP(실질적 우선순위의 최대치) 로 설정         

			if (this.waiting != null && this.waiting.dequeuedThread != null){
				if (this.effectivePriority != this.waiting.dequeuedThread.effectivePriority){
					this.waiting.dequeuedThread.calcEffectivePriority();          // 공유자원에 대한 Lockholder KThread 에 대한 EP 계산
				}
			};
			
		}

		/**
		 * 해당 KThread 의 '실질적 우선순위'를 반환 
		 *
		 * @return 해당 KThread 에 부여된 '실질적 우선순위'
		 */
		public int getEffectivePriority() {
			return this.effectivePriority;
		}

		/**
		 *  현재 KThread 에 '기본적 우선순위' 값을 설정하소 동시에, EP 를 재계산
		 *
		 * @param priority 특정 KThread 에 부여할 '기본적 우선순위'
		 */
		public void setPriority(int priority) {
			
			this.priority = priority;                // 특정 KThread 의 '기본 우선순위' 값을 설정
			this.calcEffectivePriority();            // '기본 우선순위' 값이 변경되었으므로, 이에 기반하여, 실질적 우선순위 재 계산
			if(this.waiting != null && this.waiting.dequeuedThread != null)
				this.waiting.dequeuedThread.calcEffectivePriority();
			
			System.out.println(this.thread.getName()+" has "+this.getEffectivePriority()+" priority : "+Machine.timer().getTime());
		}

		/** 
		 * 해당 메소드는, 반드시 현재 KThread 가, 주어진 priorityQueue 와 관련있는 공유 자원에 즉시 접근이 불가능할 경우에만 호출되어야 함
		 * @param waitQueue 현재 KThread가 접근을 하기 위해 대기하고 있는 PriorityThreadQueue 
		 * @see nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityThreadQueue waitQueue) {
			Lib.assertTrue(Machine.interrupt().disabled());
			// ---------- Aging Time 갱신 -----------
			long time = Machine.timer().getTime();
			this.age = time;               
			// --------------------------------------

			waitQueue.priorityQueue.add(this); // 
			this.waiting = waitQueue;          // 현재 KThread 는 해당 priorityQueue(waitQueue) 에 접근하기 위해 대기 중 (즉, waitQueue 에 대해 대기 중임)
			this.calcEffectivePriority();      // 현재 KThread 의 EP 를 재계산
		}

		/** 
		 * 해당 메소드는, 반드시 현재 KThread 가, 주어진 priorityQueue 와 관련있는 공유 자원에 접근이 가능한 경우에 호출
		 * (PriorityThreadQueue.acquire() 혹은, nextThread() 메소드에 의해서만 호출됨)
		 * @param waitQueue 현재 KThread가 접근이 가능해진 PriorityThreadQueue 
		 * @see nachos.threads.ThreadQueue#waitForAccess
		 */
		public void acquire(PriorityThreadQueue waitQueue) {
			Lib.assertTrue(Machine.interrupt().disabled());
			Lib.assertTrue(waitQueue.priorityQueue.isEmpty());          
			
			waitQueue.priorityQueue.remove(this);
			waitQueue.dequeuedThread = this;
/* 채우세요 */;                            // priorityQueue(waitQueue) 에서 막 제거된 KThread를, 현재 KThread(waitQueue 에 대한 접근을 허락받은 KThread) 로 설정 
			this.addQueue(waitQueue);                                   // 현재 상태의 priorityQueue(waitQueue) 를, onQueues에 추가
			this.calcEffectivePriority();                               // 현재 KThread 의 EP 를 재 계산
		}


		/*
		두개의 ThreadState(Scheduling 상태 정보)를 어떤 기준으로 비교할 것인가?
		* => 실질적 우선순위를 비교 (실질적 우선순위가 더 높은 KThread 가, 최종 우선순위가 가장 높음)
		* => 만일, 두 KThread 의 실질적 우선순위가 같은 경우, 두 KThread 의 Aging Time을 기반으로 하여 비교 
		*    (Aging 이 더 오래된 KThread 가, 최종 우선순위가 가장 높음)
		*/
		public int compareTo(ThreadState threadState){          // 우선순위 비교를 위하여, compareTo 메소드 Override
		    /*
			두개의 ThreadState(Scheduling 상태 정보)를 어떤 기준으로 비교할 것인가?
			=> 실질적 우선순위를 비교 
			=> 만일, 두 KThread 의 실질적 우선순위가 같은 경우, 두 KThread 의 Aging Time을 기반으로 하여 비교 
			*/
			//changed first if from > to <

			if (threadState == null)                            
				return -1;

			if (this.getEffectivePriority()<threadState.getEffectivePriority()){
				return 1;                                // this KThread 의 우선순위가, 특정 Kthread 의 우선순위보다 높음
			}
			else{ 
				if (this.getEffectivePriority() > threadState.getEffectivePriority()){
					return -1;                           // this KThread 의 우선순위가, 특정 Kthread 의 우선순위보다 낮음
				}
				
				else{
					if (this.age >= threadState.age) 
						return 1;
					else { 
						return -1; 
					}
				}
			}
		}

		

		/**
		 * Donation Queue 들에서, 특정 PriorityQueue 를 제거하고자 하는 경우 호출
		 * @param queue 
		 * Donation Queue 들에서 제거하고자 하는 PriorityQueue
		 */
		public void removeQueue(PriorityThreadQueue queue){
			onQueues.remove(queue);
			this.calcEffectivePriority();                        // 
		}

		/**
		 * Donation Queue 들에, 특정 PriorityQueue 를 추가하고자 하는 경우 호출
		 * @param queue 
		 * Donation Queue 들에 추가하고자 하는 PriorityQueue
		 */
		public void addQueue(PriorityThreadQueue queue){
			onQueues.add(queue);
			this.calcEffectivePriority();
		}

		/*
		* 특정 KThread 의 Scheduling 상태 정보를 표현하기 위한 toString() 메소드 Override
		* */
		public String toString() {
			return "ThreadState thread=" + thread + ", priority=" + getPriority() + ", effective priority=" + getEffectivePriority();
		}

		protected KThread thread;                 // 현재 Scheduling 상태 정보를 소유하고 있는 KThread
		protected int priority = priorityDefault; // 현재 KThread 의 '기본 우선순위'
		public long age = Machine.timer().getTime();  // Nachos 시간에 대하여, 현재 KThread 의 Aging 시간

		
		protected LinkedList<PriorityThreadQueue> onQueues;     /* 해당 Member Variable 의 역할(정의 목적) 은? */ 

		protected int effectivePriority;                        // 현재 KThread 의 '실질적 우선순위'
		protected PriorityThreadQueue waiting;                  // 현재 KThread 가 접근하기 위해 대기 중인 Priority Queue
	}
}


/*
(35%, 125 lines) 
PriorityScheduler 클래스 구현을 통해, 우선순위 기반 스케쥴링을 완성하세요.
우선순위 기반 스케쥴링은, Real-Time OS 구현을 위한 핵심 요소입니다.

완성된 여러분들만의 우선순위 스케쥴러를 테스팅/사용해보기 위해선, 반드시, proj1 디렉토리의 nachos.conf 파일에 사용할 스케쥴러로, 
우선순위 스케쥴러를 명시해줘야합니다. 기본적으로, ThreadedKernel 은 스케쥴러로, 라운드-로빈 스케쥴러입니다.

우선순위 스케쥴러를 완성하기 이전에, 몇가지, 알고가셔야할 것을 말씀드리겠습니다. 
Nachos의 모든 종류의 스케쥴러는, 반드시, nachos.threads.Scheduler 를 상속받아야합니다.

또한, getPriority(), getEffectivePriority(), setPriority() 메소드를 구현하셔야합니다. 
또한 필수는 아니지만, increasePriority(), decreasePriority() 메소드는 선택적으로 구현하시면 됩니다.

어떤 쓰레드를, Queue 에서 먼저 제거할지(스케쥴링할지)는, 각 쓰레드의 EP( Effective Priority)를 비교하시고, 최고 EP 를 가진 쓰레드를 우선적으로 스케쥴링 하시면 됩니다.
만약, 두개 이상의 쓰레드들이 같은 EP 를 가지고 있는 경우, Aging 기법을 적용하여, Scheduling Queue 애서 가장 오랫동안 대기하고 있는 쓰레드를 우선적으로 스케쥴링하시면 됩니다.

동시에, 스케쥴러 구현 시, 우선순위 기반 스케쥴링에서 발생할 수 있는 우선순위 역전 문제(Priority inversion)를 고려하셔야 합니다.
만약, 이런 경우를 생각해봅시다. 총 3개의 쓰레드가 준비큐에 있습니다. 이들 중, 가장 낮은 우선순위의 쓰레드(A라고 해봅시다)가 어떤 자원에 대해 Lock을 점유하고있고, 
우선순위가 가장 높은 B 쓰레드가 해당 Lock이 반환되기를 기다리고 있습니다. 그리고, 두번째로 우선순위가 높은 C 쓰레드가 있습니다. 이와 같은 경우, A 쓰레드가 가장 높은 우선순위를
가지고 있다하더라도, 우선순위가 가장 낮은 B 쓰레드가 Lock 을 반환할 때까지 대기해야하고, 동시에 B 쓰레드는 우선순위가 가장 낮기 때문에 A 쓰레드는 가장 나중에 실행될 가능성이 큽니다.
(분명히, A 쓰레드의 우선순위는 높은데도 말이죠!)  

이러한 문제에 대한 부분적인 해결책은, 자신보다 우선순위가 낮은 쓰레드(A)가, 우선순위가 높은 쓰레드(B)가 필요로하는 Lock 을 점유하고 있는 경우, 
자신(B)의 우선순위를 해당 쓰레드(A)에게 양보하는 것입니다.
이러한 해결책(우선순위 양보기능,Donation)을 적용한 스케쥴러를 구현해보시기 바랍니다.

종합적으로, 기본적으로 부여된 우선순위 와 우선순위 양보의 사항을 모두 고려하여, 특정 쓰레드의 실질적 우선순위(Effective Priority)를 반환하는 getEffectivePriority 메소드를 구현하시기 바랍니다

이번 과제에서는, 본 PriorityScheduler 클래스 외의 클래스(ex : Lock)는 수정하시면 안됩니다.

이번 과제 역시, 난이도를 낮추기 위해, 조교들이 구현한 코드를 제공토록하겠습니다. 보고서에 포함되어야 하는 내용은 다음과 같습니다.
1. \* 채우세요 *\ 부분을 채우시고, 해당 부분을채움으로써, 완성된 Method/Variable 가 어떤 의미(메소드의 역할)를 갖는가
2. 몇가지 Method (\* 해당 Method 의 역할 ... *\ 로 표시해놨습니다.) 의 역할과 실행 로직 
3. 우선순위 역전 현상 및 Deadlock 과 관련하여, 실행 결과(selfTest)에 대한 해석 (그외의 해석은 없어도 됩니다.)
   (실행 결과에서 T1 KThread가 Lock을 갖고있는 상태에서, T1 KThread 의 우선순위가 3 으로 설정되었고, T2 KThread 의 우선순위가 1 로 설정되었는데, Deadlock이 발생하는지? 발생하지 않는다면, 그 이유는?)
그 외에도, 추가 공지하고자하는 내용은 아래와 같습니다.
1. 과제 설명에서는,  increasePriority(), decreasePriority() 메소드를 선택적으로 구현하라 했으나, 첨부한 조교들의 코드는, increasePriority(), decreasePriority() 메소드ㄹ가 구현되어 있습니다.
2. 우선순위 역전 현상과 관련해서는 다음의 글(http://blog.skby.net/%EC%9A%B0%EC%84%A0%EC%88%9C%EC%9C%84-%EC%97%AD%EC%A0%84-%ED%98%84%EC%83%81/)을 참고하세요.
 */
