//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import vsdk.toolkit.processing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidOperator;

import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;

/**
Stores one vertex/face coincidence from the `sonva`/`sonvb` sets introduced by
program [MANT1988].15.1.
*/
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
