package vsdk.toolkit.environment.geometry.element;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector2Dd;
import vsdk.toolkit.common.linealAlgebra.Vector3Df;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
PlanarFace is exercised here with no material, solver element or application
class: a face is built from positions only. Port of the C++
planar_face_tests.cpp; its IsTriviallyCopyable test checks a C++ only type
property (std::is_trivially_copyable) and has no Java counterpart.
*/
class PlanarFaceTest
{
    private static PlanarFace triangle()
    {
        final Vector3Df[] p = {new Vector3Df(0, 0, 0), new Vector3Df(2, 0, 0), new Vector3Df(0, 2, 0)};
        return new PlanarFace(3, p);
    }

    private static PlanarFace square()
    {
        final Vector3Df[] p = {new Vector3Df(0, 0, 1), new Vector3Df(1, 0, 1), new Vector3Df(1, 1, 1), new Vector3Df(0, 1, 1)};
        return new PlanarFace(4, p);
    }

    // A trapezoid: an irregular quadrilateral, whose Jacobian is not constant
    private static PlanarFace trapezoid()
    {
        final Vector3Df[] p = {new Vector3Df(0, 0, 0), new Vector3Df(4, 0, 0), new Vector3Df(3, 2, 0), new Vector3Df(1, 2, 0)};
        return new PlanarFace(4, p);
    }

    private static void expectPoint(float x, float y, float z, Vector3Df p, float tolerance)
    {
        assertEquals(x, p.x(), tolerance);
        assertEquals(y, p.y(), tolerance);
        assertEquals(z, p.z(), tolerance);
    }

    // gtest's EXPECT_FLOAT_EQ: equal within 4 units in the last place
    private static void expectFloatEq(float expected, float actual)
    {
        assertEquals(expected, actual, 4.0F * Math.ulp(expected));
    }

    @Test
    void triangleGeometry()
    {
        final PlanarFace face = triangle();
        assertTrue(face.isValid());
        assertEquals(3, face.getNumberOfVertices());
        expectFloatEq(2.0F, face.getArea());
        expectPoint(0, 0, 1, face.getNormal(), 0.0F);
        expectFloatEq(0.0F, face.getPlaneConstant());
        assertEquals(2, face.getDominantAxis());
        assertFalse(face.hasJacobian());
        expectPoint(2.0F / 3.0F, 2.0F / 3.0F, 0, face.getMidPoint(), 1e-6F);
    }

    @Test
    void parallelogramHasNoJacobian()
    {
        final PlanarFace face = square();
        assertTrue(face.isValid());
        expectFloatEq(1.0F, face.getArea());
        assertFalse(face.hasJacobian());
        expectFloatEq(-1.0F, face.getPlaneConstant());
        expectFloatEq(0.0F, face.planeDistance(new Vector3Df(0.3F, 0.7F, 1.0F)));
    }

    @Test
    void irregularQuadrilateralAreaAndJacobian()
    {
        final PlanarFace face = trapezoid();
        assertTrue(face.isValid());
        expectFloatEq(6.0F, face.getArea());
        assertTrue(face.hasJacobian());
        expectFloatEq(face.getArea(), face.getJacobianA() + 0.5F * (face.getJacobianB() + face.getJacobianC()));
    }

    @Test
    void parametersInvertTheMapping()
    {
        final PlanarFace[] faces = {triangle(), square(), trapezoid()};
        for ( final PlanarFace face : faces ) {
            final Vector3Df p = face.pointAt(0.25, 0.375);
            final Vector2Dd uv = new Vector2Dd();
            assertTrue(face.parameters(p, uv));
            assertEquals(0.25, uv.x, 1e-5);
            assertEquals(0.375, uv.y, 1e-5);
        }
    }

    @Test
    void pointsOutsideAreReportedOutside()
    {
        final Vector2Dd uv = new Vector2Dd();
        assertFalse(triangle().parameters(new Vector3Df(1.5F, 1.5F, 0), uv));
        assertFalse(square().parameters(new Vector3Df(1.5F, 0.5F, 1), uv));
        assertFalse(trapezoid().parameters(new Vector3Df(0.2F, 1.8F, 0), uv));
    }

    @Test
    void uniformMappingPreservesArea()
    {
        final PlanarFace face = trapezoid();
        // The line of uniform u = 0.5 splits the area in two equal halves: by
        // symmetry of the trapezoid it is the vertical line x = 2
        final Vector3Df bottom = face.uniformPointAt(0.5, 0.0);
        final Vector3Df top = face.uniformPointAt(0.5, 1.0);
        assertEquals(2.0F, bottom.x(), 1e-5F);
        assertEquals(2.0F, top.x(), 1e-5F);

        // The line of uniform v = 0.5 leaves half of the area (3) under it: the
        // trapezoid of height h under it has area (4 + (4 - h)) h / 2 = 3
        final Vector3Df middle = face.uniformPointAt(0.5, 0.5);
        final float h = 4.0F - (float) Math.sqrt(10.0);
        assertEquals(h, middle.y(), 1e-5F);

        final Vector2Dd uv = new Vector2Dd();
        assertTrue(face.uniformParameters(face.uniformPointAt(0.3, 0.6), uv));
        assertEquals(0.3, uv.x, 1e-5);
        assertEquals(0.6, uv.y, 1e-5);
    }

