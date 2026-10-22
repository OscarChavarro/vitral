// Xt names its strings and objects as the vitral String and Object
// classes: renamed while its headers are read (see XtIntrinsics.h)
#define String XtString
#define Object XtObject
// First: the X11R6 headers (i.e. Debian 1.1) do not include it themselves
#include <X11/Intrinsic.h>
#include <X11/Shell.h>
#include <Xm/Xm.h>
#include <Xm/FileSB.h>
#undef Object
#undef String

#include "gui/xm/XmFileDialog.h"
#include "gui/xt/XtApplicationHost.h"
#include "gui/xt/XtUiFactory.h"
#include "io/FolderListing.h"
#include "vsdk/toolkit/gui/XmWidgetSupport.h"
#include "vsdk/toolkit/gui/XtWidgetSet.h"
#include "java/util/ArrayList.txx"

static const int DIALOG_WIDTH = 520;
static const int DIALOG_HEIGHT = 420;

static String withoutTrailingSlash(const String& folder)
{
    if ( folder.size() > 1 && folder[folder.size() - 1] == '/' ) {
        return folder.substr(0, folder.size() - 1);
    }
    return folder;
}

XmFileDialog::XmFileDialog(const ArrayList<FileSuffixFilter>& filters)
    : filters(filters), shell(NULL), box(NULL), done(false),
      approved(false)
{
}

XmFileDialog::~XmFileDialog()
{
    if ( shell != NULL ) {
        XtDestroyWidget(shell);
    }
}

bool XmFileDialog::showDialog(XtApplicationHost* host,
                              const String& title,
                              const String& folder,
                              const ArrayList<FileSuffixFilter>& filters,
                              String& outPath)
{
    XmFileDialog dialog(filters);
    dialog.create(host, title, FolderListing::isFolder(folder) ? folder :
                  String("."));

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

void XmFileDialog::create(XtApplicationHost* host, const String& title,
                          const String& folder)
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
#if XmVersion >= 2000
    // Motif 1.2 (i.e. LessTif 0.76) has neither: it shows the hidden files
    // and absolute paths
    XtSetArg(args[n], XmNfileFilterStyle, XmFILTER_HIDDEN_FILES); ++n;
    XtSetArg(args[n], XmNpathMode, XmPATH_MODE_RELATIVE); ++n;
#endif
    if ( fonts != NULL ) {
        XtSetArg(args[n], XmNbuttonRenderTable, fonts); ++n;
        XtSetArg(args[n], XmNlabelRenderTable, fonts); ++n;
        XtSetArg(args[n], XmNtextRenderTable, fonts); ++n;
    }
    box = XmCreateFileSelectionBox(shell, ((char*)("fileSelection")),
                                   args, n);
    XmStringFree(directory);
    XmStringFree(okLabel);
    if ( fonts != NULL ) {
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
        ((XmFileSelectionBoxCallbackStruct*)(callData));
    XtPointer userData = NULL;
    XtVaGetValues(box, XmNuserData, &userData, NULL);
    XmFileDialog* self = ((XmFileDialog*)(userData));

    String folder = withoutTrailingSlash(
        XmWidgetSupport::toText(search->dir));
    ArrayList<String> folders;
    ArrayList<String> files;
    // No temporary list in the conditional: gcc 2.7.2 crashes on it
    ArrayList<FileSuffixFilter> noFilters;
    const ArrayList<FileSuffixFilter>& filters =
        self != NULL ? self->filters : noFilters;
    FolderListing::list(folder, filters, folders, files);

    ArrayList<XmString> items;
    for ( long i = 0; i < files.size(); i++ ) {
        // Relative to the folder, as the path mode of the box asks
        items.add(XmWidgetSupport::createString(files[i]));
    }
    XtVaSetValues(box,
                  XmNfileListItems, items.isEmpty() ? NULL : items.data(),
                  XmNfileListItemCount, ((int)(items.size())),
                  XmNlistUpdated, True, NULL);
    for ( long i = 0; i < items.size(); i++ ) {
        XmStringFree(items[i]);
    }
}

void XmFileDialog::approve(Widget, XtPointer clientData, XtPointer callData)
{
    XmFileDialog* self = ((XmFileDialog*)(clientData));
    XmFileSelectionBoxCallbackStruct* selection =
        ((XmFileSelectionBoxCallbackStruct*)(callData));
    String chosen = XmWidgetSupport::toText(selection->value);
    if ( chosen.isEmpty() ) {
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
        XtVaSetValues(self->box, XmNdirectory, directory, NULL);
        XmStringFree(directory);
        return;
    }
    self->path = chosen;
    self->approved = true;
    self->done = true;
}

void XmFileDialog::cancel(Widget, XtPointer clientData, XtPointer)
{
    XmFileDialog* self = ((XmFileDialog*)(clientData));
    self->approved = false;
    self->done = true;
}

void XmFileDialog::closeRequested(void* clientData)
{
    XmFileDialog* self = ((XmFileDialog*)(clientData));
    self->approved = false;
    self->done = true;
}
