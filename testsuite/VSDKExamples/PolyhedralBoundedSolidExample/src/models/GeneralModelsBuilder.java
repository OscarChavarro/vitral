//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =
package models;

// Java classes
import java.util.ArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

// Vitral classes
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.environment.geometry.Arrow;
import vsdk.toolkit.environment.geometry.Box;
import vsdk.toolkit.environment.geometry.Cone;
import vsdk.toolkit.environment.geometry.InfinitePlane;
import vsdk.toolkit.environment.geometry.Sphere;
import vsdk.toolkit.environment.geometry.ParametricCurve;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.render.awt.AwtFontReader;
import vsdk.toolkit.processing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidModeler;
import vsdk.toolkit.processing.polyhedralBoundedSolidOperators.SimpleTestGeometryLibrary;

public class GeneralModelsBuilder
{
    public static PolyhedralBoundedSolid buildSolid(DebuggerModel model)
    {
        PolyhedralBoundedSolid mySolid;
        Matrix4x4 T, R, S, M;
        model.clampSubdivisions();

        switch ( model.getSolidModelName() ) {
          case MVFS_SMEV_SAMPLE:
            mySolid = new PolyhedralBoundedSolid();
            mySolid.mvfs(new Vector3D(0.1, 0.1, 0.1), 1, 1);
            mySolid.smev(1, 1, 4, new Vector3D(0.1, 1, 0.1));
            mySolid.smev(1, 4, 3, new Vector3D(1, 1, 0.1));
            break;
          case BOX:
            mySolid = createBox(new Vector3D(0.9, 0.9, 0.9));
            break;
          case ARC_SAMPLE:
            mySolid = new PolyhedralBoundedSolid();
            mySolid.mvfs(new Vector3D(1, 0.5, 0.1), 1, 1);
            PolyhedralBoundedSolidModeler.addArc(
                mySolid, 1, 1, 0.5, 0.5, 0.5, 0.1, 0, 270, 9);
            break;
          case CIRCULAR_LAMINA:
            mySolid = PolyhedralBoundedSolidModeler.createCircularLamina(
                0.5, 0.5, 0.5, 0.1, 12
            );
            break;
          case TRANSLATIONAL_SWEEP_EXTRUDE_FACE_PLANAR_ARC:
            mySolid = new PolyhedralBoundedSolid();
            mySolid.mvfs(new Vector3D(1, 0.5, 0.1), 1, 1);
            PolyhedralBoundedSolidModeler.addArc(
                mySolid, 1, 1, 0.5, 0.5, 0.5, 0.1, 0, 270, 18);

            T = new Matrix4x4();
            T.translation(0.0, 0.0, 0.5);
            R = new Matrix4x4();
            R.axisRotation(Math.toRadians(5), 0, 1, 0);
            S = new Matrix4x4();
            S.scale(0.5, 0.5, 0.5);
            M = T.multiply(R.multiply(S));

            PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
                mySolid, mySolid.findFace(1), M);

            break;
          case TRANSLATIONAL_SWEEP_EXTRUDE_FACE_PLANAR_CIRCULAR:
            mySolid = PolyhedralBoundedSolidModeler.createCircularLamina(
                0.5, 0.5, 0.5, 0.1, 24
            );

            T = new Matrix4x4();
            T.translation(0.0, 0.0, 0.5);
            R = new Matrix4x4();
            R.axisRotation(Math.toRadians(5), 0, 1, 0);
            S = new Matrix4x4();
            S.scale(0.5, 0.5, 0.5);
            M = T.multiply(R.multiply(S));
            PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
                mySolid, mySolid.findFace(1), M);

/*
            T = new Matrix4x4();
            T.translation(0.1, 0.1, 1.0);
            R = new Matrix4x4();
            //R.axisRotation(Math.toRadians(15), 0, 1, 0);
            S = new Matrix4x4();
            S.scale(0.2, 0.2, 0.2);
            M = T.multiply(R.multiply(S));
            PolyhedralBoundedSolidModeler.translationalSweepExtrudeFace(
                solid, solid.findFace(1), M);
*/
            break;

