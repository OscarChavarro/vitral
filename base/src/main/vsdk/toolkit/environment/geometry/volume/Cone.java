//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.environment.geometry.volume;
import java.io.Serial;

import java.util.ArrayList;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.environment.geometry.RayHit;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.processing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidModeler;

public class Cone extends Solid {
    @Serial private static final long serialVersionUID = 20060502L;
    private static final double NO_HIT = Double.POSITIVE_INFINITY;

    private double r1; // Radius at the base
    private double r2; // Radius at the top
    private double h;  // Height

    private PolyhedralBoundedSolid brepCache;
    private static final int DEFAULT_CIRCUMFERENCE_DIVISIONS = 36;
    private static final int DEFAULT_HEIGHT_DIVISIONS = 1;
    private static final int MIN_CIRCUMFERENCE_DIVISIONS = 3;
    private static final int MIN_HEIGHT_DIVISIONS = 1;

    public Cone(double r1, double r2, double h) {
        this.r1 = r1;
        this.r2 = r2;
        this.h = h;
    }

    public double getBaseRadius()
    {
        return r1;
    }

    public double getTopRadius()
    {
        return r2;
    }

    public double getHeight()
    {
        return h;
    }

    public void setBaseRadius(double val)
    {
        r1 = val;
    }

    public void setTopRadius(double val)
    {
        r2 = val;
    }

    public void setHeight(double val)
    {
        h = val;
    }

    private Ray
    doIntersectionCylinder(Ray inOutRay) {
        return null;
    }

    private Ray
    doIntersectionCylinder(Ray inOutRay, double inR, double inH,
                      RayHit outInfo) 
    {
        double A, B, C, discriminant, t0;
        double ox = inOutRay.origin().x();
        double oy = inOutRay.origin().y();
        double oz = inOutRay.origin().z();
        double dx = inOutRay.direction().x();
        double dy = inOutRay.direction().y();
        double dz = inOutRay.direction().z();

        //- Calcula el termino A --------------------------------------------
        A = VSDK.square(dx) + VSDK.square(dy);
        if ( Math.abs(A) <= VSDK.EPSILON ) return null;

        //- Calcula el termino B --------------------------------------------
        B = 2 * ((dx * ox) + (dy * oy));

        //- Calcula el termino C --------------------------------------------
        C = VSDK.square(ox) + VSDK.square(oy) -
            VSDK.square(inR);

        //- Calcula el discriminant. Si el discriminant no es positivo el -
        //- rayo no intersecta el cilindro. retorna t = 0                      
        discriminant = VSDK.square(B) - 4*A*C;
        if ( discriminant <= VSDK.EPSILON ) return null;

        //- Resuelve la ecuacion cuadratica para las raices de la ecuacion. -
        //- (-B +/- sqrt(B^2 - 4*A*C)) / 2A.                                -
        discriminant = Math.sqrt(discriminant);
        t0 = (-B-discriminant) / (2 * A);

        //- Si t0 es > 0 listo. Si no debemos calcular la otra raiz t1. -----
        if ( t0 > VSDK.EPSILON ) {
            double pz = oz + dz*t0;
            if ( pz > inH || pz < 0 ) {
                return null;
            }

            if ( outInfo != null ) {
                double px = ox + dx*t0;
                double py = oy + dy*t0;
                outInfo.p = new Vector3D(px, py, pz);
                outInfo.n = new Vector3D(px, py, 0).normalized();
            }
            return inOutRay.withT(t0);
        }

        return null;
    }

