// Xt names its strings and objects as the vitral String and Object
// classes: renamed while its headers are read (see XtIntrinsics.h)
#define String XtString
#define Object XtObject
// First: the X11R6 headers (i.e. Debian 1.1) do not include it themselves
#include <X11/Intrinsic.h>
#include <X11/StringDefs.h>
#include <X11/Shell.h>
#include <X11/Xaw/List.h>
#include <X11/Xaw/Viewport.h>
#undef Object
#undef String

#include "vsdk/toolkit/common/VSDK.h"
#include "gui/xaw/XawFileDialog.h"
#include "gui/xt/XtApplicationHost.h"
#include "gui/xt/XtUiFactory.h"
#include "io/FolderListing.h"
#include "vsdk/toolkit/gui/XtPanelWidgets.h"
#include "vsdk/toolkit/gui/XtWidgetSet.h"
#include "java/util/ArrayList.txx"

static const int DIALOG_WIDTH = 520;
static const int DIALOG_HEIGHT = 420;

XawFileDialog::XawFileDialog(XtApplicationHost* host,
                             const ArrayList<FileSuffixFilter>& filters)
    : host(host), widgets(host->getPanelWidgets()), filters(filters),
      shell(NULL), folderLabel(NULL), list(NULL), pathField(NULL),
      done(FALSE), approved(FALSE)
{
}

XawFileDialog::~XawFileDialog()
{
    if ( shell != NULL ) {
        XtDestroyWidget(shell);
    }
}

BOOLEAN XawFileDialog::showDialog(XtApplicationHost* host,
                               const String& title,
                               const String& folder,
                               const ArrayList<FileSuffixFilter>& filters,
                               String& outPath)
{
    XawFileDialog dialog(host, filters);
    dialog.create(title);
    dialog.showFolder(FolderListing::isFolder(folder) ? folder :
                      String("."));

    XtPopup(dialog.shell, XtGrabExclusive);
    XtSetKeyboardFocus(dialog.shell, dialog.pathField);

    XtAppContext appContext = XtWidgetToApplicationContext(dialog.shell);
    while ( !dialog.done ) {
        XtAppProcessEvent(appContext, XtIMAll);
    }
    XtPopdown(dialog.shell);

    if ( dialog.approved ) {
        String path = dialog.widgets->getText(dialog.pathField);
        if ( path.isEmpty() ) {
            return FALSE;
        }
        if ( path[0] != '/' ) {
            path = dialog.folder + "/" + path;
        }
        outPath = path;
    }
    return dialog.approved;
}

void XawFileDialog::create(const String& title)
{
    XFontSet fontSet = host->getPanelFontSet();
    shell = host->createDialogShell("fileDialog", transientShellWidgetClass,
                                    title.c_str());
    host->getUiFactory()->getWidgetSet()->setWindowCloseHandler(
        shell, &XawFileDialog::closeRequested, this);

    Widget form = widgets->createPanel(shell, "fileDialogForm", 0, 0,
                                       DIALOG_WIDTH, DIALOG_HEIGHT, TRUE);

    folderLabel = widgets->createLabel(form, "", fontSet,
        XtPanelWidgets::LEFT, 8, 8, DIALOG_WIDTH - 16, 24);

    Arg viewArgs[6]; Cardinal viewN = 0;
    XtSetArg(viewArgs[viewN], XtNx, 8); ++viewN;
    XtSetArg(viewArgs[viewN], XtNy, 36); ++viewN;
    XtSetArg(viewArgs[viewN], XtNwidth, DIALOG_WIDTH - 16); ++viewN;
    XtSetArg(viewArgs[viewN], XtNheight, DIALOG_HEIGHT - 120); ++viewN;
    XtSetArg(viewArgs[viewN], XtNallowVert, True); ++viewN;
    XtSetArg(viewArgs[viewN], XtNforceBars, True); ++viewN;
    Widget view = XtCreateManagedWidget("fileDialogView", viewportWidgetClass,
                                        form, viewArgs, viewN);

    Arg listArgs[5]; Cardinal listN = 0;
    XtSetArg(listArgs[listN], XtNdefaultColumns, 1); ++listN;
    XtSetArg(listArgs[listN], XtNforceColumns, True); ++listN;
    XtSetArg(listArgs[listN], XtNverticalList, True); ++listN;
    XtSetArg(listArgs[listN], XtNinternational, True); ++listN;
    XtSetArg(listArgs[listN], XtNfontSet, fontSet); ++listN;
    list = XtCreateManagedWidget("fileDialogList", listWidgetClass, view,
                                 listArgs, listN);
    XtAddCallback(list, XtNcallback, &XawFileDialog::entrySelected, this);

    pathField = widgets->createTextField(form, "", fontSet, 8,
        DIALOG_HEIGHT - 76, DIALOG_WIDTH - 16, 26,
        &XawFileDialog::pathActivated, this);

    const char* labels[] = { "Open", "Cancel" };
    XtPanelWidgets::ActivateProc callbacks[] = { &XawFileDialog::approve,
                                                 &XawFileDialog::cancel };
    for ( int i = 0; i < 2; i++ ) {
        widgets->createPushButton(form, labels[i], fontSet,
                                  DIALOG_WIDTH - 8 - (2 - i) * 108,
                                  DIALOG_HEIGHT - 40, 100, 28,
                                  callbacks[i], this);
    }
}

