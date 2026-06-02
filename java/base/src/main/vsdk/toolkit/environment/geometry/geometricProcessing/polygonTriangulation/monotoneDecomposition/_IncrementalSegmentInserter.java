package vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition;

public final class _IncrementalSegmentInserter {
    private _IncrementalSegmentInserter() {}
    private static _TriangulationTrapezoid trap(int i) { return _Construct.trapezoids.get(i); }

    private static final class BoolRef {
        boolean value;
    }

    private static void normalizeSegmentForInsertion(_TriangulationSegment segment, BoolRef isSwapped) {
        isSwapped.value = false;
        if (_Construct.greaterThan(segment.endPoint, segment.startPoint)) {
            _Point2D swapPoint = new _Point2D(segment.startPoint);
            _TriangulationTrapezoidQueryNode tmp = segment.startPointQueryNode;
            segment.startPoint.set(segment.endPoint);
            segment.endPoint.set(swapPoint);
            segment.startPointQueryNode = segment.endPointQueryNode;
            segment.endPointQueryNode = tmp;
            isSwapped.value = true;
        }
    }

    private static void updateLowerNeighbourLinksAfterSplit(int splitTrapIndex, int originalUpperTrapIndex) {
        int tmpDownIndex;

        if (((tmpDownIndex = _Construct.trapezoids.get(splitTrapIndex).lowerLeftTrapezoidIndex) > 0) &&
            (_Construct.trapezoids.get(tmpDownIndex).upperLeftTrapezoidIndex == originalUpperTrapIndex)) {
            _Construct.trapezoids.get(tmpDownIndex).upperLeftTrapezoidIndex = splitTrapIndex;
        }
        if (((tmpDownIndex = _Construct.trapezoids.get(splitTrapIndex).lowerLeftTrapezoidIndex) > 0) &&
            (_Construct.trapezoids.get(tmpDownIndex).upperRightTrapezoidIndex == originalUpperTrapIndex)) {
            _Construct.trapezoids.get(tmpDownIndex).upperRightTrapezoidIndex = splitTrapIndex;
        }

        if (((tmpDownIndex = _Construct.trapezoids.get(splitTrapIndex).lowerRightTrapezoidIndex) > 0) &&
            (_Construct.trapezoids.get(tmpDownIndex).upperLeftTrapezoidIndex == originalUpperTrapIndex)) {
            _Construct.trapezoids.get(tmpDownIndex).upperLeftTrapezoidIndex = splitTrapIndex;
        }
        if (((tmpDownIndex = _Construct.trapezoids.get(splitTrapIndex).lowerRightTrapezoidIndex) > 0) &&
            (_Construct.trapezoids.get(tmpDownIndex).upperRightTrapezoidIndex == originalUpperTrapIndex)) {
            _Construct.trapezoids.get(tmpDownIndex).upperRightTrapezoidIndex = splitTrapIndex;
        }
    }

