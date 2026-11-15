#ifndef __VTRL_FRMBFFR_MN_RDR__
#define __VTRL_FRMBFFR_MN_RDR__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"

class Widget;

// Reads the menubar from a Vitral GUI JSON document into an existing Widget.
class FramebufferMenuReader {
public:
    static BOOLEAN read(const String &json, Widget &widget);
};

#endif
