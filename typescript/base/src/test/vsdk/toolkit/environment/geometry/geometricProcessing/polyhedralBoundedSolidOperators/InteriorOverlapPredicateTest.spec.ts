import { describe, expect, it } from "vitest";

import { Random } from "java/util/Random.js";
import type { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidNumericPolicy } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { _PolyhedralBoundedSolidSetNonIntersectingClassifier } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/classification/_PolyhedralBoundedSolidSetNonIntersectingClassifier.js";
import { OrientedBox, PolyhedralBoundedSolidVolumeOracle } from "./PolyhedralBoundedSolidVolumeOracle.js";
import { PrismFixtures } from "./PrismFixtures.js";

/** One pair: two boxes given by their corners, and the truth. */
class Case {
    public constructor(
        public readonly name: string,
        public readonly boxA: number[],
        public readonly boxB: number[],
        public readonly overlap: boolean,
    ) {}
}

/**
"Do the interiors of A and B overlap?" (plan-stage11 Phase D). The
preflights of the set operator and the "result cannot be empty" guard of
`setOp` rest on this decision. It used to be taken by sampling: 27 probes
in the common bounding box and then a grid built from the vertex
coordinates. A thin overlap that falls between the samples was missed, and
more easily when the pair is not aligned with the axes.

<p>Every pair here is built from boxes, so the truth is known: the
interiors overlap exactly when the boxes overlap with positive volume
before the rigid motion applied to both. The pairs are the hard ones for a
sampling test: overlaps as thin as 10^-3 of the size, with coplanar faces,
so that no edge of one crosses a face of the other in its interior.</p>

<p>The decision has a band: an overlap thinner than about 5 tolerances is
a contact for the kernel (the tolerance is 10^-5 of the size of the pair).
The last test pins that down.</p>
 */
