#ifndef __VSDK_TOOLKIT_RENDER_SHADERS_LIGHTINGSHADER_H__
#define __VSDK_TOOLKIT_RENDER_SHADERS_LIGHTINGSHADER_H__

#include "vsdk/toolkit/render/shaders/Shader.h"

class LightingShader : public Shader {
private:
    bool specularEnabled;
    bool textureEnabled;
    bool bumpMapEnabled;

public:
    LightingShader(bool specularEnabled, bool textureEnabled, bool bumpMapEnabled);
    virtual ~LightingShader() {}

    virtual LocalShadingResult shadeLocal(
        RayHit* info,
        double viewX,
        double viewY,
        double viewZ,
        const std::vector<Light*>& lights,
        const std::vector<SimpleBody*>& objects,
        SimpleMaterial* material,
        TraceWorkspace* workspace) override;
};

#endif
