package vsdk.toolkit.environment.scene.csg;

import java.util.ArrayList;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.GeometryContainment;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayCrossingQuery;
import vsdk.toolkit.environment.geometry.element.RaySolidQueryStatus;
import vsdk.toolkit.environment.scene.AffinePlacementStatus;

/**
The POV-Ray 1.0 CSG heuristic: leaf boundary events are filtered by the
membership of the event point in sibling operands. A union keeps every
operand event, so internal surfaces remain. An intersection keeps an event
of operand i when every other operand contains the point. A difference keeps
a minuend event when no subtrahend contains it, and a subtrahend event when
the minuend contains it and no other subtrahend does. Membership is the
certified classification of the tolerance ball, measured in the coordinates
of the node whose operands are tested; Boundary counts as contained for an
intersection operand and for the complement of a subtrahend, as in POV.
An event is kept only when membership is proven and dropped when one sibling
proves exclusion; otherwise the query fails with that sibling's status.
*/
public class CsgCrossingMembershipFilter {
    private static RaySolidQueryStatus toStatus(GeometryContainment containment) {
        if (containment==GeometryContainment.InvalidInput) return RaySolidQueryStatus.InvalidInput;
        if (containment==GeometryContainment.Unsupported) return RaySolidQueryStatus.Unsupported;
        return RaySolidQueryStatus.Indeterminate;
    }

    // Certified world classification of a single operand in its parent space.
    public static GeometryContainment classifyOperand(CsgOperandView operand,
        Vector3Dd point, double tolerance) {
        CsgPointWorkspace workspace = new CsgPointWorkspace();
        return CsgPointClassification.classifyOperandContainment(operand,point,tolerance,workspace);
    }

    public static RaySolidQueryStatus select(CsgExpression expression, Ray ray,
        RayCrossingQuery query, double tolerance, CsgRaySurfaceEvents result) {
        CsgPointWorkspace workspace = new CsgPointWorkspace();
        return select(expression,ray,query,tolerance,workspace,result);
    }

    // The same selection with caller-owned scratch storage: after warm-up it
    // allocates nothing beyond the leaf queries.
    public static RaySolidQueryStatus select(CsgExpression expression, Ray ray,
        RayCrossingQuery query, double tolerance, CsgPointWorkspace workspace,
        CsgRaySurfaceEvents result) {
        result.clear();
        workspace.clear();
        if (!Double.isFinite(tolerance) || tolerance<0) return RaySolidQueryStatus.InvalidInput;
        RaySolidQueryStatus status=CsgRayLeafQuery.query(expression,ray,query,workspace.ray,result.getLeaves());
        long[] leafIndex={0};
        if (status==RaySolidQueryStatus.Supported)
            status=select(expression,ray,tolerance,result.getLeaves(),leafIndex,workspace);
        for (int i=0;status==RaySolidQueryStatus.Supported && i<workspace.events.size();++i)
            if (!result.add(workspace.events.get(i))) status=RaySolidQueryStatus.Indeterminate;
        if (status==RaySolidQueryStatus.Supported && !result.getVersions().capture(expression))
            status=RaySolidQueryStatus.Indeterminate;
        if (status!=RaySolidQueryStatus.Supported) result.clear();
        return status;
    }

    /** `keep` is a one-element output holder. */
    private static RaySolidQueryStatus decide(CsgExpression expression,
        long operandIndex, Vector3Dd point, double tolerance, CsgPointWorkspace workspace,
        boolean[] keep) {
        final CsgOperation operation=expression.getOperation();
        boolean excluded=false;
        RaySolidQueryStatus uncertain=RaySolidQueryStatus.Supported;
        for (long j=0;j<expression.getOperandCount() && !excluded;++j) {
            if (j==operandIndex) continue;
            final boolean mustContain=CsgMembershipRule.requiresContainment(operation,operandIndex,j);
            final CsgOperandView sibling=expression.getOperand(j);
            final GeometryContainment c=sibling != null ?
                CsgPointClassification.classifyOperandContainment(sibling,point,tolerance,workspace) :
                GeometryContainment.InvalidInput;
            if (c==GeometryContainment.InvalidInput) return RaySolidQueryStatus.InvalidInput;
            if (c==GeometryContainment.Boundary) continue;
            if (c==GeometryContainment.Inside) { excluded=!mustContain; continue; }
            if (c==GeometryContainment.Outside) { excluded=mustContain; continue; }
            if (uncertain==RaySolidQueryStatus.Supported) uncertain=toStatus(c);
        }
        keep[0]=false;
        if (excluded) return RaySolidQueryStatus.Supported;
        if (uncertain!=RaySolidQueryStatus.Supported) return uncertain;
        keep[0]=true;
        return RaySolidQueryStatus.Supported;
    }

    // Appends the kept events of the expression to the workspace event stack.
    // `leafIndex` is a one-element in/out holder.
    private static RaySolidQueryStatus select(CsgExpression expression, Ray ray,
        double tolerance, CsgRayLeafBatch leaves, long[] leafIndex,
        CsgPointWorkspace workspace) {
        final CsgOperation operation=expression.getOperation();
        ArrayList<CsgRayEventReference> events=workspace.events;
        for (long i=0;i<expression.getOperandCount();++i) {
            final CsgOperandView operand=expression.getOperand(i);
            final CsgExpression child=operand.getChildExpression();
            // This operand's events occupy the stack from `first` on.
            final int first=events.size();
            if (child != null) {
                // The leaf query has already validated this placement.
                Matrix4x4d[] forward={new Matrix4x4d()},inverse={new Matrix4x4d()};
                if (operand.queryAffinePlacement(forward,inverse)!=AffinePlacementStatus.Valid)
                    return RaySolidQueryStatus.Indeterminate;
                final Ray childRay=Ray.fromAffine(inverse[0].multiply(ray.getOrigin()),
                    inverse[0].withoutTranslation().multiply(ray.getDirection()),ray.getT());
                final RaySolidQueryStatus status=select(child,childRay,tolerance,leaves,leafIndex,workspace);
                if (status!=RaySolidQueryStatus.Supported) return status;
            }
            else {
                final CsgRayLeaf leaf=leaves.get(leafIndex[0]);
                if (leaf == null) return RaySolidQueryStatus.Indeterminate;
                for (long k=0;k<leaf.getClassification().boundaries.size();++k)
                    if (!events.add(new CsgRayEventReference(leafIndex[0],k))) return RaySolidQueryStatus.Indeterminate;
                ++leafIndex[0];
            }
            if (!CsgMembershipRule.filtersEvents(operation)) continue;
            // Keep the selected events in order, compacting them in place.
            int kept=first;
            final int last=events.size();
            for (int k=first;k<last;++k) {
                boolean[] keep={true};
                final CsgRayEventReference event=events.get(k);
                final double t=leaves.get(event.leafIndex).getClassification().boundaries.get((int)event.boundaryIndex).t;
                final Vector3Dd point=ray.getOrigin().add(ray.getDirection().multiply(t));
                final RaySolidQueryStatus status=decide(expression,i,point,tolerance,workspace,keep);
                if (status!=RaySolidQueryStatus.Supported) return status;
                if (keep[0]) events.set(kept++,event);
            }
            while (events.size()>kept) events.remove(events.size()-1);
        }
        return RaySolidQueryStatus.Supported;
    }
}
