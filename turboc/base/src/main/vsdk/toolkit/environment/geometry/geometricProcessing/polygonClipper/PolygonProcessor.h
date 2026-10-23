#ifndef __POLYGON_PROCESSOR__
#define __POLYGON_PROCESSOR__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/_Polygon2DContour.h"
class PolygonProcessor {
public:
    static Polygon2D* polygon2DSimplify(const Polygon2D& pol2DIn, double epsilon, BOOLEAN copy);
    static BOOLEAN contourInsidePolygon(const ArrayList<Vertex2D>& contour, const ArrayList<Vertex2D>& mainPolygon);
    static signed char isPointInsidePolygon2D(const Vertex2D& point, const ArrayList<Vertex2D>& polygon);
    static void classifyContourHoles(Polygon2D* polygon);
};

#endif
