package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologySummary;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
Regression coverage for the solid/plane split used by
PolyhedralBoundedSolidExample SPLIT_TEST_PART_2 and SPLIT_TEST_PART_3.
 */
class PolyhedralBoundedSolidSplitterMantylaRegressionTest
{
    @BeforeEach
    void reportFatalKernelErrorsAsExceptions()
    {
        VSDK.setWithSystemExit(false);
        VSDK.setWithFatalExceptions(true);
    }

    @AfterEach
    void restoreDesktopFatalErrorBehavior()
    {
        VSDK.setWithSystemExit(true);
        VSDK.setWithFatalExceptions(true);
    }

    @Test
    void given_mantylaFixture_when_splitAtZPointThree_then_bothResultsAreValidSolids()
    {
        PolyhedralBoundedSolid input =
            SimpleTestGeometryLibrary.createTestObjectMANT1986_1();
        InfinitePlane splittingPlane = new InfinitePlane(
            new Vector3Dd(0, 0, 1),
            new Vector3Dd(0, 0, 0.30));
        ArrayList<PolyhedralBoundedSolid> above = new ArrayList<>();
        ArrayList<PolyhedralBoundedSolid> below = new ArrayList<>();

        assertThatCode(() -> PolyhedralBoundedSolidModeler.split(
            input, splittingPlane, above, below)).doesNotThrowAnyException();

        assertThat(above).hasSize(1);
        assertThat(below).hasSize(1);
        assertValidSolid(above.get(0), "above");
        assertValidSolid(below.get(0), "below");
    }

    private static void assertValidSolid(
        PolyhedralBoundedSolid solid,
        String side)
    {
        PolyhedralBoundedSolidTopologySummary topology =
            PolyhedralBoundedSolidTopologySummary.from(solid);

        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(solid))
            .as("%s split result must pass intermediate validation", side)
            .isTrue();
        assertThat(topology.getFaceCount()).as("%s faces", side).isPositive();
        assertThat(topology.getShellCount()).as("%s shells", side).isPositive();
        assertThat(topology.hasUniversalContradiction())
            .as("%s topology: %s", side, topology)
            .isFalse();
    }
}
