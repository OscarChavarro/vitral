#ifndef __VITRAL_FRAMEBUFFER_MENU_READER__
#define __VITRAL_FRAMEBUFFER_MENU_READER__

#include "java/lang/String.h"

class Widget;

// Reads the menubar from a Vitral GUI JSON document into an existing Widget.
class FramebufferMenuReader {
public:
    static bool read(const java::String &json, Widget &widget);
};

#endif