    private Ray
    doIntersectionCone(Ray inOutRay, double inR, double inH,
                      RayHit outInfo) 
    {
        double A, B, C, discriminant, t0;
        double ox = inOutRay.origin().x();
        double oy = inOutRay.origin().y();
        double oz = inOutRay.origin().z();
        double dx = inOutRay.direction().x();
        double dy = inOutRay.direction().y();
        double dz = inOutRay.direction().z();
        if ( inH <= VSDK.EPSILON ) {
            return null;
        }
        double shiftedOz = oz - inH;
        double ratio = inR / inH;
        double ratioSquared = VSDK.square(ratio);

        //- Calcula el termino A --------------------------------------------
        A = VSDK.square(dx) +
            VSDK.square(dy) -
            VSDK.square(dz * ratio);
        if ( Math.abs(A) <= VSDK.EPSILON ) return null;

        //- Calcula el termino B --------------------------------------------
        B = 2 *
            ((dx * ox) +
             (dy * oy) -
             (dz * shiftedOz * ratioSquared)
             );

        //- Calcula el termino C --------------------------------------------
        C = VSDK.square(ox) +
            VSDK.square(oy) -
            VSDK.square(shiftedOz * ratio);

        //- Calcula el discriminant. Si el discriminant no es positivo el -
        //- rayo no intersecta la esfera. retorna t = 0                      
        discriminant = VSDK.square(B) - 4*A*C;
        if ( discriminant <= VSDK.EPSILON ) return null;

        //- Resuelve la ecuacion cuadratica para las raices de la ecuacion. -
        //- (-B +/- sqrt(B^2 - 4*A*C)) / 2A.                                -
        discriminant = Math.sqrt(discriminant);
        t0 = (-B-discriminant) / (2 * A);

        //- Si t0 es > 0 listo. Si no debemos calcular la otra raiz t1. -----
        if ( t0 > VSDK.EPSILON ) {
            double shiftedPz = shiftedOz + dz*t0;
            if ( shiftedPz > 0 || shiftedPz < -inH ) {
                return null;
            }

            if ( outInfo != null ) {
                double px = ox + dx*t0;
                double py = oy + dy*t0;
                outInfo.p = new Vector3D(px, py, shiftedPz + inH);
                outInfo.n =
                    new Vector3D(px, py, -shiftedPz * ratioSquared).normalized();
            }
            return inOutRay.withT(t0);
        }

        return null;

    }

    private Ray
    doIntersectionTap(Ray inOutRay, double inR, double inH,
                      RayHit outInfo) {
        double dx = inOutRay.direction().x();
        double dy = inOutRay.direction().y();
        double dz = inOutRay.direction().z();
        double ox = inOutRay.origin().x();
        double oy = inOutRay.origin().y();
        double oz = inOutRay.origin().z();

        if ( Math.abs(dz) > VSDK.EPSILON ) {
            double t = (inH - oz) / dz;
            if ( t > VSDK.EPSILON ) {
                double px = ox + dx*t;
                double py = oy + dy*t;
                if ( VSDK.square(px) + VSDK.square(py) < VSDK.square(inR) ) {
                    if ( outInfo != null ) {
                        outInfo.n = new Vector3D(0, 0, 1);
                        outInfo.p = new Vector3D(px, py, inH);
                    }
                    return inOutRay.withT(t);
                }
            }
        }
        return null;
    }

    private static boolean hasHit(double t)
    {
        return t != NO_HIT;
    }

    private double doIntersectionCylinderDistance(Ray inOutRay, double inR, double inH)
    {
        double dx = inOutRay.direction().x();
        double dy = inOutRay.direction().y();
        double dz = inOutRay.direction().z();
        double ox = inOutRay.origin().x();
        double oy = inOutRay.origin().y();
        double oz = inOutRay.origin().z();
        double A = VSDK.square(dx) + VSDK.square(dy);
        if ( Math.abs(A) <= VSDK.EPSILON ) {
            return NO_HIT;
        }

        double B = 2 * ((dx * ox) + (dy * oy));
        double C = VSDK.square(ox) + VSDK.square(oy) - VSDK.square(inR);
        double discriminant = VSDK.square(B) - 4*A*C;
        if ( discriminant <= VSDK.EPSILON ) {
            return NO_HIT;
        }

        double t0 = (-B - Math.sqrt(discriminant)) / (2 * A);
        if ( t0 <= VSDK.EPSILON ) {
            return NO_HIT;
        }
        double pz = oz + dz*t0;
        if ( pz > inH || pz < 0 ) {
            return NO_HIT;
        }
        return t0;
    }

