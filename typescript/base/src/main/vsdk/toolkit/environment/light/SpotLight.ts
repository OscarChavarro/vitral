import { VSDK } from "../../common/VSDK.js";
import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import type { Ray } from "../geometry/element/Ray.js";
import { Light } from "./Light.js";

/** Cone-shaped light with a cubic-spline falloff between falloff and radius. */
export class SpotLight extends Light {
    private readonly pointsAt: Vector3Dd;
    private readonly coefficient: number;
    private readonly radius: number;
    private readonly falloff: number;

    public constructor(
        position: Vector3Dd,
        pointsAt: Vector3Dd,
        emission: ColorRgb,
        coefficient: number,
        radius: number,
        falloff: number,
    ) {
        super(position, emission);
        this.pointsAt = pointsAt;
        this.coefficient = coefficient;
        this.radius = radius;
        this.falloff = falloff;
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

    private static cubicSpline(low: number, high: number, pos: number): number {
        if (pos < low) {
            return 0.0;
        }
        if (pos > high) {
            return 1.0;
        }
        if (high === low) {
            return 0.0;
        }

        const t = (pos - low) / (high - low);
        return (3 - 2 * t) * t * t;
    }

    public override evaluateLightResponseFactor(lightSourceRay: Ray): number {
        let spotDirection = this.getPointsAt().subtract(this.getPosition());
        const len = spotDirection.length();
        if (len <= 0.0) {
            return 0.0;
        }
        spotDirection = new Vector3Dd(spotDirection.x() / len, spotDirection.y() / len, spotDirection.z() / len);
        const cosTheta = -lightSourceRay.getDirection().dotProduct(spotDirection);
        if (cosTheta <= 0.0) {
            return 0.0;
        }
        let attenuation = Math.pow(cosTheta, this.getCoefficient());
        if (this.getRadius() > 0.0) {
            attenuation *= SpotLight.cubicSpline(this.getFalloff(), this.getRadius(), cosTheta);
        }
        return attenuation;
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
