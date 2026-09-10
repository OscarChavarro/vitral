import { Vector2Dd } from "../../../../../common/linealAlgebra/Vector2Dd.js";
import { _Construct } from "./_Construct.js";
import { _MonotoneChainNode } from "./_MonotoneChainNode.js";
import { _VertexChain } from "./_VertexChain.js";

export class _Monotone {
    public static readonly SP_SIMPLE_LRUP = 1;
    public static readonly SP_SIMPLE_LRDN = 2;
    public static readonly SP_2UP_2DN = 3;
    public static readonly SP_2UP_LEFT = 4;
    public static readonly SP_2UP_RIGHT = 5;
    public static readonly SP_2DN_LEFT = 6;
    public static readonly SP_2DN_RIGHT = 7;
    public static readonly SP_NOSPLIT = -1;
    public static readonly TR_FROM_UP = 1;
    public static readonly TR_FROM_DN = 2;
    public static readonly TRI_LHS = 1;
    public static readonly TRI_RHS = 2;
    private static monotoneChainNodes = Array.from(
        { length: _Construct.TRAPEZOID_TABLE_SIZE },
        () => new _MonotoneChainNode(),
    );
    private static vertexChains = Array.from({ length: _Construct.SEGMENT_SIZE }, () => new _VertexChain());
    private static monotonePolygonEntryNode = new Int32Array(_Construct.SEGMENT_SIZE);
    private static visitedTrapezoids = new Int32Array(_Construct.TRAPEZOID_TABLE_SIZE);
    private static nextChainNodeIndex = 0;
    private static nextOutputTriangleIndex = 0;
    private static nextMonotonePolygonIndex = 0;
    private constructor() {}

    private static newMonotone(): number {
        return ++this.nextMonotonePolygonIndex;
    }
    private static newChainElement(): number {
        return ++this.nextChainNodeIndex;
    }
    private static crossSine(first: Vector2Dd, second: Vector2Dd): number {
        return first.x * second.y - second.x * first.y;
    }
    private static length(vector: Vector2Dd): number {
        return Math.sqrt(vector.x * vector.x + vector.y * vector.y);
    }
    private static getAngle(base: Vector2Dd, next: Vector2Dd, other: Vector2Dd): number {
        const edge = new Vector2Dd(next.x - base.x, next.y - base.y);
        const toOther = new Vector2Dd(other.x - base.x, other.y - base.y);
        const cosine = _Construct.dot(edge, toOther) / this.length(edge) / this.length(toOther);
        return this.crossSine(edge, toOther) >= 0 ? cosine : -cosine - 2;
    }
    private static getVertexPositions(
        v0: number,
        v1: number,
        ip: Int32Array | number[],
        iq: Int32Array | number[],
    ): number {
        const first = this.vertexChains[v0]!,
            second = this.vertexChains[v1]!;
        let angle = -4,
            slot = 0;
        for (let i = 0; i < 4; i++) {
            if (first.adjacentVertexIndices[i]! <= 0) continue;
            const candidate = this.getAngle(
                first.point,
                this.vertexChains[first.adjacentVertexIndices[i]!]!.point,
                second.point,
            );
            if (candidate > angle) {
                angle = candidate;
                slot = i;
            }
        }
        ip[0] = slot;
        angle = -4;
        slot = 0;
        for (let i = 0; i < 4; i++) {
            if (second.adjacentVertexIndices[i]! <= 0) continue;
            const candidate = this.getAngle(
                second.point,
                this.vertexChains[second.adjacentVertexIndices[i]!]!.point,
                first.point,
            );
            if (candidate > angle) {
                angle = candidate;
                slot = i;
            }
        }
        iq[0] = slot;
        return 0;
    }
    private static makeNewMonotonePolygon(current: number, v0: number, v1: number): number {
        if (v0 <= 0 || v1 <= 0) return current;
        const ip = new Int32Array(1),
            iq = new Int32Array(1),
            created = this.newMonotone();
        const first = this.vertexChains[v0]!,
            second = this.vertexChains[v1]!;
        this.getVertexPositions(v0, v1, ip, iq);
        const p = first.chainNodeIndicesByAdjacency[ip[0]!]!,
            q = second.chainNodeIndicesByAdjacency[iq[0]!]!;
        const i = this.newChainElement(),
            j = this.newChainElement();
        this.monotoneChainNodes[i]!.vertexIndex = v0;
        this.monotoneChainNodes[j]!.vertexIndex = v1;
        this.monotoneChainNodes[i]!.nextNodeIndex = this.monotoneChainNodes[p]!.nextNodeIndex;
        this.monotoneChainNodes[this.monotoneChainNodes[p]!.nextNodeIndex]!.previousNodeIndex = i;
        this.monotoneChainNodes[i]!.previousNodeIndex = j;
        this.monotoneChainNodes[j]!.nextNodeIndex = i;
        this.monotoneChainNodes[j]!.previousNodeIndex = this.monotoneChainNodes[q]!.previousNodeIndex;
        this.monotoneChainNodes[this.monotoneChainNodes[q]!.previousNodeIndex]!.nextNodeIndex = j;
        this.monotoneChainNodes[p]!.nextNodeIndex = q;
        this.monotoneChainNodes[q]!.previousNodeIndex = p;
        const nf0 = first.adjacencySlotCount,
            nf1 = second.adjacencySlotCount;
        first.adjacentVertexIndices[ip[0]!] = v1;
        first.chainNodeIndicesByAdjacency[nf0] = i;
        first.adjacentVertexIndices[nf0] =
            this.monotoneChainNodes[this.monotoneChainNodes[i]!.nextNodeIndex]!.vertexIndex;
        second.chainNodeIndicesByAdjacency[nf1] = j;
        second.adjacentVertexIndices[nf1] = v0;
        first.adjacencySlotCount++;
        second.adjacencySlotCount++;
        this.monotonePolygonEntryNode[current] = p;
        this.monotonePolygonEntryNode[created] = i;
        return created;
    }

