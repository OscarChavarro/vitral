#include "LightingShader.h"
#include "CpuTextureSamplingConfig.h"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/statistics/RaytraceStatistics.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/light/LightType.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/geometry/elements/Ray.h"
#include "vsdk/toolkit/environment/geometry/elements/RayHit.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/media/NormalMap.h"
#include "vsdk/toolkit/render/TraceWorkspace.h"
#include <cmath>

static Vector3Dd computeBlinnPerturbedNormal(const RayHit* info, const Vector3Dd& surfaceNormal)
{
    if ( info == 0 || info->normalMap == 0 ) {
        return surfaceNormal;
    }

    Vector3Dd normalVariation =
        CpuTextureSamplingConfig::sampleNormal(info->normalMap, info->u, 1.0 - info->v);
    if ( normalVariation.length() <= VSDK::EPSILON ) {
        return surfaceNormal;
    }

    Vector3Dd baseNormal = surfaceNormal.normalized();
    Vector3Dd surfaceTangentU = info->t.normalized();
    if ( surfaceTangentU.length() <= VSDK::EPSILON ) {
        return surfaceNormal;
    }

    Vector3Dd surfaceTangentV = baseNormal.crossProduct(surfaceTangentU).normalized();
    Vector3Dd nCrossPv = baseNormal.crossProduct(surfaceTangentV);
    Vector3Dd nCrossPu = baseNormal.crossProduct(surfaceTangentU);

    Vector3Dd* bumpScalePtr = info->normalMap->getBumpMapScale();
    if ( bumpScalePtr == 0 ) {
        return surfaceNormal;
    }
    Vector3Dd bumpScale = *bumpScalePtr;
    delete bumpScalePtr;

    if ( std::abs(bumpScale.x()) <= VSDK::EPSILON || std::abs(bumpScale.y()) <= VSDK::EPSILON ) {
        return surfaceNormal;
    }

    double nz = normalVariation.z();
    if ( std::abs(nz) <= VSDK::EPSILON ) {
        nz = (nz < 0.0) ? -VSDK::EPSILON : VSDK::EPSILON;
    }

    double derivativeFu =
        -2.0 * (bumpScale.z() / bumpScale.x()) * (normalVariation.x() / nz);
    double derivativeFv =
        -2.0 * (bumpScale.z() / bumpScale.y()) * (normalVariation.y() / nz);
    Vector3Dd normalPerturbation =
        nCrossPv.multiply(derivativeFu).subtract(nCrossPu.multiply(derivativeFv));

    return surfaceNormal.add(normalPerturbation).normalized();
}

LightingShader::LightingShader(bool specularEnabledIn, bool textureEnabledIn, bool bumpMapEnabledIn)
    : specularEnabled(specularEnabledIn), textureEnabled(textureEnabledIn), bumpMapEnabled(bumpMapEnabledIn)
{
}

Shader::LocalShadingResult LightingShader::shadeLocal(
    RayHit* info, double viewX, double viewY, double viewZ,
    const std::vector<Light*>& lights, const std::vector<SimpleBody*>& objects,
    SimpleMaterial* material, TraceWorkspace* workspace)
{
    Vector3Dd surfaceNormal = info->n;
    if ( bumpMapEnabled ) {
        surfaceNormal = computeBlinnPerturbedNormal(info, surfaceNormal);
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

        if ( workspace != 0 ) {
            Vector3Dd shadowOrigin(
                info->p.x() + VSDK::EPSILON * lx,
                info->p.y() + VSDK::EPSILON * ly,
                info->p.z() + VSDK::EPSILON * lz);
            Ray shadowRay(shadowOrigin, Vector3Dd(lx, ly, lz));
            RayHit* shadowCandidateHit = workspace->shadowCandidateHit();
            shadowCandidateHit->setStoreRay(false);

            double maxShadowDistance = 1e308;
            if ( light->tipo_de_luz == LightType::POINT ) {
                double dx = light->lvec.x() - info->p.x();
                double dy = light->lvec.y() - info->p.y();
                double dz = light->lvec.z() - info->p.z();
                maxShadowDistance = std::sqrt(dx*dx + dy*dy + dz*dz) - VSDK::EPSILON;
                if ( maxShadowDistance <= VSDK::EPSILON ) {
                    continue;
                }
            }

            bool shadowed = false;
            for ( size_t oi = 0; oi < objects.size(); oi++ ) {
                SimpleBody* candidateObject = objects[oi];
                shadowCandidateHit->resetForDistanceOnly();
                RaytraceStatistics::recordShadowRay();
                if ( candidateObject != 0 && candidateObject->doIntersection(shadowRay, shadowCandidateHit) ) {
                    double hitDistance = shadowCandidateHit->hitDistance();
                    if ( hitDistance > VSDK::EPSILON && hitDistance < maxShadowDistance ) {
                        shadowed = true;
                        break;
                    }
                }
            }
            if ( shadowed ) {
                continue;
            }
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
