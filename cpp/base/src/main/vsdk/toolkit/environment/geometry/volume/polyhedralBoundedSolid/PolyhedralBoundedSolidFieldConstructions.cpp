//= References:                                                             =
//= [BLOO1988] Bloomenthal, Jules. "Polygonization of implicit surfaces".   =
//=     Computer Aided Geometric Design 5(4), 1988.                         =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#include "java/lang/Double.h"
#include "java/lang/Math.h"
#include "java/util/ArrayList.txx"
#include "java/util/HashMap.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidFieldConstructions.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidMeshConstructions.h"

namespace {

/// Interpolated vertices are kept this fraction of an edge away from its
/// nodes, so polygons never degenerate to points or lines
const double MINIMUM_EDGE_FRACTION = 0.001;
/// The grid is shifted this fraction of a cell (an irrational number), so
/// its nodes do not fall exactly on simple surfaces, which would give
/// coincident vertices
const double GRID_SHIFT = 0.5 * (java::Math::sqrt(5.0) - 2.0);
/// A quadrilateral whose fourth vertex is farther than this (relative to
/// its size) from the plane of the other three is split in triangles
const double PLANARITY_TOLERANCE = 1.0e-9;

/// A node whose field is closer to zero than this fraction of the largest
/// difference with its neighbors gets this value instead (keeping its
/// sign): the surface stays that fraction of an edge away from it, so no
/// sliver polygons appear around nodes the surface passes close to
const double NODE_SEPARATION = 0.05;
/// Offsets to the neighbors of a node along the edges of the tetrahedra
const int TETRAHEDRA_NEIGHBORS[14][3] = {
    {1, 0, 0}, {0, 1, 0}, {0, 0, 1}, {1, 1, 0}, {1, 0, 1}, {0, 1, 1}, {1, 1, 1},
    {-1, 0, 0}, {0, -1, 0}, {0, 0, -1}, {-1, -1, 0}, {-1, 0, -1}, {0, -1, -1},
    {-1, -1, -1}
};

/// The six tetrahedra of a cell, as corners of the cell (bit 0: +x, bit
/// 1: +y, bit 2: +z): paths from corner 0 to corner 7 along the axes
const int TETRAHEDRA[6][4] = {
    {0, 1, 3, 7}, {0, 1, 5, 7}, {0, 2, 3, 7},
    {0, 2, 6, 7}, {0, 4, 5, 7}, {0, 4, 6, 7}
};

class _Polygonizer
{
private:
    int nodesX;
    int nodesY;
    int nodesZ;
    Vector3Dd origin;
    Vector3Dd spacing;
    java::ArrayList<double> values;
    java::ArrayList<Vector3Dd> positions;
    java::ArrayList<java::ArrayList<int> > polygons;
    java::HashMap<long, int> edgeVertices;

    /**
    @return the values, with the ones of the nodes too close to zero
    moved away from it (see NODE_SEPARATION)
    */
    java::ArrayList<double> separateFromZero(
        const java::ArrayList<double>& original) const
    {
        java::ArrayList<double> separated(original);
        for ( int k = 1; k < nodesZ - 1; k++ ) {
            for ( int j = 1; j < nodesY - 1; j++ ) {
                for ( int i = 1; i < nodesX - 1; i++ ) {
                    int node = nodeIndex(i, j, k);
                    double v = original[node];
                    if ( !java::Double::isFinite(v) ) {
                        continue;
                    }
                    double largestDifference = 0;
                    for ( int n = 0; n < 14; n++ ) {
                        const int* offset = TETRAHEDRA_NEIGHBORS[n];
                        double w = original[nodeIndex(i + offset[0],
                            j + offset[1], k + offset[2])];
                        if ( java::Double::isFinite(w) ) {
                            largestDifference = java::Math::max(largestDifference,
                                java::Math::abs(w - v));
                        }
                    }
                    double minimum = NODE_SEPARATION * largestDifference;
                    if ( java::Math::abs(v) < minimum ) {
                        separated[node] = v < 0 ? -minimum : minimum;
                    }
                }
            }
        }
        return separated;
    }

    int nodeIndex(int i, int j, int k) const
    {
        return i + nodesX * (j + nodesY * k);
    }

