//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - April 8 2006 - Oscar Chavarro: Original base version                  =
//===========================================================================

package vsdk.toolkit.environment.geometry;

import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.environment.geometry.Geometry;

public class InfinitePlane extends HalfSpace {
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20060502L;

    // This is the infinite plane with canonical equation ax + bx + cx + d = 0
    private double a;
    private double b;
    private double c;
    private double d;

    public InfinitePlane(double a, double b, double c, double d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    public InfinitePlane(Vector3D normal, Vector3D pointInPlane) {
        normal.normalize();
        a = normal.x;
        b = normal.y;
        c = normal.z;
        d = -normal.dotProduct(pointInPlane);
    }

    public boolean
    doIntersection(Ray inout_rayo) {
        double denominator = a*inout_rayo.direction.x + b*inout_rayo.direction.y + c*inout_rayo.direction.z;
        if ( Math.abs(denominator) < VSDK.EPSILON ) return false;
        double t = -(a*inout_rayo.origin.x + b*inout_rayo.origin.y + c*inout_rayo.origin.z + d)/denominator;

        if ( t < 0 ) return false;

        inout_rayo.t = t;

        return true;
    }

    public boolean
    doIntersectionWithNegative(Ray inout_rayo) {
        double denominator = a*inout_rayo.direction.x + b*inout_rayo.direction.y + c*inout_rayo.direction.z;
        if ( Math.abs(denominator) < VSDK.EPSILON ) {
            Ray r = new Ray(inout_rayo.origin, inout_rayo.direction.multiply(-1));
            if ( doIntersection(r) ) {
                inout_rayo.t = -r.t;
                return true;
        }
        else {
                return false;
        }
    }
        double t = -(a*inout_rayo.origin.x + b*inout_rayo.origin.y + c*inout_rayo.origin.z + d)/denominator;

        inout_rayo.t = t;

        return true;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doExtraInformation.
    */
    public void doExtraInformation(Ray inRay, double inT, 
                                  GeometryIntersectionInformation outData) {
        outData.p.x = inRay.origin.x + inT*inRay.direction.x;
        outData.p.y = inRay.origin.y + inT*inRay.direction.y;
        outData.p.z = inRay.origin.z + inT*inRay.direction.z;

        outData.n = getNormal();
    }

    /**
    TODO: Current returned values are not always true!
    */
    public double[] getMinMax()
    {
        double minmax[] = new double[6];
        for ( int i = 0; i < 3; i++ ) {
        minmax[i] = Double.MIN_VALUE;
    }
        for ( int i = 3; i < 6; i++ ) {
        minmax[i] = Double.MAX_VALUE;
    }
        return minmax;
    }

    public Vector3D getNormal()
    {
        Vector3D n = new Vector3D(a, b, c);
        n.normalize();
        return n;
    }

    public double getD()
    {
        return d;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
