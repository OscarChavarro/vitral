package vsdk.toolkit.environment.geometry;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
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
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.PlanarHalfSpace;
import vsdk.toolkit.environment.geometry.volume.Sphere;
import vsdk.toolkit.environment.geometry.volume.Torus;

import static org.assertj.core.api.Assertions.assertThat;
import static vsdk.toolkit.GTestAssertions.expectDoubleEq;

/**
Port of `cpp/base/src/test/ray_crossing_tests.cpp`; each test method is named
`<cpp suite>_<cpp test>`. cpp `Ray` setters become the `with*` copies of the
immutable Java `Ray`.
*/
class RayCrossingTest
{
    @Test
    void RayCrossings_SpherePreservesAffineParameterAndOrdersRoots()
    {
        Sphere sphere = new Sphere(1);
        Ray ray = Ray.fromAffine(new Vector3Dd(-3,0,0), new Vector3Dd(2,0,0));
        RayCrossingTestCollector hits = new RayCrossingTestCollector();
        assertThat(sphere.emitRayCrossings(ray, new RayCrossingQuery(0,10), hits))
            .isEqualTo(RayCrossingSupport.Supported);
        assertThat(hits.roots).hasSize(2);
        expectDoubleEq(1, hits.roots.get(0));
        expectDoubleEq(2, hits.roots.get(1));
        assertThat(hits.geometries.get(0)).isSameAs(sphere);
        assertThat(hits.kinds.get(0)).isEqualTo(RayCrossingKind.Entering);
        assertThat(hits.kinds.get(1)).isEqualTo(RayCrossingKind.Leaving);
    }

    @Test
    void RayCrossings_InteriorTangentAndClippedIntervals()
    {
        Sphere sphere = new Sphere(1);
        RayCrossingTestCollector inside = new RayCrossingTestCollector(),
            tangent = new RayCrossingTestCollector(),
            clipped = new RayCrossingTestCollector(),
            zero = new RayCrossingTestCollector();
        sphere.emitRayCrossings(new Ray(new Vector3Dd(0,0,0),new Vector3Dd(1,0,0)),
            new RayCrossingQuery(0,10), inside);
        assertThat(inside.roots).hasSize(1);
        expectDoubleEq(1, inside.roots.get(0));
        sphere.emitRayCrossings(new Ray(new Vector3Dd(-2,1,0),new Vector3Dd(1,0,0)),
            new RayCrossingQuery(0,10), tangent);
        assertThat(tangent.roots).hasSize(1);
        expectDoubleEq(2, tangent.roots.get(0));
        assertThat(tangent.kinds.get(0)).isEqualTo(RayCrossingKind.Tangent);
        sphere.emitRayCrossings(new Ray(new Vector3Dd(-3,0,0),new Vector3Dd(1,0,0)),
            new RayCrossingQuery(2.5,3.5), clipped);
        assertThat(clipped.roots).isEmpty();
        Ray invalid = new Ray();
        invalid = invalid.withDirection(new Vector3Dd(0,0,0));
        sphere.emitRayCrossings(invalid, new RayCrossingQuery(0,10), zero);
        assertThat(zero.roots).isEmpty();
    }

    @Test
    void RayCrossings_UnsupportedIsDifferentFromMiss()
    {
        Torus unsupported = new Torus(2,1);
        Sphere sphere = new Sphere(1);
        Ray ray = new Ray(new Vector3Dd(3,0,0), new Vector3Dd(1,0,0));
        RayCrossingTestCollector hits = new RayCrossingTestCollector();
        assertThat(unsupported.emitRayCrossings(ray, new RayCrossingQuery(0,10), hits))
            .isEqualTo(RayCrossingSupport.Unsupported);
        assertThat(sphere.emitRayCrossings(ray, new RayCrossingQuery(0,10), hits))
            .isEqualTo(RayCrossingSupport.Supported);
        assertThat(hits.roots).isEmpty();
    }

