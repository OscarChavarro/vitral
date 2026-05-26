#ifndef __VSDK_TOOLKIT_RENDER_SHADERS_CONSTANTTEXTURESHADER_H__
#define __VSDK_TOOLKIT_RENDER_SHADERS_CONSTANTTEXTURESHADER_H__
#include "vsdk/toolkit/render/shaders/Shader.h"
class ConstantTextureShader : public Shader {
public:
    virtual LocalShadingResult shadeLocal(RayHit* info,double,double,double,java::ArrayList<Light*>&,java::ArrayList<SimpleBody*>&,SimpleMaterial* material,TraceWorkspace*) override;
};
#endif
