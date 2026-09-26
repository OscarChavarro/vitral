#ifndef __XT_OPENGL4_GUI_EVENT_EXECUTOR__
#define __XT_OPENGL4_GUI_EVENT_EXECUTOR__

#include <string>

#include "gui/xt/XtCommandExecutor.h"

class XtApplicationHost;

/**
Executes the commands of the GUI of the editor that need Xt (file dialogs,
the image window and language changes, that rebuild the Xt GUI) and
repaints the drawing area after each command, as `AwtJogl4GuiEventExecutor`
does for Swing. The rest of the commands are executed by the technology
independent `GuiEventExecutor`.
*/
class XtOpenGL4GuiEventExecutor : public XtCommandExecutor {
private:
    XtApplicationHost* parent;

    bool executeXtCommand(const std::string& label);

public:
    explicit XtOpenGL4GuiEventExecutor(XtApplicationHost* parent);

    virtual bool executeCommand(const std::string& label) override;
};

#endif
