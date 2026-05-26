#include "ImageExporter.h"

#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "java/io/File.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"

#include <cctype>
#include <cstdio>
#include <cstring>

bool ImageExporter::exportImage(const java::String& outputFileName, RGBImageUncompressed* image)
{
    java::File outFile(outputFileName.c_str());
    printf("Exporting result image to file \"%s\": ", outputFileName.c_str());

    java::String lower = outputFileName;
    for (size_t i = 0; i < lower.size(); i++) {
        lower[i] = std::tolower((unsigned char)lower[i]);
    }

    bool ok = false;
    if ( lower.size() >= 4 && lower.substr(lower.size()-4) == ".png" ) {
        ok = ImagePersistence::exportPNG(outFile, image);
    }
    else if ( (lower.size() >= 4 && lower.substr(lower.size()-4) == ".jpg") ||
              (lower.size() >= 5 && lower.substr(lower.size()-5) == ".jpeg") ) {
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
