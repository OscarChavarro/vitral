#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonClipper/PolygonTopologicalMerger.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/_Polygon2DContour.h"
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <unordered_map>
#include <unordered_set>
#include <vector>
#include "java/util/ArrayList.txx"

static Vertex2D copyVertex(const Vertex2D& v)
{
    return Vertex2D(v.x, v.y, v.color.r(), v.color.g(), v.color.b());
}

static bool samePoint(const Vertex2D& a, const Vertex2D& b, double epsilon)
{
    return std::fabs(a.x - b.x) <= epsilon && std::fabs(a.y - b.y) <= epsilon;
}

static bool areCollinear(const Vertex2D& a, const Vertex2D& b, const Vertex2D& c, double epsilon)
{
    double abx = b.x - a.x;
    double aby = b.y - a.y;
    double bcx = c.x - b.x;
    double bcy = c.y - b.y;
    double cross = abx * bcy - aby * bcx;
    return std::fabs(cross) <= epsilon;
}

static std::vector<Vertex2D> normalizeContour(_Polygon2DContour* contour, double epsilon)
{
    std::vector<Vertex2D> withoutDuplicates;
    std::vector<Vertex2D> simplified;
    for (long int i = 0; i < contour->vertices.size(); ++i) {
        Vertex2D v = contour->vertices[i];
        if (withoutDuplicates.empty() || !samePoint(withoutDuplicates.back(), v, epsilon)) {
            withoutDuplicates.push_back(copyVertex(v));
        }
    }
    if (withoutDuplicates.size() > 1 && samePoint(withoutDuplicates.front(), withoutDuplicates.back(), epsilon)) {
        withoutDuplicates.pop_back();
    }
    for (size_t i = 0; i < withoutDuplicates.size(); ++i) {
        const Vertex2D& prev = withoutDuplicates[(i + withoutDuplicates.size() - 1) % withoutDuplicates.size()];
        const Vertex2D& cur = withoutDuplicates[i];
        const Vertex2D& next = withoutDuplicates[(i + 1) % withoutDuplicates.size()];
        if (withoutDuplicates.size() >= 3 && areCollinear(prev, cur, next, epsilon)) {
            continue;
        }
        simplified.push_back(copyVertex(cur));
    }
    return simplified;
}

static bool areEquivalentContours(const std::vector<Vertex2D>& a, const std::vector<Vertex2D>& b, double epsilon)
{
    if (a.size() != b.size()) return false;
    int n = static_cast<int>(a.size());
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
    long long qx;
    long long qy;
    explicit PointKey(const Vertex2D& p, double epsilon)
        : qx(static_cast<long long>(std::llround(p.x / epsilon))),
          qy(static_cast<long long>(std::llround(p.y / epsilon))) {}
    bool operator==(const PointKey& o) const { return qx == o.qx && qy == o.qy; }
};

struct PointKeyHasher {
    std::size_t operator()(const PointKey& k) const
    {
        std::size_t h1 = std::hash<long long>()(k.qx);
        std::size_t h2 = std::hash<long long>()(k.qy);
        return h1 ^ (h2 << 1);
    }
};

static int compareKeys(const PointKey& a, const PointKey& b)
{
    if (a.qx < b.qx) return -1;
    if (a.qx > b.qx) return 1;
    if (a.qy < b.qy) return -1;
    if (a.qy > b.qy) return 1;
    return 0;
}

struct EdgeKey {
    Vertex2D a;
    Vertex2D b;
    PointKey ka;
    PointKey kb;
    EdgeKey(const Vertex2D& p0, const Vertex2D& p1, double epsilon)
        : a(), b(), ka(p0, epsilon), kb(p1, epsilon)
    {
        PointKey k0(p0, epsilon);
        PointKey k1(p1, epsilon);
        if (compareKeys(k0, k1) <= 0) {
            a = copyVertex(p0);
            b = copyVertex(p1);
            ka = k0;
            kb = k1;
        } else {
            a = copyVertex(p1);
            b = copyVertex(p0);
            ka = k1;
            kb = k0;
        }
    }
    bool isForward(const Vertex2D& start, double epsilon) const { return samePoint(a, start, epsilon); }
    bool operator==(const EdgeKey& o) const { return ka == o.ka && kb == o.kb; }
};

struct EdgeKeyHasher {
    std::size_t operator()(const EdgeKey& k) const
    {
        std::size_t h1 = PointKeyHasher()(k.ka);
        std::size_t h2 = PointKeyHasher()(k.kb);
        return h1 ^ (h2 << 1);
    }
};

struct Segment2D {
    Vertex2D a;
    Vertex2D b;
    Segment2D(const Vertex2D& aIn, const Vertex2D& bIn) : a(aIn), b(bIn) {}
};

struct SplitPoint {
    double t;
    Vertex2D p;
    SplitPoint(double tIn, const Vertex2D& pIn) : t(tIn), p(pIn) {}
    bool operator<(const SplitPoint& o) const { return t < o.t; }
};

