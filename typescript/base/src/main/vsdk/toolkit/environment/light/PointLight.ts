import { VSDK } from "../../common/VSDK.js";
import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import type { Ray } from "../geometry/element/Ray.js";
import { Light } from "./Light.js";

/** Omnidirectional light emitting from a finite position. */
export class PointLight extends Light {
    public constructor(position: Vector3Dd, emission: ColorRgb) {
        super(position, emission);
    }

    public override getDirectionAndDistance(surfacePoint: Vector3Dd): Light.LightDirection {
        const toLight = this.getPosition().subtract(surfacePoint);
        const distance = toLight.length();
        if (distance <= VSDK.EPSILON) {
            return new Light.LightDirection(new Vector3Dd(0, 0, 0), 0.0);
        }
        const invDistance = 1.0 / distance;
        return new Light.LightDirection(
            new Vector3Dd(toLight.x() * invDistance, toLight.y() * invDistance, toLight.z() * invDistance),
            distance - VSDK.EPSILON,
        );
    }

    public override evaluateLightResponseFactor(_lightSourceRay: Ray): number {
        return 1.0;
    }

    public override copy(): Light {
        const copy = new PointLight(this.getPosition(), this.getEmission());
        copy.setId(this.getId());
        copy.setName(this.getName());
        return copy;
    }
}
