package vsdk.toolkit.gui;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.gui.gizmo.TranslateGizmo;
import vsdk.toolkit.gui.gizmo.TranslateGizmoInteractionTechnique;
import vsdk.toolkit.gui.viewport.Viewport;

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


    private static Viewport createViewport()
    {
        Viewport viewport = new Viewport();

        // Borders take 6 pixels of the container: viewport of 400 x 400
        viewport.updatePixelArea(406, 406);
        return viewport;
    }

    /**
    Creates a technique with the Y axis handle of the gizmo selected, so a
    drag over it moves the gizmo.
    */
    private static TranslateGizmoInteractionTechnique createTechniqueWithYAxisSelected(
        Camera camera, TranslateGizmo gizmo)
    {
        TranslateGizmoInteractionTechnique technique =
            new TranslateGizmoInteractionTechnique(gizmo);
        Vector3Dd onYAxis = new Vector3Dd(0, gizmo.getCurrentScale()*0.55, 0);

        technique.processMouseClickedEvent(mouseEventAt(camera, onYAxis));
        return technique;
    }

    private static KeyEvent keyEvent(char unicode)
    {
        KeyEvent event = new KeyEvent();

        event.unicodeId = unicode;
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

    @Test
    void given_handleSelected_when_pressedInViewport_then_gestureBelongsToThatViewport()
    {
        // Arrange
        Camera camera = createCamera();
        TranslateGizmo gizmo = createGizmo(camera);
        TranslateGizmoInteractionTechnique technique =
            createTechniqueWithYAxisSelected(camera, gizmo);
        Viewport viewport = createViewport();

        // Act
        technique.processMousePressedEvent(mouseEvent(200, 200), viewport);

        // Assert
        assertThat(technique.getDragViewport()).isSameAs(viewport);
    }

    @Test
    void given_noHandleSelected_when_pressedInViewport_then_thereIsNoGesture()
    {
        // Arrange (a new gizmo has the X axis persistently selected)
        TranslateGizmo gizmo = createGizmo(createCamera());
        gizmo.setPersistentSelection(TranslateGizmo.NULL_GROUP);
        TranslateGizmoInteractionTechnique technique =
            new TranslateGizmoInteractionTechnique(gizmo);

        // Act
        technique.processMousePressedEvent(mouseEvent(200, 200), createViewport());

        // Assert
        assertThat(technique.getDragViewport()).isNull();
    }

    @Test
    void given_gesture_when_released_then_gestureEnds()
    {
        // Arrange
        Camera camera = createCamera();
        TranslateGizmo gizmo = createGizmo(camera);
        TranslateGizmoInteractionTechnique technique =
            createTechniqueWithYAxisSelected(camera, gizmo);
        technique.processMousePressedEvent(mouseEvent(200, 200), createViewport());

        // Act
        technique.processMouseReleasedEvent(mouseEvent(200, 200));

        // Assert
        assertThat(technique.getDragViewport()).isNull();
    }

    @Test
    void given_wrapEnabled_when_cursorLeavesThroughTop_then_warpsToBottomSide()
    {
        // Arrange
        Camera camera = createCamera();
        TranslateGizmo gizmo = createGizmo(camera);
        TranslateGizmoInteractionTechnique technique =
            createTechniqueWithYAxisSelected(camera, gizmo);
        technique.setCursorWrapEnabled(true);
        technique.processMousePressedEvent(mouseEvent(200, 200), createViewport());

        // Act
        technique.processMouseDraggedEvent(mouseEvent(200, -10));

        // Assert
        assertThat(technique.consumeCursorWarp())
            .isEqualTo(new TranslateGizmoInteractionTechnique.CursorWarp(200, 390));
        assertThat(technique.consumeCursorWarp()).isNull();
    }

    @Test
    void given_wrapEnabled_when_cursorLeavesThroughRightAndBottom_then_warpsAroundBothAxes()
    {
        // Arrange
        Camera camera = createCamera();
        TranslateGizmo gizmo = createGizmo(camera);
        TranslateGizmoInteractionTechnique technique =
            createTechniqueWithYAxisSelected(camera, gizmo);
        technique.setCursorWrapEnabled(true);
        technique.processMousePressedEvent(mouseEvent(200, 200), createViewport());

        // Act
        technique.processMouseDraggedEvent(mouseEvent(410, 450));

        // Assert
        assertThat(technique.consumeCursorWarp())
            .isEqualTo(new TranslateGizmoInteractionTechnique.CursorWarp(10, 50));
    }

    @Test
    void given_wrapDisabled_when_cursorLeavesViewport_then_noWarpIsRequested()
    {
        // Arrange
        Camera camera = createCamera();
        TranslateGizmo gizmo = createGizmo(camera);
        TranslateGizmoInteractionTechnique technique =
            createTechniqueWithYAxisSelected(camera, gizmo);
        technique.processMousePressedEvent(mouseEvent(200, 200), createViewport());

        // Act
        technique.processMouseDraggedEvent(mouseEvent(200, -50));

        // Assert
        assertThat(technique.consumeCursorWarp()).isNull();
    }

    @Test
    void given_cursorWrapped_when_itArrivesAtNewPlace_then_movementIsContinuous()
    {
        // Arrange: the reference is a drag without confinement, so the cursor
        // just goes out of the viewport
        Camera camera = createCamera();
        TranslateGizmo gizmo = createGizmo(camera);
        TranslateGizmoInteractionTechnique technique =
            createTechniqueWithYAxisSelected(camera, gizmo);
        technique.setCursorWrapEnabled(true);
        technique.processMousePressedEvent(mouseEvent(200, 200), createViewport());

        TranslateGizmo referenceGizmo = createGizmo(camera);
        TranslateGizmoInteractionTechnique reference =
            createTechniqueWithYAxisSelected(camera, referenceGizmo);
        reference.processMousePressedEvent(mouseEvent(200, 200));

        // Act
        technique.processMouseDraggedEvent(mouseEvent(200, -5));
        technique.processMouseDraggedEvent(mouseEvent(200, 395));
        technique.processMouseDraggedEvent(mouseEvent(200, 385));
        reference.processMouseDraggedEvent(mouseEvent(200, -15));

        // Assert
        assertThat(gizmo.getPosition().x()).isCloseTo(referenceGizmo.getPosition().x(), offset(EPS));
        assertThat(gizmo.getPosition().y()).isCloseTo(referenceGizmo.getPosition().y(), offset(EPS));
        assertThat(gizmo.getPosition().z()).isCloseTo(referenceGizmo.getPosition().z(), offset(EPS));
    }

    @Test
    void given_warpRequested_when_oldEventsArrive_then_theyAreIgnoredAndNoSecondWarpHappens()
    {
        // Arrange
        Camera camera = createCamera();
        TranslateGizmo gizmo = createGizmo(camera);
        TranslateGizmoInteractionTechnique technique =
            createTechniqueWithYAxisSelected(camera, gizmo);
        technique.setCursorWrapEnabled(true);
        technique.processMousePressedEvent(mouseEvent(200, 200), createViewport());
        technique.processMouseDraggedEvent(mouseEvent(200, -5));
        technique.consumeCursorWarp();
        Vector3Dd positionBefore = gizmo.getPosition();

        // Act: events sent by the system before the cursor is placed
        boolean changed = technique.processMouseDraggedEvent(mouseEvent(200, -30));

        // Assert
        assertThat(changed).isFalse();
        assertThat(technique.consumeCursorWarp()).isNull();
        assertThat(gizmo.getPosition()).isEqualTo(positionBefore);
    }

    @Test
    void given_warpNeverHappens_when_manyOldEventsArrive_then_gestureContinuesWithoutWrapping()
    {
        // Arrange
        Camera camera = createCamera();
        TranslateGizmo gizmo = createGizmo(camera);
        TranslateGizmoInteractionTechnique technique =
            createTechniqueWithYAxisSelected(camera, gizmo);
        technique.setCursorWrapEnabled(true);
        technique.processMousePressedEvent(mouseEvent(200, 200), createViewport());
        technique.processMouseDraggedEvent(mouseEvent(200, -5));
        technique.consumeCursorWarp();
        Vector3Dd positionAtExit = gizmo.getPosition();

        // Act: the cursor stays out of the viewport (no one placed it)
        for ( int i = 0; i < 25; i++ ) {
            technique.processMouseDraggedEvent(mouseEvent(200, -5));
        }

        // Assert: no jump, and no more requests
        assertThat(gizmo.getPosition().y()).isCloseTo(positionAtExit.y(), offset(EPS));
        assertThat(technique.consumeCursorWarp()).isNull();
        technique.processMouseDraggedEvent(mouseEvent(200, -25));
        assertThat(technique.consumeCursorWarp()).isNull();
    }

    @Test
    void given_gestureOverYAxis_when_cursorMovesOverXAxis_then_dragKeepsMovingOnlyAlongY()
    {
        // Arrange
        Camera camera = createCamera();
        TranslateGizmo gizmo = createGizmo(camera);
        TranslateGizmoInteractionTechnique technique =
            createTechniqueWithYAxisSelected(camera, gizmo);
        MouseEvent onYAxis = mouseEventAt(camera, new Vector3Dd(0, gizmo.getCurrentScale()*0.55, 0));
        MouseEvent onXAxis = mouseEventAt(camera, new Vector3Dd(gizmo.getCurrentScale()*0.55, 0, 0));
        technique.processMousePressedEvent(onYAxis, createViewport());

        // Act: a hover over other handle, as can be sent while dragging
        boolean changed = technique.processMouseMovedEvent(onXAxis);
        technique.processMouseDraggedEvent(mouseEvent(onYAxis.getX(), onYAxis.getY() - 20));

        // Assert
        assertThat(changed).isFalse();
        assertThat(gizmo.getCurrentSelection()).isEqualTo(TranslateGizmo.Y_AXIS_GROUP);
        assertThat(gizmo.getPosition().y()).isNotCloseTo(0.0, offset(EPS));
        assertThat(gizmo.getPosition().x()).isCloseTo(0.0, offset(EPS));
        assertThat(gizmo.getPosition().z()).isCloseTo(0.0, offset(EPS));
    }

    @Test
    void given_gestureOverYAxis_when_cursorMovesOverNothing_then_dragDoesNotFallBackToPersistentSelection()
    {
        // Arrange: the persistent selection is other axis than the dragged one
        Camera camera = createCamera();
        TranslateGizmo gizmo = createGizmo(camera);
        TranslateGizmoInteractionTechnique technique =
            createTechniqueWithYAxisSelected(camera, gizmo);
        MouseEvent onYAxis = mouseEventAt(camera, new Vector3Dd(0, gizmo.getCurrentScale()*0.55, 0));
        gizmo.setPersistentSelection(TranslateGizmo.Z_AXIS_GROUP);
        gizmo.setVolatileSelection(TranslateGizmo.Y_AXIS_GROUP);
        technique.processMousePressedEvent(onYAxis, createViewport());

        // Act: the volatile selection is lost, i.e. the cursor is over nothing
        gizmo.setVolatileSelection(TranslateGizmo.NULL_GROUP);
        technique.processMouseMovedEvent(mouseEvent(1, 1));
        technique.processMouseDraggedEvent(mouseEvent(onYAxis.getX(), onYAxis.getY() - 20));

        // Assert
        assertThat(gizmo.getPosition().y()).isNotCloseTo(0.0, offset(EPS));
        assertThat(gizmo.getPosition().x()).isCloseTo(0.0, offset(EPS));
        assertThat(gizmo.getPosition().z()).isCloseTo(0.0, offset(EPS));
    }

    @Test
    void given_warpRequested_when_movedEventArrivesAtTarget_then_nextDragIsNotIgnored()
    {
        // Arrange
        Camera camera = createCamera();
        TranslateGizmo gizmo = createGizmo(camera);
        TranslateGizmoInteractionTechnique technique =
            createTechniqueWithYAxisSelected(camera, gizmo);
        technique.setCursorWrapEnabled(true);
        technique.processMousePressedEvent(mouseEvent(200, 200), createViewport());
        technique.processMouseDraggedEvent(mouseEvent(200, -5));
        technique.consumeCursorWarp();

        // Act: some systems inform the placement of the cursor as a moved event
        technique.processMouseMovedEvent(mouseEvent(200, 395));
        boolean changed = technique.processMouseDraggedEvent(mouseEvent(200, 385));

        // Assert
        assertThat(changed).isTrue();
    }

    private static KeyEvent keyCode(int keycode)
    {
        KeyEvent event = new KeyEvent();

        event.keycode = keycode;
        return event;
    }

    @Test
    void given_typedCoordinates_when_enterPressed_then_gizmoMovesExactlyThere()
    {
        // Arrange
        TranslateGizmo gizmo = createGizmo(createCamera());
        TranslateGizmoInteractionTechnique technique =
            new TranslateGizmoInteractionTechnique(gizmo);

        gizmo.setPosition(new Vector3Dd(1, 2, 3));

        // Act
        boolean typed = technique.processKeyPressedEvent(keyEvent('4'));
        technique.processKeyPressedEvent(keyEvent('.'));
        technique.processKeyPressedEvent(keyEvent('2'));
        technique.processKeyPressedEvent(keyEvent('5'));
        technique.processKeyPressedEvent(keyCode(KeyEvent.KEY_TAB));
        technique.processKeyPressedEvent(keyEvent('7'));
        boolean moved = technique.processKeyPressedEvent(keyCode(KeyEvent.KEY_ENTER));

        // Assert
        assertThat(typed).isFalse();
        assertThat(moved).isTrue();
        assertThat(gizmo.getPosition().x()).isCloseTo(4.25, offset(EPS));
        assertThat(gizmo.getPosition().y()).isCloseTo(7.0, offset(EPS));
        assertThat(gizmo.getPosition().z()).isCloseTo(3.0, offset(EPS));
        assertThat(gizmo.getInputGizmo().isEditing()).isFalse();
    }

    @Test
    void given_typedCoordinates_when_gizmoDragged_then_editionIsDiscarded()
    {
        // Arrange
        Camera camera = createCamera();
        TranslateGizmo gizmo = createGizmo(camera);
        TranslateGizmoInteractionTechnique technique =
            new TranslateGizmoInteractionTechnique(gizmo);

        technique.processKeyPressedEvent(keyEvent('9'));
        assertThat(gizmo.getInputGizmo().isEditing()).isTrue();

        // Act
        technique.processMousePressedEvent(mouseEventAt(camera, new Vector3Dd(0.5, 0, 0)));
        technique.processMouseDraggedEvent(mouseEventAt(camera, new Vector3Dd(1.5, 0, 0)));

        // Assert
        assertThat(gizmo.getInputGizmo().isEditing()).isFalse();
    }

    @Test
    void given_typedCoordinates_when_movementKeyPressed_then_editionIsDiscarded()
    {
        // Arrange
        TranslateGizmo gizmo = createGizmo(createCamera());
        TranslateGizmoInteractionTechnique technique =
            new TranslateGizmoInteractionTechnique(gizmo);

        technique.processKeyPressedEvent(keyEvent('9'));

        // Act
        technique.processKeyPressedEvent(keyEvent('X'));

        // Assert
        assertThat(gizmo.getInputGizmo().isEditing()).isFalse();
        assertThat(gizmo.getPosition().x()).isCloseTo(0.1, offset(EPS));
    }

    @Test
    void given_fieldKeys_when_asked_then_techniqueReportsThemAsConsumed()
    {
        // Arrange
        TranslateGizmo gizmo = createGizmo(createCamera());
        TranslateGizmoInteractionTechnique technique =
            new TranslateGizmoInteractionTechnique(gizmo);

        // Act & Assert
        assertThat(technique.isInputGizmoKey(keyEvent('5'))).isTrue();
        assertThat(technique.isInputGizmoKey(keyCode(KeyEvent.KEY_TAB))).isTrue();
        assertThat(technique.isInputGizmoKey(keyEvent('x'))).isFalse();
    }

    @Test
    void given_gizmoGroups_when_axisHighlightAsked_then_planesHighlightTheirTwoAxes()
    {
        // Arrange
        TranslateGizmo gizmo = createGizmo(createCamera());

        // Act & Assert
        gizmo.setPersistentSelection(TranslateGizmo.Y_AXIS_GROUP);
        assertThat(gizmo.isAxisHighlighted(0)).isFalse();
        assertThat(gizmo.isAxisHighlighted(1)).isTrue();
        assertThat(gizmo.isAxisHighlighted(2)).isFalse();
        gizmo.setPersistentSelection(TranslateGizmo.XZ_PLANE_GROUP);
        assertThat(gizmo.isAxisHighlighted(0)).isTrue();
        assertThat(gizmo.isAxisHighlighted(1)).isFalse();
        assertThat(gizmo.isAxisHighlighted(2)).isTrue();
    }

    @Test
    void given_gizmoWithoutTransformation_when_inputGizmoAsked_then_doesNotFail()
    {
        // Arrange
        TranslateGizmo gizmo = new TranslateGizmo(createCamera());

        // Act & Assert
        assertThat(gizmo.getInputGizmo()).isNotNull();
        assertThat(new TranslateGizmoInteractionTechnique(gizmo)
            .isInputGizmoKey(keyEvent('5'))).isTrue();
    }

    @Test
    void given_minusAndDigits_when_enterPressed_then_gizmoMovesToNegativeCoordinate()
    {
        // Arrange
        TranslateGizmo gizmo = createGizmo(createCamera());
        TranslateGizmoInteractionTechnique technique =
            new TranslateGizmoInteractionTechnique(gizmo);

        // Act
        technique.processKeyPressedEvent(keyEvent('-'));
        technique.processKeyPressedEvent(keyEvent('4'));
        technique.processKeyPressedEvent(keyEvent('.'));
        technique.processKeyPressedEvent(keyEvent('5'));
        boolean moved = technique.processKeyPressedEvent(keyCode(KeyEvent.KEY_ENTER));

        // Assert
        assertThat(moved).isTrue();
        assertThat(gizmo.getPosition().x()).isCloseTo(-4.5, offset(EPS));
    }

    @Test
    void given_selectedYField_when_arrowAndPageKeysPressed_then_gizmoMovesInY()
    {
        // Arrange
        TranslateGizmo gizmo = createGizmo(createCamera());
        TranslateGizmoInteractionTechnique technique =
            new TranslateGizmoInteractionTechnique(gizmo);

        technique.processKeyPressedEvent(keyCode(KeyEvent.KEY_TAB));

        // Act
        boolean up = technique.processKeyPressedEvent(keyCode(KeyEvent.KEY_UP));
        technique.processKeyPressedEvent(keyCode(KeyEvent.KEY_PAGEUP));
        technique.processKeyPressedEvent(keyCode(KeyEvent.KEY_LEFT));

        // Assert: 1 + 5 - 0.1
        assertThat(up).isTrue();
        assertThat(gizmo.getPosition().x()).isCloseTo(0.0, offset(EPS));
        assertThat(gizmo.getPosition().y()).isCloseTo(5.9, offset(EPS));
        assertThat(gizmo.getInputGizmo().isEditing()).isFalse();
    }
}