    public static monotonateTrapezoids(vertexCount: number): number {
        this.visitedTrapezoids.fill(0);
        this.monotonePolygonEntryNode.fill(0);
        this.monotoneChainNodes = Array.from(
            { length: _Construct.TRAPEZOID_TABLE_SIZE },
            () => new _MonotoneChainNode(),
        );
        this.vertexChains = Array.from({ length: _Construct.SEGMENT_SIZE }, () => new _VertexChain());
        let startIndex = -1;
        for (let i = 0; i < _Construct.TRAPEZOID_TABLE_SIZE; i++) {
            const candidate = _Construct.trapezoidAt(i);
            if (candidate !== null && candidate.insidePolygon() !== 0) {
                startIndex = i;
                break;
            }
        }
        if (startIndex < 0) return 0;
        for (let i = 1; i <= vertexCount; i++) {
            const node = this.monotoneChainNodes[i]!,
                segment = _Construct.segmentAt(i),
                vertex = this.vertexChains[i]!;
            node.previousNodeIndex = segment.previousSegmentIndex;
            node.nextNodeIndex = segment.nextSegmentIndex;
            node.vertexIndex = i;
            vertex.point.set(segment.startPoint);
            vertex.adjacentVertexIndices[0] = segment.nextSegmentIndex;
            vertex.chainNodeIndicesByAdjacency[0] = i;
            vertex.adjacencySlotCount = 1;
        }
        this.nextChainNodeIndex = vertexCount;
        this.nextMonotonePolygonIndex = 0;
        this.monotonePolygonEntryNode[0] = 1;
        const start = _Construct.trapezoidAt(startIndex)!;
        if (start.upperLeftTrapezoidIndex > 0)
            this.traversePolygon(0, startIndex, start.upperLeftTrapezoidIndex, this.TR_FROM_UP);
        else if (start.lowerLeftTrapezoidIndex > 0)
            this.traversePolygon(0, startIndex, start.lowerLeftTrapezoidIndex, this.TR_FROM_DN);
        return this.newMonotone();
    }

