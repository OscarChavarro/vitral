//= References:                                                             =
//= [APPE1967] Appel, Arthur. "The notion of quantitative invisibility and  =
//=          the machine rendering of solids". Proceedings, ACM National    =
//=          meeting 1967.                                                  =
//= [MANT1986] Mantyla Martti. "Boolean Operations of 2-Manifolds through   =
//=     Vertex Neighborhood Classification". ACM Transactions on Graphics,  =
//=     Vol. 5, No. 1, January 1986, pp. 1-29.                              =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

// Vitral classes
import { Matrix4x4d } from "../../../../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../../../../common/linealAlgebra/Vector3Dd.js";
import { Box } from "../../../volume/Box.js";
import { PolyhedralBoundedSolid } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidEulerOperators } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
import { PolyhedralBoundedSolidValidationEngine } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { ProcessingElement } from "../../../../../processing/ProcessingElement.js";
import { PolyhedralBoundedSolidModeler } from "../PolyhedralBoundedSolidModeler.js";

/**
This is a utility class containing a lot of geometry examples (mostly
geometry generating procedures). This is a companion class for the
`ComputationalGeometry` and `GeometricModeler` classes, which holds
geometrical queries and other geometry generation procedures.

This class comprises static methods, depending on only of Vitral's Entity's.
From a design point of view, it can be viewed as a "strategy" design pattern
in the sense that encapsulates algorithms. It also could be viewed as a "factory"
or "abstract factory", as it is a class for creating objects, following
the data hierarchy of interface "geometry".

An important characteristic of this class is that the simple geometries that
creates, are usually "inspired" (or "borrowed") from classic textbooks and
papers from the computer graphics academic community. Most of the methods
contain references to figures and other data published by original authors.

The simple geometry test object provided here are useful because:
  - They are procedurally generated. That means that no input/output is used.
    This is useful in developing environments where input/output is not
    available, or where its operation is not ported yet (for example mobile
    devices), so this class can be used for testing Vitral software
    infrastructure, before input/output operation are made available.
  - For algorithm benchmarking. Usually, original authors has invented
    this simple test objects for testing algorithms and measuring its
    performance.  When reimplementing the original algorithms in Vitral,
    running them with similar test objects is useful when comparing
    Vitral's implementation performance.
*/
export class SimpleTestGeometryLibrary extends ProcessingElement {
    /**
    This method uses basic blocks and constructive solid geometry to build up
    a test object similar to the one appearing in the lower part of figure
    [APPE1967].7. Note that this method returns a solid with two shells with
    a total of 54 vertices, 84 edges and 32 faces, as expected from description
    reported in [APPE1967]. From that paper, a hidden line calculation of this
    object consumes 6.5 seconds of CPU time on an IBM 7094 mainframe. That
    reported time could be useful when benchmarking hidden line and other
    visualization algorithms :) This method is provided for benchmarking and
    comparison purposes!
    */
    public static createTestObjectAPPE1967_3(): PolyhedralBoundedSolid {
        let a: PolyhedralBoundedSolid;
        let b: PolyhedralBoundedSolid;

        a = SimpleTestGeometryLibrary.createTestObjectAPPE1967_1();
        b = SimpleTestGeometryLibrary.createTestObjectAPPE1967_2();

        return PolyhedralBoundedSolidModeler.setOp(a, b, PolyhedralBoundedSolidModeler.UNION);
    }

