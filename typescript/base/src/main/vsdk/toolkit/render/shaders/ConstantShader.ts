import type { TraceWorkspace } from "../TraceWorkspace.js";

import type { List } from "../../../../java/util/List.js";

import { ColorRgb } from "../../common/color/ColorRgb.js";
import type { Light } from "../../environment/light/Light.js";
import type { SimpleMaterial } from "../../environment/material/SimpleMaterial.js";
import type { RayHit } from "../../environment/geometry/element/RayHit.js";
import type { SimpleBody } from "../../environment/scene/SimpleBody.js";
import { Shader } from "./Shader.js";

// GLSL analogue: constantPixelShader.glsl
export class ConstantShader extends Shader {
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
        return new Shader.LocalShadingResult(info.n, new ColorRgb(diffuse));
    }
}
