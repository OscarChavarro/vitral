package vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition;

import java.util.ArrayList;

public final class _Construct {
    public static final int T_X = 1;
    public static final int T_Y = 2;
    public static final int T_SINK = 3;

    public static final int SEGMENT_SIZE = 200;
    public static final int QSIZE = 8 * SEGMENT_SIZE;
    public static final int TRAPEZOID_TABLE_SIZE = 4 * SEGMENT_SIZE;

    public static final int SEGMENT_FIRST_ENDPOINT = 1;
    public static final int SEGMENT_LAST_ENDPOINT = 2;

    public static final int TRIANGULATION_INFINITY = 1 << 30;
    public static final double TRIANGULATOR_EPSILON = 1.0e-7;

    public static final int S_LEFT = 1;
    public static final int S_RIGHT = 2;

    public static final int ST_VALID = 1;
    public static final int ST_INVALID = 2;

    public static int nextQueryNodeIndex;
    public static int nextTrapezoidIndex;
    public static ArrayList<_TriangulationTrapezoidQueryNode> queryNodes = new ArrayList<>();
    public static ArrayList<_TriangulationTrapezoid> trapezoids = new ArrayList<>();
    public static ArrayList<_TriangulationSegment> segments = new ArrayList<>();

    private _Construct() {}

    public static void resetTrapezoidQueryNodes() {
        for (int i = 0; i < queryNodes.size(); i++) {
            queryNodes.set(i, null);
        }
        nextQueryNodeIndex = 1;
    }

    public static void resetTrapezoids() {
        for (int i = 0; i < trapezoids.size(); i++) {
            trapezoids.set(i, null);
        }
        nextTrapezoidIndex = 1;
    }

    public static void prepareStorage(int nseg) {
        int segCapacity = nseg + 1;
        int trapCapacity = Math.max(16 * nseg + 16, TRAPEZOID_TABLE_SIZE);
        int queryCapacity = Math.max(32 * nseg + 32, QSIZE);

        if (segments.size() != segCapacity) {
            segments.clear();
            for (int i = 0; i < segCapacity; ++i) segments.add(new _TriangulationSegment());
        }

        if (trapezoids.size() != trapCapacity) {
            trapezoids.clear();
            for (int i = 0; i < trapCapacity; ++i) trapezoids.add(null);
        } else {
            for (int i = 0; i < trapezoids.size(); ++i) trapezoids.set(i, null);
        }

        if (queryNodes.size() != queryCapacity) {
            queryNodes.clear();
            for (int i = 0; i < queryCapacity; ++i) queryNodes.add(null);
        } else {
            for (int i = 0; i < queryNodes.size(); ++i) queryNodes.set(i, null);
        }

        nextQueryNodeIndex = 1;
        nextTrapezoidIndex = 1;
    }

    private static void max(_Point2D out, _Point2D a, _Point2D b) {
        if (a.y > b.y + TRIANGULATOR_EPSILON) out.set(a);
        else if (fpEqual(a.y, b.y)) out.set((a.x > b.x + TRIANGULATOR_EPSILON) ? a : b);
        else out.set(b);
    }

    private static void min(_Point2D out, _Point2D a, _Point2D b) {
        if (a.y < b.y - TRIANGULATOR_EPSILON) out.set(a);
        else if (fpEqual(a.y, b.y)) out.set((a.x < b.x) ? a : b);
        else out.set(b);
    }

    public static boolean greaterThan(_Point2D a, _Point2D b) {
        if (a.y > b.y + TRIANGULATOR_EPSILON) return true;
        if (a.y < b.y - TRIANGULATOR_EPSILON) return false;
        return a.x > b.x;
    }

    public static boolean equalTo(_Point2D a, _Point2D b) {
        return fpEqual(a.y, b.y) && fpEqual(a.x, b.x);
    }

    public static boolean greaterThanEqualTo(_Point2D a, _Point2D b) {
        if (a.y > b.y + TRIANGULATOR_EPSILON) return true;
        if (a.y < b.y - TRIANGULATOR_EPSILON) return false;
        return a.x >= b.x;
    }

