package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologySummary;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StepperMotorGuideStrictValidationTest
{
    @Test
    void given_coincidentLegacyCouplerAndSleeve_when_defaultUnion_then_failureIsImmediate()
    {
        PolyhedralBoundedSolid coupler =
            StepperMotorGuideCsgFixture.createLegacyLowerCoupler();
        PolyhedralBoundedSolid sleeve =
            StepperMotorGuideCsgFixture.createLegacyBearingSleeve();

        assertThatThrownBy(() -> PolyhedralBoundedSolidModeler.setOp(
            coupler, sleeve, PolyhedralBoundedSolidModeler.UNION,
            false, true))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Strict boolean result validation failed")
            .hasMessageContaining("op=UNION")
            .hasMessageContaining("path=normal-pipeline")
            .hasMessageContaining("TopologySummary{")
            .hasMessageContaining(
                "Face [4] has a loop with fewer than 3 edges");
    }

    @Test
    void given_coincidentLegacyCouplerAndSleeve_when_explicitOptOut_then_legacyInvalidResultIsPreserved()
    {
        PolyhedralBoundedSolid coupler =
            StepperMotorGuideCsgFixture.createLegacyLowerCoupler();
        PolyhedralBoundedSolid sleeve =
            StepperMotorGuideCsgFixture.createLegacyBearingSleeve();
        PolyhedralBoundedSolidValidationEngine
            .resetStrictValidationInvocationCount();
        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            coupler, sleeve,
            PolyhedralBoundedSolidModeler.UNION, false, true, false);
        PolyhedralBoundedSolidTopologySummary summary =
            PolyhedralBoundedSolidTopologySummary.from(result);

        assertThat(result)
            .as("legacy B union C evidence: " + summary)
            .isNotNull();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .getStrictValidationInvocationCount()).isZero();
        assertThat(StepperMotorGuideCsgFixture
            .hasLoopWithFewerThanThreeDistinctEdges(result))
            .as("legacy B union C evidence: " + summary)
            .isTrue();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateStrict(result)).isFalse();
    }

    @Test
    void given_coincidentLegacyCouplerAndSleeve_when_faceMaximizationIsDisabled_then_resultRemainsStrict()
    {
        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            StepperMotorGuideCsgFixture.createLegacyLowerCoupler(),
            StepperMotorGuideCsgFixture.createLegacyBearingSleeve(),
            PolyhedralBoundedSolidModeler.UNION, false, false, true);

        assertThat(result).isNotNull();
        assertThat(StepperMotorGuideCsgFixture
            .hasLoopWithFewerThanThreeDistinctEdges(result)).isFalse();
    }

    @Test
    void given_correctedOperationOrder_when_buildingSteppedTube_then_itIsOneGenusOneShell()
    {
        PolyhedralBoundedSolid result =
            StepperMotorGuideCsgFixture.createCorrectedSteppedTube();
        PolyhedralBoundedSolidTopologySummary summary =
            PolyhedralBoundedSolidTopologySummary.from(result);

        assertThat(summary.getShellCount())
            .as("corrected stepped tube: " + summary).isEqualTo(1);
        assertThat(summary.getAdjustedEulerCharacteristic())
            .as("corrected stepped tube: " + summary).isZero();
        assertThat(StepperMotorGuideCsgFixture
            .hasLoopWithFewerThanThreeDistinctEdges(result)).isFalse();
    }

    @Test
    void given_correctedFinalGuide_when_probingPocket_then_floorAndSteppedCavityArePreserved()
    {
        PolyhedralBoundedSolid result =
            StepperMotorGuideCsgFixture.createCorrectedFinalGuide();
        PolyhedralBoundedSolidTopologySummary summary =
            PolyhedralBoundedSolidTopologySummary.from(result);
        double cx = StepperMotorGuideCsgFixture.centerX();
        double cy = StepperMotorGuideCsgFixture.centerY();
        double mm = StepperMotorGuideCsgFixture.MILLIMETERS_TO_MODEL_UNITS;

        assertThat(summary.getShellCount())
            .as("corrected final guide: " + summary).isEqualTo(1);
        assertThat(summary.getAdjustedEulerCharacteristic())
            .as("corrected final guide: " + summary).isEqualTo(2);
        assertThat(StepperMotorGuideCsgFixture
            .hasLoopWithFewerThanThreeDistinctEdges(result)).isFalse();

        // Upper 9.02 mm circular cavity.
        assertThat(StepperMotorGuideCsgFixture.containsMaterialAt(
            result, cx + 4.0 * mm, cy,
            StepperMotorGuideCsgFixture.TRANSITION_Z + 1.0 * mm))
            .isFalse();
        // Below the transition, the truncated side of the D profile is solid.
        assertThat(StepperMotorGuideCsgFixture.containsMaterialAt(
            result, cx + 2.35 * mm, cy,
            StepperMotorGuideCsgFixture.TRANSITION_Z - 1.0 * mm))
            .isTrue();
        // The pocket is open immediately above the base and closed below it.
        assertThat(StepperMotorGuideCsgFixture.containsMaterialAt(
            result, cx, cy,
            StepperMotorGuideCsgFixture.BASE_TOP_Z + 0.5 * mm))
            .isFalse();
        assertThat(StepperMotorGuideCsgFixture.containsMaterialAt(
            result, cx, cy,
            StepperMotorGuideCsgFixture.BASE_TOP_Z - 0.5 * mm))
            .isTrue();
    }
}
