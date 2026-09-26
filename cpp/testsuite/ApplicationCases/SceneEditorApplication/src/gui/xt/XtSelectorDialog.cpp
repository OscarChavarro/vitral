#include <X11/Shell.h>

#include "gui/xt/XtApplicationHost.h"
#include "gui/xt/XtSelectorDialog.h"
#include "gui/xt/XtUiFactory.h"
#include "vsdk/toolkit/gui/XtPanelWidgets.h"
#include "vsdk/toolkit/gui/XtWidgetSet.h"

namespace {
const int DIALOG_WIDTH = 526;
const int DIALOG_HEIGHT = 530;
const int BUTTON_WIDTH = 120;
const int BUTTON_HEIGHT = 28;

void testPressed(Widget, void*)
{
}
}

XtSelectorDialog::XtSelectorDialog(XtApplicationHost* host)
    : dialog(nullptr)
{
    dialog = host->createDialogShell("selectorDialog",
                                     transientShellWidgetClass, "");
    host->getUiFactory()->getWidgetSet()->setWindowCloseHandler(
        dialog, &XtSelectorDialog::closeRequested, this);
    XtPanelWidgets* widgets = host->getPanelWidgets();
    Widget mainFrameWidget = widgets->createPanel(
        dialog, "selectorFrame", 0, 0, DIALOG_WIDTH, DIALOG_HEIGHT, true);

    // Central area (north) and bottom area (south), as the Swing layout
    widgets->createPushButton(mainFrameWidget, "Test central",
                              host->getPanelFontSet(),
                              (DIALOG_WIDTH - BUTTON_WIDTH) / 2, 8,
                              BUTTON_WIDTH, BUTTON_HEIGHT, testPressed,
                              nullptr);
    widgets->createPushButton(mainFrameWidget, "Test bottom",
                              host->getPanelFontSet(),
                              (DIALOG_WIDTH - BUTTON_WIDTH) / 2,
                              DIALOG_HEIGHT - 36, BUTTON_WIDTH,
                              BUTTON_HEIGHT, testPressed, nullptr);
}

XtSelectorDialog::~XtSelectorDialog()
{
    if ( dialog != nullptr ) {
        XtDestroyWidget(dialog);
    }
}

void XtSelectorDialog::setVisible(bool visible)
{
    if ( !visible ) {
        XtPopdown(dialog);
        return;
    }
    XtPopup(dialog, XtGrabNone);
    XRaiseWindow(XtDisplay(dialog), XtWindow(dialog));
}

void XtSelectorDialog::closeRequested(void* clientData)
{
    XtPopdown(static_cast<XtSelectorDialog*>(clientData)->dialog);
}
