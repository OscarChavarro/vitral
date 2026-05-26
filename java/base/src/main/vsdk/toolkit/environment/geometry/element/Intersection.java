package vsdk.toolkit.environment.geometry.element;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;

public class Intersection {
    public final double t;
    public final Vector3Dd point;
    public final Vector3Dd normal;

    public Intersection(double t, Vector3Dd point, Vector3Dd normal) {
        this.t = t;
        this.point = point;
        this.normal = normal;
    }
}
