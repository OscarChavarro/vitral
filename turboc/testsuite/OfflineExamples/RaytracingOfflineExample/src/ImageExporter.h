#ifndef __IMAGE_EXPORTER__
#define __IMAGE_EXPORTER__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"

class RGBImageUncompressed;

class ImageExporter {
public:
    BOOLEAN exportImage(const String& outputFileName, RGBImageUncompressed* image);
};

#endif
#include "java/lang/String.h"