    private static int splitTrapezoidAtEndpoint(int segmentIndex, _TriangulationSegment segment, boolean useFirstEndpoint) {
        _Point2D endpoint = useFirstEndpoint ? segment.startPoint : segment.endPoint;
        _Point2D otherEndpoint = useFirstEndpoint ? segment.endPoint : segment.startPoint;

        int upperTrapezoid = (useFirstEndpoint ? segment.startPointQueryNode : segment.endPointQueryNode)
            .locateEndpoint(endpoint, otherEndpoint);
        int splitTrapIndex = _Construct.allocateTrapezoidIndex();
        if (splitTrapIndex < 0) return -1;

        _Construct.trapezoids.set(splitTrapIndex, new _TriangulationTrapezoid());
        _Construct.trapezoids.get(splitTrapIndex).status = _Construct.ST_VALID;
        _Construct.trapezoids.get(splitTrapIndex).copyFrom(_Construct.trapezoids.get(upperTrapezoid));

        _Construct.trapezoids.get(upperTrapezoid).lowerPoint.y = endpoint.y;
        _Construct.trapezoids.get(upperTrapezoid).lowerPoint.x = endpoint.x;
        _Construct.trapezoids.get(splitTrapIndex).upperPoint.y = endpoint.y;
        _Construct.trapezoids.get(splitTrapIndex).upperPoint.x = endpoint.x;

        _Construct.trapezoids.get(upperTrapezoid).lowerLeftTrapezoidIndex = splitTrapIndex;
        _Construct.trapezoids.get(upperTrapezoid).lowerRightTrapezoidIndex = 0;
        _Construct.trapezoids.get(splitTrapIndex).upperLeftTrapezoidIndex = upperTrapezoid;
        _Construct.trapezoids.get(splitTrapIndex).upperRightTrapezoidIndex = 0;

        updateLowerNeighbourLinksAfterSplit(splitTrapIndex, upperTrapezoid);

        _TriangulationTrapezoidQueryNode upperSinkNode = _Construct.allocateQueryNode();
        _TriangulationTrapezoidQueryNode lowerSinkNode = _Construct.allocateQueryNode();
        _TriangulationTrapezoidQueryNode querySplitNode = _Construct.trapezoids.get(upperTrapezoid).sinkNode;

        querySplitNode.queryNodeType = _Construct.T_Y;
        querySplitNode.splitPoint.set(endpoint);
        querySplitNode.segmentIndex = segmentIndex;
        querySplitNode.leftChild = lowerSinkNode;
        querySplitNode.rightChild = upperSinkNode;

        upperSinkNode.queryNodeType = _Construct.T_SINK;
        upperSinkNode.trapezoidIndex = upperTrapezoid;
        upperSinkNode.parent = querySplitNode;

        lowerSinkNode.queryNodeType = _Construct.T_SINK;
        lowerSinkNode.trapezoidIndex = splitTrapIndex;
        lowerSinkNode.parent = querySplitNode;

        _Construct.trapezoids.get(upperTrapezoid).sinkNode = upperSinkNode;
        _Construct.trapezoids.get(splitTrapIndex).sinkNode = lowerSinkNode;

        return useFirstEndpoint ? splitTrapIndex : upperTrapezoid;
    }

    private static int locateOrInsertEndpointTrapezoid(
        int segmentIndex, _TriangulationSegment segment, boolean useFirstEndpoint,
        boolean endpointAlreadyInserted, BoolRef wasEndpointInserted) {
        _Point2D endpoint = useFirstEndpoint ? segment.startPoint : segment.endPoint;
        _Point2D otherEndpoint = useFirstEndpoint ? segment.endPoint : segment.startPoint;

        wasEndpointInserted.value = false;
        if (endpointAlreadyInserted) {
            return (useFirstEndpoint ? segment.startPointQueryNode : segment.endPointQueryNode)
                .locateEndpoint(endpoint, otherEndpoint);
        }

        wasEndpointInserted.value = true;
        return splitTrapezoidAtEndpoint(segmentIndex, segment, useFirstEndpoint);
    }

    private static boolean isLeftOf(int segmentIndex, _Point2D queryPoint) {
        _TriangulationSegment s = _Construct.segments.get(segmentIndex);
        double area;

        if (_Construct.greaterThan(s.endPoint, s.startPoint)) {
            if (_Construct.fpEqual(s.endPoint.y, queryPoint.y)) {
                area = (queryPoint.x < s.endPoint.x) ? 1.0 : -1.0;
            }
            else if (_Construct.fpEqual(s.startPoint.y, queryPoint.y)) {
                area = (queryPoint.x < s.startPoint.x) ? 1.0 : -1.0;
            }
            else {
                area = _Construct.cross(s.startPoint, s.endPoint, queryPoint);
            }
        }
        else {
            if (_Construct.fpEqual(s.endPoint.y, queryPoint.y)) {
                area = (queryPoint.x < s.endPoint.x) ? 1.0 : -1.0;
            }
            else if (_Construct.fpEqual(s.startPoint.y, queryPoint.y)) {
                area = (queryPoint.x < s.startPoint.x) ? 1.0 : -1.0;
            }
            else {
                area = _Construct.cross(s.endPoint, s.startPoint, queryPoint);
            }
        }

        return area > 0.0;
    }

    private static boolean inserted(int segmentIndex, int whichpt) {
        if (whichpt == _Construct.SEGMENT_FIRST_ENDPOINT) {
            return _Construct.segments.get(_Construct.segments.get(segmentIndex).previousSegmentIndex).hasBeenInserted;
        }
        return _Construct.segments.get(_Construct.segments.get(segmentIndex).nextSegmentIndex).hasBeenInserted;
    }

