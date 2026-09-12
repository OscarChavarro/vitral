import type { List } from "../../../../java/util/List.js";

import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import type { Light } from "../../environment/light/Light.js";
import type { SimpleMaterial } from "../../environment/material/SimpleMaterial.js";
import type { RayHit } from "../../environment/geometry/element/RayHit.js";
import type { SimpleBody } from "../../environment/scene/SimpleBody.js";
import type { TraceWorkspace } from "../TraceWorkspace.js";

export abstract class Shader {
    public abstract shadeLocal(
        info: RayHit,
        viewX: number,
        viewY: number,
        viewZ: number,
        lights: List<Light>,
        objects: List<SimpleBody>,
        material: SimpleMaterial,
        workspace: TraceWorkspace,
    ): Shader.LocalShadingResult;
}

export namespace Shader {
    /**
    Port of the Java record `Shader.LocalShadingResult`, including its
    accessors, equality and string form.
    */
    export class LocalShadingResult {
        public constructor(
            private readonly normalValue: Vector3Dd,
            private readonly colorValue: ColorRgb,
        ) {}

        public normal(): Vector3Dd {
            return this.normalValue;
        }

        public color(): ColorRgb {
            return this.colorValue;
        }

        public equals(o: unknown): boolean {
            return (
                o instanceof LocalShadingResult &&
                this.normalValue.equals(o.normalValue) &&
                this.colorValue.equals(o.colorValue)
            );
        }

        public toString(): string {
            return `LocalShadingResult[normal=${this.normalValue.toString()}, color=${this.colorValue.toString()}]`;
        }
    }
}
