#include <math.h>
#include <stddef.h>
#include <stdint.h>

#include "java/util/ArrayList.txx"
#include "java/util/HashMap.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/_Polygon2DContour.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonClipper/PolygonTopologicalMerger.h"
#include "vsdk/toolkit/common/Cpp98Support.h"
static Vertex2D copyVertex(const Vertex2D& v)
{
    return Vertex2D(v.x, v.y, v.color.r(), v.color.g(), v.color.b());
}

static bool samePoint(const Vertex2D& a, const Vertex2D& b, double epsilon)
{
    return fabs(a.x - b.x) <= epsilon && fabs(a.y - b.y) <= epsilon;
}

static bool areCollinear(const Vertex2D& a, const Vertex2D& b, const Vertex2D& c, double epsilon)
{
    double abx = b.x - a.x;
    double aby = b.y - a.y;
    double bcx = c.x - b.x;
    double bcy = c.y - b.y;
    double cross = abx * bcy - aby * bcx;
    return fabs(cross) <= epsilon;
}

static ArrayList<Vertex2D> normalizeContour(_Polygon2DContour* contour, double epsilon)
{
    ArrayList<Vertex2D> withoutDuplicates;
    ArrayList<Vertex2D> simplified;
    for (long int i = 0; i < contour->vertices.size(); ++i) {
        Vertex2D v = contour->vertices[i];
        if (withoutDuplicates.size() == 0 ||
            !samePoint(withoutDuplicates[withoutDuplicates.size() - 1], v, epsilon)) {
            withoutDuplicates.add(copyVertex(v));
        }
    }
    if (withoutDuplicates.size() > 1 &&
        samePoint(withoutDuplicates[0], withoutDuplicates[withoutDuplicates.size() - 1], epsilon)) {
        withoutDuplicates.remove(withoutDuplicates.size() - 1);
    }
    for (size_t i = 0; i < (size_t)withoutDuplicates.size(); ++i) {
        const Vertex2D& prev = withoutDuplicates[(i + withoutDuplicates.size() - 1) % withoutDuplicates.size()];
        const Vertex2D& cur = withoutDuplicates[i];
        const Vertex2D& next = withoutDuplicates[(i + 1) % withoutDuplicates.size()];
        if (withoutDuplicates.size() >= 3 && areCollinear(prev, cur, next, epsilon)) {
            continue;
        }
        simplified.add(copyVertex(cur));
    }
    return simplified;
}

static bool areEquivalentContours(const ArrayList<Vertex2D>& a, const ArrayList<Vertex2D>& b, double epsilon)
{
    if (a.size() != b.size()) return false;
    int n = ((int)(a.size()));
    if (n == 0) return true;
    for (int start = 0; start < n; ++start) {
        if (!samePoint(a[0], b[start], epsilon)) continue;
        bool forward = true;
        for (int i = 0; i < n; ++i) {
            if (!samePoint(a[i], b[(start + i) % n], epsilon)) {
                forward = false;
                break;
            }
        }
        if (forward) return true;
        bool backward = true;
        for (int i = 0; i < n; ++i) {
            int index = start - i;
            while (index < 0) index += n;
            if (!samePoint(a[i], b[index], epsilon)) {
                backward = false;
                break;
            }
        }
        if (backward) return true;
    }
    return false;
}

struct PointKey {
    long long quantizedX;
    long long quantizedY;
    explicit PointKey(const Vertex2D& p, double epsilon)
        : quantizedX(((long long)(Cpp98Support::llround(p.x / epsilon)))),
          quantizedY(((long long)(Cpp98Support::llround(p.y / epsilon)))) {}
    bool operator==(const PointKey& o) const { return quantizedX == o.quantizedX && quantizedY == o.quantizedY; }
};

struct PointKeyHasher {
    size_t operator()(const PointKey& k) const
    {
        size_t h = ((size_t)(k.quantizedX));
        size_t v = ((size_t)(k.quantizedY));
        h ^= v + 0x9e3779b97f4a7c15ULL + (h << 6) + (h >> 2);
        return h;
    }
};

static int compareKeys(const PointKey& a, const PointKey& b)
{
    if (a.quantizedX < b.quantizedX) return -1;
    if (a.quantizedX > b.quantizedX) return 1;
    if (a.quantizedY < b.quantizedY) return -1;
    if (a.quantizedY > b.quantizedY) return 1;
    return 0;
}

