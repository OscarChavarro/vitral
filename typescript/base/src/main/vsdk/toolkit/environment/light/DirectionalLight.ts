import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import type { Ray } from "../geometry/element/Ray.js";
import { Light } from "./Light.js";

/** Light with no finite position, whose stored position is its direction. */
export class DirectionalLight extends Light {
    public constructor(direction: Vector3Dd, emission: ColorRgb) {
        super(direction.normalized(), emission);
    }

    public override getDirectionAndDistance(_surfacePoint: Vector3Dd): Light.LightDirection {
        const direction = this.getPosition();
        return new Light.LightDirection(
            new Vector3Dd(-direction.x(), -direction.y(), -direction.z()),
            Number.POSITIVE_INFINITY,
        );
    }

    public override evaluateLightResponseFactor(_lightSourceRay: Ray): number {
        return 1.0;
    }

    public override copy(): Light {
        const copy = new DirectionalLight(this.getPosition(), this.getEmission());
        copy.setId(this.getId());
        copy.setName(this.getName());
        return copy;
    }
}
