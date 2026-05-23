#include "ImageExporter.h"

#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "java/io/File.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"

#include <algorithm>
#include <cctype>
#include <iostream>

bool ImageExporter::exportImage(const std::string& outputFileName, RGBImageUncompressed* image)
{
    java::File outFile(outputFileName.c_str());
    std::cout << "Exporting result image to file \"" << outputFileName << "\": ";

    std::string lower = outputFileName;
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
        std::cout << " OK!\n";
    }
    return ok;
}
