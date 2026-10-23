#ifndef __CAMERA_CONTROLLER__
#define __CAMERA_CONTROLLER__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/gui/Controller.h"
#include "vsdk/toolkit/gui/KeyEvent.h"
#include "vsdk/toolkit/gui/MouseEvent.h"
class Camera;

class CameraController : public Controller {
public:
    static const Class CLASS;
    virtual const Class *getClass() const;

    virtual ~CameraController() {}

    virtual BOOLEAN processKeyPressedEvent(const KeyEvent& keyEvent) = 0;
    virtual BOOLEAN processKeyReleasedEvent(const KeyEvent& keyEvent) = 0;
    virtual BOOLEAN processMousePressedEvent(const MouseEvent& e) = 0;
    virtual BOOLEAN processMouseReleasedEvent(const MouseEvent& e) = 0;
    virtual BOOLEAN processMouseClickedEvent(const MouseEvent& e) = 0;
    virtual BOOLEAN processMouseMovedEvent(const MouseEvent& e) = 0;
    virtual BOOLEAN processMouseDraggedEvent(const MouseEvent& e) = 0;
    virtual BOOLEAN processMouseWheelEvent(const MouseEvent& e) = 0;

    virtual Camera* getCamera() = 0;
    virtual void setCamera(Camera* camera) = 0;
    virtual void setDeltaMovement(double factor) = 0;

    virtual void tick(double inCurrentTime) {}
};

#endif
