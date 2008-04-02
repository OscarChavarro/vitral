//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - April 21 2007 - Oscar Chavarro: Original base version                 =
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =
//===========================================================================

// Java classes
import java.util.ArrayList;

// VitralSDK classes
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.environment.geometry.Arrow;
import vsdk.toolkit.environment.geometry.Box;
import vsdk.toolkit.environment.geometry.Cone;
import vsdk.toolkit.environment.geometry.InfinitePlane;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.environment.geometry.ParametricCurve;
import vsdk.toolkit.environment.geometry.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidVertex;
import vsdk.toolkit.render.awt.AwtFontReader;
import vsdk.toolkit.processing.GeometricModeler;

public class PolyhedralBoundedSolidModelingTools
{
    /**    
    */
    public static PolyhedralBoundedSolid createBox(Vector3D boxSize)
    {
        PolyhedralBoundedSolid solid;

        Matrix4x4 R = new Matrix4x4();
        R.translation(0.55, 0.55, 0.55);

        Box b = new Box(boxSize);
        solid = b.exportToPolyhedralBoundedSolid();
        solid.applyTransformation(R);
        solid.validateModel();
        return solid;
    }

    /**    
    */
    public static PolyhedralBoundedSolid createSphere(double r)
    {
        PolyhedralBoundedSolid solid;

        Matrix4x4 R = new Matrix4x4();
        R.translation(0.55, 0.55, 0.55);

        Sphere s = new Sphere(r);
        solid = s.exportToPolyhedralBoundedSolid();
        solid.applyTransformation(R);
        solid.validateModel();
        return solid;
    }

    /**    
    */
    public static PolyhedralBoundedSolid createCone(double r1, double r2, double h)
    {
        PolyhedralBoundedSolid solid;

        Matrix4x4 R = new Matrix4x4();
        R.translation(0.55, 0.55, 0.05);

        Cone c = new Cone(r1, r2, h);
        solid = c.exportToPolyhedralBoundedSolid();
        solid.applyTransformation(R);
        solid.validateModel();
        return solid;
    }

    /**    
    */
    public static PolyhedralBoundedSolid createArrow(double p1, double p2, double p3, double p4)
    {
        PolyhedralBoundedSolid solid;

        Matrix4x4 R = new Matrix4x4();
        R.translation(0.55, 0.55, 0.05);

        Arrow a = new Arrow(p1, p2, p3, p4);
        solid = a.exportToPolyhedralBoundedSolid();
        solid.applyTransformation(R);
        solid.validateModel();
        return solid;
    }

    /**
    PRE:
    Works on the output of `createBox` method, for a box from <0.1, 0.1, 0.1>
    to <1, 1, 1>
    */
    public static void extrudeBox(PolyhedralBoundedSolid solid)
    {
        //- Cube modification to holed box --------------------------------
        solid.smev(6, 5, 9, new Vector3D(0.3, 0.3, 1));
        solid.kemr(6, 6, 5, 9, 9, 5);
        solid.smev(6, 9, 10, new Vector3D(0.8, 0.3, 1));
        solid.smev(6, 10, 11, new Vector3D(0.8, 0.8, 1));
        solid.smev(6, 11, 12, new Vector3D(0.3, 0.8, 1));
        solid.mef(6, 6, 9, 10, 12, 11, 7);

        //- Box extrusion -------------------------------------------------
        solid.smev(7, 9, 13, new Vector3D(0.3, 0.3, 0.1));
        solid.smev(7, 10, 14, new Vector3D(0.8, 0.3, 0.1));
        solid.mef(7, 7, 13, 9, 14, 10, 8);
        solid.smev(7, 11, 15, new Vector3D(0.8, 0.8, 0.1));
        solid.mef(7, 7, 14, 10, 15, 11, 9);
        solid.smev(7, 12, 16, new Vector3D(0.3, 0.8, 0.1));
        solid.mef(7, 7, 15, 11, 16, 12, 10);
        solid.mef(7, 7, 13, 14, 16, 12, 11);
        solid.validateModel();
    }

