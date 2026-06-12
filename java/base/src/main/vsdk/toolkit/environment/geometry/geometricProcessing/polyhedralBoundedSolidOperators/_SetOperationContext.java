package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;

/**
Per-invocation mutable state for one boolean set-operation: the Mantyla
"son*" globals from program [MANT1988].15.1 (sonvv, sonva, sonvb, sonea,
soneb, sonfa, sonfb). Stage 7 R5 threads one instance of this object explicitly
through the pipeline (generate -> classify -> connect -> finish) so the lists
are no longer shared static fields. This removes the cross-invocation state
leak that made the operator non-re-entrant.

<p>The numeric tolerance context and the id namespace are intentionally left as
managed static state in {@link _PolyhedralBoundedSolidOperator}; only the
per-operation list state is carried here.</p>
 */
final class _SetOperationContext
{
    /** Following variable `sonvv` from program [MANT1988].15.1. */
    ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex> sonvv;

    /** Following variable `sonva` from program [MANT1988].15.1. */
    ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonva;

    /** Following variable `sonvb` from program [MANT1988].15.1. */
    ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> sonvb;

    /** Following variable `sonea` from program [MANT1988].15.1. */
    ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> sonea;

    /** Following variable `soneb` from program [MANT1988].15.1. */
    ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> soneb;

    /** Following variable `sonfa` from program [MANT1988].15.1. */
    ArrayList<_PolyhedralBoundedSolidFace> sonfa;

    /** Following variable `sonfb` from program [MANT1988].15.1. */
    ArrayList<_PolyhedralBoundedSolidFace> sonfb;
}
