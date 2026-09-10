import { describe, expect, it } from "vitest";
import { Vector2Dd } from "vsdk/toolkit/common/linealAlgebra/Vector2Dd.js";
import { _Construct } from "vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_Construct.js";
import { _IncrementalSegmentInserter } from "vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_IncrementalSegmentInserter.js";
import { _RandomSegmentOrder } from "vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_RandomSegmentOrder.js";
import { _SegmentTableBuilder } from "vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_SegmentTableBuilder.js";
import { _TriangulationTrapezoid } from "vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_TriangulationTrapezoid.js";

describe("trapezoidal-map foundation", () => {
    it("builds the Java 1-based circular segment table for multiple contours", () => {
        expect(_SegmentTableBuilder.prepareSegments([0, 0, 2, 0, 0, 2, 4, 4, 5, 4, 4, 5], 6, [3, 3], 2)).toBe(6);
        expect(_Construct.segmentAt(1).previousSegmentIndex).toBe(3);
        expect(_Construct.segmentAt(3).nextSegmentIndex).toBe(1);
        expect(_Construct.segmentAt(4).previousSegmentIndex).toBe(6);
        expect(_Construct.segmentAt(6).nextSegmentIndex).toBe(4);
        expect(_Construct.segmentAt(1).endPoint).toEqual(new Vector2Dd(2, 0));
        expect(_Construct.segmentAt(3).endPoint).toEqual(new Vector2Dd(0, 0));
    });

    it("preserves epsilon ordering and the zero-index absent-boundary convention", () => {
        expect(_Construct.greaterThan(new Vector2Dd(2, 1), new Vector2Dd(1, 1))).toBe(true);
        expect(_Construct.equalTo(new Vector2Dd(1, 1), new Vector2Dd(1 + 5e-8, 1 - 5e-8))).toBe(true);
        const trapezoid = new _TriangulationTrapezoid();
        expect(trapezoid.insidePolygon()).toBe(0);
    });

    it("bootstraps Java's four-cell query DAG from the first segment", () => {
        _SegmentTableBuilder.prepareSegments([0, 0, 0, 2, 2, 0], 3, [3], 1);
        const root = (
            _Construct as unknown as {
                initQueryStructure(index: number): ReturnType<typeof _Construct.allocateQueryNode>;
            }
        ).initQueryStructure(1)!;
        expect(root.queryNodeType).toBe(_Construct.T_Y);
        expect(_Construct.segmentAt(1).hasBeenInserted).toBe(true);
        expect(_Construct.trapezoidAt(1)!.rightSegmentIndex).toBe(1);
        expect(_Construct.trapezoidAt(2)!.leftSegmentIndex).toBe(1);
        expect(root.locateEndpoint(new Vector2Dd(-1, 1), new Vector2Dd(-1, 0))).toBe(1);
        expect(root.locateEndpoint(new Vector2Dd(1, 1), new Vector2Dd(1, 0))).toBe(2);
    });

    it("splits an endpoint trapezoid while preserving sink ownership", () => {
        _SegmentTableBuilder.prepareSegments([0, 0, 0, 2, 2, 0], 3, [3], 1);
        const root = (
            _Construct as unknown as {
                initQueryStructure(index: number): ReturnType<typeof _Construct.allocateQueryNode>;
            }
        ).initQueryStructure(1)!;
        const segment = _Construct.segmentAt(2);
        segment.startPointQueryNode = root;
        segment.endPointQueryNode = root;
        const internals = _IncrementalSegmentInserter as unknown as {
            splitTrapezoidAtEndpoint(index: number, s: typeof segment, first: boolean): number;
        };
        const lower = internals.splitTrapezoidAtEndpoint(2, segment, true);
        expect(lower).toBeGreaterThan(0);
        expect(_Construct.trapezoidAt(lower)!.sinkNode!.trapezoidIndex).toBe(lower);
    });

    it("uses Java's endpoint-aware lateral predicate during insertion", () => {
        _SegmentTableBuilder.prepareSegments([0, 0, 0, 2, 2, 0], 3, [3], 1);
        const internals = _IncrementalSegmentInserter as unknown as {
            isLeftOf(index: number, point: Vector2Dd): boolean;
        };
        expect(internals.isLeftOf(1, new Vector2Dd(-1, 1))).toBe(true);
        expect(internals.isLeftOf(1, new Vector2Dd(1, 1))).toBe(false);
        expect(internals.isLeftOf(1, new Vector2Dd(-1, 0))).toBe(true);
    });

    it("refreshes query roots for a segment not yet inserted", () => {
        _SegmentTableBuilder.prepareSegments([0, 0, 0, 2, 2, 0], 3, [3], 1);
        const internals = _Construct as unknown as {
            initQueryStructure(index: number): ReturnType<typeof _Construct.allocateQueryNode>;
            findNewRoots(index: number): number;
        };
        const root = internals.initQueryStructure(1)!;
        const pending = _Construct.segmentAt(2);
        pending.startPointQueryNode = root;
        pending.endPointQueryNode = root;
        expect(internals.findNewRoots(2)).toBe(0);
        expect(pending.startPointQueryNode!.queryNodeType).toBe(_Construct.T_SINK);
        expect(pending.endPointQueryNode!.queryNodeType).toBe(_Construct.T_SINK);
    });

    it("ports Java's complete addSegment traversal and relinking pass", () => {
        _SegmentTableBuilder.prepareSegments([0, 0, 0, 2, 2, 0], 3, [3], 1);
        const root = (
            _Construct as unknown as {
                initQueryStructure(index: number): ReturnType<typeof _Construct.allocateQueryNode>;
            }
        ).initQueryStructure(1)!;
        for (let i = 1; i <= 3; i++) {
            _Construct.segmentAt(i).startPointQueryNode = root;
            _Construct.segmentAt(i).endPointQueryNode = root;
        }
        expect(_IncrementalSegmentInserter.addSegment(2)).toBe(0);
        expect(_Construct.segmentAt(2).hasBeenInserted).toBe(true);
        expect(
            _Construct.queryNodes
                .slice(1, _Construct.nextQueryNodeIndex)
                .some((node) => node?.queryNodeType === _Construct.T_X && node.segmentIndex === 2),
        ).toBe(true);
    });

    it("constructs the complete trapezoidal map using Java's insertion schedule", () => {
        _SegmentTableBuilder.prepareSegments([0, 0, 0, 2, 2, 0], 3, [3], 1);
        _RandomSegmentOrder.generateRandomOrdering(3);
        expect(_Construct.constructTrapezoids(3)).toBe(0);
        expect([1, 2, 3].every((index) => _Construct.segmentAt(index).hasBeenInserted)).toBe(true);
        expect(
            _Construct.trapezoids
                .slice(1, _Construct.nextTrapezoidIndex)
                .some((trapezoid) => trapezoid?.status === _Construct.ST_VALID),
        ).toBe(true);
    });
});
