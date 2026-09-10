import { PolyhedralBoundedSolid } from "./PolyhedralBoundedSolid.js";
/** Topological validation helpers for the half-edge representation of [MANT1988].10.2.1–10.2.2. */
export class _PolyhedralBoundedSolidTopologicalValidator {
    public static validateTopologicalIntegrity(solid: PolyhedralBoundedSolid): boolean {
        const counts = new Map(solid.getEdgesList().map((edge) => [edge, 0]));
        for (const face of solid.getPolygonsList())
            for (const loop of face.boundariesList) {
                if (loop.boundaryStartHalfEdge === null) return false;
                for (let i = 0; i < loop.halfEdgesList.size(); i++) {
                    const half = loop.halfEdgesList.get(i)!;
                    if (
                        half.next() === null ||
                        half.parentLoop !== loop ||
                        half.parentEdge === null ||
                        !counts.has(half.parentEdge)
                    )
                        return false;
                    counts.set(half.parentEdge, counts.get(half.parentEdge)! + 1);
                }
            }
        return [...counts.entries()].every(
            ([edge, count]) => count === 2 && edge.leftHalf !== null && edge.rightHalf !== null,
        );
    }
    public static remakeEmanatingHalfedgesReferences(solid: PolyhedralBoundedSolid): void {
        for (const vertex of solid.getVerticesList()) vertex.emanatingHalfEdge = null;
        for (const face of solid.getPolygonsList())
            for (const loop of face.boundariesList)
                for (let i = 0; i < loop.halfEdgesList.size(); i++) {
                    const half = loop.halfEdgesList.get(i)!;
                    half.startingVertex.emanatingHalfEdge = half;
                }
        solid.setVerticesList(solid.getVerticesList().filter((vertex) => vertex.emanatingHalfEdge !== null));
    }
}
