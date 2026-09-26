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
                               int width, int height, bool managed) override;
    virtual Widget createLabel(Widget parent, const std::string& text,
                               XFontSet fontSet, Justify justify,
                               int x, int y, int width, int height) override;
    virtual void setLabel(Widget label, const std::string& text) override;
    virtual Widget createTextField(Widget parent, const std::string& text,
                                   XFontSet fontSet,
                                   int x, int y, int width, int height,
                                   ActivateProc activate,
                                   void* clientData) override;
    virtual std::string getText(Widget field) override;
    virtual void setText(Widget field, const std::string& text) override;
    virtual void setInvalid(Widget field, bool invalid) override;
    virtual void setForeground(Widget widget, const char* colorName) override;
    virtual Widget createPushButton(Widget parent, const std::string& label,
                                    XFontSet fontSet,
                                    int x, int y, int width, int height,
                                    ActivateProc activate,
                                    void* clientData) override;
    virtual Widget createOptionButton(Widget parent,
                                      const std::string& placeholder,
                                      const std::vector<std::string>& options,
                                      XFontSet fontSet,
                                      int x, int y, int width, int height,
                                      OptionProc choose,
                                      void* clientData) override;
};

#endif
