import { describe, expect, it } from "vitest";

import { IllegalArgumentException } from "java/lang/IllegalArgumentException.js";
import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { PolyhedralBoundedSolidTestFixtures } from "../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTestFixtures.js";

/**
Regression guard for the algebraic identities of set-operations
([MANT1988] Ch. 15.1):
<ul>
<li>Idempotence: `A∪A = A`, `A∩A = A`, `A−A = ∅`.</li>
<li>Determinism of swapped-operand difference: running `A−B`
    twice on equivalent inputs returns equivalent topologies.</li>
</ul>

<p>This class replaces the eliminated
`PolyhedralBoundedSolidSetOperatorAlgebraicPropertiesTest`, whose
assertions were inverted drift detectors (§7.3.1.E in the stage-2 plan).
The corpus is split into:</p>

<ul>
<li><b>cleanIdempotenceFixtures</b> — the 3 fixtures × 2 indices where
    §7.3.1.A identity-preflight makes `A op A_clone` satisfy the
    laws.</li>
<li><b>cleanDifferenceSwapFixtures</b> — the fixtures where
    `A−B` is deterministic.</li>
</ul>

<p>The 5 known-drift fixtures (3 absorption, 2 diff-swapped on
MANT1988_15_2_LIMIT / MANT1988_6_13) are NOT included here; they
remain as TODO documented in plan §7.3 §7.3.1.D.</p>
 */
