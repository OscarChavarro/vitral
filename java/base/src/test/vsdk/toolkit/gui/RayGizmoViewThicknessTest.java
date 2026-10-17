package vsdk.toolkit.gui;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.element.Intersection;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.volume.Arrow;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.gui.gizmo.RayGizmo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/** Verifies that ray-gizmo thickness follows the camera view, not JOGL. */
class RayGizmoViewThicknessTest
{
    private static final double EPS = 1.0e-9;

    @Test
    void perspectiveThickness_isNormalizedAgainstTheVerticalFov()
    {
        Camera camera = new Camera();
        camera.setProjectionMode(Camera.PROJECTION_MODE_PERSPECTIVE);
        camera.setFov(60.0);

        assertThat(RayGizmo.calculateViewThicknessScale(camera))
            .isCloseTo(1.0, offset(EPS));

        camera.setFov(30.0);
        assertThat(RayGizmo.calculateViewThicknessScale(camera))
            .isCloseTo(Math.tan(Math.toRadians(15.0)) / Math.tan(Math.toRadians(30.0)),
                offset(EPS));
    }

    @Test
    void orthographicThickness_isInverseToTheZoom()
    {
        Camera camera = new Camera();
        camera.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        camera.setOrthogonalZoom(4.0);

        assertThat(RayGizmo.calculateViewThicknessScale(camera))
            .isCloseTo(0.25, offset(EPS));
    }

    @Test
    void normalLength_occupiesTwentyPercentOfTheVisibleHeightAtTheHitDepth()
    {
        Camera camera = new Camera();
        camera.setProjectionMode(Camera.PROJECTION_MODE_PERSPECTIVE);
        camera.setFov(60.0);
        camera.updateViewportResize(800, 400);
        camera.setPosition(new Vector3Dd(0, 0, 0));
        camera.setFocusedPositionMaintainingOrthogonality(new Vector3Dd(0, 0, 1));

        double length = RayGizmo.calculateVisibleNormalLength(camera,
            new Vector3Dd(0, 0, 10));
        double visibleHeight = 2.0 * 10.0 * Math.tan(Math.toRadians(30.0));

        assertThat(length).isCloseTo(0.20 * visibleHeight, offset(EPS));
    }

    @Test
    void missingRayLength_isTwiceTheVisibleNormalLength()
    {
        Camera camera = new Camera();
        camera.setProjectionMode(Camera.PROJECTION_MODE_PERSPECTIVE);
        camera.setFov(60.0);
        camera.setPosition(new Vector3Dd(0, 0, 0));
        camera.setFocusedPositionMaintainingOrthogonality(new Vector3Dd(0, 0, 1));
        Vector3Dd origin = new Vector3Dd(0, 0, 10);

        assertThat(RayGizmo.calculateMissingRayLength(camera, origin)).isCloseTo(
            2.0 * RayGizmo.calculateVisibleNormalLength(camera, origin), offset(EPS));
    }

    @Test
    void missingSceneRay_usesViewNormalizedWidthAndLength()
    {
        RayGizmo gizmo = new RayGizmo(null, 0);
        gizmo.setRay(new Ray(new Vector3Dd(0, 0, 0), new Vector3Dd(0, 0, 1)), 0.0);
        gizmo.acquireSnapshot();

        Camera camera = new Camera();
        camera.setProjectionMode(Camera.PROJECTION_MODE_PERSPECTIVE);
        camera.setFov(30.0);

        SimpleBody ray = gizmo.buildScene(camera).getSimpleBodies().get(0);
        double expected = RayGizmo.calculateViewThicknessScale(camera);
        double missingLength = RayGizmo.calculateMissingRayLength(camera,
            new Vector3Dd(0, 0, 0));
        Arrow rayArrow = (Arrow)ray.getGeometry();

        assertThat(ray.getScale().x()).isCloseTo(expected, offset(EPS));
        assertThat(ray.getScale().y()).isCloseTo(expected, offset(EPS));
        assertThat(ray.getScale().z()).isCloseTo(1.0, offset(EPS));
        assertThat(rayArrow.getBaseLength()).isCloseTo(missingLength - 1.0, offset(EPS));
        assertThat(rayArrow.getHeadLength()).isCloseTo(1.0, offset(EPS));
    }

    @Test
    void intersectingRays_keepTheSameTipLengthWhileTheirShaftsReachTheHit()
    {
        Arrow shortRay = buildIntersectingRayArrow(3.0);
        Arrow longRay = buildIntersectingRayArrow(9.0);

        assertThat(shortRay.getHeadLength()).isCloseTo(1.0, offset(EPS));
        assertThat(longRay.getHeadLength()).isCloseTo(1.0, offset(EPS));
        assertThat(shortRay.getBaseLength()).isCloseTo(2.0, offset(EPS));
        assertThat(longRay.getBaseLength()).isCloseTo(8.0, offset(EPS));
        assertThat(shortRay.getHeadRadius() / shortRay.getBaseRadius()).isCloseTo(
            longRay.getHeadRadius() / longRay.getBaseRadius(), offset(EPS));
    }

    private static Arrow buildIntersectingRayArrow(double hitDistance)
    {
        RayGizmo gizmo = new RayGizmo(ray -> new Intersection(hitDistance,
            new Vector3Dd(0, 0, hitDistance), new Vector3Dd(0, 1, 0)), 0);
        gizmo.setRay(new Ray(new Vector3Dd(0, 0, 0), new Vector3Dd(0, 0, 1)), 0.0);
        gizmo.acquireSnapshot();
        return (Arrow)gizmo.buildScene(new Camera()).getSimpleBodies().get(0).getGeometry();
    }
}
