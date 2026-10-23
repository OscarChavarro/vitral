#ifndef __CAMERA_CONTROLLER_AQUYNZA__
#define __CAMERA_CONTROLLER_AQUYNZA__

#include "vsdk/toolkit/common/VSDK.h"
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

    double augmentLogarithmic(double val, double EPSILON);
    double diminishLogarithmic(double val, double EPSILON);
};


#endif
