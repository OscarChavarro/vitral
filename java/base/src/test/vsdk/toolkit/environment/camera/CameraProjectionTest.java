package vsdk.toolkit.environment.camera;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.gui.gizmo.TranslateGizmo;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
Exercises the projection of world points to viewport pixels, and its use to
give the translate gizmo a finite apparent size.
 */
class CameraProjectionTest
{
    private static Camera createOrthogonalCamera()
    {
        Camera camera = new Camera();

        camera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        camera.updateViewportResize(400, 400);
        camera.setPosition(new Vector3Dd(0, -5, 0));
        camera.setFocusedPositionMaintainingOrthogonality(new Vector3Dd(0, 5, 0));
        camera.updateVectors();
        return camera;
    }

    @Test
    void given_orthogonalCamera_when_projectingOrigin_then_returnsViewportCenter()
    {
        // Arrange
        Camera camera = createOrthogonalCamera();

        // Act
        Vector3Dd projected = camera.projectPointUsingRayMethod(new Vector3Dd(0, 0, 0));

        // Assert
        assertThat(projected).isNotNull();
        assertThat(projected.x()).isCloseTo(200.0, offset(1.0e-6));
        assertThat(projected.y()).isCloseTo(200.0, offset(1.0e-6));
    }

    @Test
    void given_orthogonalCamera_when_projectingTwoPoints_then_theyDifferInPixels()
    {
        // Arrange
        Camera camera = createOrthogonalCamera();
        Vector3Dd right = camera.getLeft().multiply(-1).normalized();

        // Act
        Vector3Dd a = camera.projectPointUsingRayMethod(new Vector3Dd(0, 0, 0));
        Vector3Dd b = camera.projectPointUsingRayMethod(right);

        // Assert
        assertThat(Vector3Dd.distance(a, b)).isGreaterThan(1.0);
    }

    @Test
    void given_translateGizmo_when_placedInView_then_hasFiniteApparentSize()
    {
        // Arrange
        Camera camera = createOrthogonalCamera();
        TranslateGizmo gizmo = new TranslateGizmo(camera);

        // Act
        gizmo.setTransformationMatrix(new Matrix4x4d());

        // Assert
        assertThat(gizmo.getElements3dsmax()).isNotEmpty();
        assertThat(gizmo.getCurrentScale()).isFinite().isPositive();
    }

    @Test
    void given_perspectiveCamera_when_projectingFocusedPoint_then_returnsViewportCenter()
    {
        // Arrange
        Camera camera = new Camera();

        camera.setProjectionMode(Camera.PROJECTION_MODE_PERSPECTIVE);
        camera.updateViewportResize(400, 300);
        camera.setPosition(new Vector3Dd(0, -5, 0));
        camera.setFocusedPositionMaintainingOrthogonality(new Vector3Dd(0, 0, 0));
        camera.updateVectors();

        // Act
        Vector3Dd projected = camera.projectPoint(new Vector3Dd(0, 0, 0));

        // Assert
        assertThat(projected).isNotNull();
        assertThat(projected.x()).isCloseTo(200.0, offset(1.0e-6));
        assertThat(projected.y()).isCloseTo(150.0, offset(1.0e-6));
    }

    @Test
    void given_perspectiveCamera_when_projectingPointOutsideViewport_then_returnsNull()
    {
        // Arrange
        Camera camera = new Camera();

        camera.setProjectionMode(Camera.PROJECTION_MODE_PERSPECTIVE);
        camera.updateViewportResize(400, 300);
        camera.setPosition(new Vector3Dd(0, -5, 0));
        camera.setFocusedPositionMaintainingOrthogonality(new Vector3Dd(0, 0, 0));
        camera.updateVectors();

        // Act
        Vector3Dd projected = camera.projectPoint(new Vector3Dd(1000, 0, 0));

        // Assert
        assertThat(projected).isNull();
    }
}
