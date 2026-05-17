#ifndef CAMERACONTROLLER_H
#define CAMERACONTROLLER_H

#include "vsdk/toolkit/gui/Controller.h"
#include "vsdk/toolkit/gui/KeyEvent.h"
#include "vsdk/toolkit/gui/MouseEvent.h"

namespace vsdk { namespace toolkit { namespace environment { namespace camera {
class Camera;
}}}}

namespace vsdk { namespace toolkit { namespace gui {

class CameraController : public Controller {
public:
    virtual ~CameraController() {}

    virtual bool processKeyPressedEvent(const KeyEvent& keyEvent) = 0;
    virtual bool processKeyReleasedEvent(const KeyEvent& keyEvent) = 0;
    virtual bool processMousePressedEvent(const MouseEvent& e) = 0;
    virtual bool processMouseReleasedEvent(const MouseEvent& e) = 0;
    virtual bool processMouseClickedEvent(const MouseEvent& e) = 0;
    virtual bool processMouseMovedEvent(const MouseEvent& e) = 0;
    virtual bool processMouseDraggedEvent(const MouseEvent& e) = 0;
    virtual bool processMouseWheelEvent(const MouseEvent& e) = 0;

    virtual vsdk::toolkit::environment::camera::Camera* getCamera() = 0;
    virtual void setCamera(vsdk::toolkit::environment::camera::Camera* camera) = 0;
    virtual void setDeltaMovement(double factor) = 0;

    virtual void tick(double inCurrentTime) {}
};

}}}

#endif // CAMERACONTROLLER_H
