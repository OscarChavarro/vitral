#ifndef __COOK_TORRANCE_SHADER__
#define __COOK_TORRANCE_SHADER__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/render/shaders/Shader.h"
class CookTorranceShader : public Shader {
private:
    BOOLEAN textureEnabled;
    BOOLEAN bumpMapEnabled;
public:
    CookTorranceShader(BOOLEAN textureEnabled, BOOLEAN bumpMapEnabled);
    virtual LocalShadingResult shadeLocal(RayHit* info,double viewX,double viewY,double viewZ,ArrayList<Light*>& lights,ArrayList<SimpleBody*>& objects,SimpleMaterial* material,TraceWorkspace* workspace);
};

#endif