    private static traversePolygon(current: number, index: number, from: number, direction: number): number {
        if (index <= 0 || this.visitedTrapezoids[index] !== 0) return 0;
        const trapezoid = _Construct.trapezoidAt(index);
        if (trapezoid === null) return 0;
        this.visitedTrapezoids[index] = 1;
        const visit = (polygon: number, next: number, dir: number) => this.traversePolygon(polygon, next, index, dir);
        const noSplit = () => {
            visit(current, trapezoid.upperLeftTrapezoidIndex, this.TR_FROM_DN);
            visit(current, trapezoid.upperRightTrapezoidIndex, this.TR_FROM_DN);
            visit(current, trapezoid.lowerLeftTrapezoidIndex, this.TR_FROM_UP);
            visit(current, trapezoid.lowerRightTrapezoidIndex, this.TR_FROM_UP);
            return this.SP_NOSPLIT;
        };
        const twoUp = trapezoid.upperLeftTrapezoidIndex > 0 && trapezoid.upperRightTrapezoidIndex > 0;
        const twoDown = trapezoid.lowerLeftTrapezoidIndex > 0 && trapezoid.lowerRightTrapezoidIndex > 0;
        if (trapezoid.upperLeftTrapezoidIndex <= 0 && trapezoid.upperRightTrapezoidIndex <= 0) {
            if (!twoDown) return noSplit();
            const v0 = _Construct.trapezoidAt(trapezoid.lowerRightTrapezoidIndex)!.leftSegmentIndex,
                v1 = trapezoid.leftSegmentIndex;
            if (from === trapezoid.lowerRightTrapezoidIndex) {
                const created = this.makeNewMonotonePolygon(current, v1, v0);
                visit(current, trapezoid.lowerRightTrapezoidIndex, this.TR_FROM_UP);
                visit(created, trapezoid.lowerLeftTrapezoidIndex, this.TR_FROM_UP);
            } else {
                const created = this.makeNewMonotonePolygon(current, v0, v1);
                visit(current, trapezoid.lowerLeftTrapezoidIndex, this.TR_FROM_UP);
                visit(created, trapezoid.lowerRightTrapezoidIndex, this.TR_FROM_UP);
            }
            return 0;
        }
        if (trapezoid.lowerLeftTrapezoidIndex <= 0 && trapezoid.lowerRightTrapezoidIndex <= 0) {
            if (!twoUp) return noSplit();
            const v0 = trapezoid.rightSegmentIndex,
                v1 = _Construct.trapezoidAt(trapezoid.upperLeftTrapezoidIndex)!.rightSegmentIndex;
            if (from === trapezoid.upperRightTrapezoidIndex) {
                const created = this.makeNewMonotonePolygon(current, v1, v0);
                visit(current, trapezoid.upperRightTrapezoidIndex, this.TR_FROM_DN);
                visit(created, trapezoid.upperLeftTrapezoidIndex, this.TR_FROM_DN);
            } else {
                const created = this.makeNewMonotonePolygon(current, v0, v1);
                visit(current, trapezoid.upperLeftTrapezoidIndex, this.TR_FROM_DN);
                visit(created, trapezoid.upperRightTrapezoidIndex, this.TR_FROM_DN);
            }
            return 0;
        }
        if (twoUp && twoDown) {
            const v0 = _Construct.trapezoidAt(trapezoid.lowerRightTrapezoidIndex)!.leftSegmentIndex,
                v1 = _Construct.trapezoidAt(trapezoid.upperLeftTrapezoidIndex)!.rightSegmentIndex;
            const right =
                (direction === this.TR_FROM_DN && trapezoid.lowerRightTrapezoidIndex === from) ||
                (direction === this.TR_FROM_UP && trapezoid.upperRightTrapezoidIndex === from);
            const created = this.makeNewMonotonePolygon(current, right ? v1 : v0, right ? v0 : v1);
            if (right) {
                visit(current, trapezoid.upperRightTrapezoidIndex, this.TR_FROM_DN);
                visit(current, trapezoid.lowerRightTrapezoidIndex, this.TR_FROM_UP);
                visit(created, trapezoid.upperLeftTrapezoidIndex, this.TR_FROM_DN);
                visit(created, trapezoid.lowerLeftTrapezoidIndex, this.TR_FROM_UP);
            } else {
                visit(current, trapezoid.upperLeftTrapezoidIndex, this.TR_FROM_DN);
                visit(current, trapezoid.lowerLeftTrapezoidIndex, this.TR_FROM_UP);
                visit(created, trapezoid.upperRightTrapezoidIndex, this.TR_FROM_DN);
                visit(created, trapezoid.lowerRightTrapezoidIndex, this.TR_FROM_UP);
            }
            return this.SP_2UP_2DN;
        }
        if (twoUp) {
            const leftCase = _Construct.equalTo(
                trapezoid.lowerPoint,
                _Construct.segmentAt(trapezoid.leftSegmentIndex).endPoint,
            );
            const v0 = leftCase
                ? _Construct.trapezoidAt(trapezoid.upperLeftTrapezoidIndex)!.rightSegmentIndex
                : trapezoid.rightSegmentIndex;
            const v1 = leftCase
                ? _Construct.segmentAt(trapezoid.leftSegmentIndex).nextSegmentIndex
                : _Construct.trapezoidAt(trapezoid.upperLeftTrapezoidIndex)!.rightSegmentIndex;
            const comesFromSplitSide =
                direction === this.TR_FROM_UP &&
                (leftCase ? trapezoid.upperLeftTrapezoidIndex : trapezoid.upperRightTrapezoidIndex) === from;
            const created = this.makeNewMonotonePolygon(
                current,
                comesFromSplitSide ? v1 : v0,
                comesFromSplitSide ? v0 : v1,
            );
            if (leftCase) {
                if (comesFromSplitSide) {
                    visit(current, trapezoid.upperLeftTrapezoidIndex, this.TR_FROM_DN);
                    visit(created, trapezoid.lowerLeftTrapezoidIndex, this.TR_FROM_UP);
                    visit(created, trapezoid.upperRightTrapezoidIndex, this.TR_FROM_DN);
                    visit(created, trapezoid.lowerRightTrapezoidIndex, this.TR_FROM_UP);
                } else {
                    visit(current, trapezoid.upperRightTrapezoidIndex, this.TR_FROM_DN);
                    visit(current, trapezoid.lowerLeftTrapezoidIndex, this.TR_FROM_UP);
                    visit(current, trapezoid.lowerRightTrapezoidIndex, this.TR_FROM_UP);
                    visit(created, trapezoid.upperLeftTrapezoidIndex, this.TR_FROM_DN);
                }
            } else if (comesFromSplitSide) {
                visit(current, trapezoid.upperRightTrapezoidIndex, this.TR_FROM_DN);
                visit(created, trapezoid.lowerRightTrapezoidIndex, this.TR_FROM_UP);
                visit(created, trapezoid.lowerLeftTrapezoidIndex, this.TR_FROM_UP);
                visit(created, trapezoid.upperLeftTrapezoidIndex, this.TR_FROM_DN);
            } else {
                visit(current, trapezoid.upperLeftTrapezoidIndex, this.TR_FROM_DN);
                visit(current, trapezoid.lowerLeftTrapezoidIndex, this.TR_FROM_UP);
                visit(current, trapezoid.lowerRightTrapezoidIndex, this.TR_FROM_UP);
                visit(created, trapezoid.upperRightTrapezoidIndex, this.TR_FROM_DN);
            }
            return leftCase ? this.SP_2UP_LEFT : this.SP_2UP_RIGHT;
        }
        if ((trapezoid.upperLeftTrapezoidIndex > 0 || trapezoid.upperRightTrapezoidIndex > 0) && twoDown) {
            const leftCase = _Construct.equalTo(
                trapezoid.upperPoint,
                _Construct.segmentAt(trapezoid.leftSegmentIndex).startPoint,
            );
            const v0 = _Construct.trapezoidAt(trapezoid.lowerRightTrapezoidIndex)!.leftSegmentIndex;
            const v1 = leftCase
                ? trapezoid.leftSegmentIndex
                : _Construct.segmentAt(trapezoid.rightSegmentIndex).nextSegmentIndex;
            const splitSide = leftCase ? trapezoid.lowerLeftTrapezoidIndex : trapezoid.lowerRightTrapezoidIndex;
            const comesFromSplitSide = direction === this.TR_FROM_DN && splitSide === from;
            const reverse = leftCase ? !comesFromSplitSide : comesFromSplitSide;
            const created = this.makeNewMonotonePolygon(current, reverse ? v1 : v0, reverse ? v0 : v1);
            if (leftCase) {
                if (!comesFromSplitSide) {
                    visit(current, trapezoid.upperRightTrapezoidIndex, this.TR_FROM_DN);
                    visit(current, trapezoid.lowerRightTrapezoidIndex, this.TR_FROM_UP);
                    visit(current, trapezoid.upperLeftTrapezoidIndex, this.TR_FROM_DN);
                    visit(created, trapezoid.lowerLeftTrapezoidIndex, this.TR_FROM_UP);
                } else {
                    visit(current, trapezoid.lowerLeftTrapezoidIndex, this.TR_FROM_UP);
                    visit(created, trapezoid.upperLeftTrapezoidIndex, this.TR_FROM_DN);
                    visit(created, trapezoid.upperRightTrapezoidIndex, this.TR_FROM_DN);
                    visit(created, trapezoid.lowerRightTrapezoidIndex, this.TR_FROM_UP);
                }
            } else if (comesFromSplitSide) {
                visit(current, trapezoid.lowerRightTrapezoidIndex, this.TR_FROM_UP);
                visit(created, trapezoid.upperRightTrapezoidIndex, this.TR_FROM_DN);
                visit(created, trapezoid.upperLeftTrapezoidIndex, this.TR_FROM_DN);
                visit(created, trapezoid.lowerLeftTrapezoidIndex, this.TR_FROM_UP);
            } else {
                visit(current, trapezoid.upperLeftTrapezoidIndex, this.TR_FROM_DN);
                visit(current, trapezoid.lowerLeftTrapezoidIndex, this.TR_FROM_UP);
                visit(current, trapezoid.upperRightTrapezoidIndex, this.TR_FROM_DN);
                visit(created, trapezoid.lowerRightTrapezoidIndex, this.TR_FROM_UP);
            }
            return leftCase ? this.SP_2DN_LEFT : this.SP_2DN_RIGHT;
        }
        if (trapezoid.leftSegmentIndex <= 0 || trapezoid.rightSegmentIndex <= 0) return noSplit();
        const lowerSplit =
            _Construct.equalTo(trapezoid.upperPoint, _Construct.segmentAt(trapezoid.leftSegmentIndex).startPoint) &&
            _Construct.equalTo(trapezoid.lowerPoint, _Construct.segmentAt(trapezoid.rightSegmentIndex).startPoint);
        const upperSplit =
            _Construct.equalTo(trapezoid.upperPoint, _Construct.segmentAt(trapezoid.rightSegmentIndex).endPoint) &&
            _Construct.equalTo(trapezoid.lowerPoint, _Construct.segmentAt(trapezoid.leftSegmentIndex).endPoint);
        if (!lowerSplit && !upperSplit) return noSplit();
        const v0 = lowerSplit
            ? trapezoid.rightSegmentIndex
            : _Construct.segmentAt(trapezoid.rightSegmentIndex).nextSegmentIndex;
        const v1 = lowerSplit
            ? trapezoid.leftSegmentIndex
            : _Construct.segmentAt(trapezoid.leftSegmentIndex).nextSegmentIndex;
        const created = this.makeNewMonotonePolygon(
            current,
            direction === this.TR_FROM_UP ? v1 : v0,
            direction === this.TR_FROM_UP ? v0 : v1,
        );
        if (direction === this.TR_FROM_UP) {
            visit(current, trapezoid.upperLeftTrapezoidIndex, this.TR_FROM_DN);
            visit(current, trapezoid.upperRightTrapezoidIndex, this.TR_FROM_DN);
            visit(created, trapezoid.lowerRightTrapezoidIndex, this.TR_FROM_UP);
            visit(created, trapezoid.lowerLeftTrapezoidIndex, this.TR_FROM_UP);
        } else {
            visit(current, trapezoid.lowerRightTrapezoidIndex, this.TR_FROM_UP);
            visit(current, trapezoid.lowerLeftTrapezoidIndex, this.TR_FROM_UP);
            visit(created, trapezoid.upperLeftTrapezoidIndex, this.TR_FROM_DN);
            visit(created, trapezoid.upperRightTrapezoidIndex, this.TR_FROM_DN);
        }
        return lowerSplit ? this.SP_SIMPLE_LRDN : this.SP_SIMPLE_LRUP;
    }

