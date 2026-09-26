#include <algorithm>
#include <climits>
#include <cstdlib>
#include <dirent.h>
#include <sys/stat.h>

#include <X11/StringDefs.h>
#include <X11/Shell.h>
#include <X11/Composite.h>
#include <X11/Xaw/Command.h>
#include <X11/Xaw/List.h>
#include <X11/Xaw/Viewport.h>

#include "gui/xt/XtApplicationHost.h"
#include "gui/xt/XtFileDialog.h"
#include "gui/xt/XtPanelWidgets.h"

namespace {

const int DIALOG_WIDTH = 520;
const int DIALOG_HEIGHT = 420;

bool isFolder(const std::string& path)
{
    struct stat status;
    return stat(path.c_str(), &status) == 0 && S_ISDIR(status.st_mode);
}

/**
@return the folder without "." and ".." components, as `File.getCanonicalPath`
*/
std::string normalizeFolder(const std::string& folder)
{
    char resolved[PATH_MAX];
    if ( realpath(folder.c_str(), resolved) != nullptr ) {
        return resolved;
    }
    return folder;
}

}

XtFileDialog::XtFileDialog(XtApplicationHost* host,
                           const std::vector<FileSuffixFilter>& filters)
    : host(host), filters(filters), shell(nullptr), folderLabel(nullptr),
      list(nullptr), pathField(nullptr), done(false), approved(false),
      wmDeleteWindow(None)
{
}

XtFileDialog::~XtFileDialog()
{
    if ( shell != nullptr ) {
        XtDestroyWidget(shell);
    }
}

