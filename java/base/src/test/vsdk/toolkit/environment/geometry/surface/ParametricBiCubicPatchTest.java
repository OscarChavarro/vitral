package vsdk.toolkit.environment.geometry.surface;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.curve.ParametricCurve;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.element.RayHit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
Exercises the evaluation of bicubic patches. `Vector3Dd` is immutable, so the
point must be the returned value of `evaluate` (the former form, which wrote
it into a given vector, returned always the origin).
 */
class ParametricBiCubicPatchTest
{
    private static final double EPS = 1.0e-9;

    @Test
    void given_planarBilinearBezierMesh_when_evaluated_then_matchesTheTypeScriptPort()
    {
        // Arrange
        Vector3Dd[][] points = new Vector3Dd[4][4];
        for ( int i = 0; i < 4; i++ ) {
            for ( int j = 0; j < 4; j++ ) {
                points[i][j] = new Vector3Dd(i / 3.0, j / 3.0, 0);
            }
        }
        ParametricBiCubicPatch patch = new ParametricBiCubicPatch();
        patch.buildBezierPatch(points);

        // Act
        Vector3Dd position = patch.evaluate(0.5, 0.5);
        Vector3Dd tangent = patch.evaluateTangent(0.5, 0.5);
        Vector3Dd binormal = patch.evaluateBinormal(0.5, 0.5);
        Vector3Dd normal = patch.evaluateNormal(0.5, 0.5);

        // Assert
        assertThat(position.x()).isCloseTo(0.5, offset(EPS));
        assertThat(position.y()).isCloseTo(0.5, offset(EPS));
        assertThat(position.z()).isCloseTo(0.0, offset(EPS));
        assertThat(tangent.x()).isCloseTo(1.0, offset(EPS));
        assertThat(binormal.y()).isCloseTo(1.0, offset(EPS));
        assertThat(normal.z()).isCloseTo(1.0, offset(EPS));
    }

    @Test
    @SuppressWarnings("deprecation")
    void given_fergusonPatch_when_evaluatedAtItsCorners_then_givesTheContourPoints()
    {
        // Arrange: the patch created by the scene editor
        ParametricBiCubicPatch patch = new ParametricBiCubicPatch();
        patch.buildFergusonPatch(buildEditorContour());

        // Act
        Vector3Dd p00 = patch.evaluate(0, 0);
        Vector3Dd p10 = patch.evaluate(1, 0);
        Vector3Dd p11 = patch.evaluate(1, 1);
        Vector3Dd p01 = patch.evaluate(0, 1);
        Vector3Dd legacy = patch.evaluate(new Vector3Dd(), 1, 1);

        // Assert
        assertThat(p00.subtract(new Vector3Dd(0, 0, 0)).length()).isLessThan(EPS);
        assertThat(p10.subtract(new Vector3Dd(1, 0, 0)).length()).isLessThan(EPS);
        assertThat(p11.subtract(new Vector3Dd(1, 1, 0.4)).length()).isLessThan(EPS);
        assertThat(p01.subtract(new Vector3Dd(0, 1, 0)).length()).isLessThan(EPS);
        assertThat(legacy).isEqualTo(p11);
    }

    @Test
    void given_planarBezierPatch_when_rayHitsItsInterior_then_reportsExactHitData()
    {
        // Arrange
        ParametricBiCubicPatch patch = buildPlanarBezierPatch();
        Ray ray = new Ray(new Vector3Dd(0.3, 0.6, 5), new Vector3Dd(0, 0, -1));
        RayHit hit = new RayHit();

        // Act
        boolean intersects = patch.doIntersectionFirstHit(ray, hit);

        // Assert
        assertThat(intersects).isTrue();
        assertThat(hit.getHitDistance()).isCloseTo(5.0, offset(EPS));
        assertThat(hit.point.subtract(new Vector3Dd(0.3, 0.6, 0)).length()).isLessThan(EPS);
        assertThat(hit.normal.z()).isCloseTo(1.0, offset(EPS));
        assertThat(hit.u).isCloseTo(0.3, offset(EPS));
        assertThat(hit.v).isCloseTo(0.6, offset(EPS));
        assertThat(hit.tangent.x()).isCloseTo(1.0, offset(EPS));
    }

