#include "CookTorranceShader.h"
#include "LightingShader.h"

CookTorranceShader::CookTorranceShader(bool textureEnabledIn, bool bumpMapEnabledIn)
    : textureEnabled(textureEnabledIn), bumpMapEnabled(bumpMapEnabledIn)
{
}

Shader::LocalShadingResult CookTorranceShader::shadeLocal(RayHit* info,double viewX,double viewY,double viewZ,const std::vector<Light*>& lights,const std::vector<SimpleBody*>& objects,SimpleMaterial* material,TraceWorkspace* workspace)
{
    LightingShader fallback(true, textureEnabled, bumpMapEnabled);
    return fallback.shadeLocal(info, viewX, viewY, viewZ, lights, objects, material, workspace);
}
