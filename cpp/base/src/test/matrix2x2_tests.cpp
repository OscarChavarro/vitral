#include <gtest/gtest.h>
#include "java/io/PrintStream.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix2x2.h"
#include "vsdk/toolkit/io/wrapper/Matrix2x2Printer.h"

TEST(Matrix2x2Test, IdentityAndAccessors) {
    const Matrix2x2 identity;
    EXPECT_EQ(identity.get(0, 0), 1.0f);
    EXPECT_EQ(identity.get(0, 1), 0.0f);
    EXPECT_EQ(identity.get(1, 0), 0.0f);
    EXPECT_EQ(identity.get(1, 1), 1.0f);
    EXPECT_EQ(identity.getTranslation(0), 0.0f);
    const Matrix2x2 m(0.5f, 0.25f, -1.0f, 2.0f, 3.0f, -4.0f);
    EXPECT_EQ(m.get(1, 0), -1.0f);
    EXPECT_EQ(m.getTranslation(1), -4.0f);
}

TEST(Matrix2x2Test, TransformPoint) {
    const Matrix2x2 m(0.5f, 0.0f, 0.0f, 0.5f, 0.5f, 0.0f);
    const Vector2Df p = m.transformPoint(Vector2Df(1.0f, 1.0f));
    EXPECT_EQ(p.x(), 1.0f);
    EXPECT_EQ(p.y(), 0.5f);
    const Matrix2x2 g(0.3f, 0.7f, 1.1f, 1.3f, 0.2f, 0.9f);
    const Vector2Df q = g.transformPoint(Vector2Df(0.6f, 0.1f));
    EXPECT_EQ(q.x(), 0.3f * 0.6f + 0.7f * 0.1f + 0.2f);
    EXPECT_EQ(q.y(), 1.1f * 0.6f + 1.3f * 0.1f + 0.9f);
}

TEST(Matrix2x2Test, MultiplyComposes) {
    const Matrix2x2 a(0.5f, 0.0f, 0.0f, 0.5f, 0.5f, 0.0f);
    const Matrix2x2 b(0.5f, 0.0f, 0.0f, 0.5f, 0.0f, 0.5f);
    const Vector2Df p(0.3f, 0.6f);
    const Vector2Df composed = a.multiply(b).transformPoint(p);
    const Vector2Df stepwise = a.transformPoint(b.transformPoint(p));
    EXPECT_EQ(composed.x(), stepwise.x());
    EXPECT_EQ(composed.y(), stepwise.y());
    EXPECT_TRUE(Matrix2x2().multiply(a) == a);
}

TEST(Matrix2x2PrinterTest, NullStreamIsIgnored) {
    Matrix2x2Printer::print(Matrix2x2(), nullptr);
    SUCCEED();
}
