#include "ConstantTextureShader.h"
#include "CpuTextureSamplingConfig.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/geometry/elements/RayHit.h"
Shader::LocalShadingResult ConstantTextureShader::shadeLocal(RayHit* info,double,double,double,const std::vector<Light*>&,const std::vector<SimpleBody*>&,SimpleMaterial* material,TraceWorkspace*)
{
    ColorRgb diffuse = material->getDiffuseReference();
    double r=diffuse.r(),g=diffuse.g(),b=diffuse.b();
    if ( info->texture != 0 ) {
        ColorRgb tc = CpuTextureSamplingConfig::sample(info->texture, info->u, 1-info->v);
        r*=tc.r(); g*=tc.g(); b*=tc.b();
    }
    return LocalShadingResult(info->n, ColorRgb(r,g,b));
}
