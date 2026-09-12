//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import type { StringBuilder } from "../../../../../../java/lang/StringBuilder.js";
import type { PolyhedralBoundedSolid } from "./PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidGeometricValidator } from "./PolyhedralBoundedSolidGeometricValidator.js";
import type { ToleranceContext } from "./PolyhedralBoundedSolidNumericPolicy.js";
import type { _PolyhedralBoundedSolidValidationStrategy } from "./_PolyhedralBoundedSolidValidationStrategy.js";

/**
Applies the face-orientation consistency expected from the 2-manifold
boundary model of [MANT1988].10.2.1. An inverted face plane normal breaks
both the visual rendering (back-facing triangles) and the face-equation
half-space test of [MANT1988].13.1 used downstream by the boolean pipeline.
*/
export class _GeometricFaceOrientationStrategy implements _PolyhedralBoundedSolidValidationStrategy {
    /**
    Delegates to the centroid-based heuristic in
    `PolyhedralBoundedSolidGeometricValidator.validateConsistentFaceOrientations`.
    */
    public validate(solid: PolyhedralBoundedSolid, numericContext: ToleranceContext, msg: StringBuilder): boolean {
        return PolyhedralBoundedSolidGeometricValidator.validateConsistentFaceOrientations(solid, numericContext, msg);
    }
}
