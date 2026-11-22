package vsdk.toolkit.common.linealAlgebra;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.io.wrapper.Matrix2x2Printer;

import static vsdk.toolkit.GTestAssertions.*;

/**
Port of `cpp/base/src/test/matrix2x2_tests.cpp`; each test method is named
`<cpp suite>_<cpp test>`.
*/
class Matrix2x2Test
{
    @Test
    void Matrix2x2Test_IdentityAndAccessors()
    {
        final Matrix2x2 identity = new Matrix2x2();
        expectEq(identity.get(0, 0), 1.0f);
        expectEq(identity.get(0, 1), 0.0f);
        expectEq(identity.get(1, 0), 0.0f);
        expectEq(identity.get(1, 1), 1.0f);
        expectEq(identity.getTranslation(0), 0.0f);
        final Matrix2x2 m = new Matrix2x2(0.5f, 0.25f, -1.0f, 2.0f, 3.0f, -4.0f);
        expectEq(m.get(1, 0), -1.0f);
        expectEq(m.getTranslation(1), -4.0f);
    }

    @Test
    void Matrix2x2Test_TransformPoint()
    {
        final Matrix2x2 m = new Matrix2x2(0.5f, 0.0f, 0.0f, 0.5f, 0.5f, 0.0f);
        final Vector2Df p = m.transformPoint(new Vector2Df(1.0f, 1.0f));
        expectEq(p.x(), 1.0f);
        expectEq(p.y(), 0.5f);
        final Matrix2x2 g = new Matrix2x2(0.3f, 0.7f, 1.1f, 1.3f, 0.2f, 0.9f);
        final Vector2Df q = g.transformPoint(new Vector2Df(0.6f, 0.1f));
        expectEq(q.x(), 0.3f * 0.6f + 0.7f * 0.1f + 0.2f);
        expectEq(q.y(), 1.1f * 0.6f + 1.3f * 0.1f + 0.9f);
    }

    @Test
    void Matrix2x2Test_MultiplyComposes()
    {
        final Matrix2x2 a = new Matrix2x2(0.5f, 0.0f, 0.0f, 0.5f, 0.5f, 0.0f);
        final Matrix2x2 b = new Matrix2x2(0.5f, 0.0f, 0.0f, 0.5f, 0.0f, 0.5f);
        final Vector2Df p = new Vector2Df(0.3f, 0.6f);
        final Vector2Df composed = a.multiply(b).transformPoint(p);
        final Vector2Df stepwise = a.transformPoint(b.transformPoint(p));
        expectEq(composed.x(), stepwise.x());
        expectEq(composed.y(), stepwise.y());
        expectTrue(new Matrix2x2().multiply(a).equals(a));
    }

    @Test
    void Matrix2x2PrinterTest_NullStreamIsIgnored()
    {
        Matrix2x2Printer.print(new Matrix2x2(), null);
    }
}