    @Test
    void rayHit()
    {
        final PlanarFace face = square();
        final Vector3Df origin = new Vector3Df(0.5F, 0.25F, 3.0F);

        final float[] normalDotDirection = new float[1];
        assertEquals(PlanarFace.Side.FRONT, face.sideFacing(new Vector3Df(0, 0, -1), normalDotDirection));
        final float t = face.planeHitParameter(origin, normalDotDirection[0]);
        expectFloatEq(2.0F, t);

        final Vector2Dd uv = new Vector2Dd();
        assertTrue(face.parameters(new Vector3Df(0.5F, 0.25F, 1.0F), uv));
        assertEquals(0.5, uv.x, 1e-6);
        assertEquals(0.25, uv.y, 1e-6);

        assertEquals(PlanarFace.Side.BACK, face.sideFacing(new Vector3Df(0, 0, 1), normalDotDirection));
        assertEquals(PlanarFace.Side.PARALLEL, face.sideFacing(new Vector3Df(1, 0, 0), normalDotDirection));
    }

    @Test
    void rayHitInOneCall()
    {
        final PlanarFace face = square();
        final Vector3Df origin = new Vector3Df(0.5F, 0.25F, 3.0F);
        final Vector3Df down = new Vector3Df(0, 0, -1);

        final PlanarFace.RayHit hit = new PlanarFace.RayHit();
        assertTrue(face.rayHit(origin, down, true, false, 0.0F, 10.0F, hit));
        assertEquals(PlanarFace.Side.FRONT, hit.side);
        expectFloatEq(2.0F, hit.distance);
        expectFloatEq(0.5F, hit.pointX);
        expectFloatEq(0.25F, hit.pointY);
        expectFloatEq(1.0F, hit.pointZ);
        assertEquals(0.5, hit.u, 1e-6);
        assertEquals(0.25, hit.v, 1e-6);

        // Same answers as the separate calls
        final float[] normalDotDirection = new float[1];
        face.sideFacing(down, normalDotDirection);
        assertEquals(face.planeHitParameter(origin, normalDotDirection[0]), hit.distance);

        assertFalse(face.rayHit(origin, down, false, true, 0.0F, 10.0F, hit));     // Front side not accepted
        assertFalse(face.rayHit(origin, down, true, false, 0.0F, 1.5F, hit));      // Too far
        assertFalse(face.rayHit(origin, down, true, false, 2.5F, 10.0F, hit));     // Too close
        assertFalse(face.rayHit(origin, new Vector3Df(1, 0, 0), true, true, 0.0F, 10.0F, hit)); // Parallel
        assertFalse(face.rayHit(new Vector3Df(5.0F, 0.25F, 3.0F), down, true, false, 0.0F, 10.0F, hit)); // Outside

        assertTrue(face.rayHit(new Vector3Df(0.5F, 0.25F, -1.0F), new Vector3Df(0, 0, 1), false, true, 0.0F, 10.0F, hit));
        assertEquals(PlanarFace.Side.BACK, hit.side);
    }

    @Test
    void facing()
    {
        final Vector3Df[] floor = {new Vector3Df(0, 0, 0), new Vector3Df(1, 0, 0), new Vector3Df(1, 1, 0), new Vector3Df(0, 1, 0)};
        final Vector3Df[] ceiling = {new Vector3Df(0, 0, 1), new Vector3Df(0, 1, 1), new Vector3Df(1, 1, 1), new Vector3Df(1, 0, 1)};
        final Vector3Df[] above = {new Vector3Df(0, 0, 2), new Vector3Df(1, 0, 2), new Vector3Df(1, 1, 2), new Vector3Df(0, 1, 2)};
        final PlanarFace floorFace = new PlanarFace(4, floor);
        final PlanarFace ceilingFace = new PlanarFace(4, ceiling);
        final PlanarFace aboveFace = new PlanarFace(4, above);
        assertTrue(floorFace.facing(ceilingFace));
        assertFalse(floorFace.facing(aboveFace)); // Same orientation: one is behind the other
        assertFalse(floorFace.facing(floorFace));  // Coplanar
    }

    @Test
    void invalidFaces()
    {
        final Vector3Df[] collinear = {new Vector3Df(0, 0, 0), new Vector3Df(1, 0, 0), new Vector3Df(2, 0, 0)};
        assertEquals(PlanarFace.BuildStatus.DEGENERATE_NORMAL, new PlanarFace(3, collinear).getStatus());
        final Vector3Df[] pentagon = {new Vector3Df(), new Vector3Df(), new Vector3Df(), new Vector3Df(), new Vector3Df()};
        assertEquals(PlanarFace.BuildStatus.UNSUPPORTED_VERTEX_COUNT, new PlanarFace(5, pentagon).getStatus());
        assertFalse(new PlanarFace().isValid());
    }
}