describe("InteriorOverlapPredicateTest", () => {
    function cases(): Case[] {
        const result: Case[] = [];
        const unit = [0, 0, 0, 1, 1, 1];

        for (const t of [0.5, 0.1, 1.0e-2, 1.0e-3]) {
            // Same cross-section, shifted along x: only the faces across x
            // differ, every other face is coplanar with one of the other.
            result.push(new Case("shifted, overlap " + t, unit, [1 - t, 0, 0, 2 - t, 1, 1], true));
            // Coplanar top and bottom, overlapping in a thin corner column.
            result.push(new Case("corner column " + t, unit, [1 - t, 1 - t, 0, 2, 2, 1], true));
            // A thin plate inside A against its top face.
            result.push(new Case("inner plate " + t, unit, [0.2, 0.2, 1 - t, 0.8, 0.8, 1], true));
            // A thin plate through A, flush with two of its faces.
            result.push(new Case("through plate " + t, unit, [0, 0.5, 0, 1, 0.5 + t, 1], true));
            // B contains A and shares five of its faces.
            result.push(new Case("cover, margin " + t, unit, [0, 0, 0, 1 + t, 1, 1], true));
        }
        result.push(new Case("identical", unit, unit, true));
        result.push(new Case("face contact", unit, [1, 0, 0, 2, 1, 1], false));
        result.push(new Case("partial face contact", unit, [1, 0.3, 0.2, 2, 1.4, 0.7], false));
        result.push(new Case("stacked, partial", unit, [0.4, 0.3, 1, 1.4, 1.3, 1.6], false));
        result.push(new Case("edge contact", unit, [1, 0.1, 1, 1.9, 0.9, 1.8], false));
        result.push(new Case("vertex contact", unit, [1, 1, 1, 2, 2, 2], false));
        result.push(new Case("apart", unit, [1.5, 0, 0, 2.5, 1, 1], false));
        result.push(new Case("thin plate on top, outside", unit, [0.2, 0.2, 1, 0.8, 0.8, 1.001], false));
        return result;
    }

    function turned(xy: number[][], angle: number, dx: number, dy: number): number[][] {
        const out: number[][] = [];
        const c = Math.cos(angle);
        const s = Math.sin(angle);
        for (let i = 0; i < xy.length; i++) {
            out.push([c * xy[i]![0]! - s * xy[i]![1]! + dx, s * xy[i]![0]! + c * xy[i]![1]! + dy]);
        }
        return out;
    }

    /** Farthest reach of a polygon along a direction. */
    function support(xy: number[][], direction: number): number {
        let best = -Number.MAX_VALUE;
        for (const p of xy) {
            best = Math.max(best, p[0]! * Math.cos(direction) + p[1]! * Math.sin(direction));
        }
        return best;
    }

    function side(from: number[], to: number[], p: number[]): number {
        return (to[0]! - from[0]!) * (p[1]! - from[1]!) - (to[1]! - from[1]!) * (p[0]! - from[0]!);
    }

    /** Area of the intersection of two convex polygons (Sutherland-Hodgman). */
    function intersectionArea(subject: number[][], clip: number[][]): number {
        let polygon: number[][] = [];
        for (const p of subject) {
            polygon.push(p);
        }
        for (let i = 0; i < clip.length && polygon.length > 0; i++) {
            const c0 = clip[i]!;
            const c1 = clip[(i + 1) % clip.length]!;
            const next: number[][] = [];
            for (let j = 0; j < polygon.length; j++) {
                const p = polygon[j]!;
                const q = polygon[(j + 1) % polygon.length]!;
                const sp = side(c0, c1, p);
                const sq = side(c0, c1, q);
                if (sp >= 0) {
                    next.push(p);
                }
                if ((sp > 0 && sq < 0) || (sp < 0 && sq > 0)) {
                    const t = sp / (sp - sq);
                    next.push([p[0]! + t * (q[0]! - p[0]!), p[1]! + t * (q[1]! - p[1]!)]);
                }
            }
            polygon = next;
        }
        if (polygon.length < 3) {
            return 0.0;
        }
        return Math.abs(PrismFixtures.polygonArea(polygon));
    }

    function solid(box: number[], motion: Matrix4x4d | null): PolyhedralBoundedSolid {
        const place = PolyhedralBoundedSolidVolumeOracle.translation(
            (box[0]! + box[3]!) / 2.0,
            (box[1]! + box[4]!) / 2.0,
            (box[2]! + box[5]!) / 2.0,
        );
        return new OrientedBox(
            box[3]! - box[0]!,
            box[4]! - box[1]!,
            box[5]! - box[2]!,
            motion === null ? place : motion.multiply(place),
        ).toSolid();
    }

    function wrongAnswers(motion: Matrix4x4d | null): string[] {
        const wrong: string[] = [];
        for (const c of cases()) {
            for (let order = 0; order < 2; order++) {
                const a = solid(c.boxA, motion);
                const b = solid(c.boxB, motion);
                const answer =
                    order === 0
                        ? _PolyhedralBoundedSolidSetNonIntersectingClassifier.interiorsOverlap(a, b)
                        : _PolyhedralBoundedSolidSetNonIntersectingClassifier.interiorsOverlap(b, a);
                if (answer !== c.overlap) {
                    wrong.push(c.name + (order === 0 ? " (A,B)" : " (B,A)") + " answered " + answer);
                }
            }
        }
        return wrong;
    }

    it("given_axisAlignedPairs_when_interiorsOverlapIsAsked_then_answerIsExact", () => {
        expect(wrongAnswers(null)).toEqual([]);
    });

    it("given_rotatedPairs_when_interiorsOverlapIsAsked_then_answerIsExact", () => {
        const random = new Random(31);
        const wrong: string[] = [];
        for (let i = 0; i < 12; i++) {
            const motion = PolyhedralBoundedSolidVolumeOracle.translation(
                random.nextDouble(),
                random.nextDouble(),
                random.nextDouble(),
            ).multiply(
                PolyhedralBoundedSolidVolumeOracle.rotation(
                    3.0 * random.nextDouble(),
                    random.nextGaussian(),
                    random.nextGaussian(),
                    random.nextGaussian(),
                ),
            );
            for (const w of wrongAnswers(motion)) {
                wrong.push("motion " + i + ": " + w);
            }
        }
        expect(wrong).toEqual([]);
    });

    /**
    Prisms over convex polygons with the same bottom and top planes. No
    edge of one crosses a face of the other in its interior (the side
    edges end on the shared planes), so nothing but the overlap test can
    tell a thin overlap from a contact. The truth is the area of the
    intersection of the two base polygons.
    */
    it("given_prismsWithCoplanarEnds_when_interiorsOverlapIsAsked_then_answerIsExact", () => {
        const random = new Random(57);
        const wrong: string[] = [];
        for (let i = 0; i < 60; i++) {
            const sidesA = 3 + random.nextInt(8);
            const sidesB = 3 + random.nextInt(8);
            const baseA = turned(PrismFixtures.regularPolygon(sidesA, 1.0), 6.3 * random.nextDouble(), 0, 0);
            const direction = 6.3 * random.nextDouble();
            // Distance at which the polygons would overlap by `depth` if
            // they were discs: thin overlaps, exact contact never.
            const depth = [0.5, 0.05, 5.0e-3, 5.0e-4, -5.0e-4, -5.0e-3, -0.05][i % 7]!;
            const baseB0 = turned(PrismFixtures.regularPolygon(sidesB, 1.0), 6.3 * random.nextDouble(), 0, 0);
            const reach = support(baseA, direction) + support(baseB0, direction + Math.PI) - depth;
            const baseB = turned(baseB0, 0, reach * Math.cos(direction), reach * Math.sin(direction));
            const truth = intersectionArea(baseA, baseB) > 1.0e-12;
            const motion =
                i % 2 === 0
                    ? null
                    : PolyhedralBoundedSolidVolumeOracle.translation(
                          random.nextDouble(),
                          random.nextDouble(),
                          random.nextDouble(),
                      ).multiply(
                          PolyhedralBoundedSolidVolumeOracle.rotation(
                              3.0 * random.nextDouble(),
                              random.nextGaussian(),
                              random.nextGaussian(),
                              random.nextGaussian(),
                          ),
                      );
            for (let order = 0; order < 2; order++) {
                const a = PrismFixtures.prism(baseA, 1.0);
                const b = PrismFixtures.prism(baseB, 1.0);
                if (motion !== null) {
                    PolyhedralBoundedSolidModeler.applyTransformation(a, motion);
                    PolyhedralBoundedSolidModeler.applyTransformation(b, motion);
                }
                const answer =
                    order === 0
                        ? _PolyhedralBoundedSolidSetNonIntersectingClassifier.interiorsOverlap(a, b)
                        : _PolyhedralBoundedSolidSetNonIntersectingClassifier.interiorsOverlap(b, a);
                if (answer !== truth) {
                    wrong.push(
                        "#" +
                            i +
                            " " +
                            sidesA +
                            "/" +
                            sidesB +
                            " sides, depth " +
                            depth +
                            (motion === null ? "" : " rotated") +
                            (order === 0 ? " (A,B)" : " (B,A)") +
                            " answered " +
                            answer +
                            ", area " +
                            intersectionArea(baseA, baseB),
                    );
                }
            }
        }
        expect(wrong).toEqual([]);
    });

    /**
    Inside the tolerance band the pair is a contact: a box that enters
    another by half a tolerance does not overlap it, and one that enters by
    twenty tolerances does.
    */
    it("given_overlapAroundTheTolerance_when_interiorsOverlapIsAsked_then_bandIsAContact", () => {
        const tolerance = PolyhedralBoundedSolidNumericPolicy.forSolids(
            solid([0, 0, 0, 1, 1, 1], null),
            solid([1, 0, 0, 2, 1, 1], null),
        ).bigEpsilon();
        expect(
            _PolyhedralBoundedSolidSetNonIntersectingClassifier.interiorsOverlap(
                solid([0, 0, 0, 1, 1, 1], null),
                solid([1 - 0.5 * tolerance, 0, 0, 2, 1, 1], null),
            ),
        ).toBe(false);
        expect(
            _PolyhedralBoundedSolidSetNonIntersectingClassifier.interiorsOverlap(
                solid([0, 0, 0, 1, 1, 1], null),
                solid([1 - 20 * tolerance, 0, 0, 2, 1, 1], null),
            ),
        ).toBe(true);
    });
});
