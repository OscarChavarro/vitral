//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - April 30 2008 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.common;

public class StopWatch
{
    private long t0;
    private long t1;

    public StopWatch()
    {
        t0 = 0;
        t1 = 1;
    }

    public void start()
    {
        t0 = System.currentTimeMillis();
    }

    public void stop()
    {
        t1 = System.currentTimeMillis();
    }

    public double getElapsedRealTime()
    {
        double a, b;
        a = (double)t0;
        b = (double)t1;
        return (b - a)/1000.0;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
