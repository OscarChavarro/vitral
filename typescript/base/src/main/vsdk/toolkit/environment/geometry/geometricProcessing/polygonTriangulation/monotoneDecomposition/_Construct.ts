import { Vector2Dd } from "../../../../../common/linealAlgebra/Vector2Dd.js";
import { _TriangulationSegment } from "./_TriangulationSegment.js";
import { _TriangulationTrapezoid } from "./_TriangulationTrapezoid.js";
import { _TriangulationTrapezoidQueryNode } from "./_TriangulationTrapezoidQueryNode.js";
import { _IncrementalSegmentInserter } from "./_IncrementalSegmentInserter.js";
import { _InsertionBatchSchedule } from "./_InsertionBatchSchedule.js";
import { _RandomSegmentOrder } from "./_RandomSegmentOrder.js";

/** Shared, 1-based storage and predicates for Seidel's trapezoidal map. */
export class _Construct {
    public static readonly T_X = 1;
    public static readonly T_Y = 2;
    public static readonly T_SINK = 3;
    public static readonly SEGMENT_SIZE = 200;
    public static readonly QSIZE = 8 * _Construct.SEGMENT_SIZE;
    public static readonly TRAPEZOID_TABLE_SIZE = 4 * _Construct.SEGMENT_SIZE;
    public static readonly SEGMENT_FIRST_ENDPOINT = 1;
    public static readonly SEGMENT_LAST_ENDPOINT = 2;
    public static readonly TRIANGULATION_INFINITY = 1 << 30;
    public static readonly TRIANGULATOR_EPSILON = 1.0e-7;
    public static readonly S_LEFT = 1;
    public static readonly S_RIGHT = 2;
    public static readonly ST_VALID = 1;
    public static readonly ST_INVALID = 2;
    public static nextQueryNodeIndex = 1;
    public static nextTrapezoidIndex = 1;
    public static queryNodes: Array<_TriangulationTrapezoidQueryNode | null> = [];
    public static trapezoids: Array<_TriangulationTrapezoid | null> = [];
    public static segments: _TriangulationSegment[] = [];
    private constructor() {}

