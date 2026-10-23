#include <float.h>
#include <errno.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/io/File.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/VSDKFatalException.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/RGBPixel.h"
#include "vsdk/toolkit/environment/geometry/element/Vertex2D.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/_Polygon2DContour.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/MonotoneDecompositionTriangulator.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/render/raster/Rasterizer2D.h"
#include "java/lang/Exception.h"
const char* const DEFAULT_INPUT_FILE =
    "../../../../etc/polygons/example01.polygon";
const int DEFAULT_ZONE_WIDTH = 512;
const int DEFAULT_ZONE_HEIGHT = 512;
const double EPSILON = 1e-9;
const int DEFAULT_IMAGE_MARGIN = 10;

static void fail(const char* method, const String& message)
{
    Logger::reportMessage("PolygonTriangulation", Logger::ERROR, method, message);
    throw VSDKFatalException(message);
}

struct PolygonModel {
    Polygon2D* polygon2D;
    String inputFileName;
    String outputFileName;
    int zoneWidth;
    int zoneHeight;

    int getImageWidth() const { return zoneWidth * 2; }
    int getImageHeight() const { return zoneHeight; }
};

struct RenderTransform {
    double minX;
    double minY;
    double scale;
    double offsetX;
    double offsetY;
};

struct CommandLineOptions {
    String inputFileName;
    String outputFileName;
    int zoneWidth;
    int zoneHeight;
    BOOLEAN showHelp;

    CommandLineOptions()
        : inputFileName(DEFAULT_INPUT_FILE)
        , outputFileName()
        , zoneWidth(DEFAULT_ZONE_WIDTH)
        , zoneHeight(DEFAULT_ZONE_HEIGHT)
        , showHelp(FALSE)
    {
    }

    PolygonModel toPolygonModel(Polygon2D& polygon2D) const
    {
        PolygonModel model;
        model.polygon2D = &polygon2D;
        model.inputFileName = inputFileName;
        model.outputFileName = outputFileName;
        model.zoneWidth = zoneWidth;
        model.zoneHeight = zoneHeight;
        return model;
    }
};

String basenameOf(const String& path)
{
    const int slash = path.find_last_of("/\\");
    if (slash == String::npos) {
        return path;
    }
    return path.substr(slash + 1);
}

BOOLEAN startsWith(const String& value, const String& prefix)
{
    return value.startsWith(prefix.c_str());
}

BOOLEAN endsWith(const String& value, const String& suffix)
{
    return value.size() >= suffix.size() &&
        value.substr(value.size() - suffix.size()) == suffix;
}

String deriveOutputFileName(const String& inputFileName)
{
    const String inputBaseName = basenameOf(inputFileName);
    const String prefix = "example";
    const String suffix = ".polygon";

    if (startsWith(inputBaseName, prefix) && endsWith(inputBaseName, suffix)) {
        const String number = inputBaseName.substr(
            prefix.size(),
            inputBaseName.size() - prefix.size() - suffix.size());
        BOOLEAN allDigits = !number.isEmpty();
        for (int i = 0; allDigits && i < number.size(); ++i) {
            const char c = number[i];
            if (c < '0' || c > '9') {
                allDigits = FALSE;
            }
        }
        if (allDigits) {
            return String("output").concat(number).concat(".png");
        }
    }
    return String("output.png");
}

template <class T>
static const T& minValue(const T& a, const T& b)
{
    return (b < a) ? b : a;
}

template <class T>
static const T& maxValue(const T& a, const T& b)
{
    return (a < b) ? b : a;
}

String requireValue(int argc, char* argv[], int index, const String& option)
{
    if (index >= argc) {
        fail("requireValue", String("Missing value for ").concat(option));
    }
    return String(argv[index]);
}

int parsePositiveInt(const String& value, const String& option)
{
    errno = 0;
    char* endPointer = 0;
    const long parsed = strtol(value.c_str(), &endPointer, 10);
    if (errno != 0 || endPointer == value.c_str() || *endPointer != '\0' ||
        parsed <= 0 || parsed > INT_MAX) {
        fail("parsePositiveInt",
            String(option).concat(" must be a positive integer: ").concat(value));
    }
    return ((int)(parsed));
}

