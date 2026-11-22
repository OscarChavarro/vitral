#include <cmath>

#include <gtest/gtest.h>
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4f.h"
TEST(Matrix4x4dTest, TranslationRotationInverse) {
    Matrix4x4d t = Matrix4x4d().translation(5, -2, 1.5);
    EXPECT_TRUE(t.multiply(Vector3Dd(1,2,3)).epsilonEquals(Vector3Dd(6,0,4.5), 1e-9));

    Matrix4x4d r = Matrix4x4d().axisRotation(M_PI/2.0, 0,0,1);
    EXPECT_TRUE(r.multiply(Vector3Dd(1,0,0)).epsilonEquals(Vector3Dd(0,1,0), 1e-8));

    Matrix4x4d m = Matrix4x4d().scale(2,3,4).multiply(Matrix4x4d().translation(5,-1,2));
    EXPECT_TRUE(m.multiply(m.invert()).epsilonEquals(Matrix4x4d(), 1e-6));
}

TEST(Matrix4x4fTest, BasicOps) {
    Matrix4x4f t = Matrix4x4f().translation(5, -2, 1.5f);
    EXPECT_TRUE(t.multiply(Vector3Df(1,2,3)).epsilonEquals(Vector3Df(6,0,4.5f), 1e-4f));

    Matrix4x4f m = Matrix4x4f().scale(2,3,4).multiply(Matrix4x4f().translation(5,-1,2));
    EXPECT_TRUE(m.multiply(m.invert()).epsilonEquals(Matrix4x4f(), 1e-4f));
}

TEST(Matrix4x4fTest, StoresFloatsExactly) {
    // 0.1f is not a double-rounded value: it must come back bit for bit
    const float values[4][4] = {
        {0.1f, 0.2f, 0.3f, 0.4f},
        {1.0f / 3.0f, 2.0f, 3.0f, 4.0f},
        {5.0f, 6.0f, 7.0f, 8.0f},
        {9.0f, 10.0f, 11.0f, 12.0f}
    };
    const Matrix4x4f m(values);
    for ( int i = 0; i < 4; i++ ) {
        for ( int j = 0; j < 4; j++ ) {
            EXPECT_EQ(m.get(i, j), values[i][j]);
        }
    }
    EXPECT_EQ(m.withVal(2, 1, 0.7f).get(2, 1), 0.7f);
}

TEST(Matrix4x4fTest, ProductsComputeInFloatFromTheFirstTerm) {
    const float a[4][4] = {
        {0.1f, 0.2f, 0.3f, 0.4f},
        {0.5f, 0.6f, 0.7f, 0.8f},
        {0.9f, 1.1f, 1.2f, 1.3f},
        {1.4f, 1.5f, 1.6f, 1.7f}
    };
    const float b[4][4] = {
        {1.7f, 0.3f, 0.11f, 2.0f},
        {0.13f, 1.9f, 0.4f, 0.5f},
        {0.6f, 0.23f, 1.3f, 0.7f},
        {0.8f, 0.9f, 0.31f, 1.0f}
    };
    const Matrix4x4f p = Matrix4x4f(a).multiply(Matrix4x4f(b));
    for ( int i = 0; i < 4; i++ ) {
        for ( int j = 0; j < 4; j++ ) {
            const float expected = a[i][0] * b[0][j] + a[i][1] * b[1][j] + a[i][2] * b[2][j] + a[i][3] * b[3][j];
            EXPECT_EQ(p.get(i, j), expected);
        }
    }
    const Vector4Df v = Matrix4x4f(a).multiply(Vector4Df(0.3f, -1.1f, 2.5f, 1.0f));
    EXPECT_EQ(v.x(), a[0][0] * 0.3f + a[0][1] * -1.1f + a[0][2] * 2.5f + a[0][3] * 1.0f);
    EXPECT_EQ(v.w(), a[3][0] * 0.3f + a[3][1] * -1.1f + a[3][2] * 2.5f + a[3][3] * 1.0f);
}

TEST(Matrix4x4fTest, RotationProjectionAndInverse) {
    const Matrix4x4f r = Matrix4x4f().axisRotation((float)(M_PI / 2.0), 0, 0, 1);
    EXPECT_TRUE(r.multiply(Vector3Df(1, 0, 0)).epsilonEquals(Vector3Df(0, 1, 0), 1e-6f));
    EXPECT_TRUE(r.multiply(r.transpose()).epsilonEquals(Matrix4x4f(), 1e-6f));

    const Matrix4x4f o = Matrix4x4f().orthogonalProjection(-2, 2, -1, 1, 1, 10);
    const Matrix4x4d od = Matrix4x4d().orthogonalProjection(-2, 2, -1, 1, 1, 10);
    for ( int i = 0; i < 4; i++ ) {
        for ( int j = 0; j < 4; j++ ) {
            EXPECT_NEAR(o.get(i, j), od.get(i, j), 1e-6);
        }
    }

    const Matrix4x4f m = Matrix4x4f().axisRotation(0.3f, 1, 2, 3).multiply(Matrix4x4f().translation(1, -2, 0.5f));
    EXPECT_TRUE(m.multiply(m.invert()).epsilonEquals(Matrix4x4f(), 1e-5f));
    EXPECT_NEAR(m.determinant(), 1.0f, 1e-5f);
    EXPECT_TRUE(m.withoutTranslation().extractTranslation() == Vector3Df(0, 0, 0));
}

TEST(Matrix4x4fTest, QuaternionRoundTrip) {
    const Matrix4x4f r = Matrix4x4f().axisRotation(0.7f, 0.0f, 1.0f, 0.0f);
    EXPECT_TRUE(r.importFromQuaternion(r.exportToQuaternion()).epsilonEquals(r, 1e-5f));
}
