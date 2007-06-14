//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - November 18 2006 - Oscar Chavarro: Original base version              =
//= - January 3 2007 - Oscar Chavarro: First phase implementation           =
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =
//===========================================================================

package vsdk.toolkit.environment.geometry;

import java.util.ArrayList;

import java.text.DecimalFormat;
import java.text.FieldPosition;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.CircularDoubleLinkedList;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolidNodes._PolyhedralBoundedSolidVertex;

/**
This class encapsulates a polyhedral boundary representation for 2-manifold
solids, as presented in [MANT1988]. As noted in [MANT1988].10.2.1:
The `PolyhedralBoundedSolid` class uses a five-level hierarchic data structure,
consisting of:
  - PolyhedralBoundedSolid
  - _PolyhedralBoundedSolidFace
  - _PolyhedralBoundedSolidLoop
  - _PolyhedralBoundedSolidHalfEdge (and _PolyhedralBoundedSolidEdge)
  - _PolyhedralBoundedSolidVertex
Current class forms the root element that gives access to faces, edges and
vertices of the model through agregations in CircularDoubleLinkedList's.
*/
public class PolyhedralBoundedSolid extends Solid {
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20061118L;

    public static final int PLUS = 1;
    public static final int MINUS = 0;

    //= Main boundary representation solid data structure =============
    public CircularDoubleLinkedList<_PolyhedralBoundedSolidFace> polygonsList;
    public CircularDoubleLinkedList<_PolyhedralBoundedSolidEdge> edgesList;
    public CircularDoubleLinkedList<_PolyhedralBoundedSolidVertex> verticesList;

    //=================================================================
    public PolyhedralBoundedSolid()
    {
        polygonsList =
            new CircularDoubleLinkedList<_PolyhedralBoundedSolidFace>();
        edgesList =
            new CircularDoubleLinkedList<_PolyhedralBoundedSolidEdge>();
        verticesList =
            new CircularDoubleLinkedList<_PolyhedralBoundedSolidVertex>();
    }

    //= SUPPORT MACROS FOR BASIC DATASTRUCTURE MANIPULATION ===========

    /**
    Find the face identified with `id`. Returns null if face not found,
    or current founded face otherwise.
    Build based over function `fface` in program [MANT1988].11.9.
    */
    public _PolyhedralBoundedSolidFace
    findFace(int id)
    {
        int i;
        _PolyhedralBoundedSolidFace facei;

        for ( i = 0; i < polygonsList.size(); i++ ) {
            facei = polygonsList.get(i);
            if ( facei.id == id ) {
                return facei;
            }
        }
        return null;
    }

    /**
    addhe: addHalfEdge.
    As described in section [MANT1988].11.2.2 and following the structure
    of sample program [MANT1988].11.3, there exist the need for a halfedge
    procedure creation where some special cases should be considered.
    */
    private _PolyhedralBoundedSolidHalfEdge addhe(
        _PolyhedralBoundedSolidEdge e,
        _PolyhedralBoundedSolidVertex v,
        _PolyhedralBoundedSolidHalfEdge where,
        int sign
    )
    {
        _PolyhedralBoundedSolidHalfEdge he;

        if ( where == null ) {
            VSDK.reportMessage(this, VSDK.FATAL_ERROR, "addhe",
            "Trying to build a halfedge from another, non-existing halfedge!");
        }
        if ( e == null ) {
            VSDK.reportMessage(this, VSDK.FATAL_ERROR, "addhe",
            "Trying to associate a halfedge to a non-existing edge!");
        }

        if ( where.parentEdge == null ) {
            he = where;
          }
          else {
            he =
                new _PolyhedralBoundedSolidHalfEdge(v, where.parentLoop, this);
            where.parentLoop.halfEdgesList.insertBefore(he, where);
            he.startingVertex = v;
        }
        he.parentEdge = e;
        he.parentLoop = where.parentLoop;

        if ( sign == PolyhedralBoundedSolid.PLUS ) {
            e.leftHalf = he;
          }
          else {
            e.rightHalf = he;
        }

        return he;
    }

    //= LOW LEVEL EULER OPERATIONS ====================================

