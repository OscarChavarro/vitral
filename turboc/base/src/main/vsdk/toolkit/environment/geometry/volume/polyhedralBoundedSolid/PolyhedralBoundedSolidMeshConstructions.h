//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#ifndef __PBS_MESH_CNSTRCTNS__
#define __PBS_MESH_CNSTRCTNS__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class PolyhedralBoundedSolid;

/**
Builds the boundary representation of a closed polygon mesh with Euler
operators only, so it can be used for any solid whose boundary is known as a
list of polygons (i.e. the polygonization of an implicit surface), of any
genus, and with any number of connected components (one shell each).

The mesh must be a closed, oriented 2-manifold: every directed edge of a face
appears once, and its reverse in exactly one other face; faces are listed
counterclockwise as seen from outside the solid, and are planar.

The faces are added one at a time in breadth first order over the face
adjacency, so each new face shares at least one edge with the ones already
built. The faces of the solid that are not mesh faces yet ("open" faces)
always cover the part of the surface still to be built; a new face is carved
out of them:
  - The first face is a lamina (`mvfs`, `smev`, `smef`, as the circular
    lamina of [MANT1988].12.2), whose other side is the first open face.
  - The polygon of a new face alternates runs of edges that already exist
    (whose open side is on the face) with runs of new edges. A run of new
    edges goes from the end of an existing run to the start of the next one,
    through vertices that do not exist yet: they are created as struts
    (`lmev`, [MANT1988].11.3.2), and the last edge closes the run:
      - with `lmef` ([MANT1988].11.3.3) when both ends are on the same loop,
        splitting the face so the new face is on the side of the polygon,
      - with `lmekr` ([MANT1988].11.5) when they are on two loops of the same
        face,
      - or with `lkfmrh` and then `lmekr` when they are on two different
        faces, as when closing a handle: the same strategy used to glue the
        ends of a closed rotational sweep in [MANT1988].12.4.2.
  - When all the edges of a face already exist, it is the open face they
    bound.
  - A finished face that shares its face record with other loops gets a face
    of its own (`lmfkrh`).
*/
class _PBSMeshBuilder;

class PolyhedralBoundedSolidMeshConstructions {
private:
    typedef _PBSMeshBuilder _MeshBuilder;

public:
    /**
    @param positions positions of the vertices of the mesh
    @param faces vertex indexes of each face, counterclockwise as seen from
    outside
    @return a new solid with the mesh as its boundary, owned by the caller,
    or null if the mesh has no faces
    @throws IllegalArgumentException if the mesh is not a closed oriented
    2-manifold
    */
    static PolyhedralBoundedSolid* createFromPolygonMesh(
        const ArrayList<Vector3Dd>& positions,
        const ArrayList<ArrayList<int> >& faces);
};

#endif
