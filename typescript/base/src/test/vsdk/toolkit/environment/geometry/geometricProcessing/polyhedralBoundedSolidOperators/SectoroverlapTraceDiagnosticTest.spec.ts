import { describe, it } from "vitest";

import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { SimpleTestGeometryLibrary } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/SimpleTestGeometryLibrary.js";
import { _PolyhedralBoundedSolidSetGeometricPredicateProcessor } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/intersection/_PolyhedralBoundedSolidSetGeometricPredicateProcessor.js";
import { _PolyhedralBoundedSolidSetNullEdgesConnector } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetNullEdgesConnector.js";
import { CsgSampleCorpus } from "./CsgSampleCorpus.js";
import { CsgSampleCorpusFixtures } from "./CsgSampleCorpusFixtures.js";

/**
§7.3.1 diagnostic harness — captures every `sectoroverlap`
invocation during a boolean operation, plus the connector's
`endsa`/`endsb` survivors, and prints a structured
report.

<p>This is the input for §7.3.2 (deciding which of the three
correction alternatives to apply). The report should answer:</p>
<ul>
<li>How many times is `sectoroverlap` called for each pending
case?</li>
<li>Which calls hit `|a2-b1| ≈ 0` (boundary-ray contact)?</li>
<li>What is the decision (true/false) for each?</li>
<li>For MANT1988_15_1 INT/SUB, which calls correlate with the 4
loose survivors that end up unmatched?</li>
</ul>

 */
describe("SectoroverlapTraceDiagnosticTest", () => {
    function pad(text: string, width: number): string {
        return text.length >= width ? text : text + " ".repeat(width - text.length);
    }

    function padStart(text: string, width: number): string {
        return text.length >= width ? text : " ".repeat(width - text.length) + text;
    }

    function signed(value: number): string {
        return (value >= 0 ? "+" : "") + value.toFixed(5);
    }

    function runDiagnostic(label: string, pair: PolyhedralBoundedSolid[], op: number): void {
        _PolyhedralBoundedSolidSetGeometricPredicateProcessor.enableSectoroverlapTrace();
        try {
            PolyhedralBoundedSolidModeler.setOp(pair[0]!, pair[1]!, op, false);
        } finally {
            const trace = _PolyhedralBoundedSolidSetGeometricPredicateProcessor.getSectoroverlapTrace();
            _PolyhedralBoundedSolidSetGeometricPredicateProcessor.disableSectoroverlapTrace();

            const looseA = _PolyhedralBoundedSolidSetNullEdgesConnector.getLastLooseACount();
            const looseB = _PolyhedralBoundedSolidSetNullEdgesConnector.getLastLooseBCount();

            console.log("\n========== " + label + " ==========");
            console.log("looseA=" + looseA + " looseB=" + looseB);
            console.log("sectoroverlap calls: " + (trace !== null ? trace.length : 0));

            if (trace !== null) {
                let trueCount = 0;
                let boundaryRayCount = 0;
                let boundaryRayTrue = 0;
                for (const e of trace) {
                    if (e.decision) trueCount++;
                    if (e.boundaryRayContact) {
                        boundaryRayCount++;
                        if (e.decision) boundaryRayTrue++;
                    }
                }
                console.log("  decisions: " + trueCount + " TRUE / " + (trace.length - trueCount) + " FALSE");
                console.log(
                    "  boundary-ray-contact calls (|a2-b1|<1e-12 or |b2-a1|<1e-12): " +
                        boundaryRayCount +
                        " (of which " +
                        boundaryRayTrue +
                        " returned TRUE)",
                );

                if (trace.length !== 0) {
                    console.log("\n  --- all entries ---");
                    console.log(
                        "  " +
                            [pad("i", 3), pad("fA", 4), pad("fB", 4), pad("vA(F->T)", 9), pad("vB(F->T)", 9)].join(
                                " | ",
                            ) +
                            " | " +
                            [pad("a1", 8), pad("a2", 8)].join(" ") +
                            " | " +
                            [pad("b1", 8), pad("b2", 8)].join(" ") +
                            " | " +
                            pad("BRC", 5) +
                            " | dec",
                    );
                    for (const e of trace) {
                        console.log(
                            "  " +
                                [
                                    padStart(String(e.callIndex), 3),
                                    padStart(String(e.faceA), 4),
                                    padStart(String(e.faceB), 4),
                                    padStart(String(e.vertexAFrom), 3) + "->" + pad(String(e.vertexATo), 3),
                                    padStart(String(e.vertexBFrom), 3) + "->" + pad(String(e.vertexBTo), 3),
                                ].join(" | ") +
                                " | " +
                                [signed(e.a1), signed(e.a2)].join(" ") +
                                " | " +
                                [signed(e.b1), signed(e.b2)].join(" ") +
                                " | " +
                                pad(e.boundaryRayContact ? "YES" : "no", 5) +
                                " | " +
                                (e.decision ? "T" : "F"),
                        );
                    }
                }
            }

            console.log("================================\n");
        }
    }

    it("trace_mant1988_15_1_intersection", () => {
        runDiagnostic(
            "MANT1988_15_1 + INTERSECTION",
            SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_1(),
            PolyhedralBoundedSolidModeler.INTERSECTION,
        );
    });

    it("trace_mant1988_15_1_subtract", () => {
        runDiagnostic(
            "MANT1988_15_1 + SUBTRACT",
            SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_1(),
            PolyhedralBoundedSolidModeler.SUBTRACT,
        );
    });

    it("trace_mant1988_15_1_union_for_comparison", () => {
        // UNION passes (looseA=0) — provides a control trace.
        runDiagnostic(
            "MANT1988_15_1 + UNION (CONTROL — passes)",
            SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_1(),
            PolyhedralBoundedSolidModeler.UNION,
        );
    });

    it("trace_hollow_brick_intersection_for_comparison", () => {
        // HOLLOW_BRICK INTERSECTION passes (looseA=0) — second control.
        runDiagnostic(
            "HOLLOW_BRICK + INTERSECTION (CONTROL — passes)",
            CsgSampleCorpusFixtures.createPair(CsgSampleCorpus.HOLLOW_BRICK),
            PolyhedralBoundedSolidModeler.INTERSECTION,
        );
    });
});
