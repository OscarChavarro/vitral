package vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition;

final class _TriangulationTrapezoidQueryNode {
    int queryNodeType;
    int segmentIndex;
    _Point2D splitPoint = new _Point2D();
    int trapezoidIndex;
    _TriangulationTrapezoidQueryNode parent;
    _TriangulationTrapezoidQueryNode leftChild;
    _TriangulationTrapezoidQueryNode rightChild;

    int locateEndpoint(_Point2D queryPoint, _Point2D otherPoint) {
        switch (queryNodeType) {
        case _Construct.T_SINK:
            return trapezoidIndex;
        case _Construct.T_Y:
            if (_Construct.greaterThan(queryPoint, splitPoint)) return rightChild.locateEndpoint(queryPoint, otherPoint);
            else if (_Construct.equalTo(queryPoint, splitPoint)) {
                if (_Construct.greaterThan(otherPoint, splitPoint)) return rightChild.locateEndpoint(queryPoint, otherPoint);
                else return leftChild.locateEndpoint(queryPoint, otherPoint);
            }
            else return leftChild.locateEndpoint(queryPoint, otherPoint);
        case _Construct.T_X:
            if (_Construct.equalTo(queryPoint, _Construct.segmentAt(segmentIndex).startPoint)
                || _Construct.equalTo(queryPoint, _Construct.segmentAt(segmentIndex).endPoint)) {
                if (_Construct.fpEqual(queryPoint.y, otherPoint.y)) {
                    return (otherPoint.x < queryPoint.x)
                        ? leftChild.locateEndpoint(queryPoint, otherPoint)
                        : rightChild.locateEndpoint(queryPoint, otherPoint);
                }
                else if (isLeftOf(segmentIndex, otherPoint)) return leftChild.locateEndpoint(queryPoint, otherPoint);
                else return rightChild.locateEndpoint(queryPoint, otherPoint);
            }
            else if (isLeftOf(segmentIndex, queryPoint)) return leftChild.locateEndpoint(queryPoint, otherPoint);
            else return rightChild.locateEndpoint(queryPoint, otherPoint);
        default:
            return 0;
        }
    }

    private static boolean isLeftOf(int segmentIndex, _Point2D queryPoint) {
        _TriangulationSegment s = _Construct.segmentAt(segmentIndex);
        double area;

        if (_Construct.greaterThan(s.endPoint, s.startPoint)) {
            if (_Construct.fpEqual(s.endPoint.y, queryPoint.y)) area = (queryPoint.x < s.endPoint.x) ? 1.0 : -1.0;
            else if (_Construct.fpEqual(s.startPoint.y, queryPoint.y)) area = (queryPoint.x < s.startPoint.x) ? 1.0 : -1.0;
            else area = _Construct.cross(s.startPoint, s.endPoint, queryPoint);
        }
        else {
            if (_Construct.fpEqual(s.endPoint.y, queryPoint.y)) area = (queryPoint.x < s.endPoint.x) ? 1.0 : -1.0;
            else if (_Construct.fpEqual(s.startPoint.y, queryPoint.y)) area = (queryPoint.x < s.startPoint.x) ? 1.0 : -1.0;
            else area = _Construct.cross(s.endPoint, s.startPoint, queryPoint);
        }

        return area > 0.0;
    }
}
