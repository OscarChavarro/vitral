package vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition;

final class _TriangulationSegment {
    _Point2D startPoint = new _Point2D();
    _Point2D endPoint = new _Point2D();
    boolean hasBeenInserted;
    _TriangulationTrapezoidQueryNode startPointQueryNode;
    _TriangulationTrapezoidQueryNode endPointQueryNode;
    int nextSegmentIndex;
    int previousSegmentIndex;
}
