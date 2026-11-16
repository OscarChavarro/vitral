package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.BooleanOracleFuzz.Family;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.BooleanOracleFuzz.FamilySummary;

import static org.assertj.core.api.Assertions.assertThat;

/**
Ratchet gate over the seeded box-pair oracle fuzz (Fable5.2Analysis.md
Section 1 and Stage A step 2). Every family must keep at least its recorded
number of correct, strict-valid results. Raise a minimum when a fix lands;
never lower one.

<p>Run with {@code gradle :base:slowTest}. The per-family failures are printed
so a regression names the cases that broke.</p>

<p>Traceability: [MANT1988] Sec. 13.3 (volume as integral property) and
Ch. 15 (set operations).</p>
 */
@Tag("slow")
class BooleanOracleFuzzGateTest
{
    /**
    Ratchet: raise only. Stage A (2026-11-16) measured 48/41/38/38/304/16;
    the B-01 ring fix (Stage C) raised the three rotated families to 48 and
    the coplanar family to 18. Stage D (vertex/face neighbourhood, witness
    containment, face-contact union) raised coplanar to 48 and the grid to
    306. The 14 grid unions touching only along an edge (B-04) are now
    rejected as non-manifold, which counts as correct: grid 320.
    */
    private static final Map<Family, Integer> MINIMUM_OK = new EnumMap<>(
        Family.class);

    static {
        MINIMUM_OK.put(Family.AXIS_ALIGNED_GENERAL_POSITION, 48);
        MINIMUM_OK.put(Family.B_ROTATED_ABOUT_Z, 48);
        MINIMUM_OK.put(Family.B_ROTATED_ABOUT_RANDOM_AXIS, 48);
        MINIMUM_OK.put(Family.RANDOM_AXIS_TRANSLATED_TO_1000, 48);
        MINIMUM_OK.put(Family.INTEGER_GRID, 320);
        MINIMUM_OK.put(Family.COPLANAR_TOP_BOTTOM_ROTATED_45, 48);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Family.class)
    void given_seededBoxPairs_when_setOp_then_correctCountDoesNotRegress(
        Family family)
    {
        FamilySummary summary = BooleanOracleFuzz.runFamily(family);

        System.out.println(family + ": " + summary.correct + " / " +
            summary.total + " correct (" + summary.okByFallback +
            " by fallback) " + summary.byOutcome);
        summary.failures.forEach(f -> System.out.println("  " + f));

        assertThat(summary.correct)
            .as("correct results in family " + family +
                " must not regress below the ratchet")
            .isGreaterThanOrEqualTo(MINIMUM_OK.get(family));
    }
}
