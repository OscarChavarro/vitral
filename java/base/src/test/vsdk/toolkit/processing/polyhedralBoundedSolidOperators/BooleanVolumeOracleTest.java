package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidVolumeOracle.OrientedBox;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidVolumeOracle.rotation;
import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidVolumeOracle.translation;

/**
Boolean set operations checked against an independent volume oracle, on
box/box configurations that are NOT part of the fixture corpus.

<p>Every row runs one operation (A u B, A n B, A \ B or B \ A) through the
public default entry point (strict validation on) and asserts both the
strict B-Rep postcondition and the exact expected volume. Rows that pass
today form a regression guard; rows that fail are kept in a disabled
work-queue test, one row per (configuration, operation).</p>

<p>Traceability: [MANT1988] Ch. 15 (set operations), Sec. 15.3 (8-way
boundary classification and "on" reclassification, Eq. 15.3), Sec. 15.6.1
(vertex/face classification: null edges inserted as an internal loop of the
pierced face, Fig. 15.6), Sec. 15.8 (result generation). Cases marked
"hidden defect" were found by the Fable 5.2 kernel analysis
(doc/polyhedralBoundedSolidDevelopment/Fable5.2Analysis.md, defects B-01
to B-05) and stay disabled until the production fix lands.</p>
 */
class BooleanVolumeOracleTest
{
    private static final int UNION = PolyhedralBoundedSolidModeler.UNION;
    private static final int INTERSECTION =
        PolyhedralBoundedSolidModeler.INTERSECTION;
    private static final int A_MINUS_B = PolyhedralBoundedSolidModeler.SUBTRACT;
    private static final int B_MINUS_A = 4;

    // -- Case catalogue ----------------------------------------------------

    /** Generic position: the section polygon only crosses edges. */
    private static OrientedBox[] cornerInCorner()
    {
        return new OrientedBox[] {
            new OrientedBox(1.4, 1.2, 1.6, translation(0, 0, 0)),
            new OrientedBox(1.1, 1.5, 1.3,
                translation(0.413, -0.287, 0.351)) };
    }

    /**
    B-01: the four peg edges pierce the interior of the block's top face, so
    the section polygon is a ring strictly inside one face ([MANT1988] Fig.
    15.6).
    */
    private static OrientedBox[] pegEndingInsideBlock()
    {
        return new OrientedBox[] {
            new OrientedBox(2, 2, 2, translation(0, 0, 0)),
            new OrientedBox(0.5, 0.5, 1.5, translation(0.1, 0.2, 1.0)) };
    }

    /** B-01 with two interior rings (entry and exit faces), peg rotated. */
    private static OrientedBox[] pegThroughBlock()
    {
        return new OrientedBox[] {
            new OrientedBox(2, 2, 2, translation(0, 0, 0)),
            new OrientedBox(0.5, 0.5, 3.0,
                translation(0.1, 0.2, 0.05).multiply(
                    rotation(0.3, 0, 0, 1))) };
    }

    /**
    B-01 with a triangular ring: a cube corner pokes through the interior of
    a slab face (three edges pierce one face). This is the generic case for
    rotated boxes: about 46% of a random sample of generically rotated box
    pairs fails because of it.
    */
    private static OrientedBox[] cubeCornerThroughSlabFace()
    {
        Matrix4x4d diagonalUp = rotation(0.37, 0, 0, 1).multiply(
            rotation(Math.acos(1.0 / Math.sqrt(3.0)), 1, -1, 0));
        return new OrientedBox[] {
            new OrientedBox(4, 4, 1, translation(0.05, 0.03, 1.2)),
            new OrientedBox(1, 1, 1, diagonalUp) };
    }

    /**
    B-05 control: top and bottom faces coplanar, both operands axis-aligned.
    Three of the four operations are answered by the axis-aligned
    cell-decomposition fallback, not by the Ch. 15 pipeline.
    */
    private static OrientedBox[] coplanarAxisAligned()
    {
        return new OrientedBox[] {
            new OrientedBox(2, 2, 1, translation(0, 0, 0)),
            new OrientedBox(1, 1, 1, translation(1.0, 0.1, 0)) };
    }

    /**
    B-05 (rigid-motion invariance): coplanarAxisAligned() with one rigid
    rotation applied to BOTH operands. A boolean must be invariant under a
    common rigid motion; here the axis-aligned fallback no longer applies
    and the general pipeline is exposed on coplanar overlapping faces
    ([MANT1988] Figs. 15.1 and 15.5).
    */
    private static OrientedBox[] coplanarRigidlyRotated()
    {
        Matrix4x4d rigid = rotation(0.5, 0.3, 1.0, 0.2);
        return new OrientedBox[] {
            new OrientedBox(2, 2, 1, rigid),
            new OrientedBox(1, 1, 1,
                rigid.multiply(translation(1.0, 0.1, 0))) };
    }