    public static resetTrapezoidQueryNodes(): void {
        this.queryNodes.fill(null);
        this.nextQueryNodeIndex = 1;
    }
    public static resetTrapezoids(): void {
        this.trapezoids.fill(null);
        this.nextTrapezoidIndex = 1;
    }
    public static prepareStorage(count: number): void {
        const segmentCapacity = count + 1;
        const trapezoidCapacity = Math.max(16 * count + 16, this.TRAPEZOID_TABLE_SIZE);
        const queryCapacity = Math.max(32 * count + 32, this.QSIZE);
        if (this.segments.length !== segmentCapacity)
            this.segments = Array.from({ length: segmentCapacity }, () => new _TriangulationSegment());
        if (this.trapezoids.length !== trapezoidCapacity)
            this.trapezoids = Array.from({ length: trapezoidCapacity }, () => null);
        else this.trapezoids.fill(null);
        if (this.queryNodes.length !== queryCapacity)
            this.queryNodes = Array.from({ length: queryCapacity }, () => null);
        else this.queryNodes.fill(null);
        this.nextQueryNodeIndex = 1;
        this.nextTrapezoidIndex = 1;
    }
    private static max(out: Vector2Dd, a: Vector2Dd, b: Vector2Dd): void {
        if (a.y > b.y + this.TRIANGULATOR_EPSILON) out.set(a);
        else if (this.fpEqual(a.y, b.y)) out.set(a.x > b.x + this.TRIANGULATOR_EPSILON ? a : b);
        else out.set(b);
    }
    private static min(out: Vector2Dd, a: Vector2Dd, b: Vector2Dd): void {
        if (a.y < b.y - this.TRIANGULATOR_EPSILON) out.set(a);
        else if (this.fpEqual(a.y, b.y)) out.set(a.x < b.x ? a : b);
        else out.set(b);
    }
    public static greaterThan(a: Vector2Dd, b: Vector2Dd): boolean {
        return a.y > b.y + this.TRIANGULATOR_EPSILON || (a.y >= b.y - this.TRIANGULATOR_EPSILON && a.x > b.x);
    }
    public static equalTo(a: Vector2Dd, b: Vector2Dd): boolean {
        return this.fpEqual(a.y, b.y) && this.fpEqual(a.x, b.x);
    }
    public static greaterThanEqualTo(a: Vector2Dd, b: Vector2Dd): boolean {
        return a.y > b.y + this.TRIANGULATOR_EPSILON || (a.y >= b.y - this.TRIANGULATOR_EPSILON && a.x >= b.x);
    }
    public static lessThan(a: Vector2Dd, b: Vector2Dd): boolean {
        return a.y < b.y - this.TRIANGULATOR_EPSILON || (a.y <= b.y + this.TRIANGULATOR_EPSILON && a.x < b.x);
    }
    public static cross(a: Vector2Dd, b: Vector2Dd, c: Vector2Dd): number {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }
    public static dot(a: Vector2Dd, b: Vector2Dd): number {
        return a.x * b.x + a.y * b.y;
    }
    public static fpEqual(a: number, b: number): boolean {
        return Math.abs(a - b) <= this.TRIANGULATOR_EPSILON;
    }
    public static segmentAt(index: number): _TriangulationSegment {
        return this.segments[index]!;
    }
    public static setSegmentInserted(index: number, value: boolean): void {
        this.segmentAt(index).hasBeenInserted = value;
    }
    public static trapezoidAt(index: number): _TriangulationTrapezoid | null {
        return this.trapezoids[index] ?? null;
    }
    public static setTrapezoidAt(index: number, value: _TriangulationTrapezoid): void {
        this.trapezoids[index] = value;
    }
    public static allocateQueryNode(): _TriangulationTrapezoidQueryNode | null {
        if (this.nextQueryNodeIndex >= this.queryNodes.length) return null;
        const node = new _TriangulationTrapezoidQueryNode();
        this.queryNodes[this.nextQueryNodeIndex++] = node;
        return node;
    }
    public static allocateTrapezoidIndex(): number {
        return this.nextTrapezoidIndex >= this.trapezoids.length ? -1 : this.nextTrapezoidIndex++;
    }

