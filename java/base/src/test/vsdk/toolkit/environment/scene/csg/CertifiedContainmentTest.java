package vsdk.toolkit.environment.scene.csg;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.element.GeometryBounds;
import vsdk.toolkit.environment.geometry.element.GeometryContainment;
import vsdk.toolkit.environment.geometry.surface.TriangleMesh;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.geometry.volume.PlanarHalfSpace;
import vsdk.toolkit.environment.geometry.volume.QuadricSurface;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.scene.AffinePlacementStatus;
import vsdk.toolkit.environment.scene.SimpleBody;

import static vsdk.toolkit.GTestAssertions.expectEq;
import static vsdk.toolkit.GTestAssertions.expectFalse;
import static vsdk.toolkit.GTestAssertions.expectGe;
import static vsdk.toolkit.GTestAssertions.expectGt;
import static vsdk.toolkit.GTestAssertions.expectLe;
import static vsdk.toolkit.GTestAssertions.expectLt;
import static vsdk.toolkit.GTestAssertions.expectNe;
import static vsdk.toolkit.GTestAssertions.expectTrue;

/**
Port of `cpp/base/src/test/certified_containment_tests.cpp`; each test method is
named `<cpp suite>_<cpp test>`.
*/
class CertifiedContainmentTest
{

/*
Certified containment for the basic solid families (stage F-1). Expected
results come from independent closed-form distances; placed cases are also
checked for soundness by sampling the world tolerance ball in long double.
*/

    @Test
    void CertifiedContainmentContract_BoxUsesEuclideanDistanceToItsBoundary()
    {
    Box box = new Box(new Vector3Dd(-1,-1,-1), new Vector3Dd(1,1,1));
    expectEq(GeometryContainment.Inside, box.classifyPointCertified(new Vector3Dd(0,0,0), 0.5));
    expectEq(GeometryContainment.Boundary, box.classifyPointCertified(new Vector3Dd(0.95,0,0), 0.1));
    expectEq(GeometryContainment.Boundary, box.classifyPointCertified(new Vector3Dd(1.05,0,0), 0.1));
    expectEq(GeometryContainment.Outside, box.classifyPointCertified(new Vector3Dd(1.2,0,0), 0.1));
    // Corner region: distance sqrt(2)*0.06 = 0.0849 and sqrt(2)*0.08 = 0.1131.
    expectEq(GeometryContainment.Boundary, box.classifyPointCertified(new Vector3Dd(1.06,1.06,0), 0.1));
    expectEq(GeometryContainment.Outside, box.classifyPointCertified(new Vector3Dd(1.08,1.08,0), 0.1));
    // A point exactly on a face is a boundary at zero tolerance.
    expectEq(GeometryContainment.Boundary, box.classifyPointCertified(new Vector3Dd(1,0.5,0), 0));
    expectEq(GeometryContainment.Inside, box.classifyPointCertified(new Vector3Dd(0.5,0.5,0), 0));
    expectEq(GeometryContainment.InvalidInput, box.classifyPointCertified(new Vector3Dd(0,0,0), -1));
    // The plain query delegates to the same certificate for positive tolerance.
    expectEq(GeometryContainment.Outside, box.classifyPoint(new Vector3Dd(1.08,1.08,0), 0.1));
}

    @Test
    void CertifiedContainmentContract_HalfSpaceNormalizesItsCoefficients()
    {
    PlanarHalfSpace half = new PlanarHalfSpace(2,0,0,-2); // x <= 1
    expectEq(GeometryContainment.Inside, half.classifyPointCertified(new Vector3Dd(0.8,5,5), 0.1));
    expectEq(GeometryContainment.Boundary, half.classifyPointCertified(new Vector3Dd(1.05,0,0), 0.1));
    expectEq(GeometryContainment.Boundary, half.classifyPointCertified(new Vector3Dd(0.95,0,0), 0.1));
    expectEq(GeometryContainment.Outside, half.classifyPointCertified(new Vector3Dd(1.2,0,0), 0.1));
    PlanarHalfSpace oblique = new PlanarHalfSpace(1,1,0,0); // distance (x+y)/sqrt(2)
    expectEq(GeometryContainment.Boundary, oblique.classifyPointCertified(new Vector3Dd(0.1,0,0), 0.1));
    expectEq(GeometryContainment.Outside, oblique.classifyPointCertified(new Vector3Dd(0.1,0.1,0), 0.1));
    expectEq(GeometryContainment.InvalidInput, new PlanarHalfSpace(0,0,0,1).classifyPointCertified(new Vector3Dd(), 0.1));
}