          case SPHERE:
            mySolid = createSphere(0.5, model.getSubdivisionCircumference(),
                model.getSubdivisionHeight());
            break;
          case CONE:
            mySolid = createCone(0.5, 0.0, 1.0,
                model.getSubdivisionCircumference(), model.getSubdivisionHeight());
            break;
          case CYLINDER:
            mySolid = createCylinder(0.5, 1.0,
                model.getSubdivisionCircumference(), model.getSubdivisionHeight());
            break;
          case CSG_MOON_BLOCK:
            mySolid = csgTest(1, CsgOperationNames.DIFFERENCE_A_MINUS_B,
                CsgSampleNames.MOON_BLOCK, model.isDebugCsg());
            model.setDebugCsg(false);
            break;
          case CSG_LAMP_SHELL:
            mySolid = createCsgLampShell(model.getSubdivisionCircumference(),
                model.getSubdivisionHeight());
            break;
          case ARROW:
            mySolid = createArrow(0.7, 0.3, 0.05, 0.1);
            break;
          case LAMINA_WITH_TWO_SHELLS:
            mySolid = createLaminaWithTwoShells();
            break;
          case LAMINA_WITH_HOLE:
            mySolid = createLaminaWithHole();
            break;
          case FONT_BLOCK:
            mySolid = createFontBlock("../../../../samples/fonts/microsoftArial.ttf", "\u7c8b\u00e1\u00d1\u3055\u3042\u307d");

            T = new Matrix4x4();
            T.translation(0.0, 0.0, 0.1);

            PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
                mySolid, mySolid.findFace(1), T);

            break;
          case GLUED_CYLINDERS:
            mySolid = createGluedCilinders();
            break;
          case EULER_OPERATORS_TEST:
            mySolid = eulerOperatorsTest();
            break;
          case ROTATIONAL_SWEEP:
            mySolid = rotationalSweepTest();
            break;
          case SPLIT_TEST_PART_1:
            mySolid = splitTest(1);
            break;
          case SPLIT_TEST_PART_2:
            mySolid = splitTest(2);
            break;
          case SPLIT_TEST_PART_3:
            mySolid = splitTest(3);
            break;
          case CSG_TEST_PART_1:
            mySolid = csgTest(1, model.getCsgOperation(), model.getCsgSample(), model.isDebugCsg());
            model.setDebugCsg(false);
            break;
          case CSG_TEST_PART_2:
            mySolid = csgTest(2, model.getCsgOperation(), model.getCsgSample(), model.isDebugCsg());
            model.setDebugCsg(false);
            break;
          case CSG_TEST_PART_3:
            mySolid = csgTest(3, model.getCsgOperation(), model.getCsgSample(), model.isDebugCsg());
            model.setDebugCsg(false);
            break;
          case CSG_MANT1988_15_2_HOLED_INTERSECTION:
            mySolid = csgMant1988_15_2Case(
                -1, PolyhedralBoundedSolidModeler.INTERSECTION, model.isDebugCsg());
            model.setDebugCsg(false);
            break;
          case CSG_MANT1988_15_2_LIMIT_INTERSECTION:
            mySolid = csgMant1988_15_2Case(
                0, PolyhedralBoundedSolidModeler.INTERSECTION, model.isDebugCsg());
            model.setDebugCsg(false);
            break;
          case CSG_MANT1988_15_2_LIMIT_DIFFERENCE:
            mySolid = csgMant1988_15_2Case(
                0, PolyhedralBoundedSolidModeler.SUBTRACT, model.isDebugCsg());
            model.setDebugCsg(false);
            break;
          case CSG_MANT1988_15_2_OPEN_DIFFERENCE:
            mySolid = csgMant1988_15_2Case(
                1, PolyhedralBoundedSolidModeler.SUBTRACT, model.isDebugCsg());
            model.setDebugCsg(false);
            break;
          case CSG_KURLANDER_BOWL:
            mySolid = CsgKurlanderBowl.create();
            break;
          case FEATURED_OBJECT:
            mySolid = featuredObject();
            break;
          case IMPORT_OR_FEATURED_OBJECT:
            File fd = new File("/tmp/solid.bin");
            if ( fd.exists() ) {
                mySolid = importFromFile("/tmp/solid.bin");
            }
            else {
                mySolid = featuredObject();
            }
            break;
          case HOLLOW_BOX:
            mySolid = createHollowBox();
            break;
          case HOLED_BOX:
          default:
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
                GeneralModelsBuilder.class,
                VSDK.WARNING,
                "importFromFile",
                "Error reading solid from file " + filename, e);
        }
        catch ( ClassNotFoundException e ) {
            VSDK.reportMessageWithException(
                GeneralModelsBuilder.class,
                VSDK.WARNING,
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
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
        return solid;
    }

    public static PolyhedralBoundedSolid createSphere(double r)
    {
        return createSphere(r, 16, 8);
    }

    public static PolyhedralBoundedSolid createSphere(double r,
        int subdivisionCircunference, int subdivisionHeight)
    {
        PolyhedralBoundedSolid solid;

        Matrix4x4 R = new Matrix4x4();
        R.translation(0.55, 0.55, 0.55);

        Sphere s = new Sphere(r);
        solid = s.exportToPolyhedralBoundedSolid(subdivisionCircunference,
            subdivisionHeight);
        solid.applyTransformation(R);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
        return solid;
    }

    public static PolyhedralBoundedSolid createCone(double r1, double r2, double h)
    {
        return createCone(r1, r2, h, 36, 1);
    }

    public static PolyhedralBoundedSolid createCone(double r1, double r2,
        double h, int subdivisionCircunference, int subdivisionHeight)
    {
        PolyhedralBoundedSolid solid;

        Matrix4x4 R = new Matrix4x4();
        R.translation(0.55, 0.55, 0.05);

        Cone c = new Cone(r1, r2, h);
        solid = c.exportToPolyhedralBoundedSolid(subdivisionCircunference,
            subdivisionHeight);
        solid.applyTransformation(R);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
        return solid;
    }

    public static PolyhedralBoundedSolid createCylinder(double r, double h)
    {
        return createCylinder(r, h, 36, 1);
    }

    public static PolyhedralBoundedSolid createCylinder(double r, double h,
        int subdivisionCircunference, int subdivisionHeight)
    {
        PolyhedralBoundedSolid solid;

        Matrix4x4 R = new Matrix4x4();
        R.translation(0.55, 0.55, 0.05);

        Cone c = new Cone(r, r, h);
        solid = c.exportToPolyhedralBoundedSolid(subdivisionCircunference,
            subdivisionHeight);
        solid.applyTransformation(R);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
        return solid;
    }

    private static PolyhedralBoundedSolid[] buildCsgMoonBlock()
    {
        PolyhedralBoundedSolid cylinderA = createCylinder(0.5, 1.0);
        PolyhedralBoundedSolid cylinderB = createCylinder(0.5, 2);

        Matrix4x4 T = new Matrix4x4();
        T.translation(0.275, 0.0, -0.5);
        cylinderB.applyTransformation(T);

        PolyhedralBoundedSolidValidationEngine.validateIntermediate(cylinderA);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(cylinderB);

        PolyhedralBoundedSolid[] operands = new PolyhedralBoundedSolid[2];
        operands[0] = cylinderA;
        operands[1] = cylinderB;
        return operands;
    }

    public static PolyhedralBoundedSolid createCsgLampShell(
        int subdivisionCircunference, int subdivisionHeight)
    {
        double outerRadius = 0.5;
        double innerRadius = 0.45;

        PolyhedralBoundedSolid outerSphere = createSphere(outerRadius,
            subdivisionCircunference, subdivisionHeight);
        PolyhedralBoundedSolid innerSphere = createSphere(innerRadius,
            subdivisionCircunference, subdivisionHeight);

        PolyhedralBoundedSolid sphericalShell = PolyhedralBoundedSolidModeler.setOp(
            outerSphere, innerSphere, PolyhedralBoundedSolidModeler.SUBTRACT, false);

        // Cube fully contains shell in X/Y, starts below it in Z and stops at
        // ~80% of shell height to mimic the unperforated lamp bowl profile.
        Box clipCubeGeometry = new Box(new Vector3D(1.4, 1.4, 1.05));
        PolyhedralBoundedSolid clipCube = clipCubeGeometry
            .exportToPolyhedralBoundedSolid();
        Matrix4x4 cubeMove = new Matrix4x4();
        cubeMove.translation(0.55, 0.55, 0.325);
        clipCube.applyTransformation(cubeMove);

        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            sphericalShell, clipCube, PolyhedralBoundedSolidModeler.INTERSECTION, false);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(result);
        return result;
    }

    public static PolyhedralBoundedSolid createArrow(double p1, double p2, double p3, double p4)
    {
        PolyhedralBoundedSolid solid;

        Matrix4x4 R = new Matrix4x4();
        R.translation(0.55, 0.55, 0.05);

        Arrow a = new Arrow(p1, p2, p3, p4);
        solid = a.exportToPolyhedralBoundedSolid();
        solid.applyTransformation(R);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
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
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
    }

    /**
    This method implements the example presented in section [MANT1988].9.3,
    and figure [MANT1988].9.11.
    */
    public static PolyhedralBoundedSolid createHoledBox()
    {
        PolyhedralBoundedSolid solid;

        solid = GeneralModelsBuilder.createBox(new Vector3D(0.9, 0.9, 0.9));
        GeneralModelsBuilder.extrudeBox(solid);
        solid.kfmrh(2, 11);
        //R.translation(-0.55, -0.55, -0.55);
        //solid.applyTransformation(R);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);

        return solid;
    }

    /**
    This method is a test for the solution to problem [MANT1988].15.1 on case of one solid
    fully inside another.
    */
    public static PolyhedralBoundedSolid createHollowBox()
    {
        PolyhedralBoundedSolid solidA;
        PolyhedralBoundedSolid solidB;
        PolyhedralBoundedSolid result;

        // Outer box.
        solidA = GeneralModelsBuilder.createBox(new Vector3D(0.9, 0.9, 0.9));

        // Inner box at 80% size, centered at the same position as solidA.
        solidB = GeneralModelsBuilder.createBox(new Vector3D(0.72, 0.72, 0.72));

        // Hollow box = outer box minus inner box.
        result = PolyhedralBoundedSolidModeler.setOp(solidA, solidB, PolyhedralBoundedSolidModeler.SUBTRACT);

        return result;
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
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
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
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
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

        solid = PolyhedralBoundedSolidModeler.createBrepFromParametricCurve(curve);

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

        solid1 = PolyhedralBoundedSolidModeler.createCircularLamina(
            0.0, 0.0, 0.5, 0.0, 6
        );
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
            solid1, solid1.findFace(1), T);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid1);

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

        solid2 = PolyhedralBoundedSolidModeler.createCircularLamina(
            0.0, 0.0, 0.5, 0.0, 6
        );
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
            solid2, solid2.findFace(1), T);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid2);

        //-----------------------------------------------------------------
        R.translation(c.x, -c.y, c.z);
        solid2.applyTransformation(R);

        //-----------------------------------------------------------------
        glue(solid1, solid2, 8, 13);

        //-----------------------------------------------------------------
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid1);
        solid1.maximizeFaces();
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid1);

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
        //PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
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

        solid = PolyhedralBoundedSolidModeler.createCircularLamina(center.x, center.y,
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
        PolyhedralBoundedSolidModeler.addArc(solid, 1, 1, 0.5, 0.25, 0.25, 0.0, 0.0, 90.0, 10);
        rotationalSweepVersion1(solid, 20);
*/
        solid = createTestTorus(4, 16);
        //-----------------------------------------------------------------
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);

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
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
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
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);

        if ( part == 1 ) {
            return solid;
        }

        PolyhedralBoundedSolidModeler.split(solid, sp, solidsAbove, solidsBelow);

        //-----------------------------------------------------------------
        if ( part == 3 ) {
            solid = solidsBelow.get(0);
        }
        else {
            solid = solidsAbove.get(0);
        }

        solid.maximizeFaces();
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);

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
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(a);

        //-----------------------------------------------------------------
        R = new Matrix4x4();
        R.translation(0.5, 0.5, 0.15+0.3);

        box = new Box(new Vector3D(0.5, 1, 0.3));
        b = box.exportToPolyhedralBoundedSolid();
        b.applyTransformation(R);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(b);

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
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(a);

        //-----------------------------------------------------------------
        T = new Matrix4x4();
        T.translation(0.5, 0.9, 0.1);
        box = new Box(new Vector3D(1, 0.2, 0.2));
        b = box.exportToPolyhedralBoundedSolid();
        b.applyTransformation(T);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(b);

        //-----------------------------------------------------------------
        T = new Matrix4x4();
        T.translation(0.1, 0.5, 0.1);
        box = new Box(new Vector3D(0.2, 1, 0.2));
        c = box.exportToPolyhedralBoundedSolid();
        c.applyTransformation(T);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(c);

        //-----------------------------------------------------------------
        T = new Matrix4x4();
        T.translation(0.9, 0.5, 0.1);
        box = new Box(new Vector3D(0.2, 1, 0.2));
        d = box.exportToPolyhedralBoundedSolid();
        d.applyTransformation(T);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(d);

        //-----------------------------------------------------------------
        x = PolyhedralBoundedSolidModeler.setOp(b, c, PolyhedralBoundedSolidModeler.UNION);
        y = PolyhedralBoundedSolidModeler.setOp(a, d, PolyhedralBoundedSolidModeler.UNION);

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
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(a);

        //-----------------------------------------------------------------
        T = new Matrix4x4();
        T.translation(0.5, 0.9, 0.1);
        box = new Box(new Vector3D(1, 0.2, 0.2));
        b = box.exportToPolyhedralBoundedSolid();
        b.applyTransformation(T);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(b);

        //-----------------------------------------------------------------
        T = new Matrix4x4();
        T.translation(0.1, 0.5, 0.1);
        box = new Box(new Vector3D(0.2, 1, 0.2));
        c = box.exportToPolyhedralBoundedSolid();
        c.applyTransformation(T);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(c);

        //-----------------------------------------------------------------
        T = new Matrix4x4();
        T.translation(0.9, 0.5, 0.1);
        box = new Box(new Vector3D(0.2, 1, 0.2));
        d = box.exportToPolyhedralBoundedSolid();
        d.applyTransformation(T);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(d);

        //-----------------------------------------------------------------
        T = new Matrix4x4();
        T.translation(0.1, 0.5, 0.1);
        box = new Box(new Vector3D(0.2, 1, 0.2));
        e = box.exportToPolyhedralBoundedSolid();
        e.applyTransformation(T);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(e);

        //-----------------------------------------------------------------
        T = new Matrix4x4();
        T.translation(0.1, 0.5, 0.9);
        box = new Box(new Vector3D(0.2, 1, 0.2));
        f = box.exportToPolyhedralBoundedSolid();
        f.applyTransformation(T);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(f);

        //-----------------------------------------------------------------
        T = new Matrix4x4();
        T.translation(0.1, 0.1, 0.5);
        box = new Box(new Vector3D(0.2, 0.2, 1));
        g = box.exportToPolyhedralBoundedSolid();
        g.applyTransformation(T);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(g);

        //-----------------------------------------------------------------
        T = new Matrix4x4();
        T.translation(0.1, 0.9, 0.5);
        box = new Box(new Vector3D(0.2, 0.2, 1));
        h = box.exportToPolyhedralBoundedSolid();
        h.applyTransformation(T);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(h);

        //-----------------------------------------------------------------