    @Test
    void BoxContract_FirstHitCrossingsAndClippedSolidShareAffineParameters()
    {
        Box box = new Box(2,2,2);
        final Ray ray = Ray.fromAffine(new Vector3Dd(-3,0,0), new Vector3Dd(2,0,0));
        RayCrossingTestCollector hits = new RayCrossingTestCollector();
        assertThat(box.emitRayCrossings(ray, new RayCrossingQuery(0,4), hits)).isEqualTo(RayCrossingSupport.Supported);
        assertThat(hits.roots).hasSize(2);
        expectDoubleEq(1, hits.roots.get(0));
        expectDoubleEq(2, hits.roots.get(1));
        assertThat(hits.kinds.get(0)).isEqualTo(RayCrossingKind.Entering);
        assertThat(hits.kinds.get(1)).isEqualTo(RayCrossingKind.Leaving);
        RayHit hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
        RayBoundary boundary = new RayBoundary();
        assertThat(box.queryFirstHit(ray, new RayCrossingQuery(0,4), hit, boundary)).isEqualTo(RayFirstHitStatus.Hit);
        expectDoubleEq(1, hit.getHitDistance());
        expectDoubleEq(-1, hit.point.x());
        expectDoubleEq(-1, hit.normal.x());
        assertThat(boundary.geometry).isSameAs(box);
        assertThat(hit.isDetailAvailabilityKnown()).isTrue();
        assertThat(hit.getAvailableDetailMask()).isEqualTo(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL);
        assertThat(box.queryFirstHit(ray, new RayCrossingQuery(1.1,1.9), hit)).isEqualTo(RayFirstHitStatus.Miss);
        RaySolidClassification solid = new RaySolidClassification();
        assertThat(box.classifyRay(ray, new RayCrossingQuery(1.1,1.9), solid)).isEqualTo(RaySolidQueryStatus.Supported);
        assertThat(solid.insideSegments).hasSize(1);
        assertThat(solid.insideSegments.get(0)).isTrue();
        assertThat(solid.boundaries).isEmpty();
        assertThat(box.classifyRay(ray, new RayCrossingQuery(1,2), solid)).isEqualTo(RaySolidQueryStatus.Supported);
        assertThat(solid.boundaries).hasSize(2);
        assertThat(solid.insideSegments).hasSize(3);
        assertThat(solid.insideSegments.get(0)).isFalse();
        assertThat(solid.insideSegments.get(1)).isTrue();
        assertThat(solid.insideSegments.get(2)).isFalse();
    }

    @Test
    void BoxContract_TangencyOverlapAndNumericalFailuresAreExplicit()
    {
        Box box = new Box(2,2,2);
        RayCrossingTestCollector tangent = new RayCrossingTestCollector();
        final Ray corner = Ray.fromAffine(new Vector3Dd(-2,0,0), new Vector3Dd(1,1,0));
        assertThat(box.emitRayCrossings(corner, new RayCrossingQuery(0,4), tangent)).isEqualTo(RayCrossingSupport.Supported);
        assertThat(tangent.roots).hasSize(1);
        expectDoubleEq(1, tangent.roots.get(0));
        assertThat(tangent.kinds.get(0)).isEqualTo(RayCrossingKind.Tangent);
        RaySolidClassification solid = new RaySolidClassification();
        assertThat(box.classifyRay(corner, new RayCrossingQuery(0,4), solid)).isEqualTo(RaySolidQueryStatus.Supported);
        assertThat(solid.insideSegments).hasSize(1);
        assertThat(solid.insideSegments.get(0)).isFalse();
        final Ray alongFace = Ray.fromAffine(new Vector3Dd(-2,1,0), new Vector3Dd(1,0,0));
        RayCrossingTestCollector overlap = new RayCrossingTestCollector();
        assertThat(box.emitRayCrossings(alongFace, new RayCrossingQuery(0,4), overlap)).isEqualTo(RayCrossingSupport.Indeterminate);
        assertThat(box.classifyRay(alongFace, new RayCrossingQuery(0,4), solid)).isEqualTo(RaySolidQueryStatus.Indeterminate);
        assertThat(solid.insideSegments).isEmpty();
        assertThat(box.queryFirstHit(
            Ray.fromAffine(new Vector3Dd(), new Vector3Dd()), new RayCrossingQuery(0,4), null))
            .isEqualTo(RayFirstHitStatus.InvalidInput);
        assertThat(box.queryFirstHit(
            corner, new RayCrossingQuery(0,4,0.001), null)).isEqualTo(RayFirstHitStatus.Unsupported);
        assertThat(box.queryFirstHit(
            Ray.fromAffine(new Vector3Dd(-1e300,0,0), new Vector3Dd(1,0,0)),
            new RayCrossingQuery(0,Double.POSITIVE_INFINITY), null)).isEqualTo(RayFirstHitStatus.Indeterminate);
        RayCrossingTestCollector parallelMiss = new RayCrossingTestCollector();
        assertThat(box.emitRayCrossings(
            Ray.fromAffine(new Vector3Dd(0,2,0), new Vector3Dd(1,0,0)),
            new RayCrossingQuery(0,4), parallelMiss)).isEqualTo(RayCrossingSupport.Supported);
        assertThat(parallelMiss.roots).isEmpty();
        Box invalid = new Box(2,-2,2);
        assertThat(invalid.queryFirstHit(corner, new RayCrossingQuery(0,4), null))
            .isEqualTo(RayFirstHitStatus.InvalidInput);
    }

