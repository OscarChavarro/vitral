import type { Ray } from "../element/Ray.js";

/**
Conservative ray filter evaluated before an exact ray/geometry hit: false
positives are allowed, false negatives are forbidden.
*/
export abstract class PreRayHitElement {
    /** Filter for ray parameters in [0, +inf). */
    public abstract mayIntersect(ray: Ray): boolean;

    /** Filter for ray parameters in [0, maxT]; the unbounded filter is a
    valid default because false positives are allowed. */
    public mayIntersectBefore(ray: Ray, maxT: number): boolean {
        void maxT;
        return this.mayIntersect(ray);
    }
}
