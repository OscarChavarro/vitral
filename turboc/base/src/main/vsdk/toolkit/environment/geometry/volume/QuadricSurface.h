#ifndef __QUADRIC_SURFACE__
#define __QUADRIC_SURFACE__

#include "vsdk/toolkit/common/VSDK.h"

#include "vsdk/toolkit/environment/geometry/volume/Solid.h"
#include "java/lang/Math.h"
#include "vsdk/toolkit/common/numerics/FloatingInterval.h"

// F(x,y,z) = square dot (x*x,y*y,z*z) + mixed dot (xy,xz,yz)
//              + linear dot (x,y,z) + constant; the solid is F <= 0.
class QuadricSurface : public Solid {
private:
    Vector3Dd square;
    Vector3Dd mixed;
    Vector3Dd linear;
    double constant;
    FloatingInterval enclose(const FloatingInterval& x, const FloatingInterval& y,
        const FloatingInterval& z) const;
    RayFirstHitStatus::Value roots(const Ray& ray, const RayCrossingQuery& query,
        double& first, double& second) const;
public:
    static const Class CLASS;
    virtual const Class *getClass() const;
    QuadricSurface(const Vector3Dd& square, const Vector3Dd& mixed,
        const Vector3Dd& linear, double constant);
    // The evaluation order matches the historical POV implementation.
    double evaluate(const Vector3Dd& point) const {
        double result = point.dotProduct(linear) + constant;
        result += point.multiply(point).dotProduct(square);
        result += mixed.x()*point.x()*point.y() + mixed.y()*point.x()*point.z() +
            mixed.z()*point.y()*point.z();
        return result;
    }
    Vector3Dd gradient(const Vector3Dd& point) const {
        const Vector3Dd base = square.multiply(2.0).multiply(point).add(linear);
        return Vector3Dd(base.x()+mixed.x()*point.y()+mixed.y()*point.z(),
            base.y()+mixed.x()*point.x()+mixed.z()*point.z(),
            base.z()+mixed.y()*point.x()+mixed.z()*point.y());
    }
    virtual Geometry* copyGeometry() const;
    Vector3Dd& squareCoefficients() { return square; }
    const Vector3Dd& squareCoefficients() const { return square; }
    Vector3Dd& mixedCoefficients() { return mixed; }
    const Vector3Dd& mixedCoefficients() const { return mixed; }
    Vector3Dd& linearCoefficients() { return linear; }
    const Vector3Dd& linearCoefficients() const { return linear; }
    double& constantCoefficient() { return constant; }
    double constantCoefficient() const { return constant; }
    static BOOLEAN solve(double a, double b, double c, double& first, double& second) {
        if (a != 0.0) {
            const double discriminant = b*b - 4.0*a*c;
            if (discriminant < 0.0) return FALSE;
            const double root = Math::sqrt(discriminant);
            first = (-b + root)/(2.0*a);
            second = (-b - root)/(2.0*a);
        } else {
            if (b == 0.0) return FALSE;
            first = second = -c/b;
        }
        return TRUE;
    }
    virtual void getBounds(GeometryBounds& result) const;
    virtual GeometryContainment::Value classifyPoint(const Vector3Dd& point, double tolerance) const;
    // Sign enclosures of F over the ball's bounding box prove Inside/Outside;
    // Boundary requires two certified points of opposite sign within the ball.
    virtual GeometryContainment::Value classifyPointCertified(const Vector3Dd& point, double tolerance) const;
    virtual GeometryContainment::Value classifyBoxCertified(const Vector3Dd& center, const Vector3Dd& halfWidths) const;
    virtual RayFirstHitStatus::Value queryFirstHit(const Ray& ray, const RayCrossingQuery& query,
        RayHit* hit, RayBoundary* boundary = NULL) const;
    virtual RayFirstHitStatus::Value querySurfaceDetail(const Ray& ray, const RayBoundary& boundary,
        RayHit* hit) const;
    virtual RayCrossingSupport::Value emitRayCrossings(const Ray& ray, const RayCrossingQuery& query,
        RayCrossingSink& sink) const;
    virtual RaySolidQueryStatus::Value classifyRay(const Ray& ray, const RayCrossingQuery& query,
        RaySolidClassification& result) const;
    virtual BOOLEAN doIntersectionFirstHit(const Ray& ray, RayHit* hit);
    virtual void doExtraInformation(const Ray& ray, double t, RayHit* hit);
    virtual int doContainmentTest(const Vector3Dd& point, double tolerance);
    virtual double* getMinMax();
};
#endif
