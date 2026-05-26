#include "vsdk/toolkit/render/shaders/ConstantShader.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/geometry/elements/RayHit.h"
#include "java/util/ArrayList.txx"
Shader::LocalShadingResult ConstantShader::shadeLocal(RayHit* info,double,double,double,java::ArrayList<Light*>&,java::ArrayList<SimpleBody*>&,SimpleMaterial* material,TraceWorkspace*)
{ return LocalShadingResult(info->n, ColorRgb(material->getDiffuseReference())); }
