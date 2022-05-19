package nachos.threads;

import nachos.machine.Machine;

public class AlarmTest {

	
	void selftest() {
		//알람 객체 선언
		Alarm a = new Alarm();
		
		KThread T1 = new KThread(new Runnable(){
            public void run(){
            	System.out.println("T1 is forked at"+Machine.timer().getTime());
            	// 스레드 T1 내부에서 300 만큼 슬립 요청
        		a.waitUntil(300);
        		System.out.println("T1 is finished at"+Machine.timer().getTime());
        } } ).setName("T1");
		
		
		KThread T2 = new KThread(new Runnable(){
            public void run(){
            	System.out.println("T2 is forked at"+Machine.timer().getTime());
            	// 스레드 T2 내부에서 200 만큼 슬립 요청
            	a.waitUntil(200);
        		System.out.println("T2 is finished at"+Machine.timer().getTime());
        } } ).setName("T2");
		
		
		KThread T3 = new KThread(new Runnable(){
            public void run(){
            	System.out.println("T3 is forked at"+Machine.timer().getTime());
        		System.out.println("T3 is finished at"+Machine.timer().getTime());
        } } ).setName("T3");
		T1.fork();
		T3.fork();
		T2.fork();
	}
	
}
