package vsdk.toolkit.gui;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.gui.gizmo.TranslateGizmo;
import vsdk.toolkit.gui.gizmo.TranslateGizmoInteractionTechnique;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
Exercises the processing of mouse and keyboard events over a translate gizmo,
independently of any rendering technology.
 */
class TranslateGizmoInteractionTechniqueTest
{
    private static final double EPS = 1.0e-6;

    private static Camera createCamera()
    {
        Camera camera = new Camera();

        camera.setProjectionMode(Camera.PROJECTION_MODE_PERSPECTIVE);
        camera.updateViewportResize(400, 400);
        camera.setPosition(new Vector3Dd(5, -6, 4));
        camera.setFocusedPositionMaintainingOrthogonality(new Vector3Dd(0, 0, 0));
        camera.updateVectors();
        return camera;
    }

    private static TranslateGizmo createGizmo(Camera camera)
    {
        TranslateGizmo gizmo = new TranslateGizmo(camera);

        gizmo.setTransformationMatrix(new Matrix4x4d());
        return gizmo;
    }

    private static MouseEvent mouseEvent(int x, int y)
    {
        MouseEvent event = new MouseEvent();

        event.setX(x);
        event.setY(y);
        return event;
    }

    private static MouseEvent mouseEventAt(Camera camera, Vector3Dd point)
    {
        Vector3Dd pixel = camera.projectPointUsingRayMethod(point);

        return mouseEvent((int)Math.round(pixel.x()), (int)Math.round(pixel.y()));
    }

    private static KeyEvent keyEvent(char unicode)
    {
        KeyEvent event = new KeyEvent();

        event.unicode_id = unicode;
        return event;
    }

    @Test
    void given_upperCaseXKey_when_keyPressed_then_gizmoMovesForwardInX()
    {
        // Arrange
        TranslateGizmo gizmo = createGizmo(createCamera());
        TranslateGizmoInteractionTechnique technique =
            new TranslateGizmoInteractionTechnique(gizmo);

        // Act
        boolean changed = technique.processKeyPressedEvent(keyEvent('X'));

        // Assert
        assertThat(changed).isTrue();
        assertThat(gizmo.getPosition().x()).isCloseTo(0.1, offset(EPS));
        assertThat(gizmo.getPosition().y()).isCloseTo(0.0, offset(EPS));
        assertThat(gizmo.getPosition().z()).isCloseTo(0.0, offset(EPS));
    }

    @Test
    void given_lowerCaseZKey_when_keyPressed_then_gizmoMovesBackwardInZ()
    {
        // Arrange
        TranslateGizmo gizmo = createGizmo(createCamera());
        TranslateGizmoInteractionTechnique technique =
            new TranslateGizmoInteractionTechnique(gizmo);

        // Act
        boolean changed = technique.processKeyPressedEvent(keyEvent('z'));

        // Assert
        assertThat(changed).isTrue();
        assertThat(gizmo.getPosition().z()).isCloseTo(-0.1, offset(EPS));
    }

    @Test
    void given_unrelatedKey_when_keyPressed_then_gizmoDoesNotChange()
    {
        // Arrange
        TranslateGizmo gizmo = createGizmo(createCamera());
        TranslateGizmoInteractionTechnique technique =
            new TranslateGizmoInteractionTechnique(gizmo);

        // Act
        boolean changed = technique.processKeyPressedEvent(keyEvent('q'));

        // Assert
        assertThat(changed).isFalse();
        assertThat(gizmo.getPosition().length()).isCloseTo(0.0, offset(EPS));
    }

    @Test
    void given_cursorOverYAxis_when_mouseClicked_then_yAxisGroupIsSelected()
    {
        // Arrange
        Camera camera = createCamera();
        TranslateGizmo gizmo = createGizmo(camera);
        TranslateGizmoInteractionTechnique technique =
            new TranslateGizmoInteractionTechnique(gizmo);
        Vector3Dd onYAxis = new Vector3Dd(0, gizmo.getCurrentScale()*0.55, 0);

        // Act
        boolean changed = technique.processMouseClickedEvent(mouseEventAt(camera, onYAxis));

        // Assert
        assertThat(changed).isTrue();
        assertThat(gizmo.getPersistentSelection()).isEqualTo(TranslateGizmo.Y_AXIS_GROUP);
        assertThat(technique.isActive()).isTrue();
    }

