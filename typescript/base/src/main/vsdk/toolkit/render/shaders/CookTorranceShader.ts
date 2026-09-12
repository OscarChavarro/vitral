//= References:                                                             =
//= [COOK1982] Cook, Robert L.; Torrance, Kenneth E. "A Reflectance Model   =
//= for Computer Graphics", ACM Transactions on Graphics, 1982.             =

import type { TraceWorkspace } from "../TraceWorkspace.js";

import type { List } from "../../../../java/util/List.js";

import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Ray } from "../../environment/geometry/element/Ray.js";
import { VSDK } from "../../common/VSDK.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import type { Light } from "../../environment/light/Light.js";
import type { SimpleMaterial } from "../../environment/material/SimpleMaterial.js";
import { MicroFacetedMaterial } from "../../environment/material/MicroFacetedMaterial.js";
import type { RayHit } from "../../environment/geometry/element/RayHit.js";
import type { SimpleBody } from "../../environment/scene/SimpleBody.js";
import { Shader } from "./Shader.js";
import { CpuTextureSamplingConfig } from "./CpuTextureSamplingConfig.js";

/**
Port of the Java private record `CookTorranceShader.MicrofacetParams`.
*/
class _MicrofacetParams {
    public constructor(
        public readonly roughness: number,
        public readonly alpha: number,
        public readonly fresnelF0: ColorRgb,
        public readonly kd: number,
        public readonly ks: number,
    ) {}
}

export class CookTorranceShader extends Shader {
    private static readonly DEFAULT_ROUGHNESS = 0.35;
    private static readonly MIN_ROUGHNESS = 0.02;
    private static readonly EPS = 1e-8;

    private readonly textureEnabled: boolean;
    private readonly bumpMapEnabled: boolean;

    public constructor(textureEnabled: boolean, bumpMapEnabled: boolean) {
        super();
        this.textureEnabled = textureEnabled;
        this.bumpMapEnabled = bumpMapEnabled;
    }

