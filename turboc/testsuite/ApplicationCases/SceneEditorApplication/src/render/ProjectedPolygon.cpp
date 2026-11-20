#include "render/ProjectedPolygon.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.h"

ProjectedPolygon::ProjectedPolygon(Polygon2D* area, const ColorRgb& color)
    : area(area), color(color)
{
}

ProjectedPolygon::~ProjectedPolygon()
{
    delete area;
}

Polygon2D* ProjectedPolygon::getArea() const
{
    return area;
}

const ColorRgb& ProjectedPolygon::getColor() const
{
    return color;
}
