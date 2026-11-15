import { Double } from "../../../../../java/lang/Double.js";
import { ArrayList } from "../../../../../java/util/ArrayList.js";
import { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { GeometryBounds } from "../../geometry/element/GeometryBounds.js";
import { GeometryContainment } from "../../geometry/element/GeometryContainment.js";
import { AffinePlacementStatus } from "../AffinePlacementStatus.js";
import { AffineToleranceMap } from "../AffineToleranceMap.js";
import type { CsgExpression } from "./CsgExpression.js";
import type { CsgOperandView } from "./CsgOperandView.js";
import { CsgOperation } from "./CsgOperation.js";
import { CsgPointAttribution } from "./CsgPointAttribution.js";
import { CsgPointCandidate } from "./CsgPointCandidate.js";
import { CsgPointEvidence } from "./CsgPointEvidence.js";
import { CsgPointProof } from "./CsgPointProof.js";
import { CsgPointWitness } from "./CsgPointWitness.js";
import type { CsgPointWorkspace } from "./CsgPointWorkspace.js";

// Conservative world-space classification. An operand boundary becomes a
// composite boundary only when every other operand is certified non-interfering.
export class CsgPointClassification {
    public static classify(expression: CsgExpression, point: Vector3Dd, tolerance: number): GeometryContainment {
        const ignored = new CsgPointWitness();
        return CsgPointClassification.classifyWithWitness(expression, point, tolerance, ignored);
    }

    // Witness is cleared when no single operand proves the returned result.
    public static classifyWithWitness(
        expression: CsgExpression,
        point: Vector3Dd,
        tolerance: number,
        witness: CsgPointWitness,
    ): GeometryContainment {
        witness.clear();
        if (
            !Double.isFinite(point.x()) ||
            !Double.isFinite(point.y()) ||
            !Double.isFinite(point.z()) ||
            !Double.isFinite(tolerance) ||
            tolerance < 0
        )
            return GeometryContainment.InvalidInput;
        const path = new ArrayList<CsgExpression>();
        return CsgPointClassification.classifyExpression(expression, point, tolerance, path, witness, null, null);
    }

    // Collect sufficient leaf facts, including cases requiring several operands.
    public static classifyWithProof(
        expression: CsgExpression,
        point: Vector3Dd,
        tolerance: number,
        proof: CsgPointProof,
    ): GeometryContainment {
        proof.clear();
        if (
            !Double.isFinite(point.x()) ||
            !Double.isFinite(point.y()) ||
            !Double.isFinite(point.z()) ||
            !Double.isFinite(tolerance) ||
            tolerance < 0
        )
            return GeometryContainment.InvalidInput;
        const path = new ArrayList<CsgExpression>();
        const result = CsgPointClassification.classifyExpression(expression, point, tolerance, path, null, proof, null);
        if (proof.isAvailable() && !proof.getVersions().capture(expression)) proof.clear();
        return result;
    }

    // All attributable owners are exposed only when the complete set is known.
    public static classifyWithAttribution(
        expression: CsgExpression,
        point: Vector3Dd,
        tolerance: number,
        attribution: CsgPointAttribution,
    ): GeometryContainment {
        attribution.clear();
        if (
            !Double.isFinite(point.x()) ||
            !Double.isFinite(point.y()) ||
            !Double.isFinite(point.z()) ||
            !Double.isFinite(tolerance) ||
            tolerance < 0
        )
            return GeometryContainment.InvalidInput;
        const path = new ArrayList<CsgExpression>();
        const result = CsgPointClassification.classifyExpression(
            expression,
            point,
            tolerance,
            path,
            null,
            null,
            attribution,
        );
        if (attribution.isComplete() && !attribution.getVersions().capture(expression)) attribution.clear();
        return result;
    }

    private static classifyExpression(
        expression: CsgExpression,
        point: Vector3Dd,
        tolerance: number,
        path: ArrayList<CsgExpression>,
        witness: CsgPointWitness | null,
        proof: CsgPointProof | null,
        attribution: CsgPointAttribution | null,
    ): GeometryContainment {
        if (path.contains(expression)) return GeometryContainment.InvalidInput;
        const count = expression.getOperandCount();
        const operation = expression.getOperation();
        if (
            count <= 0 ||
            (operation !== CsgOperation.UNION &&
                operation !== CsgOperation.INTERSECTION &&
                operation !== CsgOperation.DIFFERENCE)
        )
            return GeometryContainment.InvalidInput;
        if (!path.add(expression)) return GeometryContainment.Indeterminate;
        let hasInside = false;
        let hasOutside = false;
        let boundaryCount = 0;
        let hasUnsupported = false;
        let hasIndeterminate = false;
        let hasInvalid = false;
        let first = GeometryContainment.Indeterminate;
        const firstWitness = new CsgPointWitness();
        const decisiveWitness = new CsgPointWitness();
        const boundaryWitness = new CsgPointWitness();
        const firstProof = new CsgPointProof();
        const decisiveProof = new CsgPointProof();
        const allProof = new CsgPointProof();
        const selfProof = new CsgPointProof();
        const firstAttribution = new CsgPointAttribution();
        const boundaryAttribution = new CsgPointAttribution();
        allProof.begin();
        boundaryAttribution.begin();
        const priorResults = new ArrayList<GeometryContainment>();
        let subtractsSelf = false;
        let hasConflict = false;
        const candidates = new ArrayList<CsgPointCandidate>();
        for (let i = 0; i < count; ++i) {
            const operand = expression.getOperand(i);
            let result = GeometryContainment.InvalidInput;
            const candidate = new CsgPointWitness();
            const candidateProof = new CsgPointProof();
            const candidateAttribution = new CsgPointAttribution();
            if (operand !== null) {
                const child = operand.getChildExpression();
                if (child !== null && (!operand.isStructuralTraversalSafe() || !child.isStructuralTraversalSafe())) {
                    result = GeometryContainment.Unsupported;
                } else if (child !== null) {
                    const forward: Matrix4x4d[] = [new Matrix4x4d()];
                    const inverse: Matrix4x4d[] = [new Matrix4x4d()];
                    const placement = operand.queryAffinePlacement(forward, inverse);
                    if (placement === AffinePlacementStatus.Invalid) result = GeometryContainment.InvalidInput;
                    else if (placement === AffinePlacementStatus.Unsupported) result = GeometryContainment.Unsupported;
                    else if (placement !== AffinePlacementStatus.Valid) result = GeometryContainment.Indeterminate;
                    else {
                        let identity = true;
                        for (let row = 0; row < 4; ++row)
                            for (let column = 0; column < 4; ++column)
                                if (forward[0]!.get(row, column) !== (row === column ? 1.0 : 0.0)) identity = false;
                        if (identity)
                            result = CsgPointClassification.classifyExpression(
                                child,
                                point,
                                tolerance,
                                path,
                                candidate,
                                candidateProof,
                                candidateAttribution,
                            );
                        else {
                            const childPoint: Vector3Dd[] = [new Vector3Dd()];
                            const childTolerance: number[] = [0];
                            const childInnerTolerance: number[] = [-1];
                            const mapped = AffineToleranceMap.map(
                                forward[0]!,
                                point,
                                tolerance,
                                childPoint,
                                childTolerance,
                                childInnerTolerance,
                            );
                            if (mapped === AffinePlacementStatus.Invalid) result = GeometryContainment.InvalidInput;
                            else if (mapped === AffinePlacementStatus.Unsupported)
                                result = GeometryContainment.Unsupported;
                            else if (mapped !== AffinePlacementStatus.Valid) result = GeometryContainment.Indeterminate;
                            else {
                                result = CsgPointClassification.classifyExpression(
                                    child,
                                    childPoint[0]!,
                                    childTolerance[0]!,
                                    path,
                                    candidate,
                                    candidateProof,
                                    candidateAttribution,
                                );
                                // An outer-ball boundary is insufficient. A boundary
                                // within the certified inner ball is also within the
                                // original parent-space tolerance ball.
                                if (
                                    result === GeometryContainment.Boundary ||
                                    result === GeometryContainment.Indeterminate
                                ) {
                                    candidate.clear();
                                    candidateProof.clear();
                                    candidateAttribution.clear();
                                    if (childInnerTolerance[0]! > 0) {
                                        const innerWitness = new CsgPointWitness();
                                        const innerProof = new CsgPointProof();
                                        const innerAttribution = new CsgPointAttribution();
                                        const inner = CsgPointClassification.classifyExpression(
                                            child,
                                            childPoint[0]!,
                                            childInnerTolerance[0]!,
                                            path,
                                            innerWitness,
                                            innerProof,
                                            innerAttribution,
                                        );
                                        if (inner === GeometryContainment.Boundary) {
                                            result = inner;
                                            candidate.assign(innerWitness);
                                            candidateProof.assign(innerProof);
                                            candidateAttribution.assign(innerAttribution);
                                        } else if (
                                            inner === GeometryContainment.InvalidInput ||
                                            inner === GeometryContainment.Unsupported
                                        )
                                            result = inner;
                                        else result = GeometryContainment.Indeterminate;
                                    } else result = GeometryContainment.Indeterminate;
                                }
                            }
                        }
                    }
                } else {
                    // Conservative culling: a solid whose certified bounds miss the
                    // tolerance ball is outside; uncertain bounds never cull.
                    const bounds = new GeometryBounds();
                    operand.getSolidBounds(bounds);
                    result = bounds.excludesBall(point, tolerance)
                        ? GeometryContainment.Outside
                        : operand.classifyPointCertified(point, tolerance);
                    if (
                        result === GeometryContainment.Inside ||
                        result === GeometryContainment.Outside ||
                        result === GeometryContainment.Boundary
                    ) {
                        if (candidate.prepend(operand)) {
                            candidateProof.begin();
                            candidateProof.add(new CsgPointEvidence(candidate, result, point, tolerance));
                            if (result === GeometryContainment.Boundary) {
                                candidateAttribution.begin();
                                candidateAttribution.add(candidate);
                            }
                        }
                    }
                }
                if (child !== null && candidate.isAvailable() && !candidate.prepend(operand)) candidate.clear();
                if (child !== null && candidateProof.isAvailable() && !candidateProof.prepend(operand))
                    candidateProof.clear();
                if (child !== null && candidateAttribution.isComplete() && !candidateAttribution.prepend(operand))
                    candidateAttribution.clear();
            }
            if (
                result !== GeometryContainment.Inside &&
                result !== GeometryContainment.Outside &&
                result !== GeometryContainment.Boundary &&
                result !== GeometryContainment.Unsupported &&
                result !== GeometryContainment.Indeterminate &&
                result !== GeometryContainment.InvalidInput
            )
                result = GeometryContainment.InvalidInput;
            if (
                result !== GeometryContainment.Inside &&
                result !== GeometryContainment.Outside &&
                result !== GeometryContainment.Boundary
            ) {
                candidate.clear();
                candidateProof.clear();
                candidateAttribution.clear();
            }
            const stored = new CsgPointCandidate();
            stored.result = result;
            stored.witness.assign(candidate);
            stored.proof.assign(candidateProof);
            stored.attribution.assign(candidateAttribution);
            if (!candidates.add(stored)) {
                path.remove(path.size() - 1);
                return GeometryContainment.Indeterminate;
            }
        }
        // A certified sibling can supply a classification for an equivalent view.
        // A composite recipient does not inherit leaf attribution: its owner paths
        // must be independently resolved. Invalid or opaque views are never used.
        for (let i = 0; i < count; ++i) {
            const target = candidates.get(i);
            const destination = expression.getOperand(i);
            if (
                (target.result !== GeometryContainment.Unsupported &&
                    target.result !== GeometryContainment.Indeterminate) ||
                destination === null
            )
                continue;
            const destinationChild = destination.getChildExpression();
            if (
                destinationChild !== null
                    ? !destination.isStructuralTraversalSafe() || !destinationChild.isStructuralTraversalSafe()
                    : destination.getGeometry() === null
            )
                continue;
            for (let j = 0; j < count; ++j) {
                const source = expression.getOperand(j);
                const known = candidates.get(j);
                if (
                    i === j ||
                    source === null ||
                    (known.result !== GeometryContainment.Inside &&
                        known.result !== GeometryContainment.Outside &&
                        known.result !== GeometryContainment.Boundary) ||
                    !known.proof.isAvailable() ||
                    !destination.isSameSolid(source) ||
                    !source.isSameSolid(destination)
                )
                    continue;
                const sourceChild = source.getChildExpression();
                if (
                    sourceChild !== null
                        ? !source.isStructuralTraversalSafe() || !sourceChild.isStructuralTraversalSafe()
                        : source.getGeometry() === null
                )
                    continue;
                const sourceView = new CsgPointWitness();
                const recipient = new CsgPointWitness();
                if (!sourceView.prepend(source) || !recipient.prepend(destination)) break;
                target.proof.assign(known.proof);
                if (!target.proof.add(new CsgPointEvidence(sourceView, recipient, point, tolerance))) {
                    target.proof.clear();
                    break;
                }
                target.result = known.result;
                if (destinationChild === null) {
                    target.witness.assign(recipient);
                    if (target.result === GeometryContainment.Boundary) {
                        target.attribution.begin();
                        target.attribution.add(recipient);
                    }
                }
                break;
            }
        }
        for (let i = 0; i < count; ++i) {
            const operand = expression.getOperand(i);
            let result = candidates.get(i).result;
            const candidate = new CsgPointWitness(candidates.get(i).witness);
            const candidateProof = new CsgPointProof(candidates.get(i).proof);
            const candidateAttribution = new CsgPointAttribution(candidates.get(i).attribution);
            if (i === 0) {
                first = result;
                firstWitness.assign(candidate);
                firstProof.assign(candidateProof);
                firstAttribution.assign(candidateAttribution);
            }
            let samePriorBoundary = false;
            let matchesFirst = false;
            for (let j = 0; j < i; ++j) {
                const earlier = expression.getOperand(j);
                if (
                    operand !== null &&
                    earlier !== null &&
                    operand.isSameSolid(earlier) &&
                    earlier.isSameSolid(operand)
                ) {
                    if (priorResults.get(j) !== result) hasConflict = true;
                    if (result === GeometryContainment.Boundary && priorResults.get(j) === result)
                        samePriorBoundary = true;
                    if (
                        operation === CsgOperation.DIFFERENCE &&
                        j === 0 &&
                        priorResults.get(j) === result &&
                        (result === GeometryContainment.Inside ||
                            result === GeometryContainment.Outside ||
                            result === GeometryContainment.Boundary)
                    ) {
                        subtractsSelf = true;
                        matchesFirst = true;
                    }
                }
            }
            if (!priorResults.add(result)) {
                path.remove(path.size() - 1);
                return GeometryContainment.Indeterminate;
            }
            if (operation === CsgOperation.DIFFERENCE && i > 0) {
                if (result === GeometryContainment.Inside) result = GeometryContainment.Outside;
                else if (result === GeometryContainment.Outside) result = GeometryContainment.Inside;
                candidate.reverseOrientation();
                candidateProof.reverseOrientation();
                candidateAttribution.reverseOrientation();
            }
            if (matchesFirst && firstProof.isAvailable() && candidateProof.isAvailable() && !selfProof.isAvailable()) {
                selfProof.assign(firstProof);
                if (!selfProof.append(candidateProof)) selfProof.clear();
            }
            if (
                !decisiveProof.isAvailable() &&
                candidateProof.isAvailable() &&
                ((operation === CsgOperation.UNION && result === GeometryContainment.Inside) ||
                    (operation !== CsgOperation.UNION && result === GeometryContainment.Outside))
            )
                decisiveProof.assign(candidateProof);
            if (!allProof.append(candidateProof)) allProof.clear();
            if (
                !decisiveWitness.isAvailable() &&
                candidate.isAvailable() &&
                ((operation === CsgOperation.UNION && result === GeometryContainment.Inside) ||
                    (operation !== CsgOperation.UNION && result === GeometryContainment.Outside))
            )
                decisiveWitness.assign(candidate);
            hasInside ||= result === GeometryContainment.Inside;
            hasOutside ||= result === GeometryContainment.Outside;
            if (result === GeometryContainment.Boundary && !boundaryAttribution.append(candidateAttribution))
                boundaryAttribution.clear();
            if (result === GeometryContainment.Boundary && !samePriorBoundary) {
                ++boundaryCount;
                if (boundaryCount === 1) boundaryWitness.assign(candidate);
            } else if (
                result === GeometryContainment.Boundary &&
                boundaryCount === 1 &&
                !boundaryWitness.isAvailable() &&
                candidate.isAvailable()
            )
                boundaryWitness.assign(candidate);
            hasUnsupported ||= result === GeometryContainment.Unsupported;
            hasIndeterminate ||= result === GeometryContainment.Indeterminate;
            hasInvalid ||= result === GeometryContainment.InvalidInput;
        }
        path.remove(path.size() - 1);
        if (hasInvalid) return GeometryContainment.InvalidInput;
        if (hasConflict) return GeometryContainment.Indeterminate;
        if (subtractsSelf) {
            if (proof !== null && selfProof.isAvailable()) proof.assign(selfProof);
            return GeometryContainment.Outside;
        }
        if (count === 1) {
            if (witness !== null) witness.assign(firstWitness);
            if (proof !== null) proof.assign(firstProof);
            if (attribution !== null && first === GeometryContainment.Boundary) attribution.assign(firstAttribution);
            return first;
        }
        // A definite result requires a full tolerance ball in or out of the solid.
        // A boundary of one operand need not be a boundary of the composition.
        if (operation === CsgOperation.UNION && hasInside) {
            if (witness !== null) witness.assign(decisiveWitness);
            if (proof !== null) proof.assign(decisiveProof);
            return GeometryContainment.Inside;
        }
        if (operation !== CsgOperation.UNION && hasOutside) {
            if (witness !== null) witness.assign(decisiveWitness);
            if (proof !== null) proof.assign(decisiveProof);
            return GeometryContainment.Outside;
        }
        if (hasUnsupported) return GeometryContainment.Unsupported;
        if (hasIndeterminate) return GeometryContainment.Indeterminate;
        // With exactly one boundary and every other operand certified on the
        // non-interfering side throughout the tolerance ball, that operand's
        // boundary is exposed in the composite. Multiple boundaries can coincide
        // or hide one another, so their combination remains indeterminate.
        if (boundaryCount === 1) {
            if (witness !== null) witness.assign(boundaryWitness);
            if (proof !== null) proof.assign(allProof);
            if (attribution !== null) attribution.assign(boundaryAttribution);
            return GeometryContainment.Boundary;
        }
        if (boundaryCount > 1) return GeometryContainment.Indeterminate;
        if (proof !== null) proof.assign(allProof);
        return operation === CsgOperation.UNION ? GeometryContainment.Outside : GeometryContainment.Inside;
    }

    // The result of `classify` without collecting witnesses, proofs or
    // attributions, using the workspace's storage: it allocates nothing once
    // the workspace has grown to the expression. Equivalent operands share a
    // definite result as in `classify`.
    public static classifyContainment(
        expression: CsgExpression,
        point: Vector3Dd,
        tolerance: number,
        workspace: CsgPointWorkspace,
    ): GeometryContainment {
        if (
            !Double.isFinite(point.x()) ||
            !Double.isFinite(point.y()) ||
            !Double.isFinite(point.z()) ||
            !Double.isFinite(tolerance) ||
            tolerance < 0
        )
            return GeometryContainment.InvalidInput;
        workspace.path.clear();
        workspace.results.clear();
        return CsgPointClassification.containmentOfExpression(expression, point, tolerance, workspace);
    }

    // `classifyContainment` of a single operand in its parent's space.
    public static classifyOperandContainment(
        operand: CsgOperandView,
        point: Vector3Dd,
        tolerance: number,
        workspace: CsgPointWorkspace,
    ): GeometryContainment {
        if (
            !Double.isFinite(point.x()) ||
            !Double.isFinite(point.y()) ||
            !Double.isFinite(point.z()) ||
            !Double.isFinite(tolerance) ||
            tolerance < 0
        )
            return GeometryContainment.InvalidInput;
        workspace.path.clear();
        workspace.results.clear();
        return CsgPointClassification.containmentOfOperand(operand, point, tolerance, workspace);
    }

    private static truncate(workspace: CsgPointWorkspace, resultCount: number): void {
        while (workspace.results.size() > resultCount) workspace.results.remove(workspace.results.size() - 1);
    }

    // Mirrors the operand step of classifyExpression.
    private static containmentOfOperand(
        operand: CsgOperandView | null,
        point: Vector3Dd,
        tolerance: number,
        workspace: CsgPointWorkspace,
    ): GeometryContainment {
        if (operand === null) return GeometryContainment.InvalidInput;
        let result = GeometryContainment.InvalidInput;
        const child = operand.getChildExpression();
        if (child !== null && (!operand.isStructuralTraversalSafe() || !child.isStructuralTraversalSafe()))
            result = GeometryContainment.Unsupported;
        else if (child !== null) {
            const forward: Matrix4x4d[] = [new Matrix4x4d()];
            const inverse: Matrix4x4d[] = [new Matrix4x4d()];
            const placement = operand.queryAffinePlacement(forward, inverse);
            if (placement === AffinePlacementStatus.Invalid) result = GeometryContainment.InvalidInput;
            else if (placement === AffinePlacementStatus.Unsupported) result = GeometryContainment.Unsupported;
            else if (placement !== AffinePlacementStatus.Valid) result = GeometryContainment.Indeterminate;
            else {
                let identity = true;
                for (let row = 0; row < 4; ++row)
                    for (let column = 0; column < 4; ++column)
                        if (forward[0]!.get(row, column) !== (row === column ? 1.0 : 0.0)) identity = false;
                if (identity)
                    result = CsgPointClassification.containmentOfExpression(child, point, tolerance, workspace);
                else {
                    const childPoint: Vector3Dd[] = [new Vector3Dd()];
                    const childTolerance: number[] = [0];
                    const childInnerTolerance: number[] = [-1];
                    const mapped = AffineToleranceMap.map(
                        forward[0]!,
                        point,
                        tolerance,
                        childPoint,
                        childTolerance,
                        childInnerTolerance,
                    );
                    if (mapped === AffinePlacementStatus.Invalid) result = GeometryContainment.InvalidInput;
                    else if (mapped === AffinePlacementStatus.Unsupported) result = GeometryContainment.Unsupported;
                    else if (mapped !== AffinePlacementStatus.Valid) result = GeometryContainment.Indeterminate;
                    else {
                        result = CsgPointClassification.containmentOfExpression(
                            child,
                            childPoint[0]!,
                            childTolerance[0]!,
                            workspace,
                        );
                        // Only a boundary within the certified inner ball is a
                        // boundary within the parent-space tolerance ball.
                        if (result === GeometryContainment.Boundary || result === GeometryContainment.Indeterminate) {
                            if (childInnerTolerance[0]! > 0) {
                                const inner = CsgPointClassification.containmentOfExpression(
                                    child,
                                    childPoint[0]!,
                                    childInnerTolerance[0]!,
                                    workspace,
                                );
                                if (
                                    inner === GeometryContainment.Boundary ||
                                    inner === GeometryContainment.InvalidInput ||
                                    inner === GeometryContainment.Unsupported
                                )
                                    result = inner;
                                else result = GeometryContainment.Indeterminate;
                            } else result = GeometryContainment.Indeterminate;
                        }
                    }
                }
            }
        } else {
            const bounds = new GeometryBounds();
            operand.getSolidBounds(bounds);
            result = bounds.excludesBall(point, tolerance)
                ? GeometryContainment.Outside
                : operand.classifyPointCertified(point, tolerance);
        }
        if (
            result !== GeometryContainment.Inside &&
            result !== GeometryContainment.Outside &&
            result !== GeometryContainment.Boundary &&
            result !== GeometryContainment.Unsupported &&
            result !== GeometryContainment.Indeterminate &&
            result !== GeometryContainment.InvalidInput
        )
            result = GeometryContainment.InvalidInput;
        return result;
    }

    // Mirrors classifyExpression; a definite operand result is the condition under
    // which classifyExpression has a proof for it.
    private static containmentOfExpression(
        expression: CsgExpression,
        point: Vector3Dd,
        tolerance: number,
        workspace: CsgPointWorkspace,
    ): GeometryContainment {
        if (workspace.path.contains(expression)) return GeometryContainment.InvalidInput;
        const count = expression.getOperandCount();
        const operation = expression.getOperation();
        if (
            count <= 0 ||
            (operation !== CsgOperation.UNION &&
                operation !== CsgOperation.INTERSECTION &&
                operation !== CsgOperation.DIFFERENCE)
        )
            return GeometryContainment.InvalidInput;
        if (!workspace.path.add(expression)) return GeometryContainment.Indeterminate;
        const base = workspace.results.size();
        for (let i = 0; i < count; ++i) {
            const result = CsgPointClassification.containmentOfOperand(
                expression.getOperand(i),
                point,
                tolerance,
                workspace,
            );
            if (!workspace.results.add(result)) {
                CsgPointClassification.truncate(workspace, base);
                workspace.path.remove(workspace.path.size() - 1);
                return GeometryContainment.Indeterminate;
            }
        }
        const results = workspace.results;
        for (let i = 0; i < count; ++i) {
            const target = results.get(base + i);
            const destination = expression.getOperand(i);
            if (
                (target !== GeometryContainment.Unsupported && target !== GeometryContainment.Indeterminate) ||
                destination === null
            )
                continue;
            const destinationChild = destination.getChildExpression();
            if (
                destinationChild !== null
                    ? !destination.isStructuralTraversalSafe() || !destinationChild.isStructuralTraversalSafe()
                    : destination.getGeometry() === null
            )
                continue;
            for (let j = 0; j < count; ++j) {
                const source = expression.getOperand(j);
                const known = results.get(base + j);
                if (
                    i === j ||
                    source === null ||
                    (known !== GeometryContainment.Inside &&
                        known !== GeometryContainment.Outside &&
                        known !== GeometryContainment.Boundary) ||
                    !destination.isSameSolid(source) ||
                    !source.isSameSolid(destination)
                )
                    continue;
                const sourceChild = source.getChildExpression();
                if (
                    sourceChild !== null
                        ? !source.isStructuralTraversalSafe() || !sourceChild.isStructuralTraversalSafe()
                        : source.getGeometry() === null
                )
                    continue;
                results.set(base + i, known);
                break;
            }
        }
        let hasInside = false;
        let hasOutside = false;
        let hasUnsupported = false;
        let hasIndeterminate = false;
        let hasInvalid = false;
        let subtractsSelf = false;
        let hasConflict = false;
        let boundaryCount = 0;
        const first = results.get(base);
        for (let i = 0; i < count; ++i) {
            const operand = expression.getOperand(i);
            let result = results.get(base + i);
            let samePriorBoundary = false;
            for (let j = 0; j < i; ++j) {
                const earlier = expression.getOperand(j);
                const prior = results.get(base + j);
                if (
                    operand !== null &&
                    earlier !== null &&
                    operand.isSameSolid(earlier) &&
                    earlier.isSameSolid(operand)
                ) {
                    if (prior !== result) hasConflict = true;
                    if (result === GeometryContainment.Boundary && prior === result) samePriorBoundary = true;
                    if (
                        operation === CsgOperation.DIFFERENCE &&
                        j === 0 &&
                        prior === result &&
                        (result === GeometryContainment.Inside ||
                            result === GeometryContainment.Outside ||
                            result === GeometryContainment.Boundary)
                    )
                        subtractsSelf = true;
                }
            }
            if (operation === CsgOperation.DIFFERENCE && i > 0) {
                if (result === GeometryContainment.Inside) result = GeometryContainment.Outside;
                else if (result === GeometryContainment.Outside) result = GeometryContainment.Inside;
            }
            hasInside ||= result === GeometryContainment.Inside;
            hasOutside ||= result === GeometryContainment.Outside;
            if (result === GeometryContainment.Boundary && !samePriorBoundary) ++boundaryCount;
            hasUnsupported ||= result === GeometryContainment.Unsupported;
            hasIndeterminate ||= result === GeometryContainment.Indeterminate;
            hasInvalid ||= result === GeometryContainment.InvalidInput;
        }
        CsgPointClassification.truncate(workspace, base);
        workspace.path.remove(workspace.path.size() - 1);
        if (hasInvalid) return GeometryContainment.InvalidInput;
        if (hasConflict) return GeometryContainment.Indeterminate;
        if (subtractsSelf) return GeometryContainment.Outside;
        if (count === 1) return first;
        if (operation === CsgOperation.UNION && hasInside) return GeometryContainment.Inside;
        if (operation !== CsgOperation.UNION && hasOutside) return GeometryContainment.Outside;
        if (hasUnsupported) return GeometryContainment.Unsupported;
        if (hasIndeterminate) return GeometryContainment.Indeterminate;
        if (boundaryCount === 1) return GeometryContainment.Boundary;
        if (boundaryCount > 1) return GeometryContainment.Indeterminate;
        return operation === CsgOperation.UNION ? GeometryContainment.Outside : GeometryContainment.Inside;
    }
}
