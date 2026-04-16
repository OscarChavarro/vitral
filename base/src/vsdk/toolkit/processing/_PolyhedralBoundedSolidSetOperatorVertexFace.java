package vsdk.toolkit.processing;

import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidVertex;

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
