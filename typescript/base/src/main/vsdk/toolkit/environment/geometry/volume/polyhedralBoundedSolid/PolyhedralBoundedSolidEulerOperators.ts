import { Vector3Dd } from "../../../../common/linealAlgebra/Vector3Dd.js";
import { PolyhedralBoundedSolid } from "./PolyhedralBoundedSolid.js";
import { _PolyhedralBoundedSolidEdge } from "./nodes/_PolyhedralBoundedSolidEdge.js";
import { _PolyhedralBoundedSolidFace } from "./nodes/_PolyhedralBoundedSolidFace.js";
import { _PolyhedralBoundedSolidHalfEdge } from "./nodes/_PolyhedralBoundedSolidHalfEdge.js";
import { _PolyhedralBoundedSolidLoop } from "./nodes/_PolyhedralBoundedSolidLoop.js";
import { _PolyhedralBoundedSolidVertex } from "./nodes/_PolyhedralBoundedSolidVertex.js";

/**
 * Foundational Euler operators for `PolyhedralBoundedSolid`, following
 * [MANT1988] chapters 9 and 11.  They preserve the circular half-edge-list
 * invariant, so higher-level topology editing can compose them safely.
 */
export class PolyhedralBoundedSolidEulerOperators {
    private constructor() {}

    /** Make the one-vertex/one-face skeletal model (§9.2.2). */
    public static mvfs(solid: PolyhedralBoundedSolid, point: Vector3Dd, vertexId: number, faceId: number): void {
        if (
            solid.getPolygonsList().length !== 0 ||
            solid.getEdgesList().length !== 0 ||
            solid.getVerticesList().length !== 0
        )
            throw new Error("mvfs requires an empty solid");
        solid.setMaxVertexId(Math.max(solid.getMaxVertexId(), vertexId));
        solid.setMaxFaceId(Math.max(solid.getMaxFaceId(), faceId));
        const face = new _PolyhedralBoundedSolidFace(solid, faceId);
        const loop = new _PolyhedralBoundedSolidLoop(face);
        const vertex = new _PolyhedralBoundedSolidVertex(solid, point, vertexId);
        const halfEdge = new _PolyhedralBoundedSolidHalfEdge(vertex, loop);
        loop.halfEdgesList.add(halfEdge);
        loop.boundaryStartHalfEdge = halfEdge;
        vertex.emanatingHalfEdge = halfEdge;
    }

    /** Inverse of `mvfs`; only the skeletal model may be destroyed. */
    public static kvfs(solid: PolyhedralBoundedSolid): void {
        if (
            solid.getPolygonsList().length !== 1 ||
            solid.getEdgesList().length !== 0 ||
            solid.getVerticesList().length !== 1
        )
            throw new Error("kvfs requires a skeletal solid");
        solid.setPolygonsList([]);
        solid.setVerticesList([]);
        solid.setMaxVertexId(-1);
        solid.setMaxFaceId(-1);
    }

    private static addHalfEdge(
        edge: _PolyhedralBoundedSolidEdge,
        vertex: _PolyhedralBoundedSolidVertex,
        before: _PolyhedralBoundedSolidHalfEdge,
        side: number,
    ): _PolyhedralBoundedSolidHalfEdge {
        const halfEdge =
            before.parentEdge === null ? before : new _PolyhedralBoundedSolidHalfEdge(vertex, before.parentLoop);
        if (halfEdge !== before) before.parentLoop.halfEdgesList.insertBefore(halfEdge, before);
        halfEdge.startingVertex = vertex;
        halfEdge.parentEdge = edge;
        if (side === PolyhedralBoundedSolid.PLUS) edge.leftHalf = halfEdge;
        else edge.rightHalf = halfEdge;
        return halfEdge;
    }

