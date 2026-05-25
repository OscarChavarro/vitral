#include "vsdk/toolkit/render/Rasterizer2D.h"

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/geometry/elements/Vertex2D.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/_Polygon2DContour.h"
#include "vsdk/toolkit/media/Image.h"
#include "vsdk/toolkit/media/RGBPixel.h"

#include <algorithm>
#include <cmath>
#include <vector>

void Rasterizer2D::drawLine(Image* img, int x0, int y0, int x1, int y1, const RGBPixel& p)
{
    if (img == 0) return;
    double dx = (double)(x1 - x0);
    double dy = (double)(y1 - y0);
    int x, y;
    double xx, yy;

    if (std::abs(dx) > VSDK::EPSILON && std::abs(dy / dx) <= 1.0 && x1 > x0) {
        double dydx = dy / dx;
        for (x = x0, yy = (double)y0; x <= x1; x++) {
            y = (int)yy;
            if (x >= 0 && x < img->getXSize() && y >= 0 && y < img->getYSize()) {
                RGBPixel c(p);
                img->putPixelRgb(x, y, &c);
            }
            yy += dydx;
        }
    }
    else if (std::abs(dx) > VSDK::EPSILON && std::abs(dy / dx) <= 1.0 && x1 < x0) {
        double dydx = dy / dx;
        for (x = x1, yy = (double)y1; x <= x0; x++) {
            y = (int)yy;
            if (x >= 0 && x < img->getXSize() && y >= 0 && y < img->getYSize()) {
                RGBPixel c(p);
                img->putPixelRgb(x, y, &c);
            }
            yy += dydx;
        }
    }
    else if (std::abs(dy) > VSDK::EPSILON && y1 > y0) {
        double dxdy = dx / dy;
        for (y = y0, xx = (double)x0; y <= y1; y++) {
            x = (int)xx;
            if (x >= 0 && x < img->getXSize() && y >= 0 && y < img->getYSize()) {
                RGBPixel c(p);
                img->putPixelRgb(x, y, &c);
            }
            xx += dxdy;
        }
    }
    else if (std::abs(dy) > VSDK::EPSILON && y1 < y0) {
        double dxdy = dx / dy;
        for (y = y1, xx = (double)x1; y <= y0; y++) {
            x = (int)xx;
            if (x >= 0 && x < img->getXSize() && y >= 0 && y < img->getYSize()) {
                RGBPixel c(p);
                img->putPixelRgb(x, y, &c);
            }
            xx += dxdy;
        }
    }
}

void Rasterizer2D::drawPolygon(Image* img, const Polygon2D& p, const RGBPixel& color)
{
    if (img == 0) return;
    for (size_t i = 0; i < p.loops.size(); i++) {
        if (p.loops[i]->vertices.empty()) continue;
        Vertex2D va(0, 0), vb(0, 0);
        for (size_t j = 0; j + 1 < p.loops[i]->vertices.size(); j++) {
            va = p.loops[i]->vertices[j];
            vb = p.loops[i]->vertices[j + 1];
            drawLine(img, (int)va.x, (int)va.y, (int)vb.x, (int)vb.y, color);
        }
        va = p.loops[i]->vertices[p.loops[i]->vertices.size() - 1];
        vb = p.loops[i]->vertices[0];
        drawLine(img, (int)va.x, (int)va.y, (int)vb.x, (int)vb.y, color);
    }
}

void Rasterizer2D::fillPolygonProcessLine(
    const Vertex2D& va,
    const Vertex2D& vb,
    double h,
    std::vector<double>& spanBuffer)
{
    double dx = vb.x - va.x;
    double dy = vb.y - va.y;
    double b, x;

    if (std::abs(dx) > VSDK::EPSILON && std::abs(dy / dx) <= 1.0 + VSDK::EPSILON) {
        double dydx = dy / dx;
        b = va.y - dydx * va.x;
        if (std::abs(dydx) < VSDK::EPSILON) {
            if (std::abs(va.y - h) <= 0.5) {
                spanBuffer.push_back(va.x);
                spanBuffer.push_back(vb.x);
            }
        }
        else {
            x = (h - b) / dydx;
            if ((va.y <= h && vb.y >= h) || (va.y >= h && vb.y <= h)) {
                spanBuffer.push_back(x);
            }
        }
    }

    if (std::abs(dy) > VSDK::EPSILON && std::abs(dx / dy) <= 1.0) {
        double dxdy = dx / dy;
        b = va.x - dxdy * va.y;
        if (std::abs(dxdy) < VSDK::EPSILON) {
            if ((va.y <= h && vb.y >= h) || (va.y >= h && vb.y <= h)) {
                spanBuffer.push_back(va.x);
            }
        }
        else {
            x = dxdy * h + b;
            if ((va.y <= h && vb.y >= h) || (va.y >= h && vb.y <= h)) {
                spanBuffer.push_back(x);
            }
        }
    }
}

