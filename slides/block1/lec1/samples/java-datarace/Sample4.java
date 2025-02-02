import java.util.*;

public class Sample4 {
	static volatile ArrayList<String> list = new ArrayList<>();
	public static void main(String... args) throws Exception {
		Thread t1 = new Thread(() -> { list.add("x"); });
		Thread t2 = new Thread(() -> { list.add("y"); });
		t1.start(); t2.start();
		t1.join(); t2.join();

		for (String s : list) {
			System.out.println(s);
		}
	}
}