#include <ctype.h>
#include <stdio.h>
#include <string.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/io/File.h"
#include "ImageExporter.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
BOOLEAN ImageExporter::exportImage(const String& outputFileName, RGBImageUncompressed* image)
{
    File outFile(outputFileName.c_str());
    printf("Exporting result image to file \"%s\": ", outputFileName.c_str());

    String lower = outputFileName;
    for (int i = 0; i < lower.size(); i++) {
        lower[i] = tolower((unsigned char)lower[i]);
    }

    // C++ port note: the extensions are taken into locals, as gcc 2.7.2
    // crashes on String temporaries inside && / || operands
    BOOLEAN isPng = FALSE;
    BOOLEAN isJpeg = FALSE;
    if ( lower.size() >= 4 ) {
        String extension = lower.substr(lower.size()-4);
        isPng = extension == ".png";
        isJpeg = extension == ".jpg";
    }
    if ( lower.size() >= 5 ) {
        String extension = lower.substr(lower.size()-5);
        if ( extension == ".jpeg" ) {
            isJpeg = TRUE;
        }
    }

    BOOLEAN ok = FALSE;
    if ( isPng ) {
        ok = ImagePersistence::exportPNG(outFile, image);
    }
    else if ( isJpeg ) {
        ok = ImagePersistence::exportJPEG(outFile, image);
    }
    else {
        ok = ImagePersistence::exportPPM(outFile, image);
    }

    if ( ok ) {
        printf(" OK!\n");
    }
    return ok;
}
