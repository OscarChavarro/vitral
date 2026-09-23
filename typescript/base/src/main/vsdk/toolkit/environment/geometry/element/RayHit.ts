import { FundamentalEntity } from "../../../common/FundamentalEntity.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import type { Image } from "../../../media/Image.js";
import type { NormalMap } from "../../../media/NormalMap.js";
import type { SimpleMaterial } from "../../material/SimpleMaterial.js";
import { Ray } from "./Ray.js";
import type { GeometryRayHit } from "../Geometry.js";
export class RayHit extends FundamentalEntity implements GeometryRayHit {
    public static readonly DETAIL_NONE = 0;
    public static readonly DETAIL_POINT = 1;
    public static readonly DETAIL_NORMAL = 2;
    public static readonly DETAIL_UV = 4;
    public static readonly DETAIL_TANGENT = 8;
    public static readonly DETAIL_ALL = 15;
    public point = new Vector3Dd();
    public normal = new Vector3Dd();
    public tangent = new Vector3Dd();
    public u = 0;
    public v = 0;
    public material: SimpleMaterial | null = null;
    public texture: Image | null = null;
    public normalMap: NormalMap | null = null;
    private ray: Ray | null = null;
    private hitDistance = 0;
    private hitDistanceKnown = false;
    public constructor(
        mask = 15,
        private storeRay = true,
    ) {
        super();
        this.requiredDetailMask = mask;
    }
    private requiredDetailMask: number;
    public clear() {
        this.point = this.normal = this.tangent = new Vector3Dd();
        this.u = this.v = 0;
        this.material = this.texture = this.normalMap = null;
        this.ray = null;
        this.hitDistance = 0;
        this.hitDistanceKnown = false;
    }
    public reset(mask: number) {
        this.requiredDetailMask = mask;
        this.clear();
    }
    public resetForDistanceOnly() {
        this.requiredDetailMask = 0;
        this.ray = null;
        this.hitDistance = 0;
        this.hitDistanceKnown = false;
    }
    public override clone(other?: RayHit): RayHit | void {
        if (other === undefined) return new RayHit(this.requiredDetailMask, this.storeRay);
        Object.assign(this, other);
    }
    public getRequiredDetailMask() {
        return this.requiredDetailMask;
    }
    public setRequiredDetailMask(value: number) {
        this.requiredDetailMask = value;
    }
    public shouldStoreRay() {
        return this.storeRay;
    }
    public setStoreRay(value: boolean) {
        this.storeRay = value;
    }
    public needsPoint() {
        return !!(this.requiredDetailMask & 1);
    }
    public needsNormal() {
        return !!(this.requiredDetailMask & 2);
    }
    public needsTextureCoordinates() {
        return !!(this.requiredDetailMask & 4);
    }
    public needsTangent() {
        return !!(this.requiredDetailMask & 8);
    }
    public needsAnySurfaceData() {
        return this.requiredDetailMask !== 0;
    }
    public getRay() {
        return this.ray;
    }
    public setRay(value: Ray | null) {
        this.ray = value;
        if (value) {
            this.hitDistance = value.getT();
            this.hitDistanceKnown = true;
        }
    }
    public hasHitDistance() {
        return this.hitDistanceKnown;
    }
    public getHitDistance() {
        return this.hitDistanceKnown ? this.hitDistance : (this.ray?.getT() ?? 0);
    }
    public setHitDistance(value: number) {
        this.hitDistance = value;
        this.hitDistanceKnown = true;
    }
}
