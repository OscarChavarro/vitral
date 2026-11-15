package vsdk.toolkit.environment.geometry.surface;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;

/**
Node of the bounding sphere tree of an adaptively subdivided Bezier patch:
a leaf holds the four corners of a flat piece, an interior node the indices
of its two or four children in the tree's node list.
*/
public class SubdividedBezierPatchNode {
    public boolean leaf;
    public Vector3Dd center;
    public double radiusSquared;
    public Vector3Dd[] corners = new Vector3Dd[4];
    public long[] children = new long[4];
    public int childCount;

    public SubdividedBezierPatchNode()
    {
        leaf = true; center = new Vector3Dd(); radiusSquared = 0; childCount = 0;
        for (int i = 0; i < 4; i++) corners[i] = new Vector3Dd();
        for (int i = 0; i < 4; i++) children[i] = -1;
    }

    /** cpp copy constructor. */
    public SubdividedBezierPatchNode(SubdividedBezierPatchNode other)
    {
        leaf = other.leaf; center = other.center; radiusSquared = other.radiusSquared;
        childCount = other.childCount;
        System.arraycopy(other.corners, 0, corners, 0, 4);
        System.arraycopy(other.children, 0, children, 0, 4);
    }
}
