#include <cmath>

#include "PrismFixtures.h"

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerConstructions.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.h"

PrismPoint::PrismPoint()
{
    v[0] = v[1] = 0.0;
}

PrismPoint::PrismPoint(double x, double y)
{
    v[0] = x;
    v[1] = y;
}

double& PrismPoint::operator[](int i)
{
    return v[i];
}

double PrismPoint::operator[](int i) const
{
    return v[i];
}

bool PrismPoint::operator==(const PrismPoint& other) const
{
    return v[0] == other.v[0] && v[1] == other.v[1];
}

PolyhedralBoundedSolid* PrismFixtures::prism(const PrismPolygon& xy,
    double height)
{
    PolyhedralBoundedSolid* solid = new PolyhedralBoundedSolid();
    int i;
    int n = (int)xy.size();

    PolyhedralBoundedSolidEulerOperators::mvfs(solid,
        Vector3Dd(xy[0][0], xy[0][1], 0.0), 1, 1);
    for ( i = 1; i < n; i++ ) {
        PolyhedralBoundedSolidEulerOperators::smev(solid, 1, i, i + 1,
            Vector3Dd(xy[i][0], xy[i][1], 0.0));
    }
    PolyhedralBoundedSolidEulerOperators::smef(solid, 1, n, 1, 2);
    Matrix4x4d t;
    t = t.translation(0.0, 0.0, height);
    PolyhedralBoundedSolidEulerConstructions
        ::translationalSweepExtrudeFacePlanar(solid, solid->findFace(1), t);
    return solid;
}

PrismPolygon PrismFixtures::regularPolygon(int sides, double radius)
{
    PrismPolygon xy;
    int i;

    for ( i = 0; i < sides; i++ ) {
        double a = 2.0 * M_PI * i / sides;
        xy.add(PrismPoint(radius * std::cos(a), radius * std::sin(a)));
    }
    return xy;
}

PrismPolygon PrismFixtures::starPolygon(int points, double outerRadius,
    double innerRadius)
{
    PrismPolygon xy;
    int i;

    for ( i = 0; i < 2 * points; i++ ) {
        double a = M_PI * i / points;
        double r = i % 2 == 0 ? outerRadius : innerRadius;
        xy.add(PrismPoint(r * std::cos(a), r * std::sin(a)));
    }
    return xy;
}

double PrismFixtures::polygonArea(const PrismPolygon& xy)
{
    double area = 0.0;
    long i;

    for ( i = 0; i < xy.size(); i++ ) {
        const PrismPoint& p = xy[i];
        const PrismPoint& q = xy[(i + 1) % xy.size()];
        area += p[0] * q[1] - q[0] * p[1];
    }
    return area / 2.0;
}
