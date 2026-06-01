#include <cstdio>
#include <cstdlib>
#include <cerrno>
#include <climits>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polygonTriangulation/MonotoneDecompositionTriangulator.h"

static bool
readInteger(FILE* inputFile, int& value)
{
    char token[128];
    if (std::fscanf(inputFile, "%127s", token) != 1) {
        return false;
    }

    errno = 0;
    char* endPointer = nullptr;
    const long parsed = std::strtol(token, &endPointer, 10);
    if (errno != 0 || endPointer == token || *endPointer != '\0' ||
        parsed < INT_MIN || parsed > INT_MAX) {
        return false;
    }

    value = static_cast<int>(parsed);
    return true;
}

static bool
readDouble(FILE* inputFile, double& value)
{
    char token[128];
    if (std::fscanf(inputFile, "%127s", token) != 1) {
        return false;
    }

    errno = 0;
    char* endPointer = nullptr;
    const double parsed = std::strtod(token, &endPointer);
    if (errno != 0 || endPointer == token || *endPointer != '\0') {
        return false;
    }

    value = parsed;
    return true;
}

static bool
readDataFile(const char* filename, Polygon2D& input)
{
    FILE* inputFile = std::fopen(filename, "r");
    if (!inputFile) {
        std::perror(filename);
        return false;
    }

    int numberOfContours = 0;
    if ( !readInteger(inputFile, numberOfContours) ||
        numberOfContours <= 0 || numberOfContours > SEGMENT_SIZE) {
        std::fclose(inputFile);
        return false;
    }

    for ( int c = 0; c < numberOfContours; ++c) {
        int numberOfPoints = 0;
        if ( !readInteger(inputFile, numberOfPoints) || numberOfPoints <= 0) {
            std::fclose(inputFile);
            return false;
        }
        if ( numberOfPoints > SEGMENT_SIZE) {
            std::fclose(inputFile);
            return false;
        }

        if (c > 0) {
            input.nextLoop();
        }
        for ( int i = 0; i < numberOfPoints; ++i) {
            double x = 0.0;
            double y = 0.0;
            if (!readDouble(inputFile, x) || !readDouble(inputFile, y)) {
                std::fclose(inputFile);
                return false;
            }
            input.addVertex(x, y);
        }
    }

    std::fclose(inputFile);
    return true;
}

int
main(int argc, char* argv[])
{
    if (argc < 2) {
        std::fprintf(stderr, "usage: triangulate <filename>\n");
        return 1;
    }

    Polygon2D input;
    if (!readDataFile(argv[1], input)) {
        std::fprintf(stderr, "failed reading polygon file: %s\n", argv[1]);
        return 1;
    }

    MonotoneDecompositionTriangulator pipeline;
    java::ArrayList<MonotoneDecompositionTriangulator::Triangle> triangles;
    int triangleCount = 0;
    pipeline.triangulate(input, triangles, triangleCount);

    for (int i = 0; i < triangleCount; ++i) {
        const MonotoneDecompositionTriangulator::Triangle triangle = triangles.get(i);
        std::printf("triangle #%d: %d %d %d\n",
                    i,
                    triangle.get(0),
                    triangle.get(1),
                    triangle.get(2));
    }

    return 0;
}
