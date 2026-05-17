#ifndef RAYTRACING_OFFLINE_IMAGEEXPORTER_H
#define RAYTRACING_OFFLINE_IMAGEEXPORTER_H

#include <string>

class RGBImageUncompressed;

class ImageExporter {
public:
    bool exportImage(const std::string& outputFileName, RGBImageUncompressed* image);
};

#endif
