package vsdk.toolkit.gui;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.gui.gizmo.RotateGizmo;
import vsdk.toolkit.gui.gizmo.RotateGizmoInteractionTechnique;
import vsdk.toolkit.gui.viewport.Viewport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
Exercises the processing of mouse and keyboard events over a rotate gizmo,
independently of any rendering technology.
 */
class RotateGizmoInteractionTechniqueTest
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

    private static RotateGizmo createGizmo(Camera camera)
    {
        RotateGizmo gizmo = new RotateGizmo(camera);

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

    /**
    @return an event over a point of the given ring, at an angle far from the
    places where the rings cross each other
    */
    private static MouseEvent mouseEventOverRing(Camera camera, RotateGizmo gizmo, int ring)
    {
        Vector3Dd[] axes = {
            new Vector3Dd(1, 0, 0), new Vector3Dd(0, 1, 0), new Vector3Dd(0, 0, 1)
        };
        double angle = Math.toRadians(125);
        Vector3Dd point = gizmo.getPosition()
            .add(axes[(ring + 1) % 3].multiply(Math.cos(angle) * gizmo.getRingRadius()))
            .add(axes[(ring + 2) % 3].multiply(Math.sin(angle) * gizmo.getRingRadius()));
        Vector3Dd pixel = camera.projectPointUsingRayMethod(point);

        return mouseEvent((int)Math.round(pixel.x()), (int)Math.round(pixel.y()));
    }

    private static KeyEvent character(char unicode)
    {
        KeyEvent event = new KeyEvent();

        event.unicode_id = unicode;
        return event;
    }

    private static KeyEvent key(int keycode)
    {
        KeyEvent event = new KeyEvent();

        event.keycode = keycode;
        return event;
    }

    private static void type(RotateGizmoInteractionTechnique technique, String text)
    {
        for ( char c : text.toCharArray() ) {
            technique.processKeyPressedEvent(character(c));
        }
    }

    //= Mouse =============================================================

    @Test
    void given_cursorOverRing_when_moved_then_ringIsVolatileSelectionAndYellow()
    {
        // Arrange
        Camera camera = createCamera();
        RotateGizmo gizmo = createGizmo(camera);
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);

        // Act
        boolean changed = technique.processMouseMovedEvent(mouseEventOverRing(camera, gizmo, 1));

        // Assert
        assertThat(changed).isTrue();
        assertThat(technique.isActive()).isTrue();
        assertThat(gizmo.getVolatileSelection()).isEqualTo(RotateGizmo.Y_RING_GROUP);
        assertThat(gizmo.getPersistentSelection()).isEqualTo(RotateGizmo.NULL_GROUP);
        assertThat(gizmo.getRingColor(1).r()).isEqualTo(1.0);
        assertThat(gizmo.getRingColor(1).g()).isEqualTo(1.0);
        assertThat(gizmo.getRingColor(1).b()).isEqualTo(0.0);
    }

    @Test
    void given_cursorOverRing_when_movedAgainOverTheSame_then_noChangeIsReported()
    {
        // Arrange
        Camera camera = createCamera();
        RotateGizmo gizmo = createGizmo(camera);
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);

        technique.processMouseMovedEvent(mouseEventOverRing(camera, gizmo, 0));

        // Act
        boolean changed = technique.processMouseMovedEvent(mouseEventOverRing(camera, gizmo, 0));

        // Assert
        assertThat(changed).isFalse();
    }

    @Test
    void given_cursorOverRing_when_leavesIt_then_selectionIsCleared()
    {
        // Arrange
        Camera camera = createCamera();
        RotateGizmo gizmo = createGizmo(camera);
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);

        technique.processMouseMovedEvent(mouseEventOverRing(camera, gizmo, 2));

        // Act
        boolean changed = technique.processMouseMovedEvent(mouseEvent(2, 2));

        // Assert
        assertThat(changed).isTrue();
        assertThat(technique.isActive()).isFalse();
        assertThat(gizmo.getVolatileSelection()).isEqualTo(RotateGizmo.NULL_GROUP);
    }

    @Test
    void given_cursorOverRing_when_clicked_then_ringStaysSelectedAfterCursorLeaves()
    {
        // Arrange
        Camera camera = createCamera();
        RotateGizmo gizmo = createGizmo(camera);
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);
        MouseEvent overRing = mouseEventOverRing(camera, gizmo, 0);

        technique.processMouseMovedEvent(overRing);

        // Act
        technique.processMouseClickedEvent(overRing);
        technique.processMouseMovedEvent(mouseEvent(2, 2));

        // Assert
        assertThat(gizmo.getPersistentSelection()).isEqualTo(RotateGizmo.X_RING_GROUP);
        assertThat(gizmo.getCurrentSelection()).isEqualTo(RotateGizmo.X_RING_GROUP);
        assertThat(gizmo.isRingHighlighted(0)).isTrue();
        assertThat(gizmo.isRingHighlighted(1)).isFalse();
    }

    @Test
    void given_chosenRing_when_clickedOverNothing_then_selectionIsKept()
    {
        // Arrange
        Camera camera = createCamera();
        RotateGizmo gizmo = createGizmo(camera);
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);

        technique.processMouseClickedEvent(mouseEventOverRing(camera, gizmo, 2));

        // Act
        technique.processMouseClickedEvent(mouseEvent(2, 2));

        // Assert
        assertThat(gizmo.getPersistentSelection()).isEqualTo(RotateGizmo.Z_RING_GROUP);
    }

    @Test
    void given_chosenRing_when_clickedOverAnotherOne_then_thatOneIsChosen()
    {
        // Arrange
        Camera camera = createCamera();
        RotateGizmo gizmo = createGizmo(camera);
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);

        technique.processMouseClickedEvent(mouseEventOverRing(camera, gizmo, 2));

        // Act
        technique.processMouseClickedEvent(mouseEventOverRing(camera, gizmo, 1));

        // Assert
        assertThat(gizmo.getPersistentSelection()).isEqualTo(RotateGizmo.Y_RING_GROUP);
    }

    @Test
    void given_pressWithoutPreviousMove_when_overRing_then_gizmoIsActiveAndRingIsChosen()
    {
        // Arrange
        Camera camera = createCamera();
        RotateGizmo gizmo = createGizmo(camera);
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);

        // Act
        boolean changed = technique.processMousePressedEvent(mouseEventOverRing(camera, gizmo, 1));

        // Assert
        assertThat(changed).isFalse();
        assertThat(technique.isActive()).isTrue();
        assertThat(gizmo.getPersistentSelection()).isEqualTo(RotateGizmo.Y_RING_GROUP);
        assertThat(gizmo.getTransformationMatrix().epsilonEquals(new Matrix4x4d(), EPS)).isTrue();
    }

    //= Keyboard: numeric input ===========================================

    @Test
    void given_typedAngleAndEnter_when_keyPressed_then_gizmoTakesExactlyThatOrientation()
    {
        // Arrange
        RotateGizmo gizmo = new RotateGizmo(createCamera());

        gizmo.setTransformationMatrix(new Matrix4x4d().withTranslation(new Vector3Dd(1, 2, 3)));
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);

        // Act: 37.25 degrees around X (first box selected)
        type(technique, "37.25");
        boolean changed = technique.processKeyPressedEvent(key(KeyEvent.KEY_ENTER));

        // Assert
        double[] angles = RotateGizmo.extractAnglesInDegrees(gizmo.getTransformationMatrix());

        assertThat(changed).isTrue();
        assertThat(angles[0]).isCloseTo(37.25, offset(EPS));
        assertThat(angles[1]).isCloseTo(0.0, offset(EPS));
        assertThat(angles[2]).isCloseTo(0.0, offset(EPS));
        assertThat(Vector3Dd.distance(gizmo.getPosition(), new Vector3Dd(1, 2, 3))).isLessThan(EPS);
        assertThat(gizmo.getInputGizmo().isEditing()).isFalse();
    }

    @Test
    void given_anglesTypedInSeveralBoxes_when_enterPressed_then_allAreApplied()
    {
        // Arrange
        RotateGizmo gizmo = createGizmo(createCamera());
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);

        // Act
        type(technique, "-30");
        technique.processKeyPressedEvent(key(KeyEvent.KEY_TAB));
        type(technique, "45.5");
        technique.processKeyPressedEvent(key(KeyEvent.KEY_TAB));
        type(technique, "120");
        technique.processKeyPressedEvent(key(KeyEvent.KEY_ENTER));

        // Assert
        double[] angles = RotateGizmo.extractAnglesInDegrees(gizmo.getTransformationMatrix());

        assertThat(angles[0]).isCloseTo(-30.0, offset(EPS));
        assertThat(angles[1]).isCloseTo(45.5, offset(EPS));
        assertThat(angles[2]).isCloseTo(120.0, offset(EPS));
    }

    @Test
    void given_typedNumberWithManyDecimals_when_typing_then_onlyTwoDecimalsAreAccepted()
    {
        // Arrange
        RotateGizmo gizmo = createGizmo(createCamera());
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);

        // Act
        type(technique, "12.3456");

        // Assert
        assertThat(gizmo.getInputGizmo().getEditText(0)).isEqualTo("12.34");
    }

    @Test
    void given_typedAngleWithoutEnter_when_keyPressed_then_gizmoDoesNotChange()
    {
        // Arrange
        RotateGizmo gizmo = createGizmo(createCamera());
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);

        // Act
        type(technique, "45");

        // Assert
        assertThat(gizmo.getTransformationMatrix().epsilonEquals(new Matrix4x4d(), EPS)).isTrue();
        assertThat(gizmo.getInputGizmo().isEditing(0)).isTrue();
    }

    @Test
    void given_typedAngle_when_escapePressed_then_editionIsDiscarded()
    {
        // Arrange
        RotateGizmo gizmo = createGizmo(createCamera());
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);

        type(technique, "45");

        // Act
        boolean changed = technique.processKeyPressedEvent(key(KeyEvent.KEY_ESC));

        // Assert
        assertThat(changed).isFalse();
        assertThat(gizmo.getInputGizmo().isEditing()).isFalse();
        assertThat(gizmo.getTransformationMatrix().epsilonEquals(new Matrix4x4d(), EPS)).isTrue();
    }

    @Test
    void given_steppingKey_when_pressed_then_angleChangesRightAway()
    {
        // Arrange
        RotateGizmo gizmo = createGizmo(createCamera());
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);

        // Act: UP steps one degree, first box (X angle)
        boolean changed = technique.processKeyPressedEvent(key(KeyEvent.KEY_UP));

        // Assert
        assertThat(changed).isTrue();
        assertThat(RotateGizmo.extractAnglesInDegrees(gizmo.getTransformationMatrix())[0])
            .isCloseTo(1.0, offset(EPS));
    }

    @Test
    void given_keys_when_askedIfInputGizmoUsesThem_then_onlyNumbersAndEditionKeysAre()
    {
        // Arrange
        RotateGizmo gizmo = createGizmo(createCamera());
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);

        // Act & Assert
        assertThat(technique.isInputGizmoKey(character('5'))).isTrue();
        assertThat(technique.isInputGizmoKey(character('-'))).isTrue();
        assertThat(technique.isInputGizmoKey(key(KeyEvent.KEY_TAB))).isTrue();
        assertThat(technique.isInputGizmoKey(character('x'))).isFalse();
        assertThat(technique.isInputGizmoKey(key(KeyEvent.KEY_ENTER))).isFalse();
    }

    //= Keyboard: rotation keys ===========================================

    @Test
    void given_upperCaseXKey_when_keyPressed_then_gizmoRotatesOneDegreeAroundItsXAxis()
    {
        // Arrange
        RotateGizmo gizmo = new RotateGizmo(createCamera());

        gizmo.setTransformationMatrix(new Matrix4x4d().withTranslation(new Vector3Dd(1, 0, 0)));
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);

        // Act
        boolean changed = technique.processKeyPressedEvent(character('X'));

        // Assert
        double[] angles = RotateGizmo.extractAnglesInDegrees(gizmo.getTransformationMatrix());

        assertThat(changed).isTrue();
        assertThat(angles[0]).isCloseTo(1.0, offset(EPS));
        assertThat(Vector3Dd.distance(gizmo.getPosition(), new Vector3Dd(1, 0, 0))).isLessThan(EPS);
    }

    @Test
    void given_lowerCaseKeys_when_keyPressed_then_gizmoRotatesTheOtherWayAroundEachAxis()
    {
        // Arrange
        RotateGizmo gizmo = createGizmo(createCamera());
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);

        // Act
        technique.processKeyPressedEvent(character('z'));
        technique.processKeyPressedEvent(character('z'));

        // Assert
        double[] angles = RotateGizmo.extractAnglesInDegrees(gizmo.getTransformationMatrix());

        assertThat(angles[2]).isCloseTo(-2.0, offset(EPS));
    }

    @Test
    void given_unrelatedKey_when_keyPressed_then_nothingChanges()
    {
        // Arrange
        RotateGizmo gizmo = createGizmo(createCamera());
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);

        // Act
        boolean changed = technique.processKeyPressedEvent(character('q'));

        // Assert
        assertThat(changed).isFalse();
        assertThat(gizmo.getTransformationMatrix().epsilonEquals(new Matrix4x4d(), EPS)).isTrue();
    }

    @Test
    void given_typedNumber_when_rotationKeyPressed_then_editionIsDiscarded()
    {
        // Arrange
        RotateGizmo gizmo = createGizmo(createCamera());
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);

        type(technique, "45");

        // Act
        technique.processKeyPressedEvent(character('Y'));

        // Assert
        assertThat(gizmo.getInputGizmo().isEditing()).isFalse();
    }

    //= Mouse: drag =======================================================

    /**
    @param frame frame of the gizmo when the gesture started (the gizmo turns
    while it is dragged, and the cursor is moved over the ring as it was)
    @return an event over the point of a ring, in the frame given, at the
    given angle around its axis
    */
    private static MouseEvent mouseEventOverRingAt(Camera camera, RotateGizmo gizmo,
                                                   Matrix4x4d frame, int ring, double degrees)
    {
        Matrix4x4d rotation = frame.withoutTranslation();
        Vector3Dd u = rotation.multiply(axis((ring + 1) % 3));
        Vector3Dd v = rotation.multiply(axis((ring + 2) % 3));
        double angle = Math.toRadians(degrees);
        Vector3Dd point = frame.extractTranslation()
            .add(u.multiply(Math.cos(angle) * gizmo.getRingRadius()))
            .add(v.multiply(Math.sin(angle) * gizmo.getRingRadius()));
        Vector3Dd pixel = camera.projectPointUsingRayMethod(point);

        return mouseEvent((int)Math.round(pixel.x()), (int)Math.round(pixel.y()));
    }

    private static Vector3Dd axis(int i)
    {
        return new Vector3Dd(i == 0 ? 1 : 0, i == 1 ? 1 : 0, i == 2 ? 1 : 0);
    }

    @Test
    void given_dragOverEachRing_when_cursorGoesAroundTheAxis_then_gizmoRotatesThatAngleAroundIt()
    {
        for ( int ring = 0; ring < 3; ring++ ) {
            // Arrange
            Camera camera = createCamera();
            RotateGizmo gizmo = createGizmo(camera);
            RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);
        Matrix4x4d frame = new Matrix4x4d(gizmo.getTransformationMatrix());

            technique.processMousePressedEvent(mouseEventOverRingAt(camera, gizmo, frame, ring, 125));

            // Act: the cursor follows the point of the ring from 125 to 185 degrees
            boolean changed = false;

            for ( double degrees = 125; degrees <= 185; degrees += 5 ) {
                changed |= technique.processMouseDraggedEvent(
                    mouseEventOverRingAt(camera, gizmo, frame, ring, degrees));
            }

            // Assert
            double[] angles = RotateGizmo.extractAnglesInDegrees(gizmo.getTransformationMatrix());

            assertThat(changed).as("ring %d", ring).isTrue();
            // Integer pixels in a plane seen obliquely give about a couple of degrees of error
            assertThat(angles[ring]).as("ring %d", ring).isCloseTo(60.0, offset(3.0));
            assertThat(angles[(ring + 1) % 3]).as("ring %d", ring).isCloseTo(0.0, offset(EPS));
            assertThat(angles[(ring + 2) % 3]).as("ring %d", ring).isCloseTo(0.0, offset(EPS));
            assertThat(gizmo.getArcSweepInDegrees()).as("ring %d", ring).isCloseTo(60.0, offset(3.0));
            assertThat(gizmo.getArcRing()).isEqualTo(ring);
        }
    }

    @Test
    void given_dragging_when_cursorGoesTheOtherWay_then_rotationAndArcAreNegative()
    {
        // Arrange
        Camera camera = createCamera();
        RotateGizmo gizmo = createGizmo(camera);
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);
        Matrix4x4d frame = new Matrix4x4d(gizmo.getTransformationMatrix());

        technique.processMousePressedEvent(mouseEventOverRingAt(camera, gizmo, frame, 2, 100));

        // Act
        for ( double degrees = 100; degrees >= 40; degrees -= 5 ) {
            technique.processMouseDraggedEvent(mouseEventOverRingAt(camera, gizmo, frame, 2, degrees));
        }

        // Assert
        assertThat(RotateGizmo.extractAnglesInDegrees(gizmo.getTransformationMatrix())[2])
            .isCloseTo(-60.0, offset(1.5));
        assertThat(gizmo.getArcSweepInDegrees()).isCloseTo(-60.0, offset(1.5));
    }

    @Test
    void given_dragging_when_pointOfTheRingIsGrabbed_then_itStaysUnderTheCursor()
    {
        // Arrange: the gizmo with an orientation and a position
        Camera camera = createCamera();
        RotateGizmo gizmo = new RotateGizmo(camera);

        gizmo.setTransformationMatrix(RotateGizmo.createRotationFromAnglesInDegrees(30, 20, 10)
            .withTranslation(new Vector3Dd(0.5, -0.5, 0.3)));
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);
        Matrix4x4d frame = new Matrix4x4d(gizmo.getTransformationMatrix());
        Matrix4x4d start = new Matrix4x4d(gizmo.getTransformationMatrix());
        MouseEvent grab = mouseEventOverRingAt(camera, gizmo, frame, 1, 50);

        technique.processMousePressedEvent(grab);
        assertThat(technique.isDragging()).isTrue();

        // Act: drag until the cursor is over the point at 130 degrees
        MouseEvent target = mouseEventOverRingAt(camera, gizmo, frame, 1, 130);

        for ( int step = 0; step <= 16; step++ ) {
            technique.processMouseDraggedEvent(mouseEventOverRingAt(camera, gizmo, frame, 1, 50 + 5 * step));
        }

        // Assert: the point that was at 50 degrees is now where the cursor is,
        // the pixel of the one at 130 degrees of the ring in the start frame
        Matrix4x4d moved = gizmo.getTransformationMatrix();
        Vector3Dd grabbed = new Vector3Dd(Math.sin(Math.toRadians(50)), 0, Math.cos(Math.toRadians(50)))
            .multiply(gizmo.getRingRadius());
        Vector3Dd pixel = camera.projectPointUsingRayMethod(moved.multiply(grabbed));

        assertThat(pixel.x()).isCloseTo(target.getX(), offset(1.5));
        assertThat(pixel.y()).isCloseTo(target.getY(), offset(1.5));
        // Axis and center do not move, the frame is the start one turned around its axis
        assertThat(Vector3Dd.distance(gizmo.getAxisDirection(1),
            start.withoutTranslation().multiply(new Vector3Dd(0, 1, 0)))).isLessThan(EPS);
        assertThat(Vector3Dd.distance(gizmo.getPosition(), new Vector3Dd(0.5, -0.5, 0.3))).isLessThan(EPS);
    }

    @Test
    void given_cursorGoingSeveralTurns_when_dragging_then_rotationAccumulatesWithoutJumps()
    {
        // Arrange
        Camera camera = createCamera();
        RotateGizmo gizmo = createGizmo(camera);
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);
        Matrix4x4d frame = new Matrix4x4d(gizmo.getTransformationMatrix());

        technique.processMousePressedEvent(mouseEventOverRingAt(camera, gizmo, frame, 2, 0));

        // Act: two turns and a half, with steps of 10 degrees
        for ( double degrees = 0; degrees <= 900; degrees += 10 ) {
            technique.processMouseDraggedEvent(mouseEventOverRingAt(camera, gizmo, frame, 2, degrees));
        }

        // Assert
        assertThat(gizmo.getArcSweepInDegrees()).isCloseTo(900.0, offset(3.0));
        // 900 degrees are 180 degrees, that can be reported as +180 or -180
        double turned = RotateGizmo.extractAnglesInDegrees(gizmo.getTransformationMatrix())[2];

        assertThat(Math.abs(turned)).isCloseTo(180.0, offset(3.0));
    }

    @Test
    void given_dragging_when_released_then_arcDisappearsAndRotationIsKept()
    {
        // Arrange
        Camera camera = createCamera();
        RotateGizmo gizmo = createGizmo(camera);
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);
        Matrix4x4d frame = new Matrix4x4d(gizmo.getTransformationMatrix());

        technique.processMousePressedEvent(mouseEventOverRingAt(camera, gizmo, frame, 0, 125));
        technique.processMouseDraggedEvent(mouseEventOverRingAt(camera, gizmo, frame, 0, 125));
        technique.processMouseDraggedEvent(mouseEventOverRingAt(camera, gizmo, frame, 0, 155));
        assertThat(gizmo.isArcVisible()).isTrue();

        // Act
        boolean changed = technique.processMouseReleasedEvent(mouseEvent(0, 0));

        // Assert
        assertThat(changed).isTrue();
        assertThat(gizmo.isArcVisible()).isFalse();
        assertThat(technique.isDragging()).isFalse();
        assertThat(RotateGizmo.extractAnglesInDegrees(gizmo.getTransformationMatrix())[0])
            .isCloseTo(30.0, offset(1.5));
        // Dragging without a gesture does nothing
        assertThat(technique.processMouseDraggedEvent(mouseEventOverRingAt(camera, gizmo, frame, 0, 90))).isFalse();
        assertThat(technique.processMouseReleasedEvent(mouseEvent(0, 0))).isFalse();
    }

    @Test
    void given_pressOverNothing_when_dragged_then_nothingRotates()
    {
        // Arrange
        Camera camera = createCamera();
        RotateGizmo gizmo = createGizmo(camera);
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);
        Matrix4x4d frame = new Matrix4x4d(gizmo.getTransformationMatrix());

        technique.processMousePressedEvent(mouseEvent(2, 2));

        // Act
        boolean changed = technique.processMouseDraggedEvent(mouseEventOverRingAt(camera, gizmo, frame, 0, 90));

        // Assert
        assertThat(technique.isDragging()).isFalse();
        assertThat(changed).isFalse();
        assertThat(gizmo.getTransformationMatrix().epsilonEquals(new Matrix4x4d(), EPS)).isTrue();
    }

    @Test
    void given_pressOverRing_when_dragging_then_ringIsChosenAndHoverIsIgnored()
    {
        // Arrange
        Camera camera = createCamera();
        RotateGizmo gizmo = createGizmo(camera);
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);
        Matrix4x4d frame = new Matrix4x4d(gizmo.getTransformationMatrix());

        technique.processMousePressedEvent(mouseEventOverRingAt(camera, gizmo, frame, 1, 50));

        // Act: the cursor goes over another ring
        boolean changed = technique.processMouseMovedEvent(mouseEventOverRingAt(camera, gizmo, frame, 2, 125));

        // Assert
        assertThat(changed).isFalse();
        assertThat(gizmo.getPersistentSelection()).isEqualTo(RotateGizmo.Y_RING_GROUP);
        assertThat(gizmo.getCurrentSelection()).isEqualTo(RotateGizmo.Y_RING_GROUP);
    }

    @Test
    void given_viewport_when_pressedOverRing_then_gestureBelongsToItUntilRelease()
    {
        // Arrange
        Camera camera = createCamera();
        RotateGizmo gizmo = createGizmo(camera);
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);
        Matrix4x4d frame = new Matrix4x4d(gizmo.getTransformationMatrix());
        Viewport viewport = new Viewport();

        // Act & Assert
        technique.processMousePressedEvent(mouseEvent(2, 2), viewport);
        assertThat(technique.getDragViewport()).isNull();

        technique.processMousePressedEvent(mouseEventOverRingAt(camera, gizmo, frame, 1, 50), viewport);
        assertThat(technique.getDragViewport()).isSameAs(viewport);

        technique.processMouseReleasedEvent(mouseEvent(0, 0));
        assertThat(technique.getDragViewport()).isNull();
    }

    @Test
    void given_ringSeenEdgeOn_when_dragged_then_gizmoDoesNotJump()
    {
        // Arrange: camera in the plane of the ring around the X axis (YZ)
        Camera camera = new Camera();

        camera.setProjectionMode(Camera.PROJECTION_MODE_PERSPECTIVE);
        camera.updateViewportResize(400, 400);
        camera.setPosition(new Vector3Dd(0, -10, 0));
        camera.setFocusedPositionMaintainingOrthogonality(new Vector3Dd(0, 0, 0));
        camera.updateVectors();
        RotateGizmo gizmo = createGizmo(camera);
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);
        Matrix4x4d frame = new Matrix4x4d(gizmo.getTransformationMatrix());

        technique.processMousePressedEvent(mouseEventOverRingAt(camera, gizmo, frame, 0, 90));
        assertThat(technique.isDragging()).isTrue();

        // Act
        boolean changed = technique.processMouseDraggedEvent(mouseEventOverRingAt(camera, gizmo, frame, 0, 90));

        // Assert: ring axis is perpendicular to the view ray, no angle can be measured
        assertThat(changed).isFalse();
        assertThat(gizmo.getTransformationMatrix().epsilonEquals(new Matrix4x4d(), EPS)).isTrue();
    }

    @Test
    void given_orthogonalCamera_when_dragging_then_rotationFollowsTheCursor()
    {
        // Arrange
        Camera camera = createCamera();

        camera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        camera.updateVectors();
        RotateGizmo gizmo = createGizmo(camera);
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);
        Matrix4x4d frame = new Matrix4x4d(gizmo.getTransformationMatrix());

        technique.processMousePressedEvent(mouseEventOverRingAt(camera, gizmo, frame, 2, 20));

        // Act
        for ( double degrees = 20; degrees <= 80; degrees += 5 ) {
            technique.processMouseDraggedEvent(mouseEventOverRingAt(camera, gizmo, frame, 2, degrees));
        }

        // Assert
        assertThat(RotateGizmo.extractAnglesInDegrees(gizmo.getTransformationMatrix())[2])
            .isCloseTo(60.0, offset(1.5));
    }

    @Test
    void given_typedNumber_when_dragging_then_editionIsDiscarded()
    {
        // Arrange
        Camera camera = createCamera();
        RotateGizmo gizmo = createGizmo(camera);
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);
        Matrix4x4d frame = new Matrix4x4d(gizmo.getTransformationMatrix());

        type(technique, "45");
        technique.processMousePressedEvent(mouseEventOverRingAt(camera, gizmo, frame, 2, 20));

        // Act
        technique.processMouseDraggedEvent(mouseEventOverRingAt(camera, gizmo, frame, 2, 20));
        technique.processMouseDraggedEvent(mouseEventOverRingAt(camera, gizmo, frame, 2, 40));

        // Assert
        assertThat(gizmo.getInputGizmo().isEditing()).isFalse();
    }

    @Test
    void given_cursorMovedFarBeforeTheFirstDragEvent_when_dragging_then_thatMovementIsNotLost()
    {
        // Arrange
        Camera camera = createCamera();
        RotateGizmo gizmo = createGizmo(camera);
        RotateGizmoInteractionTechnique technique = new RotateGizmoInteractionTechnique(gizmo);
        Matrix4x4d frame = new Matrix4x4d(gizmo.getTransformationMatrix());

        technique.processMousePressedEvent(mouseEventOverRingAt(camera, gizmo, frame, 2, 125));

        // Act: the first drag event is already 40 degrees away
        technique.processMouseDraggedEvent(mouseEventOverRingAt(camera, gizmo, frame, 2, 165));

        // Assert
        assertThat(gizmo.getArcSweepInDegrees()).isCloseTo(40.0, offset(3.0));
        assertThat(RotateGizmo.extractAnglesInDegrees(gizmo.getTransformationMatrix())[2])
            .isCloseTo(40.0, offset(3.0));
    }
}
