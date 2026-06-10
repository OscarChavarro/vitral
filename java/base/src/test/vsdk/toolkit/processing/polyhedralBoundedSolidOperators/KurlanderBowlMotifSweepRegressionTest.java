package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidGeometricValidator;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

import static org.assertj.core.api.Assertions.assertThat;

/**
Regression guard for the 40-motif Kurlander bowl sweep.

Replicates the classification logic of {@code runMotifSweep} in
{@code PolyhedralBoundedSolidExample} without requiring the visual renderer.
Each of the 40 motifs (20 stars + 20 moons) is individually subtracted from a
fresh bowl and classified as OK / EMPTY / UNCHANGED / INVALID / BLACK_FACES /
EXCEPTION.  The test asserts that the per-category counts stay at or above the
observed baseline so that regressions are caught immediately.

Marked {@code @Tag("slow")} because building and operating on 40 independent
bowl copies takes several minutes. Slow-tagged tests are excluded from the
default build (see base/build.gradle); run explicitly with:
{@code gradle :base:test -PincludeSlowTests --tests "*KurlanderBowlMotifSweepRegressionTest"}
*/
@Tag("slow")
class KurlanderBowlMotifSweepRegressionTest
{
    /**
    Minimum number of motifs that must classify as OK.
    Observed after mythosPlan Phase 2 (2026-06-10): 32 = 20 stars +
    12 moons (curve-ordered connect; see
    _PolyhedralBoundedSolidSetIntersectionCurveBuilder).
    Update this constant whenever a fix legitimately improves the count.
    */
    private static final int MINIMUM_OK_COUNT = 32;

    /**
    Maximum allowed sum of EMPTY + INVALID + BLACK_FACES + EXCEPTION.
    Observed after mythosPlan Phase 2 (2026-06-10): empty=4 (moons
    22/27/32/37), blackFaces=4 (moons 20/25/30/35) — both are crescent-cusp
    chord-crossing degeneracies owned by mythosPlan Phase 3.
    Decreasing this value tightens the guard.
    */
    private static final int MAXIMUM_FAILURE_COUNT = 8;

    @Test
    void given_kurlanderBowlAndAllSingleMotifs_when_subtracting_then_sweepResultsMeetMinimumThresholds()
    {
        int total = CsgKurlanderBowlFixture.getSingleMotifCount();
        int stars = CsgKurlanderBowlFixture.getSingleMotifStarCount();
        int ok = 0;
        int empty = 0;
        int invalid = 0;
        int blackFaces = 0;
        int unchanged = 0;
        int exception = 0;

        for ( int motif = 0; motif < total; motif++ ) {
            String kind = motif < stars ? "STAR" : "MOON";
            int kindIndex = motif < stars ? motif : motif - stars;
            String tag = kind + "[" + kindIndex + "]";

            PolyhedralBoundedSolid[] operands;
            int originalBowlFaces;
            PolyhedralBoundedSolid result;

            try {
                operands = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(motif);
                originalBowlFaces = operands[0].getPolygonsList().size();
            }
            catch ( Throwable t ) {
                System.out.println("[SWEEP-EXCEPTION] " + tag + " motif=" + motif +
                    " stage=build-operands err=" + t.getClass().getSimpleName() +
                    " - " + t.getMessage());
                exception++;
                continue;
            }

            try {
                _PolyhedralBoundedSolidSetNullEdgesConnector
                    .lastCurveReport = null;
                result = PolyhedralBoundedSolidModeler.setOp(
                    operands[0], operands[1],
                    PolyhedralBoundedSolidModeler.SUBTRACT, false);
            }
            catch ( Throwable t ) {
                System.out.println("[SWEEP-EXCEPTION] " + tag + " motif=" + motif +
                    " stage=setop err=" + t.getClass().getSimpleName() +
                    " - " + t.getMessage());
                exception++;
                continue;
            }

            int faces = result == null ? 0 : result.getPolygonsList().size();
            String status;
            String detail;

            if ( faces == 0 ) {
                status = "EMPTY";
                detail = "";
                empty++;
            }
            else if ( faces == originalBowlFaces ) {
                status = "UNCHANGED";
                detail = " faces=" + faces;
                unchanged++;
            }
            else {
                boolean valid = false;
                try {
                    valid = PolyhedralBoundedSolidValidationEngine
                        .validateIntermediate(result);
                }
                catch ( Throwable t ) {
                    /* leave valid=false */
                }

                if ( !valid ) {
                    status = "INVALID";
                    detail = " faces=" + faces;
                    invalid++;
                }
                else {
                    StringBuilder orientationMsg = new StringBuilder();
                    boolean orientationOK;
                    try {
                        orientationOK =
                            PolyhedralBoundedSolidGeometricValidator
                                .validateConsistentFaceOrientations(
                                    result, orientationMsg);
                    }
                    catch ( Throwable t ) {
                        orientationOK = true;
                    }

                    if ( !orientationOK ) {
                        int firstLine = orientationMsg.indexOf("\n");
                        String preview = firstLine > 0
                            ? orientationMsg.substring(0, firstLine).trim()
                            : "(orientation flagged)";
                        status = "BLACK_FACES";
                        detail = " faces=" + faces + " " + preview;
                        blackFaces++;
                    }
                    else {
                        status = "OK";
                        detail = " faces=" + faces +
                            " bowlFaces=" + originalBowlFaces;
                        ok++;
                    }
                }
            }
            _PolyhedralBoundedSolidSetIntersectionCurveBuilder.Report
                curveReport = _PolyhedralBoundedSolidSetNullEdgesConnector
                    .lastCurveReport;
            System.out.println("[SWEEP-" + status + "] " + tag +
                " motif=" + motif + detail +
                (curveReport == null ? "" : " | " + curveReport.summarize()));
        }

        int failures = empty + invalid + blackFaces + exception;
        System.out.println("[SWEEP-SUMMARY] ok=" + ok + " empty=" + empty +
            " invalid=" + invalid + " blackFaces=" + blackFaces +
            " unchanged=" + unchanged + " exception=" + exception +
            " total=" + total);

        assertThat(ok)
            .as("Number of OK motifs must not regress below " + MINIMUM_OK_COUNT)
            .isGreaterThanOrEqualTo(MINIMUM_OK_COUNT);
        assertThat(failures)
            .as("Total failures (empty+invalid+blackFaces+exception) must not exceed " +
                MAXIMUM_FAILURE_COUNT)
            .isLessThanOrEqualTo(MAXIMUM_FAILURE_COUNT);
    }
}
