import { describe, expect, it } from "vitest";

import { IllegalStateException } from "java/lang/IllegalStateException.js";
import { RuntimeException } from "java/lang/RuntimeException.js";
import { StringBuilder } from "java/lang/StringBuilder.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { PolyhedralBoundedSolidStructuralCopy } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidStructuralCopy.js";
import { PolyhedralBoundedSolidTopologySummary } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { InvalidOperandException } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/InvalidOperandException.js";
import { InvalidSolidResultException } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/InvalidSolidResultException.js";
import { NonManifoldBooleanResultException } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/NonManifoldBooleanResultException.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { PolyhedralBoundedSolidOperationException } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidOperationException.js";
import { SolidOperationFailedException } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/SolidOperationFailedException.js";
import { CsgSampleCorpus } from "./CsgSampleCorpus.js";
import { CsgSampleCorpusFixtures } from "./CsgSampleCorpusFixtures.js";
import { OrientedBox, PolyhedralBoundedSolidVolumeOracle } from "./PolyhedralBoundedSolidVolumeOracle.js";
import { RigidMotionInvarianceGateTestSupport } from "./RigidMotionInvarianceGateTestSupport.js";

const translation = PolyhedralBoundedSolidVolumeOracle.translation;

/**
Public contract of the set operations (Fable5.2Analysis.md Stage F):
<ul>
  <li>F.1 setOp leaves its operands intact; setOpDestructive consumes
      them, as [MANT1988] Sec. 15.8 does.</li>
  <li>F.2 failures are typed, all under
      `PolyhedralBoundedSolidOperationException` (still an
      `IllegalStateException`).</li>
  <li>F.4 a right empty result is an empty solid that
      `PolyhedralBoundedSolidModeler.isEmptyResult` recognizes; an
      empty result the operands disprove is a
      `SolidOperationFailedException`, never a silent empty solid.</li>
</ul>
 */
