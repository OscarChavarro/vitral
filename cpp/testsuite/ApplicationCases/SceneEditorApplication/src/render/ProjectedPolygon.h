#ifndef __PROJECTED_POLYGON__
#define __PROJECTED_POLYGON__

#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/environment/geometry/element/Vertex2D.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/_Polygon2DContour.h"

/**
A filled 2D polygon (maybe with holes, and with several main contours),
already projected to the normalized device coordinates of a viewport (from
<-1, -1> at its lower left corner to <1, 1> at its upper right corner). It
is the technology independent output of `ProjectedSurfacesViewBuilder`, as
`ProjectedLineLayer` is for lines: any 2D drawing technology can present it
by mapping those coordinates to its own pixels. Its contours are filled with
the even-odd rule.

C++ port: it keeps its own copy of the contours.
*/
class ProjectedPolygon {
private:
    Polygon2D area;
    ColorRgb color;

    ProjectedPolygon(const ProjectedPolygon& other);
    ProjectedPolygon& operator=(const ProjectedPolygon& other);

public:
    /**
    @param area contours of the polygon, in normalized device coordinates
    @param color fill color
    */
    ProjectedPolygon(const Polygon2D* source, const ColorRgb& color)
        : color(color)
    {
        for ( long i = 0; i < area.loops.size(); i++ ) {
            delete area.loops[i];
        }
        area.loops.clear();
        for ( long l = 0; l < source->loops.size(); l++ ) {
            const _Polygon2DContour* loop = source->loops[l];
            area.nextLoop();
            for ( long i = 0; i < loop->vertices.size(); i++ ) {
                area.addVertex(loop->vertices[i].x, loop->vertices[i].y);
            }
        }
    }

    const Polygon2D* getArea() const { return &area; }
    const ColorRgb& getColor() const { return color; }
};

#endif
