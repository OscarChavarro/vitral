#include <type_traits>
#include <gtest/gtest.h>
#include "java/lang/Math.h"
#include "vsdk/toolkit/environment/geometry/element/PlanarFace.h"

// PlanarFace is exercised here with no material, solver element or
// application header: a face is built from positions only

namespace {

PlanarFace
triangle() {
    const Vector3Dd p[3] = {Vector3Dd(0, 0, 0), Vector3Dd(2, 0, 0), Vector3Dd(0, 2, 0)};
    return PlanarFace(3, p);
}

PlanarFace
square() {
    const Vector3Dd p[4] = {Vector3Dd(0, 0, 1), Vector3Dd(1, 0, 1), Vector3Dd(1, 1, 1), Vector3Dd(0, 1, 1)};
    return PlanarFace(4, p);
}

// A trapezoid: an irregular quadrilateral, whose Jacobian is not constant
PlanarFace
trapezoid() {
    const Vector3Dd p[4] = {Vector3Dd(0, 0, 0), Vector3Dd(4, 0, 0), Vector3Dd(3, 2, 0), Vector3Dd(1, 2, 0)};
    return PlanarFace(4, p);
}

void
expectPoint(double x, double y, double z, const Vector3Dd &p, double tolerance) {
    EXPECT_NEAR(x, p.x(), tolerance);
    EXPECT_NEAR(y, p.y(), tolerance);
    EXPECT_NEAR(z, p.z(), tolerance);
}

}

TEST(PlanarFace, IsTriviallyCopyable) {
    EXPECT_TRUE(std::is_trivially_copyable<PlanarFace>::value);
}

TEST(PlanarFace, TriangleGeometry) {
    const PlanarFace face = triangle();
    ASSERT_TRUE(face.isValid());
    EXPECT_EQ(3, face.getNumberOfVertices());
    EXPECT_FLOAT_EQ(2.0, face.getArea());
    expectPoint(0, 0, 1, face.getNormal(), 0.0);
    EXPECT_FLOAT_EQ(0.0, face.getPlaneConstant());
    EXPECT_EQ(2, face.getDominantAxis());
    EXPECT_FALSE(face.hasJacobian());
    expectPoint(2.0 / 3.0, 2.0 / 3.0, 0, face.getMidPoint(), 1e-6);
}

TEST(PlanarFace, ParallelogramHasNoJacobian) {
    const PlanarFace face = square();
    ASSERT_TRUE(face.isValid());
    EXPECT_FLOAT_EQ(1.0, face.getArea());
    EXPECT_FALSE(face.hasJacobian());
    EXPECT_FLOAT_EQ(-1.0, face.getPlaneConstant());
    EXPECT_FLOAT_EQ(0.0, face.planeDistance(Vector3Dd(0.3, 0.7, 1.0)));
}

TEST(PlanarFace, IrregularQuadrilateralAreaAndJacobian) {
    const PlanarFace face = trapezoid();
    ASSERT_TRUE(face.isValid());
    EXPECT_FLOAT_EQ(6.0, face.getArea());
    ASSERT_TRUE(face.hasJacobian());
    EXPECT_FLOAT_EQ(face.getArea(), face.getJacobianA() + 0.5 * (face.getJacobianB() + face.getJacobianC()));
}

TEST(PlanarFace, ParametersInvertTheMapping) {
    const PlanarFace faces[3] = {triangle(), square(), trapezoid()};
    for ( const PlanarFace &face : faces ) {
        const Vector3Dd p = face.pointAt(0.25, 0.375);
        double u;
        double v;
        EXPECT_TRUE(face.parameters(p, &u, &v));
        EXPECT_NEAR(0.25, u, 1e-5);
        EXPECT_NEAR(0.375, v, 1e-5);
    }
}

TEST(PlanarFace, PointsOutsideAreReportedOutside) {
    double u;
    double v;
    EXPECT_FALSE(triangle().parameters(Vector3Dd(1.5, 1.5, 0), &u, &v));
    EXPECT_FALSE(square().parameters(Vector3Dd(1.5, 0.5, 1), &u, &v));
    EXPECT_FALSE(trapezoid().parameters(Vector3Dd(0.2, 1.8, 0), &u, &v));
}

TEST(PlanarFace, UniformMappingPreservesArea) {
    const PlanarFace face = trapezoid();
    // The line of uniform u = 0.5 splits the area in two equal halves: by
    // symmetry of the trapezoid it is the vertical line x = 2
    const Vector3Dd bottom = face.uniformPointAt(0.5, 0.0);
    const Vector3Dd top = face.uniformPointAt(0.5, 1.0);
    EXPECT_NEAR(2.0, bottom.x(), 1e-5);
    EXPECT_NEAR(2.0, top.x(), 1e-5);

    // The line of uniform v = 0.5 leaves half of the area (3) under it: the
    // trapezoid of height h under it has area (4 + (4 - h)) h / 2 = 3
    const Vector3Dd middle = face.uniformPointAt(0.5, 0.5);
    const double h = 4.0 - static_cast<double>(java::Math::sqrt(10.0));
    EXPECT_NEAR(h, middle.y(), 1e-5);

    double u;
    double v;
    EXPECT_TRUE(face.uniformParameters(face.uniformPointAt(0.3, 0.6), &u, &v));
    EXPECT_NEAR(0.3, u, 1e-5);
    EXPECT_NEAR(0.6, v, 1e-5);
}

