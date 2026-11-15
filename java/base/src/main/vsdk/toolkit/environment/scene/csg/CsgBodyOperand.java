package vsdk.toolkit.environment.scene.csg;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.element.GeometryBounds;
import vsdk.toolkit.environment.geometry.element.GeometryContainment;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayCrossingQuery;
import vsdk.toolkit.environment.geometry.element.RaySolidClassification;
import vsdk.toolkit.environment.geometry.element.RaySolidQueryStatus;
import vsdk.toolkit.environment.scene.AffinePlacementStatus;
import vsdk.toolkit.environment.scene.SceneSolid;

/**
Borrowed scene body (any SceneSolid); retains its existing geometry and affine placement.
*/
public class CsgBodyOperand implements CsgOperandView {
    private final SceneSolid body;

    public CsgBodyOperand(SceneSolid value) {
        this.body = value;}

    @Override
    public GeometryContainment classifyPointCertified(Vector3Dd point, double tolerance) { return body.classifyPointCertified(point, tolerance); }

    @Override
    public void getSolidBounds(GeometryBounds result) { body.getSolidWorldBounds(result); }

    @Override
    public RaySolidQueryStatus classifyRay(Ray ray, RayCrossingQuery query, RaySolidClassification result) { return body.classifyRay(ray,query,result); }

    @Override
    public boolean isSameSolid(CsgOperandView other) {
    final CsgBodyOperand bodyOperand =(other instanceof CsgBodyOperand ? (CsgBodyOperand)(other) : null);
    return bodyOperand != null && CsgSolidEquivalence.same(body,bodyOperand.body);
}

    // A body whose geometry is a CSG expression is traversed structurally only
    // when the body denotes exactly that placed geometry.
    @Override
    public boolean isStructuralTraversalSafe() { return body.isStructuralTraversalSafe(); }

    @Override
    public boolean queryModificationVersion(long[] version) { version[0]=body.getModificationVersion(); return true; }

    @Override
    public Geometry getGeometry() { return body.getGeometry(); }

    @Override
    public Matrix4x4d getAffinePlacement() { return body.getAffinePlacement(); }

    @Override
    public AffinePlacementStatus queryAffinePlacement(Matrix4x4d[] forward, Matrix4x4d[] inverse) { return body.queryAffinePlacement(forward, inverse); }
}