    public shadeLocal(
        info: RayHit,
        viewX: number,
        viewY: number,
        viewZ: number,
        lights: List<Light>,
        objects: List<SimpleBody>,
        material: SimpleMaterial,
        workspace: TraceWorkspace,
    ): Shader.LocalShadingResult {
        let surfaceNormal: Vector3Dd = info.n;
        if (this.bumpMapEnabled) {
            surfaceNormal = CookTorranceShader.computeBlinnPerturbedNormal(info, surfaceNormal);
        }

        const normal: Vector3Dd = surfaceNormal.normalized();
        const normalX: number = normal.x();
        const normalY: number = normal.y();
        const normalZ: number = normal.z();
        const viewLength: number = Math.sqrt(viewX * viewX + viewY * viewY + viewZ * viewZ);
        if (viewLength <= CookTorranceShader.EPS) {
            return new Shader.LocalShadingResult(surfaceNormal, new ColorRgb(0.0, 0.0, 0.0));
        }
        const invViewLength: number = 1.0 / viewLength;
        const viewDirX: number = viewX * invViewLength;
        const viewDirY: number = viewY * invViewLength;
        const viewDirZ: number = viewZ * invViewLength;

        const ambient: ColorRgb = material.getAmbientReference();
        let outR = 0.0;
        let outG = 0.0;
        let outB = 0.0;
        for (let i = 0; i < lights.size(); i++) {
            const light: Light = lights.get(i);
            const lightEmission: ColorRgb = light.getEmission();

            if (light.isAmbient()) {
                outR += ambient.r() * lightEmission.r();
                outG += ambient.g() * lightEmission.g();
                outB += ambient.b() * lightEmission.b();
                continue;
            }

            const lightDirection: Light.LightDirection = light.getDirectionAndDistance(info.p);
            if (lightDirection.maxShadowDistance() <= VSDK.EPSILON) {
                continue;
            }
            const lightDirX: number = lightDirection.direction().x();
            const lightDirY: number = lightDirection.direction().y();
            const lightDirZ: number = lightDirection.direction().z();

            const lightSourceRay: Ray = new Ray(info.p, lightDirection.direction());
            const attenuation: number = light.evaluateLightResponseFactor(lightSourceRay);
            if (attenuation <= 0.0) {
                continue;
            }

            if (
                CookTorranceShader.isShadowed(
                    info,
                    lightDirX,
                    lightDirY,
                    lightDirZ,
                    lightDirection.maxShadowDistance(),
                    objects,
                    workspace,
                )
            ) {
                continue;
            }

            const ndotL: number = Math.max(0.0, normalX * lightDirX + normalY * lightDirY + normalZ * lightDirZ);
            if (ndotL <= 0.0) {
                continue;
            }

            const ndotV: number = Math.max(0.0, normalX * viewDirX + normalY * viewDirY + normalZ * viewDirZ);
            if (ndotV <= 0.0) {
                continue;
            }

            let halfX: number = lightDirX + viewDirX;
            let halfY: number = lightDirY + viewDirY;
            let halfZ: number = lightDirZ + viewDirZ;
            const halfLength: number = Math.sqrt(halfX * halfX + halfY * halfY + halfZ * halfZ);
            if (halfLength <= CookTorranceShader.EPS) {
                continue;
            }
            const invHalfLength: number = 1.0 / halfLength;
            halfX *= invHalfLength;
            halfY *= invHalfLength;
            halfZ *= invHalfLength;
            const ndotH: number = Math.max(0.0, normalX * halfX + normalY * halfY + normalZ * halfZ);
            const vdotH: number = Math.max(0.0, viewDirX * halfX + viewDirY * halfY + viewDirZ * halfZ);

            const params: _MicrofacetParams = CookTorranceShader.resolveMicrofacetParams(material);
            const roughness: number = Math.max(CookTorranceShader.MIN_ROUGHNESS, params.roughness);
            const alpha: number = Math.max(
                CookTorranceShader.MIN_ROUGHNESS * CookTorranceShader.MIN_ROUGHNESS,
                params.alpha,
            );

            const baseDiffuse: ColorRgb = material.getDiffuseReference();
            let diffuseR: number = baseDiffuse.r();
            let diffuseG: number = baseDiffuse.g();
            let diffuseB: number = baseDiffuse.b();
            if (this.textureEnabled && info.texture !== null) {
                const textureColor: ColorRgb = CpuTextureSamplingConfig.sample(info.texture, info.u, 1.0 - info.v)!;
                diffuseR *= textureColor.r();
                diffuseG *= textureColor.g();
                diffuseB *= textureColor.b();
            }

            const distribution: number = CookTorranceShader.beckmannDistribution(ndotH, roughness);
            const geometry: number = CookTorranceShader.smithSchlickGeometry(ndotV, ndotL, alpha);
            const fresnelPower: number = Math.pow(Math.max(0.0, 1.0 - vdotH), 5.0);
            const fresnelR: number = params.fresnelF0.r() + (1.0 - params.fresnelF0.r()) * fresnelPower;
            const fresnelG: number = params.fresnelF0.g() + (1.0 - params.fresnelF0.g()) * fresnelPower;
            const fresnelB: number = params.fresnelF0.b() + (1.0 - params.fresnelF0.b()) * fresnelPower;
            const denominator: number = Math.max(CookTorranceShader.EPS, 4.0 * ndotL * ndotV);

            const specR: number = (params.ks * distribution * geometry * fresnelR) / denominator;
            const specG: number = (params.ks * distribution * geometry * fresnelG) / denominator;
            const specB: number = (params.ks * distribution * geometry * fresnelB) / denominator;

            outR += attenuation * lightEmission.r() * (params.kd * diffuseR * ndotL + specR);
            outG += attenuation * lightEmission.g() * (params.kd * diffuseG * ndotL + specG);
            outB += attenuation * lightEmission.b() * (params.kd * diffuseB * ndotL + specB);
        }

        return new Shader.LocalShadingResult(surfaceNormal, new ColorRgb(outR, outG, outB));
    }

    private static resolveMicrofacetParams(material: SimpleMaterial): _MicrofacetParams {
        let roughness: number = CookTorranceShader.DEFAULT_ROUGHNESS;
        let alpha: number = roughness * roughness;
        let f0: ColorRgb = material.getSpecularReference();
        let kd = 1.0;
        let ks = 1.0;

        if (material instanceof MicroFacetedMaterial) {
            const microFacetedMaterial: MicroFacetedMaterial = material;
            roughness = microFacetedMaterial.getRoughness();
            alpha = microFacetedMaterial.getAlpha();
            f0 = microFacetedMaterial.getFresnelF0();
            kd = microFacetedMaterial.getKd();
            ks = microFacetedMaterial.getKs();
        }
        return new _MicrofacetParams(roughness, alpha, f0, kd, ks);
    }

