package vsdk.toolkit.render;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.environment.geometry.volume.Sphere;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
Exercises the conversion of a `Sphere` into the polyhedral bounded solid (and
the triangle attributes) shared by the sphere renderers.
 */
class SpherePolyhedralCacheTest
{
    @Test
    void given_aSphere_when_obtainingItsTessellation_then_everyFaceIsAnOutwardTriangleOnTheSphere()
    {
        // Arrange
        Sphere sphere = new Sphere(2.0);

        // Act
        SpherePolyhedralCache.Entry entry = SpherePolyhedralCache.obtain(sphere, 32, 15);

        // Assert
        assertThat(entry.getVertexCount())
            .isEqualTo(3 * entry.getSolid().getPolygonsList().size());
        float[] p = entry.getPositions();
        float[] n = entry.getNormals();
        for ( int i = 0; i < entry.getVertexCount(); i++ ) {
            double length = Math.sqrt(p[3*i]*p[3*i] + p[3*i+1]*p[3*i+1] + p[3*i+2]*p[3*i+2]);

            assertThat(length).isCloseTo(2.0, within(1e-5));
            // The normal of the parametric surface is the radial direction
            assertThat(n[3*i]).isCloseTo((float)(p[3*i] / length), within(1e-5f));
            assertThat(n[3*i+1]).isCloseTo((float)(p[3*i+1] / length), within(1e-5f));
            assertThat(n[3*i+2]).isCloseTo((float)(p[3*i+2] / length), within(1e-5f));
        }
        for ( int t = 0; t < entry.getVertexCount(); t += 3 ) {
            double ux = p[3*t+3] - p[3*t];
            double uy = p[3*t+4] - p[3*t+1];
            double uz = p[3*t+5] - p[3*t+2];
            double vx = p[3*t+6] - p[3*t];
            double vy = p[3*t+7] - p[3*t+1];
            double vz = p[3*t+8] - p[3*t+2];
            double outward = (uy*vz - uz*vy) * p[3*t] + (uz*vx - ux*vz) * p[3*t+1] +
                (ux*vy - uy*vx) * p[3*t+2];

            assertThat(outward).isGreaterThan(0.0);
        }
    }

    @Test
    void given_aTessellation_when_readingTheTextureCoordinatesOfATriangle_then_theyDoNotJumpAcrossTheSeam()
    {
        // Arrange
        SpherePolyhedralCache.Entry entry =
            SpherePolyhedralCache.obtain(new Sphere(1.0), 16, 8);
        float[] uv = entry.getUvs();

        // Act / Assert
        for ( int t = 0; t < entry.getVertexCount(); t += 3 ) {
            float min = Math.min(uv[2*t], Math.min(uv[2*t+2], uv[2*t+4]));
            float max = Math.max(uv[2*t], Math.max(uv[2*t+2], uv[2*t+4]));

            assertThat(max - min).isLessThanOrEqualTo(1.0f / 16 + 1e-5f);
        }
    }

    @Test
    void given_twoSpheresOfTheSameRadius_when_obtainingTheirTessellation_then_theEntryIsShared()
    {
        // Act
        SpherePolyhedralCache.Entry a = SpherePolyhedralCache.obtain(new Sphere(0.5), 20, 10);
        SpherePolyhedralCache.Entry b = SpherePolyhedralCache.obtain(new Sphere(0.5), 20, 10);

        // Assert
        assertThat(a).isSameAs(b);
        assertThat(a.getSolid()).isSameAs(b.getSolid());
    }
}