void printUsage()
{
    printf("Usage: PolygonTriangulation [options] [input_file] [output_file]\n");
    printf("Options:\n");
    printf("  --input, -i <file>       Polygon input file (.polygon)\n");
    printf("  --output, -o <file>      PNG output file\n");
    printf("  --zone-width <pixels>    Width of each image zone (default 512)\n");
    printf("  --zone-height <pixels>   Height of each image zone (default 512)\n");
    printf("  --help, -h               Show this help\n");
}

CommandLineOptions parseCommandLineOptions(int argc, char* argv[])
{
    CommandLineOptions options;
    int positionalIndex = 0;

    for (int i = 1; i < argc; ++i) {
        const String argument(argv[i]);

        if (argument == "--help" || argument == "-h") {
            options.showHelp = TRUE;
            continue;
        }

        if (argument == "--input" || argument == "-i") {
            options.inputFileName = requireValue(argc, argv, ++i, argument);
            continue;
        }

        if (argument == "--output" || argument == "-o") {
            options.outputFileName = requireValue(argc, argv, ++i, argument);
            continue;
        }

        if (argument == "--zone-width") {
            options.zoneWidth = parsePositiveInt(
                requireValue(argc, argv, ++i, argument), argument);
            continue;
        }

        if (argument == "--zone-height") {
            options.zoneHeight = parsePositiveInt(
                requireValue(argc, argv, ++i, argument), argument);
            continue;
        }

        if (!argument.isEmpty() && argument[0] == '-') {
            fail("parseCommandLineOptions", String("Unknown option: ").concat(argument));
        }

        if (positionalIndex == 0) {
            options.inputFileName = argument;
        }
        else if (positionalIndex == 1) {
            options.outputFileName = argument;
        }
        else {
            fail("parseCommandLineOptions",
                String("Unexpected positional argument: ").concat(argument));
        }
        positionalIndex++;
    }

    if (options.outputFileName.isEmpty()) {
        options.outputFileName = deriveOutputFileName(options.inputFileName);
    }

    return options;
}

void clearPolygonLoops(Polygon2D& polygon)
{
    for (long int i = 0; i < polygon.loops.size(); ++i) {
        delete polygon.loops[i];
    }
    polygon.loops.clear();
}

static void failReadPolygon(FILE* input, const String& fileName, const char* reason)
{
    if (input != 0) {
        fclose(input);
    }
    fail("failReadPolygon", String(reason).concat(fileName));
}

void readPolygon(const String& fileName, Polygon2D& polygon)
{
    FILE* input = fopen(fileName.c_str(), "r");
    if (!input) {
        fail("readPolygon", String("failed reading polygon file: ").concat(fileName));
    }

    int contourCount = 0;
    if (fscanf(input, "%d", &contourCount) != 1) {
        failReadPolygon(input, fileName, "Invalid or missing contour count in polygon file: ");
    }

    clearPolygonLoops(polygon);
    for (int contourIndex = 0; contourIndex < contourCount; ++contourIndex) {
        polygon.nextLoop();
        int pointCount = 0;
        if (fscanf(input, "%d", &pointCount) != 1) {
            failReadPolygon(input, fileName, "Invalid or missing point count in polygon file: ");
        }
        for (int pointIndex = 0; pointIndex < pointCount; ++pointIndex) {
            double x = 0.0;
            double y = 0.0;
            if (fscanf(input, "%lf %lf", &x, &y) != 2) {
                failReadPolygon(input, fileName, "Invalid or missing coordinate data in polygon file: ");
            }
            polygon.addVertex(x, y);
        }
    }

    fclose(input);

    if (polygon.loops.size() > 0 && polygon.loops[0]->vertices.size() == 0) {
        delete polygon.loops[0];
        polygon.loops.remove(0L);
    }
}

void triangulatePolygon(
    const Polygon2D& inputPolygon,
    ArrayList<MonotoneDecompositionTriangulator::Triangle>& triangles,
    int& triangleCount)
{
    MonotoneDecompositionTriangulator triangulator;
    triangulator.triangulate(inputPolygon, triangles, triangleCount);
}

