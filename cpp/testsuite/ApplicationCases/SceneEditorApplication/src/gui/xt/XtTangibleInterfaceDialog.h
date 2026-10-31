#ifndef __XT_TANGIBLE_INTERFACE_DIALOG__
#define __XT_TANGIBLE_INTERFACE_DIALOG__

#include "java/lang/String.h"

#include <X11/Intrinsic.h>

#include "vsdk/toolkit/render/xt/XtDialogHost.h"

class XtApplicationHost;

/**
Behavior of the dialog to connect the editor with a tangible interface
websocket service (Tools > tangible interface client), as
`AwtJogl4GuiEventExecutor.showTangibleInterfaceDialog` in the Java
application: each time it is shown, its form is built by `XtDialogHost`
from the `tangible.connect` definition of `etc/gui/dialogs.json`.

Connecting asks the host (`XtApplicationHost::connectTangibleInterface`):
the dialog closes when the connection succeeds, otherwise its connect
button turns red and asks to retry. The `DialogSession` of the form ignores
the answers to requests of a closed form or to older requests.

It lives as long as the application: only its form is discarded when the
GUI is rebuilt.
*/
class XtTangibleInterfaceDialog {
public:
    static const char* const DIALOG_ID;

    explicit XtTangibleInterfaceDialog(XtApplicationHost* host);
    ~XtTangibleInterfaceDialog();

    /**
    Builds a new form and shows it with the URL of the service last used.
    */
    void show();

    /**
    Destroys the form, i.e. before the shell of the application.
    */
    void discard();

    /**
    @return true while the form is shown
    */
    bool isVisible() const;

    /**
    @return text of the field of the service URL
    */
    java::String getServiceUrl() const;

    /**
    @return text shown by the connect button
    */
    java::String getConnectLabel() const;

    /**
    Acts as a click on a button of the form (automation service).
    @param action action of a button: "connect" or "cancel"
    @param serviceUrl if not null, typed in the field before connecting
    @return false if the form is not shown or has no such button
    */
    bool performAction(const java::String& action,
                       const java::String* serviceUrl);

private:
    XtApplicationHost* host;
    XtDialogHost::Form* form;
    /// Identifies the form shown, so answers for discarded ones are ignored
    long formSerial;
    Pixel connectBackground;

    void actionPerformed(const java::String& action);
    void connectionFinished(long serial, long request, bool connected);

    XtTangibleInterfaceDialog(const XtTangibleInterfaceDialog& other);
    XtTangibleInterfaceDialog& operator=(const XtTangibleInterfaceDialog& other);
};

#endif
