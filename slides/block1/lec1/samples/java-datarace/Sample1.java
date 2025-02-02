public class Sample1 {
	static volatile int x = 0;
	public static void main(String... args) throws Exception {
		Thread t = new Thread(() -> { x++; });
		t.start();
		System.out.println(x);
		t.join();
	}
}