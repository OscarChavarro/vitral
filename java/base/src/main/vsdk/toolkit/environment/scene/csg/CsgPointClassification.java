package vsdk.toolkit.environment.scene.csg;

import java.util.ArrayList;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.GeometryBounds;
import vsdk.toolkit.environment.geometry.element.GeometryContainment;
import vsdk.toolkit.environment.scene.AffinePlacementStatus;
import vsdk.toolkit.environment.scene.AffineToleranceMap;

/**
Conservative world-space classification. An operand boundary becomes a
composite boundary only when every other operand is certified non-interfering.
*/
public class CsgPointClassification {
    public static GeometryContainment classify(CsgExpression expression, Vector3Dd point, double tolerance)
    {
        CsgPointWitness ignored = new CsgPointWitness();
        return classifyWithWitness(expression,point,tolerance,ignored);
    }

    /** Witness is cleared when no single operand proves the returned result. */
    public static GeometryContainment classifyWithWitness(CsgExpression expression, Vector3Dd point,
        double tolerance, CsgPointWitness witness)
    {
        witness.clear();
        if (!Double.isFinite(point.x()) || !Double.isFinite(point.y()) ||
            !Double.isFinite(point.z()) || !Double.isFinite(tolerance) || tolerance < 0)
            return GeometryContainment.InvalidInput;
        ArrayList<CsgExpression> path = new ArrayList<CsgExpression>();
        return classifyExpression(expression, point, tolerance, path, witness, null, null);
    }

    /** Collect sufficient leaf facts, including cases requiring several operands. */
    public static GeometryContainment classifyWithProof(CsgExpression expression, Vector3Dd point,
        double tolerance, CsgPointProof proof)
    {
        proof.clear();
        if (!Double.isFinite(point.x()) || !Double.isFinite(point.y()) ||
            !Double.isFinite(point.z()) || !Double.isFinite(tolerance) || tolerance<0)
            return GeometryContainment.InvalidInput;
        ArrayList<CsgExpression> path = new ArrayList<CsgExpression>();
        final GeometryContainment result=classifyExpression(expression,point,tolerance,path,null,proof,null);
        if (proof.isAvailable() && !proof.getVersions().capture(expression)) proof.clear();
        return result;
    }

    /** All attributable owners are exposed only when the complete set is known. */
    public static GeometryContainment classifyWithAttribution(CsgExpression expression, Vector3Dd point,
        double tolerance, CsgPointAttribution attribution)
    {
        attribution.clear();
        if (!Double.isFinite(point.x()) || !Double.isFinite(point.y()) ||
            !Double.isFinite(point.z()) || !Double.isFinite(tolerance) || tolerance<0)
            return GeometryContainment.InvalidInput;
        ArrayList<CsgExpression> path = new ArrayList<CsgExpression>();
        final GeometryContainment result=classifyExpression(expression,point,tolerance,path,null,null,attribution);
        if (attribution.isComplete() && !attribution.getVersions().capture(expression)) attribution.clear();
        return result;
    }

