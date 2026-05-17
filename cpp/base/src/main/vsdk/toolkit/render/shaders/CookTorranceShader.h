#ifndef __VSDK_TOOLKIT_RENDER_SHADERS_COOKTORRANCESHADER_H__
#define __VSDK_TOOLKIT_RENDER_SHADERS_COOKTORRANCESHADER_H__

#include "Shader.h"

class CookTorranceShader : public Shader {
private:
    bool textureEnabled;
    bool bumpMapEnabled;
public:
    CookTorranceShader(bool textureEnabled, bool bumpMapEnabled);
    virtual LocalShadingResult shadeLocal(RayHit* info,double viewX,double viewY,double viewZ,const std::vector<Light*>& lights,const std::vector<SimpleBody*>& objects,SimpleMaterial* material,TraceWorkspace* workspace) override;
};

#endif
