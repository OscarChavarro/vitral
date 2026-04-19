//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.environment.geometry.volume;

import java.util.ArrayList;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.environment.geometry.GeometryIntersectionInformation;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.processing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidModeler;

public class Cone extends Solid {
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20060502L;

    private double r1; // Radius at the base
    private double r2; // Radius at the top
    private double h;  // Height

    private GeometryIntersectionInformation lastInfo;
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

    private boolean
    doIntersectionCylinder(Ray inOutRay) {
        return false;
    }

    private boolean
    doIntersectionCylinder(Ray inOutRay, double inR, double inH,
                      GeometryIntersectionInformation outInfo) 
    {
        double A, B, C, discriminant, t0;

        //- Translacion para concordar con la interpretacion AQUYNZA --------
        Ray r = new Ray(inOutRay);
        r.direction = r.direction.normalized();

        //- Calcula el termino A --------------------------------------------
        A = VSDK.square(r.direction.x()) +
            VSDK.square(r.direction.y());

        //- Calcula el termino B --------------------------------------------
        B = 2 *
            ( (r.direction.x() * r.origin.x()) +
              (r.direction.y() * r.origin.y()) );

        //- Calcula el termino C --------------------------------------------
        C = VSDK.square(r.origin.x()) +
            VSDK.square(r.origin.y()) -
            VSDK.square(inR);

        //- Calcula el discriminant. Si el discriminant no es positivo el -
        //- rayo no intersecta el cilindro. retorna t = 0                      
        discriminant = VSDK.square(B) - 4*A*C;
        if ( discriminant <= VSDK.EPSILON ) return false;

        //- Resuelve la ecuacion cuadratica para las raices de la ecuacion. -
        //- (-B +/- sqrt(B^2 - 4*A*C)) / 2A.                                -
        discriminant = Math.sqrt(discriminant);
        t0 = (-B-discriminant) / (2 * A);

        //- Si t0 es > 0 listo. Si no debemos calcular la otra raiz t1. -----
        if ( t0 > VSDK.EPSILON ) {
            // OJO: Aqui va el calculo del punto y la normal!
            outInfo.p = r.origin.add(r.direction.multiply(t0));
            if ( outInfo.p.z() > inH || outInfo.p.z() < 0 ) {
                return false;
            }

            // Se calcula la normal como el gradiente de la formula del cono,
            // notese que aqui se obtiene un vector escalado en 1/2 respecto al
            // gradiente (para ahorrar multiplicaciones)
            outInfo.n = outInfo.n.withX(outInfo.p.x());
            outInfo.n = outInfo.n.withY(outInfo.p.y());
            outInfo.n = outInfo.n.withZ(0);
            outInfo.n = outInfo.n.normalized();
            inOutRay.t = t0;

            return true;
        }

        return false;
    }

    private boolean
    doIntersectionCone(Ray inOutRay, double inR, double inH,
                      GeometryIntersectionInformation outInfo) 
    {
        double A, B, C, discriminant, t0;

        //- Translacion para concordar con la interpretacion AQUYNZA --------
        Ray r = new Ray(inOutRay);
        r.origin = r.origin.withZ(r.origin.z() - inH);
        r.direction = r.direction.normalized();

        //- Calcula el termino A --------------------------------------------
        A = VSDK.square(r.direction.x()) +
            VSDK.square(r.direction.y()) -
            VSDK.square(r.direction.z() * inR / inH);

        //- Calcula el termino B --------------------------------------------
        B = 2 *
            ((r.direction.x() * r.origin.x()) +
             (r.direction.y() * r.origin.y()) -
             (r.direction.z() * r.origin.z() * VSDK.square(inR)) / 
             VSDK.square(inH)
             );

        //- Calcula el termino C --------------------------------------------
        C = VSDK.square(r.origin.x()) +
            VSDK.square(r.origin.y()) -
            VSDK.square(r.origin.z() * inR / inH);

        //- Calcula el discriminant. Si el discriminant no es positivo el -
        //- rayo no intersecta la esfera. retorna t = 0                      
        discriminant = VSDK.square(B) - 4*A*C;
        if ( discriminant <= VSDK.EPSILON ) return false;

        //- Resuelve la ecuacion cuadratica para las raices de la ecuacion. -
        //- (-B +/- sqrt(B^2 - 4*A*C)) / 2A.                                -
        discriminant = Math.sqrt(discriminant);
        t0 = (-B-discriminant) / (2 * A);

        //- Si t0 es > 0 listo. Si no debemos calcular la otra raiz t1. -----
        if ( t0 > VSDK.EPSILON ) {
            // OJO: Aqui va el calculo del punto y la normal!
            outInfo.p = r.origin.add(r.direction.multiply(t0));
            if ( outInfo.p.z() > 0 || outInfo.p.z() < -inH ) {
                return false;
            }

            // Se calcula la normal como el gradiente de la formula del cono,
            // notese que aqui se obtiene un vector escalado en 1/2 respecto al
            // gradiente (para ahorrar multiplicaciones)
            outInfo.n = outInfo.n.withX(outInfo.p.x());
            outInfo.n = outInfo.n.withY(outInfo.p.y());
            outInfo.n = outInfo.n.withZ(-outInfo.p.z() * VSDK.square(inR/inH));
            outInfo.n = outInfo.n.normalized();
    
            outInfo.p = outInfo.p.withZ(outInfo.p.z() + inH);
            inOutRay.t = t0;

            return true;
        }

        return false;

    }