    /**
    mvfs: MakeVertexFaceSolid.
    Operator mvfs creates a new solid representation that consist of
    one face and one vertex with coordinates specified in `p`. This
    operator has one single level of implementation (no "low level"
    or "high level versions") as other operator has.

    As described in sections [MANT1988].9.2.2, [MANT1988].11.3.1 and
    [MANT1988].11.5.1; and following the structure of sample program
    [MANT1988].11.5, this method should be used as part of every
    PolyhedralBoundedSolid constructor process, to yield to an empty
    skeletal boundary representation solid.

    Note that all correctly builded solids are the result of a series of
    Euler operations over this generated skeleton, the "single skeletal
    plane model" ([MANT1988].9.2.2). The "solid" created here may not
    satisfy the intuitive notion of a solid object. Nevertheless, it is
    useful as the initial state of creating a boundary model with a sequence
    of Euler operations.
    */
    public void mvfs(Vector3D p, int vertexId, int faceId)
    {
        _PolyhedralBoundedSolidFace newFace;
        _PolyhedralBoundedSolidLoop newLoop;
        _PolyhedralBoundedSolidHalfEdge newHalfEdge;
        _PolyhedralBoundedSolidVertex newVertex;

        newFace = new _PolyhedralBoundedSolidFace(this, faceId);
        newLoop = new _PolyhedralBoundedSolidLoop(newFace);
        newVertex = new _PolyhedralBoundedSolidVertex(this, p, vertexId);
        newHalfEdge =
            new _PolyhedralBoundedSolidHalfEdge(newVertex, newLoop, this);
        newLoop.halfEdgesList.add(newHalfEdge);
        newLoop.boundaryStartHalfEdge = newHalfEdge;
    }

    /**
    lmev: LowlevelMakeEdgeVertex (vertex splitting operation).
    Operator lmev "splits" the vertex pointed at by `he1` and `he2`,
    and adds a new vertex and new edge between the resulting two vertices.
    The coordinates specified by `p` are assigned to the new vertex position.
    If `he1` and `he2` are the same halfedge, the new vertex and edge are
    added into the face of `he1`. The new edge is oriented from the new
    vertex to the old one.

    As described in sections [MANT1988].9.2.3, [MANT1988].11.3.2 and
    [MANT1988].11.5.1; and following the structure of sample program
    [MANT1988].11.6, this method has the effect of adding one new vertex
    and one new edge to the solid model.
    */
    public void lmev(_PolyhedralBoundedSolidHalfEdge he1,
                     _PolyhedralBoundedSolidHalfEdge he2,
                     int vertexID, Vector3D p)
    {
        _PolyhedralBoundedSolidHalfEdge he;
        _PolyhedralBoundedSolidVertex newVertex;
        _PolyhedralBoundedSolidEdge newEdge;

        if ( he1 == null && he2 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "lmev",
            "Calling with empty halfedges!");
            return;
        }

        newEdge = new _PolyhedralBoundedSolidEdge(he1.parentLoop.parentFace.parentSolid);
        newVertex = new _PolyhedralBoundedSolidVertex(he1.parentLoop.parentFace.parentSolid, p, vertexID);

        he = he1;
        while ( he != he2 ) {
            he.startingVertex = newVertex;
            he = he.mirrorHalfEdge().next();
            if ( he == he1 ) break;
        }

