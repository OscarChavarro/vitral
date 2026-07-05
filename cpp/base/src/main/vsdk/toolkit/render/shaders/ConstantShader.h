#ifndef __CONSTANTSHADER__
#define __CONSTANTSHADER__
#include "vsdk/toolkit/render/shaders/Shader.h"
class ConstantShader : public Shader {
public:
    virtual LocalShadingResult shadeLocal(RayHit* info,double,double,double,java::ArrayList<Light*>&,java::ArrayList<SimpleBody*>&,SimpleMaterial* material,TraceWorkspace*) override;
};
#endif