void XawFileDialog::showFolder(const String& newFolder)
{
    folder = FolderListing::normalizeFolder(newFolder);
    widgets->setLabel(folderLabel, folder);

    ArrayList<String> folders;
    ArrayList<String> files;
    FolderListing::list(folder, filters, folders, files);

    entries.clear();
    entries.add("../");
    long i;
    for ( i = 0; i < folders.size(); i++ ) {
        entries.add(folders[i] + "/");
    }
    entries.addAll(files);
    entryTexts.clear();
    for ( i = 0; i < entries.size(); i++ ) {
        entryTexts.add(entries[i].c_str());
    }
    // Older Xaw (i.e. 1.0.3, XawVersion 6700002, of Ubuntu 7.04 and
    // XQuartz) declares String* (XtString* here), whereas current Xaw uses
    // const char**. XawListChange only reads the entries in either API.
#if !defined(XawVersion) || XawVersion <= 6700002L
    XtString* xawEntries = (XtString*)(&entryTexts[0]);
#else
    const char** xawEntries = entryTexts.data();
#endif
    XawListChange(list, xawEntries,
                  ((int)(entryTexts.size())), 0, True);
}

void XawFileDialog::close(BOOLEAN approve)
{
    approved = approve;
    done = TRUE;
}

void XawFileDialog::entrySelected(Widget, XtPointer clientData,
                                  XtPointer callData)
{
    XawFileDialog* self = ((XawFileDialog*)(clientData));
    XawListReturnStruct* selected = ((XawListReturnStruct*)(callData));
    if ( self == NULL || selected == NULL || selected->list_index < 0 ||
         selected->list_index >= ((int)(self->entries.size())) ) {
        return;
    }
    String name = self->entries[selected->list_index];
    if ( !name.isEmpty() && name[name.size() - 1] == '/' ) {
        // Changing the list inside its own callback is allowed by Xaw
        self->showFolder(self->folder + "/" + name.substr(0, name.size() - 1));
        self->widgets->setText(self->pathField, "");
    }
    else {
        self->widgets->setText(self->pathField, self->folder + "/" + name);
    }
}

void XawFileDialog::approve(Widget, void* clientData)
{
    ((XawFileDialog*)(clientData))->close(TRUE);
}

void XawFileDialog::cancel(Widget, void* clientData)
{
    ((XawFileDialog*)(clientData))->close(FALSE);
}

void XawFileDialog::pathActivated(Widget, void* clientData)
{
    XawFileDialog* self = ((XawFileDialog*)(clientData));
    String path = self->widgets->getText(self->pathField);
    String full = !path.isEmpty() && path[0] == '/' ? path :
        self->folder + "/" + path;
    // A folder typed in the field is opened, as JFileChooser does
    if ( !path.isEmpty() && FolderListing::isFolder(full) ) {
        self->showFolder(full);
        self->widgets->setText(self->pathField, "");
        return;
    }
    self->close(TRUE);
}

void XawFileDialog::closeRequested(void* clientData)
{
    ((XawFileDialog*)(clientData))->close(FALSE);
}
