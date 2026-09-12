import { Double } from "../../../../java/lang/Double.js";
import { Entity } from "../../common/Entity.js";
import { ColorRgb } from "../../common/color/ColorRgb.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import type { Ray } from "../geometry/element/Ray.js";

/** Base class for the light models of Vitral's environment. */
export abstract class Light extends Entity {
    private position: Vector3Dd;
    private emission: ColorRgb;
    private id: number;

    /**
     * This string should be used for specific application defined
     * functionality. Can be null.
     */
    private name: string | null;

    protected constructor(position: Vector3Dd, emission: ColorRgb) {
        super();
        this.position = position;
        this.emission = emission;
        this.id = 0;
        this.name = "";
    }

    public getName(): string | null {
        return this.name;
    }

    public setName(n: string | null): void {
        this.name = n;
    }

    public getId(): number {
        return this.id;
    }

    public setId(i: number): void {
        this.id = i;
    }

    public getPosition(): Vector3Dd {
        return this.position;
    }

    public setPosition(position: Vector3Dd): void {
        this.position = position;
    }

    public getEmission(): ColorRgb {
        return this.emission;
    }

    public setEmission(emission: ColorRgb): void {
        this.emission = emission;
    }

    /**
     * True only for AmbientLight: shaders must add its contribution directly
     * from getEmission() and skip shadow sampling entirely.
     * @returns whether this light is an ambient light
     */
    public isAmbient(): boolean {
        return false;
    }

    /**
     * Surface point -> light direction (normalized) and the shadow ray
     * distance limit past which occluders no longer count (a large sentinel
     * for lights with no finite position, e.g. DirectionalLight).
     * @param surfacePoint point being shaded
     * @returns direction toward the light and the shadow ray distance limit
     */
    public abstract getDirectionAndDistance(surfacePoint: Vector3Dd): Light.LightDirection;

    /**
     * Attenuation contract carried over from the povCpp light hierarchy:
     * lightSourceRay direction points from the surface toward the light,
     * normalized.
     * @param lightSourceRay surface-to-light ray
     * @returns attenuation factor in [0, 1] to multiply into the light's
     * contribution
     */
    public abstract evaluateLightResponseFactor(lightSourceRay: Ray): number;

    public abstract copy(): Light;
}

export namespace Light {
    /**
     * Java record `Light.LightDirection`, including the accessor, equality,
     * hash, and text contracts its record declaration generates.
     */
    export class LightDirection {
        public constructor(
            private readonly directionValue: Vector3Dd,
            private readonly maxShadowDistanceValue: number,
        ) {}

        public direction(): Vector3Dd {
            return this.directionValue;
        }

        public maxShadowDistance(): number {
            return this.maxShadowDistanceValue;
        }

        public equals(o: unknown): boolean {
            return (
                o instanceof LightDirection &&
                this.directionValue.equals(o.directionValue) &&
                Double.compare(this.maxShadowDistanceValue, o.maxShadowDistanceValue) === 0
            );
        }

        public hashCode(): number {
            let hash = 0;
            hash = (Math.imul(31, hash) + this.directionValue.hashCode()) | 0;
            return (Math.imul(31, hash) + Double.hashCode(this.maxShadowDistanceValue)) | 0;
        }

        public toString(): string {
            return (
                `LightDirection[direction=${this.directionValue.toString()}` +
                `, maxShadowDistance=${Double.toString(this.maxShadowDistanceValue)}]`
            );
        }
    }
}
