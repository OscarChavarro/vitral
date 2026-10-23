#ifndef __LIGHTING_SHADER__
#define __LIGHTING_SHADER__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/render/shaders/Shader.h"
class LightingShader : public Shader {
private:
    BOOLEAN specularEnabled;
    BOOLEAN textureEnabled;
    BOOLEAN bumpMapEnabled;

public:
    LightingShader(BOOLEAN specularEnabled, BOOLEAN textureEnabled, BOOLEAN bumpMapEnabled);
    virtual ~LightingShader() {}

    virtual LocalShadingResult shadeLocal(
        RayHit* info,
        double viewX,
        double viewY,
        double viewZ,
        ArrayList<Light*>& lights,
        ArrayList<SimpleBody*>& objects,
        SimpleMaterial* material,
        TraceWorkspace* workspace);
};

#endif
