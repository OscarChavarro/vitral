//===========================================================================
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [BUST2005] Bustos, Benjamin. Keim, Daniel A. Saupe, Dietmar. Schreck,   =
//=     Tobias. Vranic, Dejan V. "Feature-Based Similarity Search in 3D     =
//=     Databases". ACM Computing Surveys, Vol. 37, No 4, December 2005,    =
//=     pp. 345-387.                                                        =
//===========================================================================

import java.util.ArrayList;

import vsdk.toolkit.media.ShapeDescriptor;

public class GeometryMetadata
{
    private String objectFilename;
    private ArrayList<ShapeDescriptor> descriptorsList;

    /**
    Given a pair of GeometryMetadata elements, this method computes the
    MinskowskiDistance between feature vectors, if shape descriptors are
    "comparable".

    Two sets of shape descriptors are "comparable" if they are of the same
    type, in the same order and with the same number of features.

    If given shape descriptors are not comparable, this method return infinite
    distance.
    */
    public double doMinskowskiDistance(GeometryMetadata other, double s)
    {
        if ( this.descriptorsList.size() !=
             other.descriptorsList.size() ) {
            return Double.MAX_VALUE;
        }
        int i, j;
        ShapeDescriptor a, b;
        double av[], bv[];
        double acum = 0;

        for ( i = 0; i < this.descriptorsList.size(); i++ ) {
            a = this.descriptorsList.get(i);
            b = other.descriptorsList.get(i);
            av = a.getFeatureVector();
            bv = b.getFeatureVector();
            if ( av.length != bv.length ) {
                return Double.MAX_VALUE;
            }
            for ( j = 0; j < av.length; j++ ) {
                acum += Math.pow(Math.abs(av[j] - bv[j]), s);
            }
        }
        return Math.pow(acum, 1/s);
    }


    public GeometryMetadata()
    {
        objectFilename = null;
        descriptorsList = new ArrayList<ShapeDescriptor>();
    }

    public void setFilename(String filename)
    {
        if ( filename != null && filename.length() > 0 ) {
            objectFilename = new String(filename);
        }
        else {
            objectFilename = null;
        }
    }

    public String getFilename()
    {
        return objectFilename;
    }

    public ArrayList<ShapeDescriptor> getDescriptors()
    {
        return descriptorsList;
    }

    public String toString()
    {
        String msg = new String(objectFilename);
        msg += "\n    . " + descriptorsList.size() + " shape descriptors\n";
        int i;
        for ( i = 0; i < descriptorsList.size(); i++ ) {
            msg += "        . " + descriptorsList.get(i).getClass().getName();
        }
        return msg;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