    private boolean
    doIntersectionTap(Ray inOutRay, double inR, double inH,
                      GeometryIntersectionInformation outInfo) {
        double t;
        Vector3D p;
        Vector3D proy;
        Vector3D o = new Vector3D(0, 0, 0);

        if ( Math.abs(inOutRay.direction.z()) > VSDK.EPSILON ) {
            t = (inH - inOutRay.origin.z()) / inOutRay.direction.z();
            if ( t > VSDK.EPSILON ) {
                p = inOutRay.origin.add(inOutRay.direction.multiply(t));
                proy = new Vector3D(p.x(), p.y(), 0);
                if ( VSDK.vectorDistance(proy, o) < inR ) {
                    inOutRay.t = t;
                    outInfo.n = outInfo.n.withX(0);
                    outInfo.n = outInfo.n.withY(0);
                    outInfo.n = outInfo.n.withZ(1);
                    outInfo.p = p;
                    return true;
                }
            }
        }
        return false;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doIntersection.
    @param inOutRay
    @return true if given ray intersects current Cone
    */
    @Override
    public boolean
    doIntersection(Ray inOutRay) {
        Ray bodyRay;
        Ray tap1Ray;
        Ray tap2Ray;
        GeometryIntersectionInformation infoTap1;
        GeometryIntersectionInformation infoTap2;
        GeometryIntersectionInformation infoBody;
        boolean tap1, tap2, body;

        bodyRay = new Ray(inOutRay);
        tap1Ray = new Ray(inOutRay);
        tap2Ray = new Ray(inOutRay);
        infoTap1 = new GeometryIntersectionInformation();
        infoTap2 = new GeometryIntersectionInformation();
        infoBody = new GeometryIntersectionInformation();

        //- Cone case -----------------------------------------------------
        if ( r2 < VSDK.EPSILON && r1 > VSDK.EPSILON ) {
            body = doIntersectionCone(bodyRay, r1, h, infoBody);
            tap1 = doIntersectionTap(tap1Ray, r1, 0, infoTap1);
            if ( (tap1 && !body) ||
                 (tap1 && body && (tap1Ray.t < bodyRay.t)) ) {
                inOutRay.origin = tap1Ray.origin;
                inOutRay.direction = tap1Ray.direction;
                inOutRay.t = tap1Ray.t;
                infoTap1.n = infoTap1.n.multiply(-1);
                lastInfo = infoTap1;
                return true;
            }
            if ( (!tap1 && body) ||
                 (tap1 && body && (tap1Ray.t > bodyRay.t)) ) {
                inOutRay.origin = bodyRay.origin;
                inOutRay.direction = bodyRay.direction;
                inOutRay.t = bodyRay.t;
                lastInfo = infoBody;
                return true;
            }
        }

        //- Cylinder case -------------------------------------------------
        int cercano = -1;

        if ( VSDK.equals(r1, r2) ) {
            body = doIntersectionCylinder(bodyRay, r1, h, infoBody);
            tap1 = doIntersectionTap(tap1Ray, r1, 0, infoTap1);
            tap2 = doIntersectionTap(tap2Ray, r1, h, infoTap2);
    
            if ( body && 
                 ((tap1 && (bodyRay.t < tap1Ray.t)) || !tap1) && 
                 ((tap2 && (bodyRay.t < tap2Ray.t)) || !tap2) ) {
                cercano = 1;
            }
            else if ( tap1 && 
                      (body && (tap1Ray.t < bodyRay.t) || !body) && 
                      (tap2 && (tap1Ray.t < tap2Ray.t) || !tap2) ) {
                cercano = 3;
            }
            else if ( tap2 ) {
                cercano = 2;
            }

            if ( cercano == 1 ) {
                inOutRay.origin = bodyRay.origin;
                inOutRay.direction = bodyRay.direction;
                inOutRay.t = bodyRay.t;
                lastInfo = infoBody;
                return true;
              }
              else if ( cercano == 2 )  {
                inOutRay.origin = tap2Ray.origin;
                inOutRay.direction = tap2Ray.direction;
                inOutRay.t = tap2Ray.t;
                lastInfo = infoTap2;
                return true;
              }
              else if ( cercano == 3 ) {
                inOutRay.origin = tap1Ray.origin;
                inOutRay.direction = tap1Ray.direction;
                inOutRay.t = tap1Ray.t;
                lastInfo = infoTap1;
                return true;
            }
        }
    
        return false;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doExtraInformation.
    @param inRay
    @param inT
    @param outData
    */
    @Override
    public void
    doExtraInformation(Ray inRay, double inT, 
                                  GeometryIntersectionInformation outData)
    {
        outData.p = lastInfo.p;
        outData.n = lastInfo.n;
        outData.n = outData.n.normalized();
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
                T.translation(0.0, 0.0, zStep);
                S = new Matrix4x4();
                S.scale(f, f, 1.0);
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
                T.translation(0.0, 0.0, zStep);
                S = new Matrix4x4();
                S.scale(f, f, 1.0);
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