void printTriangles(
    const ArrayList<MonotoneDecompositionTriangulator::Triangle>& triangles,
    int triangleCount)
{
    for (int triangleIndex = 0; triangleIndex < triangleCount; ++triangleIndex) {
        const MonotoneDecompositionTriangulator::Triangle triangle =
            triangles.get(triangleIndex);
        printf("triangle #%d: %d %d %d\n",
            triangleIndex,
            triangle.get(0),
            triangle.get(1),
            triangle.get(2));
    }
}

RenderTransform computeRenderTransform(const Polygon2D& polygon,
    const PolygonModel& model)
{
    double minX = DBL_MAX;
    double minY = DBL_MAX;
    double maxX = -DBL_MAX;
    double maxY = -DBL_MAX;

    for (long int i = 0; i < polygon.loops.size(); ++i) {
        _Polygon2DContour* contour = polygon.loops.get(i);
        for (long int j = 0; j < contour->vertices.size(); ++j) {
            const Vertex2D& vertex = contour->vertices[j];
            minX = minValue(minX, vertex.x);
            minY = minValue(minY, vertex.y);
            maxX = maxValue(maxX, vertex.x);
            maxY = maxValue(maxY, vertex.y);
        }
    }

    const double polygonWidth = maxX - minX;
    const double polygonHeight = maxY - minY;
    const double usableWidth =
        model.zoneWidth - 1.0 - 2.0 * DEFAULT_IMAGE_MARGIN;
    const double usableHeight =
        model.zoneHeight - 1.0 - 2.0 * DEFAULT_IMAGE_MARGIN;
    const double scale =
        (polygonWidth < EPSILON || polygonHeight < EPSILON) ? 1.0 :
        minValue(usableWidth / polygonWidth, usableHeight / polygonHeight);
    const double scaledWidth = polygonWidth * scale;
    const double scaledHeight = polygonHeight * scale;

    RenderTransform transform;
    transform.minX = minX;
    transform.minY = minY;
    transform.scale = scale;
    transform.offsetX = DEFAULT_IMAGE_MARGIN + (usableWidth - scaledWidth) / 2.0;
    transform.offsetY = DEFAULT_IMAGE_MARGIN + (usableHeight - scaledHeight) / 2.0;
    return transform;
}

void projectPolygon(const Polygon2D& sourcePolygon, double minX, double minY,
    double scale, double offsetX, double offsetY, Polygon2D& projectedPolygon)
{
    clearPolygonLoops(projectedPolygon);

    for (long int i = 0; i < sourcePolygon.loops.size(); ++i) {
        _Polygon2DContour* contour = sourcePolygon.loops.get(i);
        projectedPolygon.nextLoop();
        for (long int j = 0; j < contour->vertices.size(); ++j) {
            const Vertex2D& vertex = contour->vertices[j];
            const double projectedX = offsetX + (vertex.x - minX) * scale;
            const double projectedY = offsetY + (vertex.y - minY) * scale;
            projectedPolygon.addVertex(projectedX, projectedY, 0.2, 0.6, 1.0);
        }
    }
}

void renderSmoothFilledPolygon(RGBImageUncompressed& image,
    const PolygonModel& model, double minX, double minY, double scale,
    double offsetX, double offsetY, const RGBPixel& borderColor)
{
    Polygon2D projectedPolygon;
    projectPolygon(*model.polygon2D, minX, minY, scale, offsetX, offsetY,
        projectedPolygon);

    try {
        Rasterizer2D::fillSmoothPolygon(&image, projectedPolygon);
    }
    catch (...) {
    }

    Rasterizer2D::drawPolygon(&image, projectedPolygon, borderColor);
}

void renderPolygonBorder(RGBImageUncompressed& image, const PolygonModel& model,
    double minX, double minY, double scale, double offsetX, double offsetY,
    const RGBPixel& borderColor)
{
    Polygon2D projectedPolygon;
    projectPolygon(*model.polygon2D, minX, minY, scale, offsetX, offsetY,
        projectedPolygon);
    Rasterizer2D::drawPolygon(&image, projectedPolygon, borderColor);
}