struct DirectedEdge {
    Vertex2D start;
    Vertex2D end;
    DirectedEdge(const Vertex2D& s, const Vertex2D& e) : start(s), end(e) {}
};

static double parameterOnSegment(const Segment2D& seg, const Vertex2D& p, double epsilon)
{
    double dx = seg.b.x - seg.a.x;
    double dy = seg.b.y - seg.a.y;
    double len2 = dx * dx + dy * dy;
    if (len2 <= epsilon * epsilon) return -1.0;
    double t = ((p.x - seg.a.x) * dx + (p.y - seg.a.y) * dy) / len2;
    if (t < -epsilon || t > 1.0 + epsilon) return -1.0;
    if (t < 0.0) t = 0.0;
    else if (t > 1.0) t = 1.0;
    double projx = seg.a.x + t * dx;
    double projy = seg.a.y + t * dy;
    double dist2 = (p.x - projx) * (p.x - projx) + (p.y - projy) * (p.y - projy);
    if (dist2 > epsilon * epsilon) return -1.0;
    return t;
}

static void maybeAddPointOnSegment(const Segment2D& seg, const Vertex2D& p, double epsilon, std::vector<SplitPoint>& out)
{
    double t = parameterOnSegment(seg, p, epsilon);
    if (t < -0.5) return;
    out.push_back(SplitPoint(t, copyVertex(p)));
}

static std::vector<SplitPoint> dedupSplitPoints(const std::vector<SplitPoint>& in, double epsilon)
{
    std::vector<SplitPoint> out;
    for (size_t i = 0; i < in.size(); ++i) {
        const SplitPoint& cur = in[i];
        if (out.empty()
            || std::fabs(out.back().t - cur.t) > epsilon
            || !samePoint(out.back().p, cur.p, epsilon)) {
            out.push_back(cur);
        }
    }
    return out;
}

static void splitAndAccumulateSegments(
    const std::vector<Segment2D>& segments, double epsilon,
    std::unordered_map<EdgeKey, int, EdgeKeyHasher>& signedUsage)
{
    for (size_t i = 0; i < segments.size(); ++i) {
        const Segment2D& seg = segments[i];
        std::vector<SplitPoint> splitPoints;
        splitPoints.push_back(SplitPoint(0.0, seg.a));
        splitPoints.push_back(SplitPoint(1.0, seg.b));
        for (size_t j = 0; j < segments.size(); ++j) {
            maybeAddPointOnSegment(seg, segments[j].a, epsilon, splitPoints);
            maybeAddPointOnSegment(seg, segments[j].b, epsilon, splitPoints);
        }
        std::sort(splitPoints.begin(), splitPoints.end());
        std::vector<SplitPoint> dedup = dedupSplitPoints(splitPoints, epsilon);
        for (size_t j = 0; j + 1 < dedup.size(); ++j) {
            const Vertex2D& p0 = dedup[j].p;
            const Vertex2D& p1 = dedup[j + 1].p;
            if (samePoint(p0, p1, epsilon)) continue;
            EdgeKey key(p0, p1, epsilon);
            int delta = key.isForward(p0, epsilon) ? 1 : -1;
            auto it = signedUsage.find(key);
            if (it == signedUsage.end()) signedUsage[key] = delta;
            else it->second += delta;
        }
    }
}

static int chooseNextEdge(
    const std::vector<DirectedEdge>& edges, const std::vector<int>& candidates,
    const std::unordered_set<int>& used, double prevDx, double prevDy)
{
    int selected = -1;
    double bestAngle = 1e308;
    for (size_t i = 0; i < candidates.size(); ++i) {
        int idx = candidates[i];
        if (used.find(idx) != used.end()) continue;
        const DirectedEdge& e = edges[idx];
        double dx = e.end.x - e.start.x;
        double dy = e.end.y - e.start.y;
        double cross = prevDx * dy - prevDy * dx;
        double dot = prevDx * dx + prevDy * dy;
        double angle = std::atan2(cross, dot);
        if (angle <= 0.0) angle += 2.0 * M_PI;
        if (angle < bestAngle) {
            bestAngle = angle;
            selected = idx;
        }
    }
    return selected;
}

