import type { ArrayList } from "../../../../../java/util/ArrayList.js";
import { RayBoundary } from "../../geometry/element/RayBoundary.js";
import type { Ray } from "../../geometry/element/Ray.js";
import type { RayCrossingQuery } from "../../geometry/element/RayCrossingQuery.js";
import { RaySolidQueryStatus } from "../../geometry/element/RaySolidQueryStatus.js";
import type { CsgExpression } from "./CsgExpression.js";
import { CsgMembershipRule } from "./CsgMembershipRule.js";
import type { CsgRayClassification } from "./CsgRayClassification.js";
import { CsgRayEventQuery } from "./CsgRayEventQuery.js";
import { CsgRayLeafQuery } from "./CsgRayLeafQuery.js";
import { CsgRayWorkspace } from "./CsgRayWorkspace.js";

// [ROTH1982] section 3.3 ray classification: every leaf is classified as IN/OUT
// segments and the segments are combined through the expression with the
// union, intersection and difference rows of Table 3. Events with exactly
// equal parameters are applied together before the combined state is read,
// so coincident faces and touching operands yield the regularized result and
// point contacts produce no boundary. ON neighbourhoods (section 6.1) are not
// modelled. Uncertain or unsupported leaves fail the whole query.
export class CsgRothRayClassifier {
    private static evaluate(expression: CsgExpression, states: ArrayList<boolean>, leafIndex: number[]): boolean {
        const operation = expression.getOperation();
        let result = false;
        for (let i = 0; i < expression.getOperandCount(); ++i) {
            const operand = expression.getOperand(i)!;
            const child = operand.getChildExpression();
            // Every operand is visited so that the leaf order stays aligned.
            const inside =
                child !== null ? CsgRothRayClassifier.evaluate(child, states, leafIndex) : states.get(leafIndex[0]!++);
            result = i === 0 ? inside : CsgMembershipRule.combine(operation, result, inside);
        }
        return result;
    }

    // Membership of the expression for given per-leaf states, in the leaf order
    // of CsgRayLeafQuery. The expression must be the one that produced them.
    public static isInside(expression: CsgExpression, states: ArrayList<boolean>): boolean {
        const leafIndex: number[] = [0];
        return CsgRothRayClassifier.evaluate(expression, states, leafIndex);
    }

    public static classify(
        expression: CsgExpression,
        ray: Ray,
        query: RayCrossingQuery,
        result: CsgRayClassification,
    ): RaySolidQueryStatus;
    // Same query with caller-owned scratch storage; with a reused workspace
    // and result it allocates only inside leaf classification.
    public static classify(
        expression: CsgExpression,
        ray: Ray,
        query: RayCrossingQuery,
        workspace: CsgRayWorkspace,
        result: CsgRayClassification,
    ): RaySolidQueryStatus;
    public static classify(
        expression: CsgExpression,
        ray: Ray,
        query: RayCrossingQuery,
        workspaceOrResult: CsgRayWorkspace | CsgRayClassification,
        output?: CsgRayClassification,
    ): RaySolidQueryStatus {
        if (!(workspaceOrResult instanceof CsgRayWorkspace)) {
            const workspace = new CsgRayWorkspace();
            return CsgRothRayClassifier.classify(expression, ray, query, workspace, workspaceOrResult);
        }
        const workspace = workspaceOrResult;
        const result = output!;
        result.clear();
        let status = CsgRayLeafQuery.query(expression, ray, query, workspace, result.getLeaves());
        if (status === RaySolidQueryStatus.Supported)
            status = CsgRayEventQuery.validate(result.getLeaves(), workspace.cursors);
        if (status === RaySolidQueryStatus.Supported)
            status = CsgRothRayClassifier.combine(expression, workspace, result);
        if (status === RaySolidQueryStatus.Supported && !result.getVersions().capture(expression))
            status = RaySolidQueryStatus.Indeterminate;
        if (status !== RaySolidQueryStatus.Supported) result.clear();
        return status;
    }

    private static combine(
        expression: CsgExpression,
        workspace: CsgRayWorkspace,
        result: CsgRayClassification,
    ): RaySolidQueryStatus {
        const leaves = result.getLeaves();
        const states = workspace.states;
        const previous = workspace.previous;
        const trial = workspace.trial;
        states.clear();
        previous.clear();
        trial.clear();
        for (let i = 0; i < leaves.size(); ++i) {
            const initial = leaves.get(i)!.getClassification().insideSegments.get(0);
            if (!states.add(initial) || !previous.add(initial) || !trial.add(initial))
                return RaySolidQueryStatus.Indeterminate;
        }
        const solid = result.getSolid();
        let inside = CsgRothRayClassifier.isInside(expression, states);
        if (!solid.insideSegments.add(inside)) return RaySolidQueryStatus.Indeterminate;
        const t: number[] = [0];
        const group = workspace.group;
        while (CsgRayEventQuery.nextGroup(leaves, workspace.cursors, t, group)) {
            for (let i = 0; i < states.size(); ++i) previous.set(i, states.get(i));
            for (let k = 0; k < group.size(); ++k)
                states.set(
                    group.get(k).leafIndex,
                    leaves
                        .get(group.get(k).leafIndex)!
                        .getClassification()
                        .insideSegments.get(group.get(k).boundaryIndex + 1),
                );
            const next = CsgRothRayClassifier.isInside(expression, states);
            if (next === inside) continue;
            // Owners are the leaves whose transition in this group, applied alone
            // to the previous states, already yields the new membership. When no
            // single leaf suffices (e.g. coincident faces of an intersection), the
            // owners are all changed leaves oriented like the combined transition.
            if (!result.beginOwners()) return RaySolidQueryStatus.Indeterminate;
            let ownerCount = 0;
            let source = 0;
            for (let pass = 0; pass < 2 && ownerCount === 0; ++pass) {
                for (let k = 0; k < group.size(); ++k) {
                    const leaf = group.get(k).leafIndex;
                    if (k + 1 < group.size() && group.get(k + 1).leafIndex === leaf) continue;
                    if (states.get(leaf) === previous.get(leaf)) continue;
                    if (pass === 0) {
                        for (let i = 0; i < previous.size(); ++i) trial.set(i, previous.get(i));
                        trial.set(leaf, states.get(leaf));
                        if (CsgRothRayClassifier.isInside(expression, trial) !== next) continue;
                    } else if ((states.get(leaf) !== leaves.get(leaf)!.getPath().isReversed()) !== next) continue;
                    if (!result.addOwner(group.get(k))) return RaySolidQueryStatus.Indeterminate;
                    if (ownerCount++ === 0) source = k;
                }
            }
            const reference = group.get(source);
            // Append an empty boundary and assign into it, so the slot's
            // provenance storage is reused instead of copied by value.
            if (!solid.boundaries.add(new RayBoundary()) || !solid.insideSegments.add(next))
                return RaySolidQueryStatus.Indeterminate;
            solid.boundaries
                .get(solid.boundaries.size() - 1)
                .assign(leaves.get(reference.leafIndex)!.getClassification().boundaries.get(reference.boundaryIndex));
            inside = next;
        }
        return RaySolidQueryStatus.Supported;
    }
}
