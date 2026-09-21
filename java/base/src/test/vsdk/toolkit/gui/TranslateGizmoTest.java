package vsdk.toolkit.gui;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.gui.gizmo.TranslateGizmo;
import vsdk.toolkit.gui.viewport.ViewportElementScaler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
Exercises the size, line width and line geometry of the translate gizmo,
independently of any rendering technology.
 */
class TranslateGizmoTest
{
    private static final double EPS = 1.0e-6;

    private static Camera createPerspectiveCamera()
    {
        Camera camera = new Camera();

        camera.setProjectionMode(Camera.PROJECTION_MODE_PERSPECTIVE);
        camera.updateViewportResize(400, 400);
        camera.setPosition(new Vector3Dd(5, -6, 4));
        camera.setFocusedPositionMaintainingOrthogonality(new Vector3Dd(0, 0, 0));
        camera.updateVectors();
        return camera;
    }

    private static ViewportElementScaler createScaler(int width, int height)
    {
        ViewportElementScaler scaler = new ViewportElementScaler();

        scaler.setScreenResolution(width, height);
        return scaler;
    }

    @Test
    void given_legacyResolution_when_applyScale_then_keepsDesignedSizeAndWidth()
    {
        // Arrange
        TranslateGizmo gizmo = new TranslateGizmo(createPerspectiveCamera());

        // Act
        gizmo.applyScale(createScaler(1024, 768));

        // Assert
        assertThat(gizmo.getAparentSizeInPixels()).isEqualTo(100);
        assertThat(gizmo.getLineWidth()).isCloseTo(1.0, offset(EPS));
    }

    @Test
    void given_highResolution_when_applyScale_then_sizeAndWidthAreDoubled()
    {
        // Arrange
        TranslateGizmo gizmo = new TranslateGizmo(createPerspectiveCamera());

        // Act
        gizmo.applyScale(createScaler(2560, 1600));

        // Assert
        assertThat(gizmo.getAparentSizeInPixels()).isEqualTo(200);
        assertThat(gizmo.getLineWidth()).isCloseTo(2.0, offset(EPS));
    }

    @Test
    void given_userChosenBaseSize_when_applyScaleRepeatedly_then_baseSizeIsKept()
    {
        // Arrange
        TranslateGizmo gizmo = new TranslateGizmo(createPerspectiveCamera());
        ViewportElementScaler scaler = createScaler(2560, 1600);

        // Act
        gizmo.setBaseAparentSizeInPixels(150);
        gizmo.applyScale(scaler);
        gizmo.applyScale(scaler);

        // Assert
        assertThat(gizmo.getBaseAparentSizeInPixels()).isEqualTo(150);
        assertThat(gizmo.getAparentSizeInPixels()).isEqualTo(300);
    }

    @Test
    void given_invalidValues_when_set_then_areIgnored()
    {
        // Arrange
        TranslateGizmo gizmo = new TranslateGizmo(createPerspectiveCamera());

        // Act
        gizmo.setLineWidth(0);
        gizmo.setLineWidth(-1);
        gizmo.setBaseAparentSizeInPixels(0);

        // Assert
        assertThat(gizmo.getLineWidth()).isCloseTo(1.0, offset(EPS));
        assertThat(gizmo.getBaseAparentSizeInPixels()).isEqualTo(100);
    }

    @Test
    void given_gizmoInView_when_gettingLines_then_axesAndPlaneSegmentsAreGiven()
    {
        // Arrange
        Camera camera = createPerspectiveCamera();
        TranslateGizmo gizmo = new TranslateGizmo(camera);

        // Act
        gizmo.setTransformationMatrix(new Matrix4x4d());
        ArrayList<TranslateGizmo.LineSegment> lines = gizmo.getLineSegments();

        // Assert: 3 axis shafts and 6 plane handle segments
        assertThat(lines).hasSize(9);
        for ( TranslateGizmo.LineSegment line : lines ) {
            assertThat(Vector3Dd.distance(line.getStart(), line.getEnd())).isGreaterThan(0.0);
        }
    }

    @Test
    void given_lineWidthInPixels_when_buildingStrip_then_widthMatchesPixelsAndFacesCamera()
    {
        // Arrange
        Camera camera = createPerspectiveCamera();
        TranslateGizmo gizmo = new TranslateGizmo(camera);

        gizmo.applyScale(createScaler(2560, 1600));
        gizmo.setTransformationMatrix(new Matrix4x4d());
        TranslateGizmo.LineSegment line = gizmo.getLineSegments().get(0);

        // Act
        Vector3Dd[] strip = gizmo.buildLineStrip(line);

        // Assert
        Vector3Dd across = strip[0].subtract(strip[1]);
        Vector3Dd along = line.getEnd().subtract(line.getStart()).normalized();
        double pixelsPerUnit = gizmo.getAparentSizeInPixels() / gizmo.getCurrentScale();
        Vector3Dd toEye = camera.getPosition().subtract(line.getStart());

        assertThat(strip).hasSize(4);
        assertThat(across.length() * pixelsPerUnit).isCloseTo(gizmo.getLineWidth(), offset(EPS));
        assertThat(across.dotProduct(along)).isCloseTo(0.0, offset(EPS));
        assertThat(across.normalized().dotProduct(toEye.normalized())).isCloseTo(0.0, offset(EPS));
    }

    @Test
    void given_zeroLengthLine_when_buildingStrip_then_returnsNull()
    {
        // Arrange
        TranslateGizmo gizmo = new TranslateGizmo(createPerspectiveCamera());
        gizmo.setTransformationMatrix(new Matrix4x4d());
        TranslateGizmo.LineSegment line = gizmo.getLineSegments().get(0);

        // Act
        TranslateGizmo.LineSegment degenerate =
            new TranslateGizmo.LineSegment(line.getStart(), line.getStart(), line.getColor());
        Vector3Dd[] strip = gizmo.buildLineStrip(degenerate);

        // Assert
        assertThat(strip).isNull();
    }
}
