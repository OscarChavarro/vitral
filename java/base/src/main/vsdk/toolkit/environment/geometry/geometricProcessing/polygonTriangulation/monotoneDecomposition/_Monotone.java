package vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition;

import java.util.Arrays;

public final class _Monotone {
    public static final int SP_SIMPLE_LRUP = 1;
    public static final int SP_SIMPLE_LRDN = 2;
    public static final int SP_2UP_2DN = 3;
    public static final int SP_2UP_LEFT = 4;
    public static final int SP_2UP_RIGHT = 5;
    public static final int SP_2DN_LEFT = 6;
    public static final int SP_2DN_RIGHT = 7;
    public static final int SP_NOSPLIT = -1;

    public static final int TR_FROM_UP = 1;
    public static final int TR_FROM_DN = 2;

    public static final int TRI_LHS = 1;
    public static final int TRI_RHS = 2;

    private static final _MonotoneChainNode[] monotoneChainNodes = new _MonotoneChainNode[_Construct.TRAPEZOID_TABLE_SIZE];
    private static final _VertexChain[] vertexChains = new _VertexChain[_Construct.SEGMENT_SIZE];
    private static final int[] monotonePolygonEntryNode = new int[_Construct.SEGMENT_SIZE];
    private static final int[] visitedTrapezoids = new int[_Construct.TRAPEZOID_TABLE_SIZE];
    private static int nextChainNodeIndex;
    private static int nextOutputTriangleIndex;
    private static int nextMonotonePolygonIndex;

    static {
        for (int i = 0; i < monotoneChainNodes.length; i++) monotoneChainNodes[i] = new _MonotoneChainNode();
        for (int i = 0; i < vertexChains.length; i++) vertexChains[i] = new _VertexChain();
    }

    private _Monotone() {}

    private static int newMonotone() { return ++nextMonotonePolygonIndex; }
    private static int newChainElement() { return ++nextChainNodeIndex; }

    private static double crossSine(_Point2D firstVector, _Point2D secondVector) {
        return (firstVector.x * secondVector.y - secondVector.x * firstVector.y);
    }

    private static double length(_Point2D vector) {
        return Math.sqrt(vector.x * vector.x + vector.y * vector.y);
    }

    private static double getAngle(_Point2D basePoint, _Point2D nextPoint, _Point2D otherPoint) {
        _Point2D edgeFromBasePoint = new _Point2D(nextPoint.x - basePoint.x, nextPoint.y - basePoint.y);
        _Point2D edgeToOtherPoint = new _Point2D(otherPoint.x - basePoint.x, otherPoint.y - basePoint.y);

        if (crossSine(edgeFromBasePoint, edgeToOtherPoint) >= 0) {
            return _Construct.dot(edgeFromBasePoint, edgeToOtherPoint) / length(edgeFromBasePoint) / length(edgeToOtherPoint);
        }
        return (-1.0 * _Construct.dot(edgeFromBasePoint, edgeToOtherPoint) / length(edgeFromBasePoint) / length(edgeToOtherPoint) - 2);
    }

    private static int getVertexPositions(int v0, int v1, int[] ip, int[] iq) {
        _VertexChain vp0 = vertexChains[v0];
        _VertexChain vp1 = vertexChains[v1];
        double angle;
        double temp;
        int tp = 0;
        int tq = 0;

        angle = -4.0;
        for (int i = 0; i < 4; i++) {
            if (vp0.adjacentVertexIndices[i] <= 0) continue;
            temp = getAngle(vp0.point, vertexChains[vp0.adjacentVertexIndices[i]].point, vp1.point);
            if (temp > angle) {
                angle = temp;
                tp = i;
            }
        }
        ip[0] = tp;

        angle = -4.0;
        for (int i = 0; i < 4; i++) {
            if (vp1.adjacentVertexIndices[i] <= 0) continue;
            temp = getAngle(vp1.point, vertexChains[vp1.adjacentVertexIndices[i]].point, vp0.point);
            if (temp > angle) {
                angle = temp;
                tq = i;
            }
        }
        iq[0] = tq;
        return 0;
    }