    Vector3Dd nodePosition(int node) const
    {
        int i = node % nodesX;
        int j = (node / nodesX) % nodesY;
        int k = node / (nodesX * nodesY);
        return Vector3Dd(origin.x() + i * spacing.x(),
            origin.y() + j * spacing.y(), origin.z() + k * spacing.z());
    }

    bool isBorder(int node) const
    {
        int i = node % nodesX;
        int j = (node / nodesX) % nodesY;
        int k = node / (nodesX * nodesY);
        return i == 0 || j == 0 || k == 0 ||
            i == nodesX - 1 || j == nodesY - 1 || k == nodesZ - 1;
    }

    bool isInside(int node) const
    {
        return !isBorder(node) && values[node] < 0;
    }

    /**
    @return index of the vertex where the zero level crosses the edge
    from an inside node to an outside node
    */
    int edgeVertex(int inside, int outside)
    {
        long key = ((long)java::Math::min(inside, outside)) * values.size() +
            java::Math::max(inside, outside);
        int* index = edgeVertices.get(key);
        if ( index != nullptr ) {
            return *index;
        }
        double a = values[inside];
        double b = values[outside];
        double t;
        if ( isBorder(outside) ) {
            // Caps: half way to the border, so they are flat
            t = 0.5;
        }
        else if ( java::Double::isFinite(a) && java::Double::isFinite(b) &&
                  b >= 0 ) {
            t = a / (a - b);
        }
        else {
            // Not a number or infinite: no reliable interpolation
            t = 0.5;
        }
        t = java::Math::max(MINIMUM_EDGE_FRACTION,
            java::Math::min(1.0 - MINIMUM_EDGE_FRACTION, t));
        Vector3Dd p = nodePosition(inside);
        Vector3Dd q = nodePosition(outside);
        positions.add(p.add(q.subtract(p).multiply(t)));
        int newIndex = (int)positions.size() - 1;
        edgeVertices.put(key, newIndex);
        return newIndex;
    }

    void polygonizeTetrahedron(int n0, int n1, int n2, int n3)
    {
        int nodes[4] = {n0, n1, n2, n3};
        int in[4];
        int out[4];
        int insideCount = 0;
        int outsideCount = 0;

        for ( int n = 0; n < 4; n++ ) {
            int node = nodes[n];
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

        java::ArrayList<int> polygon;
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
            java::ArrayList<int> first;
            first.add(polygon[0]);
            first.add(polygon[1]);
            first.add(polygon[2]);
            polygons.add(first);
            java::ArrayList<int> second;
            second.add(polygon[0]);
            second.add(polygon[2]);
            second.add(polygon[3]);
            polygons.add(second);
        }
        else {
            polygons.add(polygon);
        }
    }

    Vector3Dd centroid(const int* nodes, int count) const
    {
        Vector3Dd sum(0, 0, 0);
        for ( int i = 0; i < count; i++ ) {
            sum = sum.add(nodePosition(nodes[i]));
        }
        return sum.multiply(1.0 / count);
    }

    Vector3Dd newellNormal(const java::ArrayList<int>& polygon) const
    {
        double x = 0;
        double y = 0;
        double z = 0;
        for ( int i = 0; i < polygon.size(); i++ ) {
            const Vector3Dd& p = positions[polygon[i]];
            const Vector3Dd& q = positions[polygon[(i + 1) % polygon.size()]];
            x += (p.y() - q.y()) * (p.z() + q.z());
            y += (p.z() - q.z()) * (p.x() + q.x());
            z += (p.x() - q.x()) * (p.y() + q.y());
        }
        return Vector3Dd(x, y, z);
    }

    bool isPlanar(const java::ArrayList<int>& quad) const
    {
        const Vector3Dd& p0 = positions[quad[0]];
        const Vector3Dd& p1 = positions[quad[1]];
        const Vector3Dd& p2 = positions[quad[2]];
        const Vector3Dd& p3 = positions[quad[3]];
        Vector3Dd n = p1.subtract(p0).crossProduct(p2.subtract(p0));
        double size = java::Math::max(p2.subtract(p0).length(),
            p3.subtract(p1).length());
        double length = n.length();
        if ( !(length > 0) || !(size > 0) ) {
            return false;
        }
        return java::Math::abs(p3.subtract(p0).dotProduct(n)) / length <=
            PLANARITY_TOLERANCE * size;
    }

