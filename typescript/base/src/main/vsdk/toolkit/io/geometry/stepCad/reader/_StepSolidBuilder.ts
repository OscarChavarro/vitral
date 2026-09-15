import { IllegalArgumentException } from "../../../../../../java/lang/IllegalArgumentException.js";
import { IllegalStateException } from "../../../../../../java/lang/IllegalStateException.js";
import { IntegerKeyHashMap } from "../../../../../../java/util/IntegerKeyHashMap.js";
import { Vector3Dd } from "../../../../common/linealAlgebra/Vector3Dd.js";
import { PolyhedralBoundedSolid } from "../../../../environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { _PolyhedralBoundedSolidEdge } from "../../../../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.js";
import { _PolyhedralBoundedSolidFace } from "../../../../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import { _PolyhedralBoundedSolidHalfEdge } from "../../../../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import { _PolyhedralBoundedSolidLoop } from "../../../../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";
import { _PolyhedralBoundedSolidVertex } from "../../../../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";
import { _StepEntity } from "./_StepEntity.js";
import { _StepTokenizer } from "./_StepTokenizer.js";

/**
Traverses the flat entity map produced by `_StepTokenizer` and
reconstructs a `PolyhedralBoundedSolid` by direct half-edge
construction.

Traversal root is the single MANIFOLD_SOLID_BREP entity, from which
the reconstruction descends:

  MANIFOLD_SOLID_BREP
    -> CLOSED_SHELL -> [ADVANCED_FACE]
       -> [FACE_OUTER_BOUND | FACE_BOUND] -> EDGE_LOOP
          -> [ORIENTED_EDGE] -> EDGE_CURVE
             -> VERTEX_POINT -> CARTESIAN_POINT

The reconstruction proceeds in four passes to avoid forward-reference
issues inherent in the circular half-edge data structure:

  Pass 1 — Vertex positions: collect every reachable VERTEX_POINT
            and create one `_PolyhedralBoundedSolidVertex` per point.
  Pass 2 — Face / loop / half-edge topology: for each ADVANCED_FACE,
            create its face, loops, and half-edges in loop traversal
            order; record which EDGE_CURVE each half-edge belongs to
            and whether the oriented edge was .T. or .F.
  Pass 3 — Edge wiring: group half-edges by EDGE_CURVE id; for each
            pair create one `_PolyhedralBoundedSolidEdge`, assign the
            .T. half as rightHalf and the .F. half as leftHalf, and
            write parentEdge on both.
  Pass 4 — Emanating half-edge: for each vertex, set
            `emanatingHalfEdge` to any half-edge that starts from it.

Orientation assumptions (same as in the writer):
  - ORIENTED_EDGE .T. → the contributing vertex for the loop position
    is the EDGE_CURVE start vertex (= rightHalf in Mantyla terms).
  - ORIENTED_EDGE .F. → the contributing vertex is the EDGE_CURVE end
    vertex.

Edge geometry tolerance:
  Vertex positions are always taken from the VERTEX_POINT references
  in EDGE_CURVE params[1] and params[2], never from the geometry
  entity in params[3].  Consequently the geometry type in params[3]
  is irrelevant to topological reconstruction and is accepted without
  validation.  Supported geometry types include:
    - LINE               — straight edge (produced by StepWriter)
    - SURFACE_CURVE      — parametric wrapper; the builder unwraps the
                           3D curve reference (param[1]) for logging
                           only; topology is unaffected.
    - B_SPLINE_CURVE_WITH_KNOTS (degree 1, two control points) —
                           treated as a straight-line segment.  This is
                           valid for faces that have been planarised
                           before export (tangent directions coincide
                           with the chord between the two VERTEX_POINTs).
  Any other geometry type is tolerated silently; its edge contributes
  only the two VERTEX_POINT endpoints to the B-rep topology.

This is an internal collaborator of `StepReader`.

Port of `vsdk.toolkit.io.geometry.stepCad.reader._StepSolidBuilder`. Two of
the Java maps decide what the reconstructed solid looks like through the
order they are iterated in: `ecStartVpId` and `ecEndVpId`, whose entry order
numbers the vertices in Pass 1 and whose key order builds the edge list in
Pass 3. Both are `HashMap<Integer, Integer>`, whose order a JVM fixes from the
keys alone, so they are `IntegerKeyHashMap`s here and the vertex ids and edge
order come out as Java's.

The other two maps, `heEdgeCurveId` and `heIsForward`, are keyed by
half-edge. `_PolyhedralBoundedSolidHalfEdge` does not override `hashCode`, so
on a JVM their iteration order follows identity hashes and changes from run
to run; the one thing it decides is which half-edge Pass 4 records as a
vertex's `emanatingHalfEdge`, which Java leaves to that chance. They are
insertion-ordered native `Map`s here, which is one of the orders a JVM run
can produce: the first half-edge built from each vertex is the one kept.
*/
export class _StepSolidBuilder {
    private readonly entities: IntegerKeyHashMap<_StepEntity>;

