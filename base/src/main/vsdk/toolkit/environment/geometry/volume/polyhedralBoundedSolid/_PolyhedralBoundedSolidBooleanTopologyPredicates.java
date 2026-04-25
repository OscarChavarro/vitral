//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =
//= [.wMANT2008] Mantyla Martti. "Personal Home Page", <<shar>> archive     =
//=     containing the C programs from [MANT1988]. Available at             =
//=     http://www.cs.hut.fi/~mam . Last visited April 12 / 2008.           =

package vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid;

import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;

/**
Utility predicates for Boolean topology support on polyhedral bounded solids.
*/
public final class _PolyhedralBoundedSolidBooleanTopologyPredicates
{
    private _PolyhedralBoundedSolidBooleanTopologyPredicates()
    {
    }

    public static boolean planesCoincidentIgnoringOrientation(
        InfinitePlane a,
        InfinitePlane b,
        double tolerance)
    {
        double a1;
        double b1;
        double c1;
        double d1;
        double a2;
        double b2;
        double c2;
        double d2;
        double l1;
        double l2;

        if ( a == null || b == null ) {
            return false;
        }

        a1 = a.getA();
        b1 = a.getB();
        c1 = a.getC();
        d1 = a.getD();
        a2 = b.getA();
        b2 = b.getB();
        c2 = b.getC();
        d2 = b.getD();

        l1 = Math.sqrt(a1*a1 + b1*b1 + c1*c1);
        l2 = Math.sqrt(a2*a2 + b2*b2 + c2*c2);
        if ( l1 <= tolerance || l2 <= tolerance ) {
            return false;
        }

        a1 /= l1;
        b1 /= l1;
        c1 /= l1;
        d1 /= l1;
        a2 /= l2;
        b2 /= l2;
        c2 /= l2;
        d2 /= l2;

        boolean sameOrientation =
            Math.abs(a2 - a1) <= tolerance &&
            Math.abs(b2 - b1) <= tolerance &&
            Math.abs(c2 - c1) <= tolerance &&
            Math.abs(d2 - d1) <= tolerance;

        boolean oppositeOrientation =
            Math.abs(a2 + a1) <= tolerance &&
            Math.abs(b2 + b1) <= tolerance &&
            Math.abs(c2 + c1) <= tolerance &&
            Math.abs(d2 + d1) <= tolerance;

        return sameOrientation || oppositeOrientation;
    }

    public static boolean loopsCoincidentFrom(
        _PolyhedralBoundedSolidHalfEdge startA,
        _PolyhedralBoundedSolidHalfEdge startB,
        boolean reverse,
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext)
    {
        _PolyhedralBoundedSolidHalfEdge heA;
        _PolyhedralBoundedSolidHalfEdge heB;

        heA = startA;
        heB = startB;
        do {
            if ( !PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                heA.startingVertex.position, heB.startingVertex.position,
                numericContext) ) {
                return false;
            }
            heA = heA.next();
            heB = reverse ? heB.previous() : heB.next();
        } while ( heA != startA );

        return true;
    }

    public static boolean loopsCoincident(
        _PolyhedralBoundedSolidLoop a,
        _PolyhedralBoundedSolidLoop b,
        PolyhedralBoundedSolidNumericPolicy.ToleranceContext numericContext)
    {
        int i;
        _PolyhedralBoundedSolidHalfEdge startA;
        _PolyhedralBoundedSolidHalfEdge scanB;

        if ( a == null || b == null ||
             a.boundaryStartHalfEdge == null || b.boundaryStartHalfEdge == null ) {
            return false;
        }
        if ( a.halfEdgesList.size() != b.halfEdgesList.size() ) {
            return false;
        }

        startA = a.boundaryStartHalfEdge;
        scanB = b.boundaryStartHalfEdge;
        for ( i = 0; i < b.halfEdgesList.size(); i++ ) {
            if ( PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                startA.startingVertex.position, scanB.startingVertex.position,
                numericContext) ) {
                if ( loopsCoincidentFrom(startA, scanB, false, numericContext) ||
                     loopsCoincidentFrom(startA, scanB, true, numericContext) ) {
                    return true;
                }
            }
            scanB = scanB.next();
        }

        return false;
    }
}
