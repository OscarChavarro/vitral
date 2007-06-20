//===========================================================================

import vsdk.toolkit.common.VSDK;

public class Result
{
    private double distance;
    private String filename;

    public Result(String filename, double distance)
    {
        this.distance = distance;
        this.filename = new String(filename);
    }

    public double getDistance()
    {
        return distance;
    }

    public String getFilename()
    {
        return filename;
    }

    public String toString()
    {
        String msg = filename + " (" + VSDK.formatDouble(distance) + ")";
        return msg;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
