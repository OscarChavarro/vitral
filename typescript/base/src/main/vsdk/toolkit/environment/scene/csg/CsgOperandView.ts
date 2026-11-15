import type { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import type { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import type { Geometry } from "../../geometry/Geometry.js";
import type { GeometryBounds } from "../../geometry/element/GeometryBounds.js";
import { GeometryContainment } from "../../geometry/element/GeometryContainment.js";
import type { Ray } from "../../geometry/element/Ray.js";
import type { RayCrossingQuery } from "../../geometry/element/RayCrossingQuery.js";
import type { RaySolidClassification } from "../../geometry/element/RaySolidClassification.js";
import { RaySolidQueryStatus } from "../../geometry/element/RaySolidQueryStatus.js";
import { AffinePlacementStatus } from "../AffinePlacementStatus.js";
import { CsgExpression } from "./CsgExpression.js";

// A borrowed operand view; its geometry, child expression and placement live
// at least as long as the expression that references this view.
export abstract class CsgOperandView {
    public abstract getGeometry(): Geometry | null;
    public abstract getAffinePlacement(): Matrix4x4d;

    // Output matrices are usable only when the returned status is Valid.
    public queryAffinePlacement(_forward: Matrix4x4d[], _inverse: Matrix4x4d[]): AffinePlacementStatus {
        return AffinePlacementStatus.Indeterminate;
    }

    public getChildExpression(): CsgExpression | null {
        const geometry: unknown = this.getGeometry();
        return CsgExpression.isInstance(geometry) ? geometry : null;
    }

    // Opt in only when child and placement fully describe this view's solid.
    public isStructuralTraversalSafe(): boolean {
        return false;
    }

    // True only when both views denote the very same placed solid.
    public isSameSolid(other: CsgOperandView): boolean {
        return this === other;
    }

    // World-space certified query; unknown operand implementations stay explicit.
    public classifyPointCertified(_point: Vector3Dd, _tolerance: number): GeometryContainment {
        return GeometryContainment.Unsupported;
    }

    // Conservative parent-space bounds of a leaf's solid; Unsupported by default.
    public getSolidBounds(result: GeometryBounds): void {
        result.setUnsupported();
    }

    // Solid segments of a leaf in this operand's parent coordinates.
    public classifyRay(_ray: Ray, _query: RayCrossingQuery, result: RaySolidClassification): RaySolidQueryStatus {
        result.clear();
        return RaySolidQueryStatus.Unsupported;
    }

    // Counter that changes whenever this view's solid or placement is edited
    // through its API; false when the view does not track its edits.
    public queryModificationVersion(_version: number[]): boolean {
        return false;
    }
}