    public static boolean lessThan(_Point2D a, _Point2D b) {
        if (a.y < b.y - TRIANGULATOR_EPSILON) return true;
        if (a.y > b.y + TRIANGULATOR_EPSILON) return false;
        return a.x < b.x;
    }

    public static double cross(_Point2D v0, _Point2D v1, _Point2D v2) {
        return ((v1.x - v0.x) * (v2.y - v0.y) - (v1.y - v0.y) * (v2.x - v0.x));
    }

    public static double dot(_Point2D v0, _Point2D v1) {
        return (v0.x * v1.x + v0.y * v1.y);
    }

    public static boolean fpEqual(double firstValue, double secondValue) {
        return Math.abs(firstValue - secondValue) <= TRIANGULATOR_EPSILON;
    }

    public static _TriangulationSegment segmentAt(int index) { return segments.get(index); }
    public static void setSegmentInserted(int index, boolean value) { segments.get(index).hasBeenInserted = value; }
    public static _TriangulationTrapezoid trapezoidAt(int index) { return trapezoids.get(index); }
    public static void setTrapezoidAt(int index, _TriangulationTrapezoid trapezoid) { trapezoids.set(index, trapezoid); }

    public static _TriangulationTrapezoidQueryNode allocateQueryNode() {
        if (nextQueryNodeIndex < queryNodes.size()) {
            _TriangulationTrapezoidQueryNode n = new _TriangulationTrapezoidQueryNode();
            queryNodes.set(nextQueryNodeIndex, n);
            nextQueryNodeIndex++;
            return n;
        }
        return null;
    }

    public static int allocateTrapezoidIndex() {
        if (nextTrapezoidIndex >= trapezoids.size()) return -1;
        return nextTrapezoidIndex++;
    }

