//===========================================================================

public class TimeReport
{
    private String label;
    private long ocurrences;
    private long elapsedTime;
    private long lastStartingTime;

    TimeReport(String label)
    {
	this.label = new String(label);
        ocurrences = 0;
        elapsedTime = 0;
    }

    public void start()
    {
        lastStartingTime = System.currentTimeMillis();
    }

    public void stop()
    {
        long currentTime = System.currentTimeMillis();
	elapsedTime += currentTime - lastStartingTime;
	ocurrences++;
    }

    public String
    getLabel()
    {
        return label;
    }

    public double
    getTime()
    {
	return ((double)elapsedTime) / 1000.0;
    }

    public long
    getOcurrences()
    {
        return ocurrences;
    }

    public String toString()
    {
	String msg;

        msg = "TIMER \"" + label + "\": " + ocurrences + " ocurrences in " +
            getTime() + " seconds.";

	return msg;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