bool XtFileDialog::showDialog(XtApplicationHost* host, const std::string& title,
                              const std::string& folder,
                              const std::vector<FileSuffixFilter>& filters,
                              std::string& outPath)
{
    XtFileDialog dialog(host, filters);
    dialog.create(title);
    dialog.showFolder(isFolder(folder) ? folder : std::string("."));

    XtPopup(dialog.shell, XtGrabExclusive);
    dialog.wmDeleteWindow = XInternAtom(XtDisplay(dialog.shell),
                                        "WM_DELETE_WINDOW", False);
    XSetWMProtocols(XtDisplay(dialog.shell), XtWindow(dialog.shell),
                    &dialog.wmDeleteWindow, 1);
    XtSetKeyboardFocus(dialog.shell, dialog.pathField);

    XtAppContext appContext = XtWidgetToApplicationContext(dialog.shell);
    while ( !dialog.done ) {
        XtAppProcessEvent(appContext, XtIMAll);
    }
    XtPopdown(dialog.shell);

    if ( dialog.approved ) {
        std::string path = XtPanelWidgets::getText(dialog.pathField);
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

void XtFileDialog::create(const std::string& title)
{
    XFontSet fontSet = host->getPanelFontSet();
    shell = host->createDialogShell("fileDialog", transientShellWidgetClass,
                                    title.c_str());
    XtAddEventHandler(shell, NoEventMask, True, &XtFileDialog::shellEvent,
                      this);

    Arg args[4]; Cardinal n = 0;
    XtSetArg(args[n], XtNwidth, DIALOG_WIDTH); ++n;
    XtSetArg(args[n], XtNheight, DIALOG_HEIGHT); ++n;
    Widget form = XtCreateManagedWidget("fileDialogForm", compositeWidgetClass,
                                        shell, args, n);

    folderLabel = XtPanelWidgets::createLabel(form, "", fontSet,
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
    XtAddCallback(list, XtNcallback, &XtFileDialog::entrySelected, this);

    pathField = XtPanelWidgets::createTextField(form, "", fontSet, 8,
        DIALOG_HEIGHT - 76, DIALOG_WIDTH - 16, 26,
        &XtFileDialog::pathActivated, this);

    const char* labels[] = { "Open", "Cancel" };
    XtCallbackProc callbacks[] = { &XtFileDialog::approve,
                                   &XtFileDialog::cancel };
    for ( int i = 0; i < 2; i++ ) {
        Arg buttonArgs[7]; Cardinal buttonN = 0;
        XtSetArg(buttonArgs[buttonN], XtNlabel, labels[i]); ++buttonN;
        XtSetArg(buttonArgs[buttonN], XtNinternational, True); ++buttonN;
        XtSetArg(buttonArgs[buttonN], XtNfontSet, fontSet); ++buttonN;
        XtSetArg(buttonArgs[buttonN], XtNx, DIALOG_WIDTH - 8 - (2 - i) * 108); ++buttonN;
        XtSetArg(buttonArgs[buttonN], XtNy, DIALOG_HEIGHT - 40); ++buttonN;
        XtSetArg(buttonArgs[buttonN], XtNwidth, 100); ++buttonN;
        XtSetArg(buttonArgs[buttonN], XtNheight, 28); ++buttonN;
        Widget button = XtCreateManagedWidget("fileDialogButton",
            commandWidgetClass, form, buttonArgs, buttonN);
        XtAddCallback(button, XtNcallback, callbacks[i], this);
    }
}

bool XtFileDialog::accepts(const std::string& path, bool folderEntry) const
{
    if ( filters.empty() || folderEntry ) {
        return true;
    }
    java::File file(path.c_str());
    for ( size_t i = 0; i < filters.size(); i++ ) {
        if ( filters[i].accept(file) ) {
            return true;
        }
    }
    return false;
}

void XtFileDialog::showFolder(const std::string& newFolder)
{
    folder = normalizeFolder(newFolder);
    XtPanelWidgets::setLabel(folderLabel, folder);

    std::vector<std::string> folders;
    std::vector<std::string> files;
    DIR* directory = opendir(folder.c_str());
    if ( directory != nullptr ) {
        struct dirent* entry;
        while ( (entry = readdir(directory)) != nullptr ) {
            std::string name = entry->d_name;
            if ( name == "." || name == ".." || name[0] == '.' ) {
                continue;
            }
            std::string path = folder + "/" + name;
            bool folderEntry = isFolder(path);
            if ( !accepts(path, folderEntry) ) {
                continue;
            }
            (folderEntry ? folders : files).push_back(
                folderEntry ? name + "/" : name);
        }
        closedir(directory);
    }
    std::sort(folders.begin(), folders.end());
    std::sort(files.begin(), files.end());

    entries.clear();
    entries.push_back("../");
    entries.insert(entries.end(), folders.begin(), folders.end());
    entries.insert(entries.end(), files.begin(), files.end());
    entryTexts.clear();
    for ( size_t i = 0; i < entries.size(); i++ ) {
        entryTexts.push_back(entries[i].c_str());
    }
    XawListChange(list, entryTexts.data(), static_cast<int>(entryTexts.size()),
                  0, True);
}

void XtFileDialog::close(bool approve)
{
    approved = approve;
    done = true;
}

void XtFileDialog::entrySelected(Widget, XtPointer clientData,
                                 XtPointer callData)
{
    XtFileDialog* self = static_cast<XtFileDialog*>(clientData);
    XawListReturnStruct* selected = static_cast<XawListReturnStruct*>(callData);
    if ( self == nullptr || selected == nullptr || selected->list_index < 0 ||
         selected->list_index >= static_cast<int>(self->entries.size()) ) {
        return;
    }
    std::string name = self->entries[selected->list_index];
    if ( !name.empty() && name[name.size() - 1] == '/' ) {
        // Changing the list inside its own callback is allowed by Xaw
        self->showFolder(self->folder + "/" + name.substr(0, name.size() - 1));
        XtPanelWidgets::setText(self->pathField, "");
    }
    else {
        XtPanelWidgets::setText(self->pathField, self->folder + "/" + name);
    }
}

void XtFileDialog::approve(Widget, XtPointer clientData, XtPointer)
{
    static_cast<XtFileDialog*>(clientData)->close(true);
}

void XtFileDialog::cancel(Widget, XtPointer clientData, XtPointer)
{
    static_cast<XtFileDialog*>(clientData)->close(false);
}

void XtFileDialog::pathActivated(Widget, void* clientData)
{
    XtFileDialog* self = static_cast<XtFileDialog*>(clientData);
    std::string path = XtPanelWidgets::getText(self->pathField);
    std::string full = !path.empty() && path[0] == '/' ? path :
        self->folder + "/" + path;
    // A folder typed in the field is opened, as JFileChooser does
    if ( !path.empty() && isFolder(full) ) {
        self->showFolder(full);
        XtPanelWidgets::setText(self->pathField, "");
        return;
    }
    self->close(true);
}

void XtFileDialog::shellEvent(Widget, XtPointer clientData, XEvent* event,
                              Boolean*)
{
    XtFileDialog* self = static_cast<XtFileDialog*>(clientData);
    if ( event->type == ClientMessage &&
         static_cast<Atom>(event->xclient.data.l[0]) == self->wmDeleteWindow ) {
        self->close(false);
    }
}
