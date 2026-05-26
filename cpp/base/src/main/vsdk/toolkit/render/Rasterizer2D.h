#ifndef __VSDK_TOOLKIT_RENDER_RASTERIZER2D_H__
#define __VSDK_TOOLKIT_RENDER_RASTERIZER2D_H__

#include "vsdk/toolkit/render/RenderingElement.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.h"
#include "java/util/ArrayList.h"

class Image;
class RGBPixel;
class Vertex2D;

class Rasterizer2D : public RenderingElement {
public:
    static void drawLine(Image* img, int x0, int y0, int x1, int y1, const RGBPixel& p);
    static void drawPolygon(Image* img, Polygon2D& p, const RGBPixel& color);
    static void fillPolygon(Image* img, Polygon2D& p, const RGBPixel& color);
    static void fillSmoothPolygon(Image* img, Polygon2D& p);

private:
    static void fillPolygonProcessLine(
        const Vertex2D& va,
        const Vertex2D& vb,
        double h,
        java::ArrayList<double>& spanBuffer);
    static void fillSmoothPolygonCalculateColor(
        Polygon2D& p,
        double x,
        double y,
        RGBPixel& outPixel);
};

#endif
