package nachos.threads;
//---------import 문---------
import nachos.machine.*;
import java.util.Vector;
import java.util.LinkedList;
//---------------------------

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */

public class Condition2 {                                                   // Monitor 의 조건 변수 구현 클래스
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
	    this.conditionLock = conditionLock;
        CV_WaitThread_List = new Vector();  // init conditional waiting set!!
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {                                           // 현재 실행중인 커널 쓰레드를 조건 변수 상에서, 대기 상태로 전환시키는 메소드 정의
	    Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	    
        /* 채우세요 */                                        // 조건 변수에 대한 Lock (Condition Lock) Release (힌트 : threads/Lock.java 참고)
 	conditionLock.release();
        ////////////////////////////////////////////////////////////
        boolean intStatus = Machine.interrupt().disable();              // 인터럽트 Off
        /* 채우세요 */                                                  // Conditional Waiting Set 에 현재 실행 상태의 쓰레드(힌트 : KThread 클래스 참고) 저장 (FIFO 방식)
	CV_WaitThread_List.add((KThread)KThread.currentThread());
        KThread.currentThread().sleep();                                // 현재 실행 상태의 쓰레드를 대기 상태로 전환시킴 
        /* 채우세요 */                                                  // 인터럽트 다시 On
	Machine.interrupt().restore(intStatus);
        //////////////////////////////////////////////////////////////

	    conditionLock.acquire();                             // 조건 변수에 대한 Lock (Condition Lock) Acquire
    } 

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {                                   // 조건 변수 상에서 대기 중인 커널 쓰레드 하나를 Wake Up 시키는 메소드 정의
	    Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        boolean intStatus = Machine.interrupt().disable();              // 인터럽트 Off
        if(CV_WaitThread_List.size() !=0) {                             // Conditional Waiting Set 에 대기 중인 커널 쓰레드들 확인
            /* 채우세요 */       // Conditional Waiting Set 의 Head 에 위치한 커널 쓰레드(KThread)를 Ready 상태로 전환 (힌트 : threads/KThread.java 참고)
	((KThread)CV_WaitThread_List.firstElement()).ready();
            CV_WaitThread_List.removeElementAt(0);        
        }
        Machine.interrupt().restore(intStatus);           // 인터럽트 On
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {                            // 조건 변수 상에서 대기 중인 모든 커널 쓰레드들을 Wake Up 시키는 메소드 정의
	    Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        /* 채우세요 */           // Conditional Waiting Set 에 존재하는 모든 커널 쓰레드들 Wake Up 시키기 (위에서 정의한 wake() 함수 사용할 것)
	while(CV_WaitThread_List.size()!=0) wake();
	
                                        
    }

    private Lock conditionLock;                 // Mutex Lock 정의
    private Vector CV_WaitThread_List = null;   // Conditional Waiting Set 정의 

    /*
    Monitor 상의 조건 변수는 2개의 구성 요소(조건 동기 큐 (Conditional Waiting Set), 이진 Mutex Lock (Condition Lock))를 가짐 
     */



    //-------------------------- 조건 변수(Condition2)가 제대로 구현됬는지를 테스팅하는 코드입니다. 수정하지 마세요 ------------------------------------
    private static class InterlockTest {        
        private static Lock lock;              // locker (Mutex)
        private static Condition2 cv;          // condition variable

        private static class Interlocker implements Runnable {
           public void run() {
              lock.acquire();                // get mutex of it(lock mutex is for sync of run method!!)
              for(int i=0; i<10;i++) {         
                  System.out.println(KThread.currentThread().getName());  // print itself name(thread name)
                  cv.wake();                     // wake up thread in conditional waiting set
                  cv.sleep();                    // release mutex lock and get into conditional waiting set
              }
              lock.release();                // release mutex for run method!!
           }
        }

        public InterlockTest() {
            lock = new Lock();             // lock and condition variable init
            cv = new Condition2(lock);     

            KThread ping = new KThread(new Interlocker()); // InterLocker Testing Routine1 (call it ping)
            ping.setName("ping");
            KThread pong = new KThread(new Interlocker()); // InterLocker Testing Routine1 (call it ping)
            pong.setName("pong");

            ping.fork();
            pong.fork();
            
            ping.join();                                  // wait for ping to finish
                                                          // when ping terminated, pong is sleeping...
                                                          // so, pong.join() makes whole system blocked!! 
        }
   }
     
   public static void selfTest() { new InterlockTest();}      // 구현된 조건 변수를 테스팅하는 코드입니다. 수정하지 마세요. 

   public static void cvTest5() {                             // 구현된 조건 변수를 테스팅하는 코드입니다. 수정하지 마세요.
      final Lock lock = new Lock();
      final Condition2 empty = new Condition2(lock);
      final LinkedList<Integer> list = new LinkedList<>();

      KThread consumer = new KThread(new Runnable() {
           public void run() {
              lock.acquire();
              while(list.isEmpty()){empty.sleep();}
              Lib.assertTrue(list.size()==5, "List should have 5 values.");
              while(!list.isEmpty()) {
                  KThread.currentThread().yield();
                  System.out.println("Removed " + list.removeFirst());
              }
              lock.release();
           }
         });

      KThread producer = new KThread(new Runnable() {
           public void run() {
              lock.acquire();
              for(int i=0; i<5;i++){
                 list.add(i);
                 System.out.println("Added " + i);
                 KThread.currentThread().yield();
              }
              empty.wake();
              lock.release();
           }
         });

     consumer.setName("Consumer");
     producer.setName("Producer");
     consumer.fork();
     producer.fork();
     consumer.join();
     producer.join();
   }
   //------------------------------------------------------------------------------------------------------------------------------------------
}