    /**
    B-02: coplanar top and bottom faces, B rotated 30 degrees about z, so
    edges of the coplanar faces cross each other properly. Today A\B
    silently returns A unchanged (wrong volume, strict-valid) and A n B dies
    with a fatal IndexOutOfBounds in the finisher.
    */
    private static OrientedBox[] coplanarWithCrossingEdges()
    {
        return new OrientedBox[] {
            new OrientedBox(2, 2, 1, translation(0, 0, 0)),
            new OrientedBox(1, 1, 1, translation(1.0, 0.1, 0).multiply(
                rotation(Math.PI / 6.0, 0, 0, 1))) };
    }

    /**
    B-02 (containment with tangent faces): B is inside A except that its top
    and bottom faces lie on A's. Classify finds the 8 vertex/face contacts
    but emits no null edge; the "no intersection after classify" branch then
    treats the operands as disjoint, so A u B returns both overlapping
    shells.
    */
    private static OrientedBox[] innerBoxWithFacesOnOuterFaces()
    {
        return new OrientedBox[] {
            new OrientedBox(2, 2, 1, translation(0, 0, 0)),
            new OrientedBox(0.5, 0.5, 1, translation(0.1, 0.1, 0).multiply(
                rotation(Math.PI / 6.0, 0, 0, 1))) };
    }

    /**
    B-03: B is a cube rotated 45 degrees whose vertical diagonal plane
    coincides with A's face x = 1, so two edges of B lie IN that face and
    half of B sticks out. A preflight classifies B as contained in A:
    A u B returns A, A n B returns B, B \ A returns the empty solid. Those
    three pass strict validation, so the wrong answer is silent.
    */
    private static OrientedBox[] diamondStraddlingFace()
    {
        return new OrientedBox[] {
            new OrientedBox(2, 2, 2, translation(0, 0, 0)),
            new OrientedBox(1, 1, 1, translation(1.0, 0, 0.2).multiply(
                rotation(Math.PI / 4.0, 0, 0, 1))) };
    }

    /**
    B-04: two unit cubes sharing one full face. The regularized union is a
    single 1 x 1 x 2 box; the touching-only preflight returns the two
    unglued shells instead, and the default strict postcondition rejects
    them ("Faces intersect improperly"), so the default call throws.
    */
    private static OrientedBox[] cubesSharingAFace()
    {
        return new OrientedBox[] {
            new OrientedBox(1, 1, 1, translation(0, 0, 0)),
            new OrientedBox(1, 1, 1, translation(1, 0, 0)) };
    }

    /**
    B-04: two boxes touching along an edge segment only. [MANT1988] Sec.
    15.2 criterion 3' would admit the two-shell pseudomanifold union; the
    kernel rejects it by design as non-manifold (decision 2026-11-16). The
    other three operations are manifold and checked against the oracle.
    */
    private static OrientedBox[] boxesTouchingAlongAnEdge()
    {
        return new OrientedBox[] {
            new OrientedBox(1, 1, 2, translation(0.5, 0.5, 1.0)),
            new OrientedBox(2, 2, 2, translation(-1.0, 2.0, 2.0)) };
    }

    // -- Operations that match the oracle today (regression guard) ---------

