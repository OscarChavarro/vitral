#ifndef __GTK4_SYSTEM__
#define __GTK4_SYSTEM__

#include <gtk/gtk.h>

#include "vsdk/toolkit/gui/KeyEvent.h"
#include "vsdk/toolkit/gui/MouseEvent.h"

class Gtk4System {
public:
    static MouseEvent mouseEvent(double x, double y, guint button,
                                 GdkModifierType state);
    static KeyEvent keyEvent(guint keyval, GdkModifierType state);

private:
    Gtk4System();
};

#endif
