import { describe, expect, it } from "vitest";
import { Camera } from "vsdk/toolkit/environment/camera/Camera.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";

describe("Camera", () => {
    it("generates the centre perspective ray along its front vector", () => {
        const camera = new Camera();
        camera.updateViewportResize(320, 320);
        const ray = camera.generateRay(160, 159);

        expect(ray.getOrigin()).toEqual(new Vector3Dd(0, -5, 1));
        expect(ray.getDirection()).toEqual(new Vector3Dd(0, 1, 0));
    });

    it("generates parallel rays with different origins in orthogonal mode", () => {
        const camera = new Camera();
        camera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        camera.updateViewportResize(100, 100);
        const left = camera.generateRay(0, 50);
        const right = camera.generateRay(99, 50);

        expect(left.getDirection()).toEqual(right.getDirection());
        expect(left.getOrigin()).not.toEqual(right.getOrigin());
    });

    it("exports an immutable frame snapshot and clips against the frustum", () => {
        const camera = new Camera();
        const snapshot = camera.exportToCameraSnapshot(640, 480);
        const clipped = camera.clipLineCohenSutherlandPlanesResult(new Vector3Dd(0, -4.9, 1), new Vector3Dd(0, 200, 1));

        expect(snapshot.getViewportXSize()).toBe(640);
        expect(snapshot.getViewportYSize()).toBe(480);
        expect(clipped).not.toBeNull();
        expect(camera.clipLineCohenSutherlandPlanes(new Vector3Dd(1000, -5, 1), new Vector3Dd(1001, -5, 1))).toBe(
            false,
        );
    });

    it("projects the focus point to the viewport centre and clones independently", () => {
        const camera = new Camera();
        camera.updateViewportResize(640, 480);
        const projected = camera.projectPointResult(camera.getFocusedPosition());
        const rayProjected = camera.projectPointUsingRayMethodResult(camera.getFocusedPosition());
        const copy = camera.clone() as Camera;

        expect(projected).toEqual(new Vector3Dd(320, 240, 0));
        expect(rayProjected).toEqual(new Vector3Dd(320, 240, 0));
        copy.setPosition(new Vector3Dd(1, 2, 3));
        expect(camera.getPosition()).toEqual(new Vector3Dd(0, -5, 1));
    });

    it("normalizes and clips lines in the canonical volume", () => {
        const camera = new Camera();
        const clipped = camera.clipLineCohenSutherlandCanonicVolumeResult(
            new Vector3Dd(0, -4.9, 1),
            new Vector3Dd(0, 200, 1),
        );

        expect(clipped).not.toBeNull();
        expect(clipped![0].z()).toBeLessThanOrEqual(0);
        expect(clipped![1].z()).toBeLessThanOrEqual(0);
        expect(camera.toString()).toContain("<Camera>");
    });

    it("maintains an orthonormal frame and records model modifications", () => {
        const camera = new Camera();
        const version = camera.getModificationVersion();
        camera.setFocusedPositionMaintainingOrthogonality(new Vector3Dd(0, 5, 1));
        camera.setFov(90);

        expect(camera.getFront().length()).toBeCloseTo(1);
        expect(camera.getLeft().length()).toBeCloseTo(1);
        expect(camera.getUp().length()).toBeCloseTo(1);
        expect(camera.getFront().dotProduct(camera.getLeft())).toBeCloseTo(0);
        expect(camera.getFront().dotProduct(camera.getUp())).toBeCloseTo(0);
        expect(camera.getLeft().dotProduct(camera.getUp())).toBeCloseTo(0);
        expect(camera.getModificationVersion()).toBeGreaterThan(version);
    });

    it("builds perspective and orthogonal view-volume matrices", () => {
        const camera = new Camera();
        camera.updateViewportResize(400, 200);
        const perspective = camera.calculateViewVolumeMatrix();
        camera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        const orthogonal = camera.calculateViewVolumeMatrix();

        expect(perspective.get(3, 2)).toBe(-1);
        expect(perspective.get(3, 3)).toBe(0);
        expect(orthogonal.get(3, 3)).toBe(1);
        expect(orthogonal.get(0, 0)).toBeCloseTo(0.5);
        expect(camera.viewport2UnitSquareTransform().multiply(new Vector3Dd(200, 100, 0))).toEqual(
            new Vector3Dd(0, 0, 0),
        );
    });
});
