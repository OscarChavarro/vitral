package vsdk.toolkit.environment.geometry.volume;

import java.util.Random;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayHit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
Exercises the intersection of rays with the filled voxels of a volume (the
cube <-1, -1, -1>-<1, 1, 1>), traversed voxel by voxel.
 */
class VoxelVolumeTest
{
    private static final double EPS = 1.0e-9;
    private static final byte FILLED = (byte)-1;

    private static VoxelVolume createVolume(int n)
    {
        VoxelVolume volume = new VoxelVolume();
        volume.init(n, n, n);
        return volume;
    }

    @Test
    void given_oneFilledVoxel_when_rayEntersItsTopFace_then_hitsTheFaceWithItsNormal()
    {
        // Arrange: in a 4^3 volume the voxel (1, 1, 1) spans [-0.5, 0]^3
        VoxelVolume volume = createVolume(4);
        volume.putVoxel(1, 1, 1, FILLED);
        Ray ray = new Ray(new Vector3Dd(-0.25, -0.25, 5), new Vector3Dd(0, 0, -1));
        RayHit hit = new RayHit(RayHit.DETAIL_ALL, true);

        // Act
        boolean intersects = volume.doIntersectionFirstHit(ray, hit);

        // Assert
        assertThat(intersects).isTrue();
        assertThat(hit.getRay().getT()).isCloseTo(5.0, offset(EPS));
        assertThat(hit.normal.x()).isCloseTo(0.0, offset(EPS));
        assertThat(hit.normal.y()).isCloseTo(0.0, offset(EPS));
        assertThat(hit.normal.z()).isCloseTo(1.0, offset(EPS));
        assertThat(hit.point.z()).isCloseTo(0.0, offset(EPS));
    }

    @Test
    void given_oneFilledVoxel_when_rayPassesBesideIt_then_missesTheVolume()
    {
        // Arrange
        VoxelVolume volume = createVolume(4);
        volume.putVoxel(1, 1, 1, FILLED);
        Ray ray = new Ray(new Vector3Dd(0.25, -0.25, 5), new Vector3Dd(0, 0, -1));

        // Act & Assert
        assertThat(volume.doIntersectionFirstHit(ray, new RayHit())).isFalse();
    }

    @Test
    void given_fullVolume_when_rayComesFromOutside_then_hitsTheFaceOfTheCube()
    {
        // Arrange
        VoxelVolume volume = createVolume(8);
        fill(volume);
        Ray ray = new Ray(new Vector3Dd(-5, 0.3, -0.2), new Vector3Dd(1, 0, 0));
        RayHit hit = new RayHit(RayHit.DETAIL_ALL, true);

        // Act
        boolean intersects = volume.doIntersectionFirstHit(ray, hit);

        // Assert
        assertThat(intersects).isTrue();
        assertThat(hit.getRay().getT()).isCloseTo(4.0, offset(EPS));
        assertThat(hit.normal.x()).isCloseTo(-1.0, offset(EPS));
    }

    @Test
    void given_rayStartingInsideFilledVoxels_when_intersected_then_onlyEnteredVoxelsAreHit()
    {
        // Arrange: two filled voxels of the row y = z = 0 of a 4^3 volume,
        // x = 0 spans [-1, -0.5] and x = 3 spans [0.5, 1]
        VoxelVolume volume = createVolume(4);
        volume.putVoxel(0, 0, 0, FILLED);
        volume.putVoxel(3, 0, 0, FILLED);
        Ray ray = new Ray(new Vector3Dd(-0.75, -0.75, -0.75), new Vector3Dd(1, 0, 0));
        RayHit hit = new RayHit(RayHit.DETAIL_ALL, true);

        // Act
        boolean intersects = volume.doIntersectionFirstHit(ray, hit);

        // Assert
        assertThat(intersects).isTrue();
        assertThat(hit.getRay().getT()).isCloseTo(1.25, offset(EPS));
        assertThat(hit.normal.x()).isCloseTo(-1.0, offset(EPS));
    }

