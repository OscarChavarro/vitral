package vsdk.toolkit.environment.geometry.boundingVolumeHierarchy;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;

/**
Reciprocals of a ray direction used by the slab test against axis aligned
boxes (Kay and Kajiya 1986). A component with magnitude under `1e-12` is
degenerate: the ray is parallel to that slab, its reciprocal is stored as zero
and the slab test checks the origin coordinate instead. It is computed once
per ray direction on the hottest path of a render.
*/
public final class RaySlabReciprocals {
    private final double inverseX;
    private final double inverseY;
    private final double inverseZ;
    private final boolean degenerateX;
    private final boolean degenerateY;
    private final boolean degenerateZ;

    public RaySlabReciprocals()
    {
        inverseX = 0.0;
        inverseY = 0.0;
        inverseZ = 0.0;
        degenerateX = true;
        degenerateY = true;
        degenerateZ = true;
    }

    public RaySlabReciprocals(Vector3Dd direction)
    {
        degenerateX = direction.x() > -1e-12 && direction.x() < 1e-12;
        degenerateY = direction.y() > -1e-12 && direction.y() < 1e-12;
        degenerateZ = direction.z() > -1e-12 && direction.z() < 1e-12;
        inverseX = degenerateX ? 0.0 : 1.0 / direction.x();
        inverseY = degenerateY ? 0.0 : 1.0 / direction.y();
        inverseZ = degenerateZ ? 0.0 : 1.0 / direction.z();
    }

    public double getInverseX() { return inverseX; }
    public double getInverseY() { return inverseY; }
    public double getInverseZ() { return inverseZ; }
    public boolean isDegenerateX() { return degenerateX; }
    public boolean isDegenerateY() { return degenerateY; }
    public boolean isDegenerateZ() { return degenerateZ; }
}
