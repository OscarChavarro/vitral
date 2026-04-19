package vsdk.toolkit.environment.geometry.volume;
import java.io.Serial;

import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.GeometryIntersectionInformation;
import vsdk.toolkit.environment.geometry.RayHit;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.processing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidModeler;

public class Arrow extends Solid {
    @Serial private static final long serialVersionUID = 20060502L;

    private double baseLength;
    private double headLength;
    private double baseRadius;
    private double headRadius;

    private Cone baseCylinder;
    private Cone headCone;
    private Cone lastElement;

    private PolyhedralBoundedSolid brepCache;

    public Arrow(double baseLength, double headLength, double baseRadius, double headRadius) {
        this.baseLength = baseLength;
        this.headLength = headLength;
        this.baseRadius = baseRadius;
        this.headRadius = headRadius;
        baseCylinder = new Cone(baseRadius, baseRadius, baseLength);
        headCone = new Cone(headRadius, 0, headLength);
        lastElement = baseCylinder;
    }

    public double getBaseLength()
    {
        return baseLength;
    }

    public void setBaseLength(double val)
    {
        baseLength = val;
        baseCylinder.setHeight(val);
    }

    public double getHeadLength()
    {
        return headLength;
    }

    public void setHeadLength(double val)
    {
        headLength = val;
        headCone.setHeight(val);
    }

    public double getBaseRadius()
    {
        return baseRadius;
    }

    public void setBaseRadius(double val)
    {
        baseRadius = val;
        baseCylinder.setBaseRadius(val);
        baseCylinder.setTopRadius(val);
    }

    public double getHeadRadius()
    {
        return headRadius;
    }

    public void setHeadRadius(double val)
    {
        headRadius = val;
        headCone.setBaseRadius(val);
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doIntersection.
    @param inOutRay
    @return true if given ray intersects current Arrow
    */
    public Ray
    doIntersection(Ray inOutRay) {
        Ray headRay, baseRay;
        GeometryIntersectionInformation headInfo, baseInfo;
        Vector3D tr = new Vector3D(0, 0, -baseLength);

        headRay = new Ray(inOutRay.origin().add(tr), inOutRay.direction());
        baseRay = new Ray(inOutRay);

        Ray baseHit = baseCylinder.doIntersection(baseRay);
        Ray headHit = headCone.doIntersection(headRay);

        if ( (baseHit != null && headHit == null) ||
             (baseHit != null && headHit != null && (baseHit.t() < headHit.t()) ) ) {
            lastElement = baseCylinder;
            return inOutRay.withT(baseHit.t());
        }
        else if ( (baseHit == null && headHit != null) ||
                  (baseHit != null && headHit != null && (headHit.t() < baseHit.t()) ) ) {
            lastElement = headCone;
            return inOutRay.withT(headHit.t());
        }

        return null;
    }

    @Override
    public boolean doIntersection(Ray inRay, RayHit outHit)
    {
        Vector3D tr = new Vector3D(0, 0, -baseLength);
        Ray shiftedHeadRay = new Ray(inRay.origin().add(tr), inRay.direction(), inRay.t());

        RayHit baseHit = new RayHit();
        RayHit headHit = new RayHit();
        boolean hasBase = baseCylinder.doIntersection(inRay, baseHit);
        boolean hasHead = headCone.doIntersection(shiftedHeadRay, headHit);

        if ( !hasBase && !hasHead ) {
            return false;
        }
        if ( outHit != null ) {
            if ( hasBase && (!hasHead || baseHit.ray().t() < headHit.ray().t()) ) {
                outHit.clone(baseHit);
                outHit.setRay(inRay.withT(baseHit.ray().t()));
            }
            else {
                outHit.clone(headHit);
                outHit.setRay(inRay.withT(headHit.ray().t()));
                outHit.p = outHit.p.withZ(outHit.p.z() + baseLength);
            }
        }
        return true;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doExtraInformation.
    @param inRay
    @param inT
    @param outData
    */
    public void
    doExtraInformation(Ray inRay, double inT, 
           GeometryIntersectionInformation outData) {
        RayHit hit = new RayHit();
        if ( doIntersection(inRay.withT(inT), hit) ) {
            outData.clone(hit);
        }
    }

    /**
    @return a new 6 valued double array containing the coordinates of a min-max
    bounding box for current geometry.
    */
    @Override
    public double[] getMinMax()
    {
        double [] minmax = new double[6];
        double r = Math.max(baseRadius, headRadius);

        minmax[0] = -r;
        minmax[1] = -r;
        minmax[2] = 0;
        minmax[3] = r;
        minmax[4] = r;
        minmax[5] = baseLength + headLength;

        return minmax;
    }

    @Override
    public PolyhedralBoundedSolid exportToPolyhedralBoundedSolid()
    {
        if ( brepCache == null ) {
            brepCache = buildPolyhedralBoundedSolid();
        }
        return brepCache;
    }

    /**
    Current implementation of the cylinder follows the idea suggested on
    section [MANT1988].12.3.1 and program [MANT1988].12.4, where the
    cylinder is built upon a circular lamina base and an extrusion 
    (translational sweep) operation. The cone case is done manually,
    */
    private PolyhedralBoundedSolid buildPolyhedralBoundedSolid()
    {
        PolyhedralBoundedSolid solid;
        Matrix4x4 T, S, M;
        int nsides = 36/4;

        solid = PolyhedralBoundedSolidModeler.createCircularLamina(
            0.0, 0.0, baseRadius, 0.0, nsides
        );

        // Cylinder case
        T = new Matrix4x4();
        T = T.translation(0.0, 0.0, baseLength);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
            solid, solid.findFace(1), T);

        T = new Matrix4x4();
        T = T.translation(0.0, 0.0, 0);
        double f = headRadius / baseRadius;
        S = new Matrix4x4();
        S = S.scale(f, f, 1);
        M = T.multiply(S);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
            solid, solid.findFace(1), M);

        // Cone case
        Vector3D apex;
        int i;
        int base1 = 2*nsides+1;
        int base2 = 3*nsides+1;

        apex = new Vector3D(0, 0, baseLength + headLength);
        solid.smev(1, base1, base2, apex);

        for ( i = 0; i < nsides-2; i++ ) {
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

        return solid;
    }

}
