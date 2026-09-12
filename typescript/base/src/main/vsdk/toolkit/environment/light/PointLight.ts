import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { VSDK } from "../../common/VSDK.js";
import type { Ray } from "../geometry/element/Ray.js";
import { Light } from "./Light.js";
export class PointLight extends Light {
    public constructor(position: Vector3Dd, emission: ColorRgb) {
        super(position, emission);
    }
    public override getDirectionAndDistance(point: Vector3Dd): Light.LightDirection {
        const v = this.getPosition().subtract(point),
            distance = v.length();
        if (distance <= VSDK.EPSILON) return new Light.LightDirection(new Vector3Dd(), 0);
        return new Light.LightDirection(v.multiply(1 / distance), distance - VSDK.EPSILON);
    }
    public override evaluateLightResponseFactor(_ray: Ray): number {
        return 1;
    }
    public override copy(): Light {
        const copy = new PointLight(this.getPosition(), this.getEmission());
        copy.setId(this.getId());
        copy.setName(this.getName());
        return copy;
    }
}
