import { describe, expect, it } from "vitest";

import { StringBuilder } from "java/lang/StringBuilder.js";
import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidGeometricValidator } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { CsgKurlanderBowlFixture } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/CsgKurlanderBowlFixture.js";
import { _PolyhedralBoundedSolidSetNullEdgesConnector } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetNullEdgesConnector.js";
import { yieldToEventLoop } from "./_HarnessEventLoopYield.js";

/**
Harness accommodation: JUnit has no per-test time budget, and the Java version
even documents that this sweep takes several minutes.
*/
const SWEEP_TIMEOUT_MS = 3_600_000;

/**
Regression guard for the 40-motif Kurlander bowl sweep.

Replicates the classification logic of `runMotifSweep` in
`PolyhedralBoundedSolidExample` without requiring the visual renderer.
Each of the 40 motifs (20 stars + 20 moons) is individually subtracted from a
fresh bowl and classified as OK / EMPTY / UNCHANGED / INVALID / BLACK_FACES /
EXCEPTION.  The test asserts that the per-category counts stay at or above the
observed baseline so that regressions are caught immediately.

Building and operating on 40 independent bowl copies takes several minutes.
Run explicitly with:
`npx vitest run --root base <this file>`
*/
describe("KurlanderBowlMotifSweepRegressionTest", () => {
    /**
    Minimum number of motifs that must classify as OK.
    Observed after mythosPlan Phase 3 (2026-06-11): 40/40 — all stars and
    all moons (curve-ordered connect + curve-neighbor ring rescue; see
    _PolyhedralBoundedSolidSetIntersectionCurveBuilder and
    _PolyhedralBoundedSolidSetNullEdgesConnector.rescueRingFaceNearMiss).
    */
    const MINIMUM_OK_COUNT = 40;

    /**
    Maximum allowed sum of EMPTY + INVALID + BLACK_FACES + EXCEPTION.
    Observed after mythosPlan Phase 3 (2026-06-11): 0.
    */
    const MAXIMUM_FAILURE_COUNT = 0;

    it(
        "given_kurlanderBowlAndAllSingleMotifs_when_subtracting_then_sweepResultsMeetMinimumThresholds",
        async () => {
            const total = CsgKurlanderBowlFixture.getSingleMotifCount();
            const stars = CsgKurlanderBowlFixture.getSingleMotifStarCount();
            let ok = 0;
            let empty = 0;
            let invalid = 0;
            let blackFaces = 0;
            let unchanged = 0;
            let exception = 0;

            for (let motif = 0; motif < total; motif++) {
                await yieldToEventLoop();
                const kind = motif < stars ? "STAR" : "MOON";
                const kindIndex = motif < stars ? motif : motif - stars;
                const tag = kind + "[" + kindIndex + "]";

                let operands: PolyhedralBoundedSolid[];
                let originalBowlFaces: number;
                let result: PolyhedralBoundedSolid | null;

                try {
                    operands = CsgKurlanderBowlFixture.createBowlAndFirstStarOperands(motif);
                    originalBowlFaces = operands[0]!.getPolygonsList().size();
                } catch (t) {
                    console.log(
                        "[SWEEP-EXCEPTION] " +
                            tag +
                            " motif=" +
                            motif +
                            " stage=build-operands err=" +
                            (t as Error).constructor.name +
                            " - " +
                            (t as Error).message,
                    );
                    exception++;
                    continue;
                }

                try {
                    _PolyhedralBoundedSolidSetNullEdgesConnector.lastCurveReport = null;
                    result = PolyhedralBoundedSolidModeler.setOp(
                        operands[0]!,
                        operands[1]!,
                        PolyhedralBoundedSolidModeler.SUBTRACT,
                        false,
                    );
                } catch (t) {
                    console.log(
                        "[SWEEP-EXCEPTION] " +
                            tag +
                            " motif=" +
                            motif +
                            " stage=setop err=" +
                            (t as Error).constructor.name +
                            " - " +
                            (t as Error).message,
                    );
                    exception++;
                    continue;
                }

                const faces = result === null ? 0 : result.getPolygonsList().size();
                let status: string;
                let detail: string;

                if (faces === 0) {
                    status = "EMPTY";
                    detail = "";
                    empty++;
                } else if (faces === originalBowlFaces) {
                    status = "UNCHANGED";
                    detail = " faces=" + faces;
                    unchanged++;
                } else {
                    let valid = false;
                    try {
                        valid = PolyhedralBoundedSolidValidationEngine.validateIntermediate(result!);
                    } catch {
                        /* leave valid=false */
                    }

                    if (!valid) {
                        status = "INVALID";
                        detail = " faces=" + faces;
                        invalid++;
                    } else {
                        const orientationMsg = new StringBuilder();
                        let orientationOK: boolean;
                        try {
                            orientationOK = PolyhedralBoundedSolidGeometricValidator.validateConsistentFaceOrientations(
                                result!,
                                orientationMsg,
                            );
                        } catch {
                            orientationOK = true;
                        }

                        if (!orientationOK) {
                            const firstLine = orientationMsg.toString().indexOf("\n");
                            const preview =
                                firstLine > 0
                                    ? orientationMsg.toString().substring(0, firstLine).trim()
                                    : "(orientation flagged)";
                            status = "BLACK_FACES";
                            detail = " faces=" + faces + " " + preview;
                            blackFaces++;
                        } else {
                            status = "OK";
                            detail = " faces=" + faces + " bowlFaces=" + originalBowlFaces;
                            ok++;
                        }
                    }
                }
                const curveReport = _PolyhedralBoundedSolidSetNullEdgesConnector.lastCurveReport;
                console.log(
                    "[SWEEP-" +
                        status +
                        "] " +
                        tag +
                        " motif=" +
                        motif +
                        detail +
                        (curveReport === null ? "" : " | " + curveReport.summarize()),
                );
            }

            const failures = empty + invalid + blackFaces + exception;
            console.log(
                "[SWEEP-SUMMARY] ok=" +
                    ok +
                    " empty=" +
                    empty +
                    " invalid=" +
                    invalid +
                    " blackFaces=" +
                    blackFaces +
                    " unchanged=" +
                    unchanged +
                    " exception=" +
                    exception +
                    " total=" +
                    total,
            );

            expect(ok, "Number of OK motifs must not regress below " + MINIMUM_OK_COUNT).toBeGreaterThanOrEqual(
                MINIMUM_OK_COUNT,
            );
            expect(
                failures,
                "Total failures (empty+invalid+blackFaces+exception) must not exceed " + MAXIMUM_FAILURE_COUNT,
            ).toBeLessThanOrEqual(MAXIMUM_FAILURE_COUNT);
        },
        SWEEP_TIMEOUT_MS,
    );
});