    /** Low-level make-edge-vertex (§9.2.3). */
    public static lmev(
        solid: PolyhedralBoundedSolid,
        first: _PolyhedralBoundedSolidHalfEdge,
        second: _PolyhedralBoundedSolidHalfEdge,
        vertexId: number,
        point: Vector3Dd,
    ): void {
        if (first === null || second === null || first.startingVertex !== second.startingVertex)
            throw new Error("lmev requires two half-edges starting at the same vertex");
        solid.setMaxVertexId(Math.max(solid.getMaxVertexId(), vertexId));
        const edge = new _PolyhedralBoundedSolidEdge(solid);
        const oldVertex = first.startingVertex;
        const newVertex = new _PolyhedralBoundedSolidVertex(solid, point, vertexId);
        if (first === second) {
            const oldHalf = this.addHalfEdge(edge, oldVertex, first, PolyhedralBoundedSolid.PLUS);
            const newHalf = this.addHalfEdge(edge, newVertex, first, PolyhedralBoundedSolid.MINUS);
            oldVertex.emanatingHalfEdge = oldHalf;
            newVertex.emanatingHalfEdge = newHalf;
            first.parentLoop.boundaryStartHalfEdge ??= first;
            return;
        }
        let cursor: _PolyhedralBoundedSolidHalfEdge | null = first;
        const visited = new Set<_PolyhedralBoundedSolidHalfEdge>();
        while (cursor !== null && cursor !== second && !visited.has(cursor)) {
            visited.add(cursor);
            cursor.startingVertex = newVertex;
            cursor = cursor.mirrorHalfEdge()?.next() ?? null;
        }
        if (cursor !== second) throw new Error("lmev could not traverse the vertex neighbourhood");
        const plus = this.addHalfEdge(edge, newVertex, second, PolyhedralBoundedSolid.PLUS);
        const minus = this.addHalfEdge(edge, oldVertex, first, PolyhedralBoundedSolid.MINUS);
        newVertex.emanatingHalfEdge = plus;
        oldVertex.emanatingHalfEdge = minus;
    }

    /** Low-level kill-edge-vertex (§9.2.3), the inverse of `lmev`. */
    public static lkev(
        solid: PolyhedralBoundedSolid,
        first: _PolyhedralBoundedSolidHalfEdge,
        second: _PolyhedralBoundedSolidHalfEdge,
    ): void {
        if (
            first === null ||
            second === null ||
            first === second ||
            first.parentEdge === null ||
            first.parentEdge !== second.parentEdge
        )
            throw new Error("lkev requires distinct mates of the same edge");
        const edge = first.parentEdge,
            removed = first.startingVertex,
            survivor = second.startingVertex;
        for (const face of solid.getPolygonsList())
            for (const loop of face.boundariesList)
                for (let index = 0; index < loop.halfEdgesList.size(); index++) {
                    const halfEdge = loop.halfEdgesList.get(index)!;
                    if (halfEdge.startingVertex === removed) halfEdge.startingVertex = survivor;
                }
        const firstLoop = first.parentLoop,
            secondLoop = second.parentLoop;
        firstLoop.unlistHalfEdge(first);
        secondLoop.unlistHalfEdge(second);
        solid.setEdgesList(solid.getEdgesList().filter((candidate) => candidate !== edge));
        solid.setVerticesList(solid.getVerticesList().filter((vertex) => vertex !== removed));
        if (secondLoop.halfEdgesList.size() === 0) {
            second.parentEdge = null;
            second.parentLoop = secondLoop;
            secondLoop.halfEdgesList.add(second);
            secondLoop.boundaryStartHalfEdge = second;
        }
        survivor.emanatingHalfEdge = secondLoop.boundaryStartHalfEdge;
    }

    /** Low-level kill-edge-face: merge the two faces separated by an edge. */
    public static lkef(
        solid: PolyhedralBoundedSolid,
        first: _PolyhedralBoundedSolidHalfEdge,
        second: _PolyhedralBoundedSolidHalfEdge,
    ): void {
        if (first === null || second === null || first.parentEdge === null || first.parentEdge !== second.parentEdge)
            throw new Error("lkef requires mates of one edge");
        if (first.parentLoop.parentFace === second.parentLoop.parentFace)
            throw new Error("lkef requires half-edges in different faces");
        const edge = first.parentEdge,
            retainedLoop = first.parentLoop,
            killedLoop = second.parentLoop,
            killedFace = killedLoop.parentFace as _PolyhedralBoundedSolidFace,
            pivot = first.next();
        if (pivot === null) throw new Error("lkef found a broken retained loop");
        const migrated = this.cycleFrom(second).slice(1);
        retainedLoop.unlistHalfEdge(first);
        killedLoop.unlistHalfEdge(second);
        solid.setEdgesList(solid.getEdgesList().filter((candidate) => candidate !== edge));
        const faceIndex = solid.getPolygonsList().indexOf(killedFace);
        if (faceIndex >= 0) solid.getPolygonsList().splice(faceIndex, 1);
        const loopIndex = killedFace.boundariesList.indexOf(killedLoop);
        if (loopIndex >= 0) killedFace.boundariesList.splice(loopIndex, 1);
        let insertionPivot = pivot;
        for (let index = migrated.length - 1; index >= 0; index--) {
            const halfEdge = migrated[index]!;
            if (halfEdge === second) continue;
            killedLoop.unlistHalfEdge(halfEdge);
            halfEdge.parentLoop = retainedLoop;
            retainedLoop.halfEdgesList.insertBefore(halfEdge, insertionPivot);
            insertionPivot = halfEdge;
        }
        for (const loop of [...killedFace.boundariesList]) {
            const index = killedFace.boundariesList.indexOf(loop);
            if (index >= 0) killedFace.boundariesList.splice(index, 1);
            loop.parentFace = retainedLoop.parentFace;
            retainedLoop.parentFace.boundariesList.push(loop);
        }
        retainedLoop.boundaryStartHalfEdge = retainedLoop.halfEdgesList.get(0);
    }

