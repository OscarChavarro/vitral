package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;

public class _BoundaryRepresentationFromCurveBuildState
{
    final PolyhedralBoundedSolid solid;
    boolean firstLoop;
    boolean beginningOfLoop;
    int nextVertexId;
    int lastLoopStartVertexId;
    int nextFaceId;
    Vector3Dd firstPointInLoop;
    Vector3Dd lastAcceptedPoint;
    double weldEpsilon;
    int verticesInCurrentLoop;

    public _BoundaryRepresentationFromCurveBuildState()
    {
        solid = new PolyhedralBoundedSolid();
        firstLoop = true;
        beginningOfLoop = true;
        nextVertexId = 1;
        lastLoopStartVertexId = 1;
        nextFaceId = 1;
        firstPointInLoop = new Vector3Dd();
        lastAcceptedPoint = null;
        weldEpsilon = PolyhedralBoundedSolidNumericPolicy.BREP_BIG_EPSILON;
        verticesInCurrentLoop = 0;
    }
}
