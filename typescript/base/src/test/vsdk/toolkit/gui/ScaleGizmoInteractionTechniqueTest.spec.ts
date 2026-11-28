import { describe, expect, it } from "vitest";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Camera } from "vsdk/toolkit/environment/camera/Camera.js";
import { ScaleGizmo } from "vsdk/toolkit/gui/gizmo/ScaleGizmo.js";
import { ScaleGizmoInteractionTechnique } from "vsdk/toolkit/gui/gizmo/ScaleGizmoInteractionTechnique.js";
import { Viewport } from "vsdk/toolkit/gui/viewport/Viewport.js";
import { MouseEvent } from "vsdk/toolkit/gui/MouseEvent.js";
import { KeyEvent } from "vsdk/toolkit/gui/KeyEvent.js";

/**
Exercises the processing of mouse and keyboard events over a scale gizmo,
independently of any rendering technology.
 */
describe("ScaleGizmoInteractionTechniqueTest", () => {
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

    function createGizmo(camera: Camera): ScaleGizmo {
        let gizmo = new ScaleGizmo(camera);

        gizmo.setTransformationMatrix(new Matrix4x4d());
        return gizmo;
    }

    function mouseEvent(x: number, y: number): MouseEvent {
        let event = new MouseEvent();

        event.setX(x);
        event.setY(y);
        return event;
    }

    function mouseEventOverPoint(camera: Camera, point: Vector3Dd): MouseEvent {
        let pixel = camera.projectPointUsingRayMethod(point);

        return mouseEvent(javaRound(pixel.x()), javaRound(pixel.y()));
    }

    it("given_cursorOverAnAxis_when_moved_then_becomesTheVolatileSelection", () => {
        // Arrange
        let camera = createCamera();
        let gizmo = createGizmo(camera);
        let technique = new ScaleGizmoInteractionTechnique(gizmo);

        // Act
        let changed = technique.processMouseMovedEvent(mouseEventOverPoint(camera, gizmo.getTipPosition(1)));

        // Assert
        expect(changed).toBe(true);
        expect(technique.isActive()).toBe(true);
        isEqualTo(gizmo.getVolatileSelection(), ScaleGizmo.Y_AXIS_GROUP);
        isEqualTo(gizmo.getCurrentSelection(), ScaleGizmo.Y_AXIS_GROUP);
    });

    it("given_cursorOverNoHandle_when_moved_then_volatileSelectionIsCleared", () => {
        // Arrange
        let camera = createCamera();
        let gizmo = createGizmo(camera);
        let technique = new ScaleGizmoInteractionTechnique(gizmo);

        technique.processMouseMovedEvent(mouseEventOverPoint(camera, gizmo.getTipPosition(0)));

        // Act
        technique.processMouseMovedEvent(mouseEvent(5, 5));

        // Assert
        expect(technique.isActive()).toBe(false);
        isEqualTo(gizmo.getVolatileSelection(), ScaleGizmo.NULL_GROUP);
    });

    it("given_cursorOverAHandle_when_clicked_then_becomesThePersistentSelection", () => {
        // Arrange
        let camera = createCamera();
        let gizmo = createGizmo(camera);
        let technique = new ScaleGizmoInteractionTechnique(gizmo);

        // Act
        let changed = technique.processMouseClickedEvent(mouseEventOverPoint(camera, gizmo.getTipPosition(2)));

        // Assert
        expect(changed).toBe(true);
        isEqualTo(gizmo.getPersistentSelection(), ScaleGizmo.Z_AXIS_GROUP);
    });

    it("given_clickOverNoHandle_when_processed_then_keepsThePreviousSelection", () => {
        // Arrange
        let camera = createCamera();
        let gizmo = createGizmo(camera);
        let technique = new ScaleGizmoInteractionTechnique(gizmo);

        gizmo.setPersistentSelection(ScaleGizmo.X_AXIS_GROUP);

        // Act
        let changed = technique.processMouseClickedEvent(mouseEvent(5, 5));

        // Assert
        expect(changed).toBe(false);
        isEqualTo(gizmo.getPersistentSelection(), ScaleGizmo.X_AXIS_GROUP);
    });

    it("given_pressOverAnAxis_when_draggedAway_then_onlyThatAxisGrows", () => {
        // Arrange
        let camera = createCamera();
        let gizmo = createGizmo(camera);
        let technique = new ScaleGizmoInteractionTechnique(gizmo);
        let viewport = new Viewport();
        let press = mouseEventOverPoint(camera, gizmo.getTipPosition(0));

        gizmo.setVolatileSelection(gizmo.pickElement(camera.generateRay(press.getX(), press.getY())));
        technique.processMousePressedEvent(press, viewport);

        // Act: drag further away from the projected origin, in screen space
        // (a 3D point that far along the axis could leave the view frustum)
        let origin = camera.projectPointUsingRayMethod(gizmo.getPosition());
        let originX = javaRound(origin.x());
        let originY = javaRound(origin.y());
        let fartherX = originX + 4 * (press.getX() - originX);
        let fartherY = originY + 4 * (press.getY() - originY);
        let changed = technique.processMouseDraggedEvent(mouseEvent(fartherX, fartherY));

        // Assert
        expect(changed).toBe(true);
        expect(gizmo.getScale().x()).toBeGreaterThan(1.0);
        closeTo(gizmo.getScale().y(), 1.0, EPS);
        closeTo(gizmo.getScale().z(), 1.0, EPS);
    });

    it("given_pressOverATwoAxisBand_when_draggedTowardTheGizmo_then_bothAxesShrink", () => {
        // Arrange: the XZ band is the one facing this camera (the other two
        // planes are seen from behind it)
        let camera = createCamera();
        let gizmo = createGizmo(camera);
        let technique = new ScaleGizmoInteractionTechnique(gizmo);
        let viewport = new Viewport();
        let quad = gizmo.buildBandQuad(ScaleGizmo.XZ_GROUP);
        let bandPosition = quad[0].add(quad[1]).add(quad[2]).add(quad[3]).multiply(0.25);
        let press = mouseEventOverPoint(camera, bandPosition);

        gizmo.setVolatileSelection(gizmo.pickElement(camera.generateRay(press.getX(), press.getY())));
        isEqualTo(gizmo.getCurrentSelection(), ScaleGizmo.XZ_GROUP);
        technique.processMousePressedEvent(press, viewport);

        // Act: drag toward the origin of the gizmo
        let changed = technique.processMouseDraggedEvent(mouseEventOverPoint(camera, gizmo.getPosition()));

        // Assert
        expect(changed).toBe(true);
        expect(gizmo.getScale().x()).toBeLessThan(1.0);
        closeTo(gizmo.getScale().y(), 1.0, EPS);
        expect(gizmo.getScale().z()).toBeLessThan(1.0);
        expect(technique.getDragViewport()).toBe(viewport);
    });

    it("given_pressOverTheUniformHandle_when_dragged_then_everyAxisScalesTogether", () => {
        // Arrange
        let camera = createCamera();
        let gizmo = createGizmo(camera);
        let technique = new ScaleGizmoInteractionTechnique(gizmo);
        let viewport = new Viewport();
        let triangles = gizmo.buildUniformTriangles();
        let triangleCentroid = triangles[0]
            .add(triangles[1])
            .add(triangles[2])
            .multiply(1.0 / 3.0);
        let press = mouseEventOverPoint(camera, triangleCentroid);

        gizmo.setVolatileSelection(gizmo.pickElement(camera.generateRay(press.getX(), press.getY())));
        isEqualTo(gizmo.getCurrentSelection(), ScaleGizmo.UNIFORM_GROUP);
        technique.processMousePressedEvent(press, viewport);

        // Drag further away from the projected origin, in screen space (a
        // 3D point that far along the diagonal could leave the view frustum)
        let origin = camera.projectPointUsingRayMethod(gizmo.getPosition());
        let originX = javaRound(origin.x());
        let originY = javaRound(origin.y());
        let fartherX = originX + 4 * (press.getX() - originX);
        let fartherY = originY + 4 * (press.getY() - originY);

        // Act
        let changed = technique.processMouseDraggedEvent(mouseEvent(fartherX, fartherY));

        // Assert
        expect(changed).toBe(true);
        expect(gizmo.getScale().x()).toBeGreaterThan(1.0);
        expect(gizmo.getScale().y()).toBeGreaterThan(1.0);
        expect(gizmo.getScale().z()).toBeGreaterThan(1.0);
        closeTo(gizmo.getScale().x(), gizmo.getScale().y(), EPS);
    });

    it("given_pressOverNoHandle_when_dragged_then_nothingIsScaled", () => {
        // Arrange: a group is selected (i.e. chosen with a previous click),
        // but the press happens far from every handle
        let camera = createCamera();
        let gizmo = createGizmo(camera);
        let technique = new ScaleGizmoInteractionTechnique(gizmo);
        let viewport = new Viewport();

        gizmo.setPersistentSelection(ScaleGizmo.UNIFORM_GROUP);
        technique.processMousePressedEvent(mouseEvent(5, 5), viewport);

        // Act
        let changed = technique.processMouseDraggedEvent(mouseEvent(200, 200));

        // Assert
        expect(changed).toBe(false);
        expect(technique.isDragging()).toBe(false);
        expect(technique.getDragViewport()).toBeNull();
        closeTo(gizmo.getScale().x(), 1.0, EPS);
        closeTo(gizmo.getScale().y(), 1.0, EPS);
        closeTo(gizmo.getScale().z(), 1.0, EPS);
    });

    it("given_dragInCourse_when_released_then_gestureEnds", () => {
        // Arrange
        let camera = createCamera();
        let gizmo = createGizmo(camera);
        let technique = new ScaleGizmoInteractionTechnique(gizmo);
        let viewport = new Viewport();
        let press = mouseEventOverPoint(camera, gizmo.getTipPosition(0));

        gizmo.setVolatileSelection(gizmo.pickElement(camera.generateRay(press.getX(), press.getY())));
        technique.processMousePressedEvent(press, viewport);

        // Act
        technique.processMouseReleasedEvent(press);

        // Assert
        expect(technique.getDragViewport()).toBeNull();
    });

    it("given_keyEvent_when_isInputGizmoKeyAsked_then_delegatesToTheGizmoInputBoxes", () => {
        // Arrange
        let gizmo = createGizmo(createCamera());
        let technique = new ScaleGizmoInteractionTechnique(gizmo);
        let digit = new KeyEvent();

        digit.unicodeId = "5".charCodeAt(0);

        // Act & Assert
        expect(technique.isInputGizmoKey(digit)).toBe(true);
    });
});