    private static GeometryContainment classifyExpression(CsgExpression expression, Vector3Dd point,
        double tolerance, ArrayList<CsgExpression> path, CsgPointWitness witness, CsgPointProof proof,
        CsgPointAttribution attribution)
    {
        if (path.contains(expression)) return GeometryContainment.InvalidInput;
        final long count = expression.getOperandCount();
        final CsgOperation operation = expression.getOperation();
        if (count <= 0 || (operation != CsgOperation.UNION && operation != CsgOperation.INTERSECTION &&
            operation != CsgOperation.DIFFERENCE)) return GeometryContainment.InvalidInput;
        if (!path.add(expression)) return GeometryContainment.Indeterminate;
        boolean hasInside = false, hasOutside = false;
        long boundaryCount = 0;
        boolean hasUnsupported = false, hasIndeterminate = false, hasInvalid = false;
        GeometryContainment first = GeometryContainment.Indeterminate;
        CsgPointWitness firstWitness = new CsgPointWitness(), decisiveWitness = new CsgPointWitness(), boundaryWitness = new CsgPointWitness();
        CsgPointProof firstProof = new CsgPointProof(), decisiveProof = new CsgPointProof(), allProof = new CsgPointProof(), selfProof = new CsgPointProof();
        CsgPointAttribution firstAttribution = new CsgPointAttribution(), boundaryAttribution = new CsgPointAttribution();
        allProof.begin(); boundaryAttribution.begin();
        ArrayList<GeometryContainment> priorResults = new ArrayList<GeometryContainment>();
        boolean subtractsSelf=false, hasConflict=false;
        ArrayList<CsgPointCandidate> candidates = new ArrayList<CsgPointCandidate>();
        for (long i = 0; i < count; ++i) {
            final CsgOperandView operand = expression.getOperand(i);
            GeometryContainment result = GeometryContainment.InvalidInput;
            CsgPointWitness candidate = new CsgPointWitness();
            CsgPointProof candidateProof = new CsgPointProof();
            CsgPointAttribution candidateAttribution = new CsgPointAttribution();
            if (operand != null) {
                final CsgExpression child = operand.getChildExpression();
                if (child != null && (!operand.isStructuralTraversalSafe() || !child.isStructuralTraversalSafe())) {
                    result=GeometryContainment.Unsupported;
                }
                else if (child != null) {
                    Matrix4x4d[] forward = {new Matrix4x4d()}, inverse = {new Matrix4x4d()};
                    final AffinePlacementStatus placement = operand.queryAffinePlacement(forward, inverse);
                    if (placement == AffinePlacementStatus.Invalid) result = GeometryContainment.InvalidInput;
                    else if (placement == AffinePlacementStatus.Unsupported) result = GeometryContainment.Unsupported;
                    else if (placement != AffinePlacementStatus.Valid) result = GeometryContainment.Indeterminate;
                    else {
                        boolean identity = true;
                        for (int row = 0; row < 4; ++row) for (int column = 0; column < 4; ++column)
                            if (forward[0].get(row,column) != (row == column ? 1.0 : 0.0)) identity = false;
                        if (identity) result=classifyExpression(child,point,tolerance,path,candidate,candidateProof,candidateAttribution);
                        else {
                            Vector3Dd[] childPoint = {new Vector3Dd()};
                            double[] childTolerance={0}, childInnerTolerance={-1};
                            final AffinePlacementStatus mapped=AffineToleranceMap.map(
                                forward[0],point,tolerance,childPoint,childTolerance,childInnerTolerance);
                            if (mapped==AffinePlacementStatus.Invalid) result=GeometryContainment.InvalidInput;
                            else if (mapped==AffinePlacementStatus.Unsupported) result=GeometryContainment.Unsupported;
                            else if (mapped!=AffinePlacementStatus.Valid) result=GeometryContainment.Indeterminate;
                            else {
                                result=classifyExpression(child,childPoint[0],childTolerance[0],path,candidate,candidateProof,candidateAttribution);
                                // An outer-ball boundary is insufficient. A boundary
                                // within the certified inner ball is also within the
                                // original parent-space tolerance ball.
                                if (result==GeometryContainment.Boundary || result==GeometryContainment.Indeterminate) {
                                    candidate.clear();
                                    candidateProof.clear();
                                    candidateAttribution.clear();
                                    if (childInnerTolerance[0]>0) {
                                        CsgPointWitness innerWitness = new CsgPointWitness();
                                        CsgPointProof innerProof = new CsgPointProof();
                                        CsgPointAttribution innerAttribution = new CsgPointAttribution();
                                        final GeometryContainment inner=classifyExpression(child,childPoint[0],
                                            childInnerTolerance[0],path,innerWitness,innerProof,innerAttribution);
                                        if (inner==GeometryContainment.Boundary) {
                                            result=inner;
                                            candidate.assign(innerWitness);
                                            candidateProof.assign(innerProof);
                                            candidateAttribution.assign(innerAttribution);
                                        }
                                        else if (inner==GeometryContainment.InvalidInput ||
                                            inner==GeometryContainment.Unsupported) result=inner;
                                        else result=GeometryContainment.Indeterminate;
                                    }
                                    else result=GeometryContainment.Indeterminate;
                                }
                            }
                        }
                    }
                }
                else {
                    // Conservative culling: a solid whose certified bounds miss the
                    // tolerance ball is outside; uncertain bounds never cull.
                    GeometryBounds bounds = new GeometryBounds();
                    operand.getSolidBounds(bounds);
                    result = bounds.excludesBall(point,tolerance) ? GeometryContainment.Outside :
                        operand.classifyPointCertified(point,tolerance);
                    if (result==GeometryContainment.Inside || result==GeometryContainment.Outside ||
                        result==GeometryContainment.Boundary) {
                        if (candidate.prepend(operand)) {
                            candidateProof.begin();
                            candidateProof.add(new CsgPointEvidence(candidate,result,point,tolerance));
                            if (result==GeometryContainment.Boundary) {
                                candidateAttribution.begin(); candidateAttribution.add(candidate);
                            }
                        }
                    }
                }
                if (child != null && candidate.isAvailable() && !candidate.prepend(operand)) candidate.clear();
                if (child != null && candidateProof.isAvailable() && !candidateProof.prepend(operand)) candidateProof.clear();
                if (child != null && candidateAttribution.isComplete() && !candidateAttribution.prepend(operand)) candidateAttribution.clear();
            }
            if (result != GeometryContainment.Inside && result != GeometryContainment.Outside &&
                result != GeometryContainment.Boundary && result != GeometryContainment.Unsupported &&
                result != GeometryContainment.Indeterminate && result != GeometryContainment.InvalidInput)
                result = GeometryContainment.InvalidInput;
            if (result!=GeometryContainment.Inside && result!=GeometryContainment.Outside &&
                result!=GeometryContainment.Boundary) { candidate.clear(); candidateProof.clear(); candidateAttribution.clear(); }
            CsgPointCandidate stored = new CsgPointCandidate();
            stored.result=result; stored.witness.assign(candidate); stored.proof.assign(candidateProof); stored.attribution.assign(candidateAttribution);
            if (!candidates.add(stored)) { path.remove(path.size()-1); return GeometryContainment.Indeterminate; }
        }
        // A certified sibling can supply a classification for an equivalent view.
        // A composite recipient does not inherit leaf attribution: its owner paths
        // must be independently resolved. Invalid or opaque views are never used.
        for (long i=0;i<count;++i) {
            CsgPointCandidate target=candidates.get((int)i);
            final CsgOperandView destination=expression.getOperand(i);
            if ((target.result!=GeometryContainment.Unsupported && target.result!=GeometryContainment.Indeterminate) || destination == null) continue;
            final CsgExpression destinationChild=destination.getChildExpression();
            if (destinationChild != null ? (!destination.isStructuralTraversalSafe() || !destinationChild.isStructuralTraversalSafe()) : destination.getGeometry() == null) continue;
            for (long j=0;j<count;++j) {
                final CsgOperandView source=expression.getOperand(j);
                final CsgPointCandidate known=candidates.get((int)j);
                if (i==j || source == null || (known.result!=GeometryContainment.Inside && known.result!=GeometryContainment.Outside &&
                     known.result!=GeometryContainment.Boundary) || !known.proof.isAvailable() ||
                    !destination.isSameSolid(source) || !source.isSameSolid(destination)) continue;
                final CsgExpression sourceChild=source.getChildExpression();
                if (sourceChild != null ? (!source.isStructuralTraversalSafe() || !sourceChild.isStructuralTraversalSafe()) : source.getGeometry() == null) continue;
                CsgPointWitness sourceView = new CsgPointWitness(), recipient = new CsgPointWitness();
                if (!sourceView.prepend(source) || !recipient.prepend(destination)) break;
                target.proof.assign(known.proof);
                if (!target.proof.add(new CsgPointEvidence(sourceView,recipient,point,tolerance))) { target.proof.clear(); break; }
                target.result=known.result;
                if (destinationChild == null) {
                    target.witness.assign(recipient);
                    if (target.result==GeometryContainment.Boundary) {
                        target.attribution.begin(); target.attribution.add(recipient);
                    }
                }
                break;
            }
        }
        for (long i=0;i<count;++i) {
            final CsgOperandView operand=expression.getOperand(i);
            GeometryContainment result=candidates.get((int)i).result;
            CsgPointWitness candidate=new CsgPointWitness(candidates.get((int)i).witness);
            CsgPointProof candidateProof=new CsgPointProof(candidates.get((int)i).proof);
            CsgPointAttribution candidateAttribution=new CsgPointAttribution(candidates.get((int)i).attribution);
            if (i == 0) { first = result; firstWitness.assign(candidate); firstProof.assign(candidateProof); firstAttribution.assign(candidateAttribution); }
            boolean samePriorBoundary=false, matchesFirst=false;
            for (long j=0;j<i;++j) {
                final CsgOperandView earlier=expression.getOperand(j);
                if (operand != null && earlier != null && operand.isSameSolid(earlier) && earlier.isSameSolid(operand)) {
                    if (priorResults.get((int)j)!=result) hasConflict=true;
                    if (result==GeometryContainment.Boundary && priorResults.get((int)j)==result) samePriorBoundary=true;
                    if (operation==CsgOperation.DIFFERENCE && j==0 && priorResults.get((int)j)==result &&
                        (result==GeometryContainment.Inside || result==GeometryContainment.Outside ||
                         result==GeometryContainment.Boundary)) { subtractsSelf=true; matchesFirst=true; }
                }
            }
            if (!priorResults.add(result)) { path.remove(path.size()-1); return GeometryContainment.Indeterminate; }
            if (operation == CsgOperation.DIFFERENCE && i > 0) {
                if (result == GeometryContainment.Inside) result = GeometryContainment.Outside;
                else if (result == GeometryContainment.Outside) result = GeometryContainment.Inside;
                candidate.reverseOrientation();
                candidateProof.reverseOrientation();
                candidateAttribution.reverseOrientation();
            }
            if (matchesFirst && firstProof.isAvailable() && candidateProof.isAvailable() && !selfProof.isAvailable()) {
                selfProof.assign(firstProof);
                if (!selfProof.append(candidateProof)) selfProof.clear();
            }
            if (!decisiveProof.isAvailable() && candidateProof.isAvailable() &&
                ((operation==CsgOperation.UNION && result==GeometryContainment.Inside) ||
                 (operation!=CsgOperation.UNION && result==GeometryContainment.Outside)))
                decisiveProof.assign(candidateProof);
            if (!allProof.append(candidateProof)) allProof.clear();
            if (!decisiveWitness.isAvailable() && candidate.isAvailable() &&
                ((operation==CsgOperation.UNION && result==GeometryContainment.Inside) ||
                 (operation!=CsgOperation.UNION && result==GeometryContainment.Outside)))
                decisiveWitness.assign(candidate);
            hasInside |= result == GeometryContainment.Inside;
            hasOutside |= result == GeometryContainment.Outside;
            if (result == GeometryContainment.Boundary && !boundaryAttribution.append(candidateAttribution))
                boundaryAttribution.clear();
            if (result == GeometryContainment.Boundary && !samePriorBoundary) {
                ++boundaryCount;
                if (boundaryCount==1) boundaryWitness.assign(candidate);
            }
            else if (result == GeometryContainment.Boundary && boundaryCount==1 &&
                !boundaryWitness.isAvailable() && candidate.isAvailable()) boundaryWitness.assign(candidate);
            hasUnsupported |= result == GeometryContainment.Unsupported;
            hasIndeterminate |= result == GeometryContainment.Indeterminate;
            hasInvalid |= result == GeometryContainment.InvalidInput;
        }
        path.remove(path.size()-1);
        if (hasInvalid) return GeometryContainment.InvalidInput;
        if (hasConflict) return GeometryContainment.Indeterminate;
        if (subtractsSelf) { if (proof != null && selfProof.isAvailable()) proof.assign(selfProof); return GeometryContainment.Outside; }
        if (count == 1) {
            if (witness != null) witness.assign(firstWitness);
            if (proof != null) proof.assign(firstProof);
            if (attribution != null && first==GeometryContainment.Boundary) attribution.assign(firstAttribution);
            return first;
        }
        // A definite result requires a full tolerance ball in or out of the solid.
        // A boundary of one operand need not be a boundary of the composition.
        if (operation == CsgOperation.UNION && hasInside) {
            if (witness != null) witness.assign(decisiveWitness);
            if (proof != null) proof.assign(decisiveProof);
            return GeometryContainment.Inside;
        }
        if (operation != CsgOperation.UNION && hasOutside) {
            if (witness != null) witness.assign(decisiveWitness);
            if (proof != null) proof.assign(decisiveProof);
            return GeometryContainment.Outside;
        }
        if (hasUnsupported) return GeometryContainment.Unsupported;
        if (hasIndeterminate) return GeometryContainment.Indeterminate;
        // With exactly one boundary and every other operand certified on the
        // non-interfering side throughout the tolerance ball, that operand's
        // boundary is exposed in the composite. Multiple boundaries can coincide
        // or hide one another, so their combination remains indeterminate.
        if (boundaryCount==1) {
            if (witness != null) witness.assign(boundaryWitness);
            if (proof != null) proof.assign(allProof);
            if (attribution != null) attribution.assign(boundaryAttribution);
            return GeometryContainment.Boundary;
        }
        if (boundaryCount>1) return GeometryContainment.Indeterminate;
        if (proof != null) proof.assign(allProof);
        return operation == CsgOperation.UNION ? GeometryContainment.Outside : GeometryContainment.Inside;
    }