ArrayList<Vertex2D> flattenVertices(const Polygon2D& polygon)
{
    ArrayList<Vertex2D> vertices;
    for (long int i = 0; i < polygon.loops.size(); ++i) {
        _Polygon2DContour* contour = polygon.loops.get(i);
        for (long int j = 0; j < contour->vertices.size(); ++j) {
            vertices.add(contour->vertices[j]);
        }
    }
    return vertices;
}

BOOLEAN isValidTriangleIndex(const MonotoneDecompositionTriangulator::Triangle& triangle,
    size_t vertexCount)
{
    return triangle.get(0) >= 0 && triangle.get(1) >= 0 &&
        triangle.get(2) >= 0 &&
        ((size_t)(triangle.get(0))) < vertexCount &&
        ((size_t)(triangle.get(1))) < vertexCount &&
        ((size_t)(triangle.get(2))) < vertexCount;
}

int projectCoordinate(double value, double minValue, double scale, double offset)
{
    return ((int)(offset + (value - minValue) * scale));
}

void fillTriangle(RGBImageUncompressed& image, int x0, int y0, int x1, int y1,
    int x2, int y2, RGBPixel& color)
{
    const int imageWidth = image.getXSize();
    const int imageHeight = image.getYSize();
    int yMin = minValue(y0, minValue(y1, y2));
    yMin = maxValue(0, yMin);
    int yMax = maxValue(y0, maxValue(y1, y2));
    yMax = minValue(imageHeight - 1, yMax);
    const int vertices[3][2] = {{x0, y0}, {x1, y1}, {x2, y2}};

    for (int y = yMin; y <= yMax; ++y) {
        int xMin = INT_MAX;
        int xMax = INT_MIN;

        for (int edgeIndex = 0; edgeIndex < 3; ++edgeIndex) {
            const int ax = vertices[edgeIndex][0];
            const int ay = vertices[edgeIndex][1];
            const int bx = vertices[(edgeIndex + 1) % 3][0];
            const int by = vertices[(edgeIndex + 1) % 3][1];

            if (ay == by) {
                if (ay == y) {
                    xMin = minValue(xMin, minValue(ax, bx));
                    xMax = maxValue(xMax, maxValue(ax, bx));
                }
            }
            else if ((ay <= y && y <= by) || (by <= y && y <= ay)) {
                const double interpolationFactor =
                    ((double)(y - ay)) / ((double)(by - ay));
                const int xIntersection = ((int)(
                    ax + interpolationFactor * (bx - ax)));
                xMin = minValue(xMin, xIntersection);
                xMax = maxValue(xMax, xIntersection);
            }
        }

        if (xMin > xMax) {
            continue;
        }

        xMin = maxValue(0, xMin);
        xMax = minValue(imageWidth - 1, xMax);

        for (int x = xMin; x <= xMax; ++x) {
            image.putPixelRgb(x, y, &color);
        }
    }
}

