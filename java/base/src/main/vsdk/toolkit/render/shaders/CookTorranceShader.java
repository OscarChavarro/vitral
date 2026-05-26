//= References:                                                             =
//= [COOK1982] Cook, Robert L.; Torrance, Kenneth E. "A Reflectance Model   =
//= for Computer Graphics", ACM Transactions on Graphics, 1982.             =

package vsdk.toolkit.render.shaders;
import vsdk.toolkit.render.TraceWorkspace;

import java.util.List;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.light.Light;
import vsdk.toolkit.environment.light.LightType;
import vsdk.toolkit.environment.material.SimpleMaterial;
import vsdk.toolkit.environment.material.MicroFacetedMaterial;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.scene.SimpleBody;

public final class CookTorranceShader extends Shader {
    private static final double DEFAULT_ROUGHNESS = 0.35;
    private static final double MIN_ROUGHNESS = 0.02;
    private static final double EPS = 1e-8;

    private final boolean textureEnabled;
    private final boolean bumpMapEnabled;

    public CookTorranceShader(boolean textureEnabled, boolean bumpMapEnabled)
    {
        this.textureEnabled = textureEnabled;
        this.bumpMapEnabled = bumpMapEnabled;
    }

    @Override
    public LocalShadingResult shadeLocal(
        RayHit info,
        double viewX,
        double viewY,
        double viewZ,
        List<Light> lights,
        List<SimpleBody> objects,
        SimpleMaterial material,
        TraceWorkspace workspace)
    {
        Vector3Dd surfaceNormal = info.n;
        if ( bumpMapEnabled ) {
            surfaceNormal = computeBlinnPerturbedNormal(info, surfaceNormal);
        }

        Vector3Dd normal = surfaceNormal.normalized();
        double normalX = normal.x();
        double normalY = normal.y();
        double normalZ = normal.z();
        double viewLength = Math.sqrt(viewX * viewX + viewY * viewY + viewZ * viewZ);
        if ( viewLength <= EPS ) {
            return new LocalShadingResult(surfaceNormal, new ColorRgb(0.0, 0.0, 0.0));
        }
        double invViewLength = 1.0 / viewLength;
        double viewDirX = viewX * invViewLength;
        double viewDirY = viewY * invViewLength;
        double viewDirZ = viewZ * invViewLength;

        ColorRgb ambient = material.getAmbientReference();
        double outR = 0.0;
        double outG = 0.0;
        double outB = 0.0;
        for ( int i = 0; i < lights.size(); i++ ) {
            Light light = lights.get(i);
            ColorRgb lightEmission = light.getSpecularReference();

            if ( light.tipo_de_luz == LightType.AMBIENT ) {
                outR += ambient.r() * lightEmission.r();
                outG += ambient.g() * lightEmission.g();
                outB += ambient.b() * lightEmission.b();
                continue;
            }

            LightDirection lightDirection = resolveLightDirection(light, info);
            if ( lightDirection == null ) {
                continue;
            }
            double lightDirX = lightDirection.x;
            double lightDirY = lightDirection.y;
            double lightDirZ = lightDirection.z;
            if ( isShadowed(
                info,
                lightDirX,
                lightDirY,
                lightDirZ,
                lightDirection.maxShadowDistance,
                objects,
                workspace) ) {
                continue;
            }

            double ndotL = Math.max(0.0, normalX * lightDirX + normalY * lightDirY + normalZ * lightDirZ);
            if ( ndotL <= 0.0 ) {
                continue;
            }

            double ndotV = Math.max(0.0, normalX * viewDirX + normalY * viewDirY + normalZ * viewDirZ);
            if ( ndotV <= 0.0 ) {
                continue;
            }

            double halfX = lightDirX + viewDirX;
            double halfY = lightDirY + viewDirY;
            double halfZ = lightDirZ + viewDirZ;
            double halfLength = Math.sqrt(halfX * halfX + halfY * halfY + halfZ * halfZ);
            if ( halfLength <= EPS ) {
                continue;
            }
            double invHalfLength = 1.0 / halfLength;
            halfX *= invHalfLength;
            halfY *= invHalfLength;
            halfZ *= invHalfLength;
            double ndotH = Math.max(0.0, normalX * halfX + normalY * halfY + normalZ * halfZ);
            double vdotH = Math.max(0.0, viewDirX * halfX + viewDirY * halfY + viewDirZ * halfZ);

            MicrofacetParams params = resolveMicrofacetParams(material);
            double roughness = Math.max(MIN_ROUGHNESS, params.roughness);
            double alpha = Math.max(MIN_ROUGHNESS * MIN_ROUGHNESS, params.alpha);

            ColorRgb baseDiffuse = material.getDiffuseReference();
            double diffuseR = baseDiffuse.r();
            double diffuseG = baseDiffuse.g();
            double diffuseB = baseDiffuse.b();
            if ( textureEnabled && info.texture != null ) {
                ColorRgb textureColor =
                    CpuTextureSamplingConfig.sample(info.texture, info.u, 1.0 - info.v);
                diffuseR *= textureColor.r();
                diffuseG *= textureColor.g();
                diffuseB *= textureColor.b();
            }

            double distribution = beckmannDistribution(ndotH, roughness);
            double geometry = smithSchlickGeometry(ndotV, ndotL, alpha);
            double fresnelPower = Math.pow(Math.max(0.0, 1.0 - vdotH), 5.0);
            double fresnelR =
                params.fresnelF0.r() + (1.0 - params.fresnelF0.r()) * fresnelPower;
            double fresnelG =
                params.fresnelF0.g() + (1.0 - params.fresnelF0.g()) * fresnelPower;
            double fresnelB =
                params.fresnelF0.b() + (1.0 - params.fresnelF0.b()) * fresnelPower;
            double denominator = Math.max(EPS, 4.0 * ndotL * ndotV);

            double specR = params.ks * distribution * geometry * fresnelR / denominator;
            double specG = params.ks * distribution * geometry * fresnelG / denominator;
            double specB = params.ks * distribution * geometry * fresnelB / denominator;

            outR += lightEmission.r() * (params.kd * diffuseR * ndotL + specR);
            outG += lightEmission.g() * (params.kd * diffuseG * ndotL + specG);
            outB += lightEmission.b() * (params.kd * diffuseB * ndotL + specB);
        }

        return new LocalShadingResult(surfaceNormal, new ColorRgb(outR, outG, outB));
    }