    /**
    The result of `classify` without collecting witnesses, proofs or
    attributions, using the workspace's storage: it allocates nothing once
    the workspace has grown to the expression. Equivalent operands share a
    definite result as in `classify`.
    */
    public static GeometryContainment classifyContainment(CsgExpression expression, Vector3Dd point,
        double tolerance, CsgPointWorkspace workspace)
    {
        if (!Double.isFinite(point.x()) || !Double.isFinite(point.y()) ||
            !Double.isFinite(point.z()) || !Double.isFinite(tolerance) || tolerance<0)
            return GeometryContainment.InvalidInput;
        workspace.path.clear();
        workspace.results.clear();
        return containmentOfExpression(expression,point,tolerance,workspace);
    }

    /** `classifyContainment` of a single operand in its parent's space. */
    public static GeometryContainment classifyOperandContainment(CsgOperandView operand, Vector3Dd point,
        double tolerance, CsgPointWorkspace workspace)
    {
        if (!Double.isFinite(point.x()) || !Double.isFinite(point.y()) ||
            !Double.isFinite(point.z()) || !Double.isFinite(tolerance) || tolerance<0)
            return GeometryContainment.InvalidInput;
        workspace.path.clear();
        workspace.results.clear();
        return containmentOfOperand(operand,point,tolerance,workspace);
    }

