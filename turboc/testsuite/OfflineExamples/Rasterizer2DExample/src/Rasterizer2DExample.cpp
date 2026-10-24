#include <math.h>
#include <stdio.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/io/File.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/RGBPixel.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/render/raster/Rasterizer2D.h"

const int OUTPUT_WIDTH = 320;
const int OUTPUT_HEIGHT = 240;

static String argOrDefault(int argc, char** argv, int idx, const String& fallback)
{
    if (argc > idx && argv[idx] != 0 && String(argv[idx]).size() > 0) return String(argv[idx]);
    return fallback;
}

static int runLineTest(const String& outputFileName)
{
    RGBImageUncompressed img;
    img.init(OUTPUT_WIDTH, OUTPUT_HEIGHT);
    RGBPixel color((char)0xFF, 0, 0);

    for (double a = 0; a < 360.0; a += 15.0) {
        int x = 160 + (int)(100.0 * cos(a * M_PI / 180.0));
        int y = 120 + (int)(100.0 * sin(a * M_PI / 180.0));
        Rasterizer2D::drawLine(&img, 160, 120, x, y, color);
    }

    File out(outputFileName.c_str());
    BOOLEAN ok = ImagePersistence::exportPPM(out, &img);
    printf("Resulting image has been written to \"%s\"\n", outputFileName.c_str());
    return ok ? 0 : 1;
}

static int runPolygonTest(const String& outputFileName)
{
    RGBImageUncompressed img;
    img.init(OUTPUT_WIDTH, OUTPUT_HEIGHT);

    RGBPixel fillcolor((char)0xFF, 0, 0);
    RGBPixel bordercolor((char)0xFF, (char)0xFF, 0);

    Polygon2D pol;
    pol.addVertex(35, 25);
    pol.addVertex(200, 100);
    pol.addVertex(50, 150);
    pol.nextLoop();
    pol.addVertex(60, 75);
    pol.addVertex(125, 75);
    pol.addVertex(115, 110);

    Rasterizer2D::fillPolygon(&img, pol, fillcolor);
    Rasterizer2D::drawPolygon(&img, pol, bordercolor);

    File out(outputFileName.c_str());
    BOOLEAN ok = ImagePersistence::exportPPM(out, &img);
    printf("Resulting image has been written to \"%s\"\n", outputFileName.c_str());
    return ok ? 0 : 1;
}

static int runSmoothPolygonTest(const String& outputFileName)
{
    RGBImageUncompressed img;
    img.init(OUTPUT_WIDTH, OUTPUT_HEIGHT);
    img.createTestPattern();

    RGBPixel bordercolor((char)0xFF, (char)0xFF, 0);
    Polygon2D pol;
    pol.addVertex(35, 25, 1.0, 0.0, 0.0);
    pol.addVertex(200, 100, 0.0, 1.0, 0.0);
    pol.addVertex(50, 150, 0.0, 0.0, 1.0);
    pol.nextLoop();
    pol.addVertex(60, 75, 1.0, 1.0, 0.0);
    pol.addVertex(125, 75, 0.0, 1.0, 1.0);
    pol.addVertex(115, 110, 1.0, 0.0, 1.0);

    Rasterizer2D::fillSmoothPolygon(&img, pol);
    Rasterizer2D::drawPolygon(&img, pol, bordercolor);

    File out(outputFileName.c_str());
    BOOLEAN ok = ImagePersistence::exportPPM(out, &img);
    printf("Resulting image has been written to \"%s\"\n", outputFileName.c_str());
    return ok ? 0 : 1;
}

int main(int argc, char** argv)
{
    String mode = argOrDefault(argc, argv, 1, "line");
    String outputFileName = argOrDefault(argc, argv, 2, "output1.ppm");

    if (mode == "line") return runLineTest(outputFileName);
    if (mode == "polygon") return runPolygonTest(outputFileName);
    if (mode == "smooth") return runSmoothPolygonTest(outputFileName);

    fprintf(stderr, "Unknown mode: %s\n", mode.c_str());
    fprintf(stderr, "Usage: Rasterizer2DExample [line|polygon|smooth] [output.ppm]\n");
    return 1;
}