    private static int makeNewMonotonePolygon(int mcur, int v0, int v1) {
        int[] ip = new int[1];
        int[] iq = new int[1];
        int mnew;

        if (v0 <= 0 || v1 <= 0) {
            return mcur;
        }

        mnew = newMonotone();

        _VertexChain vp0 = vertexChains[v0];
        _VertexChain vp1 = vertexChains[v1];

        getVertexPositions(v0, v1, ip, iq);

        int p = vp0.chainNodeIndicesByAdjacency[ip[0]];
        int q = vp1.chainNodeIndicesByAdjacency[iq[0]];

        int i = newChainElement();
        int j = newChainElement();

        monotoneChainNodes[i].vertexIndex = v0;
        monotoneChainNodes[j].vertexIndex = v1;

        monotoneChainNodes[i].nextNodeIndex = monotoneChainNodes[p].nextNodeIndex;
        monotoneChainNodes[monotoneChainNodes[p].nextNodeIndex].previousNodeIndex = i;
        monotoneChainNodes[i].previousNodeIndex = j;
        monotoneChainNodes[j].nextNodeIndex = i;
        monotoneChainNodes[j].previousNodeIndex = monotoneChainNodes[q].previousNodeIndex;
        monotoneChainNodes[monotoneChainNodes[q].previousNodeIndex].nextNodeIndex = j;

        monotoneChainNodes[p].nextNodeIndex = q;
        monotoneChainNodes[q].previousNodeIndex = p;

        int nf0 = vp0.adjacencySlotCount;
        int nf1 = vp1.adjacencySlotCount;

        vp0.adjacentVertexIndices[ip[0]] = v1;

        vp0.chainNodeIndicesByAdjacency[nf0] = i;
        vp0.adjacentVertexIndices[nf0] = monotoneChainNodes[monotoneChainNodes[i].nextNodeIndex].vertexIndex;
        vp1.chainNodeIndicesByAdjacency[nf1] = j;
        vp1.adjacentVertexIndices[nf1] = v0;

        vp0.adjacencySlotCount++;
        vp1.adjacencySlotCount++;

        monotonePolygonEntryNode[mcur] = p;
        monotonePolygonEntryNode[mnew] = i;
        return mnew;
    }

    public static int monotonateTrapezoids(int n) {
        Arrays.fill(visitedTrapezoids, 0);
        Arrays.fill(monotonePolygonEntryNode, 0);
        for (int i = 0; i < monotoneChainNodes.length; i++) {
            monotoneChainNodes[i] = new _MonotoneChainNode();
        }
        for (int i = 0; i < vertexChains.length; i++) {
            vertexChains[i] = new _VertexChain();
        }

        int trapezoidStart = -1;
        for (int i = 0; i < _Construct.TRAPEZOID_TABLE_SIZE; i++) {
            _TriangulationTrapezoid t = _Construct.trapezoidAt(i);
            if (t != null && t.insidePolygon() != 0) {
                trapezoidStart = i;
                break;
            }
        }
        if (trapezoidStart < 0) return 0;

        for (int i = 1; i <= n; i++) {
            monotoneChainNodes[i].previousNodeIndex = _Construct.segmentAt(i).previousSegmentIndex;
            monotoneChainNodes[i].nextNodeIndex = _Construct.segmentAt(i).nextSegmentIndex;
            monotoneChainNodes[i].vertexIndex = i;
            vertexChains[i].point.set(_Construct.segmentAt(i).startPoint);
            vertexChains[i].adjacentVertexIndices[0] = _Construct.segmentAt(i).nextSegmentIndex;
            vertexChains[i].chainNodeIndicesByAdjacency[0] = i;
            vertexChains[i].adjacencySlotCount = 1;
        }

        nextChainNodeIndex = n;
        nextMonotonePolygonIndex = 0;
        monotonePolygonEntryNode[0] = 1;

        _TriangulationTrapezoid start = _Construct.trapezoidAt(trapezoidStart);
        if (start.upperLeftTrapezoidIndex > 0) {
            traversePolygon(0, trapezoidStart, start.upperLeftTrapezoidIndex, TR_FROM_UP);
        }
        else if (start.lowerLeftTrapezoidIndex > 0) {
            traversePolygon(0, trapezoidStart, start.lowerLeftTrapezoidIndex, TR_FROM_DN);
        }

        return newMonotone();
    }

