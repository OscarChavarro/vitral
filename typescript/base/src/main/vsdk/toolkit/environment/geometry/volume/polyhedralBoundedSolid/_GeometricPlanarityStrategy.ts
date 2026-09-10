import { PolyhedralBoundedSolid } from "./PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidGeometricValidator } from "./PolyhedralBoundedSolidGeometricValidator.js";
import type { ToleranceContext } from "./PolyhedralBoundedSolidNumericPolicy.js";
import type { _PolyhedralBoundedSolidValidationStrategy } from "./_PolyhedralBoundedSolidValidationStrategy.js";
/** Applies the planar-face consistency required by [MANT1988].10.2.1 and face equations of chapter 13.1. */ export class _GeometricPlanarityStrategy implements _PolyhedralBoundedSolidValidationStrategy<PolyhedralBoundedSolid> {
    public validate(s: PolyhedralBoundedSolid, c: ToleranceContext, m: string[]): boolean {
        return PolyhedralBoundedSolidGeometricValidator.validateAllFacesPlanarityAndPlanes(s, c, m);
    }
}
