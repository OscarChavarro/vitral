package vsdk.toolkit.environment.geometry.volume;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayCrossingQuery;
import vsdk.toolkit.environment.geometry.element.RayHit;
import vsdk.toolkit.environment.geometry.element.RaySolidClassification;
import vsdk.toolkit.environment.geometry.element.RaySolidQueryStatus;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologySummary;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
Exercises the `Tube` (axis along Z, base at z = 0): ray intersection, ray
classification, point containment, and its boundary representation built
with Euler operators, also for the washer (height 0) case.
*/
class TubeTest
{
    private static final double TOLERANCE = 0.01;
    private static final double EPS = 1e-9;

    private static Ray ray(double ox, double oy, double oz,
                           double dx, double dy, double dz)
    {
        return new Ray(new Vector3Dd(ox, oy, oz), new Vector3Dd(dx, dy, dz));
    }

    private static RayHit fullHit()
    {
        return new RayHit(RayHit.DETAIL_ALL, true);
    }

    @Test
    void given_tube_when_rayFromOutsideHitsExternalWall_then_normalPointsOut()
    {
        Tube tube = new Tube(1, 2, 3);
        RayHit hit = fullHit();

        assertThat(tube.doIntersectionFirstHit(ray(-5, 0, 1, 1, 0, 0), hit)).isTrue();
        assertThat(hit.getRay().getT()).isCloseTo(3, within(EPS));
        assertThat(hit.normal.x()).isCloseTo(-1, within(EPS));
    }

    @Test
    void given_tube_when_rayStartsInHole_then_hitsInternalWallWithNormalToAxis()
    {
        Tube tube = new Tube(1, 2, 3);
        RayHit hit = fullHit();

        assertThat(tube.doIntersectionFirstHit(ray(0, 0, 1, 1, 0, 0), hit)).isTrue();
        assertThat(hit.getRay().getT()).isCloseTo(1, within(EPS));
        assertThat(hit.normal.x()).isCloseTo(-1, within(EPS));
    }

    @Test
    void given_tube_when_verticalRayOverWallOrHole_then_capOrNothingIsHit()
    {
        Tube tube = new Tube(1, 2, 3);
        RayHit hit = fullHit();

        assertThat(tube.doIntersectionFirstHit(ray(1.5, 0, 10, 0, 0, -1), hit)).isTrue();
        assertThat(hit.getRay().getT()).isCloseTo(7, within(EPS));
        assertThat(hit.normal.z()).isCloseTo(1, within(EPS));
        assertThat(tube.doIntersectionFirstHit(ray(0.5, 0, 10, 0, 0, -1), fullHit())).isFalse();
        assertThat(tube.doIntersectionFirstHit(ray(2.5, 0, 10, 0, 0, -1), fullHit())).isFalse();
    }

    @Test
    void given_solidCylinderTube_when_rayCrossesAxis_then_oneInterval()
    {
        Tube tube = new Tube(0, 2, 3);
        RaySolidClassification c = new RaySolidClassification();

        assertThat(tube.classifyRay(ray(-5, 0, 1, 1, 0, 0),
            new RayCrossingQuery(0, 100), c)).isEqualTo(RaySolidQueryStatus.Supported);
        assertThat(c.boundaries).hasSize(2);
        assertThat(c.boundaries.get(0).t).isCloseTo(3, within(EPS));
        assertThat(c.boundaries.get(1).t).isCloseTo(7, within(EPS));
    }

    @Test
    void given_tube_when_rayCrossesHole_then_twoInteriorIntervals()
    {
        Tube tube = new Tube(1, 2, 3);
        RaySolidClassification c = new RaySolidClassification();

        assertThat(tube.classifyRay(ray(-5, 0, 1, 1, 0, 0),
            new RayCrossingQuery(0, 100), c)).isEqualTo(RaySolidQueryStatus.Supported);
        assertThat(c.insideSegments.get(0)).isFalse();
        assertThat(c.boundaries).hasSize(4);
        double[] expected = {3, 4, 6, 7};
        for ( int i = 0; i < 4; i++ ) {
            assertThat(c.boundaries.get(i).t).isCloseTo(expected[i], within(EPS));
            assertThat(c.insideSegments.get(i + 1)).isEqualTo(i % 2 == 0);
        }
    }

    @Test
    void given_tube_when_queryStartsInsideWall_then_startsInside()
    {
        Tube tube = new Tube(1, 2, 3);
        RaySolidClassification c = new RaySolidClassification();

        tube.classifyRay(ray(-5, 0, 1, 1, 0, 0), new RayCrossingQuery(3.5, 100), c);
        assertThat(c.insideSegments.get(0)).isTrue();
        assertThat(c.boundaries).hasSize(3);
        assertThat(c.boundaries.get(0).t).isCloseTo(4, within(EPS));
    }

    @Test
    void given_tube_when_pointsClassified_then_wallHoleAndCapsAreRespected()
    {
        Tube tube = new Tube(1, 2, 3);

        assertThat(tube.doContainmentTest(new Vector3Dd(1.5, 0, 1), TOLERANCE))
            .isEqualTo(Geometry.INSIDE);
        assertThat(tube.doContainmentTest(new Vector3Dd(0, 0.5, 1), TOLERANCE))
            .isEqualTo(Geometry.OUTSIDE);
        assertThat(tube.doContainmentTest(new Vector3Dd(0, 1.0, 1), TOLERANCE))
            .isEqualTo(Geometry.LIMIT);
        assertThat(tube.doContainmentTest(new Vector3Dd(2.0, 0, 1), TOLERANCE))
            .isEqualTo(Geometry.LIMIT);
        assertThat(tube.doContainmentTest(new Vector3Dd(1.5, 0, 3), TOLERANCE))
            .isEqualTo(Geometry.LIMIT);
        assertThat(tube.doContainmentTest(new Vector3Dd(2.5, 0, 1), TOLERANCE))
            .isEqualTo(Geometry.OUTSIDE);
        assertThat(tube.doContainmentTest(new Vector3Dd(1.5, 0, -0.5), TOLERANCE))
            .isEqualTo(Geometry.OUTSIDE);
    }

