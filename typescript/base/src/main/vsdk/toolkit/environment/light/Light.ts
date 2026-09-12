import { FundamentalEntity } from "../../common/FundamentalEntity.js";
import { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { ColorRgb } from "../../common/color/ColorRgb.js";
import type { Ray } from "../geometry/element/Ray.js";
export abstract class Light extends FundamentalEntity {
    private name = "";
    private id = 0;
    public constructor(
        private position: Vector3Dd,
        private emission: ColorRgb,
    ) {
        super();
    }
    public getName(): string {
        return this.name;
    }
    public setName(name: string): void {
        this.name = name;
    }
    public getId(): number {
        return this.id;
    }
    public setId(id: number): void {
        this.id = id;
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
    public isAmbient(): boolean {
        return false;
    }
    public abstract getDirectionAndDistance(surfacePoint: Vector3Dd): Light.LightDirection;
    public abstract evaluateLightResponseFactor(lightSourceRay: Ray): number;
    public abstract copy(): Light;
}
export namespace Light {
    export class LightDirection {
        public constructor(
            private readonly _direction: Vector3Dd,
            private readonly _maxShadowDistance: number,
        ) {}
        public direction(): Vector3Dd {
            return this._direction;
        }
        public maxShadowDistance(): number {
            return this._maxShadowDistance;
        }
    }
}
