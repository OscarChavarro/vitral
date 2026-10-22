#ifndef __XT_UI_FACTORY__
#define __XT_UI_FACTORY__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"

#include "io/FileSuffixFilter.h"

class XtApplicationHost;
class XtWidgetSet;

/**
What the Xt GUI of the editor needs from the widget set it is built with
(Athena or Motif) beyond the `XtWidgetSet` of the toolkit: the look of the
GUI and its file dialog.

The widget set is chosen when building (CMake option
`VITRAL_XT_WIDGET_SET`), which compiles either `XawUiFactory` or
`XmUiFactory`: each one defines `createUiFactory`.
*/
class XtUiFactory {
public:
    virtual ~XtUiFactory() {}

    /**
    @return the widget set of the GUI
    */
    virtual XtWidgetSet* getWidgetSet() = 0;

    /**
    @return the X resources file that gives the GUI its look, merged over
    the user's resources, or null if the widget set keeps its own look
    */
    virtual const char* getResourceFile() = 0;

    /**
    Shows a modal dialog to choose a file, as `JFileChooser.showOpenDialog`
    does: it runs the Xt events until the dialog is closed.
    @param host application showing the dialog
    @param title title of the dialog (i.e. "Open", "Save")
    @param folder folder shown first
    @param filters accepted kinds of files, or none for all of them
    @param outPath the chosen file, when approved
    @return true if the user approved a file (`APPROVE_OPTION`)
    */
    virtual bool showFileDialog(XtApplicationHost* host,
                                const String& title,
                                const String& folder,
                                const ArrayList<FileSuffixFilter>& filters,
                                String& outPath) = 0;
};

/**
@return the factory of the widget set the application is built with
(delete it after the GUI)
*/
XtUiFactory* createUiFactory();

#endif