    /**
    This method uses basic blocks and constructive solid geometry to build up
    a test object similar to the one appearing in the upper part of figure
    [APPE1967].7.
    */
    public static createTestObjectAPPE1967_2(): PolyhedralBoundedSolid {
        let a: PolyhedralBoundedSolid;
        let b: PolyhedralBoundedSolid;
        let c: PolyhedralBoundedSolid;
        let d: PolyhedralBoundedSolid;
        let ab: PolyhedralBoundedSolid;
        let cd: PolyhedralBoundedSolid;
        let abcd: PolyhedralBoundedSolid;
        let T: Matrix4x4d;
        let box: Box;

        //-----------------------------------------------------------------
        T = new Matrix4x4d();
        T = T.translation(0.3, 0.1 + 0.4, 0.1 + 0.4);
        box = new Box(new Vector3Dd(0.6, 0.2, 0.2));
        a = box.exportToPolyhedralBoundedSolid();
        PolyhedralBoundedSolidModeler.applyTransformation(a, T);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(a);

        //-----------------------------------------------------------------
        T = new Matrix4x4d();
        T = T.translation(0.5, 0.5, 0.5);
        box = new Box(new Vector3Dd(0.2, 0.2, 1.0));
        b = box.exportToPolyhedralBoundedSolid();
        PolyhedralBoundedSolidModeler.applyTransformation(b, T);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(b);

        //-----------------------------------------------------------------
        T = new Matrix4x4d();
        T = T.translation(0.7, 0.5, 0.9);
        box = new Box(new Vector3Dd(0.6, 0.2, 0.2));
        c = box.exportToPolyhedralBoundedSolid();
        PolyhedralBoundedSolidModeler.applyTransformation(c, T);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(c);

        //-----------------------------------------------------------------
        T = new Matrix4x4d();
        T = T.translation(0.9, 0.5, 0.9);
        box = new Box(new Vector3Dd(0.2, 1.0, 0.2));
        d = box.exportToPolyhedralBoundedSolid();
        PolyhedralBoundedSolidModeler.applyTransformation(d, T);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(d);

        //-----------------------------------------------------------------
        ab = PolyhedralBoundedSolidModeler.setOp(a, b, PolyhedralBoundedSolidModeler.UNION);
        cd = PolyhedralBoundedSolidModeler.setOp(c, d, PolyhedralBoundedSolidModeler.UNION);
        abcd = PolyhedralBoundedSolidModeler.setOp(ab, cd, PolyhedralBoundedSolidModeler.UNION);
        return abcd;
    }

