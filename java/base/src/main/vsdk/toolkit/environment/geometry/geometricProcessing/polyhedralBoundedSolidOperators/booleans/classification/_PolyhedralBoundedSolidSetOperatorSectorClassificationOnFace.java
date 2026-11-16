//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;

/**
This class is used to store vertex / halfedge neigborhood information for the
vertex/face classifier, in a similar fashion to as presented in section
[MANT1988].14.5, and program [MANT1988].14.3., but biased for the set
operation algorithm as proposed on section [MANT1988].15..1. and problem
[MANT1988].15.4.
*/
public class _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
    extends _PolyhedralBoundedSolidSectorClassification
{
    public static final int AinB = 11;
    public static final int AoutB = 12;
    public static final int BinA = 13;
    public static final int BoutA = 14;
    public static final int AonBplus = 15;
    public static final int AonBminus = 16;
    public static final int BonAplus = 17;
    public static final int BonAminus = 18;

    public static final int COPLANAR_UNKNOWN = 0;
    public static final int COPLANAR_DISJOINT = 1;
    public static final int COPLANAR_TOUCHING = 2;
    public static final int COPLANAR_OVERLAP = 3;

    public InfinitePlane referencePlane;

    // Following attributes are not taken from [MANT1988], and all operations
    // on them are fine tunning options aditional to original algorithm.
    public boolean reverse = false;
    public int coplanarRelation = COPLANAR_UNKNOWN;

    public _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace()
    {
        ;
    }

    public _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace(
        _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace other)
    {
        this.sector = other.sector;
        this.referencePlane = other.referencePlane;
        this.classification = other.classification;
        this.isWide = other.isWide;
        this.position = other.position;
        this.situation = other.situation;
        this.reverse = other.reverse;
        this.coplanarRelation = other.coplanarRelation;
    }

    /**
    Current method implements the set of changes from table [MANT1988].15.3.
    for the edge reclassification rules for the third stage of a vertex/face
    classifier.
    */
    public void applyRules(int op)
    {
        if ( op == UNION ) {
            switch ( classification ) {
              case AonBplus:     classification = AoutB;    break;
              case AonBminus:    classification = AinB;    break;
              case BonAplus:     classification = BinA;    break;
              case BonAminus:    classification = BinA;    break;
            }
        }
        else if ( op == INTERSECTION ) {
            switch ( classification ) {
              case AonBplus:     classification = AinB;    break;
              case AonBminus:    classification = AoutB;    break;
              case BonAplus:     classification = BoutA;    break;
              case BonAminus:    classification = BoutA;    break;
            }
        }
        else if ( op == SUBTRACT) {
            switch ( classification ) {
              case AonBplus:     classification = AinB;    break;
              case AonBminus:    classification = AoutB;    break;
              case BonAplus:     classification = BoutA;    break;
              case BonAminus:    classification = BoutA;    break;
            }
        }
    }

    public void updateLabel(int BvsA)
    {
        InfinitePlane a = sector.parentLoop.parentFace.getContainingPlane();
        InfinitePlane b = referencePlane;

        if ( BvsA == 0 ) {
            switch ( classification ) {
              case ABOVE: classification = AoutB; break;
              case BELOW: classification = AinB; break;
              case ON:
                if ( coplanarRelation != COPLANAR_UNKNOWN &&
                     coplanarRelation != COPLANAR_OVERLAP ) {
                    classification = AoutB;
                }
                else if ( a.overlapsWith(b, numericContext.bigEpsilon()) ) {
                    classification = AonBplus;
                }
                else {
                    classification = AonBminus;
                }
                break;
            }
        }
        else {
            switch ( classification ) {
              case ABOVE: classification = BoutA; break;
              case BELOW: classification = BinA; break;
              case ON:
                if ( coplanarRelation != COPLANAR_UNKNOWN &&
                     coplanarRelation != COPLANAR_OVERLAP ) {
                    classification = BoutA;
                }
                else if ( a.overlapsWith(b, numericContext.bigEpsilon()) ) {
                    classification = BonAplus;
                }
                else {
                    classification = BonAminus;
                }
                break;
            }
        }
    }

    public String toString()
    {
        String msg = "Sector(";
        msg = msg + sector + " | ";
        switch ( classification ) {
          case ABOVE: msg = msg + " ABOVE"; break;
          case BELOW: msg = msg + " BELOW"; break;
          case ON: msg = msg + " ON"; break;
          case AinB: msg = msg + "AinB"; break;
          case AoutB: msg = msg + "AoutB"; break;
          case BinA: msg = msg + "BinA"; break;
          case BoutA: msg = msg + "BoutA"; break;
          case AonBplus: msg = msg + "AonBplus"; break;
          case AonBminus: msg = msg + "AonBminus"; break;
          case BonAplus: msg = msg + "BonAplus"; break;
          case BonAminus: msg = msg + "BonAminus"; break;
          default: msg = msg + "<INVALID!>"; break;
        }
        msg = msg + " ";
        if ( isWide ) {
            msg = msg + "(W) ";
        }
        //msg = msg + ", pos: " + position;

        switch ( situation ) {
          case COPLANAR_FACE: msg = msg + "<COPLANAR_FACE>"; break;
          case INPLANE_EDGE: msg = msg + "<INPLANE_EDGE>"; break;
          case CROSSING_EDGE: msg = msg + "<CROSSING_EDGE>"; break;
          default: msg = msg + "<UNDEFINED>"; break;
        }

        msg = msg + ")";
        return msg;
    }
}
