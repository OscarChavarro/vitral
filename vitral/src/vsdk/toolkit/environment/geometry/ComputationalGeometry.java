//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - May 10 2007 - Oscar Chavarro: Original base version                   =
//===========================================================================
package vsdk.toolkit.environment.geometry;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.InfinitePlane;

/**
This class contains common computational geometry operations.
*/
public class ComputationalGeometry
{
    private static InfinitePlane workPlane;

    static {
        workPlane = new InfinitePlane(1, 1, 1, 0);
    }

    public static int triangleContainmentTest(
        Vector3D p0, Vector3D p1, Vector3D p2, Vector3D p,
        double distanceTolerance)
    {
    Vector3D n, a, b;

    a = p1.substract(p0);
    b = p2.substract(p0);
    n = a.crossProduct(b);
    n.normalize();

        workPlane.setFromPointNormal(p0, n);

        if ( workPlane.doContainmentTest(p, distanceTolerance) == 
             Geometry.LIMIT ) {
            // Barycentric coordinates test containment technique
            Vector3D c = p.substract(p0);
        double dot00, dot01, dot02, dot11, dot12, invDenom, u, v;
        dot00 = a.dotProduct(a);
        dot01 = a.dotProduct(b);
        dot02 = a.dotProduct(c);
        dot11 = b.dotProduct(b);
        dot12 = b.dotProduct(c);
            invDenom = 1 / (dot00 * dot11 - dot01 * dot01);
            u = (dot11 * dot02 - dot01 * dot12) * invDenom;
            v = (dot00 * dot12 - dot01 * dot02) * invDenom;
            if ( (u > 0) && (v > 0) && (u + v < 1) ) {
                return Geometry.LIMIT;
        }
        }
        return Geometry.OUTSIDE;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
