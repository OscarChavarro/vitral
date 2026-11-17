// C++ port of Java's CameraControlGizmoTest (gui/gizmo).
#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "java/lang/Math.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#include "vsdk/toolkit/environment/geometry/volume/Tube.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/gui/MouseEvent.h"
#include "vsdk/toolkit/gui/gizmo/CameraControlGizmo.h"
#include "vsdk/toolkit/gui/gizmo/CameraControlGizmoAnimation.h"
#include "vsdk/toolkit/gui/gizmo/CameraControlGizmoInteractionTechnique.h"

/*
Exercises the model of the `CameraControlGizmo` (26 regions and a compass),
the views it computes, the camera animation and the mouse interaction.
*/

static const double EPS = 1e-6;

/** Perspective camera at (0, -5, 0) looking at the origin (+Y), Z up. */
static void
frontCamera(Camera& camera)
{
    Matrix4x4d rotation;
    CameraControlGizmo::createCameraRotation(Vector3Dd(0, 1, 0),
        Vector3Dd(0, 0, 1), rotation);
    camera.setPosition(Vector3Dd(0, -5, 0));
    camera.setRotation(rotation);
    camera.setFocusedPositionDirect(Vector3Dd(0, 0, 0));
    camera.updateVectors();
}

static Vector3Dd
front(const Matrix4x4d& r)
{
    return Vector3Dd(r.get(0, 0), r.get(1, 0), r.get(2, 0));
}

static Vector3Dd
up(const Matrix4x4d& r)
{
    return Vector3Dd(r.get(0, 2), r.get(1, 2), r.get(2, 2));
}

static MouseEvent
mouse(int x, int y)
{
    MouseEvent e;
    e.setX(x);
    e.setY(y);
    e.setButton(MouseEvent::BUTTON1);
    e.setModifiers(MouseEvent::BUTTON1_DOWN_MASK);
    return e;
}

TEST(CameraControlGizmoTest, given_gizmo_when_built_then_has26BoxesAndOneTubeWithExpectedKinds)
{
    CameraControlGizmo gizmo;
    int faces = 0, edges = 0, corners = 0;

    ASSERT_EQ((long)CameraControlGizmo::NUMBER_OF_PARTS, gizmo.getParts().size());
    for ( int r = 0; r < CameraControlGizmo::NUMBER_OF_CUBE_REGIONS; r++ ) {
        EXPECT_TRUE(gizmo.getParts()[r]->getGeometry()->instanceOf(Box::CLASS));
        switch ( gizmo.getRegionKind(r) ) {
          case CameraControlGizmo::KIND_FACE: faces++; break;
          case CameraControlGizmo::KIND_EDGE: edges++; break;
          case CameraControlGizmo::KIND_CORNER: corners++; break;
          default: break;
        }
    }
    EXPECT_TRUE(gizmo.getParts()[CameraControlGizmo::COMPASS_REGION]
        ->getGeometry()->instanceOf(Tube::CLASS));
    EXPECT_EQ(6, faces);
    EXPECT_EQ(12, edges);
    EXPECT_EQ(8, corners);
    EXPECT_TRUE(gizmo.getFaceLabel(gizmo.findRegion(0, 0, 1)) == "TOP");
    EXPECT_TRUE(gizmo.getFaceLabel(gizmo.findRegion(0, -1, 0)) == "FRONT");
    EXPECT_TRUE(gizmo.regionName(gizmo.findRegion(1, -1, 1)) ==
        "TOP FRONT RIGHT");
}

TEST(CameraControlGizmoTest, given_frontCamera_when_centerOfGizmoPicked_then_frontFaceIsFound)
{
    CameraControlGizmo gizmo;
    Camera camera;
    frontCamera(camera);
    gizmo.setCamera(&camera);
    gizmo.placeInViewport(400, 80, 20, NULL);
    int cx = gizmo.getHudX() + gizmo.getSizeInPixels() / 2;
    int cy = gizmo.getHudY() + gizmo.getSizeInPixels() / 2;

    EXPECT_EQ(gizmo.findRegion(0, -1, 0), gizmo.pickRegion(cx, cy));
    // Upper part of the square: the top edge of the front face
    int top = gizmo.getHudY() + (int)(gizmo.getSizeInPixels() *
        (0.5 - 0.9 / (2 * CameraControlGizmo::HUD_HALF_EXTENT)));
    EXPECT_EQ(gizmo.findRegion(0, -1, 1), gizmo.pickRegion(cx, top));
    EXPECT_EQ((int)CameraControlGizmo::NO_REGION,
        gizmo.pickRegion(gizmo.getHudX() - 1, cy));
}

