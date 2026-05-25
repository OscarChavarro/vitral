//= References:                                                             =
//= [BAER2002] Baeentzen, Jakob Andreas. Aanaes, Henrik. "Generating Signed =
//=     Distance Fields From Triangle Meshes",  Technical report            =
//=     IMM-TR-2002-21, Thecnical University of Denmark, 2002.              =

package vsdk.toolkit.environment.geometry.elements;
import java.io.Serial;
import vsdk.toolkit.common.FundamentalEntity;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.processing.Containment;

public class Triangle extends FundamentalEntity
{
    @Serial private static final long serialVersionUID = 20060502L;

    public int p0;
    public int p1;
    public int p2;

    public Vector3Dd normal;

    public Triangle() {
        normal = new Vector3Dd(0, 0, 0);
    }

    /**
    @param p0
    @param p1
    @param p2
    */
    public Triangle(int p0, int p1, int p2) {
        this.p0 = p0;
        this.p1 = p1;
        this.p2 = p2;
        normal = new Vector3Dd(0, 0, 0);
    }

    /**
    @return point p0
    */
    public int getPoint0() {
        return this.p0;
    }

    /**
    @return point p1
    */
    public int getPoint1() {
        return this.p1;
    }

    /**
    @return point p2
    */
    public int getPoint2() {
        return this.p2;
    }

    /**
    @param p0
    */
    public void setPoint0(int p0) {
        this.p0 = p0;
    }

    /**
    @param p1
    */
    public void setPoint1(int p1) {
        this.p1 = p1;
    }

    /**
    @param p2
    */
    public void setPoint2(int p2) {
        this.p2 = p2;
    }

    public static Intersection doIntersectionWithTriangle(
        Ray ray,
        Vector3Dd v0,
        Vector3Dd v1,
        Vector3Dd v2)
    {
        Vector3Dd e1 = v1.subtract(v0);
        Vector3Dd e2 = v2.subtract(v0);
        Vector3Dd h = ray.direction().crossProduct(e2);
        double a = e1.dotProduct(h);
        if ( Math.abs(a) < VSDK.EPSILON ) return null;
        double f = 1.0 / a;
        Vector3Dd s = ray.origin().subtract(v0);
        double u = f * s.dotProduct(h);
        if ( u < 0 || u > 1 ) return null;
        Vector3Dd q = s.crossProduct(e1);
        double v = f * ray.direction().dotProduct(q);
        if ( v < 0 || u + v > 1 ) return null;
        double t = f * e2.dotProduct(q);
        if ( t <= VSDK.EPSILON ) return null;

        return new Intersection(
            t,
            ray.origin().add(ray.direction().multiply(t)),
            e1.crossProduct(e2).normalized()
        );
    }

    private static Vector3Dd closestPointOnTriangle(
        Vector3Dd p,
        Vector3Dd a,
        Vector3Dd b,
        Vector3Dd c)
    {
        Vector3Dd ab = b.subtract(a);
        Vector3Dd ac = c.subtract(a);
        Vector3Dd ap = p.subtract(a);
        double d1 = ab.dotProduct(ap);
        double d2 = ac.dotProduct(ap);
        if ( d1 <= 0 && d2 <= 0 ) return a;

        Vector3Dd bp = p.subtract(b);
        double d3 = ab.dotProduct(bp);
        double d4 = ac.dotProduct(bp);
        if ( d3 >= 0 && d4 <= d3 ) return b;

        double vc = d1*d4 - d3*d2;
        if ( vc <= 0 && d1 >= 0 && d3 <= 0 ) {
            return a.add(ab.multiply(d1/(d1-d3)));
        }

        Vector3Dd cp = p.subtract(c);
        double d5 = ab.dotProduct(cp);
        double d6 = ac.dotProduct(cp);
        if ( d6 >= 0 && d5 <= d6 ) return c;

        double vb = d5*d2 - d1*d6;
        if ( vb <= 0 && d2 >= 0 && d6 <= 0 ) {
            return a.add(ac.multiply(d2/(d2-d6)));
        }

        double va = d3*d6 - d5*d4;
        if ( va <= 0 && (d4-d3) >= 0 && (d5-d6) >= 0 ) {
            return b.add(c.subtract(b).multiply((d4-d3)/((d4-d3)+(d5-d6))));
        }

        double denom = 1.0/(va+vb+vc);
        double v = vb*denom;
        double w = vc*denom;
        return a.add(ab.multiply(v)).add(ac.multiply(w));
    }

    public static int containmentTest(
        Vector3Dd p0,
        Vector3Dd p1,
        Vector3Dd p2,
        Vector3Dd p,
        double distanceTolerance)
    {
        /*
        This method calculates containment test for triangle defined by its
        3 vertex positions. It implements a region classification based
        strategy proposed in [BAER2002]. For a given triangle and with
        respect to triangle's containing plane, a point lies in one of 7
        regions:
           - R1: inside triangle
           - R2: outside triangle, near edge 1
           - R3: outside triangle, near edge 2
           - R4: outside triangle, near edge 3
           - R5: outside triangle, near vertex 1
           - R6: outside triangle, near vertex 2
           - R7: outside triangle, near vertex 3
        */
        Vector3Dd q = closestPointOnTriangle(p, p0, p1, p2);
        if ( q.subtract(p).length() <= distanceTolerance ) {
            return Containment.LIMIT.value();
        }
        return Containment.OUTSIDE.value();
    }

    public static void minMax(
        Vector3Dd p0,
        Vector3Dd p1,
        Vector3Dd p2,
        double[] mm)
    {
        mm[0] = Math.min(p0.x(), Math.min(p1.x(), p2.x()));
        mm[1] = Math.min(p0.y(), Math.min(p1.y(), p2.y()));
        mm[2] = Math.min(p0.z(), Math.min(p1.z(), p2.z()));
        mm[3] = Math.max(p0.x(), Math.max(p1.x(), p2.x()));
        mm[4] = Math.max(p0.y(), Math.max(p1.y(), p2.y()));
        mm[5] = Math.max(p0.z(), Math.max(p1.z(), p2.z()));
    }

    /**
    Provides an object to text report convertion, optimized for human
    readability and debugging. Do not use this method for serialization
    or persistence purposes.
    @return human readable representation of current triangle
    */
    @Override
    public String toString() {

        return "f < " + p0 + ", " + p1 + ", " + p2 + " >";
    }

}