/*
        ac = PolyhedralBoundedSolidModeler.setOp(a, c, PolyhedralBoundedSolidModeler.UNION);
        bd = PolyhedralBoundedSolidModeler.setOp(b, d, PolyhedralBoundedSolidModeler.UNION);
        abcd = PolyhedralBoundedSolidModeler.setOp(bd, ac, PolyhedralBoundedSolidModeler.UNION);
        eg = PolyhedralBoundedSolidModeler.setOp(e, g, PolyhedralBoundedSolidModeler.UNION);
        fh = PolyhedralBoundedSolidModeler.setOp(f, h, PolyhedralBoundedSolidModeler.UNION);
        efgh = PolyhedralBoundedSolidModeler.setOp(eg, fh, PolyhedralBoundedSolidModeler.UNION);
        total = PolyhedralBoundedSolidModeler.setOp(abcd, efgh, PolyhedralBoundedSolidModeler.UNION);
*/
        ac = PolyhedralBoundedSolidModeler.setOp(a, c, PolyhedralBoundedSolidModeler.UNION);

        operands[0] = ac;
        operands[1] = g;
        return operands;
    }

    public static PolyhedralBoundedSolid csgTest(int part,
        CsgOperationNames op,
        CsgSampleNames sample,
        boolean withDebug)
    {
        PolyhedralBoundedSolid res = null;
        PolyhedralBoundedSolid operands[] = null;

        System.out.printf("Creating C.S.G. test object with parts %d, " + 
            "operation %s, and sample pair %s\n", part,
            op.getLabel(), sample.getLabel());

        switch ( sample ) {
            case MANT1986_2:
                operands =
                    SimpleTestGeometryLibrary.createTestObjectPairMANT1986_2();
                break;
            case STACKED_BLOCKS:
                operands = buildCsgTest2();
                break;
            case MANT1986_3:
                operands =
                    SimpleTestGeometryLibrary.createTestObjectPairMANT1986_3();
                break;
            case HOLLOW_BRICK:
                operands = buildCsgTest4();
                break;
            case CROSS_PAIR:
                operands = buildCsgTest5();
                break;
            case MOON_BLOCK:
                operands = buildCsgMoonBlock();
                break;
            case MANT1988_15_2_HOLED:
                operands =
                    SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_2(-1);
                break;
            case MANT1988_6_13:
                operands =
                    SimpleTestGeometryLibrary.createTestObjectPairMANT1988_6_13();
                break;
            case MANT1988_15_1:
            default:
                operands =
                    SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_1();
                break;
        }

        //-----------------------------------------------------------------
        if ( op == CsgOperationNames.UNION ) {
            res = PolyhedralBoundedSolidModeler.setOp(operands[0], operands[1],
                                         PolyhedralBoundedSolidModeler.UNION, withDebug);
        }
        else if ( op == CsgOperationNames.INTERSECTION ) {
            res = PolyhedralBoundedSolidModeler.setOp(operands[0], operands[1],
                                         PolyhedralBoundedSolidModeler.INTERSECTION, withDebug);
        }
        else if ( op == CsgOperationNames.DIFFERENCE_A_MINUS_B ) {
            res = PolyhedralBoundedSolidModeler.setOp(operands[0], operands[1],
                                         PolyhedralBoundedSolidModeler.SUBTRACT, withDebug);
        }
        else {
            res = PolyhedralBoundedSolidModeler.setOp(operands[1], operands[0],
                                         PolyhedralBoundedSolidModeler.SUBTRACT, withDebug);
        }

        //-----------------------------------------------------------------
        //PolyhedralBoundedSolidValidationEngine.validateIntermediate(operands[0]);
        //PolyhedralBoundedSolidValidationEngine.validateIntermediate(operands[1]);
        //PolyhedralBoundedSolidValidationEngine.validateIntermediate(res);

        if ( part == 2 ) {
            return operands[0];
        }
        if ( part == 3 ) {
            return operands[1];
        }
        return res;
    }

    private static PolyhedralBoundedSolid csgMant1988_15_2Case(
        int situation, int op, boolean withDebug)
    {
        PolyhedralBoundedSolid[] operands;

        operands = SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_2(
            situation);
        return PolyhedralBoundedSolidModeler.setOp(operands[0], operands[1], op, withDebug);
    }

    public static PolyhedralBoundedSolid featuredObject()
    {
        return SimpleTestGeometryLibrary.createTestObjectAPPE1967_3();
/*
        PolyhedralBoundedSolid ops[];
        ops = SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_2();
        return PolyhedralBoundedSolidModeler.setOp(ops[0], ops[1],
                                      PolyhedralBoundedSolidModeler.DIFFERENCE);
*/
    }
}