describe("AlgebraicIdentityRegressionTest", () => {
    function createPair(corpusKey: string): PolyhedralBoundedSolid[] {
        if ("MANT1986_2" === corpusKey) {
            return PolyhedralBoundedSolidTestFixtures.createMant1986_2Pair();
        }
        if ("MANT1988_15_2_LIMIT" === corpusKey) {
            return PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(0);
        }
        if ("MANT1988_6_13" === corpusKey) {
            return PolyhedralBoundedSolidTestFixtures.createMant1988_6_13Pair();
        }
        throw new IllegalArgumentException("Unsupported corpus: " + corpusKey);
    }

    function boundingBoxMatches(solid: PolyhedralBoundedSolid, baselineMinMax: Float64Array | number[]): boolean {
        const actualMinMax = solid.getMinMax();
        for (let i = 0; i < 6; i++) {
            if (Math.abs(actualMinMax[i]! - baselineMinMax[i]!) > 1.0e-6) {
                return false;
            }
        }
        return true;
    }

    function isEmpty(s: PolyhedralBoundedSolid | null): boolean {
        return (
            s !== null &&
            s.getPolygonsList().size() === 0 &&
            s.getEdgesList().size() === 0 &&
            s.getVerticesList().size() === 0
        );
    }

    function cleanIdempotenceFixtures(): [string, number][] {
        // After §7.3.1.A (identity preflight in setOp) all 6 idempotence
        // cases are clean: A∪A = A∩A = A, A−A = ∅.
        return [
            ["MANT1986_2", 0],
            ["MANT1986_2", 1],
            ["MANT1988_15_2_LIMIT", 0],
            ["MANT1988_15_2_LIMIT", 1],
            ["MANT1988_6_13", 0],
            ["MANT1988_6_13", 1],
        ];
    }

    function cleanDifferenceSwapFixtures(): [string][] {
        // Post-§7.3.1.A re-mapping (§7.3.1.B): all 3 fixtures pass the
        // diff-swap determinism (same f/e/v across runs AND validate).
        return [["MANT1986_2"], ["MANT1988_15_2_LIMIT"], ["MANT1988_6_13"]];
    }

    function cleanAbsorptionFixtures(): [string][] {
        // Post-§7.3.1.A: only MANT1986_2 is clean. The two remaining
        // drift fixtures (MANT1988_15_2_LIMIT, MANT1988_6_13) are the
        // §7.3.1.D target.
        return [["MANT1986_2"]];
    }

    it.each(cleanIdempotenceFixtures())("idempotence: %s idx=%s", (corpusKey: string, solidIndex: number) => {
        const baseline = createPair(corpusKey)[solidIndex]!;
        const baselineMinMax = baseline.getMinMax();

        const unionLeft = createPair(corpusKey)[solidIndex]!;
        const unionRight = createPair(corpusKey)[solidIndex]!;
        const interLeft = createPair(corpusKey)[solidIndex]!;
        const interRight = createPair(corpusKey)[solidIndex]!;
        const diffLeft = createPair(corpusKey)[solidIndex]!;
        const diffRight = createPair(corpusKey)[solidIndex]!;

        const union = PolyhedralBoundedSolidModeler.setOp(
            unionLeft,
            unionRight,
            PolyhedralBoundedSolidModeler.UNION,
            false,
        );
        const inter = PolyhedralBoundedSolidModeler.setOp(
            interLeft,
            interRight,
            PolyhedralBoundedSolidModeler.INTERSECTION,
            false,
        );
        const diff = PolyhedralBoundedSolidModeler.setOp(
            diffLeft,
            diffRight,
            PolyhedralBoundedSolidModeler.SUBTRACT,
            false,
        );

        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(union)).toBe(true);
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(inter)).toBe(true);
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(diff)).toBe(true);

        expect(boundingBoxMatches(union, baselineMinMax), "A∪A bounding box must match A").toBe(true);
        expect(boundingBoxMatches(inter, baselineMinMax), "A∩A bounding box must match A").toBe(true);
        expect(isEmpty(diff), "A−A must be empty").toBe(true);
    });

    it.each(cleanDifferenceSwapFixtures())("diff-swap determinism: %s", (corpusKey: string) => {
        const pairA = createPair(corpusKey);
        const pairB = createPair(corpusKey);
        const pairC = createPair(corpusKey);
        const pairD = createPair(corpusKey);

        const abFirst = PolyhedralBoundedSolidModeler.setOp(
            pairA[0]!,
            pairA[1]!,
            PolyhedralBoundedSolidModeler.SUBTRACT,
            false,
            true,
            false,
        );
        const abSecond = PolyhedralBoundedSolidModeler.setOp(
            pairB[0]!,
            pairB[1]!,
            PolyhedralBoundedSolidModeler.SUBTRACT,
            false,
            true,
            false,
        );
        const baFirst = PolyhedralBoundedSolidModeler.setOp(
            pairC[1]!,
            pairC[0]!,
            PolyhedralBoundedSolidModeler.SUBTRACT,
            false,
            true,
            false,
        );
        const baSecond = PolyhedralBoundedSolidModeler.setOp(
            pairD[1]!,
            pairD[0]!,
            PolyhedralBoundedSolidModeler.SUBTRACT,
            false,
            true,
            false,
        );

        expect(abFirst.getPolygonsList().size()).toBe(abSecond.getPolygonsList().size());
        expect(abFirst.getEdgesList().size()).toBe(abSecond.getEdgesList().size());
        expect(abFirst.getVerticesList().size()).toBe(abSecond.getVerticesList().size());
        expect(baFirst.getPolygonsList().size()).toBe(baSecond.getPolygonsList().size());
        expect(baFirst.getEdgesList().size()).toBe(baSecond.getEdgesList().size());
        expect(baFirst.getVerticesList().size()).toBe(baSecond.getVerticesList().size());
    });

    it.each(cleanAbsorptionFixtures())("absorption: %s", (corpusKey: string) => {
        // A ∪ (A ∩ B) = A   and   A ∩ (A ∪ B) = A
        const baselineLeft = createPair(corpusKey)[0]!;
        const baselineMinMax = baselineLeft.getMinMax();

        const pairForIntersection = createPair(corpusKey);
        const pairForUnion = createPair(corpusKey);
        const pairForFinalUnion = createPair(corpusKey);
        const pairForFinalIntersection = createPair(corpusKey);

        const aIntersectionB = PolyhedralBoundedSolidModeler.setOp(
            pairForIntersection[0]!,
            pairForIntersection[1]!,
            PolyhedralBoundedSolidModeler.INTERSECTION,
            false,
        );
        const firstAbsorption = PolyhedralBoundedSolidModeler.setOp(
            pairForFinalUnion[0]!,
            aIntersectionB,
            PolyhedralBoundedSolidModeler.UNION,
            false,
        );

        const aUnionB = PolyhedralBoundedSolidModeler.setOp(
            pairForUnion[0]!,
            pairForUnion[1]!,
            PolyhedralBoundedSolidModeler.UNION,
            false,
        );
        const secondAbsorption = PolyhedralBoundedSolidModeler.setOp(
            pairForFinalIntersection[0]!,
            aUnionB,
            PolyhedralBoundedSolidModeler.INTERSECTION,
            false,
        );

        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(firstAbsorption)).toBe(true);
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(secondAbsorption)).toBe(true);
        expect(boundingBoxMatches(firstAbsorption, baselineMinMax), "A ∪ (A ∩ B) bbox must match A").toBe(true);
        expect(boundingBoxMatches(secondAbsorption, baselineMinMax), "A ∩ (A ∪ B) bbox must match A").toBe(true);
    });
});