static bool containsIndex(const ArrayList<int>& values, int value)
{
    for (long int i = 0; i < values.size(); ++i) {
        if (values[i] == value) {
            return true;
        }
    }
    return false;
}

static void addOutgoingIndex(
    HashMap<PointKey, ArrayList<int> >& outgoing,
    const PointKey& key, int index)
{
    ArrayList<int>* values = outgoing.get(key);
    if (values == 0) {
        ArrayList<int> newValues;
        newValues.add(index);
        outgoing.put(key, newValues);
        return;
    }
    values->add(index);
}

struct EdgeKey {
    Vertex2D start;
    Vertex2D end;
    PointKey startKey;
    PointKey endKey;
    EdgeKey(const Vertex2D& p0, const Vertex2D& p1, double epsilon)
        : start(), end(), startKey(p0, epsilon), endKey(p1, epsilon)
    {
        PointKey k0(p0, epsilon);
        PointKey k1(p1, epsilon);
        if (compareKeys(k0, k1) <= 0) {
            start = copyVertex(p0);
            end = copyVertex(p1);
            startKey = k0;
            endKey = k1;
        } else {
            start = copyVertex(p1);
            end = copyVertex(p0);
            startKey = k1;
            endKey = k0;
        }
    }
    bool isForward(const Vertex2D& candidate, double epsilon) const { return samePoint(start, candidate, epsilon); }
    bool operator==(const EdgeKey& o) const { return startKey == o.startKey && endKey == o.endKey; }
};

struct EdgeKeyHasher {
    size_t operator()(const EdgeKey& k) const
    {
        size_t h1 = PointKeyHasher()(k.startKey);
        size_t h2 = PointKeyHasher()(k.endKey);
        return h1 ^ (h2 << 1);
    }
};

struct Segment2D {
    Vertex2D start;
    Vertex2D end;
    Segment2D() : start(), end() {}
    Segment2D(const Vertex2D& startIn, const Vertex2D& endIn) : start(startIn), end(endIn) {}
};

struct SplitPoint {
    double segmentParameter;
    Vertex2D point;
    SplitPoint() : segmentParameter(0.0), point() {}
    SplitPoint(double segmentParameterIn, const Vertex2D& pointIn) : segmentParameter(segmentParameterIn), point(pointIn) {}
    bool operator<(const SplitPoint& o) const { return segmentParameter < o.segmentParameter; }
};

static void sortSplitPoints(ArrayList<SplitPoint>& values)
{
    for (size_t i = 1; i < (size_t)values.size(); ++i) {
        SplitPoint key = values[i];
        size_t j = i;
        while (j > 0 && key < values[j - 1]) {
            values[j] = values[j - 1];
            --j;
        }
        values[j] = key;
    }
}

struct DirectedEdge {
    Vertex2D start;
    Vertex2D end;
    DirectedEdge() : start(), end() {}
    DirectedEdge(const Vertex2D& s, const Vertex2D& e) : start(s), end(e) {}
};

static double parameterOnSegment(const Segment2D& seg, const Vertex2D& p, double epsilon)
{
    double dx = seg.end.x - seg.start.x;
    double dy = seg.end.y - seg.start.y;
    double len2 = dx * dx + dy * dy;
    if (len2 <= epsilon * epsilon) return -1.0;
    double t = ((p.x - seg.start.x) * dx + (p.y - seg.start.y) * dy) / len2;
    if (t < -epsilon || t > 1.0 + epsilon) return -1.0;
    if (t < 0.0) t = 0.0;
    else if (t > 1.0) t = 1.0;
    double projx = seg.start.x + t * dx;
    double projy = seg.start.y + t * dy;
    double dist2 = (p.x - projx) * (p.x - projx) + (p.y - projy) * (p.y - projy);
    if (dist2 > epsilon * epsilon) return -1.0;
    return t;
}

static void maybeAddPointOnSegment(const Segment2D& seg, const Vertex2D& p, double epsilon, ArrayList<SplitPoint>& out)
{
    double t = parameterOnSegment(seg, p, epsilon);
    if (t < -0.5) return;
    out.add(SplitPoint(t, copyVertex(p)));
}