    private readonly vertexByVpId: IntegerKeyHashMap<_PolyhedralBoundedSolidVertex>;
    private readonly ecStartVpId: IntegerKeyHashMap<number>;
    private readonly ecEndVpId: IntegerKeyHashMap<number>;

    /** Per half-edge: which STEP EDGE_CURVE id it references. */
    private readonly heEdgeCurveId: Map<_PolyhedralBoundedSolidHalfEdge, number>;

    /** Per half-edge: true when the ORIENTED_EDGE had orientation .T. */
    private readonly heIsForward: Map<_PolyhedralBoundedSolidHalfEdge, boolean>;

    /**
    Scale factor applied to every vertex coordinate to convert the file's
    length unit to metres.  Detected from the SI_UNIT length entity; defaults
    to 1.0 (metres) when the unit cannot be determined.
    */
    private readonly unitToMetre: number;

    private constructor(entities: IntegerKeyHashMap<_StepEntity>) {
        this.entities = entities;
        this.vertexByVpId = new IntegerKeyHashMap<_PolyhedralBoundedSolidVertex>();
        this.ecStartVpId = new IntegerKeyHashMap<number>();
        this.ecEndVpId = new IntegerKeyHashMap<number>();
        this.heEdgeCurveId = new Map<_PolyhedralBoundedSolidHalfEdge, number>();
        this.heIsForward = new Map<_PolyhedralBoundedSolidHalfEdge, boolean>();
        this.unitToMetre = this.detectLengthUnitScale();
    }

    public static build(entities: IntegerKeyHashMap<_StepEntity>): PolyhedralBoundedSolid {
        return new _StepSolidBuilder(entities).buildInternal();
    }

    //=================================================================

    private buildInternal(): PolyhedralBoundedSolid {
        const closedShellId: number = this.findClosedShellId();
        const faceIds: number[] = this.collectFaceIds(closedShellId);

        this.collectEdgeCurveEndpoints(faceIds);

        const solid = new PolyhedralBoundedSolid();

        this.pass1CreateVertices(solid);
        this.pass2CreateFacesAndHalfEdges(solid, faceIds);
        this.pass3WireEdges(solid);
        this.pass4SetEmanatingHalfEdges();

        this.updateMaxIds(solid);
        return solid;
    }

    //=================================================================
    //= TRAVERSAL =====================================================

    private findClosedShellId(): number {
        for (const e of this.entities.values()) {
            if ("MANIFOLD_SOLID_BREP" === e.name) {
                return _StepTokenizer.parseRef(e.params[1]!);
            }
        }
        throw new IllegalStateException("STEP file contains no MANIFOLD_SOLID_BREP entity.");
    }

    private collectFaceIds(closedShellId: number): number[] {
        const shell: _StepEntity = this.require(closedShellId, "CLOSED_SHELL");
        const faceAgg: string[] = _StepTokenizer.parseAggregate(shell.params[1]!);
        const ids: number[] = [];
        for (const tok of faceAgg) {
            ids.push(_StepTokenizer.parseRef(tok));
        }
        return ids;
    }

