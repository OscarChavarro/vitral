package vsdk.toolkit.environment.geometry.volume;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.RayCrossingTestCollector;
import vsdk.toolkit.environment.geometry.element.GeometryBounds;
import vsdk.toolkit.environment.geometry.element.GeometryContainment;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayBoundary;
import vsdk.toolkit.environment.geometry.element.RayCrossingKind;
import vsdk.toolkit.environment.geometry.element.RayCrossingQuery;
import vsdk.toolkit.environment.geometry.element.RayCrossingSupport;
import vsdk.toolkit.environment.geometry.element.RayFirstHitStatus;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.geometry.element.RaySolidClassification;
import vsdk.toolkit.environment.geometry.element.RaySolidQueryStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static vsdk.toolkit.GTestAssertions.expectNear;

/**
Port of `cpp/base/src/test/implicit_solid_tests.cpp`; each test method is
named `<cpp suite>_<cpp test>`.
*/
class ImplicitSolidTest
{
    @Test
    void PolynomialSurfaceContract_TermIndicesFollowPovRayOrderAndRoundTrip()
    {
        assertThat(PolynomialSurface.getTermIndex(4, 4, 0, 0)).isEqualTo(0);
        assertThat(PolynomialSurface.getTermIndex(4, 1, 3, 0)).isEqualTo(10);
        assertThat(PolynomialSurface.getTermIndex(4, 0, 0, 0)).isEqualTo(34);
        assertThat(PolynomialSurface.getTermIndex(2, 1, 1, 1)).isEqualTo(-1);
        assertThat(PolynomialSurface.getTermIndex(3, -1, 0, 0)).isEqualTo(-1);
        for ( int order = PolynomialSurface.MINIMUM_ORDER; order <= PolynomialSurface.MAXIMUM_ORDER; ++order ) {
            for ( int i = 0; i < PolynomialSurface.getTermCount(order); ++i ) {
                int[] x = {0}, y = {0}, z = {0}, w = {0};
                PolynomialSurface.getTermPowers(order, i, x, y, z, w);
                assertThat(x[0] + y[0] + z[0] + w[0]).isEqualTo(order);
                assertThat(PolynomialSurface.getTermIndex(order, x[0], y[0], z[0])).isEqualTo(i);
            }
        }
        assertThatThrownBy(() -> new PolynomialSurface(1, false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PolynomialSurface(8, false)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void PolynomialSurfaceContract_RayPolynomialsReproduceTheFieldAlongTheRay()
    {
        PolynomialSurface quartic = new PolynomialSurface(4, false);
        for ( int i = 0; i < 35; ++i ) quartic.getCoefficients()[i] = 0.25*((i*7)%11) - 1.0;
        final Vector3Dd origin = new Vector3Dd(0.3, -0.7, 1.1), direction = new Vector3Dd(0.48, 0.6, 0.64);
        double[] special = new double[5], general = new double[5];
        quartic.getQuarticRayPolynomial(origin, direction, special);
        quartic.getRayPolynomial(origin, direction, general, 0.0);
        for ( double t = -2; t <= 2; t += 0.5 ) {
            final double expected = quartic.evaluate(origin.add(direction.multiply(t)));
            expectNear(ImplicitSolidTestFactory.evaluateUnivariate(special, 4, t), expected, 1e-9*(1 + Math.abs(expected)));
            expectNear(ImplicitSolidTestFactory.evaluateUnivariate(general, 4, t), expected, 1e-9*(1 + Math.abs(expected)));
        }
    }

    @Test
    void PolynomialSurfaceContract_TorusCrossingsClassificationAndDetail()
    {
        for ( int sturm = 0; sturm < 2; ++sturm ) {
            final PolynomialSurface solid = ImplicitSolidTestFactory.createTorus(sturm != 0);
            final Ray ray = Ray.fromAffine(new Vector3Dd(-5, 0, 0), new Vector3Dd(2, 0, 0));
            final RayCrossingQuery query = new RayCrossingQuery(0, Double.POSITIVE_INFINITY);
            RayCrossingTestCollector crossings = new RayCrossingTestCollector();
            assertThat(solid.emitRayCrossings(ray, query, crossings)).isEqualTo(RayCrossingSupport.Supported);
            assertThat(crossings.roots).hasSize(4);
            final double[] expected = {1.25, 1.75, 3.25, 3.75};
            for ( int i = 0; i < 4; ++i ) {
                expectNear(crossings.roots.get(i), expected[i], 1e-9);
                assertThat(crossings.kinds.get(i))
                    .isEqualTo(i % 2 == 0 ? RayCrossingKind.Entering : RayCrossingKind.Leaving);
            }
            RaySolidClassification classification = new RaySolidClassification();
            assertThat(solid.classifyRay(ray, query, classification)).isEqualTo(RaySolidQueryStatus.Supported);
            assertThat(classification.boundaries).hasSize(4);
            assertThat(classification.insideSegments).hasSize(5);
            for ( int i = 0; i < 5; ++i ) assertThat(classification.insideSegments.get(i)).isEqualTo(i % 2 == 1);

            RayHit hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
            RayBoundary boundary = new RayBoundary();
            assertThat(solid.queryFirstHit(ray, query, hit, boundary)).isEqualTo(RayFirstHitStatus.Hit);
            expectNear(boundary.t, 1.25, 1e-9);
            expectNear(hit.point.x(), -2.5, 1e-9);
            expectNear(hit.normal.x(), -1, 1e-9);
            assertThat(solid.queryFirstHit(new Ray(new Vector3Dd(0, 0, 5), new Vector3Dd(0, 0, -1)), query, null))
                .isEqualTo(RayFirstHitStatus.Miss);
        }
    }

    @Test
    void PolynomialSurfaceContract_CertifiedContainmentAndUnboundedBounds()
    {
        final PolynomialSurface solid = ImplicitSolidTestFactory.createTorus(false);
        assertThat(solid.classifyPointCertified(new Vector3Dd(2, 0, 0), 0.1)).isEqualTo(GeometryContainment.Inside);
        assertThat(solid.classifyPointCertified(new Vector3Dd(0, 0, 0), 0.1)).isEqualTo(GeometryContainment.Outside);
        assertThat(solid.classifyPointCertified(new Vector3Dd(2.5, 0, 0), 0.01)).isEqualTo(GeometryContainment.Boundary);
        assertThat(solid.classifyPoint(new Vector3Dd(0, 2, 0.2), 0)).isEqualTo(GeometryContainment.Inside);
        assertThat(solid.classifyBoxCertified(new Vector3Dd(0, 0, 3), new Vector3Dd(0.5, 0.5, 0.5)))
            .isEqualTo(GeometryContainment.Outside);
        GeometryBounds bounds = new GeometryBounds();
        solid.getBounds(bounds);
        assertThat(bounds.getKind()).isEqualTo(GeometryBounds.Kind.Unbounded);
    }

    @Test
    void PolynomialSurfaceContract_SexticUsesTheGeneralExpansion()
    {
        // |p|^6 - 1: the unit sphere written as an order 6 polynomial.
        PolynomialSurface solid = new PolynomialSurface(6, false);
        double[] a = solid.getCoefficients();
        a[PolynomialSurface.getTermIndex(6, 6, 0, 0)] = 1;
        a[PolynomialSurface.getTermIndex(6, 0, 6, 0)] = 1;
        a[PolynomialSurface.getTermIndex(6, 0, 0, 6)] = 1;
        a[PolynomialSurface.getTermIndex(6, 4, 2, 0)] = 3;
        a[PolynomialSurface.getTermIndex(6, 4, 0, 2)] = 3;
        a[PolynomialSurface.getTermIndex(6, 2, 4, 0)] = 3;
        a[PolynomialSurface.getTermIndex(6, 0, 4, 2)] = 3;
        a[PolynomialSurface.getTermIndex(6, 2, 0, 4)] = 3;
        a[PolynomialSurface.getTermIndex(6, 0, 2, 4)] = 3;
        a[PolynomialSurface.getTermIndex(6, 2, 2, 2)] = 6;
        a[PolynomialSurface.getTermIndex(6, 0, 0, 0)] = -1;
        RaySolidClassification classification = new RaySolidClassification();
        assertThat(solid.classifyRay(new Ray(new Vector3Dd(-3, 0.6, 0), new Vector3Dd(1, 0, 0)),
            new RayCrossingQuery(0, 10), classification)).isEqualTo(RaySolidQueryStatus.Supported);
        assertThat(classification.boundaries).hasSize(2);
        expectNear(classification.boundaries.get(0).t, 2.2, 1e-8);
        expectNear(classification.boundaries.get(1).t, 3.8, 1e-8);
        assertThat(solid.classifyRay(new Ray(new Vector3Dd(-3, 0, 0), new Vector3Dd(1, 0, 0)),
            new RayCrossingQuery(Double.NEGATIVE_INFINITY, 10), classification))
            .isEqualTo(RaySolidQueryStatus.Unsupported);
    }

    @Test
    void BlobSolidContract_SingleComponentIsTheThresholdSphere()
    {
        // (1 - d^2/4)^2 > 0.5 inside d^2 < 4 (1 - sqrt(0.5)).
        final double radius = Math.sqrt(4.0*(1.0 - Math.sqrt(0.5)));
        for ( int sturm = 0; sturm < 2; ++sturm ) {
            BlobSolid blob = new BlobSolid(0.5, sturm != 0);
            blob.addComponent(new BlobComponent(new Vector3Dd(0, 0, 0), 2.0, 1.0));
            final Ray ray = Ray.fromAffine(new Vector3Dd(-5, 0, 0), new Vector3Dd(1, 0, 0));
            RaySolidClassification classification = new RaySolidClassification();
            assertThat(blob.classifyRay(ray, new RayCrossingQuery(0, 20), classification)).isEqualTo(RaySolidQueryStatus.Supported);
            assertThat(classification.boundaries).hasSize(2);
            expectNear(classification.boundaries.get(0).t, 5 - radius, 1e-9);
            expectNear(classification.boundaries.get(1).t, 5 + radius, 1e-9);
            assertThat(classification.insideSegments.get(0)).isFalse();
            assertThat(classification.insideSegments.get(1)).isTrue();
            RayHit hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
            assertThat(blob.doIntersectionFirstHit(ray, hit)).isTrue();
            expectNear(hit.normal.x(), -1, 1e-12);
            assertThat(blob.classifyPointCertified(new Vector3Dd(0, 0, 0), 0.1)).isEqualTo(GeometryContainment.Inside);
            assertThat(blob.classifyPointCertified(new Vector3Dd(3, 0, 0), 0.5)).isEqualTo(GeometryContainment.Outside);
            assertThat(blob.classifyPointCertified(new Vector3Dd(0, radius, 0), 0.01)).isEqualTo(GeometryContainment.Boundary);
        }
    }

    @Test
    void BlobSolidContract_OverlappingComponentsMergeAndBoundsFollowStrengthSigns()
    {
        BlobSolid blob = new BlobSolid(0.6, false);
        blob.addComponent(new BlobComponent(new Vector3Dd(-0.8, 0, 0), 1.5, 1.0));
        blob.addComponent(new BlobComponent(new Vector3Dd(0.8, 0, 0), 1.5, 1.0));
        RayCrossingTestCollector crossings = new RayCrossingTestCollector();
        assertThat(blob.emitRayCrossings(Ray.fromAffine(new Vector3Dd(-5, 0, 0), new Vector3Dd(1, 0, 0)),
            new RayCrossingQuery(0, Double.POSITIVE_INFINITY), crossings)).isEqualTo(RayCrossingSupport.Supported);
        assertThat(crossings.roots).hasSize(2);
        assertThat(crossings.kinds.get(0)).isEqualTo(RayCrossingKind.Entering);
        assertThat(crossings.kinds.get(1)).isEqualTo(RayCrossingKind.Leaving);
        assertThat(blob.classifyPoint(new Vector3Dd(0, 0, 0), 0)).isEqualTo(GeometryContainment.Inside);
        GeometryBounds bounds = new GeometryBounds();
        blob.getBounds(bounds);
        assertThat(bounds.getKind()).isEqualTo(GeometryBounds.Kind.Finite);
        assertThat(bounds.getMinimum().x()).isLessThanOrEqualTo(-2.3);
        assertThat(bounds.getMaximum().x()).isGreaterThanOrEqualTo(2.3);
        assertThat(bounds.getMaximum().x()).isLessThanOrEqualTo(2.3 + 1e-12);
        blob.addComponent(new BlobComponent(new Vector3Dd(10, 0, 0), 1.0, -1.0));
        blob.getBounds(bounds);
        assertThat(bounds.getMaximum().x()).isLessThanOrEqualTo(2.3 + 1e-12);
        BlobSolid everywhere = new BlobSolid(-0.1, false);
        everywhere.addComponent(new BlobComponent(new Vector3Dd(0, 0, 0), 1.0, 1.0));
        everywhere.getBounds(bounds);
        assertThat(bounds.getKind()).isEqualTo(GeometryBounds.Kind.Unbounded);
        assertThat(new BlobComponent(new Vector3Dd(0, 0, 0), 1e-6, 1.0).isDegenerate()).isTrue();
    }
}
