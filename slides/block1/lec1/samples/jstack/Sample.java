public class Sample {

	static volatile Runnable lambda = null;	
	public static void main(String... args) throws Exception {
		Thread A = new Thread(() -> { lambda.run(); });
		Thread B = new Thread(() -> { try { A.join(); } catch(Throwable t) {} }); 

		lambda = () -> { try { B.join(); } catch(Throwable t) {} };
		A.start(); B.start();
		A.join(); B.join();
	}
}