//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.environment.geometry.volume;
import java.io.Serial;

import vsdk.toolkit.common.RaytraceProfiling;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.environment.geometry.RayHit;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;

public class Sphere extends Solid {
    @Serial private static final long serialVersionUID = 20060502L;

    private double _radius;
    private double _radius_squared;

    private PolyhedralBoundedSolid brepCache;
    private static final int DEFAULT_PARALLELS = 8;
    private static final int DEFAULT_MERIDIANS = 16;
    private static final int MIN_PARALLELS = 2;
    private static final int MIN_MERIDIANS = 3;

    public Sphere(double r) {
        _radius = r;
        _radius_squared = _radius*_radius;
        brepCache = null;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doIntersection.
    @param inout_rayo
    @return true if given ray intersects current Sphere
    */
    public Ray
    doIntersection(Ray inout_rayo) {
        double dx = -inout_rayo.origin().x();
        double dy = -inout_rayo.origin().y();
        double dz = -inout_rayo.origin().z();
        Vector3D direction = inout_rayo.direction();
        double v = direction.x()*dx + direction.y()*dy + direction.z()*dz;

        // Test if the inout_rayo actually intersects the sphere
        double t = _radius_squared + v*v 
                  - dx*dx
                  - dy*dy
                  - dz*dz;
        if ( t < 0 ) {
            return null;
        }

        // Test if the intersection is in the positive
        // inout_rayo direction
        t = v - Math.sqrt(t);
        if ( t < 0 ) {
            return null;
        }

        return inout_rayo.withT(t);
    }

    @Override
    public boolean doIntersection(Ray inRay, RayHit outHit)
    {
        double dx = -inRay.origin().x();
        double dy = -inRay.origin().y();
        double dz = -inRay.origin().z();
        Vector3D direction = inRay.direction();
        double projection =
            direction.x()*dx + direction.y()*dy + direction.z()*dz;

        double discriminant = _radius_squared + projection*projection
                            - dx*dx
                            - dy*dy
                            - dz*dz;
        if ( discriminant < 0 ) {
            return false;
        }

        double t = projection - Math.sqrt(discriminant);
        if ( t < 0 ) {
            return false;
        }

        if ( outHit != null ) {
            if ( outHit.shouldStoreRay() || outHit.needsAnySurfaceData() ) {
                Ray hitRay = inRay.withT(t);
                outHit.setRay(hitRay);
                if ( outHit.needsAnySurfaceData() ) {
                    doExtraInformation(hitRay, t, outHit);
                    outHit.setRay(hitRay);
                }
            }
            else {
                outHit.setHitDistance(t);
            }
        }
        return true;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doExtraInformation.
    @param inT
    */
    public void
    doExtraInformation(Ray inRay, double inT, 
                                  RayHit outData) {
        RaytraceProfiling.recordGeometryDetailComputation();
        boolean needsNormalVector =
            outData.needsNormal() ||
            outData.needsTextureCoordinates() ||
            outData.needsTangent();
        if ( !outData.needsPoint() && !needsNormalVector ) {
            return;
        }

        Vector3D point = new Vector3D(
            inRay.origin().x() + inT*inRay.direction().x(),
            inRay.origin().y() + inT*inRay.direction().y(),
            inRay.origin().z() + inT*inRay.direction().z());
        if ( outData.needsPoint() ) {
            outData.p = point;
        }

        Vector3D normal = null;
        if ( needsNormalVector ) {
            normal = new Vector3D(point).normalized();
            if ( outData.needsNormal() ) {
                outData.n = normal;
            }
        }

        if ( !outData.needsTextureCoordinates() && !outData.needsTangent() ) {
            return;
        }

        double theta;
        double phi = Math.acos(normal.z());

        if ( normal.x() > VSDK.EPSILON ) {
            theta = Math.atan(normal.y() / normal.x()) + 3*Math.PI/2;
        }
        else if ( normal.x() < VSDK.EPSILON ) {
            theta = Math.atan(normal.y() / normal.x()) + 3*Math.PI/2;
            theta += Math.PI;
            if ( theta > 2*Math.PI ) {
                theta -= 2*Math.PI;
            }
        }
        else {
            theta = 0.0;
        }

        if ( outData.needsTextureCoordinates() ) {
            outData.u = ((theta+Math.PI/2)/(2*Math.PI));
            outData.v = 1 - (phi / Math.PI);
        }
        if ( outData.needsTangent() ) {
            outData.t = new Vector3D(
                Math.sin(theta-Math.PI/2),
                -Math.cos(theta-Math.PI/2),
                0);
        }
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doContainmentTest.
    @return INSIDE, OUTSIDE or LIMIT constant value
    */
    @Override
    public int doContainmentTest(Vector3D p, double distanceTolerance)
    {
        double l = p.length();
        if ( l < _radius - distanceTolerance ) {
            return INSIDE;
        }
        else if ( l > _radius + distanceTolerance ) {
            return OUTSIDE;
        }
        return LIMIT;
    }

    /**
    @return a new 6 valued double array containing the coordinates of a min-max
    bounding box for current geometry.
    */
    @Override
    public double[] getMinMax()
    {
        double[] minmax = new double[6];
        for ( int i = 0; i < 3; i++ ) {
            minmax[i] = -_radius;
        }
        for ( int i = 3; i < 6; i++ ) {
            minmax[i] = _radius;
        }
        return minmax;
    }

    public double getRadius()
    {
        return _radius;
    }

    public double getRadiusSquared()
    {
        return _radius_squared;
    }

    public void setRadius(double r)
    {
        _radius = r;
        _radius_squared = r*r;
    }

    private static Vector3D
    spherePosition(double theta, double t, double r)
    {
        double phi = (t-0.5)*Math.PI;
        return new Vector3D(
            Math.cos(phi) * Math.cos(theta) * r,
            Math.cos(phi) * Math.sin(theta) * r,
            Math.sin(phi) * r);
    }

    @Override
    public PolyhedralBoundedSolid exportToPolyhedralBoundedSolid()
    {
        if ( brepCache == null ) {
            brepCache = buildPolyhedralBoundedSolid(DEFAULT_MERIDIANS,
                DEFAULT_PARALLELS);
        }
        return brepCache;
    }

    public PolyhedralBoundedSolid exportToPolyhedralBoundedSolid(
        int meridians, int parallels)
    {
        int normalizedMeridians = Math.max(MIN_MERIDIANS, meridians);
        int normalizedParallels = Math.max(MIN_PARALLELS, parallels);

        if ( normalizedMeridians == DEFAULT_MERIDIANS &&
             normalizedParallels == DEFAULT_PARALLELS ) {
            return exportToPolyhedralBoundedSolid();
        }

        return buildPolyhedralBoundedSolid(normalizedMeridians,
            normalizedParallels);
    }

    /**
    Given current sphere, this method generates a "polyhedral ball"
    aproximation.
    Note that this method follows a similar strategy to the one proposed on
    function "ball", from program [MANT1988].12.6, but it is expressed entirely
    on "low level" operators, and doesn't rely on the previous availability of
    generalized rotational sweep operations.
    */
    private PolyhedralBoundedSolid buildPolyhedralBoundedSolid(
        int nmeridians, int nparalels)
    {
        double theta;
        double phi;
        double dtheta = 2*Math.PI / ((double)nmeridians);
        double dphi = 1.0 / ((double)nparalels);
        int i, base2, base1;
        Vector3D pos;

        PolyhedralBoundedSolid solid;

        //- Build triangles for lower cap ---------------------------------
        solid = new PolyhedralBoundedSolid();
        pos = new Vector3D(0, 0, -_radius);
        solid.mvfs(pos, 1, 1);

        pos = new Vector3D();
        pos = spherePosition(dtheta, dphi, _radius);
        solid.smev(1, 1, 3, pos);
        pos = new Vector3D();
        pos = spherePosition(0, dphi, _radius);
        solid.smev(1, 3, 2, pos);

        solid.mef(1, 1, 1, 3, 2, 3, 2);

        for ( i = 2; i < nmeridians; i++ ) {
            theta = dtheta * ((double)i);
            pos = new Vector3D();
            pos = spherePosition(theta, dphi, _radius);
            solid.smev(1, 1, (i+1)+1, pos);
            // Next face is <(1), (i+1), (i+0)>
            solid.mef(1,        /* seed face, always face 1 */
                      1,        /* seed face, always face 1 */
                      (i+0)+1,  /* start of half edge 1 */
                      (1),      /* end of half edge 1 */
                      (i+1)+1,  /* start of half edge 2 */
                      (1),      /* end of half edge 2 */
                      i+1       /* new face id */);
        }
        // Next face is <(1), (2), (i+1)>
        solid.mef(1,        /* seed face, always face 1 */
                  1,        /* seed face, always face 1 */
                  (i+1),    /* start of half edge 1 */
                  (1),      /* end of half edge 1 */
                  (2),      /* start of half edge 2 */
                  (3),      /* end of half edge 2 */
                  i+1       /* new face id */);
        base2 = i+2;
        base1 = 2;

        //- Build side quads for sphere body ------------------------------
        int p;
        for ( p = 0; p < nparalels-2; p++ ) {
            phi = ((double)(p+2)) / ((double)nparalels);
            for ( i = 0; i < nmeridians; i++ ) {
                theta = dtheta * ((double)i);
                pos = new Vector3D();
                pos = spherePosition(theta, phi, _radius);
                solid.smev(1, (i)+base1, (i)+base2, pos);
                if ( i > 0 ) {
                    // Next face is <(i), (i+base2), (i-1+base2), (i-1)>
                    solid.mef(1,           /* seed face, always face 1 */
                              1,           /* seed face, always face 1 */
                              (i-1)+base2, /* start of half edge 1 */
                              (i-1)+base1, /* end of half edge 1 */
                              (i)+base2,   /* start of half edge 2 */
                              (i)+base1,   /* end of half edge 2 */
                              base2+i+1    /* new face id */);
                }
            }
            solid.mef(1,           /* seed face, always face 1 */
                      1,           /* seed face, always face 1 */
                      (i+base2-1), /* start of half edge 1 */
                      (base1+i-1), /* end of half edge 1 */
                      (base2),     /* start of half edge 2 */
                      (base2+1),   /* end of half edge 2 */
                      base2+i+1    /* new face id */);
            base1 = base2;
            base2 += nmeridians;
        }

        //- Build triangles for upper cap --------------------------------
        pos = new Vector3D(0, 0, _radius);
        solid.smev(1, base1, base2, pos);

        for ( i = 0; i < nmeridians-2; i++ ) {
            solid.mef(1,           /* seed face, always face 1 */
                      1,           /* seed face, always face 1 */
                      base2,       /* start of half edge 1 */
                      base1+i,     /* end of half edge 1 */
                      base1+i+1,   /* start of half edge 2 */
                      base1+i+2,   /* end of half edge 2 */
                      base2+i+1    /* new face id */);
        }

        solid.mef(1,           /* seed face, always face 1 */
                  1,           /* seed face, always face 1 */
                  base2,       /* start of half edge 1 */
                  base1+i,     /* end of half edge 1 */
                  base1+i+1,   /* start of half edge 2 */
                  base1,   /* end of half edge 2 */
                  base2+i+1    /* new face id */);

        //-----------------------------------------------------------------
        return solid;
    }

    /**
    Given a (thetha, phi) spherical coordinate in the surface of current
    Sphere, this method writes on to `p` Vector3D the (x, y, z) coordinates
    of the corresponding point on Sphere's surface.
    \todo  check this method for efficiency improvement
    @param p
    @param theta
    @param phi
    */
    public Vector3D
    spherePosition(double theta, double phi)
    {
        return new Vector3D(
            Math.cos(phi) * Math.cos(theta) * _radius,
            -Math.cos(phi) * Math.sin(theta) * _radius,
            Math.sin(phi) * _radius);
    }

    /**
    Given a (thetha, phi) spherical coordinate in the surface of current
    Sphere, this method writes on to `n` Vector3D the (nx, ny, nz) coordinates
    of the surface normal at corresponding point on Sphere's surface.
    \todo  check this method for efficiency improvement
    @param n
    @param theta
    @param phi
    */
    public Vector3D
    sphereNormal(double theta, double phi)
    {
        return new Vector3D(
            Math.cos(phi) * Math.cos(theta),
            -Math.cos(phi) * Math.sin(theta),
            Math.sin(phi));
    }

    /**
    Given a (thetha, phi) spherical coordinate in the surface of current
    Sphere, this method writes on to `n` Vector3D the (tx, ty, tz) coordinates
    of the surface tangent at corresponding point on Sphere's surface. Tangents
    are aligned with respect to Sphere's equator.
    \todo  check this method for efficiency improvement
    \todo  check this method for efficiency improvement
    @param t
    @param theta
    @param phi
    */
    public Vector3D
    sphereTangent(double theta, double phi)
    {
        return new Vector3D(
            Math.sin(theta),
            Math.cos(theta),
            0);
    }

    /**
    Given a (thetha, phi) spherical coordinate in the surface of current
    Sphere, this method writes on to `n` Vector3D the (bx, by, bz) coordinates
    of the surface tangent binormal at corresponding point on Sphere's surface. 
    Tangents binormals are perpendicular to both normal and tangent.
    \todo  check this method for efficiency improvement
    @param b
    @param theta
    @param phi
    */
    public Vector3D
    sphereBinormal(double theta, double phi)
    {
        return new Vector3D(
            -Math.sin(phi)*Math.cos(theta),
            Math.sin(phi)*Math.sin(theta),
            Math.cos(phi)*Math.cos(theta)*Math.cos(theta) +
            Math.cos(phi)*Math.sin(theta)*Math.sin(theta));
    }

}
