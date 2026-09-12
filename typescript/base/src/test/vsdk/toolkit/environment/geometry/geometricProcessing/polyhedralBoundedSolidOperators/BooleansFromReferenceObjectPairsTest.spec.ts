import { beforeEach, describe, expect, it } from "vitest";

import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";
import { Sphere } from "vsdk/toolkit/environment/geometry/volume/Sphere.js";
import { Math as JavaMath } from "java/lang/Math.js";
import { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidEulerOperators } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { SimpleTestGeometryLibrary } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/SimpleTestGeometryLibrary.js";
import { CsgSampleCorpus } from "./CsgSampleCorpus.js";
import { CsgSampleCorpusFixtures } from "./CsgSampleCorpusFixtures.js";
import { TopologicalSummary } from "./BooleansFromReferenceObjectPairsTestSupport.js";

/**
Harness accommodation: JUnit has no per-test time budget; the reference
matrix runs 33 boolean operations over tessellated operands.
*/
const REFERENCE_TIMEOUT_MS = 900_000;

/**
Systematic boolean regression matrix based on reference object pairs and
single-object reference solids.

<p>Traceability: `doc/references/optimizationsOverMANT1988.md`,
section "Code improvement impact matrix".</p>
 */
describe("BooleansFromReferenceObjectPairsTest", () => {
    /** Java's `ReferenceBooleanOperation` enum. */
    const ReferenceBooleanOperation = {
        UNION: "UNION",
        INTERSECTION: "INTERSECTION",
        DIFFERENCE_A_MINUS_B: "A-B",
        DIFFERENCE_B_MINUS_A: "B-A",
    } as const;
    type ReferenceBooleanOperation = (typeof ReferenceBooleanOperation)[keyof typeof ReferenceBooleanOperation];

    let operandA: PolyhedralBoundedSolid | null;
    let operandB: PolyhedralBoundedSolid | null;

    beforeEach(() => {
        operandA = null;
        operandB = null;
    });

    function runBooleanOperation(operation: ReferenceBooleanOperation): PolyhedralBoundedSolid {
        switch (operation) {
            case ReferenceBooleanOperation.UNION:
                return PolyhedralBoundedSolidModeler.setOp(
                    operandA!,
                    operandB!,
                    PolyhedralBoundedSolidModeler.UNION,
                    false,
                    true,
                    false,
                );
            case ReferenceBooleanOperation.INTERSECTION:
                return PolyhedralBoundedSolidModeler.setOp(
                    operandA!,
                    operandB!,
                    PolyhedralBoundedSolidModeler.INTERSECTION,
                    false,
                    true,
                    false,
                );
            case ReferenceBooleanOperation.DIFFERENCE_A_MINUS_B:
                return PolyhedralBoundedSolidModeler.setOp(
                    operandA!,
                    operandB!,
                    PolyhedralBoundedSolidModeler.SUBTRACT,
                    false,
                    true,
                    false,
                );
            case ReferenceBooleanOperation.DIFFERENCE_B_MINUS_A:
            default:
                return PolyhedralBoundedSolidModeler.setOp(
                    operandB!,
                    operandA!,
                    PolyhedralBoundedSolidModeler.SUBTRACT,
                    false,
                    true,
                    false,
                );
        }
    }

    function expectedMANT1986_2Union(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            12,
            30,
            20,
            12,
            0,
            2,
            [12],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4, 6, 6, 6, 6, 6, 6],
            [0, -180000, 0, 1240000, 500000, 1020000],
        );
    }

    function expectedMANT1986_2Intersection(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            6,
            12,
            8,
            6,
            0,
            2,
            [6],
            [1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4],
            [240000, 0, 420000, 1000000, 320000, 600000],
        );
    }

    function expectedMANT1986_2DifferenceAB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            9,
            21,
            14,
            9,
            0,
            2,
            [9],
            [1, 1, 1, 1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4, 6, 6, 6],
            [0, 0, 0, 1000000, 500000, 600000],
        );
    }

    function expectedMANT1986_2DifferenceBA(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            9,
            21,
            14,
            9,
            0,
            2,
            [9],
            [1, 1, 1, 1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4, 6, 6, 6],
            [240000, -180000, 420000, 1240000, 320000, 1020000],
        );
    }

    function expectedSTACKED_BLOCKSUnion(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            14,
            32,
            20,
            14,
            0,
            2,
            [14],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 6, 6, 6, 6],
            [0, 0, 0, 1000000, 1000000, 600000],
        );
    }

    function expectedSTACKED_BLOCKSIntersection(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            2,
            4,
            4,
            2,
            0,
            2,
            [2],
            [1, 1],
            [4, 4],
            [250000, 250000, 300000, 750000, 750000, 300000],
        );
    }

    function expectedSTACKED_BLOCKSDifferenceAB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            6,
            12,
            8,
            6,
            0,
            2,
            [6],
            [1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4],
            [0, 250000, 0, 1000000, 750000, 300000],
        );
    }

    function expectedSTACKED_BLOCKSDifferenceBA(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            6,
            12,
            8,
            6,
            0,
            2,
            [6],
            [1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4],
            [250000, 0, 300000, 750000, 1000000, 600000],
        );
    }

    function expectedMOON_BLOCKUnion(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            76,
            222,
            148,
            76,
            0,
            2,
            [76],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1,
            ],
            [
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                8, 8, 36, 36, 38, 38,
            ],
            [50000, 50000, -450000, 1325000, 1050000, 1550000],
        );
    }

    function expectedMOON_BLOCKIntersection(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            34,
            96,
            64,
            34,
            0,
            2,
            [34],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 32, 32],
            [325000, 71175, 50000, 1050000, 1028825, 1050000],
        );
    }

    function expectedMOON_BLOCKDifferenceAB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            40,
            114,
            76,
            40,
            0,
            2,
            [40],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1,
            ],
            [
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 38, 38,
            ],
            [50000, 50000, 50000, 687500, 1050000, 1050000],
        );
    }

    function expectedMOON_BLOCKDifferenceBA(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            70,
            204,
            136,
            70,
            0,
            2,
            [70],
            [
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
            ],
            [
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 8, 8, 32, 32, 36,
                36,
            ],
            [325000, 50000, -450000, 1325000, 1050000, 1550000],
        );
    }

    function expectedCROSS_PAIRUnion(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            12,
            27,
            17,
            12,
            0,
            2,
            [12],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4, 4, 4, 4, 6, 6, 6],
            [0, 0, 0, 1000000, 1000000, 1000000],
        );
    }

    function expectedHOLLOW_BRICKUnion(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            10,
            24,
            16,
            12,
            2,
            2,
            [10],
            [1, 1, 1, 1, 1, 1, 1, 1, 2, 2],
            [4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4],
            [0, 0, 0, 1000000, 1000000, 200000],
        );
    }

    function expectedHOLLOW_BRICKIntersection(): TopologicalSummary {
        return TopologicalSummary.of(
            2,
            12,
            24,
            16,
            12,
            0,
            4,
            [6, 6],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4],
            [0, 0, 0, 1000000, 1000000, 200000],
        );
    }

    function expectedHOLLOW_BRICKDifferenceAB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            8,
            18,
            12,
            8,
            0,
            2,
            [8],
            [1, 1, 1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4, 6, 6],
            [0, 200000, 0, 800000, 1000000, 200000],
        );
    }

    function expectedHOLLOW_BRICKDifferenceBA(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            8,
            18,
            12,
            8,
            0,
            2,
            [8],
            [1, 1, 1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4, 6, 6],
            [200000, 0, 0, 1000000, 800000, 200000],
        );
    }

    function expectedMANT1988_6_13Union(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            11,
            27,
            18,
            11,
            0,
            2,
            [11],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4, 4, 5, 5, 8, 8],
            [0, 0, 0, 1000000, 1000000, 324324],
        );
    }

    function expectedMANT1988_6_13Intersection(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            12,
            30,
            20,
            12,
            0,
            2,
            [12],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 8, 8],
            [0, 0, 0, 837838, 1000000, 324324],
        );
    }

    function expectedMANT1988_6_13DifferenceAB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            7,
            15,
            10,
            7,
            0,
            2,
            [7],
            [1, 1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 5, 5],
            [0, 243243, 81081, 837838, 756757, 324324],
        );
    }

    function expectedMANT1988_6_13DifferenceBA(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            12,
            30,
            20,
            12,
            0,
            2,
            [12],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 8, 8],
            [432432, 0, 0, 1000000, 1000000, 324324],
        );
    }

    function expectedMANT1988_15_2HoledUnion(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            14,
            30,
            20,
            16,
            2,
            4,
            [14],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2],
            [3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4],
            [0, 0, 0, 775000, 1000000, 600000],
        );
    }

    function expectedMANT1988_15_2HoledIntersection(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            7,
            15,
            12,
            9,
            2,
            4,
            [7],
            [1, 1, 1, 1, 1, 2, 2],
            [3, 3, 3, 3, 3, 3, 4, 4, 4],
            [137500, 225000, 250000, 637500, 775000, 550000],
        );
    }

    function expectedMANT1988_15_2HoledDifferenceAB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            9,
            21,
            14,
            11,
            2,
            2,
            [9],
            [1, 1, 1, 1, 1, 1, 1, 2, 2],
            [3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4],
            [137500, 0, 0, 637500, 1000000, 600000],
        );
    }

    function expectedMANT1988_15_2HoledDifferenceBA(): TopologicalSummary {
        return TopologicalSummary.of(
            2,
            12,
            24,
            18,
            14,
            2,
            6,
            [6, 6],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2],
            [3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4],
            [0, 225000, 250000, 775000, 775000, 550000],
        );
    }

    function expectedMANT1988_15_1Union(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            10,
            24,
            16,
            10,
            0,
            2,
            [10],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [3, 3, 4, 4, 4, 4, 6, 6, 6, 8],
            [0, 0, 0, 1000000, 1000000, 1000000],
        );
    }

    function expectedMANT1988_15_1Intersection(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            10,
            24,
            16,
            10,
            0,
            2,
            [10],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4, 6, 6, 6, 6],
            [0, 0, 0, 1000000, 1000000, 1000000],
        );
    }

    function expectedMANT1988_15_1DifferenceAB(): TopologicalSummary {
        return TopologicalSummary.of(
            2,
            10,
            18,
            12,
            10,
            0,
            4,
            [5, 5],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [3, 3, 3, 3, 4, 4, 4, 4, 4, 4],
            [0, 0, 583333, 333333, 1000000, 1000000],
        );
    }

    function expectedMANT1988_15_1DifferenceBA(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            8,
            18,
            12,
            8,
            0,
            2,
            [8],
            [1, 1, 1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4, 6, 6],
            [333333, 0, 250000, 1000000, 1000000, 1000000],
        );
    }

    function expectedMANT1988_3Union(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            14,
            35,
            23,
            14,
            0,
            2,
            [14],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4, 4, 5, 5, 6, 6, 6, 6, 8],
            [50000, -160000, 50000, 970000, 470000, 770000],
        );
    }

    function expectedMANT1988_3Intersection(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            6,
            12,
            8,
            6,
            0,
            2,
            [6],
            [1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4],
            [750000, 50000, 230000, 970000, 260000, 410000],
        );
    }

    function expectedMANT1988_3DifferenceAB(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            11,
            27,
            18,
            11,
            0,
            2,
            [11],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4, 4, 6, 6, 6, 8],
            [50000, 50000, 50000, 970000, 470000, 770000],
        );
    }

    function expectedMANT1988_3DifferenceBA(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            8,
            18,
            12,
            8,
            0,
            2,
            [8],
            [1, 1, 1, 1, 1, 1, 1, 1],
            [4, 4, 4, 4, 4, 4, 6, 6],
            [390000, -160000, 230000, 970000, 260000, 410000],
        );
    }

    function expectedLampShellSummary(): TopologicalSummary {
        return TopologicalSummary.of(
            1,
            13,
            24,
            14,
            14,
            1,
            3,
            [13],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2],
            [3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4],
            [300000, 116987, 50000, 1050000, 983013, 850000],
        );
    }

    function expectedFeaturedObjectSummary(): TopologicalSummary {
        return TopologicalSummary.of(
            2,
            32,
            84,
            54,
            34,
            2,
            2,
            [16, 16],
            [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2],
            [4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 6, 6, 8, 8, 8, 8, 10, 10],
            [0, 0, 0, 1000000, 1000000, 1000000],
        );
    }

    function expectedKurlanderBowlSummary(): TopologicalSummary {
        return TopologicalSummary.placeholder("CSG_KURLANDER_BOWL");
    }

    // -----------------------------------------------------------------
    // Local builders for reference single objects
    // -----------------------------------------------------------------

    function createSphere(radius: number, subdivisionsC: number, subdivisionsH: number): PolyhedralBoundedSolid {
        let move = new Matrix4x4d();
        move = move.translation(0.55, 0.55, 0.55);
        const sphere = new Sphere(radius);
        const solid = sphere.exportToPolyhedralBoundedSolid(subdivisionsC, subdivisionsH);
        PolyhedralBoundedSolidModeler.applyTransformation(solid, move);
        return solid;
    }

    function createCsgLampShellReference(): PolyhedralBoundedSolid {
        const outerRadius = 0.5;
        const innerRadius = 0.45;
        const subdivisionCircumference = 3;
        const subdivisionHeight = 1;

        const outerSphere = createSphere(outerRadius, subdivisionCircumference, subdivisionHeight);
        const innerSphere = createSphere(innerRadius, subdivisionCircumference, subdivisionHeight);
        const sphericalShell = PolyhedralBoundedSolidModeler.setOp(
            outerSphere,
            innerSphere,
            PolyhedralBoundedSolidModeler.SUBTRACT,
            false,
        );

        const clipCubeGeometry = new Box(new Vector3Dd(1.4, 1.4, 1.05));
        const clipCube = clipCubeGeometry.exportToPolyhedralBoundedSolid();
        let cubeMove = new Matrix4x4d();
        cubeMove = cubeMove.translation(0.55, 0.55, 0.325);
        PolyhedralBoundedSolidModeler.applyTransformation(clipCube, cubeMove);

        return PolyhedralBoundedSolidModeler.setOp(
            sphericalShell,
            clipCube,
            PolyhedralBoundedSolidModeler.INTERSECTION,
            false,
        );
    }

    /** Java's private nested `KurlanderBowlBuilder`. */
    const KurlanderBowlBuilder = {
        CYLINDER_SIDES: 30,
        OBJECT_SCALE: 0.1,

        s(value: number): number {
            return value * KurlanderBowlBuilder.OBJECT_SCALE;
        },

        booleanOp(a: PolyhedralBoundedSolid, b: PolyhedralBoundedSolid, op: number): PolyhedralBoundedSolid {
            return PolyhedralBoundedSolidModeler.setOp(a, b, op, false);
        },

        createSphere(radius: number, center: Vector3Dd): PolyhedralBoundedSolid {
            const solid = new Sphere(radius).exportToPolyhedralBoundedSolid();
            let t = new Matrix4x4d();
            t = t.translation(center);
            PolyhedralBoundedSolidModeler.applyTransformation(solid, t);
            return solid;
        },

        createCylinder(radius: number, height: number, translation: Vector3Dd): PolyhedralBoundedSolid {
            const solid = PolyhedralBoundedSolidModeler.createCircularLamina(
                0.0,
                0.0,
                radius,
                0.0,
                KurlanderBowlBuilder.CYLINDER_SIDES,
            );
            let sweep = new Matrix4x4d();
            sweep = sweep.translation(0.0, 0.0, height);
            PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(solid, solid.findFace(1)!, sweep);

            let move = new Matrix4x4d();
            move = move.translation(translation);
            PolyhedralBoundedSolidModeler.applyTransformation(solid, move);
            return solid;
        },

        createExtrudedPolygon(points: Vector3Dd[], thickness: number): PolyhedralBoundedSolid {
            const solid = new PolyhedralBoundedSolid();
            let i: number;
            PolyhedralBoundedSolidEulerOperators.mvfs(solid, points[0]!, 1, 1);
            for (i = 1; i < points.length; i++) {
                PolyhedralBoundedSolidEulerOperators.smev(solid, 1, i, i + 1, points[i]!);
            }
            PolyhedralBoundedSolidEulerOperators.smef(solid, 1, points.length, 1, 2);

            let t = new Matrix4x4d();
            t = t.translation(0.0, 0.0, thickness);
            PolyhedralBoundedSolidModeler.translationalSweepExtrudeFacePlanar(solid, solid.findFace(1)!, t);
            return solid;
        },

        createStar(): PolyhedralBoundedSolid {
            const n = 10;
            const outerR = KurlanderBowlBuilder.s(2.0);
            const innerR = KurlanderBowlBuilder.s(0.77);
            const start = JavaMath.toRadians(-90.0);
            const points = new Array<Vector3Dd>(n);
            let i: number;

            for (i = 0; i < n; i++) {
                const a = start + (i * Math.PI) / 5.0;
                const r = i % 2 === 0 ? outerR : innerR;
                points[i] = new Vector3Dd(r * Math.cos(a), r * Math.sin(a), 0.0);
            }

            return KurlanderBowlBuilder.createExtrudedPolygon(points, KurlanderBowlBuilder.s(5.5));
        },

        createMoon(): PolyhedralBoundedSolid {
            const a = KurlanderBowlBuilder.createCylinder(
                KurlanderBowlBuilder.s(1.5),
                KurlanderBowlBuilder.s(5.0),
                new Vector3Dd(0, 0, 0),
            );
            const b = KurlanderBowlBuilder.createCylinder(
                KurlanderBowlBuilder.s(1.5),
                KurlanderBowlBuilder.s(5.0),
                new Vector3Dd(KurlanderBowlBuilder.s(1.1), 0, KurlanderBowlBuilder.s(0.6)),
            );
            return KurlanderBowlBuilder.booleanOp(a, b, PolyhedralBoundedSolidModeler.SUBTRACT);
        },

        placeMotif(motif: PolyhedralBoundedSolid, z: number, azimuthDeg: number): PolyhedralBoundedSolid {
            let t = new Matrix4x4d();
            let ry = new Matrix4x4d();
            let rz = new Matrix4x4d();
            let m: Matrix4x4d;

            t = t.translation(KurlanderBowlBuilder.s(6.0), 0.0, KurlanderBowlBuilder.s(z));
            ry = ry.axisRotation(JavaMath.toRadians(90.0), 0, 1, 0);
            rz = rz.axisRotation(JavaMath.toRadians(azimuthDeg), 0, 0, 1);
            m = rz.multiply(ry.multiply(t));
            PolyhedralBoundedSolidModeler.applyTransformation(motif, m);
            return motif;
        },

        create(): PolyhedralBoundedSolid {
            const outer = KurlanderBowlBuilder.createSphere(
                KurlanderBowlBuilder.s(10.0),
                new Vector3Dd(0, 0, KurlanderBowlBuilder.s(10.0)),
            );
            const inner = KurlanderBowlBuilder.createSphere(
                KurlanderBowlBuilder.s(9.5),
                new Vector3Dd(0, 0, KurlanderBowlBuilder.s(10.0)),
            );
            let shell = KurlanderBowlBuilder.booleanOp(outer, inner, PolyhedralBoundedSolidModeler.SUBTRACT);

            let i: number;
            for (i = 1; i <= 4; i++) {
                const base = -90.0 * i;
                shell = KurlanderBowlBuilder.booleanOp(
                    shell,
                    KurlanderBowlBuilder.placeMotif(KurlanderBowlBuilder.createMoon(), 4.0, base),
                    PolyhedralBoundedSolidModeler.SUBTRACT,
                );
                shell = KurlanderBowlBuilder.booleanOp(
                    shell,
                    KurlanderBowlBuilder.placeMotif(KurlanderBowlBuilder.createMoon(), 14.0, base),
                    PolyhedralBoundedSolidModeler.SUBTRACT,
                );
                shell = KurlanderBowlBuilder.booleanOp(
                    shell,
                    KurlanderBowlBuilder.placeMotif(KurlanderBowlBuilder.createMoon(), 11.5, base - 22.5),
                    PolyhedralBoundedSolidModeler.SUBTRACT,
                );
                shell = KurlanderBowlBuilder.booleanOp(
                    shell,
                    KurlanderBowlBuilder.placeMotif(KurlanderBowlBuilder.createMoon(), 9.0, base - 45.0),
                    PolyhedralBoundedSolidModeler.SUBTRACT,
                );
                shell = KurlanderBowlBuilder.booleanOp(
                    shell,
                    KurlanderBowlBuilder.placeMotif(KurlanderBowlBuilder.createMoon(), 6.5, base - 67.5),
                    PolyhedralBoundedSolidModeler.SUBTRACT,
                );
            }

            for (i = 1; i <= 4; i++) {
                const base = -90.0 * i;
                shell = KurlanderBowlBuilder.booleanOp(
                    shell,
                    KurlanderBowlBuilder.placeMotif(KurlanderBowlBuilder.createStar(), 9.0, base),
                    PolyhedralBoundedSolidModeler.SUBTRACT,
                );
                shell = KurlanderBowlBuilder.booleanOp(
                    shell,
                    KurlanderBowlBuilder.placeMotif(KurlanderBowlBuilder.createStar(), 6.5, base - 22.5),
                    PolyhedralBoundedSolidModeler.SUBTRACT,
                );
                shell = KurlanderBowlBuilder.booleanOp(
                    shell,
                    KurlanderBowlBuilder.placeMotif(KurlanderBowlBuilder.createStar(), 14.0, base - 45.0),
                    PolyhedralBoundedSolidModeler.SUBTRACT,
                );
                shell = KurlanderBowlBuilder.booleanOp(
                    shell,
                    KurlanderBowlBuilder.placeMotif(KurlanderBowlBuilder.createStar(), 4.0, base - 45.0),
                    PolyhedralBoundedSolidModeler.SUBTRACT,
                );
                shell = KurlanderBowlBuilder.booleanOp(
                    shell,
                    KurlanderBowlBuilder.placeMotif(KurlanderBowlBuilder.createStar(), 11.5, base - 67.5),
                    PolyhedralBoundedSolidModeler.SUBTRACT,
                );
            }

            const guide = KurlanderBowlBuilder.createCylinder(
                KurlanderBowlBuilder.s(10.5),
                KurlanderBowlBuilder.s(16.5),
                new Vector3Dd(0, 0, 0),
            );
            return KurlanderBowlBuilder.booleanOp(shell, guide, PolyhedralBoundedSolidModeler.INTERSECTION);
        },
    };

    function createCsgKurlanderBowlReference(): PolyhedralBoundedSolid {
        return KurlanderBowlBuilder.create();
    }

    function pairCase(
        sample: CsgSampleCorpus,
        operation: ReferenceBooleanOperation,
        expected: TopologicalSummary,
    ): [
        CsgSampleCorpus,
        ReferenceBooleanOperation,
        PolyhedralBoundedSolid,
        PolyhedralBoundedSolid,
        TopologicalSummary,
    ] {
        const operands = CsgSampleCorpusFixtures.createPair(sample);
        return [sample, operation, operands[0]!, operands[1]!, expected];
    }

    function legacyPassingReferencePairs(): [
        CsgSampleCorpus,
        ReferenceBooleanOperation,
        PolyhedralBoundedSolid,
        PolyhedralBoundedSolid,
        TopologicalSummary,
    ][] {
        return [
            pairCase(CsgSampleCorpus.MANT1986_2, ReferenceBooleanOperation.UNION, expectedMANT1986_2Union()),
            pairCase(
                CsgSampleCorpus.MANT1986_2,
                ReferenceBooleanOperation.INTERSECTION,
                expectedMANT1986_2Intersection(),
            ),
            pairCase(
                CsgSampleCorpus.MANT1986_2,
                ReferenceBooleanOperation.DIFFERENCE_A_MINUS_B,
                expectedMANT1986_2DifferenceAB(),
            ),
            pairCase(
                CsgSampleCorpus.MANT1986_2,
                ReferenceBooleanOperation.DIFFERENCE_B_MINUS_A,
                expectedMANT1986_2DifferenceBA(),
            ),

            pairCase(CsgSampleCorpus.STACKED_BLOCKS, ReferenceBooleanOperation.UNION, expectedSTACKED_BLOCKSUnion()),
            pairCase(
                CsgSampleCorpus.STACKED_BLOCKS,
                ReferenceBooleanOperation.INTERSECTION,
                expectedSTACKED_BLOCKSIntersection(),
            ),
            pairCase(
                CsgSampleCorpus.STACKED_BLOCKS,
                ReferenceBooleanOperation.DIFFERENCE_A_MINUS_B,
                expectedSTACKED_BLOCKSDifferenceAB(),
            ),
            pairCase(
                CsgSampleCorpus.STACKED_BLOCKS,
                ReferenceBooleanOperation.DIFFERENCE_B_MINUS_A,
                expectedSTACKED_BLOCKSDifferenceBA(),
            ),

            pairCase(CsgSampleCorpus.MOON_BLOCK, ReferenceBooleanOperation.UNION, expectedMOON_BLOCKUnion()),
            pairCase(
                CsgSampleCorpus.MOON_BLOCK,
                ReferenceBooleanOperation.INTERSECTION,
                expectedMOON_BLOCKIntersection(),
            ),
            pairCase(
                CsgSampleCorpus.MOON_BLOCK,
                ReferenceBooleanOperation.DIFFERENCE_A_MINUS_B,
                expectedMOON_BLOCKDifferenceAB(),
            ),
            pairCase(
                CsgSampleCorpus.MOON_BLOCK,
                ReferenceBooleanOperation.DIFFERENCE_B_MINUS_A,
                expectedMOON_BLOCKDifferenceBA(),
            ),

            pairCase(CsgSampleCorpus.CROSS_PAIR, ReferenceBooleanOperation.UNION, expectedCROSS_PAIRUnion()),

            pairCase(CsgSampleCorpus.HOLLOW_BRICK, ReferenceBooleanOperation.UNION, expectedHOLLOW_BRICKUnion()),
            pairCase(
                CsgSampleCorpus.HOLLOW_BRICK,
                ReferenceBooleanOperation.INTERSECTION,
                expectedHOLLOW_BRICKIntersection(),
            ),
            pairCase(
                CsgSampleCorpus.HOLLOW_BRICK,
                ReferenceBooleanOperation.DIFFERENCE_A_MINUS_B,
                expectedHOLLOW_BRICKDifferenceAB(),
            ),
            pairCase(
                CsgSampleCorpus.HOLLOW_BRICK,
                ReferenceBooleanOperation.DIFFERENCE_B_MINUS_A,
                expectedHOLLOW_BRICKDifferenceBA(),
            ),

            pairCase(CsgSampleCorpus.MANT1988_6_13, ReferenceBooleanOperation.UNION, expectedMANT1988_6_13Union()),
            pairCase(
                CsgSampleCorpus.MANT1988_6_13,
                ReferenceBooleanOperation.INTERSECTION,
                expectedMANT1988_6_13Intersection(),
            ),
            pairCase(
                CsgSampleCorpus.MANT1988_6_13,
                ReferenceBooleanOperation.DIFFERENCE_A_MINUS_B,
                expectedMANT1988_6_13DifferenceAB(),
            ),
            pairCase(
                CsgSampleCorpus.MANT1988_6_13,
                ReferenceBooleanOperation.DIFFERENCE_B_MINUS_A,
                expectedMANT1988_6_13DifferenceBA(),
            ),

            pairCase(
                CsgSampleCorpus.MANT1988_15_2_HOLED,
                ReferenceBooleanOperation.UNION,
                expectedMANT1988_15_2HoledUnion(),
            ),
            pairCase(
                CsgSampleCorpus.MANT1988_15_2_HOLED,
                ReferenceBooleanOperation.INTERSECTION,
                expectedMANT1988_15_2HoledIntersection(),
            ),
            pairCase(
                CsgSampleCorpus.MANT1988_15_2_HOLED,
                ReferenceBooleanOperation.DIFFERENCE_A_MINUS_B,
                expectedMANT1988_15_2HoledDifferenceAB(),
            ),
            pairCase(
                CsgSampleCorpus.MANT1988_15_2_HOLED,
                ReferenceBooleanOperation.DIFFERENCE_B_MINUS_A,
                expectedMANT1988_15_2HoledDifferenceBA(),
            ),

            pairCase(CsgSampleCorpus.MANT1988_15_1, ReferenceBooleanOperation.UNION, expectedMANT1988_15_1Union()),
            pairCase(
                CsgSampleCorpus.MANT1988_15_1,
                ReferenceBooleanOperation.INTERSECTION,
                expectedMANT1988_15_1Intersection(),
            ),
            pairCase(
                CsgSampleCorpus.MANT1988_15_1,
                ReferenceBooleanOperation.DIFFERENCE_A_MINUS_B,
                expectedMANT1988_15_1DifferenceAB(),
            ),
            pairCase(
                CsgSampleCorpus.MANT1988_15_1,
                ReferenceBooleanOperation.DIFFERENCE_B_MINUS_A,
                expectedMANT1988_15_1DifferenceBA(),
            ),

            pairCase(CsgSampleCorpus.MANT1988_3, ReferenceBooleanOperation.UNION, expectedMANT1988_3Union()),
            pairCase(
                CsgSampleCorpus.MANT1988_3,
                ReferenceBooleanOperation.INTERSECTION,
                expectedMANT1988_3Intersection(),
            ),
            pairCase(
                CsgSampleCorpus.MANT1988_3,
                ReferenceBooleanOperation.DIFFERENCE_A_MINUS_B,
                expectedMANT1988_3DifferenceAB(),
            ),
            pairCase(
                CsgSampleCorpus.MANT1988_3,
                ReferenceBooleanOperation.DIFFERENCE_B_MINUS_A,
                expectedMANT1988_3DifferenceBA(),
            ),
        ];
    }

    it.each(legacyPassingReferencePairs())(
        "%s + %s",
        (
            sample: CsgSampleCorpus,
            operation: ReferenceBooleanOperation,
            inputOperandA: PolyhedralBoundedSolid,
            inputOperandB: PolyhedralBoundedSolid,
            expected: TopologicalSummary,
        ) => {
            // Arrange
            operandA = inputOperandA;
            operandB = inputOperandB;

            // Action
            const result = runBooleanOperation(operation);
            const actual = TopologicalSummary.from(result);

            // Assert
            expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(result)).toBe(true);
            expect(actual.equals(expected), `${sample} ${operation}: ${actual} != ${expected}`).toBe(true);
        },
        REFERENCE_TIMEOUT_MS,
    );

    it(
        "given_csgLampShell_when_buildingReferenceSolid_then_topologySummaryMatchesReference",
        () => {
            // Arrange

            // Action
            const result = createCsgLampShellReference();
            const actual = TopologicalSummary.from(result);
            const expected = expectedLampShellSummary();

            // Assert
            expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(result)).toBe(true);
            expect(actual.equals(expected), `${actual} != ${expected}`).toBe(true);
        },
        REFERENCE_TIMEOUT_MS,
    );

    it(
        "given_featuredObject_when_buildingReferenceSolid_then_topologySummaryMatchesReference",
        () => {
            // Arrange

            // Action
            const result = SimpleTestGeometryLibrary.createTestObjectAPPE1967_3();
            const actual = TopologicalSummary.from(result);
            const expected = expectedFeaturedObjectSummary();

            // Assert
            expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(result)).toBe(true);
            expect(actual.equals(expected), `${actual} != ${expected}`).toBe(true);
        },
        REFERENCE_TIMEOUT_MS,
    );

    // Java: @Disabled("`CSG_KURLANDER_BOWL` is marked as failing (❌) in the
    // current legacy matrix and remains pending explicit revalidation")
    it.skip("given_csgKurlanderBowl_when_buildingReferenceSolid_then_topologySummaryMatchesReference", () => {
        // Arrange

        // Action
        const result = createCsgKurlanderBowlReference();
        const actual = TopologicalSummary.from(result);
        const expected = expectedKurlanderBowlSummary();

        // Assert
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(result)).toBe(true);
        expect(actual.equals(expected)).toBe(true);
    });

    // Java: @Disabled("Utility snapshot for refreshing hardcoded expected
    // summaries after intentional baseline updates")
    it.skip("dumpReferenceSummariesForBaselineRefresh", () => {
        // Arrange

        // Action
        for (const args of legacyPassingReferencePairs()) {
            const sample = args[0];
            const op = args[1];
            operandA = args[2];
            operandB = args[3];
            const result = runBooleanOperation(op);
            const summary = TopologicalSummary.from(result);
            console.log(sample.name() + " + " + op + " => " + summary.toLiteral());
        }

        const lamp = createCsgLampShellReference();
        const featured = SimpleTestGeometryLibrary.createTestObjectAPPE1967_3();
        const kurlander = createCsgKurlanderBowlReference();

        console.log("CSG_LAMP_SHELL => " + TopologicalSummary.from(lamp).toLiteral());
        console.log("FEATURED_OBJECT => " + TopologicalSummary.from(featured).toLiteral());
        console.log("CSG_KURLANDER_BOWL => " + TopologicalSummary.from(kurlander).toLiteral());

        // Assert
        // TODO: Add assertions once this utility test is converted into a deterministic baseline check.
    });
});
