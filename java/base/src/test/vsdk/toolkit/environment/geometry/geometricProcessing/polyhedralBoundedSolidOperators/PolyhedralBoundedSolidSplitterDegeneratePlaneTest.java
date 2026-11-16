package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
piece strict-valid, non-empty when the plane really cuts the solid, and on
the side of the plane its list names. The input solid must come back
unchanged and must not appear in the output lists.

<p>Traceability: [MANT1988] Ch. 14. Section 14.1 requires the algorithm to
be "general, i.e., it must work correctly for all possible geometric
configurations of solids and planes"; Sections 14.4-14.5 (splitgenerate,
ON-vertex neighborhoods, reclassification rules, Figs. 14.7-14.9) exist
precisely for vertices that lie on the splitting plane. The planes through
vertices reproduce defect S-01 of the Fable 5.2 kernel analysis
(doc/polyhedralBoundedSolidDevelopment/Fable5.2Analysis.md), fixed in
Stage E.</p>
 */
class PolyhedralBoundedSolidSplitterDegeneratePlaneTest
{
    private static final double TOLERANCE = 1.0e-6;

    static Stream<Arguments> cuttingPlanes()
    {
        return Stream.of(
            Arguments.of("generic oblique plane",
                new Vector3Dd(0.3, 0.5, 0.8), new Vector3Dd(0.1, 0, 0.05)),
            Arguments.of("axis plane through the middle",
                new Vector3Dd(0, 0, 1), new Vector3Dd(0, 0, 0.1)),
            Arguments.of("plane containing one cube edge",
                new Vector3Dd(1, -0.5, 0), new Vector3Dd(1, 1, 0)),
            Arguments.of("diagonal plane through two opposite edges",
                new Vector3Dd(1, -1, 0), new Vector3Dd(0, 0, 0)),
            // S-01 (Stage E): planes through vertices.
            Arguments.of("corner cut through three vertices (x+y+z=1)",
                new Vector3Dd(1, 1, 1), new Vector3Dd(1, 1, -1)),
            Arguments.of("generic plane through exactly one vertex",
                new Vector3Dd(1, 0.7, 0.4), new Vector3Dd(1, 1, -1)),
            Arguments.of("corner cut with the normal reversed",
                new Vector3Dd(-1, -1, -1), new Vector3Dd(1, 1, -1)),
            Arguments.of("hexagonal section through six edge midpoints",
                new Vector3Dd(1, 1, 1), new Vector3Dd(0, 0, 0)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cuttingPlanes")
    void given_cuttingPlane_when_split_then_volumeIsConservedOnTheRightSides(
        String name, Vector3Dd normal, Vector3Dd point)
    {
        OrientedBox cube = new OrientedBox(2, 2, 2, translation(0, 0, 0));
        PolyhedralBoundedSolid solid = cube.toSolid();
        InfinitePlane plane = new InfinitePlane(normal.normalized(), point);
        ArrayList<PolyhedralBoundedSolid> above = new ArrayList<>();
        ArrayList<PolyhedralBoundedSolid> below = new ArrayList<>();
        double total = 0.0;

        PolyhedralBoundedSolidModeler.split(solid, plane, above, below);

        assertInputUnchanged(solid, cube, above, below);
        assertThat(above).as("pieces above").isNotEmpty();
        assertThat(below).as("pieces below").isNotEmpty();
        for ( PolyhedralBoundedSolid piece : above ) {
            total += assertValidPieceOnSide(piece, plane, +1);
        }
        for ( PolyhedralBoundedSolid piece : below ) {
            total += assertValidPieceOnSide(piece, plane, -1);
        }
        assertThat(total).isCloseTo(cube.volume(),
            org.assertj.core.data.Offset.offset(TOLERANCE));
    }

    static Stream<Arguments> planesThatDoNotCut()
    {
        return Stream.of(
            Arguments.of("plane missing the cube, cube below",
                new Vector3Dd(0, 0, 1), new Vector3Dd(0, 0, 5), -1),
            Arguments.of("plane missing the cube, cube above",
                new Vector3Dd(0, 0, 1), new Vector3Dd(0, 0, -5), +1),
            Arguments.of("plane touching one vertex from outside",
                new Vector3Dd(1, 1, 1), new Vector3Dd(1, 1, 1), -1),
            Arguments.of("plane containing the top face",
                new Vector3Dd(0, 0, 1), new Vector3Dd(0, 0, 1), -1),
            Arguments.of("plane containing the bottom face",
                new Vector3Dd(0, 0, 1), new Vector3Dd(0, 0, -1), +1),
            Arguments.of("plane touching one edge from outside",
                new Vector3Dd(1, 1, 0), new Vector3Dd(1, 1, 0), -1));
    }

    /**
    A plane that does not cut the solid leaves it whole: a copy goes to the
    list of the side it lies on, an empty solid to the other one. Before
    Stage E the input object itself always went to "above".
    */
    @ParameterizedTest(name = "{0}")
    @MethodSource("planesThatDoNotCut")
    void given_planeThatDoesNotCut_when_split_then_aCopyGoesToItsSide(
        String name, Vector3Dd normal, Vector3Dd point, int side)
    {
        OrientedBox cube = new OrientedBox(2, 2, 2, translation(0, 0, 0));
        PolyhedralBoundedSolid solid = cube.toSolid();
        InfinitePlane plane = new InfinitePlane(normal.normalized(), point);
        ArrayList<PolyhedralBoundedSolid> above = new ArrayList<>();
        ArrayList<PolyhedralBoundedSolid> below = new ArrayList<>();

        PolyhedralBoundedSolidModeler.split(solid, plane, above, below);

        assertInputUnchanged(solid, cube, above, below);
        assertThat(above).hasSize(1);
        assertThat(below).hasSize(1);
        PolyhedralBoundedSolid whole = side > 0 ? above.get(0) : below.get(0);
        PolyhedralBoundedSolid empty = side > 0 ? below.get(0) : above.get(0);
        assertThat(empty.getPolygonsList().size()).isZero();
        assertThat(assertValidPieceOnSide(whole, plane, side))
            .isCloseTo(cube.volume(),
                org.assertj.core.data.Offset.offset(TOLERANCE));
    }

    private static void assertInputUnchanged(PolyhedralBoundedSolid solid,
        OrientedBox cube, ArrayList<PolyhedralBoundedSolid> above,
        ArrayList<PolyhedralBoundedSolid> below)
    {
        assertThat(solid.getPolygonsList().size())
            .as("input faces after split").isEqualTo(6);
        assertThat(PolyhedralBoundedSolidVolumeOracle.solidVolume(solid))
            .as("input volume after split")
            .isCloseTo(cube.volume(),
                org.assertj.core.data.Offset.offset(TOLERANCE));
        assertThat(above).as("above holds the input object")
            .noneMatch(p -> p == solid);
        assertThat(below).as("below holds the input object")
            .noneMatch(p -> p == solid);
    }

    private static double assertValidPieceOnSide(PolyhedralBoundedSolid piece,
        InfinitePlane plane, int side)
    {
        StringBuilder strictReport = new StringBuilder();
        int i;

        assertThat(piece.getPolygonsList().size())
            .as("piece face count").isGreaterThanOrEqualTo(4);
        assertThat(PolyhedralBoundedSolidValidationEngine.validateStrict(
            piece, strictReport))
            .as("strict validation: %s", strictReport).isTrue();
        for ( i = 0; i < piece.getVerticesList().size(); i++ ) {
            assertThat(side * plane.pointDistance(
                piece.getVerticesList().get(i).position))
                .as("vertex side").isGreaterThanOrEqualTo(-TOLERANCE);
        }
        return PolyhedralBoundedSolidVolumeOracle.solidVolume(piece);
    }
}
