package vsdk.toolkit.environment.scene.csg;

import java.util.ArrayList;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayBoundary;
import vsdk.toolkit.environment.geometry.element.RayCrossingQuery;
import vsdk.toolkit.environment.geometry.element.RaySolidClassification;
import vsdk.toolkit.environment.geometry.element.RaySolidQueryStatus;

/**
[ROTH1982] section 3.3 ray classification: every leaf is classified as IN/OUT
segments and the segments are combined through the expression with the
union, intersection and difference rows of Table 3. Events with exactly
equal parameters are applied together before the combined state is read,
so coincident faces and touching operands yield the regularized result and
point contacts produce no boundary. ON neighbourhoods (section 6.1) are not
modelled. Uncertain or unsupported leaves fail the whole query.
*/
public class CsgRothRayClassifier {
    /** `leafIndex` is a one-element in/out holder. */
    private static boolean evaluate(CsgExpression expression, ArrayList<Boolean> states, long[] leafIndex) {
        final CsgOperation operation=expression.getOperation();
        boolean result=false;
        for (long i=0;i<expression.getOperandCount();++i) {
            final CsgOperandView operand=expression.getOperand(i);
            final CsgExpression child=operand.getChildExpression();
            // Every operand is visited so that the leaf order stays aligned.
            final boolean inside=child != null ? evaluate(child,states,leafIndex) : states.get((int)(leafIndex[0]++));
            result=i==0 ? inside : CsgMembershipRule.combine(operation,result,inside);
        }
        return result;
    }

    private static RaySolidQueryStatus combine(CsgExpression expression,
        CsgRayWorkspace workspace, CsgRayClassification result) {
        final CsgRayLeafBatch leaves=result.getLeaves();
        ArrayList<Boolean> states=workspace.states;
        ArrayList<Boolean> previous=workspace.previous;
        ArrayList<Boolean> trial=workspace.trial;
        states.clear(); previous.clear(); trial.clear();
        for (long i=0;i<leaves.size();++i) {
            final boolean initial=leaves.get(i).getClassification().insideSegments.get(0);
            if (!states.add(initial) || !previous.add(initial) || !trial.add(initial))
                return RaySolidQueryStatus.Indeterminate;
        }
        RaySolidClassification solid=result.getSolid();
        boolean inside=isInside(expression,states);
        if (!solid.insideSegments.add(inside)) return RaySolidQueryStatus.Indeterminate;
        double[] t={0};
        ArrayList<CsgRayEventReference> group=workspace.group;
        while (CsgRayEventQuery.nextGroup(leaves,workspace.cursors,t,group)) {
            for (int i=0;i<states.size();++i) previous.set(i,states.get(i));
            for (int k=0;k<group.size();++k)
                states.set((int)group.get(k).leafIndex,
                    leaves.get(group.get(k).leafIndex).getClassification().insideSegments.get((int)(group.get(k).boundaryIndex+1)));
            final boolean next=isInside(expression,states);
            if (next==inside) continue;
            // Owners are the leaves whose transition in this group, applied alone
            // to the previous states, already yields the new membership. When no
            // single leaf suffices (e.g. coincident faces of an intersection), the
            // owners are all changed leaves oriented like the combined transition.
            if (!result.beginOwners()) return RaySolidQueryStatus.Indeterminate;
            long ownerCount=0;
            int source=0;
            for (int pass=0;pass<2 && ownerCount==0;++pass) {
                for (int k=0;k<group.size();++k) {
                    final int leaf=(int)group.get(k).leafIndex;
                    if (k+1<group.size() && group.get(k+1).leafIndex==leaf) continue;
                    if (states.get(leaf).booleanValue()==previous.get(leaf).booleanValue()) continue;
                    if (pass==0) {
                        for (int i=0;i<previous.size();++i) trial.set(i,previous.get(i));
                        trial.set(leaf,states.get(leaf));
                        if (isInside(expression,trial)!=next) continue;
                    }
                    else if ((states.get(leaf)!=leaves.get(leaf).getPath().isReversed())!=next) continue;
                    if (!result.addOwner(group.get(k))) return RaySolidQueryStatus.Indeterminate;
                    if (ownerCount++==0) source=k;
                }
            }
            final CsgRayEventReference reference=group.get(source);
            // Append an empty boundary and assign into it, so the slot's
            // provenance storage is reused instead of copied by value.
            if (!solid.boundaries.add(new RayBoundary()) || !solid.insideSegments.add(next))
                return RaySolidQueryStatus.Indeterminate;
            solid.boundaries.get(solid.boundaries.size()-1).assign(
                leaves.get(reference.leafIndex).getClassification().boundaries.get((int)reference.boundaryIndex));
            inside=next;
        }
        return RaySolidQueryStatus.Supported;
    }

    public static RaySolidQueryStatus classify(CsgExpression expression, Ray ray, RayCrossingQuery query, CsgRayClassification result) {
    CsgRayWorkspace workspace = new CsgRayWorkspace();
    return classify(expression,ray,query,workspace,result);
}

    // Same query with caller-owned scratch storage; with a reused workspace
    // and result it allocates only inside leaf classification.
    public static RaySolidQueryStatus classify(CsgExpression expression, Ray ray, RayCrossingQuery query, CsgRayWorkspace workspace, CsgRayClassification result) {
    result.clear();
    RaySolidQueryStatus status=CsgRayLeafQuery.query(expression,ray,query,workspace,result.getLeaves());
    if (status==RaySolidQueryStatus.Supported)
        status=CsgRayEventQuery.validate(result.getLeaves(),workspace.cursors);
    if (status==RaySolidQueryStatus.Supported) status=combine(expression,workspace,result);
    if (status==RaySolidQueryStatus.Supported && !result.getVersions().capture(expression))
        status=RaySolidQueryStatus.Indeterminate;
    if (status!=RaySolidQueryStatus.Supported) result.clear();
    return status;
}

    // Membership of the expression for given per-leaf states, in the leaf order
    // of CsgRayLeafQuery. The expression must be the one that produced them.
    public static boolean isInside(CsgExpression expression, ArrayList<Boolean> states) {
    long[] leafIndex = {0};
    return evaluate(expression,states,leafIndex);
}
}

