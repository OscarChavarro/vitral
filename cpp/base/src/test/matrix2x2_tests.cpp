#include <gtest/gtest.h>
#include "java/io/PrintStream.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix2x2.h"
#include "vsdk/toolkit/io/wrapper/Matrix2x2Printer.h"

TEST(Matrix2x2Test, IdentityAndAccessors) {
    const Matrix2x2 identity;
    EXPECT_EQ(identity.get(0, 0), 1.0);
    EXPECT_EQ(identity.get(0, 1), 0.0);
    EXPECT_EQ(identity.get(1, 0), 0.0);
    EXPECT_EQ(identity.get(1, 1), 1.0);
    EXPECT_EQ(identity.getTranslation(0), 0.0);
    const Matrix2x2 m(0.5, 0.25, -1.0, 2.0, 3.0, -4.0);
    EXPECT_EQ(m.get(1, 0), -1.0);
    EXPECT_EQ(m.getTranslation(1), -4.0);
}

TEST(Matrix2x2Test, TransformPoint) {
    const Matrix2x2 m(0.5, 0.0, 0.0, 0.5, 0.5, 0.0);
    const Vector2Dd p = m.transformPoint(Vector2Dd(1.0, 1.0));
    EXPECT_EQ(p.x, 1.0);
    EXPECT_EQ(p.y, 0.5);
    const Matrix2x2 g(0.3, 0.7, 1.1, 1.3, 0.2, 0.9);
    const Vector2Dd q = g.transformPoint(Vector2Dd(0.6, 0.1));
    EXPECT_EQ(q.x, 0.3 * 0.6 + 0.7 * 0.1 + 0.2);
    EXPECT_EQ(q.y, 1.1 * 0.6 + 1.3 * 0.1 + 0.9);
}

TEST(Matrix2x2Test, MultiplyComposes) {
    const Matrix2x2 a(0.5, 0.0, 0.0, 0.5, 0.5, 0.0);
    const Matrix2x2 b(0.5, 0.0, 0.0, 0.5, 0.0, 0.5);
    const Vector2Dd p(0.3, 0.6);
    const Vector2Dd composed = a.multiply(b).transformPoint(p);
    const Vector2Dd stepwise = a.transformPoint(b.transformPoint(p));
    EXPECT_EQ(composed.x, stepwise.x);
    EXPECT_EQ(composed.y, stepwise.y);
    EXPECT_TRUE(Matrix2x2().multiply(a) == a);
}

TEST(Matrix2x2PrinterTest, NullStreamIsIgnored) {
    Matrix2x2Printer::print(Matrix2x2(), nullptr);
    SUCCEED();
}
