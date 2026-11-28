import { describe, expect, it } from "vitest";
import { CameraControllerAquynza } from "vsdk/toolkit/gui/CameraControllerAquynza.js";
import { CameraControllerOrbiter } from "vsdk/toolkit/gui/CameraControllerOrbiter.js";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Camera } from "vsdk/toolkit/environment/camera/Camera.js";
import { MouseEvent } from "vsdk/toolkit/gui/MouseEvent.js";
import { KeyEvent } from "vsdk/toolkit/gui/KeyEvent.js";

describe("CameraControllerInteractionTest", () => {
    function closeTo(actual: number, expected: number, offset: number): void {
        expect(Math.abs(actual - expected)).toBeLessThanOrEqual(offset);
    }

    const EPSILON = 1.0e-12;

    it("given_orbiterSecondaryDrag_when_rotating_then_cameraKeepsOriginalBasisAndAppliesSingleDelta", () => {
        // Arrange
        let camera = new Camera();
        let controller = new CameraControllerOrbiter(camera);
        let originalRotation = camera.getRotation();
        let frontAxis = extractColumn(originalRotation, 0);
        let expectedRotation = new Matrix4x4d()
            .axisRotation(0.05, frontAxis.x(), frontAxis.y(), frontAxis.z())
            .multiply(originalRotation);

        // Action
        controller.processMousePressedEvent(mouseEvent(10, 10, 0));
        let updated = controller.processMouseDraggedEvent(mouseEvent(15, 10, MouseEvent.BUTTON3_DOWN_MASK));

        // Assert
        expect(updated).toBe(true);
        assertMatrixEquals(camera.getRotation(), expectedRotation);
    });

    it("given_aquynzaPrimaryDrag_when_rotating_then_cameraAppliesBothDragDeltasOverOriginalRotation", () => {
        // Arrange
        let camera = new Camera();
        let controller = new CameraControllerAquynza(camera);
        let originalRotation = camera.getRotation();
        let leftAxis = extractColumn(originalRotation, 1);
        let upAxis = extractColumn(originalRotation, 2);
        let expectedRotation = new Matrix4x4d()
            .axisRotation(-0.05, upAxis.x(), upAxis.y(), upAxis.z())
            .multiply(
                new Matrix4x4d()
                    .axisRotation(0.03, leftAxis.x(), leftAxis.y(), leftAxis.z())
                    .multiply(originalRotation),
            );

        // Action
        controller.processMousePressedEvent(mouseEvent(10, 10, 0));
        let updated = controller.processMouseDraggedEvent(mouseEvent(15, 13, MouseEvent.BUTTON1_DOWN_MASK));

        // Assert
        expect(updated).toBe(true);
        assertMatrixEquals(camera.getRotation(), expectedRotation);
    });

    it("given_aquynzaSecondaryAdvanceAfterPress_when_dragging_then_verticalDeltaUsesPressedYCoordinate", () => {
        // Arrange
        let camera = new Camera();
        let controller = new CameraControllerAquynza(camera);
        let originalPosition = camera.getPosition();
        let frontAxis = extractColumn(camera.getRotation(), 0);
        let expectedPosition = originalPosition.subtract(frontAxis.multiply(0.05));

        // Action
        controller.processMousePressedEvent(mouseEvent(0, 100, 0));
        let updated = controller.processMouseDraggedEvent(mouseEvent(0, 101, MouseEvent.BUTTON3_DOWN_MASK));

        // Assert
        expect(updated).toBe(true);
        assertVectorEquals(camera.getPosition(), expectedPosition);
    });

    it("given_orbiterTranslation_when_orbitingAfterwards_then_usesTranslatedFocusAsPointOfInterest", () => {
        // Arrange
        let camera = new Camera();
        let controller = new CameraControllerOrbiter(camera);
        controller.setPointOfInterest(new Vector3Dd(0, 0, 0));

        // Action
        let translated = controller.processKeyPressedEvent(keyEvent(KeyEvent.KEY_X));
        let translatedFocus = camera.getFocusedPosition();
        let orbited = controller.processKeyPressedEvent(keyEvent(KeyEvent.KEY_LEFT));

        // Assert
        expect(translated).toBe(true);
        expect(orbited).toBe(true);
        assertVectorEquals(controller.getPointOfInterest(), translatedFocus);
        assertVectorEquals(camera.getFocusedPosition(), translatedFocus);
    });

    function mouseEvent(x: number, y: number, modifiers: number): MouseEvent {
        let event = new MouseEvent();
        event.setX(x);
        event.setY(y);
        event.setModifiers(modifiers);
        return event;
    }

    function keyEvent(keyCode: number): KeyEvent {
        let event = new KeyEvent();
        event.keycode = keyCode;
        return event;
    }

    function extractColumn(matrix: Matrix4x4d, column: number): Vector3Dd {
        return new Vector3Dd(matrix.get(0, column), matrix.get(1, column), matrix.get(2, column));
    }

    function assertMatrixEquals(actual: Matrix4x4d, expected: Matrix4x4d): void {
        for (let row = 0; row < 3; row++) {
            for (let column = 0; column < 3; column++) {
                closeTo(actual.get(row, column), expected.get(row, column), EPSILON);
            }
        }
    }

    function assertVectorEquals(actual: Vector3Dd, expected: Vector3Dd): void {
        closeTo(actual.x(), expected.x(), EPSILON);
        closeTo(actual.y(), expected.y(), EPSILON);
        closeTo(actual.z(), expected.z(), EPSILON);
    }
});
