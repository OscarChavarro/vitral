package vsdk.toolkit.environment.geometry.boundingVolumeHierarchy;

import vsdk.toolkit.environment.geometry.element.Ray;

/**
Conservative ray filter evaluated before an exact ray/geometry hit: false
positives are allowed, false negatives are forbidden.
*/
public abstract class PreRayHitElement {
    /** Filter for ray parameters in [0, +inf). */
    public abstract boolean mayIntersect(Ray ray);

    /** Filter for ray parameters in [0, maxT]; the unbounded filter is a
    valid default because false positives are allowed. */
    public boolean mayIntersectBefore(Ray ray, double maxT)
    {
        return mayIntersect(ray);
    }
}
