import { describe, expect, it } from "vitest";
import { Camera } from "vsdk/toolkit/environment/camera/Camera.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { TranslateGizmo } from "vsdk/toolkit/gui/gizmo/TranslateGizmo.js";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";

/**
Exercises the projection of world points to viewport pixels, and its use to
give the translate gizmo a finite apparent size.
 */
describe("CameraProjectionTest", () => {
    function createOrthogonalCamera(): Camera {
        const camera = new Camera();

        camera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        camera.updateViewportResize(400, 400);
        camera.setPosition(new Vector3Dd(0, -5, 0));
        camera.setFocusedPositionMaintainingOrthogonality(new Vector3Dd(0, 5, 0));
        camera.updateVectors();
        return camera;
    }

    it("given_orthogonalCamera_when_projectingOrigin_then_returnsViewportCenter", () => {
        // Arrange
        const camera = createOrthogonalCamera();

        // Act
        const projected = camera.projectPointUsingRayMethod(new Vector3Dd(0, 0, 0));

        // Assert
        expect(projected).not.toBeNull();
        expect(Math.abs(projected!.x() - 200.0)).toBeLessThanOrEqual(1.0e-6);
        expect(Math.abs(projected!.y() - 200.0)).toBeLessThanOrEqual(1.0e-6);
    });

    it("given_orthogonalCamera_when_projectingTwoPoints_then_theyDifferInPixels", () => {
        // Arrange
        const camera = createOrthogonalCamera();
        const right = camera.getLeft().multiply(-1).normalized();

        // Act
        const a = camera.projectPointUsingRayMethod(new Vector3Dd(0, 0, 0));
        const b = camera.projectPointUsingRayMethod(right);

        // Assert
        expect(Vector3Dd.distance(a!, b!)).toBeGreaterThan(1.0);
    });

    it("given_translateGizmo_when_placedInView_then_hasFiniteApparentSize", () => {
        // Arrange
        const camera = createOrthogonalCamera();
        const gizmo = new TranslateGizmo(camera);

        // Act
        gizmo.setTransformationMatrix(new Matrix4x4d());

        // Assert
        expect(gizmo.getElements3dsmax().length).toBeGreaterThan(0);
        expect(Number.isFinite(gizmo.getCurrentScale())).toBe(true);
        expect(gizmo.getCurrentScale()).toBeGreaterThan(0);
    });

    it("given_perspectiveCamera_when_projectingFocusedPoint_then_returnsViewportCenter", () => {
        // Arrange
        const camera = new Camera();

        camera.setProjectionMode(Camera.PROJECTION_MODE_PERSPECTIVE);
        camera.updateViewportResize(400, 300);
        camera.setPosition(new Vector3Dd(0, -5, 0));
        camera.setFocusedPositionMaintainingOrthogonality(new Vector3Dd(0, 0, 0));
        camera.updateVectors();

        // Act
        const projected = camera.projectPoint(new Vector3Dd(0, 0, 0));

        // Assert
        expect(projected).not.toBeNull();
        expect(Math.abs(projected!.x() - 200.0)).toBeLessThanOrEqual(1.0e-6);
        expect(Math.abs(projected!.y() - 150.0)).toBeLessThanOrEqual(1.0e-6);
    });

    it("given_perspectiveCamera_when_projectingPointOutsideViewport_then_returnsNull", () => {
        // Arrange
        const camera = new Camera();

        camera.setProjectionMode(Camera.PROJECTION_MODE_PERSPECTIVE);
        camera.updateViewportResize(400, 300);
        camera.setPosition(new Vector3Dd(0, -5, 0));
        camera.setFocusedPositionMaintainingOrthogonality(new Vector3Dd(0, 0, 0));
        camera.updateVectors();

        // Act
        const projected = camera.projectPoint(new Vector3Dd(1000, 0, 0));

        // Assert
        expect(projected).toBeNull();
    });
});
