//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import { IllegalArgumentException } from "../../../../../../java/lang/IllegalArgumentException.js";
import { IllegalStateException } from "../../../../../../java/lang/IllegalStateException.js";
import { ArrayList } from "../../../../../../java/util/ArrayList.js";
import type { List } from "../../../../../../java/util/List.js";
import type { Vector3Dd } from "../../../../common/linealAlgebra/Vector3Dd.js";
import { PolyhedralBoundedSolid } from "./PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidEulerOperators } from "./PolyhedralBoundedSolidEulerOperators.js";
import { PolyhedralBoundedSolidValidationEngine } from "./PolyhedralBoundedSolidValidationEngine.js";
import type { _PolyhedralBoundedSolidFace } from "./nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "./nodes/_PolyhedralBoundedSolidHalfEdge.js";
import type { _PolyhedralBoundedSolidVertex } from "./nodes/_PolyhedralBoundedSolidVertex.js";

/**
Builds the boundary representation of a closed polygon mesh with Euler
operators only, so it can be used for any solid whose boundary is known as a
list of polygons (i.e. the polygonization of an implicit surface), of any
genus, and with any number of connected components (one shell each).

The mesh must be a closed, oriented 2-manifold: every directed edge of a face
appears once, and its reverse in exactly one other face; faces are listed
counterclockwise as seen from outside the solid, and are planar.

The faces are added one at a time in breadth first order over the face
adjacency, so each new face shares at least one edge with the ones already
built. The faces of the solid that are not mesh faces yet ("open" faces)
always cover the part of the surface still to be built; a new face is carved
out of them:
  - The first face is a lamina (`mvfs`, `smev`, `smef`, as the circular
    lamina of [MANT1988].12.2), whose other side is the first open face.
  - The polygon of a new face alternates runs of edges that already exist
    (whose open side is on the face) with runs of new edges. A run of new
    edges goes from the end of an existing run to the start of the next one,
    through vertices that do not exist yet: they are created as struts
    (`lmev`, [MANT1988].11.3.2), and the last edge closes the run:
      - with `lmef` ([MANT1988].11.3.3) when both ends are on the same loop,
        splitting the face so the new face is on the side of the polygon,
      - with `lmekr` ([MANT1988].11.5) when they are on two loops of the same
        face,
      - or with `lkfmrh` and then `lmekr` when they are on two different
        faces, as when closing a handle: the same strategy used to glue the
        ends of a closed rotational sweep in [MANT1988].12.4.2.
  - When all the edges of a face already exist, it is the open face they
    bound.
  - A finished face that shares its face record with other loops gets a face
    of its own (`lmfkrh`).
*/
export class PolyhedralBoundedSolidMeshConstructions {
    /**
    @param positions positions of the vertices of the mesh
    @param faces vertex indexes of each face, counterclockwise as seen from
    outside
    @return a new solid with the mesh as its boundary, or null if the mesh
    has no faces
    @throws IllegalArgumentException if the mesh is not a closed oriented
    2-manifold
    */
    public static createFromPolygonMesh(
        positions: List<Vector3Dd>,
        faces: List<number[]> | null,
    ): PolyhedralBoundedSolid | null {
        if (faces === null || faces.isEmpty()) {
            return null;
        }
        const solid = new _MeshBuilder(positions, faces).build();
        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
        return solid;
    }
}

/**
Java private static nested class
`PolyhedralBoundedSolidMeshConstructions._MeshBuilder`.

Java keys its `HashMap<Long, ...>` maps by the numeric value of the directed
edge and its `Set` of faces by identity: native `Map` and `Set` give the same
lookups. The only iteration over one of them (the border check of
`indexMesh`) follows insertion order instead of the `java.util.HashMap`
order, which can only change which border edge an exception message names.
*/
class _MeshBuilder {
    private readonly positions: List<Vector3Dd>;
    private readonly faces: List<number[]>;
    private readonly numberOfVertices: number;
    private readonly solid: PolyhedralBoundedSolid;

    /// Mesh face of each directed edge
    private readonly faceOfDirectedEdge: Map<number, number>;
    /// Half-edge of the solid along each directed edge already created
    private readonly halfEdges: Map<number, _PolyhedralBoundedSolidHalfEdge>;
    /// Half-edges of the solid starting at each vertex
    private readonly outgoing: ArrayList<ArrayList<_PolyhedralBoundedSolidHalfEdge>>;
    private readonly vertexCreated: boolean[];
    private readonly builtFaces: Set<_PolyhedralBoundedSolidFace>;
    private registeredEdges: number;

