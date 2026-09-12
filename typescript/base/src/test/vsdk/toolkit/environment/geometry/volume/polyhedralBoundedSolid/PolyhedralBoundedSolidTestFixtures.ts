import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";
import { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { SimpleTestGeometryLibrary } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/SimpleTestGeometryLibrary.js";

/**
Shared bounded-solid fixtures for Euler, validation, and set-operation
tests.

<p>Traceability: box and MANT fixtures are drawn from [MANT1988] Ch. 6
B-Rep examples, Ch. 9 Euler-construction examples, Fig. 6.13, and Ch. 15
boolean set-operation examples.</p>
 */
export class PolyhedralBoundedSolidTestFixtures {
    private constructor() {}

    public static createBoxSolid(
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

    public static createDisjointBoxPair(): PolyhedralBoundedSolid[] {
        const solidA = PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        const solidB = PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0, 4.0, 0.0, 0.0);
        return [solidA, solidB];
    }

    public static createTouchingBoxPair(): PolyhedralBoundedSolid[] {
        const solidA = PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        const solidB = PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0, 1.0, 0.0, 0.0);
        return [solidA, solidB];
    }

    public static createContainmentBoxPair(): PolyhedralBoundedSolid[] {
        const inner = PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        const outer = PolyhedralBoundedSolidTestFixtures.createBoxSolid(4.0, 4.0, 4.0, 0.0, 0.0, 0.0);
        return [inner, outer];
    }

    public static createMant1986_1Solid(): PolyhedralBoundedSolid {
        return SimpleTestGeometryLibrary.createTestObjectMANT1986_1();
    }

    public static createMant1986_2Pair(): PolyhedralBoundedSolid[] {
        return SimpleTestGeometryLibrary.createTestObjectPairMANT1986_2();
    }

    public static createMant1988_3Pair(): PolyhedralBoundedSolid[] {
        return SimpleTestGeometryLibrary.createTestObjectPairMANT1988_3();
    }

    public static createMant1988_6_13Pair(): PolyhedralBoundedSolid[] {
        return SimpleTestGeometryLibrary.createTestObjectPairMANT1988_6_13();
    }

    public static createMant1988_15_1Pair(): PolyhedralBoundedSolid[] {
        return SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_1();
    }

    public static createMant1988_15_2LimitPair(): PolyhedralBoundedSolid[] {
        return PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(0);
    }

    public static createMant1988_15_2Pair(situation: number): PolyhedralBoundedSolid[] {
        return SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_2(situation);
    }
}
