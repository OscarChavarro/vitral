import { describe, expect, it } from "vitest";

import { IllegalStateException } from "java/lang/IllegalStateException.js";
import { PolyhedralBoundedSolidTopologySummary } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { StepperMotorGuideCsgFixture } from "./StepperMotorGuideCsgFixture.js";

/**
Harness accommodation: JUnit has no per-test time budget; the stepper-guide
fixture chains several strict boolean operations on 32/64-sided profiles.
*/
const GUIDE_TIMEOUT_MS = 900_000;

describe("StepperMotorGuideStrictValidationTest", () => {
    it(
        "given_coincidentLegacyCouplerAndSleeve_when_defaultUnion_then_failureIsImmediate",
        () => {
            const coupler = StepperMotorGuideCsgFixture.createLegacyLowerCoupler();
            const sleeve = StepperMotorGuideCsgFixture.createLegacyBearingSleeve();

            let thrown: unknown = null;
            try {
                PolyhedralBoundedSolidModeler.setOp(coupler, sleeve, PolyhedralBoundedSolidModeler.UNION, false, true);
            } catch (e) {
                thrown = e;
            }
            expect(thrown).toBeInstanceOf(IllegalStateException);
            const message = String((thrown as Error).message);
            expect(message).toContain("Strict boolean result validation failed");
            expect(message).toContain("op=UNION");
            expect(message).toContain("path=normal-pipeline");
            expect(message).toContain("TopologySummary{");
            expect(message).toContain("Face [4] has a loop with fewer than 3 edges");
        },
        GUIDE_TIMEOUT_MS,
    );

    it(
        "given_coincidentLegacyCouplerAndSleeve_when_explicitOptOut_then_legacyInvalidResultIsPreserved",
        () => {
            const coupler = StepperMotorGuideCsgFixture.createLegacyLowerCoupler();
            const sleeve = StepperMotorGuideCsgFixture.createLegacyBearingSleeve();
            PolyhedralBoundedSolidValidationEngine.resetStrictValidationInvocationCount();
            const result = PolyhedralBoundedSolidModeler.setOp(
                coupler,
                sleeve,
                PolyhedralBoundedSolidModeler.UNION,
                false,
                true,
                false,
            );
            const summary = PolyhedralBoundedSolidTopologySummary.from(result);

            expect(result, "legacy B union C evidence: " + summary).not.toBeNull();
            expect(PolyhedralBoundedSolidValidationEngine.getStrictValidationInvocationCount()).toBe(0n);
            expect(
                StepperMotorGuideCsgFixture.hasLoopWithFewerThanThreeDistinctEdges(result),
                "legacy B union C evidence: " + summary,
            ).toBe(true);
            expect(PolyhedralBoundedSolidValidationEngine.validateStrict(result)).toBe(false);
        },
        GUIDE_TIMEOUT_MS,
    );

    it(
        "given_coincidentLegacyCouplerAndSleeve_when_faceMaximizationIsDisabled_then_resultRemainsStrict",
        () => {
            const result = PolyhedralBoundedSolidModeler.setOp(
                StepperMotorGuideCsgFixture.createLegacyLowerCoupler(),
                StepperMotorGuideCsgFixture.createLegacyBearingSleeve(),
                PolyhedralBoundedSolidModeler.UNION,
                false,
                false,
                true,
            );

            expect(result).not.toBeNull();
            expect(StepperMotorGuideCsgFixture.hasLoopWithFewerThanThreeDistinctEdges(result)).toBe(false);
        },
        GUIDE_TIMEOUT_MS,
    );

    it(
        "given_correctedOperationOrder_when_buildingSteppedTube_then_itIsOneGenusOneShell",
        () => {
            const result = StepperMotorGuideCsgFixture.createCorrectedSteppedTube();
            const summary = PolyhedralBoundedSolidTopologySummary.from(result);

            expect(summary.getShellCount(), "corrected stepped tube: " + summary).toBe(1);
            expect(summary.getAdjustedEulerCharacteristic(), "corrected stepped tube: " + summary).toBe(0);
            expect(StepperMotorGuideCsgFixture.hasLoopWithFewerThanThreeDistinctEdges(result)).toBe(false);
        },
        GUIDE_TIMEOUT_MS,
    );

    it(
        "given_correctedFinalGuide_when_probingPocket_then_floorAndSteppedCavityArePreserved",
        () => {
            const result = StepperMotorGuideCsgFixture.createCorrectedFinalGuide();
            const summary = PolyhedralBoundedSolidTopologySummary.from(result);
            const cx = StepperMotorGuideCsgFixture.centerX();
            const cy = StepperMotorGuideCsgFixture.centerY();
            const mm = StepperMotorGuideCsgFixture.MILLIMETERS_TO_MODEL_UNITS;

            expect(summary.getShellCount(), "corrected final guide: " + summary).toBe(1);
            expect(summary.getAdjustedEulerCharacteristic(), "corrected final guide: " + summary).toBe(2);
            expect(StepperMotorGuideCsgFixture.hasLoopWithFewerThanThreeDistinctEdges(result)).toBe(false);

            // Upper 9.02 mm circular cavity.
            expect(
                StepperMotorGuideCsgFixture.containsMaterialAt(
                    result,
                    cx + 4.0 * mm,
                    cy,
                    StepperMotorGuideCsgFixture.TRANSITION_Z + 1.0 * mm,
                ),
            ).toBe(false);
            // Below the transition, the truncated side of the D profile is solid.
            expect(
                StepperMotorGuideCsgFixture.containsMaterialAt(
                    result,
                    cx + 2.35 * mm,
                    cy,
                    StepperMotorGuideCsgFixture.TRANSITION_Z - 1.0 * mm,
                ),
            ).toBe(true);
            // The pocket is open immediately above the base and closed below it.
            expect(
                StepperMotorGuideCsgFixture.containsMaterialAt(
                    result,
                    cx,
                    cy,
                    StepperMotorGuideCsgFixture.BASE_TOP_Z + 0.5 * mm,
                ),
            ).toBe(false);
            expect(
                StepperMotorGuideCsgFixture.containsMaterialAt(
                    result,
                    cx,
                    cy,
                    StepperMotorGuideCsgFixture.BASE_TOP_Z - 0.5 * mm,
                ),
            ).toBe(true);
        },
        GUIDE_TIMEOUT_MS,
    );
});