    private static int traversePolygon(int mcur, int trnum, int from, int dir) {
        _TriangulationTrapezoid trapezoid;
        int mnew;
        int v0;
        int v1;
        int retval = 0;

        if ((trnum <= 0) || visitedTrapezoids[trnum] != 0) return 0;
        if (_Construct.trapezoidAt(trnum) == null) return 0;
        trapezoid = _Construct.trapezoidAt(trnum);
        visitedTrapezoids[trnum] = 1;

        if ((trapezoid.upperLeftTrapezoidIndex <= 0) && (trapezoid.upperRightTrapezoidIndex <= 0)) {
            if ((trapezoid.lowerLeftTrapezoidIndex > 0) && (trapezoid.lowerRightTrapezoidIndex > 0)) {
                v0 = _Construct.trapezoidAt(trapezoid.lowerRightTrapezoidIndex).leftSegmentIndex;
                v1 = trapezoid.leftSegmentIndex;
                if (from == trapezoid.lowerRightTrapezoidIndex) {
                    mnew = makeNewMonotonePolygon(mcur, v1, v0);
                    traversePolygon(mcur, trapezoid.lowerRightTrapezoidIndex, trnum, TR_FROM_UP);
                    traversePolygon(mnew, trapezoid.lowerLeftTrapezoidIndex, trnum, TR_FROM_UP);
                }
                else {
                    mnew = makeNewMonotonePolygon(mcur, v0, v1);
                    traversePolygon(mcur, trapezoid.lowerLeftTrapezoidIndex, trnum, TR_FROM_UP);
                    traversePolygon(mnew, trapezoid.lowerRightTrapezoidIndex, trnum, TR_FROM_UP);
                }
            }
            else {
                retval = SP_NOSPLIT;
                traversePolygon(mcur, trapezoid.upperLeftTrapezoidIndex, trnum, TR_FROM_DN);
                traversePolygon(mcur, trapezoid.upperRightTrapezoidIndex, trnum, TR_FROM_DN);
                traversePolygon(mcur, trapezoid.lowerLeftTrapezoidIndex, trnum, TR_FROM_UP);
                traversePolygon(mcur, trapezoid.lowerRightTrapezoidIndex, trnum, TR_FROM_UP);
            }
        }
        else if ((trapezoid.lowerLeftTrapezoidIndex <= 0) && (trapezoid.lowerRightTrapezoidIndex <= 0)) {
            if ((trapezoid.upperLeftTrapezoidIndex > 0) && (trapezoid.upperRightTrapezoidIndex > 0)) {
                v0 = trapezoid.rightSegmentIndex;
                v1 = _Construct.trapezoidAt(trapezoid.upperLeftTrapezoidIndex).rightSegmentIndex;
                if (from == trapezoid.upperRightTrapezoidIndex) {
                    mnew = makeNewMonotonePolygon(mcur, v1, v0);
                    traversePolygon(mcur, trapezoid.upperRightTrapezoidIndex, trnum, TR_FROM_DN);
                    traversePolygon(mnew, trapezoid.upperLeftTrapezoidIndex, trnum, TR_FROM_DN);
                }
                else {
                    mnew = makeNewMonotonePolygon(mcur, v0, v1);
                    traversePolygon(mcur, trapezoid.upperLeftTrapezoidIndex, trnum, TR_FROM_DN);
                    traversePolygon(mnew, trapezoid.upperRightTrapezoidIndex, trnum, TR_FROM_DN);
                }
            }
            else {
                retval = SP_NOSPLIT;
                traversePolygon(mcur, trapezoid.upperLeftTrapezoidIndex, trnum, TR_FROM_DN);
                traversePolygon(mcur, trapezoid.upperRightTrapezoidIndex, trnum, TR_FROM_DN);
                traversePolygon(mcur, trapezoid.lowerLeftTrapezoidIndex, trnum, TR_FROM_UP);
                traversePolygon(mcur, trapezoid.lowerRightTrapezoidIndex, trnum, TR_FROM_UP);
            }
        }
        else if ((trapezoid.upperLeftTrapezoidIndex > 0) && (trapezoid.upperRightTrapezoidIndex > 0)) {
            if ((trapezoid.lowerLeftTrapezoidIndex > 0) && (trapezoid.lowerRightTrapezoidIndex > 0)) {
                v0 = _Construct.trapezoidAt(trapezoid.lowerRightTrapezoidIndex).leftSegmentIndex;
                v1 = _Construct.trapezoidAt(trapezoid.upperLeftTrapezoidIndex).rightSegmentIndex;
                retval = SP_2UP_2DN;
                if (((dir == TR_FROM_DN) && (trapezoid.lowerRightTrapezoidIndex == from)) ||
                    ((dir == TR_FROM_UP) && (trapezoid.upperRightTrapezoidIndex == from))) {
                    mnew = makeNewMonotonePolygon(mcur, v1, v0);
                    traversePolygon(mcur, trapezoid.upperRightTrapezoidIndex, trnum, TR_FROM_DN);
                    traversePolygon(mcur, trapezoid.lowerRightTrapezoidIndex, trnum, TR_FROM_UP);
                    traversePolygon(mnew, trapezoid.upperLeftTrapezoidIndex, trnum, TR_FROM_DN);
                    traversePolygon(mnew, trapezoid.lowerLeftTrapezoidIndex, trnum, TR_FROM_UP);
                }
                else {
                    mnew = makeNewMonotonePolygon(mcur, v0, v1);
                    traversePolygon(mcur, trapezoid.upperLeftTrapezoidIndex, trnum, TR_FROM_DN);
                    traversePolygon(mcur, trapezoid.lowerLeftTrapezoidIndex, trnum, TR_FROM_UP);
                    traversePolygon(mnew, trapezoid.upperRightTrapezoidIndex, trnum, TR_FROM_DN);
                    traversePolygon(mnew, trapezoid.lowerRightTrapezoidIndex, trnum, TR_FROM_UP);
                }
            }
            else {
                if (_Construct.equalTo(trapezoid.lowerPoint, _Construct.segmentAt(trapezoid.leftSegmentIndex).endPoint)) {
                    v0 = _Construct.trapezoidAt(trapezoid.upperLeftTrapezoidIndex).rightSegmentIndex;
                    v1 = _Construct.segmentAt(trapezoid.leftSegmentIndex).nextSegmentIndex;
                    retval = SP_2UP_LEFT;
                    if ((dir == TR_FROM_UP) && (trapezoid.upperLeftTrapezoidIndex == from)) {
                        mnew = makeNewMonotonePolygon(mcur, v1, v0);
                        traversePolygon(mcur, trapezoid.upperLeftTrapezoidIndex, trnum, TR_FROM_DN);
                        traversePolygon(mnew, trapezoid.lowerLeftTrapezoidIndex, trnum, TR_FROM_UP);
                        traversePolygon(mnew, trapezoid.upperRightTrapezoidIndex, trnum, TR_FROM_DN);
                        traversePolygon(mnew, trapezoid.lowerRightTrapezoidIndex, trnum, TR_FROM_UP);
                    }
                    else {
                        mnew = makeNewMonotonePolygon(mcur, v0, v1);
                        traversePolygon(mcur, trapezoid.upperRightTrapezoidIndex, trnum, TR_FROM_DN);
                        traversePolygon(mcur, trapezoid.lowerLeftTrapezoidIndex, trnum, TR_FROM_UP);
                        traversePolygon(mcur, trapezoid.lowerRightTrapezoidIndex, trnum, TR_FROM_UP);
                        traversePolygon(mnew, trapezoid.upperLeftTrapezoidIndex, trnum, TR_FROM_DN);
                    }
                }
                else {
                    v0 = trapezoid.rightSegmentIndex;
                    v1 = _Construct.trapezoidAt(trapezoid.upperLeftTrapezoidIndex).rightSegmentIndex;
                    retval = SP_2UP_RIGHT;
                    if ((dir == TR_FROM_UP) && (trapezoid.upperRightTrapezoidIndex == from)) {
                        mnew = makeNewMonotonePolygon(mcur, v1, v0);
                        traversePolygon(mcur, trapezoid.upperRightTrapezoidIndex, trnum, TR_FROM_DN);
                        traversePolygon(mnew, trapezoid.lowerRightTrapezoidIndex, trnum, TR_FROM_UP);
                        traversePolygon(mnew, trapezoid.lowerLeftTrapezoidIndex, trnum, TR_FROM_UP);
                        traversePolygon(mnew, trapezoid.upperLeftTrapezoidIndex, trnum, TR_FROM_DN);
                    }
                    else {
                        mnew = makeNewMonotonePolygon(mcur, v0, v1);
                        traversePolygon(mcur, trapezoid.upperLeftTrapezoidIndex, trnum, TR_FROM_DN);
                        traversePolygon(mcur, trapezoid.lowerLeftTrapezoidIndex, trnum, TR_FROM_UP);
                        traversePolygon(mcur, trapezoid.lowerRightTrapezoidIndex, trnum, TR_FROM_UP);
                        traversePolygon(mnew, trapezoid.upperRightTrapezoidIndex, trnum, TR_FROM_DN);
                    }
                }
            }
        }
        else if ((trapezoid.upperLeftTrapezoidIndex > 0) || (trapezoid.upperRightTrapezoidIndex > 0)) {
            if ((trapezoid.lowerLeftTrapezoidIndex > 0) && (trapezoid.lowerRightTrapezoidIndex > 0)) {
                if (_Construct.equalTo(trapezoid.upperPoint, _Construct.segmentAt(trapezoid.leftSegmentIndex).startPoint)) {
                    v0 = _Construct.trapezoidAt(trapezoid.lowerRightTrapezoidIndex).leftSegmentIndex;
                    v1 = trapezoid.leftSegmentIndex;
                    retval = SP_2DN_LEFT;
                    if (!((dir == TR_FROM_DN) && (trapezoid.lowerLeftTrapezoidIndex == from))) {
                        mnew = makeNewMonotonePolygon(mcur, v1, v0);
                        traversePolygon(mcur, trapezoid.upperRightTrapezoidIndex, trnum, TR_FROM_DN);
                        traversePolygon(mcur, trapezoid.lowerRightTrapezoidIndex, trnum, TR_FROM_UP);
                        traversePolygon(mcur, trapezoid.upperLeftTrapezoidIndex, trnum, TR_FROM_DN);
                        traversePolygon(mnew, trapezoid.lowerLeftTrapezoidIndex, trnum, TR_FROM_UP);
                    }
                    else {
                        mnew = makeNewMonotonePolygon(mcur, v0, v1);
                        traversePolygon(mcur, trapezoid.lowerLeftTrapezoidIndex, trnum, TR_FROM_UP);
                        traversePolygon(mnew, trapezoid.upperLeftTrapezoidIndex, trnum, TR_FROM_DN);
                        traversePolygon(mnew, trapezoid.upperRightTrapezoidIndex, trnum, TR_FROM_DN);
                        traversePolygon(mnew, trapezoid.lowerRightTrapezoidIndex, trnum, TR_FROM_UP);
                    }
                }
                else {
                    v0 = _Construct.trapezoidAt(trapezoid.lowerRightTrapezoidIndex).leftSegmentIndex;
                    v1 = _Construct.segmentAt(trapezoid.rightSegmentIndex).nextSegmentIndex;
                    retval = SP_2DN_RIGHT;
                    if ((dir == TR_FROM_DN) && (trapezoid.lowerRightTrapezoidIndex == from)) {
                        mnew = makeNewMonotonePolygon(mcur, v1, v0);
                        traversePolygon(mcur, trapezoid.lowerRightTrapezoidIndex, trnum, TR_FROM_UP);
                        traversePolygon(mnew, trapezoid.upperRightTrapezoidIndex, trnum, TR_FROM_DN);
                        traversePolygon(mnew, trapezoid.upperLeftTrapezoidIndex, trnum, TR_FROM_DN);
                        traversePolygon(mnew, trapezoid.lowerLeftTrapezoidIndex, trnum, TR_FROM_UP);
                    }
                    else {
                        mnew = makeNewMonotonePolygon(mcur, v0, v1);
                        traversePolygon(mcur, trapezoid.upperLeftTrapezoidIndex, trnum, TR_FROM_DN);
                        traversePolygon(mcur, trapezoid.lowerLeftTrapezoidIndex, trnum, TR_FROM_UP);
                        traversePolygon(mcur, trapezoid.upperRightTrapezoidIndex, trnum, TR_FROM_DN);
                        traversePolygon(mnew, trapezoid.lowerRightTrapezoidIndex, trnum, TR_FROM_UP);
                    }
                }
            }
            else {
                if (trapezoid.leftSegmentIndex <= 0 || trapezoid.rightSegmentIndex <= 0) {
                    retval = SP_NOSPLIT;
                    traversePolygon(mcur, trapezoid.upperLeftTrapezoidIndex, trnum, TR_FROM_DN);
                    traversePolygon(mcur, trapezoid.upperRightTrapezoidIndex, trnum, TR_FROM_DN);
                    traversePolygon(mcur, trapezoid.lowerLeftTrapezoidIndex, trnum, TR_FROM_UP);
                    traversePolygon(mcur, trapezoid.lowerRightTrapezoidIndex, trnum, TR_FROM_UP);
                    return retval;
                }
                if (_Construct.equalTo(trapezoid.upperPoint, _Construct.segmentAt(trapezoid.leftSegmentIndex).startPoint) &&
                    _Construct.equalTo(trapezoid.lowerPoint, _Construct.segmentAt(trapezoid.rightSegmentIndex).startPoint)) {
                    v0 = trapezoid.rightSegmentIndex;
                    v1 = trapezoid.leftSegmentIndex;
                    retval = SP_SIMPLE_LRDN;
                    if (dir == TR_FROM_UP) {
                        mnew = makeNewMonotonePolygon(mcur, v1, v0);
                        traversePolygon(mcur, trapezoid.upperLeftTrapezoidIndex, trnum, TR_FROM_DN);
                        traversePolygon(mcur, trapezoid.upperRightTrapezoidIndex, trnum, TR_FROM_DN);
                        traversePolygon(mnew, trapezoid.lowerRightTrapezoidIndex, trnum, TR_FROM_UP);
                        traversePolygon(mnew, trapezoid.lowerLeftTrapezoidIndex, trnum, TR_FROM_UP);
                    }
                    else {
                        mnew = makeNewMonotonePolygon(mcur, v0, v1);
                        traversePolygon(mcur, trapezoid.lowerRightTrapezoidIndex, trnum, TR_FROM_UP);
                        traversePolygon(mcur, trapezoid.lowerLeftTrapezoidIndex, trnum, TR_FROM_UP);
                        traversePolygon(mnew, trapezoid.upperLeftTrapezoidIndex, trnum, TR_FROM_DN);
                        traversePolygon(mnew, trapezoid.upperRightTrapezoidIndex, trnum, TR_FROM_DN);
                    }
                }
                else if (_Construct.equalTo(trapezoid.upperPoint, _Construct.segmentAt(trapezoid.rightSegmentIndex).endPoint) &&
                         _Construct.equalTo(trapezoid.lowerPoint, _Construct.segmentAt(trapezoid.leftSegmentIndex).endPoint)) {
                    v0 = _Construct.segmentAt(trapezoid.rightSegmentIndex).nextSegmentIndex;
                    v1 = _Construct.segmentAt(trapezoid.leftSegmentIndex).nextSegmentIndex;
                    retval = SP_SIMPLE_LRUP;
                    if (dir == TR_FROM_UP) {
                        mnew = makeNewMonotonePolygon(mcur, v1, v0);
                        traversePolygon(mcur, trapezoid.upperLeftTrapezoidIndex, trnum, TR_FROM_DN);
                        traversePolygon(mcur, trapezoid.upperRightTrapezoidIndex, trnum, TR_FROM_DN);
                        traversePolygon(mnew, trapezoid.lowerRightTrapezoidIndex, trnum, TR_FROM_UP);
                        traversePolygon(mnew, trapezoid.lowerLeftTrapezoidIndex, trnum, TR_FROM_UP);
                    }
                    else {
                        mnew = makeNewMonotonePolygon(mcur, v0, v1);
                        traversePolygon(mcur, trapezoid.lowerRightTrapezoidIndex, trnum, TR_FROM_UP);
                        traversePolygon(mcur, trapezoid.lowerLeftTrapezoidIndex, trnum, TR_FROM_UP);
                        traversePolygon(mnew, trapezoid.upperLeftTrapezoidIndex, trnum, TR_FROM_DN);
                        traversePolygon(mnew, trapezoid.upperRightTrapezoidIndex, trnum, TR_FROM_DN);
                    }
                }
                else {
                    retval = SP_NOSPLIT;
                    traversePolygon(mcur, trapezoid.upperLeftTrapezoidIndex, trnum, TR_FROM_DN);
                    traversePolygon(mcur, trapezoid.lowerLeftTrapezoidIndex, trnum, TR_FROM_UP);
                    traversePolygon(mcur, trapezoid.upperRightTrapezoidIndex, trnum, TR_FROM_DN);
                    traversePolygon(mcur, trapezoid.lowerRightTrapezoidIndex, trnum, TR_FROM_UP);
                }
            }
        }

        return retval;
    }

