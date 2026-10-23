#include <math.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/gui/gizmo/InputGizmo.h"
#include "vsdk/toolkit/gui/gizmo/RayGizmo.h"
#include "vsdk/toolkit/gui/gizmo/RayGizmoInteractionTechniques.h"
#include "java/lang/Math.h"

const double RayGizmoInteractionTechniques::ORIGIN_STEP = 0.1;
const double RayGizmoInteractionTechniques::ANGLE_STEP_DEGREES = 5.0;

RayGizmoInteractionTechniques::RayGizmoInteractionTechniques(RayGizmo* gizmo)
    : gizmo(gizmo), active(FALSE), origin(0, 0, 0),
      yawInDegrees(0.0), pitchInDegrees(0.0)
{
    initialize();
}

void RayGizmoInteractionTechniques::initialize()
{
    Ray ray = currentRay();
    origin = ray.getOrigin();
    updateAnglesFromDirection(ray.getDirection());
    active = gizmo != 0 && gizmo->isVisible();
    updateInputGizmo();
}

RayGizmoInteractionTechniques::RayGizmoInteractionTechniques(
    RayGizmo* gizmo, int activationKey1)
    : gizmo(gizmo), active(FALSE), origin(0, 0, 0),
      yawInDegrees(0.0), pitchInDegrees(0.0)
{
    initialize();
    activationKeys.add(activationKey1);
}

RayGizmoInteractionTechniques::RayGizmoInteractionTechniques(
    RayGizmo* gizmo, int activationKey1, int activationKey2)
    : gizmo(gizmo), active(FALSE), origin(0, 0, 0),
      yawInDegrees(0.0), pitchInDegrees(0.0)
{
    initialize();
    activationKeys.add(activationKey1);
    activationKeys.add(activationKey2);
}

RayGizmoInteractionTechniques::~RayGizmoInteractionTechniques()
{
}

BOOLEAN RayGizmoInteractionTechniques::isActive() const
{
    return active;
}

void RayGizmoInteractionTechniques::setActive(BOOLEAN active)
{
    this->active = active;
    if ( gizmo == 0 ) {
        return;
    }
    gizmo->setVisible(active);
    gizmo->setDisableAfterElapsedSeconds(
        active ? HUGE_VAL : RayGizmo::DEFAULT_DISABLE_TIME);
    if ( active ) {
        applyRay();
    }
}

InputGizmo* RayGizmoInteractionTechniques::getInputGizmo()
{
    return gizmo != 0 ? gizmo->getInputGizmo() : 0;
}

BOOLEAN RayGizmoInteractionTechniques::processKeyPressedEvent(const KeyEvent& event)
{
    if ( gizmo == 0 ) {
        return FALSE;
    }
    active = gizmo->isVisible();
    if ( isActivationKey(event) || event.keycode == KeyEvent::KEY_NUM5 ) {
        setActive(!active);
        return TRUE;
    }
    if ( !active ) {
        return FALSE;
    }
    if ( processNumericPadCommand(event) ) {
        getInputGizmo()->cancelEditing();
        applyRay();
        return TRUE;
    }
    if ( getInputGizmo()->consumesKey(event) ) {
        getInputGizmo()->processKeyPressedEvent(event);
        if ( getInputGizmo()->consumeCommit() ) {
            applyInputGizmoValues();
            getInputGizmo()->cancelEditing();
            applyRay();
        }
        return TRUE;
    }
    return FALSE;
}

void RayGizmoInteractionTechniques::setRay(const Ray* ray)
{
    if ( ray == 0 || gizmo == 0 ) {
        return;
    }
    origin = ray->getOrigin();
    updateAnglesFromDirection(ray->getDirection());
    updateInputGizmo();
    gizmo->setRay(*ray, 0.0);
}

