package vsdk.toolkit.environment.geometry.boundingVolumeHierarchy;

/**
One node of a `BinnedSahHierarchy`, the double precision form of
`VulkanBvhNode` (`CasNode` of `computeAccelerationStructure.glsl`):

- inner node (`count == 0`): children at `leftOrFirst` and `leftOrFirst + 1`;
- leaf (`count > 0`): entries `leftOrFirst` .. `leftOrFirst + count - 1`
  of the primitive order.

As in [WALD2007].3.6: the child pair of an inner node is stored next to
each other, a leaf refers to one contiguous block of the primitive ids.
*/
public class BinnedSahHierarchyNode {
    public final double[] lo = new double[3];
    public int leftOrFirst;
    public final double[] hi = new double[3];
    public int count;

    public BinnedSahHierarchyNode()
    {
    }

    /** Value copy (cpp copies nodes by value) */
    public BinnedSahHierarchyNode(BinnedSahHierarchyNode other)
    {
        for ( int a = 0; a < 3; a++ ) {
            lo[a] = other.lo[a];
            hi[a] = other.hi[a];
        }
        leftOrFirst = other.leftOrFirst;
        count = other.count;
    }
}
