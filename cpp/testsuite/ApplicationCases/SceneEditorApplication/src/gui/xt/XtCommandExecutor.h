#ifndef __XT_COMMAND_EXECUTOR__
#define __XT_COMMAND_EXECUTOR__

#include <string>

/**
Executes the commands of the menus and buttons of the Xt GUI, identified by
their `IDC_*` names, as `AwtCommandExecutor` does for the Swing one.
*/
class XtCommandExecutor {
public:
    virtual ~XtCommandExecutor() {}

    /**
    @param label identifier of the command
    @return false if the command failed
    */
    virtual bool executeCommand(const std::string& label) = 0;
};

#endif