BOOLEAN RayGizmoInteractionTechniques::processNumericPadCommand(const KeyEvent& event)
{
    switch ( event.keycode ) {
      case KeyEvent::KEY_NUM4:
        origin = origin.add(Vector3Dd(-ORIGIN_STEP, 0, 0));
        return TRUE;
      case KeyEvent::KEY_NUM6:
        origin = origin.add(Vector3Dd(ORIGIN_STEP, 0, 0));
        return TRUE;
      case KeyEvent::KEY_NUM2:
        origin = origin.add(Vector3Dd(0, -ORIGIN_STEP, 0));
        return TRUE;
      case KeyEvent::KEY_NUM8:
        origin = origin.add(Vector3Dd(0, ORIGIN_STEP, 0));
        return TRUE;
      case KeyEvent::KEY_NUM1:
        origin = origin.add(Vector3Dd(0, 0, -ORIGIN_STEP));
        return TRUE;
      case KeyEvent::KEY_NUM7:
        origin = origin.add(Vector3Dd(0, 0, ORIGIN_STEP));
        return TRUE;
      case KeyEvent::KEY_NUMASTERISK:
        yawInDegrees -= ANGLE_STEP_DEGREES;
        return TRUE;
      case KeyEvent::KEY_NUMSLASH:
        yawInDegrees += ANGLE_STEP_DEGREES;
        return TRUE;
      case KeyEvent::KEY_NUMPLUS:
        pitchInDegrees += ANGLE_STEP_DEGREES;
        clampPitch();
        return TRUE;
      case KeyEvent::KEY_NUMMINUS:
        pitchInDegrees -= ANGLE_STEP_DEGREES;
        clampPitch();
        return TRUE;
      case KeyEvent::KEY_NUM9:
        gizmo->setMaxNumOfReflections(gizmo->getMaxNumOfReflections() + 1);
        return TRUE;
      case KeyEvent::KEY_NUM3:
        gizmo->setMaxNumOfReflections(gizmo->getMaxNumOfReflections() - 1);
        return TRUE;
      default:
        return FALSE;
    }
}

BOOLEAN RayGizmoInteractionTechniques::isActivationKey(const KeyEvent& event) const
{
    for ( long i = 0; i < activationKeys.size(); i++ ) {
        if ( event.keycode == activationKeys.get(i) ) {
            return TRUE;
        }
    }
    return FALSE;
}

Ray RayGizmoInteractionTechniques::currentRay() const
{
    return gizmo != 0 ?
        Ray(gizmo->getPosition(), gizmo->getDirection()) :
        Ray(Vector3Dd(0, 0, 0), Vector3Dd(0, 0, 1));
}

void RayGizmoInteractionTechniques::applyRay()
{
    updateInputGizmo();
    gizmo->setRay(Ray(origin, direction()), 0.0);
}

void RayGizmoInteractionTechniques::applyInputGizmoValues()
{
    ArrayList<double> values = getInputGizmo()->getValuesWithEdits();

    origin = Vector3Dd(values.get(0), values.get(1), values.get(2));
    yawInDegrees = values.get(3);
    pitchInDegrees = values.get(4);
    clampPitch();
}

void RayGizmoInteractionTechniques::updateInputGizmo()
{
    InputGizmo* input = getInputGizmo();

    if ( input == 0 ) {
        return;
    }
    input->setValue(0, origin.x());
    input->setValue(1, origin.y());
    input->setValue(2, origin.z());
    input->setValue(3, yawInDegrees);
    input->setValue(4, pitchInDegrees);
}

void RayGizmoInteractionTechniques::updateAnglesFromDirection(
    const Vector3Dd& direction)
{
    Vector3Dd d = direction.length() > 1e-9 ?
        direction.normalized() : Vector3Dd(0, 0, 1);

    yawInDegrees = atan2(d.y(), d.x()) * 180.0 / M_PI;
    pitchInDegrees = asin(Math::max(-1.0, Math::min(1.0, d.z()))) *
        180.0 / M_PI;
    clampPitch();
}

Vector3Dd RayGizmoInteractionTechniques::direction() const
{
    double yaw = yawInDegrees * M_PI / 180.0;
    double pitch = pitchInDegrees * M_PI / 180.0;
    double cp = cos(pitch);

    return Vector3Dd(
        cp * cos(yaw),
        cp * sin(yaw),
        sin(pitch));
}

void RayGizmoInteractionTechniques::clampPitch()
{
    if ( pitchInDegrees > 89.0 ) {
        pitchInDegrees = 89.0;
    }
    if ( pitchInDegrees < -89.0 ) {
        pitchInDegrees = -89.0;
    }
}
