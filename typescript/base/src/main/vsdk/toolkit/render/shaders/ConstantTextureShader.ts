import type { TraceWorkspace } from "../TraceWorkspace.js";

import type { List } from "../../../../java/util/List.js";

import { ColorRgb } from "../../common/color/ColorRgb.js";
import type { Light } from "../../environment/light/Light.js";
import type { SimpleMaterial } from "../../environment/material/SimpleMaterial.js";
import type { RayHit } from "../../environment/geometry/element/RayHit.js";
import type { SimpleBody } from "../../environment/scene/SimpleBody.js";
import { Shader } from "./Shader.js";
import { CpuTextureSamplingConfig } from "./CpuTextureSamplingConfig.js";

// GLSL analogue: constantTexturePixelShader.glsl
export class ConstantTextureShader extends Shader {
    public shadeLocal(
        info: RayHit,
        _viewX: number,
        _viewY: number,
        _viewZ: number,
        _lights: List<Light>,
        _objects: List<SimpleBody>,
        material: SimpleMaterial,
        _workspace: TraceWorkspace,
    ): Shader.LocalShadingResult {
        const diffuse: ColorRgb = material.getDiffuseReference();
        let r: number = diffuse.r();
        let g: number = diffuse.g();
        let b: number = diffuse.b();

        if (info.texture !== null) {
            const textureColor: ColorRgb = CpuTextureSamplingConfig.sample(info.texture, info.u, 1 - info.v)!;
            r *= textureColor.r();
            g *= textureColor.g();
            b *= textureColor.b();
        }

        return new Shader.LocalShadingResult(info.n, new ColorRgb(r, g, b));
    }
}