    @Test
    void BoxContract_EuclideanContainmentBoundsAndIndependentCopy()
    {
        Box box = new Box(2,4,6);
        assertThat(box.classifyPoint(new Vector3Dd(0,0,0), 0)).isEqualTo(GeometryContainment.Inside);
        assertThat(box.classifyPoint(new Vector3Dd(1,0,0), 0)).isEqualTo(GeometryContainment.Boundary);
        assertThat(box.classifyPoint(new Vector3Dd(1.05,2.05,0), 0.1)).isEqualTo(GeometryContainment.Boundary);
        assertThat(box.classifyPoint(new Vector3Dd(1.08,2.08,0), 0.1)).isEqualTo(GeometryContainment.Outside);
        assertThat(box.classifyPoint(new Vector3Dd(), -1)).isEqualTo(GeometryContainment.InvalidInput);
        GeometryBounds bounds = new GeometryBounds();
        box.getBounds(bounds);
        assertThat(bounds.getKind()).isEqualTo(GeometryBounds.Kind.Finite);
        expectDoubleEq(-1, bounds.getMinimum().x());
        expectDoubleEq(3, bounds.getMaximum().z());
        Geometry copy = box.copyGeometry();
        assertThat(copy).isNotNull();
        box.setSize(4,4,4);
        assertThat(copy.classifyPoint(new Vector3Dd(1.5,0,0), 0)).isEqualTo(GeometryContainment.Outside);
    }

    @Test
    void BoxContract_ArbitraryBoundsAndParallelPolicy()
    {
        final Vector3Dd minimum = new Vector3Dd(-2,-1,-3), maximum = new Vector3Dd(4,3,1);
        double[] entry = {Double.NEGATIVE_INFINITY}, exit = {Double.POSITIVE_INFINITY};
        boolean[] overlap = {false};
        assertThat(Box.intersectBounds(new Vector3Dd(-4,0,0), new Vector3Dd(1,0,0),
            minimum, maximum, 0, true, entry, exit, overlap)).isEqualTo(RayFirstHitStatus.Hit);
        expectDoubleEq(2, entry[0]);
        expectDoubleEq(8, exit[0]);
        assertThat(overlap[0]).isFalse();
        entry[0] = 0; exit[0] = Double.POSITIVE_INFINITY;
        assertThat(Box.intersectBounds(new Vector3Dd(5,0,0),
            new Vector3Dd(-1e-8,1,0), minimum, maximum, 1e-7, false, entry, exit, overlap))
            .isEqualTo(RayFirstHitStatus.Miss);
        entry[0] = Double.NEGATIVE_INFINITY; exit[0] = Double.POSITIVE_INFINITY;
        assertThat(Box.intersectBounds(new Vector3Dd(-4,3,0), new Vector3Dd(1,0,0),
            minimum, maximum, 0, true, entry, exit, overlap)).isEqualTo(RayFirstHitStatus.Hit);
        assertThat(overlap[0]).isTrue();
    }