    @Test
    void CertifiedContainmentContract_QuadricProvesSidesAndSignChangesOnly()
    {
    // x^2/4 + y^2 + z^2 <= 1: semi-axes 2, 1, 1.
    QuadricSurface ellipsoid = new QuadricSurface(new Vector3Dd(0.25,1,1), new Vector3Dd(), new Vector3Dd(), -1);
    expectEq(GeometryContainment.Inside, ellipsoid.classifyPointCertified(new Vector3Dd(), 0.5));
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPointCertified(new Vector3Dd(2.05,0,0), 0.1));
    expectEq(GeometryContainment.Boundary, ellipsoid.classifyPointCertified(new Vector3Dd(0,0.95,0), 0.1));
    expectEq(GeometryContainment.Outside, ellipsoid.classifyPointCertified(new Vector3Dd(2.3,0,0), 0.1));
    expectEq(GeometryContainment.Outside, ellipsoid.classifyPointCertified(new Vector3Dd(0,1.5,0), 0.1));
    // Zero tolerance: sides are proven by the sign of F; the surface itself is not.
    expectEq(GeometryContainment.Inside, ellipsoid.classifyPointCertified(new Vector3Dd(1.9,0,0), 0));
    expectEq(GeometryContainment.Indeterminate, ellipsoid.classifyPointCertified(new Vector3Dd(2,0,0), 0));
    // A saddle with a mixed term: xy <= 1.
    QuadricSurface saddle = new QuadricSurface(new Vector3Dd(), new Vector3Dd(1,0,0), new Vector3Dd(), -1);
    expectEq(GeometryContainment.Boundary, saddle.classifyPointCertified(new Vector3Dd(1,1,0), 0.01));
    expectEq(GeometryContainment.Inside, saddle.classifyPointCertified(new Vector3Dd(-1,1,0), 0.1));
    // A tangent-only contact (x^2 <= 0) has no sign change and is not claimed.
    QuadricSurface slab = new QuadricSurface(new Vector3Dd(1,0,0), new Vector3Dd(), new Vector3Dd(), 0);
    expectEq(GeometryContainment.Indeterminate, slab.classifyPointCertified(new Vector3Dd(0.01,0,0), 0.1));
    expectEq(GeometryContainment.Outside, slab.classifyPointCertified(new Vector3Dd(0.5,0,0), 0.1));
}

    // The cpp sampler evaluates in x87 `long double`; Java has no extended
    // type, so the signs are evaluated in binary64 (see the port report).
    private static class CertifiedContainmentSampler {
        // Signs of the local implicit description at ball samples: -1 inside, +1 outside.
        static double boxValue(final double[] p) {
            double value = -1e300;
            for (int i = 0; i < 3; ++i) {
                final double d = Math.abs(p[i]) - 1.0;
                if (d > value) value = d;
            }
            return value;
        }
        static double halfSpaceValue(final double[] p) { return p[0] + 2.0*p[1] - p[2] - 0.5; }
        static double ellipsoidValue(final double[] p) {
            return 0.25*p[0]*p[0] + p[1]*p[1] + p[2]*p[2] + 0.5*p[0]*p[1] - 1.0;
        }
        static double value(int family, final double[] p) {
            return family == 0 ? boxValue(p) : family == 1 ? halfSpaceValue(p) : ellipsoidValue(p);
        }
        static void local(final Matrix4x4d inverse, final Vector3Dd world, double[] out) {
            for (int i = 0; i < 3; ++i) {
                out[i] = inverse.get(i,3);
                out[i] += inverse.get(i,0)*world.x() + inverse.get(i,1)*world.y() +
                    inverse.get(i,2)*world.z();
            }
        }
        // Returns -1/+1 when every sample has that sign, 0 when both appear.
        static int sampleSign(int family, final Matrix4x4d inverse, final Vector3Dd centre, double radius) {
            boolean negative = false, positive = false;
            final int count = 96;
            for (int shell = 0; shell <= 2; ++shell) {
                final double r = radius * shell / 2.0;
                for (int k = 0; k < count; ++k) {
                    final double z = 1.0 - (2.0*k + 1.0)/count;
                    final double ring = Math.sqrt(1.0 - z*z);
                    final double angle = 2.399963229728653 * k;
                    final Vector3Dd q = centre.add(new Vector3Dd(ring*Math.cos(angle), ring*Math.sin(angle), z).multiply(r));
                    double[] p = new double[3];
                    local(inverse, q, p);
                    final double v = value(family, p);
                    if (v < 0) negative = true;
                    if (v > 0) positive = true;
                    if (shell == 0) break;
                }
            }
            return negative && positive ? 0 : negative ? -1 : 1;
        }
    }

    // Exact comparison of a binary64 bound with a sum of products, standing in
    // for the cpp `long double` accumulation: the sum is kept as an unevaluated
    // pair (hi, lo) built with error-free transformations. The product error
    // uses Dekker's TwoProduct with Veltkamp splitting (no fused multiply-add,
    // as the Java port rules require; exact for the finite operands used here).
    private static double twoProductError(double a, double b, double product) {
        final double split = 134217729.0; // 2^27 + 1
        final double ca = split*a, ah = ca - (ca - a), al = a - ah;
        final double cb = split*b, bh = cb - (cb - b), bl = b - bh;
        return ((ah*bh - product) + ah*bl + al*bh) + al*bl;
    }

    private static double[] exactAffineRow(Matrix4x4d placement, int row, double[] p) {
        double hi = placement.get(row,3), lo = 0;
        for (int column = 0; column < 3; ++column) {
            final double product = placement.get(row,column) * p[column];
            final double productError = twoProductError(placement.get(row,column), p[column], product);
            final double sum = hi + product;
            final double bp = sum - hi;
            final double sumError = (hi - (sum - bp)) + (product - bp);
            hi = sum; lo += sumError + productError;
            final double renormalized = hi + lo;
            lo = lo - (renormalized - hi);
            hi = renormalized;
        }
        return new double[] {hi, lo};
    }

    private static boolean boundBelow(double bound, double[] value)
    { return bound < value[0] || (bound == value[0] && value[1] >= 0); }

    private static boolean boundAbove(double bound, double[] value)
    { return bound > value[0] || (bound == value[0] && value[1] <= 0); }

    @Test
    void CertifiedContainmentContract_PlacedFamiliesAreSoundUnderShearReflectionAndRotation()
    {
    Box box = new Box(new Vector3Dd(-1,-1,-1), new Vector3Dd(1,1,1));
    PlanarHalfSpace half = new PlanarHalfSpace(1,2,-1,-0.5);
    QuadricSurface ellipsoid = new QuadricSurface(new Vector3Dd(0.25,1,1), new Vector3Dd(0.5,0,0), new Vector3Dd(), -1);
    final Geometry[] geometries = {box, half, ellipsoid};
    final Matrix4x4d[] placements = {
        new Matrix4x4d().translation(1,-2,0.5).multiply(new Matrix4x4d().scale(3,0.5,1.5)),
        new Matrix4x4d().translation(-1,0,2).multiply(Matrix4x4d.identityMatrix().withVal(0,1,0.75)
            .multiply(new Matrix4x4d().scale(-1,2,1))),
        new Matrix4x4d().axisRotation(0.7, 1, 2, 3).multiply(new Matrix4x4d().scale(2,1,0.25))
    };
    int definite = 0, total = 0;
    for (int family = 0; family < 3; ++family) {
        for (int placement = 0; placement < 3; ++placement) {
            SimpleBody body = new SimpleBody();
            body.setGeometryReference(geometries[family]);
            final Matrix4x4d inverse = placements[placement].inverse();
            expectEq(AffinePlacementStatus.Valid, body.setAffinePlacement(placements[placement], inverse));
            for (int step = -40; step <= 40; ++step) {
                final Vector3Dd local = new Vector3Dd(0.05*step, 0.3, -0.2);
                final Vector3Dd world = placements[placement].multiply(local);
                for (int t = 0; t < 2; ++t) {
                    final double tolerance = t == 0 ? 0.05 : 0.2;
                    final GeometryContainment result = body.classifyPointCertified(world, tolerance);
                    ++total;
                    if (result == GeometryContainment.Indeterminate) continue;
                    expectNe(GeometryContainment.Unsupported, result);
                    expectNe(GeometryContainment.InvalidInput, result);
                    ++definite;
                    final int sign = CertifiedContainmentSampler.sampleSign(family, inverse, world, tolerance);
                    if (result == GeometryContainment.Inside) expectEq(-1, sign, "" + family + " " + placement + " " + step);
                    if (result == GeometryContainment.Outside) expectEq(1, sign, "" + family + " " + placement + " " + step);
                }
            }
        }
    }
    // The certificates must decide most of the sweep, not merely stay sound.
    expectGt(definite, total * 3 / 4);
}

    @Test
    void CertifiedContainmentContract_AnisotropicBoxUsesTheTightAxisEnclosure()
    {
    Box box = new Box(2,2,2); // [-1,1]^3
    SimpleBody body = new SimpleBody();
    body.setGeometryReference(box);
    body.setPosition(new Vector3Dd(10,0,0));
    body.setScale(new Vector3Dd(-2,3,1)); // world x in [8,12], y in [-3,3]
    // World distance 0.15 from the x face. The enclosing local ball would have
    // radius about 0.117 against a local distance 0.075; the axis box is exact.
    expectEq(GeometryContainment.Outside, body.classifyPointCertified(new Vector3Dd(7.85,0,0), 0.1));
    expectEq(GeometryContainment.Inside, body.classifyPointCertified(new Vector3Dd(8.15,0,0), 0.1));
    expectEq(GeometryContainment.Boundary, body.classifyPointCertified(new Vector3Dd(8.05,0,0), 0.1));
    // The inner local ball has radius tolerance/3 = 0.02 against a local face
    // distance 0.025; two certified world samples still prove the boundary.
    expectEq(GeometryContainment.Boundary, body.classifyPointCertified(new Vector3Dd(12.05,0,0), 0.06));
    expectEq(GeometryContainment.Outside, body.classifyPointCertified(new Vector3Dd(12.07,0,0), 0.06));
}

    @Test
    void CertifiedContainmentContract_CsgLeavesOfEveryBasicFamilyAreCertified()
    {
    SimpleBody block = new SimpleBody();
    SimpleBody cut = new SimpleBody();
    SimpleBody rounded = new SimpleBody();
    block.setGeometry(new Box(new Vector3Dd(-1,-1,-1), new Vector3Dd(1,1,1)));
    cut.setGeometry(new PlanarHalfSpace(0,0,-1,0)); // z >= 0
    rounded.setGeometry(new QuadricSurface(new Vector3Dd(1,1,1), new Vector3Dd(), new Vector3Dd(), -2.25));
    rounded.setScale(new Vector3Dd(1,1,0.5));
    CsgBodyOperand a = new CsgBodyOperand(block);
    CsgBodyOperand b = new CsgBodyOperand(cut);
    CsgBodyOperand c = new CsgBodyOperand(rounded);
    CsgTree root = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(root.addOperand(a)); expectTrue(root.addOperand(b));
    expectEq(GeometryContainment.Inside, CsgPointClassification.classify(root, new Vector3Dd(0,0,-0.5), 0.1));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classify(root, new Vector3Dd(0,0,0.5), 0.1));
    CsgPointWitness witness = new CsgPointWitness();
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classifyWithWitness(root, new Vector3Dd(0,0,0.02), 0.05, witness));
    expectEq(1, witness.getLength());
    expectEq(b, witness.getOperand(0));
    expectTrue(witness.isReversed());
    CsgTree both = new CsgTree(CsgOperation.INTERSECTION);
    expectTrue(both.addOperand(a)); expectTrue(both.addOperand(c));
    // The scaled quadric reaches z = 0.75 on the axis; the box reaches z = 1.
    expectEq(GeometryContainment.Boundary, CsgPointClassification.classify(both, new Vector3Dd(0,0,0.76), 0.05));
    expectEq(GeometryContainment.Outside, CsgPointClassification.classify(both, new Vector3Dd(0,0,0.9), 0.05));
    expectEq(GeometryContainment.Inside, CsgPointClassification.classify(both, new Vector3Dd(0,0,0.5), 0.05));
}


    @Test
    void ConservativeBoundsContract_PlacedBoundsEncloseExactCornersAndStayExactWhenRepresentable()
    {
    GeometryBounds local = new GeometryBounds();
    expectTrue(local.setFinite(new Vector3Dd(-0.1,-0.3,0.7), new Vector3Dd(0.2,0.9,1.3)));
    final Matrix4x4d placement = new Matrix4x4d().translation(0.1,-0.7,0.3).multiply(
        Matrix4x4d.identityMatrix().withVal(0,1,0.3).withVal(1,2,-0.7).withVal(2,0,0.1));
    final GeometryBounds placed = local.transformed(placement);
    expectEq(GeometryBounds.Kind.Finite, placed.getKind());
    final double[] lo = {placed.getMinimum().x(), placed.getMinimum().y(), placed.getMinimum().z()};
    final double[] hi = {placed.getMaximum().x(), placed.getMaximum().y(), placed.getMaximum().z()};
    for (int corner = 0; corner < 8; ++corner) {
        final double[] p = {(corner & 1)!=0 ? 0.2 : -0.1, (corner & 2)!=0 ? 0.9 : -0.3, (corner & 4)!=0 ? 1.3 : 0.7};
        for (int row = 0; row < 3; ++row) {
            final double[] value = exactAffineRow(placement, row, p);
            expectTrue(boundBelow(lo[row], value));
            expectTrue(boundAbove(hi[row], value));
        }
    }
    GeometryBounds unit = new GeometryBounds();
    expectTrue(unit.setFinite(new Vector3Dd(-1,-1,-1), new Vector3Dd(1,1,1)));
    final GeometryBounds shifted = unit.transformed(new Matrix4x4d().translation(10,0,0).multiply(new Matrix4x4d().scale(2,3,1)));
    expectEq(8, shifted.getMinimum().x()); expectEq(12, shifted.getMaximum().x());
    expectEq(-3, shifted.getMinimum().y()); expectEq(3, shifted.getMaximum().y());
}

    @Test
    void ConservativeBoundsContract_IntersectionInflationAndBallExclusionStayConservative()
    {
    GeometryBounds a = new GeometryBounds();
    GeometryBounds b = new GeometryBounds();
    GeometryBounds unknown = new GeometryBounds();
    GeometryBounds unbounded = new GeometryBounds();
    GeometryBounds empty = new GeometryBounds();
    expectTrue(a.setFinite(new Vector3Dd(0,0,0), new Vector3Dd(2,2,2)));
    expectTrue(b.setFinite(new Vector3Dd(1,1,1), new Vector3Dd(3,3,3)));
    unbounded.setUnbounded(); empty.setEmpty();
    final GeometryBounds both = a.intersecting(b);
    expectEq(1, both.getMinimum().x()); expectEq(2, both.getMaximum().x());
    expectEq(GeometryBounds.Kind.Finite, a.intersecting(unknown).getKind());
    expectEq(GeometryBounds.Kind.Finite, unbounded.intersecting(a).getKind());
    expectEq(GeometryBounds.Kind.Unsupported, unknown.intersecting(unknown).getKind());
    GeometryBounds far = new GeometryBounds(); expectTrue(far.setFinite(new Vector3Dd(5,5,5), new Vector3Dd(6,6,6)));
    expectEq(GeometryBounds.Kind.Empty, a.intersecting(far).getKind());
    final GeometryBounds grown = a.inflated(0.1);
    expectLe(grown.getMinimum().x(), -0.1); expectGe(grown.getMaximum().x(), 2.1);
    expectTrue(a.excludesBall(new Vector3Dd(2.2,1,1), 0.1));
    expectFalse(a.excludesBall(new Vector3Dd(2.05,1,1), 0.1));
    expectFalse(a.excludesBall(new Vector3Dd(1,1,1), 0.1));
    // Corner distance sqrt(2)*0.08 = 0.113 exceeds 0.1; per-axis checks would not.
    expectTrue(a.excludesBall(new Vector3Dd(2.08,2.08,1), 0.1));
    expectTrue(empty.excludesBall(new Vector3Dd(), 1));
    expectFalse(unbounded.excludesBall(new Vector3Dd(100,0,0), 0.1));
    expectFalse(unknown.excludesBall(new Vector3Dd(100,0,0), 0.1));
}

    @Test
    void ConservativeBoundsContract_CsgBoundsFollowBooleanOperationsAndPlacement()
    {
    SimpleBody left = new SimpleBody();
    SimpleBody right = new SimpleBody();
    SimpleBody plane = new SimpleBody();
    SimpleBody mesh = new SimpleBody();
    left.setGeometry(new Sphere(1));
    right.setGeometry(new Sphere(1)); right.setPosition(new Vector3Dd(1.5,0,0));
    plane.setGeometry(new PlanarHalfSpace(1,0,0,0));
    mesh.setGeometry(new TriangleMesh());
    CsgBodyOperand a = new CsgBodyOperand(left);
    CsgBodyOperand b = new CsgBodyOperand(right);
    CsgBodyOperand c = new CsgBodyOperand(plane);
    CsgBodyOperand m = new CsgBodyOperand(mesh);
    GeometryBounds bounds = new GeometryBounds();
    CsgTree join = new CsgTree(CsgOperation.UNION);
    CsgTree meet = new CsgTree(CsgOperation.INTERSECTION);
    CsgTree cut = new CsgTree(CsgOperation.DIFFERENCE);
    expectTrue(join.addOperand(a)); expectTrue(join.addOperand(b));
    expectTrue(meet.addOperand(a)); expectTrue(meet.addOperand(b)); expectTrue(meet.addOperand(c));
    expectTrue(cut.addOperand(b)); expectTrue(cut.addOperand(c));
    CsgSolidBounds.query(join, bounds);
    expectEq(GeometryBounds.Kind.Finite, bounds.getKind());
    expectLe(bounds.getMinimum().x(), -1); expectGe(bounds.getMaximum().x(), 2.5);
    CsgSolidBounds.query(meet, bounds);
    expectEq(GeometryBounds.Kind.Finite, bounds.getKind());
    expectLe(bounds.getMinimum().x(), 0.5); expectGe(bounds.getMaximum().x(), 1);
    expectLt(bounds.getMaximum().x(), 1.01);
    CsgSolidBounds.query(cut, bounds);
    expectLe(bounds.getMinimum().x(), 0.5); expectGe(bounds.getMaximum().x(), 2.5);
    CsgPlacedExpression placed = new CsgPlacedExpression(join);
    final Matrix4x4d forward = new Matrix4x4d().translation(0,10,0).multiply(new Matrix4x4d().scale(1,2,1));
    expectEq(AffinePlacementStatus.Valid, placed.setAffinePlacement(forward, forward.inverse()));
    CsgTree root = new CsgTree(CsgOperation.UNION); expectTrue(root.addOperand(placed));
    CsgSolidBounds.query(root, bounds);
    expectLe(bounds.getMinimum().y(), 8); expectGe(bounds.getMaximum().y(), 12);
    // A surface has no solid bounds; a union with it stays unknown.
    CsgTree withSurface = new CsgTree(CsgOperation.UNION);
    expectTrue(withSurface.addOperand(a)); expectTrue(withSurface.addOperand(m));
    CsgSolidBounds.query(withSurface, bounds);
    expectEq(GeometryBounds.Kind.Unsupported, bounds.getKind());
}

    @Test
    void ConservativeBoundsContract_LeafCullingDecidesOnlyProvenDisjointSolids()
    {
    SimpleBody cone = new SimpleBody();
    SimpleBody mesh = new SimpleBody();
    cone.setGeometry(new Cone(1,1,1));
    mesh.setGeometry(new TriangleMesh());
    CsgBodyOperand c = new CsgBodyOperand(cone);
    CsgBodyOperand m = new CsgBodyOperand(mesh);
    CsgTree solid = new CsgTree(CsgOperation.UNION);
    CsgTree surface = new CsgTree(CsgOperation.UNION);
    expectTrue(solid.addOperand(c)); expectTrue(surface.addOperand(m));
    // The cone has no certified containment, but its certified bounds prove
    // that a distant ball is outside.
    expectEq(GeometryContainment.Outside, CsgPointClassification.classify(solid, new Vector3Dd(10,0,0), 0.1));
    expectEq(GeometryContainment.Unsupported, CsgPointClassification.classify(solid, new Vector3Dd(0,0,0.5), 0.1));
    // An open surface never acquires an exterior from its bounds.
    expectEq(GeometryContainment.Unsupported, CsgPointClassification.classify(surface, new Vector3Dd(10,0,0), 0.1));
}
}
