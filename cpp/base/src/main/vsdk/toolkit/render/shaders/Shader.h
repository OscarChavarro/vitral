#ifndef __VSDK_TOOLKIT_RENDER_SHADERS_SHADER_H__
#define __VSDK_TOOLKIT_RENDER_SHADERS_SHADER_H__

#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "java/util/ArrayList.h"

class RayHit;
class Light;
class SimpleBody;
class SimpleMaterial;
class TraceWorkspace;

class Shader {
public:
    struct LocalShadingResult {
        Vector3Dd normal;
        ColorRgb color;
        LocalShadingResult(const Vector3Dd& n, const ColorRgb& c) : normal(n), color(c) {}
    };

    virtual ~Shader() {}
    virtual LocalShadingResult shadeLocal(
        RayHit* info,
        double viewX,
        double viewY,
        double viewZ,
        java::ArrayList<Light*>& lights,
        java::ArrayList<SimpleBody*>& objects,
        SimpleMaterial* material,
        TraceWorkspace* workspace) = 0;
};

#endif
