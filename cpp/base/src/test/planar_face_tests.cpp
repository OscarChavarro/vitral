#include <type_traits>
#include <gtest/gtest.h>
#include "java/lang/Math.h"
#include "vsdk/toolkit/environment/geometry/element/PlanarFace.h"

// PlanarFace is exercised here with no material, solver element or
// application header: a face is built from positions only

namespace {

PlanarFace
triangle() {
    const Vector3Df p[3] = {Vector3Df(0, 0, 0), Vector3Df(2, 0, 0), Vector3Df(0, 2, 0)};
    return PlanarFace(3, p);
}

PlanarFace
square() {
    const Vector3Df p[4] = {Vector3Df(0, 0, 1), Vector3Df(1, 0, 1), Vector3Df(1, 1, 1), Vector3Df(0, 1, 1)};
    return PlanarFace(4, p);
}

// A trapezoid: an irregular quadrilateral, whose Jacobian is not constant
PlanarFace
trapezoid() {
    const Vector3Df p[4] = {Vector3Df(0, 0, 0), Vector3Df(4, 0, 0), Vector3Df(3, 2, 0), Vector3Df(1, 2, 0)};
    return PlanarFace(4, p);
}

void
expectPoint(float x, float y, float z, const Vector3Df &p, float tolerance) {
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
    EXPECT_FLOAT_EQ(2.0F, face.getArea());
    expectPoint(0, 0, 1, face.getNormal(), 0.0F);
    EXPECT_FLOAT_EQ(0.0F, face.getPlaneConstant());
    EXPECT_EQ(2, face.getDominantAxis());
    EXPECT_FALSE(face.hasJacobian());
    expectPoint(2.0F / 3.0F, 2.0F / 3.0F, 0, face.getMidPoint(), 1e-6F);
}

TEST(PlanarFace, ParallelogramHasNoJacobian) {
    const PlanarFace face = square();
    ASSERT_TRUE(face.isValid());
    EXPECT_FLOAT_EQ(1.0F, face.getArea());
    EXPECT_FALSE(face.hasJacobian());
    EXPECT_FLOAT_EQ(-1.0F, face.getPlaneConstant());
    EXPECT_FLOAT_EQ(0.0F, face.planeDistance(Vector3Df(0.3F, 0.7F, 1.0F)));
}

TEST(PlanarFace, IrregularQuadrilateralAreaAndJacobian) {
    const PlanarFace face = trapezoid();
    ASSERT_TRUE(face.isValid());
    EXPECT_FLOAT_EQ(6.0F, face.getArea());
    ASSERT_TRUE(face.hasJacobian());
    EXPECT_FLOAT_EQ(face.getArea(), face.getJacobianA() + 0.5F * (face.getJacobianB() + face.getJacobianC()));
}

TEST(PlanarFace, ParametersInvertTheMapping) {
    const PlanarFace faces[3] = {triangle(), square(), trapezoid()};
    for ( const PlanarFace &face : faces ) {
        const Vector3Df p = face.pointAt(0.25, 0.375);
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
    EXPECT_FALSE(triangle().parameters(Vector3Df(1.5F, 1.5F, 0), &u, &v));
    EXPECT_FALSE(square().parameters(Vector3Df(1.5F, 0.5F, 1), &u, &v));
    EXPECT_FALSE(trapezoid().parameters(Vector3Df(0.2F, 1.8F, 0), &u, &v));
}

TEST(PlanarFace, UniformMappingPreservesArea) {
    const PlanarFace face = trapezoid();
    // The line of uniform u = 0.5 splits the area in two equal halves: by
    // symmetry of the trapezoid it is the vertical line x = 2
    const Vector3Df bottom = face.uniformPointAt(0.5, 0.0);
    const Vector3Df top = face.uniformPointAt(0.5, 1.0);
    EXPECT_NEAR(2.0F, bottom.x(), 1e-5F);
    EXPECT_NEAR(2.0F, top.x(), 1e-5F);

    // The line of uniform v = 0.5 leaves half of the area (3) under it: the
    // trapezoid of height h under it has area (4 + (4 - h)) h / 2 = 3
    const Vector3Df middle = face.uniformPointAt(0.5, 0.5);
    const float h = 4.0F - static_cast<float>(java::Math::sqrt(10.0));
    EXPECT_NEAR(h, middle.y(), 1e-5F);

    double u;
    double v;
    EXPECT_TRUE(face.uniformParameters(face.uniformPointAt(0.3, 0.6), &u, &v));
    EXPECT_NEAR(0.3, u, 1e-5);
    EXPECT_NEAR(0.6, v, 1e-5);
}

TEST(PlanarFace, RayHit) {
    const PlanarFace face = square();
    const Vector3Df origin(0.5F, 0.25F, 3.0F);

    float normalDotDirection;
    EXPECT_EQ(PlanarFace::Side::FRONT, face.sideFacing(Vector3Df(0, 0, -1), &normalDotDirection));
    const float t = face.planeHitParameter(origin, normalDotDirection);
    EXPECT_FLOAT_EQ(2.0F, t);

    double u;
    double v;
    EXPECT_TRUE(face.parameters(Vector3Df(0.5F, 0.25F, 1.0F), &u, &v));
    EXPECT_NEAR(0.5, u, 1e-6);
    EXPECT_NEAR(0.25, v, 1e-6);

    EXPECT_EQ(PlanarFace::Side::BACK, face.sideFacing(Vector3Df(0, 0, 1), &normalDotDirection));
    EXPECT_EQ(PlanarFace::Side::PARALLEL, face.sideFacing(Vector3Df(1, 0, 0), &normalDotDirection));
}

TEST(PlanarFace, RayHitInOneCall) {
    const PlanarFace face = square();
    const Vector3Df origin(0.5F, 0.25F, 3.0F);
    const Vector3Df down(0, 0, -1);

    PlanarFace::RayHit hit;
    ASSERT_TRUE(face.rayHit(origin, down, true, false, 0.0F, 10.0F, &hit));
    EXPECT_EQ(PlanarFace::Side::FRONT, hit.getSide());
    EXPECT_FLOAT_EQ(2.0F, hit.getDistance());
    EXPECT_FLOAT_EQ(0.5F, hit.getPointX());
    EXPECT_FLOAT_EQ(0.25F, hit.getPointY());
    EXPECT_FLOAT_EQ(1.0F, hit.getPointZ());
    EXPECT_NEAR(0.5, hit.getU(), 1e-6);
    EXPECT_NEAR(0.25, hit.getV(), 1e-6);

    // Same answers as the separate calls
    float normalDotDirection;
    face.sideFacing(down, &normalDotDirection);
    EXPECT_EQ(face.planeHitParameter(origin, normalDotDirection), hit.getDistance());

    EXPECT_FALSE(face.rayHit(origin, down, false, true, 0.0F, 10.0F, &hit));     // Front side not accepted
    EXPECT_FALSE(face.rayHit(origin, down, true, false, 0.0F, 1.5F, &hit));      // Too far
    EXPECT_FALSE(face.rayHit(origin, down, true, false, 2.5F, 10.0F, &hit));     // Too close
    EXPECT_FALSE(face.rayHit(origin, Vector3Df(1, 0, 0), true, true, 0.0F, 10.0F, &hit)); // Parallel
    EXPECT_FALSE(face.rayHit(Vector3Df(5.0F, 0.25F, 3.0F), down, true, false, 0.0F, 10.0F, &hit)); // Outside

    ASSERT_TRUE(face.rayHit(Vector3Df(0.5F, 0.25F, -1.0F), Vector3Df(0, 0, 1), false, true, 0.0F, 10.0F, &hit));
    EXPECT_EQ(PlanarFace::Side::BACK, hit.getSide());
}

TEST(PlanarFace, Facing) {
    const Vector3Df floor[4] = {Vector3Df(0, 0, 0), Vector3Df(1, 0, 0), Vector3Df(1, 1, 0), Vector3Df(0, 1, 0)};
    const Vector3Df ceiling[4] = {Vector3Df(0, 0, 1), Vector3Df(0, 1, 1), Vector3Df(1, 1, 1), Vector3Df(1, 0, 1)};
    const Vector3Df above[4] = {Vector3Df(0, 0, 2), Vector3Df(1, 0, 2), Vector3Df(1, 1, 2), Vector3Df(0, 1, 2)};
    const PlanarFace floorFace(4, floor);
    const PlanarFace ceilingFace(4, ceiling);
    const PlanarFace aboveFace(4, above);
    EXPECT_TRUE(floorFace.facing(ceilingFace));
    EXPECT_FALSE(floorFace.facing(aboveFace)); // Same orientation: one is behind the other
    EXPECT_FALSE(floorFace.facing(floorFace));  // Coplanar
}

TEST(PlanarFace, InvalidFaces) {
    const Vector3Df collinear[3] = {Vector3Df(0, 0, 0), Vector3Df(1, 0, 0), Vector3Df(2, 0, 0)};
    EXPECT_EQ(PlanarFace::BuildStatus::DEGENERATE_NORMAL, PlanarFace(3, collinear).getStatus());
    const Vector3Df pentagon[5] = {Vector3Df(), Vector3Df(), Vector3Df(), Vector3Df(), Vector3Df()};
    EXPECT_EQ(PlanarFace::BuildStatus::UNSUPPORTED_VERTEX_COUNT, PlanarFace(5, pentagon).getStatus());
    EXPECT_FALSE(PlanarFace().isValid());
}