    /**
    This method implements the example presented in section [MANT1988].9.3,
    and figure [MANT1988].9.11.
    */
    public static PolyhedralBoundedSolid createHoledBox()
    {
        PolyhedralBoundedSolid solid;

        solid = PolyhedralBoundedSolidModelingTools.createBox(
                new Vector3D(0.9, 0.9, 0.9));
        PolyhedralBoundedSolidModelingTools.extrudeBox(solid);
        solid.kfmrh(2, 11);
        //R.translation(-0.55, -0.55, -0.55);
        //solid.applyTransformation(R);
        solid.validateModel();

        return solid;
    }

    public static PolyhedralBoundedSolid createLaminaWithTwoShells()
    {
        //- Basic lamina --------------------------------------------------
        Matrix4x4 R = new Matrix4x4();
        PolyhedralBoundedSolid solid;

        R.translation(0.55, 0.55, 0.55);
        solid = new PolyhedralBoundedSolid();
        solid.mvfs(new Vector3D(-0.5, -0.5, 0), 1, 1);
        solid.smev(1, 1, 4, new Vector3D(-0.5, 0.0, 0));
        solid.smev(1, 4, 3, new Vector3D(0.5, 0.0, 0));
        solid.smev(1, 3, 2, new Vector3D(0.5, -0.5, 0));
        solid.mef(1, 1, 1, 4, 2, 3, 2);

        //- Hole ----------------------------------------------------------
        solid.smev(1, 1, 5, new Vector3D(-0.3, 0.1, 0));
        solid.kemr(1, 1, 1, 5, 5, 1);
        solid.smev(1, 5, 6, new Vector3D(0.0, 0.4, 0));
        solid.smev(1, 6, 7, new Vector3D(0.3, 0.1, 0));
        solid.mef(1, /* face1 */
                  1, /* face2 */
                  5, /* v1 */
                  6, /* v2 */
                  7, /* v3 */
                  6, /* v4 */
                  3  /* newfaceid */);

        solid.kfmrh(2, 3);

        //-----------------------------------------------------------------
        solid.applyTransformation(R);
        solid.validateModel();
        return solid;

    }

    public static PolyhedralBoundedSolid createLaminaWithHole()
    {
        //- Basic lamina --------------------------------------------------
        Matrix4x4 R = new Matrix4x4();
        PolyhedralBoundedSolid solid;

        R.translation(0.55, 0.55, 0.55);
        solid = new PolyhedralBoundedSolid();
        solid.mvfs(new Vector3D(-0.5, -0.5, 0), 1, 1);
        solid.smev(1, 1, 4, new Vector3D(-0.5, 0.5, 0));
        solid.smev(1, 4, 3, new Vector3D(0.5, 0.5, 0));
        solid.smev(1, 3, 2, new Vector3D(0.5, -0.5, 0));
        solid.mef(1, 1, 1, 4, 2, 3, 2);

        //- Hole ----------------------------------------------------------
        solid.smev(1, 1, 5, new Vector3D(-0.3, -0.3, 0));
        solid.kemr(1, 1, 1, 5, 5, 1);
        solid.smev(1, 5, 6, new Vector3D(0.0, 0.3, 0));
        solid.smev(1, 6, 7, new Vector3D(0.3, -0.3, 0));
        solid.mef(1, /* face1 */
                  1, /* face2 */
                  5, /* v1 */
                  6, /* v2 */
                  7, /* v3 */
                  6, /* v4 */
                  3  /* newfaceid */);

        solid.kfmrh(2, 3);

        //-----------------------------------------------------------------
        solid.applyTransformation(R);
        solid.validateModel();
        return solid;
    }

