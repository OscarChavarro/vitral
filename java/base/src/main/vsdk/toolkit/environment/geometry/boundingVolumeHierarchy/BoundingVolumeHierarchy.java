package vsdk.toolkit.environment.geometry.boundingVolumeHierarchy;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;

/**
Bounding volume of a geometry or of a group of geometries. All queries are
conservative: false positives are allowed, false negatives are not.
*/
public abstract class BoundingVolumeHierarchy extends PreRayHitElement {
    public abstract boolean containsPoint(Vector3Dd point, double tolerance);
    public abstract boolean isUnbounded();

    /** Conservative axis aligned extent of the whole volume. */
    public abstract AxisAlignedBoundingBox axisAlignedExtent();

    /** Caller owns the returned copy. */
    public abstract BoundingVolumeHierarchy copy();
}
