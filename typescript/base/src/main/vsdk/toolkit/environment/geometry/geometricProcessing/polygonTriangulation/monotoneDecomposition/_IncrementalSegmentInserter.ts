import { Vector2Dd } from "../../../../../common/linealAlgebra/Vector2Dd.js";
import { _Construct } from "./_Construct.js";
import { _TriangulationSegment } from "./_TriangulationSegment.js";
import { _TriangulationTrapezoid } from "./_TriangulationTrapezoid.js";
import { _TriangulationTrapezoidQueryNode } from "./_TriangulationTrapezoidQueryNode.js";

/** Incremental Seidel-map insertion. The traversal/relinking pass follows below. */
export class _IncrementalSegmentInserter {
    private constructor() {}
    private static trap(index: number): _TriangulationTrapezoid {
        return _Construct.trapezoidAt(index)!;
    }

    private static normalizeSegmentForInsertion(segment: _TriangulationSegment, swapped: { value: boolean }): void {
        swapped.value = false;
        if (_Construct.greaterThan(segment.endPoint, segment.startPoint)) {
            const point = new Vector2Dd(segment.startPoint),
                node = segment.startPointQueryNode;
            segment.startPoint.set(segment.endPoint);
            segment.endPoint.set(point);
            segment.startPointQueryNode = segment.endPointQueryNode;
            segment.endPointQueryNode = node;
            swapped.value = true;
        }
    }

    private static updateLowerNeighbourLinksAfterSplit(split: number, originalUpper: number): void {
        const trapezoid = this.trap(split);
        for (const lower of [trapezoid.lowerLeftTrapezoidIndex, trapezoid.lowerRightTrapezoidIndex]) {
            if (lower <= 0) continue;
            const neighbour = this.trap(lower);
            if (neighbour.upperLeftTrapezoidIndex === originalUpper) neighbour.upperLeftTrapezoidIndex = split;
            if (neighbour.upperRightTrapezoidIndex === originalUpper) neighbour.upperRightTrapezoidIndex = split;
        }
    }

    private static splitTrapezoidAtEndpoint(index: number, segment: _TriangulationSegment, first: boolean): number {
        const endpoint = first ? segment.startPoint : segment.endPoint;
        const other = first ? segment.endPoint : segment.startPoint;
        const root = first ? segment.startPointQueryNode : segment.endPointQueryNode;
        const upper = root!.locateEndpoint(endpoint, other),
            lower = _Construct.allocateTrapezoidIndex();
        if (lower < 0) return -1;
        const old = this.trap(upper),
            copy = new _TriangulationTrapezoid();
        copy.copyFrom(old);
        _Construct.setTrapezoidAt(lower, copy);
        old.lowerPoint.set(endpoint);
        copy.upperPoint.set(endpoint);
        old.lowerLeftTrapezoidIndex = lower;
        old.lowerRightTrapezoidIndex = 0;
        copy.upperLeftTrapezoidIndex = upper;
        copy.upperRightTrapezoidIndex = 0;
        this.updateLowerNeighbourLinksAfterSplit(lower, upper);
        const upperSink = _Construct.allocateQueryNode(),
            lowerSink = _Construct.allocateQueryNode(),
            query = old.sinkNode;
        if (!upperSink || !lowerSink || !query) return -1;
        query.queryNodeType = _Construct.T_Y;
        query.splitPoint.set(endpoint);
        query.segmentIndex = index;
        query.leftChild = lowerSink;
        query.rightChild = upperSink;
        upperSink.queryNodeType = _Construct.T_SINK;
        upperSink.trapezoidIndex = upper;
        upperSink.parent = query;
        lowerSink.queryNodeType = _Construct.T_SINK;
        lowerSink.trapezoidIndex = lower;
        lowerSink.parent = query;
        old.sinkNode = upperSink;
        copy.sinkNode = lowerSink;
        return first ? lower : upper;
    }

    private static inserted(index: number, endpoint: number): boolean {
        const segment = _Construct.segmentAt(index);
        return _Construct.segmentAt(
            endpoint === _Construct.SEGMENT_FIRST_ENDPOINT ? segment.previousSegmentIndex : segment.nextSegmentIndex,
        ).hasBeenInserted;
    }

