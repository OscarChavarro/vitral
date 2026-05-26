//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;

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
class _PolyhedralBoundedSolidIdNamespace
{
    private int nextVertexId;
    private int nextFaceId;

    /**
    Initialises the namespace from the current maxima of both operands.
    Must be called <em>after</em> {@code updmaxnames} so that the IDs of
    solidB have already been offset past solidA.
    @param solidA first boolean operand.
    @param solidB second boolean operand (IDs already offset by updmaxnames).
    */
    _PolyhedralBoundedSolidIdNamespace(PolyhedralBoundedSolid solidA,
                                       PolyhedralBoundedSolid solidB)
    {
        int maxV;
        int maxF;

        maxV = solidA.getMaxVertexId();
        if ( solidB.getMaxVertexId() > maxV ) {
            maxV = solidB.getMaxVertexId();
        }
        maxF = solidA.getMaxFaceId();
        if ( solidB.getMaxFaceId() > maxF ) {
            maxF = solidB.getMaxFaceId();
        }

        nextVertexId = maxV + 1;
        nextFaceId   = maxF + 1;
    }

    /**
    Returns the next available vertex ID and advances the counter.
    Also updates both solids' stored maximum so that any subsequent Euler
    operator call sees a consistent state.
    @param solidA first operand to keep in sync.
    @param solidB second operand to keep in sync.
    @return a vertex ID that is unique within this pipeline invocation.
    */
    int nextVertexId(PolyhedralBoundedSolid solidA,
                     PolyhedralBoundedSolid solidB)
    {
        int id;

        id = nextVertexId;
        nextVertexId++;

        if ( id > solidA.getMaxVertexId() ) {
            solidA.setMaxVertexId(id);
        }
        if ( id > solidB.getMaxVertexId() ) {
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
    */
    int nextFaceId(PolyhedralBoundedSolid solidA,
                   PolyhedralBoundedSolid solidB)
    {
        int id;

        id = nextFaceId;
        nextFaceId++;

        if ( id > solidA.getMaxFaceId() ) {
            solidA.setMaxFaceId(id);
        }
        if ( id > solidB.getMaxFaceId() ) {
            solidB.setMaxFaceId(id);
        }

        return id;
    }

    /**
    Returns the next available face ID using only one solid's context — for
    use in the Finisher where the result solid is independent of the two
    original operands.
    @param solid the result solid to keep in sync.
    @return a face ID unique within this pipeline invocation.
    */
    int nextFaceId(PolyhedralBoundedSolid solid)
    {
        int id;

        id = nextFaceId;
        nextFaceId++;

        if ( id > solid.getMaxFaceId() ) {
            solid.setMaxFaceId(id);
        }

        return id;
    }

    /**
    Peek at the next vertex ID without consuming it.  Useful for assertions.
    @return the ID that the next {@code nextVertexId(...)} call would return.
    */
    int peekNextVertexId()
    {
        return nextVertexId;
    }

    /**
    Peek at the next face ID without consuming it.  Useful for assertions.
    @return the ID that the next {@code nextFaceId(...)} call would return.
    */
    int peekNextFaceId()
    {
        return nextFaceId;
    }
}
