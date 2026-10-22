#ifndef __CAMERA_CONTROLLER_BLENDER__
#define __CAMERA_CONTROLLER_BLENDER__

#include "vsdk/toolkit/gui/CameraController.h"
class Camera;

class CameraControllerBlender : public CameraController {
public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    explicit CameraControllerBlender(Camera* camera);
    virtual ~CameraControllerBlender() {}

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
    void setDeltaMovement(double factor);
    void tick(double inCurrentTime) {}

private:
    Camera* camera;

    double augmentLogarithmic(double val, double epsilon);
    double diminishLogarithmic(double val, double epsilon);
};

#endif
