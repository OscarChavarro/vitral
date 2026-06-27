#include <cmath>

#include "java/util/ArrayList.txx"
#include "processing/IconGenerator.h"
#include "processing/StyledCalligraphic2DBuffer.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/media/Calligraphic2DBuffer.h"
#include "vsdk/toolkit/environment/geometry/volume/Arrow.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/gui/gizmo/LightGizmoOmniBillboard.h"
#include "vsdk/toolkit/render/hiddenLine/HiddenLineRenderer.h"
Calligraphic2DBuffer* IconGenerator::buildVisibleIcon(SimpleScene& scene, const Camera& camera) const {
    StyledCalligraphic2DBuffer* lineSet = new StyledCalligraphic2DBuffer();
    Calligraphic2DBuffer hiddenLineSet;
    HiddenLineRenderer::executeAppelAlgorithm(
        scene.getSimpleBodies(),
        &camera,
        &lineSet->visibleContourLines(),
        &lineSet->visibleInternalLines(),
        &hiddenLineSet);
    return lineSet;
}

IconGenerator::IconGenerator() {
}

IconGenerator::~IconGenerator() {
}

Calligraphic2DBuffer* IconGenerator::generate(const java::String& title) const {
    if (title == "Ray") {
        return generateRayIcon();
    }
    if (title == "Object") {
        return generateObjectManipulatorIcon();
    }
    if (title == "Omni\nLight" || title == "Camera") {
        return generateOmniLightIcon();
    }
    return nullptr;
}

Calligraphic2DBuffer*
IconGenerator::generateRayIcon() const {
    Camera camera;
    camera.setPosition(Vector3Dd(4, 0, 2));
    camera.setFocusedPositionMaintainingOrthogonality(Vector3Dd(0, 0, 0));
    camera.setNearPlaneDistance(0.01);
    camera.setFarPlaneDistance(600.0);
    camera.updateViewportResize(512, 512);

    SimpleScene scene;
    SimpleBody* arrowBody = new SimpleBody();
    arrowBody->setGeometry(new Arrow(1.0, 0.5, 0.15, 0.3));
    arrowBody->setPosition(Vector3Dd(0, 0, 0));
    Matrix4x4d rx = Matrix4x4d().axisRotation(-M_PI / 2.0, Vector3Dd(1, 0, 0));
    Matrix4x4d ry = Matrix4x4d().axisRotation(M_PI / 8.0, Vector3Dd(0, 0, 1));
    Matrix4x4d R = ry.multiply(rx);
    arrowBody->setRotation(R);
    scene.addBody(arrowBody);

    return buildVisibleIcon(scene, camera);
}

Calligraphic2DBuffer*
IconGenerator::generateObjectManipulatorIcon() const {
    Camera camera;
    camera.setPosition(Vector3Dd(4, 3, 2));
    camera.setFocusedPositionMaintainingOrthogonality(Vector3Dd(0, 0, 0));
    camera.setNearPlaneDistance(0.01);
    camera.setFarPlaneDistance(600.0);
    camera.updateViewportResize(512, 512);

    SimpleScene scene;
    SimpleBody* boxBody = new SimpleBody();
    boxBody->setGeometry(new Box(Vector3Dd(1, 1, 1)));
    boxBody->setPosition(Vector3Dd(0, 0, 0));
    scene.addBody(boxBody);

    return buildVisibleIcon(scene, camera);
}

Calligraphic2DBuffer*
IconGenerator::generateOmniLightIcon() const {
    return new Calligraphic2DBuffer(LightGizmoOmniBillboard::createLinePattern());
}
