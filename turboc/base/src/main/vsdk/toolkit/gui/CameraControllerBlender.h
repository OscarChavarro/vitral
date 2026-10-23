#ifndef __CAMERA_CONTROLLER_BLENDER__
#define __CAMERA_CONTROLLER_BLENDER__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/gui/CameraController.h"
class Camera;

class CameraControllerBlender : public CameraController {
public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    explicit CameraControllerBlender(Camera* camera);
    virtual ~CameraControllerBlender() {}

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
    void setDeltaMovement(double factor);
    void tick(double inCurrentTime) {}

private:
    Camera* camera;

    double augmentLogarithmic(double val, double epsilon);
    double diminishLogarithmic(double val, double epsilon);
};

#endif
