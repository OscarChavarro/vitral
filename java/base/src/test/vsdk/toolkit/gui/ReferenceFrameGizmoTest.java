package vsdk.toolkit.gui;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.gui.gizmo.ReferenceFrameGizmo;
import vsdk.toolkit.gui.viewport.ViewportElementScaler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.offset;

/**
Exercises the rendering independent model and rotation estimation of the
reference frame gizmo.
 */
class ReferenceFrameGizmoTest
{
    private static final double EPS = 1.0e-9;

    @Test
    void given_newGizmo_when_queried_then_hasDefaultModel()
    {
        // Arrange / Act
        ReferenceFrameGizmo gizmo = new ReferenceFrameGizmo();

        // Assert
        assertThat(gizmo.isVisible()).isTrue();
        assertThat(gizmo.getSizeInPixels()).isEqualTo(ReferenceFrameGizmo.DEFAULT_SIZE_IN_PIXELS);
        assertThat(gizmo.getAxisLabel(ReferenceFrameGizmo.AXIS_X)).isEqualTo("X");
        assertThat(gizmo.getAxisLabel(ReferenceFrameGizmo.AXIS_Y)).isEqualTo("Y");
        assertThat(gizmo.getAxisLabel(ReferenceFrameGizmo.AXIS_Z)).isEqualTo("Z");
        assertThat(gizmo.getAxisEnd(ReferenceFrameGizmo.AXIS_Y).y()).isEqualTo(gizmo.getAxisLength());
    }

    @Test
    void given_notPositiveValues_when_set_then_areIgnored()
    {
        // Arrange
        ReferenceFrameGizmo gizmo = new ReferenceFrameGizmo();

        // Act
        gizmo.setSizeInPixels(0);
        gizmo.setAxisLength(-1);

        // Assert
        assertThat(gizmo.getSizeInPixels()).isEqualTo(ReferenceFrameGizmo.DEFAULT_SIZE_IN_PIXELS);
        assertThat(gizmo.getAxisLength()).isEqualTo(ReferenceFrameGizmo.DEFAULT_AXIS_LENGTH);
        assertThatThrownBy(() -> gizmo.getAxisColor(3)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void given_identityCameraRotation_when_estimateAxisDirection_then_followsGizmoConvention()
    {
        // Arrange
        ReferenceFrameGizmo gizmo = new ReferenceFrameGizmo();
        Matrix4x4d identity = Matrix4x4d.identityMatrix();

        // Act
        Vector3Dd x = gizmo.estimateAxisDirection(ReferenceFrameGizmo.AXIS_X, identity);
        Vector3Dd y = gizmo.estimateAxisDirection(ReferenceFrameGizmo.AXIS_Y, identity);
        Vector3Dd z = gizmo.estimateAxisDirection(ReferenceFrameGizmo.AXIS_Z, identity);

        // Assert
        assertVector(x, 0, 0, -1);
        assertVector(y, -1, 0, 0);
        assertVector(z, 0, 1, 0);
    }

    @Test
    void given_rotatedCamera_when_estimateOrientation_then_axesRemainOrthonormal()
    {
        // Arrange
        ReferenceFrameGizmo gizmo = new ReferenceFrameGizmo();
        Matrix4x4d cameraRotation = new Matrix4x4d().eulerAnglesRotation(
            Math.toRadians(35), Math.toRadians(-20), 0);

        // Act
        Vector3Dd x = gizmo.estimateAxisDirection(ReferenceFrameGizmo.AXIS_X, cameraRotation);
        Vector3Dd y = gizmo.estimateAxisDirection(ReferenceFrameGizmo.AXIS_Y, cameraRotation);
        Vector3Dd z = gizmo.estimateAxisDirection(ReferenceFrameGizmo.AXIS_Z, cameraRotation);

        // Assert
        assertThat(x.dotProduct(y)).isCloseTo(0, offset(EPS));
        assertThat(y.dotProduct(z)).isCloseTo(0, offset(EPS));
        assertThat(x.dotProduct(z)).isCloseTo(0, offset(EPS));
        assertThat(x.crossProduct(y).dotProduct(z)).isCloseTo(1, offset(EPS));
    }

    @Test
    void given_screenResolutions_when_applyScale_then_sizeAndLineWidthFollowThem()
    {
        // Arrange
        ReferenceFrameGizmo legacy = new ReferenceFrameGizmo();
        ReferenceFrameGizmo large = new ReferenceFrameGizmo();
        ViewportElementScaler legacyScaler = new ViewportElementScaler();
        ViewportElementScaler largeScaler = new ViewportElementScaler();
        legacyScaler.setScreenResolution(1024, 768);
        largeScaler.setScreenResolution(2560, 1600);

        // Act
        legacy.applyScale(legacyScaler);
        large.applyScale(largeScaler);

        // Assert
        assertThat(legacy.getSizeInPixels()).isEqualTo(64);
        assertThat(legacy.getLineWidth()).isCloseTo(1.0, offset(EPS));
        assertThat(large.getSizeInPixels()).isEqualTo(128);
        assertThat(large.getLineWidth()).isCloseTo(2.0, offset(EPS));
    }

    @Test
    void given_axisAndLineWidth_when_buildAxisStrip_then_isRectangleOfThatWidth()
    {
        // Arrange
        ReferenceFrameGizmo gizmo = new ReferenceFrameGizmo();
        gizmo.setSizeInPixels(100);
        gizmo.setLineWidth(4);
        Matrix4x4d identity = Matrix4x4d.identityMatrix();

        // Act
        Vector3Dd[] strip = gizmo.buildAxisStrip(ReferenceFrameGizmo.AXIS_Y, identity);

        // Assert: 4 pixels of width are 4/50 canonical units (area of 100 pixels covers [-1, 1])
        assertThat(strip).hasSize(4);
        assertThat(Vector3Dd.distance(strip[0], strip[1])).isCloseTo(0.08, offset(EPS));
        assertThat(Vector3Dd.distance(strip[2], strip[3])).isCloseTo(0.08, offset(EPS));
        assertThat(strip[0].z()).isCloseTo(strip[1].z(), offset(EPS));
    }

    @Test
    void given_axisPointingToViewer_when_buildAxisStrip_then_isSquareDot()
    {
        // Arrange: with the identity camera rotation, the Y axis points along -X and Z along +Y
        // (see given_identityCameraRotation...), so X points along -Z: to the viewer
        ReferenceFrameGizmo gizmo = new ReferenceFrameGizmo();
        gizmo.setSizeInPixels(100);
        gizmo.setLineWidth(4);

        // Act
        Vector3Dd[] strip = gizmo.buildAxisStrip(ReferenceFrameGizmo.AXIS_X, Matrix4x4d.identityMatrix());

        // Assert
        assertThat(Vector3Dd.distance(strip[0], strip[1])).isCloseTo(0.08, offset(EPS));
        assertThat(Math.hypot(strip[2].x() - strip[0].x(), strip[2].y() - strip[0].y()))
            .isCloseTo(0.08, offset(EPS));
    }

    private static void assertVector(Vector3Dd v, double x, double y, double z)
    {
        assertThat(v.x()).isCloseTo(x, offset(EPS));
        assertThat(v.y()).isCloseTo(y, offset(EPS));
        assertThat(v.z()).isCloseTo(z, offset(EPS));
    }
}
