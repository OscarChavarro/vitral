#include <math.h>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/element/Vertex2D.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/_Polygon2DContour.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_ContourAwarePolygonTriangulator.h"
#include "vsdk/toolkit/common/Cpp98Support.h"
const double EPSILON = 1.0e-9;

template <class T>
static const T& minValue(const T& a, const T& b)
{
    return (b < a) ? b : a;
}

template <class T>
static const T& maxValue(const T& a, const T& b)
{
    return (a < b) ? b : a;
}

template <class T>
static void reverseArray(T* values, size_t count)
{
    if (count < 2) {
        return;
    }
    size_t left = 0;
    size_t right = count - 1;
    while (left < right) {
        T temp = values[left];
        values[left] = values[right];
        values[right] = temp;
        ++left;
        --right;
    }
}

struct IndexedVertex {
    double x;
    double y;
    int originalIndex;
};

struct ContourData {
    ArrayList<IndexedVertex> vertices;
    ArrayList<int> childContours;
    double signedArea;
    int parentContour;
    int depth;

    ContourData() : vertices(), childContours(), signedArea(0.0),
        parentContour(-1), depth(-1) {}
};

double signedArea(const ArrayList<IndexedVertex> &vertices)
{
    double area = 0.0;
    for (size_t i = 0; i < (size_t)vertices.size(); i++) {
        const IndexedVertex &current = vertices[i];
        const IndexedVertex &next = vertices[(i + 1) % vertices.size()];
        area += current.x * next.y - next.x * current.y;
    }
    return area * 0.5;
}

bool samePoint(const IndexedVertex &a, const IndexedVertex &b)
{
    return Cpp98Support::abs(a.x - b.x) <= EPSILON && Cpp98Support::abs(a.y - b.y) <= EPSILON;
}

double cross(const IndexedVertex &a, const IndexedVertex &b,
    const IndexedVertex &c)
{
    return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
}

ArrayList<IndexedVertex> removeConsecutiveDuplicates(
    const ArrayList<IndexedVertex> &polygon)
{
    ArrayList<IndexedVertex> result;
    for (size_t i = 0; i < (size_t)polygon.size(); i++) {
        if (result.size() == 0 || !samePoint(result[result.size() - 1], polygon[i])) {
            result.add(polygon[i]);
        }
    }
    if (result.size() > 1 && samePoint(result[0], result[result.size() - 1])) {
        result.remove(result.size() - 1);
    }
    return result;
}

void normalizeOrientation(ArrayList<IndexedVertex> &vertices,
    bool wantCounterClockwise)
{
    const double area = signedArea(vertices);
    if ((wantCounterClockwise && area < 0.0) ||
        (!wantCounterClockwise && area > 0.0)) {
        reverseArray(vertices.data(), vertices.size());
    }
}

bool pointOnSegment(double x, double y, const IndexedVertex &a,
    const IndexedVertex &b)
{
    const double areaTwice = (b.x - a.x) * (y - a.y) -
        (b.y - a.y) * (x - a.x);
    if (Cpp98Support::abs(areaTwice) > EPSILON) {
        return false;
    }

    return x >= minValue(a.x, b.x) - EPSILON &&
        x <= maxValue(a.x, b.x) + EPSILON &&
        y >= minValue(a.y, b.y) - EPSILON &&
        y <= maxValue(a.y, b.y) + EPSILON;
}

bool containsPoint(const ArrayList<IndexedVertex> &contour, double x, double y)
{
    bool inside = false;
    for (size_t i = 0, j = (size_t)contour.size() - 1; i < (size_t)contour.size(); j = i++) {
        const IndexedVertex &a = contour[i];
        const IndexedVertex &b = contour[j];

        if (pointOnSegment(x, y, a, b)) {
            return true;
        }

        const bool intersects = ((a.y > y) != (b.y > y)) &&
            (x < (b.x - a.x) * (y - a.y) / (b.y - a.y) + a.x);
        if (intersects) {
            inside = !inside;
        }
    }
    return inside;
}

double squaredDistance(const IndexedVertex &a, const IndexedVertex &b)
{
    const double dx = a.x - b.x;
    const double dy = a.y - b.y;
    return dx * dx + dy * dy;
}