    public static PolyhedralBoundedSolid createFontBlock(String fontFile, String msg)
    {
        //-----------------------------------------------------------------
        AwtFontReader fontReader = new AwtFontReader();
        ParametricCurve curve = null;

        for ( int i = 0; i < msg.length(); i++ ) {
            if ( i != 5 ) continue;
            String character = msg.substring(i, i+1);
            curve = fontReader.extractGlyph(fontFile, character);
            curve.setApproximationSteps(2);
            break;
        }

        //-----------------------------------------------------------------
        PolyhedralBoundedSolid solid;

        solid = GeometricModeler.createBrepFromParametricCurve(curve);

        return solid;
    }

    /**
    After algorithm decribed a section [MANT1988].12.4, and program
    [MANT1988].12.7.
    */
    private static void
    glue(PolyhedralBoundedSolid solid1, PolyhedralBoundedSolid solid2,
         int faceid1, int faceid2)
    {
        solid1.merge(solid2);
        solid1.kfmrh(faceid1, faceid2);
        solid1.loopGlue(faceid1);
    }

    /**    
    After example on [MANT1988].12.4.
    */
    public static PolyhedralBoundedSolid createGluedCilinders()
    {
        //- Create cilynder 1 ---------------------------------------------
        PolyhedralBoundedSolid solid1;

        Matrix4x4 T = new Matrix4x4();
        T.translation(0, 0, 0.4);

        solid1 = GeometricModeler.createCircularLamina(
            0.0, 0.0, 0.5, 0.0, 6
        );
        GeometricModeler.translationalSweepExtrudeFacePlanar(
            solid1, solid1.findFace(1), T);
        solid1.validateModel();

        //-----------------------------------------------------------------
        double ang = (2*Math.PI) / 6;
        Matrix4x4 R = new Matrix4x4();
        Vector3D a = new Vector3D(0.5, 0, 0);
        Vector3D b = new Vector3D(0.5*Math.cos(ang), 0.5*Math.sin(ang), 0);
        Vector3D c = a.add(b);
        c = c.multiply(0.5);
        R.translation(-c.x, c.y, c.z);
        solid1.applyTransformation(R);

        //- Create cilynder 2 ---------------------------------------------
        PolyhedralBoundedSolid solid2;

        solid2 = GeometricModeler.createCircularLamina(
            0.0, 0.0, 0.5, 0.0, 6
        );
        GeometricModeler.translationalSweepExtrudeFacePlanar(
            solid2, solid2.findFace(1), T);
        solid2.validateModel();

        //-----------------------------------------------------------------
        R.translation(c.x, -c.y, c.z);
        solid2.applyTransformation(R);

        //-----------------------------------------------------------------
        glue(solid1, solid2, 8, 13);

        //-----------------------------------------------------------------
        solid1.validateModel();
        solid1.maximizeFaces();
        solid1.validateModel();

        return solid1;
    }
    
    public static PolyhedralBoundedSolid eulerOperatorsTest()
    {
        PolyhedralBoundedSolid solid;
        solid = new PolyhedralBoundedSolid();
        solid.mvfs(new Vector3D(0.1, 0.1, 0), 1, 1);
        solid.smev(1, 1, 2, new Vector3D(1.0, 0.2, 0));

        _PolyhedralBoundedSolidFace face;
        face = solid.findFace(1);
        //solid.lkev(face.findHalfEdge(2), face.findHalfEdge(1));

        solid.smev(1, 2, 3, new Vector3D(0.5, 1, 0));

        //-----------------------------------------------------------------
        _PolyhedralBoundedSolidHalfEdge h1, h2;

        h1 = face.findHalfEdge(3);
        h2 = face.findHalfEdge(1);

        solid.lmef(h1, h2, 2);

        //-----------------------------------------------------------------

        solid.validateModel();
        System.out.println(solid);
        return solid;
    }