    @Test
    void InfinitePlaneContract_SurfaceQueryPreservesAffineParameterAndDetail()
    {
        InfinitePlane plane = new InfinitePlane(2,0,0,-2);
        final Ray ray = Ray.fromAffine(new Vector3Dd(-3,0,0), new Vector3Dd(2,0,0));
        RayCrossingTestCollector crossings = new RayCrossingTestCollector();
        assertThat(plane.emitRayCrossings(ray, new RayCrossingQuery(0,4), crossings)).isEqualTo(RayCrossingSupport.Supported);
        assertThat(crossings.roots).hasSize(1);
        expectDoubleEq(2, crossings.roots.get(0));
        assertThat(crossings.kinds.get(0)).isEqualTo(RayCrossingKind.Unclassified);
        assertThat(crossings.geometries.get(0)).isSameAs(plane);
        RayHit hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
        RayBoundary boundary = new RayBoundary();
        assertThat(plane.queryFirstHit(ray, new RayCrossingQuery(2,2), hit, boundary)).isEqualTo(RayFirstHitStatus.Hit);
        expectDoubleEq(2, hit.getHitDistance());
        expectDoubleEq(1, hit.point.x());
        expectDoubleEq(1, hit.normal.x());
        assertThat(boundary.geometry).isSameAs(plane);
        assertThat(hit.getAvailableDetailMask()).isEqualTo(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL);
        assertThat(plane.queryFirstHit(ray, new RayCrossingQuery(0,1), hit)).isEqualTo(RayFirstHitStatus.Miss);
        assertThat(hit.hasHitDistance()).isFalse();
        RaySolidClassification solid = new RaySolidClassification();
        assertThat(plane.classifyRay(ray, new RayCrossingQuery(0,4), solid)).isEqualTo(RaySolidQueryStatus.Unsupported);
        assertThat(plane.classifyPoint(new Vector3Dd(), 0)).isEqualTo(GeometryContainment.Unsupported);
    }

    @Test
    void InfinitePlaneContract_ParallelCoincidentDegenerateAndBoundsAreExplicit()
    {
        InfinitePlane plane = new InfinitePlane(1,0,0,0);
        RayCrossingTestCollector events = new RayCrossingTestCollector();
        final Ray parallel = Ray.fromAffine(new Vector3Dd(1,0,0), new Vector3Dd(0,1,0));
        final Ray coincident = Ray.fromAffine(new Vector3Dd(), new Vector3Dd(0,1,0));
        assertThat(plane.emitRayCrossings(parallel, new RayCrossingQuery(0,4), events)).isEqualTo(RayCrossingSupport.Supported);
        assertThat(events.roots).isEmpty();
        assertThat(plane.emitRayCrossings(coincident, new RayCrossingQuery(0,4), events)).isEqualTo(RayCrossingSupport.Indeterminate);
        InfinitePlane invalid = new InfinitePlane(0,0,0,1);
        assertThat(invalid.queryFirstHit(parallel, new RayCrossingQuery(0,4), null)).isEqualTo(RayFirstHitStatus.InvalidInput);
        InfinitePlane tiny = new InfinitePlane(1e-200,0,0,0);
        assertThat(tiny.queryFirstHit(parallel, new RayCrossingQuery(0,4), null)).isEqualTo(RayFirstHitStatus.Indeterminate);
        InfinitePlane shallow = new InfinitePlane(1e-100,0,0,1);
        assertThat(shallow.queryFirstHit(
            Ray.fromAffine(new Vector3Dd(), new Vector3Dd(1e-250,1,0)), new RayCrossingQuery(0,4), null))
            .isEqualTo(RayFirstHitStatus.Indeterminate);
        GeometryBounds bounds = new GeometryBounds();
        plane.getBounds(bounds);
        assertThat(bounds.getKind()).isEqualTo(GeometryBounds.Kind.Unbounded);
        invalid.getBounds(bounds);
        assertThat(bounds.getKind()).isEqualTo(GeometryBounds.Kind.Indeterminate);
        Geometry copy = plane.copyGeometry();
        assertThat(copy).isNotNull();
        plane.setD(-5);
        assertThat(copy.queryFirstHit(
            Ray.fromAffine(new Vector3Dd(-2,0,0), new Vector3Dd(1,0,0)), new RayCrossingQuery(2,2), null))
            .isEqualTo(RayFirstHitStatus.Hit);
        double[] depth = {0};
        assertThat(InfinitePlane.intersectionDepth(0.001, 0.0005, 0.001, 0, 10, false, depth))
            .isEqualTo(RayFirstHitStatus.Miss);
        assertThat(InfinitePlane.intersectionDepth(0.001, 0.001, 0.001, 0, 10, false, depth))
            .isEqualTo(RayFirstHitStatus.Hit);
        expectDoubleEq(1, depth[0]);
    }

