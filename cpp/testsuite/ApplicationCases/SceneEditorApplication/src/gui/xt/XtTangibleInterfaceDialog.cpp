#include "java/lang/String.h"
#include <X11/Shell.h>
#include <X11/StringDefs.h>

#include "gui/xt/XtApplicationHost.h"
#include "gui/xt/XtTangibleInterfaceDialog.h"
#include "gui/xt/XtUiFactory.h"
#include "vsdk/toolkit/gui/XtWidgetSet.h"

const char* const XtTangibleInterfaceDialog::DIALOG_ID = "tangible.connect";

namespace {
const char* SERVICE_URL = "serviceUrl";
const char* CONNECT = "connect";
const char* CANCEL = "cancel";

java::String trim(const java::String& text)
{
    return text.trim();
}
}

XtTangibleInterfaceDialog::XtTangibleInterfaceDialog(XtApplicationHost* host)
    : host(host), form(nullptr), formSerial(0), connectBackground(0)
{
}

XtTangibleInterfaceDialog::~XtTangibleInterfaceDialog()
{
    discard();
}

void XtTangibleInterfaceDialog::discard()
{
    delete form;
    form = nullptr;
    ++formSerial;
}

void XtTangibleInterfaceDialog::show()
{
    const WidgetDialog* definition = host->getDialogById(DIALOG_ID);
    if ( definition == nullptr ) {
        host->showStatusMessage(java::String("Dialog not defined: ") + DIALOG_ID);
        return;
    }
    discard();
    XtApplicationHost* messages = host;
    form = XtDialogHost::form(
        host->createDialogShell("tangibleConnect", transientShellWidgetClass, ""),
        host->getUiFactory()->getWidgetSet(), host->getPanelFontSet(),
        [messages](const java::String& key) {
            return messages->getMessage(key.c_str());
        },
        definition,
        [this](const java::String& action) { actionPerformed(action); });
    XtVaGetValues(form->action(CONNECT), XtNbackground, &connectBackground,
                  nullptr);
    form->setText(SERVICE_URL, host->getTangibleServiceUrl());
    form->show();
}

void XtTangibleInterfaceDialog::actionPerformed(const java::String& action)
{
    if ( form == nullptr ) {
        return;
    }
    if ( action == CANCEL ) {
        form->close();
        return;
    }
    if ( action != CONNECT ) {
        return;
    }
    java::String serviceUrl = trim(form->getText(SERVICE_URL));
    form->session()->setDraft(SERVICE_URL, serviceUrl.c_str());
    if ( serviceUrl.isEmpty() ) {
        return;
    }
    long request = form->session()->beginAction();
    if ( request < 0 ) {
        return;
    }
    XtVaSetValues(form->action(CONNECT), XtNbackground, connectBackground,
                  nullptr);
    form->setActionLabel(CONNECT,
                         host->getMessage("IDM_TANGIBLE_INTERFACE_CONNECT"));
    long serial = formSerial;
    host->connectTangibleInterface(serviceUrl,
        [this, serial, request](bool connected) {
            connectionFinished(serial, request, connected);
        });
}

void XtTangibleInterfaceDialog::connectionFinished(long serial, long request,
                                                   bool connected)
{
    if ( form == nullptr || serial != formSerial ||
         !form->session()->completeAction(request) ) {
        return;
    }
    if ( connected ) {
        form->close();
        return;
    }
    XtVaSetValues(form->action(CONNECT), XtVaTypedArg, XtNbackground,
                  XtRString, "red", static_cast<int>(sizeof("red")), nullptr);
    form->setActionLabel(CONNECT,
        host->getMessage("IDM_TANGIBLE_INTERFACE_COULD_NOT_CONNECT_RETRY"));
}

bool XtTangibleInterfaceDialog::isVisible() const
{
    return form != nullptr && form->isVisible();
}

java::String XtTangibleInterfaceDialog::getServiceUrl() const
{
    return form != nullptr ? form->getText(SERVICE_URL) : java::String();
}

java::String XtTangibleInterfaceDialog::getConnectLabel() const
{
    return form != nullptr ? form->getActionLabel(CONNECT) : java::String();
}

bool XtTangibleInterfaceDialog::performAction(const java::String& action,
                                              const java::String* serviceUrl)
{
    if ( !isVisible() || form->action(action) == nullptr ) {
        return false;
    }
    if ( action == CONNECT && serviceUrl != nullptr ) {
        form->setText(SERVICE_URL, *serviceUrl);
    }
    actionPerformed(action);
    return true;
}
