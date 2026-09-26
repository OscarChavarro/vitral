#ifndef __XAW_INTRINSICS__
#define __XAW_INTRINSICS__

/**
Includes the Athena (Xaw) headers for the sources that also use the vitral
`Widget` class, with the Xt `Widget` type renamed as `XtIntrinsics.h`
does. It must be included before any other Xt header of the source.
*/
#include "vsdk/toolkit/gui/XtIntrinsics.h"

#define Widget XtIntrinsicWidget
#include <X11/Xaw/Command.h>
#include <X11/Xaw/Label.h>
#include <X11/Xaw/MenuButton.h>
#include <X11/Xaw/SimpleMenu.h>
#include <X11/Xaw/SmeBSB.h>
#include <X11/Xaw/SmeLine.h>
#undef Widget

#endif
