#ifndef __XM_INTRINSICS__
#define __XM_INTRINSICS__

/**
Includes the Motif (Xm) headers for the sources that also use the vitral
`Widget` class, with the Xt `Widget` type renamed as `XtIntrinsics.h`
does. It must be included before any other Xt or Motif header of the
source.
*/
#include "vsdk/toolkit/gui/XtIntrinsics.h"

#define Widget XtIntrinsicWidget
#include <Xm/Xm.h>
#include <Xm/BulletinB.h>
#include <Xm/CascadeB.h>
#include <Xm/Label.h>
#include <Xm/PushB.h>
#include <Xm/RowColumn.h>
#include <Xm/Separator.h>
#include <Xm/VendorS.h>
#undef Widget

#endif
