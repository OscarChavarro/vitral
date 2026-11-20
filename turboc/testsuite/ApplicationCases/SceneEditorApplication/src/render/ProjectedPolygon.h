#ifndef __PROJECTED_POLYGON__
#define __PROJECTED_POLYGON__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
class Polygon2D;

/**
A filled 2D polygon (maybe with holes, and with several main contours),
already projected to the normalized device coordinates of a viewport (from
<-1, -1> at its lower left corner to <1, 1> at its upper right corner). It
is the technology independent output of `ProjectedSurfacesViewBuilder`, as
`ProjectedLineLayer` is for lines: any 2D drawing technology can present it
by mapping those coordinates to its own pixels. Its contours are filled with
the even-odd rule.

C++ port note: the polygon owns its area.
*/
class ProjectedPolygon
{
private:
    Polygon2D* area;
    ColorRgb color;

    ProjectedPolygon(const ProjectedPolygon& other);
    ProjectedPolygon& operator=(const ProjectedPolygon& other);

public:
    /**
    @param area contours of the polygon, in normalized device coordinates
    (owned by the new polygon)
    @param color fill color
    */
    ProjectedPolygon(Polygon2D* area, const ColorRgb& color);
    ~ProjectedPolygon();

    Polygon2D* getArea() const;
    const ColorRgb& getColor() const;
};

#endif