    /** Low-level kill-edge-make-ring: split a loop into its two components. */
    public static lkemr(
        solid: PolyhedralBoundedSolid,
        first: _PolyhedralBoundedSolidHalfEdge,
        second: _PolyhedralBoundedSolidHalfEdge,
    ): void {
        if (
            first === null ||
            second === null ||
            first.parentEdge === null ||
            first.parentEdge !== second.parentEdge ||
            first.parentLoop !== second.parentLoop
        )
            throw new Error("lkemr requires mates in one loop");
        const oldLoop = first.parentLoop,
            edge = first.parentEdge,
            newLoop = new _PolyhedralBoundedSolidLoop(oldLoop.parentFace);
        const migrated = this.cycleAfter(first, second);
        for (const halfEdge of migrated) {
            oldLoop.unlistHalfEdge(halfEdge);
            halfEdge.parentLoop = newLoop;
            newLoop.halfEdgesList.add(halfEdge);
        }
        oldLoop.unlistHalfEdge(first);
        newLoop.unlistHalfEdge(second);
        solid.setEdgesList(solid.getEdgesList().filter((candidate) => candidate !== edge));
        this.ensureDegenerateLoopAnchor(oldLoop);
        this.ensureDegenerateLoopAnchor(newLoop);
    }

    /** Low-level make-edge-kill-ring: bridge two loops belonging to one face. */
    public static lmekr(
        solid: PolyhedralBoundedSolid,
        first: _PolyhedralBoundedSolidHalfEdge,
        second: _PolyhedralBoundedSolidHalfEdge,
    ): void {
        if (
            first === null ||
            second === null ||
            first.parentLoop === second.parentLoop ||
            first.parentLoop.parentFace !== second.parentLoop.parentFace
        )
            throw new Error("lmekr requires distinct loops on one face");
        const retained = first.parentLoop,
            killed = second.parentLoop,
            edge = new _PolyhedralBoundedSolidEdge(solid),
            migrated = this.cycleFrom(second);
        const bridgeIntoSecond = new _PolyhedralBoundedSolidHalfEdge(first.startingVertex, retained);
        const bridgeIntoFirst = new _PolyhedralBoundedSolidHalfEdge(second.startingVertex, retained);
        bridgeIntoSecond.parentEdge = edge;
        bridgeIntoFirst.parentEdge = edge;
        edge.rightHalf = bridgeIntoSecond;
        edge.leftHalf = bridgeIntoFirst;
        retained.halfEdgesList.insertBefore(bridgeIntoSecond, first);
        for (const halfEdge of migrated) {
            killed.unlistHalfEdge(halfEdge);
            halfEdge.parentLoop = retained;
            retained.halfEdgesList.insertBefore(halfEdge, first);
        }
        retained.halfEdgesList.insertBefore(bridgeIntoFirst, first);
        const index = retained.parentFace.boundariesList.indexOf(killed);
        if (index >= 0) retained.parentFace.boundariesList.splice(index, 1);
        retained.boundaryStartHalfEdge = retained.halfEdgesList.get(0);
    }

