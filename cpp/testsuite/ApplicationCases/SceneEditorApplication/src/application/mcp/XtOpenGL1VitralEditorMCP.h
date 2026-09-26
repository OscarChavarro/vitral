#ifndef __XT_OPENGL1_VITRAL_EDITOR_MCP__
#define __XT_OPENGL1_VITRAL_EDITOR_MCP__

#include "java/lang/Runnable.h"
#include "java/lang/Thread.h"

class XtOpenGL1SceneEditorApplication;

/**
Automation service of the editor (MCP-like JSON-RPC over TCP, see `MCP.md`
of the Java application): listens on a local TCP port and serves each
connection in its own thread with an `XtOpenGL1VitralEditorMCPProtocol`.
*/
class XtOpenGL1VitralEditorMCP : public java::Runnable {
private:
    XtOpenGL1SceneEditorApplication* parent;
    const int tcpPort;
    java::Thread networkThread;

public:
    /**
    Starts listening in its own thread, which lives as long as the process.
    @param parent application to serve (referenced)
    */
    explicit XtOpenGL1VitralEditorMCP(XtOpenGL1SceneEditorApplication* parent);

    virtual void run() override;
};

#endif
