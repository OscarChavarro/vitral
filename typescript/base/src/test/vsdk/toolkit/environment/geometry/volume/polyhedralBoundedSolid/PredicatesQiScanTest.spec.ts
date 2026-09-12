import { describe, expect, it } from "vitest";

import { Math as JavaMath } from "java/lang/Math.js";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { PolyhedralBoundedSolidPredicates } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidPredicates.js";
import { SimpleTestGeometryLibrary } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/SimpleTestGeometryLibrary.js";
import { yieldToEventLoop } from "../../geometricProcessing/polyhedralBoundedSolidOperators/_HarnessEventLoopYield.js";

/**
Harness accommodation: JUnit has no per-test time budget; this scan sweeps a
30deg grid over every edge of the APPE1967 object.
*/
const QI_SCAN_TIMEOUT_MS = 1_800_000;

/**
Validates the robust `PolyhedralBoundedSolidPredicates.quantitativeInvisibility`
against an independent ray-march of the APPE1967 featured object (the union of 12
axis-aligned boxes). The march is a sampling oracle, so at a near-tangent line of
sight it cannot tell a measure-zero graze from a real thin occluder; those cases
are genuinely ambiguous (either visible or hidden is acceptable). The test
therefore measures the maximal INSIDE-RUN DEPTH along each line of sight and only
flags a disagreement as a real error when the depth is unambiguous: clearly
substantial (a real occluder, QI must be > 0) or clearly ~zero (a tangent, QI
must be 0). The ambiguous in-between band is skipped. The committed scan uses a
30deg / 8-sample grid for speed; the full 15deg / 12-sample scan was also
verified to report 0 real errors during development.
 */
describe("PredicatesQiScanTest", () => {
    const BOX = [
        [0.5, 0.1, 0.1, 0.5, 0.1, 0.1],
        [0.5, 0.9, 0.1, 0.5, 0.1, 0.1],
        [0.1, 0.5, 0.1, 0.1, 0.5, 0.1],
        [0.9, 0.5, 0.1, 0.1, 0.5, 0.1],
        [0.1, 0.5, 0.1, 0.1, 0.5, 0.1],
        [0.1, 0.5, 0.9, 0.1, 0.5, 0.1],
        [0.1, 0.1, 0.5, 0.1, 0.1, 0.5],
        [0.1, 0.9, 0.5, 0.1, 0.1, 0.5],
        [0.3, 0.5, 0.5, 0.3, 0.1, 0.1],
        [0.5, 0.5, 0.5, 0.1, 0.1, 0.5],
        [0.7, 0.5, 0.9, 0.3, 0.1, 0.1],
        [0.9, 0.5, 0.9, 0.1, 0.5, 0.1],
    ];

    function bi(p: Vector3Dd): boolean {
        for (const b of BOX)
            if (
                Math.abs(p.x() - b[0]!) <= b[3]! - 1e-7 &&
                Math.abs(p.y() - b[1]!) <= b[4]! - 1e-7 &&
                Math.abs(p.z() - b[2]!) <= b[5]! - 1e-7
            )
                return true;
        return false;
    }

    function coarseOccluded(oe: Vector3Dd, lp: Vector3Dd): boolean {
        const d = lp.subtract(oe);
        for (let i = 1; i < 2000; i++) {
            const s = i / 2000;
            if (s >= 1 - 3e-4) break;
            if (bi(oe.add(d.multiply(s)))) return true;
        }
        return false;
    }

    // max inside-run depth (in local units) of the segment oeLocal -> lpLocal
    function insideDepth(oe: Vector3Dd, lp: Vector3Dd): number {
        const d = lp.subtract(oe);
        const L = d.length();
        const N = 200000;
        let prev = false,
            runStart = 0,
            maxDepth = 0;
        for (let i = 0; i <= N; i++) {
            const s = i / N;
            if (s >= 1 - 3e-4) break;
            const inside = bi(oe.add(d.multiply(s)));
            if (inside && !prev) runStart = s;
            if (!inside && prev) {
                const dp = (s - runStart) * L;
                if (dp > maxDepth) maxDepth = dp;
            }
            prev = inside;
        }
        if (prev) {
            const dp = (1 - runStart) * L;
            if (dp > maxDepth) maxDepth = dp;
        }
        return maxDepth;
    }

    it(
        "quantitativeInvisibility_matchesGroundTruthOutsideAmbiguousBand",
        async () => {
            const solid = SimpleTestGeometryLibrary.createTestObjectAPPE1967_3();
            const eye = new Vector3Dd(2, -1, 2);
            const CLEAR_OCCLUDER = 5.0e-3,
                CLEAR_TANGENT = 5.0e-5;
            let errors = 0;
            for (let za = 0; za < 360; za += 30)
                for (let xa = 0; xa < 360; xa += 30) {
                    // Harness accommodation: vitest's worker RPC times out after a
                    // fixed 60 s, so the scan releases the worker's event loop on
                    // every grid step. Java has no such constraint.
                    await yieldToEventLoop();
                    const R = new Matrix4x4d()
                        .axisRotation(JavaMath.toRadians(za), 0, 0, 1)
                        .multiply(new Matrix4x4d().axisRotation(JavaMath.toRadians(xa), 1, 0, 0));
                    const Rinv = R.inverse();
                    const oe = Rinv.multiply(eye);
                    for (let ei = 0; ei < solid.getEdgesList().size(); ei++) {
                        const e = solid.getEdgesList().get(ei)!;
                        const a = e.leftHalf!.startingVertex.position,
                            b = e.rightHalf!.startingVertex.position;
                        for (let k = 1; k <= 8; k++) {
                            const t = k / 9.0;
                            const lp = a.multiply(1 - t).add(b.multiply(t));
                            const qi = PolyhedralBoundedSolidPredicates.quantitativeInvisibility(solid, oe, lp);
                            const qiOccluded = qi > 0;
                            const coarse = coarseOccluded(oe, lp);
                            if (qiOccluded === coarse) continue; // agree: not a candidate
                            const depth = insideDepth(oe, lp); // resolve only the grazing candidates
                            if (depth > CLEAR_OCCLUDER && !qiOccluded) {
                                errors++;
                                if (errors <= 25)
                                    console.log(
                                        `MISS rot(${za},${xa}) e=${ei} t=${t.toFixed(2)} depth=${depth.toFixed(5)} qi=${qi}`,
                                    );
                            } else if (depth < CLEAR_TANGENT && qiOccluded) {
                                errors++;
                                if (errors <= 25)
                                    console.log(
                                        `PHANTOM rot(${za},${xa}) e=${ei} t=${t.toFixed(2)} depth=${depth.toFixed(6)} qi=${qi}`,
                                    );
                            }
                        }
                    }
                }
            console.log("Robust QI real errors (outside ambiguous band) TOTAL=" + errors);
            expect(errors).toBe(0);
        },
        QI_SCAN_TIMEOUT_MS,
    );
});
