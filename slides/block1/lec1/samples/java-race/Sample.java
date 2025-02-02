public class Sample {
	public static void main(String... args) throws Exception {
		Thread t1 = new Thread(() -> { System.out.println("Thread A"); });
		Thread t2 = new Thread(() -> { System.out.println("Thread B"); });
		t1.start(); t2.start();
		t1.join(); t2.join();
	}
}