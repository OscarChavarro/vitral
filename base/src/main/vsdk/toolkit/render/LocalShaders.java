//= References:                                                             =
//= [BLIN1978b] Blinn, James F. "Simulation of wrinkled surfaces", SIGGRAPH =
//=          proceedings, 1978.                                             =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

package vsdk.toolkit.render;

import java.util.List;

import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.RaytraceStatistics;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.LightType;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.geometry.RayHit;
import vsdk.toolkit.environment.scene.SimpleBody;

final class LocalShaderSelector {
    private LocalShaderSelector()
    {
    }

    static Shader select(RendererConfiguration qualitySelection)
    {
        // Java software shader strategy selection aligned to JOGL4 selector:
        // jogl4/.../Jogl4RendererConfigurationShaderSelector.
        int shadingType = qualitySelection.getShadingType();
        boolean textureEnabled = qualitySelection.isTextureSet();
        boolean bumpMapEnabled = qualitySelection.isBumpMapSet();

        if ( shadingType == RendererConfiguration.SHADING_TYPE_NOLIGHT ) {
            if ( textureEnabled ) {
                return new ConstantTextureShader();
            }
            return new ConstantShader();
        }

        if ( shadingType == RendererConfiguration.SHADING_TYPE_FLAT ) {
            if ( textureEnabled ) {
                return new FlatTexturedShader();
            }
            return new FlatShader();
        }

        if ( shadingType == RendererConfiguration.SHADING_TYPE_GOURAUD ) {
            return new GouraudTextureShader(textureEnabled);
        }

        if ( shadingType == RendererConfiguration.SHADING_TYPE_PHONG ) {
            if ( textureEnabled && bumpMapEnabled ) {
                return new PhongTextureBumpShader();
            }
            return new PhongTextureShader(textureEnabled);
        }

        return new GouraudTextureShader(textureEnabled);
    }
}

abstract class LightingShader extends Shader {
    private final boolean specularEnabled;
    private final boolean textureEnabled;
    private final boolean bumpMapEnabled;

    protected LightingShader(
        boolean specularEnabled,
        boolean textureEnabled,
        boolean bumpMapEnabled)
    {
        this.specularEnabled = specularEnabled;
        this.textureEnabled = textureEnabled;
        this.bumpMapEnabled = bumpMapEnabled;
    }

