package vitral.application;

import vsdk.toolkit.common.VSDK;

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

        msg = label + "-> N:" + ocurrences + " t: " +
            VSDK.formatDouble(getTime()) + "sec" + " Rate: " +
            VSDK.formatDouble(((double)ocurrences)/getTime()) + "/sec";

        return msg;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
