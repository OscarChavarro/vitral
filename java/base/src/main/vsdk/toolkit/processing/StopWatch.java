package vsdk.toolkit.processing;

public class StopWatch extends ProcessingElement
{
    private long startTime;
    private long stopTime;
    private boolean running;

    public StopWatch()
    {
        startTime = 0;
        stopTime = 1;
        running = false;
    }

    public void start()
    {
        //startTime = System.currentTimeMillis();
        running = true;
        startTime = System.nanoTime();
    }

    public void stop()
    {
        //stopTime = System.currentTimeMillis();
        if ( running ) {
            stopTime = System.nanoTime();
        }
        running = false;
    }

    public double getElapsedRealTime()
    {
        if ( running ) {
            stopTime = System.nanoTime();
        }
        double a, b;
        a = (double)startTime;
        b = (double)stopTime;
        //return (b - a)/1000.0;
        return (b - a)/1000000000.0;
    }
}
