#include <math.h>

#include "PolyhedralBoundedSolidVolumeOracle.h"

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"

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

BOOLEAN OracleHalfSpace::operator==(const OracleHalfSpace& other) const
{
    return v[0] == other.v[0] && v[1] == other.v[1] &&
        v[2] == other.v[2] && v[3] == other.v[3];
}

PolyhedralBoundedSolidVolumeOracle::OrientedBox::OrientedBox()
{
    size[0] = size[1] = size[2] = 0.0;
}

PolyhedralBoundedSolidVolumeOracle::OrientedBox::OrientedBox(double sx,
    double sy, double sz, const Matrix4x4d& inTransform) :
    transform(inTransform)
{
    size[0] = sx;
    size[1] = sy;
    size[2] = sz;
}

PolyhedralBoundedSolid*
PolyhedralBoundedSolidVolumeOracle::OrientedBox::toSolid() const
{
    Box box(Vector3Dd(size[0], size[1], size[2]));
    PolyhedralBoundedSolid* solid = box.exportToPolyhedralBoundedSolid();

    PolyhedralBoundedSolidModeler::applyTransformation(solid, transform);
    return solid;
}

ArrayList<OracleHalfSpace>
PolyhedralBoundedSolidVolumeOracle::OrientedBox::halfSpaces() const
{
    ArrayList<OracleHalfSpace> planes;
    Vector3Dd origin = transform.multiply(Vector3Dd(0, 0, 0));
    Vector3Dd axes[3];
    int i;

    axes[0] = transform.multiply(Vector3Dd(1, 0, 0)).subtract(origin);
    axes[1] = transform.multiply(Vector3Dd(0, 1, 0)).subtract(origin);
    axes[2] = transform.multiply(Vector3Dd(0, 0, 1)).subtract(origin);
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

double PolyhedralBoundedSolidVolumeOracle::OrientedBox::volume() const
{
    return convexVolume(halfSpaces());
}

Matrix4x4d PolyhedralBoundedSolidVolumeOracle::translation(double x,
    double y, double z)
{
    return Matrix4x4d().translation(x, y, z);
}

Matrix4x4d PolyhedralBoundedSolidVolumeOracle::rotation(double angle,
    double x, double y, double z)
{
    return Matrix4x4d().axisRotation(angle, x, y, z);
}

double PolyhedralBoundedSolidVolumeOracle::intersectionVolume(
    const OrientedBox& a, const OrientedBox& b)
{
    ArrayList<OracleHalfSpace> all = a.halfSpaces();

    all.addAll(b.halfSpaces());
    return convexVolume(all);
}

double PolyhedralBoundedSolidVolumeOracle::expectedVolume(
    const OrientedBox& a, const OrientedBox& b, int op)
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

double PolyhedralBoundedSolidVolumeOracle::convexVolume(
    const ArrayList<OracleHalfSpace>& input)
{
    ArrayList<OracleHalfSpace> planes;
    double volume = 0.0;
    long k;
    long m;

    for ( m = 0; m < input.size(); m++ ) {
        const OracleHalfSpace& p = input[m];
        BOOLEAN duplicate = FALSE;
        long r;
        for ( r = 0; r < planes.size(); r++ ) {
            const OracleHalfSpace& q = planes[r];
            if ( fabs(p.v[0] - q.v[0]) < 1e-12 &&
                 fabs(p.v[1] - q.v[1]) < 1e-12 &&
                 fabs(p.v[2] - q.v[2]) < 1e-12 &&
                 fabs(p.v[3] - q.v[3]) < 1e-12 ) {
                duplicate = TRUE;
            }
        }
        if ( !duplicate ) {
            planes.add(p);
        }
    }

    for ( k = 0; k < planes.size(); k++ ) {
        OracleHalfSpace p = planes[k];
        Vector3Dd n(p.v[0], p.v[1], p.v[2]);
        Vector3Dd helper;
        if ( fabs(p.v[0]) < 0.9 ) {
            helper = Vector3Dd(1, 0, 0);
        }
        else {
            helper = Vector3Dd(0, 1, 0);
        }
        Vector3Dd u = n.crossProduct(helper).normalized();
        Vector3Dd v = n.crossProduct(u);
        Vector3Dd c = n.multiply(p.v[3]);
        double big = 1.0e4;
        ArrayList<Vector3Dd> polygon;
        long j;
        long i;

        polygon.add(c.add(u.multiply(big)).add(v.multiply(big)));
        polygon.add(c.add(u.multiply(-big)).add(v.multiply(big)));
        polygon.add(c.add(u.multiply(-big)).add(v.multiply(-big)));
        polygon.add(c.add(u.multiply(big)).add(v.multiply(-big)));

        for ( j = 0; j < planes.size() && !polygon.isEmpty(); j++ ) {
            if ( j == k ) {
                continue;
            }
            OracleHalfSpace q = planes[j];
            Vector3Dd qn(q.v[0], q.v[1], q.v[2]);
            ArrayList<Vector3Dd> clipped;

            for ( i = 0; i < polygon.size(); i++ ) {
                Vector3Dd a = polygon[i];
                Vector3Dd b = polygon[(i + 1) % polygon.size()];
                double da = qn.dotProduct(a) - q.v[3];
                double db = qn.dotProduct(b) - q.v[3];

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
        for ( i = 0; i < polygon.size(); i++ ) {
            areaVector = areaVector.add(
                polygon[i].subtract(c).crossProduct(
                    polygon[(i + 1) % polygon.size()].subtract(c)));
        }
        volume += p.v[3] * fabs(areaVector.dotProduct(n)) / 6.0;
    }
    return volume;
}

double PolyhedralBoundedSolidVolumeOracle::solidVolume(
    PolyhedralBoundedSolid* solid)
{
    double volume = 0.0;
    long i;

    for ( i = 0; i < solid->getPolygonsList().size(); i++ ) {
        _PolyhedralBoundedSolidFace* face = solid->getPolygonsList().get(i);
        Vector3Dd areaVector(0, 0, 0);
        Vector3Dd anchor;
        BOOLEAN hasAnchor = FALSE;
        long j;

        for ( j = 0; j < face->boundariesList.size(); j++ ) {
            _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(j);
            long n = loop->halfEdgesList.size();
            long k;

            for ( k = 0; k < n; k++ ) {
                Vector3Dd a =
                    loop->halfEdgesList.get(k)->startingVertex->position;
                Vector3Dd b = loop->halfEdgesList.get((k + 1) % n)
                    ->startingVertex->position;
                if ( !hasAnchor ) {
                    anchor = a;
                    hasAnchor = TRUE;
                }
                areaVector = areaVector.add(a.crossProduct(b));
            }
        }
        if ( hasAnchor ) {
            volume += anchor.dotProduct(areaVector) / 6.0;
        }
    }
    return fabs(volume);
}
