import { Double } from "../../../../../java/lang/Double.js";
import type { ArrayList } from "../../../../../java/util/ArrayList.js";
import { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import { Ray } from "../../geometry/element/Ray.js";
import type { RayCrossingQuery } from "../../geometry/element/RayCrossingQuery.js";
import { RaySolidQueryStatus } from "../../geometry/element/RaySolidQueryStatus.js";
import { AffinePlacementStatus } from "../AffinePlacementStatus.js";
import type { CsgExpression } from "./CsgExpression.js";
import { CsgOperation } from "./CsgOperation.js";
import type { CsgRayLeafBatch } from "./CsgRayLeafBatch.js";
import { CsgRayWorkspace } from "./CsgRayWorkspace.js";

// Collects typed leaf intervals, preserving ties; it does not combine them into
// a CSG solid. Failure clears the output and never exposes partial leaves.
export class CsgRayLeafQuery {
    public static query(
        expression: CsgExpression,
        ray: Ray,
        query: RayCrossingQuery,
        result: CsgRayLeafBatch,
    ): RaySolidQueryStatus;
    // Same query with caller-owned scratch storage.
    public static query(
        expression: CsgExpression,
        ray: Ray,
        query: RayCrossingQuery,
        workspace: CsgRayWorkspace,
        result: CsgRayLeafBatch,
    ): RaySolidQueryStatus;
    public static query(
        expression: CsgExpression,
        ray: Ray,
        query: RayCrossingQuery,
        workspaceOrResult: CsgRayWorkspace | CsgRayLeafBatch,
        output?: CsgRayLeafBatch,
    ): RaySolidQueryStatus {
        if (!(workspaceOrResult instanceof CsgRayWorkspace)) {
            const workspace = new CsgRayWorkspace();
            return CsgRayLeafQuery.query(expression, ray, query, workspace, workspaceOrResult);
        }
        const workspace = workspaceOrResult;
        const result = output!;
        result.clear();
        const origin = ray.getOrigin();
        const direction = ray.getDirection();
        if (
            !Double.isFinite(origin.x()) ||
            !Double.isFinite(origin.y()) ||
            !Double.isFinite(origin.z()) ||
            !Double.isFinite(direction.x()) ||
            !Double.isFinite(direction.y()) ||
            !Double.isFinite(direction.z()) ||
            (direction.x() === 0 && direction.y() === 0 && direction.z() === 0) ||
            Double.isNaN(query.minT) ||
            Double.isNaN(query.maxT) ||
            query.minT >= query.maxT ||
            query.minimumChordSquared !== 0 ||
            query.minimumExteriorApproach !== Double.NEGATIVE_INFINITY
        )
            return RaySolidQueryStatus.InvalidInput;
        workspace.path.clear();
        const status = CsgRayLeafQuery.collect(expression, ray, query, workspace.path, result);
        if (status !== RaySolidQueryStatus.Supported) result.clear();
        return status;
    }

    private static collect(
        expression: CsgExpression,
        ray: Ray,
        query: RayCrossingQuery,
        path: ArrayList<CsgExpression>,
        result: CsgRayLeafBatch,
    ): RaySolidQueryStatus {
        if (path.contains(expression)) return RaySolidQueryStatus.InvalidInput;
        const operation = expression.getOperation();
        const count = expression.getOperandCount();
        if (
            count <= 0 ||
            (operation !== CsgOperation.UNION &&
                operation !== CsgOperation.INTERSECTION &&
                operation !== CsgOperation.DIFFERENCE)
        )
            return RaySolidQueryStatus.InvalidInput;
        if (!path.add(expression)) return RaySolidQueryStatus.Indeterminate;
        for (let i = 0; i < count; ++i) {
            const operand = expression.getOperand(i);
            if (operand === null) {
                path.remove(path.size() - 1);
                return RaySolidQueryStatus.InvalidInput;
            }
            const first = result.size();
            const child = operand.getChildExpression();
            const forward: Matrix4x4d[] = [new Matrix4x4d()];
            const inverse: Matrix4x4d[] = [new Matrix4x4d()];
            let status = RaySolidQueryStatus.Supported;
            if (child !== null && (!operand.isStructuralTraversalSafe() || !child.isStructuralTraversalSafe()))
                status = RaySolidQueryStatus.Unsupported;
            else if (child !== null) {
                const placement = operand.queryAffinePlacement(forward, inverse);
                if (placement === AffinePlacementStatus.Invalid) status = RaySolidQueryStatus.InvalidInput;
                else if (placement === AffinePlacementStatus.Unsupported) status = RaySolidQueryStatus.Unsupported;
                else if (placement !== AffinePlacementStatus.Valid) status = RaySolidQueryStatus.Indeterminate;
                else {
                    const origin = inverse[0]!.multiply(ray.getOrigin());
                    const direction = inverse[0]!.withoutTranslation().multiply(ray.getDirection());
                    if (
                        !Double.isFinite(origin.x()) ||
                        !Double.isFinite(origin.y()) ||
                        !Double.isFinite(origin.z()) ||
                        !Double.isFinite(direction.x()) ||
                        !Double.isFinite(direction.y()) ||
                        !Double.isFinite(direction.z()) ||
                        (direction.x() === 0 && direction.y() === 0 && direction.z() === 0)
                    )
                        status = RaySolidQueryStatus.Indeterminate;
                    else
                        status = CsgRayLeafQuery.collect(
                            child,
                            Ray.fromAffine(origin, direction, ray.getT()),
                            query,
                            path,
                            result,
                        );
                }
            } else {
                const leaf = result.acquire();
                if (leaf === null) status = RaySolidQueryStatus.Indeterminate;
                else status = operand.classifyRay(ray, query, leaf.getClassification());
                if (status === RaySolidQueryStatus.Supported) {
                    const classification = leaf!.getClassification();
                    if (classification.insideSegments.size() !== classification.boundaries.size() + 1)
                        status = RaySolidQueryStatus.Indeterminate;
                    for (
                        let k = 0;
                        status === RaySolidQueryStatus.Supported && k < classification.boundaries.size();
                        ++k
                    ) {
                        const t = classification.boundaries.get(k).t;
                        if (
                            classification.boundaries.get(k).geometry === null ||
                            !Double.isFinite(t) ||
                            t < query.minT ||
                            t > query.maxT ||
                            (k > 0 && t < classification.boundaries.get(k - 1).t) ||
                            classification.insideSegments.get(k) === classification.insideSegments.get(k + 1)
                        )
                            status = RaySolidQueryStatus.Indeterminate;
                    }
                }
            }
            if (status !== RaySolidQueryStatus.Supported) {
                path.remove(path.size() - 1);
                return status;
            }
            for (let j = first; j < result.size(); ++j) {
                const leaf = result.get(j)!;
                if (
                    !leaf.prependOperand(operand) ||
                    (child !== null && !leaf.appendPlacement(operand, forward[0]!, inverse[0]!))
                ) {
                    path.remove(path.size() - 1);
                    return RaySolidQueryStatus.Indeterminate;
                }
                if (operation === CsgOperation.DIFFERENCE && i > 0) leaf.reverseOrientation();
            }
        }
        path.remove(path.size() - 1);
        return RaySolidQueryStatus.Supported;
    }
}
