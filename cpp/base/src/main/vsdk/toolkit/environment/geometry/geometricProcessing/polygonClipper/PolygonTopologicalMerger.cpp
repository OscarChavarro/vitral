#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonClipper/PolygonTopologicalMerger.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/_Polygon2DContour.h"
#include <cmath>
#include "java/util/ArrayList.txx"

void PolygonTopologicalMerger::mergeInPlace(Polygon2D* polygon) { mergeInPlace(polygon, 1E-9); }

void PolygonTopologicalMerger::mergeInPlace(Polygon2D* polygon, double epsilon)
{
    if (polygon == 0) return;
    for (long int i = 0; i < polygon->loops.size(); ++i) {
        _Polygon2DContour* c = polygon->loops[i];
        for (long int j = 1; j < c->vertices.size();) {
            Vertex2D a = c->vertices[j-1];
            Vertex2D b = c->vertices[j];
            if (std::fabs(a.x-b.x) <= epsilon && std::fabs(a.y-b.y) <= epsilon) c->vertices.remove(j);
            else ++j;
        }
    }
}
