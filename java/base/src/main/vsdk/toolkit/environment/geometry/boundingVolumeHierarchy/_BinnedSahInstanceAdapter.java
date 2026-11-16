//= References:                                                             =
//= [WALD2007] Wald, Ingo. "On fast Construction of SAH-based Bounding      =
//=     Volume Hierarchies". Proceedings of the 2007 IEEE Symposium on      =
//=     Interactive Ray Tracing, 2007, pp. 33-40.                           =

package vsdk.toolkit.environment.geometry.boundingVolumeHierarchy;

import vsdk.toolkit.environment.geometry.element.Ray;

/** Leaf test of the inner hierarchy: takes the ray to object space */
class _BinnedSahInstanceAdapter implements BinnedSahLeafVisitor {
    final BinnedSahInstanceHierarchy owner;
    final Ray worldRay;
    final BinnedSahInstanceVisitor visitor;

    _BinnedSahInstanceAdapter(BinnedSahInstanceHierarchy inOwner,
        Ray inWorldRay, BinnedSahInstanceVisitor inVisitor)
    {
        owner = inOwner;
        worldRay = inWorldRay;
        visitor = inVisitor;
    }

    @Override
    public void visit(int instance, double[] inOutBestT)
    {
        visitor.visit(instance, owner.toObjectSpace(instance, worldRay), inOutBestT);
    }
}
