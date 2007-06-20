//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - May 10 2007 - Oscar Chavarro: Original base version                   =
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [BAER2002] Baeentzen, Jakob Andreas. Aanaes, Henrik. "Generating Signed =
//=     Distance Fields From Triangle Meshes",  Technical report            =
//=     IMM-TR-2002-21, Thecnical University of Denmark, 2002.              =
//===========================================================================

package vsdk.toolkit.processing;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.InfinitePlane;

/**
This class contains common computational geometry operations.
*/
public class ComputationalGeometry extends ProcessingElement
{
    private static InfinitePlane workPlane;

    static {
        workPlane = new InfinitePlane(1, 1, 1, 0);
    }

    /**
    Given a line that passes between points `p0` and `p2`, this method 
    determines if point `p` falls under `distanceTolerance` in such line.
    */
    public static int lineContainmentTest(Vector3D p0, Vector3D p1,
                                   Vector3D p, double distanceTolerance) {
        double d;
        Vector3D a, b;
        Vector3D lineVector = p1.substract(p0);

        double denominator = lineVector.length();
        if ( denominator < VSDK.EPSILON ) return Geometry.OUTSIDE;

        a = p1.substract(p0);
        b = p0.substract(p);
        double numerator = a.crossProduct(b).length();
        d = (numerator / denominator);

        if ( d <= distanceTolerance ) return Geometry.LIMIT;
        return Geometry.OUTSIDE;
    }

    /**
    Given a line that passes between points `p0` and `p2`, this method 
    determines if point `p` falls under `distanceTolerance` in such line.
    */
    public static int lineSegmentContainmentTest(Vector3D p0, Vector3D p1,
                                   Vector3D p, double distanceTolerance) {
        double d;
        Vector3D a, b;
        Vector3D lineVector = p1.substract(p0);
        Vector3D pointVector = p.substract(p0);

        double denominator = lineVector.length();
        if ( denominator < VSDK.EPSILON ) return Geometry.OUTSIDE;

        a = p1.substract(p0);
        b = p0.substract(p);
        double numerator = a.crossProduct(b).length();
        d = (numerator / denominator);

        if ( d <= distanceTolerance ) {
            double t = pointVector.dotProduct(lineVector) / lineVector.length();
            if ( t < 0 || t > 1 ) return Geometry.OUTSIDE;

            return Geometry.LIMIT;
        }
        return Geometry.OUTSIDE;
    }

    /**
    This method calculates containment test for triangle defined by its
    3 vertex positions.  It implements a region classification based
    strategy proposed in [BAER2002].  For a given triangle and with respect
    to triangle's containing plane, a poing lies in one of 7 regions:
       - R1: inside triangle
       - R2: outside triangle, near edge 1
       - R3: outside triangle, near edge 2
       - R4: outside triangle, near edge 3
       - R5: outside triangle, near vertex 1
       - R6: outside triangle, near vertex 2
       - R7: outside triangle, near vertex 3
    */
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
            // Barycentric coordinates test containment technique (Region 1)
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
            if ( (u >= 0) && (v >= 0) && (u + v <= 1) ) {
                // R1
                return Geometry.LIMIT;
            }
            else if ( (u <= 0) && (v >= 0) && (u + v <= 1) ) {
                // R2
                return
                    lineSegmentContainmentTest(p0, p2, p, distanceTolerance);
            }
            else if ( (u >= 0) && (v <= 0) && (u + v <= 1) ) {
                // R3
                return
                    lineSegmentContainmentTest(p0, p1, p, distanceTolerance);
            }
            else if ( (u >= 0) && (v >= 0) && (u + v >= 1) ) {
                // R4
                return
                    lineSegmentContainmentTest(p1, p2, p, distanceTolerance);
            }
            else if ( (u <= 0) && (v <= 0) ) {
                // R5
                if ( VSDK.vectorDistance(p, p0) < distanceTolerance ) {
                    return Geometry.LIMIT;
                }
            }
            else if ( (u <= 0) && (v >= 1) ) {
                // R6
                if ( VSDK.vectorDistance(p, p2) < distanceTolerance ) {
                    return Geometry.LIMIT;
                }
            }
            else {
                // R7
                if ( VSDK.vectorDistance(p, p1) < distanceTolerance ) {
                    return Geometry.LIMIT;
                }
            }
        }
        return Geometry.OUTSIDE;
    }

    public static void triangleMinMax(
        Vector3D p0, Vector3D p1, Vector3D p2, double minMax[])
    {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;

        if ( p0.x < minX ) minX = p0.x;
        if ( p0.y < minY ) minY = p0.y;
        if ( p0.z < minZ ) minZ = p0.z;
        if ( p0.x > maxX ) maxX = p0.x;
        if ( p0.y > maxY ) maxY = p0.y;
        if ( p0.z > maxZ ) maxZ = p0.z;

        if ( p1.x < minX ) minX = p1.x;
        if ( p1.y < minY ) minY = p1.y;
        if ( p1.z < minZ ) minZ = p1.z;
        if ( p1.x > maxX ) maxX = p1.x;
        if ( p1.y > maxY ) maxY = p1.y;
        if ( p1.z > maxZ ) maxZ = p1.z;

        if ( p2.x < minX ) minX = p2.x;
        if ( p2.y < minY ) minY = p2.y;
        if ( p2.z < minZ ) minZ = p2.z;
        if ( p2.x > maxX ) maxX = p2.x;
        if ( p2.y > maxY ) maxY = p2.y;
        if ( p2.z > maxZ ) maxZ = p2.z;

        minMax[0] = minX;
        minMax[1] = minY;
        minMax[2] = minZ;
        minMax[3] = maxX;
        minMax[4] = maxY;
        minMax[5] = maxZ;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