    private double doIntersectionConeDistance(Ray inOutRay, double inR, double inH)
    {
        if ( inH <= VSDK.EPSILON ) {
            return NO_HIT;
        }

        double dx = inOutRay.direction().x();
        double dy = inOutRay.direction().y();
        double dz = inOutRay.direction().z();
        double ox = inOutRay.origin().x();
        double oy = inOutRay.origin().y();
        double shiftedOz = inOutRay.origin().z() - inH;
        double ratio = inR / inH;
        double ratioSquared = VSDK.square(ratio);
        double A = VSDK.square(dx) + VSDK.square(dy) - VSDK.square(dz * ratio);
        if ( Math.abs(A) <= VSDK.EPSILON ) {
            return NO_HIT;
        }

        double B = 2 * ((dx * ox) + (dy * oy) - (dz * shiftedOz * ratioSquared));
        double C =
            VSDK.square(ox) + VSDK.square(oy) - VSDK.square(shiftedOz * ratio);
        double discriminant = VSDK.square(B) - 4*A*C;
        if ( discriminant <= VSDK.EPSILON ) {
            return NO_HIT;
        }

        double t0 = (-B - Math.sqrt(discriminant)) / (2 * A);
        if ( t0 <= VSDK.EPSILON ) {
            return NO_HIT;
        }
        double shiftedPz = shiftedOz + dz*t0;
        if ( shiftedPz > 0 || shiftedPz < -inH ) {
            return NO_HIT;
        }
        return t0;
    }

    private double doIntersectionTapDistance(Ray inOutRay, double inR, double inH)
    {
        double dz = inOutRay.direction().z();
        if ( Math.abs(dz) <= VSDK.EPSILON ) {
            return NO_HIT;
        }

        double t = (inH - inOutRay.origin().z()) / dz;
        if ( t <= VSDK.EPSILON ) {
            return NO_HIT;
        }

        double px = inOutRay.origin().x() + inOutRay.direction().x()*t;
        double py = inOutRay.origin().y() + inOutRay.direction().y()*t;
        if ( VSDK.square(px) + VSDK.square(py) >= VSDK.square(inR) ) {
            return NO_HIT;
        }
        return t;
    }

