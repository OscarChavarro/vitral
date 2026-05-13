package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;

public class _BoundaryRepresentationFromCurveBuildState
{
    final PolyhedralBoundedSolid solid;
    boolean firstLoop;
    boolean beginningOfLoop;
    int nextVertexId;
    int lastLoopStartVertexId;
    int nextFaceId;
    Vector3D firstPointInLoop;
    Vector3D lastAcceptedPoint;

    public _BoundaryRepresentationFromCurveBuildState()
    {
        solid = new PolyhedralBoundedSolid();
        firstLoop = true;
        beginningOfLoop = true;
        nextVertexId = 1;
        lastLoopStartVertexId = 1;
        nextFaceId = 1;
        firstPointInLoop = new Vector3D();
        lastAcceptedPoint = null;
    }
}
