#ifndef __XT_OPENGL4_GUI_EVENT_EXECUTOR__
#define __XT_OPENGL4_GUI_EVENT_EXECUTOR__

#include <string>

#include "vsdk/toolkit/gui/CommandListener.h"

class XtApplicationHost;

/**
Executes the commands of the GUI of the editor that need Xt (file dialogs,
the image window and language changes, that rebuild the Xt GUI) and
repaints the drawing area after each command, as `AwtJogl4GuiEventExecutor`
does for Swing (both are the `CommandListener` of the menus and buttons
built from the GUI definition). The rest of the commands are executed by the technology
independent `GuiEventExecutor`.
*/
class XtOpenGL4GuiEventExecutor : public CommandListener {
private:
    XtApplicationHost* parent;

    bool executeXtCommand(const std::string& label);

public:
    explicit XtOpenGL4GuiEventExecutor(XtApplicationHost* parent);

    /**
    @param label identifier of the command (`IDC_*`)
    @return false if the command failed
    */
    virtual bool executeCommand(const java::String& label) override;
};

#endif