TEST(CameraControlGizmoTest, given_frontView_when_topViewComputed_then_lookingDownWithFrontAtBottom)
{
    CameraControlGizmo gizmo;
    Camera camera;
    frontCamera(camera);
    Matrix4x4d r;
    ASSERT_TRUE(gizmo.calculateViewRotation(gizmo.findRegion(0, 0, 1),
        camera.getRotation(), r));

    EXPECT_NEAR(-1, front(r).z(), EPS);
    // What was ahead (+Y) is up on the screen
    EXPECT_NEAR(1, up(r).y(), EPS);
}

TEST(CameraControlGizmoTest, given_cornerRegion_when_viewComputed_then_lookingFromCornerUpright)
{
    CameraControlGizmo gizmo;
    Camera camera;
    frontCamera(camera);
    Matrix4x4d r;
    ASSERT_TRUE(gizmo.calculateViewRotation(gizmo.findRegion(1, -1, 1),
        camera.getRotation(), r));
    double k = 1 / Math::sqrt(3.0);

    EXPECT_NEAR(-k, front(r).x(), EPS);
    EXPECT_NEAR(k, front(r).y(), EPS);
    EXPECT_NEAR(-k, front(r).z(), EPS);
    EXPECT_GT(up(r).z(), 0);
    EXPECT_LT(Math::abs(front(r).dotProduct(up(r))), EPS);
}

TEST(CameraControlGizmoTest, given_animation_when_advanced_then_orbitsPivotAndReachesTarget)
{
    CameraControlGizmo gizmo;
    Camera camera;
    frontCamera(camera);
    Matrix4x4d target;
    ASSERT_TRUE(gizmo.calculateViewRotation(gizmo.findRegion(1, 0, 0),
        camera.getRotation(), target));
    CameraControlGizmoAnimation animation(&camera, target, 1.0);

    EXPECT_NEAR(Math::PI / 2, animation.getAngle(), EPS);
    animation.advance(0);
    animation.advance(VSDK_LL(500000000));
    // Half way: still 5 units from the pivot, turned 45 degrees
    EXPECT_NEAR(5, camera.getPosition().length(), EPS);
    EXPECT_NEAR(-Math::sqrt(0.5), camera.getFront().x(), EPS);
    EXPECT_TRUE(animation.isRunning());
    animation.advance(VSDK_LL(999999999));
    EXPECT_NEAR(-1, front(camera.getRotation()).x(), 1e-6);
    animation.advance(VSDK_LL(1000000000));
    EXPECT_FALSE(animation.isRunning());
    EXPECT_NEAR(5, camera.getPosition().x(), EPS);
    EXPECT_NEAR(0, camera.getFocusedPosition().length(), EPS);
}

TEST(CameraControlGizmoTest, given_halfTurn_when_axisAngleExtracted_then_angleIsPi)
{
    Matrix4x4d r = Matrix4x4d().axisRotation(Math::PI, Vector3Dd(0, 0, 1));
    double aa[4];
    CameraControlGizmoAnimation::extractAxisAngle(r, aa);

    EXPECT_NEAR(Math::PI, aa[3], EPS);
    EXPECT_NEAR(1, Math::abs(aa[2]), EPS);
}

TEST(CameraControlGizmoTest, given_technique_when_clickOnFace_then_animationTurnsCameraToThatView)
{
    CameraControlGizmo gizmo;
    Camera camera;
    frontCamera(camera);
    gizmo.setCamera(&camera);
    gizmo.placeInViewport(400, 0, 0, NULL);
    CameraControlGizmoInteractionTechnique technique(&gizmo);
    int cx = gizmo.getHudX() + gizmo.getSizeInPixels() / 2;
    int top = gizmo.getHudY() + (int)(gizmo.getSizeInPixels() *
        (0.5 - 0.9 / (2 * CameraControlGizmo::HUD_HALF_EXTENT)));

    EXPECT_TRUE(technique.processMouseMovedEvent(mouse(cx, top)));
    EXPECT_TRUE(gizmo.isActive());
    EXPECT_TRUE(technique.processMousePressedEvent(mouse(cx, top)));
    EXPECT_TRUE(technique.processMouseReleasedEvent(mouse(cx, top)));
    EXPECT_TRUE(technique.isAnimating());
    technique.advanceAnimation(0);
    // C++ port note: Java advances to 10 s; 2 s fits the 32 bit `long long`
    // of Turbo C++ and is also after the end of the animation
    technique.advanceAnimation(VSDK_LL(2000000000));
    EXPECT_FALSE(technique.isAnimating());
    // The top front edge: looking from (0, -1, 1)
    EXPECT_NEAR(Math::sqrt(0.5), camera.getFront().y(), EPS);
    EXPECT_NEAR(-Math::sqrt(0.5), camera.getFront().z(), EPS);
}

