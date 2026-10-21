#ifndef __CAMERA_CONTROLLER_ORBITER__
#define __CAMERA_CONTROLLER_ORBITER__

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/gui/CameraController.h"
class Camera;

class CameraControllerOrbiter : public CameraController {
public:
    explicit CameraControllerOrbiter(Camera* camera);
    virtual ~CameraControllerOrbiter() {}

    double getDeltaMovement() const;
    void setDeltaMovement(double val);

    Vector3Dd getPointOfInterest() const;
    void setPointOfInterest(const Vector3Dd& poi);

    bool processKeyPressedEvent(const KeyEvent& keyEvent);
    bool processKeyReleasedEvent(const KeyEvent& keyEvent);
    bool processMousePressedEvent(const MouseEvent& e);
    bool processMouseReleasedEvent(const MouseEvent& e);
    bool processMouseClickedEvent(const MouseEvent& e);
    bool processMouseMovedEvent(const MouseEvent& e);
    bool processMouseDraggedEvent(const MouseEvent& e);
    bool processMouseWheelEvent(const MouseEvent& e);

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
    bool orbitAroundPointOfInterest(double yawDelta, double pitchDelta);
};

#endif
