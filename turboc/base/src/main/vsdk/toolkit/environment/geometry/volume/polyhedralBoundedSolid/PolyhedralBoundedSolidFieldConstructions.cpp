//= References:                                                             =
//= [BLOO1988] Bloomenthal, Jules. "Polygonization of implicit surfaces".   =
//=     Computer Aided Geometric Design 5(4), 1988.                         =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#include <stddef.h>
#include <math.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Double.h"
#include "java/lang/Math.h"
#include "java/util/ArrayList.txx"
#include "java/util/HashMap.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidFieldConstructions.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidMeshConstructions.h"

/// Interpolated vertices are kept this fraction of an edge away from its
/// nodes, so polygons never degenerate to points or lines
static const double MINIMUM_EDGE_FRACTION = 0.001;
/// The grid is shifted this fraction of a cell (an irrational number), so
/// its nodes do not fall exactly on simple surfaces, which would give
/// coincident vertices
static const double GRID_SHIFT = 0.5 * (sqrt(5.0) - 2.0);
/// A quadrilateral whose fourth vertex is farther than this (relative to
/// its size) from the plane of the other three is split in triangles
static const double PLANARITY_TOLERANCE = 1.0e-9;

/// A node whose field is closer to zero than this fraction of the largest
/// difference with its neighbors gets this value instead (keeping its
/// sign): the surface stays that fraction of an edge away from it, so no
/// sliver polygons appear around nodes the surface passes close to
static const double NODE_SEPARATION = 0.05;
/// Offsets to the neighbors of a node along the edges of the tetrahedra
static const int TETRAHEDRA_NEIGHBORS[14][3] = {
    {1, 0, 0}, {0, 1, 0}, {0, 0, 1}, {1, 1, 0}, {1, 0, 1}, {0, 1, 1}, {1, 1, 1},
    {-1, 0, 0}, {0, -1, 0}, {0, 0, -1}, {-1, -1, 0}, {-1, 0, -1}, {0, -1, -1},
    {-1, -1, -1}
};

/// The six tetrahedra of a cell, as corners of the cell (bit 0: +x, bit
/// 1: +y, bit 2: +z): paths from corner 0 to corner 7 along the axes
static const int TETRAHEDRA[6][4] = {
    {0, 1, 3, 7}, {0, 1, 5, 7}, {0, 2, 3, 7},
    {0, 2, 6, 7}, {0, 4, 5, 7}, {0, 4, 6, 7}
};

/**
C++ port note: node indexes are `long`, as `int` is 16 bits on Turbo C++
3.0; the vertex indexes of the polygons are `int`, as the vertex ids of a
`PolyhedralBoundedSolid`.
*/
class PolyhedralBoundedSolidFieldConstructions::_Polygonizer {
private:
    int nodesX;
    int nodesY;
    int nodesZ;
    Vector3Dd origin;
    Vector3Dd spacing;
    ArrayList<double> values;
    ArrayList<Vector3Dd> positions;
    ArrayList<ArrayList<int> > polygons;
    HashMap<long, int> edgeVertices;

    ArrayList<double> separateFromZero(const ArrayList<double>& original) const;
    long nodeIndex(long i, long j, long k) const;
    Vector3Dd nodePosition(long node) const;
    BOOLEAN isBorder(long node) const;
    BOOLEAN isInside(long node) const;
    int edgeVertex(long inside, long outside);
    void polygonizeTetrahedron(long n0, long n1, long n2, long n3);
    Vector3Dd centroid(const long* nodes, int count) const;
    Vector3Dd newellNormal(const ArrayList<int>& polygon) const;
    BOOLEAN isPlanar(const ArrayList<int>& quad) const;
    static void reverse(ArrayList<int>& polygon);

public:
    _Polygonizer(int nodesX, int nodesY, int nodesZ, const Vector3Dd& origin,
        const Vector3Dd& spacing, const ArrayList<double>& values);
    PolyhedralBoundedSolid* build();
};

