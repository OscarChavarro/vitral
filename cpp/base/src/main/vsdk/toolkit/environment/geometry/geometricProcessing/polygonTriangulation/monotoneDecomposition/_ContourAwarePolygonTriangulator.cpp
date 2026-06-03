#include <algorithm>
#include <cmath>
#include <limits>
#include <vector>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/monotoneDecomposition/_ContourAwarePolygonTriangulator.h"
#include "vsdk/toolkit/environment/geometry/element/Vertex2D.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/_Polygon2DContour.h"

namespace {

const double EPSILON = 1.0e-9;

struct IndexedVertex {
    double x;
    double y;
    int originalIndex;
};

struct ContourData {
    std::vector<IndexedVertex> vertices;
    std::vector<int> childContours;
    double signedArea = 0.0;
    int parentContour = -1;
    int depth = -1;
};

double signedArea(const std::vector<IndexedVertex> &vertices)
{
    double area = 0.0;
    for (size_t i = 0; i < vertices.size(); i++) {
        const IndexedVertex &current = vertices[i];
        const IndexedVertex &next = vertices[(i + 1) % vertices.size()];
        area += current.x * next.y - next.x * current.y;
    }
    return area * 0.5;
}

bool samePoint(const IndexedVertex &a, const IndexedVertex &b)
{
    return std::abs(a.x - b.x) <= EPSILON && std::abs(a.y - b.y) <= EPSILON;
}

double cross(const IndexedVertex &a, const IndexedVertex &b,
    const IndexedVertex &c)
{
    return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
}

std::vector<IndexedVertex> removeConsecutiveDuplicates(
    const std::vector<IndexedVertex> &polygon)
{
    std::vector<IndexedVertex> result;
    for (size_t i = 0; i < polygon.size(); i++) {
        if (result.empty() || !samePoint(result.back(), polygon[i])) {
            result.push_back(polygon[i]);
        }
    }
    if (result.size() > 1 && samePoint(result.front(), result.back())) {
        result.pop_back();
    }
    return result;
}

void normalizeOrientation(std::vector<IndexedVertex> &vertices,
    bool wantCounterClockwise)
{
    const double area = signedArea(vertices);
    if ((wantCounterClockwise && area < 0.0) ||
        (!wantCounterClockwise && area > 0.0)) {
        std::reverse(vertices.begin(), vertices.end());
    }
}

bool pointOnSegment(double x, double y, const IndexedVertex &a,
    const IndexedVertex &b)
{
    const double areaTwice = (b.x - a.x) * (y - a.y) -
        (b.y - a.y) * (x - a.x);
    if (std::abs(areaTwice) > EPSILON) {
        return false;
    }

    return x >= std::min(a.x, b.x) - EPSILON &&
        x <= std::max(a.x, b.x) + EPSILON &&
        y >= std::min(a.y, b.y) - EPSILON &&
        y <= std::max(a.y, b.y) + EPSILON;
}

bool containsPoint(const std::vector<IndexedVertex> &contour, double x, double y)
{
    bool inside = false;
    for (size_t i = 0, j = contour.size() - 1; i < contour.size(); j = i++) {
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
    const double minA = std::min(a0, a1);
    const double maxA = std::max(a0, a1);
    const double minB = std::min(b0, b1);
    const double maxB = std::max(b0, b1);
    return std::max(minA, minB) <= std::min(maxA, maxB) + EPSILON;
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

    if (std::abs(abC) <= EPSILON && std::abs(abD) <= EPSILON &&
        std::abs(cdA) <= EPSILON && std::abs(cdB) <= EPSILON) {
        return rangesOverlap(a.x, b.x, c.x, d.x) &&
            rangesOverlap(a.y, b.y, c.y, d.y);
    }

    return ((abC > EPSILON && abD < -EPSILON) ||
               (abC < -EPSILON && abD > EPSILON)) &&
        ((cdA > EPSILON && cdB < -EPSILON) ||
            (cdA < -EPSILON && cdB > EPSILON));
}

bool segmentCrossesAnyEdge(const IndexedVertex &a, const IndexedVertex &b,
    const std::vector<IndexedVertex> &boundary, int allowedVertexIndex)
{
    for (size_t i = 0; i < boundary.size(); i++) {
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
    const std::vector<IndexedVertex> &polygon, int polygonVertexIndex,
    const std::vector<IndexedVertex> &hole, int holeVertexIndex)
{
    if (segmentCrossesAnyEdge(a, b, polygon, polygonVertexIndex)) {
        return true;
    }
    return segmentCrossesAnyEdge(a, b, hole, holeVertexIndex);
}

int findRightmostVertexIndex(const std::vector<IndexedVertex> &contour)
{
    int bestIndex = 0;
    IndexedVertex bestVertex = contour[0];
    for (size_t i = 1; i < contour.size(); i++) {
        const IndexedVertex &candidate = contour[i];
        if (candidate.x > bestVertex.x + EPSILON ||
            (std::abs(candidate.x - bestVertex.x) <= EPSILON &&
                candidate.y < bestVertex.y)) {
            bestVertex = candidate;
            bestIndex = static_cast<int>(i);
        }
    }
    return bestIndex;
}

int computeDepth(std::vector<ContourData> &contours, int contourIndex)
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

std::vector<ContourData> flattenContours(const Polygon2D &input)
{
    std::vector<ContourData> contours;
    int flattenedIndex = 0;
    Polygon2D &mutableInput = const_cast<Polygon2D &>(input);

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
            contour.vertices.push_back(
                {vertex.x, vertex.y, flattenedIndex++});
        }
        contour.signedArea = signedArea(contour.vertices);
        if (std::abs(contour.signedArea) > EPSILON) {
            contours.push_back(contour);
        }
    }

    return contours;
}

void classifyContourHierarchy(std::vector<ContourData> &contours)
{
    for (size_t i = 0; i < contours.size(); i++) {
        ContourData &contour = contours[i];
        const IndexedVertex &probe = contour.vertices[0];
        double bestContainerArea = std::numeric_limits<double>::infinity();

        for (size_t j = 0; j < contours.size(); j++) {
            if (i == j) {
                continue;
            }

            ContourData &candidate = contours[j];
            const double candidateAbsArea = std::abs(candidate.signedArea);
            if (candidateAbsArea <= std::abs(contour.signedArea) + EPSILON) {
                continue;
            }

            if (containsPoint(candidate.vertices, probe.x, probe.y) &&
                candidateAbsArea < bestContainerArea) {
                bestContainerArea = candidateAbsArea;
                contour.parentContour = static_cast<int>(j);
            }
        }
    }

    for (size_t i = 0; i < contours.size(); i++) {
        computeDepth(contours, static_cast<int>(i));
    }

    for (size_t i = 0; i < contours.size(); i++) {
        const int parentIndex = contours[i].parentContour;
        if (parentIndex >= 0) {
            contours[parentIndex].childContours.push_back(static_cast<int>(i));
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

void simplifyCollinearVertices(std::vector<IndexedVertex> &polygon)
{
    bool removed;
    do {
        removed = false;
        if (polygon.size() <= 3) {
            return;
        }
        for (size_t i = 0; i < polygon.size(); i++) {
            const IndexedVertex &previous =
                polygon[(i + polygon.size() - 1) % polygon.size()];
            const IndexedVertex &current = polygon[i];
            const IndexedVertex &next = polygon[(i + 1) % polygon.size()];
            if (samePoint(previous, current) || samePoint(current, next) ||
                std::abs(cross(previous, current, next)) <= EPSILON) {
                polygon.erase(polygon.begin() + static_cast<long>(i));
                removed = true;
                break;
            }
        }
    }
    while (removed);
}

bool earClipSimplePolygon(const std::vector<IndexedVertex> &polygon,
    java::ArrayList<MonotoneDecompositionTriangulator::Triangle> &output)
{
    std::vector<IndexedVertex> work = removeConsecutiveDuplicates(polygon);
    simplifyCollinearVertices(work);
    if (work.size() < 3) {
        return true;
    }

    if (signedArea(work) < 0.0) {
        std::reverse(work.begin(), work.end());
    }

    int guard = static_cast<int>(work.size() * work.size());
    while (work.size() > 3 && guard-- > 0) {
        bool clippedEar = false;

        for (size_t i = 0; i < work.size(); i++) {
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
            for (size_t j = 0; j < work.size(); j++) {
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

            work.erase(work.begin() + static_cast<long>(i));
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
        std::abs(cross(work[0], work[1], work[2])) > EPSILON) {
        MonotoneDecompositionTriangulator::Triangle triangle;
        triangle.add(work[0].originalIndex);
        triangle.add(work[1].originalIndex);
        triangle.add(work[2].originalIndex);
        output.add(triangle);
        return true;
    }

    return work.empty() || work.size() < 3;
}

int findRightmostHoleIndex(
    const std::vector<std::vector<IndexedVertex> > &holes)
{
    int bestHoleIndex = 0;
    IndexedVertex bestVertex = holes[0][findRightmostVertexIndex(holes[0])];

    for (size_t holeIndex = 1; holeIndex < holes.size(); holeIndex++) {
        const IndexedVertex &candidate =
            holes[holeIndex][findRightmostVertexIndex(holes[holeIndex])];
        if (candidate.x > bestVertex.x + EPSILON ||
            (std::abs(candidate.x - bestVertex.x) <= EPSILON &&
                candidate.y < bestVertex.y)) {
            bestVertex = candidate;
            bestHoleIndex = static_cast<int>(holeIndex);
        }
    }
    return bestHoleIndex;
}

int findVisibleBridgeVertex(const std::vector<IndexedVertex> &polygon,
    const std::vector<IndexedVertex> &hole, int holeVertexIndex,
    const std::vector<std::vector<IndexedVertex> > &remainingHoles)
{
    const IndexedVertex &holeVertex = hole[holeVertexIndex];
    int bestPolygonVertexIndex = -1;
    double bestDistanceSquared = std::numeric_limits<double>::infinity();

    for (size_t polygonVertexIndex = 0;
         polygonVertexIndex < polygon.size(); polygonVertexIndex++) {
        const IndexedVertex &polygonVertex = polygon[polygonVertexIndex];
        if (segmentCrossesBoundary(holeVertex, polygonVertex, polygon,
                static_cast<int>(polygonVertexIndex), hole, holeVertexIndex)) {
            continue;
        }

        bool crossesRemainingHole = false;
        for (size_t holeIndex = 0; holeIndex < remainingHoles.size();
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
            bestPolygonVertexIndex = static_cast<int>(polygonVertexIndex);
        }
    }

    return bestPolygonVertexIndex;
}

std::vector<IndexedVertex> bridgeHoleIntoPolygon(
    const std::vector<IndexedVertex> &polygonInput,
    const std::vector<IndexedVertex> &holeInput,
    const std::vector<std::vector<IndexedVertex> > &remainingHoles,
    bool &ok)
{
    const std::vector<IndexedVertex> polygon =
        removeConsecutiveDuplicates(polygonInput);
    const std::vector<IndexedVertex> hole =
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
        return std::vector<IndexedVertex>();
    }

    std::vector<IndexedVertex> merged;
    for (int i = 0; i <= bridgePolygonVertexIndex; i++) {
        merged.push_back(polygon[i]);
    }

    merged.push_back(holeVertex);
    for (size_t offset = 1; offset < hole.size(); offset++) {
        const int index =
            (holeVertexIndex + static_cast<int>(offset)) % static_cast<int>(hole.size());
        merged.push_back(hole[index]);
    }
    merged.push_back(holeVertex);

    for (size_t i = static_cast<size_t>(bridgePolygonVertexIndex);
         i < polygon.size(); i++) {
        merged.push_back(polygon[i]);
    }

    ok = true;
    return removeConsecutiveDuplicates(merged);
}

bool triangulateFilledRegion(const std::vector<IndexedVertex> &outerInput,
    const std::vector<std::vector<IndexedVertex> > &holesInput,
    java::ArrayList<MonotoneDecompositionTriangulator::Triangle> &output)
{
    std::vector<IndexedVertex> outer = outerInput;
    normalizeOrientation(outer, true);

    std::vector<std::vector<IndexedVertex> > holes = holesInput;
    for (size_t i = 0; i < holes.size(); i++) {
        normalizeOrientation(holes[i], false);
    }

    std::vector<IndexedVertex> polygon = removeConsecutiveDuplicates(outer);
    if (polygon.size() < 3) {
        return true;
    }

    std::vector<std::vector<IndexedVertex> > remainingHoles = holes;
    while (!remainingHoles.empty()) {
        const int nextHoleIndex = findRightmostHoleIndex(remainingHoles);
        const std::vector<IndexedVertex> hole = remainingHoles[nextHoleIndex];
        remainingHoles.erase(remainingHoles.begin() + nextHoleIndex);

        bool ok = false;
        polygon = bridgeHoleIntoPolygon(polygon, hole, remainingHoles, ok);
        if (!ok) {
            return false;
        }
    }

    return earClipSimplePolygon(polygon, output);
}

} // namespace

int _ContourAwarePolygonTriangulator::triangulate(
    const Polygon2D &input,
    java::ArrayList<MonotoneDecompositionTriangulator::Triangle> &output)
{
    std::vector<ContourData> contours = flattenContours(input);
    if (contours.empty()) {
        output.clear();
        return 0;
    }

    classifyContourHierarchy(contours);

    java::ArrayList<MonotoneDecompositionTriangulator::Triangle> triangles;
    for (size_t contourIndex = 0; contourIndex < contours.size();
         contourIndex++) {
        const ContourData &contour = contours[contourIndex];
        if ((contour.depth % 2) == 0) {
            continue;
        }

        std::vector<std::vector<IndexedVertex> > holes;
        for (size_t childIndex = 0; childIndex < contour.childContours.size();
             childIndex++) {
            holes.push_back(
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
    return static_cast<int>(output.size());
}
