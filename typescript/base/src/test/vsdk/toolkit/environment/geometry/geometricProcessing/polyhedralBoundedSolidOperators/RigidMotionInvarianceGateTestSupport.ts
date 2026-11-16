import { StringBuilder } from "java/lang/StringBuilder.js";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidTopologySummary } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { BooleanOracleFuzz } from "./BooleanOracleFuzz.js";
import type { CsgSampleCorpus } from "./CsgSampleCorpus.js";
import { CsgSampleCorpusFixtures } from "./CsgSampleCorpusFixtures.js";
import { PolyhedralBoundedSolidVolumeOracle } from "./PolyhedralBoundedSolidVolumeOracle.js";

/**
Package-visible members of `RigidMotionInvarianceGateTest` from the Java
suite (`GENERIC_ROTATION`, `Outcome`, `run`). They live in a sibling
module because `SetOpContractTest` reuses the rotation and importing a
`.spec.ts` file would re-run that file's tests.
 */
export class RigidMotionInvarianceGateTestSupport {
    public static readonly GENERIC_ROTATION = new Matrix4x4d().axisRotation(0.61, 0.3, 1.0, 0.2);

    private constructor() {}

    public static run(sample: CsgSampleCorpus, op: number, motion: Matrix4x4d | null): RigidMotionOutcome {
        const operands = CsgSampleCorpusFixtures.createPair(sample);
        const outcome = new RigidMotionOutcome();

        if (motion !== null) {
            PolyhedralBoundedSolidModeler.applyTransformation(operands[0]!, motion);
            PolyhedralBoundedSolidModeler.applyTransformation(operands[1]!, motion);
        }
        const a: PolyhedralBoundedSolid = op === BooleanOracleFuzz.B_MINUS_A ? operands[1]! : operands[0]!;
        const b: PolyhedralBoundedSolid = op === BooleanOracleFuzz.B_MINUS_A ? operands[0]! : operands[1]!;
        try {
            const result = PolyhedralBoundedSolidModeler.setOp(
                a,
                b,
                op === BooleanOracleFuzz.B_MINUS_A ? PolyhedralBoundedSolidModeler.SUBTRACT : op,
                false,
                true,
                false,
            );
            const topology = PolyhedralBoundedSolidTopologySummary.from(result);
            outcome.summary =
                "F=" +
                topology.getFaceCount() +
                " E=" +
                topology.getEdgeCount() +
                " V=" +
                topology.getVertexCount() +
                " S=" +
                topology.getShellCount();
            outcome.volume = PolyhedralBoundedSolidVolumeOracle.solidVolume(result);
            outcome.strictValid = PolyhedralBoundedSolidValidationEngine.validateStrict(result, new StringBuilder());
        } catch (t) {
            outcome.summary = "exception " + (t as Error).constructor.name;
            outcome.volume = Number.NaN;
        }
        outcome.path = PolyhedralBoundedSolidModeler.getLastSetOpResultPath();
        return outcome;
    }
}

/** Java `RigidMotionInvarianceGateTest.Outcome`. */
export class RigidMotionOutcome {
    public summary = "";
    public volume = 0.0;
    public path: string | null = null;
    public strictValid = false;
}
