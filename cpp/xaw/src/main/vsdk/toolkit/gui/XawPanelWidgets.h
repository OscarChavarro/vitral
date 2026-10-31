#ifndef __XAW_PANEL_WIDGETS__
#define __XAW_PANEL_WIDGETS__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
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
                               int width, int height, bool managed) override;
    virtual Widget createLabel(Widget parent, const java::String& text,
                               XFontSet fontSet, Justify justify,
                               int x, int y, int width, int height) override;
    virtual void setLabel(Widget label, const java::String& text) override;
    virtual Widget createTextField(Widget parent, const java::String& text,
                                   XFontSet fontSet,
                                   int x, int y, int width, int height,
                                   ActivateProc activate,
                                   void* clientData) override;
    virtual java::String getText(Widget field) override;
    virtual void setText(Widget field, const java::String& text) override;
    virtual void setInvalid(Widget field, bool invalid) override;
    virtual void setForeground(Widget widget, const char* colorName) override;
    virtual Widget createPushButton(Widget parent, const java::String& label,
                                    XFontSet fontSet,
                                    int x, int y, int width, int height,
                                    ActivateProc activate,
                                    void* clientData) override;
    virtual Widget createOptionButton(Widget parent,
                                      const java::String& placeholder,
                                      const java::ArrayList<java::String>& options,
                                      XFontSet fontSet,
                                      int x, int y, int width, int height,
                                      OptionProc choose,
                                      void* clientData) override;
};

#endif
