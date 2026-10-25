#include <math.h>
#include <stdio.h>
#include <stdlib.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/io/File.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/RGBPixel.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/render/raster/Rasterizer2D.h"

// Scene coordinates were authored on this reference canvas.  Keep geometry in
// these logical units so changing the output size does not require changing
// every primitive in the example.
const int REFERENCE_WIDTH = 320;
const int REFERENCE_HEIGHT = 240;
const int DEFAULT_OUTPUT_WIDTH = 640;
const int DEFAULT_OUTPUT_HEIGHT = 480;

static String argOrDefault(int argc, char** argv, int idx, const String& fallback)
{
    if (argc > idx && argv[idx] != 0 && String(argv[idx]).size() > 0) return String(argv[idx]);
    return fallback;
}

static int positiveIntOrDefault(int argc, char** argv, int idx, int fallback)
{
    int value;
    if (argc <= idx || argv[idx] == 0) return fallback;
    value = atoi(argv[idx]);
    return value > 0 ? value : fallback;
}

static int scaleX(int x, int outputWidth)
{
    return (int)(((long)x * outputWidth + REFERENCE_WIDTH / 2) / REFERENCE_WIDTH);
}

static int scaleY(int y, int outputHeight)
{
    return (int)(((long)y * outputHeight + REFERENCE_HEIGHT / 2) / REFERENCE_HEIGHT);
}

static int runLineTest(const String& outputFileName, int outputWidth, int outputHeight)
{
    RGBImageUncompressed img;
    img.init(outputWidth, outputHeight);
    RGBPixel color((char)0xFF, 0, 0);

    for (double a = 0; a < 360.0; a += 15.0) {
        int x = 160 + (int)(100.0 * cos(a * M_PI / 180.0));
        int y = 120 + (int)(100.0 * sin(a * M_PI / 180.0));
        Rasterizer2D::drawLine(&img, scaleX(160, outputWidth), scaleY(120, outputHeight),
                               scaleX(x, outputWidth), scaleY(y, outputHeight), color);
    }

    File out(outputFileName.c_str());
    BOOLEAN ok = ImagePersistence::exportPPM(out, &img);
    printf("Resulting image has been written to \"%s\"\n", outputFileName.c_str());
    return ok ? 0 : 1;
}

static int runPolygonTest(const String& outputFileName, int outputWidth, int outputHeight)
{
    RGBImageUncompressed img;
    img.init(outputWidth, outputHeight);

    RGBPixel fillcolor((char)0xFF, 0, 0);
    RGBPixel bordercolor((char)0xFF, (char)0xFF, 0);

    Polygon2D pol;
    pol.addVertex(scaleX(35, outputWidth), scaleY(25, outputHeight));
    pol.addVertex(scaleX(200, outputWidth), scaleY(100, outputHeight));
    pol.addVertex(scaleX(50, outputWidth), scaleY(150, outputHeight));
    pol.nextLoop();
    pol.addVertex(scaleX(60, outputWidth), scaleY(75, outputHeight));
    pol.addVertex(scaleX(125, outputWidth), scaleY(75, outputHeight));
    pol.addVertex(scaleX(115, outputWidth), scaleY(110, outputHeight));

    Rasterizer2D::fillPolygon(&img, pol, fillcolor);
    Rasterizer2D::drawPolygon(&img, pol, bordercolor);

    File out(outputFileName.c_str());
    BOOLEAN ok = ImagePersistence::exportPPM(out, &img);
    printf("Resulting image has been written to \"%s\"\n", outputFileName.c_str());
    return ok ? 0 : 1;
}

static int runSmoothPolygonTest(const String& outputFileName, int outputWidth, int outputHeight)
{
    RGBImageUncompressed img;
    img.init(outputWidth, outputHeight);
    img.createTestPattern();

    RGBPixel bordercolor((char)0xFF, (char)0xFF, 0);
    Polygon2D pol;
    pol.addVertex(scaleX(35, outputWidth), scaleY(25, outputHeight), 1.0, 0.0, 0.0);
    pol.addVertex(scaleX(200, outputWidth), scaleY(100, outputHeight), 0.0, 1.0, 0.0);
    pol.addVertex(scaleX(50, outputWidth), scaleY(150, outputHeight), 0.0, 0.0, 1.0);
    pol.nextLoop();
    pol.addVertex(scaleX(60, outputWidth), scaleY(75, outputHeight), 1.0, 1.0, 0.0);
    pol.addVertex(scaleX(125, outputWidth), scaleY(75, outputHeight), 0.0, 1.0, 1.0);
    pol.addVertex(scaleX(115, outputWidth), scaleY(110, outputHeight), 1.0, 0.0, 1.0);

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
    int outputWidth = positiveIntOrDefault(argc, argv, 3, DEFAULT_OUTPUT_WIDTH);
    int outputHeight = positiveIntOrDefault(argc, argv, 4, DEFAULT_OUTPUT_HEIGHT);

    if (mode == "line") return runLineTest(outputFileName, outputWidth, outputHeight);
    if (mode == "polygon") return runPolygonTest(outputFileName, outputWidth, outputHeight);
    if (mode == "smooth") return runSmoothPolygonTest(outputFileName, outputWidth, outputHeight);

    fprintf(stderr, "Unknown mode: %s\n", mode.c_str());
    fprintf(stderr, "Usage: Rasterizer2DExample [line|polygon|smooth] [output.ppm] [width] [height]\n");
    return 1;
}
