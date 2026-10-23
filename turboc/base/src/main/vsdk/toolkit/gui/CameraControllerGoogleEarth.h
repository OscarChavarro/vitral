#ifndef __CAMERA_CONTROLLER_GOOGLE_EARTH__
#define __CAMERA_CONTROLLER_GOOGLE_EARTH__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/gui/CameraController.h"
class Camera;

class CameraControllerGoogleEarth : public CameraController {
public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    explicit CameraControllerGoogleEarth(Camera* camera);
    virtual ~CameraControllerGoogleEarth() {}

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

    void zoomOut(double jumpValue);
    void zoomIn(double jumpValue);

private:
    Camera* camera;
    double jumpStep;
    int xOld;
    int yOld;
};

#endif
