package vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition;

final class _TriangulationTrapezoid {
    // [SEID1991].3 Bounding segment indices. The original reference allocates
    // the trapezoid table with calloc, so unassigned bounding segments default
    // to 0, which is the reserved zeroed "no segment" slot (segmentAt(0)). All
    // logic compares these fields with > 0 / <= 0, so 0 behaves as "absent"
    // while keeping segmentAt() in bounds for degenerate trapezoids (a negative
    // sentinel would index out of the segment table and crash).
    int leftSegmentIndex = 0;
    int rightSegmentIndex = 0;
    _Point2D upperPoint = new _Point2D();
    _Point2D lowerPoint = new _Point2D();
    int upperLeftTrapezoidIndex;
    int upperRightTrapezoidIndex;
    int lowerLeftTrapezoidIndex;
    int lowerRightTrapezoidIndex;
    _TriangulationTrapezoidQueryNode sinkNode;
    int savedUpperNeighborIndex;
    int savedUpperNeighborSide;
    int status = 1;

    void copyFrom(_TriangulationTrapezoid other) {
        leftSegmentIndex = other.leftSegmentIndex;
        rightSegmentIndex = other.rightSegmentIndex;
        upperPoint.set(other.upperPoint);
        lowerPoint.set(other.lowerPoint);
        upperLeftTrapezoidIndex = other.upperLeftTrapezoidIndex;
        upperRightTrapezoidIndex = other.upperRightTrapezoidIndex;
        lowerLeftTrapezoidIndex = other.lowerLeftTrapezoidIndex;
        lowerRightTrapezoidIndex = other.lowerRightTrapezoidIndex;
        sinkNode = other.sinkNode;
        savedUpperNeighborIndex = other.savedUpperNeighborIndex;
        savedUpperNeighborSide = other.savedUpperNeighborSide;
        status = other.status;
    }

    int insidePolygon() {
        int rightBoundarySegmentIndex = this.rightSegmentIndex;

        if (status == _Construct.ST_INVALID) return 0;
        if ((leftSegmentIndex <= 0) || (rightBoundarySegmentIndex <= 0)) return 0;
        if (((upperLeftTrapezoidIndex <= 0) && (upperRightTrapezoidIndex <= 0)) ||
            ((lowerLeftTrapezoidIndex <= 0) && (lowerRightTrapezoidIndex <= 0))) {
            return _Construct.greaterThan(
                _Construct.segmentAt(rightBoundarySegmentIndex).endPoint,
                _Construct.segmentAt(rightBoundarySegmentIndex).startPoint) ? 1 : 0;
        }
        return 0;
    }
}
