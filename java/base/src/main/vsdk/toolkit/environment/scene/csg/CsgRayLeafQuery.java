package vsdk.toolkit.environment.scene.csg;

import java.util.ArrayList;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayCrossingQuery;
import vsdk.toolkit.environment.geometry.element.RaySolidClassification;
import vsdk.toolkit.environment.geometry.element.RaySolidQueryStatus;
import vsdk.toolkit.environment.scene.AffinePlacementStatus;

/**
Collects typed leaf intervals, preserving ties; it does not combine them into
a CSG solid. Failure clears the output and never exposes partial leaves.
*/
public class CsgRayLeafQuery {
    private static RaySolidQueryStatus collect(CsgExpression expression, Ray ray, RayCrossingQuery query, ArrayList<CsgExpression> path, CsgRayLeafBatch result) {
    if (path.contains(expression)) return RaySolidQueryStatus.InvalidInput;
    final CsgOperation operation=expression.getOperation();
    final long count=expression.getOperandCount();
    if (count<=0 || (operation!=CsgOperation.UNION && operation!=CsgOperation.INTERSECTION &&
        operation!=CsgOperation.DIFFERENCE)) return RaySolidQueryStatus.InvalidInput;
    if (!path.add(expression)) return RaySolidQueryStatus.Indeterminate;
    for (long i=0;i<count;++i) {
        final CsgOperandView operand =expression.getOperand(i);
        if (operand == null) { path.remove(path.size()-1); return RaySolidQueryStatus.InvalidInput; }
        final long first=result.size();
        final CsgExpression child =operand.getChildExpression();
        Matrix4x4d[] forward = {null};
        Matrix4x4d[] inverse = {null};
        RaySolidQueryStatus status=RaySolidQueryStatus.Supported;
        if (child != null && (!operand.isStructuralTraversalSafe() || !child.isStructuralTraversalSafe())) status=RaySolidQueryStatus.Unsupported;
        else if (child != null ) {
            final AffinePlacementStatus placement=operand.queryAffinePlacement(forward,inverse);
            if (placement==AffinePlacementStatus.Invalid) status=RaySolidQueryStatus.InvalidInput;
            else if (placement==AffinePlacementStatus.Unsupported) status=RaySolidQueryStatus.Unsupported;
            else if (placement!=AffinePlacementStatus.Valid) status=RaySolidQueryStatus.Indeterminate;
            else {
                final Vector3Dd origin=inverse[0].multiply(ray.getOrigin());
                final Vector3Dd direction=inverse[0].withoutTranslation().multiply(ray.getDirection());
                if (!Double.isFinite(origin.x()) || !Double.isFinite(origin.y()) ||
                    !Double.isFinite(origin.z()) || !Double.isFinite(direction.x()) ||
                    !Double.isFinite(direction.y()) || !Double.isFinite(direction.z()) ||
                    (direction.x()==0 && direction.y()==0 && direction.z()==0))
                    status=RaySolidQueryStatus.Indeterminate;
                else status=collect(child,Ray.fromAffine(origin,direction,ray.getT()),query,path,result);
            }
        }
        else {
            CsgRayLeaf leaf =result.acquire();
            if (leaf == null) status=RaySolidQueryStatus.Indeterminate;
            else status=operand.classifyRay(ray,query,leaf.getClassification());
            if (status==RaySolidQueryStatus.Supported) {
                final RaySolidClassification classification =leaf.getClassification();
                if (classification.insideSegments.size()!=classification.boundaries.size()+1)
                    status=RaySolidQueryStatus.Indeterminate;
                for (long k=0;status==RaySolidQueryStatus.Supported && k<classification.boundaries.size();++k) {
                    final double t=classification.boundaries.get((int)(k)).t;
                    if (classification.boundaries.get((int)(k)).geometry == null || !Double.isFinite(t) ||
                        t<query.minT || t>query.maxT || (k>0 && t<classification.boundaries.get((int)(k-1)).t) ||
                        classification.insideSegments.get((int)(k)).booleanValue()==classification.insideSegments.get((int)(k+1)).booleanValue())
                        status=RaySolidQueryStatus.Indeterminate;
                }
            }
        }
        if (status!=RaySolidQueryStatus.Supported) { path.remove(path.size()-1); return status; }
        for (long j=first;j<result.size();++j) {
            CsgRayLeaf leaf =result.get(j);
            if (!leaf.prependOperand(operand) || (child != null && !leaf.appendPlacement(operand,forward[0],inverse[0]))) {
                path.remove(path.size()-1); return RaySolidQueryStatus.Indeterminate;
            }
            if (operation==CsgOperation.DIFFERENCE && i>0) leaf.reverseOrientation();
        }
    }
    path.remove(path.size()-1);
    return RaySolidQueryStatus.Supported;
}

    public static RaySolidQueryStatus query(CsgExpression expression, Ray ray, RayCrossingQuery query, CsgRayLeafBatch result) {
    CsgRayWorkspace workspace = new CsgRayWorkspace();
    return CsgRayLeafQuery.query(expression,ray,query,workspace,result);
}

    // Same query with caller-owned scratch storage.
    public static RaySolidQueryStatus query(CsgExpression expression, Ray ray, RayCrossingQuery query, CsgRayWorkspace workspace, CsgRayLeafBatch result) {
    result.clear();
    final Vector3Dd origin =ray.getOrigin();
    final Vector3Dd direction =ray.getDirection();
    if (!Double.isFinite(origin.x()) || !Double.isFinite(origin.y()) ||
        !Double.isFinite(origin.z()) || !Double.isFinite(direction.x()) ||
        !Double.isFinite(direction.y()) || !Double.isFinite(direction.z()) ||
        (direction.x()==0 && direction.y()==0 && direction.z()==0) ||
        Double.isNaN(query.minT) || Double.isNaN(query.maxT) || query.minT>=query.maxT ||
        query.minimumChordSquared!=0 || query.minimumExteriorApproach!=Double.NEGATIVE_INFINITY)
        return RaySolidQueryStatus.InvalidInput;
    workspace.path.clear();
    final RaySolidQueryStatus status=collect(expression,ray,query,workspace.path,result);
    if (status!=RaySolidQueryStatus.Supported) result.clear();
    return status;
}
}