    /**
    This method uses basic blocks and constructive solid geometry to build up
    a test object similar to the one appearing in the middle of figure
    [APPE1967].7.
    */
    public static createTestObjectAPPE1967_1(): PolyhedralBoundedSolid {
        let a: PolyhedralBoundedSolid;
        let b: PolyhedralBoundedSolid;
        let c: PolyhedralBoundedSolid;
        let d: PolyhedralBoundedSolid;
        let e: PolyhedralBoundedSolid;
        let f: PolyhedralBoundedSolid;
        let g: PolyhedralBoundedSolid;
        let h: PolyhedralBoundedSolid;
        let ac: PolyhedralBoundedSolid;
        let bd: PolyhedralBoundedSolid;
        let eg: PolyhedralBoundedSolid;
        let fh: PolyhedralBoundedSolid;
        let abcd: PolyhedralBoundedSolid;
        let efgh: PolyhedralBoundedSolid;
        let total: PolyhedralBoundedSolid;
        let T: Matrix4x4d;
        let box: Box;

        //-----------------------------------------------------------------
        T = new Matrix4x4d();
        T = T.translation(0.5, 0.1, 0.1);
        box = new Box(new Vector3Dd(1, 0.2, 0.2));
        a = box.exportToPolyhedralBoundedSolid();
        PolyhedralBoundedSolidModeler.applyTransformation(a, T);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(a);

        //-----------------------------------------------------------------
        T = new Matrix4x4d();
        T = T.translation(0.5, 0.9, 0.1);
        box = new Box(new Vector3Dd(1, 0.2, 0.2));
        b = box.exportToPolyhedralBoundedSolid();
        PolyhedralBoundedSolidModeler.applyTransformation(b, T);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(b);

        //-----------------------------------------------------------------
        T = new Matrix4x4d();
        T = T.translation(0.1, 0.5, 0.1);
        box = new Box(new Vector3Dd(0.2, 1, 0.2));
        c = box.exportToPolyhedralBoundedSolid();
        PolyhedralBoundedSolidModeler.applyTransformation(c, T);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(c);

        //-----------------------------------------------------------------
        T = new Matrix4x4d();
        T = T.translation(0.9, 0.5, 0.1);
        box = new Box(new Vector3Dd(0.2, 1, 0.2));
        d = box.exportToPolyhedralBoundedSolid();
        PolyhedralBoundedSolidModeler.applyTransformation(d, T);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(d);

        //-----------------------------------------------------------------
        ac = PolyhedralBoundedSolidModeler.setOp(a, c, PolyhedralBoundedSolidModeler.UNION);
        bd = PolyhedralBoundedSolidModeler.setOp(b, d, PolyhedralBoundedSolidModeler.UNION);
        abcd = PolyhedralBoundedSolidModeler.setOp(bd, ac, PolyhedralBoundedSolidModeler.UNION);

        //-----------------------------------------------------------------
        T = new Matrix4x4d();
        T = T.translation(0.1, 0.5, 0.1);
        box = new Box(new Vector3Dd(0.2, 1, 0.2));
        e = box.exportToPolyhedralBoundedSolid();
        PolyhedralBoundedSolidModeler.applyTransformation(e, T);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(e);

        //-----------------------------------------------------------------
        T = new Matrix4x4d();
        T = T.translation(0.1, 0.5, 0.9);
        box = new Box(new Vector3Dd(0.2, 1, 0.2));
        f = box.exportToPolyhedralBoundedSolid();
        PolyhedralBoundedSolidModeler.applyTransformation(f, T);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(f);

        //-----------------------------------------------------------------
        T = new Matrix4x4d();
        T = T.translation(0.1, 0.1, 0.5);
        box = new Box(new Vector3Dd(0.2, 0.2, 1));
        g = box.exportToPolyhedralBoundedSolid();
        PolyhedralBoundedSolidModeler.applyTransformation(g, T);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(g);

        //-----------------------------------------------------------------
        T = new Matrix4x4d();
        T = T.translation(0.1, 0.9, 0.5);
        box = new Box(new Vector3Dd(0.2, 0.2, 1));
        h = box.exportToPolyhedralBoundedSolid();
        PolyhedralBoundedSolidModeler.applyTransformation(h, T);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(h);

        //-----------------------------------------------------------------
        eg = PolyhedralBoundedSolidModeler.setOp(e, g, PolyhedralBoundedSolidModeler.UNION);
        fh = PolyhedralBoundedSolidModeler.setOp(f, h, PolyhedralBoundedSolidModeler.UNION);
        efgh = PolyhedralBoundedSolidModeler.setOp(eg, fh, PolyhedralBoundedSolidModeler.UNION);
        total = PolyhedralBoundedSolidModeler.setOp(abcd, efgh, PolyhedralBoundedSolidModeler.UNION);

        return total;
    }

