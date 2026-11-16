package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidVolumeOracle.OrientedBox;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologySummary;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidVolumeOracle.translation;

/**
Lamina policy (decision of 2026-11-16): a lamina, two faces back to back
with zero volume, is a valid result. The intersection of two solids that
share part of a face is returned as such a lamina, not as the regularised
empty set, because it is topologically sound and easy to display. A
collapsed lamina, whose area is below EPSILON scaled by the square of the
model scale, is not valid. Tolerances follow the size of the model, so a
small lamina is valid at its own scale; collapsed means a sliver.
 */
class LaminaPolicyTest
{
    @Test
    void given_blocksSharingPartOfAFace_when_intersecting_then_validLamina()
    {
        PolyhedralBoundedSolid a = new OrientedBox(2, 2, 1,
            translation(0, 0, 0)).toSolid();
        PolyhedralBoundedSolid b = new OrientedBox(1, 1, 1,
            translation(0.5, 0.5, 1.0)).toSolid();

        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            a, b, PolyhedralBoundedSolidModeler.INTERSECTION);
        PolyhedralBoundedSolidTopologySummary summary =
            PolyhedralBoundedSolidTopologySummary.from(result);

        assertThat(PolyhedralBoundedSolidModeler.isEmptyResult(result))
            .isFalse();
        assertThat(result.getPolygonsList().size()).isEqualTo(2);
        assertThat(summary.getShellCount()).isEqualTo(1);
        assertThat(PolyhedralBoundedSolidVolumeOracle.solidVolume(result))
            .isCloseTo(0.0, offset(1.0e-12));
        assertThat(PolyhedralBoundedSolidValidationEngine.validateStrict(
            result, new StringBuilder())).isTrue();
    }

    @Test
    void given_laminaWithRealArea_when_validatedStrictly_then_valid()
    {
        PolyhedralBoundedSolid lamina = triangleLamina(1.0, 1.0e-2);

        assertThat(PolyhedralBoundedSolidValidationEngine.validateStrict(
            lamina, new StringBuilder())).isTrue();
    }

    @Test
    void given_collapsedLamina_when_validatedStrictly_then_rejected()
    {
        PolyhedralBoundedSolid lamina = triangleLamina(1.0, 1.0e-7);
        StringBuilder report = new StringBuilder();

        assertThat(PolyhedralBoundedSolidValidationEngine.validateStrict(
            lamina, report)).isFalse();
        // A sliver this thin has no measurable plane either; either report
        // rejects it.
        assertThat(report.toString()).containsAnyOf("is collapsed",
            "was not able to compute containing plane");
    }

    /**
    A right triangle lamina with legs `length` and `height` (area
    length height / 2) in the plane z = 0. With length 1 the model scale is
    about 1.
    */
    private static PolyhedralBoundedSolid triangleLamina(double length,
        double height)
    {
        PolyhedralBoundedSolid solid = new PolyhedralBoundedSolid();

        PolyhedralBoundedSolidEulerOperators.mvfs(solid,
            new Vector3Dd(0, 0, 0), 1, 1);
        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 1, 2,
            new Vector3Dd(length, 0, 0));
        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 2, 3,
            new Vector3Dd(0, height, 0));
        PolyhedralBoundedSolidEulerOperators.smef(solid, 1, 3, 1, 2);
        return solid;
    }
}
