import { Geometry, type GeometryRay, type GeometryRayHit } from "../Geometry.js";
export abstract class Curve<
    TRay extends GeometryRay = GeometryRay,
    THit extends GeometryRayHit = GeometryRayHit,
> extends Geometry<TRay, THit> {
    public doIntersectionFirstHit(_inRay: TRay, _outHit: THit): boolean {
        return false;
    }
    public override doExtraInformation(_inRay: TRay, _inT: number, _outHit: THit): void {}
}
