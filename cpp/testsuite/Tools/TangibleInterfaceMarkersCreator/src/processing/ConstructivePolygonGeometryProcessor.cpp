#include "processing/ConstructivePolygonGeometryProcessor.h"

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonClipper/PolygonProcessor.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/_Polygon2DContour.h"
#include <algorithm>
#include <map>
#include <set>
#include <utility>
#include <vector>

static bool pointInLoop(const _Polygon2DContour* loop, double x, double y)
{
    if (!loop || loop->vertices.size() < 3) return false;
    bool inside = false;
    const long n = loop->vertices.size();
    for (long i = 0, j = n - 1; i < n; j = i++) {
        const Vertex2D vi = loop->vertices.get(i);
        const Vertex2D vj = loop->vertices.get(j);
        const bool intersects = ((vi.y > y) != (vj.y > y)) &&
            (x < (vj.x - vi.x) * (y - vi.y) / ((vj.y - vi.y) + 1e-30) + vi.x);
        if (intersects) inside = !inside;
    }
    return inside;
}

static bool pointInPolygon(const Polygon2D* polygon, double x, double y)
{
    if (!polygon) return false;
    bool inside = false;
    for (long i = 0; i < polygon->loops.size(); ++i) {
        _Polygon2DContour* loop = polygon->loops.get(i);
        if (!loop || loop->vertices.size() < 3) continue;
        if (pointInLoop(loop, x, y)) inside = !inside;
    }
    return inside;
}

static void addVertexIfNeeded(std::vector<std::pair<long, long> >& loop, long x, long y)
{
    if (loop.empty() || loop.back().first != x || loop.back().second != y) {
        loop.push_back(std::make_pair(x, y));
    }
}

static std::vector<std::pair<long, long> > traceLoop(
    std::map<std::pair<long, long>, std::vector<std::pair<long, long> > >& adjacency)
{
    std::vector<std::pair<long, long> > loop;
    std::pair<long, long> start;
    bool foundStart = false;

    for (std::map<std::pair<long, long>, std::vector<std::pair<long, long> > >::iterator it = adjacency.begin(); it != adjacency.end(); ++it) {
        if (!it->second.empty()) {
            start = it->first;
            foundStart = true;
            break;
        }
    }
    if (!foundStart) return loop;

    std::pair<long, long> current = start;
    std::pair<long, long> prev = std::make_pair(9223372036854775807LL, 9223372036854775807LL);
    addVertexIfNeeded(loop, current.first, current.second);

    for (long guard = 0; guard < 1000000; ++guard) {
        std::vector<std::pair<long, long> >& nexts = adjacency[current];
        if (nexts.empty()) break;

        std::pair<long, long> next = nexts.back();
        if (nexts.size() > 1 && next == prev) {
            next = nexts[nexts.size() - 2];
        }

        bool removed = false;
        for (long i = 0; i < (long)nexts.size(); ++i) {
            if (nexts[i] == next) {
                nexts.erase(nexts.begin() + i);
                removed = true;
                break;
            }
        }
        if (!removed) break;

        std::vector<std::pair<long, long> >& backList = adjacency[next];
        for (long i = 0; i < (long)backList.size(); ++i) {
            if (backList[i] == current) {
                backList.erase(backList.begin() + i);
                break;
            }
        }

        prev = current;
        current = next;
        addVertexIfNeeded(loop, current.first, current.second);

        if (current == start) break;
    }

    return loop;
}

ConstructivePolygonGeometryProcessor::ConstructivePolygonGeometryProcessor() {
}

ConstructivePolygonGeometryProcessor::~ConstructivePolygonGeometryProcessor() {
}

Polygon2D ConstructivePolygonGeometryProcessor::execute(java::ArrayList<Polygon2D*>* polygons) const
{
    Polygon2D out;
    if (!polygons || polygons->size() <= 0) {
        return out;
    }

    std::set<double> xs;
    std::set<double> ys;
    for (long p = 0; p < polygons->size(); ++p) {
        Polygon2D* polygon = polygons->get(p);
        if (!polygon) continue;
        for (long i = 0; i < polygon->loops.size(); ++i) {
            _Polygon2DContour* loop = polygon->loops.get(i);
            if (!loop) continue;
            for (long j = 0; j < loop->vertices.size(); ++j) {
                const Vertex2D v = loop->vertices.get(j);
                xs.insert(v.x);
                ys.insert(v.y);
            }
        }
    }

    if (xs.size() < 2 || ys.size() < 2) {
        return out;
    }

    std::vector<double> xv(xs.begin(), xs.end());
    std::vector<double> yv(ys.begin(), ys.end());
    const long nx = (long)xv.size();
    const long ny = (long)yv.size();

    std::vector<std::vector<unsigned char> > filled(nx - 1, std::vector<unsigned char>(ny - 1, 0));
    for (long ix = 0; ix < nx - 1; ++ix) {
        for (long iy = 0; iy < ny - 1; ++iy) {
            const double cx = (xv[ix] + xv[ix + 1]) * 0.5;
            const double cy = (yv[iy] + yv[iy + 1]) * 0.5;
            bool inAny = false;
            for (long p = 0; p < polygons->size(); ++p) {
                if (pointInPolygon(polygons->get(p), cx, cy)) {
                    inAny = true;
                    break;
                }
            }
            filled[ix][iy] = inAny ? 1 : 0;
        }
    }

    std::map<std::pair<long, long>, std::vector<std::pair<long, long> > > adjacency;
    for (long ix = 0; ix < nx - 1; ++ix) {
        for (long iy = 0; iy < ny - 1; ++iy) {
            if (!filled[ix][iy]) continue;

            if (iy == 0 || !filled[ix][iy - 1]) {
                std::pair<long, long> a = std::make_pair(ix, iy);
                std::pair<long, long> b = std::make_pair(ix + 1, iy);
                adjacency[a].push_back(b);
                adjacency[b].push_back(a);
            }
            if (ix == nx - 2 || !filled[ix + 1][iy]) {
                std::pair<long, long> a = std::make_pair(ix + 1, iy);
                std::pair<long, long> b = std::make_pair(ix + 1, iy + 1);
                adjacency[a].push_back(b);
                adjacency[b].push_back(a);
            }
            if (iy == ny - 2 || !filled[ix][iy + 1]) {
                std::pair<long, long> a = std::make_pair(ix + 1, iy + 1);
                std::pair<long, long> b = std::make_pair(ix, iy + 1);
                adjacency[a].push_back(b);
                adjacency[b].push_back(a);
            }
            if (ix == 0 || !filled[ix - 1][iy]) {
                std::pair<long, long> a = std::make_pair(ix, iy + 1);
                std::pair<long, long> b = std::make_pair(ix, iy);
                adjacency[a].push_back(b);
                adjacency[b].push_back(a);
            }
        }
    }

    bool firstLoop = true;
    while (true) {
        std::vector<std::pair<long, long> > loop = traceLoop(adjacency);
        if (loop.size() < 4) break;
        if (!firstLoop) out.nextLoop();
        firstLoop = false;

        for (long i = 0; i < (long)loop.size() - 1; ++i) {
            const long gx = loop[i].first;
            const long gy = loop[i].second;
            out.addVertex(xv[gx], yv[gy], 0.0, 0.0, 0.0);
        }
    }

    PolygonProcessor::classifyContourHoles(&out);
    return out;
}