static std::vector<Vertex2D> traceLoop(
    const std::vector<DirectedEdge>& edges, int startEdgeIndex,
    const std::unordered_map<PointKey, std::vector<int>, PointKeyHasher>& outgoing,
    std::unordered_set<int>& used, double epsilon)
{
    std::vector<Vertex2D> loop;
    const DirectedEdge& startEdge = edges[startEdgeIndex];
    Vertex2D start = startEdge.start;
    Vertex2D current = startEdge.end;
    double prevDx = startEdge.end.x - startEdge.start.x;
    double prevDy = startEdge.end.y - startEdge.start.y;
    int guard = static_cast<int>(edges.size()) * 2 + 4;

    used.insert(startEdgeIndex);
    loop.push_back(copyVertex(start));
    loop.push_back(copyVertex(current));

    while (!samePoint(current, start, epsilon) && guard > 0) {
        PointKey key(current, epsilon);
        auto it = outgoing.find(key);
        int nextEdge = -1;
        if (it != outgoing.end()) {
            nextEdge = chooseNextEdge(edges, it->second, used, prevDx, prevDy);
        }
        if (nextEdge < 0) break;
        used.insert(nextEdge);
        const DirectedEdge& e = edges[nextEdge];
        prevDx = e.end.x - e.start.x;
        prevDy = e.end.y - e.start.y;
        current = e.end;
        if (!samePoint(current, start, epsilon)) {
            loop.push_back(copyVertex(current));
        }
        guard--;
    }
    if (loop.size() > 1 && samePoint(loop.front(), loop.back(), epsilon)) {
        loop.pop_back();
    }
    return loop;
}

static std::vector<std::vector<Vertex2D>> extractLoopsFromBoundaryEdges(
    const std::vector<DirectedEdge>& edges, double epsilon)
{
    std::unordered_map<PointKey, std::vector<int>, PointKeyHasher> outgoing;
    std::unordered_set<int> used;
    std::vector<std::vector<Vertex2D>> loops;
    for (size_t i = 0; i < edges.size(); ++i) {
        PointKey key(edges[i].start, epsilon);
        outgoing[key].push_back(static_cast<int>(i));
    }
    for (size_t i = 0; i < edges.size(); ++i) {
        if (used.find(static_cast<int>(i)) != used.end()) continue;
        std::vector<Vertex2D> loop = traceLoop(edges, static_cast<int>(i), outgoing, used, epsilon);
        if (loop.size() >= 3) loops.push_back(loop);
    }
    return loops;
}

static std::vector<std::vector<Vertex2D>> weldInternalEdges(
    const std::vector<std::vector<Vertex2D>>& contours, double epsilon)
{
    std::vector<Segment2D> segments;
    for (size_t i = 0; i < contours.size(); ++i) {
        const std::vector<Vertex2D>& contour = contours[i];
        for (size_t j = 0; j < contour.size(); ++j) {
            const Vertex2D& a = contour[j];
            const Vertex2D& b = contour[(j + 1) % contour.size()];
            if (samePoint(a, b, epsilon)) continue;
            segments.push_back(Segment2D(copyVertex(a), copyVertex(b)));
        }
    }
    if (segments.empty()) return contours;

    std::unordered_map<EdgeKey, int, EdgeKeyHasher> signedUsage;
    splitAndAccumulateSegments(segments, epsilon, signedUsage);

    std::vector<DirectedEdge> boundaryEdges;
    for (auto it = signedUsage.begin(); it != signedUsage.end(); ++it) {
        int balance = it->second;
        if (balance == 0) continue;
        if (balance > 0) boundaryEdges.push_back(DirectedEdge(it->first.a, it->first.b));
        else boundaryEdges.push_back(DirectedEdge(it->first.b, it->first.a));
    }
    if (boundaryEdges.empty()) return std::vector<std::vector<Vertex2D>>();
    return extractLoopsFromBoundaryEdges(boundaryEdges, epsilon);
}

void PolygonTopologicalMerger::mergeInPlace(Polygon2D* polygon) { mergeInPlace(polygon, 1E-9); }

void PolygonTopologicalMerger::mergeInPlace(Polygon2D* polygon, double epsilon)
{
    if (polygon == 0) return;
    std::vector<std::vector<Vertex2D>> canonicalContours;

    for (long int i = 0; i < polygon->loops.size(); ++i) {
        _Polygon2DContour* contour = polygon->loops.get(i);
        std::vector<Vertex2D> normalized = normalizeContour(contour, epsilon);
        if (normalized.size() < 3) continue;

        bool duplicate = false;
        for (size_t j = 0; j < canonicalContours.size(); ++j) {
            if (areEquivalentContours(canonicalContours[j], normalized, epsilon)) {
                duplicate = true;
                break;
            }
        }
        if (!duplicate) canonicalContours.push_back(normalized);
    }

    canonicalContours = weldInternalEdges(canonicalContours, epsilon);

    for (long int i = 0; i < polygon->loops.size(); ++i) {
        delete polygon->loops[i];
    }
    polygon->loops.clear();

    for (size_t i = 0; i < canonicalContours.size(); ++i) {
        polygon->nextLoop();
        const std::vector<Vertex2D>& contour = canonicalContours[i];
        for (size_t j = 0; j < contour.size(); ++j) {
            const Vertex2D& v = contour[j];
            polygon->addVertex(v.x, v.y, v.color.r(), v.color.g(), v.color.b());
        }
    }

    if (polygon->loops.size() == 0) polygon->nextLoop();
}