    @Override
    final Vector3D shadeLocal(
        RayHit info,
        double viewX,
        double viewY,
        double viewZ,
        List<Light> lights,
        List<SimpleBody> objects,
        Material material,
        TraceWorkspace workspace,
        ColorRgb outColor)
    {
        Vector3D surfaceNormal = info.n;

        if ( bumpMapEnabled ) {
            surfaceNormal = computeBlinnPerturbedNormal(info, surfaceNormal);
        }

        double normalX = surfaceNormal.x();
        double normalY = surfaceNormal.y();
        double normalZ = surfaceNormal.z();

        for ( int i = 0; i < lights.size(); i++ ) {
            Light light = lights.get(i);
            ColorRgb lightEmission = light.getSpecularReference();

            if ( light.tipo_de_luz == LightType.AMBIENT ) {
                ColorRgb ambient = material.getAmbientReference();
                outColor.r += ambient.r*lightEmission.r;
                outColor.g += ambient.g*lightEmission.g;
                outColor.b += ambient.b*lightEmission.b;
                continue;
            }

            double lx;
            double ly;
            double lz;
            double maxShadowDistance = Double.MAX_VALUE;
            if ( light.tipo_de_luz == LightType.POINT ) {
                lx = light.lvec.x() - info.p.x();
                ly = light.lvec.y() - info.p.y();
                lz = light.lvec.z() - info.p.z();
                double lightDistanceSquared = lx*lx + ly*ly + lz*lz;
                if ( lightDistanceSquared <= VSDK.EPSILON ) {
                    continue;
                }
                double lightDistance = Math.sqrt(lightDistanceSquared);
                double invLightDistance = 1.0 / lightDistance;
                lx *= invLightDistance;
                ly *= invLightDistance;
                lz *= invLightDistance;
                maxShadowDistance = lightDistance - VSDK.EPSILON;
                if ( maxShadowDistance <= VSDK.EPSILON ) {
                    continue;
                }
            }
            else {
                lx = -light.lvec.x();
                ly = -light.lvec.y();
                lz = -light.lvec.z();
            }

            Vector3D shadowOffset = new Vector3D(
                info.p.x() + VSDK.EPSILON*lx,
                info.p.y() + VSDK.EPSILON*ly,
                info.p.z() + VSDK.EPSILON*lz);
            RaytraceStatistics.recordShadowRay();
            Ray shadowRay = new Ray(shadowOffset, new Vector3D(lx, ly, lz));
            if ( anyThingInRayDirection(
                     shadowRay,
                     objects,
                     maxShadowDistance,
                     workspace.shadowCandidateHit) ) {
                continue;
            }

            double lambert = normalX*lx + normalY*ly + normalZ*lz;
            if ( lambert <= 0 ) {
                continue;
            }

            ColorRgb diffuse = material.getDiffuseReference();
            double diffuseR = diffuse.r;
            double diffuseG = diffuse.g;
            double diffuseB = diffuse.b;

            // Keep parity with current CPU pipeline: texture modulates
            // diffuse term in local lighting stages.
            if ( textureEnabled && info.texture != null ) {
                ColorRgb textureColor =
                    info.texture.getColorRgbBiLinear(info.u, 1-info.v);
                diffuseR *= textureColor.r;
                diffuseG *= textureColor.g;
                diffuseB *= textureColor.b;
            }

            if ( (diffuseR + diffuseG + diffuseB) > 0 ) {
                outColor.r += lambert*diffuseR*lightEmission.r;
                outColor.g += lambert*diffuseG*lightEmission.g;
                outColor.b += lambert*diffuseB*lightEmission.b;
            }
            if ( !specularEnabled ) {
                continue;
            }

            ColorRgb specular = material.getSpecularReference();
            if ( (specular.r + specular.g + specular.b) <= 0 ) {
                continue;
            }

            double twoLambert = 2*lambert;
            double reflectedViewX = twoLambert*normalX - lx;
            double reflectedViewY = twoLambert*normalY - ly;
            double reflectedViewZ = twoLambert*normalZ - lz;
            double spec =
                viewX*reflectedViewX +
                viewY*reflectedViewY +
                viewZ*reflectedViewZ;
            if ( spec > 0 ) {
                spec = ((specular.r + specular.g + specular.b)/3)*(
                    Math.pow(spec, material.getPhongExponent()));
                outColor.r += spec*lightEmission.r;
                outColor.g += spec*lightEmission.g;
                outColor.b += spec*lightEmission.b;
            }
        }

        return surfaceNormal;
    }

    private static Vector3D computeBlinnPerturbedNormal(
        RayHit info,
        Vector3D surfaceNormal)
    {
        if ( info.normalMap == null ) {
            return surfaceNormal;
        }

        // [BLIN1978b], section 2:
        // N' = N + D, D = ( Fu (N x Pv) - Fv (N x Pu) ) / |N|
        Vector3D normalVariation = info.normalMap.getNormal(info.u, 1-info.v);
        if ( normalVariation == null ) {
            return surfaceNormal;
        }

        Vector3D baseNormal = surfaceNormal.normalized();
        Vector3D surfaceTangentU = info.t.normalized();
        Vector3D surfaceTangentV = baseNormal.crossProduct(surfaceTangentU).normalized();
        Vector3D nCrossPv = baseNormal.crossProduct(surfaceTangentV);
        Vector3D nCrossPu = baseNormal.crossProduct(surfaceTangentU);

        Vector3D bumpScale = info.normalMap.getBumpMapScale();
        double nz = normalVariation.z();
        if ( Math.abs(nz) <= VSDK.EPSILON ) {
            nz = (nz < 0) ? -VSDK.EPSILON : VSDK.EPSILON;
        }

        double derivativeFu =
            -2.0 * (bumpScale.z() / bumpScale.x()) * (normalVariation.x() / nz);
        double derivativeFv =
            -2.0 * (bumpScale.z() / bumpScale.y()) * (normalVariation.y() / nz);
        Vector3D normalPerturbation =
            nCrossPv.multiply(derivativeFu).subtract(nCrossPu.multiply(derivativeFv));
        return surfaceNormal.add(normalPerturbation).normalized();
    }

