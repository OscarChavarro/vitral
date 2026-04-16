//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

// Java classes
import java.util.ArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

// VitralSDK classes
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
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
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.render.awt.AwtFontReader;
import vsdk.toolkit.render.jogl.JoglPolyhedralBoundedSolidDebugger;
import vsdk.toolkit.processing.GeometricModeler;
import vsdk.toolkit.processing.PolyhedralBoundedSolidSetOperator;
import vsdk.toolkit.processing.SimpleTestGeometryLibrary;

public class BoundedSolidTestSelector
{
    public static PolyhedralBoundedSolid buildSolid(DebuggerModel model)
    {
        PolyhedralBoundedSolid mySolid;
        Matrix4x4 T, R, S, M;

        switch ( model.solidType % 24 ) {
          case 0:
            mySolid = new PolyhedralBoundedSolid();
            mySolid.mvfs(new Vector3D(0.1, 0.1, 0.1), 1, 1);
            mySolid.smev(1, 1, 4, new Vector3D(0.1, 1, 0.1));
            mySolid.smev(1, 4, 3, new Vector3D(1, 1, 0.1));
            break;
          case 1:
            mySolid = createBox(new Vector3D(0.9, 0.9, 0.9));
            break;
          case 3:
            mySolid = new PolyhedralBoundedSolid();
            mySolid.mvfs(new Vector3D(1, 0.5, 0.1), 1, 1);
            GeometricModeler.addArc(
                mySolid, 1, 1, 0.5, 0.5, 0.5, 0.1, 0, 270, 9);
            break;
          case 4:
            mySolid = GeometricModeler.createCircularLamina(
                0.5, 0.5, 0.5, 0.1, 12
            );
            break;
          case 5:
            mySolid = new PolyhedralBoundedSolid();
            mySolid.mvfs(new Vector3D(1, 0.5, 0.1), 1, 1);
            GeometricModeler.addArc(
                mySolid, 1, 1, 0.5, 0.5, 0.5, 0.1, 0, 270, 18);

            T = new Matrix4x4();
            T.translation(0.0, 0.0, 0.5);
            R = new Matrix4x4();
            R.axisRotation(Math.toRadians(5), 0, 1, 0);
            S = new Matrix4x4();
            S.scale(0.5, 0.5, 0.5);
            M = T.multiply(R.multiply(S));

            GeometricModeler.translationalSweepExtrudeFacePlanar(
                mySolid, mySolid.findFace(1), M);

            break;
          case 6:
            mySolid = GeometricModeler.createCircularLamina(
                0.5, 0.5, 0.5, 0.1, 24
            );

            T = new Matrix4x4();
            T.translation(0.0, 0.0, 0.5);
            R = new Matrix4x4();
            R.axisRotation(Math.toRadians(5), 0, 1, 0);
            S = new Matrix4x4();
            S.scale(0.5, 0.5, 0.5);
            M = T.multiply(R.multiply(S));
            GeometricModeler.translationalSweepExtrudeFacePlanar(
                mySolid, mySolid.findFace(1), M);

/*
            T = new Matrix4x4();
            T.translation(0.1, 0.1, 1.0);
            R = new Matrix4x4();
            //R.axisRotation(Math.toRadians(15), 0, 1, 0);
            S = new Matrix4x4();
            S.scale(0.2, 0.2, 0.2);
            M = T.multiply(R.multiply(S));
            GeometricModeler.translationalSweepExtrudeFace(
                solid, solid.findFace(1), M);
*/
            break;

          case 7:
            mySolid = createSphere(0.5);
            break;
          case 8:
            mySolid = createCone(0.5, 0.0, 1.0);
            break;
          case 9:
            mySolid = createArrow(0.7, 0.3, 0.05, 0.1);
            break;
          case 10:
            mySolid = createLaminaWithTwoShells();
            break;
          case 11:
            mySolid = createLaminaWithHole();
            break;
          case 12:
            mySolid = createFontBlock("../../../../samples/fonts/microsoftArial.ttf", "\u7c8b\u00e1\u00d1\u3055\u3042\u307d");

            T = new Matrix4x4();
            T.translation(0.0, 0.0, 0.1);

            GeometricModeler.translationalSweepExtrudeFacePlanar(
                mySolid, mySolid.findFace(1), T);

            break;
          case 13:
            mySolid = createGluedCilinders();
            break;
          case 14:
            mySolid = eulerOperatorsTest();
            break;
          case 15:
            mySolid = rotationalSweepTest();
            break;
          case 16:
            mySolid = splitTest(1);
            break;
          case 17:
            mySolid = splitTest(2);
            break;
          case 18:
            mySolid = splitTest(3);
            break;
          case 19:
            mySolid = csgTest(1, model.csgOperation, model.csgSample, model.debugCsg);
            model.debugCsg = false;
            break;
          case 20:
            mySolid = csgTest(2, model.csgOperation, model.csgSample, model.debugCsg);
            model.debugCsg = false;
            break;
          case 21:
            mySolid = csgTest(3, model.csgOperation, model.csgSample, model.debugCsg);
            model.debugCsg = false;
            break;
          case 22:
            mySolid = featuredObject();
            break;
          case 23:
            File fd = new File("/tmp/solid.bin");
            if ( fd.exists() ) {
                mySolid = importFromFile("/tmp/solid.bin");
            }
            else {
                mySolid = featuredObject();
            }
            break;
          case 2: default:
            mySolid = createHoledBox();
            break;
        }

        return mySolid;
    }