    static void reverse(java::ArrayList<int>& polygon)
    {
        for ( long i = 0, j = polygon.size() - 1; i < j; i++, j-- ) {
            int tmp = polygon[i];
            polygon[i] = polygon[j];
            polygon[j] = tmp;
        }
    }

public:
    _Polygonizer(int inNodesX, int inNodesY, int inNodesZ,
        const Vector3Dd& inOrigin, const Vector3Dd& inSpacing,
        const java::ArrayList<double>& inValues) :
        nodesX(inNodesX), nodesY(inNodesY), nodesZ(inNodesZ),
        origin(inOrigin), spacing(inSpacing)
    {
        values = separateFromZero(inValues);
    }

    PolyhedralBoundedSolid* build()
    {
        int corners[8];

        for ( int k = 0; k < nodesZ - 1; k++ ) {
            for ( int j = 0; j < nodesY - 1; j++ ) {
                for ( int i = 0; i < nodesX - 1; i++ ) {
                    for ( int c = 0; c < 8; c++ ) {
                        corners[c] = nodeIndex(i + (c & 1),
                            j + ((c >> 1) & 1), k + ((c >> 2) & 1));
                    }
                    for ( int t = 0; t < 6; t++ ) {
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
};

}

PolyhedralBoundedSolid*
PolyhedralBoundedSolidFieldConstructions::createFromScalarField(
    const ScalarField& field, const Vector3Dd& minimum,
    const Vector3Dd& maximum, int divisions)
{
    Vector3Dd size = maximum.subtract(minimum);
    double largest = java::Math::max(size.x(),
        java::Math::max(size.y(), size.z()));
    if ( !(largest > 0) || divisions < 1 ) {
        return nullptr;
    }
    double h = largest / divisions;
    int nodesX = java::Math::max(2,
        (int)java::Math::ceil(size.x() / h - 1.0e-9) + 1);
    int nodesY = java::Math::max(2,
        (int)java::Math::ceil(size.y() / h - 1.0e-9) + 1);
    int nodesZ = java::Math::max(2,
        (int)java::Math::ceil(size.z() / h - 1.0e-9) + 1);
    // Grid centered on the box, slightly shifted
    Vector3Dd origin(
        minimum.x() + 0.5 * (size.x() - (nodesX - 1) * h) + GRID_SHIFT * h,
        minimum.y() + 0.5 * (size.y() - (nodesY - 1) * h) + GRID_SHIFT * h,
        minimum.z() + 0.5 * (size.z() - (nodesZ - 1) * h) + GRID_SHIFT * h);
    java::ArrayList<double> values;
    values.reserve((long)nodesX * nodesY * nodesZ);
    for ( long n = 0; n < (long)nodesX * nodesY * nodesZ; n++ ) {
        values.add(0.0);
    }

    for ( int k = 0; k < nodesZ; k++ ) {
        for ( int j = 0; j < nodesY; j++ ) {
            for ( int i = 0; i < nodesX; i++ ) {
                values[i + nodesX * (j + nodesY * k)] = field.apply(
                    Vector3Dd(origin.x() + i * h, origin.y() + j * h,
                        origin.z() + k * h));
            }
        }
    }
    return createFromSampledField(nodesX, nodesY, nodesZ, origin,
        Vector3Dd(h, h, h), values);
}

PolyhedralBoundedSolid*
PolyhedralBoundedSolidFieldConstructions::createFromBoundedScalarField(
    const ScalarField& field, const Vector3Dd& minimum,
    const Vector3Dd& maximum, int divisions)
{
    Vector3Dd size = maximum.subtract(minimum);
    double largest = java::Math::max(size.x(),
        java::Math::max(size.y(), size.z()));
    if ( !(largest > 0) || divisions < 1 ) {
        return nullptr;
    }
    double margin = 1.5 * largest / divisions;
    Vector3Dd m(margin, margin, margin);
    return createFromScalarField(field, minimum.subtract(m), maximum.add(m),
        divisions + 3);
}

PolyhedralBoundedSolid*
PolyhedralBoundedSolidFieldConstructions::createFromSampledField(
    int nodesX, int nodesY, int nodesZ, const Vector3Dd& origin,
    const Vector3Dd& spacing, const java::ArrayList<double>& values)
{
    return _Polygonizer(nodesX, nodesY, nodesZ, origin, spacing, values).build();
}