    @Test
    void given_planarBezierPatch_when_rayMissesOrPointsAway_then_reportsNoHit()
    {
        // Arrange
        ParametricBiCubicPatch patch = buildPlanarBezierPatch();
        Ray outside = new Ray(new Vector3Dd(1.5, 0.5, 5), new Vector3Dd(0, 0, -1));
        Ray away = new Ray(new Vector3Dd(0.5, 0.5, 5), new Vector3Dd(0, 0, 1));
        Ray parallel = new Ray(new Vector3Dd(-1, 0.5, 1), new Vector3Dd(1, 0, 0));

        // Act / Assert
        assertThat(patch.doIntersectionFirstHit(outside, new RayHit())).isFalse();
        assertThat(patch.doIntersectionFirstHit(away, new RayHit())).isFalse();
        assertThat(patch.doIntersectionFirstHit(parallel, new RayHit())).isFalse();
        assertThat(patch.doIntersectionFirstHit(outside)).isNull();
    }

    @Test
    void given_bezierPatch_when_askedForBezierNet_then_returnsItsControlPoints()
    {
        // Arrange
        Vector3Dd[][] points = buildDomeNet();
        ParametricBiCubicPatch patch = new ParametricBiCubicPatch();
        patch.buildBezierPatch(points);

        // Act / Assert
        for ( int i = 0; i < 4; i++ ) {
            for ( int j = 0; j < 4; j++ ) {
                assertThat(patch.getBezierControlPoint(i, j)
                    .subtract(points[i][j]).length()).isLessThan(EPS);
            }
        }
    }

    @Test
    void given_curvedPatches_when_raysAimAtSurfacePoints_then_hitsLieOnTheSurface()
    {
        // Arrange
        ParametricBiCubicPatch dome = new ParametricBiCubicPatch();
        dome.buildBezierPatch(buildDomeNet());
        ParametricBiCubicPatch ferguson = new ParametricBiCubicPatch();
        ferguson.buildFergusonPatch(buildEditorContour());
        ParametricBiCubicPatch[] patches = { dome, ferguson };
        Vector3Dd[] eyes = {
            new Vector3Dd(0.3, -0.2, 4), new Vector3Dd(-3, 2, 1.5),
            new Vector3Dd(2, 3, -2)
        };

        for ( ParametricBiCubicPatch patch : patches ) {
            for ( Vector3Dd eye : eyes ) {
                for ( int a = 1; a < 10; a++ ) {
                    for ( int b = 1; b < 10; b++ ) {
                        Vector3Dd target = patch.evaluate(a / 10.0, b / 10.0);
                        Vector3Dd direction = target.subtract(eye).normalized();
                        double targetDistance = target.subtract(eye).length();
                        RayHit hit = new RayHit();

                        // Act
                        boolean intersects = patch.doIntersectionFirstHit(
                            new Ray(eye, direction), hit);

                        // Assert: the nearest hit is on the surface, not
                        // beyond the aimed point, and its (u, v) are coherent
                        assertThat(intersects).isTrue();
                        assertThat(hit.getHitDistance())
                            .isLessThanOrEqualTo(targetDistance + 1.0e-6);
                        Vector3Dd onSurface = patch.evaluate(hit.u, hit.v);
                        assertThat(onSurface.subtract(hit.point).length())
                            .isLessThan(1.0e-6);
                        assertThat(hit.normal.dotProduct(direction))
                            .isLessThanOrEqualTo(0.0);
                    }
                }
            }
        }
    }

