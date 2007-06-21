//===========================================================================

import vsdk.toolkit.common.VSDK;

public class Result implements Comparable <Result>
{
    private double distance;
    private long id;
    private String filename;

    public Result(String filename, double distance, long id)
    {
        this.distance = distance;
        this.filename = new String(filename);
	this.id = id;
    }

    public double getDistance()
    {
        return distance;
    }

    public String getFilename()
    {
        return filename;
    }

    public long getId()
    {
	return id;
    }

    public int compareTo(Result other)
    {
        if ( this.distance < other.distance ) return -1;
	else if ( this.distance > other.distance ) return 1;
	return 0;
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
