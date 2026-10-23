#ifndef __XT_TANGIBLE_INTERFACE_DIALOG__
#define __XT_TANGIBLE_INTERFACE_DIALOG__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.h"

#include "java/util/function/Consumer.h"

// Xt names its strings and objects as the vitral String and Object
// classes: renamed while its headers are read (see XtIntrinsics.h)
#define String XtString
#define Object XtObject
#include <X11/Intrinsic.h>
#undef Object
#undef String

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
class XtTangibleInterfaceDialog :
    private XtDialogHost::MessageSource,
    private XtDialogHost::ActionListener
{
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
    BOOLEAN isVisible() const;

    /**
    @return text of the field of the service URL
    */
    String getServiceUrl() const;

    /**
    @return text shown by the connect button
    */
    String getConnectLabel() const;

    /**
    Acts as a click on a button of the form (automation service).
    @param action action of a button: "connect" or "cancel"
    @param serviceUrl if not null, typed in the field before connecting
    @return false if the form is not shown or has no such button
    */
    BOOLEAN performAction(const String& action,
                       const String* serviceUrl);

private:
    /**
    Receives the answer of one connection request (a lambda in the C++11
    port): each request has its own, so late answers of older requests are
    told apart.
    */
    class ConnectionListener : public Consumer<BOOLEAN> {
    public:
        ConnectionListener(XtTangibleInterfaceDialog* dialog, long serial,
                           long request)
            : dialog(dialog), serial(serial), request(request) {}
        virtual void accept(BOOLEAN connected)
        {
            dialog->connectionFinished(serial, request, connected);
        }
    private:
        XtTangibleInterfaceDialog* dialog;
        long serial;
        long request;
    };
    friend class ConnectionListener;

    XtApplicationHost* host;
    /// Listeners of the requests made (they may be answered at any time)
    ArrayList<ConnectionListener*> connectionListeners;
    XtDialogHost::Form* form;
    /// Identifies the form shown, so answers for discarded ones are ignored
    long formSerial;
    Pixel connectBackground;

    // XtDialogHost::MessageSource and XtDialogHost::ActionListener (lambdas
    // in the C++11 port)
    virtual String message(const String& key);
    virtual void actionPerformed(const String& action);
    void connectionFinished(long serial, long request, BOOLEAN connected);

    XtTangibleInterfaceDialog(const XtTangibleInterfaceDialog& other);
    XtTangibleInterfaceDialog& operator=(const XtTangibleInterfaceDialog& other);
};

#endif
