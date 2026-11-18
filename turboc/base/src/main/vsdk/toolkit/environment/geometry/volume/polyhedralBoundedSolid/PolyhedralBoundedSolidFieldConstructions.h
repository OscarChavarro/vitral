//= References:                                                             =
//= [BLOO1988] Bloomenthal, Jules. "Polygonization of implicit surfaces".   =
//=     Computer Aided Geometric Design 5(4), 1988.                         =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#ifndef __PBS_FIELD_CNSTRCTNS__
#define __PBS_FIELD_CNSTRCTNS__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "java/util/function/ToDoubleFunction.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class PolyhedralBoundedSolid;

/**
Polyhedral approximations of solids given by a scalar field: the solid is the
region where the field is negative. The field is sampled on a regular grid,
and its zero level is polygonized by marching tetrahedra [BLOO1988]: each
grid cell is split into six tetrahedra sharing its main diagonal (the same
split in all the cells, so neighboring cells agree on their common faces),
and the field is taken as linear inside each tetrahedron. That gives one
triangle or one planar quadrilateral per tetrahedron crossed by the surface,
and a closed 2-manifold surface: the nodes on the border of the grid are taken
as outside, so a solid that does not fit in the grid is cut by it (with flat
caps half a cell inside the border).

The polygons are given to `PolyhedralBoundedSolidMeshConstructions`, so the
boundary representation is built only with Euler operators [MANT1988], for
any genus of the surface.
*/
class _PBSPolygonizer;

class PolyhedralBoundedSolidFieldConstructions {
private:
    typedef _PBSPolygonizer _Polygonizer;

public:
    /**
    Samples a field over a box and builds the solid where it is negative.
    @param field scalar field
    @param minimum minimum corner of the box
    @param maximum maximum corner of the box
    @param divisions number of grid cells along the largest side of the box
    (cells are cubes)
    @return the polyhedral approximation of the solid inside the box, owned
    by the caller, or null if it is empty
    */
    static PolyhedralBoundedSolid* createFromScalarField(
        ToDoubleFunction<Vector3Dd>& field, const Vector3Dd& minimum,
        const Vector3Dd& maximum, int divisions);

    /**
    Samples a field over a box enlarged by a margin of a cell and a half on
    each side, so a solid that fits in the box is not cut by the grid.
    @param field scalar field
    @param minimum minimum corner of a box containing the solid
    @param maximum maximum corner of a box containing the solid
    @param divisions number of grid cells along the largest side of the box
    @return the polyhedral approximation of the solid, owned by the caller,
    or null if it is empty
    */
    static PolyhedralBoundedSolid* createFromBoundedScalarField(
        ToDoubleFunction<Vector3Dd>& field, const Vector3Dd& minimum,
        const Vector3Dd& maximum, int divisions);

    /**
    Builds the solid where a sampled field is negative.
    @param nodesX number of grid nodes along X (at least 2)
    @param nodesY number of grid nodes along Y (at least 2)
    @param nodesZ number of grid nodes along Z (at least 2)
    @param origin position of node (0, 0, 0)
    @param spacing distance between nodes along each axis
    @param values field at each node (i, j, k), at index i + nodesX*(j + nodesY*k);
    nodes on the border of the grid are taken as outside
    @return the polyhedral approximation of the solid, owned by the caller,
    or null if it is empty
    */
    static PolyhedralBoundedSolid* createFromSampledField(
        int nodesX, int nodesY, int nodesZ, const Vector3Dd& origin,
        const Vector3Dd& spacing, const ArrayList<double>& values);
};

#endif
