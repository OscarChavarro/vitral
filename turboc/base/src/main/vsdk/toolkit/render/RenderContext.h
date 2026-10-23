#ifndef __RENDER_CONTEXT__
#define __RENDER_CONTEXT__

#include "vsdk/toolkit/common/VSDK.h"

class Shader;

class RenderContext {
public:
    BOOLEAN localLightingEnabled;
    BOOLEAN textureEnabled;
    BOOLEAN bumpMappingEnabled;
    Shader* localShader;

    RenderContext(
        BOOLEAN localLightingEnabled,
        BOOLEAN textureEnabled,
        BOOLEAN bumpMappingEnabled,
        Shader* localShader)
        : localLightingEnabled(localLightingEnabled),
          textureEnabled(textureEnabled),
          bumpMappingEnabled(bumpMappingEnabled),
          localShader(localShader)
    {
    }
};

#endif
