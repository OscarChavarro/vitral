#ifndef __XT_INTRINSICS__
#define __XT_INTRINSICS__

/**
Includes the Xt and Xaw headers for the sources that also use the vitral
`Widget` class, naming the Xt `Widget` type `XtIntrinsicWidget` instead
(`XtWidget`, of "vsdk/toolkit/gui/XtWidget.h", is the same type). It must
be included before any other Xt header of the source: Xt headers are read
only once.

The functions of Xt have C linkage, so renaming the type of their
parameters does not change the symbols they link to.
*/
#if defined(_XtIntrinsic_h) && !defined(__XT_INTRINSICS_RENAMED__)
#error "vsdk/toolkit/gui/XtIntrinsics.h must be included before any Xt header"
#endif
#define __XT_INTRINSICS_RENAMED__

#define Widget XtIntrinsicWidget
#include <X11/Intrinsic.h>
#include <X11/StringDefs.h>
#include <X11/Shell.h>
#include <X11/Composite.h>
#include <X11/Xaw/Command.h>
#include <X11/Xaw/Label.h>
#include <X11/Xaw/MenuButton.h>
#include <X11/Xaw/SimpleMenu.h>
#include <X11/Xaw/SmeBSB.h>
#include <X11/Xaw/SmeLine.h>
#undef Widget

#include "vsdk/toolkit/gui/XtWidget.h"

#endif