    private static PolyhedralBoundedSolid importFromFile(String filename)
    {
        PolyhedralBoundedSolid mysolid = null;

        try {
            File fd = new File(filename);
            FileInputStream fis = new FileInputStream(fd);
            ObjectInputStream ois = new ObjectInputStream(fis);
            mysolid = (PolyhedralBoundedSolid)ois.readObject();

            fis.close();
        }
        catch ( IOException e ) {
            VSDK.reportMessageWithException(
                BoundedSolidTestSelector.class,
                VSDK.FATAL_ERROR,
                "importFromFile",
                "Error reading solid from file " + filename, e);
        }
        catch ( ClassNotFoundException e ) {
            VSDK.reportMessageWithException(
                BoundedSolidTestSelector.class,
                VSDK.FATAL_ERROR,
                "importFromFile",
                "Error reading solid from file " + filename, e);
        }

        return mysolid;
    }

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

        solid = BoundedSolidTestSelector.createBox(
                new Vector3D(0.9, 0.9, 0.9));
        BoundedSolidTestSelector.extrudeBox(solid);
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
    After algorithm decribed on section [MANT1988].12.4, and program
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
    This method builds a test solid for evaluating the gluing algorithm in
    a controlled way, as proposed on the example from section [MANT1988].12.4.
    It is similar to the solid shown on figure [MANT1988].12.2.
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
        _PolyhedralBoundedSolidHalfEdge h1, h2;
        _PolyhedralBoundedSolidFace face;

/*
        solid = new PolyhedralBoundedSolid();
        solid.mvfs(new Vector3D(0.1, 0.1, 0), 1, 1);
        solid.smev(1, 1, 2, new Vector3D(1.0, 0.2, 0));

        face = solid.findFace(1);
        //solid.lkev(face.findHalfEdge(2), face.findHalfEdge(1));

        solid.smev(1, 2, 3, new Vector3D(0.5, 1, 0));

        //-----------------------------------------------------------------

        h1 = face.findHalfEdge(3);
        h2 = face.findHalfEdge(1);

        solid.lmef(h1, h2, 2);

        //-----------------------------------------------------------------

        h1 = face.findHalfEdge(1);
        solid.lmev(h1, h1, solid.getMaxVertexId()+1, new Vector3D(0.1, 0.1, 0.4));
*/

/*
        solid = createBox(new Vector3D(1, 1, 1));

        //-----------------------------------------------------------------
        face = solid.findFace(3);
        h1 = face.findHalfEdge(1);
        face = solid.findFace(2);
        h2 = face.findHalfEdge(1);

        solid.lmev(h1, h2, solid.getMaxVertexId()+1, new Vector3D(0.55, 0.05, 0.05));

        solid.lkev(h1, h1.mirrorHalfEdge());
*/

        //-----------------------------------------------------------------
        solid = new PolyhedralBoundedSolid();
        solid.mvfs(new Vector3D(0.0, 0.0, 0.0), 1, 1);
        solid.smev(1, 1, 2, new Vector3D(1.0, 0.0, 0.0));
        solid.smev(1, 2, 3, new Vector3D(1.0, 1.0, 0.0));

