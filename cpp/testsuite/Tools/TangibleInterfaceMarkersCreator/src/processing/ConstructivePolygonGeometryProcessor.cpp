#include "java/util/Collections.txx"
#include "java/util/TreeMap.txx"
#include "java/util/TreeSet.txx"
#include "java/util/ArrayList.txx"
#include "processing/ConstructivePolygonGeometryProcessor.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/_Polygon2DContour.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonClipper/PolygonProcessor.h"
/** A vertex of the cell grid, by its column and row indices */
class GridPoint {
  public:
    long x;
    long y;

    GridPoint() : x(0), y(0) {}
    GridPoint(long x, long y) : x(x), y(y) {}

    bool operator<(const GridPoint& other) const
    {
        return x < other.x || (x == other.x && y < other.y);
    }

    bool operator==(const GridPoint& other) const
    {
        return x == other.x && y == other.y;
    }
};

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

static void addVertexIfNeeded(java::ArrayList<GridPoint >& loop, long x, long y)
{
    if (loop.isEmpty() || loop[loop.size() - 1].x != x || loop[loop.size() - 1].y != y) {
        loop.add(GridPoint(x, y));
    }
}

static java::ArrayList<GridPoint > traceLoop(
    java::TreeMap<GridPoint, java::ArrayList<GridPoint > >& adjacency)
{
    java::ArrayList<GridPoint > loop;
    GridPoint start;
    bool foundStart = false;

    for (long it = 0; it < adjacency.size(); it++) {
        if (!adjacency.getValue(it).isEmpty()) {
            start = adjacency.getKey(it);
            foundStart = true;
            break;
        }
    }
    if (!foundStart) return loop;

    GridPoint current = start;
    GridPoint prev = GridPoint(9223372036854775807LL, 9223372036854775807LL);
    addVertexIfNeeded(loop, current.x, current.y);

    for (long guard = 0; guard < 1000000; ++guard) {
        java::ArrayList<GridPoint >& nexts = adjacency.putIfAbsent(current, java::ArrayList<GridPoint >());
        if (nexts.isEmpty()) break;

        GridPoint next = nexts[nexts.size() - 1];
        if (nexts.size() > 1 && next == prev) {
            next = nexts[nexts.size() - 2];
        }

        bool removed = false;
        for (long i = 0; i < (long)nexts.size(); ++i) {
            if (nexts[i] == next) {
                nexts.remove(i);
                removed = true;
                break;
            }
        }
        if (!removed) break;

        java::ArrayList<GridPoint >& backList = adjacency.putIfAbsent(next, java::ArrayList<GridPoint >());
        for (long i = 0; i < (long)backList.size(); ++i) {
            if (backList[i] == current) {
                backList.remove(i);
                break;
            }
        }

        prev = current;
        current = next;
        addVertexIfNeeded(loop, current.x, current.y);

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

    java::TreeSet<double> xs;
    java::TreeSet<double> ys;
    for (long p = 0; p < polygons->size(); ++p) {
        Polygon2D* polygon = polygons->get(p);
        if (!polygon) continue;
        for (long i = 0; i < polygon->loops.size(); ++i) {
            _Polygon2DContour* loop = polygon->loops.get(i);
            if (!loop) continue;
            for (long j = 0; j < loop->vertices.size(); ++j) {
                const Vertex2D v = loop->vertices.get(j);
                xs.add(v.x);
                ys.add(v.y);
            }
        }
    }

    if (xs.size() < 2 || ys.size() < 2) {
        return out;
    }

    java::ArrayList<double> xv;
    java::ArrayList<double> yv;
    for (long i = 0; i < xs.size(); ++i) xv.add(xs.get(i));
    for (long i = 0; i < ys.size(); ++i) yv.add(ys.get(i));
    const long nx = (long)xv.size();
    const long ny = (long)yv.size();

    java::ArrayList<java::ArrayList<unsigned char> > filled = java::Collections::nCopies<java::ArrayList<unsigned char> >(nx - 1, java::Collections::nCopies<unsigned char>(ny - 1, (unsigned char)0));
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

    java::TreeMap<GridPoint, java::ArrayList<GridPoint > > adjacency;
    for (long ix = 0; ix < nx - 1; ++ix) {
        for (long iy = 0; iy < ny - 1; ++iy) {
            if (!filled[ix][iy]) continue;

            if (iy == 0 || !filled[ix][iy - 1]) {
                GridPoint a = GridPoint(ix, iy);
                GridPoint b = GridPoint(ix + 1, iy);
                adjacency.putIfAbsent(a, java::ArrayList<GridPoint >()).add(b);
                adjacency.putIfAbsent(b, java::ArrayList<GridPoint >()).add(a);
            }
            if (ix == nx - 2 || !filled[ix + 1][iy]) {
                GridPoint a = GridPoint(ix + 1, iy);
                GridPoint b = GridPoint(ix + 1, iy + 1);
                adjacency.putIfAbsent(a, java::ArrayList<GridPoint >()).add(b);
                adjacency.putIfAbsent(b, java::ArrayList<GridPoint >()).add(a);
            }
            if (iy == ny - 2 || !filled[ix][iy + 1]) {
                GridPoint a = GridPoint(ix + 1, iy + 1);
                GridPoint b = GridPoint(ix, iy + 1);
                adjacency.putIfAbsent(a, java::ArrayList<GridPoint >()).add(b);
                adjacency.putIfAbsent(b, java::ArrayList<GridPoint >()).add(a);
            }
            if (ix == 0 || !filled[ix - 1][iy]) {
                GridPoint a = GridPoint(ix, iy + 1);
                GridPoint b = GridPoint(ix, iy);
                adjacency.putIfAbsent(a, java::ArrayList<GridPoint >()).add(b);
                adjacency.putIfAbsent(b, java::ArrayList<GridPoint >()).add(a);
            }
        }
    }

    bool firstLoop = true;
    while (true) {
        java::ArrayList<GridPoint > loop = traceLoop(adjacency);
        if (loop.size() < 4) break;
        if (!firstLoop) out.nextLoop();
        firstLoop = false;

        for (long i = 0; i < (long)loop.size() - 1; ++i) {
            const long gx = loop[i].x;
            const long gy = loop[i].y;
            out.addVertex(xv[gx], yv[gy], 0.0, 0.0, 0.0);
        }
    }

    PolygonProcessor::classifyContourHoles(&out);
    return out;
}
