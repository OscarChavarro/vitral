package vsdk.toolkit.processing;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidEdge;

/**
Class `_PolyhedralBoundedSolidSplitterNullEdge` plays a role of a decorator
design patern for class `_PolyhedralBoundedSolidEdge`, and adds sort-ability.
*/
public class _PolyhedralBoundedSolidSetOperatorNullEdge
    extends PolyhedralBoundedSolidOperator
    implements Comparable<_PolyhedralBoundedSolidSetOperatorNullEdge>
{
    public _PolyhedralBoundedSolidEdge e;

    public _PolyhedralBoundedSolidSetOperatorNullEdge(
        _PolyhedralBoundedSolidEdge e)
    {
        this.e = e;
    }

    public int compareTo(_PolyhedralBoundedSolidSetOperatorNullEdge other)
    {
        Vector3D a;
        Vector3D b;

        a = this.e.rightHalf.startingVertex.position;
        b = other.e.rightHalf.startingVertex.position;

        if ( PolyhedralBoundedSolid.compareValue(a.x, b.x, 10*VSDK.EPSILON) != 0 ) {
            if ( a.x < b.x ) {
                return -1;
            }
            return 1;
        }
        else {
            if ( PolyhedralBoundedSolid.compareValue(a.y, b.y, 10*VSDK.EPSILON) != 0 ) {
                if ( a.y < b.y ) {
                    return -1;
                }
                return 1;
            }
            else {
                if ( a.z < b.z ) {
                    return -1;
                }
                return 1;
            }
        }
    }

    @Override
    public String toString()
    {
        return e.toString() + " (sorted with respect to position " + this.e.rightHalf.startingVertex.position + ")";
    }
}
