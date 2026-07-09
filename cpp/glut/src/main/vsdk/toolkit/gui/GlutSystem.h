#ifndef __GLUT_SYSTEM__
#define __GLUT_SYSTEM__

#include "vsdk/toolkit/gui/KeyEvent.h"
#include "vsdk/toolkit/gui/MouseEvent.h"
class GlutSystem {
public:
    static KeyEvent glut2vsdkKeyEvent(unsigned char glutKey, int modifiers);
    static KeyEvent glut2vsdkSpecialKeyEvent(int glutSpecialKey, int modifiers);
    static MouseEvent glut2vsdkMouseEvent(int button, int state, int x, int y);
    static MouseEvent glut2vsdkMotionEvent(int x, int y);
    static MouseEvent glut2vsdkWheelEvent(int wheel, int direction, int x, int y);

private:
    GlutSystem();
    ~GlutSystem();
};

#endif