    static Stream<Arguments> operationsThatMatchTheOracleToday()
    {
        return Stream.of(
            operations("corner-in-corner", cornerInCorner(),
                UNION, INTERSECTION, A_MINUS_B, B_MINUS_A),
            // B-01 fixed 2026-11-16: all four operations.
            operations("peg-ending-inside-block", pegEndingInsideBlock(),
                UNION, INTERSECTION, A_MINUS_B, B_MINUS_A),
            operations("peg-through-block", pegThroughBlock(),
                UNION, INTERSECTION, A_MINUS_B, B_MINUS_A),
            operations("cube-corner-through-slab-face",
                cubeCornerThroughSlabFace(),
                UNION, INTERSECTION, A_MINUS_B, B_MINUS_A),
            operations("coplanar-axis-aligned", coplanarAxisAligned(),
                UNION, INTERSECTION, A_MINUS_B, B_MINUS_A),
            // Stage D (2026-11-16): vertex/face neighbourhood fix, witness
            // containment and face-contact union.
            operations("coplanar-rigidly-rotated", coplanarRigidlyRotated(),
                UNION, INTERSECTION, A_MINUS_B, B_MINUS_A),
            operations("coplanar-with-crossing-edges",
                coplanarWithCrossingEdges(),
                UNION, INTERSECTION, A_MINUS_B, B_MINUS_A),
            operations("inner-box-with-faces-on-outer-faces",
                innerBoxWithFacesOnOuterFaces(),
                UNION, INTERSECTION, A_MINUS_B, B_MINUS_A),
            operations("diamond-straddling-face", diamondStraddlingFace(),
                UNION, INTERSECTION, A_MINUS_B, B_MINUS_A),
            operations("cubes-sharing-a-face", cubesSharingAFace(),
                UNION, INTERSECTION, A_MINUS_B, B_MINUS_A),
            operations("boxes-touching-along-an-edge",
                boxesTouchingAlongAnEdge(),
                INTERSECTION, A_MINUS_B, B_MINUS_A)
        ).flatMap(s -> s);
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("operationsThatMatchTheOracleToday")
    void given_boxPair_when_setOp_then_resultIsStrictValidAndVolumeMatchesOracle(
        String name, String opName, OrientedBox a, OrientedBox b, int op)
    {
        assertBooleanMatchesOracle(a, b, op);
    }

    // -- Non-manifold results are rejected (B-04) -------------------------

    /**
    The union of two solids that share only an edge (or a vertex) is two
    shells meeting at a non-manifold boundary. setOp must refuse it with the
    typed exception, with or without the strict postcondition, and record
    the rejection path.
    */
    @ParameterizedTest(name = "strict={0}")
    @ValueSource(booleans = {true, false})
    void given_boxesTouchingAlongAnEdge_when_union_then_rejectedAsNonManifold(
        boolean doStrictValidation)
    {
        OrientedBox[] pair = boxesTouchingAlongAnEdge();

        assertThatThrownBy(() ->
            PolyhedralBoundedSolidModeler.setOp(pair[0].toSolid(),
                pair[1].toSolid(), UNION, false, true, doStrictValidation))
            .isInstanceOf(NonManifoldBooleanResultException.class);
        assertThat(PolyhedralBoundedSolidModeler.getLastSetOpResultPath())
            .isEqualTo("rejected-non-manifold-union");
    }

    /**
    Contract C-01 (fixed in Stage F): setOp no longer consumes its operands.
    The book's algorithm does ([MANT1988] Sec. 15.8: "the parts of A and B
    not required in the final result will remain in the original solids");
    setOp now runs on structural copies, and the consuming form is
    setOpDestructive.
    */
    @Test
    void given_operands_when_setOp_then_operandsAreStillTheSameValidSolids()
    {
        OrientedBox[] pair = cornerInCorner();
        PolyhedralBoundedSolid a = pair[0].toSolid();
        PolyhedralBoundedSolid b = pair[1].toSolid();

        PolyhedralBoundedSolidModeler.setOp(a, b, UNION);

        assertThat(PolyhedralBoundedSolidVolumeOracle.solidVolume(a))
            .isCloseTo(pair[0].volume(),
                org.assertj.core.data.Offset.offset(1e-9));
        assertThat(PolyhedralBoundedSolidVolumeOracle.solidVolume(b))
            .isCloseTo(pair[1].volume(),
                org.assertj.core.data.Offset.offset(1e-9));
        assertThat(a.getPolygonsList().size()).isEqualTo(6);
        assertThat(b.getPolygonsList().size()).isEqualTo(6);
    }

    // ----------------------------------------------------------------------

    private static Stream<Arguments> operations(
        String name, OrientedBox[] pair, int... ops)
    {
        Stream.Builder<Arguments> rows = Stream.builder();

        for ( int op : ops ) {
            String opName = op == UNION ? "UNION"
                : op == INTERSECTION ? "INTERSECTION"
                : op == A_MINUS_B ? "A-B" : "B-A";
            rows.add(Arguments.of(name, opName, pair[0], pair[1], op));
        }
        return rows.build();
    }

    private static void assertBooleanMatchesOracle(
        OrientedBox a, OrientedBox b, int op)
    {
        OrientedBox first = (op == B_MINUS_A) ? b : a;
        OrientedBox second = (op == B_MINUS_A) ? a : b;
        int kernelOp = (op == B_MINUS_A) ? A_MINUS_B : op;
        double expected = PolyhedralBoundedSolidVolumeOracle.expectedVolume(
            first, second, kernelOp);
        double tolerance = 1.0e-6 * Math.max(1.0, a.volume() + b.volume());

        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            first.toSolid(), second.toSolid(), kernelOp);

        StringBuilder strictReport = new StringBuilder();
        assertThat(PolyhedralBoundedSolidValidationEngine.validateStrict(
            result, strictReport))
            .as("strict validation: %s", strictReport)
            .isTrue();
        assertThat(PolyhedralBoundedSolidVolumeOracle.solidVolume(result))
            .as("result volume")
            .isCloseTo(expected,
                org.assertj.core.data.Offset.offset(tolerance));
    }
}
