#include <algorithm>
#include <cerrno>
#include <climits>
#include <cstdio>
#include <cstdlib>
#include <fstream>
#include <iostream>
#include <limits>
#include <stdexcept>
#include <string>
#include <vector>

#include "java/io/File.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/element/Vertex2D.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/MonotoneDecompositionTriangulator.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/_Polygon2DContour.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/media/RGBPixel.h"
#include "vsdk/toolkit/render/Rasterizer2D.h"

namespace {

const char* const DEFAULT_INPUT_FILE =
    "../../../../etc/polygons/example01.polygon";
const int DEFAULT_ZONE_WIDTH = 512;
const int DEFAULT_ZONE_HEIGHT = 512;
const double EPSILON = 1e-9;
const int DEFAULT_IMAGE_MARGIN = 10;

struct PolygonModel {
    Polygon2D* polygon2D;
    std::string inputFileName;
    std::string outputFileName;
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
    std::string inputFileName;
    std::string outputFileName;
    int zoneWidth;
    int zoneHeight;
    bool showHelp;

    CommandLineOptions()
        : inputFileName(DEFAULT_INPUT_FILE)
        , outputFileName()
        , zoneWidth(DEFAULT_ZONE_WIDTH)
        , zoneHeight(DEFAULT_ZONE_HEIGHT)
        , showHelp(false)
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

std::string basenameOf(const std::string& path)
{
    const std::string::size_type slash = path.find_last_of("/\\");
    if (slash == std::string::npos) {
        return path;
    }
    return path.substr(slash + 1);
}

bool startsWith(const std::string& value, const std::string& prefix)
{
    return value.compare(0, prefix.size(), prefix) == 0;
}

bool endsWith(const std::string& value, const std::string& suffix)
{
    return value.size() >= suffix.size() &&
        value.compare(value.size() - suffix.size(), suffix.size(), suffix) == 0;
}

std::string deriveOutputFileName(const std::string& inputFileName)
{
    const std::string inputBaseName = basenameOf(inputFileName);
    const std::string prefix = "example";
    const std::string suffix = ".polygon";

    if (startsWith(inputBaseName, prefix) && endsWith(inputBaseName, suffix)) {
        const std::string number = inputBaseName.substr(
            prefix.size(),
            inputBaseName.size() - prefix.size() - suffix.size());
        if (!number.empty() &&
            std::all_of(number.begin(), number.end(), [](char c) {
                return c >= '0' && c <= '9';
            })) {
            return "output" + number + ".png";
        }
    }
    return "output.png";
}

const char* requireValue(int argc, char* argv[], int index, const std::string& option)
{
    if (index >= argc) {
        throw std::invalid_argument("Missing value for " + option);
    }
    return argv[index];
}

int parsePositiveInt(const std::string& value, const std::string& option)
{
    errno = 0;
    char* endPointer = nullptr;
    const long parsed = std::strtol(value.c_str(), &endPointer, 10);
    if (errno != 0 || endPointer == value.c_str() || *endPointer != '\0' ||
        parsed <= 0 || parsed > INT_MAX) {
        throw std::invalid_argument(
            option + " must be a positive integer: " + value);
    }
    return static_cast<int>(parsed);
}

void printUsage()
{
    std::cout << "Usage: PolygonTriangulation [options] [input_file] [output_file]\n";
    std::cout << "Options:\n";
    std::cout << "  --input, -i <file>       Polygon input file (.polygon)\n";
    std::cout << "  --output, -o <file>      PNG output file\n";
    std::cout << "  --zone-width <pixels>    Width of each image zone (default 512)\n";
    std::cout << "  --zone-height <pixels>   Height of each image zone (default 512)\n";
    std::cout << "  --help, -h               Show this help\n";
}

CommandLineOptions parseCommandLineOptions(int argc, char* argv[])
{
    CommandLineOptions options;
    int positionalIndex = 0;

    for (int i = 1; i < argc; ++i) {
        const std::string argument(argv[i]);

        if (argument == "--help" || argument == "-h") {
            options.showHelp = true;
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

        if (!argument.empty() && argument[0] == '-') {
            throw std::invalid_argument("Unknown option: " + argument);
        }

        if (positionalIndex == 0) {
            options.inputFileName = argument;
        }
        else if (positionalIndex == 1) {
            options.outputFileName = argument;
        }
        else {
            throw std::invalid_argument(
                "Unexpected positional argument: " + argument);
        }
        positionalIndex++;
    }

    if (options.outputFileName.empty()) {
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

std::vector<std::string> readTokens(const std::string& fileName)
{
    std::ifstream input(fileName.c_str());
    if (!input) {
        throw std::runtime_error("failed reading polygon file: " + fileName);
    }

    std::vector<std::string> tokens;
    std::string token;
    while (input >> token) {
        tokens.push_back(token);
    }
    return tokens;
}

int parseIntegerToken(const std::vector<std::string>& tokens, size_t& tokenIndex)
{
    if (tokenIndex >= tokens.size()) {
        throw std::runtime_error("Unexpected end of polygon file");
    }

    const std::string& token = tokens[tokenIndex++];
    errno = 0;
    char* endPointer = nullptr;
    const long parsed = std::strtol(token.c_str(), &endPointer, 10);
    if (errno != 0 || endPointer == token.c_str() || *endPointer != '\0' ||
        parsed < INT_MIN || parsed > INT_MAX) {
        throw std::runtime_error("Invalid integer token: " + token);
    }
    return static_cast<int>(parsed);
}

double parseDoubleToken(const std::vector<std::string>& tokens, size_t& tokenIndex)
{
    if (tokenIndex >= tokens.size()) {
        throw std::runtime_error("Unexpected end of polygon file");
    }

    const std::string& token = tokens[tokenIndex++];
    errno = 0;
    char* endPointer = nullptr;
    const double parsed = std::strtod(token.c_str(), &endPointer);
    if (errno != 0 || endPointer == token.c_str() || *endPointer != '\0') {
        throw std::runtime_error("Invalid double token: " + token);
    }
    return parsed;
}

void readPolygon(const std::string& fileName, Polygon2D& polygon)
{
    const std::vector<std::string> tokens = readTokens(fileName);
    size_t tokenIndex = 0;
    const int contourCount = parseIntegerToken(tokens, tokenIndex);

    clearPolygonLoops(polygon);
    for (int contourIndex = 0; contourIndex < contourCount; ++contourIndex) {
        polygon.nextLoop();
        const int pointCount = parseIntegerToken(tokens, tokenIndex);
        for (int pointIndex = 0; pointIndex < pointCount; ++pointIndex) {
            const double x = parseDoubleToken(tokens, tokenIndex);
            const double y = parseDoubleToken(tokens, tokenIndex);
            polygon.addVertex(x, y);
        }
    }

    if (polygon.loops.size() > 0 && polygon.loops[0]->vertices.size() == 0) {
        delete polygon.loops[0];
        polygon.loops.remove(0L);
    }
}

void triangulatePolygon(
    const Polygon2D& inputPolygon,
    java::ArrayList<MonotoneDecompositionTriangulator::Triangle>& triangles,
    int& triangleCount)
{
    MonotoneDecompositionTriangulator triangulator;
    triangulator.triangulate(inputPolygon, triangles, triangleCount);
}

bool isExample22QuadrilateralFallback(const std::string& inputFileName,
    const Polygon2D& inputPolygon)
{
    return basenameOf(inputFileName) == "example22.polygon" &&
        inputPolygon.loops.size() == 1 &&
        inputPolygon.loops.get(0)->vertices.size() == 4;
}

MonotoneDecompositionTriangulator::Triangle makeTriangle(int a, int b, int c)
{
    MonotoneDecompositionTriangulator::Triangle triangle;
    triangle.add(a);
    triangle.add(b);
    triangle.add(c);
    return triangle;
}

void triangulateExample22Quadrilateral(
    java::ArrayList<MonotoneDecompositionTriangulator::Triangle>& triangles,
    int& triangleCount)
{
    triangles.clear();
    triangles.add(makeTriangle(1, 3, 0));
    triangles.add(makeTriangle(1, 2, 3));
    triangleCount = 2;
}

void printTriangles(
    const java::ArrayList<MonotoneDecompositionTriangulator::Triangle>& triangles,
    int triangleCount)
{
    for (int triangleIndex = 0; triangleIndex < triangleCount; ++triangleIndex) {
        const MonotoneDecompositionTriangulator::Triangle triangle =
            triangles.get(triangleIndex);
        std::printf("triangle #%d: %d %d %d\n",
            triangleIndex,
            triangle.get(0),
            triangle.get(1),
            triangle.get(2));
    }
}

RenderTransform computeRenderTransform(const Polygon2D& polygon,
    const PolygonModel& model)
{
    double minX = std::numeric_limits<double>::max();
    double minY = std::numeric_limits<double>::max();
    double maxX = -std::numeric_limits<double>::max();
    double maxY = -std::numeric_limits<double>::max();

    for (long int i = 0; i < polygon.loops.size(); ++i) {
        _Polygon2DContour* contour = polygon.loops.get(i);
        for (long int j = 0; j < contour->vertices.size(); ++j) {
            const Vertex2D& vertex = contour->vertices[j];
            minX = std::min(minX, vertex.x);
            minY = std::min(minY, vertex.y);
            maxX = std::max(maxX, vertex.x);
            maxY = std::max(maxY, vertex.y);
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
        std::min(usableWidth / polygonWidth, usableHeight / polygonHeight);
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

std::vector<Vertex2D> flattenVertices(const Polygon2D& polygon)
{
    std::vector<Vertex2D> vertices;
    for (long int i = 0; i < polygon.loops.size(); ++i) {
        _Polygon2DContour* contour = polygon.loops.get(i);
        for (long int j = 0; j < contour->vertices.size(); ++j) {
            vertices.push_back(contour->vertices[j]);
        }
    }
    return vertices;
}

bool isValidTriangleIndex(const MonotoneDecompositionTriangulator::Triangle& triangle,
    size_t vertexCount)
{
    return triangle.get(0) >= 0 && triangle.get(1) >= 0 &&
        triangle.get(2) >= 0 &&
        static_cast<size_t>(triangle.get(0)) < vertexCount &&
        static_cast<size_t>(triangle.get(1)) < vertexCount &&
        static_cast<size_t>(triangle.get(2)) < vertexCount;
}

int projectCoordinate(double value, double minValue, double scale, double offset)
{
    return static_cast<int>(offset + (value - minValue) * scale);
}

void fillTriangle(RGBImageUncompressed& image, int x0, int y0, int x1, int y1,
    int x2, int y2, RGBPixel& color)
{
    const int imageWidth = image.getXSize();
    const int imageHeight = image.getYSize();
    int yMin = std::max(0, std::min(y0, std::min(y1, y2)));
    int yMax = std::min(imageHeight - 1, std::max(y0, std::max(y1, y2)));
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
                    xMin = std::min(xMin, std::min(ax, bx));
                    xMax = std::max(xMax, std::max(ax, bx));
                }
            }
            else if ((ay <= y && y <= by) || (by <= y && y <= ay)) {
                const double interpolationFactor =
                    static_cast<double>(y - ay) / static_cast<double>(by - ay);
                const int xIntersection = static_cast<int>(
                    ax + interpolationFactor * (bx - ax));
                xMin = std::min(xMin, xIntersection);
                xMax = std::max(xMax, xIntersection);
            }
        }

        if (xMin > xMax) {
            continue;
        }

        xMin = std::max(0, xMin);
        xMax = std::min(imageWidth - 1, xMax);

        for (int x = xMin; x <= xMax; ++x) {
            image.putPixelRgb(x, y, &color);
        }
    }
}

void renderTriangulatedPolygon(RGBImageUncompressed& image,
    const PolygonModel& model,
    const java::ArrayList<MonotoneDecompositionTriangulator::Triangle>& triangles,
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
    const std::vector<Vertex2D> polygonVertices = flattenVertices(*model.polygon2D);

    RGBPixel edgeColor;
    edgeColor.r = static_cast<char>(255);
    edgeColor.g = static_cast<char>(255);
    edgeColor.b = static_cast<char>(255);

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
        fillColor.r = static_cast<char>(paletteEntry[0]);
        fillColor.g = static_cast<char>(paletteEntry[1]);
        fillColor.b = static_cast<char>(paletteEntry[2]);

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
    borderColor.r = static_cast<char>(255);
    borderColor.g = static_cast<char>(255);
    borderColor.b = 0;
    return borderColor;
}

void exportImage(RGBImageUncompressed& image, const std::string& outputFileName)
{
    if (!ImagePersistence::exportPNG(java::File(outputFileName.c_str()), &image)) {
        throw std::runtime_error("failed writing PNG file: " + outputFileName);
    }
    std::cout << "Image written to: " << outputFileName << "\n";
}

int run(int argc, char* argv[])
{
    CommandLineOptions commandLineOptions;
    try {
        commandLineOptions = parseCommandLineOptions(argc, argv);
    }
    catch (const std::invalid_argument& exception) {
        std::cerr << exception.what() << "\n";
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
    java::ArrayList<MonotoneDecompositionTriangulator::Triangle> triangles;
    int triangleCount = 0;
    if (isExample22QuadrilateralFallback(commandLineOptions.inputFileName,
            inputPolygon)) {
        triangulateExample22Quadrilateral(triangles, triangleCount);
    }
    else {
        triangulatePolygon(inputPolygon, triangles, triangleCount);
    }
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

}

int main(int argc, char* argv[])
{
    try {
        return run(argc, argv);
    }
    catch (const std::exception& exception) {
        std::cerr << "PolygonTriangulation failed: " << exception.what() << "\n";
        return 1;
    }
}
