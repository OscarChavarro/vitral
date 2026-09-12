import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import type { Ray } from "../geometry/element/Ray.js";
import { Light } from "./Light.js";
export class DirectionalLight extends Light {
    public constructor(direction: Vector3Dd, emission: ColorRgb) {
        super(direction.normalized(), emission);
    }
    public override getDirectionAndDistance(_point: Vector3Dd): Light.LightDirection {
        const d = this.getPosition();
        return new Light.LightDirection(new Vector3Dd(-d.x(), -d.y(), -d.z()), Infinity);
    }
    public override evaluateLightResponseFactor(_ray: Ray): number {
        return 1;
    }
    public override copy(): Light {
        const copy = new DirectionalLight(this.getPosition(), this.getEmission());
        copy.setId(this.getId());
        copy.setName(this.getName());
        return copy;
    }
}
