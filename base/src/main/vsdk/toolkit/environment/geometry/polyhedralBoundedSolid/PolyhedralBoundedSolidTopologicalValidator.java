//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

package vsdk.toolkit.environment.geometry.polyhedralBoundedSolid;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;

/**
Topological validation helpers for the half-edge representation described in
[MANT1988].10.2.1 and [MANT1988].10.2.2.
*/
public class PolyhedralBoundedSolidTopologicalValidator
{
    private PolyhedralBoundedSolidTopologicalValidator()
    {
    }

    /**
    Checks the fundamental half-edge consistency expected from the graph and
    identification structure described in [MANT1988].10.2.1 and
    [MANT1988].10.2.2.
    */
    public static boolean validateTopologicalIntegrity(PolyhedralBoundedSolid solid)
    {
        int i, j, k;
        _PolyhedralBoundedSolidEdge e;
        _PolyhedralBoundedSolidHalfEdge h1, h2;
        _PolyhedralBoundedSolidFace f;
        _PolyhedralBoundedSolidLoop l;

        for ( i = 0; i < solid.edgesList.size(); i++ ) {
            e = solid.edgesList.get(i);
            h1 = e.rightHalf;
            h2 = e.leftHalf;
            if ( h1 == null || h2 == null ) {
                VSDK.reportMessage(solid, VSDK.WARNING, "validateTopologicalIntegrity",
                "Edge with null halfedge!");
                return false;
            }
            if ( h1.parentLoop.parentFace.parentSolid !=
                 h2.parentLoop.parentFace.parentSolid ) {
                VSDK.reportMessage(solid, VSDK.WARNING, "validateTopologicalIntegrity",
                "Edge belonging to two different solids!");
                return false;
            }
        }

        int edgeCount[];
        edgeCount = new int[solid.edgesList.size()];
        for ( i = 0; i < edgeCount.length; i++ ) {
            edgeCount[i] = 0;
        }

        for ( i = 0; i < solid.polygonsList.size(); i++ ) {
            f = solid.polygonsList.get(i);
            for ( j = 0; j < f.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidHalfEdge he, heStart;
                l = f.boundariesList.get(j);

                he = l.boundaryStartHalfEdge;
                if ( he == null ) {
                    VSDK.reportMessage(solid, VSDK.WARNING,
                    "validateTopologicalIntegrity",
                    "Loop without starting halfedge\n" +
                    "Offending solid:\n" +
                    solid.toString());
                    return false;
                }
                heStart = he;
                do {
                    he = he.next();
                    if ( he == null ) {
                        VSDK.reportMessage(solid, VSDK.WARNING, "validateTopologicalIntegrity",
                        "Not closed loop!");
                        return false;
                    }

                    for ( k = 0; k < edgeCount.length; k++ ) {
                        if ( he.parentEdge == solid.edgesList.get(k) ) {
                            edgeCount[k]++;
                            break;
                        }
                    }
                } while( he != heStart );
            }
        }

        for ( i = 0; i < edgeCount.length; i++ ) {
            if ( edgeCount[i] != 2 ) {
                VSDK.reportMessage(solid, VSDK.WARNING, "validateTopologicalIntegrity",
                    "Edges with different halfedges than 2!");
                return false;
            }
        }

        return true;
    }

    /**
    Rebuilds the vertex-to-emanating-halfedge links required by the vertex node
    definition of [MANT1988].10.2.1 and used throughout the Euler-operator
    programs of chapter [MANT1988].11.
    */
    public static void remakeEmanatingHalfedgesReferences(PolyhedralBoundedSolid solid)
    {
        int i, j;

        for ( i = 0; i < solid.verticesList.size(); i++ ) {
            solid.verticesList.get(i).emanatingHalfEdge = null;
        }

        for ( i = 0; i < solid.polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.polygonsList.get(i);
            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop loop;
                _PolyhedralBoundedSolidHalfEdge he, heStart;

                loop = face.boundariesList.get(j);
                he = loop.boundaryStartHalfEdge;
                if ( he == null ) {
                    continue;
                }
                heStart = he;
                do {
                    he.startingVertex.emanatingHalfEdge = he;
                    he = he.next();
                    if ( he == null ) {
                        break;
                    }
                } while( he != heStart );
            }
        }

        for ( i = 0; i < solid.verticesList.size(); i++ ) {
            if ( solid.verticesList.get(i).emanatingHalfEdge == null ) {
                solid.verticesList.locateWindowAtElem(solid.verticesList.get(i));
                solid.verticesList.removeElemAtWindow();
                i--;
            }
        }
    }
}