        //-----------------------------------------------------------------
        //solid.validateModel();
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
    private static void rotationalSweepVersion1(PolyhedralBoundedSolid solid, int nfaces)
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
    private static void rotationalSweepVersion2(PolyhedralBoundedSolid solid, int nfaces)
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

    /**
    This method builds a test solid for evaluating the second version of the
    rotational sweep algorithm in a controlled way, as proposed on the example
    from section [MANT1988].12.5.
    The created solid is is similar to the solid shown on figure
    [MANT1988].12.5. (in particular when seting 4 sides);
    */
    private static PolyhedralBoundedSolid createTestTorus(int nsides, int nrad)
    {
        PolyhedralBoundedSolid solid;
        Vector3D center = new Vector3D(0.5, 0.5, 0);

        solid = GeometricModeler.createCircularLamina(center.x, center.y,
                                                      0.2, 0.0, nsides);

        // For seting 4 sided case to be equal to figure [MANT1988].12.5.
        // an aditional rotation must be applied to the lamina prior to the
        // rotational sweep.
        Matrix4x4 T1 = new Matrix4x4();
        Matrix4x4 T2 = new Matrix4x4();
        Matrix4x4 R = new Matrix4x4();
        Matrix4x4 M;
        T1.translation(center.multiply(-1));
        T2.translation(center);
        R.axisRotation(Math.PI/((double)nsides), 0, 0, 1);
        M = T2.multiply(R.multiply(T1));
        solid.applyTransformation(M);

        rotationalSweepVersion2(solid, nrad);
        return solid;
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
        solid = createTestTorus(4, 16);
        //-----------------------------------------------------------------
        solid.validateModel();

        return solid;
    }

