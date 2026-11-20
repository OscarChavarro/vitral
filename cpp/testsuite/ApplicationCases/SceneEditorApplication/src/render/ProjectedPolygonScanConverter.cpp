#include <cmath>

#include "java/util/ArrayList.txx"
#include "render/ProjectedPolygon.h"
#include "render/ProjectedPolygonScanConverter.h"

void ProjectedPolygonScanConverter::fill(const ProjectedPolygon& polygon,
    int x0, int y0, int width, int height, SpanSink& sink)
{
    const Polygon2D* area = polygon.getArea();
    java::ArrayList<double> px;
    java::ArrayList<double> py;
    java::ArrayList<int> starts;
    double minY = 0.0;
    double maxY = 0.0;
    bool first = true;

    // Vertices in pixel coordinates, as AwtProjectedPolygonRenderer maps them
    for ( long l = 0; l < area->loops.size(); l++ ) {
        const _Polygon2DContour* loop = area->loops[l];
        starts.add((int)px.size());
        for ( long i = 0; i < loop->vertices.size(); i++ ) {
            double vx = x0 + (loop->vertices[i].x + 1.0) / 2.0 * width;
            double vy = y0 + (1.0 - loop->vertices[i].y) / 2.0 * height;
            px.add(vx);
            py.add(vy);
            if ( first || vy < minY ) minY = vy;
            if ( first || vy > maxY ) maxY = vy;
            first = false;
        }
    }
    starts.add((int)px.size());
    if ( first ) {
        return;
    }

    int rowStart = (int)std::floor(minY);
    int rowEnd = (int)std::ceil(maxY);
    if ( rowStart < y0 ) rowStart = y0;
    if ( rowEnd > y0 + height ) rowEnd = y0 + height;

    java::ArrayList<double> crossings;
    for ( int row = rowStart; row < rowEnd; row++ ) {
        double center = row + 0.5;
        crossings.clear();
        for ( long l = 0; l + 1 < starts.size(); l++ ) {
            long begin = starts[l];
            long end = starts[l + 1];
            if ( end - begin < 3 ) {
                continue;
            }
            for ( long i = begin; i < end; i++ ) {
                long j = i + 1 < end ? i + 1 : begin;
                double ya = py[i];
                double yb = py[j];
                if ( (ya <= center) == (yb <= center) ) {
                    continue;
                }
                double t = (center - ya) / (yb - ya);
                double cx = px[i] + t * (px[j] - px[i]);
                // Insertion in order (few crossings by row)
                long k = crossings.size();
                crossings.add(cx);
                while ( k > 0 && crossings[k - 1] > cx ) {
                    crossings[k] = crossings[k - 1];
                    k--;
                }
                crossings[k] = cx;
            }
        }
        for ( long k = 0; k + 1 < crossings.size(); k += 2 ) {
            int a = (int)std::ceil(crossings[k] - 0.5);
            int b = (int)std::ceil(crossings[k + 1] - 0.5);
            if ( a < x0 ) a = x0;
            if ( b > x0 + width ) b = x0 + width;
            if ( a < b ) {
                sink.fillSpan(a, b, row);
            }
        }
    }
}
