import { PolyhedralBoundedSolid } from "./PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidGeometricValidator } from "./PolyhedralBoundedSolidGeometricValidator.js";
import type { ToleranceContext } from "./PolyhedralBoundedSolidNumericPolicy.js";
import type { _PolyhedralBoundedSolidValidationStrategy } from "./_PolyhedralBoundedSolidValidationStrategy.js";
/** Applies the face-orientation consistency required by the 2-manifold model of [MANT1988].10.2.1. */ export class _GeometricFaceOrientationStrategy implements _PolyhedralBoundedSolidValidationStrategy<PolyhedralBoundedSolid> {
    public validate(s: PolyhedralBoundedSolid, c: ToleranceContext, m: string[]): boolean {
        return PolyhedralBoundedSolidGeometricValidator.validateConsistentFaceOrientations(s, c, m);
    }
}