void renderTriangulatedPolygon(RGBImageUncompressed& image,
    const PolygonModel& model,
    const ArrayList<MonotoneDecompositionTriangulator::Triangle>& triangles,
    int triangleCount, double minX, double minY, double scale, double offsetX,
    double offsetY, const RGBPixel& borderColor)
{
    const int palette[][3] = {
        {210, 90, 90},
        {90, 200, 90},
        {90, 90, 220},
        {210, 200, 80},
        {80, 200, 200},
        {200, 80, 200},
        {200, 140, 60},
        {140, 80, 200},
    };
    const ArrayList<Vertex2D> polygonVertices = flattenVertices(*model.polygon2D);

    RGBPixel edgeColor;
    edgeColor.r = ((char)(255));
    edgeColor.g = ((char)(255));
    edgeColor.b = ((char)(255));

    RGBPixel fillColor;

    for (int i = 0; i < triangleCount; ++i) {
        const MonotoneDecompositionTriangulator::Triangle triangle = triangles.get(i);
        if (!isValidTriangleIndex(triangle, polygonVertices.size())) {
            continue;
        }

        const Vertex2D& vertexA = polygonVertices[triangle.get(0)];
        const Vertex2D& vertexB = polygonVertices[triangle.get(1)];
        const Vertex2D& vertexC = polygonVertices[triangle.get(2)];

        const int ax = projectCoordinate(vertexA.x, minX, scale, offsetX);
        const int ay = projectCoordinate(vertexA.y, minY, scale, offsetY);
        const int bx = projectCoordinate(vertexB.x, minX, scale, offsetX);
        const int by = projectCoordinate(vertexB.y, minY, scale, offsetY);
        const int cx = projectCoordinate(vertexC.x, minX, scale, offsetX);
        const int cy = projectCoordinate(vertexC.y, minY, scale, offsetY);

        const int* paletteEntry = palette[i % (sizeof(palette) / sizeof(palette[0]))];
        fillColor.r = ((char)(paletteEntry[0]));
        fillColor.g = ((char)(paletteEntry[1]));
        fillColor.b = ((char)(paletteEntry[2]));

        fillTriangle(image, ax, ay, bx, by, cx, cy, fillColor);
        Rasterizer2D::drawLine(&image, ax, ay, bx, by, edgeColor);
        Rasterizer2D::drawLine(&image, bx, by, cx, cy, edgeColor);
        Rasterizer2D::drawLine(&image, cx, cy, ax, ay, edgeColor);
    }

    renderPolygonBorder(image, model, minX, minY, scale, offsetX, offsetY,
        borderColor);
}

void createWorkingImage(const PolygonModel& model, RGBImageUncompressed& image)
{
    image.init(model.getImageWidth(), model.getImageHeight());
    image.createTestPattern();
}

RGBPixel createBorderColor()
{
    RGBPixel borderColor;
    borderColor.r = ((char)(255));
    borderColor.g = ((char)(255));
    borderColor.b = 0;
    return borderColor;
}

void exportImage(RGBImageUncompressed& image, const String& outputFileName)
{
    if (!ImagePersistence::exportPNG(File(outputFileName.c_str()), &image)) {
        fail("exportImage", String("failed writing PNG file: ").concat(outputFileName));
    }
    printf("Image written to: %s\n", outputFileName.toCString());
}

int run(int argc, char* argv[])
{
    CommandLineOptions commandLineOptions;
    try {
        commandLineOptions = parseCommandLineOptions(argc, argv);
    }
    catch (const Exception& exception) {
        Logger::reportMessageWithException("PolygonTriangulation", Logger::ERROR, "run", "invalid command line argument", &exception);
        printUsage();
        return 0;
    }

    if (commandLineOptions.showHelp) {
        printUsage();
        return 0;
    }

    Polygon2D inputPolygon;
    readPolygon(commandLineOptions.inputFileName, inputPolygon);

    PolygonModel model = commandLineOptions.toPolygonModel(inputPolygon);
    ArrayList<MonotoneDecompositionTriangulator::Triangle> triangles;
    int triangleCount = 0;
    triangulatePolygon(inputPolygon, triangles, triangleCount);
    printTriangles(triangles, triangleCount);

    RGBImageUncompressed image;
    createWorkingImage(model, image);
    const RenderTransform renderTransform =
        computeRenderTransform(inputPolygon, model);
    const RGBPixel borderColor = createBorderColor();

    renderSmoothFilledPolygon(image, model, renderTransform.minX,
        renderTransform.minY, renderTransform.scale, renderTransform.offsetX,
        renderTransform.offsetY, borderColor);
    renderTriangulatedPolygon(image, model, triangles, triangleCount,
        renderTransform.minX, renderTransform.minY, renderTransform.scale,
        renderTransform.offsetX + model.zoneWidth, renderTransform.offsetY,
        borderColor);
    exportImage(image, model.outputFileName);

    return 0;
}

int main(int argc, char* argv[])
{
    try {
        return run(argc, argv);
    }
    catch (const Exception& exception) {
        Logger::reportMessageWithException("PolygonTriangulation", Logger::ERROR, "main", "PolygonTriangulation failed", &exception);
        return 1;
    }
}
