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
        expectEq(identity.get(0, 0), 1.0);
        expectEq(identity.get(0, 1), 0.0);
        expectEq(identity.get(1, 0), 0.0);
        expectEq(identity.get(1, 1), 1.0);
        expectEq(identity.getTranslation(0), 0.0);
        final Matrix2x2 m = new Matrix2x2(0.5, 0.25, -1.0, 2.0, 3.0, -4.0);
        expectEq(m.get(1, 0), -1.0);
        expectEq(m.getTranslation(1), -4.0);
    }

    @Test
    void Matrix2x2Test_TransformPoint()
    {
        final Matrix2x2 m = new Matrix2x2(0.5, 0.0, 0.0, 0.5, 0.5, 0.0);
        final Vector2Dd p = m.transformPoint(new Vector2Dd(1.0, 1.0));
        expectEq(p.x(), 1.0);
        expectEq(p.y(), 0.5);
        final Matrix2x2 g = new Matrix2x2(0.3, 0.7, 1.1, 1.3, 0.2, 0.9);
        final Vector2Dd q = g.transformPoint(new Vector2Dd(0.6, 0.1));
        expectEq(q.x(), 0.3 * 0.6 + 0.7 * 0.1 + 0.2);
        expectEq(q.y(), 1.1 * 0.6 + 1.3 * 0.1 + 0.9);
    }

    @Test
    void Matrix2x2Test_MultiplyComposes()
    {
        final Matrix2x2 a = new Matrix2x2(0.5, 0.0, 0.0, 0.5, 0.5, 0.0);
        final Matrix2x2 b = new Matrix2x2(0.5, 0.0, 0.0, 0.5, 0.0, 0.5);
        final Vector2Dd p = new Vector2Dd(0.3, 0.6);
        final Vector2Dd composed = a.multiply(b).transformPoint(p);
        final Vector2Dd stepwise = a.transformPoint(b.transformPoint(p));
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
