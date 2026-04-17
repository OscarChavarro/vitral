package vsdk.toolkit.processing;

import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;

public class _PolyhedralBoundedSolidSetOperatorVertexVertex
    extends PolyhedralBoundedSolidOperator
{
    public _PolyhedralBoundedSolidVertex va;
    public _PolyhedralBoundedSolidVertex vb;

    public String toString()
    {
        String msg = "(" + va + ") / (" + vb + "}";
        return msg;
    }
}