PolyhedralBoundedSolid* PolyhedralBoundedSolidFieldConstructions::createFromScalarField(
    ToDoubleFunction<Vector3Dd>& field, const Vector3Dd& minimum,
    const Vector3Dd& maximum, int divisions)
{
    Vector3Dd size = maximum.subtract(minimum);
    double largest = Math::max(size.x(), Math::max(size.y(), size.z()));
    if ( !(largest > 0) || divisions < 1 ) {
        return NULL;
    }
    double h = largest / divisions;
    int nodesX = Math::max(2, (int)Math::ceil(size.x() / h - 1.0e-9) + 1);
    int nodesY = Math::max(2, (int)Math::ceil(size.y() / h - 1.0e-9) + 1);
    int nodesZ = Math::max(2, (int)Math::ceil(size.z() / h - 1.0e-9) + 1);
    // Grid centered on the box, slightly shifted
    Vector3Dd origin(
        minimum.x() + 0.5 * (size.x() - (nodesX - 1) * h) + GRID_SHIFT * h,
        minimum.y() + 0.5 * (size.y() - (nodesY - 1) * h) + GRID_SHIFT * h,
        minimum.z() + 0.5 * (size.z() - (nodesZ - 1) * h) + GRID_SHIFT * h);
    ArrayList<double> values;
    long i;
    long j;
    long k;

    for ( k = 0; k < nodesZ; k++ ) {
        for ( j = 0; j < nodesY; j++ ) {
            for ( i = 0; i < nodesX; i++ ) {
                // Index i + nodesX * (j + nodesY * k), in this order
                values.add(field.applyAsDouble(
                    Vector3Dd(origin.x() + i * h, origin.y() + j * h,
                        origin.z() + k * h)));
            }
        }
    }
    return createFromSampledField(nodesX, nodesY, nodesZ, origin,
        Vector3Dd(h, h, h), values);
}

PolyhedralBoundedSolid* PolyhedralBoundedSolidFieldConstructions::createFromBoundedScalarField(
    ToDoubleFunction<Vector3Dd>& field, const Vector3Dd& minimum,
    const Vector3Dd& maximum, int divisions)
{
    Vector3Dd size = maximum.subtract(minimum);
    double largest = Math::max(size.x(), Math::max(size.y(), size.z()));
    if ( !(largest > 0) || divisions < 1 ) {
        return NULL;
    }
    double margin = 1.5 * largest / divisions;
    Vector3Dd m(margin, margin, margin);
    return createFromScalarField(field, minimum.subtract(m), maximum.add(m),
        divisions + 3);
}

PolyhedralBoundedSolid* PolyhedralBoundedSolidFieldConstructions::createFromSampledField(
    int nodesX, int nodesY, int nodesZ, const Vector3Dd& origin,
    const Vector3Dd& spacing, const ArrayList<double>& values)
{
    _Polygonizer polygonizer(nodesX, nodesY, nodesZ, origin, spacing, values);
    return polygonizer.build();
}

//= _Polygonizer ============================================================

PolyhedralBoundedSolidFieldConstructions::_Polygonizer::_Polygonizer(
    int nodesX, int nodesY, int nodesZ, const Vector3Dd& origin,
    const Vector3Dd& spacing, const ArrayList<double>& values)
    : nodesX(nodesX), nodesY(nodesY), nodesZ(nodesZ), origin(origin),
      spacing(spacing), values(), positions(), polygons(), edgeVertices()
{
    this->values = separateFromZero(values);
}

/**
@return the values, with the ones of the nodes too close to zero
moved away from it (see NODE_SEPARATION)
*/
ArrayList<double> PolyhedralBoundedSolidFieldConstructions::_Polygonizer::separateFromZero(
    const ArrayList<double>& original) const
{
    ArrayList<double> separated(original);
    long i;
    long j;
    long k;
    int n;

    for ( k = 1; k < nodesZ - 1; k++ ) {
        for ( j = 1; j < nodesY - 1; j++ ) {
            for ( i = 1; i < nodesX - 1; i++ ) {
                long node = nodeIndex(i, j, k);
                double v = original[node];
                if ( !Double::isFinite(v) ) {
                    continue;
                }
                double largestDifference = 0;
                for ( n = 0; n < 14; n++ ) {
                    const int* offset = TETRAHEDRA_NEIGHBORS[n];
                    double w = original[nodeIndex(i + offset[0],
                        j + offset[1], k + offset[2])];
                    if ( Double::isFinite(w) ) {
                        largestDifference = Math::max(largestDifference,
                            Math::abs(w - v));
                    }
                }
                double minimum = NODE_SEPARATION * largestDifference;
                if ( Math::abs(v) < minimum ) {
                    separated[node] = v < 0 ? -minimum : minimum;
                }
            }
        }
    }
    return separated;
}

