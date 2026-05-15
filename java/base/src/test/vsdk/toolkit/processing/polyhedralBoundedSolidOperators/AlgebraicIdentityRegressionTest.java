package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTestFixtures;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

import static org.assertj.core.api.Assertions.assertThat;

/**
Regression guard for the algebraic identities of set-operations
([MANT1988] Ch. 15.1):
<ul>
<li>Idempotence: {@code A∪A = A}, {@code A∩A = A}, {@code A−A = ∅}.</li>
<li>Determinism of swapped-operand difference: running {@code A−B}
    twice on equivalent inputs returns equivalent topologies.</li>
</ul>

<p>This class replaces the eliminated
{@code PolyhedralBoundedSolidSetOperatorAlgebraicPropertiesTest}, whose
assertions were inverted drift detectors (§7.3.1.E in the stage-2 plan).
The corpus is split into:</p>

<ul>
<li><b>cleanIdempotenceFixtures</b> — the 3 fixtures × 2 indices where
    §7.3.1.A identity-preflight makes {@code A op A_clone} satisfy the
    laws.</li>
<li><b>cleanDifferenceSwapFixtures</b> — the fixtures where
    {@code A−B} is deterministic.</li>
</ul>

<p>The 5 known-drift fixtures (3 absorption, 2 diff-swapped on
MANT1988_15_2_LIMIT / MANT1988_6_13) are NOT included here; they
remain as TODO documented in plan §7.3 §7.3.1.D.</p>
 */
