package vsdk.toolkit.environment.geometry.surface;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.curve.ParametricCurve;

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
