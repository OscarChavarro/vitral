package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.BooleanVolumeIdentityFuzz.Family;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.BooleanVolumeIdentityFuzz.Summary;

import static org.assertj.core.api.Assertions.assertThat;

/**
Gate over {@link BooleanVolumeIdentityFuzz} (plan-stage11 Phase B): prisms
of N sides and star prisms in general position, checked with the volume
identities of the four operations.

<p>Two conditions per family:</p>
<ul>
<li>No silent error, ever: a returned set of results that breaks an
    identity, or an intersection outside [0, min(|A|, |B|)], fails the
    test whatever the ratchet says.</li>
<li>Ratchet on the count of correct cases: raise it when a fix lands,
    never lower it. The cases below the total are typed rejections of the
    pipeline on non-convex faces (plan-stage11 Phase F).</li>
</ul>

<p>Traceability: [MANT1988] Sec. 13.3 (volume) and Ch. 15.</p>
 */
class BooleanVolumeIdentityFuzzGateTest
{
    /** First measurement (2026-11-16): convex 16/16, star and convex 13/16, stars 11/16. After plan-stage11 Phase F: 16, 15, 16. */
    private static final Map<Family, Integer> MINIMUM_OK =
        new EnumMap<>(Family.class);

    static {
        MINIMUM_OK.put(Family.CONVEX_PRISMS, 16);
        MINIMUM_OK.put(Family.STAR_AND_CONVEX, 16);
        MINIMUM_OK.put(Family.STAR_PRISMS, 16);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Family.class)
    void given_seededPrisms_when_fourOperations_then_volumeIdentitiesHold(
        Family family)
    {
        Summary summary = BooleanVolumeIdentityFuzz.runFamily(family);

        System.out.println(family + ": " + summary.correct + " / " +
            summary.total + " " + summary.byOutcome);
        summary.failures.forEach(f -> System.out.println("  " + f));

        assertThat(summary.byOutcome)
            .as("results that break a volume identity are silent errors")
            .doesNotContainKeys("IDENTITY", "RANGE", "OPERAND");
        assertThat(summary.correct)
            .as("correct cases in family " + family)
            .isGreaterThanOrEqualTo(MINIMUM_OK.get(family));
    }
}
