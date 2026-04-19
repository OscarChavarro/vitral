//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;

/**
Stores one vertex/vertex coincidence from the `sonvv` set introduced by
program [MANT1988].15.1.
*/
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
