#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector2Dd.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

// The double precision operations of Vector2Dd and Vector3Dd that the
// radiosity clients use (the cases of the cpp port's vector_tests.cpp)

TEST(Vector2DdTest, SubtractAndLengthSquared) {
    const Vector2Dd d = Vector2Dd(4.0, -1.0).subtract(Vector2Dd(1.0, 3.0));
    EXPECT_EQ(d.x, 3.0);
    EXPECT_EQ(d.y, -4.0);
    EXPECT_EQ(d.lengthSquared(), 25.0);
}

TEST(Vector3DdTest, LengthsAndDistances) {
    const Vector3Dd a(1.0, -2.0, 2.0);
    EXPECT_EQ(a.lengthSquared(), 9.0);
    EXPECT_EQ(a.manhattanLength(), 5.0);
    const Vector3Dd b(4.0, 2.0, 2.0);
    EXPECT_EQ(a.distance(b), 5.0);
    EXPECT_EQ(a.distanceSquared(b), 25.0);
    EXPECT_EQ(a.distance(b), b.subtract(a).length());
}

TEST(Vector3DdTest, MultiplyByInverseKeepsSmallDivisors) {
    const Vector3Dd v(2.0, 4.0, -6.0);
    const Vector3Dd scaled = v.multiplyByInverse(2.0, 1e-6);
    EXPECT_EQ(scaled.x(), 1.0);
    EXPECT_EQ(scaled.y(), 2.0);
    EXPECT_EQ(scaled.z(), -3.0);
    EXPECT_TRUE(v.multiplyByInverse(1e-7, 1e-6) == v);
    EXPECT_TRUE(v.multiplyByInverse(-1e-7, 1e-6) == v);
    // The factor is computed once: x * (1 / s), not x / s
    const double s = 3.0;
    EXPECT_EQ(Vector3Dd(1.0, 0.0, 0.0).multiplyByInverse(s, 1e-6).x(), (1.0 / s) * 1.0);
}

TEST(Vector3DdTest, NormalizedWithEpsilon) {
    const Vector3Dd n = Vector3Dd(3.0, 4.0, 12.0).normalized(1e-6);
    const double factor = 1.0 / 13.0;
    EXPECT_EQ(n.x(), factor * 3.0);
    EXPECT_EQ(n.y(), factor * 4.0);
    EXPECT_EQ(n.z(), factor * 12.0);
    const Vector3Dd tiny(1e-8, 0.0, 0.0);
    EXPECT_TRUE(tiny.normalized(1e-6) == tiny);
}

TEST(Vector3DdTest, DominantAxis) {
    EXPECT_EQ(Vector3Dd(-5.0, 1.0, 2.0).dominantAxis(), 0);
    EXPECT_EQ(Vector3Dd(1.0, -5.0, 2.0).dominantAxis(), 1);
    EXPECT_EQ(Vector3Dd(1.0, 2.0, -5.0).dominantAxis(), 2);
    EXPECT_EQ(Vector3Dd(3.0, -3.0, 3.0).dominantAxis(), 0);
    EXPECT_EQ(Vector3Dd(1.0, 3.0, -3.0).dominantAxis(), 1);
}

TEST(Vector3DdTest, AssignmentReplacesValue) {
    Vector3Dd v(1.0, 2.0, 3.0);
    v = v.withY(5.0);
    EXPECT_TRUE(v == Vector3Dd(1.0, 5.0, 3.0));
}

TEST(Vector3DdTest, IndexedComponentsMatchNamedAccessors) {
    const Vector3Dd value(1.5, -2.0, 3.25);
    EXPECT_DOUBLE_EQ(value.x(), value.component(0));
    EXPECT_DOUBLE_EQ(value.y(), value.component(1));
    EXPECT_DOUBLE_EQ(value.z(), value.component(2));
    const Vector3Dd copied(value);
    EXPECT_DOUBLE_EQ(value.component(2), copied.component(2));
}

VITRAL_TEST_MAIN()