static ArrayList<SplitPoint> dedupSplitPoints(const ArrayList<SplitPoint>& in, double epsilon)
{
    ArrayList<SplitPoint> out;
    for (size_t i = 0; i < (size_t)in.size(); ++i) {
        const SplitPoint& cur = in[i];
        if (out.size() == 0
            || fabs(out[out.size() - 1].segmentParameter - cur.segmentParameter) > epsilon
            || !samePoint(out[out.size() - 1].point, cur.point, epsilon)) {
            out.add(cur);
        }
    }
    return out;
}

static void splitAndAccumulateSegments(
    const ArrayList<Segment2D>& segments, double epsilon,
    HashMap<EdgeKey, int>& signedUsage)
{
    for (size_t i = 0; i < (size_t)segments.size(); ++i) {
        const Segment2D& seg = segments[i];
        ArrayList<SplitPoint> splitPoints;
        splitPoints.add(SplitPoint(0.0, seg.start));
        splitPoints.add(SplitPoint(1.0, seg.end));
        for (size_t j = 0; j < (size_t)segments.size(); ++j) {
            maybeAddPointOnSegment(seg, segments[j].start, epsilon, splitPoints);
            maybeAddPointOnSegment(seg, segments[j].end, epsilon, splitPoints);
        }
        sortSplitPoints(splitPoints);
        ArrayList<SplitPoint> dedup = dedupSplitPoints(splitPoints, epsilon);
        for (size_t j = 0; j + 1 < (size_t)dedup.size(); ++j) {
            const Vertex2D& p0 = dedup[j].point;
            const Vertex2D& p1 = dedup[j + 1].point;
            if (samePoint(p0, p1, epsilon)) continue;
            EdgeKey key(p0, p1, epsilon);
            int delta = key.isForward(p0, epsilon) ? 1 : -1;
            int* current = signedUsage.get(key);
            if (current == 0) {
                signedUsage.put(key, delta);
            }
            else {
                *current += delta;
            }
        }
    }
}

static int chooseNextEdge(
    const ArrayList<DirectedEdge>& edges, const ArrayList<int>& candidates,
    const ArrayList<int>& used, double prevDx, double prevDy)
{
    int selected = -1;
    double bestAngle = 1e308;
    for (size_t i = 0; i < (size_t)candidates.size(); ++i) {
        int idx = candidates[i];
        if (containsIndex(used, idx)) continue;
        const DirectedEdge& e = edges[idx];
        double dx = e.end.x - e.start.x;
        double dy = e.end.y - e.start.y;
        double cross = prevDx * dy - prevDy * dx;
        double dot = prevDx * dx + prevDy * dy;
        double angle = atan2(cross, dot);
        if (angle <= 0.0) angle += 2.0 * M_PI;
        if (angle < bestAngle) {
            bestAngle = angle;
            selected = idx;
        }
    }
    return selected;
}

static ArrayList<Vertex2D> traceLoop(
    const ArrayList<DirectedEdge>& edges, int startEdgeIndex,
    const HashMap<PointKey, ArrayList<int> >& outgoing,
    ArrayList<int>& used, double epsilon)
{
    ArrayList<Vertex2D> loop;
    const DirectedEdge& startEdge = edges[startEdgeIndex];
    Vertex2D start = startEdge.start;
    Vertex2D current = startEdge.end;
    double prevDx = startEdge.end.x - startEdge.start.x;
    double prevDy = startEdge.end.y - startEdge.start.y;
    int guard = ((int)(edges.size())) * 2 + 4;

    used.add(startEdgeIndex);
    loop.add(copyVertex(start));
    loop.add(copyVertex(current));

    while (!samePoint(current, start, epsilon) && guard > 0) {
        PointKey key(current, epsilon);
        int nextEdge = -1;
        const ArrayList<int>* candidates = outgoing.get(key);
        if (candidates != 0) {
            nextEdge = chooseNextEdge(edges, *candidates, used, prevDx, prevDy);
        }
        if (nextEdge < 0) break;
        used.add(nextEdge);
        const DirectedEdge& e = edges[nextEdge];
        prevDx = e.end.x - e.start.x;
        prevDy = e.end.y - e.start.y;
        current = e.end;
        if (!samePoint(current, start, epsilon)) {
            loop.add(copyVertex(current));
        }
        guard--;
    }
    if (loop.size() > 1 && samePoint(loop[0], loop[loop.size() - 1], epsilon)) {
        loop.remove(loop.size() - 1);
    }
    return loop;
}

