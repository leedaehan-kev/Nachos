package nachos.threads;

//----- import 문 -------
import nachos.machine.*;
//import java.util.PriorityQueue
import java.util.Vector;
/* 채우세요 */            
//-----------------------                        


/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */

public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */

    private Vector WaitQ = null;                  // asserted Thread 들에 대한 Wating Queue
                                                  // SelfWaitThread 를 해당 Vector 에 저장할 것입니다.

    public class SelfWaitThread {                 // 'waitUntil(long)' 메소드를 호출한, 쓰레드 클래스
        /* 채우세요 */      
	private KThread waitThread = null;                      // 대기 상태에 있는 쓰레드에 정의 (힌트 : KThread)
        private long waitTime = 0;                // 쓰레드의 대기 시간에 대한 정의

        public SelfWaitThread(KThread thread, long time) {   // 대기 상태로 전환된 쓰레드 정의 (대기 시간 및 대상 쓰레드)
               waitTime = time;                                
               waitThread = thread;
        }
       
        public KThread getSelfWaitThread() {return waitThread;}  // 현재 대기중인 커널 쓰레드 반환 메소드 정의
        /* 채우세요 */     
	public long getSelfWaitTime(){ return waitTime;}                                      // 현재 대기중인 커널 쓰레드의 대기시간 반환 메소드(public long getSelfWaitTime()) 정의
   
    } 
    
    public Alarm() {         
        /* 채우세요. */    
	WaitQ= new Vector();                               // asserted Thread 들에 대한 Wating Queue (Vector) 초기화
        Machine.timer().setInterruptHandler(new Runnable() {
                public void run() { timerInterrupt(); }
            });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {                          // Timer 인터럽트에 대한 Handler 메소드
  
        long now = Machine.timer().getTime();               // Timer 가 작동되고 현재 시점까지의 경과 시간을 가져옴
        /* 채우세요 */                                      // 인터럽트 Off (힌트 : machine/Interrupt.java 파일 참고)
        boolean intStatus = Machine.interrupt().disable();
        int i=0;     

        while(WaitQ.size() > i){                            //  Wating Queue 에 존재하는 모든 쓰레드들 확인
            SelfWaitThread tmp;
            /* 채우세요 */                                  // Wating Queue 에 존재하는 i 번째 쓰레드 가져오기 (SelfWaitThread tmp 활용)
            tmp=(SelfWaitThread)WaitQ.elementAt(i); // need to re act
            if(now > tmp.getSelfWaitTime()){                             // 해당 커널 쓰레드의 대기 시간이 지난 경우, 해당 쓰레드를 대기 상태로 전환시킴 
               tmp.getSelfWaitThread().ready();             // 해당 쓰레드를 준비 상태로 전환시킴 (Context Switch)
               WaitQ.removeElementAt(i);                             // 해당 쓰레드를 Waiting Queue 에서 제거시킴
               if(i != 0)i--;
            }
            i++;                                          
        }
        Machine.interrupt().restore(intStatus);             // 인터럽트 On
        KThread.currentThread().yield();                    // 준비 상태로 Context Switch 된 쓰레드를 Run
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param   x       the minimum number of clock ticks to wait.
     *
     * @see     nachos.machine.Timer#getTime()
     */

    public void waitUntil(long x) {                  // x 라는 시간 동안 대기하는 쓰레드 정의하는 메소드
        
        long wakeTime = Machine.timer().getTime() + x;     // 대기 상태가 종료되는 시간을 구함
        /* 채우세요 */                                     // 인터럽트 Off
	boolean intStatus = Machine.interrupt().disable();
       
        SelfWaitThread tmp = new SelfWaitThread(KThread.currentThread(),wakeTime);     // 대기 상태로 전환된 쓰레드 정의 (SelfWaitThread 객체 생성)
        /* 채우세요 */                                     // waitUntil 메소드를 호출한 쓰레드를 Waiting Queue 에 저장 (FIFO 방식)

	WaitQ.add((SelfWaitThread)tmp);
        /* 채우세요 */                                     // 해당 쓰레드를 대기 상태로 진입 시킴(힌트 : threads/KThread.java 파일 참고)
	tmp.getSelfWaitThread().sleep();
        Machine.interrupt().restore(intStatus);            // 인터럽트 On
        
    }

   public static void alarmTest1() {                 // Alarm 테스트용 코드입니다. 수정하지 마세요.
       int durations[] = {1000,10*1000, 100*1000};   
       long t0,t1;
 
       for(int d : durations){
          t0 = Machine.timer().getTime();             
          ThreadedKernel.alarm.waitUntil(d);
          t1 = Machine.timer().getTime();
          System.out.println("alarmTest1: waited for "+(t1-t0)+"ticks");  
       }
  }
 
  public static void selfTest() {                    // 테스트 용 코드입니다. 수정하지 마세요.
      alarmTest1();
  }   
}