    public static int triangulateMonotonePolygons(int numberOfVertices, int numberOfMonotonePolygons, int[][] op) {
        int i;
        _Point2D yMaxPoint;
        _Point2D yMinPoint;
        int p;
        int vfirst;
        int posmax;
        int v;
        int vcount;
        boolean processed;

        nextOutputTriangleIndex = 0;
        for (i = 0; i < numberOfMonotonePolygons; i++) {
            vcount = 1;
            processed = false;
            vfirst = monotoneChainNodes[monotonePolygonEntryNode[i]].vertexIndex;
            yMaxPoint = new _Point2D(vertexChains[vfirst].point);
            yMinPoint = new _Point2D(vertexChains[vfirst].point);
            posmax = monotonePolygonEntryNode[i];
            monotoneChainNodes[monotonePolygonEntryNode[i]].isMarked = true;
            p = monotoneChainNodes[monotonePolygonEntryNode[i]].nextNodeIndex;
            while ((v = monotoneChainNodes[p].vertexIndex) != vfirst) {
                if (monotoneChainNodes[p].isMarked) {
                    processed = true;
                    break;
                }
                else {
                    monotoneChainNodes[p].isMarked = true;
                }

                if (_Construct.greaterThan(vertexChains[v].point, yMaxPoint)) {
                    yMaxPoint = new _Point2D(vertexChains[v].point);
                    posmax = p;
                }
                if (_Construct.lessThan(vertexChains[v].point, yMinPoint)) {
                    yMinPoint = new _Point2D(vertexChains[v].point);
                }
                p = monotoneChainNodes[p].nextNodeIndex;
                vcount++;
            }

            if (processed) continue;

            if (vcount == 3) {
                op[nextOutputTriangleIndex][0] = monotoneChainNodes[p].vertexIndex;
                op[nextOutputTriangleIndex][1] = monotoneChainNodes[monotoneChainNodes[p].nextNodeIndex].vertexIndex;
                op[nextOutputTriangleIndex][2] = monotoneChainNodes[monotoneChainNodes[p].previousNodeIndex].vertexIndex;
                nextOutputTriangleIndex++;
            }
            else {
                v = monotoneChainNodes[monotoneChainNodes[posmax].nextNodeIndex].vertexIndex;
                if (_Construct.equalTo(vertexChains[v].point, yMinPoint)) {
                    triangulateSinglePolygon(numberOfVertices, posmax, TRI_LHS, op);
                }
                else {
                    triangulateSinglePolygon(numberOfVertices, posmax, TRI_RHS, op);
                }
            }
        }

        return nextOutputTriangleIndex;
    }

