// Xt names its strings and objects as the vitral String and Object
// classes: renamed while its headers are read (see XtIntrinsics.h)
#define String XtString
#define Object XtObject
// First: the X11R6 headers (i.e. Debian 1.1) do not include it themselves
#include <X11/Intrinsic.h>
#include <X11/Shell.h>
#include <X11/StringDefs.h>
#undef Object
#undef String

#include "vsdk/toolkit/common/VSDK.h"
#include "gui/xt/XtApplicationHost.h"
#include "gui/xt/XtTangibleInterfaceDialog.h"
#include "gui/xt/XtUiFactory.h"
#include "vsdk/toolkit/gui/XtWidgetSet.h"

const char* const XtTangibleInterfaceDialog::DIALOG_ID = "tangible.connect";

static const char* SERVICE_URL = "serviceUrl";
static const char* CONNECT = "connect";
static const char* CANCEL = "cancel";

static String trim(const String& text)
{
    int start = 0;
    int end = text.size();
    while ( start < end && (text[start] == ' ' || text[start] == '\t') ) {
        start++;
    }
    while ( end > start &&
            (text[end - 1] == ' ' || text[end - 1] == '\t') ) {
        end--;
    }
    return text.substring(start, end);
}

XtTangibleInterfaceDialog::XtTangibleInterfaceDialog(XtApplicationHost* host)
    : host(host), form(NULL), formSerial(0), connectBackground(0)
{
}

XtTangibleInterfaceDialog::~XtTangibleInterfaceDialog()
{
    discard();
    for ( long i = 0; i < connectionListeners.size(); i++ ) {
        delete connectionListeners[i];
    }
}

void XtTangibleInterfaceDialog::discard()
{
    delete form;
    form = NULL;
    ++formSerial;
}

void XtTangibleInterfaceDialog::show()
{
    const WidgetDialog* definition = host->getDialogById(DIALOG_ID);
    if ( definition == NULL ) {
        host->showStatusMessage(String("Dialog not defined: ") + DIALOG_ID);
        return;
    }
    discard();
    form = XtDialogHost::form(
        host->createDialogShell("tangibleConnect", transientShellWidgetClass, ""),
        host->getUiFactory()->getWidgetSet(), host->getPanelFontSet(),
        this, definition, this);
    XtVaGetValues(form->action(CONNECT), XtNbackground, &connectBackground,
                  NULL);
    form->setText(SERVICE_URL, host->getTangibleServiceUrl());
    form->show();
}

String XtTangibleInterfaceDialog::message(const String& key)
{
    return host->getMessage(key.c_str());
}

void XtTangibleInterfaceDialog::actionPerformed(const String& action)
{
    if ( form == NULL ) {
        return;
    }
    if ( action == CANCEL ) {
        form->close();
        return;
    }
    if ( action != CONNECT ) {
        return;
    }
    String serviceUrl = trim(form->getText(SERVICE_URL));
    form->session()->setDraft(SERVICE_URL, serviceUrl.c_str());
    if ( serviceUrl.isEmpty() ) {
        return;
    }
    long request = form->session()->beginAction();
    if ( request < 0 ) {
        return;
    }
    XtVaSetValues(form->action(CONNECT), XtNbackground, connectBackground,
                  NULL);
    form->setActionLabel(CONNECT,
                         host->getMessage("IDM_TANGIBLE_INTERFACE_CONNECT"));
    ConnectionListener* listener =
        new ConnectionListener(this, formSerial, request);
    connectionListeners.add(listener);
    host->connectTangibleInterface(serviceUrl, listener);
}

void XtTangibleInterfaceDialog::connectionFinished(long serial, long request,
                                                   BOOLEAN connected)
{
    if ( form == NULL || serial != formSerial ||
         !form->session()->completeAction(request) ) {
        return;
    }
    if ( connected ) {
        form->close();
        return;
    }
    XtVaSetValues(form->action(CONNECT), XtVaTypedArg, XtNbackground,
                  XtRString, "red", ((int)(sizeof("red"))), NULL);
    form->setActionLabel(CONNECT,
        host->getMessage("IDM_TANGIBLE_INTERFACE_COULD_NOT_CONNECT_RETRY"));
}

BOOLEAN XtTangibleInterfaceDialog::isVisible() const
{
    return form != NULL && form->isVisible();
}

String XtTangibleInterfaceDialog::getServiceUrl() const
{
    return form != NULL ? form->getText(SERVICE_URL) : String();
}

String XtTangibleInterfaceDialog::getConnectLabel() const
{
    return form != NULL ? form->getActionLabel(CONNECT) : String();
}

BOOLEAN XtTangibleInterfaceDialog::performAction(const String& action,
                                              const String* serviceUrl)
{
    if ( !isVisible() || form->action(action) == NULL ) {
        return FALSE;
    }
    if ( action == CONNECT && serviceUrl != NULL ) {
        form->setText(SERVICE_URL, *serviceUrl);
    }
    actionPerformed(action);
    return TRUE;
}