    private static int mergeTrapezoids(int segmentIndex, int tfirst, int tlast, int side) {
        int t = tfirst;
        int tnext;

        while ((t > 0) && _Construct.greaterThanEqualTo(trap(t).lowerPoint, trap(tlast).lowerPoint)) {
            boolean cond;
            if (side == _Construct.S_LEFT) {
                cond = ((((tnext = trap(t).lowerLeftTrapezoidIndex) > 0) && (trap(tnext).rightSegmentIndex == segmentIndex)) ||
                        (((tnext = trap(t).lowerRightTrapezoidIndex) > 0) && (trap(tnext).rightSegmentIndex == segmentIndex)));
            }
            else {
                cond = ((((tnext = trap(t).lowerLeftTrapezoidIndex) > 0) && (trap(tnext).leftSegmentIndex == segmentIndex)) ||
                        (((tnext = trap(t).lowerRightTrapezoidIndex) > 0) && (trap(tnext).leftSegmentIndex == segmentIndex)));
            }

            if (cond) {
                if ((trap(t).leftSegmentIndex == trap(tnext).leftSegmentIndex) &&
                    (trap(t).rightSegmentIndex == trap(tnext).rightSegmentIndex)) {
                    _TriangulationTrapezoidQueryNode ptnext = trap(tnext).sinkNode.parent;

                    if (ptnext.leftChild == trap(tnext).sinkNode) ptnext.leftChild = trap(t).sinkNode;
                    else ptnext.rightChild = trap(t).sinkNode;

                    trap(t).lowerLeftTrapezoidIndex = trap(tnext).lowerLeftTrapezoidIndex;
                    if (trap(t).lowerLeftTrapezoidIndex > 0) {
                        if (trap(trap(t).lowerLeftTrapezoidIndex).upperLeftTrapezoidIndex == tnext) {
                            trap(trap(t).lowerLeftTrapezoidIndex).upperLeftTrapezoidIndex = t;
                        }
                        else if (trap(trap(t).lowerLeftTrapezoidIndex).upperRightTrapezoidIndex == tnext) {
                            trap(trap(t).lowerLeftTrapezoidIndex).upperRightTrapezoidIndex = t;
                        }
                    }

                    trap(t).lowerRightTrapezoidIndex = trap(tnext).lowerRightTrapezoidIndex;
                    if (trap(t).lowerRightTrapezoidIndex > 0) {
                        if (trap(trap(t).lowerRightTrapezoidIndex).upperLeftTrapezoidIndex == tnext) {
                            trap(trap(t).lowerRightTrapezoidIndex).upperLeftTrapezoidIndex = t;
                        }
                        else if (trap(trap(t).lowerRightTrapezoidIndex).upperRightTrapezoidIndex == tnext) {
                            trap(trap(t).lowerRightTrapezoidIndex).upperRightTrapezoidIndex = t;
                        }
                    }

                    trap(t).lowerPoint.set(trap(tnext).lowerPoint);
                    trap(tnext).status = _Construct.ST_INVALID;
                }
                else {
                    t = tnext;
                }
            }
            else {
                t = tnext;
            }
        }
        return 0;
    }

