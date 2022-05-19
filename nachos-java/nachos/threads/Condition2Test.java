package nachos.threads;

public class Condition2Test {

	public static void selfTest()
	{
		final Lock myLock = new Lock();
		final Condition2 myCond = new Condition2(myLock);
		// 스레드 T1 선언 cond.sleep() 테스트용
		KThread T1 = new KThread(new Runnable(){
            public void run(){
                myLock.acquire();
                System.out.println("T1 is sleep");
                myCond.sleep();
                System.out.println("T1 wock up");
				myLock.release();
				System.out.println("T1 complete");
				
        } } ).setName("T1");
		T1.fork();
		// 스레드 T2 선언 cond.wake() 테스트용
		KThread T2 = new KThread(new Runnable(){

			public void run(){
                myLock.acquire();
                myCond.wake();
				System.out.println("T2 call wake()");
				myLock.release();
				System.out.println("T2 complete");
				
        } } ).setName("T2");
		//T2 -> T1가 실행될 수 있도록 순서 지정
		T2.fork();
		T1.join();
		
		// 스레드 T3 선언 wait 하고있는 스레드가 없을 때 wake 테스트용
		KThread T3 = new KThread(new Runnable(){
			public void run(){
                myLock.acquire();
                myCond.wake();
				System.out.println("T3 call wake()");
				myLock.release();
				System.out.println("T3 complete");
				
        } } ).setName("T3");
		
		T3.fork();
	}
		
		
}