    /** Bootstrap the four initial cells and the X/Y/SINK query DAG. */
    private static initQueryStructure(segmentIndex: number): _TriangulationTrapezoidQueryNode | null {
        const segment = this.segmentAt(segmentIndex);
        this.resetTrapezoids();
        this.resetTrapezoidQueryNodes();
        const upper = this.allocateQueryNode(),
            outsideUpper = this.allocateQueryNode(),
            lower = this.allocateQueryNode();
        const outsideLower = this.allocateQueryNode(),
            split = this.allocateQueryNode(),
            left = this.allocateQueryNode(),
            right = this.allocateQueryNode();
        if (!upper || !outsideUpper || !lower || !outsideLower || !split || !left || !right) return null;
        upper.queryNodeType = this.T_Y;
        this.max(upper.splitPoint, segment.startPoint, segment.endPoint);
        upper.rightChild = outsideUpper;
        outsideUpper.queryNodeType = this.T_SINK;
        outsideUpper.parent = upper;
        upper.leftChild = lower;
        lower.queryNodeType = this.T_Y;
        lower.parent = upper;
        this.min(lower.splitPoint, segment.startPoint, segment.endPoint);
        lower.leftChild = outsideLower;
        outsideLower.queryNodeType = this.T_SINK;
        outsideLower.parent = lower;
        lower.rightChild = split;
        split.queryNodeType = this.T_X;
        split.segmentIndex = segmentIndex;
        split.parent = lower;
        split.leftChild = left;
        left.queryNodeType = this.T_SINK;
        left.parent = split;
        split.rightChild = right;
        right.queryNodeType = this.T_SINK;
        right.parent = split;
        const first = this.allocateTrapezoidIndex(),
            second = this.allocateTrapezoidIndex(),
            bottom = this.allocateTrapezoidIndex(),
            top = this.allocateTrapezoidIndex();
        if (first < 0 || second < 0 || bottom < 0 || top < 0) return null;
        for (const index of [first, second, bottom, top]) this.trapezoids[index] = new _TriangulationTrapezoid();
        const t1 = this.trapezoidAt(first)!,
            t2 = this.trapezoidAt(second)!,
            t3 = this.trapezoidAt(bottom)!,
            t4 = this.trapezoidAt(top)!;
        t1.upperPoint.set(upper.splitPoint);
        t2.upperPoint.set(upper.splitPoint);
        t4.lowerPoint.set(upper.splitPoint);
        t1.lowerPoint.set(lower.splitPoint);
        t2.lowerPoint.set(lower.splitPoint);
        t3.upperPoint.set(lower.splitPoint);
        t4.upperPoint.set(this.TRIANGULATION_INFINITY, this.TRIANGULATION_INFINITY);
        t3.lowerPoint.set(-this.TRIANGULATION_INFINITY, -this.TRIANGULATION_INFINITY);
        t1.rightSegmentIndex = segmentIndex;
        t2.leftSegmentIndex = segmentIndex;
        t1.upperLeftTrapezoidIndex = top;
        t2.upperLeftTrapezoidIndex = top;
        t1.lowerLeftTrapezoidIndex = bottom;
        t2.lowerLeftTrapezoidIndex = bottom;
        t4.lowerLeftTrapezoidIndex = first;
        t3.upperLeftTrapezoidIndex = first;
        t4.lowerRightTrapezoidIndex = second;
        t3.upperRightTrapezoidIndex = second;
        t1.sinkNode = left;
        t2.sinkNode = right;
        t3.sinkNode = outsideLower;
        t4.sinkNode = outsideUpper;
        outsideUpper.trapezoidIndex = top;
        outsideLower.trapezoidIndex = bottom;
        left.trapezoidIndex = first;
        right.trapezoidIndex = second;
        segment.hasBeenInserted = true;
        return upper;
    }

    private static findNewRoots(segmentIndex: number): number {
        const segment = this.segmentAt(segmentIndex);
        if (segment.hasBeenInserted) return 0;
        let trapezoid = segment.startPointQueryNode!.locateEndpoint(segment.startPoint, segment.endPoint);
        segment.startPointQueryNode = this.trapezoidAt(trapezoid)!.sinkNode;
        trapezoid = segment.endPointQueryNode!.locateEndpoint(segment.endPoint, segment.startPoint);
        segment.endPointQueryNode = this.trapezoidAt(trapezoid)!.sinkNode;
        return 0;
    }

    public static constructTrapezoids(nseg: number): number {
        this.prepareStorage(nseg);
        const root = this.initQueryStructure(_RandomSegmentOrder.chooseSegment());

        for (let i = 1; i <= nseg; i++) {
            this.segments[i]!.startPointQueryNode = root;
            this.segments[i]!.endPointQueryNode = root;
        }

        for (let h = 1; h <= _InsertionBatchSchedule.mathLogStarN(nseg); h++) {
            for (
                let i = _InsertionBatchSchedule.mathN(nseg, h - 1) + 1;
                i <= _InsertionBatchSchedule.mathN(nseg, h);
                i++
            ) {
                _IncrementalSegmentInserter.addSegment(_RandomSegmentOrder.chooseSegment());
            }
            for (let i = 1; i <= nseg; i++) this.findNewRoots(i);
        }

        for (
            let i = _InsertionBatchSchedule.mathN(nseg, _InsertionBatchSchedule.mathLogStarN(nseg)) + 1;
            i <= nseg;
            i++
        ) {
            _IncrementalSegmentInserter.addSegment(_RandomSegmentOrder.chooseSegment());
        }
        return 0;
    }
}
