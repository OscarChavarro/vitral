package vsdk.toolkit.environment.geometry.polyhedralBoundedSolid;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.processing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidModeler;
import vsdk.toolkit.processing.polyhedralBoundedSolidOperators.SimpleTestGeometryLibrary;

/**
Shared bounded-solid fixtures for Euler, validation, and set-operation
tests.

<p>Traceability: box and MANT fixtures are drawn from [MANT1988] Ch. 6
B-Rep examples, Ch. 9 Euler-construction examples, Fig. 6.13, and Ch. 15
boolean set-operation examples.</p>
 */
public class PolyhedralBoundedSolidTestFixtures
{
    private PolyhedralBoundedSolidTestFixtures()
    {
    }

    public static PolyhedralBoundedSolid createBoxSolid(double sx, double sy,
                                                        double sz, double tx,
                                                        double ty, double tz)
    {
        Box box = new Box(new Vector3D(sx, sy, sz));
        PolyhedralBoundedSolid solid = box.exportToPolyhedralBoundedSolid();
        Matrix4x4 translation = new Matrix4x4();
        translation = translation.translation(tx, ty, tz);
        PolyhedralBoundedSolidModeler.applyTransformation(solid, translation);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
        return solid;
    }

    public static PolyhedralBoundedSolid[] createDisjointBoxPair()
    {
        PolyhedralBoundedSolid solidA = createBoxSolid(1.0, 1.0, 1.0,
            0.0, 0.0, 0.0);
        PolyhedralBoundedSolid solidB = createBoxSolid(1.0, 1.0, 1.0,
            4.0, 0.0, 0.0);
        return new PolyhedralBoundedSolid[] { solidA, solidB  };
    }

    public static PolyhedralBoundedSolid[] createTouchingBoxPair()
    {
        PolyhedralBoundedSolid solidA = createBoxSolid(1.0, 1.0, 1.0,
            0.0, 0.0, 0.0);
        PolyhedralBoundedSolid solidB = createBoxSolid(1.0, 1.0, 1.0,
            1.0, 0.0, 0.0);
        return new PolyhedralBoundedSolid[] { solidA, solidB  };
    }

    public static PolyhedralBoundedSolid[] createContainmentBoxPair()
    {
        PolyhedralBoundedSolid inner = createBoxSolid(1.0, 1.0, 1.0,
            0.0, 0.0, 0.0);
        PolyhedralBoundedSolid outer = createBoxSolid(4.0, 4.0, 4.0,
            0.0, 0.0, 0.0);
        return new PolyhedralBoundedSolid[] { inner, outer  };
    }

    public static PolyhedralBoundedSolid createMant1986_1Solid()
    {
        return SimpleTestGeometryLibrary.createTestObjectMANT1986_1();
    }

    public static PolyhedralBoundedSolid[] createMant1986_2Pair()
    {
        return SimpleTestGeometryLibrary.createTestObjectPairMANT1986_2();
    }

    public static PolyhedralBoundedSolid[] createMant1988_3Pair()
    {
        return SimpleTestGeometryLibrary.createTestObjectPairMANT1988_3();
    }

    public static PolyhedralBoundedSolid[] createMant1988_6_13Pair()
    {
        return SimpleTestGeometryLibrary.createTestObjectPairMANT1988_6_13();
    }

    public static PolyhedralBoundedSolid[] createMant1988_15_1Pair()
    {
        return SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_1();
    }

    public static PolyhedralBoundedSolid[] createMant1988_15_2LimitPair()
    {
        return createMant1988_15_2Pair(0);
    }

    public static PolyhedralBoundedSolid[] createMant1988_15_2Pair(int situation)
    {
        return SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_2(
            situation);
    }
}