    public static int addSegment(int segmentIndex) {
        _TriangulationSegment s = new _TriangulationSegment();
        _TriangulationTrapezoidQueryNode sk;
        int tfirst;
        int tlast;
        int tfirstr = 0;
        int tlastr = 0;
        int tfirstl;
        int tlastl;
        _TriangulationTrapezoidQueryNode i1;
        _TriangulationTrapezoidQueryNode i2;
        int t;
        int tn;
        int tribot = 0;
        BoolRef isSwapped = new BoolRef();
        int tmptriseg;

        _TriangulationSegment src = _Construct.segments.get(segmentIndex);
        s.startPoint.set(src.startPoint);
        s.endPoint.set(src.endPoint);
        s.hasBeenInserted = src.hasBeenInserted;
        s.startPointQueryNode = src.startPointQueryNode;
        s.endPointQueryNode = src.endPointQueryNode;
        s.nextSegmentIndex = src.nextSegmentIndex;
        s.previousSegmentIndex = src.previousSegmentIndex;
        normalizeSegmentForInsertion(s, isSwapped);

        BoolRef insertedFirstEndpoint = new BoolRef();
        boolean endpoint0AlreadyInserted =
            isSwapped.value ? inserted(segmentIndex, _Construct.SEGMENT_LAST_ENDPOINT)
                            : inserted(segmentIndex, _Construct.SEGMENT_FIRST_ENDPOINT);
        tfirst = locateOrInsertEndpointTrapezoid(segmentIndex, s, true, endpoint0AlreadyInserted, insertedFirstEndpoint);
        if (tfirst < 0) return -1;

        BoolRef insertedLastEndpoint = new BoolRef();
        boolean endpoint1AlreadyInserted =
            isSwapped.value ? inserted(segmentIndex, _Construct.SEGMENT_FIRST_ENDPOINT)
                            : inserted(segmentIndex, _Construct.SEGMENT_LAST_ENDPOINT);
        tlast = locateOrInsertEndpointTrapezoid(segmentIndex, s, false, endpoint1AlreadyInserted, insertedLastEndpoint);
        if (tlast < 0) return -1;
        tribot = insertedLastEndpoint.value ? 0 : 1;

        t = tfirst;
        while ((t > 0) && _Construct.greaterThanEqualTo(trap(t).lowerPoint, trap(tlast).lowerPoint)) {
            int savedTrapezoidIndex;
            int savedNewTrapezoidIndex;
            sk = trap(t).sinkNode;
            i1 = _Construct.allocateQueryNode();
            i2 = _Construct.allocateQueryNode();

            sk.queryNodeType = _Construct.T_X;
            sk.segmentIndex = segmentIndex;
            sk.leftChild = i1;
            sk.rightChild = i2;

            i1.queryNodeType = _Construct.T_SINK;
            i1.trapezoidIndex = t;
            i1.parent = sk;

            i2.queryNodeType = _Construct.T_SINK;
            tn = _Construct.allocateTrapezoidIndex();
            if (tn < 0) return -1;
            _Construct.trapezoids.set(tn, new _TriangulationTrapezoid());
            i2.trapezoidIndex = tn;
            trap(tn).status = _Construct.ST_VALID;
            i2.parent = sk;

            if (t == tfirst) tfirstr = tn;
            if (_Construct.equalTo(trap(t).lowerPoint, trap(tlast).lowerPoint)) tlastr = tn;

            trap(tn).copyFrom(trap(t));
            trap(t).sinkNode = i1;
            trap(tn).sinkNode = i2;
            savedTrapezoidIndex = t;
            savedNewTrapezoidIndex = tn;

            if ((trap(t).lowerLeftTrapezoidIndex <= 0) && (trap(t).lowerRightTrapezoidIndex <= 0)) {
                break;
            }
            else if ((trap(t).lowerLeftTrapezoidIndex > 0) && (trap(t).lowerRightTrapezoidIndex <= 0)) {
                if ((trap(t).upperLeftTrapezoidIndex > 0) && (trap(t).upperRightTrapezoidIndex > 0)) {
                    if (trap(t).savedUpperNeighborIndex > 0) {
                        if (trap(t).savedUpperNeighborSide == _Construct.S_LEFT) {
                            trap(tn).upperLeftTrapezoidIndex = trap(t).upperRightTrapezoidIndex;
                            trap(t).upperRightTrapezoidIndex = -1;
                            trap(tn).upperRightTrapezoidIndex = trap(t).savedUpperNeighborIndex;
                            trap(trap(t).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                            trap(trap(tn).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = tn;
                            trap(trap(tn).upperRightTrapezoidIndex).lowerLeftTrapezoidIndex = tn;
                        }
                        else {
                            trap(tn).upperRightTrapezoidIndex = -1;
                            trap(tn).upperLeftTrapezoidIndex = trap(t).upperRightTrapezoidIndex;
                            trap(t).upperRightTrapezoidIndex = trap(t).upperLeftTrapezoidIndex;
                            trap(t).upperLeftTrapezoidIndex = trap(t).savedUpperNeighborIndex;
                            trap(trap(t).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                            trap(trap(t).upperRightTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                            trap(trap(tn).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = tn;
                        }
                        trap(t).savedUpperNeighborIndex = 0;
                        trap(tn).savedUpperNeighborIndex = 0;
                    }
                    else {
                        trap(tn).upperLeftTrapezoidIndex = trap(t).upperRightTrapezoidIndex;
                        trap(t).upperRightTrapezoidIndex = -1;
                        trap(tn).upperRightTrapezoidIndex = -1;
                        trap(trap(tn).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = tn;
                    }
                }
                else {
                    int tmpUpperIndex = trap(t).upperLeftTrapezoidIndex;
                    int td0 = trap(tmpUpperIndex).lowerLeftTrapezoidIndex;
                    int td1 = trap(tmpUpperIndex).lowerRightTrapezoidIndex;
                    if (td0 > 0 && td1 > 0) {
                        if ((trap(td0).rightSegmentIndex > 0) && !isLeftOf(trap(td0).rightSegmentIndex, s.endPoint)) {
                            trap(t).upperLeftTrapezoidIndex = -1;
                            trap(t).upperRightTrapezoidIndex = -1;
                            trap(tn).upperRightTrapezoidIndex = -1;
                            trap(trap(tn).upperLeftTrapezoidIndex).lowerRightTrapezoidIndex = tn;
                        }
                        else {
                            trap(tn).upperLeftTrapezoidIndex = -1;
                            trap(tn).upperRightTrapezoidIndex = -1;
                            trap(t).upperRightTrapezoidIndex = -1;
                            trap(trap(t).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                        }
                    }
                    else {
                        trap(trap(t).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                        trap(trap(t).upperLeftTrapezoidIndex).lowerRightTrapezoidIndex = tn;
                    }
                }

                if (_Construct.fpEqual(trap(t).lowerPoint.y, trap(tlast).lowerPoint.y) &&
                    _Construct.fpEqual(trap(t).lowerPoint.x, trap(tlast).lowerPoint.x) && tribot != 0) {
                    tmptriseg = isSwapped.value ? _Construct.segments.get(segmentIndex).previousSegmentIndex
                                                : _Construct.segments.get(segmentIndex).nextSegmentIndex;
                    if ((tmptriseg > 0) && isLeftOf(tmptriseg, s.startPoint)) {
                        trap(trap(t).lowerLeftTrapezoidIndex).upperLeftTrapezoidIndex = t;
                        trap(tn).lowerLeftTrapezoidIndex = -1;
                        trap(tn).lowerRightTrapezoidIndex = -1;
                    }
                    else {
                        trap(trap(tn).lowerLeftTrapezoidIndex).upperRightTrapezoidIndex = tn;
                        trap(t).lowerLeftTrapezoidIndex = -1;
                        trap(t).lowerRightTrapezoidIndex = -1;
                    }
                }
                else {
                    if ((trap(trap(t).lowerLeftTrapezoidIndex).upperLeftTrapezoidIndex > 0) &&
                        (trap(trap(t).lowerLeftTrapezoidIndex).upperRightTrapezoidIndex > 0)) {
                        if (trap(trap(t).lowerLeftTrapezoidIndex).upperLeftTrapezoidIndex == t) {
                            trap(trap(t).lowerLeftTrapezoidIndex).savedUpperNeighborIndex = trap(trap(t).lowerLeftTrapezoidIndex).upperRightTrapezoidIndex;
                            trap(trap(t).lowerLeftTrapezoidIndex).savedUpperNeighborSide = _Construct.S_LEFT;
                        }
                        else {
                            trap(trap(t).lowerLeftTrapezoidIndex).savedUpperNeighborIndex = trap(trap(t).lowerLeftTrapezoidIndex).upperLeftTrapezoidIndex;
                            trap(trap(t).lowerLeftTrapezoidIndex).savedUpperNeighborSide = _Construct.S_RIGHT;
                        }
                    }
                    trap(trap(t).lowerLeftTrapezoidIndex).upperLeftTrapezoidIndex = t;
                    trap(trap(t).lowerLeftTrapezoidIndex).upperRightTrapezoidIndex = tn;
                }
                t = trap(t).lowerLeftTrapezoidIndex;
            }
            else if ((trap(t).lowerLeftTrapezoidIndex <= 0) && (trap(t).lowerRightTrapezoidIndex > 0)) {
                if ((trap(t).upperLeftTrapezoidIndex > 0) && (trap(t).upperRightTrapezoidIndex > 0)) {
                    if (trap(t).savedUpperNeighborIndex > 0) {
                        if (trap(t).savedUpperNeighborSide == _Construct.S_LEFT) {
                            trap(tn).upperLeftTrapezoidIndex = trap(t).upperRightTrapezoidIndex;
                            trap(t).upperRightTrapezoidIndex = -1;
                            trap(tn).upperRightTrapezoidIndex = trap(t).savedUpperNeighborIndex;

                            trap(trap(t).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                            trap(trap(tn).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = tn;
                            trap(trap(tn).upperRightTrapezoidIndex).lowerLeftTrapezoidIndex = tn;
                        }
                        else {
                            trap(tn).upperRightTrapezoidIndex = -1;
                            trap(tn).upperLeftTrapezoidIndex = trap(t).upperRightTrapezoidIndex;
                            trap(t).upperRightTrapezoidIndex = trap(t).upperLeftTrapezoidIndex;
                            trap(t).upperLeftTrapezoidIndex = trap(t).savedUpperNeighborIndex;

                            trap(trap(t).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                            trap(trap(t).upperRightTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                            trap(trap(tn).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = tn;
                        }
                        trap(t).savedUpperNeighborIndex = 0;
                        trap(tn).savedUpperNeighborIndex = 0;
                    }
                    else {
                        trap(tn).upperLeftTrapezoidIndex = trap(t).upperRightTrapezoidIndex;
                        trap(t).upperRightTrapezoidIndex = -1;
                        trap(tn).upperRightTrapezoidIndex = -1;
                        trap(trap(tn).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = tn;
                    }
                }
                else {
                    int tmpUpperIndex = trap(t).upperLeftTrapezoidIndex;
                    int td0 = trap(tmpUpperIndex).lowerLeftTrapezoidIndex;
                    int td1 = trap(tmpUpperIndex).lowerRightTrapezoidIndex;
                    if (td0 > 0 && td1 > 0) {
                        if ((trap(td0).rightSegmentIndex > 0) &&
                            !isLeftOf(trap(td0).rightSegmentIndex, s.endPoint)) {
                            trap(t).upperLeftTrapezoidIndex = -1;
                            trap(t).upperRightTrapezoidIndex = -1;
                            trap(tn).upperRightTrapezoidIndex = -1;
                            trap(trap(tn).upperLeftTrapezoidIndex).lowerRightTrapezoidIndex = tn;
                        }
                        else {
                            trap(tn).upperLeftTrapezoidIndex = -1;
                            trap(tn).upperRightTrapezoidIndex = -1;
                            trap(t).upperRightTrapezoidIndex = -1;
                            trap(trap(t).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                        }
                    }
                    else {
                        trap(trap(t).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                        trap(trap(t).upperLeftTrapezoidIndex).lowerRightTrapezoidIndex = tn;
                    }
                }

                if (_Construct.fpEqual(trap(t).lowerPoint.y, trap(tlast).lowerPoint.y) &&
                    _Construct.fpEqual(trap(t).lowerPoint.x, trap(tlast).lowerPoint.x) &&
                    tribot != 0) {
                    tmptriseg = isSwapped.value ? _Construct.segments.get(segmentIndex).previousSegmentIndex
                                                : _Construct.segments.get(segmentIndex).nextSegmentIndex;
                    if ((tmptriseg > 0) && isLeftOf(tmptriseg, s.startPoint)) {
                        trap(trap(t).lowerRightTrapezoidIndex).upperLeftTrapezoidIndex = t;
                        trap(tn).lowerLeftTrapezoidIndex = -1;
                        trap(tn).lowerRightTrapezoidIndex = -1;
                    }
                    else {
                        trap(trap(tn).lowerRightTrapezoidIndex).upperRightTrapezoidIndex = tn;
                        trap(t).lowerLeftTrapezoidIndex = -1;
                        trap(t).lowerRightTrapezoidIndex = -1;
                    }
                }
                else {
                    if ((trap(trap(t).lowerRightTrapezoidIndex).upperLeftTrapezoidIndex > 0) &&
                        (trap(trap(t).lowerRightTrapezoidIndex).upperRightTrapezoidIndex > 0)) {
                        if (trap(trap(t).lowerRightTrapezoidIndex).upperLeftTrapezoidIndex == t) {
                            trap(trap(t).lowerRightTrapezoidIndex).savedUpperNeighborIndex = trap(trap(t).lowerRightTrapezoidIndex).upperRightTrapezoidIndex;
                            trap(trap(t).lowerRightTrapezoidIndex).savedUpperNeighborSide = _Construct.S_LEFT;
                        }
                        else {
                            trap(trap(t).lowerRightTrapezoidIndex).savedUpperNeighborIndex = trap(trap(t).lowerRightTrapezoidIndex).upperLeftTrapezoidIndex;
                            trap(trap(t).lowerRightTrapezoidIndex).savedUpperNeighborSide = _Construct.S_RIGHT;
                        }
                    }
                    trap(trap(t).lowerRightTrapezoidIndex).upperLeftTrapezoidIndex = t;
                    trap(trap(t).lowerRightTrapezoidIndex).upperRightTrapezoidIndex = tn;
                }
                t = trap(t).lowerRightTrapezoidIndex;
            }
            else {
                double y0;
                double yt;
                _Point2D tmpPoint = new _Point2D();
                int tnext;
                boolean isD0;

                isD0 = false;
                if (_Construct.fpEqual(trap(t).lowerPoint.y, s.startPoint.y)) {
                    if (trap(t).lowerPoint.x > s.startPoint.x) {
                        isD0 = true;
                    }
                }
                else {
                    tmpPoint.y = y0 = trap(t).lowerPoint.y;
                    yt = (y0 - s.startPoint.y) / (s.endPoint.y - s.startPoint.y);
                    tmpPoint.x = s.startPoint.x + yt * (s.endPoint.x - s.startPoint.x);

                    if (_Construct.lessThan(tmpPoint, trap(t).lowerPoint)) {
                        isD0 = true;
                    }
                }

                if ((trap(t).upperLeftTrapezoidIndex > 0) &&
                    (trap(t).upperRightTrapezoidIndex > 0)) {
                    if (trap(t).savedUpperNeighborIndex > 0) {
                        if (trap(t).savedUpperNeighborSide == _Construct.S_LEFT) {
                            trap(tn).upperLeftTrapezoidIndex = trap(t).upperRightTrapezoidIndex;
                            trap(t).upperRightTrapezoidIndex = -1;
                            trap(tn).upperRightTrapezoidIndex = trap(t).savedUpperNeighborIndex;

                            trap(trap(t).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                            trap(trap(tn).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = tn;
                            trap(trap(tn).upperRightTrapezoidIndex).lowerLeftTrapezoidIndex = tn;
                        }
                        else {
                            trap(tn).upperRightTrapezoidIndex = -1;
                            trap(tn).upperLeftTrapezoidIndex = trap(t).upperRightTrapezoidIndex;
                            trap(t).upperRightTrapezoidIndex = trap(t).upperLeftTrapezoidIndex;
                            trap(t).upperLeftTrapezoidIndex = trap(t).savedUpperNeighborIndex;

                            trap(trap(t).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                            trap(trap(t).upperRightTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                            trap(trap(tn).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = tn;
                        }

                        trap(t).savedUpperNeighborIndex = 0;
                        trap(tn).savedUpperNeighborIndex = 0;
                    }
                    else {
                        trap(tn).upperLeftTrapezoidIndex = trap(t).upperRightTrapezoidIndex;
                        trap(tn).upperRightTrapezoidIndex = -1;
                        trap(t).upperRightTrapezoidIndex = -1;
                        trap(trap(tn).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = tn;
                    }
                }
                else {
                    int tmpUpperIndex = trap(t).upperLeftTrapezoidIndex;
                    int td0;
                    int td1;

                    td0 = trap(tmpUpperIndex).lowerLeftTrapezoidIndex;
                    td1 = trap(tmpUpperIndex).lowerRightTrapezoidIndex;
                    if (td0 > 0 && td1 > 0) {
                        if ((trap(td0).rightSegmentIndex > 0) && !isLeftOf(trap(td0).rightSegmentIndex, s.endPoint)) {
                            trap(t).upperLeftTrapezoidIndex = -1;
                            trap(t).upperRightTrapezoidIndex = -1;
                            trap(tn).upperRightTrapezoidIndex = -1;
                            trap(trap(tn).upperLeftTrapezoidIndex).lowerRightTrapezoidIndex = tn;
                        }
                        else {
                            trap(tn).upperLeftTrapezoidIndex = -1;
                            trap(tn).upperRightTrapezoidIndex = -1;
                            trap(t).upperRightTrapezoidIndex = -1;
                            trap(trap(t).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                        }
                    }
                    else {
                        trap(trap(t).upperLeftTrapezoidIndex).lowerLeftTrapezoidIndex = t;
                        trap(trap(t).upperLeftTrapezoidIndex).lowerRightTrapezoidIndex = tn;
                    }
                }

                if (_Construct.fpEqual(trap(t).lowerPoint.y, trap(tlast).lowerPoint.y) &&
                    _Construct.fpEqual(trap(t).lowerPoint.x, trap(tlast).lowerPoint.x) &&
                    tribot != 0) {
                    if (isSwapped.value) {
                        tmptriseg = _Construct.segments.get(segmentIndex).previousSegmentIndex;
                    }
                    else {
                        tmptriseg = _Construct.segments.get(segmentIndex).nextSegmentIndex;
                    }

                    if ((tmptriseg > 0) && isLeftOf(tmptriseg, s.startPoint)) {
                        trap(trap(t).lowerLeftTrapezoidIndex).upperLeftTrapezoidIndex = t;
                        trap(tn).lowerLeftTrapezoidIndex = -1;
                        trap(tn).lowerRightTrapezoidIndex = -1;
                    }
                    else {
                        trap(trap(tn).lowerLeftTrapezoidIndex).upperRightTrapezoidIndex = tn;
                        trap(t).lowerLeftTrapezoidIndex = -1;
                        trap(t).lowerRightTrapezoidIndex = -1;
                    }
                    tnext = trap(t).lowerRightTrapezoidIndex;
                }
                else if (isD0) {
                    trap(trap(t).lowerLeftTrapezoidIndex).upperLeftTrapezoidIndex = t;
                    trap(trap(t).lowerLeftTrapezoidIndex).upperRightTrapezoidIndex = tn;
                    trap(trap(t).lowerRightTrapezoidIndex).upperLeftTrapezoidIndex = tn;
                    trap(trap(t).lowerRightTrapezoidIndex).upperRightTrapezoidIndex = -1;

                    trap(t).lowerRightTrapezoidIndex = -1;

                    tnext = trap(t).lowerLeftTrapezoidIndex;
                }
                else {
                    trap(trap(t).lowerLeftTrapezoidIndex).upperLeftTrapezoidIndex = t;
                    trap(trap(t).lowerLeftTrapezoidIndex).upperRightTrapezoidIndex = -1;
                    trap(trap(t).lowerRightTrapezoidIndex).upperLeftTrapezoidIndex = t;
                    trap(trap(t).lowerRightTrapezoidIndex).upperRightTrapezoidIndex = tn;

                    trap(tn).lowerLeftTrapezoidIndex = trap(t).lowerRightTrapezoidIndex;
                    trap(tn).lowerRightTrapezoidIndex = -1;

                    tnext = trap(t).lowerRightTrapezoidIndex;
                }

                t = tnext;
            }
            trap(savedTrapezoidIndex).rightSegmentIndex = segmentIndex;
            trap(savedNewTrapezoidIndex).leftSegmentIndex = segmentIndex;
        }

        tfirstl = tfirst;
        tlastl = tlast;
        mergeTrapezoids(segmentIndex, tfirstl, tlastl, _Construct.S_LEFT);
        mergeTrapezoids(segmentIndex, tfirstr, tlastr, _Construct.S_RIGHT);
        _Construct.segments.get(segmentIndex).hasBeenInserted = true;
        return 0;
    }
}
