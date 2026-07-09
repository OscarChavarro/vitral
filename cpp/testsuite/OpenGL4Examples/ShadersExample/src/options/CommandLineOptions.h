#ifndef __COMMAND_LINE_OPTIONS__
#define __COMMAND_LINE_OPTIONS__

#include "java/lang/String.h"
#include "../model/ShaderOperationMode.h"
class CommandLineOptions {
public:
    enum class TextureFilterOption {
        LINEAR,
        NEAREST
    };

    bool offline;
    java::String offlineOutputPath;
    ShaderOperationMode method;
    bool hasRotation;
    double rotationDegrees;
    bool hasLightRotation;
    double lightRotationDegrees;
    bool hasWithTexture;
    bool withTexture;
    bool hasWithBumpMap;
    bool withBumpMap;
    bool hasShadingType;
    int shadingType;
    bool hasTextureFilter;
    TextureFilterOption textureFilter;
    bool hasMeridians;
    int meridians;
    bool hasParallels;
    int parallels;
    bool hasCpuTextureOffsetU;
    double cpuTextureOffsetUTexels;
    bool hasCpuTextureOffsetV;
    double cpuTextureOffsetVTexels;
    bool showHud;
    int width;
    int height;

    CommandLineOptions();
    static CommandLineOptions parse(int argc, char** argv);
};

#endif