    public static triangulateMonotonePolygons(_numberOfVertices: number, count: number, output: number[][]): number {
        this.nextOutputTriangleIndex = 0;
        for (let i = 0; i < count; i++) {
            let vertexCount = 1,
                processed = false;
            const first = this.monotoneChainNodes[this.monotonePolygonEntryNode[i]!]!.vertexIndex;
            let yMaximum = new Vector2Dd(this.vertexChains[first]!.point);
            let yMinimum = new Vector2Dd(this.vertexChains[first]!.point);
            let maximumPosition = this.monotonePolygonEntryNode[i]!;
            this.monotoneChainNodes[maximumPosition]!.isMarked = true;
            let p = this.monotoneChainNodes[maximumPosition]!.nextNodeIndex;
            let vertex: number;
            while ((vertex = this.monotoneChainNodes[p]!.vertexIndex) !== first) {
                if (this.monotoneChainNodes[p]!.isMarked) {
                    processed = true;
                    break;
                }
                this.monotoneChainNodes[p]!.isMarked = true;
                if (_Construct.greaterThan(this.vertexChains[vertex]!.point, yMaximum)) {
                    yMaximum = new Vector2Dd(this.vertexChains[vertex]!.point);
                    maximumPosition = p;
                }
                if (_Construct.lessThan(this.vertexChains[vertex]!.point, yMinimum))
                    yMinimum = new Vector2Dd(this.vertexChains[vertex]!.point);
                p = this.monotoneChainNodes[p]!.nextNodeIndex;
                vertexCount++;
            }
            if (processed) continue;
            if (vertexCount === 3) {
                output[this.nextOutputTriangleIndex++] = [
                    this.monotoneChainNodes[p]!.vertexIndex,
                    this.monotoneChainNodes[this.monotoneChainNodes[p]!.nextNodeIndex]!.vertexIndex,
                    this.monotoneChainNodes[this.monotoneChainNodes[p]!.previousNodeIndex]!.vertexIndex,
                ];
            } else {
                vertex = this.monotoneChainNodes[this.monotoneChainNodes[maximumPosition]!.nextNodeIndex]!.vertexIndex;
                this.triangulateSinglePolygon(
                    _numberOfVertices,
                    maximumPosition,
                    _Construct.equalTo(this.vertexChains[vertex]!.point, yMinimum) ? this.TRI_LHS : this.TRI_RHS,
                    output,
                );
            }
        }
        return this.nextOutputTriangleIndex;
    }

