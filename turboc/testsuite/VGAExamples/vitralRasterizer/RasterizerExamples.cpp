#include "RasterizerExamples.h"
#include "View.h"
#include <math.h>
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/RGBPixel.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.h"
#include "vsdk/toolkit/render/raster/Rasterizer2D.h"

void
RasterizerExamples::drawLines(RGBImageUncompressed *img, int width, int height) {
    View v(width, height, 120, 40, 400, 400);
    RGBPixel color((char)0xFF, 0, 0);
    for (int degrees = 0; degrees < 360; degrees += 15) {
        double a = degrees * 3.14159265358979323846 / 180.0;
        Rasterizer2D::drawLine(img, (int)v.x(320), (int)v.y(240), (int)v.x(320.0 + 200.0 * cos(a)),
                               (int)v.y(240.0 + 200.0 * sin(a)), color);
    }
}

void
RasterizerExamples::drawPolygons(RGBImageUncompressed *img, int width, int height) {
    View v(width, height, 70, 50, 330, 250);
    RGBPixel fillColor((char)0xFF, 0, 0);
    RGBPixel borderColor((char)0xFF, (char)0xFF, 0);
    Polygon2D pol;
    pol.addVertex(v.x(70), v.y(50));
    pol.addVertex(v.x(400), v.y(200));
    pol.addVertex(v.x(100), v.y(300));
    pol.nextLoop();
    pol.addVertex(v.x(120), v.y(150));
    pol.addVertex(v.x(250), v.y(150));
    pol.addVertex(v.x(230), v.y(220));
    Rasterizer2D::fillPolygon(img, pol, fillColor);
    Rasterizer2D::drawPolygon(img, pol, borderColor);
}

void
RasterizerExamples::drawSmoothPolygons(RGBImageUncompressed *img, int width, int height) {
    View v(width, height, 70, 50, 330, 250);
    RGBPixel borderColor((char)0xFF, (char)0xFF, 0);
    img->createTestPattern();
    Polygon2D pol;
    pol.addVertex(v.x(70), v.y(50), 1.0, 0.0, 0.0);
    pol.addVertex(v.x(400), v.y(200), 0.0, 1.0, 0.0);
    pol.addVertex(v.x(100), v.y(300), 0.0, 0.0, 1.0);
    pol.nextLoop();
    pol.addVertex(v.x(120), v.y(150), 1.0, 1.0, 0.0);
    pol.addVertex(v.x(250), v.y(150), 0.0, 1.0, 1.0);
    pol.addVertex(v.x(230), v.y(220), 1.0, 0.0, 1.0);
    Rasterizer2D::fillSmoothPolygon(img, pol);
    Rasterizer2D::drawPolygon(img, pol, borderColor);
}

void
RasterizerExamples::draw(int which, RGBImageUncompressed *img, int width, int height) {
    switch (which) {
    case 0: drawLines(img, width, height); break;
    case 1: drawPolygons(img, width, height); break;
    default: drawSmoothPolygons(img, width, height); break;
    }
}
