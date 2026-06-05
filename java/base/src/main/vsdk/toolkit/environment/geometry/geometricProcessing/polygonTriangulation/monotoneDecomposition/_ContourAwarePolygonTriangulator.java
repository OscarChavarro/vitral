package vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.monotoneDecomposition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vsdk.toolkit.environment.geometry.element.Vertex2D;
import vsdk.toolkit.environment.geometry.geometricProcessing.polygonTriangulation.MonotoneDecompositionTriangulator;
import vsdk.toolkit.environment.geometry.surface.polygon.Polygon2D;
import vsdk.toolkit.environment.geometry.surface.polygon._Polygon2DContour;

public final class _ContourAwarePolygonTriangulator {
    private static final double EPSILON = 1.0e-9;

    private _ContourAwarePolygonTriangulator() {}

    public static int triangulate(
        Polygon2D input,
        List<MonotoneDecompositionTriangulator.Triangle> output)
    {
        ArrayList<_ContourData> contours = flattenContours(input);
        if (contours.isEmpty()) {
            output.clear();
            return 0;
        }

        classifyContourHierarchy(contours);

        ArrayList<MonotoneDecompositionTriangulator.Triangle> triangles =
            new ArrayList<>();
        for (int contourIndex = 0; contourIndex < contours.size(); contourIndex++) {
            _ContourData contour = contours.get(contourIndex);
            if ((contour.depth % 2) == 0) {
                continue;
            }

            ArrayList<List<_IndexedVertex>> holes = new ArrayList<>();
            for (int childIndex : contour.childContours) {
                holes.add(copyVertices(contours.get(childIndex).vertices));
            }

            if (!triangulateFilledRegion(copyVertices(contour.vertices), holes, triangles)) {
                return -1;
            }
        }

        output.clear();
        output.addAll(triangles);
        return output.size();
    }

    private static ArrayList<_ContourData> flattenContours(Polygon2D input) {
        ArrayList<_ContourData> contours = new ArrayList<>();
        int flattenedIndex = 0;

        for (_Polygon2DContour loop : input.loops) {
            if (loop.vertices.isEmpty()) {
                continue;
            }

            _ContourData contour = new _ContourData();
            for (Vertex2D vertex : loop.vertices) {
                contour.vertices.add(new _IndexedVertex(
                    vertex.x, vertex.y, flattenedIndex++));
            }
            contour.signedArea = signedArea(contour.vertices);
            if (Math.abs(contour.signedArea) > EPSILON) {
                contours.add(contour);
            }
        }

        return contours;
    }

    private static void classifyContourHierarchy(ArrayList<_ContourData> contours) {
        for (int i = 0; i < contours.size(); i++) {
            _ContourData contour = contours.get(i);
            _IndexedVertex probe = contour.vertices.get(0);
            double bestContainerArea = Double.POSITIVE_INFINITY;

            for (int j = 0; j < contours.size(); j++) {
                if (i == j) {
                    continue;
                }

                _ContourData candidate = contours.get(j);
                double candidateAbsArea = Math.abs(candidate.signedArea);
                if (candidateAbsArea <= Math.abs(contour.signedArea) + EPSILON) {
                    continue;
                }

                if (containsPoint(candidate.vertices, probe.x, probe.y)) {
                    if (candidateAbsArea < bestContainerArea) {
                        bestContainerArea = candidateAbsArea;
                        contour.parentContour = j;
                    }
                }
            }
        }

        for (int i = 0; i < contours.size(); i++) {
            computeDepth(contours, i);
        }

        for (int i = 0; i < contours.size(); i++) {
            int parentIndex = contours.get(i).parentContour;
            if (parentIndex >= 0) {
                contours.get(parentIndex).childContours.add(i);
            }
        }
    }

    private static int computeDepth(ArrayList<_ContourData> contours, int contourIndex) {
        _ContourData contour = contours.get(contourIndex);
        if (contour.depth >= 0) {
            return contour.depth;
        }
        if (contour.parentContour < 0) {
            contour.depth = 1;
        }
        else {
            contour.depth = computeDepth(contours, contour.parentContour) + 1;
        }
        return contour.depth;
    }

    private static boolean triangulateFilledRegion(
        List<_IndexedVertex> outer,
        ArrayList<List<_IndexedVertex>> holes,
        ArrayList<MonotoneDecompositionTriangulator.Triangle> output)
    {
        normalizeOrientation(outer, true);
        for (List<_IndexedVertex> hole : holes) {
            normalizeOrientation(hole, false);
        }

        List<_IndexedVertex> polygon = removeConsecutiveDuplicates(outer);
        if (polygon.size() < 3) {
            return true;
        }

        ArrayList<List<_IndexedVertex>> remainingHoles = new ArrayList<>(holes);
        while (!remainingHoles.isEmpty()) {
            int nextHoleIndex = findRightmostHoleIndex(remainingHoles);
            List<_IndexedVertex> hole = remainingHoles.remove(nextHoleIndex);
            polygon = bridgeHoleIntoPolygon(polygon, hole, remainingHoles);
            if (polygon == null) {
                return false;
            }
        }

        return earClipSimplePolygon(polygon, output);
    }