long PolyhedralBoundedSolidFieldConstructions::_Polygonizer::nodeIndex(
    long i, long j, long k) const
{
    return i + nodesX * (j + nodesY * k);
}

Vector3Dd PolyhedralBoundedSolidFieldConstructions::_Polygonizer::nodePosition(
    long node) const
{
    long i = node % nodesX;
    long j = (node / nodesX) % nodesY;
    long k = node / (((long)nodesX) * nodesY);
    return Vector3Dd(origin.x() + i * spacing.x(),
        origin.y() + j * spacing.y(), origin.z() + k * spacing.z());
}

BOOLEAN PolyhedralBoundedSolidFieldConstructions::_Polygonizer::isBorder(
    long node) const
{
    long i = node % nodesX;
    long j = (node / nodesX) % nodesY;
    long k = node / (((long)nodesX) * nodesY);
    return i == 0 || j == 0 || k == 0 ||
        i == nodesX - 1 || j == nodesY - 1 || k == nodesZ - 1;
}

BOOLEAN PolyhedralBoundedSolidFieldConstructions::_Polygonizer::isInside(
    long node) const
{
    return !isBorder(node) && values[node] < 0;
}


/**
@return index of the vertex where the zero level crosses the edge
from an inside node to an outside node
*/
int PolyhedralBoundedSolidFieldConstructions::_Polygonizer::edgeVertex(
    long inside, long outside)
{
    long key = Math::min(inside, outside) * values.size() +
        Math::max(inside, outside);
    const int* known = edgeVertices.get(key);
    if ( known != NULL ) {
        return *known;
    }
    double a = values[inside];
    double b = values[outside];
    double t;
    if ( isBorder(outside) ) {
        // Caps: half way to the border, so they are flat
        t = 0.5;
    }
    else if ( Double::isFinite(a) && Double::isFinite(b) && b >= 0 ) {
        t = a / (a - b);
    }
    else {
        // Not a number or infinite: no reliable interpolation
        t = 0.5;
    }
    t = Math::max(MINIMUM_EDGE_FRACTION,
        Math::min(1.0 - MINIMUM_EDGE_FRACTION, t));
    Vector3Dd p = nodePosition(inside);
    Vector3Dd q = nodePosition(outside);
    positions.add(p.add(q.subtract(p).multiply(t)));
    int index = (int)(positions.size() - 1);
    edgeVertices.put(key, index);
    return index;
}

PolyhedralBoundedSolid* PolyhedralBoundedSolidFieldConstructions::_Polygonizer::build()
{
    long corners[8];
    long i;
    long j;
    long k;
    int c;
    int t;

    for ( k = 0; k < nodesZ - 1; k++ ) {
        for ( j = 0; j < nodesY - 1; j++ ) {
            for ( i = 0; i < nodesX - 1; i++ ) {
                for ( c = 0; c < 8; c++ ) {
                    corners[c] = nodeIndex(i + (c & 1),
                        j + ((c >> 1) & 1), k + ((c >> 2) & 1));
                }
                for ( t = 0; t < 6; t++ ) {
                    const int* tetrahedron = TETRAHEDRA[t];
                    polygonizeTetrahedron(corners[tetrahedron[0]],
                        corners[tetrahedron[1]], corners[tetrahedron[2]],
                        corners[tetrahedron[3]]);
                }
            }
        }
    }
    return PolyhedralBoundedSolidMeshConstructions::createFromPolygonMesh(
        positions, polygons);
}

