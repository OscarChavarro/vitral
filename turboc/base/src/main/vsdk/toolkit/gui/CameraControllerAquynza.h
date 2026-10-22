#ifndef __CAMERA_CONTROLLER_AQUYNZA__
#define __CAMERA_CONTROLLER_AQUYNZA__

#include "vsdk/toolkit/gui/CameraController.h"
class Camera;

class CameraControllerAquynza : public CameraController {
public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    explicit CameraControllerAquynza(Camera* camera);
    virtual ~CameraControllerAquynza() {}

    double getDeltaMovement() const;
    void setDeltaMovement(double val);

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

    double augmentLogarithmic(double val, double EPSILON);
    double diminishLogarithmic(double val, double EPSILON);
};


#endif
