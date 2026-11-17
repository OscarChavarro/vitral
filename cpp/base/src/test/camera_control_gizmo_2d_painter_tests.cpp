#include <gtest/gtest.h>

#include "java/lang/String.h"
#include "java/lang/System.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/gui/gizmo/CameraControlGizmo.h"
#include "vsdk/toolkit/render/gizmo/CameraControlGizmo2DPainter.h"
#include "vsdk/toolkit/render/gizmo/Gizmo2DDrawList.h"

/*
Exercises the 2D presentation of the `CameraControlGizmo`: hidden line
removal of its parts with Appel's algorithm, filled faces and labels, all in
its HUD area. C++ counterpart of Java's `CameraControlGizmo2DPainterTest`.
*/
namespace {

void cornerCamera(Camera& camera)
{
    Matrix4x4d r;
    camera.setPosition(Vector3Dd(5, -5, 5));
    CameraControlGizmo::createCameraRotation(Vector3Dd(-1, 1, -1),
        Vector3Dd(0, 0, 1), &r);
    camera.setRotation(r);
    camera.setFocusedPositionDirect(Vector3Dd(0, 0, 0));
    camera.updateVectors();
}

}

TEST(CameraControlGizmo2DPainterTest, given_cornerView_when_painted_then_visibleLinesFacesAndThreeLabelsInHud)
{
    CameraControlGizmo gizmo;
    Camera camera;
    cornerCamera(camera);
    gizmo.setCamera(&camera);
    gizmo.placeInViewport(400, 80, 20, nullptr);
    CameraControlGizmo2DPainter painter;
    Gizmo2DDrawList list;

    painter.paint(&list, &gizmo, nullptr);

    int lines = 0;
    int polygons = 0;
    java::ArrayList<java::String> labels;
    double minX = gizmo.getHudX() - 1;
    double maxX = gizmo.getHudX() + gizmo.getSizeInPixels() + 1;
    double minY = gizmo.getHudY() - 1;
    double maxY = gizmo.getHudY() + gizmo.getSizeInPixels() + 1;
    java::ArrayList<Gizmo2DDrawList::Primitive> primitives =
        list.getPrimitivesInDrawingOrder();
    for ( long k = 0; k < primitives.size(); k++ ) {
        const Gizmo2DDrawList::Primitive& p = primitives.get(k);
        switch ( p.getKind() ) {
          case Gizmo2DDrawList::Kind::LINE:
            lines++;
            break;
          case Gizmo2DDrawList::Kind::POLYGON:
            polygons++;
            break;
          case Gizmo2DDrawList::Kind::LABEL:
            labels.add(p.getText());
            break;
        }
        for ( int i = 0; i < p.getNumberOfVertices(); i++ ) {
            EXPECT_GE(p.getX(i), minX);
            EXPECT_LE(p.getX(i), maxX);
            EXPECT_GE(p.getY(i), minY);
            EXPECT_LE(p.getY(i), maxY);
        }
    }
    EXPECT_GT(lines, 50);
    EXPECT_GT(polygons, 26);
    ASSERT_EQ(labels.size(), 3);
    EXPECT_TRUE(labels.contains(java::String("TOP")));
    EXPECT_TRUE(labels.contains(java::String("FRONT")));
    EXPECT_TRUE(labels.contains(java::String("RIGHT")));

    // The lines are kept while the orientation does not change
    long long start = java::System::nanoTime();
    list.clear();
    painter.paint(&list, &gizmo, nullptr);
    EXPECT_LT((java::System::nanoTime() - start) / 1.0e6, 200.0);
}