    private static boolean anyThingInRayDirection(
        Ray inRay,
        List<SimpleBody> objects,
        double maxDistance,
        RayHit candidateHit)
    {
        candidateHit.setStoreRay(false);
        RaytraceStatistics.recordSceneTraversal();
        for ( int i = 0; i < objects.size(); i++ ) {
            SimpleBody body = objects.get(i);
            candidateHit.resetForDistanceOnly();
            RaytraceStatistics.recordObjectIntersectionTest();
            if ( body.doIntersection(inRay, candidateHit) ) {
                double hitDistance = candidateHit.hitDistance();
                if ( hitDistance > VSDK.EPSILON && hitDistance < maxDistance ) {
                    return true;
                }
            }
        }
        return false;
    }
}

// GLSL analogue: constantPixelShader.glsl
final class ConstantShader extends Shader {
    @Override
    Vector3D shadeLocal(
        RayHit info,
        double viewX,
        double viewY,
        double viewZ,
        List<Light> lights,
        List<SimpleBody> objects,
        Material material,
        TraceWorkspace workspace,
        ColorRgb outColor)
    {
        ColorRgb diffuse = material.getDiffuseReference();
        outColor.r += diffuse.r;
        outColor.g += diffuse.g;
        outColor.b += diffuse.b;
        return info.n;
    }
}

// GLSL analogue: constantTexturePixelShader.glsl
final class ConstantTextureShader extends Shader {
    @Override
    Vector3D shadeLocal(
        RayHit info,
        double viewX,
        double viewY,
        double viewZ,
        List<Light> lights,
        List<SimpleBody> objects,
        Material material,
        TraceWorkspace workspace,
        ColorRgb outColor)
    {
        ColorRgb diffuse = material.getDiffuseReference();
        double r = diffuse.r;
        double g = diffuse.g;
        double b = diffuse.b;
        if ( info.texture != null ) {
            ColorRgb textureColor = info.texture.getColorRgbBiLinear(info.u, 1-info.v);
            r *= textureColor.r;
            g *= textureColor.g;
            b *= textureColor.b;
        }
        outColor.r += r;
        outColor.g += g;
        outColor.b += b;
        return info.n;
    }
}

// GLSL analogue: flatPixelShader.glsl
final class FlatShader extends LightingShader {
    FlatShader()
    {
        super(true, false, false);
    }
}

// GLSL analogue: flatTexturedPixelShader.glsl
final class FlatTexturedShader extends LightingShader {
    FlatTexturedShader()
    {
        super(true, true, false);
    }
}

// GLSL analogue: gouraudTexturePixelShader.glsl
final class GouraudTextureShader extends LightingShader {
    GouraudTextureShader(boolean textureEnabled)
    {
        super(true, textureEnabled, false);
    }
}

// GLSL analogue: phongTexturePixelShader.glsl
final class PhongTextureShader extends LightingShader {
    PhongTextureShader(boolean textureEnabled)
    {
        super(true, textureEnabled, false);
    }
}

// GLSL analogue: phongTextureBumpPixelShader.glsl
final class PhongTextureBumpShader extends LightingShader {
    PhongTextureBumpShader()
    {
        super(true, true, true);
    }
}
