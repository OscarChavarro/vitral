#include "java/lang/String.h"
#ifndef RAYTRACING_OFFLINE_IMAGEEXPORTER_H
#define RAYTRACING_OFFLINE_IMAGEEXPORTER_H

#include "java/lang/String.h"

class RGBImageUncompressed;

class ImageExporter {
public:
    bool exportImage(const java::String& outputFileName, RGBImageUncompressed* image);
};

#endif
