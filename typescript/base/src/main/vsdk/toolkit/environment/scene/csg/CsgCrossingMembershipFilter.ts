import { Double } from "../../../../../java/lang/Double.js";
import { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import type { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { GeometryContainment } from "../../geometry/element/GeometryContainment.js";
import { Ray } from "../../geometry/element/Ray.js";
import type { RayCrossingQuery } from "../../geometry/element/RayCrossingQuery.js";
import { RaySolidQueryStatus } from "../../geometry/element/RaySolidQueryStatus.js";
import { AffinePlacementStatus } from "../AffinePlacementStatus.js";
import type { CsgExpression } from "./CsgExpression.js";
import { CsgMembershipRule } from "./CsgMembershipRule.js";
import { CsgMembershipTest } from "./CsgMembershipTest.js";
import type { CsgOperandView } from "./CsgOperandView.js";
import { CsgPointClassification } from "./CsgPointClassification.js";
import { CsgPointWorkspace } from "./CsgPointWorkspace.js";
import { CsgRayEventReference } from "./CsgRayEventReference.js";
import type { CsgRayLeafBatch } from "./CsgRayLeafBatch.js";
import { CsgRayLeafQuery } from "./CsgRayLeafQuery.js";
import type { CsgRaySurfaceEvents } from "./CsgRaySurfaceEvents.js";

// The POV-Ray 1.0 CSG heuristic: leaf boundary events are filtered by the
// membership of the event point in sibling operands. A union keeps every
// operand event, so internal surfaces remain. An intersection keeps an event
// of operand i when every other operand contains the point. A difference keeps
// a minuend event when no subtrahend contains it, and a subtrahend event when
// the minuend contains it and no other subtrahend does. Membership is
// measured in the coordinates of the node whose operands are tested, with the
// test chosen by setMembershipTest (see CsgMembershipTest): by default the
// sign of each primitive at the point, as POV-Ray 1.0 does, and otherwise the
// certified classification of the tolerance ball (also the fallback when the
// sign cannot be taken). Boundary counts as contained for an intersection
// operand and for the complement of a subtrahend, as in POV. An event is kept
// only when membership is decided and dropped when one sibling excludes it;
// otherwise the query fails with that sibling's status.
export class CsgCrossingMembershipFilter {
    private static membershipTest: CsgMembershipTest = CsgMembershipTest.SIGN;
    // Deepest nesting of expressions that the sign test follows
    private static readonly MAX_SIGN_DEPTH = 64;

    // SIGN by default
    public static setMembershipTest(test: CsgMembershipTest): void {
        CsgCrossingMembershipFilter.membershipTest = test;
    }

    public static getMembershipTest(): CsgMembershipTest {
        return CsgCrossingMembershipFilter.membershipTest;
    }

    // POV-Ray 1.0 inside test of an operand: Inside, Outside or Boundary, or
    // Indeterminate when it cannot decide (the certified test is used then).
    private static signContainment(operand: CsgOperandView, point: Vector3Dd, depth: number): GeometryContainment {
        if (depth > CsgCrossingMembershipFilter.MAX_SIGN_DEPTH) return GeometryContainment.Indeterminate;
        const forward: Matrix4x4d[] = [new Matrix4x4d()];
        const inverse: Matrix4x4d[] = [new Matrix4x4d()];
        if (operand.queryAffinePlacement(forward, inverse) !== AffinePlacementStatus.Valid)
            return GeometryContainment.Indeterminate;
        const local = inverse[0]!.multiply(point);
        const child = operand.getChildExpression();
        if (child === null) {
            const geometry = operand.getGeometry();
            return geometry !== null ? geometry.classifyPoint(local, 0) : GeometryContainment.Indeterminate;
        }
        const operation = child.getOperation();
        let inside = false;
        for (let j = 0; j < child.getOperandCount(); ++j) {
            if (j > 0 && CsgMembershipRule.isDecided(operation, inside)) break;
            const next = child.getOperand(j);
            if (next === null) return GeometryContainment.Indeterminate;
            const c = CsgCrossingMembershipFilter.signContainment(next, local, depth + 1);
            if (
                c !== GeometryContainment.Inside &&
                c !== GeometryContainment.Outside &&
                c !== GeometryContainment.Boundary
            )
                return GeometryContainment.Indeterminate;
            const operandInside = c !== GeometryContainment.Outside;
            inside = j === 0 ? operandInside : CsgMembershipRule.combine(operation, inside, operandInside);
        }
        return inside ? GeometryContainment.Inside : GeometryContainment.Outside;
    }

    // The membership of a sibling operand under the configured test.
    private static classifySibling(
        sibling: CsgOperandView,
        point: Vector3Dd,
        tolerance: number,
        workspace: CsgPointWorkspace,
    ): GeometryContainment {
        if (CsgCrossingMembershipFilter.membershipTest === CsgMembershipTest.SIGN) {
            const c = CsgCrossingMembershipFilter.signContainment(sibling, point, 0);
            if (
                c === GeometryContainment.Inside ||
                c === GeometryContainment.Outside ||
                c === GeometryContainment.Boundary
            )
                return c;
        }
        return CsgPointClassification.classifyOperandContainment(sibling, point, tolerance, workspace);
    }

    private static toStatus(containment: GeometryContainment): RaySolidQueryStatus {
        if (containment === GeometryContainment.InvalidInput) return RaySolidQueryStatus.InvalidInput;
        if (containment === GeometryContainment.Unsupported) return RaySolidQueryStatus.Unsupported;
        return RaySolidQueryStatus.Indeterminate;
    }

    // Certified world classification of a single operand in its parent space.
    public static classifyOperand(operand: CsgOperandView, point: Vector3Dd, tolerance: number): GeometryContainment {
        const workspace = new CsgPointWorkspace();
        return CsgPointClassification.classifyOperandContainment(operand, point, tolerance, workspace);
    }

    public static select(
        expression: CsgExpression,
        ray: Ray,
        query: RayCrossingQuery,
        tolerance: number,
        result: CsgRaySurfaceEvents,
    ): RaySolidQueryStatus;
    // The same selection with caller-owned scratch storage: after warm-up it
    // allocates nothing beyond the leaf queries.
    public static select(
        expression: CsgExpression,
        ray: Ray,
        query: RayCrossingQuery,
        tolerance: number,
        workspace: CsgPointWorkspace,
        result: CsgRaySurfaceEvents,
    ): RaySolidQueryStatus;
    public static select(
        expression: CsgExpression,
        ray: Ray,
        query: RayCrossingQuery,
        tolerance: number,
        workspaceOrResult: CsgPointWorkspace | CsgRaySurfaceEvents,
        output?: CsgRaySurfaceEvents,
    ): RaySolidQueryStatus {
        if (!(workspaceOrResult instanceof CsgPointWorkspace)) {
            const workspace = new CsgPointWorkspace();
            return CsgCrossingMembershipFilter.select(expression, ray, query, tolerance, workspace, workspaceOrResult);
        }
        const workspace = workspaceOrResult;
        const result = output!;
        result.clear();
        workspace.clear();
        if (!Double.isFinite(tolerance) || tolerance < 0) return RaySolidQueryStatus.InvalidInput;
        let status = CsgRayLeafQuery.query(expression, ray, query, workspace.ray, result.getLeaves());
        const leafIndex: number[] = [0];
        if (status === RaySolidQueryStatus.Supported)
            status = CsgCrossingMembershipFilter.selectEvents(
                expression,
                ray,
                tolerance,
                result.getLeaves(),
                leafIndex,
                workspace,
            );
        for (let i = 0; status === RaySolidQueryStatus.Supported && i < workspace.events.size(); ++i)
            if (!result.add(workspace.events.get(i))) status = RaySolidQueryStatus.Indeterminate;
        if (status === RaySolidQueryStatus.Supported && !result.getVersions().capture(expression))
            status = RaySolidQueryStatus.Indeterminate;
        if (status !== RaySolidQueryStatus.Supported) result.clear();
        return status;
    }

    private static decide(
        expression: CsgExpression,
        operandIndex: number,
        point: Vector3Dd,
        tolerance: number,
        workspace: CsgPointWorkspace,
        keep: boolean[],
    ): RaySolidQueryStatus {
        const operation = expression.getOperation();
        let excluded = false;
        let uncertain = RaySolidQueryStatus.Supported;
        for (let j = 0; j < expression.getOperandCount() && !excluded; ++j) {
            if (j === operandIndex) continue;
            const mustContain = CsgMembershipRule.requiresContainment(operation, operandIndex, j);
            const sibling = expression.getOperand(j);
            const c =
                sibling !== null
                    ? CsgCrossingMembershipFilter.classifySibling(sibling, point, tolerance, workspace)
                    : GeometryContainment.InvalidInput;
            if (c === GeometryContainment.InvalidInput) return RaySolidQueryStatus.InvalidInput;
            if (c === GeometryContainment.Boundary) continue;
            if (c === GeometryContainment.Inside) {
                excluded = !mustContain;
                continue;
            }
            if (c === GeometryContainment.Outside) {
                excluded = mustContain;
                continue;
            }
            if (uncertain === RaySolidQueryStatus.Supported) uncertain = CsgCrossingMembershipFilter.toStatus(c);
        }
        keep[0] = false;
        if (excluded) return RaySolidQueryStatus.Supported;
        if (uncertain !== RaySolidQueryStatus.Supported) return uncertain;
        keep[0] = true;
        return RaySolidQueryStatus.Supported;
    }

    // Appends the kept events of the expression to the workspace event stack.
    // (C++ private overload `select`.)
    private static selectEvents(
        expression: CsgExpression,
        ray: Ray,
        tolerance: number,
        leaves: CsgRayLeafBatch,
        leafIndex: number[],
        workspace: CsgPointWorkspace,
    ): RaySolidQueryStatus {
        const operation = expression.getOperation();
        const events = workspace.events;
        for (let i = 0; i < expression.getOperandCount(); ++i) {
            const operand = expression.getOperand(i)!;
            const child = operand.getChildExpression();
            // This operand's events occupy the stack from `first` on.
            const first = events.size();
            if (child !== null) {
                // The leaf query has already validated this placement.
                const forward: Matrix4x4d[] = [new Matrix4x4d()];
                const inverse: Matrix4x4d[] = [new Matrix4x4d()];
                if (operand.queryAffinePlacement(forward, inverse) !== AffinePlacementStatus.Valid)
                    return RaySolidQueryStatus.Indeterminate;
                const childRay = Ray.fromAffine(
                    inverse[0]!.multiply(ray.getOrigin()),
                    inverse[0]!.withoutTranslation().multiply(ray.getDirection()),
                    ray.getT(),
                );
                const status = CsgCrossingMembershipFilter.selectEvents(
                    child,
                    childRay,
                    tolerance,
                    leaves,
                    leafIndex,
                    workspace,
                );
                if (status !== RaySolidQueryStatus.Supported) return status;
            } else {
                const leaf = leaves.get(leafIndex[0]!);
                if (leaf === null) return RaySolidQueryStatus.Indeterminate;
                for (let k = 0; k < leaf.getClassification().boundaries.size(); ++k)
                    if (!events.add(new CsgRayEventReference(leafIndex[0]!, k)))
                        return RaySolidQueryStatus.Indeterminate;
                leafIndex[0] = leafIndex[0]! + 1;
            }
            if (!CsgMembershipRule.filtersEvents(operation)) continue;
            // Keep the selected events in order, compacting them in place.
            let kept = first;
            const last = events.size();
            for (let k = first; k < last; ++k) {
                const keep: boolean[] = [true];
                const event = new CsgRayEventReference(events.get(k));
                const t = leaves.get(event.leafIndex)!.getClassification().boundaries.get(event.boundaryIndex).t;
                const point = ray.getOrigin().add(ray.getDirection().multiply(t));
                const status = CsgCrossingMembershipFilter.decide(expression, i, point, tolerance, workspace, keep);
                if (status !== RaySolidQueryStatus.Supported) return status;
                if (keep[0]) events.set(kept++, event);
            }
            while (events.size() > kept) events.remove(events.size() - 1);
        }
        return RaySolidQueryStatus.Supported;
    }
}