    @Test
    void given_cursorOutsideGizmo_when_mouseMoved_then_isNotActive()
    {
        // Arrange
        Camera camera = createCamera();
        TranslateGizmo gizmo = createGizmo(camera);
        TranslateGizmoInteractionTechnique technique =
            new TranslateGizmoInteractionTechnique(gizmo);

        // Act
        technique.processMouseMovedEvent(mouseEvent(2, 2));

        // Assert
        assertThat(technique.isActive()).isFalse();
        assertThat(gizmo.getVolatileSelection()).isEqualTo(TranslateGizmo.NULL_GROUP);
    }

    @Test
    void given_xAxisSelected_when_mouseDragged_then_gizmoMovesOnlyAlongX()
    {
        // Arrange
        Camera camera = createCamera();
        TranslateGizmo gizmo = createGizmo(camera);
        TranslateGizmoInteractionTechnique technique =
            new TranslateGizmoInteractionTechnique(gizmo);
        Vector3Dd start = new Vector3Dd(0.5, 0, 0);
        Vector3Dd end = new Vector3Dd(1.5, 0, 0);

        // Act
        technique.processMousePressedEvent(mouseEventAt(camera, start));
        boolean changed = technique.processMouseDraggedEvent(mouseEventAt(camera, end));

        // Assert
        assertThat(changed).isTrue();
        assertThat(gizmo.getPosition().x()).isCloseTo(1.0, offset(0.05));
        assertThat(gizmo.getPosition().y()).isCloseTo(0.0, offset(EPS));
        assertThat(gizmo.getPosition().z()).isCloseTo(0.0, offset(EPS));
        assertThat(gizmo.isSelectedResizing()).isFalse();
    }

    @Test
    void given_xAxisDrag_when_cursorSweepsHorizontallyWithRepeatedSamples_then_movementIsSmooth()
    {
        // Arrange: regression of the plane flip-flop that depended on the
        // last cursor delta (repeated samples or leftward moves jumped)
        Camera camera = createCamera();
        TranslateGizmo gizmo = createGizmo(camera);
        TranslateGizmoInteractionTechnique technique =
            new TranslateGizmoInteractionTechnique(gizmo);
        Vector3Dd start = new Vector3Dd(0.5, 0, 0);
        MouseEvent startEvent = mouseEventAt(camera, start);
        int[] sweep = {0, 1, 1, 0, 2, 0, 0, 1, -1, -1, 0, -2, 1, 1, 0, 3, 3, 0};

        technique.processMousePressedEvent(startEvent);

        // Act
        int x = startEvent.getX();
        double previousX = gizmo.getPosition().x();
        double maxStep = 0;

        for ( int delta : sweep ) {
            x += delta;
            technique.processMouseDraggedEvent(mouseEvent(x, startEvent.getY()));
            double currentX = gizmo.getPosition().x();

            assertThat(Double.isNaN(currentX)).isFalse();
            if ( delta == 0 ) {
                assertThat(currentX).isCloseTo(previousX, offset(EPS));
            }
            maxStep = Math.max(maxStep, Math.abs(currentX - previousX));
            previousX = currentX;
        }

        // Assert: a step of at most 3 pixels is a small displacement
        assertThat(maxStep).isLessThan(0.2);
    }

    @Test
    void given_mouseReleased_when_processed_then_resizingIsRestored()
    {
        // Arrange
        TranslateGizmo gizmo = createGizmo(createCamera());
        TranslateGizmoInteractionTechnique technique =
            new TranslateGizmoInteractionTechnique(gizmo);

        gizmo.setSelectedResizing(false);

        // Act
        boolean changed = technique.processMouseReleasedEvent(mouseEvent(10, 10));

        // Assert
        assertThat(changed).isTrue();
        assertThat(gizmo.isSelectedResizing()).isTrue();
    }
}
