//= References:                                                             =
//= [WALD2007] Wald, Ingo. "On fast Construction of SAH-based Bounding      =
//=     Volume Hierarchies". Proceedings of the 2007 IEEE Symposium on      =
//=     Interactive Ray Tracing, 2007, pp. 33-40.                           =

package vsdk.toolkit.environment.geometry.boundingVolumeHierarchy;

import vsdk.toolkit.environment.geometry.element.Ray;

/**
Leaf test of `BinnedSahInstanceHierarchy.traverseClosest`: tests the
geometry of one instance with the ray already taken to its object space,
and lowers `inOutBestT[0]` when it finds a closer hit (or applies its own
tie rule, for instance the lowest instance index on equal distances).
*/
public interface BinnedSahInstanceVisitor {
    /**
    @param objectRay the world ray with origin and direction multiplied by
    the world to object matrix of the instance; the direction is not
    normalized, so a parameter `t` along it is the same `t` along the world
    ray, and `inOutBestT[0]` is comparable across instances
    @param inOutBestT one-element input/output holder
    */
    void visit(int instance, Ray objectRay, double[] inOutBestT);
}
