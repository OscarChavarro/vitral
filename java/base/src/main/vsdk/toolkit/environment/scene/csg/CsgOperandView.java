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
import vsdk.toolkit.environment.geometry.volume.Solid;
import vsdk.toolkit.environment.scene.AffinePlacementStatus;

/**
A borrowed operand view; its geometry, child expression and placement live
at least as long as the expression that references this view.
*/
public interface CsgOperandView {
    Geometry getGeometry();

    Matrix4x4d getAffinePlacement();

    // Output matrices are usable only when the returned status is Valid.
    default AffinePlacementStatus queryAffinePlacement(Matrix4x4d[] forward, Matrix4x4d[] inverse) { return AffinePlacementStatus.Indeterminate; }

    default CsgExpression getChildExpression() {
    return getGeometry() instanceof CsgExpression e ? e : null;
}

    // Opt in only when child and placement fully describe this view's solid.
    default boolean isStructuralTraversalSafe() { return false; }

    // True only when both views denote the very same placed solid.
    default boolean isSameSolid(CsgOperandView other) { return this==other; }

    // World-space certified query; unknown operand implementations stay explicit.
    default GeometryContainment classifyPointCertified(Vector3Dd point, double tolerance) { return GeometryContainment.Unsupported; }

    // Conservative parent-space bounds of a leaf's solid; Unsupported by default.
    default void getSolidBounds(GeometryBounds result) { result.setUnsupported(); }

    // Solid segments of a leaf in this operand's parent coordinates.
    default RaySolidQueryStatus classifyRay(Ray ray, RayCrossingQuery query, RaySolidClassification result) { result.clear(); return RaySolidQueryStatus.Unsupported; }

    // Counter that changes whenever this view's solid or placement is edited
    // through its API; false when the view does not track its edits.
    default boolean queryModificationVersion(long[] version) { return false; }
}

