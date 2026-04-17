package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import vsdk.toolkit.processing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidOperator;

import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.InfinitePlane;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;

/**
This class is used to store vertex / halfedge neigborhood information for the
vertex/face classifier, in a similar fashion to as presented in section
[MANT1988].14.5, and program [MANT1988].14.3., but biased for the set
operation algorithm as proposed on section [MANT1988].15..1. and problem
[MANT1988].15.4.
*/
public class _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
    extends PolyhedralBoundedSolidOperator
{
    public static final int ABOVE = 1;
    public static final int BELOW = -1;
    public static final int ON = 0;

    public static final int AinB = 11;
    public static final int AoutB = 12;
    public static final int BinA = 13;
    public static final int BoutA = 14;
    public static final int AonBplus = 15;
    public static final int AonBminus = 16;
    public static final int BonAplus = 17;
    public static final int BonAminus = 18;

    public static final int COPLANAR_FACE = 10;
    public static final int INPLANE_EDGE = 20;
    public static final int CROSSING_EDGE = 30;
    public static final int UNDEFINED = 40;

    public static final int COPLANAR_UNKNOWN = 0;
    public static final int COPLANAR_DISJOINT = 1;
    public static final int COPLANAR_TOUCHING = 2;
    public static final int COPLANAR_OVERLAP = 3;

    public _PolyhedralBoundedSolidHalfEdge sector;
    public InfinitePlane referencePlane;
    public int cl;

    // Following attributes are not taken from [MANT1988], and all operations
    // on them are fine tunning options aditional to original algorithm.
    public boolean isWide = false;
    public Vector3D position;
    public int situation = UNDEFINED;
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
        this.cl = other.cl;
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
            switch ( cl ) {
              case AonBplus:     cl = AoutB;    break;
              case AonBminus:    cl = AinB;    break;
              case BonAplus:     cl = BinA;    break;
              case BonAminus:    cl = BinA;    break;
            }
        }
        else if ( op == INTERSECTION ) {
            switch ( cl ) {
              case AonBplus:     cl = AinB;    break;
              case AonBminus:    cl = AoutB;    break;
              case BonAplus:     cl = BoutA;    break;
              case BonAminus:    cl = BoutA;    break;
            }
        }
        else if ( op == DIFFERENCE ) {
            switch ( cl ) {
              case AonBplus:     cl = AinB;    break;
              case AonBminus:    cl = AoutB;    break;
              case BonAplus:     cl = BoutA;    break;
              case BonAminus:    cl = BoutA;    break;
            }
        }
    }

    public void updateLabel(int BvsA)
    {
        InfinitePlane a = sector.parentLoop.parentFace.containingPlane;
        InfinitePlane b = referencePlane;

        if ( BvsA == 0 ) {
            switch ( cl ) {
              case ABOVE: cl = AoutB; break;
              case BELOW: cl = AinB; break;
              case ON:
                if ( coplanarRelation != COPLANAR_UNKNOWN &&
                     coplanarRelation != COPLANAR_OVERLAP ) {
                    cl = AoutB;
                }
                else if ( a.overlapsWith(b, numericContext.bigEpsilon()) ) {
                    cl = AonBplus;
                }
                else {
                    cl = AonBminus;
                }
                break;
            }
        }
        else {
            switch ( cl ) {
              case ABOVE: cl = BoutA; break;
              case BELOW: cl = BinA; break;
              case ON:
                if ( coplanarRelation != COPLANAR_UNKNOWN &&
                     coplanarRelation != COPLANAR_OVERLAP ) {
                    cl = BoutA;
                }
                else if ( a.overlapsWith(b, numericContext.bigEpsilon()) ) {
                    cl = BonAplus;
                }
                else {
                    cl = BonAminus;
                }
                break;
            }
        }
    }

    public String toString()
    {
        String msg = "Sector(";
        msg = msg + sector + " | ";
        switch ( cl ) {
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
