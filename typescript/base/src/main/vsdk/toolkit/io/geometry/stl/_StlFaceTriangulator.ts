import { IllegalStateException } from "../../../../../java/lang/IllegalStateException.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { MonotoneDecompositionTriangulator } from "../../../environment/geometry/geometricProcessing/polygonTriangulation/MonotoneDecompositionTriangulator.js";
import type { InfinitePlane } from "../../../environment/geometry/surface/InfinitePlane.js";
import { Polygon2D } from "../../../environment/geometry/surface/polygon/Polygon2D.js";
import type { PolyhedralBoundedSolid } from "../../../environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import {
    PolyhedralBoundedSolidNumericPolicy,
    type ToleranceContext,
} from "../../../environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import type { _PolyhedralBoundedSolidFace } from "../../../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "../../../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import type { _PolyhedralBoundedSolidLoop } from "../../../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";
import { _StlFacetEmitter } from "./_StlFacetEmitter.js";

class ProjectedVertex {
    public constructor(
        public readonly original: Vector3Dd,
        public readonly x: number,
        public readonly y: number,
    ) {}
}

class FaceBasis {
    public constructor(
        public readonly origin: Vector3Dd,
        public readonly u: Vector3Dd,
        public readonly v: Vector3Dd,
        public readonly normal: Vector3Dd,
    ) {}
}

/**
Port of `vsdk.toolkit.io.geometry.stl._StlFaceTriangulator`, a package-private
class of the Java module, with its two private nested classes `ProjectedVertex`
and `FaceBasis` as module-private classes: each face is projected onto an
orthonormal basis of its containing plane, triangulated by the ported
`MonotoneDecompositionTriangulator`, and every triangle is lifted back to the
original vertices, flipped when it disagrees with the face normal.
*/
export class _StlFaceTriangulator {
    private constructor() {}

    public static triangulateSolid(solid: PolyhedralBoundedSolid): _StlFacetEmitter.Facet[] {
        const facets: _StlFacetEmitter.Facet[] = [];
        let i: number;
        for (i = 0; i < solid.getPolygonsList().size(); i++) {
            facets.push(..._StlFaceTriangulator.triangulateFace(solid.getPolygonsList().get(i)!));
        }
        return facets;
    }

    private static triangulateFace(face: _PolyhedralBoundedSolidFace): _StlFacetEmitter.Facet[] {
        const numericContext: ToleranceContext = PolyhedralBoundedSolidNumericPolicy.forFace(face);
        const plane: InfinitePlane | null = face.getContainingPlane();
        if (plane === null) {
            throw new IllegalStateException("STL export rejected: face " + face.id + " has no containing plane");
        }

        const basis: FaceBasis = _StlFaceTriangulator.buildBasis(face, plane);
        const flattenedVertices: ProjectedVertex[] = [];
        const originalVertices: Vector3Dd[] = [];
        const polygon: Polygon2D = _StlFaceTriangulator.buildProjectedPolygon(
            face,
            basis,
            numericContext,
            flattenedVertices,
            originalVertices,
        );

        const triangles: MonotoneDecompositionTriangulator.Triangle[] = [];
        const triangulator = new MonotoneDecompositionTriangulator();
        const triangleCount: number = triangulator.triangulate(polygon, triangles);
        if (triangleCount <= 0 || triangles.length === 0) {
            throw new IllegalStateException(
                "STL export rejected: face " + face.id + " triangulation produced no triangles",
            );
        }

        const facets: _StlFacetEmitter.Facet[] = [];
        let i: number;
        for (i = 0; i < triangles.length; i++) {
            const triangle: MonotoneDecompositionTriangulator.Triangle = triangles[i]!;
            facets.push(
                _StlFaceTriangulator.buildFacet(face, triangle, originalVertices, basis.normal, numericContext, i),
            );
        }
        return facets;
    }