bool rangesOverlap(double a0, double a1, double b0, double b1)
{
    const double minA = minValue(a0, a1);
    const double maxA = maxValue(a0, a1);
    const double minB = minValue(b0, b1);
    const double maxB = maxValue(b0, b1);
    return maxValue(minA, minB) <= minValue(maxA, maxB) + EPSILON;
}

bool segmentsIntersectProperly(const IndexedVertex &a, const IndexedVertex &b,
    const IndexedVertex &c, const IndexedVertex &d)
{
    if (samePoint(a, c) || samePoint(a, d) || samePoint(b, c) ||
        samePoint(b, d)) {
        return false;
    }

    const double abC = cross(a, b, c);
    const double abD = cross(a, b, d);
    const double cdA = cross(c, d, a);
    const double cdB = cross(c, d, b);

    if (Cpp98Support::abs(abC) <= EPSILON && Cpp98Support::abs(abD) <= EPSILON &&
        Cpp98Support::abs(cdA) <= EPSILON && Cpp98Support::abs(cdB) <= EPSILON) {
        return rangesOverlap(a.x, b.x, c.x, d.x) &&
            rangesOverlap(a.y, b.y, c.y, d.y);
    }

    return ((abC > EPSILON && abD < -EPSILON) ||
               (abC < -EPSILON && abD > EPSILON)) &&
        ((cdA > EPSILON && cdB < -EPSILON) ||
            (cdA < -EPSILON && cdB > EPSILON));
}

bool segmentCrossesAnyEdge(const IndexedVertex &a, const IndexedVertex &b,
    const ArrayList<IndexedVertex> &boundary, int allowedVertexIndex)
{
    for (size_t i = 0; i < (size_t)boundary.size(); i++) {
        const IndexedVertex &edgeStart = boundary[i];
        const IndexedVertex &edgeEnd = boundary[(i + 1) % boundary.size()];

        if (allowedVertexIndex >= 0) {
            const IndexedVertex &allowed = boundary[allowedVertexIndex];
            if (edgeStart.originalIndex == allowed.originalIndex ||
                edgeEnd.originalIndex == allowed.originalIndex) {
                continue;
            }
        }

        if (segmentsIntersectProperly(a, b, edgeStart, edgeEnd)) {
            return true;
        }
    }
    return false;
}

bool segmentCrossesBoundary(const IndexedVertex &a, const IndexedVertex &b,
    const ArrayList<IndexedVertex> &polygon, int polygonVertexIndex,
    const ArrayList<IndexedVertex> &hole, int holeVertexIndex)
{
    if (segmentCrossesAnyEdge(a, b, polygon, polygonVertexIndex)) {
        return true;
    }
    return segmentCrossesAnyEdge(a, b, hole, holeVertexIndex);
}

int findRightmostVertexIndex(const ArrayList<IndexedVertex> &contour)
{
    int bestIndex = 0;
    IndexedVertex bestVertex = contour[0];
    for (size_t i = 1; i < (size_t)contour.size(); i++) {
        const IndexedVertex &candidate = contour[i];
        if (candidate.x > bestVertex.x + EPSILON ||
            (Cpp98Support::abs(candidate.x - bestVertex.x) <= EPSILON &&
                candidate.y < bestVertex.y)) {
            bestVertex = candidate;
            bestIndex = ((int)(i));
        }
    }
    return bestIndex;
}