    public constructor(positions: List<Vector3Dd>, faces: List<number[]>) {
        this.positions = positions;
        this.faces = faces;
        this.numberOfVertices = positions.size();
        this.solid = new PolyhedralBoundedSolid();
        this.faceOfDirectedEdge = new Map<number, number>();
        this.halfEdges = new Map<number, _PolyhedralBoundedSolidHalfEdge>();
        this.outgoing = new ArrayList<ArrayList<_PolyhedralBoundedSolidHalfEdge>>();
        this.vertexCreated = new Array<boolean>(this.numberOfVertices).fill(false);
        this.builtFaces = new Set<_PolyhedralBoundedSolidFace>();
        this.registeredEdges = 0;
        for (let i = 0; i < this.numberOfVertices; i++) {
            this.outgoing.add(new ArrayList<_PolyhedralBoundedSolidHalfEdge>());
        }
    }

    private key(from: number, to: number): number {
        return from * this.numberOfVertices + to;
    }

    private static meshIndex(vertex: _PolyhedralBoundedSolidVertex): number {
        return vertex.id - 1;
    }

    //= Mesh checks =====================================================

    private indexMesh(): void {
        for (let f = 0; f < this.faces.size(); f++) {
            const polygon = this.faces.get(f);
            if (polygon.length < 3) {
                throw new IllegalArgumentException("Face " + f + " has less than three vertices");
            }
            for (let i = 0; i < polygon.length; i++) {
                const from = polygon[i]!;
                const to = polygon[(i + 1) % polygon.length]!;
                if (from < 0 || from >= this.numberOfVertices || from === to) {
                    throw new IllegalArgumentException("Face " + f + " has an invalid edge");
                }
                const previous = this.faceOfDirectedEdge.get(this.key(from, to));
                this.faceOfDirectedEdge.set(this.key(from, to), f);
                if (previous !== undefined) {
                    throw new IllegalArgumentException(
                        "Edge " + from + "-" + to + " is used twice in the same direction",
                    );
                }
            }
        }
        for (const directed of this.faceOfDirectedEdge.keys()) {
            const from = Math.trunc(directed / this.numberOfVertices);
            const to = directed % this.numberOfVertices;
            if (!this.faceOfDirectedEdge.has(this.key(to, from))) {
                throw new IllegalArgumentException("Edge " + from + "-" + to + " is on the border of the mesh");
            }
        }
    }

    //= Bookkeeping =====================================================

    /**
    Indexes the half-edges of the edges created since the last call.
    */
    private registerNewEdges(): void {
        const edges = this.solid.getEdgesList();
        for (; this.registeredEdges < edges.size(); this.registeredEdges++) {
            const edge = edges.get(this.registeredEdges)!;
            this.registerHalfEdge(edge.rightHalf!);
            this.registerHalfEdge(edge.leftHalf!);
        }
    }

    private registerHalfEdge(he: _PolyhedralBoundedSolidHalfEdge): void {
        const from = _MeshBuilder.meshIndex(he.startingVertex);
        const to = _MeshBuilder.meshIndex(he.mirrorHalfEdge()!.startingVertex);
        this.halfEdges.set(this.key(from, to), he);
        this.outgoing.get(from).add(he);
    }

    private isOpen(face: _PolyhedralBoundedSolidFace): boolean {
        return !this.builtFaces.has(face);
    }

    private static faceOf(he: _PolyhedralBoundedSolidHalfEdge): _PolyhedralBoundedSolidFace {
        return he.parentLoop.parentFace;
    }

    //= Construction ====================================================

    public build(): PolyhedralBoundedSolid {
        this.indexMesh();

        const queued = new Array<boolean>(this.faces.size()).fill(false);
        // Java ArrayDeque<Integer> used as a FIFO queue (add / poll)
        const queue: number[] = [];
        let queueHead = 0;

        // Each connected component of the mesh is a shell
        for (let first = 0; first < this.faces.size(); first++) {
            if (queued[first]) {
                continue;
            }
            this.buildFirstFace(this.faces.get(first));
            queued[first] = true;
            this.enqueueNeighbors(first, queued, queue);

            while (queueHead < queue.length) {
                const f = queue[queueHead++]!;
                this.buildFace(f);
                this.enqueueNeighbors(f, queued, queue);
            }
        }

        if (this.solid.getPolygonsList().size() !== this.faces.size()) {
            throw new IllegalStateException(
                "Mesh construction left " +
                    (this.solid.getPolygonsList().size() - this.faces.size()) +
                    " faces that are not part of the mesh",
            );
        }
        return this.solid;
    }