    private static buildBasis(face: _PolyhedralBoundedSolidFace, plane: InfinitePlane): FaceBasis {
        const normal: Vector3Dd = plane.getNormal().normalized();
        const origin: Vector3Dd = _StlFaceTriangulator.chooseFaceAnchor(face);
        const referenceAxis: Vector3Dd = _StlFaceTriangulator.chooseReferenceAxis(normal);
        const u: Vector3Dd = referenceAxis.crossProduct(normal).normalized();
        const v: Vector3Dd = normal.crossProduct(u).normalized();
        return new FaceBasis(origin, u, v, normal);
    }

    private static buildProjectedPolygon(
        face: _PolyhedralBoundedSolidFace,
        basis: FaceBasis,
        numericContext: ToleranceContext,
        flattenedVertices: ProjectedVertex[],
        originalVertices: Vector3Dd[],
    ): Polygon2D {
        const polygon = new Polygon2D();
        polygon.loops.length = 0;

        let i: number;
        for (i = 0; i < face.boundariesList.size(); i++) {
            const loop: _PolyhedralBoundedSolidLoop | null = face.boundariesList.get(i);
            if (loop === null || loop.boundaryStartHalfEdge === null) {
                throw new IllegalStateException(
                    "STL export rejected: face " + face.id + " loop " + i + " has no traversable half-edge",
                );
            }

            const loopVertices: ProjectedVertex[] = _StlFaceTriangulator.collectLoopVertices(face, i, loop, basis);
            if (loopVertices.length < 3) {
                throw new IllegalStateException(
                    "STL export rejected: face " + face.id + " loop " + i + " has fewer than 3 vertices",
                );
            }

            const area: number = _StlFaceTriangulator.signedArea(loopVertices);
            if (Math.abs(area) <= PolyhedralBoundedSolidNumericPolicy.areaTolerance2D(numericContext)) {
                throw new IllegalStateException(
                    "STL export rejected: face " + face.id + " loop " + i + " has near-zero projected area",
                );
            }

            polygon.nextLoop();
            let j: number;
            for (j = 0; j < loopVertices.length; j++) {
                const vertex: ProjectedVertex = loopVertices[j]!;
                polygon.addVertex(vertex.x, vertex.y);
                flattenedVertices.push(vertex);
                originalVertices.push(vertex.original);
            }
        }

        if (polygon.loops.length !== 0 && polygon.loops[0]!.vertices.length === 0) {
            polygon.eraseLastLoop();
        }
        return polygon;
    }

    private static collectLoopVertices(
        face: _PolyhedralBoundedSolidFace,
        loopIndex: number,
        loop: _PolyhedralBoundedSolidLoop,
        basis: FaceBasis,
    ): ProjectedVertex[] {
        const vertices: ProjectedVertex[] = [];
        const start: _PolyhedralBoundedSolidHalfEdge | null = loop.boundaryStartHalfEdge;
        let current: _PolyhedralBoundedSolidHalfEdge | null = start;
        do {
            if (current === null || current.startingVertex === null || current.startingVertex.position === null) {
                throw new IllegalStateException(
                    "STL export rejected: face " + face.id + " loop " + loopIndex + " is not traversable",
                );
            }
            const point: Vector3Dd = current.startingVertex.position;
            const delta: Vector3Dd = point.subtract(basis.origin);
            vertices.push(new ProjectedVertex(point, delta.dotProduct(basis.u), delta.dotProduct(basis.v)));
            current = current.next();
            if (current === null) {
                throw new IllegalStateException(
                    "STL export rejected: face " + face.id + " loop " + loopIndex + " is not closed",
                );
            }
        } while (current !== start);
        return vertices;
    }