    private static _TriangulationTrapezoidQueryNode initQueryStructure(int segmentIndex) {
        _TriangulationSegment s = segments.get(segmentIndex);
        nextQueryNodeIndex = 1;
        resetTrapezoids();
        resetTrapezoidQueryNodes();

        _TriangulationTrapezoidQueryNode i1 = allocateQueryNode();
        i1.queryNodeType = T_Y;
        max(i1.splitPoint, s.startPoint, s.endPoint);
        _TriangulationTrapezoidQueryNode root = i1;

        _TriangulationTrapezoidQueryNode i2 = allocateQueryNode();
        i1.rightChild = i2;
        i2.queryNodeType = T_SINK;
        i2.parent = i1;

        _TriangulationTrapezoidQueryNode i3 = allocateQueryNode();
        i1.leftChild = i3;
        i3.queryNodeType = T_Y;
        min(i3.splitPoint, s.startPoint, s.endPoint);
        i3.parent = i1;

        _TriangulationTrapezoidQueryNode i4 = allocateQueryNode();
        i3.leftChild = i4;
        i4.queryNodeType = T_SINK;
        i4.parent = i3;

        _TriangulationTrapezoidQueryNode i5 = allocateQueryNode();
        i3.rightChild = i5;
        i5.queryNodeType = T_X;
        i5.segmentIndex = segmentIndex;
        i5.parent = i3;

        _TriangulationTrapezoidQueryNode i6 = allocateQueryNode();
        i5.leftChild = i6;
        i6.queryNodeType = T_SINK;
        i6.parent = i5;

        _TriangulationTrapezoidQueryNode i7 = allocateQueryNode();
        i5.rightChild = i7;
        i7.queryNodeType = T_SINK;
        i7.parent = i5;

        int t1 = nextTrapezoidIndex++;
        int t2 = nextTrapezoidIndex++;
        int t3 = nextTrapezoidIndex++;
        int t4 = nextTrapezoidIndex++;
        trapezoids.set(t1, new _TriangulationTrapezoid());
        trapezoids.set(t2, new _TriangulationTrapezoid());
        trapezoids.set(t3, new _TriangulationTrapezoid());
        trapezoids.set(t4, new _TriangulationTrapezoid());

        trapezoids.get(t1).upperPoint.set(i1.splitPoint);
        trapezoids.get(t2).upperPoint.set(i1.splitPoint);
        trapezoids.get(t4).lowerPoint.set(i1.splitPoint);
        trapezoids.get(t1).lowerPoint.set(i3.splitPoint);
        trapezoids.get(t2).lowerPoint.set(i3.splitPoint);
        trapezoids.get(t3).upperPoint.set(i3.splitPoint);
        trapezoids.get(t4).upperPoint.set(TRIANGULATION_INFINITY, TRIANGULATION_INFINITY);
        trapezoids.get(t3).lowerPoint.set(-TRIANGULATION_INFINITY, -TRIANGULATION_INFINITY);

        trapezoids.get(t1).rightSegmentIndex = segmentIndex;
        trapezoids.get(t2).leftSegmentIndex = segmentIndex;
        trapezoids.get(t1).upperLeftTrapezoidIndex = t4;
        trapezoids.get(t2).upperLeftTrapezoidIndex = t4;
        trapezoids.get(t1).lowerLeftTrapezoidIndex = t3;
        trapezoids.get(t2).lowerLeftTrapezoidIndex = t3;
        trapezoids.get(t4).lowerLeftTrapezoidIndex = t1;
        trapezoids.get(t3).upperLeftTrapezoidIndex = t1;
        trapezoids.get(t4).lowerRightTrapezoidIndex = t2;
        trapezoids.get(t3).upperRightTrapezoidIndex = t2;

        trapezoids.get(t1).sinkNode = i6;
        trapezoids.get(t2).sinkNode = i7;
        trapezoids.get(t3).sinkNode = i4;
        trapezoids.get(t4).sinkNode = i2;

        trapezoids.get(t1).status = ST_VALID;
        trapezoids.get(t2).status = ST_VALID;
        trapezoids.get(t3).status = ST_VALID;
        trapezoids.get(t4).status = ST_VALID;

        i2.trapezoidIndex = t4;
        i4.trapezoidIndex = t3;
        i6.trapezoidIndex = t1;
        i7.trapezoidIndex = t2;

        s.hasBeenInserted = true;
        return root;
    }

    private static int findNewRoots(int segmentIndex) {
        _TriangulationSegment s = segments.get(segmentIndex);
        if (s.hasBeenInserted) return 0;
        int trapezoidIndex = s.startPointQueryNode.locateEndpoint(s.startPoint, s.endPoint);
        s.startPointQueryNode = trapezoids.get(trapezoidIndex).sinkNode;
        trapezoidIndex = s.endPointQueryNode.locateEndpoint(s.endPoint, s.startPoint);
        s.endPointQueryNode = trapezoids.get(trapezoidIndex).sinkNode;
        return 0;
    }

    public static int constructTrapezoids(int nseg) {
        prepareStorage(nseg);
        _TriangulationTrapezoidQueryNode root = initQueryStructure(_RandomSegmentOrder.chooseSegment());

        for (int i = 1; i <= nseg; i++) {
            segments.get(i).startPointQueryNode = root;
            segments.get(i).endPointQueryNode = root;
        }

        for (int h = 1; h <= _InsertionBatchSchedule.mathLogStarN(nseg); h++) {
            for (int i = _InsertionBatchSchedule.mathN(nseg, h - 1) + 1;
                 i <= _InsertionBatchSchedule.mathN(nseg, h); i++) {
                _IncrementalSegmentInserter.addSegment(_RandomSegmentOrder.chooseSegment());
            }
            for (int i = 1; i <= nseg; i++) findNewRoots(i);
        }

        for (int i = _InsertionBatchSchedule.mathN(nseg, _InsertionBatchSchedule.mathLogStarN(nseg)) + 1;
             i <= nseg; i++) {
            _IncrementalSegmentInserter.addSegment(_RandomSegmentOrder.chooseSegment());
        }
        return 0;
    }
}