    private enqueueNeighbors(f: number, queued: boolean[], queue: number[]): void {
        const polygon = this.faces.get(f);
        for (let i = 0; i < polygon.length; i++) {
            const from = polygon[i]!;
            const to = polygon[(i + 1) % polygon.length]!;
            const neighbor = this.faceOfDirectedEdge.get(this.key(to, from))!;
            if (!queued[neighbor]) {
                queued[neighbor] = true;
                queue.push(neighbor);
            }
        }
    }

    /**
    [MANT1988].12.2: a lamina with the vertices of the polygon; its side
    that follows the order of the polygon is the mesh face, the other one
    is the first open face.
    */
    private buildFirstFace(polygon: number[]): void {
        const n = polygon.length;
        const faceId = this.solid.getMaxFaceId() + 1;

        PolyhedralBoundedSolidEulerOperators.mvfs(this.solid, this.positions.get(polygon[0]!), polygon[0]! + 1, faceId);
        this.vertexCreated[polygon[0]!] = true;
        for (let i = 1; i < n; i++) {
            PolyhedralBoundedSolidEulerOperators.smev(
                this.solid,
                faceId,
                polygon[i - 1]! + 1,
                polygon[i]! + 1,
                this.positions.get(polygon[i]!),
            );
            this.vertexCreated[polygon[i]!] = true;
        }
        PolyhedralBoundedSolidEulerOperators.smef(this.solid, faceId, polygon[n - 1]! + 1, polygon[0]! + 1, faceId + 1);
        this.registerNewEdges();
        this.finishFace(polygon);
    }

    private edgeExists(from: number, to: number): boolean {
        return this.halfEdges.has(this.key(from, to));
    }

    private buildFace(f: number): void {
        const polygon = this.faces.get(f);
        const n = polygon.length;
        let start = -1;

        // A run of new edges that starts right after an existing one
        for (let i = 0; i < n; i++) {
            const previousExists = this.edgeExists(polygon[(i + n - 1) % n]!, polygon[i]!);
            const currentExists = this.edgeExists(polygon[i]!, polygon[(i + 1) % n]!);
            if (previousExists && !currentExists) {
                start = i;
                break;
            }
        }

        if (start >= 0) {
            let processed = 0;
            let i = start;
            while (processed < n) {
                const from = polygon[i]!;
                const to = polygon[(i + 1) % n]!;
                if (this.edgeExists(from, to)) {
                    i = (i + 1) % n;
                    processed++;
                    continue;
                }
                // New run: from polygon[i] through new vertices up to
                // the first vertex that already exists
                const newVertices = new ArrayList<number>();
                let j = (i + 1) % n;
                let steps = 1;
                while (!this.vertexCreated[polygon[j]!]) {
                    newVertices.add(polygon[j]!);
                    j = (j + 1) % n;
                    steps++;
                }
                this.buildRun(
                    f,
                    polygon[(i + n - 1) % n]!,
                    polygon[i]!,
                    newVertices,
                    polygon[j]!,
                    polygon[(j + 1) % n]!,
                );
                i = j;
                processed += steps;
            }
        }
        this.finishFace(polygon);
    }

    /**
    Adds a run of new edges of mesh face `f`, from vertex `a` (whose
    previous edge in the face, from `beforeA`, already exists) through the
    new vertices to vertex `b` (followed in the face by `afterB`).
    */
    private buildRun(
        f: number,
        beforeA: number,
        a: number,
        newVertices: List<number>,
        b: number,
        afterB: number,
    ): void {
        let corner = this.halfEdges.get(this.key(beforeA, a))!.next()!;
        let target: _PolyhedralBoundedSolidHalfEdge;
        let last = a;

        if (this.edgeExists(b, afterB)) {
            target = this.halfEdges.get(this.key(b, afterB))!;
        } else {
            target = this.findCorner(b, f);
        }

        for (const w of newVertices) {
            PolyhedralBoundedSolidEulerOperators.lmev(this.solid, corner, corner, w + 1, this.positions.get(w));
            this.vertexCreated[w] = true;
            this.registerNewEdges();
            // The strut is last -> w -> last, before the corner: the
            // corner at w is its returning half-edge
            corner = this.halfEdges.get(this.key(w, last))!;
            last = w;
        }
        this.connect(corner, target);
        this.registerNewEdges();
    }