    @Test
    void given_planarAndCurvedPatches_when_rayLeavesFromTheSurface_then_doesNotHitItself()
    {
        // Arrange: shadow rays as built by LightingShader, with an origin
        // offset of VSDK.EPSILON along the light direction
        ParametricBiCubicPatch dome = new ParametricBiCubicPatch();
        dome.buildBezierPatch(buildDomeNet());
        Vector3Dd lightDirection = new Vector3Dd(-0.1, 0.05, 1).normalized();

        for ( int a = 0; a <= 20; a++ ) {
            for ( int b = 0; b <= 20; b++ ) {
                Vector3Dd p = dome.evaluate(a / 20.0, b / 20.0);
                Vector3Dd origin = p.add(lightDirection.multiply(1.0e-6));
                RayHit hit = new RayHit(RayHit.DETAIL_NONE, false);

                // Act
                boolean intersects = dome.doIntersectionFirstHit(
                    new Ray(origin, lightDirection), hit);

                // Assert
                assertThat(intersects)
                    .as("self hit from (%d, %d) at distance %s", a, b,
                        hit.getHitDistance())
                    .isFalse();
            }
        }
    }

    @Test
    void given_fergusonPatch_when_minMaxRequested_then_containsSampledSurface()
    {
        // Arrange
        ParametricBiCubicPatch patch = new ParametricBiCubicPatch();
        patch.buildFergusonPatch(buildEditorContour());

        // Act
        double[] minMax = patch.getMinMax();

        // Assert
        for ( int a = 0; a <= 20; a++ ) {
            for ( int b = 0; b <= 20; b++ ) {
                Vector3Dd p = patch.evaluate(a / 20.0, b / 20.0);
                assertThat(p.x()).isBetween(minMax[0] - EPS, minMax[3] + EPS);
                assertThat(p.y()).isBetween(minMax[1] - EPS, minMax[4] + EPS);
                assertThat(p.z()).isBetween(minMax[2] - EPS, minMax[5] + EPS);
            }
        }
    }

    private static ParametricBiCubicPatch buildPlanarBezierPatch()
    {
        Vector3Dd[][] points = new Vector3Dd[4][4];
        for ( int i = 0; i < 4; i++ ) {
            for ( int j = 0; j < 4; j++ ) {
                points[i][j] = new Vector3Dd(i / 3.0, j / 3.0, 0);
            }
        }
        ParametricBiCubicPatch patch = new ParametricBiCubicPatch();
        patch.buildBezierPatch(points);
        return patch;
    }

    private static Vector3Dd[][] buildDomeNet()
    {
        Vector3Dd[][] points = new Vector3Dd[4][4];
        for ( int i = 0; i < 4; i++ ) {
            for ( int j = 0; j < 4; j++ ) {
                boolean inner = (i == 1 || i == 2) && (j == 1 || j == 2);
                points[i][j] = new Vector3Dd(
                    i / 3.0 + 0.1 * Math.sin(j), j / 3.0, inner ? 1.2 : 0.2 * i);
            }
        }
        return points;
    }

    private static ParametricCurve buildEditorContour()
    {
        ParametricCurve contour = new ParametricCurve();

        contour.addPoint(new Vector3Dd[] {
            new Vector3Dd(0, 0, 0), new Vector3Dd(0, -1, 0), new Vector3Dd(1, 0, 0)
        }, ParametricCurve.HERMITE);
        contour.addPoint(new Vector3Dd[] {
            new Vector3Dd(1, 0, 0), new Vector3Dd(1, 0, 0), new Vector3Dd(0, 1, 0)
        }, ParametricCurve.HERMITE);
        contour.addPoint(new Vector3Dd[] {
            new Vector3Dd(1, 1, 0.4), new Vector3Dd(0, 1, 0), new Vector3Dd(-1, 0, 0)
        }, ParametricCurve.HERMITE);
        contour.addPoint(new Vector3Dd[] {
            new Vector3Dd(0, 1, 0), new Vector3Dd(-1, 0, 0), new Vector3Dd(0, -1, 0)
        }, ParametricCurve.HERMITE);
        contour.addPoint(contour.getPoint(0), ParametricCurve.HERMITE);
        return contour;
    }
}
