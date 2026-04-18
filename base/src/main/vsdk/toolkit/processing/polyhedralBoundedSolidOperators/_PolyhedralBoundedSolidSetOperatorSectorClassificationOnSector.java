//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import vsdk.toolkit.processing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidOperator;

import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;

/**
This class is used to store sector / sector neigborhood information for the
vertex/vertex classifier as proposed on section [MANT1988].15.5. and program
[MANT1988].15.6.
*/
public class _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector
    extends PolyhedralBoundedSolidOperator
{
    public int secta;
    public int sectb;
    public int s1a;
    public int s2a;
    public int s1b;
    public int s2b;
    public boolean intersect;
    public _PolyhedralBoundedSolidHalfEdge hea;
    public _PolyhedralBoundedSolidHalfEdge heb;
    public boolean wa;
    public boolean wb;
    public static final int ON = 0;
    public static final int OUT = 1;
    public static final int IN = -1;

    private String label(int i)
    {
        String msg = "<Unknown>";
        switch ( i ) {
          case ON: msg = "on"; break;
          case OUT: msg = "OUT"; break;
          case IN: msg = "IN"; break;
        }
        return msg;
    }

    public void fillCases()
    {
        if ( s1a == ON ) {
            switch ( s2a ) {
            case IN: s1a = OUT; break;
            case OUT: s1a = IN; break;
            }
        }
        if ( s2a == ON ) {
            switch ( s1a ) {
            case IN: s2a = OUT; break;
            case OUT: s2a = IN; break;
            }
        }
        if ( s1b == ON ) {
            switch ( s2b ) {
            case IN: s1b = OUT; break;
            case OUT: s1b = IN; break;
            }
        }
        if ( s2b == ON ) {
            switch ( s1b ) {
            case IN: s2b = OUT; break;
            case OUT: s2b = IN; break;
            }
        }
    }

    public String toString()
    {
        String msg = "Sector pair ";

        msg = msg + "A[" + (secta+1) + "] / B[" + (sectb+1) + "]: ";

        msg = msg + "VERTICES ( " +
            hea.startingVertex.id + "-" +
            (hea.next()).startingVertex.id + (wa?"(W)":"(nw)") + " / " +
            heb.startingVertex.id + "-" +
            (heb.next()).startingVertex.id + (wb?"(W)":"(nw)") + " ) - ";
        msg = msg + "[" + label(s1a) + "/" + label(s2a) + ", " + label(s1b) + "/" + label(s2b) + "] ";
        if ( intersect ) {
            msg = msg + "intersecting";
        }
        else {
            msg = msg + "(droped)";
        }

        if ( s1a != 0 && s1b != 0 && s2a != 0 && s2b != 0 && intersect ) {
            msg += " (**) ";
        }

        return msg;
    }
}
