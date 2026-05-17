#ifndef CAMERACONTROLLERAQUYNZA_H
#define CAMERACONTROLLERAQUYNZA_H

#include "vsdk/toolkit/gui/CameraController.h"

namespace vsdk { namespace toolkit { namespace environment { namespace camera {
class Camera;
}}}}

namespace vsdk { namespace toolkit { namespace gui {

class CameraControllerAquynza : public CameraController {
public:
    explicit CameraControllerAquynza(vsdk::toolkit::environment::camera::Camera* camera);
    virtual ~CameraControllerAquynza() {}

    double getDeltaMovement() const;
    void setDeltaMovement(double val) override;

    bool processKeyPressedEvent(const KeyEvent& keyEvent) override;
    bool processKeyReleasedEvent(const KeyEvent& keyEvent) override;
    bool processMousePressedEvent(const MouseEvent& e) override;
    bool processMouseReleasedEvent(const MouseEvent& e) override;
    bool processMouseClickedEvent(const MouseEvent& e) override;
    bool processMouseMovedEvent(const MouseEvent& e) override;
    bool processMouseDraggedEvent(const MouseEvent& e) override;
    bool processMouseWheelEvent(const MouseEvent& e) override;

    vsdk::toolkit::environment::camera::Camera* getCamera() override;
    void setCamera(vsdk::toolkit::environment::camera::Camera* camera) override;
    void tick(double inCurrentTime) override {}

private:
    vsdk::toolkit::environment::camera::Camera* camera;
    int oldMouseX;
    int oldMouseY;
    double deltaMov;

    double augmentLogarithmic(double val, double EPSILON);
    double diminishLogarithmic(double val, double EPSILON);
};

}}}

#endif // CAMERACONTROLLERAQUYNZA_H
