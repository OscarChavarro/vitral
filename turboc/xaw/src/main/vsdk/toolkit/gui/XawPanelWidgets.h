#ifndef __XAW_PANEL_WIDGETS__
#define __XAW_PANEL_WIDGETS__

#include "vsdk/toolkit/gui/XtPanelWidgets.h"

/**
The panel building blocks with Athena (Xaw) widgets: panels are plain
Composites, labels are Labels, text fields are one line AsciiTexts (Athena
has no "activate" callback: Return is bound with a translation) and buttons
are Commands. Option buttons are MenuButtons with a SimpleMenu of options.
*/
class XawPanelWidgets : public XtPanelWidgets {
public:
    virtual Widget createPanel(Widget parent, const char* name, int x, int y,
                               int width, int height, bool managed);
    virtual Widget createLabel(Widget parent, const String& text,
                               XFontSet fontSet, Justify justify,
                               int x, int y, int width, int height);
    virtual void setLabel(Widget label, const String& text);
    virtual Widget createTextField(Widget parent, const String& text,
                                   XFontSet fontSet,
                                   int x, int y, int width, int height,
                                   ActivateProc activate,
                                   void* clientData);
    virtual String getText(Widget field);
    virtual void setText(Widget field, const String& text);
    virtual void setInvalid(Widget field, bool invalid);
    virtual void setForeground(Widget widget, const char* colorName);
    virtual Widget createPushButton(Widget parent, const String& label,
                                    XFontSet fontSet,
                                    int x, int y, int width, int height,
                                    ActivateProc activate,
                                    void* clientData);
    virtual Widget createOptionButton(Widget parent,
                                      const String& placeholder,
                                      const ArrayList<String>& options,
                                      XFontSet fontSet,
                                      int x, int y, int width, int height,
                                      OptionProc choose,
                                      void* clientData);
};

#endif