static ArrayList< ArrayList<Vertex2D> > extractLoopsFromBoundaryEdges(
    const ArrayList<DirectedEdge>& edges, double epsilon)
{
    HashMap<PointKey, ArrayList<int> > outgoing;
    ArrayList<int> used;
    ArrayList< ArrayList<Vertex2D> > loops;
    for (size_t i = 0; i < (size_t)edges.size(); ++i) {
        PointKey key(edges[i].start, epsilon);
        addOutgoingIndex(outgoing, key, ((int)(i)));
    }
    for (size_t i = 0; i < (size_t)edges.size(); ++i) {
        if (containsIndex(used, ((int)(i)))) continue;
        ArrayList<Vertex2D> loop = traceLoop(edges, ((int)(i)), outgoing, used, epsilon);
        if (loop.size() >= 3) loops.add(loop);
    }
    return loops;
}

static ArrayList< ArrayList<Vertex2D> > weldInternalEdges(
    const ArrayList< ArrayList<Vertex2D> >& contours, double epsilon)
{
    ArrayList<Segment2D> segments;
    for (size_t i = 0; i < (size_t)contours.size(); ++i) {
        const ArrayList<Vertex2D>& contour = contours[i];
        for (size_t j = 0; j < (size_t)contour.size(); ++j) {
            const Vertex2D& a = contour[j];
            const Vertex2D& b = contour[(j + 1) % contour.size()];
            if (samePoint(a, b, epsilon)) continue;
            segments.add(Segment2D(copyVertex(a), copyVertex(b)));
        }
    }
    if (segments.size() == 0) return contours;

    HashMap<EdgeKey, int> signedUsage;
    splitAndAccumulateSegments(segments, epsilon, signedUsage);

    ArrayList<DirectedEdge> boundaryEdges;
    for (size_t i = 0; i < (size_t)segments.size(); ++i) {
        const Segment2D& seg = segments[i];
        EdgeKey key(seg.start, seg.end, epsilon);
        const int* balance = signedUsage.get(key);
        if (balance == 0 || *balance == 0) continue;
        if (*balance > 0) boundaryEdges.add(DirectedEdge(key.start, key.end));
        else boundaryEdges.add(DirectedEdge(key.end, key.start));
    }
    if (boundaryEdges.size() == 0) return ArrayList< ArrayList<Vertex2D> >();
    return extractLoopsFromBoundaryEdges(boundaryEdges, epsilon);
}

void PolygonTopologicalMerger::mergeInPlace(Polygon2D* polygon) { mergeInPlace(polygon, 1E-9); }

void PolygonTopologicalMerger::mergeInPlace(Polygon2D* polygon, double epsilon)
{
    if (polygon == 0) return;
    ArrayList< ArrayList<Vertex2D> > canonicalContours;

    for (long int i = 0; i < polygon->loops.size(); ++i) {
        _Polygon2DContour* contour = polygon->loops.get(i);
        ArrayList<Vertex2D> normalized = normalizeContour(contour, epsilon);
        if (normalized.size() < 3) continue;

        bool duplicate = false;
        for (size_t j = 0; j < (size_t)canonicalContours.size(); ++j) {
            if (areEquivalentContours(canonicalContours[j], normalized, epsilon)) {
                duplicate = true;
                break;
            }
        }
        if (!duplicate) canonicalContours.add(normalized);
    }

    canonicalContours = weldInternalEdges(canonicalContours, epsilon);

    for (long int i = 0; i < polygon->loops.size(); ++i) {
        delete polygon->loops[i];
    }
    polygon->loops.clear();

    for (size_t i = 0; i < (size_t)canonicalContours.size(); ++i) {
        polygon->nextLoop();
        const ArrayList<Vertex2D>& contour = canonicalContours[i];
        for (size_t j = 0; j < (size_t)contour.size(); ++j) {
            const Vertex2D& v = contour[j];
            polygon->addVertex(v.x, v.y, v.color.r(), v.color.g(), v.color.b());
        }
    }

    if (polygon->loops.size() == 0) polygon->nextLoop();
}