    /**
    Pre-collects the start/end VERTEX_POINT ids for every EDGE_CURVE
    reachable from the given ADVANCED_FACE list so that Pass 2 can
    resolve vertex references without re-traversal.

    Vertex positions are taken exclusively from EDGE_CURVE params[1]
    (start VERTEX_POINT) and params[2] (end VERTEX_POINT).  The
    geometry entity in params[3] (LINE, SURFACE_CURVE,
    B_SPLINE_CURVE_WITH_KNOTS, etc.) is inspected only for diagnostic
    logging and does not affect topology reconstruction.
    */
    private collectEdgeCurveEndpoints(faceIds: number[]): void {
        for (const faceId of faceIds) {
            const face: _StepEntity = this.require(faceId, "ADVANCED_FACE");
            const boundsAgg: string[] = _StepTokenizer.parseAggregate(face.params[1]!);
            for (const boundTok of boundsAgg) {
                const boundId: number = _StepTokenizer.parseRef(boundTok);
                const bound: _StepEntity | undefined = this.entities.get(boundId);
                if (bound === undefined) {
                    continue;
                }
                const loopId: number = _StepTokenizer.parseRef(bound.params[1]!);
                const loop: _StepEntity = this.require(loopId, "EDGE_LOOP");
                const oeAgg: string[] = _StepTokenizer.parseAggregate(loop.params[1]!);
                for (const oeTok of oeAgg) {
                    const oeId: number = _StepTokenizer.parseRef(oeTok);
                    const oe: _StepEntity = this.require(oeId, "ORIENTED_EDGE");
                    const ecId: number = _StepTokenizer.parseRef(oe.params[3]!);
                    if (this.ecStartVpId.containsKey(ecId)) {
                        continue;
                    }
                    const ec: _StepEntity = this.require(ecId, "EDGE_CURVE");
                    this.ecStartVpId.put(ecId, _StepTokenizer.parseRef(ec.params[1]!));
                    this.ecEndVpId.put(ecId, _StepTokenizer.parseRef(ec.params[2]!));
                    this.logEdgeGeometryType(ecId, ec);
                }
            }
        }
    }

    /**
    Resolves and logs the effective 3D geometry type for the given
    EDGE_CURVE.  SURFACE_CURVE wrappers are unwrapped one level to
    expose the underlying curve (LINE or B_SPLINE_CURVE_WITH_KNOTS).
    B_SPLINE edges are accepted and treated as straight-line segments
    because the faces are assumed to be planarised.
    */
    private logEdgeGeometryType(ecId: number, ec: _StepEntity): void {
        if (ec.params.length < 4) {
            return;
        }
        const geomTok: string = ec.params[3]!.trim();
        if (geomTok.length === 0 || geomTok.charAt(0) !== "#") {
            return;
        }
        let geomId: number;
        try {
            geomId = _StepTokenizer.parseRef(geomTok);
        } catch (ignored) {
            if (ignored instanceof IllegalArgumentException) {
                return;
            }
            throw ignored;
        }
        const geomEntity: _StepEntity | undefined = this.entities.get(geomId);
        if (geomEntity === undefined) {
            return;
        }
        let geomName: string = geomEntity.name;
        if ("SURFACE_CURVE" === geomName && geomEntity.params.length >= 2) {
            const innerTok: string = geomEntity.params[1]!.trim();
            if (innerTok.length !== 0 && innerTok.charAt(0) === "#") {
                try {
                    const innerGeomId: number = _StepTokenizer.parseRef(innerTok);
                    const innerGeom: _StepEntity | undefined = this.entities.get(innerGeomId);
                    if (innerGeom !== undefined) {
                        geomName = "SURFACE_CURVE->" + innerGeom.name;
                        if ("B_SPLINE_CURVE_WITH_KNOTS" === innerGeom.name) {
                            console.error(
                                "[StepReader] EDGE_CURVE #" +
                                    ecId +
                                    ": geometry is " +
                                    geomName +
                                    " — treating as straight-line segment" +
                                    " (planarised solid assumed).",
                            );
                        }
                    }
                } catch (ignored) {
                    // inner param is not a reference; keep geomName as-is
                    if (!(ignored instanceof IllegalArgumentException)) {
                        throw ignored;
                    }
                }
            }
        } else if ("B_SPLINE_CURVE_WITH_KNOTS" === geomName) {
            console.error(
                "[StepReader] EDGE_CURVE #" +
                    ecId +
                    ": geometry is B_SPLINE_CURVE_WITH_KNOTS" +
                    " — treating as straight-line segment" +
                    " (planarised solid assumed).",
            );
        }
    }

