#ifndef __IMAGEEXPORTER__
#define __IMAGEEXPORTER__

#include "java/lang/String.h"

class RGBImageUncompressed;

class ImageExporter {
public:
    bool exportImage(const java::String& outputFileName, RGBImageUncompressed* image);
};

#endif
#include "java/lang/String.h"
