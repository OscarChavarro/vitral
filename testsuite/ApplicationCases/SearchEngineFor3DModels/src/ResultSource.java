import vsdk.toolkit.common.VSDK;

public class ResultSource
{
    public static final int SPHERICAL_HARMONIC = 0;
    public static final int CUBE13VIEW = 0;

    public int source;
    public int sketchId;
    public double distance;

    public ResultSource(int source, double distance, int sketchId)
    {
        this.source = source;
        this.distance = distance;
        this.sketchId = sketchId;
    }

    public ResultSource(ResultSource other)
    {
        this.source = other.source;
        this.distance = other.distance;
        this.sketchId = other.sketchId;
    }

    public int getSource()
    {
        return source;
    }

    public int getSketchId()
    {
        return sketchId;
    }

    public double getDistance()
    {
        return distance;
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