    //=================================================================
    //= PASS 1: VERTICES ==============================================

    private pass1CreateVertices(solid: PolyhedralBoundedSolid): void {
        let pbsId = 1;
        for (const vpId of this.collectAllVertexPointIds()) {
            const pos: Vector3Dd = this.resolveVertexPosition(vpId);
            const v = new _PolyhedralBoundedSolidVertex(solid, pos, pbsId);
            this.vertexByVpId.put(vpId, v);
            pbsId++;
        }
    }

    private collectAllVertexPointIds(): number[] {
        const vpIds: number[] = [];
        for (const e of this.ecStartVpId.entrySet()) {
            if (!vpIds.includes(e.value)) {
                vpIds.push(e.value);
            }
        }
        for (const e of this.ecEndVpId.entrySet()) {
            if (!vpIds.includes(e.value)) {
                vpIds.push(e.value);
            }
        }
        return vpIds;
    }

    private resolveVertexPosition(vpId: number): Vector3Dd {
        const vp: _StepEntity = this.require(vpId, "VERTEX_POINT");
        const cpId: number = _StepTokenizer.parseRef(vp.params[1]!);
        const cp: _StepEntity = this.require(cpId, "CARTESIAN_POINT");
        const coords: string[] = _StepTokenizer.parseAggregate(cp.params[1]!);
        const x: number = _StepTokenizer.parseDouble(coords[0]!) * this.unitToMetre;
        const y: number = _StepTokenizer.parseDouble(coords[1]!) * this.unitToMetre;
        const z: number = _StepTokenizer.parseDouble(coords[2]!) * this.unitToMetre;
        return new Vector3Dd(x, y, z);
    }

    /**
    Scans the entity map for a compound entity that declares both
    LENGTH_UNIT and SI_UNIT, extracts the SI prefix, and returns the
    corresponding scale factor to convert to metres.

    Recognised SI prefixes and their scale factors:
      .MILLI.  → 0.001    .CENTI.  → 0.01
      .DECI.   → 0.1      (none/$) → 1.0
      .DECA.   → 10       .HECTO.  → 100
      .KILO.   → 1000

    Returns 1.0 (metres assumed) when no matching entity is found or the
    prefix is unrecognised.
    */
    private detectLengthUnitScale(): number {
        for (const e of this.entities.values()) {
            if (_StepEntity.COMPLEX_NAME !== e.name) {
                continue;
            }
            const body: string = e.params[0]!;
            if (!body.includes("LENGTH_UNIT") || !body.includes("SI_UNIT")) {
                continue;
            }
            // Extract the SI_UNIT prefix from the compound body.
            // The SI_UNIT sub-entity has the form: SI_UNIT(prefix,name)
            // where prefix may be $ (none) or an enum like .MILLI.
            const siPos: number = body.indexOf("SI_UNIT(");
            if (siPos < 0) {
                continue;
            }
            const open: number = siPos + "SI_UNIT(".length;
            const close: number = body.indexOf(")", open);
            if (close < 0) {
                continue;
            }
            const siParams: string = body.substring(open, close).trim();
            // siParams = "prefix,name" e.g. ".MILLI.,.METRE." or "$,.METRE."
            const comma: number = siParams.indexOf(",");
            const prefix: string = comma >= 0 ? siParams.substring(0, comma).trim() : siParams.trim();
            const scale: number = _StepSolidBuilder.siPrefixToScale(prefix);
            if (scale !== 1.0) {
                console.error(
                    "[StepReader] Length unit prefix " +
                        prefix +
                        " detected — scaling all vertex coordinates by " +
                        scale +
                        " to convert to metres.",
                );
            }
            return scale;
        }
        return 1.0;
    }