    private boolean doIntersectionDistanceOnly(Ray inOutRay, RayHit outHit)
    {
        double winnerT = NO_HIT;

        if ( r2 < VSDK.EPSILON && r1 > VSDK.EPSILON ) {
            double bodyT = doIntersectionConeDistance(inOutRay, r1, h);
            double tap1T = doIntersectionTapDistance(inOutRay, r1, 0);
            if ( hasHit(tap1T) && (!hasHit(bodyT) || tap1T < bodyT) ) {
                winnerT = tap1T;
            }
            else if ( hasHit(bodyT) ) {
                winnerT = bodyT;
            }
        }
        else if ( VSDK.equals(r1, r2) ) {
            double bodyT = doIntersectionCylinderDistance(inOutRay, r1, h);
            double tap1T = doIntersectionTapDistance(inOutRay, r1, 0);
            double tap2T = doIntersectionTapDistance(inOutRay, r1, h);

            if ( hasHit(bodyT) &&
                 ((hasHit(tap1T) && bodyT < tap1T) || !hasHit(tap1T)) &&
                 ((hasHit(tap2T) && bodyT < tap2T) || !hasHit(tap2T)) ) {
                winnerT = bodyT;
            }
            else if ( hasHit(tap1T) &&
                      ((hasHit(bodyT) && tap1T < bodyT) || !hasHit(bodyT)) &&
                      ((hasHit(tap2T) && tap1T < tap2T) || !hasHit(tap2T)) ) {
                winnerT = tap1T;
            }
            else if ( hasHit(tap2T) ) {
                winnerT = tap2T;
            }
        }

        if ( !hasHit(winnerT) ) {
            return false;
        }
        if ( outHit != null ) {
            if ( outHit.shouldStoreRay() ) {
                outHit.setRay(inOutRay.withT(winnerT));
            }
            else {
                outHit.setHitDistance(winnerT);
            }
        }
        return true;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doIntersection.
    @param inOutRay
    @return true if given ray intersects current Cone
    */
    public Ray
    doIntersection(Ray inOutRay) {
        RayHit hit = new RayHit();
        if ( doIntersection(inOutRay, hit) ) {
            return hit.ray();
        }
        return null;
    }

    @Override
    public boolean doIntersection(Ray inOutRay, RayHit outHit)
    {
        if ( outHit == null || !outHit.needsAnySurfaceData() ) {
            return doIntersectionDistanceOnly(inOutRay, outHit);
        }

        RayHit infoTap1 = new RayHit();
        RayHit infoTap2 = new RayHit();
        RayHit infoBody = new RayHit();
        Ray bodyHit;
        Ray tap1Hit;
        Ray tap2Hit;
        Ray winner = null;
        RayHit winnerInfo = null;

        if ( r2 < VSDK.EPSILON && r1 > VSDK.EPSILON ) {
            bodyHit = doIntersectionCone(inOutRay, r1, h, infoBody);
            tap1Hit = doIntersectionTap(inOutRay, r1, 0, infoTap1);
            if ( (tap1Hit != null && bodyHit == null) ||
                 (tap1Hit != null && bodyHit != null && (tap1Hit.t() < bodyHit.t())) ) {
                infoTap1.n = infoTap1.n.multiply(-1);
                winner = inOutRay.withT(tap1Hit.t());
                winnerInfo = infoTap1;
            }
            else if ( bodyHit != null ) {
                winner = inOutRay.withT(bodyHit.t());
                winnerInfo = infoBody;
            }
        }
        else if ( VSDK.equals(r1, r2) ) {
            int nearest = -1;
            bodyHit = doIntersectionCylinder(inOutRay, r1, h, infoBody);
            tap1Hit = doIntersectionTap(inOutRay, r1, 0, infoTap1);
            tap2Hit = doIntersectionTap(inOutRay, r1, h, infoTap2);

            if ( bodyHit != null &&
                 ((tap1Hit != null && (bodyHit.t() < tap1Hit.t())) || tap1Hit == null) &&
                 ((tap2Hit != null && (bodyHit.t() < tap2Hit.t())) || tap2Hit == null) ) {
                nearest = 1;
            }
            else if ( tap1Hit != null &&
                      (bodyHit != null && (tap1Hit.t() < bodyHit.t()) || bodyHit == null) &&
                      (tap2Hit != null && (tap1Hit.t() < tap2Hit.t()) || tap2Hit == null) ) {
                nearest = 3;
            }
            else if ( tap2Hit != null ) {
                nearest = 2;
            }

            if ( nearest == 1 ) {
                winner = inOutRay.withT(bodyHit.t());
                winnerInfo = infoBody;
            }
            else if ( nearest == 2 ) {
                winner = inOutRay.withT(tap2Hit.t());
                winnerInfo = infoTap2;
            }
            else if ( nearest == 3 ) {
                winner = inOutRay.withT(tap1Hit.t());
                winnerInfo = infoTap1;
            }
        }

        if ( winner == null ) {
            return false;
        }
        if ( outHit != null ) {
            outHit.setRay(winner);
            outHit.p = new Vector3D(winnerInfo.p);
            outHit.n = new Vector3D(winnerInfo.n).normalized();
            outHit.t = new Vector3D(winnerInfo.t);
            outHit.u = winnerInfo.u;
            outHit.v = winnerInfo.v;
            outHit.material = winnerInfo.material;
            outHit.texture = winnerInfo.texture;
            outHit.normalMap = winnerInfo.normalMap;
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
                                  RayHit outData)
    {
        RayHit hit = new RayHit();
        if ( doIntersection(inRay.withT(Double.MAX_VALUE), hit) ) {
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
        // TODO!
        double [] minmax = new double[6];

        double r = Math.max(r1, r2);

        minmax[0] = -r;
        minmax[1] = -r;
        minmax[2] = 0;
        minmax[3] = r;
        minmax[4] = r;
        minmax[5] = h;

        return minmax;
    }

    @Override
    public PolyhedralBoundedSolid exportToPolyhedralBoundedSolid()
    {
        if ( brepCache == null ) {
            brepCache = buildPolyhedralBoundedSolid(
                DEFAULT_CIRCUMFERENCE_DIVISIONS, DEFAULT_HEIGHT_DIVISIONS);
        }
        return brepCache;
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
        double apexZ)
    {
        _PolyhedralBoundedSolidFace topFace = solid.findFace(1);
        if ( topFace == null || topFace.boundariesList.size() <= 0 ) {
            return;
        }

        _PolyhedralBoundedSolidLoop loop = topFace.boundariesList.get(0);
        _PolyhedralBoundedSolidHalfEdge start = loop.boundaryStartHalfEdge;
        if ( start == null ) {
            return;
        }

        ArrayList<Integer> ringVertexIds = new ArrayList<Integer>();
        _PolyhedralBoundedSolidHalfEdge he = start;
        do {
            ringVertexIds.add(Integer.valueOf(he.startingVertex.id));
            he = he.next();
        } while ( he != start );

        if ( ringVertexIds.size() < 3 ) {
            return;
        }

        int apexVertexId = solid.getMaxVertexId() + 1;
        solid.smev(1, ringVertexIds.get(0).intValue(), apexVertexId,
            new Vector3D(0.0, 0.0, apexZ));

        int i;
        for ( i = 0; i < ringVertexIds.size() - 2; i++ ) {
            solid.mef(1, 1,
                apexVertexId,
                ringVertexIds.get(i).intValue(),
                ringVertexIds.get(i+1).intValue(),
                ringVertexIds.get(i+2).intValue(),
                solid.getMaxFaceId() + 1);
        }

        solid.mef(1, 1,
            apexVertexId,
            ringVertexIds.get(ringVertexIds.size()-2).intValue(),
            ringVertexIds.get(ringVertexIds.size()-1).intValue(),
            ringVertexIds.get(0).intValue(),
            solid.getMaxFaceId() + 1);
    }

    private PolyhedralBoundedSolid buildPolyhedralBoundedSolid(int nsides,
        int heightDivisions)
    {
        PolyhedralBoundedSolid solid;
        Matrix4x4 T, S, M;

        solid = PolyhedralBoundedSolidModeler.createCircularLamina(
            0.0, 0.0, r1, 0.0, nsides
        );

        if ( r2 > VSDK.EPSILON && r1 > VSDK.EPSILON ) {
            double prevRadius = r1;
            double zStep = h / ((double)heightDivisions);
            int i;
            for ( i = 1; i <= heightDivisions; i++ ) {
                double nextRadius = r1 + (r2 - r1) *
                    (((double)i) / ((double)heightDivisions));
                double f = nextRadius / prevRadius;
                T = new Matrix4x4();
                T = T.translation(0.0, 0.0, zStep);
                S = new Matrix4x4();
                S = S.scale(f, f, 1.0);
                M = T.multiply(S);
                PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
                    solid, solid.findFace(1), M);
                prevRadius = nextRadius;
            }
        }
        else if ( r2 <= VSDK.EPSILON && r1 > VSDK.EPSILON ) {
            // Cone case, with optional vertical subdivisions.
            double prevRadius = r1;
            double zStep = h / ((double)heightDivisions);
            int i;
            for ( i = 1; i < heightDivisions; i++ ) {
                double nextRadius = r1 *
                    (1.0 - (((double)i) / ((double)heightDivisions)));
                double f = nextRadius / prevRadius;
                T = new Matrix4x4();
                T = T.translation(0.0, 0.0, zStep);
                S = new Matrix4x4();
                S = S.scale(f, f, 1.0);
                M = T.multiply(S);
                PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
                    solid, solid.findFace(1), M);
                prevRadius = nextRadius;
            }
            closeTopFaceToApex(solid, h);
        }
        return solid;
    }

}