    private static int triangulateSinglePolygon(int nvert, int posmax, int side, int[][] op) {
        int v;
        int[] rc = new int[_Construct.SEGMENT_SIZE];
        int ri = 0;
        int endv;
        int tmp;
        int chainNodeIndicesByAdjacency;

        if (side == TRI_RHS) {
            rc[0] = monotoneChainNodes[posmax].vertexIndex;
            tmp = monotoneChainNodes[posmax].nextNodeIndex;
            rc[1] = monotoneChainNodes[tmp].vertexIndex;
            ri = 1;

            chainNodeIndicesByAdjacency = monotoneChainNodes[tmp].nextNodeIndex;
            v = monotoneChainNodes[chainNodeIndicesByAdjacency].vertexIndex;

            endv = monotoneChainNodes[monotoneChainNodes[posmax].previousNodeIndex].vertexIndex;
            if (endv == 0) endv = nvert;
        }
        else {
            tmp = monotoneChainNodes[posmax].nextNodeIndex;
            rc[0] = monotoneChainNodes[tmp].vertexIndex;
            tmp = monotoneChainNodes[tmp].nextNodeIndex;
            rc[1] = monotoneChainNodes[tmp].vertexIndex;
            ri = 1;

            chainNodeIndicesByAdjacency = monotoneChainNodes[tmp].nextNodeIndex;
            v = monotoneChainNodes[chainNodeIndicesByAdjacency].vertexIndex;

            endv = monotoneChainNodes[posmax].vertexIndex;
        }

        while ((v != endv) || (ri > 1)) {
            if (ri > 0) {
                if (_Construct.cross(vertexChains[v].point, vertexChains[rc[ri - 1]].point, vertexChains[rc[ri]].point) > 0) {
                    op[nextOutputTriangleIndex][0] = rc[ri - 1];
                    op[nextOutputTriangleIndex][1] = rc[ri];
                    op[nextOutputTriangleIndex][2] = v;
                    nextOutputTriangleIndex++;
                    ri--;
                }
                else {
                    ri++;
                    rc[ri] = v;
                    chainNodeIndicesByAdjacency = monotoneChainNodes[chainNodeIndicesByAdjacency].nextNodeIndex;
                    v = monotoneChainNodes[chainNodeIndicesByAdjacency].vertexIndex;
                }
            }
            else {
                rc[++ri] = v;
                chainNodeIndicesByAdjacency = monotoneChainNodes[chainNodeIndicesByAdjacency].nextNodeIndex;
                v = monotoneChainNodes[chainNodeIndicesByAdjacency].vertexIndex;
            }
        }

        op[nextOutputTriangleIndex][0] = rc[ri - 1];
        op[nextOutputTriangleIndex][1] = rc[ri];
        op[nextOutputTriangleIndex][2] = v;
        nextOutputTriangleIndex++;

        return 0;
    }
}