void PolyhedralBoundedSolidFieldConstructions::_Polygonizer::polygonizeTetrahedron(
    long n0, long n1, long n2, long n3)
{
    long nodes[4];
    long in[4];
    long out[4];
    int insideCount = 0;
    int outsideCount = 0;
    int m;

    nodes[0] = n0;
    nodes[1] = n1;
    nodes[2] = n2;
    nodes[3] = n3;
    for ( m = 0; m < 4; m++ ) {
        long node = nodes[m];
        if ( isInside(node) ) {
            in[insideCount++] = node;
        }
        else {
            out[outsideCount++] = node;
        }
    }
    if ( insideCount == 0 || outsideCount == 0 ) {
        return;
    }

    ArrayList<int> polygon;
    if ( insideCount == 1 ) {
        polygon.add(edgeVertex(in[0], out[0]));
        polygon.add(edgeVertex(in[0], out[1]));
        polygon.add(edgeVertex(in[0], out[2]));
    }
    else if ( insideCount == 3 ) {
        polygon.add(edgeVertex(in[0], out[0]));
        polygon.add(edgeVertex(in[1], out[0]));
        polygon.add(edgeVertex(in[2], out[0]));
    }
    else {
        // Consecutive vertices share a node of the tetrahedron
        polygon.add(edgeVertex(in[0], out[0]));
        polygon.add(edgeVertex(in[0], out[1]));
        polygon.add(edgeVertex(in[1], out[1]));
        polygon.add(edgeVertex(in[1], out[0]));
    }

    // Counterclockwise as seen from outside: the normal points from
    // the inside nodes to the outside ones
    Vector3Dd towardsOutside = centroid(out, outsideCount)
        .subtract(centroid(in, insideCount));
    if ( newellNormal(polygon).dotProduct(towardsOutside) < 0 ) {
        reverse(polygon);
    }

    if ( polygon.size() == 4 && !isPlanar(polygon) ) {
        ArrayList<int> first;
        ArrayList<int> second;
        first.add(polygon[0]);
        first.add(polygon[1]);
        first.add(polygon[2]);
        second.add(polygon[0]);
        second.add(polygon[2]);
        second.add(polygon[3]);
        polygons.add(first);
        polygons.add(second);
    }
    else {
        polygons.add(polygon);
    }
}

Vector3Dd PolyhedralBoundedSolidFieldConstructions::_Polygonizer::centroid(
    const long* nodes, int count) const
{
    Vector3Dd sum(0, 0, 0);
    int i;
    for ( i = 0; i < count; i++ ) {
        sum = sum.add(nodePosition(nodes[i]));
    }
    return sum.multiply(1.0 / count);
}

Vector3Dd PolyhedralBoundedSolidFieldConstructions::_Polygonizer::newellNormal(
    const ArrayList<int>& polygon) const
{
    double x = 0;
    double y = 0;
    double z = 0;
    long i;
    for ( i = 0; i < polygon.size(); i++ ) {
        const Vector3Dd& p = positions[polygon[i]];
        const Vector3Dd& q = positions[polygon[(i + 1) % polygon.size()]];
        x += (p.y() - q.y()) * (p.z() + q.z());
        y += (p.z() - q.z()) * (p.x() + q.x());
        z += (p.x() - q.x()) * (p.y() + q.y());
    }
    return Vector3Dd(x, y, z);
}

BOOLEAN PolyhedralBoundedSolidFieldConstructions::_Polygonizer::isPlanar(
    const ArrayList<int>& quad) const
{
    const Vector3Dd& p0 = positions[quad[0]];
    const Vector3Dd& p1 = positions[quad[1]];
    const Vector3Dd& p2 = positions[quad[2]];
    const Vector3Dd& p3 = positions[quad[3]];
    Vector3Dd n = p1.subtract(p0).crossProduct(p2.subtract(p0));
    double size = Math::max(p2.subtract(p0).length(), p3.subtract(p1).length());
    double length = n.length();
    if ( !(length > 0) || !(size > 0) ) {
        return FALSE;
    }
    return Math::abs(p3.subtract(p0).dotProduct(n)) / length <=
        PLANARITY_TOLERANCE * size;
}

void PolyhedralBoundedSolidFieldConstructions::_Polygonizer::reverse(
    ArrayList<int>& polygon)
{
    long i;
    long j;
    for ( i = 0, j = polygon.size() - 1; i < j; i++, j-- ) {
        int tmp = polygon[i];
        polygon[i] = polygon[j];
        polygon[j] = tmp;
    }
}