    @Test
    void PlanarHalfSpaceContract_ContainmentBoundsAndIndependentCopy()
    {
        PlanarHalfSpace half = new PlanarHalfSpace(2,0,0,-2);
        assertThat(half.classifyPoint(new Vector3Dd(0,0,0), 0)).isEqualTo(GeometryContainment.Inside);
        assertThat(half.classifyPoint(new Vector3Dd(1,0,0), 0)).isEqualTo(GeometryContainment.Boundary);
        assertThat(half.classifyPoint(new Vector3Dd(1.05,0,0), 0.1)).isEqualTo(GeometryContainment.Boundary);
        assertThat(half.classifyPoint(new Vector3Dd(1.2,0,0), 0.1)).isEqualTo(GeometryContainment.Outside);
        assertThat(half.classifyPoint(new Vector3Dd(), -1)).isEqualTo(GeometryContainment.InvalidInput);
        PlanarHalfSpace invalid = new PlanarHalfSpace(0,0,0,1);
        assertThat(invalid.classifyPoint(new Vector3Dd(), 0)).isEqualTo(GeometryContainment.InvalidInput);
        PlanarHalfSpace tiny = new PlanarHalfSpace(1e-200,0,0,0);
        assertThat(tiny.classifyPoint(new Vector3Dd(), 0)).isEqualTo(GeometryContainment.Indeterminate);
        PlanarHalfSpace shallow = new PlanarHalfSpace(1e-100,0,0,0);
        assertThat(shallow.classifyPoint(new Vector3Dd(1e-250,0,0), 0)).isEqualTo(GeometryContainment.Indeterminate);
        GeometryBounds bounds = new GeometryBounds();
        half.getBounds(bounds);
        assertThat(bounds.getKind()).isEqualTo(GeometryBounds.Kind.Unbounded);
        Geometry copy = half.copyGeometry();
        assertThat(copy).isNotNull();
        assertThat(copy.classifyPoint(new Vector3Dd(), 0)).isEqualTo(GeometryContainment.Inside);
    }