        _PolyhedralBoundedSolidVertex oldVertex = he2.startingVertex;
        if ( he1.parentEdge != null ) {
            addhe(newEdge, oldVertex, he2, PLUS);
            addhe(newEdge, newVertex, he1, MINUS);
        }
        else {
            _PolyhedralBoundedSolidHalfEdge x, y;
            x = addhe(newEdge, oldVertex, he1, PLUS);
            y = addhe(newEdge, newVertex, he1, MINUS);
            //halfEdgesList.swapElements(x, y);
        }
        newVertex.emanatingHalfEdge = he2.previous();
        he2.startingVertex.emanatingHalfEdge = he2;
    }

    /**
    lmef: LowlevelMakeEdgeFace (face splitting operator).
    Operator lmef adds a new edge between halfedges `he1` and `he2`,
    and "splits" their common face into two faces such that `he1` will
    occur in the new face `f`, and `he2` remains in the old face. The
    new edge is oriented from he1.startingVertex to he2.startingVertex.
    Halfedges `he1` and `he2` must belong to the same loop (i.e.
    he1.parentLoop == he2.parentLoop ). They may be equal, in which case
    a "circular" face with just one edge is created. A pointer to the new
    face is returned.

    As described in sections [MANT1988].9.2.3, [MANT1988].11.3.3 and
    [MANT1988].11.5.1; and following the structure of sample program
    [MANT1988].11.7, this method is the dual to lmev. Note that creates
    the halfedges as usual, and then swaps them.
    */
    public _PolyhedralBoundedSolidFace lmef(
        _PolyhedralBoundedSolidHalfEdge he1,
        _PolyhedralBoundedSolidHalfEdge he2,
         int faceId)
    {
        _PolyhedralBoundedSolidFace newFace;
        _PolyhedralBoundedSolidLoop newLoop;
        _PolyhedralBoundedSolidLoop oldLoop;
        _PolyhedralBoundedSolidEdge newEdge;
        _PolyhedralBoundedSolidHalfEdge he, nhe1, nhe2, temp;

        newFace = new _PolyhedralBoundedSolidFace(he1.parentLoop.parentFace.parentSolid, faceId);
        oldLoop = he1.parentLoop;
        newLoop = new _PolyhedralBoundedSolidLoop(newFace);
        newEdge = new _PolyhedralBoundedSolidEdge(he1.parentLoop.parentFace.parentSolid);

        ArrayList<_PolyhedralBoundedSolidHalfEdge> migratedHalfEdges;
        migratedHalfEdges = new ArrayList<_PolyhedralBoundedSolidHalfEdge>();

        he = he1;
        while ( he != he2 ) {
            migratedHalfEdges.add(he);
            he = he.next();
            if ( he == he1 ) break;
        }

        int i;
        for ( i = 0; i < migratedHalfEdges.size(); i++ ) {
            he = migratedHalfEdges.get(i);
            he.parentLoop = newLoop;
            oldLoop.unlistHalfEdge(he);
            newLoop.halfEdgesList.add(he);
        }

        nhe1 = addhe(newEdge, he2.startingVertex, he1, MINUS);
        nhe2 = addhe(newEdge, he1.startingVertex, he2, PLUS);

        newLoop.boundaryStartHalfEdge = nhe1;
        he2.parentLoop.boundaryStartHalfEdge = nhe2;

        return newFace;
    }

    /**
    lkemr: LowlevelKillEdgeMakeRing (loop splitting operator).
    Operator lkemr removes the edge of `he1` and `he2`, and divides their
    common loop into two components (i.e., it creates a new "ring").  If
    the original loop was "outer", the component of `he1.startingVertex`
    becomes the new "outer" loop. (If this default is inappropiate, you
    can simply swap the arguments of lkemr, or use lringmv to make the
    desired loop "outer").  It is assumed that
    he1.parentEdge == he2.parentEdge and 
    he1.parentLoop == he2.parentLoop.

    As described in section [MANT1988].9.2.3, the operator splits a loop
    into two new ones by removing and edge that appears twice in it. Hence
    the operator divides a connected bounding curve of a face into two
    bounding curves, and has the net effect of removing one edge and adding
    one ring to the PolyhedralBoundedSolid data structure. The special
    cases that one or both of the resulting loops are empty are also
    included. Note that current code follows section [MANT1988].11.3.4
    and the structure of sample program [MANT1988].11.8.
    */
    public void lkemr(
        _PolyhedralBoundedSolidHalfEdge he1,
        _PolyhedralBoundedSolidHalfEdge he2)
    {
        //-----------------------------------------------------------------
        _PolyhedralBoundedSolidHalfEdge he3 = null, he4;
        _PolyhedralBoundedSolidLoop newLoop;
        _PolyhedralBoundedSolidLoop oldLoop;
        _PolyhedralBoundedSolidEdge killedEdge;

        oldLoop = he1.parentLoop;
        newLoop = new _PolyhedralBoundedSolidLoop(oldLoop.parentFace);
        killedEdge = he1.parentEdge;

        //-----------------------------------------------------------------
        ArrayList<_PolyhedralBoundedSolidHalfEdge> migratedHalfEdges;
        migratedHalfEdges = new ArrayList<_PolyhedralBoundedSolidHalfEdge>();

        he4 = he1.next();
        do {
            migratedHalfEdges.add(he4);
            if ( he4 == he2 ) break;
        } while ( (he4 = he4.next()) != he2 );

        //-----------------------------------------------------------------
        int i;
        for ( i = 0; i < migratedHalfEdges.size(); i++ ) {
            he3 = migratedHalfEdges.get(i);
            oldLoop.halfEdgesList.locateWindowAtElem(he3);
            oldLoop.halfEdgesList.removeElemAtWindow();
            newLoop.halfEdgesList.add(he3);
            he3.parentLoop = newLoop;
        }
        oldLoop.boundaryStartHalfEdge = oldLoop.halfEdgesList.get(0);
        newLoop.boundaryStartHalfEdge = newLoop.halfEdgesList.get(0);
        //-----------------------------------------------------------------
        // delhe(h1)
        oldLoop.halfEdgesList.locateWindowAtElem(he1);
        oldLoop.halfEdgesList.removeElemAtWindow();

        // delhe(h2)
        oldLoop.halfEdgesList.locateWindowAtElem(he2);
        oldLoop.halfEdgesList.removeElemAtWindow();

        if ( newLoop.halfEdgesList.size() <= 1 ) {
            newLoop.boundaryStartHalfEdge.parentEdge = null;
            newLoop.halfEdgesList.get(0).startingVertex.emanatingHalfEdge = null;
        }
        if ( oldLoop.halfEdgesList.size() <= 1 ) {
            oldLoop.boundaryStartHalfEdge.parentEdge = null;
            oldLoop.halfEdgesList.get(0).startingVertex.emanatingHalfEdge = null;
        }

        edgesList.locateWindowAtElem(killedEdge);
        edgesList.removeElemAtWindow();
    }

    //= HIGH LEVEL EULER OPERATIONS ===================================

    /**
    mev: (High level version) MakeEdgeVertex (vertex splitting operation).
    Operator `mev` divides the cicle of edges around the vertex `v1` so
    that all edges from `v1` -> `v2` (inclusive) to `v1` -> `v3` (exclusive)
    will become adjacent to a new vertex `v4`.  The vertices `v1` and `v4`
    are joined with a new edge.  Coordinates defined in `p` are assigned
    to `v4`.
    Similarly to the corresponding low level operator `lmev`, various
    special cases are possible.  Of particular use is the "line-drawing"
    case that `f1` = `f2` and `v2` = `v3`.  In this case, a new edge to a
    new vertex will be added within the face. We shall call such "dangling"
    edges <I>struts</I>.  This case occurs so frequently that it is included
    a "convenience" procedure `smev` that performs this operation.  The
    procedure assumes that `v1` appears just once in `f1`; hence, the
    argument `v2` can be left out.
    @return true if operation succeded, false otherway
    */
    public boolean mev(int f1, int f2,
                   int v1, int v2, int v3, int v4, Vector3D p)
    {
        _PolyhedralBoundedSolidFace oldface1, oldface2;
        _PolyhedralBoundedSolidHalfEdge he1, he2;

        oldface1 = findFace(f1);
        if ( oldface1 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "mev",
            "Face " + f1 + " not found.");
            return false;
        }
        oldface2 = findFace(f2);
        if ( oldface2 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "mev",
            "Face " + f2 + " not found.");
            return false;
        }
        he1 = oldface1.findHalfEdge(v1, v2);
        if ( he1 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "mev",
            "Edge " + v1 + " - " + v2 + " not found in face " + f1 + ".");
            return false;
        }
        he2 = oldface2.findHalfEdge(v1, v3);
        if ( he1 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "mev",
            "Edge " + v1 + " - " + v3 + " not found in face " + f2 + ".");
            return false;
        }
    lmev(he1, he2, v4, p);
        return true;
    }

    /**
    mev: (High level version) MakeEdgeFace (face splitting operation).
    Executes a `lmef` in halfedges v1-v2, v3-v4 in respective faces `f1`
    and `f2`, and assigns to the new face the `newfaceid`.
    */
    public boolean mef(int f1, int f2,
                       int v1, int v2, int v3, int v4, int newfaceid)
    {
        _PolyhedralBoundedSolidFace oldface1, oldface2;
        _PolyhedralBoundedSolidHalfEdge he1, he2;

        oldface1 = findFace(f1);
        if ( oldface1 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "mef",
            "Face " + f1 + " not found.");
            return false;
        }
        oldface2 = findFace(f2);
        if ( oldface2 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "mef",
            "Face " + f2 + " not found.");
            return false;
        }
        he1 = oldface1.findHalfEdge(v1, v2);
        if ( he1 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "mef",
            "Edge " + v1 + " - " + v2 + " not found in face " + f1 + ".");
            return false;
        }
        he2 = oldface2.findHalfEdge(v3, v4);
        if ( he1 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "mef",
            "Edge " + v3 + " - " + v3 + " not found in face " + f2 + ".");
            return false;
        }
    lmef(he1, he2, newfaceid);
        return true;
    }

    /**
    kemr: (High level version) KillEdgeMakeRing (loop splitting operation).
    Executes a `lkemr` in halfedges v1-v2, v3-v4 in respective faces `f1`
    and `f2`.
    */
    public boolean kemr(int f1, int f2,
                       int v1, int v2, int v3, int v4)
    {
        _PolyhedralBoundedSolidFace oldface1, oldface2;
        _PolyhedralBoundedSolidHalfEdge he1, he2;

        oldface1 = findFace(f1);
        if ( oldface1 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "kemr",
            "Face " + f1 + " not found.");
            return false;
        }
        oldface2 = findFace(f2);
        if ( oldface2 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "kemr",
            "Face " + f2 + " not found.");
            return false;
        }
        he1 = oldface1.findHalfEdge(v1, v2);
        if ( he1 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "kemr",
            "Edge " + v1 + " - " + v2 + " not found in face " + f1 + ".");
            return false;
        }
        he2 = oldface2.findHalfEdge(v3, v4);
        if ( he1 == null ) {
            VSDK.reportMessage(this, VSDK.WARNING, "kemr",
            "Edge " + v3 + " - " + v3 + " not found in face " + f2 + ".");
            return false;
        }
    lkemr(he1, he2);
        return true;
    }

    //=================================================================

    public boolean
    doIntersection(Ray inout_rayo) {
        VSDK.reportMessage(this, VSDK.WARNING, "doIntersection",
            "Method not implemented");
        return false;
    }

    public void
    doExtraInformation(Ray inRay, double inT, 
                                  GeometryIntersectionInformation outData) {
        VSDK.reportMessage(this, VSDK.WARNING, "doExtraInformation",
            "Method not implemented");
    }

    public double[] getMinMax()
    {
        double minmax[] = new double[6];
        for ( int i = 0; i < 3; i++ ) {
            minmax[i] = -1.0;
        }
        for ( int i = 3; i < 6; i++ ) {
            minmax[i] = 1.0;
        }

        VSDK.reportMessage(this, VSDK.WARNING, "getMinMax",
            "Method not implemented");

        return minmax;
    }

    //= TEXTUAL QUERY OPERATIONS ======================================

    private String intPreSpaces(int val, int fieldSize)
    {
        DecimalFormat f = new DecimalFormat("0");
        String cad;

        cad = f.format(val, new StringBuffer(""), new FieldPosition(0)).toString();

        int remain = fieldSize - cad.length();

        for ( ; remain > 0; remain-- ) {
            cad = " " + cad;
        }
        return cad;
    }

    public String toString()
    {
        String msg = "";
        int i, j;

        msg += "= POLYHEDRAL BOUNDED SOLID STRUCTURE ==========================================\n";
        msg += "Solid with " + verticesList.size() + " vertices:\n";
        for ( i = 0; i < verticesList.size(); i++ ) {
            _PolyhedralBoundedSolidVertex v;
            v = verticesList.get(i);
            msg += "  - " + v + "\n";
        }

        msg += "Solid with " + edgesList.size() + " edges:\n";
        for ( i = 0; i < edgesList.size(); i++ ) {
            _PolyhedralBoundedSolidEdge e = edgesList.get(i);
            msg += "  - " + e + "\n";
        }
        msg += "Solid with " + polygonsList.size() + " faces:\n";

        for ( i = 0; i < polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = polygonsList.get(i);
            msg += "  - " + face + "\n";
            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop;
                _PolyhedralBoundedSolidHalfEdge he, heStart;

                msg += "    . Loop " + j + ", with halfedges: \n";
                loop = face.boundariesList.get(j);


                msg += "      | HeID | StartVertex | End Vertex | nccw He | pccwHe | parentEdge\n";
                msg += "      +------+-------------+------------+---------+--------+-----------\n";

                he = loop.boundaryStartHalfEdge;
                if ( he == null ) {
                    msg += "<Loop without starting halfedge!>\n";
                    continue;
                }
                heStart = he;
                do {
                    he = he.next();
                    if ( he == null ) {
                        // Loop is not closed!
                        msg += "      |  - (not closed loop)\n";
                        break;
                    }

                    /*
                    System.out.printf("      %4d | %11d | %10d | %7d | %6d | %10d",
                        he.id, he.startingVertex.id,
                        he.next().startingVertex.id,
                        he.next().id, he.previous().id, he.parentEdge.id);
                    */
                    msg += "      | " +
                        intPreSpaces(he.id, 4) + " | " +
                        intPreSpaces(he.startingVertex.id, 11) + " | " +
                        intPreSpaces(he.next().startingVertex.id, 10) + " | " +
                        intPreSpaces(he.next().id, 7) + " | " +
                        intPreSpaces(he.previous().id, 6) + " | ";
                    msg += (he.parentEdge!=null)?
                        intPreSpaces(he.parentEdge.id, 10):"    <null>";
                    msg += "\n";

                } while( he != heStart );
            }
        }
        msg += "= END OF POLYHEDRAL BOUNDED SOLID STRUCTURE ===================================\n";
        return msg;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
