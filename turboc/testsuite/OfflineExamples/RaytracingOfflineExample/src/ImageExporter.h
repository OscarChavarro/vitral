#ifndef __IMAGE_EXPORTER__
#define __IMAGE_EXPORTER__

#include "java/lang/String.h"

class RGBImageUncompressed;

class ImageExporter {
public:
    bool exportImage(const java::String& outputFileName, RGBImageUncompressed* image);
};

#endif
#include "java/lang/String.h"
