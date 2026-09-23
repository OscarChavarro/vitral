#include <cmath>

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "model/ApplicationModel.h"
#include "gui/VisualRayDebugController.h"

namespace {
const double ORIGIN_STEP = 0.1;
const double ANGLE_STEP = 5.0 * M_PI / 180.0;
}

VisualRayDebugController::VisualRayDebugController(ApplicationModel* model)
    : model(model)
{
}

bool VisualRayDebugController::processKeyPressedEvent(const KeyEvent& event)
{
    if ( event.unicodeId == KeyEvent::KEY_NONE ) {
        return false;
    }

    switch ( event.unicodeId ) {
      case '4':
        moveOrigin(-ORIGIN_STEP, 0, 0);
        return true;
      case '6':
        moveOrigin(ORIGIN_STEP, 0, 0);
        return true;
      case '8':
        moveOrigin(0, ORIGIN_STEP, 0);
        return true;
      case '2':
        moveOrigin(0, -ORIGIN_STEP, 0);
        return true;
      case '1':
        moveOrigin(0, 0, -ORIGIN_STEP);
        return true;
      case '7':
        moveOrigin(0, 0, ORIGIN_STEP);
        return true;
      case '9':
        if ( model->isWithVisualDebugRay() ) {
            model->setVisualDebugRayLevels(model->getVisualDebugRayLevels() + 1);
        }
        return true;
      case '3':
        if ( model->isWithVisualDebugRay() ) {
            model->setVisualDebugRayLevels(model->getVisualDebugRayLevels() - 1);
            if ( model->getVisualDebugRayLevels() < 0 ) {
                model->setVisualDebugRayLevels(0);
            }
        }
        return true;
      case '5':
        model->setWithVisualDebugRay(!model->isWithVisualDebugRay());
        return true;
      case '*':
        rotateDirection(-ANGLE_STEP, 0);
        return true;
      case '/':
        rotateDirection(ANGLE_STEP, 0);
        return true;
      case '+':
        rotateDirection(0, ANGLE_STEP);
        return true;
      case '-':
        rotateDirection(0, -ANGLE_STEP);
        return true;
      default:
        return false;
    }
}

void VisualRayDebugController::moveOrigin(double dx, double dy, double dz)
{
    if ( !model->isWithVisualDebugRay() ||
         model->getVisualDebugRay() == nullptr ) {
        return;
    }
    Ray ray = *model->getVisualDebugRay();
    Vector3Dd origin = ray.getOrigin();

    Ray moved = ray.withOrigin(Vector3Dd(
        origin.x() + dx, origin.y() + dy, origin.z() + dz));
    model->setVisualDebugRay(&moved);
}

void VisualRayDebugController::rotateDirection(double deltaTheta,
                                               double deltaPhi)
{
    if ( !model->isWithVisualDebugRay() ||
         model->getVisualDebugRay() == nullptr ) {
        return;
    }
    Ray ray = *model->getVisualDebugRay();
    double theta = ray.getDirection().obtainSphericalThetaAngle();
    double phi = ray.getDirection().obtainSphericalPhiAngle();

    theta += deltaTheta;
    phi += deltaPhi;
    if ( phi > M_PI ) phi = M_PI;
    if ( phi < 0 ) phi = 0;
    Ray rotated = ray.withDirection(Vector3Dd::fromSpherical(1, theta, phi));
    model->setVisualDebugRay(&rotated);
}
