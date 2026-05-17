#ifndef __VSDK_TOOLKIT_RENDER_SHADERS_SHADERSELECTOR_H__
#define __VSDK_TOOLKIT_RENDER_SHADERS_SHADERSELECTOR_H__

class RendererConfiguration;
class Shader;

class ShaderSelector {
public:
    static Shader* select(const RendererConfiguration* qualitySelection);
};

#endif