    private static siPrefixToScale(prefix: string): number {
        switch (prefix) {
            case ".ATTO.":
                return 1e-18;
            case ".FEMTO.":
                return 1e-15;
            case ".PICO.":
                return 1e-12;
            case ".NANO.":
                return 1e-9;
            case ".MICRO.":
                return 1e-6;
            case ".MILLI.":
                return 1e-3;
            case ".CENTI.":
                return 1e-2;
            case ".DECI.":
                return 1e-1;
            case ".DECA.":
                return 1e1;
            case ".HECTO.":
                return 1e2;
            case ".KILO.":
                return 1e3;
            case ".MEGA.":
                return 1e6;
            case ".GIGA.":
                return 1e9;
            case ".TERA.":
                return 1e12;
            default:
                return 1.0; // $ or unrecognised = metres
        }
    }

    //=================================================================
    //= PASS 2: FACES, LOOPS, HALF-EDGES ==============================

    private pass2CreateFacesAndHalfEdges(solid: PolyhedralBoundedSolid, faceIds: number[]): void {
        let faceSeqId = 1;
        for (const faceId of faceIds) {
            const faceEntity: _StepEntity = this.require(faceId, "ADVANCED_FACE");
            const face = new _PolyhedralBoundedSolidFace(solid, faceSeqId);
            faceSeqId++;
            const faceSameSense: boolean = this.parseFaceSameSense(faceEntity);
            this.buildLoopsForFace(face, faceEntity, faceSameSense);
        }
    }

    /**
    Reads ADVANCED_FACE param[3] (same_sense flag).
    Returns true when the face normal agrees with the underlying surface
    normal (.T.), false when it is reversed (.F.).  Any parse failure
    defaults to true (no flip).
    */
    private parseFaceSameSense(faceEntity: _StepEntity): boolean {
        if (faceEntity.params.length < 4) {
            return true;
        }
        return ".F." !== faceEntity.params[3]!.trim();
    }

    private buildLoopsForFace(
        face: _PolyhedralBoundedSolidFace,
        faceEntity: _StepEntity,
        faceSameSense: boolean,
    ): void {
        const boundsAgg: string[] = _StepTokenizer.parseAggregate(faceEntity.params[1]!);
        for (const boundTok of boundsAgg) {
            const boundId: number = _StepTokenizer.parseRef(boundTok);
            const bound: _StepEntity | undefined = this.entities.get(boundId);
            if (bound === undefined) {
                continue;
            }
            const loopId: number = _StepTokenizer.parseRef(bound.params[1]!);
            const loop: _StepEntity = this.require(loopId, "EDGE_LOOP");
            this.buildLoop(face, loop, faceSameSense);
        }
    }

    private buildLoop(face: _PolyhedralBoundedSolidFace, loopEntity: _StepEntity, faceSameSense: boolean): void {
        const loop = new _PolyhedralBoundedSolidLoop(face);

        const oeAgg: string[] = _StepTokenizer.parseAggregate(loopEntity.params[1]!);

        const halfEdges: _PolyhedralBoundedSolidHalfEdge[] = [];

        for (const oeTok of oeAgg) {
            const oeId: number = _StepTokenizer.parseRef(oeTok);
            const oe: _StepEntity = this.require(oeId, "ORIENTED_EDGE");
            const ecId: number = _StepTokenizer.parseRef(oe.params[3]!);
            // When same_sense = .F. the face normal is flipped, which means
            // the loop traversal direction is also reversed.  XOR-flipping
            // isForward restores the canonical half-edge direction so that
            // pass3 always sees exactly one .T. (rightHalf) and one .F.
            // (leftHalf) per EDGE_CURVE.
            let isForward: boolean = ".T." === oe.params[4]!.trim();
            if (!faceSameSense) {
                isForward = !isForward;
            }

            const vpId: number = isForward ? this.ecStartVpId.get(ecId)! : this.ecEndVpId.get(ecId)!;
            const v: _PolyhedralBoundedSolidVertex | undefined = this.vertexByVpId.get(vpId);
            if (v === undefined) {
                throw new IllegalStateException("Vertex not found for VERTEX_POINT id " + vpId);
            }

            const he = new _PolyhedralBoundedSolidHalfEdge(v, loop, face.parentSolid);
            loop.halfEdgesList.add(he);
            this.heEdgeCurveId.set(he, ecId);
            this.heIsForward.set(he, isForward);
            halfEdges.push(he);
        }

        if (halfEdges.length === 0) {
            return;
        }

        // When same_sense = .F., the STEP loop is wound in the opposite
        // direction (CW from outside).  The isForward flip above corrects
        // the rightHalf/leftHalf assignment for pass3, but the list order
        // still reflects the CW traversal.  Reversing the list here restores
        // CCW winding for rendering without affecting the per-edge direction
        // flags already recorded in heIsForward.
        if (!faceSameSense) {
            loop.halfEdgesList.reverse();
        }

        loop.boundaryStartHalfEdge = loop.halfEdgesList.get(0);
    }

