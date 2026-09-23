package vsdk.toolkit.gui;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.gui.gizmo.ScaleGizmo;
import vsdk.toolkit.gui.gizmo.ScaleGizmoInteractionTechnique;
import vsdk.toolkit.gui.viewport.Viewport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
Exercises the processing of mouse and keyboard events over a scale gizmo,
independently of any rendering technology.
 */
class ScaleGizmoInteractionTechniqueTest
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

    private static ScaleGizmo createGizmo(Camera camera)
    {
        ScaleGizmo gizmo = new ScaleGizmo(camera);

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

    private static MouseEvent mouseEventOverPoint(Camera camera, Vector3Dd point)
    {
        Vector3Dd pixel = camera.projectPointUsingRayMethod(point);

        return mouseEvent((int)Math.round(pixel.x()), (int)Math.round(pixel.y()));
    }

    @Test
    void given_cursorOverAnAxis_when_moved_then_becomesTheVolatileSelection()
    {
        // Arrange
        Camera camera = createCamera();
        ScaleGizmo gizmo = createGizmo(camera);
        ScaleGizmoInteractionTechnique technique = new ScaleGizmoInteractionTechnique(gizmo);

        // Act
        boolean changed = technique.processMouseMovedEvent(
            mouseEventOverPoint(camera, gizmo.getTipPosition(1)));

        // Assert
        assertThat(changed).isTrue();
        assertThat(technique.isActive()).isTrue();
        assertThat(gizmo.getVolatileSelection()).isEqualTo(ScaleGizmo.Y_AXIS_GROUP);
        assertThat(gizmo.getCurrentSelection()).isEqualTo(ScaleGizmo.Y_AXIS_GROUP);
    }

    @Test
    void given_cursorOverNoHandle_when_moved_then_volatileSelectionIsCleared()
    {
        // Arrange
        Camera camera = createCamera();
        ScaleGizmo gizmo = createGizmo(camera);
        ScaleGizmoInteractionTechnique technique = new ScaleGizmoInteractionTechnique(gizmo);

        technique.processMouseMovedEvent(mouseEventOverPoint(camera, gizmo.getTipPosition(0)));

        // Act
        technique.processMouseMovedEvent(mouseEvent(5, 5));

        // Assert
        assertThat(technique.isActive()).isFalse();
        assertThat(gizmo.getVolatileSelection()).isEqualTo(ScaleGizmo.NULL_GROUP);
    }

    @Test
    void given_cursorOverAHandle_when_clicked_then_becomesThePersistentSelection()
    {
        // Arrange
        Camera camera = createCamera();
        ScaleGizmo gizmo = createGizmo(camera);
        ScaleGizmoInteractionTechnique technique = new ScaleGizmoInteractionTechnique(gizmo);

        // Act
        boolean changed = technique.processMouseClickedEvent(
            mouseEventOverPoint(camera, gizmo.getTipPosition(2)));

        // Assert
        assertThat(changed).isTrue();
        assertThat(gizmo.getPersistentSelection()).isEqualTo(ScaleGizmo.Z_AXIS_GROUP);
    }

    @Test
    void given_clickOverNoHandle_when_processed_then_keepsThePreviousSelection()
    {
        // Arrange
        Camera camera = createCamera();
        ScaleGizmo gizmo = createGizmo(camera);
        ScaleGizmoInteractionTechnique technique = new ScaleGizmoInteractionTechnique(gizmo);

        gizmo.setPersistentSelection(ScaleGizmo.X_AXIS_GROUP);

        // Act
        boolean changed = technique.processMouseClickedEvent(mouseEvent(5, 5));

        // Assert
        assertThat(changed).isFalse();
        assertThat(gizmo.getPersistentSelection()).isEqualTo(ScaleGizmo.X_AXIS_GROUP);
    }

    @Test
    void given_pressOverAnAxis_when_draggedAway_then_onlyThatAxisGrows()
    {
        // Arrange
        Camera camera = createCamera();
        ScaleGizmo gizmo = createGizmo(camera);
        ScaleGizmoInteractionTechnique technique = new ScaleGizmoInteractionTechnique(gizmo);
        Viewport viewport = new Viewport();
        MouseEvent press = mouseEventOverPoint(camera, gizmo.getTipPosition(0));

        gizmo.setVolatileSelection(gizmo.pickElement(camera.generateRay(press.getX(), press.getY())));
        technique.processMousePressedEvent(press, viewport);

        // Act: drag further away from the projected origin, in screen space
        // (a 3D point that far along the axis could leave the view frustum)
        Vector3Dd origin = camera.projectPointUsingRayMethod(gizmo.getPosition());
        int originX = (int)Math.round(origin.x());
        int originY = (int)Math.round(origin.y());
        int fartherX = originX + 4*(press.getX() - originX);
        int fartherY = originY + 4*(press.getY() - originY);
        boolean changed = technique.processMouseDraggedEvent(mouseEvent(fartherX, fartherY));

        // Assert
        assertThat(changed).isTrue();
        assertThat(gizmo.getScale().x()).isGreaterThan(1.0);
        assertThat(gizmo.getScale().y()).isCloseTo(1.0, offset(EPS));
        assertThat(gizmo.getScale().z()).isCloseTo(1.0, offset(EPS));
    }

    @Test
    void given_pressOverATwoAxisBand_when_draggedTowardTheGizmo_then_bothAxesShrink()
    {
        // Arrange: the XZ band is the one facing this camera (the other two
        // planes are seen from behind it)
        Camera camera = createCamera();
        ScaleGizmo gizmo = createGizmo(camera);
        ScaleGizmoInteractionTechnique technique = new ScaleGizmoInteractionTechnique(gizmo);
        Viewport viewport = new Viewport();
        Vector3Dd[] quad = gizmo.buildBandQuad(ScaleGizmo.XZ_GROUP);
        Vector3Dd bandPosition = quad[0].add(quad[1]).add(quad[2]).add(quad[3]).multiply(0.25);
        MouseEvent press = mouseEventOverPoint(camera, bandPosition);

        gizmo.setVolatileSelection(gizmo.pickElement(camera.generateRay(press.getX(), press.getY())));
        assertThat(gizmo.getCurrentSelection()).isEqualTo(ScaleGizmo.XZ_GROUP);
        technique.processMousePressedEvent(press, viewport);

        // Act: drag toward the origin of the gizmo
        boolean changed = technique.processMouseDraggedEvent(
            mouseEventOverPoint(camera, gizmo.getPosition()));

        // Assert
        assertThat(changed).isTrue();
        assertThat(gizmo.getScale().x()).isLessThan(1.0);
        assertThat(gizmo.getScale().y()).isCloseTo(1.0, offset(EPS));
        assertThat(gizmo.getScale().z()).isLessThan(1.0);
        assertThat(technique.getDragViewport()).isSameAs(viewport);
    }

    @Test
    void given_pressOverTheUniformHandle_when_dragged_then_everyAxisScalesTogether()
    {
        // Arrange
        Camera camera = createCamera();
        ScaleGizmo gizmo = createGizmo(camera);
        ScaleGizmoInteractionTechnique technique = new ScaleGizmoInteractionTechnique(gizmo);
        Viewport viewport = new Viewport();
        Vector3Dd[] triangles = gizmo.buildUniformTriangles();
        Vector3Dd triangleCentroid = triangles[0].add(triangles[1])
            .add(triangles[2]).multiply(1.0/3.0);
        MouseEvent press = mouseEventOverPoint(camera, triangleCentroid);

        gizmo.setVolatileSelection(gizmo.pickElement(camera.generateRay(press.getX(), press.getY())));
        assertThat(gizmo.getCurrentSelection()).isEqualTo(ScaleGizmo.UNIFORM_GROUP);
        technique.processMousePressedEvent(press, viewport);

        // Drag further away from the projected origin, in screen space (a
        // 3D point that far along the diagonal could leave the view frustum)
        Vector3Dd origin = camera.projectPointUsingRayMethod(gizmo.getPosition());
        int originX = (int)Math.round(origin.x());
        int originY = (int)Math.round(origin.y());
        int fartherX = originX + 4*(press.getX() - originX);
        int fartherY = originY + 4*(press.getY() - originY);

        // Act
        boolean changed = technique.processMouseDraggedEvent(mouseEvent(fartherX, fartherY));

        // Assert
        assertThat(changed).isTrue();
        assertThat(gizmo.getScale().x()).isGreaterThan(1.0);
        assertThat(gizmo.getScale().y()).isGreaterThan(1.0);
        assertThat(gizmo.getScale().z()).isGreaterThan(1.0);
        assertThat(gizmo.getScale().x()).isCloseTo(gizmo.getScale().y(), offset(EPS));
    }

    @Test
    void given_pressOverNoHandle_when_dragged_then_nothingIsScaled()
    {
        // Arrange: a group is selected (i.e. chosen with a previous click),
        // but the press happens far from every handle
        Camera camera = createCamera();
        ScaleGizmo gizmo = createGizmo(camera);
        ScaleGizmoInteractionTechnique technique = new ScaleGizmoInteractionTechnique(gizmo);
        Viewport viewport = new Viewport();

        gizmo.setPersistentSelection(ScaleGizmo.UNIFORM_GROUP);
        technique.processMousePressedEvent(mouseEvent(5, 5), viewport);

        // Act
        boolean changed = technique.processMouseDraggedEvent(mouseEvent(200, 200));

        // Assert
        assertThat(changed).isFalse();
        assertThat(technique.isDragging()).isFalse();
        assertThat(technique.getDragViewport()).isNull();
        assertThat(gizmo.getScale().x()).isCloseTo(1.0, offset(EPS));
        assertThat(gizmo.getScale().y()).isCloseTo(1.0, offset(EPS));
        assertThat(gizmo.getScale().z()).isCloseTo(1.0, offset(EPS));
    }

    @Test
    void given_dragInCourse_when_released_then_gestureEnds()
    {
        // Arrange
        Camera camera = createCamera();
        ScaleGizmo gizmo = createGizmo(camera);
        ScaleGizmoInteractionTechnique technique = new ScaleGizmoInteractionTechnique(gizmo);
        Viewport viewport = new Viewport();
        MouseEvent press = mouseEventOverPoint(camera, gizmo.getTipPosition(0));

        gizmo.setVolatileSelection(gizmo.pickElement(camera.generateRay(press.getX(), press.getY())));
        technique.processMousePressedEvent(press, viewport);

        // Act
        technique.processMouseReleasedEvent(press);

        // Assert
        assertThat(technique.getDragViewport()).isNull();
    }

    @Test
    void given_keyEvent_when_isInputGizmoKeyAsked_then_delegatesToTheGizmoInputBoxes()
    {
        // Arrange
        ScaleGizmo gizmo = createGizmo(createCamera());
        ScaleGizmoInteractionTechnique technique = new ScaleGizmoInteractionTechnique(gizmo);
        KeyEvent digit = new KeyEvent();

        digit.unicodeId = '5';

        // Act & Assert
        assertThat(technique.isInputGizmoKey(digit)).isTrue();
    }
}