    /**
    This method builds a test solid for evaluating the splitting algorithm in
    a controlled way. The generated object is similar to that shown on figures
    [MANT1986].4., [MANT1986].5., [MANT1986].8., [MANT1986].10.,
    [MANT1988].14.2., [MANT1988].14.3., and [MANT1988].14.6.

    Generated solid is interesting when splitting with respect to the plane
    Z=0.3 because stress the splitting algorithm to consider multiple vertex
    classification cases.
    */
    public static createTestObjectMANT1986_1(): PolyhedralBoundedSolid {
        let solid: PolyhedralBoundedSolid;

        solid = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolidEulerOperators.mvfs(solid, new Vector3Dd(0.0, 0.4, 0.0), 1, 1);
        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 1, 2, new Vector3Dd(0.94, 0.4, 0.0));
        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 2, 3, new Vector3Dd(0.94, 0.4, 0.46));
        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 3, 4, new Vector3Dd(0.6, 0.4, 0.3));
        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 4, 5, new Vector3Dd(0.37, 0.4, 0.3));
        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 5, 6, new Vector3Dd(0.18, 0.4, 0.46));
        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 6, 7, new Vector3Dd(0.0, 0.4, 0.3));
        PolyhedralBoundedSolidEulerOperators.mef(solid, 1, 1, 7, 6, 1, 2, 2);

        let T = new Matrix4x4d();
        T = T.translation(0, -0.4, 0);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(solid, solid.findFace(1)!, T);

        return solid;
    }

    /**
    This method builds a test sample pair of solids for evaluating
    the set operations algorithm in a controlled way.
    The generated objects are similar to that shown on figures
    [MANT1986].11. and [MANT1988].15.4.
    This set correspond to the simpler of all cases for CSG operations
    test, and its processing in set operations are characterized by
    the following consequences:
      - Only the vertex-face classifier is called (can be processed
        without using a vertex-vertex classifier).
      - On the vertex-face classifier, the second stage (reclassification
        on sectors) is not used, due to non-coplanar cases on neighborhoods.
    */
    public static createTestObjectPairMANT1986_2(): PolyhedralBoundedSolid[] {
        let operands: PolyhedralBoundedSolid[];
        let a: PolyhedralBoundedSolid;
        let b: PolyhedralBoundedSolid;

        operands = new Array<PolyhedralBoundedSolid>(2);

        //-----------------------------------------------------------------
        let R = new Matrix4x4d();
        R = R.translation(0.5, 0.25, 0.3);

        let box = new Box(new Vector3Dd(1, 0.5, 0.6));
        a = box.exportToPolyhedralBoundedSolid();
        PolyhedralBoundedSolidModeler.applyTransformation(a, R);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(a);

        //-----------------------------------------------------------------
        R = new Matrix4x4d();
        R = R.translation(0.5 + 0.24, 0.25 - 0.18, 0.3 + 0.42);

        box = new Box(new Vector3Dd(1, 0.5, 0.6));
        b = box.exportToPolyhedralBoundedSolid();
        PolyhedralBoundedSolidModeler.applyTransformation(b, R);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(b);

        //-----------------------------------------------------------------
        operands[0] = a;
        operands[1] = b;

        return operands;
    }

    /**
    This method builds a test sample pair of solids for evaluating
    the set operations algorithm in a controlled way.
    The generated objects are similar to that shown on figures
    [MANT1986].12. and [MANT1988].15.5.
    The generated simple solids pair is interesting because in the
    current positions their vertices touches in several different ways,
    so a vertex neighborhood classifier is stressed to manage different
    cases. Some faces between the solid are overlap, making this sample
    example a "difficult" one for set operations.
    */
    public static createTestObjectPairMANT1988_3(): PolyhedralBoundedSolid[] {
        let operands: PolyhedralBoundedSolid[];
        let a: PolyhedralBoundedSolid;
        let b: PolyhedralBoundedSolid;

        operands = new Array<PolyhedralBoundedSolid>(2);

        //-----------------------------------------------------------------
        a = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolidEulerOperators.mvfs(a, new Vector3Dd(0.0 + 0.05, 0.42 + 0.05, 0.0 + 0.05), 1, 1);
        PolyhedralBoundedSolidEulerOperators.smev(a, 1, 1, 2, new Vector3Dd(0.92 + 0.05, 0.42 + 0.05, 0.0 + 0.05));
        PolyhedralBoundedSolidEulerOperators.smev(a, 1, 2, 3, new Vector3Dd(0.92 + 0.05, 0.42 + 0.05, 0.72 + 0.05));
        PolyhedralBoundedSolidEulerOperators.smev(a, 1, 3, 4, new Vector3Dd(0.7 + 0.05, 0.42 + 0.05, 0.72 + 0.05));
        PolyhedralBoundedSolidEulerOperators.smev(a, 1, 4, 5, new Vector3Dd(0.7 + 0.05, 0.42 + 0.05, 0.18 + 0.05));
        PolyhedralBoundedSolidEulerOperators.smev(a, 1, 5, 6, new Vector3Dd(0.0 + 0.05, 0.42 + 0.05, 0.18 + 0.05));
        PolyhedralBoundedSolidEulerOperators.mef(a, 1, 1, 6, 5, 1, 2, 2);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(a);

        let T = new Matrix4x4d();
        T = T.translation(0, -0.42, 0);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(a, a.findFace(1)!, T);

        //-----------------------------------------------------------------
        let transformationMatrix = new Matrix4x4d();
        transformationMatrix = transformationMatrix.translation(
            0.05 + 0.58 / 2.0 + (0.92 - 0.58) /*+ 0.0001*/,
            0.05 + 0.42 / 2.0 - 0.42 / 2.0,
            0.05 + 0.18 / 2.0 + 0.18 /*+ 0.0001*/,
        );

        const box = new Box(new Vector3Dd(0.58, 0.42, 0.18));
        b = box.exportToPolyhedralBoundedSolid();
        PolyhedralBoundedSolidModeler.applyTransformation(b, transformationMatrix);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(b);

        //-----------------------------------------------------------------
        operands[0] = a;
        operands[1] = b;

        return operands;
    }

    /**
    This method creates a simple test model pair of objects similar to that
    shown on figure [MANT1988].6.13. These two extrusion solids are an
    interesting example of view profiles, where a 3D solid can be reconstructed
    from its left and front view using boolean set operations (a.k.a.
    "profile set operations" at section [MANT1988].6.4.2.).
    */
    public static createTestObjectPairMANT1988_6_13(): PolyhedralBoundedSolid[] {
        let operands: PolyhedralBoundedSolid[];

        operands = new Array<PolyhedralBoundedSolid>(2);

        //-----------------------------------------------------------------
        let leftView: PolyhedralBoundedSolid;
        leftView = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolidEulerOperators.mvfs(leftView, new Vector3Dd(0, 1, 0), 1, 1);
        PolyhedralBoundedSolidEulerOperators.smev(leftView, 1, 1, 2, new Vector3Dd(3.1 / 3.7, 1, 0));
        PolyhedralBoundedSolidEulerOperators.smev(leftView, 1, 2, 3, new Vector3Dd(3.1 / 3.7, 1, 0.6 / 3.7));
        PolyhedralBoundedSolidEulerOperators.smev(leftView, 1, 3, 4, new Vector3Dd(1.6 / 3.7, 1, 1.2 / 3.7));
        PolyhedralBoundedSolidEulerOperators.smev(leftView, 1, 4, 5, new Vector3Dd(0, 1, 1.2 / 3.7));
        PolyhedralBoundedSolidEulerOperators.mef(leftView, 1, 1, 5, 4, 1, 2, 2);

        let T = new Matrix4x4d();
        T = T.translation(0, -1.0, 0);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(leftView, leftView.findFace(1)!, T);

        //-----------------------------------------------------------------
        let frontView: PolyhedralBoundedSolid;
        frontView = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolidEulerOperators.mvfs(frontView, new Vector3Dd(0, 0, 0), 1, 1);
        PolyhedralBoundedSolidEulerOperators.smev(frontView, 1, 1, 2, new Vector3Dd(0, 1, 0));
        PolyhedralBoundedSolidEulerOperators.smev(frontView, 1, 2, 3, new Vector3Dd(0, 1, 1.2 / 3.7));
        PolyhedralBoundedSolidEulerOperators.smev(frontView, 1, 3, 4, new Vector3Dd(0, 2.8 / 3.7, 1.2 / 3.7));
        PolyhedralBoundedSolidEulerOperators.smev(frontView, 1, 4, 5, new Vector3Dd(0, 2.8 / 3.7, 0.3 / 3.7));
        PolyhedralBoundedSolidEulerOperators.smev(frontView, 1, 5, 6, new Vector3Dd(0, 0.9 / 3.7, 0.3 / 3.7));
        PolyhedralBoundedSolidEulerOperators.smev(frontView, 1, 6, 7, new Vector3Dd(0, 0.9 / 3.7, 1.2 / 3.7));
        PolyhedralBoundedSolidEulerOperators.smev(frontView, 1, 7, 8, new Vector3Dd(0, 0, 1.2 / 3.7));
        PolyhedralBoundedSolidEulerOperators.mef(frontView, 1, 1, 8, 7, 1, 2, 2);

        T = new Matrix4x4d();
        T = T.translation(1, 0, 0);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(frontView, frontView.findFace(1)!, T);

        //-----------------------------------------------------------------
        operands[0] = leftView;
        operands[1] = frontView;

        return operands;
    }

    /**
    This method creates a simple test model pair of objects similar to that
    shown on figure [MANT1988].15.1. These two extrusion solids are an
    interesting example of view profiles, where a 3D solid can be reconstructed
    from its left and front view using boolean set operations (a.k.a.
    "profile set operations" at section [MANT1988].6.4.2.). As commented
    on section [MANT1988].15.1., this simple pair of solids are difficult
    to intersect due to its overlap faces.
    */
    public static createTestObjectPairMANT1988_15_1(): PolyhedralBoundedSolid[] {
        let operands: PolyhedralBoundedSolid[];

        operands = new Array<PolyhedralBoundedSolid>(2);

        //-----------------------------------------------------------------
        let leftView: PolyhedralBoundedSolid;
        leftView = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolidEulerOperators.mvfs(leftView, new Vector3Dd(0, 1, 0), 1, 1);
        PolyhedralBoundedSolidEulerOperators.smev(leftView, 1, 1, 2, new Vector3Dd(1, 1, 0));
        PolyhedralBoundedSolidEulerOperators.smev(leftView, 1, 2, 3, new Vector3Dd(1, 1, 0.25));
        PolyhedralBoundedSolidEulerOperators.smev(leftView, 1, 3, 4, new Vector3Dd(1.0 / 3.0, 1, 0.25));
        PolyhedralBoundedSolidEulerOperators.smev(leftView, 1, 4, 5, new Vector3Dd(1.0 / 3.0, 1, 1));
        PolyhedralBoundedSolidEulerOperators.smev(leftView, 1, 5, 6, new Vector3Dd(0, 1, 1));
        PolyhedralBoundedSolidEulerOperators.mef(leftView, 1, 1, 6, 5, 1, 2, 2);

        let T = new Matrix4x4d();
        T = T.translation(0, -1.0, 0);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(leftView, leftView.findFace(1)!, T);

        //-----------------------------------------------------------------
        let frontView: PolyhedralBoundedSolid;
        frontView = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolidEulerOperators.mvfs(frontView, new Vector3Dd(0, 0, 0), 1, 1);
        PolyhedralBoundedSolidEulerOperators.smev(frontView, 1, 1, 2, new Vector3Dd(0, 1, 0));
        PolyhedralBoundedSolidEulerOperators.smev(frontView, 1, 2, 3, new Vector3Dd(0, 1, 7.0 / 12.0));
        PolyhedralBoundedSolidEulerOperators.smev(frontView, 1, 3, 4, new Vector3Dd(0, 5.0 / 6.0, 1));
        PolyhedralBoundedSolidEulerOperators.smev(frontView, 1, 4, 5, new Vector3Dd(0, 1.0 / 6.0, 1));
        PolyhedralBoundedSolidEulerOperators.smev(frontView, 1, 5, 6, new Vector3Dd(0, 0, 7.0 / 12.0));
        PolyhedralBoundedSolidEulerOperators.mef(frontView, 1, 1, 6, 5, 1, 2, 2);

        T = new Matrix4x4d();
        T = T.translation(1, 0, 0);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(frontView, frontView.findFace(1)!, T);

        //-----------------------------------------------------------------
        operands[0] = leftView;
        operands[1] = frontView;

        return operands;
    }

    /**
    This method creates a simple test model pair of objects similar to that
    shown on figures [MANT1988].15.2. and [MANT1988].15.3. These pair of solid
    are interesting when the position of the wedge is such that its upper edge
    touches the upper face of the block.
    Situation can be:
      -1: Holed object
      0: Limit case
      1: Open object
    */
    public static createTestObjectPairMANT1988_15_2(situation: number): PolyhedralBoundedSolid[] {
        let operands: PolyhedralBoundedSolid[];

        operands = new Array<PolyhedralBoundedSolid>(2);

        //-----------------------------------------------------------------
        let block: PolyhedralBoundedSolid;
        let transformationMatrix: Matrix4x4d;

        transformationMatrix = new Matrix4x4d();
        transformationMatrix = transformationMatrix.translation(0.25 + 0.1375, 0.5, 0.3);

        const box = new Box(new Vector3Dd(0.5, 1, 0.6));
        block = box.exportToPolyhedralBoundedSolid();
        PolyhedralBoundedSolidModeler.applyTransformation(block, transformationMatrix);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(block);

        //-----------------------------------------------------------------
        let wedge: PolyhedralBoundedSolid;
        const facesModelDelta = 0.05;

        wedge = new PolyhedralBoundedSolid();

        switch (situation) {
            case -1: {
                // Lowered wedge, generating a closed holed object
                PolyhedralBoundedSolidEulerOperators.mvfs(wedge, new Vector3Dd(0, 0.225, 0.3 - facesModelDelta), 1, 1);
                PolyhedralBoundedSolidEulerOperators.smev(
                    wedge,
                    1,
                    1,
                    2,
                    new Vector3Dd(0, 0.775, 0.3 - facesModelDelta),
                );
                PolyhedralBoundedSolidEulerOperators.smev(wedge, 1, 2, 3, new Vector3Dd(0, 0.5, 0.6 - facesModelDelta));
                break;
            }
            case 1: {
                // Raised wedge, generating an open object with no holes
                PolyhedralBoundedSolidEulerOperators.mvfs(wedge, new Vector3Dd(0, 0.225, 0.3 + facesModelDelta), 1, 1);
                PolyhedralBoundedSolidEulerOperators.smev(
                    wedge,
                    1,
                    1,
                    2,
                    new Vector3Dd(0, 0.775, 0.3 + facesModelDelta),
                );
                PolyhedralBoundedSolidEulerOperators.smev(wedge, 1, 2, 3, new Vector3Dd(0, 0.5, 0.6 + facesModelDelta));
                break;
            }
            default: {
                // Original on edge wedge, generating non-manifold object
                PolyhedralBoundedSolidEulerOperators.mvfs(wedge, new Vector3Dd(0, 0.225, 0.3), 1, 1);
                PolyhedralBoundedSolidEulerOperators.smev(wedge, 1, 1, 2, new Vector3Dd(0, 0.775, 0.3));
                PolyhedralBoundedSolidEulerOperators.smev(wedge, 1, 2, 3, new Vector3Dd(0, 0.5, 0.6));
                break;
            }
        }

        PolyhedralBoundedSolidEulerOperators.mef(wedge, 1, 1, 3, 2, 1, 2, 2);

        transformationMatrix = new Matrix4x4d();
        transformationMatrix = transformationMatrix.translation(0.775, 0, 0);
        PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(
            wedge,
            wedge.findFace(1)!,
            transformationMatrix,
        );

        //-----------------------------------------------------------------
        operands[0] = block;
        operands[1] = wedge;

        return operands;
    }
}
