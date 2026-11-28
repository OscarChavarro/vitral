import { describe, expect, it } from "vitest";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Camera } from "vsdk/toolkit/environment/camera/Camera.js";
import { KeyEvent } from "vsdk/toolkit/gui/KeyEvent.js";
import { MouseEvent } from "vsdk/toolkit/gui/MouseEvent.js";
import { TranslateGizmo } from "vsdk/toolkit/gui/gizmo/TranslateGizmo.js";
import {
    CursorWarp,
    TranslateGizmoInteractionTechnique,
} from "vsdk/toolkit/gui/gizmo/TranslateGizmoInteractionTechnique.js";
import { Viewport } from "vsdk/toolkit/gui/viewport/Viewport.js";

/**
Exercises the processing of mouse and keyboard events over a translate gizmo,
independently of any rendering technology.
 */
describe("TranslateGizmoInteractionTechniqueTest", () => {
    const EPS = 1.0e-6;

    function closeTo(actual: number, expected: number, offset: number): void {
        expect(Math.abs(actual - expected)).toBeLessThanOrEqual(offset);
    }

    function notCloseTo(actual: number, expected: number, offset: number): void {
        expect(Math.abs(actual - expected)).toBeGreaterThan(offset);
    }

    /** Java `(int)Math.round(double)`. */
    function javaRound(value: number): number {
        return Math.floor(value + 0.5);
    }

    function createCamera(): Camera {
        const camera = new Camera();

        camera.setProjectionMode(Camera.PROJECTION_MODE_PERSPECTIVE);
        camera.updateViewportResize(400, 400);
        camera.setPosition(new Vector3Dd(5, -6, 4));
        camera.setFocusedPositionMaintainingOrthogonality(new Vector3Dd(0, 0, 0));
        camera.updateVectors();
        return camera;
    }

    function createGizmo(camera: Camera): TranslateGizmo {
        const gizmo = new TranslateGizmo(camera);

        gizmo.setTransformationMatrix(new Matrix4x4d());
        return gizmo;
    }

    function mouseEvent(x: number, y: number): MouseEvent {
        const event = new MouseEvent();

        event.setX(x);
        event.setY(y);
        return event;
    }

    function mouseEventAt(camera: Camera, point: Vector3Dd): MouseEvent {
        const pixel = camera.projectPointUsingRayMethod(point)!;

        return mouseEvent(javaRound(pixel.x()), javaRound(pixel.y()));
    }

    function createViewport(): Viewport {
        const viewport = new Viewport();

        // Borders take 6 pixels of the container: viewport of 400 x 400
        viewport.updatePixelArea(406, 406);
        return viewport;
    }

    /**
    Creates a technique with the Y axis handle of the gizmo selected, so a
    drag over it moves the gizmo.
    */
    function createTechniqueWithYAxisSelected(
        camera: Camera,
        gizmo: TranslateGizmo,
    ): TranslateGizmoInteractionTechnique {
        const technique = new TranslateGizmoInteractionTechnique(gizmo);
        const onYAxis = new Vector3Dd(0, gizmo.getCurrentScale() * 0.55, 0);

        technique.processMouseClickedEvent(mouseEventAt(camera, onYAxis));
        return technique;
    }

    function keyEvent(unicode: string): KeyEvent {
        const event = new KeyEvent();

        event.unicodeId = unicode.charCodeAt(0);
        return event;
    }

    function keyCode(keycode: number): KeyEvent {
        const event = new KeyEvent();

        event.keycode = keycode;
        return event;
    }

    it("given_upperCaseXKey_when_keyPressed_then_gizmoMovesForwardInX", () => {
        // Arrange
        const gizmo = createGizmo(createCamera());
        const technique = new TranslateGizmoInteractionTechnique(gizmo);

        // Act
        const changed = technique.processKeyPressedEvent(keyEvent("X"));

        // Assert
        expect(changed).toBe(true);
        closeTo(gizmo.getPosition().x(), 0.1, EPS);
        closeTo(gizmo.getPosition().y(), 0.0, EPS);
        closeTo(gizmo.getPosition().z(), 0.0, EPS);
    });

    it("given_lowerCaseZKey_when_keyPressed_then_gizmoMovesBackwardInZ", () => {
        // Arrange
        const gizmo = createGizmo(createCamera());
        const technique = new TranslateGizmoInteractionTechnique(gizmo);

        // Act
        const changed = technique.processKeyPressedEvent(keyEvent("z"));

        // Assert
        expect(changed).toBe(true);
        closeTo(gizmo.getPosition().z(), -0.1, EPS);
    });

    it("given_unrelatedKey_when_keyPressed_then_gizmoDoesNotChange", () => {
        // Arrange
        const gizmo = createGizmo(createCamera());
        const technique = new TranslateGizmoInteractionTechnique(gizmo);

        // Act
        const changed = technique.processKeyPressedEvent(keyEvent("q"));

        // Assert
        expect(changed).toBe(false);
        closeTo(gizmo.getPosition().length(), 0.0, EPS);
    });

    it("given_cursorOverYAxis_when_mouseClicked_then_yAxisGroupIsSelected", () => {
        // Arrange
        const camera = createCamera();
        const gizmo = createGizmo(camera);
        const technique = new TranslateGizmoInteractionTechnique(gizmo);
        const onYAxis = new Vector3Dd(0, gizmo.getCurrentScale() * 0.55, 0);

        // Act
        const changed = technique.processMouseClickedEvent(mouseEventAt(camera, onYAxis));

        // Assert
        expect(changed).toBe(true);
        expect(gizmo.getPersistentSelection()).toBe(TranslateGizmo.Y_AXIS_GROUP);
        expect(technique.isActive()).toBe(true);
    });

    it("given_cursorOutsideGizmo_when_mouseMoved_then_isNotActive", () => {
        // Arrange
        const camera = createCamera();
        const gizmo = createGizmo(camera);
        const technique = new TranslateGizmoInteractionTechnique(gizmo);

        // Act
        technique.processMouseMovedEvent(mouseEvent(2, 2));

        // Assert
        expect(technique.isActive()).toBe(false);
        expect(gizmo.getVolatileSelection()).toBe(TranslateGizmo.NULL_GROUP);
    });

    it("given_xAxisSelected_when_mouseDragged_then_gizmoMovesOnlyAlongX", () => {
        // Arrange
        const camera = createCamera();
        const gizmo = createGizmo(camera);
        const technique = new TranslateGizmoInteractionTechnique(gizmo);
        const start = new Vector3Dd(0.5, 0, 0);
        const end = new Vector3Dd(1.5, 0, 0);

        // Act
        technique.processMousePressedEvent(mouseEventAt(camera, start));
        const changed = technique.processMouseDraggedEvent(mouseEventAt(camera, end));

        // Assert
        expect(changed).toBe(true);
        closeTo(gizmo.getPosition().x(), 1.0, 0.05);
        closeTo(gizmo.getPosition().y(), 0.0, EPS);
        closeTo(gizmo.getPosition().z(), 0.0, EPS);
        expect(gizmo.isSelectedResizing()).toBe(false);
    });

    it("given_xAxisDrag_when_cursorSweepsHorizontallyWithRepeatedSamples_then_movementIsSmooth", () => {
        // Arrange: regression of the plane flip-flop that depended on the
        // last cursor delta (repeated samples or leftward moves jumped)
        const camera = createCamera();
        const gizmo = createGizmo(camera);
        const technique = new TranslateGizmoInteractionTechnique(gizmo);
        const start = new Vector3Dd(0.5, 0, 0);
        const startEvent = mouseEventAt(camera, start);
        const sweep = [0, 1, 1, 0, 2, 0, 0, 1, -1, -1, 0, -2, 1, 1, 0, 3, 3, 0];

        technique.processMousePressedEvent(startEvent);

        // Act
        let x = startEvent.getX();
        let previousX = gizmo.getPosition().x();
        let maxStep = 0;

        for (const delta of sweep) {
            x += delta;
            technique.processMouseDraggedEvent(mouseEvent(x, startEvent.getY()));
            const currentX = gizmo.getPosition().x();

            expect(Number.isNaN(currentX)).toBe(false);
            if (delta === 0) {
                closeTo(currentX, previousX, EPS);
            }
            maxStep = Math.max(maxStep, Math.abs(currentX - previousX));
            previousX = currentX;
        }

        // Assert: a step of at most 3 pixels is a small displacement
        expect(maxStep).toBeLessThan(0.2);
    });

    it("given_mouseReleased_when_processed_then_resizingIsRestored", () => {
        // Arrange
        const gizmo = createGizmo(createCamera());
        const technique = new TranslateGizmoInteractionTechnique(gizmo);

        gizmo.setSelectedResizing(false);

        // Act
        const changed = technique.processMouseReleasedEvent(mouseEvent(10, 10));

        // Assert
        expect(changed).toBe(true);
        expect(gizmo.isSelectedResizing()).toBe(true);
    });

    it("given_handleSelected_when_pressedInViewport_then_gestureBelongsToThatViewport", () => {
        // Arrange
        const camera = createCamera();
        const gizmo = createGizmo(camera);
        const technique = createTechniqueWithYAxisSelected(camera, gizmo);
        const viewport = createViewport();

        // Act
        technique.processMousePressedEvent(mouseEvent(200, 200), viewport);

        // Assert
        expect(technique.getDragViewport()).toBe(viewport);
    });

    it("given_noHandleSelected_when_pressedInViewport_then_thereIsNoGesture", () => {
        // Arrange (a new gizmo has the X axis persistently selected)
        const gizmo = createGizmo(createCamera());
        gizmo.setPersistentSelection(TranslateGizmo.NULL_GROUP);
        const technique = new TranslateGizmoInteractionTechnique(gizmo);

        // Act
        technique.processMousePressedEvent(mouseEvent(200, 200), createViewport());

        // Assert
        expect(technique.getDragViewport()).toBeNull();
    });

    it("given_gesture_when_released_then_gestureEnds", () => {
        // Arrange
        const camera = createCamera();
        const gizmo = createGizmo(camera);
        const technique = createTechniqueWithYAxisSelected(camera, gizmo);
        technique.processMousePressedEvent(mouseEvent(200, 200), createViewport());

        // Act
        technique.processMouseReleasedEvent(mouseEvent(200, 200));

        // Assert
        expect(technique.getDragViewport()).toBeNull();
    });

    it("given_wrapEnabled_when_cursorLeavesThroughTop_then_warpsToBottomSide", () => {
        // Arrange
        const camera = createCamera();
        const gizmo = createGizmo(camera);
        const technique = createTechniqueWithYAxisSelected(camera, gizmo);
        technique.setCursorWrapEnabled(true);
        technique.processMousePressedEvent(mouseEvent(200, 200), createViewport());

        // Act
        technique.processMouseDraggedEvent(mouseEvent(200, -10));

        // Assert
        expect(technique.consumeCursorWarp()).toEqual(new CursorWarp(200, 390));
        expect(technique.consumeCursorWarp()).toBeNull();
    });

    it("given_wrapEnabled_when_cursorLeavesThroughRightAndBottom_then_warpsAroundBothAxes", () => {
        // Arrange
        const camera = createCamera();
        const gizmo = createGizmo(camera);
        const technique = createTechniqueWithYAxisSelected(camera, gizmo);
        technique.setCursorWrapEnabled(true);
        technique.processMousePressedEvent(mouseEvent(200, 200), createViewport());

        // Act
        technique.processMouseDraggedEvent(mouseEvent(410, 450));

        // Assert
        expect(technique.consumeCursorWarp()).toEqual(new CursorWarp(10, 50));
    });

    it("given_wrapDisabled_when_cursorLeavesViewport_then_noWarpIsRequested", () => {
        // Arrange
        const camera = createCamera();
        const gizmo = createGizmo(camera);
        const technique = createTechniqueWithYAxisSelected(camera, gizmo);
        technique.processMousePressedEvent(mouseEvent(200, 200), createViewport());

        // Act
        technique.processMouseDraggedEvent(mouseEvent(200, -50));

        // Assert
        expect(technique.consumeCursorWarp()).toBeNull();
    });

    it("given_cursorWrapped_when_itArrivesAtNewPlace_then_movementIsContinuous", () => {
        // Arrange: the reference is a drag without confinement, so the cursor
        // just goes out of the viewport
        const camera = createCamera();
        const gizmo = createGizmo(camera);
        const technique = createTechniqueWithYAxisSelected(camera, gizmo);
        technique.setCursorWrapEnabled(true);
        technique.processMousePressedEvent(mouseEvent(200, 200), createViewport());

        const referenceGizmo = createGizmo(camera);
        const reference = createTechniqueWithYAxisSelected(camera, referenceGizmo);
        reference.processMousePressedEvent(mouseEvent(200, 200));

        // Act
        technique.processMouseDraggedEvent(mouseEvent(200, -5));
        technique.processMouseDraggedEvent(mouseEvent(200, 395));
        technique.processMouseDraggedEvent(mouseEvent(200, 385));
        reference.processMouseDraggedEvent(mouseEvent(200, -15));

        // Assert
        closeTo(gizmo.getPosition().x(), referenceGizmo.getPosition().x(), EPS);
        closeTo(gizmo.getPosition().y(), referenceGizmo.getPosition().y(), EPS);
        closeTo(gizmo.getPosition().z(), referenceGizmo.getPosition().z(), EPS);
    });

    it("given_warpRequested_when_oldEventsArrive_then_theyAreIgnoredAndNoSecondWarpHappens", () => {
        // Arrange
        const camera = createCamera();
        const gizmo = createGizmo(camera);
        const technique = createTechniqueWithYAxisSelected(camera, gizmo);
        technique.setCursorWrapEnabled(true);
        technique.processMousePressedEvent(mouseEvent(200, 200), createViewport());
        technique.processMouseDraggedEvent(mouseEvent(200, -5));
        technique.consumeCursorWarp();
        const positionBefore = gizmo.getPosition();

        // Act: events sent by the system before the cursor is placed
        const changed = technique.processMouseDraggedEvent(mouseEvent(200, -30));

        // Assert
        expect(changed).toBe(false);
        expect(technique.consumeCursorWarp()).toBeNull();
        expect(gizmo.getPosition().equals(positionBefore)).toBe(true);
    });

    it("given_warpNeverHappens_when_manyOldEventsArrive_then_gestureContinuesWithoutWrapping", () => {
        // Arrange
        const camera = createCamera();
        const gizmo = createGizmo(camera);
        const technique = createTechniqueWithYAxisSelected(camera, gizmo);
        technique.setCursorWrapEnabled(true);
        technique.processMousePressedEvent(mouseEvent(200, 200), createViewport());
        technique.processMouseDraggedEvent(mouseEvent(200, -5));
        technique.consumeCursorWarp();
        const positionAtExit = gizmo.getPosition();

        // Act: the cursor stays out of the viewport (no one placed it)
        for (let i = 0; i < 25; i++) {
            technique.processMouseDraggedEvent(mouseEvent(200, -5));
        }

        // Assert: no jump, and no more requests
        closeTo(gizmo.getPosition().y(), positionAtExit.y(), EPS);
        expect(technique.consumeCursorWarp()).toBeNull();
        technique.processMouseDraggedEvent(mouseEvent(200, -25));
        expect(technique.consumeCursorWarp()).toBeNull();
    });

    it("given_gestureOverYAxis_when_cursorMovesOverXAxis_then_dragKeepsMovingOnlyAlongY", () => {
        // Arrange
        const camera = createCamera();
        const gizmo = createGizmo(camera);
        const technique = createTechniqueWithYAxisSelected(camera, gizmo);
        const onYAxis = mouseEventAt(camera, new Vector3Dd(0, gizmo.getCurrentScale() * 0.55, 0));
        const onXAxis = mouseEventAt(camera, new Vector3Dd(gizmo.getCurrentScale() * 0.55, 0, 0));
        technique.processMousePressedEvent(onYAxis, createViewport());

        // Act: a hover over other handle, as can be sent while dragging
        const changed = technique.processMouseMovedEvent(onXAxis);
        technique.processMouseDraggedEvent(mouseEvent(onYAxis.getX(), onYAxis.getY() - 20));

        // Assert
        expect(changed).toBe(false);
        expect(gizmo.getCurrentSelection()).toBe(TranslateGizmo.Y_AXIS_GROUP);
        notCloseTo(gizmo.getPosition().y(), 0.0, EPS);
        closeTo(gizmo.getPosition().x(), 0.0, EPS);
        closeTo(gizmo.getPosition().z(), 0.0, EPS);
    });

    it("given_gestureOverYAxis_when_cursorMovesOverNothing_then_dragDoesNotFallBackToPersistentSelection", () => {
        // Arrange: the persistent selection is other axis than the dragged one
        const camera = createCamera();
        const gizmo = createGizmo(camera);
        const technique = createTechniqueWithYAxisSelected(camera, gizmo);
        const onYAxis = mouseEventAt(camera, new Vector3Dd(0, gizmo.getCurrentScale() * 0.55, 0));
        gizmo.setPersistentSelection(TranslateGizmo.Z_AXIS_GROUP);
        gizmo.setVolatileSelection(TranslateGizmo.Y_AXIS_GROUP);
        technique.processMousePressedEvent(onYAxis, createViewport());

        // Act: the volatile selection is lost, i.e. the cursor is over nothing
        gizmo.setVolatileSelection(TranslateGizmo.NULL_GROUP);
        technique.processMouseMovedEvent(mouseEvent(1, 1));
        technique.processMouseDraggedEvent(mouseEvent(onYAxis.getX(), onYAxis.getY() - 20));

        // Assert
        notCloseTo(gizmo.getPosition().y(), 0.0, EPS);
        closeTo(gizmo.getPosition().x(), 0.0, EPS);
        closeTo(gizmo.getPosition().z(), 0.0, EPS);
    });

    it("given_warpRequested_when_movedEventArrivesAtTarget_then_nextDragIsNotIgnored", () => {
        // Arrange
        const camera = createCamera();
        const gizmo = createGizmo(camera);
        const technique = createTechniqueWithYAxisSelected(camera, gizmo);
        technique.setCursorWrapEnabled(true);
        technique.processMousePressedEvent(mouseEvent(200, 200), createViewport());
        technique.processMouseDraggedEvent(mouseEvent(200, -5));
        technique.consumeCursorWarp();

        // Act: some systems inform the placement of the cursor as a moved event
        technique.processMouseMovedEvent(mouseEvent(200, 395));
        const changed = technique.processMouseDraggedEvent(mouseEvent(200, 385));

        // Assert
        expect(changed).toBe(true);
    });

    it("given_typedCoordinates_when_enterPressed_then_gizmoMovesExactlyThere", () => {
        // Arrange
        const gizmo = createGizmo(createCamera());
        const technique = new TranslateGizmoInteractionTechnique(gizmo);

        gizmo.setPosition(new Vector3Dd(1, 2, 3));

        // Act
        const typed = technique.processKeyPressedEvent(keyEvent("4"));
        technique.processKeyPressedEvent(keyEvent("."));
        technique.processKeyPressedEvent(keyEvent("2"));
        technique.processKeyPressedEvent(keyEvent("5"));
        technique.processKeyPressedEvent(keyCode(KeyEvent.KEY_TAB));
        technique.processKeyPressedEvent(keyEvent("7"));
        const moved = technique.processKeyPressedEvent(keyCode(KeyEvent.KEY_ENTER));

        // Assert
        expect(typed).toBe(false);
        expect(moved).toBe(true);
        closeTo(gizmo.getPosition().x(), 4.25, EPS);
        closeTo(gizmo.getPosition().y(), 7.0, EPS);
        closeTo(gizmo.getPosition().z(), 3.0, EPS);
        expect(gizmo.getInputGizmo().isEditing()).toBe(false);
    });

    it("given_typedCoordinates_when_gizmoDragged_then_editionIsDiscarded", () => {
        // Arrange
        const camera = createCamera();
        const gizmo = createGizmo(camera);
        const technique = new TranslateGizmoInteractionTechnique(gizmo);

        technique.processKeyPressedEvent(keyEvent("9"));
        expect(gizmo.getInputGizmo().isEditing()).toBe(true);

        // Act
        technique.processMousePressedEvent(mouseEventAt(camera, new Vector3Dd(0.5, 0, 0)));
        technique.processMouseDraggedEvent(mouseEventAt(camera, new Vector3Dd(1.5, 0, 0)));

        // Assert
        expect(gizmo.getInputGizmo().isEditing()).toBe(false);
    });

    it("given_typedCoordinates_when_movementKeyPressed_then_editionIsDiscarded", () => {
        // Arrange
        const gizmo = createGizmo(createCamera());
        const technique = new TranslateGizmoInteractionTechnique(gizmo);

        technique.processKeyPressedEvent(keyEvent("9"));

        // Act
        technique.processKeyPressedEvent(keyEvent("X"));

        // Assert
        expect(gizmo.getInputGizmo().isEditing()).toBe(false);
        closeTo(gizmo.getPosition().x(), 0.1, EPS);
    });

    it("given_fieldKeys_when_asked_then_techniqueReportsThemAsConsumed", () => {
        // Arrange
        const gizmo = createGizmo(createCamera());
        const technique = new TranslateGizmoInteractionTechnique(gizmo);

        // Act & Assert
        expect(technique.isInputGizmoKey(keyEvent("5"))).toBe(true);
        expect(technique.isInputGizmoKey(keyCode(KeyEvent.KEY_TAB))).toBe(true);
        expect(technique.isInputGizmoKey(keyEvent("x"))).toBe(false);
    });

    it("given_gizmoGroups_when_axisHighlightAsked_then_planesHighlightTheirTwoAxes", () => {
        // Arrange
        const gizmo = createGizmo(createCamera());

        // Act & Assert
        gizmo.setPersistentSelection(TranslateGizmo.Y_AXIS_GROUP);
        expect(gizmo.isAxisHighlighted(0)).toBe(false);
        expect(gizmo.isAxisHighlighted(1)).toBe(true);
        expect(gizmo.isAxisHighlighted(2)).toBe(false);
        gizmo.setPersistentSelection(TranslateGizmo.XZ_PLANE_GROUP);
        expect(gizmo.isAxisHighlighted(0)).toBe(true);
        expect(gizmo.isAxisHighlighted(1)).toBe(false);
        expect(gizmo.isAxisHighlighted(2)).toBe(true);
    });

    it("given_gizmoWithoutTransformation_when_inputGizmoAsked_then_doesNotFail", () => {
        // Arrange
        const gizmo = new TranslateGizmo(createCamera());

        // Act & Assert
        expect(gizmo.getInputGizmo()).not.toBeNull();
        expect(new TranslateGizmoInteractionTechnique(gizmo).isInputGizmoKey(keyEvent("5"))).toBe(true);
    });

    it("given_minusAndDigits_when_enterPressed_then_gizmoMovesToNegativeCoordinate", () => {
        // Arrange
        const gizmo = createGizmo(createCamera());
        const technique = new TranslateGizmoInteractionTechnique(gizmo);

        // Act
        technique.processKeyPressedEvent(keyEvent("-"));
        technique.processKeyPressedEvent(keyEvent("4"));
        technique.processKeyPressedEvent(keyEvent("."));
        technique.processKeyPressedEvent(keyEvent("5"));
        const moved = technique.processKeyPressedEvent(keyCode(KeyEvent.KEY_ENTER));

        // Assert
        expect(moved).toBe(true);
        closeTo(gizmo.getPosition().x(), -4.5, EPS);
    });

    it("given_selectedYField_when_arrowAndPageKeysPressed_then_gizmoMovesInY", () => {
        // Arrange
        const gizmo = createGizmo(createCamera());
        const technique = new TranslateGizmoInteractionTechnique(gizmo);

        technique.processKeyPressedEvent(keyCode(KeyEvent.KEY_TAB));

        // Act
        const up = technique.processKeyPressedEvent(keyCode(KeyEvent.KEY_UP));
        technique.processKeyPressedEvent(keyCode(KeyEvent.KEY_PAGEUP));
        technique.processKeyPressedEvent(keyCode(KeyEvent.KEY_LEFT));

        // Assert: 1 + 5 - 0.1
        expect(up).toBe(true);
        closeTo(gizmo.getPosition().x(), 0.0, EPS);
        closeTo(gizmo.getPosition().y(), 5.9, EPS);
        expect(gizmo.getInputGizmo().isEditing()).toBe(false);
    });
});
