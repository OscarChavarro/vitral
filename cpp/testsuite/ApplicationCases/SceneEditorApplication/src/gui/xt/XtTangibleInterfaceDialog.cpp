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

std::string trim(const std::string& text)
{
    size_t start = text.find_first_not_of(" \t");
    if ( start == std::string::npos ) {
        return "";
    }
    return text.substr(start, text.find_last_not_of(" \t") - start + 1);
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
        host->showStatusMessage(std::string("Dialog not defined: ") + DIALOG_ID);
        return;
    }
    discard();
    XtApplicationHost* messages = host;
    form = XtDialogHost::form(
        host->createDialogShell("tangibleConnect", transientShellWidgetClass, ""),
        host->getUiFactory()->getWidgetSet(), host->getPanelFontSet(),
        [messages](const std::string& key) {
            return messages->getMessage(key.c_str());
        },
        definition,
        [this](const std::string& action) { actionPerformed(action); });
    XtVaGetValues(form->action(CONNECT), XtNbackground, &connectBackground,
                  nullptr);
    form->setText(SERVICE_URL, host->getTangibleServiceUrl());
    form->show();
}

void XtTangibleInterfaceDialog::actionPerformed(const std::string& action)
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
    std::string serviceUrl = trim(form->getText(SERVICE_URL));
    form->session()->setDraft(SERVICE_URL, serviceUrl.c_str());
    if ( serviceUrl.empty() ) {
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

std::string XtTangibleInterfaceDialog::getServiceUrl() const
{
    return form != nullptr ? form->getText(SERVICE_URL) : std::string();
}

std::string XtTangibleInterfaceDialog::getConnectLabel() const
{
    return form != nullptr ? form->getActionLabel(CONNECT) : std::string();
}

bool XtTangibleInterfaceDialog::performAction(const std::string& action,
                                              const std::string* serviceUrl)
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
