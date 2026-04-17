package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import vsdk.toolkit.processing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidOperator;

import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;

public class _PolyhedralBoundedSolidSetOperatorVertexFace
    extends PolyhedralBoundedSolidOperator
{
    public _PolyhedralBoundedSolidVertex v;
    public _PolyhedralBoundedSolidFace f;

    public String toString()
    {
        String msg = "{" + v + " / " + f + "}";
        return msg;
    }
}