    /**
    Current method implements a simple and restricted rotational sweep (lathe)
    algorithm for wires (solids with one face, and one open loop) in the z=0
    plane, to be rotated about the x axis, as described in section
    [MANT1988].12.3.2, and presented in program [MANT1988].12.5.
    This version of the rotational sweep has some limitations and
    characteristics:
      - It is the simpler form of rotational sweep, and serves as the base to
        develop complex/generalized versions of the algorithm.
      - The rotation axis is fixed to be the x-axis
      - The profile path must be open (a "wire" solid with just one face with
        one loop, which is open, with a single, connected and nonforking string
        of edges)
      - All edges must lie on the half plane [y>0,z=0], and must not touch the
        x axis.
    */
    public static void rotationalSweepVersion1(PolyhedralBoundedSolid solid, int nfaces)
    {
        _PolyhedralBoundedSolidHalfEdge first, cfirst, last, scan = null;
        _PolyhedralBoundedSolidFace tailf;
        Vector3D v;
        Matrix4x4 M;

        first = solid.polygonsList.get(0).boundariesList.get(0).boundaryStartHalfEdge;
        while ( first.parentEdge != first.next().parentEdge ) {
            first = first.next();
        }
        last = first.next();
        while ( last.parentEdge != last.next().parentEdge ) {
            last = last.next();
        }
        cfirst = first;
        M = new Matrix4x4();
        M.axisRotation( (2*Math.PI) / ((double)nfaces), 1, 0, 0);

        int i;
        for ( i = 0; i < nfaces-1; i++ ) {
            v = M.multiply(cfirst.next().startingVertex.position);
            solid.lmev(cfirst.next(), cfirst.next(), solid.getMaxVertexId()+1, v);
            scan = cfirst.next();
            while ( scan != last.next() ) {
                v = M.multiply(scan.previous().startingVertex.position);
                solid.lmev(scan.previous(), scan.previous(), solid.getMaxVertexId()+1, v);
                solid.lmef(scan.previous().previous(), scan.next(), solid.getMaxFaceId()+1);
                scan = (scan.next().next()).mirrorHalfEdge();
            }
            last = scan;
            cfirst = (cfirst.next().next()).mirrorHalfEdge();
        }
        tailf = solid.lmef(cfirst.next(), first.mirrorHalfEdge(), solid.getMaxFaceId()+1);
        while ( cfirst != scan ) {
            solid.lmef(cfirst, cfirst.next().next().next(), solid.getMaxFaceId()+1);
            cfirst = (cfirst.previous()).mirrorHalfEdge().previous();
        }
    }

    /**
    Current method implements a variant of simple and restricted rotational
    sweep (lathe) algorithm for wires and laminas in the z=0 plane, to be
    rotated about the x axis, as described in section [MANT1988].12.5, and
    presented in programs [MANT1988].12.5 and [MANT1988].12.11.
    */
    public static void rotationalSweepVersion2(PolyhedralBoundedSolid solid, int nfaces)
    {
        //-----------------------------------------------------------------
        _PolyhedralBoundedSolidHalfEdge first, cfirst, last, scan = null;
        _PolyhedralBoundedSolidHalfEdge h;
        _PolyhedralBoundedSolidFace tailf = null;
        _PolyhedralBoundedSolidFace headf = null;
        boolean closedFigure = false;
        Vector3D v;
        Matrix4x4 M;

        //-----------------------------------------------------------------
        if ( solid.polygonsList.size() > 1 ) {
            // Assume it's a lamina
            closedFigure = true;
            h = solid.polygonsList.get(0).boundariesList.get(0).boundaryStartHalfEdge;
            solid.lmev(h, (h.mirrorHalfEdge()).next(),
                       solid.getMaxVertexId()+1, h.startingVertex.position);

            solid.lkef(h.previous(), (h.previous()).mirrorHalfEdge());
            headf = solid.polygonsList.get(0);

        }

        //-----------------------------------------------------------------
        first = solid.polygonsList.get(0).boundariesList.get(0).boundaryStartHalfEdge;
        while ( first.parentEdge != first.next().parentEdge ) {
            first = first.next();
        }
        last = first.next();
        while ( last.parentEdge != last.next().parentEdge ) {
            last = last.next();
        }
        cfirst = first;
        M = new Matrix4x4();
        M.axisRotation( (2*Math.PI) / ((double)nfaces), 1, 0, 0);

        int i;
        for ( i = 0; i < nfaces-1; i++ ) {
            v = M.multiply(cfirst.next().startingVertex.position);
            solid.lmev(cfirst.next(), cfirst.next(), solid.getMaxVertexId()+1, v);
            scan = cfirst.next();
            while ( scan != last.next() ) {
                v = M.multiply(scan.previous().startingVertex.position);
                solid.lmev(scan.previous(), scan.previous(), solid.getMaxVertexId()+1, v);
                solid.lmef(scan.previous().previous(), scan.next(), solid.getMaxFaceId()+1);
                scan = (scan.next().next()).mirrorHalfEdge();
            }
            last = scan;
            cfirst = (cfirst.next().next()).mirrorHalfEdge();
        }
        tailf = solid.lmef(cfirst.next(), first.mirrorHalfEdge(), solid.getMaxFaceId()+1);
        while ( cfirst != scan ) {
            solid.lmef(cfirst, cfirst.next().next().next(), solid.getMaxFaceId()+1);
            cfirst = (cfirst.previous()).mirrorHalfEdge().previous();
        }

        //-----------------------------------------------------------------
        if ( closedFigure ) {
            solid.lkfmrh(headf, tailf);
            solid.loopGlue(headf.id);
        }

        //-----------------------------------------------------------------
    }

