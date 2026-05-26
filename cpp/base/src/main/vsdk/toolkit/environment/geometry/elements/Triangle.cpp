#include "vsdk/toolkit/environment/geometry/elements/Triangle.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/geometry/elements/Ray.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/processing/Containment.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include <java/lang/Math.h>
#include "java/lang/String.h"
#include <cmath>
#include <cstdio>
#include "java/lang/String.h"

Triangle::Triangle() : p0(0), p1(0), p2(0), normal(0, 0, 0) {}
Triangle::Triangle(int inP0, int inP1, int inP2)
    : p0(inP0), p1(inP1), p2(inP2), normal(0, 0, 0) {}

int Triangle::getPoint0() const { return p0; }
int Triangle::getPoint1() const { return p1; }
int Triangle::getPoint2() const { return p2; }

void Triangle::setPoint0(int inP0) { p0 = inP0; }
void Triangle::setPoint1(int inP1) { p1 = inP1; }
void Triangle::setPoint2(int inP2) { p2 = inP2; }

static Vector3Dd closestPointOnTriangle(const Vector3Dd& p, const Vector3Dd& a, const Vector3Dd& b, const Vector3Dd& c)
{
    Vector3Dd ab = b.subtract(a), ac = c.subtract(a), ap = p.subtract(a);
    double d1 = ab.dotProduct(ap), d2 = ac.dotProduct(ap);
    if (d1 <= 0 && d2 <= 0) return a;
    Vector3Dd bp = p.subtract(b);
    double d3 = ab.dotProduct(bp), d4 = ac.dotProduct(bp);
    if (d3 >= 0 && d4 <= d3) return b;
    double vc = d1*d4 - d3*d2;
    if (vc <= 0 && d1 >= 0 && d3 <= 0) return a.add(ab.multiply(d1/(d1-d3)));
    Vector3Dd cp = p.subtract(c);
    double d5 = ab.dotProduct(cp), d6 = ac.dotProduct(cp);
    if (d6 >= 0 && d5 <= d6) return c;
    double vb = d5*d2 - d1*d6;
    if (vb <= 0 && d2 >= 0 && d6 <= 0) return a.add(ac.multiply(d2/(d2-d6)));
    double va = d3*d6 - d5*d4;
    if (va <= 0 && (d4-d3) >= 0 && (d5-d6) >= 0) return b.add(c.subtract(b).multiply((d4-d3)/((d4-d3)+(d5-d6))));
    double denom = 1.0/(va+vb+vc);
    double v = vb*denom, w = vc*denom;
    return a.add(ab.multiply(v)).add(ac.multiply(w));
}

Intersection* Triangle::doIntersectionWithTriangle(const Ray& ray, const Vector3Dd& v0, const Vector3Dd& v1, const Vector3Dd& v2)
{
    Vector3Dd e1 = v1.subtract(v0), e2 = v2.subtract(v0), h = ray.direction().crossProduct(e2);
    double a = e1.dotProduct(h);
    if (std::abs(a) < VSDK::EPSILON) return 0;
    double f = 1.0/a;
    Vector3Dd s = ray.origin().subtract(v0);
    double u = f * s.dotProduct(h);
    if (u < 0 || u > 1) return 0;
    Vector3Dd q = s.crossProduct(e1);
    double v = f * ray.direction().dotProduct(q);
    if (v < 0 || u + v > 1) return 0;
    double t = f * e2.dotProduct(q);
    if (t <= VSDK::EPSILON) return 0;

    Intersection* out = new Intersection();
    out->t = t;
    out->point = ray.origin().add(ray.direction().multiply(t));
    out->normal = e1.crossProduct(e2).normalized();
    return out;
}

int Triangle::containmentTest(const Vector3Dd& p0, const Vector3Dd& p1, const Vector3Dd& p2, const Vector3Dd& p, double distanceTolerance)
{
    Vector3Dd q = closestPointOnTriangle(p, p0, p1, p2);
    if (q.subtract(p).length() <= distanceTolerance) return static_cast<int>(Containment::LIMIT);
    return static_cast<int>(Containment::OUTSIDE);
}

void Triangle::minMax(const Vector3Dd& p0, const Vector3Dd& p1, const Vector3Dd& p2, double mm[6])
{
    mm[0] = java::Math::min(p0.x(), java::Math::min(p1.x(), p2.x()));
    mm[1] = java::Math::min(p0.y(), java::Math::min(p1.y(), p2.y()));
    mm[2] = java::Math::min(p0.z(), java::Math::min(p1.z(), p2.z()));
    mm[3] = java::Math::max(p0.x(), java::Math::max(p1.x(), p2.x()));
    mm[4] = java::Math::max(p0.y(), java::Math::max(p1.y(), p2.y()));
    mm[5] = java::Math::max(p0.z(), java::Math::max(p1.z(), p2.z()));
}

static java::String intToStr(int val) {
    char buf[32];
    snprintf(buf, sizeof(buf), "%d", val);
    return java::String(buf);
}

java::String Triangle::toString() const
{
    return java::String("f < ") + intToStr(p0) + ", " +
        intToStr(p1) + ", " +
        intToStr(p2) + " >";
}
