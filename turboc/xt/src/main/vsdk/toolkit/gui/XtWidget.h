#ifndef __XT_WIDGET__
#define __XT_WIDGET__

/**
The Xt `Widget` type under a name that does not collide with the vitral
`Widget` class (the GUI context of `vsdk/toolkit/gui/widget`): headers
that deal with both use it. It is the very same type as the `Widget` of
<X11/Intrinsic.h>, so both names can be mixed.
*/
struct _WidgetRec;
typedef struct _WidgetRec* XtWidget;

#endif
