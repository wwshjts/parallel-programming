public class Sample {

	static class ThreadA extends Thread {
		private int idx;
		
		ThreadA(int i) {
			this.idx = i;
		}

		@Override
		public void run() {
			System.out.printf("Thread A, idx = %d: hello world\n", this.idx);
		}
	}

	public static void main(String... args) throws Exception {
		Thread t = new ThreadA(1);
		t.start(); // !!! not t.run !!!
		t.join();
	}
}