import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";
import { Cone } from "vsdk/toolkit/environment/geometry/volume/Cone.js";
import { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { PolyhedralBoundedSolidTestFixtures } from "../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTestFixtures.js";
import { CsgSampleCorpus } from "./CsgSampleCorpus.js";

/**
Builds reusable operand pairs for the boolean set-operation corpus.

<p>Traceability: [MANT1988] Ch. 12 sweep/generator examples for primitive
construction and Ch. 15 boolean examples for pairwise set-operation
scenarios.</p>
 */
export class CsgSampleCorpusFixtures {
    private constructor() {}

    public static createPair(sample: CsgSampleCorpus): PolyhedralBoundedSolid[] {
        switch (sample) {
            case CsgSampleCorpus.HOLLOW_BRICK:
                return CsgSampleCorpusFixtures.createHollowBrickPair();
            case CsgSampleCorpus.MANT1986_2:
                return PolyhedralBoundedSolidTestFixtures.createMant1986_2Pair();
            case CsgSampleCorpus.STACKED_BLOCKS:
                return CsgSampleCorpusFixtures.createStackedBlocksPair();
            case CsgSampleCorpus.CROSS_PAIR:
                return CsgSampleCorpusFixtures.createCrossPair();
            case CsgSampleCorpus.MOON_BLOCK:
                return CsgSampleCorpusFixtures.createMoonBlockPair();
            case CsgSampleCorpus.MANT1988_6_13:
                return PolyhedralBoundedSolidTestFixtures.createMant1988_6_13Pair();
            case CsgSampleCorpus.MANT1988_3:
                return PolyhedralBoundedSolidTestFixtures.createMant1988_3Pair();
            case CsgSampleCorpus.MANT1988_15_2_HOLED:
                return PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(-1);
            case CsgSampleCorpus.MANT1988_15_1:
            default:
                return PolyhedralBoundedSolidTestFixtures.createMant1988_15_1Pair();
        }
    }

    private static createMoonBlockPair(): PolyhedralBoundedSolid[] {
        const operands = new Array<PolyhedralBoundedSolid>(2);
        const cylinderA = CsgSampleCorpusFixtures.createTranslatedCylinder(0.5, 1.0);
        const cylinderB = CsgSampleCorpusFixtures.createTranslatedCylinder(0.5, 2.0);
        let translation = new Matrix4x4d();

        translation = translation.translation(0.275, 0.0, -0.5);
        PolyhedralBoundedSolidModeler.applyTransformation(cylinderB, translation);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(cylinderA);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(cylinderB);

        operands[0] = cylinderA;
        operands[1] = cylinderB;
        return operands;
    }

    private static createTranslatedCylinder(radius: number, height: number): PolyhedralBoundedSolid {
        const solid = new Cone(radius, radius, height).exportToPolyhedralBoundedSolid();
        let translation = new Matrix4x4d();

        translation = translation.translation(0.55, 0.55, 0.05);
        PolyhedralBoundedSolidModeler.applyTransformation(solid, translation);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
        return solid;
    }

    private static createStackedBlocksPair(): PolyhedralBoundedSolid[] {
        const operands = new Array<PolyhedralBoundedSolid>(2);

        operands[0] = CsgSampleCorpusFixtures.createTranslatedBox(1.0, 0.5, 0.3, 0.5, 0.5, 0.15);
        operands[1] = CsgSampleCorpusFixtures.createTranslatedBox(0.5, 1.0, 0.3, 0.5, 0.5, 0.45);
        return operands;
    }

    private static createHollowBrickPair(): PolyhedralBoundedSolid[] {
        const operands = new Array<PolyhedralBoundedSolid>(2);
        const a = CsgSampleCorpusFixtures.createTranslatedBox(1.0, 0.2, 0.2, 0.5, 0.1, 0.1);
        const b = CsgSampleCorpusFixtures.createTranslatedBox(1.0, 0.2, 0.2, 0.5, 0.9, 0.1);
        const c = CsgSampleCorpusFixtures.createTranslatedBox(0.2, 1.0, 0.2, 0.1, 0.5, 0.1);
        const d = CsgSampleCorpusFixtures.createTranslatedBox(0.2, 1.0, 0.2, 0.9, 0.5, 0.1);

        operands[0] = PolyhedralBoundedSolidModeler.setOp(b, c, PolyhedralBoundedSolidModeler.UNION, false);
        operands[1] = PolyhedralBoundedSolidModeler.setOp(a, d, PolyhedralBoundedSolidModeler.UNION, false);
        return operands;
    }

    private static createCrossPair(): PolyhedralBoundedSolid[] {
        const operands = new Array<PolyhedralBoundedSolid>(2);
        const a = CsgSampleCorpusFixtures.createTranslatedBox(1.0, 0.2, 0.2, 0.5, 0.1, 0.1);
        const c = CsgSampleCorpusFixtures.createTranslatedBox(0.2, 1.0, 0.2, 0.1, 0.5, 0.1);
        const g = CsgSampleCorpusFixtures.createTranslatedBox(0.2, 0.2, 1.0, 0.1, 0.1, 0.5);

        operands[0] = PolyhedralBoundedSolidModeler.setOp(a, c, PolyhedralBoundedSolidModeler.UNION, false);
        operands[1] = g;
        return operands;
    }

    private static createTranslatedBox(
        sx: number,
        sy: number,
        sz: number,
        tx: number,
        ty: number,
        tz: number,
    ): PolyhedralBoundedSolid {
        const box = new Box(new Vector3Dd(sx, sy, sz));
        const solid = box.exportToPolyhedralBoundedSolid();
        let translation = new Matrix4x4d();

        translation = translation.translation(tx, ty, tz);
        PolyhedralBoundedSolidModeler.applyTransformation(solid, translation);
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
        return solid;
    }
}
