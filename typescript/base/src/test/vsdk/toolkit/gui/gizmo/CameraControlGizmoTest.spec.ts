// Port of java/base/src/test/vsdk/toolkit/gui/gizmo/CameraControlGizmoTest.java
import { describe, expect, it } from "vitest";
import { expectNear } from "../../../../GTestAssertions.js";

import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Camera } from "vsdk/toolkit/environment/camera/Camera.js";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";
import { Tube } from "vsdk/toolkit/environment/geometry/volume/Tube.js";
import { MouseEvent } from "vsdk/toolkit/gui/MouseEvent.js";
import { CameraControlGizmo } from "vsdk/toolkit/gui/gizmo/CameraControlGizmo.js";
import { CameraControlGizmoAnimation } from "vsdk/toolkit/gui/gizmo/CameraControlGizmoAnimation.js";
import { CameraControlGizmoInteractionTechnique } from "vsdk/toolkit/gui/gizmo/CameraControlGizmoInteractionTechnique.js";

/**
Exercises the model of the `CameraControlGizmo` (26 regions and a compass),
the views it computes, the camera animation and the mouse interaction.
*/
describe("CameraControlGizmoTest", () => {
    const EPS = 1e-6;

    /** Perspective camera at (0, -5, 0) looking at the origin (+Y), Z up. */
    function frontCamera(): Camera {
        const camera = new Camera();
        camera.setPosition(new Vector3Dd(0, -5, 0));
        camera.setRotation(CameraControlGizmo.createCameraRotation(new Vector3Dd(0, 1, 0), new Vector3Dd(0, 0, 1))!);
        camera.setFocusedPositionDirect(new Vector3Dd(0, 0, 0));
        camera.updateVectors();
        return camera;
    }

    function front(r: Matrix4x4d): Vector3Dd {
        return new Vector3Dd(r.get(0, 0), r.get(1, 0), r.get(2, 0));
    }

    function up(r: Matrix4x4d): Vector3Dd {
        return new Vector3Dd(r.get(0, 2), r.get(1, 2), r.get(2, 2));
    }

    function mouse(x: number, y: number): MouseEvent {
        const e = new MouseEvent();
        e.setX(x);
        e.setY(y);
        e.setButton(MouseEvent.BUTTON1);
        e.setModifiers(MouseEvent.BUTTON1_DOWN_MASK);
        return e;
    }

    it("given_gizmo_when_built_then_has26BoxesAndOneTubeWithExpectedKinds", () => {
        const gizmo = new CameraControlGizmo();
        let faces = 0,
            edges = 0,
            corners = 0;

        expect(gizmo.getParts().size()).toBe(CameraControlGizmo.NUMBER_OF_PARTS);
        for (let r = 0; r < CameraControlGizmo.NUMBER_OF_CUBE_REGIONS; r++) {
            expect(gizmo.getParts().get(r).getGeometry()).toBeInstanceOf(Box);
            switch (gizmo.getRegionKind(r)) {
                case CameraControlGizmo.KIND_FACE:
                    faces++;
                    break;
                case CameraControlGizmo.KIND_EDGE:
                    edges++;
                    break;
                case CameraControlGizmo.KIND_CORNER:
                    corners++;
                    break;
                default:
                    break;
            }
        }
        expect(gizmo.getParts().get(CameraControlGizmo.COMPASS_REGION).getGeometry()).toBeInstanceOf(Tube);
        expect(faces).toBe(6);
        expect(edges).toBe(12);
        expect(corners).toBe(8);
        expect(gizmo.getFaceLabel(gizmo.findRegion(0, 0, 1))).toBe("TOP");
        expect(gizmo.getFaceLabel(gizmo.findRegion(0, -1, 0))).toBe("FRONT");
        expect(gizmo.regionName(gizmo.findRegion(1, -1, 1))).toBe("TOP FRONT RIGHT");
    });

    it("given_frontCamera_when_centerOfGizmoPicked_then_frontFaceIsFound", () => {
        const gizmo = new CameraControlGizmo();
        gizmo.setCamera(frontCamera());
        gizmo.placeInViewport(400, 80, 20, null);
        const cx = gizmo.getHudX() + Math.trunc(gizmo.getSizeInPixels() / 2);
        const cy = gizmo.getHudY() + Math.trunc(gizmo.getSizeInPixels() / 2);

        expect(gizmo.pickRegion(cx, cy)).toBe(gizmo.findRegion(0, -1, 0));
        // Upper part of the square: the top edge of the front face
        const top =
            gizmo.getHudY() +
            Math.trunc(gizmo.getSizeInPixels() * (0.5 - 0.9 / (2 * CameraControlGizmo.HUD_HALF_EXTENT)));
        expect(gizmo.pickRegion(cx, top)).toBe(gizmo.findRegion(0, -1, 1));
        expect(gizmo.pickRegion(gizmo.getHudX() - 1, cy)).toBe(CameraControlGizmo.NO_REGION);
    });

    it("given_frontView_when_topViewComputed_then_lookingDownWithFrontAtBottom", () => {
        const gizmo = new CameraControlGizmo();
        const camera = frontCamera();
        const r = gizmo.calculateViewRotation(gizmo.findRegion(0, 0, 1), camera.getRotation())!;

        expectNear(-1, front(r).z(), EPS);
        // What was ahead (+Y) is up on the screen
        expectNear(1, up(r).y(), EPS);
    });

    it("given_cornerRegion_when_viewComputed_then_lookingFromCornerUpright", () => {
        const gizmo = new CameraControlGizmo();
        const r = gizmo.calculateViewRotation(gizmo.findRegion(1, -1, 1), frontCamera().getRotation())!;
        const k = 1 / Math.sqrt(3);

        expectNear(-k, front(r).x(), EPS);
        expectNear(k, front(r).y(), EPS);
        expectNear(-k, front(r).z(), EPS);
        expect(up(r).z()).toBeGreaterThan(0);
        expect(Math.abs(front(r).dotProduct(up(r)))).toBeLessThan(EPS);
    });

    it("given_animation_when_advanced_then_orbitsPivotAndReachesTarget", () => {
        const gizmo = new CameraControlGizmo();
        const camera = frontCamera();
        const target = gizmo.calculateViewRotation(gizmo.findRegion(1, 0, 0), camera.getRotation())!;
        const animation = new CameraControlGizmoAnimation(camera, target, 1.0);

        expectNear(Math.PI / 2, animation.getAngle(), EPS);
        animation.advance(0);
        animation.advance(500_000_000);
        // Half way: still 5 units from the pivot, turned 45 degrees
        expectNear(5, camera.getPosition().length(), EPS);
        expectNear(-Math.sqrt(0.5), camera.getFront().x(), EPS);
        expect(animation.isRunning()).toBe(true);
        animation.advance(999_999_999);
        expectNear(-1, front(camera.getRotation()).x(), 1e-6);
        animation.advance(1_000_000_000);
        expect(animation.isRunning()).toBe(false);
        expectNear(5, camera.getPosition().x(), EPS);
        expectNear(0, camera.getFocusedPosition().length(), EPS);
    });

    it("given_halfTurn_when_axisAngleExtracted_then_angleIsPi", () => {
        const r = new Matrix4x4d().axisRotation(Math.PI, new Vector3Dd(0, 0, 1));
        const aa = CameraControlGizmoAnimation.extractAxisAngle(r);

        expectNear(Math.PI, aa[3]!, EPS);
        expectNear(1, Math.abs(aa[2]!), EPS);
    });

    it("given_technique_when_clickOnFace_then_animationTurnsCameraToThatView", () => {
        const gizmo = new CameraControlGizmo();
        const camera = frontCamera();
        gizmo.setCamera(camera);
        gizmo.placeInViewport(400, 0, 0, null);
        const technique = new CameraControlGizmoInteractionTechnique(gizmo);
        const cx = gizmo.getHudX() + Math.trunc(gizmo.getSizeInPixels() / 2);
        const top =
            gizmo.getHudY() +
            Math.trunc(gizmo.getSizeInPixels() * (0.5 - 0.9 / (2 * CameraControlGizmo.HUD_HALF_EXTENT)));

        expect(technique.processMouseMovedEvent(mouse(cx, top))).toBe(true);
        expect(gizmo.isActive()).toBe(true);
        expect(technique.processMousePressedEvent(mouse(cx, top))).toBe(true);
        expect(technique.processMouseReleasedEvent(mouse(cx, top))).toBe(true);
        expect(technique.isAnimating()).toBe(true);
        technique.advanceAnimation(0);
        technique.advanceAnimation(10_000_000_000);
        expect(technique.isAnimating()).toBe(false);
        // The top front edge: looking from (0, -1, 1)
        expectNear(Math.sqrt(0.5), camera.getFront().y(), EPS);
        expectNear(-Math.sqrt(0.5), camera.getFront().z(), EPS);
    });

    it("given_technique_when_draggingCube_then_cameraOrbitsKeepingUpright", () => {
        const gizmo = new CameraControlGizmo();
        const camera = frontCamera();
        gizmo.setCamera(camera);
        gizmo.placeInViewport(400, 0, 0, null);
        const technique = new CameraControlGizmoInteractionTechnique(gizmo);
        const cx = gizmo.getHudX() + Math.trunc(gizmo.getSizeInPixels() / 2);
        const cy = gizmo.getHudY() + Math.trunc(gizmo.getSizeInPixels() / 2);

        technique.processMousePressedEvent(mouse(cx, cy));
        expect(technique.processMouseDraggedEvent(mouse(cx + 1, cy))).toBe(false);
        expect(technique.processMouseDraggedEvent(mouse(cx + 50, cy + 30))).toBe(true);
        technique.processMouseReleasedEvent(mouse(cx + 50, cy + 30));

        expect(technique.isAnimating()).toBe(false);
        expectNear(0, camera.getFocusedPosition().length(), EPS);
        expectNear(5, camera.getPosition().length(), EPS);
        // Moved down: the camera went up; moved right: it turned around Z
        expect(camera.getPosition().z()).toBeGreaterThan(0);
        expect(camera.getPosition().x()).toBeLessThan(0);
        expectNear(0, camera.getLeft().z(), EPS);
    });

    it("given_compassDrag_when_cursorMoves_then_grabbedPointStaysUnderCursor", () => {
        const gizmo = new CameraControlGizmo();
        const camera = frontCamera();
        // Looking from above and in front, so the compass is seen well
        camera.setRotation(CameraControlGizmo.createCameraRotation(new Vector3Dd(0, 1, -1), new Vector3Dd(0, 0, 1))!);
        camera.setPosition(new Vector3Dd(0, -5, 5));
        camera.setFocusedPositionDirect(new Vector3Dd(0, 0, 0));
        camera.updateVectors();
        gizmo.setCamera(camera);
        gizmo.placeInViewport(400, 0, 0, null);
        const technique = new CameraControlGizmoInteractionTechnique(gizmo);

        // Find a pixel over the compass, in front of the cube
        let x = -1,
            y = -1;
        const s = gizmo.getSizeInPixels();
        for (let py = s - 1; py >= 0 && x < 0; py--) {
            const px = gizmo.getHudX() + Math.trunc(s / 2);
            if (gizmo.pickRegion(px, gizmo.getHudY() + py) === CameraControlGizmo.COMPASS_REGION) {
                x = px;
                y = gizmo.getHudY() + py;
            }
        }
        expect(x).toBeGreaterThanOrEqual(0);
        const before = CameraControlGizmo.compassAngleAt(
            x - gizmo.getHudX(),
            y - gizmo.getHudY(),
            gizmo.getHudCamera(),
        );

        technique.processMousePressedEvent(mouse(x, y));
        technique.processMouseDraggedEvent(mouse(x + 20, y));
        technique.processMouseReleasedEvent(mouse(x + 20, y));

        // With the new camera, the cursor is over the same point of the compass
        const after = CameraControlGizmo.compassAngleAt(
            x + 20 - gizmo.getHudX(),
            y - gizmo.getHudY(),
            gizmo.getHudCamera(),
        );
        expectNear(before, after, 1e-3);
        expectNear(-Math.sqrt(0.5), camera.getFront().z(), EPS);
    });
});
