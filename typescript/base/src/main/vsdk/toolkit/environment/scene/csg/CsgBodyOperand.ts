import type { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import type { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import type { Geometry } from "../../geometry/Geometry.js";
import type { GeometryBounds } from "../../geometry/element/GeometryBounds.js";
import type { GeometryContainment } from "../../geometry/element/GeometryContainment.js";
import type { Ray } from "../../geometry/element/Ray.js";
import type { RayCrossingQuery } from "../../geometry/element/RayCrossingQuery.js";
import type { RaySolidClassification } from "../../geometry/element/RaySolidClassification.js";
import type { RaySolidQueryStatus } from "../../geometry/element/RaySolidQueryStatus.js";
import type { AffinePlacementStatus } from "../AffinePlacementStatus.js";
import type { SceneSolid } from "../SceneSolid.js";
import { CsgOperandView } from "./CsgOperandView.js";
import { CsgSolidEquivalence } from "./CsgSolidEquivalence.js";

// Borrowed scene body (any SceneSolid); retains its existing geometry and affine placement.
export class CsgBodyOperand extends CsgOperandView {
    private readonly body: SceneSolid;

    public constructor(value: SceneSolid) {
        super();
        this.body = value;
    }

    public getGeometry(): Geometry | null {
        return this.body.getGeometry();
    }

    public getAffinePlacement(): Matrix4x4d {
        return this.body.getAffinePlacement();
    }

    public override queryAffinePlacement(forward: Matrix4x4d[], inverse: Matrix4x4d[]): AffinePlacementStatus {
        return this.body.queryAffinePlacement(forward, inverse);
    }

    public override classifyPointCertified(point: Vector3Dd, tolerance: number): GeometryContainment {
        return this.body.classifyPointCertified(point, tolerance);
    }

    public override getSolidBounds(result: GeometryBounds): void {
        this.body.getSolidWorldBounds(result);
    }

    public override isSameSolid(other: CsgOperandView): boolean {
        const bodyOperand = other instanceof CsgBodyOperand ? other : null;
        return bodyOperand !== null && CsgSolidEquivalence.same(this.body, bodyOperand.body);
    }

    public override classifyRay(
        ray: Ray,
        query: RayCrossingQuery,
        result: RaySolidClassification,
    ): RaySolidQueryStatus {
        return this.body.classifyRay(ray, query, result);
    }

    // A body whose geometry is a CSG expression is traversed structurally only
    // when the body denotes exactly that placed geometry.
    public override isStructuralTraversalSafe(): boolean {
        return this.body.isStructuralTraversalSafe();
    }

    public override queryModificationVersion(version: number[]): boolean {
        version[0] = this.body.getModificationVersion();
        return true;
    }
}
