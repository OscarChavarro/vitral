import { PolyhedralBoundedSolidNumericPolicy, type ToleranceContext } from "./PolyhedralBoundedSolidNumericPolicy.js";
import { PolyhedralBoundedSolid } from "./PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidEulerOperators } from "./PolyhedralBoundedSolidEulerOperators.js";
import { _PolyhedralBoundedSolidFace } from "./nodes/_PolyhedralBoundedSolidFace.js";
import { _PolyhedralBoundedSolidLoop } from "./nodes/_PolyhedralBoundedSolidLoop.js";

/** Topology-cleanup operations built on the Euler operators ([MANT1988].12). */
export class PolyhedralBoundedSolidTopologyEditing {
    private constructor() {}

    /** Re-establish owning-face and boundary-start references after topology surgery. */
    public static remakeLoopBoundaryStartHalfEdgesReferences(solid: PolyhedralBoundedSolid): void {
        for (const face of solid.getPolygonsList())
            for (const loop of face.boundariesList) {
                loop.parentFace = face;
                loop.boundaryStartHalfEdge = loop.halfEdgesList.size() === 0 ? null : loop.halfEdgesList.get(0);
            }
    }

    /** Assign consecutive ids, starting at one, to every addressable B-rep element. */
    public static compactIds(solid: PolyhedralBoundedSolid): void {
        solid.getVerticesList().forEach((vertex, index) => {
            vertex.id = index + 1;
        });
        solid.getEdgesList().forEach((edge, index) => {
            edge.id = index + 1;
        });
        let halfEdgeId = 1;
        solid.getPolygonsList().forEach((face, faceIndex) => {
            face.id = faceIndex + 1;
            for (const loop of face.boundariesList)
                for (let index = 0; index < loop.halfEdgesList.size(); index++)
                    loop.halfEdgesList.get(index)!.id = halfEdgeId++;
        });
        solid.setMaxVertexId(solid.getVerticesList().length);
        solid.setMaxFaceId(solid.getPolygonsList().length);
        this.remakeLoopBoundaryStartHalfEdgesReferences(solid);
    }

    /** Glue two coincident boundaries of a face, using the same bridge/remove sequence as [MANT1988].12.4.2. */
    public static loopGlue(solid: PolyhedralBoundedSolid, faceOrId: _PolyhedralBoundedSolidFace | number): void {
        const face = typeof faceOrId === "number" ? solid.findFace(faceOrId) : faceOrId;
        if (face === null || face.boundariesList.length < 2) return;
        const context = PolyhedralBoundedSolidNumericPolicy.forSolid(solid);
        let pair: null | [_PolyhedralBoundedSolidLoop, _PolyhedralBoundedSolidLoop, number, number] = null;
        for (let a = 0; a < face.boundariesList.length && !pair; a++)
            for (let b = a + 1; b < face.boundariesList.length && !pair; b++) {
                const first = face.boundariesList[a]!,
                    second = face.boundariesList[b]!;
                for (let i = 0; i < first.halfEdgesList.size() && !pair; i++)
                    for (let j = 0; j < second.halfEdgesList.size(); j++)
                        if (
                            PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                                first.halfEdgesList.get(i)!.startingVertex.position,
                                second.halfEdgesList.get(j)!.startingVertex.position,
                                context,
                            )
                        )
                            pair = [first, second, i, j];
            }
        if (pair === null) return;
        const [first, second, firstIndex, secondIndex] = pair,
            firstHalf = first.halfEdgesList.get(firstIndex)!,
            secondHalf = second.halfEdgesList.get(secondIndex)!;
        if (first.halfEdgesList.size() < 3 || second.halfEdgesList.size() < 3) {
            for (const loop of [first, second]) {
                const index = face.boundariesList.indexOf(loop);
                if (index >= 0) face.boundariesList.splice(index, 1);
            }
            this.remakeLoopBoundaryStartHalfEdgesReferences(solid);
            return;
        }
        PolyhedralBoundedSolidEulerOperators.lmekr(solid, firstHalf, secondHalf);
        this.remakeLoopBoundaryStartHalfEdgesReferences(solid);
    }

    /** Conservative maximal-face cleanup: only applies local reductions whose preconditions can be proven from topology and plane equations. */
    public static maximizeFaces(solid: PolyhedralBoundedSolid): void {
        for (let pass = 0; pass < solid.getEdgesList().length + solid.getVerticesList().length + 1; pass++) {
            const context = PolyhedralBoundedSolidNumericPolicy.forSolid(solid);
            const nullEdge = solid
                .getEdgesList()
                .find(
                    (edge) =>
                        edge.leftHalf !== null &&
                        edge.rightHalf !== null &&
                        PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                            edge.leftHalf.startingVertex.position,
                            edge.rightHalf.startingVertex.position,
                            context,
                        ),
                );
            if (nullEdge?.leftHalf && nullEdge.rightHalf) {
                PolyhedralBoundedSolidEulerOperators.lkev(solid, nullEdge.leftHalf, nullEdge.rightHalf);
                continue;
            }
            let changed = false;
            for (const edge of [...solid.getEdgesList()]) {
                const left = edge.leftHalf,
                    right = edge.rightHalf;
                if (left === null || right === null) continue;
                const leftFace = left.parentLoop.parentFace as _PolyhedralBoundedSolidFace,
                    rightFace = right.parentLoop.parentFace as _PolyhedralBoundedSolidFace;
                if (
                    leftFace === rightFace &&
                    left.parentLoop === right.parentLoop &&
                    (left.next() === right || right.next() === left)
                ) {
                    PolyhedralBoundedSolidEulerOperators.lkev(solid, left, right);
                    changed = true;
                    break;
                }
                const a = leftFace.getContainingPlane(),
                    b = rightFace.getContainingPlane();
                if (leftFace !== rightFace && a !== null && b !== null && a.overlapsWith(b, context.epsilon())) {
                    PolyhedralBoundedSolidEulerOperators.lkef(solid, left, right);
                    changed = true;
                    break;
                }
            }
            if (!changed) break;
        }
        this.remakeLoopBoundaryStartHalfEdgesReferences(solid);
    }

    /** Collapse zero-length topological edges, restarting after each Euler edit. */
    public static weldCoincidentVertices(
        solid: PolyhedralBoundedSolid,
        context: ToleranceContext = PolyhedralBoundedSolidNumericPolicy.forPoints(
            solid.getVerticesList().map((vertex) => vertex.position),
        ),
    ): number {
        let count = 0;
        for (;;) {
            const edge = solid
                .getEdgesList()
                .find(
                    (candidate) =>
                        candidate.leftHalf !== null &&
                        candidate.rightHalf !== null &&
                        PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                            candidate.leftHalf.startingVertex.position,
                            candidate.rightHalf.startingVertex.position,
                            context,
                        ),
                );
            if (edge === undefined || edge.leftHalf === null || edge.rightHalf === null) break;
            PolyhedralBoundedSolidEulerOperators.lkev(solid, edge.leftHalf, edge.rightHalf);
            count++;
        }
        this.remakeLoopBoundaryStartHalfEdgesReferences(solid);
        return count;
    }
}
