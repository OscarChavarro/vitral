public class MyTimer
{
    private String name;
    private long startTime;
    private long stopTime;
    private boolean running = false;
    private double elapsedSeconds;
    
    MyTimer(String string) {
        name = string;
        elapsedSeconds = 0.0;
        System.out.println("Timer [" + name + "] started");
        start();
    }
    
    public double elapsed() {
        if (running) {
            long l = System.currentTimeMillis();
            elapsedSeconds = (double) (l - startTime) / 1000.0;
        }
        return elapsedSeconds;
    }
    
    public void start() {
        startTime = System.currentTimeMillis();
        running = true;
    }
    
    public void stop() {
        stopTime = System.currentTimeMillis();
        running = false;
        elapsedSeconds = (double) (stopTime - startTime) / 1000.0;
        System.out
            .println("Timer [" + name + "] stopped at " + elapsedSeconds);
    }
    
    public String toString() {
        return Double.toString(elapsedSeconds);
    }
}
