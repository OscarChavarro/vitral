package vsdk.toolkit.environment.geometry.volume;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;
import java.io.Serial;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidModeler;

public class Arrow extends Solid {
    @Serial private static final long serialVersionUID = 20060502L;
    private static final double NO_HIT = Double.POSITIVE_INFINITY;

    private double baseLength;
    private double headLength;
    private double baseRadius;
    private double headRadius;

    private Cone baseCylinder;
    private Cone headCone;
    private Cone lastElement;

    private static final int DEFAULT_CIRCUMFERENCE_DIVISIONS = 36/4;
    private static final int DEFAULT_HEIGHT_DIVISIONS = 1;
    private static final int MIN_CIRCUMFERENCE_DIVISIONS = 3;
    private static final int MIN_HEIGHT_DIVISIONS = 1;

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
        Vector3Dd tr = new Vector3Dd(0, 0, -baseLength);

        Ray headRay = new Ray(inOutRay.origin().add(tr), inOutRay.direction());
        Ray baseRay = new Ray(inOutRay);

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

    private boolean doIntersectionDistanceOnly(Ray inRay, RayHit outHit)
    {
        Vector3Dd shiftedHeadOrigin = new Vector3Dd(
            inRay.origin().x(),
            inRay.origin().y(),
            inRay.origin().z() - baseLength);
        Ray shiftedHeadRay =
            new Ray(shiftedHeadOrigin, inRay.direction(), inRay.t());

        RayHit candidateHit;
        boolean shouldStoreRay = false;
        if ( outHit != null ) {
            candidateHit = outHit;
            shouldStoreRay = outHit.shouldStoreRay();
        }
        else {
            candidateHit = new RayHit(RayHit.DETAIL_NONE, false);
        }
        candidateHit.setStoreRay(false);

        double baseT = NO_HIT;
        candidateHit.resetForDistanceOnly();
        if ( baseCylinder.doIntersection(inRay, candidateHit) ) {
            baseT = candidateHit.hitDistance();
        }

        double headT = NO_HIT;
        candidateHit.resetForDistanceOnly();
        if ( headCone.doIntersection(shiftedHeadRay, candidateHit) ) {
            headT = candidateHit.hitDistance();
        }

        double winnerT = baseT < headT ? baseT : headT;
        if ( winnerT == NO_HIT ) {
            return false;
        }

        if ( outHit != null ) {
            if ( shouldStoreRay ) {
                outHit.setRay(inRay.withT(winnerT));
            }
            else {
                outHit.setHitDistance(winnerT);
            }
            outHit.setStoreRay(shouldStoreRay);
        }
        return true;
    }

