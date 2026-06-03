package vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition;

import vsdk.toolkit.common.linealAlgebra.Vector2Dd;

final class _TriangulationSegment {
    Vector2Dd startPoint = new Vector2Dd();
    Vector2Dd endPoint = new Vector2Dd();
    boolean hasBeenInserted;
    _TriangulationTrapezoidQueryNode startPointQueryNode;
    _TriangulationTrapezoidQueryNode endPointQueryNode;
    int nextSegmentIndex;
    int previousSegmentIndex;
}
