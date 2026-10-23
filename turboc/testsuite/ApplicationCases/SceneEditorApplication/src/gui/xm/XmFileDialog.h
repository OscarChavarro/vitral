#ifndef __XM_FILE_DIALOG__
#define __XM_FILE_DIALOG__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.h"

// Xt names its strings and objects as the vitral String and Object
// classes: renamed while its headers are read (see XtIntrinsics.h)
#define String XtString
#define Object XtObject
#include <X11/Intrinsic.h>
#undef Object
#undef String

#include "io/FileSuffixFilter.h"

class XtApplicationHost;

/**
Modal dialog to choose a file with the file selection box of Motif, as
Swing `JFileChooser` does: its file list shows the files accepted by the
filters (see `FolderListing`: Motif patterns take only one suffix), its
folder list browses the folders and its selection field can be typed.

`showDialog` blocks, as `JFileChooser.showOpenDialog`, running the Xt events
(of every window) until the dialog is closed; only the dialog accepts user
input meanwhile.
*/
class XmFileDialog {
public:
    /**
    @param host application showing the dialog
    @param title title of the dialog, also the text of its OK button
    @param folder folder shown first
    @param filters accepted kinds of files, or none for all of them
    @param outPath the chosen file, when approved
    @return true if the user approved a file (`APPROVE_OPTION`)
    */
    static BOOLEAN showDialog(XtApplicationHost* host, const String& title,
                           const String& folder,
                           const ArrayList<FileSuffixFilter>& filters,
                           String& outPath);

private:
    ArrayList<FileSuffixFilter> filters;
    Widget shell;
    Widget box;
    BOOLEAN done;
    BOOLEAN approved;
    String path;

    explicit XmFileDialog(const ArrayList<FileSuffixFilter>& filters);
    ~XmFileDialog();
    void create(XtApplicationHost* host, const String& title,
                const String& folder);

    static void searchFiles(Widget box, XtPointer callData);
    static void approve(Widget, XtPointer clientData, XtPointer callData);
    static void cancel(Widget, XtPointer clientData, XtPointer);
    static void closeRequested(void* clientData);
};

#endif