describe("SetOpContractTest", () => {
    function corpusOperations(): [CsgSampleCorpus, number][] {
        const rows: [CsgSampleCorpus, number][] = [];
        for (const sample of CsgSampleCorpus.values()) {
            for (const op of [
                PolyhedralBoundedSolidModeler.UNION,
                PolyhedralBoundedSolidModeler.INTERSECTION,
                PolyhedralBoundedSolidModeler.SUBTRACT,
            ]) {
                rows.push([sample, op]);
            }
        }
        return rows;
    }

    it.each(corpusOperations())(
        "given_referencePair_when_setOp_then_operandsAreUnchanged: %s op=%i",
        (sample: CsgSampleCorpus, op: number) => {
            const pair = CsgSampleCorpusFixtures.createPair(sample);
            const beforeA = PolyhedralBoundedSolidTopologySummary.from(pair[0]!).toString();
            const beforeB = PolyhedralBoundedSolidTopologySummary.from(pair[1]!).toString();
            const volumeA = PolyhedralBoundedSolidVolumeOracle.solidVolume(pair[0]!);
            const volumeB = PolyhedralBoundedSolidVolumeOracle.solidVolume(pair[1]!);

            try {
                PolyhedralBoundedSolidModeler.setOp(pair[0]!, pair[1]!, op, false, true, false);
            } catch (e) {
                // The contract holds after a failure as well.
                if (!(e instanceof PolyhedralBoundedSolidOperationException)) {
                    throw e;
                }
            }

            expect(PolyhedralBoundedSolidTopologySummary.from(pair[0]!).toString()).toBe(beforeA);
            expect(PolyhedralBoundedSolidTopologySummary.from(pair[1]!).toString()).toBe(beforeB);
            expect(PolyhedralBoundedSolidVolumeOracle.solidVolume(pair[0]!)).toBe(volumeA);
            expect(PolyhedralBoundedSolidVolumeOracle.solidVolume(pair[1]!)).toBe(volumeB);
        },
    );

    it("given_operands_when_setOpDestructive_then_operandsAreConsumed", () => {
        const a = new OrientedBox(2, 2, 2, translation(0, 0, 0)).toSolid();
        const b = new OrientedBox(2, 2, 2, translation(1, 1, 1)).toSolid();

        const result = PolyhedralBoundedSolidModeler.setOpDestructive(
            a,
            b,
            PolyhedralBoundedSolidModeler.UNION,
            false,
            true,
            true,
        );

        expect(Math.abs(PolyhedralBoundedSolidVolumeOracle.solidVolume(result) - 15.0)).toBeLessThanOrEqual(1.0e-9);
        expect(
            Math.abs(PolyhedralBoundedSolidVolumeOracle.solidVolume(a) - 8.0),
            "operand A after a destructive union",
        ).toBeGreaterThan(1.0e-9);
    });

    it("given_sameSolidTwice_when_subtracting_then_resultIsTheEmptySolid", () => {
        const a = new OrientedBox(1, 2, 3, translation(0.5, 0, 0)).toSolid();
        const b = PolyhedralBoundedSolidStructuralCopy.copy(a);

        const result = PolyhedralBoundedSolidModeler.setOp(a, b, PolyhedralBoundedSolidModeler.SUBTRACT);

        expect(PolyhedralBoundedSolidModeler.isEmptyResult(result)).toBe(true);
    });

    it.each([PolyhedralBoundedSolidModeler.INTERSECTION, PolyhedralBoundedSolidModeler.SUBTRACT])(
        "given_disjointOrContainedOperands_when_emptyByDefinition_then_emptySolid: op=%i",
        (op: number) => {
            const a =
                op === PolyhedralBoundedSolidModeler.SUBTRACT
                    ? new OrientedBox(1, 1, 1, translation(0, 0, 0)).toSolid()
                    : new OrientedBox(1, 1, 1, translation(5, 0, 0)).toSolid();
            const b = new OrientedBox(3, 3, 3, translation(0, 0, 0)).toSolid();

            const result = PolyhedralBoundedSolidModeler.setOp(a, b, op);

            expect(PolyhedralBoundedSolidModeler.isEmptyResult(result)).toBe(true);
        },
    );

    /**
    The rotated cross pair used to come back as an empty solid without any
    report (Stage D notes). Union and intersection give the solid; the
    difference leaves two bars that touch only along an edge, a non-manifold
    result that is rejected (policy of B-04).
    */
    it.each([
        PolyhedralBoundedSolidModeler.UNION,
        PolyhedralBoundedSolidModeler.INTERSECTION,
        PolyhedralBoundedSolidModeler.SUBTRACT,
    ])("given_rotatedCrossPair_when_setOp_then_neverASilentEmptySolid: op=%i", (op: number) => {
        const pair = CsgSampleCorpusFixtures.createPair(CsgSampleCorpus.CROSS_PAIR);
        PolyhedralBoundedSolidModeler.applyTransformation(
            pair[0]!,
            RigidMotionInvarianceGateTestSupport.GENERIC_ROTATION,
        );
        PolyhedralBoundedSolidModeler.applyTransformation(
            pair[1]!,
            RigidMotionInvarianceGateTestSupport.GENERIC_ROTATION,
        );

        if (op === PolyhedralBoundedSolidModeler.SUBTRACT) {
            expect(() => PolyhedralBoundedSolidModeler.setOp(pair[0]!, pair[1]!, op, false, true, false)).toThrow(
                NonManifoldBooleanResultException,
            );
            return;
        }
        const result = PolyhedralBoundedSolidModeler.setOp(pair[0]!, pair[1]!, op, false, true, false);

        expect(PolyhedralBoundedSolidModeler.isEmptyResult(result), "silent empty result").toBe(false);
        expect(PolyhedralBoundedSolidValidationEngine.validateStrict(result, new StringBuilder())).toBe(true);
    });

    it("given_typedFailures_then_theyShareTheBaseAndStayIllegalState", () => {
        const nonManifold = new NonManifoldBooleanResultException("x");
        expect(nonManifold).toBeInstanceOf(PolyhedralBoundedSolidOperationException);
        expect(nonManifold).toBeInstanceOf(IllegalStateException);
        expect(new InvalidSolidResultException("x")).toBeInstanceOf(PolyhedralBoundedSolidOperationException);
        const failed = new SolidOperationFailedException("x", new RuntimeException());
        expect(failed).toBeInstanceOf(PolyhedralBoundedSolidOperationException);
        expect(failed.getCause()).toBeInstanceOf(RuntimeException);
        expect(new InvalidOperandException("x")).toBeInstanceOf(PolyhedralBoundedSolidOperationException);
    });

    /**
    An operand that is not a valid boundary model stops the operation before
    the pipeline runs: a cube with one vertex lifted has non-planar faces.
    */
    it("given_operandWithNonPlanarFaces_when_setOp_then_invalidOperand", () => {
        const warped = new OrientedBox(1, 1, 1, translation(0, 0, 0)).toSolid();
        const box = new OrientedBox(1, 1, 1, translation(0.3, 0.2, 0.1)).toSolid();
        warped.getVerticesList().get(0)!.position = warped
            .getVerticesList()
            .get(0)!
            .position.add(new Vector3Dd(0.1, 0.2, 0.3));

        expect(() => PolyhedralBoundedSolidModeler.setOp(warped, box, PolyhedralBoundedSolidModeler.UNION)).toThrow(
            InvalidOperandException,
        );
    });
});