    private static buildFacet(
        face: _PolyhedralBoundedSolidFace,
        triangle: MonotoneDecompositionTriangulator.Triangle,
        originalVertices: Vector3Dd[],
        faceNormal: Vector3Dd,
        numericContext: ToleranceContext,
        triangleIndex: number,
    ): _StlFacetEmitter.Facet {
        _StlFaceTriangulator.validateTriangleIndices(face, triangle, originalVertices.length);

        const a: Vector3Dd = originalVertices[triangle.point0]!;
        let b: Vector3Dd = originalVertices[triangle.point1]!;
        let c: Vector3Dd = originalVertices[triangle.point2]!;

        if (
            !PolyhedralBoundedSolidNumericPolicy.pointsSeparated(a, b, numericContext) ||
            !PolyhedralBoundedSolidNumericPolicy.pointsSeparated(b, c, numericContext) ||
            !PolyhedralBoundedSolidNumericPolicy.pointsSeparated(a, c, numericContext)
        ) {
            throw new IllegalStateException(
                "STL export rejected: face " +
                    face.id +
                    " triangulation produced " +
                    "a repeated-vertex triangle at index " +
                    triangleIndex,
            );
        }

        let normal: Vector3Dd = b.subtract(a).crossProduct(c.subtract(a));
        const normalLength: number = normal.length();
        if (normalLength <= PolyhedralBoundedSolidNumericPolicy.areaTolerance2D(numericContext)) {
            throw new IllegalStateException(
                "STL export rejected: face " +
                    face.id +
                    " triangulation produced " +
                    "a zero-area triangle at index " +
                    triangleIndex,
            );
        }
        normal = normal.multiply(1.0 / normalLength);

        if (normal.dotProduct(faceNormal) < 0.0) {
            const temp: Vector3Dd = b;
            b = c;
            c = temp;
            normal = b.subtract(a).crossProduct(c.subtract(a)).normalized();
            if (normal.dotProduct(faceNormal) < 0.0) {
                normal = faceNormal;
            }
        }

        return new _StlFacetEmitter.Facet(normal, a, b, c);
    }

    private static validateTriangleIndices(
        face: _PolyhedralBoundedSolidFace,
        triangle: MonotoneDecompositionTriangulator.Triangle,
        vertexCount: number,
    ): void {
        if (
            triangle.point0 < 0 ||
            triangle.point0 >= vertexCount ||
            triangle.point1 < 0 ||
            triangle.point1 >= vertexCount ||
            triangle.point2 < 0 ||
            triangle.point2 >= vertexCount
        ) {
            throw new IllegalStateException(
                "STL export rejected: face " + face.id + " triangulation returned an out-of-range index",
            );
        }
    }

    private static signedArea(vertices: ProjectedVertex[]): number {
        let areaTwice = 0.0;
        let i: number;
        for (i = 0; i < vertices.length; i++) {
            const current: ProjectedVertex = vertices[i]!;
            const next: ProjectedVertex = vertices[(i + 1) % vertices.length]!;
            areaTwice += current.x * next.y - next.x * current.y;
        }
        return areaTwice * 0.5;
    }

    private static chooseFaceAnchor(face: _PolyhedralBoundedSolidFace): Vector3Dd {
        let i: number;
        for (i = 0; i < face.boundariesList.size(); i++) {
            const loop: _PolyhedralBoundedSolidLoop | null = face.boundariesList.get(i);
            if (
                loop !== null &&
                loop.boundaryStartHalfEdge !== null &&
                loop.boundaryStartHalfEdge.startingVertex !== null
            ) {
                return loop.boundaryStartHalfEdge.startingVertex.position;
            }
        }
        throw new IllegalStateException("STL export rejected: face " + face.id + " has no anchor vertex");
    }

    private static chooseReferenceAxis(normal: Vector3Dd): Vector3Dd {
        const ax: number = Math.abs(normal.x());
        const ay: number = Math.abs(normal.y());
        const az: number = Math.abs(normal.z());
        if (ax <= ay && ax <= az) {
            return new Vector3Dd(1.0, 0.0, 0.0);
        }
        if (ay <= az) {
            return new Vector3Dd(0.0, 1.0, 0.0);
        }
        return new Vector3Dd(0.0, 0.0, 1.0);
    }
}
