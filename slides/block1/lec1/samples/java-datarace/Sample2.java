public class Sample2 {
	static volatile int x = 0;
	public static void main(String... args) throws Exception {
		Thread t = new Thread(() -> { x++; });
		t.start();
		x++;
		System.out.println(x);
		t.join();
	}
}