    public static PolyhedralBoundedSolid splitTest(int part)
    {
        //- Basic lamina --------------------------------------------------
        //PolyhedralBoundedSolid solid = createHoledBox();
        //PolyhedralBoundedSolid solid = createBox(new Vector3D(0.9, 0.9, 0.9));

        PolyhedralBoundedSolid solid = SimpleTestGeometryLibrary.createTestObjectMANT1986_1();

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

        sp = new InfinitePlane(new Vector3D(0, 0, 1) /*n*/,
                               new Vector3D(0, 0, 0.30) /*p*/);

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

    /**
    This method builds a test sample pair of solids for evaluating
    the set operations algorithm in a controlled way.
    This set correspond to a simple cases for CSG operations test: two
    blocks without intersecting vertex pairs (only edge/face
    intersections are present). The resulting gluing face can be a variation
    of the method `createTestObjectsPairMANT1986_2`, if blocks are translated
    so their parallel faces don't touch; or can be one simple test case for
    the complex sector intersection.
    */
    private static PolyhedralBoundedSolid[] buildCsgTest2()
    {
        PolyhedralBoundedSolid operands[];
        PolyhedralBoundedSolid a, b;

        operands = new PolyhedralBoundedSolid[2];

        //-----------------------------------------------------------------
        Matrix4x4 R = new Matrix4x4();
        R.translation(0.5, 0.5, 0.15);

        Box box = new Box(new Vector3D(1, 0.5, 0.3));
        a = box.exportToPolyhedralBoundedSolid();
        a.applyTransformation(R);
        a.validateModel();

        //-----------------------------------------------------------------
        R = new Matrix4x4();
        R.translation(0.5, 0.5, 0.15+0.3);

        box = new Box(new Vector3D(0.5, 1, 0.3));
        b = box.exportToPolyhedralBoundedSolid();
        b.applyTransformation(R);
        b.validateModel();

        //-----------------------------------------------------------------
        operands[0] = a;
        operands[1] = b;

        return operands;
    }

    /**
    Makes a hollowed brick from two L-shaped boxes. Note that on UNION
    operation this object leads to an interesting topological problem for
    PolyhedralBoundedSolid.maximizeFaces operation.
    */
    private static PolyhedralBoundedSolid[] buildCsgTest4()
    {
        PolyhedralBoundedSolid operands[];
        operands = new PolyhedralBoundedSolid[2];

        PolyhedralBoundedSolid a;
        PolyhedralBoundedSolid b;
        PolyhedralBoundedSolid c;
        PolyhedralBoundedSolid d;
        PolyhedralBoundedSolid x;
        PolyhedralBoundedSolid y;
        Matrix4x4 T;
        Box box;

        //-----------------------------------------------------------------
        T = new Matrix4x4();
        T.translation(0.5, 0.1, 0.1);
        box = new Box(new Vector3D(1, 0.2, 0.2));
        a = box.exportToPolyhedralBoundedSolid();
        a.applyTransformation(T);
        a.validateModel();

        //-----------------------------------------------------------------
        T = new Matrix4x4();
        T.translation(0.5, 0.9, 0.1);
        box = new Box(new Vector3D(1, 0.2, 0.2));
        b = box.exportToPolyhedralBoundedSolid();
        b.applyTransformation(T);
        b.validateModel();

        //-----------------------------------------------------------------
        T = new Matrix4x4();
        T.translation(0.1, 0.5, 0.1);
        box = new Box(new Vector3D(0.2, 1, 0.2));
        c = box.exportToPolyhedralBoundedSolid();
        c.applyTransformation(T);
        c.validateModel();

        //-----------------------------------------------------------------
        T = new Matrix4x4();
        T.translation(0.9, 0.5, 0.1);
        box = new Box(new Vector3D(0.2, 1, 0.2));
        d = box.exportToPolyhedralBoundedSolid();
        d.applyTransformation(T);
        d.validateModel();

        //-----------------------------------------------------------------
        x = GeometricModeler.setOp(b, c, GeometricModeler.UNION);
        y = GeometricModeler.setOp(a, d, GeometricModeler.UNION);

        operands[0] = x;
        operands[1] = y;
        return operands;
    }

    private static PolyhedralBoundedSolid[] buildCsgTest5()
    {
        PolyhedralBoundedSolid operands[];
        operands = new PolyhedralBoundedSolid[2];

        PolyhedralBoundedSolid a;
        PolyhedralBoundedSolid b;
        PolyhedralBoundedSolid c;
        PolyhedralBoundedSolid d;
        PolyhedralBoundedSolid e;
        PolyhedralBoundedSolid f;
        PolyhedralBoundedSolid g;
        PolyhedralBoundedSolid h;
        PolyhedralBoundedSolid ac;
        PolyhedralBoundedSolid bd;
        PolyhedralBoundedSolid eg;
        PolyhedralBoundedSolid fh;
        PolyhedralBoundedSolid abcd;
        PolyhedralBoundedSolid efgh;
        PolyhedralBoundedSolid total;
        Matrix4x4 T;
        Box box;

        //-----------------------------------------------------------------
        T = new Matrix4x4();
        T.translation(0.5, 0.1, 0.1);
        box = new Box(new Vector3D(1, 0.2, 0.2));
        a = box.exportToPolyhedralBoundedSolid();
        a.applyTransformation(T);
        a.validateModel();

        //-----------------------------------------------------------------
        T = new Matrix4x4();
        T.translation(0.5, 0.9, 0.1);
        box = new Box(new Vector3D(1, 0.2, 0.2));
        b = box.exportToPolyhedralBoundedSolid();
        b.applyTransformation(T);
        b.validateModel();

        //-----------------------------------------------------------------
        T = new Matrix4x4();
        T.translation(0.1, 0.5, 0.1);
        box = new Box(new Vector3D(0.2, 1, 0.2));
        c = box.exportToPolyhedralBoundedSolid();
        c.applyTransformation(T);
        c.validateModel();

        //-----------------------------------------------------------------
        T = new Matrix4x4();
        T.translation(0.9, 0.5, 0.1);
        box = new Box(new Vector3D(0.2, 1, 0.2));
        d = box.exportToPolyhedralBoundedSolid();
        d.applyTransformation(T);
        d.validateModel();

        //-----------------------------------------------------------------
        T = new Matrix4x4();
        T.translation(0.1, 0.5, 0.1);
        box = new Box(new Vector3D(0.2, 1, 0.2));
        e = box.exportToPolyhedralBoundedSolid();
        e.applyTransformation(T);
        e.validateModel();

        //-----------------------------------------------------------------
        T = new Matrix4x4();
        T.translation(0.1, 0.5, 0.9);
        box = new Box(new Vector3D(0.2, 1, 0.2));
        f = box.exportToPolyhedralBoundedSolid();
        f.applyTransformation(T);
        f.validateModel();

        //-----------------------------------------------------------------
        T = new Matrix4x4();
        T.translation(0.1, 0.1, 0.5);
        box = new Box(new Vector3D(0.2, 0.2, 1));
        g = box.exportToPolyhedralBoundedSolid();
        g.applyTransformation(T);
        g.validateModel();

        //-----------------------------------------------------------------
        T = new Matrix4x4();
        T.translation(0.1, 0.9, 0.5);
        box = new Box(new Vector3D(0.2, 0.2, 1));
        h = box.exportToPolyhedralBoundedSolid();
        h.applyTransformation(T);
        h.validateModel();

        //-----------------------------------------------------------------
/*
        ac = GeometricModeler.setOp(a, c, GeometricModeler.UNION);
        bd = GeometricModeler.setOp(b, d, GeometricModeler.UNION);
        abcd = GeometricModeler.setOp(bd, ac, GeometricModeler.UNION);
        eg = GeometricModeler.setOp(e, g, GeometricModeler.UNION);
        fh = GeometricModeler.setOp(f, h, GeometricModeler.UNION);
        efgh = GeometricModeler.setOp(eg, fh, GeometricModeler.UNION);
        total = GeometricModeler.setOp(abcd, efgh, GeometricModeler.UNION);
*/
        ac = GeometricModeler.setOp(a, c, GeometricModeler.UNION);

        operands[0] = ac;
        operands[1] = g;
        return operands;
    }

    public static PolyhedralBoundedSolid csgTest(int part, int op, int set,
        boolean withDebug)
    {
        PolyhedralBoundedSolid res = null;
        PolyhedralBoundedSolid operands[] = null;

        System.out.printf("Creating C.S.G. test object with parts %d, " + 
            "operation %s, and sample pair %d\n", part, 
            op==0?"UNION":
                (op==1?"INTERSECTION":
                    (op==2?"DIFERENCE A-B":"DIFFERENCE B-A")), set);

        switch ( set ) {
            case 0:
              operands =
                  SimpleTestGeometryLibrary.createTestObjectPairMANT1986_2();
              break;
            case 1:
              operands = buildCsgTest2(); break;
            case 2: default:
              operands = 
                  SimpleTestGeometryLibrary.createTestObjectPairMANT1986_3();
              break;
            case 3:
              operands = buildCsgTest4();
              break;
            case 4:
              operands = buildCsgTest5();
              break;
            case 5:
              operands =
                SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_2(-1);
              break;
            case 6:
              operands =
                  SimpleTestGeometryLibrary.createTestObjectPairMANT1988_6_13();
              break;
            case 7:
              operands =
                  SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_1();
              break;
        }

        //-----------------------------------------------------------------
        if ( op == 0 ) {
            res = GeometricModeler.setOp(operands[0], operands[1],
                                         GeometricModeler.UNION, withDebug);
        }
        else if ( op == 1 ) {
            res = GeometricModeler.setOp(operands[0], operands[1],
                                         GeometricModeler.INTERSECTION, withDebug);
        }
        else if ( op == 2 ) {
            res = GeometricModeler.setOp(operands[0], operands[1],
                                         GeometricModeler.DIFFERENCE, withDebug);
        }
        else {
            res = GeometricModeler.setOp(operands[1], operands[0],
                                         GeometricModeler.DIFFERENCE, withDebug);
        }

        //-----------------------------------------------------------------
        //operands[0].validateModel();
        //operands[1].validateModel();
        //res.validateModel();

        if ( part == 2 ) {
            return operands[0];
        }
        if ( part == 3 ) {
            return operands[1];
        }
        return res;
    }

    public static PolyhedralBoundedSolid featuredObject()
    {
        return SimpleTestGeometryLibrary.createTestObjectAPPE1967_3();
/*
        PolyhedralBoundedSolid ops[];
        ops = SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_2();
        return GeometricModeler.setOp(ops[0], ops[1],
                                      GeometricModeler.DIFFERENCE);
*/
    }
}