    private static void truncate(CsgPointWorkspace workspace, long resultCount)
    {
        while (workspace.results.size()>resultCount) workspace.results.remove(workspace.results.size()-1);
    }

    // Mirrors the operand step of classifyExpression.
    private static GeometryContainment containmentOfOperand(CsgOperandView operand, Vector3Dd point,
        double tolerance, CsgPointWorkspace workspace)
    {
        if (operand == null) return GeometryContainment.InvalidInput;
        GeometryContainment result=GeometryContainment.InvalidInput;
        final CsgExpression child=operand.getChildExpression();
        if (child != null && (!operand.isStructuralTraversalSafe() || !child.isStructuralTraversalSafe()))
            result=GeometryContainment.Unsupported;
        else if (child != null) {
            Matrix4x4d[] forward = {new Matrix4x4d()}, inverse = {new Matrix4x4d()};
            final AffinePlacementStatus placement=operand.queryAffinePlacement(forward,inverse);
            if (placement==AffinePlacementStatus.Invalid) result=GeometryContainment.InvalidInput;
            else if (placement==AffinePlacementStatus.Unsupported) result=GeometryContainment.Unsupported;
            else if (placement!=AffinePlacementStatus.Valid) result=GeometryContainment.Indeterminate;
            else {
                boolean identity=true;
                for (int row=0;row<4;++row) for (int column=0;column<4;++column)
                    if (forward[0].get(row,column)!=(row==column ? 1.0 : 0.0)) identity=false;
                if (identity) result=containmentOfExpression(child,point,tolerance,workspace);
                else {
                    Vector3Dd[] childPoint = {new Vector3Dd()};
                    double[] childTolerance={0}, childInnerTolerance={-1};
                    final AffinePlacementStatus mapped=AffineToleranceMap.map(
                        forward[0],point,tolerance,childPoint,childTolerance,childInnerTolerance);
                    if (mapped==AffinePlacementStatus.Invalid) result=GeometryContainment.InvalidInput;
                    else if (mapped==AffinePlacementStatus.Unsupported) result=GeometryContainment.Unsupported;
                    else if (mapped!=AffinePlacementStatus.Valid) result=GeometryContainment.Indeterminate;
                    else {
                        result=containmentOfExpression(child,childPoint[0],childTolerance[0],workspace);
                        // Only a boundary within the certified inner ball is a
                        // boundary within the parent-space tolerance ball.
                        if (result==GeometryContainment.Boundary || result==GeometryContainment.Indeterminate) {
                            if (childInnerTolerance[0]>0) {
                                final GeometryContainment inner=containmentOfExpression(child,childPoint[0],
                                    childInnerTolerance[0],workspace);
                                if (inner==GeometryContainment.Boundary || inner==GeometryContainment.InvalidInput ||
                                    inner==GeometryContainment.Unsupported) result=inner;
                                else result=GeometryContainment.Indeterminate;
                            }
                            else result=GeometryContainment.Indeterminate;
                        }
                    }
                }
            }
        }
        else {
            GeometryBounds bounds = new GeometryBounds();
            operand.getSolidBounds(bounds);
            result=bounds.excludesBall(point,tolerance) ? GeometryContainment.Outside :
                operand.classifyPointCertified(point,tolerance);
        }
        if (result!=GeometryContainment.Inside && result!=GeometryContainment.Outside &&
            result!=GeometryContainment.Boundary && result!=GeometryContainment.Unsupported &&
            result!=GeometryContainment.Indeterminate && result!=GeometryContainment.InvalidInput)
            result=GeometryContainment.InvalidInput;
        return result;
    }

