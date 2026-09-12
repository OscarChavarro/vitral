import { Vector3Dd } from "../../../../../common/linealAlgebra/Vector3Dd.js";
import { PolyhedralBoundedSolid } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidNumericPolicy } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";

export class _BoundaryRepresentationFromCurveBuildState {
    public readonly solid = new PolyhedralBoundedSolid();
    public firstLoop = true;
    public beginningOfLoop = true;
    public nextVertexId = 1;
    public lastLoopStartVertexId = 1;
    public nextFaceId = 1;
    public firstPointInLoop = new Vector3Dd();
    public lastAcceptedPoint: Vector3Dd | null = null;
    public weldEpsilon = PolyhedralBoundedSolidNumericPolicy.BREP_BIG_EPSILON;
    public verticesInCurrentLoop = 0;
}
