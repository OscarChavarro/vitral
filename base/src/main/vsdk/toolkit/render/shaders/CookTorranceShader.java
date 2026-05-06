//= References:                                                             =
//= [COOK1982] Cook, Robert L.; Torrance, Kenneth E. "A Reflectance Model   =
//= for Computer Graphics", ACM Transactions on Graphics, 1982.             =

package vsdk.toolkit.render.shaders;
import vsdk.toolkit.render.TraceWorkspace;

import java.util.List;

import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.LightType;
import vsdk.toolkit.environment.Material;
import vsdk.toolkit.environment.MicroFacetedMaterial;
import vsdk.toolkit.environment.geometry.RayHit;
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
    public Vector3D shadeLocal(
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

        Vector3D normal = surfaceNormal.normalized();
        Vector3D viewDir = new Vector3D(viewX, viewY, viewZ).normalized();

        ColorRgb ambient = material.getAmbientReference();
        for ( int i = 0; i < lights.size(); i++ ) {
            Light light = lights.get(i);
            ColorRgb lightEmission = light.getSpecularReference();

            if ( light.tipo_de_luz == LightType.AMBIENT ) {
                outColor.r += ambient.r * lightEmission.r;
                outColor.g += ambient.g * lightEmission.g;
                outColor.b += ambient.b * lightEmission.b;
                continue;
            }

            Vector3D lightDirection = resolveLightDirection(light, info);
            if ( lightDirection == null ) {
                continue;
            }

            double ndotL = Math.max(0.0, normal.dotProduct(lightDirection));
            if ( ndotL <= 0.0 ) {
                continue;
            }

            double ndotV = Math.max(0.0, normal.dotProduct(viewDir));
            if ( ndotV <= 0.0 ) {
                continue;
            }

            Vector3D halfVector = lightDirection.add(viewDir).normalized();
            double ndotH = Math.max(0.0, normal.dotProduct(halfVector));
            double vdotH = Math.max(0.0, viewDir.dotProduct(halfVector));

            MicrofacetParams params = resolveMicrofacetParams(material);
            double roughness = Math.max(MIN_ROUGHNESS, params.roughness);
            double alpha = Math.max(MIN_ROUGHNESS * MIN_ROUGHNESS, params.alpha);

            ColorRgb baseDiffuse = material.getDiffuseReference();
            double diffuseR = baseDiffuse.r;
            double diffuseG = baseDiffuse.g;
            double diffuseB = baseDiffuse.b;
            if ( textureEnabled && info.texture != null ) {
                ColorRgb textureColor =
                    CpuTextureSamplingConfig.sample(info.texture, info.u, 1.0 - info.v);
                diffuseR *= textureColor.r;
                diffuseG *= textureColor.g;
                diffuseB *= textureColor.b;
            }

            double distribution = beckmannDistribution(ndotH, roughness);
            double geometry = smithSchlickGeometry(ndotV, ndotL, alpha);
            ColorRgb fresnel = schlickFresnel(vdotH, params.fresnelF0);
            double denominator = Math.max(EPS, 4.0 * ndotL * ndotV);

            double specR = params.ks * distribution * geometry * fresnel.r / denominator;
            double specG = params.ks * distribution * geometry * fresnel.g / denominator;
            double specB = params.ks * distribution * geometry * fresnel.b / denominator;

            outColor.r += lightEmission.r * (params.kd * diffuseR * ndotL + specR);
            outColor.g += lightEmission.g * (params.kd * diffuseG * ndotL + specG);
            outColor.b += lightEmission.b * (params.kd * diffuseB * ndotL + specB);
        }

        return surfaceNormal;
    }

    private static Vector3D resolveLightDirection(Light light, RayHit info)
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
            return new Vector3D(lx * invLength, ly * invLength, lz * invLength);
        }
        return new Vector3D(-light.lvec.x(), -light.lvec.y(), -light.lvec.z()).normalized();
    }

    private static MicrofacetParams resolveMicrofacetParams(Material material)
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

    private static ColorRgb schlickFresnel(double vdotH, ColorRgb f0)
    {
        double power = Math.pow(Math.max(0.0, 1.0 - vdotH), 5.0);
        return new ColorRgb(
            f0.r + (1.0 - f0.r) * power,
            f0.g + (1.0 - f0.g) * power,
            f0.b + (1.0 - f0.b) * power);
    }

    private static Vector3D computeBlinnPerturbedNormal(
        RayHit info,
        Vector3D surfaceNormal)
    {
        if ( info.normalMap == null || info.t == null ) {
            return surfaceNormal;
        }
        Vector3D normalVariation =
            CpuTextureSamplingConfig.sampleNormal(info.normalMap, info.u, 1.0 - info.v);
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

    private record MicrofacetParams(
        double roughness,
        double alpha,
        ColorRgb fresnelF0,
        double kd,
        double ks)
    {
    }
}
