#include "LightingShader.h"
#include "CpuTextureSamplingConfig.h"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/light/LightType.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/geometry/elements/RayHit.h"
#include <cmath>

LightingShader::LightingShader(bool specularEnabledIn, bool textureEnabledIn, bool bumpMapEnabledIn)
    : specularEnabled(specularEnabledIn), textureEnabled(textureEnabledIn), bumpMapEnabled(bumpMapEnabledIn)
{
}

Shader::LocalShadingResult LightingShader::shadeLocal(
    RayHit* info, double viewX, double viewY, double viewZ,
    const std::vector<Light*>& lights, const std::vector<SimpleBody*>&,
    SimpleMaterial* material, TraceWorkspace*)
{
    Vector3Dd surfaceNormal = info->n;
    if ( bumpMapEnabled && info->normalMap != 0 ) {
        surfaceNormal = CpuTextureSamplingConfig::sampleNormal(info->normalMap, info->u, 1.0 - info->v);
        if ( surfaceNormal.length() <= VSDK::EPSILON ) surfaceNormal = info->n;
    }

    double nx = surfaceNormal.x();
    double ny = surfaceNormal.y();
    double nz = surfaceNormal.z();
    double outR = 0.0, outG = 0.0, outB = 0.0;

    for ( size_t i = 0; i < lights.size(); i++ ) {
        Light* light = lights[i];
        if ( light == 0 ) continue;
        const ColorRgb& lightEmission = light->getSpecularReference();

        if ( light->tipo_de_luz == LightType::AMBIENT ) {
            const ColorRgb& ambient = material->getAmbientReference();
            outR += ambient.r() * lightEmission.r();
            outG += ambient.g() * lightEmission.g();
            outB += ambient.b() * lightEmission.b();
            continue;
        }

        double lx = -light->lvec.x();
        double ly = -light->lvec.y();
        double lz = -light->lvec.z();
        if ( light->tipo_de_luz == LightType::POINT ) {
            lx = light->lvec.x() - info->p.x();
            ly = light->lvec.y() - info->p.y();
            lz = light->lvec.z() - info->p.z();
            double d = std::sqrt(lx*lx + ly*ly + lz*lz);
            if ( d <= VSDK::EPSILON ) continue;
            lx /= d; ly /= d; lz /= d;
        }

        double lambert = nx*lx + ny*ly + nz*lz;
        if ( lambert <= 0 ) continue;

        ColorRgb diffuse = material->getDiffuseReference();
        double dr = diffuse.r(), dg = diffuse.g(), db = diffuse.b();
        if ( textureEnabled && info->texture != 0 ) {
            ColorRgb tc = CpuTextureSamplingConfig::sample(info->texture, info->u, 1 - info->v);
            dr *= tc.r(); dg *= tc.g(); db *= tc.b();
        }

        outR += lambert * dr * lightEmission.r();
        outG += lambert * dg * lightEmission.g();
        outB += lambert * db * lightEmission.b();

        if ( specularEnabled ) {
            const ColorRgb& specular = material->getSpecularReference();
            double spec = viewX*(2*lambert*nx-lx) + viewY*(2*lambert*ny-ly) + viewZ*(2*lambert*nz-lz);
            if ( spec > 0 ) {
                spec = ((specular.r()+specular.g()+specular.b())/3.0) * std::pow(spec, material->getPhongExponent());
                outR += spec*lightEmission.r();
                outG += spec*lightEmission.g();
                outB += spec*lightEmission.b();
            }
        }
    }

    return LocalShadingResult(surfaceNormal, ColorRgb(outR, outG, outB));
}
