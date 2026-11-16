//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import type { Vector3Dd } from "../../../../common/linealAlgebra/Vector3Dd.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import { _PolyhedralBoundedSolidOperator } from "./_PolyhedralBoundedSolidOperator.js";

/**
One entry of the neighbourhood of a vertex classified against a plane, as
in section [MANT1988].14.5 and program [MANT1988].14.3: the splitter and the
vertex/face classifier of the set operations ([MANT1988].15.6.1, problem
[MANT1988].15.4) use the same entries and differ only in their
reclassification rules. Built by {@link _PolyhedralBoundedSolidVertexNeighborhood}.
*/
export class _PolyhedralBoundedSolidSectorClassification extends _PolyhedralBoundedSolidOperator {
    public static readonly ABOVE = 1;
    public static readonly BELOW = -1;
    public static readonly ON = 0;

    public static readonly COPLANAR_FACE = 10;
    public static readonly INPLANE_EDGE = 20;
    public static readonly CROSSING_EDGE = 30;
    public static readonly UNDEFINED = 40;

    public sector!: _PolyhedralBoundedSolidHalfEdge;
    public classification = 0;

    // Following attributes are not taken from [MANT1988], and all operations
    // on them are fine tunning options aditional to original algorithm.
    public isWide = false;
    public position!: Vector3Dd;
    public situation = _PolyhedralBoundedSolidSectorClassification.UNDEFINED;
}