int computeDepth(ArrayList<ContourData> &contours, int contourIndex)
{
    ContourData &contour = contours[contourIndex];
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

ArrayList<ContourData> flattenContours(const Polygon2D &input)
{
    ArrayList<ContourData> contours;
    int flattenedIndex = 0;
    Polygon2D &mutableInput = ((Polygon2D &)(input));

    for (long contourIndex = 0; contourIndex < mutableInput.loops.size();
         contourIndex++) {
        _Polygon2DContour *loop = mutableInput.loops.get(contourIndex);
        if (loop->vertices.size() <= 0) {
            continue;
        }

        ContourData contour;
        for (long vertexIndex = 0; vertexIndex < loop->vertices.size();
             vertexIndex++) {
            const Vertex2D &vertex = loop->vertices[vertexIndex];
            IndexedVertex indexedVertex;
            indexedVertex.x = vertex.x;
            indexedVertex.y = vertex.y;
            indexedVertex.originalIndex = flattenedIndex++;
            contour.vertices.add(indexedVertex);
        }
        contour.signedArea = signedArea(contour.vertices);
        if (Cpp98Support::abs(contour.signedArea) > EPSILON) {
            contours.add(contour);
        }
    }

    return contours;
}

void classifyContourHierarchy(ArrayList<ContourData> &contours)
{
    for (size_t i = 0; i < (size_t)contours.size(); i++) {
        ContourData &contour = contours[i];
        const IndexedVertex &probe = contour.vertices[0];
        double bestContainerArea = HUGE_VAL;

        for (size_t j = 0; j < (size_t)contours.size(); j++) {
            if (i == j) {
                continue;
            }

            ContourData &candidate = contours[j];
            const double candidateAbsArea = Cpp98Support::abs(candidate.signedArea);
            if (candidateAbsArea <= Cpp98Support::abs(contour.signedArea) + EPSILON) {
                continue;
            }

            if (containsPoint(candidate.vertices, probe.x, probe.y) &&
                candidateAbsArea < bestContainerArea) {
                bestContainerArea = candidateAbsArea;
                contour.parentContour = ((int)(j));
            }
        }
    }

    for (size_t i = 0; i < (size_t)contours.size(); i++) {
        computeDepth(contours, ((int)(i)));
    }

    for (size_t i = 0; i < (size_t)contours.size(); i++) {
        const int parentIndex = contours[i].parentContour;
        if (parentIndex >= 0) {
            contours[parentIndex].childContours.add(((int)(i)));
        }
    }
}

bool hasRepeatedVertex(const IndexedVertex &a, const IndexedVertex &b,
    const IndexedVertex &c)
{
    return a.originalIndex == b.originalIndex ||
        b.originalIndex == c.originalIndex ||
        a.originalIndex == c.originalIndex;
}

bool pointInTriangle(const IndexedVertex &a, const IndexedVertex &b,
    const IndexedVertex &c, const IndexedVertex &p)
{
    if (samePoint(a, p) || samePoint(b, p) || samePoint(c, p)) {
        return false;
    }

    const double ab = cross(a, b, p);
    const double bc = cross(b, c, p);
    const double ca = cross(c, a, p);
    const bool hasNegative = ab < -EPSILON || bc < -EPSILON || ca < -EPSILON;
    const bool hasPositive = ab > EPSILON || bc > EPSILON || ca > EPSILON;
    return !(hasNegative && hasPositive);
}

void simplifyCollinearVertices(ArrayList<IndexedVertex> &polygon)
{
    bool removed;
    do {
        removed = false;
        if (polygon.size() <= 3) {
            return;
        }
        for (size_t i = 0; i < (size_t)polygon.size(); i++) {
            const IndexedVertex &previous =
                polygon[(i + polygon.size() - 1) % polygon.size()];
            const IndexedVertex &current = polygon[i];
            const IndexedVertex &next = polygon[(i + 1) % polygon.size()];
            if (samePoint(previous, current) || samePoint(current, next) ||
                Cpp98Support::abs(cross(previous, current, next)) <= EPSILON) {
                polygon.remove(((long)(i)));
                removed = true;
                break;
            }
        }
    }
    while (removed);
}

bool earClipSimplePolygon(const ArrayList<IndexedVertex> &polygon,
    ArrayList<MonotoneDecompositionTriangulator::Triangle> &output)
{
    ArrayList<IndexedVertex> work = removeConsecutiveDuplicates(polygon);
    simplifyCollinearVertices(work);
    if (work.size() < 3) {
        return true;
    }

    if (signedArea(work) < 0.0) {
        reverseArray(work.data(), work.size());
    }

    int guard = ((int)(work.size() * work.size()));
    while (work.size() > 3 && guard-- > 0) {
        bool clippedEar = false;

        for (size_t i = 0; i < (size_t)work.size(); i++) {
            const IndexedVertex &previous =
                work[(i + work.size() - 1) % work.size()];
            const IndexedVertex &current = work[i];
            const IndexedVertex &next = work[(i + 1) % work.size()];

            if (cross(previous, current, next) <= EPSILON) {
                continue;
            }
            if (hasRepeatedVertex(previous, current, next)) {
                continue;
            }

            bool containsOtherVertex = false;
            for (size_t j = 0; j < (size_t)work.size(); j++) {
                if (j == i || j == (i + 1) % work.size() ||
                    j == (i + work.size() - 1) % work.size()) {
                    continue;
                }
                if (pointInTriangle(previous, current, next, work[j])) {
                    containsOtherVertex = true;
                    break;
                }
            }
            if (containsOtherVertex) {
                continue;
            }

            MonotoneDecompositionTriangulator::Triangle triangle;
            triangle.add(previous.originalIndex);
            triangle.add(current.originalIndex);
            triangle.add(next.originalIndex);
            output.add(triangle);

            work.remove(((long)(i)));
            simplifyCollinearVertices(work);
            clippedEar = true;
            break;
        }

        if (!clippedEar) {
            return false;
        }
    }

    if (work.size() == 3 &&
        !hasRepeatedVertex(work[0], work[1], work[2]) &&
        Cpp98Support::abs(cross(work[0], work[1], work[2])) > EPSILON) {
        MonotoneDecompositionTriangulator::Triangle triangle;
        triangle.add(work[0].originalIndex);
        triangle.add(work[1].originalIndex);
        triangle.add(work[2].originalIndex);
        output.add(triangle);
        return true;
    }

    return work.size() == 0 || work.size() < 3;
}

int findRightmostHoleIndex(
    const ArrayList< ArrayList<IndexedVertex> > &holes)
{
    int bestHoleIndex = 0;
    IndexedVertex bestVertex = holes[0][findRightmostVertexIndex(holes[0])];

    for (size_t holeIndex = 1; holeIndex < (size_t)holes.size(); holeIndex++) {
        const IndexedVertex &candidate =
            holes[holeIndex][findRightmostVertexIndex(holes[holeIndex])];
        if (candidate.x > bestVertex.x + EPSILON ||
            (Cpp98Support::abs(candidate.x - bestVertex.x) <= EPSILON &&
                candidate.y < bestVertex.y)) {
            bestVertex = candidate;
            bestHoleIndex = ((int)(holeIndex));
        }
    }
    return bestHoleIndex;
}

int findVisibleBridgeVertex(const ArrayList<IndexedVertex> &polygon,
    const ArrayList<IndexedVertex> &hole, int holeVertexIndex,
    const ArrayList< ArrayList<IndexedVertex> > &remainingHoles)
{
    const IndexedVertex &holeVertex = hole[holeVertexIndex];
    int bestPolygonVertexIndex = -1;
    double bestDistanceSquared = HUGE_VAL;

    for (size_t polygonVertexIndex = 0;
         polygonVertexIndex < (size_t)polygon.size(); polygonVertexIndex++) {
        const IndexedVertex &polygonVertex = polygon[polygonVertexIndex];
        if (segmentCrossesBoundary(holeVertex, polygonVertex, polygon,
                ((int)(polygonVertexIndex)), hole, holeVertexIndex)) {
            continue;
        }

        bool crossesRemainingHole = false;
        for (size_t holeIndex = 0; holeIndex < (size_t)remainingHoles.size();
             holeIndex++) {
            if (segmentCrossesAnyEdge(holeVertex, polygonVertex,
                    remainingHoles[holeIndex], -1)) {
                crossesRemainingHole = true;
                break;
            }
        }
        if (crossesRemainingHole) {
            continue;
        }

        const double midX = (holeVertex.x + polygonVertex.x) * 0.5;
        const double midY = (holeVertex.y + polygonVertex.y) * 0.5;
        if (!containsPoint(polygon, midX, midY)) {
            continue;
        }
        if (containsPoint(hole, midX, midY)) {
            continue;
        }

        const double distanceSquared =
            squaredDistance(holeVertex, polygonVertex);
        if (distanceSquared < bestDistanceSquared) {
            bestDistanceSquared = distanceSquared;
            bestPolygonVertexIndex = ((int)(polygonVertexIndex));
        }
    }

    return bestPolygonVertexIndex;
}

ArrayList<IndexedVertex> bridgeHoleIntoPolygon(
    const ArrayList<IndexedVertex> &polygonInput,
    const ArrayList<IndexedVertex> &holeInput,
    const ArrayList< ArrayList<IndexedVertex> > &remainingHoles,
    bool &ok)
{
    const ArrayList<IndexedVertex> polygon =
        removeConsecutiveDuplicates(polygonInput);
    const ArrayList<IndexedVertex> hole =
        removeConsecutiveDuplicates(holeInput);
    if (hole.size() < 3) {
        ok = true;
        return polygon;
    }

    const int holeVertexIndex = findRightmostVertexIndex(hole);
    const IndexedVertex &holeVertex = hole[holeVertexIndex];
    const int bridgePolygonVertexIndex = findVisibleBridgeVertex(
        polygon, hole, holeVertexIndex, remainingHoles);
    if (bridgePolygonVertexIndex < 0) {
        ok = false;
        return ArrayList<IndexedVertex>();
    }

    ArrayList<IndexedVertex> merged;
    for (int i = 0; i <= bridgePolygonVertexIndex; i++) {
        merged.add(polygon[i]);
    }

    merged.add(holeVertex);
    for (size_t offset = 1; offset < (size_t)hole.size(); offset++) {
        const int index =
            (holeVertexIndex + ((int)(offset))) % ((int)(hole.size()));
        merged.add(hole[index]);
    }
    merged.add(holeVertex);

    for (size_t i = ((size_t)(bridgePolygonVertexIndex));
         i < (size_t)polygon.size(); i++) {
        merged.add(polygon[i]);
    }

    ok = true;
    return removeConsecutiveDuplicates(merged);
}

bool triangulateFilledRegion(const ArrayList<IndexedVertex> &outerInput,
    const ArrayList< ArrayList<IndexedVertex> > &holesInput,
    ArrayList<MonotoneDecompositionTriangulator::Triangle> &output)
{
    ArrayList<IndexedVertex> outer = outerInput;
    normalizeOrientation(outer, true);

    ArrayList< ArrayList<IndexedVertex> > holes = holesInput;
    for (size_t i = 0; i < (size_t)holes.size(); i++) {
        normalizeOrientation(holes[i], false);
    }

    ArrayList<IndexedVertex> polygon = removeConsecutiveDuplicates(outer);
    if (polygon.size() < 3) {
        return true;
    }

    ArrayList< ArrayList<IndexedVertex> > remainingHoles = holes;
    while (remainingHoles.size() > 0) {
        const int nextHoleIndex = findRightmostHoleIndex(remainingHoles);
        const ArrayList<IndexedVertex> hole = remainingHoles[nextHoleIndex];
        remainingHoles.remove(nextHoleIndex);

        bool ok = false;
        polygon = bridgeHoleIntoPolygon(polygon, hole, remainingHoles, ok);
        if (!ok) {
            return false;
        }
    }

    return earClipSimplePolygon(polygon, output);
}

int _ContourAwarePolygonTriangulator::triangulate(
    const Polygon2D &input,
    ArrayList<MonotoneDecompositionTriangulator::Triangle> &output)
{
    ArrayList<ContourData> contours = flattenContours(input);
    if (contours.size() == 0) {
        output.clear();
        return 0;
    }

    classifyContourHierarchy(contours);

    ArrayList<MonotoneDecompositionTriangulator::Triangle> triangles;
    for (size_t contourIndex = 0; contourIndex < (size_t)contours.size();
         contourIndex++) {
        const ContourData &contour = contours[contourIndex];
        if ((contour.depth % 2) == 0) {
            continue;
        }

        ArrayList< ArrayList<IndexedVertex> > holes;
        for (size_t childIndex = 0; childIndex < (size_t)contour.childContours.size();
             childIndex++) {
            holes.add(
                contours[contour.childContours[childIndex]].vertices);
        }

        if (!triangulateFilledRegion(contour.vertices, holes, triangles)) {
            return -1;
        }
    }

    output.clear();
    for (long i = 0; i < triangles.size(); i++) {
        output.add(triangles.get(i));
    }
    return ((int)(output.size()));
}