    @Override
    public boolean doIntersection(Ray inRay, RayHit outHit)
    {
        if ( outHit == null || !outHit.needsAnySurfaceData() ) {
            return doIntersectionDistanceOnly(inRay, outHit);
        }

        Vector3Dd tr = new Vector3Dd(0, 0, -baseLength);
        Ray shiftedHeadRay = new Ray(inRay.origin().add(tr), inRay.direction(), inRay.t());

        RayHit baseHit = new RayHit(outHit.requiredDetailMask());
        RayHit headHit = new RayHit(outHit.requiredDetailMask());
        boolean hasBase = baseCylinder.doIntersection(inRay, baseHit);
        boolean hasHead = headCone.doIntersection(shiftedHeadRay, headHit);

        if ( !hasBase && !hasHead ) {
            return false;
        }

        double baseT =
            hasBase ? (baseHit.ray() != null ? baseHit.ray().t() : baseHit.hitDistance()) :
            NO_HIT;
        double headT =
            hasHead ? (headHit.ray() != null ? headHit.ray().t() : headHit.hitDistance()) :
            NO_HIT;

        if ( hasBase && (!hasHead || baseT < headT) ) {
            outHit.clone(baseHit);
            outHit.setRay(inRay.withT(baseT));
        }
        else {
            outHit.clone(headHit);
            outHit.setRay(inRay.withT(headT));
            if ( outHit.p != null ) {
                outHit.p = new Vector3Dd(outHit.p.x(), outHit.p.y(), outHit.p.z() + baseLength);
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
           RayHit outData) {
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
        return buildPolyhedralBoundedSolid(
            DEFAULT_CIRCUMFERENCE_DIVISIONS, DEFAULT_HEIGHT_DIVISIONS);
    }

    public PolyhedralBoundedSolid exportToPolyhedralBoundedSolid(
        int circumferenceDivisions, int heightDivisions)
    {
        int normalizedCircumferenceDivisions = Math.max(
            MIN_CIRCUMFERENCE_DIVISIONS, circumferenceDivisions);
        int normalizedHeightDivisions = Math.max(
            MIN_HEIGHT_DIVISIONS, heightDivisions);

        if ( normalizedCircumferenceDivisions ==
                 DEFAULT_CIRCUMFERENCE_DIVISIONS &&
             normalizedHeightDivisions == DEFAULT_HEIGHT_DIVISIONS ) {
            return exportToPolyhedralBoundedSolid();
        }

        return buildPolyhedralBoundedSolid(normalizedCircumferenceDivisions,
            normalizedHeightDivisions);
    }

    /**
    Current implementation of the cylinder follows the idea suggested on
    section [MANT1988].12.3.1 and program [MANT1988].12.4, where the
    cylinder is built upon a circular lamina base and an extrusion 
    (translational sweep) operation. The cone case is done manually,
    */
    private static void closeTopFaceToApex(PolyhedralBoundedSolid solid,
        int nsides, double apexZ)
    {
        Vector3Dd apex = new Vector3Dd(0, 0, apexZ);
        int i;
        int base1 = 2*nsides+1;
        int base2 = 3*nsides+1;

        PolyhedralBoundedSolidEulerOperators.smev(
            solid, 1, base1, base2, apex);

        for ( i = 0; i < nsides-2; i++ ) {
            PolyhedralBoundedSolidEulerOperators.mef(solid, 1,
                1,
                base2,
                base1+i,
                base1+i+1,
                base1+i+2,
                base2+i+1);
        }

        PolyhedralBoundedSolidEulerOperators.mef(solid, 1,
            1,
            base2,
            base1+i,
            base1+i+1,
            base1,
            base2+i+1);
    }

    private PolyhedralBoundedSolid buildPolyhedralBoundedSolid(int nsides,
        int heightDivisions)
    {
        PolyhedralBoundedSolid solid;
        Matrix4x4d T, S, M;

        solid = PolyhedralBoundedSolidModeler.createCircularLamina(
            0.0, 0.0, baseRadius, 0.0, nsides
        );

        // Cylinder case
        double cylinderZStep = baseLength / ((double)heightDivisions);
        int i;
        for ( i = 0; i < heightDivisions; i++ ) {
            T = new Matrix4x4d();
            T = T.translation(0.0, 0.0, cylinderZStep);
            PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
                solid, solid.findFace(1), T);
        }

        T = new Matrix4x4d();
        T = T.translation(0.0, 0.0, 0);
        double f = headRadius / baseRadius;
        S = new Matrix4x4d();
        S = S.scale(f, f, 1);
        M = T.multiply(S);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
            solid, solid.findFace(1), M);

        // Cone case
        double prevRadius = headRadius;
        double coneZStep = headLength / ((double)heightDivisions);
        for ( i = 1; i < heightDivisions; i++ ) {
            double nextRadius = headRadius *
                (1.0 - (((double)i) / ((double)heightDivisions)));
            double coneScale = prevRadius > VSDK.EPSILON ?
                nextRadius / prevRadius : 0.0;
            T = new Matrix4x4d();
            T = T.translation(0.0, 0.0, coneZStep);
            S = new Matrix4x4d();
            S = S.scale(coneScale, coneScale, 1.0);
            M = T.multiply(S);
            PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
                solid, solid.findFace(1), M);
            prevRadius = nextRadius;
        }

        closeTopFaceToApex(solid, nsides, baseLength + headLength);

        return solid;
    }

}
