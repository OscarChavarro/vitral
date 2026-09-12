import { ArrayList } from "../../../../../../../java/util/ArrayList.js";
import type { _PolyhedralBoundedSolidFace } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidSetOperatorNullEdge } from "./topology/_PolyhedralBoundedSolidSetOperatorNullEdge.js";
import type { _PolyhedralBoundedSolidSetOperatorVertexFace } from "./classification/_PolyhedralBoundedSolidSetOperatorVertexFace.js";
import type { _PolyhedralBoundedSolidSetOperatorVertexVertex } from "./classification/_PolyhedralBoundedSolidSetOperatorVertexVertex.js";

/** Per-invocation mutable son* state from [MANT1988].15.1. */
export class _SetOperationContext {
    public sonvv: ArrayList<_PolyhedralBoundedSolidSetOperatorVertexVertex> | null = null;
    public sonva: ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> | null = null;
    public sonvb: ArrayList<_PolyhedralBoundedSolidSetOperatorVertexFace> | null = null;
    public sonea: ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> | null = null;
    public soneb: ArrayList<_PolyhedralBoundedSolidSetOperatorNullEdge> | null = null;
    public sonfa: ArrayList<_PolyhedralBoundedSolidFace> | null = null;
    public sonfb: ArrayList<_PolyhedralBoundedSolidFace> | null = null;
}