    private static LightDirection resolveLightDirection(Light light, RayHit info)
    {
        if ( light.tipo_de_luz == LightType.POINT ) {
            double lx = light.lvec.x() - info.p.x();
            double ly = light.lvec.y() - info.p.y();
            double lz = light.lvec.z() - info.p.z();
            double lengthSquared = lx * lx + ly * ly + lz * lz;
            if ( lengthSquared <= VSDK.EPSILON ) {
                return null;
            }
            double invLength = 1.0 / Math.sqrt(lengthSquared);
            return new LightDirection(
                lx * invLength,
                ly * invLength,
                lz * invLength,
                Math.sqrt(lengthSquared) - VSDK.EPSILON);
        }
        double lx = -light.lvec.x();
        double ly = -light.lvec.y();
        double lz = -light.lvec.z();
        double lengthSquared = lx * lx + ly * ly + lz * lz;
        if ( lengthSquared <= EPS ) {
            return null;
        }
        double invLength = 1.0 / Math.sqrt(lengthSquared);
        return new LightDirection(
            lx * invLength,
            ly * invLength,
            lz * invLength,
            Double.POSITIVE_INFINITY);
    }

    private static MicrofacetParams resolveMicrofacetParams(SimpleMaterial material)
    {
        double roughness = DEFAULT_ROUGHNESS;
        double alpha = roughness * roughness;
        ColorRgb f0 = material.getSpecularReference();
        double kd = 1.0;
        double ks = 1.0;

        if ( material instanceof MicroFacetedMaterial microFacetedMaterial ) {
            roughness = microFacetedMaterial.getRoughness();
            alpha = microFacetedMaterial.getAlpha();
            f0 = microFacetedMaterial.getFresnelF0();
            kd = microFacetedMaterial.getKd();
            ks = microFacetedMaterial.getKs();
        }
        return new MicrofacetParams(roughness, alpha, f0, kd, ks);
    }