    public static PolyhedralBoundedSolid rotationalSweepTest()
    {
        PolyhedralBoundedSolid solid;
        
        //-----------------------------------------------------------------
/*
        solid = new PolyhedralBoundedSolid();
        solid.mvfs(new Vector3D(0.75, 0.25, 0), 1, 1);
        GeometricModeler.addArc(solid, 1, 1, 0.5, 0.25, 0.25, 0.0, 0.0, 90.0, 10);
        rotationalSweepVersion1(solid, 20);
*/
      
        solid = GeometricModeler.createCircularLamina(0.5, 0.5, 0.2, 0.0, 8);
        rotationalSweepVersion2(solid, 16);

        //-----------------------------------------------------------------
        solid.validateModel();

        return solid;
    }

    public static PolyhedralBoundedSolid splitTest(int part)
    {
        //- Basic lamina --------------------------------------------------
        //PolyhedralBoundedSolid solid = createHoledBox();
        //PolyhedralBoundedSolid solid = createBox(new Vector3D(0.9, 0.9, 0.9));

        Matrix4x4 R = new Matrix4x4();
        PolyhedralBoundedSolid solid;
        R.translation(0.55, 0.55, 0.55);
        solid = new PolyhedralBoundedSolid();
        solid.mvfs(new Vector3D(0.00+0.05, 0.00+0.05, 0), 1, 1);
        solid.smev(1, 1, 2, new Vector3D(0.94+0.05, 0.00+0.05, 0));
        solid.smev(1, 2, 3, new Vector3D(0.94+0.05, 0.46+0.05, 0));
        solid.smev(1, 3, 4, new Vector3D(0.60+0.05, 0.30+0.05, 0));
        solid.smev(1, 4, 5, new Vector3D(0.37+0.05, 0.30+0.05, 0));
        solid.smev(1, 5, 6, new Vector3D(0.18+0.05, 0.46+0.05, 0));
        solid.smev(1, 6, 7, new Vector3D(0.00+0.05, 0.30+0.05, 0));
        solid.mef(1, 1, 7, 6, 1, 2, 2);
        Matrix4x4 T = new Matrix4x4();
        T.translation(0, 0, 0.4);
        GeometricModeler.translationalSweepExtrudeFacePlanar(
            solid, solid.findFace(1), T);

/*
        Matrix4x4 R = new Matrix4x4();
        PolyhedralBoundedSolid solid;
        R.translation(0.55, 0.55, 0.55);
        solid = new PolyhedralBoundedSolid();
        solid.mvfs(new Vector3D(0.00+0.05, 0.00+0.05, 0), 1, 1);
        solid.smev(1, 1, 2, new Vector3D(0.94+0.05, 0.00+0.05, 0));
        solid.smev(1, 2, 3, new Vector3D(0.94+0.05, 0.46+0.05, 0));
        solid.smev(1, 3, 4, new Vector3D(0.00+0.05, 0.30+0.05, 0));
        solid.mef(1, 1, 4, 3, 1, 2, 2);
        Matrix4x4 T = new Matrix4x4();
        T.translation(0, 0, 0.4);
        GeometricModeler.translationalSweepExtrudeFacePlanar(
            solid, solid.findFace(1), T);
*/

        //-----------------------------------------------------------------
        InfinitePlane sp;
        ArrayList <PolyhedralBoundedSolid> solidsAbove;
        ArrayList <PolyhedralBoundedSolid> solidsBelow;

        solidsAbove = new ArrayList <PolyhedralBoundedSolid>();
        solidsBelow = new ArrayList <PolyhedralBoundedSolid>();

        sp = new InfinitePlane(new Vector3D(0, 1, 0) /*n*/,
                               new Vector3D(0, 0.30+0.05, 0) /*p*/);

//        sp = new InfinitePlane(new Vector3D(0, 0, 1) /*n*/,
//                               new Vector3D(0, 0, 0.5) /*p*/);

        //-----------------------------------------------------------------
        solid.validateModel();

        if ( part == 1 ) {
            return solid;
        }

        GeometricModeler.split(solid, sp, solidsAbove, solidsBelow);

        //-----------------------------------------------------------------
        if ( part == 3 ) {
            solid = solidsBelow.get(0);
        }
        else {
            solid = solidsAbove.get(0);
        }

        solid.maximizeFaces();
        solid.validateModel();

        return solid;
    }

