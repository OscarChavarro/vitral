#include <X11/Shell.h>
#include <Xm/Xm.h>
#include <Xm/FileSB.h>

#include "gui/xm/XmFileDialog.h"
#include "gui/xt/XtApplicationHost.h"
#include "gui/xt/XtUiFactory.h"
#include "io/FolderListing.h"
#include "vsdk/toolkit/gui/XmWidgetSupport.h"
#include "vsdk/toolkit/gui/XtWidgetSet.h"

namespace {
const int DIALOG_WIDTH = 520;
const int DIALOG_HEIGHT = 420;

std::string withoutTrailingSlash(const std::string& folder)
{
    if ( folder.size() > 1 && folder[folder.size() - 1] == '/' ) {
        return folder.substr(0, folder.size() - 1);
    }
    return folder;
}
}

XmFileDialog::XmFileDialog(const std::vector<FileSuffixFilter>& filters)
    : filters(filters), shell(nullptr), box(nullptr), done(false),
      approved(false)
{
}

XmFileDialog::~XmFileDialog()
{
    if ( shell != nullptr ) {
        XtDestroyWidget(shell);
    }
}

bool XmFileDialog::showDialog(XtApplicationHost* host,
                              const std::string& title,
                              const std::string& folder,
                              const std::vector<FileSuffixFilter>& filters,
                              std::string& outPath)
{
    XmFileDialog dialog(filters);
    dialog.create(host, title, FolderListing::isFolder(folder) ? folder :
                  std::string("."));

    XtPopup(dialog.shell, XtGrabExclusive);
    XmProcessTraversal(XmFileSelectionBoxGetChild(dialog.box, XmDIALOG_TEXT),
                       XmTRAVERSE_CURRENT);

    XtAppContext appContext = XtWidgetToApplicationContext(dialog.shell);
    while ( !dialog.done ) {
        XtAppProcessEvent(appContext, XtIMAll);
    }
    XtPopdown(dialog.shell);

    if ( dialog.approved ) {
        outPath = dialog.path;
    }
    return dialog.approved;
}

void XmFileDialog::create(XtApplicationHost* host, const std::string& title,
                          const std::string& folder)
{
    shell = host->createDialogShell("fileDialog", transientShellWidgetClass,
                                    title.c_str());
    host->getUiFactory()->getWidgetSet()->setWindowCloseHandler(
        shell, &XmFileDialog::closeRequested, this);

    XmString directory = XmWidgetSupport::createString(
        FolderListing::normalizeFolder(folder));
    XmString okLabel = XmWidgetSupport::createString(title);
    XmRenderTable fonts = XmWidgetSupport::createRenderTable(
        shell, host->getPanelFontSet());
    Arg args[12]; Cardinal n = 0;
    XtSetArg(args[n], XmNwidth, DIALOG_WIDTH); ++n;
    XtSetArg(args[n], XmNheight, DIALOG_HEIGHT); ++n;
    XtSetArg(args[n], XmNdirectory, directory); ++n;
    XtSetArg(args[n], XmNokLabelString, okLabel); ++n;
    XtSetArg(args[n], XmNfileSearchProc, &XmFileDialog::searchFiles); ++n;
    // The search procedure finds the dialog here
    XtSetArg(args[n], XmNuserData, this); ++n;
    XtSetArg(args[n], XmNfileFilterStyle, XmFILTER_HIDDEN_FILES); ++n;
    XtSetArg(args[n], XmNpathMode, XmPATH_MODE_RELATIVE); ++n;
    if ( fonts != nullptr ) {
        XtSetArg(args[n], XmNbuttonRenderTable, fonts); ++n;
        XtSetArg(args[n], XmNlabelRenderTable, fonts); ++n;
        XtSetArg(args[n], XmNtextRenderTable, fonts); ++n;
    }
    box = XmCreateFileSelectionBox(shell, const_cast<char*>("fileSelection"),
                                   args, n);
    XmStringFree(directory);
    XmStringFree(okLabel);
    if ( fonts != nullptr ) {
        XmRenderTableFree(fonts);
    }
    XtUnmanageChild(XmFileSelectionBoxGetChild(box, XmDIALOG_HELP_BUTTON));
    // The filters are the ones of the application, not a pattern to edit
    XtUnmanageChild(XmFileSelectionBoxGetChild(box, XmDIALOG_FILTER_LABEL));
    XtUnmanageChild(XmFileSelectionBoxGetChild(box, XmDIALOG_FILTER_TEXT));
    XtAddCallback(box, XmNokCallback, &XmFileDialog::approve, this);
    XtAddCallback(box, XmNcancelCallback, &XmFileDialog::cancel, this);
    XtManageChild(box);
}

/**
Lists the files of the folder being shown that the filters accept: the
file list of the box, as its default procedure does with its pattern.
*/
void XmFileDialog::searchFiles(Widget box, XtPointer callData)
{
    XmFileSelectionBoxCallbackStruct* search =
        static_cast<XmFileSelectionBoxCallbackStruct*>(callData);
    XtPointer userData = nullptr;
    XtVaGetValues(box, XmNuserData, &userData, nullptr);
    XmFileDialog* self = static_cast<XmFileDialog*>(userData);

    std::string folder = withoutTrailingSlash(
        XmWidgetSupport::toText(search->dir));
    std::vector<std::string> folders;
    std::vector<std::string> files;
    FolderListing::list(folder, self != nullptr ? self->filters :
                        std::vector<FileSuffixFilter>(), folders, files);

    std::vector<XmString> items;
    for ( size_t i = 0; i < files.size(); i++ ) {
        // Relative to the folder, as the path mode of the box asks
        items.push_back(XmWidgetSupport::createString(files[i]));
    }
    XtVaSetValues(box,
                  XmNfileListItems, items.empty() ? nullptr : items.data(),
                  XmNfileListItemCount, static_cast<int>(items.size()),
                  XmNlistUpdated, True, nullptr);
    for ( size_t i = 0; i < items.size(); i++ ) {
        XmStringFree(items[i]);
    }
}

void XmFileDialog::approve(Widget, XtPointer clientData, XtPointer callData)
{
    XmFileDialog* self = static_cast<XmFileDialog*>(clientData);
    XmFileSelectionBoxCallbackStruct* selection =
        static_cast<XmFileSelectionBoxCallbackStruct*>(callData);
    std::string chosen = XmWidgetSupport::toText(selection->value);
    if ( chosen.empty() ) {
        return;
    }
    if ( chosen[0] != '/' ) {
        chosen = withoutTrailingSlash(XmWidgetSupport::toText(selection->dir)) +
            "/" + chosen;
    }
    // A folder typed in the field is opened, as JFileChooser does
    if ( FolderListing::isFolder(chosen) ) {
        XmString directory = XmWidgetSupport::createString(
            FolderListing::normalizeFolder(chosen));
        XtVaSetValues(self->box, XmNdirectory, directory, nullptr);
        XmStringFree(directory);
        return;
    }
    self->path = chosen;
    self->approved = true;
    self->done = true;
}

void XmFileDialog::cancel(Widget, XtPointer clientData, XtPointer)
{
    XmFileDialog* self = static_cast<XmFileDialog*>(clientData);
    self->approved = false;
    self->done = true;
}

void XmFileDialog::closeRequested(void* clientData)
{
    XmFileDialog* self = static_cast<XmFileDialog*>(clientData);
    self->approved = false;
    self->done = true;
}
