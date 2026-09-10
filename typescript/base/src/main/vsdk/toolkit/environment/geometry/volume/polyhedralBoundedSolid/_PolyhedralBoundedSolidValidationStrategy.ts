import type { ToleranceContext } from "./PolyhedralBoundedSolidNumericPolicy.js";
/** Strategy abstraction for checks preserving the half-edge invariants of [MANT1988].10 and geometric validity conditions of chapters 13 and 15. */
export interface _PolyhedralBoundedSolidValidationStrategy<TSolid = unknown> {
    validate(solid: TSolid, numericContext: ToleranceContext, message: string[]): boolean;
}