    /** Move/reorder a ring while maintaining outer-loop-at-index-zero convention. */
    public static lringmv(
        _solid: PolyhedralBoundedSolid,
        loop: _PolyhedralBoundedSolidLoop,
        destination: _PolyhedralBoundedSolidFace,
        setAsOuterLoop: boolean,
    ): boolean {
        if (loop === null || destination === null || loop.parentFace === null) return false;
        const source = loop.parentFace;
        if (source === destination) {
            const index = destination.boundariesList.indexOf(loop);
            if (index < 0 || (!setAsOuterLoop && destination.boundariesList.length <= 1)) return false;
            destination.boundariesList.splice(index, 1);
            destination.boundariesList.splice(
                setAsOuterLoop ? 0 : Math.min(1, destination.boundariesList.length),
                0,
                loop,
            );
            return true;
        }
        if (source.boundariesList.length <= 1 || (!setAsOuterLoop && destination.boundariesList.length === 0))
            return false;
        const index = source.boundariesList.indexOf(loop);
        if (index < 0) return false;
        source.boundariesList.splice(index, 1);
        loop.parentFace = destination;
        destination.boundariesList.splice(setAsOuterLoop ? 0 : destination.boundariesList.length, 0, loop);
        return true;
    }

    public static lkfmrh(
        solid: PolyhedralBoundedSolid,
        destination: _PolyhedralBoundedSolidFace,
        source: _PolyhedralBoundedSolidFace,
    ): void {
        if (source.boundariesList.length !== 1) throw new Error("lkfmrh source must have one loop");
        const loop = source.boundariesList[0]!;
        source.boundariesList.splice(0, 1);
        loop.parentFace = destination;
        destination.boundariesList.push(loop);
        solid.setPolygonsList(solid.getPolygonsList().filter((face) => face !== source));
    }
    public static lkimrh(
        solid: PolyhedralBoundedSolid,
        destination: _PolyhedralBoundedSolidFace,
        source: _PolyhedralBoundedSolidFace,
    ): void {
        this.lkfmrh(solid, destination, source);
    }
    public static lmfkrh(
        solid: PolyhedralBoundedSolid,
        loop: _PolyhedralBoundedSolidLoop,
        faceId: number,
    ): _PolyhedralBoundedSolidFace {
        const old = loop.parentFace,
            index = old.boundariesList.indexOf(loop);
        if (index < 0) throw new Error("lmfkrh loop is not owned by its face");
        old.boundariesList.splice(index, 1);
        const face = new _PolyhedralBoundedSolidFace(solid, faceId);
        loop.parentFace = face;
        face.boundariesList.push(loop);
        solid.setMaxFaceId(Math.max(solid.getMaxFaceId(), faceId));
        return face;
    }
    public static lmikrh(
        solid: PolyhedralBoundedSolid,
        loop: _PolyhedralBoundedSolidLoop,
        faceId: number,
    ): _PolyhedralBoundedSolidFace {
        return this.lmfkrh(solid, loop, faceId);
    }

    /** Low-level make-edge-face (§9.2.3): split a loop and its face in two. */
    public static lmef(
        solid: PolyhedralBoundedSolid,
        first: _PolyhedralBoundedSolidHalfEdge,
        second: _PolyhedralBoundedSolidHalfEdge,
        faceId: number,
    ): _PolyhedralBoundedSolidFace {
        if (first === null || second === null || first.parentLoop !== second.parentLoop)
            throw new Error("lmef requires half-edges in the same loop");
        solid.setMaxFaceId(Math.max(solid.getMaxFaceId(), faceId));
        const oldLoop = first.parentLoop;
        const face = new _PolyhedralBoundedSolidFace(solid, faceId);
        const loop = new _PolyhedralBoundedSolidLoop(face);
        for (let cursor: _PolyhedralBoundedSolidHalfEdge | null = first; cursor !== second;) {
            if (cursor === null) throw new Error("lmef found a broken loop");
            const next = cursor.next();
            oldLoop.unlistHalfEdge(cursor);
            cursor.parentLoop = loop;
            loop.halfEdgesList.add(cursor);
            cursor = next;
        }
        const edge = new _PolyhedralBoundedSolidEdge(solid);
        const intoNew = this.addHalfEdge(
            edge,
            second.startingVertex,
            loop.halfEdgesList.get(0) ?? second,
            PolyhedralBoundedSolid.MINUS,
        );
        const intoOld = this.addHalfEdge(edge, first.startingVertex, second, PolyhedralBoundedSolid.PLUS);
        loop.boundaryStartHalfEdge = intoNew;
        oldLoop.boundaryStartHalfEdge = intoOld;
        return face;
    }