    private static isLeftOf(segmentIndex: number, point: Vector2Dd): boolean {
        const segment = _Construct.segmentAt(segmentIndex);
        let area: number;
        if (_Construct.greaterThan(segment.endPoint, segment.startPoint)) {
            area = _Construct.fpEqual(segment.endPoint.y, point.y)
                ? point.x < segment.endPoint.x
                    ? 1
                    : -1
                : _Construct.fpEqual(segment.startPoint.y, point.y)
                  ? point.x < segment.startPoint.x
                      ? 1
                      : -1
                  : _Construct.cross(segment.startPoint, segment.endPoint, point);
        } else {
            area = _Construct.fpEqual(segment.endPoint.y, point.y)
                ? point.x < segment.endPoint.x
                    ? 1
                    : -1
                : _Construct.fpEqual(segment.startPoint.y, point.y)
                  ? point.x < segment.startPoint.x
                      ? 1
                      : -1
                  : _Construct.cross(segment.endPoint, segment.startPoint, point);
        }
        return area > 0;
    }

    private static locateOrInsertEndpointTrapezoid(
        index: number,
        segment: _TriangulationSegment,
        first: boolean,
        known: boolean,
        inserted: { value: boolean },
    ): number {
        inserted.value = false;
        if (known) {
            const endpoint = first ? segment.startPoint : segment.endPoint;
            return (first ? segment.startPointQueryNode : segment.endPointQueryNode)!.locateEndpoint(
                endpoint,
                first ? segment.endPoint : segment.startPoint,
            );
        }
        inserted.value = true;
        return this.splitTrapezoidAtEndpoint(index, segment, first);
    }

    private static mergeTrapezoids(segmentIndex: number, first: number, last: number, side: number): number {
        let current = first;
        while (
            current > 0 &&
            _Construct.greaterThanEqualTo(this.trap(current).lowerPoint, this.trap(last).lowerPoint)
        ) {
            const cell = this.trap(current);
            const candidates = [cell.lowerLeftTrapezoidIndex, cell.lowerRightTrapezoidIndex];
            const next =
                candidates.find(
                    (value) =>
                        value > 0 &&
                        (side === _Construct.S_LEFT
                            ? this.trap(value).rightSegmentIndex
                            : this.trap(value).leftSegmentIndex) === segmentIndex,
                ) ?? 0;
            if (next <= 0) break;
            const following = this.trap(next);
            if (
                cell.leftSegmentIndex !== following.leftSegmentIndex ||
                cell.rightSegmentIndex !== following.rightSegmentIndex
            ) {
                current = next;
                continue;
            }
            const parent = following.sinkNode!.parent!;
            if (parent.leftChild === following.sinkNode) parent.leftChild = cell.sinkNode;
            else parent.rightChild = cell.sinkNode;
            for (const field of ["lowerLeftTrapezoidIndex", "lowerRightTrapezoidIndex"] as const) {
                cell[field] = following[field];
                if (cell[field] <= 0) continue;
                const neighbour = this.trap(cell[field]);
                if (neighbour.upperLeftTrapezoidIndex === next) neighbour.upperLeftTrapezoidIndex = current;
                else if (neighbour.upperRightTrapezoidIndex === next) neighbour.upperRightTrapezoidIndex = current;
            }
            cell.lowerPoint.set(following.lowerPoint);
            following.status = _Construct.ST_INVALID;
        }
        return 0;
    }