void Rasterizer2D::fillPolygon(Image* img, const Polygon2D& p, const RGBPixel& color)
{
    if (img == 0) return;
    int minx = img->getXSize();
    int miny = img->getYSize();
    int maxx = 0;
    int maxy = 0;

    for (size_t i = 0; i < p.loops.size(); i++) {
        for (size_t j = 0; j < p.loops[i]->vertices.size(); j++) {
            const Vertex2D& va = p.loops[i]->vertices[j];
            if (va.x < minx && va.x >= 0) minx = (int)va.x;
            if (va.x > maxx && va.x < img->getXSize()) maxx = (int)va.x;
            if (va.y < miny && va.y >= 0) miny = (int)va.y;
            if (va.y > maxy && va.y < img->getYSize()) maxy = (int)va.y;
        }
    }

    for (int y = miny; y <= maxy; y++) {
        std::vector<double> spanBuffer;
        spanBuffer.reserve(p.loops.size() * 4);
        double h = y;

        for (size_t i = 0; i < p.loops.size(); i++) {
            if (p.loops[i]->vertices.empty()) continue;
            for (size_t j = 0; j + 1 < p.loops[i]->vertices.size(); j++) {
                fillPolygonProcessLine(p.loops[i]->vertices[j], p.loops[i]->vertices[j + 1], h, spanBuffer);
            }
            fillPolygonProcessLine(
                p.loops[i]->vertices[p.loops[i]->vertices.size() - 1],
                p.loops[i]->vertices[0], h, spanBuffer);
        }

        std::sort(spanBuffer.begin(), spanBuffer.end());
        bool state = false;
        for (size_t s = 0; s + 1 < spanBuffer.size(); s++) {
            double xs1 = spanBuffer[s];
            double xs2 = spanBuffer[s + 1];
            state = !state;
            if (xs2 < minx || xs1 > maxx) continue;
            else if (xs2 < minx) xs2 = minx;
            if (xs2 > maxx) xs2 = maxx;
            for (int x = (int)xs1; state && x < (int)xs2; x++) {
                RGBPixel c(color);
                img->putPixelRgb(x, y, &c);
            }
        }
    }
}

void Rasterizer2D::fillSmoothPolygonCalculateColor(
    const Polygon2D& p, double x, double y, RGBPixel& outPixel)
{
    double outR = 0.0, outG = 0.0, outB = 0.0;
    double totaldistance = 0.0;

    for (size_t i = 0; i < p.loops.size(); i++) {
        for (size_t j = 0; j < p.loops[i]->vertices.size(); j++) {
            const Vertex2D& va = p.loops[i]->vertices[j];
            double distance = 1.0 / (1.0 + std::sqrt((va.x - x) * (va.x - x) + (va.y - y) * (va.y - y)));
            totaldistance += distance;
            outR += va.color.r() * distance;
            outG += va.color.g() * distance;
            outB += va.color.b() * distance;
        }
    }
    if (totaldistance <= VSDK::EPSILON) {
        outPixel.r = 0; outPixel.g = 0; outPixel.b = 0;
        return;
    }
    double normalizedR = outR / totaldistance;
    double normalizedG = outG / totaldistance;
    double normalizedB = outB / totaldistance;
    double clippedR = std::max(0.0, std::min(1.0, normalizedR));
    double clippedG = std::max(0.0, std::min(1.0, normalizedG));
    double clippedB = std::max(0.0, std::min(1.0, normalizedB));
    int rr = (int)(clippedR * 255.0);
    int gg = (int)(clippedG * 255.0);
    int bb = (int)(clippedB * 255.0);
    outPixel.r = (char)(rr & 0xFF);
    outPixel.g = (char)(gg & 0xFF);
    outPixel.b = (char)(bb & 0xFF);
}

void Rasterizer2D::fillSmoothPolygon(Image* img, const Polygon2D& p)
{
    if (img == 0) return;
    int minx = img->getXSize();
    int miny = img->getYSize();
    int maxx = 0;
    int maxy = 0;

    for (size_t i = 0; i < p.loops.size(); i++) {
        for (size_t j = 0; j < p.loops[i]->vertices.size(); j++) {
            const Vertex2D& va = p.loops[i]->vertices[j];
            if (va.x < minx && va.x >= 0) minx = (int)va.x;
            if (va.x > maxx && va.x < img->getXSize()) maxx = (int)va.x;
            if (va.y < miny && va.y >= 0) miny = (int)va.y;
            if (va.y > maxy && va.y < img->getYSize()) maxy = (int)va.y;
        }
    }

    for (int y = miny; y <= maxy; y++) {
        std::vector<double> spanBuffer;
        spanBuffer.reserve(p.loops.size() * 4);
        double h = y;

        for (size_t i = 0; i < p.loops.size(); i++) {
            if (p.loops[i]->vertices.empty()) continue;
            for (size_t j = 0; j + 1 < p.loops[i]->vertices.size(); j++) {
                fillPolygonProcessLine(p.loops[i]->vertices[j], p.loops[i]->vertices[j + 1], h, spanBuffer);
            }
            fillPolygonProcessLine(
                p.loops[i]->vertices[p.loops[i]->vertices.size() - 1],
                p.loops[i]->vertices[0], h, spanBuffer);
        }

        std::sort(spanBuffer.begin(), spanBuffer.end());
        bool state = false;
        RGBPixel color;
        for (size_t s = 0; s + 1 < spanBuffer.size(); s++) {
            double xs1 = spanBuffer[s];
            double xs2 = spanBuffer[s + 1];
            state = !state;
            if (xs2 < minx || xs1 > maxx) continue;
            else if (xs2 < minx) xs2 = minx;
            if (xs2 > maxx) xs2 = maxx;
            for (int x = (int)xs1; state && x < (int)xs2; x++) {
                fillSmoothPolygonCalculateColor(p, x, y, color);
                img->putPixelRgb(x, y, &color);
            }
        }
    }
}
