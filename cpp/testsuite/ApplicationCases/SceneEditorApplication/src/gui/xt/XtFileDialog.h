#ifndef __XT_FILE_DIALOG__
#define __XT_FILE_DIALOG__

#include <string>
#include <vector>

#include <X11/Intrinsic.h>

#include "io/FileSuffixFilter.h"

class XtApplicationHost;

/**
Modal dialog to choose a file, as Swing `JFileChooser` does (Xaw has no
file chooser): the files of a folder accepted by the filters and its
subfolders, which can be browsed, and a field with the path of the chosen
file, which can also be typed.

`showDialog` blocks, as `JFileChooser.showOpenDialog`, running the Xt events
(of every window) until the dialog is closed; only the dialog accepts user
input meanwhile.
*/
class XtFileDialog {
public:
    /**
    @param host application showing the dialog
    @param title title of the dialog
    @param folder folder shown first
    @param filters accepted kinds of files, or none for all of them
    @param outPath the chosen file, when approved
    @return true if the user approved a file (`APPROVE_OPTION`)
    */
    static bool showDialog(XtApplicationHost* host, const std::string& title,
                           const std::string& folder,
                           const std::vector<FileSuffixFilter>& filters,
                           std::string& outPath);

private:
    XtApplicationHost* host;
    std::vector<FileSuffixFilter> filters;
    std::string folder;
    std::vector<std::string> entries;
    std::vector<const char*> entryTexts;
    Widget shell;
    Widget folderLabel;
    Widget list;
    Widget pathField;
    bool done;
    bool approved;
    Atom wmDeleteWindow;

    XtFileDialog(XtApplicationHost* host,
                 const std::vector<FileSuffixFilter>& filters);
    ~XtFileDialog();
    void create(const std::string& title);
    void showFolder(const std::string& newFolder);
    bool accepts(const std::string& path, bool isFolder) const;
    void close(bool approve);

    static void entrySelected(Widget, XtPointer clientData, XtPointer callData);
    static void approve(Widget, XtPointer clientData, XtPointer);
    static void cancel(Widget, XtPointer clientData, XtPointer);
    static void pathActivated(Widget field, void* clientData);
    static void shellEvent(Widget, XtPointer clientData, XEvent* event,
                           Boolean*);
};

#endif
