package vsdk.toolkit.environment.geometry;

import java.io.Serial;

/**
RayHit describes the result of a ray/geometry intersection.
It extends GeometryIntersectionInformation for backwards compatibility.
*/
public class RayHit extends GeometryIntersectionInformation {
    @Serial
    private static final long serialVersionUID = 20260419L;

    public RayHit()
    {
        super();
    }

    public RayHit(RayHit other)
    {
        super(other);
    }

    public final void clone(RayHit other)
    {
        super.clone(other);
    }
}
