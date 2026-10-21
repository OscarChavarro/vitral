#ifndef __XT_GENERIC_EDITOR__
#define __XT_GENERIC_EDITOR__

#include <string>
#include <utility>
#include <vector>

// Xt names its strings and objects as the vitral String and Object
// classes: they are renamed while its headers are read (see
// XtIntrinsics.h)
#define String XtString
#define Object XtObject
#include <X11/Intrinsic.h>
#undef Object
#undef String

#include "vsdk/toolkit/gui/editor/GenericEditor.h"

class XtPanelWidgets;

/**
Xt presentation of a `GenericEditor`, as `AwtGenericEditor` is for Swing:
a title, one labeled text field per control specification and a message
line. All the logic (reading, validating and writing values through the
accessors registered by the entity) lives in `GenericEditor`; this class
only creates widgets, with the `XtPanelWidgets` of the widget set of the
application (Athena or Motif), and forwards what the user types (confirmed
with Return).
*/
class XtGenericEditor : public GenericEditor {
private:
    XtPanelWidgets* widgets;
    Widget container;
    XFontSet fontSet;
    int width;
    int nextY;
    Widget messageLabel;
    /// Last message, kept to show it once the message label exists
    std::string pendingMessage;
    std::vector<std::pair<Widget, const ControlSpecification*> > fields;

    static void fieldActivated(Widget field, void* clientData);

protected:
    virtual void beginBuild(const String& title);
    virtual void addControl(const ControlSpecification* specification,
                            const String& value);
    virtual void endBuild();
    virtual void showValidationMessage(const char* message);
    virtual void setControlValue(const ControlSpecification* specification,
                                 const String& value);
    virtual void clearControls(const String& message);

public:
    /**
    @param widgets builder of the widgets of the editor (referenced)
    @param container panel that will hold the editor, placing its
    children at explicit positions; its previous contents are removed on
    each `build`
    @param fontSet font set of the labels
    @param width width available in the container
    */
    XtGenericEditor(XtPanelWidgets* widgets, Widget container,
                    XFontSet fontSet, int width);
};

#endif