    private static beckmannDistribution(ndotH: number, roughness: number): number {
        if (ndotH <= 0.0) {
            return 0.0;
        }
        const ndotHSquared: number = ndotH * ndotH;
        const tanSquaredTheta: number = (1.0 - ndotHSquared) / Math.max(CookTorranceShader.EPS, ndotHSquared);
        const mSquared: number = roughness * roughness;
        const exponent: number = -tanSquaredTheta / Math.max(CookTorranceShader.EPS, mSquared);
        return (
            Math.exp(exponent) / (Math.PI * Math.max(CookTorranceShader.EPS, mSquared) * ndotHSquared * ndotHSquared)
        );
    }

    private static smithSchlickGeometry(ndotV: number, ndotL: number, alpha: number): number {
        const k: number = alpha * 0.5;
        const gV: number = ndotV / (ndotV * (1.0 - k) + k);
        const gL: number = ndotL / (ndotL * (1.0 - k) + k);
        return gV * gL;
    }

    /**
    Java also guards on `info.t == null` here. `RayHit.t` is a non-nullable
    `Vector3Dd` field in the TypeScript port (it is initialized to the zero
    vector and only ever reassigned to another vector), so that half of the
    guard is unreachable and is left out.
    */
    private static computeBlinnPerturbedNormal(info: RayHit, surfaceNormal: Vector3Dd): Vector3Dd {
        if (info.normalMap === null) {
            return surfaceNormal;
        }
        const normalVariation: Vector3Dd | null = CpuTextureSamplingConfig.sampleNormal(
            info.normalMap,
            info.u,
            1.0 - info.v,
        );
        if (normalVariation === null) {
            return surfaceNormal;
        }

        const baseNormal: Vector3Dd = surfaceNormal.normalized();
        const surfaceTangentU: Vector3Dd = info.t.normalized();
        const surfaceTangentV: Vector3Dd = baseNormal.crossProduct(surfaceTangentU).normalized();
        const nCrossPv: Vector3Dd = baseNormal.crossProduct(surfaceTangentV);
        const nCrossPu: Vector3Dd = baseNormal.crossProduct(surfaceTangentU);

        const bumpScale: Vector3Dd = info.normalMap.getBumpMapScale();
        let nz: number = normalVariation.z();
        if (Math.abs(nz) <= VSDK.EPSILON) {
            nz = nz < 0 ? -VSDK.EPSILON : VSDK.EPSILON;
        }

        const derivativeFu: number = -2.0 * (bumpScale.z() / bumpScale.x()) * (normalVariation.x() / nz);
        const derivativeFv: number = -2.0 * (bumpScale.z() / bumpScale.y()) * (normalVariation.y() / nz);
        const normalPerturbation: Vector3Dd = nCrossPv.multiply(derivativeFu).subtract(nCrossPu.multiply(derivativeFv));
        return surfaceNormal.add(normalPerturbation).normalized();
    }

    private static isShadowed(
        info: RayHit,
        lightDirX: number,
        lightDirY: number,
        lightDirZ: number,
        maxShadowDistance: number,
        objects: List<SimpleBody>,
        workspace: TraceWorkspace,
    ): boolean {
        if (maxShadowDistance <= VSDK.EPSILON) {
            return true;
        }
        const shadowOrigin: Vector3Dd = new Vector3Dd(
            info.p.x() + VSDK.EPSILON * lightDirX,
            info.p.y() + VSDK.EPSILON * lightDirY,
            info.p.z() + VSDK.EPSILON * lightDirZ,
        );
        const shadowDirection: Vector3Dd = new Vector3Dd(lightDirX, lightDirY, lightDirZ);
        const shadowRay: Ray = new Ray(shadowOrigin, shadowDirection);
        const shadowCandidateHit: RayHit = workspace.shadowCandidateHit();
        shadowCandidateHit.setStoreRay(false);

        for (let i = 0; i < objects.size(); i++) {
            const candidateObject: SimpleBody = objects.get(i);
            shadowCandidateHit.resetForDistanceOnly();
            if (candidateObject.doIntersectionFirstHit(shadowRay, shadowCandidateHit)) {
                const hitDistance: number = shadowCandidateHit.hitDistance();
                if (hitDistance > VSDK.EPSILON && hitDistance < maxShadowDistance) {
                    return true;
                }
            }
        }
        return false;
    }
}
