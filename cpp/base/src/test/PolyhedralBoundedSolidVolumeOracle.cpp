#include <cmath>

#include "PolyhedralBoundedSolidVolumeOracle.h"

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

OracleHalfSpace::OracleHalfSpace()
{
    v[0] = v[1] = v[2] = v[3] = 0.0;
}

OracleHalfSpace::OracleHalfSpace(double nx, double ny, double nz, double d)
{
    v[0] = nx;
    v[1] = ny;
    v[2] = nz;
    v[3] = d;
}

double OracleHalfSpace::operator[](int i) const
{
    return v[i];
}

bool OracleHalfSpace::operator==(const OracleHalfSpace& other) const
{
    return v[0] == other.v[0] && v[1] == other.v[1] &&
        v[2] == other.v[2] && v[3] == other.v[3];
}

typedef PolyhedralBoundedSolidVolumeOracle Oracle;

Oracle::OrientedBox::OrientedBox()
{
    size[0] = size[1] = size[2] = 0.0;
}

Oracle::OrientedBox::OrientedBox(double sx, double sy, double sz,
    const Matrix4x4d& transform) : transform(transform)
{
    size[0] = sx;
    size[1] = sy;
    size[2] = sz;
}

PolyhedralBoundedSolid* Oracle::OrientedBox::toSolid() const
{
    Box box(Vector3Dd(size[0], size[1], size[2]));
    PolyhedralBoundedSolid* solid = box.exportToPolyhedralBoundedSolid();
    PolyhedralBoundedSolidModeler::applyTransformation(solid, transform);
    return solid;
}

java::ArrayList<OracleHalfSpace> Oracle::OrientedBox::halfSpaces() const
{
    java::ArrayList<OracleHalfSpace> planes;
    Vector3Dd origin = transform.multiply(Vector3Dd(0, 0, 0));
    Vector3Dd axes[3] = {
        transform.multiply(Vector3Dd(1, 0, 0)).subtract(origin),
        transform.multiply(Vector3Dd(0, 1, 0)).subtract(origin),
        transform.multiply(Vector3Dd(0, 0, 1)).subtract(origin)
    };
    int i;

    for ( i = 0; i < 3; i++ ) {
        double stretch = axes[i].length();
        Vector3Dd n = axes[i].normalized();
        double center = n.dotProduct(origin);
        double half = stretch * size[i] / 2.0;
        planes.add(OracleHalfSpace(n.x(), n.y(), n.z(), center + half));
        planes.add(OracleHalfSpace(-n.x(), -n.y(), -n.z(), -center + half));
    }
    return planes;
}

double Oracle::OrientedBox::volume() const
{
    return convexVolume(halfSpaces());
}

Matrix4x4d Oracle::translation(double x, double y, double z)
{
    return Matrix4x4d().translation(x, y, z);
}

Matrix4x4d Oracle::rotation(double angle, double x, double y, double z)
{
    return Matrix4x4d().axisRotation(angle, x, y, z);
}

double Oracle::intersectionVolume(const OrientedBox& a, const OrientedBox& b)
{
    java::ArrayList<OracleHalfSpace> all = a.halfSpaces();
    all.addAll(b.halfSpaces());
    return convexVolume(all);
}

double Oracle::expectedVolume(const OrientedBox& a, const OrientedBox& b,
    int op)
{
    double i = intersectionVolume(a, b);
    if ( op == PolyhedralBoundedSolidModeler::UNION ) {
        return a.volume() + b.volume() - i;
    }
    if ( op == PolyhedralBoundedSolidModeler::INTERSECTION ) {
        return i;
    }
    return a.volume() - i;
}

