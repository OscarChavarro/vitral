package vsdk.toolkit.environment.geometry.volume;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.Ray;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
Exercises the intersection of rays with a torus (whose axis is the Z axis),
specially the rejection of roots of the intersection equation that do not
belong to the surface.
 */
class TorusTest
{
    private static final double EPS = 1.0e-6;

    @Test
    void given_rayAlongTheAxisOfATube_when_intersected_then_hitsTheNearestSurface()
    {
        // Arrange
        Torus torus = new Torus(1.0, 0.25);
        Ray ray = new Ray(new Vector3Dd(1, 0, 5), new Vector3Dd(0, 0, -1));

        // Act
        Ray hit = torus.doIntersectionFirstHit(ray);

        // Assert
        assertThat(hit).isNotNull();
        assertThat(hit.getT()).isCloseTo(4.75, offset(EPS));
    }

    @Test
    void given_rayThroughTheHole_when_intersected_then_missesTheTorus()
    {
        // Arrange
        Torus torus = new Torus(1.0, 0.25);
        Ray ray = new Ray(new Vector3Dd(0, 0, 5), new Vector3Dd(0, 0, -1));

        // Act & Assert
        assertThat(torus.doIntersectionFirstHit(ray)).isNull();
    }

    @Test
    void given_veryDistantOrigin_when_intersected_then_hitsTheOuterEquator()
    {
        // Arrange
        Torus torus = new Torus(1.0, 0.25);
        Ray ray = new Ray(new Vector3Dd(1000, 0, 0), new Vector3Dd(-1, 0, 0));

        // Act
        Ray hit = torus.doIntersectionFirstHit(ray);

        // Assert
        assertThat(hit).isNotNull();
        assertThat(hit.getT()).isCloseTo(998.75, offset(1.0e-4));
    }

    @Test
    void given_rayPassingNearTheTorusButNotTouchingIt_when_intersected_then_missesIt()
    {
        // Arrange: ray found by a rotate gizmo, for which the quartic solver
        // gives a real root (t = 11.26) whose point is far from the torus
        Torus torus = new Torus(1.0132456102380434, 0.07599342076785325);
        Ray ray = new Ray(
            new Vector3Dd(-1.9999999999999996, -3.0, 2.5000000000000004),
            new Vector3Dd(0.3958157631719224, 0.7164180056829774, -0.5745216469010146));

        // Act & Assert
        assertThat(torus.doIntersectionFirstHit(ray)).isNull();
    }

    @Test
    void given_rayTouchingTheTube_when_intersected_then_hitPointIsOnTheSurface()
    {
        // Arrange
        Torus torus = new Torus(2.0, 0.5);
        Vector3Dd direction = new Vector3Dd(0.3, 0.2, -1).normalized();
        Vector3Dd target = new Vector3Dd(0, 2.0, 0.5);
        Ray ray = new Ray(target.subtract(direction.multiply(7)), direction);

        // Act
        Ray hit = torus.doIntersectionFirstHit(ray);

        // Assert
        assertThat(hit).isNotNull();
        Vector3Dd point = ray.getOrigin().add(direction.multiply(hit.getT()));
        double distanceToCircle = Math.hypot(Math.hypot(point.x(), point.y()) - 2.0, point.z());

        assertThat(distanceToCircle).isCloseTo(0.5, offset(EPS));
        assertThat(hit.getT()).isLessThanOrEqualTo(7.0 + EPS);
    }
}
