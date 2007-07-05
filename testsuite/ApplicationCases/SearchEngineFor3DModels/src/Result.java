//===========================================================================

import java.util.ArrayList;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.media.GeometryMetadata;

public class Result implements Comparable <Result>
{
    private double distance;
    private long id;
    private String filename;
    private ArrayList<ResultSource> parts;
    private GeometryMetadata descriptor;

    public Result(String filename, long id, ResultSource part)
    {
        this.distance = distance;
        this.filename = new String(filename);
        this.id = id;
        descriptor = null;
        parts = new ArrayList<ResultSource>();
        parts.add(part);
    }

    private void updateDistance()
    {
        int i;
        double d;

        distance = Double.MAX_VALUE;

        for ( i = 0; i < parts.size(); i++ ) {
            d = parts.get(i).distance;
            if ( d < distance ) {
                distance = d;
            }
        }
    }

    public GeometryMetadata getDescriptor()
    {
        return descriptor;
    }

    public void setDescriptor(GeometryMetadata descriptor)
    {
        this.descriptor = descriptor;
    }

    public void addSource(ResultSource part)
    {
        parts.add(part);
        updateDistance();
    }

    public double getDistance()
    {
        updateDistance();
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
        updateDistance();
        other.updateDistance();
        if ( this.distance < other.distance ) return -1;
        else if ( this.distance > other.distance ) return 1;
        return 0;
    }

    public String toString()
    {
        int i;
        updateDistance();
        String msg = filename + " (" + VSDK.formatDouble(distance) + ") : ";
        for ( i = 0; i < parts.size(); i++ ) {
            msg += parts.get(i).toString();
            if ( i < parts.size()-1 ) {
                msg += "&";
            }
        }
        return msg;
    }

    public ArrayList<ResultSource> getParts()
    {
        return parts;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