    public static smev(
        solid: PolyhedralBoundedSolid,
        faceId: number,
        vertexId: number,
        newVertexId: number,
        point: Vector3Dd,
    ): boolean {
        const halfEdge = solid.findFace(faceId)?.findHalfEdge(vertexId);
        if (halfEdge === null || halfEdge === undefined) return false;
        this.lmev(solid, halfEdge, halfEdge, newVertexId, point);
        return true;
    }
    public static mev(
        solid: PolyhedralBoundedSolid,
        f1: number,
        f2: number,
        v1: number,
        v2: number,
        v3: number,
        v4: number,
        point: Vector3Dd,
    ): boolean {
        const first = solid.findFace(f1)?.findHalfEdge(v1, v2),
            second = solid.findFace(f2)?.findHalfEdge(v1, v3);
        if (first === null || first === undefined || second === null || second === undefined) return false;
        this.lmev(solid, first, second, v4, point);
        return true;
    }
    public static smef(
        solid: PolyhedralBoundedSolid,
        faceId: number,
        firstVertex: number,
        secondVertex: number,
        newFaceId: number,
    ): boolean {
        const face = solid.findFace(faceId),
            first = face?.findHalfEdge(firstVertex),
            second = face?.findHalfEdge(secondVertex);
        if (first === null || first === undefined || second === null || second === undefined) return false;
        this.lmef(solid, first, second, newFaceId);
        return true;
    }
    public static mef(
        solid: PolyhedralBoundedSolid,
        f1: number,
        f2: number,
        v1: number,
        v2: number,
        v3: number,
        v4: number,
        newFaceId: number,
    ): boolean {
        const first = solid.findFace(f1)?.findHalfEdge(v1, v2),
            second = solid.findFace(f2)?.findHalfEdge(v3, v4);
        if (first === null || first === undefined || second === null || second === undefined) return false;
        this.lmef(solid, first, second, newFaceId);
        return true;
    }
    public static kemr(
        solid: PolyhedralBoundedSolid,
        f1: number,
        f2: number,
        v1: number,
        v2: number,
        v3: number,
        v4: number,
    ): boolean {
        const first = solid.findFace(f1)?.findHalfEdge(v1, v2),
            second = solid.findFace(f2)?.findHalfEdge(v3, v4);
        if (first === null || first === undefined || second === null || second === undefined) return false;
        this.lkemr(solid, first, second);
        return true;
    }
    public static kfmrh(solid: PolyhedralBoundedSolid, destinationFaceId: number, sourceFaceId: number): boolean {
        const destination = solid.findFace(destinationFaceId),
            source = solid.findFace(sourceFaceId);
        if (destination === null || source === null) return false;
        this.lkfmrh(solid, destination, source);
        return true;
    }

    private static cycleFrom(start: _PolyhedralBoundedSolidHalfEdge): _PolyhedralBoundedSolidHalfEdge[] {
        const result: _PolyhedralBoundedSolidHalfEdge[] = [];
        let cursor: _PolyhedralBoundedSolidHalfEdge | null = start;
        do {
            if (cursor === null || result.includes(cursor)) throw new Error("Broken circular half-edge list");
            result.push(cursor);
            cursor = cursor.next();
        } while (cursor !== start);
        return result;
    }
    private static cycleAfter(
        start: _PolyhedralBoundedSolidHalfEdge,
        until: _PolyhedralBoundedSolidHalfEdge,
    ): _PolyhedralBoundedSolidHalfEdge[] {
        const result: _PolyhedralBoundedSolidHalfEdge[] = [];
        let cursor = start.next();
        while (cursor !== null) {
            result.push(cursor);
            if (cursor === until) return result;
            cursor = cursor.next();
            if (cursor === start) break;
        }
        if (result[result.length - 1] !== until) throw new Error("Half-edge is not reachable in its loop");
        return result;
    }
    private static ensureDegenerateLoopAnchor(loop: _PolyhedralBoundedSolidLoop): void {
        if (loop.halfEdgesList.size() === 0) throw new Error("Euler operation produced an empty loop");
        loop.boundaryStartHalfEdge = loop.halfEdgesList.get(0);
        if (loop.halfEdgesList.size() === 1) {
            const halfEdge = loop.boundaryStartHalfEdge!;
            halfEdge.parentEdge = null;
            halfEdge.startingVertex.emanatingHalfEdge = null;
        }
    }
}