TEST(CameraControlGizmoTest, given_technique_when_draggingCube_then_cameraOrbitsKeepingUpright)
{
    CameraControlGizmo gizmo;
    Camera camera;
    frontCamera(camera);
    gizmo.setCamera(&camera);
    gizmo.placeInViewport(400, 0, 0, NULL);
    CameraControlGizmoInteractionTechnique technique(&gizmo);
    int cx = gizmo.getHudX() + gizmo.getSizeInPixels() / 2;
    int cy = gizmo.getHudY() + gizmo.getSizeInPixels() / 2;

    technique.processMousePressedEvent(mouse(cx, cy));
    EXPECT_FALSE(technique.processMouseDraggedEvent(mouse(cx + 1, cy)));
    EXPECT_TRUE(technique.processMouseDraggedEvent(mouse(cx + 50, cy + 30)));
    technique.processMouseReleasedEvent(mouse(cx + 50, cy + 30));

    EXPECT_FALSE(technique.isAnimating());
    EXPECT_NEAR(0, camera.getFocusedPosition().length(), EPS);
    EXPECT_NEAR(5, camera.getPosition().length(), EPS);
    // Moved down: the camera went up; moved right: it turned around Z
    EXPECT_GT(camera.getPosition().z(), 0);
    EXPECT_LT(camera.getPosition().x(), 0);
    EXPECT_NEAR(0, camera.getLeft().z(), EPS);
}

TEST(CameraControlGizmoTest, given_compassDrag_when_cursorMoves_then_grabbedPointStaysUnderCursor)
{
    CameraControlGizmo gizmo;
    Camera camera;
    frontCamera(camera);
    // Looking from above and in front, so the compass is seen well
    Matrix4x4d rotation;
    CameraControlGizmo::createCameraRotation(Vector3Dd(0, 1, -1),
        Vector3Dd(0, 0, 1), rotation);
    camera.setRotation(rotation);
    camera.setPosition(Vector3Dd(0, -5, 5));
    camera.setFocusedPositionDirect(Vector3Dd(0, 0, 0));
    camera.updateVectors();
    gizmo.setCamera(&camera);
    gizmo.placeInViewport(400, 0, 0, NULL);
    CameraControlGizmoInteractionTechnique technique(&gizmo);

    // Find a pixel over the compass, in front of the cube
    int x = -1, y = -1;
    int s = gizmo.getSizeInPixels();
    for ( int py = s - 1; py >= 0 && x < 0; py-- ) {
        int px = gizmo.getHudX() + s / 2;
        if ( gizmo.pickRegion(px, gizmo.getHudY() + py) ==
             CameraControlGizmo::COMPASS_REGION ) {
            x = px;
            y = gizmo.getHudY() + py;
        }
    }
    ASSERT_GE(x, 0);
    double before = CameraControlGizmo::compassAngleAt(x - gizmo.getHudX(),
        y - gizmo.getHudY(), *gizmo.getHudCamera());

    technique.processMousePressedEvent(mouse(x, y));
    technique.processMouseDraggedEvent(mouse(x + 20, y));
    technique.processMouseReleasedEvent(mouse(x + 20, y));

    // With the new camera, the cursor is over the same point of the compass
    double after = CameraControlGizmo::compassAngleAt(x + 20 - gizmo.getHudX(),
        y - gizmo.getHudY(), *gizmo.getHudCamera());
    EXPECT_NEAR(before, after, 1e-3);
    EXPECT_NEAR(-Math::sqrt(0.5), camera.getFront().z(), EPS);
}

VITRAL_TEST_MAIN()
