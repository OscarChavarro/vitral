public class MyTimer
{
    private String my_name;
    private long start_time;
    private long stop_time;
    private boolean running = false;
    private double elapsed_seconds;
    
    MyTimer(String string) {
	my_name = string;
	elapsed_seconds = 0.0;
	System.out.println("Timer [" + my_name + "] started");
	start();
    }
    
    public double elapsed() {
	if (running) {
	    long l = System.currentTimeMillis();
	    elapsed_seconds = (double) (l - start_time) / 1000.0;
	}
	return elapsed_seconds;
    }
    
    public void start() {
	start_time = System.currentTimeMillis();
	running = true;
    }
    
    public void stop() {
	stop_time = System.currentTimeMillis();
	running = false;
	elapsed_seconds = (double) (stop_time - start_time) / 1000.0;
	System.out
	    .println("Timer [" + my_name + "] stopped at " + elapsed_seconds);
    }
    
    public String toString() {
	return Double.toString(elapsed_seconds);
    }
}