class AlgebraicIdentityRegressionTest
{
    @ParameterizedTest(name = "idempotence: {0} idx={1}")
    @MethodSource("cleanIdempotenceFixtures")
    void given_cleanFixture_when_idempotentOperations_then_identitiesHold(
        String corpusKey, int solidIndex)
    {
        PolyhedralBoundedSolid baseline = createPair(corpusKey)[solidIndex];
        double[] baselineMinMax = baseline.getMinMax();

        PolyhedralBoundedSolid unionLeft = createPair(corpusKey)[solidIndex];
        PolyhedralBoundedSolid unionRight = createPair(corpusKey)[solidIndex];
        PolyhedralBoundedSolid interLeft = createPair(corpusKey)[solidIndex];
        PolyhedralBoundedSolid interRight = createPair(corpusKey)[solidIndex];
        PolyhedralBoundedSolid diffLeft = createPair(corpusKey)[solidIndex];
        PolyhedralBoundedSolid diffRight = createPair(corpusKey)[solidIndex];

        PolyhedralBoundedSolid union = PolyhedralBoundedSolidModeler.setOp(
            unionLeft, unionRight,
            PolyhedralBoundedSolidModeler.UNION, false);
        PolyhedralBoundedSolid inter = PolyhedralBoundedSolidModeler.setOp(
            interLeft, interRight,
            PolyhedralBoundedSolidModeler.INTERSECTION, false);
        PolyhedralBoundedSolid diff = PolyhedralBoundedSolidModeler.setOp(
            diffLeft, diffRight,
            PolyhedralBoundedSolidModeler.SUBTRACT, false);

        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(union)).isTrue();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(inter)).isTrue();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(diff)).isTrue();

        assertThat(boundingBoxMatches(union, baselineMinMax))
            .as("A∪A bounding box must match A")
            .isTrue();
        assertThat(boundingBoxMatches(inter, baselineMinMax))
            .as("A∩A bounding box must match A")
            .isTrue();
        assertThat(isEmpty(diff))
            .as("A−A must be empty")
            .isTrue();
    }

    @ParameterizedTest(name = "diff-swap determinism: {0}")
    @MethodSource("cleanDifferenceSwapFixtures")
    void given_cleanFixturePair_when_differenceComputedTwice_then_determinism(
        String corpusKey)
    {
        PolyhedralBoundedSolid[] pairA = createPair(corpusKey);
        PolyhedralBoundedSolid[] pairB = createPair(corpusKey);
        PolyhedralBoundedSolid[] pairC = createPair(corpusKey);
        PolyhedralBoundedSolid[] pairD = createPair(corpusKey);

        PolyhedralBoundedSolid abFirst = PolyhedralBoundedSolidModeler.setOp(
            pairA[0], pairA[1],
            PolyhedralBoundedSolidModeler.SUBTRACT, false);
        PolyhedralBoundedSolid abSecond = PolyhedralBoundedSolidModeler.setOp(
            pairB[0], pairB[1],
            PolyhedralBoundedSolidModeler.SUBTRACT, false);
        PolyhedralBoundedSolid baFirst = PolyhedralBoundedSolidModeler.setOp(
            pairC[1], pairC[0],
            PolyhedralBoundedSolidModeler.SUBTRACT, false);
        PolyhedralBoundedSolid baSecond = PolyhedralBoundedSolidModeler.setOp(
            pairD[1], pairD[0],
            PolyhedralBoundedSolidModeler.SUBTRACT, false);

        assertThat(abFirst.getPolygonsList().size())
            .isEqualTo(abSecond.getPolygonsList().size());
        assertThat(abFirst.getEdgesList().size())
            .isEqualTo(abSecond.getEdgesList().size());
        assertThat(abFirst.getVerticesList().size())
            .isEqualTo(abSecond.getVerticesList().size());
        assertThat(baFirst.getPolygonsList().size())
            .isEqualTo(baSecond.getPolygonsList().size());
        assertThat(baFirst.getEdgesList().size())
            .isEqualTo(baSecond.getEdgesList().size());
        assertThat(baFirst.getVerticesList().size())
            .isEqualTo(baSecond.getVerticesList().size());
    }

    private static Stream<Arguments> cleanIdempotenceFixtures()
    {
        // After §7.3.1.A (identity preflight in setOp) all 6 idempotence
        // cases are clean: A∪A = A∩A = A, A−A = ∅.
        return Stream.of(
            Arguments.of("MANT1986_2", 0),
            Arguments.of("MANT1986_2", 1),
            Arguments.of("MANT1988_15_2_LIMIT", 0),
            Arguments.of("MANT1988_15_2_LIMIT", 1),
            Arguments.of("MANT1988_6_13", 0),
            Arguments.of("MANT1988_6_13", 1));
    }

    private static Stream<Arguments> cleanDifferenceSwapFixtures()
    {
        // Post-§7.3.1.A re-mapping (§7.3.1.B): all 3 fixtures pass the
        // diff-swap determinism (same f/e/v across runs AND validate).
        return Stream.of(
            Arguments.of("MANT1986_2"),
            Arguments.of("MANT1988_15_2_LIMIT"),
            Arguments.of("MANT1988_6_13"));
    }

    @ParameterizedTest(name = "absorption: {0}")
    @MethodSource("cleanAbsorptionFixtures")
    void given_cleanFixturePair_when_absorptionIdentities_then_identitiesHold(
        String corpusKey)
    {
        // A ∪ (A ∩ B) = A   and   A ∩ (A ∪ B) = A
        PolyhedralBoundedSolid baselineLeft = createPair(corpusKey)[0];
        double[] baselineMinMax = baselineLeft.getMinMax();

        PolyhedralBoundedSolid[] pairForIntersection = createPair(corpusKey);
        PolyhedralBoundedSolid[] pairForUnion = createPair(corpusKey);
        PolyhedralBoundedSolid[] pairForFinalUnion = createPair(corpusKey);
        PolyhedralBoundedSolid[] pairForFinalIntersection = createPair(corpusKey);

        PolyhedralBoundedSolid aIntersectionB =
            PolyhedralBoundedSolidModeler.setOp(
                pairForIntersection[0], pairForIntersection[1],
                PolyhedralBoundedSolidModeler.INTERSECTION, false);
        PolyhedralBoundedSolid firstAbsorption =
            PolyhedralBoundedSolidModeler.setOp(
                pairForFinalUnion[0], aIntersectionB,
                PolyhedralBoundedSolidModeler.UNION, false);

        PolyhedralBoundedSolid aUnionB = PolyhedralBoundedSolidModeler.setOp(
            pairForUnion[0], pairForUnion[1],
            PolyhedralBoundedSolidModeler.UNION, false);
        PolyhedralBoundedSolid secondAbsorption =
            PolyhedralBoundedSolidModeler.setOp(
                pairForFinalIntersection[0], aUnionB,
                PolyhedralBoundedSolidModeler.INTERSECTION, false);

        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(firstAbsorption)).isTrue();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(secondAbsorption)).isTrue();
        assertThat(boundingBoxMatches(firstAbsorption, baselineMinMax))
            .as("A ∪ (A ∩ B) bbox must match A")
            .isTrue();
        assertThat(boundingBoxMatches(secondAbsorption, baselineMinMax))
            .as("A ∩ (A ∪ B) bbox must match A")
            .isTrue();
    }

    private static Stream<Arguments> cleanAbsorptionFixtures()
    {
        // Post-§7.3.1.A: only MANT1986_2 is clean. The two remaining
        // drift fixtures (MANT1988_15_2_LIMIT, MANT1988_6_13) are the
        // §7.3.1.D target.
        return Stream.of(
            Arguments.of("MANT1986_2"));
    }

    private static PolyhedralBoundedSolid[] createPair(String corpusKey)
    {
        if ( "MANT1986_2".equals(corpusKey) ) {
            return PolyhedralBoundedSolidTestFixtures.createMant1986_2Pair();
        }
        if ( "MANT1988_15_2_LIMIT".equals(corpusKey) ) {
            return PolyhedralBoundedSolidTestFixtures
                .createMant1988_15_2Pair(0);
        }
        if ( "MANT1988_6_13".equals(corpusKey) ) {
            return PolyhedralBoundedSolidTestFixtures.createMant1988_6_13Pair();
        }
        throw new IllegalArgumentException("Unsupported corpus: " + corpusKey);
    }

    private static boolean boundingBoxMatches(
        PolyhedralBoundedSolid solid, double[] baselineMinMax)
    {
        double[] actualMinMax = solid.getMinMax();
        for ( int i = 0; i < 6; i++ ) {
            if ( Math.abs(actualMinMax[i] - baselineMinMax[i]) > 1.0e-6 ) {
                return false;
            }
        }
        return true;
    }

    private static boolean isEmpty(PolyhedralBoundedSolid s)
    {
        return s != null
            && s.getPolygonsList().size() == 0
            && s.getEdgesList().size() == 0
            && s.getVerticesList().size() == 0;
    }
}
