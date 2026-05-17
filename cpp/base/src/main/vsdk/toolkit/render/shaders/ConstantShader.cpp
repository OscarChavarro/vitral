#include "ConstantShader.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/geometry/elements/RayHit.h"
Shader::LocalShadingResult ConstantShader::shadeLocal(RayHit* info,double,double,double,const std::vector<Light*>&,const std::vector<SimpleBody*>&,SimpleMaterial* material,TraceWorkspace*)
{ return LocalShadingResult(info->n, ColorRgb(material->getDiffuseReference())); }
