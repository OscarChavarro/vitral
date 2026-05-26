#include <cstdio>
#include "java/lang/String.h"

#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "java/io/File.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"

static void performImageOperation1(RGBImageUncompressed* img)
{
    if (img == 0) return;
    const int xSize = img->getXSize();
    const int ySize = img->getYSize();
    const char r = (char)0xFF;
    const char g = 0;
    const char b = 0;

    for (int y = 0; y < ySize / 2; y++) {
        for (int x = 0; x < xSize / 2; x++) {
            img->putPixel(x, y, r, g, b);
        }
    }
}

int main(int argc, char** argv)
{
    java::String imageFilename = "../../../../etc/images/render.jpg";
    java::String outputFilename = "output.png";
    if (argc > 1 && argv[1] != 0 && java::String(argv[1]).size() > 0) {
        outputFilename = argv[1];
    }

    RGBImageUncompressed* img = 0;
    try {
        img = ImagePersistence::importRGB(java::File(imageFilename.c_str()));
    }
    catch (...) {
        std::fprintf(stderr, "Error: could not read the image file \"%s\".\n", imageFilename.c_str());
        std::fprintf(stderr, "Check you have access to that file from current working directory.\n");
        return 1;
    }

    if (img == 0) {
        std::fprintf(stderr, "Error: image import returned null\n");
        return 1;
    }

    performImageOperation1(img);
    bool ok = ImagePersistence::exportPNG(java::File(outputFilename.c_str()), img);
    delete img;

    if (!ok) {
        std::fprintf(stderr, "Error: could not write output file \"%s\".\n", outputFilename.c_str());
        return 1;
    }

    std::printf("Resulting image has been written to \"%s\"\n", outputFilename.c_str());
    return 0;
}
