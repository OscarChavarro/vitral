import { Geometry } from "../Geometry.js";
import type { Ray } from "../element/Ray.js";
import type { RayHit } from "../element/RayHit.js";
import type { Calligraphic3DBuffer } from "../../../media/Calligraphic3DBuffer.js";

export abstract class Curve extends Geometry {
    /**
    This method is provided to ease the integration with generic operation
    Geometry.doIntersectionFirstHit.  A default behavior of not responding the test
    is provided here for 1-dimensional forms. Note that a Loft creation
    between current curve and a circle curve for emulating a tube like
    structure, gives as a result a 2-dimensional Surface, from which a
    Loft.doIntersectionFirstHit operation will give similar results to that
    expected from this 1-dimensional case. However, as real mathematical
    1-dimensional objects are infinitively thin, a doIntersectionFirstHit operation
    will always fail as the operation is regularized for constructive
    solid modelling compatible interpretation.

    Java overload `doIntersectionFirstHit(Ray r)`, which returns null.
    @param r
    @return always if false
    */
    public doIntersectionFirstHit(r: Ray): Ray | null;
    public override doIntersectionFirstHit(inRay: Ray, outHit: RayHit): boolean;
    public override doIntersectionFirstHit(inRay: Ray, outHit: RayHit | null): boolean;
    public override doIntersectionFirstHit(inRay: Ray, outHit?: RayHit | null): Ray | null | boolean {
        if (outHit === undefined) {
            return null;
        }
        const hit: Ray | null = this.doIntersectionFirstHit(inRay);
        if (hit === null) {
            return false;
        }
        if (outHit !== null) {
            outHit.setRay(hit);
            this.doExtraInformation(hit, hit.getT(), outHit);
        }
        return true;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doExtraInformation.
    @param inRay
    @param intT
    @param outData
    */
    public override doExtraInformation(_inRay: Ray, _intT: number, _outData: RayHit): void {}

    /**
    Exports a line approximation of current curve, as the 3D segments of a
    polyline per curve segment, in the coordinates of the curve. It is the
    generic representation used to draw curves without a 3D graphics API,
    and to remove their hidden parts (see `HiddenLineRenderer`).
    @return a new buffer with the segments approximating the curve
    */
    public abstract exportToCalligraphic3DBuffer(): Calligraphic3DBuffer;
}
