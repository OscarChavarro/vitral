#include <math.h>

#include "PrismFixtures.h"

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerConstructions.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.h"

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

PolyhedralBoundedSolid* PrismFixtures::prism(const PrismPolygon& xy,
    double height)
{
    PolyhedralBoundedSolid* solid = new PolyhedralBoundedSolid();
    int n = (int)xy.size();
    int i;

    PolyhedralBoundedSolidEulerOperators::mvfs(solid,
        Vector3Dd(xy[0].x, xy[0].y, 0.0), 1, 1);
    for ( i = 1; i < n; i++ ) {
        PolyhedralBoundedSolidEulerOperators::smev(solid, 1, i, i + 1,
            Vector3Dd(xy[i].x, xy[i].y, 0.0));
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
        xy.add(Vector2Dd(radius * cos(a), radius * sin(a)));
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
        double r;
        if ( i % 2 == 0 ) {
            r = outerRadius;
        }
        else {
            r = innerRadius;
        }
        xy.add(Vector2Dd(r * cos(a), r * sin(a)));
    }
    return xy;
}

double PrismFixtures::polygonArea(const PrismPolygon& xy)
{
    double area = 0.0;
    long i;

    for ( i = 0; i < xy.size(); i++ ) {
        const Vector2Dd& p = xy[i];
        const Vector2Dd& q = xy[(i + 1) % xy.size()];
        area += p.x * q.y - q.x * p.y;
    }
    return area / 2.0;
}