    /**
    Adds the edge from the vertex of `corner` to the vertex of `target`,
    before both half-edges.
    */
    private connect(corner: _PolyhedralBoundedSolidHalfEdge, target: _PolyhedralBoundedSolidHalfEdge): void {
        if (corner.parentLoop === target.parentLoop) {
            // The new face is the one from target to corner
            PolyhedralBoundedSolidEulerOperators.lmef(this.solid, target, corner, this.solid.getMaxFaceId() + 1);
            return;
        }

        const cornerFace = _MeshBuilder.faceOf(corner);
        const targetFace = _MeshBuilder.faceOf(target);

        if (cornerFace !== targetFace) {
            if (!this.isOpen(cornerFace) || !this.isOpen(targetFace)) {
                throw new IllegalStateException("Mesh construction tried to merge a finished face");
            }
            // Both loops in one face: a ring of the other one
            if (targetFace.boundariesList.size() === 1) {
                PolyhedralBoundedSolidEulerOperators.lkfmrh(this.solid, cornerFace, targetFace);
            } else if (cornerFace.boundariesList.size() === 1) {
                PolyhedralBoundedSolidEulerOperators.lkfmrh(this.solid, targetFace, cornerFace);
            } else {
                while (targetFace.boundariesList.size() > 1) {
                    PolyhedralBoundedSolidEulerOperators.lringmv(
                        this.solid,
                        targetFace.boundariesList.get(1),
                        cornerFace,
                        false,
                    );
                }
                PolyhedralBoundedSolidEulerOperators.lkfmrh(this.solid, cornerFace, targetFace);
            }
        }
        PolyhedralBoundedSolidEulerOperators.lmekr(this.solid, corner, target);
    }

    /**
    Finds, among the open corners of vertex `x` (the open half-edges that
    start at it), the one where mesh face `f` goes: the one whose gap
    between its previous edge and its own edge contains `f` in the star of
    `x`.
    */
    private findCorner(x: number, f: number): _PolyhedralBoundedSolidHalfEdge {
        const candidates = new ArrayList<_PolyhedralBoundedSolidHalfEdge>();

        for (const he of this.outgoing.get(x)) {
            if (this.isOpen(_MeshBuilder.faceOf(he))) {
                candidates.add(he);
            }
        }
        if (candidates.size() === 1) {
            return candidates.get(0);
        }
        for (const he of candidates) {
            const incoming = _MeshBuilder.meshIndex(he.previous()!.startingVertex);
            const outgoingTo = _MeshBuilder.meshIndex(he.mirrorHalfEdge()!.startingVertex);
            let g = this.faceOfDirectedEdge.get(this.key(incoming, x))!;
            const end = this.faceOfDirectedEdge.get(this.key(x, outgoingTo))!;
            for (let steps = 0; steps <= this.faces.size(); steps++) {
                if (g === f) {
                    return he;
                }
                if (g === end) {
                    break;
                }
                g = this.faceOfDirectedEdge.get(this.key(this.nextInFace(g, x), x))!;
            }
        }
        throw new IllegalStateException("Mesh construction found no corner at vertex " + x + " for face " + f);
    }

    private nextInFace(f: number, x: number): number {
        const polygon = this.faces.get(f);
        for (let i = 0; i < polygon.length; i++) {
            if (polygon[i] === x) {
                return polygon[(i + 1) % polygon.length]!;
            }
        }
        throw new IllegalStateException("Vertex " + x + " is not in face " + f);
    }

    /**
    Once all the edges of the polygon exist, its half-edges form a loop:
    its face becomes a mesh face.
    */
    private finishFace(polygon: number[]): void {
        const he = this.halfEdges.get(this.key(polygon[0]!, polygon[1]!))!;
        const loop = he.parentLoop;
        let face = loop.parentFace;

        if (loop.halfEdgesList.size() !== polygon.length || !this.isOpen(face)) {
            throw new IllegalStateException(
                "Mesh construction could not close a face of " + polygon.length + " vertices",
            );
        }
        if (face.boundariesList.size() > 1) {
            face = PolyhedralBoundedSolidEulerOperators.lmfkrh(this.solid, loop, this.solid.getMaxFaceId() + 1);
        }
        this.builtFaces.add(face);
    }
}
