//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - March 19 2006 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vsdk.toolkit.common;

public class ProgressMonitorConsole extends ProgressMonitor {

    private double currentPercent;
    private double jumpPercent;
    private int lastPrintedLabel;

    public ProgressMonitorConsole()
    {
    }

    public void begin()
    {
        currentPercent = 0;
        lastPrintedLabel = 0;
        jumpPercent = 2;
        System.out.print("[ 0% ");
    }

    public void end()
    {
        System.out.println(" 100% ]");
    }

    private boolean
    testLabelLimit(int limit)
    {
        if ( limit == lastPrintedLabel ) return false;

        if ( currentPercent - 6*jumpPercent/10 < limit &&
             currentPercent + 6*jumpPercent/10 > limit )
        {
            System.out.print(" " + limit + "% ");
            lastPrintedLabel = limit;
            return true;
        }
        return false;
    }

    public void
    update(double minValue, double maxValue, double currentValue)
    {
        double v = 100 * (currentValue - minValue) / (maxValue - minValue);

        while ( currentPercent + jumpPercent < v ) {
            currentPercent += jumpPercent;

            if ( !testLabelLimit(25) &&
                 !testLabelLimit(50) && 
                 !testLabelLimit(75) ) {
                System.out.print("-");
            }
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