    @Test
    void PlanarHalfSpaceContract_OrderedCrossingAndEndpointStates()
    {
        PlanarHalfSpace half = new PlanarHalfSpace(2,0,0,-2);
        final Ray leaving = Ray.fromAffine(new Vector3Dd(-3,0,0), new Vector3Dd(2,0,0));
        RayCrossingTestCollector crossings = new RayCrossingTestCollector();
        assertThat(half.emitRayCrossings(leaving, new RayCrossingQuery(0,4), crossings)).isEqualTo(RayCrossingSupport.Supported);
        assertThat(crossings.roots).hasSize(1);
        expectDoubleEq(2, crossings.roots.get(0));
        assertThat(crossings.kinds.get(0)).isEqualTo(RayCrossingKind.Leaving);
        assertThat(crossings.geometries.get(0)).isSameAs(half);
        RayHit hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL, false);
        RayBoundary boundary = new RayBoundary();
        assertThat(half.queryFirstHit(leaving, new RayCrossingQuery(2,2), hit, boundary)).isEqualTo(RayFirstHitStatus.Hit);
        assertThat(boundary.geometry).isSameAs(half);
        expectDoubleEq(1, hit.point.x());
        expectDoubleEq(1, hit.normal.x());
        RaySolidClassification solid = new RaySolidClassification();
        assertThat(half.classifyRay(leaving, new RayCrossingQuery(2,4), solid)).isEqualTo(RaySolidQueryStatus.Supported);
        assertThat(solid.boundaries).hasSize(1);
        assertThat(solid.insideSegments).hasSize(2);
        assertThat(solid.insideSegments.get(0)).isTrue();
        assertThat(solid.insideSegments.get(1)).isFalse();
        final Ray entering = Ray.fromAffine(new Vector3Dd(3,0,0), new Vector3Dd(-2,0,0));
        RayCrossingTestCollector opposite = new RayCrossingTestCollector();
        assertThat(half.emitRayCrossings(entering, new RayCrossingQuery(0,4), opposite)).isEqualTo(RayCrossingSupport.Supported);
        assertThat(opposite.roots).hasSize(1);
        expectDoubleEq(1, opposite.roots.get(0));
        assertThat(opposite.kinds.get(0)).isEqualTo(RayCrossingKind.Entering);
        assertThat(half.classifyRay(entering, new RayCrossingQuery(1,4), solid)).isEqualTo(RaySolidQueryStatus.Supported);
        assertThat(solid.insideSegments).hasSize(2);
        assertThat(solid.insideSegments.get(0)).isFalse();
        assertThat(solid.insideSegments.get(1)).isTrue();
    }

    @Test
    void PlanarHalfSpaceContract_ParallelAndClippedIntervalsRemainDistinct()
    {
        PlanarHalfSpace half = new PlanarHalfSpace(1,0,0,0);
        RaySolidClassification solid = new RaySolidClassification();
        assertThat(half.classifyRay(
            Ray.fromAffine(new Vector3Dd(-1,0,0), new Vector3Dd(0,1,0)), new RayCrossingQuery(0,4), solid))
            .isEqualTo(RaySolidQueryStatus.Supported);
        assertThat(solid.insideSegments).hasSize(1);
        assertThat(solid.insideSegments.get(0)).isTrue();
        assertThat(half.classifyRay(
            Ray.fromAffine(new Vector3Dd(1,0,0), new Vector3Dd(0,1,0)), new RayCrossingQuery(0,4), solid))
            .isEqualTo(RaySolidQueryStatus.Supported);
        assertThat(solid.insideSegments).hasSize(1);
        assertThat(solid.insideSegments.get(0)).isFalse();
        assertThat(half.classifyRay(
            Ray.fromAffine(new Vector3Dd(), new Vector3Dd(0,1,0)), new RayCrossingQuery(0,4), solid))
            .isEqualTo(RaySolidQueryStatus.Indeterminate);
        assertThat(solid.insideSegments).isEmpty();
        assertThat(half.classifyRay(
            Ray.fromAffine(new Vector3Dd(-3,0,0), new Vector3Dd(1,0,0)), new RayCrossingQuery(0,2), solid))
            .isEqualTo(RaySolidQueryStatus.Supported);
        assertThat(solid.insideSegments).hasSize(1);
        assertThat(solid.insideSegments.get(0)).isTrue();
        assertThat(solid.boundaries).isEmpty();
    }

    @Test
    void RaySolidClassification_NonunitRayAndClippedInterior()
    {
        Sphere sphere = new Sphere(1);
        Ray ray = Ray.fromAffine(new Vector3Dd(-3,0,0), new Vector3Dd(2,0,0));
        RaySolidClassification result = new RaySolidClassification();
        assertThat(sphere.classifyRay(ray, new RayCrossingQuery(0,4), result)).isEqualTo(RaySolidQueryStatus.Supported);
        assertThat(result.boundaries).hasSize(2);
        expectDoubleEq(1, result.boundaries.get(0).t);
        expectDoubleEq(2, result.boundaries.get(1).t);
        assertThat(result.boundaries.get(0).geometry).isSameAs(sphere);
        assertThat(result.boundaries.get(0).normalReversed).isFalse();
        assertThat(result.insideSegments).hasSize(3);
        assertThat(result.insideSegments.get(0)).isEqualTo(false);
        assertThat(result.insideSegments.get(1)).isEqualTo(true);
        assertThat(result.insideSegments.get(2)).isEqualTo(false);
        assertThat(sphere.classifyRay(ray, new RayCrossingQuery(1.25,1.75), result)).isEqualTo(RaySolidQueryStatus.Supported);
        assertThat(result.boundaries).isEmpty();
        assertThat(result.insideSegments).hasSize(1);
        assertThat(result.insideSegments.get(0)).isEqualTo(true);
    }

    @Test
    void RaySolidClassification_InteriorStartAndEndpointLimitingStates()
    {
        Sphere sphere = new Sphere(1);
        Ray ray = Ray.fromAffine(new Vector3Dd(0,0,0), new Vector3Dd(2,0,0));
        RaySolidClassification result = new RaySolidClassification();
        sphere.classifyRay(ray, new RayCrossingQuery(0,2), result);
        assertThat(result.boundaries).hasSize(1);
        expectDoubleEq(0.5, result.boundaries.get(0).t);
        assertThat(result.insideSegments).hasSize(2);
        assertThat(result.insideSegments.get(0)).isEqualTo(true);
        assertThat(result.insideSegments.get(1)).isEqualTo(false);
        ray = ray.withOrigin(new Vector3Dd(-3,0,0));
        sphere.classifyRay(ray, new RayCrossingQuery(1,2), result);
        assertThat(result.boundaries).hasSize(2);
        assertThat(result.insideSegments).hasSize(3);
        assertThat(result.insideSegments.get(0)).isEqualTo(false);
        assertThat(result.insideSegments.get(1)).isEqualTo(true);
        assertThat(result.insideSegments.get(2)).isEqualTo(false);
        sphere.classifyRay(ray, new RayCrossingQuery(2,3), result);
        assertThat(result.boundaries).hasSize(1);
        expectDoubleEq(2, result.boundaries.get(0).t);
        assertThat(result.insideSegments).hasSize(2);
        assertThat(result.insideSegments.get(0)).isEqualTo(true);
        assertThat(result.insideSegments.get(1)).isEqualTo(false);
    }

    @Test
    void RaySolidClassification_TangencyIsASurfaceEventButNotAnInterior()
    {
        Sphere sphere = new Sphere(1);
        Ray ray = new Ray(new Vector3Dd(-2,1,0), new Vector3Dd(1,0,0));
        RayCrossingTestCollector crossings = new RayCrossingTestCollector();
        sphere.emitRayCrossings(ray, new RayCrossingQuery(0,4), crossings);
        assertThat(crossings.roots).hasSize(1);
        assertThat(crossings.kinds.get(0)).isEqualTo(RayCrossingKind.Tangent);
        RaySolidClassification result = new RaySolidClassification();
        assertThat(sphere.classifyRay(ray, new RayCrossingQuery(0,4), result)).isEqualTo(RaySolidQueryStatus.Supported);
        assertThat(result.boundaries).isEmpty();
        assertThat(result.insideSegments).hasSize(1);
        assertThat(result.insideSegments.get(0)).isEqualTo(false);
    }

    @Test
    void RaySolidClassification_MissAndUnboundedInterval()
    {
        Sphere sphere = new Sphere(1);
        RaySolidClassification result = new RaySolidClassification();
        Ray ray = new Ray(new Vector3Dd(-3,2,0), new Vector3Dd(1,0,0));
        sphere.classifyRay(ray, new RayCrossingQuery(0,4), result);
        assertThat(result.boundaries).isEmpty();
        assertThat(result.insideSegments).hasSize(1);
        assertThat(result.insideSegments.get(0)).isEqualTo(false);
        ray = ray.withOrigin(new Vector3Dd(-3,0,0));
        final double inf = Double.POSITIVE_INFINITY;
        sphere.classifyRay(ray, new RayCrossingQuery(-inf,inf), result);
        assertThat(result.boundaries).hasSize(2);
        expectDoubleEq(2, result.boundaries.get(0).t);
        expectDoubleEq(4, result.boundaries.get(1).t);
        assertThat(result.insideSegments).hasSize(3);
        assertThat(result.insideSegments.get(0)).isEqualTo(false);
        assertThat(result.insideSegments.get(1)).isEqualTo(true);
        assertThat(result.insideSegments.get(2)).isEqualTo(false);
    }

    @Test
    void RaySolidClassification_InvalidUnsupportedAndIndeterminateClearReusedResult()
    {
        Sphere sphere = new Sphere(1);
        Torus unsupported = new Torus(2,1);
        Ray ray = new Ray(new Vector3Dd(-3,0,0), new Vector3Dd(1,0,0));
        RaySolidClassification result = new RaySolidClassification();
        sphere.classifyRay(ray, new RayCrossingQuery(0,4), result);
        assertThat(unsupported.classifyRay(ray, new RayCrossingQuery(0,4), result)).isEqualTo(RaySolidQueryStatus.Unsupported);
        assertThat(result.boundaries).isEmpty();
        assertThat(result.insideSegments).isEmpty();
        final RayCrossingQuery[] invalidQueries = {new RayCrossingQuery(4,0), new RayCrossingQuery(1,1),
                new RayCrossingQuery(0,4,0.001), new RayCrossingQuery(0,4,0,0.001),
                new RayCrossingQuery(Double.NaN,4)};
        for ( final RayCrossingQuery query : invalidQueries ) {
            sphere.classifyRay(ray, new RayCrossingQuery(0,4), result);
            assertThat(sphere.classifyRay(ray, query, result)).isEqualTo(RaySolidQueryStatus.InvalidInput);
            assertThat(result.boundaries).isEmpty();
            assertThat(result.insideSegments).isEmpty();
        }
        ray = ray.withDirection(new Vector3Dd(0,0,0));
        assertThat(sphere.classifyRay(ray, new RayCrossingQuery(0,4), result)).isEqualTo(RaySolidQueryStatus.InvalidInput);
        ray = ray.withDirection(new Vector3Dd(1e300,0,0));
        assertThat(sphere.classifyRay(ray, new RayCrossingQuery(0,4), result)).isEqualTo(RaySolidQueryStatus.Indeterminate);
        assertThat(result.insideSegments).isEmpty();
    }

    @Test
    void RaySolidClassification_UnrepresentableRadiusIsNotReportedAsEmpty()
    {
        Sphere tiny = new Sphere(1e-300);
        Ray ray = new Ray(new Vector3Dd(0,0,0), new Vector3Dd(1,0,0));
        RaySolidClassification result = new RaySolidClassification();
        assertThat(tiny.classifyRay(ray, new RayCrossingQuery(-1e-301,1e-301), result))
            .isEqualTo(RaySolidQueryStatus.Indeterminate);
        assertThat(result.boundaries).isEmpty();
        assertThat(result.insideSegments).isEmpty();
    }

    /**
    The cpp test also checks that `boundaries.data()` and
    `insideSegments.data()` keep their storage after a reuse; Java lists do
    not expose their storage, so those two checks are not ported.
    */
    @Test
    void RaySolidClassification_NativeListsRetainCapacityAndCopiesAreIndependent()
    {
        Sphere sphere = new Sphere(1);
        Ray ray = new Ray(new Vector3Dd(-3,0,0), new Vector3Dd(1,0,0));
        RaySolidClassification result = new RaySolidClassification();
        sphere.classifyRay(ray, new RayCrossingQuery(0,5), result);
        assertThat(result.boundaries).hasSize(2);
        RaySolidClassification copied = new RaySolidClassification(result);
        RaySolidClassification assigned = new RaySolidClassification();
        assigned.assign(result);
        sphere.classifyRay(ray, new RayCrossingQuery(2.5,3.5), result);
        assertThat(result.boundaries).isEmpty();
        assertThat(copied.boundaries).hasSize(2);
        assertThat(assigned.boundaries).hasSize(2);
        copied.boundaries.get(0).t = 99;
        expectDoubleEq(2, assigned.boundaries.get(0).t);
        assertThat(copied.insideSegments).hasSize(3);
        assertThat(result.insideSegments).hasSize(1);
        assertThat(result.insideSegments.get(0)).isTrue();
    }

    @Test
    void RaySolidClassification_JavaDoubleClassifiesSpecialValues()
    {
        assertThat(Double.isNaN(Double.NaN)).isTrue();
        assertThat(Double.isFinite(Double.NaN)).isFalse();
        assertThat(Double.isFinite(Double.POSITIVE_INFINITY)).isFalse();
        assertThat(Double.isFinite(Double.NEGATIVE_INFINITY)).isFalse();
        assertThat(Double.isFinite(Double.MAX_VALUE)).isTrue();
        assertThat(Double.isFinite(Double.MIN_VALUE)).isTrue();
        assertThat(Double.isNaN(0)).isFalse();
    }

    @Test
    void GeometryBoundsContract_ExplicitStatesAndSphereBounds()
    {
        GeometryBounds bounds = new GeometryBounds();
        assertThat(bounds.getKind()).isEqualTo(GeometryBounds.Kind.Unsupported);
        Sphere sphere = new Sphere(2);
        sphere.getBounds(bounds);
        assertThat(bounds.getKind()).isEqualTo(GeometryBounds.Kind.Finite);
        expectDoubleEq(-2, bounds.getMinimum().x());
        expectDoubleEq(2, bounds.getMaximum().z());
        Box box = new Box(1,1,1);
        box.getBounds(bounds);
        assertThat(bounds.getKind()).isEqualTo(GeometryBounds.Kind.Finite);
        bounds.setEmpty();
        assertThat(bounds.getKind()).isEqualTo(GeometryBounds.Kind.Empty);
        bounds.setUnbounded();
        assertThat(bounds.getKind()).isEqualTo(GeometryBounds.Kind.Unbounded);
        assertThat(bounds.setFinite(new Vector3Dd(2,0,0), new Vector3Dd(1,1,1))).isFalse();
        assertThat(bounds.getKind()).isEqualTo(GeometryBounds.Kind.Indeterminate);
        assertThat(bounds.setFinite(new Vector3Dd(), new Vector3Dd(Double.POSITIVE_INFINITY,1,1))).isFalse();
        assertThat(bounds.getKind()).isEqualTo(GeometryBounds.Kind.Indeterminate);
    }
}
