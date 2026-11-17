package vsdk.toolkit.gui.gizmo;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.volume.Box;
import vsdk.toolkit.environment.geometry.volume.Tube;
import vsdk.toolkit.gui.MouseEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
Exercises the model of the `CameraControlGizmo` (26 regions and a compass),
the views it computes, the camera animation and the mouse interaction.
*/
class CameraControlGizmoTest
{
    private static final double EPS = 1e-6;

    /** Perspective camera at (0, -5, 0) looking at the origin (+Y), Z up. */
    private static Camera frontCamera()
    {
        Camera camera = new Camera();
        camera.setPosition(new Vector3Dd(0, -5, 0));
        camera.setRotation(CameraControlGizmo.createCameraRotation(
            new Vector3Dd(0, 1, 0), new Vector3Dd(0, 0, 1)));
        camera.setFocusedPositionDirect(new Vector3Dd(0, 0, 0));
        camera.updateVectors();
        return camera;
    }

    private static Vector3Dd front(Matrix4x4d r)
    {
        return new Vector3Dd(r.get(0, 0), r.get(1, 0), r.get(2, 0));
    }

    private static Vector3Dd up(Matrix4x4d r)
    {
        return new Vector3Dd(r.get(0, 2), r.get(1, 2), r.get(2, 2));
    }

    private static MouseEvent mouse(int x, int y)
    {
        MouseEvent e = new MouseEvent();
        e.setX(x);
        e.setY(y);
        e.setButton(MouseEvent.BUTTON1);
        e.setModifiers(MouseEvent.BUTTON1_DOWN_MASK);
        return e;
    }

    @Test
    void given_gizmo_when_built_then_has26BoxesAndOneTubeWithExpectedKinds()
    {
        CameraControlGizmo gizmo = new CameraControlGizmo();
        int faces = 0, edges = 0, corners = 0;

        assertThat(gizmo.getParts()).hasSize(CameraControlGizmo.NUMBER_OF_PARTS);
        for ( int r = 0; r < CameraControlGizmo.NUMBER_OF_CUBE_REGIONS; r++ ) {
            assertThat(gizmo.getParts().get(r).getGeometry()).isInstanceOf(Box.class);
            switch ( gizmo.getRegionKind(r) ) {
              case CameraControlGizmo.KIND_FACE -> faces++;
              case CameraControlGizmo.KIND_EDGE -> edges++;
              case CameraControlGizmo.KIND_CORNER -> corners++;
              default -> { }
            }
        }
        assertThat(gizmo.getParts().get(CameraControlGizmo.COMPASS_REGION)
            .getGeometry()).isInstanceOf(Tube.class);
        assertThat(faces).isEqualTo(6);
        assertThat(edges).isEqualTo(12);
        assertThat(corners).isEqualTo(8);
        assertThat(gizmo.getFaceLabel(gizmo.findRegion(0, 0, 1))).isEqualTo("TOP");
        assertThat(gizmo.getFaceLabel(gizmo.findRegion(0, -1, 0))).isEqualTo("FRONT");
        assertThat(gizmo.regionName(gizmo.findRegion(1, -1, 1)))
            .isEqualTo("TOP FRONT RIGHT");
    }

    @Test
    void given_frontCamera_when_centerOfGizmoPicked_then_frontFaceIsFound()
    {
        CameraControlGizmo gizmo = new CameraControlGizmo();
        gizmo.setCamera(frontCamera());
        gizmo.placeInViewport(400, 80, 20, null);
        int cx = gizmo.getHudX() + gizmo.getSizeInPixels() / 2;
        int cy = gizmo.getHudY() + gizmo.getSizeInPixels() / 2;

        assertThat(gizmo.pickRegion(cx, cy)).isEqualTo(gizmo.findRegion(0, -1, 0));
        // Upper part of the square: the top edge of the front face
        int top = gizmo.getHudY() + (int)(gizmo.getSizeInPixels() *
            (0.5 - 0.9 / (2 * CameraControlGizmo.HUD_HALF_EXTENT)));
        assertThat(gizmo.pickRegion(cx, top)).isEqualTo(gizmo.findRegion(0, -1, 1));
        assertThat(gizmo.pickRegion(gizmo.getHudX() - 1, cy))
            .isEqualTo(CameraControlGizmo.NO_REGION);
    }

