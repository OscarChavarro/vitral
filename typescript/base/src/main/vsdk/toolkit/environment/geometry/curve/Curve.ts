import { Geometry, type GeometryRay, type GeometryRayHit } from "../Geometry.js";
import type { Calligraphic3DBuffer } from "../../../media/Calligraphic3DBuffer.js";
export abstract class Curve<
    TRay extends GeometryRay = GeometryRay,
    THit extends GeometryRayHit = GeometryRayHit,
> extends Geometry<TRay, THit> {
    public doIntersectionFirstHit(_inRay: TRay, _outHit: THit): boolean {
        return false;
    }
    public override doExtraInformation(_inRay: TRay, _inT: number, _outHit: THit): void {}

    /**
    Exports a line approximation of current curve, as the 3D segments of a
    polyline per curve segment, in the coordinates of the curve. It is the
    generic representation used to draw curves without a 3D graphics API,
    and to remove their hidden parts (see `HiddenLineRenderer`).
    @return a new buffer with the segments approximating the curve
    */
    public abstract exportToCalligraphic3DBuffer(): Calligraphic3DBuffer;
}
