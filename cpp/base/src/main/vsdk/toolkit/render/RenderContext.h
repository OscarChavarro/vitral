#ifndef __RENDERCONTEXT__
#define __RENDERCONTEXT__

class Shader;

class RenderContext {
public:
    bool localLightingEnabled;
    bool textureEnabled;
    bool bumpMappingEnabled;
    Shader* localShader;

    RenderContext(
        bool localLightingEnabled,
        bool textureEnabled,
        bool bumpMappingEnabled,
        Shader* localShader)
        : localLightingEnabled(localLightingEnabled),
          textureEnabled(textureEnabled),
          bumpMappingEnabled(bumpMappingEnabled),
          localShader(localShader)
    {
    }
};

#endif
