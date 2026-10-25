#include <cmath>
#include <cstdio>
#include <cstdlib>

#include "java/io/File.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/RGBPixel.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/render/raster/Rasterizer2D.h"
static java::String argOrDefault(int argc, char** argv, int idx, const java::String& fallback)
{
    if (argc > idx && argv[idx] != 0 && java::String(argv[idx]).size() > 0) return java::String(argv[idx]);
    return fallback;
}

const int DEFAULT_OUTPUT_WIDTH = 640;
const int DEFAULT_OUTPUT_HEIGHT = 480;

static int positiveIntOrDefault(int argc, char** argv, int idx, int fallback)
{
    if (argc <= idx || argv[idx] == 0) return fallback;
    int value = std::atoi(argv[idx]);
    return value > 0 ? value : fallback;
}

static int runLineTest(const java::String& outputFileName, int outputWidth, int outputHeight)
{
    RGBImageUncompressed img;
    img.init(outputWidth, outputHeight);
    RGBPixel color((char)0xFF, 0, 0);

    for (double a = 0; a < 360.0; a += 15.0) {
        int x = 320 + (int)(200.0 * std::cos(a * M_PI / 180.0));
        int y = 240 + (int)(200.0 * std::sin(a * M_PI / 180.0));
        Rasterizer2D::drawLine(&img, 320, 240, x, y, color);
    }

    java::File out(outputFileName.c_str());
    bool ok = ImagePersistence::exportPNG(out, &img);
    std::printf("Resulting image has been written to \"%s\"\n", outputFileName.c_str());
    return ok ? 0 : 1;
}

static int runPolygonTest(const java::String& outputFileName, int outputWidth, int outputHeight)
{
    RGBImageUncompressed img;
    img.init(outputWidth, outputHeight);

    RGBPixel fillcolor((char)0xFF, 0, 0);
    RGBPixel bordercolor((char)0xFF, (char)0xFF, 0);

    Polygon2D pol;
    pol.addVertex(70, 50);
    pol.addVertex(400, 200);
    pol.addVertex(100, 300);
    pol.nextLoop();
    pol.addVertex(120, 150);
    pol.addVertex(250, 150);
    pol.addVertex(230, 220);

    Rasterizer2D::fillPolygon(&img, pol, fillcolor);
    Rasterizer2D::drawPolygon(&img, pol, bordercolor);

    java::File out(outputFileName.c_str());
    bool ok = ImagePersistence::exportPNG(out, &img);
    std::printf("Resulting image has been written to \"%s\"\n", outputFileName.c_str());
    return ok ? 0 : 1;
}

static int runSmoothPolygonTest(const java::String& outputFileName, int outputWidth, int outputHeight)
{
    RGBImageUncompressed img;
    img.init(outputWidth, outputHeight);
    img.createTestPattern();

    RGBPixel bordercolor((char)0xFF, (char)0xFF, 0);
    Polygon2D pol;
    pol.addVertex(70, 50, 1.0, 0.0, 0.0);
    pol.addVertex(400, 200, 0.0, 1.0, 0.0);
    pol.addVertex(100, 300, 0.0, 0.0, 1.0);
    pol.nextLoop();
    pol.addVertex(120, 150, 1.0, 1.0, 0.0);
    pol.addVertex(250, 150, 0.0, 1.0, 1.0);
    pol.addVertex(230, 220, 1.0, 0.0, 1.0);

    Rasterizer2D::fillSmoothPolygon(&img, pol);
    Rasterizer2D::drawPolygon(&img, pol, bordercolor);

    java::File out(outputFileName.c_str());
    bool ok = ImagePersistence::exportPNG(out, &img);
    std::printf("Resulting image has been written to \"%s\"\n", outputFileName.c_str());
    return ok ? 0 : 1;
}

int main(int argc, char** argv)
{
    java::String mode = argOrDefault(argc, argv, 1, "line");
    java::String outputFileName = argOrDefault(argc, argv, 2, "output1.png");
    int outputWidth = positiveIntOrDefault(argc, argv, 3, DEFAULT_OUTPUT_WIDTH);
    int outputHeight = positiveIntOrDefault(argc, argv, 4, DEFAULT_OUTPUT_HEIGHT);

    if (mode == "line") return runLineTest(outputFileName, outputWidth, outputHeight);
    if (mode == "polygon") return runPolygonTest(outputFileName, outputWidth, outputHeight);
    if (mode == "smooth") return runSmoothPolygonTest(outputFileName, outputWidth, outputHeight);

    std::fprintf(stderr, "Unknown mode: %s\n", mode.c_str());
    std::fprintf(stderr, "Usage: Rasterizer2DExample [line|polygon|smooth] [output.png] [width] [height]\n");
    return 1;
}