    @Test
    void given_washer_when_raysAndPointsTested_then_onlyTheAnnulusIsHit()
    {
        Tube washer = new Tube(1, 2, 0);
        RayHit hit = fullHit();
        RaySolidClassification c = new RaySolidClassification();

        assertThat(washer.isWasher()).isTrue();
        assertThat(washer.doIntersectionFirstHit(ray(1.5, 0, 5, 0, 0, -1), hit)).isTrue();
        assertThat(hit.getRay().getT()).isCloseTo(5, within(EPS));
        assertThat(hit.normal.z()).isCloseTo(1, within(EPS));
        assertThat(washer.doIntersectionFirstHit(ray(1.5, 0, -5, 0, 0, 1), hit)).isTrue();
        assertThat(hit.normal.z()).isCloseTo(-1, within(EPS));
        assertThat(washer.doIntersectionFirstHit(ray(0.5, 0, 5, 0, 0, -1), fullHit())).isFalse();
        assertThat(washer.doContainmentTest(new Vector3Dd(1.5, 0, 0), TOLERANCE))
            .isEqualTo(Geometry.LIMIT);
        assertThat(washer.doContainmentTest(new Vector3Dd(0.5, 0, 0), TOLERANCE))
            .isEqualTo(Geometry.OUTSIDE);
        washer.classifyRay(ray(1.5, 0, 5, 0, 0, -1), new RayCrossingQuery(0, 100), c);
        assertThat(c.boundaries).isEmpty();
    }

    private static void assertValidBrep(PolyhedralBoundedSolid solid,
        int expectedVertices, int expectedEdges, int expectedFaces)
    {
        PolyhedralBoundedSolidTopologySummary topology =
            PolyhedralBoundedSolidTopologySummary.from(solid);

        assertThat(topology.getVertexCount()).isEqualTo(expectedVertices);
        assertThat(topology.getEdgeCount()).isEqualTo(expectedEdges);
        assertThat(topology.getFaceCount()).isEqualTo(expectedFaces);
        assertThat(topology.getShellCount()).isEqualTo(1);
        StringBuilder msg = new StringBuilder();
        assertThat(PolyhedralBoundedSolidValidationEngine.validateStrict(solid, msg))
            .as(msg.toString()).isTrue();
    }

    @Test
    void given_tube_when_exportedToBrep_then_isValidSolidOfGenusOne()
    {
        int n = 12;
        PolyhedralBoundedSolid solid =
            new Tube(1, 2, 3).exportToPolyhedralBoundedSolid(n);

        // Top and bottom annuli (one face each, with a ring) plus n external
        // and n internal side faces; V - E + F - (L - F) = 2 (S - H) with
        // one ring in each cap and genus 1
        assertValidBrep(solid, 4 * n, 6 * n, 2 * n + 2);
        for ( int i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);
            Vector3Dd normal = face.getContainingPlane().getNormal();
            Vector3Dd p = face.boundariesList.get(0).boundaryStartHalfEdge
                .startingVertex.position;
            Vector3Dd centroid = faceCentroid(face);
            if ( Math.abs(normal.z()) > 0.5 ) {
                // Caps: outward along Z
                assertThat(Math.signum(normal.z()))
                    .isEqualTo(p.z() > 1.5 ? 1.0 : -1.0);
            }
            else {
                // Walls: away from the axis outside, towards it inside
                double radial = normal.x() * centroid.x() + normal.y() * centroid.y();
                double rho = Math.hypot(centroid.x(), centroid.y());
                assertThat(Math.signum(radial)).isEqualTo(rho > 1.5 ? 1.0 : -1.0);
            }
        }
    }

    @Test
    void given_washer_when_exportedToBrep_then_isLaminaWithHole()
    {
        int n = 12;
        PolyhedralBoundedSolid solid =
            new Tube(1, 2, 0).exportToPolyhedralBoundedSolid(n);
        PolyhedralBoundedSolidTopologySummary topology =
            PolyhedralBoundedSolidTopologySummary.from(solid);

        assertThat(topology.getVertexCount()).isEqualTo(2 * n);
        assertThat(topology.getEdgeCount()).isEqualTo(2 * n);
        assertThat(topology.getFaceCount()).isEqualTo(2);
        for ( int i = 0; i < solid.getPolygonsList().size(); i++ ) {
            assertThat(solid.getPolygonsList().get(i).boundariesList.size())
                .isEqualTo(2);
        }
    }

    @Test
    void given_solidCylinderTube_when_exportedToBrep_then_isValid()
    {
        int n = 12;
        assertValidBrep(new Tube(0, 2, 3).exportToPolyhedralBoundedSolid(n),
            2 * n, 3 * n, n + 2);
    }

    private static Vector3Dd faceCentroid(_PolyhedralBoundedSolidFace face)
    {
        var start = face.boundariesList.get(0).boundaryStartHalfEdge;
        var he = start;
        Vector3Dd sum = new Vector3Dd(0, 0, 0);
        int count = 0;
        do {
            sum = sum.add(he.startingVertex.position);
            count++;
            he = he.next();
        } while ( he != start );
        return sum.multiply(1.0 / count);
    }
}
