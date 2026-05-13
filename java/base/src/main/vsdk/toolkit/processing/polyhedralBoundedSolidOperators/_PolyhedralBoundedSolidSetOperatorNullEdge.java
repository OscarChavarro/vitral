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

    private static int comparePoint(Vector3D a, Vector3D b)
    {
        int cmpX = PolyhedralBoundedSolidNumericPolicy.compare(
            a.x(), b.x(), numericContext.bigEpsilon());
        if ( cmpX != 0 ) {
            return cmpX;
        }

        int cmpY = PolyhedralBoundedSolidNumericPolicy.compare(
            a.y(), b.y(), numericContext.bigEpsilon());
        if ( cmpY != 0 ) {
            return cmpY;
        }

        return PolyhedralBoundedSolidNumericPolicy.compare(
            a.z(), b.z(), numericContext.bigEpsilon());
    }

    private static Vector3D canonicalFirstEndpoint(
        _PolyhedralBoundedSolidEdge edge)
    {
        Vector3D right;
        Vector3D left;

        right = edge.rightHalf.startingVertex.position;
        left = edge.leftHalf.startingVertex.position;
        if ( comparePoint(right, left) <= 0 ) {
            return right;
        }
        return left;
    }

    private static Vector3D canonicalSecondEndpoint(
        _PolyhedralBoundedSolidEdge edge)
    {
        Vector3D right;
        Vector3D left;

        right = edge.rightHalf.startingVertex.position;
        left = edge.leftHalf.startingVertex.position;
        if ( comparePoint(right, left) <= 0 ) {
            return left;
        }
        return right;
    }

    public int compareTo(_PolyhedralBoundedSolidSetOperatorNullEdge other)
    {
        int firstEndpointComparison;

        firstEndpointComparison = comparePoint(
            canonicalFirstEndpoint(this.e),
            canonicalFirstEndpoint(other.e));
        if ( firstEndpointComparison != 0 ) {
            return firstEndpointComparison;
        }
        return comparePoint(
            canonicalSecondEndpoint(this.e),
            canonicalSecondEndpoint(other.e));
    }

    @Override
    public String toString()
    {
        return e.toString() + " (sorted with respect to segment " +
            canonicalFirstEndpoint(this.e) + " -> " +
            canonicalSecondEndpoint(this.e) + ")";
    }
}
