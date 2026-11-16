//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;

/**
One entry of the neighbourhood of a vertex classified against a plane, as
in section [MANT1988].14.5 and program [MANT1988].14.3: the splitter and the
vertex/face classifier of the set operations ([MANT1988].15.6.1, problem
[MANT1988].15.4) use the same entries and differ only in their
reclassification rules. Built by {@link _PolyhedralBoundedSolidVertexNeighborhood}.
*/
public class _PolyhedralBoundedSolidSectorClassification
    extends _PolyhedralBoundedSolidOperator
{
    public static final int ABOVE = 1;
    public static final int BELOW = -1;
    public static final int ON = 0;

    public static final int COPLANAR_FACE = 10;
    public static final int INPLANE_EDGE = 20;
    public static final int CROSSING_EDGE = 30;
    public static final int UNDEFINED = 40;

    public _PolyhedralBoundedSolidHalfEdge sector;
    public int cl;

    // Following attributes are not taken from [MANT1988], and all operations
    // on them are fine tunning options aditional to original algorithm.
    public boolean isWide = false;
    public Vector3Dd position;
    public int situation = UNDEFINED;
}
