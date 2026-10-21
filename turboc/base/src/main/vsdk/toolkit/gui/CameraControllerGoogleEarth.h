#ifndef __CAMERA_CONTROLLER_GOOGLE_EARTH__
#define __CAMERA_CONTROLLER_GOOGLE_EARTH__

#include "vsdk/toolkit/gui/CameraController.h"
class Camera;

class CameraControllerGoogleEarth : public CameraController {
public:
    explicit CameraControllerGoogleEarth(Camera* camera);
    virtual ~CameraControllerGoogleEarth() {}

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

    void zoomOut(double jumpValue);
    void zoomIn(double jumpValue);

private:
    Camera* camera;
    double jumpStep;
    int xOld;
    int yOld;
};

#endif
