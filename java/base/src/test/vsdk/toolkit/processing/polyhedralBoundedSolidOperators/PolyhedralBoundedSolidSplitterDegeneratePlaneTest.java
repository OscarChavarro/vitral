package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidVolumeOracle.OrientedBox;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidVolumeOracle.translation;

/**
Splitting a cube [-1,1]^3 with planes in generic and degenerate position,
checked by volume conservation: vol(above) + vol(below) = vol(solid), every
piece strict-valid and non-empty when the plane really cuts the solid.

<p>Traceability: [MANT1988] Ch. 14. Section 14.1 requires the algorithm to
be "general, i.e., it must work correctly for all possible geometric
configurations of solids and planes"; Sections 14.4-14.5 (splitgenerate,
ON-vertex neighborhoods, reclassification rules, Figs. 14.7-14.9) exist
precisely for vertices that lie on the splitting plane. Cases marked
"hidden defect" were found by the Fable 5.2 kernel analysis
(doc/polyhedralBoundedSolidDevelopment/Fable5.2Analysis.md, defect S-01).</p>
 */
class PolyhedralBoundedSolidSplitterDegeneratePlaneTest
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

    static Stream<Arguments> planesThatWorkToday()
    {
        return Stream.of(
            Arguments.of("generic oblique plane",
                new Vector3Dd(0.3, 0.5, 0.8), new Vector3Dd(0.1, 0, 0.05)),
            Arguments.of("axis plane through the middle",
                new Vector3Dd(0, 0, 1), new Vector3Dd(0, 0, 0.1)),
            Arguments.of("plane containing one cube edge",
                new Vector3Dd(1, -0.5, 0), new Vector3Dd(1, 1, 0)),
            Arguments.of("diagonal plane through two opposite edges",
                new Vector3Dd(1, -1, 0), new Vector3Dd(0, 0, 0)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("planesThatWorkToday")
    void given_cuttingPlane_when_split_then_volumeIsConserved(
        String name, Vector3Dd normal, Vector3Dd point)
    {
        assertSplitConservesVolume(normal, point);
    }

    static Stream<Arguments> planesThroughVertices()
    {
        return Stream.of(
            Arguments.of("corner cut through three vertices (x+y+z=1)",
                new Vector3Dd(1, 1, 1), new Vector3Dd(1, 1, -1)),
            Arguments.of("generic plane through exactly one vertex",
                new Vector3Dd(1, 0.7, 0.4), new Vector3Dd(1, 1, -1)));
    }

    /**
    Hidden defect S-01: when the splitting plane passes through a vertex of
    the solid while cutting it, split returns two EMPTY solids, empties the
    input solid as well, and reports nothing. The textbook corner cut
    x + y + z = 1 of a cube is the smallest reproduction.
    */
    @Disabled("Hidden defect S-01: plane through a vertex empties the solid; see Fable5.2Analysis.md")
    @ParameterizedTest(name = "{0}")
    @MethodSource("planesThroughVertices")
    void given_planeThroughVertex_when_split_then_volumeIsConserved(
        String name, Vector3Dd normal, Vector3Dd point)
    {
        assertSplitConservesVolume(normal, point);
    }

    private static void assertSplitConservesVolume(
        Vector3Dd normal, Vector3Dd point)
    {
        OrientedBox cube = new OrientedBox(2, 2, 2, translation(0, 0, 0));
        PolyhedralBoundedSolid solid = cube.toSolid();
        ArrayList<PolyhedralBoundedSolid> above = new ArrayList<>();
        ArrayList<PolyhedralBoundedSolid> below = new ArrayList<>();
        double total = 0.0;

        PolyhedralBoundedSolidModeler.split(solid,
            new InfinitePlane(normal.normalized(), point), above, below);

        assertThat(above).as("pieces above").isNotEmpty();
        assertThat(below).as("pieces below").isNotEmpty();
        ArrayList<PolyhedralBoundedSolid> pieces = new ArrayList<>(above);
        pieces.addAll(below);
        for ( PolyhedralBoundedSolid piece : pieces ) {
            StringBuilder strictReport = new StringBuilder();

            assertThat(piece.getPolygonsList().size())
                .as("piece face count").isGreaterThanOrEqualTo(4);
            assertThat(PolyhedralBoundedSolidValidationEngine.validateStrict(
                piece, strictReport))
                .as("strict validation: %s", strictReport).isTrue();
            total += PolyhedralBoundedSolidVolumeOracle.solidVolume(piece);
        }
        assertThat(total).isCloseTo(cube.volume(),
            org.assertj.core.data.Offset.offset(1.0e-6));
    }
}
