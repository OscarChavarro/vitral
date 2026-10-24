#include <stdio.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/io/File.h"
#include "ImageExporter.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
BOOLEAN ImageExporter::exportImage(const String& outputFileName, RGBImageUncompressed* image)
{
    File outFile(outputFileName.c_str());
    printf("Exporting result image to file \"%s\": ", outputFileName.c_str());

    // Turbo C++ DOS examples intentionally use the built-in PPM writer;
    // PNG/JPEG need optional libraries unavailable in the guest.
    BOOLEAN ok = ImagePersistence::exportPPM(outFile, image);

    if ( ok ) {
        printf(" OK!\n");
    }
    return ok;
}
