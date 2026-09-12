//= References:                                                             =
//= [BLIN1978b] Blinn, James F. "Simulation of wrinkled surfaces", SIGGRAPH =
//=          proceedings, 1978.                                             =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

import type { TraceWorkspace } from "../TraceWorkspace.js";

import type { List } from "../../../../java/util/List.js";

import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Ray } from "../../environment/geometry/element/Ray.js";
import { VSDK } from "../../common/VSDK.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import type { Light } from "../../environment/light/Light.js";
import type { SimpleMaterial } from "../../environment/material/SimpleMaterial.js";
import type { RayHit } from "../../environment/geometry/element/RayHit.js";
import type { SimpleBody } from "../../environment/scene/SimpleBody.js";
import { Shader } from "./Shader.js";
import { CpuTextureSamplingConfig } from "./CpuTextureSamplingConfig.js";

export abstract class LightingShader extends Shader {
    private readonly specularEnabled: boolean;
    private readonly textureEnabled: boolean;
    private readonly bumpMapEnabled: boolean;

    protected constructor(specularEnabled: boolean, textureEnabled: boolean, bumpMapEnabled: boolean) {
        super();
        this.specularEnabled = specularEnabled;
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
            surfaceNormal = LightingShader.computeBlinnPerturbedNormal(info, surfaceNormal);
        }

        const normalX: number = surfaceNormal.x();
        const normalY: number = surfaceNormal.y();
        const normalZ: number = surfaceNormal.z();
        let outR = 0.0;
        let outG = 0.0;
        let outB = 0.0;

        for (let i = 0; i < lights.size(); i++) {
            const light: Light = lights.get(i);
            const lightEmission: ColorRgb = light.getEmission();

            if (light.isAmbient()) {
                const ambient: ColorRgb = material.getAmbientReference();
                outR += ambient.r() * lightEmission.r();
                outG += ambient.g() * lightEmission.g();
                outB += ambient.b() * lightEmission.b();
                continue;
            }

            const lightDirection: Light.LightDirection = light.getDirectionAndDistance(info.p);
            const maxShadowDistance: number = lightDirection.maxShadowDistance();
            if (maxShadowDistance <= VSDK.EPSILON) {
                continue;
            }
            const lx: number = lightDirection.direction().x();
            const ly: number = lightDirection.direction().y();
            const lz: number = lightDirection.direction().z();

            const shadowDirection: Vector3Dd = new Vector3Dd(lx, ly, lz);
            const lightSourceRay: Ray = new Ray(info.p, shadowDirection);
            const attenuation: number = light.evaluateLightResponseFactor(lightSourceRay);
            if (attenuation <= 0.0) {
                continue;
            }

            if (LightingShader.isShadowed(info, lx, ly, lz, maxShadowDistance, objects, workspace)) {
                continue;
            }

            const lambert: number = normalX * lx + normalY * ly + normalZ * lz;
            if (lambert <= 0) {
                continue;
            }

            const diffuse: ColorRgb = material.getDiffuseReference();
            let diffuseR: number = diffuse.r();
            let diffuseG: number = diffuse.g();
            let diffuseB: number = diffuse.b();

            if (this.textureEnabled && info.texture !== null) {
                const textureColor: ColorRgb = CpuTextureSamplingConfig.sample(info.texture, info.u, 1 - info.v)!;
                diffuseR *= textureColor.r();
                diffuseG *= textureColor.g();
                diffuseB *= textureColor.b();
            }

            if (diffuseR + diffuseG + diffuseB > 0) {
                outR += attenuation * lambert * diffuseR * lightEmission.r();
                outG += attenuation * lambert * diffuseG * lightEmission.g();
                outB += attenuation * lambert * diffuseB * lightEmission.b();
            }
            if (!this.specularEnabled) {
                continue;
            }

            const specular: ColorRgb = material.getSpecularReference();
            if (specular.r() + specular.g() + specular.b() <= 0) {
                continue;
            }

            const twoLambert: number = 2 * lambert;
            const reflectedViewX: number = twoLambert * normalX - lx;
            const reflectedViewY: number = twoLambert * normalY - ly;
            const reflectedViewZ: number = twoLambert * normalZ - lz;
            let spec: number = viewX * reflectedViewX + viewY * reflectedViewY + viewZ * reflectedViewZ;
            if (spec > 0) {
                spec = ((specular.r() + specular.g() + specular.b()) / 3) * Math.pow(spec, material.getPhongExponent());
                outR += attenuation * spec * lightEmission.r();
                outG += attenuation * spec * lightEmission.g();
                outB += attenuation * spec * lightEmission.b();
            }
        }

        return new Shader.LocalShadingResult(surfaceNormal, new ColorRgb(outR, outG, outB));
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

    private static computeBlinnPerturbedNormal(info: RayHit, surfaceNormal: Vector3Dd): Vector3Dd {
        if (info.normalMap === null) {
            return surfaceNormal;
        }
        const normalVariation: Vector3Dd | null = CpuTextureSamplingConfig.sampleNormal(
            info.normalMap,
            info.u,
            1 - info.v,
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
}
