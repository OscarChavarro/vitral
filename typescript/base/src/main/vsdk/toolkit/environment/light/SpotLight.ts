import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { VSDK } from "../../common/VSDK.js";
import type { Ray } from "../geometry/element/Ray.js";
import { Light } from "./Light.js";
export class SpotLight extends Light {
    public constructor(
        position: Vector3Dd,
        private readonly pointsAt: Vector3Dd,
        emission: ColorRgb,
        private readonly coefficient: number,
        private readonly radius: number,
        private readonly falloff: number,
    ) {
        super(position, emission);
    }
    public getPointsAt(): Vector3Dd {
        return this.pointsAt;
    }
    public getCoefficient(): number {
        return this.coefficient;
    }
    public getRadius(): number {
        return this.radius;
    }
    public getFalloff(): number {
        return this.falloff;
    }
    public override getDirectionAndDistance(point: Vector3Dd): Light.LightDirection {
        const v = this.getPosition().subtract(point),
            distance = v.length();
        if (distance <= VSDK.EPSILON) return new Light.LightDirection(new Vector3Dd(), 0);
        return new Light.LightDirection(v.multiply(1 / distance), distance - VSDK.EPSILON);
    }
    private static cubicSpline(low: number, high: number, pos: number): number {
        if (pos < low) return 0;
        if (pos > high) return 1;
        if (high === low) return 0;
        const t = (pos - low) / (high - low);
        return (3 - 2 * t) * t * t;
    }
    public override evaluateLightResponseFactor(ray: Ray): number {
        let d = this.pointsAt.subtract(this.getPosition());
        const length = d.length();
        if (length <= 0) return 0;
        d = d.multiply(1 / length);
        const cosine = -ray.getDirection().dotProduct(d);
        if (cosine <= 0) return 0;
        let response = Math.pow(cosine, this.coefficient);
        if (this.radius > 0) response *= SpotLight.cubicSpline(this.falloff, this.radius, cosine);
        return response;
    }
    public override copy(): Light {
        const copy = new SpotLight(
            this.getPosition(),
            this.pointsAt,
            this.getEmission(),
            this.coefficient,
            this.radius,
            this.falloff,
        );
        copy.setId(this.getId());
        copy.setName(this.getName());
        return copy;
    }
}
