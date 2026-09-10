import { Vector3Dd } from "../../../../common/linealAlgebra/Vector3Dd.js";
import { PolyhedralBoundedSolid } from "./PolyhedralBoundedSolid.js";
import { _PolyhedralBoundedSolidEdge } from "./nodes/_PolyhedralBoundedSolidEdge.js";
import { _PolyhedralBoundedSolidFace } from "./nodes/_PolyhedralBoundedSolidFace.js";
import { _PolyhedralBoundedSolidHalfEdge } from "./nodes/_PolyhedralBoundedSolidHalfEdge.js";
import { _PolyhedralBoundedSolidLoop } from "./nodes/_PolyhedralBoundedSolidLoop.js";
import { _PolyhedralBoundedSolidVertex } from "./nodes/_PolyhedralBoundedSolidVertex.js";

/** Builds a closed half-edge B-rep directly from consistently oriented polygon indices. */
export class PolyhedralBoundedSolidBuilder {
    private constructor() {}
    public static fromPolygons(
        points: readonly Vector3Dd[],
        polygons: readonly (readonly number[])[],
    ): PolyhedralBoundedSolid {
        const solid = new PolyhedralBoundedSolid();
        const vertices = points.map((point, index) => new _PolyhedralBoundedSolidVertex(solid, point, index + 1));
        const directed = new Map<string, _PolyhedralBoundedSolidHalfEdge>();
        for (let faceIndex = 0; faceIndex < polygons.length; faceIndex++) {
            const polygon = polygons[faceIndex]!;
            if (polygon.length < 3) throw new Error("A B-rep face requires at least three vertices");
            const face = new _PolyhedralBoundedSolidFace(solid, faceIndex + 1);
            const loop = new _PolyhedralBoundedSolidLoop(face);
            for (const index of polygon) {
                const vertex = vertices[index];
                if (vertex === undefined)
                    throw new Error(`Face ${faceIndex + 1} references an unknown vertex ${index}`);
                const halfEdge = new _PolyhedralBoundedSolidHalfEdge(vertex, loop);
                loop.halfEdgesList.add(halfEdge);
                vertex.emanatingHalfEdge ??= halfEdge;
            }
            loop.boundaryStartHalfEdge = loop.halfEdgesList.get(0);
            for (let index = 0; index < polygon.length; index++) {
                const halfEdge = loop.halfEdgesList.get(index)!;
                const a = polygon[index]!,
                    b = polygon[(index + 1) % polygon.length]!;
                const reverse = directed.get(`${b}:${a}`);
                if (reverse !== undefined) {
                    if (reverse.parentEdge !== null) throw new Error("A B-rep edge may have only two incident faces");
                    halfEdge.parentEdge = reverse.parentEdge = new _PolyhedralBoundedSolidEdge(solid);
                    halfEdge.parentEdge.leftHalf = halfEdge;
                    halfEdge.parentEdge.rightHalf = reverse;
                } else directed.set(`${a}:${b}`, halfEdge);
            }
        }
        for (const halfEdge of directed.values())
            if (halfEdge.parentEdge === null) throw new Error("Polygons do not form a closed oriented solid");
        solid.setMaxVertexId(vertices.length);
        solid.setMaxFaceId(polygons.length);
        return solid;
    }
}