    private static int findRightmostHoleIndex(ArrayList<List<_IndexedVertex>> holes) {
        int bestHoleIndex = 0;
        _IndexedVertex bestVertex = null;

        for (int holeIndex = 0; holeIndex < holes.size(); holeIndex++) {
            _IndexedVertex candidate = holes.get(holeIndex).get(findRightmostVertexIndex(holes.get(holeIndex)));
            if (bestVertex == null ||
                candidate.x > bestVertex.x + EPSILON ||
                (Math.abs(candidate.x - bestVertex.x) <= EPSILON && candidate.y < bestVertex.y)) {
                bestVertex = candidate;
                bestHoleIndex = holeIndex;
            }
        }

        return bestHoleIndex;
    }

    private static List<_IndexedVertex> bridgeHoleIntoPolygon(
        List<_IndexedVertex> polygon,
        List<_IndexedVertex> hole,
        ArrayList<List<_IndexedVertex>> remainingHoles)
    {
        hole = removeConsecutiveDuplicates(hole);
        if (hole.size() < 3) {
            return polygon;
        }

        int holeVertexIndex = findRightmostVertexIndex(hole);
        _IndexedVertex holeVertex = hole.get(holeVertexIndex);
        int bridgePolygonVertexIndex = findVisibleBridgeVertex(
            polygon, hole, holeVertexIndex, remainingHoles);

        if (bridgePolygonVertexIndex < 0) {
            return null;
        }

        ArrayList<_IndexedVertex> merged = new ArrayList<>();
        for (int i = 0; i <= bridgePolygonVertexIndex; i++) {
            merged.add(polygon.get(i));
        }

        merged.add(holeVertex);
        for (int offset = 1; offset < hole.size(); offset++) {
            int index = (holeVertexIndex + offset) % hole.size();
            merged.add(hole.get(index));
        }
        merged.add(holeVertex);

        for (int i = bridgePolygonVertexIndex; i < polygon.size(); i++) {
            merged.add(polygon.get(i));
        }

        return removeConsecutiveDuplicates(merged);
    }

    private static int findVisibleBridgeVertex(
        List<_IndexedVertex> polygon,
        List<_IndexedVertex> hole,
        int holeVertexIndex,
        ArrayList<List<_IndexedVertex>> remainingHoles)
    {
        _IndexedVertex holeVertex = hole.get(holeVertexIndex);
        int bestPolygonVertexIndex = -1;
        double bestDistanceSquared = Double.POSITIVE_INFINITY;

        for (int polygonVertexIndex = 0; polygonVertexIndex < polygon.size(); polygonVertexIndex++) {
            _IndexedVertex polygonVertex = polygon.get(polygonVertexIndex);
            if (segmentCrossesBoundary(holeVertex, polygonVertex, polygon, polygonVertexIndex, hole, holeVertexIndex)) {
                continue;
            }

            boolean crossesRemainingHole = false;
            for (List<_IndexedVertex> remainingHole : remainingHoles) {
                if (segmentCrossesAnyEdge(holeVertex, polygonVertex, remainingHole, -1)) {
                    crossesRemainingHole = true;
                    break;
                }
            }
            if (crossesRemainingHole) {
                continue;
            }

            double midX = (holeVertex.x + polygonVertex.x) * 0.5;
            double midY = (holeVertex.y + polygonVertex.y) * 0.5;
            if (!containsPoint(polygon, midX, midY)) {
                continue;
            }
            if (containsPoint(hole, midX, midY)) {
                continue;
            }

            double distanceSquared = squaredDistance(holeVertex, polygonVertex);
            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                bestPolygonVertexIndex = polygonVertexIndex;
            }
        }

