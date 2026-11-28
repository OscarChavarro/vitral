import { describe, expect, it } from "vitest";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Camera } from "vsdk/toolkit/environment/camera/Camera.js";
import { Intersection } from "vsdk/toolkit/environment/geometry/element/Intersection.js";
import { Ray } from "vsdk/toolkit/environment/geometry/element/Ray.js";
import { Arrow } from "vsdk/toolkit/environment/geometry/volume/Arrow.js";
import { RayGizmo } from "vsdk/toolkit/gui/gizmo/RayGizmo.js";
import { Math as JavaMath } from "java/lang/Math.js";

/** Verifies that ray-gizmo thickness follows the camera view, not JOGL. */
describe("RayGizmoViewThicknessTest", () => {
    function closeTo(actual: number, expected: number, offset: number): void {
        expect(Math.abs(actual - expected)).toBeLessThanOrEqual(offset);
    }

    const EPS = 1.0e-9;

    it("perspectiveThickness_isNormalizedAgainstTheVerticalFov", () => {
        let camera = new Camera();
        camera.setProjectionMode(Camera.PROJECTION_MODE_PERSPECTIVE);
        camera.setFov(60.0);

        closeTo(RayGizmo.calculateViewThicknessScale(camera), 1.0, EPS);

        camera.setFov(30.0);
        closeTo(
            RayGizmo.calculateViewThicknessScale(camera),
            Math.tan(JavaMath.toRadians(15.0)) / Math.tan(JavaMath.toRadians(30.0)),
            EPS,
        );
    });

    it("orthographicThickness_isInverseToTheZoom", () => {
        let camera = new Camera();
        camera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        camera.setOrthogonalZoom(4.0);

        closeTo(RayGizmo.calculateViewThicknessScale(camera), 0.25, EPS);
    });

    it("normalLength_occupiesTwentyPercentOfTheVisibleHeightAtTheHitDepth", () => {
        let camera = new Camera();
        camera.setProjectionMode(Camera.PROJECTION_MODE_PERSPECTIVE);
        camera.setFov(60.0);
        camera.updateViewportResize(800, 400);
        camera.setPosition(new Vector3Dd(0, 0, 0));
        camera.setFocusedPositionMaintainingOrthogonality(new Vector3Dd(0, 0, 1));

        let length = RayGizmo.calculateVisibleNormalLength(camera, new Vector3Dd(0, 0, 10));
        let visibleHeight = 2.0 * 10.0 * Math.tan(JavaMath.toRadians(30.0));

        closeTo(length, 0.2 * visibleHeight, EPS);
    });

    it("missingRayLength_isTwiceTheVisibleNormalLength", () => {
        let camera = new Camera();
        camera.setProjectionMode(Camera.PROJECTION_MODE_PERSPECTIVE);
        camera.setFov(60.0);
        camera.setPosition(new Vector3Dd(0, 0, 0));
        camera.setFocusedPositionMaintainingOrthogonality(new Vector3Dd(0, 0, 1));
        let origin = new Vector3Dd(0, 0, 10);

        closeTo(
            RayGizmo.calculateMissingRayLength(camera, origin),
            2.0 * RayGizmo.calculateVisibleNormalLength(camera, origin),
            EPS,
        );
    });

    it("missingSceneRay_usesViewNormalizedWidthAndLength", () => {
        let gizmo = new RayGizmo(null, 0);
        gizmo.setRay(new Ray(new Vector3Dd(0, 0, 0), new Vector3Dd(0, 0, 1)), 0.0);
        gizmo.acquireSnapshot();

        let camera = new Camera();
        camera.setProjectionMode(Camera.PROJECTION_MODE_PERSPECTIVE);
        camera.setFov(30.0);

        let ray = gizmo.buildScene(camera).getSimpleBodies().get(0);
        let expected = RayGizmo.calculateViewThicknessScale(camera);
        let missingLength = RayGizmo.calculateMissingRayLength(camera, new Vector3Dd(0, 0, 0));
        let rayArrow = ray.getGeometry();

        closeTo(ray.getScale().x(), expected, EPS);
        closeTo(ray.getScale().y(), expected, EPS);
        closeTo(ray.getScale().z(), 1.0, EPS);
        closeTo(rayArrow.getBaseLength(), missingLength - 1.0, EPS);
        closeTo(rayArrow.getHeadLength(), 1.0, EPS);
    });

    it("intersectingRays_keepTheSameTipLengthWhileTheirShaftsReachTheHit", () => {
        let shortRay = buildIntersectingRayArrow(3.0);
        let longRay = buildIntersectingRayArrow(9.0);

        closeTo(shortRay.getHeadLength(), 1.0, EPS);
        closeTo(longRay.getHeadLength(), 1.0, EPS);
        closeTo(shortRay.getBaseLength(), 2.0, EPS);
        closeTo(longRay.getBaseLength(), 8.0, EPS);
        closeTo(
            shortRay.getHeadRadius() / shortRay.getBaseRadius(),
            longRay.getHeadRadius() / longRay.getBaseRadius(),
            EPS,
        );
    });

    function buildIntersectingRayArrow(hitDistance: number): Arrow {
        let gizmo = new RayGizmo(
            (_ray) => new Intersection(hitDistance, new Vector3Dd(0, 0, hitDistance), new Vector3Dd(0, 1, 0)),
            0,
        );
        gizmo.setRay(new Ray(new Vector3Dd(0, 0, 0), new Vector3Dd(0, 0, 1)), 0.0);
        gizmo.acquireSnapshot();
        return gizmo.buildScene(new Camera()).getSimpleBodies().get(0).getGeometry();
    }
});
