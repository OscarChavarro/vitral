package vsdk.toolkit.environment.geometry;

import java.io.Serial;

import vsdk.toolkit.common.FundamentalEntity;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.RaytraceProfiling;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.NormalMap;
import vsdk.toolkit.environment.Material;

/**
RayHit describes the result of a ray/geometry intersection.
*/
public class RayHit extends FundamentalEntity {
    @Serial
    private static final long serialVersionUID = 20260420L;

    public static final int DETAIL_NONE = 0;
    public static final int DETAIL_POINT = 1 << 0;
    public static final int DETAIL_NORMAL = 1 << 1;
    public static final int DETAIL_UV = 1 << 2;
    public static final int DETAIL_TANGENT = 1 << 3;
    public static final int DETAIL_ALL =
        DETAIL_POINT | DETAIL_NORMAL | DETAIL_UV | DETAIL_TANGENT;

    private static final Vector3D ZERO_VECTOR = new Vector3D();

    public Vector3D p; // Intersection point coordinates
    public Vector3D n; // Surface normal at intersection point
    public Vector3D t; // Surface tangent at intersection point
    // Note that surface binormal at intersection point must be calculated
    // by the application as the cross product (n x t).

    public double u; // Texture coordinate of intersection point
    public double v;

    // This can be null.
    public Material material; // Internal geometry selected material

    // This can be null.
    public Image texture; // Internal geometry selected texture map

    // This can be null.
    public NormalMap normalMap; // Internal geometry selected normal map

    // This can be null for miss results.
    private Ray ray; // Intersected ray with valid t at hit point

    private int requiredDetailMask;
    private boolean storeRay;
    private double hitDistance;
    private boolean hasHitDistance;

    public RayHit()
    {
        this(DETAIL_ALL, true);
    }

    public RayHit(int requiredDetailMask)
    {
        this(requiredDetailMask, true);
    }

    public RayHit(int requiredDetailMask, boolean storeRay)
    {
        this.requiredDetailMask = requiredDetailMask;
        this.storeRay = storeRay;
        clear();
        RaytraceProfiling.recordRayHitInstance();
    }

    public RayHit(RayHit other)
    {
        this(other.requiredDetailMask);
        clone(other);
    }

    public final void clear()
    {
        p = ZERO_VECTOR;
        n = ZERO_VECTOR;
        t = ZERO_VECTOR;
        u = 0;
        v = 0;
        material = null;
        texture = null;
        normalMap = null;
        ray = null;
        hitDistance = 0;
        hasHitDistance = false;
    }

    public final void reset(int newRequiredDetailMask)
    {
        requiredDetailMask = newRequiredDetailMask;
        clear();
    }

    public void resetForDistanceOnly()
    {
        requiredDetailMask = DETAIL_NONE;
        ray = null;
        hitDistance = 0;
        hasHitDistance = false;
    }

    public final void clone(RayHit other)
    {
        RaytraceProfiling.recordHitInfoClone();
        this.requiredDetailMask = other.requiredDetailMask;
        this.storeRay = other.storeRay;
        this.hitDistance = other.hitDistance;
        this.hasHitDistance = other.hasHitDistance;
        this.p = other.p;
        this.n = other.n;
        this.t = other.t;
        this.u = other.u;
        this.v = other.v;
        this.material = other.material;
        this.texture = other.texture;
        this.normalMap = other.normalMap;
        this.ray = other.ray;
    }

    public int requiredDetailMask()
    {
        return requiredDetailMask;
    }

    public void setRequiredDetailMask(int requiredDetailMask)
    {
        this.requiredDetailMask = requiredDetailMask;
    }

    public boolean shouldStoreRay()
    {
        return storeRay;
    }

    public void setStoreRay(boolean storeRay)
    {
        this.storeRay = storeRay;
    }

    public boolean needsPoint()
    {
        return (requiredDetailMask & DETAIL_POINT) != 0;
    }

    public boolean needsNormal()
    {
        return (requiredDetailMask & DETAIL_NORMAL) != 0;
    }

    public boolean needsTextureCoordinates()
    {
        return (requiredDetailMask & DETAIL_UV) != 0;
    }

    public boolean needsTangent()
    {
        return (requiredDetailMask & DETAIL_TANGENT) != 0;
    }

    public boolean needsAnySurfaceData()
    {
        return requiredDetailMask != DETAIL_NONE;
    }

    public Ray ray()
    {
        return ray;
    }

    public void setRay(Ray ray)
    {
        this.ray = ray;
        if ( ray != null ) {
            hitDistance = ray.t();
            hasHitDistance = true;
        }
    }

    public boolean hasHitDistance()
    {
        return hasHitDistance;
    }

    public double hitDistance()
    {
        if ( hasHitDistance ) {
            return hitDistance;
        }
        if ( ray != null ) {
            return ray.t();
        }
        return 0;
    }

    public void setHitDistance(double hitDistance)
    {
        this.hitDistance = hitDistance;
        this.hasHitDistance = true;
    }
}