    private static triangulateSinglePolygon(
        vertexCount: number,
        maximumPosition: number,
        side: number,
        output: number[][],
    ): number {
        const reflexChain = new Int32Array(_Construct.SEGMENT_SIZE);
        let reflexIndex = 1,
            cursor: number,
            vertex: number,
            end: number;
        if (side === this.TRI_RHS) {
            reflexChain[0] = this.monotoneChainNodes[maximumPosition]!.vertexIndex;
            cursor = this.monotoneChainNodes[maximumPosition]!.nextNodeIndex;
            reflexChain[1] = this.monotoneChainNodes[cursor]!.vertexIndex;
            cursor = this.monotoneChainNodes[cursor]!.nextNodeIndex;
            vertex = this.monotoneChainNodes[cursor]!.vertexIndex;
            end = this.monotoneChainNodes[this.monotoneChainNodes[maximumPosition]!.previousNodeIndex]!.vertexIndex;
            if (end === 0) end = vertexCount;
        } else {
            cursor = this.monotoneChainNodes[maximumPosition]!.nextNodeIndex;
            reflexChain[0] = this.monotoneChainNodes[cursor]!.vertexIndex;
            cursor = this.monotoneChainNodes[cursor]!.nextNodeIndex;
            reflexChain[1] = this.monotoneChainNodes[cursor]!.vertexIndex;
            cursor = this.monotoneChainNodes[cursor]!.nextNodeIndex;
            vertex = this.monotoneChainNodes[cursor]!.vertexIndex;
            end = this.monotoneChainNodes[maximumPosition]!.vertexIndex;
        }
        while (vertex !== end || reflexIndex > 1) {
            if (
                reflexIndex > 0 &&
                _Construct.cross(
                    this.vertexChains[vertex]!.point,
                    this.vertexChains[reflexChain[reflexIndex - 1]!]!.point,
                    this.vertexChains[reflexChain[reflexIndex]!]!.point,
                ) > 0
            ) {
                output[this.nextOutputTriangleIndex++] = [
                    reflexChain[reflexIndex - 1]!,
                    reflexChain[reflexIndex]!,
                    vertex,
                ];
                reflexIndex--;
            } else {
                reflexChain[++reflexIndex] = vertex;
                cursor = this.monotoneChainNodes[cursor]!.nextNodeIndex;
                vertex = this.monotoneChainNodes[cursor]!.vertexIndex;
            }
        }
        output[this.nextOutputTriangleIndex++] = [reflexChain[reflexIndex - 1]!, reflexChain[reflexIndex]!, vertex];
        return 0;
    }
}
