package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.PolyhedralBoundedSolidTestFixtures;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

/**
Builds reusable operand pairs for the boolean set-operation corpus.

<p>Traceability: [MANT1988] Ch. 12 sweep/generator examples for primitive
construction and Ch. 15 boolean examples for pairwise set-operation
scenarios.</p>
 */
final class CsgSampleCorpusFixtures
{
    private CsgSampleCorpusFixtures()
    {
    }

    static PolyhedralBoundedSolid[] createPair(CsgSampleCorpus sample)
    {
        switch ( sample ) {
            case HOLLOW_BRICK:
                return createHollowBrickPair();
            case MANT1986_2:
                return PolyhedralBoundedSolidTestFixtures.createMant1986_2Pair();
            case STACKED_BLOCKS:
                return createStackedBlocksPair();
            case CROSS_PAIR:
                return createCrossPair();
            case MOON_BLOCK:
                return createMoonBlockPair();
            case MANT1988_6_13:
                return PolyhedralBoundedSolidTestFixtures.createMant1988_6_13Pair();
            case MANT1988_3:
                return PolyhedralBoundedSolidTestFixtures.createMant1988_3Pair();
            case MANT1988_15_2_HOLED:
                return PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(-1);
            case MANT1988_15_1:
            default:
                return PolyhedralBoundedSolidTestFixtures.createMant1988_15_1Pair();
        }
    }

    private static PolyhedralBoundedSolid[] createMoonBlockPair()
    {
        PolyhedralBoundedSolid[] operands = new PolyhedralBoundedSolid[2];
        PolyhedralBoundedSolid cylinderA = createTranslatedCylinder(0.5, 1.0);
        PolyhedralBoundedSolid cylinderB = createTranslatedCylinder(0.5, 2.0);
        Matrix4x4 translation = new Matrix4x4();

        translation = translation.translation(0.275, 0.0, -0.5);
        PolyhedralBoundedSolidEulerOperators.applyTransformation(cylinderB, translation);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(cylinderA);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(cylinderB);

        operands[0] = cylinderA;
        operands[1] = cylinderB;
        return operands;
    }

    private static PolyhedralBoundedSolid createTranslatedCylinder(
        double radius, double height)
    {
        PolyhedralBoundedSolid solid = new Cone(radius, radius, height)
            .exportToPolyhedralBoundedSolid();
        Matrix4x4 translation = new Matrix4x4();

        translation = translation.translation(0.55, 0.55, 0.05);
        PolyhedralBoundedSolidEulerOperators.applyTransformation(solid, translation);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
        return solid;
    }

    private static PolyhedralBoundedSolid[] createStackedBlocksPair()
    {
        PolyhedralBoundedSolid[] operands = new PolyhedralBoundedSolid[2];

        operands[0] = createTranslatedBox(1.0, 0.5, 0.3, 0.5, 0.5, 0.15);
        operands[1] = createTranslatedBox(0.5, 1.0, 0.3, 0.5, 0.5, 0.45);
        return operands;
    }

    private static PolyhedralBoundedSolid[] createHollowBrickPair()
    {
        PolyhedralBoundedSolid[] operands = new PolyhedralBoundedSolid[2];
        PolyhedralBoundedSolid a = createTranslatedBox(1.0, 0.2, 0.2,
            0.5, 0.1, 0.1);
        PolyhedralBoundedSolid b = createTranslatedBox(1.0, 0.2, 0.2,
            0.5, 0.9, 0.1);
        PolyhedralBoundedSolid c = createTranslatedBox(0.2, 1.0, 0.2,
            0.1, 0.5, 0.1);
        PolyhedralBoundedSolid d = createTranslatedBox(0.2, 1.0, 0.2,
            0.9, 0.5, 0.1);

        operands[0] = PolyhedralBoundedSolidModeler.setOp(
            b, c, PolyhedralBoundedSolidModeler.UNION, false);
        operands[1] = PolyhedralBoundedSolidModeler.setOp(
            a, d, PolyhedralBoundedSolidModeler.UNION, false);
        return operands;
    }

    private static PolyhedralBoundedSolid[] createCrossPair()
    {
        PolyhedralBoundedSolid[] operands = new PolyhedralBoundedSolid[2];
        PolyhedralBoundedSolid a = createTranslatedBox(1.0, 0.2, 0.2,
            0.5, 0.1, 0.1);
        PolyhedralBoundedSolid c = createTranslatedBox(0.2, 1.0, 0.2,
            0.1, 0.5, 0.1);
        PolyhedralBoundedSolid g = createTranslatedBox(0.2, 0.2, 1.0,
            0.1, 0.1, 0.5);

        operands[0] = PolyhedralBoundedSolidModeler.setOp(
            a, c, PolyhedralBoundedSolidModeler.UNION, false);
        operands[1] = g;
        return operands;
    }

    private static PolyhedralBoundedSolid createTranslatedBox(
        double sx, double sy, double sz,
        double tx, double ty, double tz)
    {
        Box box = new Box(new Vector3D(sx, sy, sz));
        PolyhedralBoundedSolid solid = box.exportToPolyhedralBoundedSolid();
        Matrix4x4 translation = new Matrix4x4();

        translation = translation.translation(tx, ty, tz);
        PolyhedralBoundedSolidEulerOperators.applyTransformation(solid, translation);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
        return solid;
    }
}
