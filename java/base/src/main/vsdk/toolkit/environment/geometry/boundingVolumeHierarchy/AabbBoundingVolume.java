package vsdk.toolkit.environment.geometry.boundingVolumeHierarchy;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.Ray;

/** Bounding volume made of one axis aligned box. */
public final class AabbBoundingVolume extends BoundingVolumeHierarchy {
    private final AxisAlignedBoundingBox bounds;

    public AabbBoundingVolume()
    {
        bounds = AxisAlignedBoundingBox.unbounded();
    }

    public AabbBoundingVolume(AxisAlignedBoundingBox bounds)
    {
        this.bounds = bounds;
    }

    public AabbBoundingVolume(Vector3Dd min, Vector3Dd max)
    {
        bounds = new AxisAlignedBoundingBox(min, max);
    }

    public AxisAlignedBoundingBox getBounds()
    {
        return bounds;
    }

    @Override
    public boolean mayIntersect(Ray ray)
    {
        return bounds.intersectsRayForward(ray);
    }

    @Override
    public boolean mayIntersectBefore(Ray ray, double maxT)
    {
        return bounds.intersectsRayBefore(ray, maxT);
    }

    @Override
    public boolean containsPoint(Vector3Dd point, double tolerance)
    {
        return bounds.containsPointWithTolerance(point, tolerance);
    }

    @Override
    public boolean isUnbounded()
    {
        return bounds.isUnbounded();
    }

    @Override
    public AxisAlignedBoundingBox axisAlignedExtent()
    {
        return bounds;
    }

    @Override
    public BoundingVolumeHierarchy copy()
    {
        return new AabbBoundingVolume(bounds);
    }
}
