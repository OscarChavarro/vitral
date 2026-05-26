#include "ImageExporter.h"

#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "java/io/File.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"

#include <algorithm>
#include <cctype>
#include <cstdio>

bool ImageExporter::exportImage(const java::String& outputFileName, RGBImageUncompressed* image)
{
    java::File outFile(outputFileName.c_str());
    printf("Exporting result image to file \"%s\": ", outputFileName.c_str());

    java::String lower = outputFileName;
    std::transform(lower.begin(), lower.end(), lower.begin(), [](unsigned char c){ return (char)std::tolower(c); });

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
