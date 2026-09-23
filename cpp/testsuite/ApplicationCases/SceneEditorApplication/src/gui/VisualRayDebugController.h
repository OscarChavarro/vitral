#ifndef __VISUAL_RAY_DEBUG_CONTROLLER__
#define __VISUAL_RAY_DEBUG_CONTROLLER__

#include "vsdk/toolkit/gui/KeyEvent.h"

class ApplicationModel;

/**
Keyboard control of the visual debug ray of the application model. It
processes only vitral events.

Keys (numeric keypad): `5` shows / hides the ray, `4` / `6` move its origin
along X, `2` / `8` along Y, `1` / `7` along Z, `9` / `3` change the number of
reflection levels, `*` and `/` rotate its direction around the vertical axis
and `+` and `-` around the horizontal one.
*/
class VisualRayDebugController {
private:
    ApplicationModel* model;

    void moveOrigin(double dx, double dy, double dz);
    void rotateDirection(double deltaTheta, double deltaPhi);

public:
    explicit VisualRayDebugController(ApplicationModel* model);

    /**
    @param event key pressed event
    @return true if the key is one of the visual debug ray commands
    */
    bool processKeyPressedEvent(const KeyEvent& event);
};

#endif