double Oracle::convexVolume(const java::ArrayList<OracleHalfSpace>& input)
{
    java::ArrayList<OracleHalfSpace> planes;
    double volume = 0.0;
    long k;
    long m;

    for ( m = 0; m < input.size(); m++ ) {
        const OracleHalfSpace& p = input[m];
        bool duplicate = false;
        long r;
        for ( r = 0; r < planes.size(); r++ ) {
            const OracleHalfSpace& q = planes[r];
            if ( std::fabs(p[0] - q[0]) < 1e-12 &&
                 std::fabs(p[1] - q[1]) < 1e-12 &&
                 std::fabs(p[2] - q[2]) < 1e-12 &&
                 std::fabs(p[3] - q[3]) < 1e-12 ) {
                duplicate = true;
            }
        }
        if ( !duplicate ) {
            planes.add(p);
        }
    }
    for ( k = 0; k < planes.size(); k++ ) {
        OracleHalfSpace p = planes[k];
        Vector3Dd n(p[0], p[1], p[2]);
        Vector3Dd helper = std::fabs(p[0]) < 0.9
            ? Vector3Dd(1, 0, 0) : Vector3Dd(0, 1, 0);
        Vector3Dd u = n.crossProduct(helper).normalized();
        Vector3Dd v = n.crossProduct(u);
        Vector3Dd c = n.multiply(p[3]);
        double big = 1.0e4;
        java::ArrayList<Vector3Dd> polygon;
        long j;
        polygon.add(c.add(u.multiply(big)).add(v.multiply(big)));
        polygon.add(c.add(u.multiply(-big)).add(v.multiply(big)));
        polygon.add(c.add(u.multiply(-big)).add(v.multiply(-big)));
        polygon.add(c.add(u.multiply(big)).add(v.multiply(-big)));
        for ( j = 0; j < planes.size() && !polygon.isEmpty(); j++ ) {
            if ( j == k ) {
                continue;
            }
            OracleHalfSpace q = planes[j];
            Vector3Dd qn(q[0], q[1], q[2]);
            java::ArrayList<Vector3Dd> clipped;
            long i;
            for ( i = 0; i < polygon.size(); i++ ) {
                Vector3Dd a = polygon[i];
                Vector3Dd b = polygon[(i + 1) % polygon.size()];
                double da = qn.dotProduct(a) - q[3];
                double db = qn.dotProduct(b) - q[3];
                if ( da <= 0 ) {
                    clipped.add(a);
                }
                if ( (da < 0 && db > 0) || (da > 0 && db < 0) ) {
                    clipped.add(a.add(
                        b.subtract(a).multiply(da / (da - db))));
                }
            }
            polygon = clipped;
        }
        if ( polygon.size() < 3 ) {
            continue;
        }
        Vector3Dd areaVector(0, 0, 0);
        long i;
        for ( i = 0; i < polygon.size(); i++ ) {
            areaVector = areaVector.add(
                polygon[i].subtract(c).crossProduct(
                    polygon[(i + 1) % polygon.size()].subtract(c)));
        }
        volume += p[3] * std::fabs(areaVector.dotProduct(n)) / 6.0;
    }
    return volume;
}

double Oracle::solidVolume(PolyhedralBoundedSolid* solid)
{
    double volume = 0.0;
    int i;

    for ( i = 0; i < solid->getPolygonsList().size(); i++ ) {
        _PolyhedralBoundedSolidFace* face = solid->getPolygonsList().get(i);
        Vector3Dd areaVector(0, 0, 0);
        bool hasAnchor = false;
        Vector3Dd anchor;
        int j;
        for ( j = 0; j < face->boundariesList.size(); j++ ) {
            _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(j);
            int n = loop->halfEdgesList.size();
            int k;
            for ( k = 0; k < n; k++ ) {
                Vector3Dd a =
                    loop->halfEdgesList.get(k)->startingVertex->position;
                Vector3Dd b = loop->halfEdgesList.get((k + 1) % n)
                    ->startingVertex->position;
                if ( !hasAnchor ) {
                    anchor = a;
                    hasAnchor = true;
                }
                areaVector = areaVector.add(a.crossProduct(b));
            }
        }
        if ( hasAnchor ) {
            volume += anchor.dotProduct(areaVector) / 6.0;
        }
    }
    return std::fabs(volume);
}