    @Test
    void given_frontView_when_topViewComputed_then_lookingDownWithFrontAtBottom()
    {
        CameraControlGizmo gizmo = new CameraControlGizmo();
        Camera camera = frontCamera();
        Matrix4x4d r = gizmo.calculateViewRotation(gizmo.findRegion(0, 0, 1),
            camera.getRotation());

        assertThat(front(r).z()).isCloseTo(-1, within(EPS));
        // What was ahead (+Y) is up on the screen
        assertThat(up(r).y()).isCloseTo(1, within(EPS));
    }

    @Test
    void given_cornerRegion_when_viewComputed_then_lookingFromCornerUpright()
    {
        CameraControlGizmo gizmo = new CameraControlGizmo();
        Matrix4x4d r = gizmo.calculateViewRotation(gizmo.findRegion(1, -1, 1),
            frontCamera().getRotation());
        double k = 1 / Math.sqrt(3);

        assertThat(front(r).x()).isCloseTo(-k, within(EPS));
        assertThat(front(r).y()).isCloseTo(k, within(EPS));
        assertThat(front(r).z()).isCloseTo(-k, within(EPS));
        assertThat(up(r).z()).isGreaterThan(0);
        assertThat(Math.abs(front(r).dotProduct(up(r)))).isLessThan(EPS);
    }

    @Test
    void given_animation_when_advanced_then_orbitsPivotAndReachesTarget()
    {
        CameraControlGizmo gizmo = new CameraControlGizmo();
        Camera camera = frontCamera();
        Matrix4x4d target = gizmo.calculateViewRotation(gizmo.findRegion(1, 0, 0),
            camera.getRotation());
        CameraControlGizmoAnimation animation =
            new CameraControlGizmoAnimation(camera, target, 1.0);

        assertThat(animation.getAngle()).isCloseTo(Math.PI / 2, within(EPS));
        animation.advance(0);
        animation.advance(500_000_000L);
        // Half way: still 5 units from the pivot, turned 45 degrees
        assertThat(camera.getPosition().length()).isCloseTo(5, within(EPS));
        assertThat(camera.getFront().x()).isCloseTo(-Math.sqrt(0.5), within(EPS));
        assertThat(animation.isRunning()).isTrue();
        animation.advance(999_999_999L);
        assertThat(front(camera.getRotation()).x()).isCloseTo(-1, within(1e-6));
        animation.advance(1_000_000_000L);
        assertThat(animation.isRunning()).isFalse();
        assertThat(camera.getPosition().x()).isCloseTo(5, within(EPS));
        assertThat(camera.getFocusedPosition().length()).isCloseTo(0, within(EPS));
    }

    @Test
    void given_halfTurn_when_axisAngleExtracted_then_angleIsPi()
    {
        Matrix4x4d r = new Matrix4x4d().axisRotation(Math.PI, new Vector3Dd(0, 0, 1));
        double[] aa = CameraControlGizmoAnimation.extractAxisAngle(r);

        assertThat(aa[3]).isCloseTo(Math.PI, within(EPS));
        assertThat(Math.abs(aa[2])).isCloseTo(1, within(EPS));
    }

    @Test
    void given_technique_when_clickOnFace_then_animationTurnsCameraToThatView()
    {
        CameraControlGizmo gizmo = new CameraControlGizmo();
        Camera camera = frontCamera();
        gizmo.setCamera(camera);
        gizmo.placeInViewport(400, 0, 0, null);
        CameraControlGizmoInteractionTechnique technique =
            new CameraControlGizmoInteractionTechnique(gizmo);
        int cx = gizmo.getHudX() + gizmo.getSizeInPixels() / 2;
        int top = gizmo.getHudY() + (int)(gizmo.getSizeInPixels() *
            (0.5 - 0.9 / (2 * CameraControlGizmo.HUD_HALF_EXTENT)));

        assertThat(technique.processMouseMovedEvent(mouse(cx, top))).isTrue();
        assertThat(gizmo.isActive()).isTrue();
        assertThat(technique.processMousePressedEvent(mouse(cx, top))).isTrue();
        assertThat(technique.processMouseReleasedEvent(mouse(cx, top))).isTrue();
        assertThat(technique.isAnimating()).isTrue();
        technique.advanceAnimation(0);
        technique.advanceAnimation(10_000_000_000L);
        assertThat(technique.isAnimating()).isFalse();
        // The top front edge: looking from (0, -1, 1)
        assertThat(camera.getFront().y()).isCloseTo(Math.sqrt(0.5), within(EPS));
        assertThat(camera.getFront().z()).isCloseTo(-Math.sqrt(0.5), within(EPS));
    }