    public static PolyhedralBoundedSolid csgTest(int part)
    {
        PolyhedralBoundedSolid a;
        PolyhedralBoundedSolid b = createBox(new Vector3D(0.9, 0.9, 0.9));
        PolyhedralBoundedSolid res;

        //-----------------------------------------------------------------
        a = new PolyhedralBoundedSolid();
        a.mvfs(new Vector3D(0.00+0.05, 0.42+0.05, 0.00+0.05), 1, 1);
        a.smev(1, 1, 2, new Vector3D(0.92+0.05, 0.42+0.05, 0.00+0.05));
        a.smev(1, 2, 3, new Vector3D(0.92+0.05, 0.42+0.05, 0.72+0.05));
        a.smev(1, 3, 4, new Vector3D(0.70+0.05, 0.42+0.05, 0.72+0.05));
        a.smev(1, 4, 5, new Vector3D(0.70+0.05, 0.42+0.05, 0.18+0.05));
        a.smev(1, 5, 6, new Vector3D(0.00+0.05, 0.42+0.05, 0.18+0.05));
        a.mef(1, 1, 6, 5, 1, 2, 2);
        a.validateModel();

        Matrix4x4 T = new Matrix4x4();
        T.translation(0, -0.42, 0);
        GeometricModeler.translationalSweepExtrudeFacePlanar(
            a, a.findFace(1), T);

        //-----------------------------------------------------------------
        Matrix4x4 R = new Matrix4x4();
        R.translation(0.05 +0.58/2.0+(0.92-0.58),
                      0.05 + 0.42/2.0 - 0.42/2.0,
                      0.05 + 0.18/2.0 + 0.18);

        Box box = new Box(new Vector3D(0.58, 0.42, 0.18));
        b = box.exportToPolyhedralBoundedSolid();
        b.applyTransformation(R);
        b.validateModel();

        //-----------------------------------------------------------------
        res = GeometricModeler.setOp(a, b, GeometricModeler.UNION);

        //-----------------------------------------------------------------
        res.validateModel();

        if ( part == 1 ) {
            return a;
        }
        return b;
    }

}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
