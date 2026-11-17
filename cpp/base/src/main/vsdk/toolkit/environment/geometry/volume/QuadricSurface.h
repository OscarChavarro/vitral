#ifndef __QUADRIC_SURFACE__
#define __QUADRIC_SURFACE__

#include "vsdk/toolkit/environment/geometry/volume/Solid.h"
#include "java/lang/Math.h"
#include "vsdk/toolkit/common/numerics/FloatingInterval.h"
class PolyhedralBoundedSolid;

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
    RayFirstHitStatus roots(const Ray& ray, const RayCrossingQuery& query,
        double& first, double& second) const;

    /**
    The state is the ten coefficients of
    Ax^2 + By^2 + Cz^2 + Dxy + Exz + Fyz + Gx + Hy + Iz + J = 0, any real
    value being valid for each one.
    */
    void addControlSpecifications();
public:
    QuadricSurface(const Vector3Dd& square, const Vector3Dd& mixed,
        const Vector3Dd& linear, double constant);

    double getSquareX() const { return square.x(); }
    double getSquareY() const { return square.y(); }
    double getSquareZ() const { return square.z(); }
    double getMixedXY() const { return mixed.x(); }
    double getMixedXZ() const { return mixed.y(); }
    double getMixedYZ() const { return mixed.z(); }
    double getLinearX() const { return linear.x(); }
    double getLinearY() const { return linear.y(); }
    double getLinearZ() const { return linear.z(); }
    double getConstant() const { return constant; }

    void setSquareX(double value) { square = square.withX(value); }
    void setSquareY(double value) { square = square.withY(value); }
    void setSquareZ(double value) { square = square.withZ(value); }
    void setMixedXY(double value) { mixed = mixed.withX(value); }
    void setMixedXZ(double value) { mixed = mixed.withY(value); }
    void setMixedYZ(double value) { mixed = mixed.withZ(value); }
    void setLinearX(double value) { linear = linear.withX(value); }
    void setLinearY(double value) { linear = linear.withY(value); }
    void setLinearZ(double value) { linear = linear.withZ(value); }
    void setConstant(double value) { constant = value; }
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
    Geometry* copyGeometry() const override;
    Vector3Dd& squareCoefficients() { return square; }
    const Vector3Dd& squareCoefficients() const { return square; }
    Vector3Dd& mixedCoefficients() { return mixed; }
    const Vector3Dd& mixedCoefficients() const { return mixed; }
    Vector3Dd& linearCoefficients() { return linear; }
    const Vector3Dd& linearCoefficients() const { return linear; }
    double& constantCoefficient() { return constant; }
    double constantCoefficient() const { return constant; }
    static bool solve(double a, double b, double c, double& first, double& second) {
        if (a != 0.0) {
            const double discriminant = b*b - 4.0*a*c;
            if (discriminant < 0.0) return false;
            const double root = java::Math::sqrt(discriminant);
            first = (-b + root)/(2.0*a);
            second = (-b - root)/(2.0*a);
        } else {
            if (b == 0.0) return false;
            first = second = -c/b;
        }
        return true;
    }
    void getBounds(GeometryBounds& result) const override;
    GeometryContainment classifyPoint(const Vector3Dd& point, double tolerance) const override;
    // Sign enclosures of F over the ball's bounding box prove Inside/Outside;
    // Boundary requires two certified points of opposite sign within the ball.
    GeometryContainment classifyPointCertified(const Vector3Dd& point, double tolerance) const override;
    GeometryContainment classifyBoxCertified(const Vector3Dd& center, const Vector3Dd& halfWidths) const override;
    RayFirstHitStatus queryFirstHit(const Ray& ray, const RayCrossingQuery& query,
        RayHit* hit, RayBoundary* boundary = nullptr) const override;
    RayFirstHitStatus querySurfaceDetail(const Ray& ray, const RayBoundary& boundary,
        RayHit* hit) const override;
    RayCrossingSupport emitRayCrossings(const Ray& ray, const RayCrossingQuery& query,
        RayCrossingSink& sink) const override;
    RaySolidQueryStatus classifyRay(const Ray& ray, const RayCrossingQuery& query,
        RaySolidClassification& result) const override;
    bool doIntersectionFirstHit(const Ray& ray, RayHit* hit) override;
    void doExtraInformation(const Ray& ray, double t, RayHit* hit) override;
    int doContainmentTest(const Vector3Dd& point, double tolerance) override;
    double* getMinMax() override;

    //= Boundary representation =============================================

    /**
    Polyhedral approximation with the default number of divisions of
    `ImplicitSolid` (see `exportToPolyhedralBoundedSolid(int)`).
    */
    PolyhedralBoundedSolid* exportToPolyhedralBoundedSolid() override;

    /**
    Polyhedral approximation of the solid F <= 0, built with Euler
    operators by `PolyhedralBoundedSolidFieldConstructions`. A bounded
    quadric (an ellipsoid) is polygonized inside its bounds; an unbounded one
    (i.e. a cylinder or a paraboloid) is clipped by the cube of half side
    `ImplicitSolid::DEFAULT_CLIPPING_HALF_SIZE` centered at the origin.
    @param divisions number of grid cells along the largest side of the
    polygonized box (at least 1)
    @return a new polyhedral approximation, or null if the solid is empty or
    its bounds can not be determined
    */
    PolyhedralBoundedSolid* exportToPolyhedralBoundedSolid(int divisions);

    /**
    Polyhedral approximation of the part of the solid F <= 0 inside a box.
    @param divisions number of grid cells along the largest side of the box
    @param minimum minimum corner of the box
    @param maximum maximum corner of the box
    @return a new polyhedral approximation, or null if it is empty
    */
    PolyhedralBoundedSolid* exportToPolyhedralBoundedSolid(int divisions,
        const Vector3Dd& minimum, const Vector3Dd& maximum);
};
#endif
