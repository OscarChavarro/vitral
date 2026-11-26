#include <gtest/gtest.h>
#include "vsdk/toolkit/common/linealAlgebra/Vector2Dd.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector2Df.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Df.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector4Dd.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector4Df.h"
TEST(Vector2DdTest, AddAndScale) {
    EXPECT_TRUE(Vector2Dd(1.5, -2.0).add(Vector2Dd(-0.5, 3.0)).epsilonEquals(Vector2Dd(1.0, 1.0), 1e-9));
    EXPECT_TRUE(Vector2Dd(2.0, -3.0).multiply(2.5).epsilonEquals(Vector2Dd(5.0, -7.5), 1e-12));
}

TEST(Vector2DfTest, AddAndScale) {
    EXPECT_TRUE(Vector2Df(1.5f, -2.0f).add(Vector2Df(-0.5f, 3.0f)).epsilonEquals(Vector2Df(1.0f, 1.0f), 1e-6f));
    EXPECT_TRUE(Vector2Df(2.0f, -3.0f).multiply(2.5f).epsilonEquals(Vector2Df(5.0f, -7.5f), 1e-5f));
}

TEST(Vector3DdTest, CrossAndNormalize) {
    Vector3Dd c = Vector3Dd(1,0,0).crossProduct(Vector3Dd(0,1,0));
    EXPECT_TRUE(c.epsilonEquals(Vector3Dd(0,0,1), 1e-9));
    EXPECT_TRUE(Vector3Dd(3,4,12).normalized().epsilonEquals(Vector3Dd(3,4,12).multiply(1.0/13.0), 1e-12));
}

TEST(Vector3DfTest, CrossAndNormalize) {
    Vector3Df c = Vector3Df(1,0,0).crossProduct(Vector3Df(0,1,0));
    EXPECT_TRUE(c.epsilonEquals(Vector3Df(0,0,1), 1e-5f));
    EXPECT_TRUE(Vector3Df(3,4,12).normalized().epsilonEquals(Vector3Df(3,4,12).multiply(1.0f/13.0f), 1e-5f));
}

TEST(Vector4Test, DivideByW) {
    EXPECT_TRUE(Vector4Dd(4,6,8,2).dividedByW().epsilonEquals(Vector4Dd(2,3,4,1), 1e-12));
    EXPECT_TRUE(Vector4Df(4,6,8,2).dividedByW().epsilonEquals(Vector4Df(2,3,4,1), 1e-5f));
}

TEST(Vector2DfTest, SubtractAndLengthSquared) {
    const Vector2Df d = Vector2Df(4.0f, -1.0f).subtract(Vector2Df(1.0f, 3.0f));
    EXPECT_EQ(d.x(), 3.0f);
    EXPECT_EQ(d.y(), -4.0f);
    EXPECT_EQ(d.lengthSquared(), 25.0f);
}

TEST(Vector3DfTest, LengthsAndDistances) {
    const Vector3Df a(1.0f, -2.0f, 2.0f);
    EXPECT_EQ(a.lengthSquared(), 9.0f);
    EXPECT_EQ(a.manhattanLength(), 5.0f);
    const Vector3Df b(4.0f, 2.0f, 2.0f);
    EXPECT_EQ(a.distance(b), 5.0f);
    EXPECT_EQ(a.distanceSquared(b), 25.0f);
    EXPECT_EQ(a.distance(b), b.subtract(a).length());
}

TEST(Vector3DfTest, MultiplyByInverseKeepsSmallDivisors) {
    const Vector3Df v(2.0f, 4.0f, -6.0f);
    const Vector3Df scaled = v.multiplyByInverse(2.0f, 1e-6f);
    EXPECT_EQ(scaled.x(), 1.0f);
    EXPECT_EQ(scaled.y(), 2.0f);
    EXPECT_EQ(scaled.z(), -3.0f);
    EXPECT_TRUE(v.multiplyByInverse(1e-7f, 1e-6f) == v);
    EXPECT_TRUE(v.multiplyByInverse(-1e-7f, 1e-6f) == v);
    // The factor is computed once: x * (1 / s), not x / s
    const float s = 3.0f;
    EXPECT_EQ(Vector3Df(1.0f, 0.0f, 0.0f).multiplyByInverse(s, 1e-6f).x(), (1.0f / s) * 1.0f);
}

TEST(Vector3DfTest, NormalizedWithEpsilon) {
    const Vector3Df n = Vector3Df(3.0f, 4.0f, 12.0f).normalized(1e-6f);
    const float factor = 1.0f / 13.0f;
    EXPECT_EQ(n.x(), factor * 3.0f);
    EXPECT_EQ(n.y(), factor * 4.0f);
    EXPECT_EQ(n.z(), factor * 12.0f);
    const Vector3Df tiny(1e-8f, 0.0f, 0.0f);
    EXPECT_TRUE(tiny.normalized(1e-6f) == tiny);
}

TEST(Vector3DfTest, DominantAxis) {
    EXPECT_EQ(Vector3Df(-5.0f, 1.0f, 2.0f).dominantAxis(), 0);
    EXPECT_EQ(Vector3Df(1.0f, -5.0f, 2.0f).dominantAxis(), 1);
    EXPECT_EQ(Vector3Df(1.0f, 2.0f, -5.0f).dominantAxis(), 2);
    EXPECT_EQ(Vector3Df(3.0f, -3.0f, 3.0f).dominantAxis(), 0);
    EXPECT_EQ(Vector3Df(1.0f, 3.0f, -3.0f).dominantAxis(), 1);
}

TEST(Vector3DfTest, AssignmentReplacesValue) {
    Vector3Df v(1.0f, 2.0f, 3.0f);
    v = v.withY(5.0f);
    EXPECT_TRUE(v == Vector3Df(1.0f, 5.0f, 3.0f));
}

TEST(Vector3DfTest, IndexedComponentsMatchNamedAccessors)
{
    const Vector3Df value(1.5f, -2.0f, 3.25f);
    EXPECT_FLOAT_EQ(value.x(), value.component(0));
    EXPECT_FLOAT_EQ(value.y(), value.component(1));
    EXPECT_FLOAT_EQ(value.z(), value.component(2));
    const Vector3Df copied(value);
    EXPECT_FLOAT_EQ(value.component(2), copied.component(2));
}

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

TEST(Vector3DdTest, IndexedComponentsMatchNamedAccessors)
{
    const Vector3Dd value(1.5, -2.0, 3.25);
    EXPECT_DOUBLE_EQ(value.x(), value.component(0));
    EXPECT_DOUBLE_EQ(value.y(), value.component(1));
    EXPECT_DOUBLE_EQ(value.z(), value.component(2));
    const Vector3Dd copied(value);
    EXPECT_DOUBLE_EQ(value.component(2), copied.component(2));
}
