#include <cmath>
#include <cstdio>
#include <string>

#include "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "java/io/File.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/RGBPixel.h"
#include "vsdk/toolkit/render/Rasterizer2D.h"

static std::string argOrDefault(int argc, char** argv, int idx, const std::string& fallback)
{
    if (argc > idx && argv[idx] != 0 && std::string(argv[idx]).size() > 0) return std::string(argv[idx]);
    return fallback;
}

static int runLineTest(const std::string& outputFileName)
{
    RGBImageUncompressed img;
    img.init(640, 480);
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

static int runPolygonTest(const std::string& outputFileName)
{
    RGBImageUncompressed img;
    img.init(640, 480);

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

static int runSmoothPolygonTest(const std::string& outputFileName)
{
    RGBImageUncompressed img;
    img.init(640, 480);
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
    std::string mode = argOrDefault(argc, argv, 1, "line");
    std::string outputFileName = argOrDefault(argc, argv, 2, "output1.png");

    if (mode == "line") return runLineTest(outputFileName);
    if (mode == "polygon") return runPolygonTest(outputFileName);
    if (mode == "smooth") return runSmoothPolygonTest(outputFileName);

    std::fprintf(stderr, "Unknown mode: %s\n", mode.c_str());
    std::fprintf(stderr, "Usage: Rasterizer2DExample [line|polygon|smooth] [output.png]\n");
    return 1;
}
