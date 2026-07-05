#ifndef __GLFWSYSTEM__
#define __GLFWSYSTEM__

#include "vsdk/toolkit/gui/KeyEvent.h"
#include "vsdk/toolkit/gui/MouseEvent.h"
class GlfwSystem {
public:
    static KeyEvent glfw2vsdkKeyEvent(int glfwKey, int glfwMods);
    static KeyEvent glfw2vsdkSpecialKeyEvent(int glfwKey, int glfwMods);
    static MouseEvent glfw2vsdkMouseEvent(int glfwButton, int glfwAction, double x, double y);
    static MouseEvent glfw2vsdkMotionEvent(double x, double y);
    static MouseEvent glfw2vsdkWheelEvent(double xoffset, double yoffset);

private:
    GlfwSystem();
    ~GlfwSystem();
};

#endif
