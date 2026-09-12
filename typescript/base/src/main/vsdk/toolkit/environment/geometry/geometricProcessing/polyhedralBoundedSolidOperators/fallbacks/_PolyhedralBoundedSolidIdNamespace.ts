//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import type { PolyhedralBoundedSolid } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import type { _PolyhedralBoundedSolidFace } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidVertex } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";

/**
Centralizes face-ID and vertex-ID allocation for the duration of a single
boolean set-operation.  Replaces the scattered ad-hoc pattern
{@code max(A.getMaxVertexId(), B.getMaxVertexId()) + 1} used by the
intersector and the connector, which produced duplicate IDs when called
multiple times without updating both solids' stored maxima.

<p>Usage: construct once per {@code setOp} call after {@code updmaxnames},
then inject into the Intersector and Finisher so that every vertex or face
created during the pipeline consumes a globally unique ID.</p>

<p>Traceability: [MANT1988] §13.1 {@code getmaxnames} / {@code updmaxnames}.</p>
*/
export class _PolyhedralBoundedSolidIdNamespace {
    private nextVertexIdValue: number;
    private nextFaceIdValue: number;

    /**
    Initialises the namespace from the current maxima of both operands.
    Must be called <em>after</em> {@code updmaxnames} so that the IDs of
    solidB have already been offset past solidA.
    @param solidA first boolean operand.
    @param solidB second boolean operand (IDs already offset by updmaxnames).
    */
    public constructor(solidA: PolyhedralBoundedSolid, solidB: PolyhedralBoundedSolid) {
        let maxV: number;
        let maxF: number;

        maxV = solidA.getMaxVertexId();
        if (solidB.getMaxVertexId() > maxV) {
            maxV = solidB.getMaxVertexId();
        }
        maxF = solidA.getMaxFaceId();
        if (solidB.getMaxFaceId() > maxF) {
            maxF = solidB.getMaxFaceId();
        }

        this.nextVertexIdValue = maxV + 1;
        this.nextFaceIdValue = maxF + 1;
    }

    /**
    Returns the next available vertex ID and advances the counter.
    Also updates both solids' stored maximum so that any subsequent Euler
    operator call sees a consistent state.
    @param solidA first operand to keep in sync.
    @param solidB second operand to keep in sync.
    @return a vertex ID that is unique within this pipeline invocation.
    */
    public nextVertexId(solidA: PolyhedralBoundedSolid, solidB: PolyhedralBoundedSolid): number {
        let id: number;

        id = this.nextVertexIdValue;
        this.nextVertexIdValue++;

        if (id > solidA.getMaxVertexId()) {
            solidA.setMaxVertexId(id);
        }
        if (id > solidB.getMaxVertexId()) {
            solidB.setMaxVertexId(id);
        }

        return id;
    }

    /**
    Returns the next available face ID and advances the counter.
    Also updates both solids' stored maximum.
    @param solidA first operand to keep in sync.
    @param solidB second operand to keep in sync.
    @return a face ID that is unique within this pipeline invocation.

    Java overload `nextFaceId(PolyhedralBoundedSolid solid)`: returns the next
    available face ID using only one solid's context — for use in the Finisher
    where the result solid is independent of the two original operands.
    */
    public nextFaceId(solidA: PolyhedralBoundedSolid, solidB?: PolyhedralBoundedSolid): number {
        let id: number;

        id = this.nextFaceIdValue;
        this.nextFaceIdValue++;

        if (solidB === undefined) {
            const solid = solidA;
            if (id > solid.getMaxFaceId()) {
                solid.setMaxFaceId(id);
            }
            return id;
        }

        if (id > solidA.getMaxFaceId()) {
            solidA.setMaxFaceId(id);
        }
        if (id > solidB.getMaxFaceId()) {
            solidB.setMaxFaceId(id);
        }

        return id;
    }

    /**
    Peek at the next vertex ID without consuming it.  Useful for assertions.
    @return the ID that the next {@code nextVertexId(...)} call would return.
    */
    public peekNextVertexId(): number {
        return this.nextVertexIdValue;
    }

    /**
    Peek at the next face ID without consuming it.  Useful for assertions.
    @return the ID that the next {@code nextFaceId(...)} call would return.
    */
    public peekNextFaceId(): number {
        return this.nextFaceIdValue;
    }

    /**
    Procedure `updmaxnames` functionality is described on section
    [MANT1988].15.4. Increments the face and vertex identifiers of
    `solidToUpdate` so that they do not overlap with `referenceSolid`
    identifiers. Centralized here in Stage 7 R3 so the ID-renaming policy
    lives in one place; {@link _PolyhedralBoundedSolidSetOperator#updmaxnames}
    delegates to this method.
    @param solidToUpdate solid whose IDs are offset past the reference.
    @param referenceSolid solid whose maxima define the offset.
    */
    public static updmaxnames(solidToUpdate: PolyhedralBoundedSolid, referenceSolid: PolyhedralBoundedSolid): void {
        let v: _PolyhedralBoundedSolidVertex;
        let f: _PolyhedralBoundedSolidFace;
        let i: number;

        for (i = 0; i < solidToUpdate.getVerticesList().size(); i++) {
            v = solidToUpdate.getVerticesList().get(i)!;
            v.id += referenceSolid.getMaxVertexId();
            if (v.id > solidToUpdate.getMaxVertexId()) {
                solidToUpdate.setMaxVertexId(v.id);
            }
        }

        for (i = 0; i < solidToUpdate.getPolygonsList().size(); i++) {
            f = solidToUpdate.getPolygonsList().get(i)!;
            f.id += referenceSolid.getMaxFaceId();
            if (f.id > solidToUpdate.getMaxFaceId()) {
                solidToUpdate.setMaxFaceId(f.id);
            }
        }
    }

    /**
    Resolves the next vertex ID through the given namespace when available,
    falling back to the legacy {@code max(maxVertexId)+1} policy when no
    namespace is active. Centralizes the null-namespace fallback that used
    to live inline in {@link _PolyhedralBoundedSolidSetOperator}.
    @param current first operand.
    @param other second operand.
    @param namespace active namespace, or {@code null} to use the fallback.
    @return a vertex ID unique within the current pipeline invocation.
    */
    public static nextVertexId(
        current: PolyhedralBoundedSolid,
        other: PolyhedralBoundedSolid,
        namespace: _PolyhedralBoundedSolidIdNamespace | null,
    ): number {
        let a: number;
        let b: number;

        if (namespace !== null) {
            return namespace.nextVertexId(current, other);
        }
        a = current.getMaxVertexId();
        b = other.getMaxVertexId();
        return (b > a ? b : a) + 1;
    }
}
