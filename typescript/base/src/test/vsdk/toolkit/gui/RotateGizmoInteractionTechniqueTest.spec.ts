import { describe, expect, it } from "vitest";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Camera } from "vsdk/toolkit/environment/camera/Camera.js";
import { RotateGizmo } from "vsdk/toolkit/gui/gizmo/RotateGizmo.js";
import { RotateGizmoInteractionTechnique } from "vsdk/toolkit/gui/gizmo/RotateGizmoInteractionTechnique.js";
import { Viewport } from "vsdk/toolkit/gui/viewport/Viewport.js";
import { MouseEvent } from "vsdk/toolkit/gui/MouseEvent.js";
import { KeyEvent } from "vsdk/toolkit/gui/KeyEvent.js";
import { Math as JavaMath } from "java/lang/Math.js";

/**
Exercises the processing of mouse and keyboard events over a rotate gizmo,
independently of any rendering technology.
 */
describe("RotateGizmoInteractionTechniqueTest", () => {
    function closeTo(actual: number, expected: number, offset: number): void {
        expect(Math.abs(actual - expected)).toBeLessThanOrEqual(offset);
    }

    /** AssertJ `isEqualTo`: Java `equals` when the value defines it. */
    function isEqualTo(actual: unknown, expected: unknown): void {
        const a = actual as { equals?: (other: unknown) => boolean } | null;
        if (a !== null && typeof a === "object" && typeof a.equals === "function") {
            expect(a.equals(expected)).toBe(true);
        } else {
            expect(actual).toEqual(expected);
        }
    }

    /** Java `(int)Math.round(double)`. */
    function javaRound(value: number): number {
        return Math.floor(value + 0.5);
    }

    const EPS = 1.0e-6;

    function createCamera(): Camera {
        let camera = new Camera();

        camera.setProjectionMode(Camera.PROJECTION_MODE_PERSPECTIVE);
        camera.updateViewportResize(400, 400);
        camera.setPosition(new Vector3Dd(5, -6, 4));
        camera.setFocusedPositionMaintainingOrthogonality(new Vector3Dd(0, 0, 0));
        camera.updateVectors();
        return camera;
    }

    function createGizmo(camera: Camera): RotateGizmo {
        let gizmo = new RotateGizmo(camera);

        gizmo.setTransformationMatrix(new Matrix4x4d());
        return gizmo;
    }

    function mouseEvent(x: number, y: number): MouseEvent {
        let event = new MouseEvent();

        event.setX(x);
        event.setY(y);
        return event;
    }

    /// Angle (degrees), for each ring, that is both far from the places
    /// where the rings cross each other and on the half of `createCamera()`
    /// that faces it (the far half of a ring cannot be picked)
    const VISIBLE_ANGLE_DEGREES = [145, 80, 280];

    /**
    @return an event over a point of the given ring, at an angle far from the
    places where the rings cross each other, and picakble (see
    `VISIBLE_ANGLE_DEGREES`)
    */
    function mouseEventOverRing(camera: Camera, gizmo: RotateGizmo, ring: number): MouseEvent {
        let axes = [new Vector3Dd(1, 0, 0), new Vector3Dd(0, 1, 0), new Vector3Dd(0, 0, 1)];
        let angle = JavaMath.toRadians(VISIBLE_ANGLE_DEGREES[ring]);
        let point = gizmo
            .getPosition()
            .add(axes[(ring + 1) % 3].multiply(Math.cos(angle) * gizmo.getRingRadius()))
            .add(axes[(ring + 2) % 3].multiply(Math.sin(angle) * gizmo.getRingRadius()));
        let pixel = camera.projectPointUsingRayMethod(point);

        return mouseEvent(javaRound(pixel.x()), javaRound(pixel.y()));
    }

    function character(unicode: string): KeyEvent {
        let event = new KeyEvent();

        event.unicodeId = unicode.charCodeAt(0);
        return event;
    }

    function key(keycode: number): KeyEvent {
        let event = new KeyEvent();

        event.keycode = keycode;
        return event;
    }

    function type(technique: RotateGizmoInteractionTechnique, text: string): void {
        for (const c of text) {
            technique.processKeyPressedEvent(character(c));
        }
    }

    //= Mouse =============================================================

    it("given_cursorOverRing_when_moved_then_ringIsVolatileSelectionAndYellow", () => {
        // Arrange
        let camera = createCamera();
        let gizmo = createGizmo(camera);
        let technique = new RotateGizmoInteractionTechnique(gizmo);

        // Act
        let changed = technique.processMouseMovedEvent(mouseEventOverRing(camera, gizmo, 1));

        // Assert
        expect(changed).toBe(true);
        expect(technique.isActive()).toBe(true);
        isEqualTo(gizmo.getVolatileSelection(), RotateGizmo.Y_RING_GROUP);
        isEqualTo(gizmo.getPersistentSelection(), RotateGizmo.NULL_GROUP);
        isEqualTo(gizmo.getRingColor(1).r(), 1.0);
        isEqualTo(gizmo.getRingColor(1).g(), 1.0);
        isEqualTo(gizmo.getRingColor(1).b(), 0.0);
    });

    it("given_cursorOverRing_when_movedAgainOverTheSame_then_noChangeIsReported", () => {
        // Arrange
        let camera = createCamera();
        let gizmo = createGizmo(camera);
        let technique = new RotateGizmoInteractionTechnique(gizmo);

        technique.processMouseMovedEvent(mouseEventOverRing(camera, gizmo, 0));

        // Act
        let changed = technique.processMouseMovedEvent(mouseEventOverRing(camera, gizmo, 0));

        // Assert
        expect(changed).toBe(false);
    });

    it("given_cursorOverRing_when_leavesIt_then_selectionIsCleared", () => {
        // Arrange
        let camera = createCamera();
        let gizmo = createGizmo(camera);
        let technique = new RotateGizmoInteractionTechnique(gizmo);

        technique.processMouseMovedEvent(mouseEventOverRing(camera, gizmo, 2));

        // Act
        let changed = technique.processMouseMovedEvent(mouseEvent(2, 2));

        // Assert
        expect(changed).toBe(true);
        expect(technique.isActive()).toBe(false);
        isEqualTo(gizmo.getVolatileSelection(), RotateGizmo.NULL_GROUP);
    });

    it("given_cursorOverRing_when_clicked_then_ringStaysSelectedAfterCursorLeaves", () => {
        // Arrange
        let camera = createCamera();
        let gizmo = createGizmo(camera);
        let technique = new RotateGizmoInteractionTechnique(gizmo);
        let overRing = mouseEventOverRing(camera, gizmo, 0);

        technique.processMouseMovedEvent(overRing);

        // Act
        technique.processMouseClickedEvent(overRing);
        technique.processMouseMovedEvent(mouseEvent(2, 2));

        // Assert
        isEqualTo(gizmo.getPersistentSelection(), RotateGizmo.X_RING_GROUP);
        isEqualTo(gizmo.getCurrentSelection(), RotateGizmo.X_RING_GROUP);
        expect(gizmo.isRingHighlighted(0)).toBe(true);
        expect(gizmo.isRingHighlighted(1)).toBe(false);
    });

    it("given_chosenRing_when_clickedOverNothing_then_selectionIsKept", () => {
        // Arrange
        let camera = createCamera();
        let gizmo = createGizmo(camera);
        let technique = new RotateGizmoInteractionTechnique(gizmo);

        technique.processMouseClickedEvent(mouseEventOverRing(camera, gizmo, 2));

        // Act
        technique.processMouseClickedEvent(mouseEvent(2, 2));

        // Assert
        isEqualTo(gizmo.getPersistentSelection(), RotateGizmo.Z_RING_GROUP);
    });

    it("given_chosenRing_when_clickedOverAnotherOne_then_thatOneIsChosen", () => {
        // Arrange
        let camera = createCamera();
        let gizmo = createGizmo(camera);
        let technique = new RotateGizmoInteractionTechnique(gizmo);

        technique.processMouseClickedEvent(mouseEventOverRing(camera, gizmo, 2));

        // Act
        technique.processMouseClickedEvent(mouseEventOverRing(camera, gizmo, 1));

        // Assert
        isEqualTo(gizmo.getPersistentSelection(), RotateGizmo.Y_RING_GROUP);
    });

    it("given_pressWithoutPreviousMove_when_overRing_then_gizmoIsActiveAndRingIsChosen", () => {
        // Arrange
        let camera = createCamera();
        let gizmo = createGizmo(camera);
        let technique = new RotateGizmoInteractionTechnique(gizmo);

        // Act
        let changed = technique.processMousePressedEvent(mouseEventOverRing(camera, gizmo, 1));

        // Assert
        expect(changed).toBe(false);
        expect(technique.isActive()).toBe(true);
        isEqualTo(gizmo.getPersistentSelection(), RotateGizmo.Y_RING_GROUP);
        expect(gizmo.getTransformationMatrix().epsilonEquals(new Matrix4x4d(), EPS)).toBe(true);
    });

    //= Keyboard: numeric input ===========================================

    it("given_typedAngleAndEnter_when_keyPressed_then_gizmoTakesExactlyThatOrientation", () => {
        // Arrange
        let gizmo = new RotateGizmo(createCamera());

        gizmo.setTransformationMatrix(new Matrix4x4d().withTranslation(new Vector3Dd(1, 2, 3)));
        let technique = new RotateGizmoInteractionTechnique(gizmo);

        // Act: 37.25 degrees around X (first box selected)
        type(technique, "37.25");
        let changed = technique.processKeyPressedEvent(key(KeyEvent.KEY_ENTER));

        // Assert
        let angles = RotateGizmo.extractAnglesInDegrees(gizmo.getTransformationMatrix());

        expect(changed).toBe(true);
        closeTo(angles[0], 37.25, EPS);
        closeTo(angles[1], 0.0, EPS);
        closeTo(angles[2], 0.0, EPS);
        expect(Vector3Dd.distance(gizmo.getPosition(), new Vector3Dd(1, 2, 3))).toBeLessThan(EPS);
        expect(gizmo.getInputGizmo().isEditing()).toBe(false);
    });

    it("given_anglesTypedInSeveralBoxes_when_enterPressed_then_allAreApplied", () => {
        // Arrange
        let gizmo = createGizmo(createCamera());
        let technique = new RotateGizmoInteractionTechnique(gizmo);

        // Act
        type(technique, "-30");
        technique.processKeyPressedEvent(key(KeyEvent.KEY_TAB));
        type(technique, "45.5");
        technique.processKeyPressedEvent(key(KeyEvent.KEY_TAB));
        type(technique, "120");
        technique.processKeyPressedEvent(key(KeyEvent.KEY_ENTER));

        // Assert
        let angles = RotateGizmo.extractAnglesInDegrees(gizmo.getTransformationMatrix());

        closeTo(angles[0], -30.0, EPS);
        closeTo(angles[1], 45.5, EPS);
        closeTo(angles[2], 120.0, EPS);
    });

    it("given_typedNumberWithManyDecimals_when_typing_then_onlyTwoDecimalsAreAccepted", () => {
        // Arrange
        let gizmo = createGizmo(createCamera());
        let technique = new RotateGizmoInteractionTechnique(gizmo);

        // Act
        type(technique, "12.3456");

        // Assert
        isEqualTo(gizmo.getInputGizmo().getEditText(0), "12.34");
    });

    it("given_typedAngleWithoutEnter_when_keyPressed_then_gizmoDoesNotChange", () => {
        // Arrange
        let gizmo = createGizmo(createCamera());
        let technique = new RotateGizmoInteractionTechnique(gizmo);

        // Act
        type(technique, "45");

        // Assert
        expect(gizmo.getTransformationMatrix().epsilonEquals(new Matrix4x4d(), EPS)).toBe(true);
        expect(gizmo.getInputGizmo().isEditing(0)).toBe(true);
    });

    it("given_typedAngle_when_escapePressed_then_editionIsDiscarded", () => {
        // Arrange
        let gizmo = createGizmo(createCamera());
        let technique = new RotateGizmoInteractionTechnique(gizmo);

        type(technique, "45");

        // Act
        let changed = technique.processKeyPressedEvent(key(KeyEvent.KEY_ESC));

        // Assert
        expect(changed).toBe(false);
        expect(gizmo.getInputGizmo().isEditing()).toBe(false);
        expect(gizmo.getTransformationMatrix().epsilonEquals(new Matrix4x4d(), EPS)).toBe(true);
    });

    it("given_steppingKey_when_pressed_then_angleChangesRightAway", () => {
        // Arrange
        let gizmo = createGizmo(createCamera());
        let technique = new RotateGizmoInteractionTechnique(gizmo);

        // Act: UP steps by the level 2 rotation increment (5 degrees), first
        // box (X angle) -- see InputGizmoValueChangeRules.forRotation()
        let changed = technique.processKeyPressedEvent(key(KeyEvent.KEY_UP));

        // Assert
        expect(changed).toBe(true);
        closeTo(RotateGizmo.extractAnglesInDegrees(gizmo.getTransformationMatrix())[0], 5.0, EPS);
    });

    it("given_keys_when_askedIfInputGizmoUsesThem_then_onlyNumbersAndEditionKeysAre", () => {
        // Arrange
        let gizmo = createGizmo(createCamera());
        let technique = new RotateGizmoInteractionTechnique(gizmo);

        // Act & Assert
        expect(technique.isInputGizmoKey(character("5"))).toBe(true);
        expect(technique.isInputGizmoKey(character("-"))).toBe(true);
        expect(technique.isInputGizmoKey(key(KeyEvent.KEY_TAB))).toBe(true);
        expect(technique.isInputGizmoKey(character("x"))).toBe(false);
        expect(technique.isInputGizmoKey(key(KeyEvent.KEY_ENTER))).toBe(false);
    });

    //= Keyboard: rotation keys ===========================================

    it("given_upperCaseXKey_when_keyPressed_then_gizmoRotatesOneDegreeAroundItsXAxis", () => {
        // Arrange
        let gizmo = new RotateGizmo(createCamera());

        gizmo.setTransformationMatrix(new Matrix4x4d().withTranslation(new Vector3Dd(1, 0, 0)));
        let technique = new RotateGizmoInteractionTechnique(gizmo);

        // Act
        let changed = technique.processKeyPressedEvent(character("X"));

        // Assert
        let angles = RotateGizmo.extractAnglesInDegrees(gizmo.getTransformationMatrix());

        expect(changed).toBe(true);
        closeTo(angles[0], 1.0, EPS);
        expect(Vector3Dd.distance(gizmo.getPosition(), new Vector3Dd(1, 0, 0))).toBeLessThan(EPS);
    });

    it("given_lowerCaseKeys_when_keyPressed_then_gizmoRotatesTheOtherWayAroundEachAxis", () => {
        // Arrange
        let gizmo = createGizmo(createCamera());
        let technique = new RotateGizmoInteractionTechnique(gizmo);

        // Act
        technique.processKeyPressedEvent(character("z"));
        technique.processKeyPressedEvent(character("z"));

        // Assert
        let angles = RotateGizmo.extractAnglesInDegrees(gizmo.getTransformationMatrix());

        closeTo(angles[2], -2.0, EPS);
    });

    it("given_unrelatedKey_when_keyPressed_then_nothingChanges", () => {
        // Arrange
        let gizmo = createGizmo(createCamera());
        let technique = new RotateGizmoInteractionTechnique(gizmo);

        // Act
        let changed = technique.processKeyPressedEvent(character("q"));

        // Assert
        expect(changed).toBe(false);
        expect(gizmo.getTransformationMatrix().epsilonEquals(new Matrix4x4d(), EPS)).toBe(true);
    });

    it("given_typedNumber_when_rotationKeyPressed_then_editionIsDiscarded", () => {
        // Arrange
        let gizmo = createGizmo(createCamera());
        let technique = new RotateGizmoInteractionTechnique(gizmo);

        type(technique, "45");

        // Act
        technique.processKeyPressedEvent(character("Y"));

        // Assert
        expect(gizmo.getInputGizmo().isEditing()).toBe(false);
    });

    //= Mouse: drag =======================================================

    /**
    @param frame frame of the gizmo when the gesture started (the gizmo turns
    while it is dragged, and the cursor is moved over the ring as it was)
    @return an event over the point of a ring, in the frame given, at the
    given angle around its axis
    */
    function mouseEventOverRingAt(
        camera: Camera,
        gizmo: RotateGizmo,
        frame: Matrix4x4d,
        ring: number,
        degrees: number,
    ): MouseEvent {
        let rotation = frame.withoutTranslation();
        let u = rotation.multiply(axis((ring + 1) % 3));
        let v = rotation.multiply(axis((ring + 2) % 3));
        let angle = JavaMath.toRadians(degrees);
        let point = frame
            .extractTranslation()
            .add(u.multiply(Math.cos(angle) * gizmo.getRingRadius()))
            .add(v.multiply(Math.sin(angle) * gizmo.getRingRadius()));
        let pixel = camera.projectPointUsingRayMethod(point);

        return mouseEvent(javaRound(pixel.x()), javaRound(pixel.y()));
    }

    function axis(i: number): Vector3Dd {
        return new Vector3Dd(i === 0 ? 1 : 0, i === 1 ? 1 : 0, i === 2 ? 1 : 0);
    }

    it("given_dragOverEachRing_when_cursorGoesAroundTheAxis_then_gizmoRotatesThatAngleAroundIt", () => {
        // The press must land on the half of each ring that faces the
        // camera (see VISIBLE_ANGLE_DEGREES); once dragging starts, the
        // angle is tracked from the cursor regardless of that (only the
        // initial pick needs a visible point)
        let startDegrees = [125, 125, 250];

        for (let ring = 0; ring < 3; ring++) {
            // Arrange
            let camera = createCamera();
            let gizmo = createGizmo(camera);
            let technique = new RotateGizmoInteractionTechnique(gizmo);
            let frame = new Matrix4x4d(gizmo.getTransformationMatrix());
            let start = startDegrees[ring];

            technique.processMousePressedEvent(mouseEventOverRingAt(camera, gizmo, frame, ring, start));

            // Act: the cursor follows the point of the ring 60 degrees on
            let changed = false;

            for (let degrees = start; degrees <= start + 60; degrees += 5) {
                changed =
                    technique.processMouseDraggedEvent(mouseEventOverRingAt(camera, gizmo, frame, ring, degrees)) ||
                    changed;
            }

            // Assert
            let angles = RotateGizmo.extractAnglesInDegrees(gizmo.getTransformationMatrix());

            expect(changed).toBe(true);
            // Integer pixels in a plane seen obliquely give about a couple of degrees of error
            closeTo(angles[ring], 60.0, 3.0);
            closeTo(angles[(ring + 1) % 3], 0.0, EPS);
            closeTo(angles[(ring + 2) % 3], 0.0, EPS);
            closeTo(gizmo.getArcSweepInDegrees(), 60.0, 3.0);
            isEqualTo(gizmo.getArcRing(), ring);
        }
    });

    it("given_dragging_when_cursorGoesTheOtherWay_then_rotationAndArcAreNegative", () => {
        // Arrange
        let camera = createCamera();
        let gizmo = createGizmo(camera);
        let technique = new RotateGizmoInteractionTechnique(gizmo);
        let frame = new Matrix4x4d(gizmo.getTransformationMatrix());

        // The press must land on the half of the ring that faces the camera
        technique.processMousePressedEvent(mouseEventOverRingAt(camera, gizmo, frame, 2, 310));

        // Act
        for (let degrees = 310; degrees >= 250; degrees -= 5) {
            technique.processMouseDraggedEvent(mouseEventOverRingAt(camera, gizmo, frame, 2, degrees));
        }

        // Assert
        closeTo(RotateGizmo.extractAnglesInDegrees(gizmo.getTransformationMatrix())[2], -60.0, 1.5);
        closeTo(gizmo.getArcSweepInDegrees(), -60.0, 1.5);
    });

    it("given_dragging_when_pointOfTheRingIsGrabbed_then_itStaysUnderTheCursor", () => {
        // Arrange: the gizmo with an orientation and a position
        let camera = createCamera();
        let gizmo = new RotateGizmo(camera);

        gizmo.setTransformationMatrix(
            RotateGizmo.createRotationFromAnglesInDegrees(30, 20, 10).withTranslation(new Vector3Dd(0.5, -0.5, 0.3)),
        );
        let technique = new RotateGizmoInteractionTechnique(gizmo);
        let frame = new Matrix4x4d(gizmo.getTransformationMatrix());
        let start = new Matrix4x4d(gizmo.getTransformationMatrix());
        let grab = mouseEventOverRingAt(camera, gizmo, frame, 1, 50);

        technique.processMousePressedEvent(grab);
        expect(technique.isDragging()).toBe(true);

        // Act: drag until the cursor is over the point at 130 degrees
        let target = mouseEventOverRingAt(camera, gizmo, frame, 1, 130);

        for (let step = 0; step <= 16; step++) {
            technique.processMouseDraggedEvent(mouseEventOverRingAt(camera, gizmo, frame, 1, 50 + 5 * step));
        }

        // Assert: the point that was at 50 degrees is now where the cursor is,
        // the pixel of the one at 130 degrees of the ring in the start frame
        let moved = gizmo.getTransformationMatrix();
        let grabbed = new Vector3Dd(Math.sin(JavaMath.toRadians(50)), 0, Math.cos(JavaMath.toRadians(50))).multiply(
            gizmo.getRingRadius(),
        );
        let pixel = camera.projectPointUsingRayMethod(moved.multiply(grabbed));

        closeTo(pixel.x(), target.getX(), 1.5);
        closeTo(pixel.y(), target.getY(), 1.5);
        // Axis and center do not move, the frame is the start one turned around its axis
        expect(
            Vector3Dd.distance(gizmo.getAxisDirection(1), start.withoutTranslation().multiply(new Vector3Dd(0, 1, 0))),
        ).toBeLessThan(EPS);
        expect(Vector3Dd.distance(gizmo.getPosition(), new Vector3Dd(0.5, -0.5, 0.3))).toBeLessThan(EPS);
    });

    it("given_cursorGoingSeveralTurns_when_dragging_then_rotationAccumulatesWithoutJumps", () => {
        // Arrange
        let camera = createCamera();
        let gizmo = createGizmo(camera);
        let technique = new RotateGizmoInteractionTechnique(gizmo);
        let frame = new Matrix4x4d(gizmo.getTransformationMatrix());

        technique.processMousePressedEvent(mouseEventOverRingAt(camera, gizmo, frame, 2, 0));

        // Act: two turns and a half, with steps of 10 degrees
        for (let degrees = 0; degrees <= 900; degrees += 10) {
            technique.processMouseDraggedEvent(mouseEventOverRingAt(camera, gizmo, frame, 2, degrees));
        }

        // Assert
        closeTo(gizmo.getArcSweepInDegrees(), 900.0, 3.0);
        // 900 degrees are 180 degrees, that can be reported as +180 or -180
        let turned = RotateGizmo.extractAnglesInDegrees(gizmo.getTransformationMatrix())[2];

        closeTo(Math.abs(turned), 180.0, 3.0);
    });

    it("given_dragging_when_released_then_arcDisappearsAndRotationIsKept", () => {
        // Arrange
        let camera = createCamera();
        let gizmo = createGizmo(camera);
        let technique = new RotateGizmoInteractionTechnique(gizmo);
        let frame = new Matrix4x4d(gizmo.getTransformationMatrix());

        technique.processMousePressedEvent(mouseEventOverRingAt(camera, gizmo, frame, 0, 125));
        technique.processMouseDraggedEvent(mouseEventOverRingAt(camera, gizmo, frame, 0, 125));
        technique.processMouseDraggedEvent(mouseEventOverRingAt(camera, gizmo, frame, 0, 155));
        expect(gizmo.isArcVisible()).toBe(true);

        // Act
        let changed = technique.processMouseReleasedEvent(mouseEvent(0, 0));

        // Assert
        expect(changed).toBe(true);
        expect(gizmo.isArcVisible()).toBe(false);
        expect(technique.isDragging()).toBe(false);
        closeTo(RotateGizmo.extractAnglesInDegrees(gizmo.getTransformationMatrix())[0], 30.0, 1.5);
        // Dragging without a gesture does nothing
        expect(technique.processMouseDraggedEvent(mouseEventOverRingAt(camera, gizmo, frame, 0, 90))).toBe(false);
        expect(technique.processMouseReleasedEvent(mouseEvent(0, 0))).toBe(false);
    });

    it("given_pressOverNothing_when_dragged_then_nothingRotates", () => {
        // Arrange
        let camera = createCamera();
        let gizmo = createGizmo(camera);
        let technique = new RotateGizmoInteractionTechnique(gizmo);
        let frame = new Matrix4x4d(gizmo.getTransformationMatrix());

        technique.processMousePressedEvent(mouseEvent(2, 2));

        // Act
        let changed = technique.processMouseDraggedEvent(mouseEventOverRingAt(camera, gizmo, frame, 0, 90));

        // Assert
        expect(technique.isDragging()).toBe(false);
        expect(changed).toBe(false);
        expect(gizmo.getTransformationMatrix().epsilonEquals(new Matrix4x4d(), EPS)).toBe(true);
    });

    it("given_pressOverRing_when_dragging_then_ringIsChosenAndHoverIsIgnored", () => {
        // Arrange
        let camera = createCamera();
        let gizmo = createGizmo(camera);
        let technique = new RotateGizmoInteractionTechnique(gizmo);
        let frame = new Matrix4x4d(gizmo.getTransformationMatrix());

        technique.processMousePressedEvent(mouseEventOverRingAt(camera, gizmo, frame, 1, 50));

        // Act: the cursor goes over another ring
        let changed = technique.processMouseMovedEvent(mouseEventOverRingAt(camera, gizmo, frame, 2, 125));

        // Assert
        expect(changed).toBe(false);
        isEqualTo(gizmo.getPersistentSelection(), RotateGizmo.Y_RING_GROUP);
        isEqualTo(gizmo.getCurrentSelection(), RotateGizmo.Y_RING_GROUP);
    });

    it("given_viewport_when_pressedOverRing_then_gestureBelongsToItUntilRelease", () => {
        // Arrange
        let camera = createCamera();
        let gizmo = createGizmo(camera);
        let technique = new RotateGizmoInteractionTechnique(gizmo);
        let frame = new Matrix4x4d(gizmo.getTransformationMatrix());
        let viewport = new Viewport();

        // Act & Assert
        technique.processMousePressedEvent(mouseEvent(2, 2), viewport);
        expect(technique.getDragViewport()).toBeNull();

        technique.processMousePressedEvent(mouseEventOverRingAt(camera, gizmo, frame, 1, 50), viewport);
        expect(technique.getDragViewport()).toBe(viewport);

        technique.processMouseReleasedEvent(mouseEvent(0, 0));
        expect(technique.getDragViewport()).toBeNull();
    });

    it("given_ringSeenEdgeOn_when_dragged_then_gizmoDoesNotJump", () => {
        // Arrange: camera in the plane of the ring around the X axis (YZ)
        let camera = new Camera();

        camera.setProjectionMode(Camera.PROJECTION_MODE_PERSPECTIVE);
        camera.updateViewportResize(400, 400);
        camera.setPosition(new Vector3Dd(0, -10, 0));
        camera.setFocusedPositionMaintainingOrthogonality(new Vector3Dd(0, 0, 0));
        camera.updateVectors();
        let gizmo = createGizmo(camera);
        let technique = new RotateGizmoInteractionTechnique(gizmo);
        let frame = new Matrix4x4d(gizmo.getTransformationMatrix());

        technique.processMousePressedEvent(mouseEventOverRingAt(camera, gizmo, frame, 0, 90));
        expect(technique.isDragging()).toBe(true);

        // Act
        let changed = technique.processMouseDraggedEvent(mouseEventOverRingAt(camera, gizmo, frame, 0, 90));

        // Assert: ring axis is perpendicular to the view ray, no angle can be measured
        expect(changed).toBe(false);
        expect(gizmo.getTransformationMatrix().epsilonEquals(new Matrix4x4d(), EPS)).toBe(true);
    });

    it("given_orthogonalCamera_when_dragging_then_rotationFollowsTheCursor", () => {
        // Arrange
        let camera = createCamera();

        camera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        camera.updateVectors();
        let gizmo = createGizmo(camera);
        let technique = new RotateGizmoInteractionTechnique(gizmo);
        let frame = new Matrix4x4d(gizmo.getTransformationMatrix());

        technique.processMousePressedEvent(mouseEventOverRingAt(camera, gizmo, frame, 2, 20));

        // Act
        for (let degrees = 20; degrees <= 80; degrees += 5) {
            technique.processMouseDraggedEvent(mouseEventOverRingAt(camera, gizmo, frame, 2, degrees));
        }

        // Assert
        closeTo(RotateGizmo.extractAnglesInDegrees(gizmo.getTransformationMatrix())[2], 60.0, 1.5);
    });

    it("given_typedNumber_when_dragging_then_editionIsDiscarded", () => {
        // Arrange
        let camera = createCamera();
        let gizmo = createGizmo(camera);
        let technique = new RotateGizmoInteractionTechnique(gizmo);
        let frame = new Matrix4x4d(gizmo.getTransformationMatrix());

        type(technique, "45");
        technique.processMousePressedEvent(mouseEventOverRingAt(camera, gizmo, frame, 2, 20));

        // Act
        technique.processMouseDraggedEvent(mouseEventOverRingAt(camera, gizmo, frame, 2, 20));
        technique.processMouseDraggedEvent(mouseEventOverRingAt(camera, gizmo, frame, 2, 40));

        // Assert
        expect(gizmo.getInputGizmo().isEditing()).toBe(false);
    });

    it("given_cursorMovedFarBeforeTheFirstDragEvent_when_dragging_then_thatMovementIsNotLost", () => {
        // Arrange
        let camera = createCamera();
        let gizmo = createGizmo(camera);
        let technique = new RotateGizmoInteractionTechnique(gizmo);
        let frame = new Matrix4x4d(gizmo.getTransformationMatrix());

        // The press must land on the half of the ring that faces the camera
        technique.processMousePressedEvent(mouseEventOverRingAt(camera, gizmo, frame, 2, 250));

        // Act: the first drag event is already 40 degrees away
        technique.processMouseDraggedEvent(mouseEventOverRingAt(camera, gizmo, frame, 2, 290));

        // Assert
        closeTo(gizmo.getArcSweepInDegrees(), 40.0, 3.0);
        closeTo(RotateGizmo.extractAnglesInDegrees(gizmo.getTransformationMatrix())[2], 40.0, 3.0);
    });
});
