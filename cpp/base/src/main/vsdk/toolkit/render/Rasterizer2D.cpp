#include "vsdk/toolkit/render/Rasterizer2D.h"

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/geometry/element/Vertex2D.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/_Polygon2DContour.h"
#include "vsdk/toolkit/media/Image.h"
#include "vsdk/toolkit/media/RGBPixel.h"
#include "java/util/ArrayList.txx"

#include <java/lang/Math.h>
#include <cmath>
#include <stdexcept>

static void quicksortDoubles(double* array, int left0, int right0)
{
    int left = left0;
    int right = right0 + 1;
    double pivot;
    double temp;

    pivot = array[left0];
    do {
        do {
            left++;
        } while (left <= right0 && array[left] < pivot);
        do {
            right--;
            if (right < 0) {
                throw std::out_of_range("quicksort span underflow");
            }
        } while (array[right] > pivot);
        if (left < right) {
            temp = array[left];
            array[left] = array[right];
            array[right] = temp;
        }
    } while (left <= right);

    temp = array[left0];
    array[left0] = array[right];
    array[right] = temp;

    if (left0 < right) {
        quicksortDoubles(array, left0, right);
    }
    if (left < right0) {
        quicksortDoubles(array, left, right0);
    }
}

static void sortDoubles(java::ArrayList<double>& arr)
{
    if (arr.size() <= 0) {
        throw std::out_of_range("Cannot sort an empty span buffer");
    }
    quicksortDoubles(arr.data(), 0, static_cast<int>(arr.size()) - 1);
}

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

void Rasterizer2D::drawPolygon(Image* img, Polygon2D& p, const RGBPixel& color)
{
    if (img == 0) return;
    for (long int i = 0; i < p.loops.size(); i++) {
        if (p.loops[i]->vertices.size() == 0) continue;
        Vertex2D va(0, 0), vb(0, 0);
        for (long int j = 0; j + 1 < p.loops[i]->vertices.size(); j++) {
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
    java::ArrayList<double>& spanBuffer)
{
    double dx = vb.x - va.x;
    double dy = vb.y - va.y;
    double b, x;

    if (std::abs(dx) > VSDK::EPSILON && std::abs(dy / dx) <= 1.0 + VSDK::EPSILON) {
        double dydx = dy / dx;
        b = va.y - dydx * va.x;
        if (std::abs(dydx) < VSDK::EPSILON) {
            if (std::abs(va.y - h) <= 0.5) {
                spanBuffer.add(va.x);
                spanBuffer.add(vb.x);
            }
        }
        else {
            x = (h - b) / dydx;
            if ((va.y <= h && vb.y >= h) || (va.y >= h && vb.y <= h)) {
                spanBuffer.add(x);
            }
        }
    }

    if (std::abs(dy) > VSDK::EPSILON && std::abs(dx / dy) <= 1.0) {
        double dxdy = dx / dy;
        b = va.x - dxdy * va.y;
        if (std::abs(dxdy) < VSDK::EPSILON) {
            if ((va.y <= h && vb.y >= h) || (va.y >= h && vb.y <= h)) {
                spanBuffer.add(va.x);
            }
        }
        else {
            x = dxdy * h + b;
            if ((va.y <= h && vb.y >= h) || (va.y >= h && vb.y <= h)) {
                spanBuffer.add(x);
            }
        }
    }
}

void Rasterizer2D::fillPolygon(Image* img, Polygon2D& p, const RGBPixel& color)
{
    if (img == 0) return;
    int minx = img->getXSize();
    int miny = img->getYSize();
    int maxx = 0;
    int maxy = 0;

    for (long int i = 0; i < p.loops.size(); i++) {
        for (long int j = 0; j < p.loops[i]->vertices.size(); j++) {
            const Vertex2D& va = p.loops[i]->vertices[j];
            if (va.x < minx && va.x >= 0) minx = (int)va.x;
            if (va.x > maxx && va.x < img->getXSize()) maxx = (int)va.x;
            if (va.y < miny && va.y >= 0) miny = (int)va.y;
            if (va.y > maxy && va.y < img->getYSize()) maxy = (int)va.y;
        }
    }

    for (int y = miny; y <= maxy; y++) {
        java::ArrayList<double> spanBuffer;
        double h = y;

        for (long int i = 0; i < p.loops.size(); i++) {
            if (p.loops[i]->vertices.size() == 0) continue;
            for (long int j = 0; j + 1 < p.loops[i]->vertices.size(); j++) {
                fillPolygonProcessLine(p.loops[i]->vertices[j], p.loops[i]->vertices[j + 1], h, spanBuffer);
            }
            fillPolygonProcessLine(
                p.loops[i]->vertices[p.loops[i]->vertices.size() - 1],
                p.loops[i]->vertices[0], h, spanBuffer);
        }

        sortDoubles(spanBuffer);
        bool state = false;
        for (long int s = 0; s + 1 < spanBuffer.size(); s++) {
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
    Polygon2D& p, double x, double y, RGBPixel& outPixel)
{
    double outR = 0.0, outG = 0.0, outB = 0.0;
    double totaldistance = 0.0;

    for (long int i = 0; i < p.loops.size(); i++) {
        for (long int j = 0; j < p.loops[i]->vertices.size(); j++) {
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
    double clippedR = java::Math::max(0.0, java::Math::min(1.0, normalizedR));
    double clippedG = java::Math::max(0.0, java::Math::min(1.0, normalizedG));
    double clippedB = java::Math::max(0.0, java::Math::min(1.0, normalizedB));
    int rr = (int)(clippedR * 255.0);
    int gg = (int)(clippedG * 255.0);
    int bb = (int)(clippedB * 255.0);
    outPixel.r = (char)(rr & 0xFF);
    outPixel.g = (char)(gg & 0xFF);
    outPixel.b = (char)(bb & 0xFF);
}

void Rasterizer2D::fillSmoothPolygon(Image* img, Polygon2D& p)
{
    if (img == 0) return;
    int minx = img->getXSize();
    int miny = img->getYSize();
    int maxx = 0;
    int maxy = 0;

    for (long int i = 0; i < p.loops.size(); i++) {
        for (long int j = 0; j < p.loops[i]->vertices.size(); j++) {
            const Vertex2D& va = p.loops[i]->vertices[j];
            if (va.x < minx && va.x >= 0) minx = (int)va.x;
            if (va.x > maxx && va.x < img->getXSize()) maxx = (int)va.x;
            if (va.y < miny && va.y >= 0) miny = (int)va.y;
            if (va.y > maxy && va.y < img->getYSize()) maxy = (int)va.y;
        }
    }

    for (int y = miny; y <= maxy; y++) {
        java::ArrayList<double> spanBuffer;
        double h = y;

        for (long int i = 0; i < p.loops.size(); i++) {
            if (p.loops[i]->vertices.size() == 0) continue;
            for (long int j = 0; j + 1 < p.loops[i]->vertices.size(); j++) {
                fillPolygonProcessLine(p.loops[i]->vertices[j], p.loops[i]->vertices[j + 1], h, spanBuffer);
            }
            fillPolygonProcessLine(
                p.loops[i]->vertices[p.loops[i]->vertices.size() - 1],
                p.loops[i]->vertices[0], h, spanBuffer);
        }

        sortDoubles(spanBuffer);
        bool state = false;
        RGBPixel color;
        for (long int s = 0; s + 1 < spanBuffer.size(); s++) {
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