    // Mirrors classifyExpression; a definite operand result is the condition under
    // which classifyExpression has a proof for it.
    private static GeometryContainment containmentOfExpression(CsgExpression expression, Vector3Dd point,
        double tolerance, CsgPointWorkspace workspace)
    {
        if (workspace.path.contains(expression)) return GeometryContainment.InvalidInput;
        final long count=expression.getOperandCount();
        final CsgOperation operation=expression.getOperation();
        if (count<=0 || (operation!=CsgOperation.UNION && operation!=CsgOperation.INTERSECTION &&
            operation!=CsgOperation.DIFFERENCE)) return GeometryContainment.InvalidInput;
        if (!workspace.path.add(expression)) return GeometryContainment.Indeterminate;
        final int base=workspace.results.size();
        for (long i=0;i<count;++i) {
            final GeometryContainment result=containmentOfOperand(expression.getOperand(i),point,tolerance,workspace);
            if (!workspace.results.add(result)) {
                truncate(workspace,base);
                workspace.path.remove(workspace.path.size()-1);
                return GeometryContainment.Indeterminate;
            }
        }
        ArrayList<GeometryContainment> results=workspace.results;
        for (int i=0;i<count;++i) {
            final GeometryContainment target=results.get(base+i);
            final CsgOperandView destination=expression.getOperand(i);
            if ((target!=GeometryContainment.Unsupported && target!=GeometryContainment.Indeterminate) || destination == null) continue;
            final CsgExpression destinationChild=destination.getChildExpression();
            if (destinationChild != null ? (!destination.isStructuralTraversalSafe() || !destinationChild.isStructuralTraversalSafe()) : destination.getGeometry() == null) continue;
            for (int j=0;j<count;++j) {
                final CsgOperandView source=expression.getOperand(j);
                final GeometryContainment known=results.get(base+j);
                if (i==j || source == null || (known!=GeometryContainment.Inside && known!=GeometryContainment.Outside &&
                     known!=GeometryContainment.Boundary) ||
                    !destination.isSameSolid(source) || !source.isSameSolid(destination)) continue;
                final CsgExpression sourceChild=source.getChildExpression();
                if (sourceChild != null ? (!source.isStructuralTraversalSafe() || !sourceChild.isStructuralTraversalSafe()) : source.getGeometry() == null) continue;
                results.set(base+i, known);
                break;
            }
        }
        boolean hasInside=false, hasOutside=false, hasUnsupported=false, hasIndeterminate=false, hasInvalid=false;
        boolean subtractsSelf=false, hasConflict=false;
        long boundaryCount=0;
        final GeometryContainment first=results.get(base);
        for (int i=0;i<count;++i) {
            final CsgOperandView operand=expression.getOperand(i);
            GeometryContainment result=results.get(base+i);
            boolean samePriorBoundary=false;
            for (int j=0;j<i;++j) {
                final CsgOperandView earlier=expression.getOperand(j);
                final GeometryContainment prior=results.get(base+j);
                if (operand != null && earlier != null && operand.isSameSolid(earlier) && earlier.isSameSolid(operand)) {
                    if (prior!=result) hasConflict=true;
                    if (result==GeometryContainment.Boundary && prior==result) samePriorBoundary=true;
                    if (operation==CsgOperation.DIFFERENCE && j==0 && prior==result &&
                        (result==GeometryContainment.Inside || result==GeometryContainment.Outside ||
                         result==GeometryContainment.Boundary)) subtractsSelf=true;
                }
            }
            if (operation==CsgOperation.DIFFERENCE && i>0) {
                if (result==GeometryContainment.Inside) result=GeometryContainment.Outside;
                else if (result==GeometryContainment.Outside) result=GeometryContainment.Inside;
            }
            hasInside |= result==GeometryContainment.Inside;
            hasOutside |= result==GeometryContainment.Outside;
            if (result==GeometryContainment.Boundary && !samePriorBoundary) ++boundaryCount;
            hasUnsupported |= result==GeometryContainment.Unsupported;
            hasIndeterminate |= result==GeometryContainment.Indeterminate;
            hasInvalid |= result==GeometryContainment.InvalidInput;
        }
        truncate(workspace,base);
        workspace.path.remove(workspace.path.size()-1);
        if (hasInvalid) return GeometryContainment.InvalidInput;
        if (hasConflict) return GeometryContainment.Indeterminate;
        if (subtractsSelf) return GeometryContainment.Outside;
        if (count==1) return first;
        if (operation==CsgOperation.UNION && hasInside) return GeometryContainment.Inside;
        if (operation!=CsgOperation.UNION && hasOutside) return GeometryContainment.Outside;
        if (hasUnsupported) return GeometryContainment.Unsupported;
        if (hasIndeterminate) return GeometryContainment.Indeterminate;
        if (boundaryCount==1) return GeometryContainment.Boundary;
        if (boundaryCount>1) return GeometryContainment.Indeterminate;
        return operation==CsgOperation.UNION ? GeometryContainment.Outside : GeometryContainment.Inside;
    }
}
