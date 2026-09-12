import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import type { Ray } from "../geometry/element/Ray.js";
import { Light } from "./Light.js";

/** Position-less light whose emission is added to every shaded point. */
export class AmbientLight extends Light {
    public constructor(emission: ColorRgb) {
        super(new Vector3Dd(0, 0, 0), emission);
    }

    public override isAmbient(): boolean {
        return true;
    }

    public override getDirectionAndDistance(_surfacePoint: Vector3Dd): Light.LightDirection {
        // Never sampled: shaders resolve the ambient contribution from
        // getEmission() directly and skip the shadow ray entirely.
        return new Light.LightDirection(new Vector3Dd(0, 0, 0), 0.0);
    }

    public override evaluateLightResponseFactor(_lightSourceRay: Ray): number {
        return 0.0;
    }

    public override copy(): Light {
        const copy = new AmbientLight(this.getEmission());
        copy.setId(this.getId());
        copy.setName(this.getName());
        return copy;
    }
}
