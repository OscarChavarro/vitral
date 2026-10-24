#include <stdio.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/io/File.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/processing/ImageProcessing.h"

const int OUTPUT_WIDTH = 320;
const int OUTPUT_HEIGHT = 240;
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
    String imageFilename = "../../../../etc/images/render.jpg";
    String outputFilename = "output.ppm";
    if (argc > 1 && argv[1] != 0 && String(argv[1]).size() > 0) {
        outputFilename = argv[1];
    }

    RGBImageUncompressed* img = 0;
    try {
        img = ImagePersistence::importRGB(File(imageFilename.c_str()));
    }
#ifndef VSDK_WITHOUT_EXCEPTIONS
    catch (...) {
        fprintf(stderr, "Error: could not read the image file \"%s\".\n", imageFilename.c_str());
        fprintf(stderr, "Check you have access to that file from current working directory.\n");
        return 1;
    }
#endif

    if (img == 0) {
        fprintf(stderr, "Error: image import returned null\n");
        return 1;
    }

    // Keep the generated offline image at the Turbo C target resolution.
    RGBImageUncompressed* resized = new RGBImageUncompressed();
    if (resized == 0 || !resized->init(OUTPUT_WIDTH, OUTPUT_HEIGHT)) {
        delete resized;
        delete img;
        fprintf(stderr, "Error: could not allocate the %dx%d output image.\n",
                OUTPUT_WIDTH, OUTPUT_HEIGHT);
        return 1;
    }
    ImageProcessing::resize(img, resized);
    delete img;
    img = resized;

    performImageOperation1(img);
    BOOLEAN ok = ImagePersistence::exportPPM(File(outputFilename.c_str()), img);
    delete img;

    if (!ok) {
        fprintf(stderr, "Error: could not write output file \"%s\".\n", outputFilename.c_str());
        return 1;
    }

    printf("Resulting image has been written to \"%s\"\n", outputFilename.c_str());
    return 0;
}