    public static addSegment(segmentIndex: number): number {
        const s = new _TriangulationSegment();
        let sk: _TriangulationTrapezoidQueryNode;
        let tfirstr = 0;
        let tlastr = 0;
        let i1: _TriangulationTrapezoidQueryNode;
        let i2: _TriangulationTrapezoidQueryNode;
        let t: number;
        let tn: number;
        let tribot = 0;
        const isSwapped = { value: false };
        let tmptriseg: number;

        const src = _Construct.segmentAt(segmentIndex);
        s.startPoint.set(src.startPoint);
        s.endPoint.set(src.endPoint);
        s.hasBeenInserted = src.hasBeenInserted;
        s.startPointQueryNode = src.startPointQueryNode;
        s.endPointQueryNode = src.endPointQueryNode;
        s.nextSegmentIndex = src.nextSegmentIndex;
        s.previousSegmentIndex = src.previousSegmentIndex;
        this.normalizeSegmentForInsertion(s, isSwapped);

        const insertedFirstEndpoint = { value: false };
        const endpoint0AlreadyInserted = isSwapped.value
            ? this.inserted(segmentIndex, _Construct.SEGMENT_LAST_ENDPOINT)
            : this.inserted(segmentIndex, _Construct.SEGMENT_FIRST_ENDPOINT);
        const tfirst = this.locateOrInsertEndpointTrapezoid(
            segmentIndex,
            s,
            true,
            endpoint0AlreadyInserted,
            insertedFirstEndpoint,
        );
        if (tfirst < 0) return -1;

        const insertedLastEndpoint = { value: false };
        const endpoint1AlreadyInserted = isSwapped.value
            ? this.inserted(segmentIndex, _Construct.SEGMENT_FIRST_ENDPOINT)
            : this.inserted(segmentIndex, _Construct.SEGMENT_LAST_ENDPOINT);
        const tlast = this.locateOrInsertEndpointTrapezoid(
            segmentIndex,
            s,
            false,
            endpoint1AlreadyInserted,
            insertedLastEndpoint,
        );
        if (tlast < 0) return -1;
        tribot = insertedLastEndpoint.value ? 0 : 1;

        t = tfirst;
        while (t > 0 && _Construct.greaterThanEqualTo(this.trap(t).lowerPoint, this.trap(tlast).lowerPoint)) {
            const savedTrapezoidIndex = t;
            sk = this.trap(t).sinkNode!;
            i1 = _Construct.allocateQueryNode()!;
            i2 = _Construct.allocateQueryNode()!;

            sk.queryNodeType = _Construct.T_X;
            sk.segmentIndex = segmentIndex;
            sk.leftChild = i1;
            sk.rightChild = i2;

            i1.queryNodeType = _Construct.T_SINK;
            i1.trapezoidIndex = t;
            i1.parent = sk;

            i2.queryNodeType = _Construct.T_SINK;
            tn = _Construct.allocateTrapezoidIndex();
            if (tn < 0) return -1;
            _Construct.setTrapezoidAt(tn, new _TriangulationTrapezoid());
            i2.trapezoidIndex = tn;
            this.trap(tn).status = _Construct.ST_VALID;
            i2.parent = sk;

            if (t === tfirst) tfirstr = tn;
            if (_Construct.equalTo(this.trap(t).lowerPoint, this.trap(tlast).lowerPoint)) tlastr = tn;

            this.trap(tn).copyFrom(this.trap(t));
            this.trap(t).sinkNode = i1;
            this.trap(tn).sinkNode = i2;
            const savedNewTrapezoidIndex = tn;

            if (this.trap(t).lowerLeftTrapezoidIndex <= 0 && this.trap(t).lowerRightTrapezoidIndex <= 0) {
                break;
            } else if (this.trap(t).lowerLeftTrapezoidIndex > 0 && this.trap(t).lowerRightTrapezoidIndex <= 0) {
                if (this.trap(t).upperLeftTrapezoidIndex > 0 && this.trap(t).upperRightTrapezoidIndex > 0) {
                    if (this.trap(t).savedUpperNeighborIndex > 0) {
                        if (this.trap(t).savedUpperNeighborSide === _Construct.S_LEFT) {
                            this.trap(tn).upperLeftTrapezoidIndex = this.trap(t).upperRightTrapezoidIndex;
                            this.trap(t).upperRightTrapezoidIndex = -1;
                            this.trap(tn).upperRightTrapezoidIndex = this.trap(t).savedUpperNeighborIndex;
                            this.trap(this.trap(t).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                            this.trap(this.trap(tn).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = tn;
                            this.trap(this.trap(tn).upperRightTrapezoidIndex).lowerLeftTrapezoidIndex = tn;
                        } else {
                            this.trap(tn).upperRightTrapezoidIndex = -1;
                            this.trap(tn).upperLeftTrapezoidIndex = this.trap(t).upperRightTrapezoidIndex;
                            this.trap(t).upperRightTrapezoidIndex = this.trap(t).upperLeftTrapezoidIndex;
                            this.trap(t).upperLeftTrapezoidIndex = this.trap(t).savedUpperNeighborIndex;
                            this.trap(this.trap(t).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                            this.trap(this.trap(t).upperRightTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                            this.trap(this.trap(tn).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = tn;
                        }
                        this.trap(t).savedUpperNeighborIndex = 0;
                        this.trap(tn).savedUpperNeighborIndex = 0;
                    } else {
                        this.trap(tn).upperLeftTrapezoidIndex = this.trap(t).upperRightTrapezoidIndex;
                        this.trap(t).upperRightTrapezoidIndex = -1;
                        this.trap(tn).upperRightTrapezoidIndex = -1;
                        this.trap(this.trap(tn).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = tn;
                    }
                } else {
                    const tmpUpperIndex = this.trap(t).upperLeftTrapezoidIndex;
                    const td0 = this.trap(tmpUpperIndex).lowerLeftTrapezoidIndex;
                    const td1 = this.trap(tmpUpperIndex).lowerRightTrapezoidIndex;
                    if (td0 > 0 && td1 > 0) {
                        if (
                            this.trap(td0).rightSegmentIndex > 0 &&
                            !this.isLeftOf(this.trap(td0).rightSegmentIndex, s.endPoint)
                        ) {
                            this.trap(t).upperLeftTrapezoidIndex = -1;
                            this.trap(t).upperRightTrapezoidIndex = -1;
                            this.trap(tn).upperRightTrapezoidIndex = -1;
                            this.trap(this.trap(tn).upperLeftTrapezoidIndex).lowerRightTrapezoidIndex = tn;
                        } else {
                            this.trap(tn).upperLeftTrapezoidIndex = -1;
                            this.trap(tn).upperRightTrapezoidIndex = -1;
                            this.trap(t).upperRightTrapezoidIndex = -1;
                            this.trap(this.trap(t).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                        }
                    } else {
                        this.trap(this.trap(t).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                        this.trap(this.trap(t).upperLeftTrapezoidIndex).lowerRightTrapezoidIndex = tn;
                    }
                }

                if (
                    _Construct.fpEqual(this.trap(t).lowerPoint.y, this.trap(tlast).lowerPoint.y) &&
                    _Construct.fpEqual(this.trap(t).lowerPoint.x, this.trap(tlast).lowerPoint.x) &&
                    tribot !== 0
                ) {
                    tmptriseg = isSwapped.value
                        ? _Construct.segmentAt(segmentIndex).previousSegmentIndex
                        : _Construct.segmentAt(segmentIndex).nextSegmentIndex;
                    if (tmptriseg > 0 && this.isLeftOf(tmptriseg, s.startPoint)) {
                        this.trap(this.trap(t).lowerLeftTrapezoidIndex).upperLeftTrapezoidIndex = t;
                        this.trap(tn).lowerLeftTrapezoidIndex = -1;
                        this.trap(tn).lowerRightTrapezoidIndex = -1;
                    } else {
                        this.trap(this.trap(tn).lowerLeftTrapezoidIndex).upperRightTrapezoidIndex = tn;
                        this.trap(t).lowerLeftTrapezoidIndex = -1;
                        this.trap(t).lowerRightTrapezoidIndex = -1;
                    }
                } else {
                    const lower = this.trap(this.trap(t).lowerLeftTrapezoidIndex);
                    if (lower.upperLeftTrapezoidIndex > 0 && lower.upperRightTrapezoidIndex > 0) {
                        if (lower.upperLeftTrapezoidIndex === t) {
                            lower.savedUpperNeighborIndex = lower.upperRightTrapezoidIndex;
                            lower.savedUpperNeighborSide = _Construct.S_LEFT;
                        } else {
                            lower.savedUpperNeighborIndex = lower.upperLeftTrapezoidIndex;
                            lower.savedUpperNeighborSide = _Construct.S_RIGHT;
                        }
                    }
                    lower.upperLeftTrapezoidIndex = t;
                    lower.upperRightTrapezoidIndex = tn;
                }
                t = this.trap(t).lowerLeftTrapezoidIndex;
            } else if (this.trap(t).lowerLeftTrapezoidIndex <= 0 && this.trap(t).lowerRightTrapezoidIndex > 0) {
                if (this.trap(t).upperLeftTrapezoidIndex > 0 && this.trap(t).upperRightTrapezoidIndex > 0) {
                    if (this.trap(t).savedUpperNeighborIndex > 0) {
                        if (this.trap(t).savedUpperNeighborSide === _Construct.S_LEFT) {
                            this.trap(tn).upperLeftTrapezoidIndex = this.trap(t).upperRightTrapezoidIndex;
                            this.trap(t).upperRightTrapezoidIndex = -1;
                            this.trap(tn).upperRightTrapezoidIndex = this.trap(t).savedUpperNeighborIndex;
                            this.trap(this.trap(t).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                            this.trap(this.trap(tn).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = tn;
                            this.trap(this.trap(tn).upperRightTrapezoidIndex).lowerLeftTrapezoidIndex = tn;
                        } else {
                            this.trap(tn).upperRightTrapezoidIndex = -1;
                            this.trap(tn).upperLeftTrapezoidIndex = this.trap(t).upperRightTrapezoidIndex;
                            this.trap(t).upperRightTrapezoidIndex = this.trap(t).upperLeftTrapezoidIndex;
                            this.trap(t).upperLeftTrapezoidIndex = this.trap(t).savedUpperNeighborIndex;
                            this.trap(this.trap(t).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                            this.trap(this.trap(t).upperRightTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                            this.trap(this.trap(tn).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = tn;
                        }
                        this.trap(t).savedUpperNeighborIndex = 0;
                        this.trap(tn).savedUpperNeighborIndex = 0;
                    } else {
                        this.trap(tn).upperLeftTrapezoidIndex = this.trap(t).upperRightTrapezoidIndex;
                        this.trap(t).upperRightTrapezoidIndex = -1;
                        this.trap(tn).upperRightTrapezoidIndex = -1;
                        this.trap(this.trap(tn).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = tn;
                    }
                } else {
                    const tmpUpperIndex = this.trap(t).upperLeftTrapezoidIndex;
                    const td0 = this.trap(tmpUpperIndex).lowerLeftTrapezoidIndex;
                    const td1 = this.trap(tmpUpperIndex).lowerRightTrapezoidIndex;
                    if (td0 > 0 && td1 > 0) {
                        if (
                            this.trap(td0).rightSegmentIndex > 0 &&
                            !this.isLeftOf(this.trap(td0).rightSegmentIndex, s.endPoint)
                        ) {
                            this.trap(t).upperLeftTrapezoidIndex = -1;
                            this.trap(t).upperRightTrapezoidIndex = -1;
                            this.trap(tn).upperRightTrapezoidIndex = -1;
                            this.trap(this.trap(tn).upperLeftTrapezoidIndex).lowerRightTrapezoidIndex = tn;
                        } else {
                            this.trap(tn).upperLeftTrapezoidIndex = -1;
                            this.trap(tn).upperRightTrapezoidIndex = -1;
                            this.trap(t).upperRightTrapezoidIndex = -1;
                            this.trap(this.trap(t).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                        }
                    } else {
                        this.trap(this.trap(t).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                        this.trap(this.trap(t).upperLeftTrapezoidIndex).lowerRightTrapezoidIndex = tn;
                    }
                }

                if (
                    _Construct.fpEqual(this.trap(t).lowerPoint.y, this.trap(tlast).lowerPoint.y) &&
                    _Construct.fpEqual(this.trap(t).lowerPoint.x, this.trap(tlast).lowerPoint.x) &&
                    tribot !== 0
                ) {
                    tmptriseg = isSwapped.value
                        ? _Construct.segmentAt(segmentIndex).previousSegmentIndex
                        : _Construct.segmentAt(segmentIndex).nextSegmentIndex;
                    if (tmptriseg > 0 && this.isLeftOf(tmptriseg, s.startPoint)) {
                        this.trap(this.trap(t).lowerRightTrapezoidIndex).upperLeftTrapezoidIndex = t;
                        this.trap(tn).lowerLeftTrapezoidIndex = -1;
                        this.trap(tn).lowerRightTrapezoidIndex = -1;
                    } else {
                        this.trap(this.trap(tn).lowerRightTrapezoidIndex).upperRightTrapezoidIndex = tn;
                        this.trap(t).lowerLeftTrapezoidIndex = -1;
                        this.trap(t).lowerRightTrapezoidIndex = -1;
                    }
                } else {
                    const lower = this.trap(this.trap(t).lowerRightTrapezoidIndex);
                    if (lower.upperLeftTrapezoidIndex > 0 && lower.upperRightTrapezoidIndex > 0) {
                        if (lower.upperLeftTrapezoidIndex === t) {
                            lower.savedUpperNeighborIndex = lower.upperRightTrapezoidIndex;
                            lower.savedUpperNeighborSide = _Construct.S_LEFT;
                        } else {
                            lower.savedUpperNeighborIndex = lower.upperLeftTrapezoidIndex;
                            lower.savedUpperNeighborSide = _Construct.S_RIGHT;
                        }
                    }
                    lower.upperLeftTrapezoidIndex = t;
                    lower.upperRightTrapezoidIndex = tn;
                }
                t = this.trap(t).lowerRightTrapezoidIndex;
            } else {
                const tmpPoint = new Vector2Dd();
                let tnext: number;
                let isD0 = false;

                if (_Construct.fpEqual(this.trap(t).lowerPoint.y, s.startPoint.y)) {
                    if (this.trap(t).lowerPoint.x > s.startPoint.x) isD0 = true;
                } else {
                    const y0 = this.trap(t).lowerPoint.y;
                    tmpPoint.y = y0;
                    const yt = (y0 - s.startPoint.y) / (s.endPoint.y - s.startPoint.y);
                    tmpPoint.x = s.startPoint.x + yt * (s.endPoint.x - s.startPoint.x);
                    if (_Construct.lessThan(tmpPoint, this.trap(t).lowerPoint)) isD0 = true;
                }

                if (this.trap(t).upperLeftTrapezoidIndex > 0 && this.trap(t).upperRightTrapezoidIndex > 0) {
                    if (this.trap(t).savedUpperNeighborIndex > 0) {
                        if (this.trap(t).savedUpperNeighborSide === _Construct.S_LEFT) {
                            this.trap(tn).upperLeftTrapezoidIndex = this.trap(t).upperRightTrapezoidIndex;
                            this.trap(t).upperRightTrapezoidIndex = -1;
                            this.trap(tn).upperRightTrapezoidIndex = this.trap(t).savedUpperNeighborIndex;
                            this.trap(this.trap(t).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                            this.trap(this.trap(tn).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = tn;
                            this.trap(this.trap(tn).upperRightTrapezoidIndex).lowerLeftTrapezoidIndex = tn;
                        } else {
                            this.trap(tn).upperRightTrapezoidIndex = -1;
                            this.trap(tn).upperLeftTrapezoidIndex = this.trap(t).upperRightTrapezoidIndex;
                            this.trap(t).upperRightTrapezoidIndex = this.trap(t).upperLeftTrapezoidIndex;
                            this.trap(t).upperLeftTrapezoidIndex = this.trap(t).savedUpperNeighborIndex;
                            this.trap(this.trap(t).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                            this.trap(this.trap(t).upperRightTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                            this.trap(this.trap(tn).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = tn;
                        }
                        this.trap(t).savedUpperNeighborIndex = 0;
                        this.trap(tn).savedUpperNeighborIndex = 0;
                    } else {
                        this.trap(tn).upperLeftTrapezoidIndex = this.trap(t).upperRightTrapezoidIndex;
                        this.trap(tn).upperRightTrapezoidIndex = -1;
                        this.trap(t).upperRightTrapezoidIndex = -1;
                        this.trap(this.trap(tn).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = tn;
                    }
                } else {
                    const tmpUpperIndex = this.trap(t).upperLeftTrapezoidIndex;
                    const td0 = this.trap(tmpUpperIndex).lowerLeftTrapezoidIndex;
                    const td1 = this.trap(tmpUpperIndex).lowerRightTrapezoidIndex;
                    if (td0 > 0 && td1 > 0) {
                        if (
                            this.trap(td0).rightSegmentIndex > 0 &&
                            !this.isLeftOf(this.trap(td0).rightSegmentIndex, s.endPoint)
                        ) {
                            this.trap(t).upperLeftTrapezoidIndex = -1;
                            this.trap(t).upperRightTrapezoidIndex = -1;
                            this.trap(tn).upperRightTrapezoidIndex = -1;
                            this.trap(this.trap(tn).upperLeftTrapezoidIndex).lowerRightTrapezoidIndex = tn;
                        } else {
                            this.trap(tn).upperLeftTrapezoidIndex = -1;
                            this.trap(tn).upperRightTrapezoidIndex = -1;
                            this.trap(t).upperRightTrapezoidIndex = -1;
                            this.trap(this.trap(t).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                        }
                    } else {
                        this.trap(this.trap(t).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                        this.trap(this.trap(t).upperLeftTrapezoidIndex).lowerRightTrapezoidIndex = tn;
                    }
                }

                if (
                    _Construct.fpEqual(this.trap(t).lowerPoint.y, this.trap(tlast).lowerPoint.y) &&
                    _Construct.fpEqual(this.trap(t).lowerPoint.x, this.trap(tlast).lowerPoint.x) &&
                    tribot !== 0
                ) {
                    this.trap(this.trap(t).lowerLeftTrapezoidIndex).upperLeftTrapezoidIndex = t;
                    this.trap(this.trap(t).lowerLeftTrapezoidIndex).upperRightTrapezoidIndex = -1;
                    this.trap(this.trap(t).lowerRightTrapezoidIndex).upperLeftTrapezoidIndex = tn;
                    this.trap(this.trap(t).lowerRightTrapezoidIndex).upperRightTrapezoidIndex = -1;
                    this.trap(tn).lowerLeftTrapezoidIndex = this.trap(t).lowerRightTrapezoidIndex;
                    this.trap(t).lowerRightTrapezoidIndex = -1;
                    this.trap(tn).lowerRightTrapezoidIndex = -1;
                    tnext = this.trap(t).lowerRightTrapezoidIndex;
                } else if (isD0) {
                    this.trap(this.trap(t).lowerLeftTrapezoidIndex).upperLeftTrapezoidIndex = t;
                    this.trap(this.trap(t).lowerLeftTrapezoidIndex).upperRightTrapezoidIndex = tn;
                    this.trap(this.trap(t).lowerRightTrapezoidIndex).upperLeftTrapezoidIndex = tn;
                    this.trap(this.trap(t).lowerRightTrapezoidIndex).upperRightTrapezoidIndex = -1;
                    this.trap(t).lowerRightTrapezoidIndex = -1;
                    tnext = this.trap(t).lowerLeftTrapezoidIndex;
                } else {
                    this.trap(this.trap(t).lowerLeftTrapezoidIndex).upperLeftTrapezoidIndex = t;
                    this.trap(this.trap(t).lowerLeftTrapezoidIndex).upperRightTrapezoidIndex = -1;
                    this.trap(this.trap(t).lowerRightTrapezoidIndex).upperLeftTrapezoidIndex = t;
                    this.trap(this.trap(t).lowerRightTrapezoidIndex).upperRightTrapezoidIndex = tn;
                    this.trap(tn).lowerLeftTrapezoidIndex = this.trap(t).lowerRightTrapezoidIndex;
                    this.trap(tn).lowerRightTrapezoidIndex = -1;
                    tnext = this.trap(t).lowerRightTrapezoidIndex;
                }
                t = tnext;
            }
            this.trap(savedTrapezoidIndex).rightSegmentIndex = segmentIndex;
            this.trap(savedNewTrapezoidIndex).leftSegmentIndex = segmentIndex;
        }

        this.mergeTrapezoids(segmentIndex, tfirst, tlast, _Construct.S_LEFT);
        this.mergeTrapezoids(segmentIndex, tfirstr, tlastr, _Construct.S_RIGHT);
        _Construct.segmentAt(segmentIndex).hasBeenInserted = true;
        return 0;
    }
}