        return bestPolygonVertexIndex;
    }

    private static boolean segmentCrossesBoundary(
        _IndexedVertex a,
        _IndexedVertex b,
        List<_IndexedVertex> polygon,
        int polygonVertexIndex,
        List<_IndexedVertex> hole,
        int holeVertexIndex)
    {
        if (segmentCrossesAnyEdge(a, b, polygon, polygonVertexIndex)) {
            return true;
        }

        return segmentCrossesAnyEdge(a, b, hole, holeVertexIndex);
    }

    private static boolean segmentCrossesAnyEdge(
        _IndexedVertex a,
        _IndexedVertex b,
        List<_IndexedVertex> boundary,
        int allowedVertexIndex)
    {
        for (int i = 0; i < boundary.size(); i++) {
            _IndexedVertex edgeStart = boundary.get(i);
            _IndexedVertex edgeEnd = boundary.get((i + 1) % boundary.size());

            if (allowedVertexIndex >= 0) {
                if (edgeStart.originalIndex == boundary.get(allowedVertexIndex).originalIndex ||
                    edgeEnd.originalIndex == boundary.get(allowedVertexIndex).originalIndex) {
                    continue;
                }
            }

            if (segmentsIntersectProperly(a, b, edgeStart, edgeEnd)) {
                return true;
            }
        }
        return false;
    }

    private static boolean earClipSimplePolygon(
        List<_IndexedVertex> polygon,
        ArrayList<MonotoneDecompositionTriangulator.Triangle> output)
    {
        ArrayList<_IndexedVertex> work = new ArrayList<>(removeConsecutiveDuplicates(polygon));
        simplifyCollinearVertices(work);
        if (work.size() < 3) {
            return true;
        }

        if (signedArea(work) < 0.0) {
            Collections.reverse(work);
        }

        int guard = work.size() * work.size();
        while (work.size() > 3 && guard-- > 0) {
            boolean clippedEar = false;

            for (int i = 0; i < work.size(); i++) {
                _IndexedVertex previous = work.get((i + work.size() - 1) % work.size());
                _IndexedVertex current = work.get(i);
                _IndexedVertex next = work.get((i + 1) % work.size());

                double areaTwice = cross(previous, current, next);
                if (areaTwice <= EPSILON) {
                    continue;
                }
                if (hasRepeatedVertex(previous, current, next)) {
                    continue;
                }

                boolean containsOtherVertex = false;
                for (int j = 0; j < work.size(); j++) {
                    if (j == i || j == (i + 1) % work.size() || j == (i + work.size() - 1) % work.size()) {
                        continue;
                    }

                    _IndexedVertex other = work.get(j);
                    if (pointInTriangle(previous, current, next, other)) {
                        containsOtherVertex = true;
                        break;
                    }
                }
                if (containsOtherVertex) {
                    continue;
                }

                output.add(new MonotoneDecompositionTriangulator.Triangle(
                    previous.originalIndex,
                    current.originalIndex,
                    next.originalIndex));
                work.remove(i);
                simplifyCollinearVertices(work);
                clippedEar = true;
                break;
            }

            if (!clippedEar) {
                return false;
            }
        }

        if (work.size() == 3 && !hasRepeatedVertex(work.get(0), work.get(1), work.get(2)) &&
            Math.abs(cross(work.get(0), work.get(1), work.get(2))) > EPSILON) {
            output.add(new MonotoneDecompositionTriangulator.Triangle(
                work.get(0).originalIndex,
                work.get(1).originalIndex,
                work.get(2).originalIndex));
            return true;
        }

        return work.isEmpty() || work.size() < 3;
    }

    private static boolean hasRepeatedVertex(
        _IndexedVertex a,
        _IndexedVertex b,
        _IndexedVertex c)
    {
        return a.originalIndex == b.originalIndex ||
            b.originalIndex == c.originalIndex ||
            a.originalIndex == c.originalIndex;
    }

    private static void simplifyCollinearVertices(ArrayList<_IndexedVertex> polygon) {
        boolean removed;
        do {
            removed = false;
            if (polygon.size() <= 3) {
                return;
            }
            for (int i = 0; i < polygon.size(); i++) {
                _IndexedVertex previous = polygon.get((i + polygon.size() - 1) % polygon.size());
                _IndexedVertex current = polygon.get(i);
                _IndexedVertex next = polygon.get((i + 1) % polygon.size());
                if (samePoint(previous, current) || samePoint(current, next)) {
                    polygon.remove(i);
                    removed = true;
                    break;
                }
            }
        }
        while (removed);
    }

    private static List<_IndexedVertex> removeConsecutiveDuplicates(List<_IndexedVertex> polygon) {
        ArrayList<_IndexedVertex> result = new ArrayList<>();
        for (_IndexedVertex vertex : polygon) {
            if (result.isEmpty() || !samePoint(result.get(result.size() - 1), vertex)) {
                result.add(vertex);
            }
        }
        if (result.size() > 1 && samePoint(result.get(0), result.get(result.size() - 1))) {
            result.remove(result.size() - 1);
        }
        return result;
    }

    private static int findRightmostVertexIndex(List<_IndexedVertex> contour) {
        int bestIndex = 0;
        _IndexedVertex bestVertex = contour.get(0);
        for (int i = 1; i < contour.size(); i++) {
            _IndexedVertex candidate = contour.get(i);
            if (candidate.x > bestVertex.x + EPSILON ||
                (Math.abs(candidate.x - bestVertex.x) <= EPSILON && candidate.y < bestVertex.y)) {
                bestVertex = candidate;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static void normalizeOrientation(List<_IndexedVertex> vertices, boolean wantCounterClockwise) {
        double area = signedArea(vertices);
        if ((wantCounterClockwise && area < 0.0) || (!wantCounterClockwise && area > 0.0)) {
            Collections.reverse(vertices);
        }
    }

    private static ArrayList<_IndexedVertex> copyVertices(List<_IndexedVertex> source) {
        return new ArrayList<>(source);
    }

    private static double signedArea(List<_IndexedVertex> vertices) {
        double area = 0.0;
        for (int i = 0; i < vertices.size(); i++) {
            _IndexedVertex current = vertices.get(i);
            _IndexedVertex next = vertices.get((i + 1) % vertices.size());
            area += current.x * next.y - next.x * current.y;
        }
        return area * 0.5;
    }

    private static boolean containsPoint(List<_IndexedVertex> contour, double x, double y) {
        boolean inside = false;
        for (int i = 0, j = contour.size() - 1; i < contour.size(); j = i++) {
            _IndexedVertex a = contour.get(i);
            _IndexedVertex b = contour.get(j);

            if (pointOnSegment(x, y, a, b)) {
                return true;
            }

            boolean intersects = ((a.y > y) != (b.y > y)) &&
                (x < (b.x - a.x) * (y - a.y) / (b.y - a.y) + a.x);
            if (intersects) {
                inside = !inside;
            }
        }
        return inside;
    }

    private static boolean pointOnSegment(double x, double y, _IndexedVertex a, _IndexedVertex b) {
        double areaTwice = (b.x - a.x) * (y - a.y) - (b.y - a.y) * (x - a.x);
        if (Math.abs(areaTwice) > EPSILON) {
            return false;
        }

        return x >= Math.min(a.x, b.x) - EPSILON &&
            x <= Math.max(a.x, b.x) + EPSILON &&
            y >= Math.min(a.y, b.y) - EPSILON &&
            y <= Math.max(a.y, b.y) + EPSILON;
    }

    private static boolean pointInTriangle(
        _IndexedVertex a,
        _IndexedVertex b,
        _IndexedVertex c,
        _IndexedVertex p)
    {
        if (samePoint(a, p) || samePoint(b, p) || samePoint(c, p)) {
            return false;
        }

        double ab = cross(a, b, p);
        double bc = cross(b, c, p);
        double ca = cross(c, a, p);

        boolean hasNegative = ab < -EPSILON || bc < -EPSILON || ca < -EPSILON;
        boolean hasPositive = ab > EPSILON || bc > EPSILON || ca > EPSILON;
        return !(hasNegative && hasPositive);
    }

    private static double cross(_IndexedVertex a, _IndexedVertex b, _IndexedVertex c) {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
    }

    private static double squaredDistance(_IndexedVertex a, _IndexedVertex b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        return dx * dx + dy * dy;
    }

    private static boolean samePoint(_IndexedVertex a, _IndexedVertex b) {
        return Math.abs(a.x - b.x) <= EPSILON && Math.abs(a.y - b.y) <= EPSILON;
    }

    private static boolean segmentsIntersectProperly(
        _IndexedVertex a,
        _IndexedVertex b,
        _IndexedVertex c,
        _IndexedVertex d)
    {
        if (samePoint(a, c) || samePoint(a, d) || samePoint(b, c) || samePoint(b, d)) {
            return false;
        }

        double abC = cross(a, b, c);
        double abD = cross(a, b, d);
        double cdA = cross(c, d, a);
        double cdB = cross(c, d, b);

        if (Math.abs(abC) <= EPSILON && Math.abs(abD) <= EPSILON &&
            Math.abs(cdA) <= EPSILON && Math.abs(cdB) <= EPSILON) {
            return rangesOverlap(a.x, b.x, c.x, d.x) && rangesOverlap(a.y, b.y, c.y, d.y);
        }

        return ((abC > EPSILON && abD < -EPSILON) || (abC < -EPSILON && abD > EPSILON)) &&
            ((cdA > EPSILON && cdB < -EPSILON) || (cdA < -EPSILON && cdB > EPSILON));
    }

    private static boolean rangesOverlap(double a0, double a1, double b0, double b1) {
        double minA = Math.min(a0, a1);
        double maxA = Math.max(a0, a1);
        double minB = Math.min(b0, b1);
        double maxB = Math.max(b0, b1);
        return Math.max(minA, minB) <= Math.min(maxA, maxB) + EPSILON;
    }
}
