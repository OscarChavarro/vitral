//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;

/**
Class `_PolyhedralBoundedSolidSetOperatorNullEdge` decorates a null edge so it
can participate in the connect stage driven by the `sonea`/`soneb` sets of
program [MANT1988].15.1 and later consumed by section [MANT1988].15.7.
*/
public class _PolyhedralBoundedSolidSetOperatorNullEdge
    extends _PolyhedralBoundedSolidOperator
    implements Comparable<_PolyhedralBoundedSolidSetOperatorNullEdge>
{
    private static PolyhedralBoundedSolidNumericPolicy.ToleranceContext
        numericContext = PolyhedralBoundedSolidNumericPolicy.defaultContext();

    public _PolyhedralBoundedSolidEdge e;

    public _PolyhedralBoundedSolidSetOperatorNullEdge(
        _PolyhedralBoundedSolidEdge e)
    {
        this.e = e;
    }

    public static void setNumericContext(
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext context)
    {
        if ( context == null ) {
            numericContext = PolyhedralBoundedSolidNumericPolicy.defaultContext();
        }
        else {
            numericContext = context;
        }
    }

    public int compareTo(_PolyhedralBoundedSolidSetOperatorNullEdge other)
    {
        Vector3D a;
        Vector3D b;

        a = this.e.rightHalf.startingVertex.position;
        b = other.e.rightHalf.startingVertex.position;

        if ( PolyhedralBoundedSolidNumericPolicy
            .compare(a.x(), b.x(), numericContext.bigEpsilon()) != 0 ) {
            if ( a.x() < b.x() ) {
                return -1;
            }
            return 1;
        }
        else {
            if ( PolyhedralBoundedSolidNumericPolicy
                .compare(a.y(), b.y(), numericContext.bigEpsilon()) != 0 ) {
                if ( a.y() < b.y() ) {
                    return -1;
                }
                return 1;
            }
            else {
                if ( a.z() < b.z() ) {
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
