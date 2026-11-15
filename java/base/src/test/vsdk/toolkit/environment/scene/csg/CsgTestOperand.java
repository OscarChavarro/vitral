package vsdk.toolkit.environment.scene.csg;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.element.GeometryContainment;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayCrossingQuery;
import vsdk.toolkit.environment.geometry.element.RaySolidClassification;
import vsdk.toolkit.environment.geometry.element.RaySolidQueryStatus;

public class CsgTestOperand implements CsgOperandView {
    private final GeometryContainment result;
    private final GeometryContainment smallerResult;
    private final double switchLimit;
    private boolean malformedRay;

    public CsgTestOperand(GeometryContainment value) { result = value; smallerResult = value; switchLimit = -1; malformedRay = false; }
    public CsgTestOperand(GeometryContainment value, GeometryContainment smaller, double limit)
    { result = value; smallerResult = smaller; switchLimit = limit; malformedRay = false; }

    public void setMalformedRay(boolean value) { malformedRay=value; }

    @Override public RaySolidQueryStatus classifyRay(Ray ray, RayCrossingQuery query, RaySolidClassification result)
    { result.clear(); return malformedRay ? RaySolidQueryStatus.Supported : RaySolidQueryStatus.Unsupported; }
    @Override public Geometry getGeometry() { return null; }
    @Override public Matrix4x4d getAffinePlacement() { return Matrix4x4d.identityMatrix(); }
    @Override public GeometryContainment classifyPointCertified(Vector3Dd point, double tolerance)
    { return tolerance<=switchLimit ? smallerResult : result; }
}
