//===========================================================================

import vsdk.toolkit.common.VSDK;

public class ResultSource
{
    public static final int SPHERICAL_HARMONIC = 0;
    public static final int CUBE13VIEW = 0;

    public int source;
    public double distance;

    public ResultSource(int source, double distance)
    {
        this.source = source;
        this.distance = distance;
    }

    public ResultSource(ResultSource other)
    {
        this.source = other.source;
        this.distance = other.distance;    
    }

    public String toString()
    {
        String msg;

        msg = "(";
        if ( source == SPHERICAL_HARMONIC ) {
            msg += "SH";
        }
        else if ( source >= CUBE13VIEW+1 && source <= CUBE13VIEW+13 ) {
            msg += "CV" + (source - CUBE13VIEW);
        }
        else {
            msg += "?";
        }
        msg += ":" + VSDK.formatDouble(distance);
        msg += ")";

        return msg;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
