#include <X11/StringDefs.h>
#include <X11/Shell.h>
#include <X11/Xaw/List.h>
#include <X11/Xaw/Viewport.h>

#include "gui/xaw/XawFileDialog.h"
#include "gui/xt/XtApplicationHost.h"
#include "gui/xt/XtUiFactory.h"
#include "io/FolderListing.h"
#include "vsdk/toolkit/gui/XtPanelWidgets.h"
#include "vsdk/toolkit/gui/XtWidgetSet.h"

namespace {
const int DIALOG_WIDTH = 520;
const int DIALOG_HEIGHT = 420;
}

XawFileDialog::XawFileDialog(XtApplicationHost* host,
                             const std::vector<FileSuffixFilter>& filters)
    : host(host), widgets(host->getPanelWidgets()), filters(filters),
      shell(nullptr), folderLabel(nullptr), list(nullptr), pathField(nullptr),
      done(false), approved(false)
{
}

XawFileDialog::~XawFileDialog()
{
    if ( shell != nullptr ) {
        XtDestroyWidget(shell);
    }
}

bool XawFileDialog::showDialog(XtApplicationHost* host,
                               const std::string& title,
                               const std::string& folder,
                               const std::vector<FileSuffixFilter>& filters,
                               std::string& outPath)
{
    XawFileDialog dialog(host, filters);
    dialog.create(title);
    dialog.showFolder(FolderListing::isFolder(folder) ? folder :
                      std::string("."));

    XtPopup(dialog.shell, XtGrabExclusive);
    XtSetKeyboardFocus(dialog.shell, dialog.pathField);

    XtAppContext appContext = XtWidgetToApplicationContext(dialog.shell);
    while ( !dialog.done ) {
        XtAppProcessEvent(appContext, XtIMAll);
    }
    XtPopdown(dialog.shell);

    if ( dialog.approved ) {
        std::string path = dialog.widgets->getText(dialog.pathField);
        if ( path.empty() ) {
            return false;
        }
        if ( path[0] != '/' ) {
            path = dialog.folder + "/" + path;
        }
        outPath = path;
    }
    return dialog.approved;
}

void XawFileDialog::create(const std::string& title)
{
    XFontSet fontSet = host->getPanelFontSet();
    shell = host->createDialogShell("fileDialog", transientShellWidgetClass,
                                    title.c_str());
    host->getUiFactory()->getWidgetSet()->setWindowCloseHandler(
        shell, &XawFileDialog::closeRequested, this);

    Widget form = widgets->createPanel(shell, "fileDialogForm", 0, 0,
                                       DIALOG_WIDTH, DIALOG_HEIGHT, true);

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

void XawFileDialog::showFolder(const std::string& newFolder)
{
    folder = FolderListing::normalizeFolder(newFolder);
    widgets->setLabel(folderLabel, folder);

    std::vector<std::string> folders;
    std::vector<std::string> files;
    FolderListing::list(folder, filters, folders, files);

    entries.clear();
    entries.push_back("../");
    for ( size_t i = 0; i < folders.size(); i++ ) {
        entries.push_back(folders[i] + "/");
    }
    entries.insert(entries.end(), files.begin(), files.end());
    entryTexts.clear();
    for ( size_t i = 0; i < entries.size(); i++ ) {
        entryTexts.push_back(entries[i].c_str());
    }
    XawListChange(list, entryTexts.data(), static_cast<int>(entryTexts.size()),
                  0, True);
}

void XawFileDialog::close(bool approve)
{
    approved = approve;
    done = true;
}

void XawFileDialog::entrySelected(Widget, XtPointer clientData,
                                  XtPointer callData)
{
    XawFileDialog* self = static_cast<XawFileDialog*>(clientData);
    XawListReturnStruct* selected = static_cast<XawListReturnStruct*>(callData);
    if ( self == nullptr || selected == nullptr || selected->list_index < 0 ||
         selected->list_index >= static_cast<int>(self->entries.size()) ) {
        return;
    }
    std::string name = self->entries[selected->list_index];
    if ( !name.empty() && name[name.size() - 1] == '/' ) {
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
    static_cast<XawFileDialog*>(clientData)->close(true);
}

void XawFileDialog::cancel(Widget, void* clientData)
{
    static_cast<XawFileDialog*>(clientData)->close(false);
}

void XawFileDialog::pathActivated(Widget, void* clientData)
{
    XawFileDialog* self = static_cast<XawFileDialog*>(clientData);
    std::string path = self->widgets->getText(self->pathField);
    std::string full = !path.empty() && path[0] == '/' ? path :
        self->folder + "/" + path;
    // A folder typed in the field is opened, as JFileChooser does
    if ( !path.empty() && FolderListing::isFolder(full) ) {
        self->showFolder(full);
        self->widgets->setText(self->pathField, "");
        return;
    }
    self->close(true);
}

void XawFileDialog::closeRequested(void* clientData)
{
    static_cast<XawFileDialog*>(clientData)->close(false);
}
