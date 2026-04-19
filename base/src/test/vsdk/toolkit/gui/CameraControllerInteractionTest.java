package vsdk.toolkit.gui;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CameraControllerInteractionTest
{
    private static final double EPSILON = 1.0e-12;

    @Test
    void given_orbiterSecondaryDrag_when_rotating_then_cameraKeepsOriginalBasisAndAppliesSingleDelta()
    {
        Camera camera = new Camera();
        CameraControllerOrbiter controller = new CameraControllerOrbiter(camera);
        Matrix4x4 originalRotation = camera.getRotation();
        Vector3D frontAxis = extractColumn(originalRotation, 0);
        Matrix4x4 expectedRotation = new Matrix4x4()
            .axisRotation(0.05, frontAxis.x(), frontAxis.y(), frontAxis.z())
            .multiply(originalRotation);

        controller.processMousePressedEvent(mouseEvent(10, 10, 0));
        boolean updated = controller.processMouseDraggedEvent(
            mouseEvent(15, 10, MouseEvent.BUTTON3_DOWN_MASK));

        assertThat(updated).isTrue();
        assertMatrixEquals(camera.getRotation(), expectedRotation);
    }

    @Test
    void given_aquynzaPrimaryDrag_when_rotating_then_cameraAppliesBothDragDeltasOverOriginalRotation()
    {
        Camera camera = new Camera();
        CameraControllerAquynza controller = new CameraControllerAquynza(camera);
        Matrix4x4 originalRotation = camera.getRotation();
        Vector3D leftAxis = extractColumn(originalRotation, 1);
        Vector3D upAxis = extractColumn(originalRotation, 2);
        Matrix4x4 expectedRotation = new Matrix4x4()
            .axisRotation(-0.05, upAxis.x(), upAxis.y(), upAxis.z())
            .multiply(
                new Matrix4x4()
                    .axisRotation(0.03, leftAxis.x(), leftAxis.y(), leftAxis.z())
                    .multiply(originalRotation));

        controller.processMousePressedEvent(mouseEvent(10, 10, 0));
        boolean updated = controller.processMouseDraggedEvent(
            mouseEvent(15, 13, MouseEvent.BUTTON1_DOWN_MASK));

        assertThat(updated).isTrue();
        assertMatrixEquals(camera.getRotation(), expectedRotation);
    }

    @Test
    void given_aquynzaSecondaryAdvanceAfterPress_when_dragging_then_verticalDeltaUsesPressedYCoordinate()
    {
        Camera camera = new Camera();
        CameraControllerAquynza controller = new CameraControllerAquynza(camera);
        Vector3D originalPosition = camera.getPosition();
        Vector3D frontAxis = extractColumn(camera.getRotation(), 0);
        Vector3D expectedPosition = originalPosition.subtract(
            frontAxis.multiply(0.05));

        controller.processMousePressedEvent(mouseEvent(0, 100, 0));
        boolean updated = controller.processMouseDraggedEvent(
            mouseEvent(0, 101, MouseEvent.BUTTON3_DOWN_MASK));

        assertThat(updated).isTrue();
        assertVectorEquals(camera.getPosition(), expectedPosition);
    }

    private static MouseEvent mouseEvent(int x, int y, int modifiers)
    {
        MouseEvent event = new MouseEvent();
        event.setX(x);
        event.setY(y);
        event.setModifiers(modifiers);
        return event;
    }

    private static Vector3D extractColumn(Matrix4x4 matrix, int column)
    {
        return new Vector3D(matrix.get(0, column), matrix.get(1, column),
            matrix.get(2, column));
    }

    private static void assertMatrixEquals(Matrix4x4 actual, Matrix4x4 expected)
    {
        for ( int row = 0; row < 3; row++ ) {
            for ( int column = 0; column < 3; column++ ) {
                assertThat(actual.get(row, column))
                    .isCloseTo(expected.get(row, column), within(EPSILON));
            }
        }
    }

    private static void assertVectorEquals(Vector3D actual, Vector3D expected)
    {
        assertThat(actual.x()).isCloseTo(expected.x(), within(EPSILON));
        assertThat(actual.y()).isCloseTo(expected.y(), within(EPSILON));
        assertThat(actual.z()).isCloseTo(expected.z(), within(EPSILON));
    }
}
