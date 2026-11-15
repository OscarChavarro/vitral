package vsdk.toolkit.environment.scene.csg;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.element.GeometryContainment;
import vsdk.toolkit.environment.scene.SimpleBody;

/**
An application body that preserves its core solid for equivalence but
cannot certify point containment through its own policies.
*/
public class UncertifiedTestBody extends SimpleBody {
    @Override public GeometryContainment classifyPointCertified(Vector3Dd point, double tolerance)
    { return GeometryContainment.Unsupported; }
    @Override public Geometry getSolidEquivalenceGeometry() { return getGeometry(); }
}
