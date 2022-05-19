package nachos.threads;

// 스레드에서 1부터 num까지의 합을 구하는 클래스
public class KThreadTest implements Runnable {
	
	int num;
	int sum;
	
	// KThreadTest 생성자 코드
	public KThreadTest(int num) {
		this.num = num;
		sum = 0;
	}
	
	public void run() {
		// 스레드 시작
		System.out.println("*** thread " + num + " is started" );
	    
		for (int i=1; i<=num; i++) {
	    	sum+= i;
	    	System.out.println("*** thread " + num + " looped "
				   + i + " times");
	    }
	    //스레드 끝
		System.out.println("*** thread " + num + " is finished : " + num);
	    
	}

}