    private static double beckmannDistribution(double ndotH, double roughness)
    {
        if ( ndotH <= 0.0 ) {
            return 0.0;
        }
        double ndotHSquared = ndotH * ndotH;
        double tanSquaredTheta = (1.0 - ndotHSquared) / Math.max(EPS, ndotHSquared);
        double mSquared = roughness * roughness;
        double exponent = -tanSquaredTheta / Math.max(EPS, mSquared);
        return Math.exp(exponent) /
               (Math.PI * Math.max(EPS, mSquared) * ndotHSquared * ndotHSquared);
    }

    private static double smithSchlickGeometry(double ndotV, double ndotL, double alpha)
    {
        double k = alpha * 0.5;
        double gV = ndotV / (ndotV * (1.0 - k) + k);
        double gL = ndotL / (ndotL * (1.0 - k) + k);
        return gV * gL;
    }

    private static Vector3Dd computeBlinnPerturbedNormal(
        RayHit info,
        Vector3Dd surfaceNormal)
    {
        if ( info.normalMap == null || info.t == null ) {
            return surfaceNormal;
        }
        Vector3Dd normalVariation =
            CpuTextureSamplingConfig.sampleNormal(info.normalMap, info.u, 1.0 - info.v);
        if ( normalVariation == null ) {
            return surfaceNormal;
        }

        Vector3Dd baseNormal = surfaceNormal.normalized();
        Vector3Dd surfaceTangentU = info.t.normalized();
        Vector3Dd surfaceTangentV = baseNormal.crossProduct(surfaceTangentU).normalized();
        Vector3Dd nCrossPv = baseNormal.crossProduct(surfaceTangentV);
        Vector3Dd nCrossPu = baseNormal.crossProduct(surfaceTangentU);

        Vector3Dd bumpScale = info.normalMap.getBumpMapScale();
        double nz = normalVariation.z();
        if ( Math.abs(nz) <= VSDK.EPSILON ) {
            nz = (nz < 0) ? -VSDK.EPSILON : VSDK.EPSILON;
        }

        double derivativeFu =
            -2.0 * (bumpScale.z() / bumpScale.x()) * (normalVariation.x() / nz);
        double derivativeFv =
            -2.0 * (bumpScale.z() / bumpScale.y()) * (normalVariation.y() / nz);
        Vector3Dd normalPerturbation =
            nCrossPv.multiply(derivativeFu).subtract(nCrossPu.multiply(derivativeFv));
        return surfaceNormal.add(normalPerturbation).normalized();
    }

    private record MicrofacetParams(
        double roughness,
        double alpha,
        ColorRgb fresnelF0,
        double kd,
        double ks)
    {
    }

    private record LightDirection(
        double x,
        double y,
        double z,
        double maxShadowDistance)
    {
    }

    private static boolean isShadowed(
        RayHit info,
        double lightDirX,
        double lightDirY,
        double lightDirZ,
        double maxShadowDistance,
        List<SimpleBody> objects,
        TraceWorkspace workspace)
    {
        if ( maxShadowDistance <= VSDK.EPSILON ) {
            return true;
        }
        Vector3Dd shadowOrigin = new Vector3Dd(
            info.p.x() + VSDK.EPSILON * lightDirX,
            info.p.y() + VSDK.EPSILON * lightDirY,
            info.p.z() + VSDK.EPSILON * lightDirZ);
        Vector3Dd shadowDirection = new Vector3Dd(lightDirX, lightDirY, lightDirZ);
        Ray shadowRay = new Ray(shadowOrigin, shadowDirection);
        RayHit shadowCandidateHit = workspace.shadowCandidateHit();
        shadowCandidateHit.setStoreRay(false);

        for ( int i = 0; i < objects.size(); i++ ) {
            SimpleBody candidateObject = objects.get(i);
            shadowCandidateHit.resetForDistanceOnly();
            if ( candidateObject.doIntersection(shadowRay, shadowCandidateHit) ) {
                double hitDistance = shadowCandidateHit.hitDistance();
                if ( hitDistance > VSDK.EPSILON && hitDistance < maxShadowDistance ) {
                    return true;
                }
            }
        }
        return false;
    }
}
