#include "vsdk/toolkit/render/shaders/ConstantTextureShader.h"
#include "vsdk/toolkit/render/shaders/CpuTextureSamplingConfig.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/geometry/elements/RayHit.h"
#include "java/util/ArrayList.txx"
Shader::LocalShadingResult ConstantTextureShader::shadeLocal(RayHit* info,double,double,double,java::ArrayList<Light*>&,java::ArrayList<SimpleBody*>&,SimpleMaterial* material,TraceWorkspace*)
{
    ColorRgb diffuse = material->getDiffuseReference();
    double r=diffuse.r(),g=diffuse.g(),b=diffuse.b();
    if ( info->texture != 0 ) {
        ColorRgb tc = CpuTextureSamplingConfig::sample(info->texture, info->u, 1-info->v);
        r*=tc.r(); g*=tc.g(); b*=tc.b();
    }
    return LocalShadingResult(info->n, ColorRgb(r,g,b));
}