    @Test
    void given_technique_when_draggingCube_then_cameraOrbitsKeepingUpright()
    {
        CameraControlGizmo gizmo = new CameraControlGizmo();
        Camera camera = frontCamera();
        gizmo.setCamera(camera);
        gizmo.placeInViewport(400, 0, 0, null);
        CameraControlGizmoInteractionTechnique technique =
            new CameraControlGizmoInteractionTechnique(gizmo);
        int cx = gizmo.getHudX() + gizmo.getSizeInPixels() / 2;
        int cy = gizmo.getHudY() + gizmo.getSizeInPixels() / 2;

        technique.processMousePressedEvent(mouse(cx, cy));
        assertThat(technique.processMouseDraggedEvent(mouse(cx + 1, cy))).isFalse();
        assertThat(technique.processMouseDraggedEvent(mouse(cx + 50, cy + 30))).isTrue();
        technique.processMouseReleasedEvent(mouse(cx + 50, cy + 30));

        assertThat(technique.isAnimating()).isFalse();
        assertThat(camera.getFocusedPosition().length()).isCloseTo(0, within(EPS));
        assertThat(camera.getPosition().length()).isCloseTo(5, within(EPS));
        // Moved down: the camera went up; moved right: it turned around Z
        assertThat(camera.getPosition().z()).isGreaterThan(0);
        assertThat(camera.getPosition().x()).isLessThan(0);
        assertThat(camera.getLeft().z()).isCloseTo(0, within(EPS));
    }

    @Test
    void given_compassDrag_when_cursorMoves_then_grabbedPointStaysUnderCursor()
    {
        CameraControlGizmo gizmo = new CameraControlGizmo();
        Camera camera = frontCamera();
        // Looking from above and in front, so the compass is seen well
        camera.setRotation(CameraControlGizmo.createCameraRotation(
            new Vector3Dd(0, 1, -1), new Vector3Dd(0, 0, 1)));
        camera.setPosition(new Vector3Dd(0, -5, 5));
        camera.setFocusedPositionDirect(new Vector3Dd(0, 0, 0));
        camera.updateVectors();
        gizmo.setCamera(camera);
        gizmo.placeInViewport(400, 0, 0, null);
        CameraControlGizmoInteractionTechnique technique =
            new CameraControlGizmoInteractionTechnique(gizmo);

        // Find a pixel over the compass, in front of the cube
        int x = -1, y = -1;
        int s = gizmo.getSizeInPixels();
        for ( int py = s - 1; py >= 0 && x < 0; py-- ) {
            int px = gizmo.getHudX() + s / 2;
            if ( gizmo.pickRegion(px, gizmo.getHudY() + py) ==
                 CameraControlGizmo.COMPASS_REGION ) {
                x = px;
                y = gizmo.getHudY() + py;
            }
        }
        assertThat(x).isGreaterThanOrEqualTo(0);
        double before = CameraControlGizmo.compassAngleAt(x - gizmo.getHudX(),
            y - gizmo.getHudY(), gizmo.getHudCamera());

        technique.processMousePressedEvent(mouse(x, y));
        technique.processMouseDraggedEvent(mouse(x + 20, y));
        technique.processMouseReleasedEvent(mouse(x + 20, y));

        // With the new camera, the cursor is over the same point of the compass
        double after = CameraControlGizmo.compassAngleAt(x + 20 - gizmo.getHudX(),
            y - gizmo.getHudY(), gizmo.getHudCamera());
        assertThat(after).isCloseTo(before, within(1e-3));
        assertThat(camera.getFront().z()).isCloseTo(-Math.sqrt(0.5), within(EPS));
    }
}