TEST(PlanarFace, RayHit) {
    const PlanarFace face = square();
    const Vector3Dd origin(0.5, 0.25, 3.0);

    double normalDotDirection;
    EXPECT_EQ(PlanarFace::Side::FRONT, face.sideFacing(Vector3Dd(0, 0, -1), &normalDotDirection));
    const double t = face.planeHitParameter(origin, normalDotDirection);
    EXPECT_FLOAT_EQ(2.0, t);

    double u;
    double v;
    EXPECT_TRUE(face.parameters(Vector3Dd(0.5, 0.25, 1.0), &u, &v));
    EXPECT_NEAR(0.5, u, 1e-6);
    EXPECT_NEAR(0.25, v, 1e-6);

    EXPECT_EQ(PlanarFace::Side::BACK, face.sideFacing(Vector3Dd(0, 0, 1), &normalDotDirection));
    EXPECT_EQ(PlanarFace::Side::PARALLEL, face.sideFacing(Vector3Dd(1, 0, 0), &normalDotDirection));
}

TEST(PlanarFace, RayHitInOneCall) {
    const PlanarFace face = square();
    const Vector3Dd origin(0.5, 0.25, 3.0);
    const Vector3Dd down(0, 0, -1);

    PlanarFace::RayHit hit;
    ASSERT_TRUE(face.rayHit(origin, down, true, false, 0.0, 10.0, &hit));
    EXPECT_EQ(PlanarFace::Side::FRONT, hit.getSide());
    EXPECT_FLOAT_EQ(2.0, hit.getDistance());
    EXPECT_FLOAT_EQ(0.5, hit.getPointX());
    EXPECT_FLOAT_EQ(0.25, hit.getPointY());
    EXPECT_FLOAT_EQ(1.0, hit.getPointZ());
    EXPECT_NEAR(0.5, hit.getU(), 1e-6);
    EXPECT_NEAR(0.25, hit.getV(), 1e-6);

    // Same answers as the separate calls
    double normalDotDirection;
    face.sideFacing(down, &normalDotDirection);
    EXPECT_EQ(face.planeHitParameter(origin, normalDotDirection), hit.getDistance());

    EXPECT_FALSE(face.rayHit(origin, down, false, true, 0.0, 10.0, &hit));     // Front side not accepted
    EXPECT_FALSE(face.rayHit(origin, down, true, false, 0.0, 1.5, &hit));      // Too far
    EXPECT_FALSE(face.rayHit(origin, down, true, false, 2.5, 10.0, &hit));     // Too close
    EXPECT_FALSE(face.rayHit(origin, Vector3Dd(1, 0, 0), true, true, 0.0, 10.0, &hit)); // Parallel
    EXPECT_FALSE(face.rayHit(Vector3Dd(5.0, 0.25, 3.0), down, true, false, 0.0, 10.0, &hit)); // Outside

    ASSERT_TRUE(face.rayHit(Vector3Dd(0.5, 0.25, -1.0), Vector3Dd(0, 0, 1), false, true, 0.0, 10.0, &hit));
    EXPECT_EQ(PlanarFace::Side::BACK, hit.getSide());
}

TEST(PlanarFace, Facing) {
    const Vector3Dd floor[4] = {Vector3Dd(0, 0, 0), Vector3Dd(1, 0, 0), Vector3Dd(1, 1, 0), Vector3Dd(0, 1, 0)};
    const Vector3Dd ceiling[4] = {Vector3Dd(0, 0, 1), Vector3Dd(0, 1, 1), Vector3Dd(1, 1, 1), Vector3Dd(1, 0, 1)};
    const Vector3Dd above[4] = {Vector3Dd(0, 0, 2), Vector3Dd(1, 0, 2), Vector3Dd(1, 1, 2), Vector3Dd(0, 1, 2)};
    const PlanarFace floorFace(4, floor);
    const PlanarFace ceilingFace(4, ceiling);
    const PlanarFace aboveFace(4, above);
    EXPECT_TRUE(floorFace.facing(ceilingFace));
    EXPECT_FALSE(floorFace.facing(aboveFace)); // Same orientation: one is behind the other
    EXPECT_FALSE(floorFace.facing(floorFace));  // Coplanar
}

TEST(PlanarFace, InvalidFaces) {
    const Vector3Dd collinear[3] = {Vector3Dd(0, 0, 0), Vector3Dd(1, 0, 0), Vector3Dd(2, 0, 0)};
    EXPECT_EQ(PlanarFace::BuildStatus::DEGENERATE_NORMAL, PlanarFace(3, collinear).getStatus());
    const Vector3Dd pentagon[5] = {Vector3Dd(), Vector3Dd(), Vector3Dd(), Vector3Dd(), Vector3Dd()};
    EXPECT_EQ(PlanarFace::BuildStatus::UNSUPPORTED_VERTEX_COUNT, PlanarFace(5, pentagon).getStatus());
    EXPECT_FALSE(PlanarFace().isValid());
}