    @Test
    void given_volumeBelowThreshold_when_intersected_then_missesTheVolume()
    {
        // Arrange
        VoxelVolume volume = createVolume(4);
        volume.putVoxel(1, 1, 1, (byte)100);
        Ray ray = new Ray(new Vector3Dd(-0.25, -0.25, 5), new Vector3Dd(0, 0, -1));

        // Act & Assert
        assertThat(volume.doIntersectionFirstHit(ray, new RayHit())).isFalse();
        volume.setThreshold(50);
        assertThat(volume.doIntersectionFirstHit(ray, new RayHit())).isTrue();
    }

    @Test
    void given_randomVolumeAndRays_when_traversed_then_firstHitMatchesTheNearestFilledVoxel()
    {
        // Arrange
        Random random = new Random(1234);
        int n = 7;
        VoxelVolume volume = createVolume(n);
        for ( int z = 0; z < n; z++ ) {
            for ( int y = 0; y < n; y++ ) {
                for ( int x = 0; x < n; x++ ) {
                    if ( random.nextDouble() < 0.08 ) {
                        volume.putVoxel(x, y, z, FILLED);
                    }
                }
            }
        }

        for ( int i = 0; i < 2000; i++ ) {
            // Rays from outside the volume cube, aimed at a point inside it
            Vector3Dd origin = randomDirection(random).multiply(4.0);
            Vector3Dd target = new Vector3Dd(random.nextDouble() * 2 - 1,
                random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1);
            Vector3Dd direction = target.subtract(origin).normalized();
            Ray ray = new Ray(origin, direction);
            RayHit hit = new RayHit(RayHit.DETAIL_NONE, true);

            // Act
            boolean intersects = volume.doIntersectionFirstHit(ray, hit);
            double expected = bruteForceFirstHit(volume, origin, direction);

            // Assert
            if ( Double.isInfinite(expected) ) {
                assertThat(intersects).as("ray %d", i).isFalse();
            }
            else {
                assertThat(intersects).as("ray %d", i).isTrue();
                assertThat(hit.getRay().getT()).as("ray %d", i)
                    .isCloseTo(expected, offset(1.0e-7));
            }
        }
    }

    private static void fill(VoxelVolume volume)
    {
        for ( int z = 0; z < volume.getZSize(); z++ ) {
            for ( int y = 0; y < volume.getYSize(); y++ ) {
                for ( int x = 0; x < volume.getXSize(); x++ ) {
                    volume.putVoxel(x, y, z, FILLED);
                }
            }
        }
    }

    private static Vector3Dd randomDirection(Random random)
    {
        Vector3Dd v;
        do {
            v = new Vector3Dd(random.nextDouble() * 2 - 1,
                random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1);
        } while ( v.length() < 0.1 || v.length() > 1 );
        return v.normalized();
    }

    /**
    @return the smallest entry distance of the ray into the box of a filled
    voxel, or infinity
    */
    private static double bruteForceFirstHit(VoxelVolume volume,
        Vector3Dd origin, Vector3Dd direction)
    {
        double best = Double.POSITIVE_INFINITY;
        int n = volume.getXSize();
        double cell = 2.0 / n;
        double[] o = { origin.x(), origin.y(), origin.z() };
        double[] d = { direction.x(), direction.y(), direction.z() };

        for ( int z = 0; z < n; z++ ) {
            for ( int y = 0; y < n; y++ ) {
                for ( int x = 0; x < n; x++ ) {
                    if ( !volume.isFilled(x, y, z) ) {
                        continue;
                    }
                    int[] g = { x, y, z };
                    double tNear = Double.NEGATIVE_INFINITY;
                    double tFar = Double.POSITIVE_INFINITY;
                    for ( int a = 0; a < 3; a++ ) {
                        double lo = -1 + g[a] * cell;
                        double hi = lo + cell;
                        if ( Math.abs(d[a]) < 1e-15 ) {
                            if ( o[a] < lo || o[a] > hi ) {
                                tNear = Double.POSITIVE_INFINITY;
                            }
                            continue;
                        }
                        double t1 = (lo - o[a]) / d[a];
                        double t2 = (hi - o[a]) / d[a];
                        tNear = Math.max(tNear, Math.min(t1, t2));
                        tFar = Math.min(tFar, Math.max(t1, t2));
                    }
                    if ( tNear <= tFar && tNear >= 0 && tNear < best ) {
                        best = tNear;
                    }
                }
            }
        }
        return best;
    }
}
