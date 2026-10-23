#ifndef __CAMERA_CONTROLLER_ORBITER__
#define __CAMERA_CONTROLLER_ORBITER__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/gui/CameraController.h"
class Camera;

class CameraControllerOrbiter : public CameraController {
public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    explicit CameraControllerOrbiter(Camera* camera);
    virtual ~CameraControllerOrbiter() {}

    double getDeltaMovement() const;
    void setDeltaMovement(double val);

    Vector3Dd getPointOfInterest() const;
    void setPointOfInterest(const Vector3Dd& poi);

    BOOLEAN processKeyPressedEvent(const KeyEvent& keyEvent);
    BOOLEAN processKeyReleasedEvent(const KeyEvent& keyEvent);
    BOOLEAN processMousePressedEvent(const MouseEvent& e);
    BOOLEAN processMouseReleasedEvent(const MouseEvent& e);
    BOOLEAN processMouseClickedEvent(const MouseEvent& e);
    BOOLEAN processMouseMovedEvent(const MouseEvent& e);
    BOOLEAN processMouseDraggedEvent(const MouseEvent& e);
    BOOLEAN processMouseWheelEvent(const MouseEvent& e);

    Camera* getCamera();
    void setCamera(Camera* camera);
    void tick(double inCurrentTime) {}

private:
    Camera* camera;
    int oldMouseX;
    int oldMouseY;
    double deltaMovement;
    Vector3Dd pointOfInterest;

    double augmentLogarithmic(double val, double epsilon);
    double diminishLogarithmic(double val, double epsilon);
    BOOLEAN orbitAroundPointOfInterest(double yawDelta, double pitchDelta);
};

#endif