    //=================================================================
    //= PASS 3: EDGE WIRING ===========================================

    private pass3WireEdges(solid: PolyhedralBoundedSolid): void {
        const forwardHe = new IntegerKeyHashMap<_PolyhedralBoundedSolidHalfEdge>();
        const reverseHe = new IntegerKeyHashMap<_PolyhedralBoundedSolidHalfEdge>();

        for (const [he, ecId] of this.heEdgeCurveId) {
            const forward: boolean = this.heIsForward.get(he)!;
            if (forward) {
                forwardHe.put(ecId, he);
            } else {
                reverseHe.put(ecId, he);
            }
        }

        for (const ecId of this.ecStartVpId.keySet()) {
            const right: _PolyhedralBoundedSolidHalfEdge | undefined = forwardHe.get(ecId);
            const left: _PolyhedralBoundedSolidHalfEdge | undefined = reverseHe.get(ecId);
            if (right === undefined || left === undefined) {
                throw new IllegalStateException(
                    "EDGE_CURVE #" +
                        ecId +
                        " is not referenced by exactly two ORIENTED_EDGEs " +
                        "with opposite orientations.",
                );
            }
            const edge = new _PolyhedralBoundedSolidEdge(solid);
            edge.rightHalf = right;
            edge.leftHalf = left;
            right.parentEdge = edge;
            left.parentEdge = edge;
        }
    }

    //=================================================================
    //= PASS 4: EMANATING HALF-EDGE ===================================

    private pass4SetEmanatingHalfEdges(): void {
        for (const he of this.heEdgeCurveId.keys()) {
            const v: _PolyhedralBoundedSolidVertex = he.startingVertex;
            if (v.emanatingHalfEdge === null) {
                v.emanatingHalfEdge = he;
            }
        }
    }

    //=================================================================
    //= UTILITIES =====================================================

    private updateMaxIds(solid: PolyhedralBoundedSolid): void {
        let maxV = 0;
        let i: number;
        for (i = 0; i < solid.getVerticesList().size(); i++) {
            const vid: number = solid.getVerticesList().get(i)!.id;
            if (vid > maxV) {
                maxV = vid;
            }
        }
        let maxF = 0;
        for (i = 0; i < solid.getPolygonsList().size(); i++) {
            const fid: number = solid.getPolygonsList().get(i)!.id;
            if (fid > maxF) {
                maxF = fid;
            }
        }
        solid.setMaxVertexId(maxV);
        solid.setMaxFaceId(maxF);
    }

    private require(id: number, expectedName: string): _StepEntity {
        const e: _StepEntity | undefined = this.entities.get(id);
        if (e === undefined) {
            throw new IllegalStateException("Expected entity #" + id + " (" + expectedName + ") but it was not found.");
        }
        if (expectedName !== e.name) {
            throw new IllegalStateException(
                "Entity #" + id + " expected to be " + expectedName + " but is " + e.name + ".",
            );
        }
        return e;
    }
}
