#ifndef __XT_OPENGL4_VITRAL_EDITOR_MCP__
#define __XT_OPENGL4_VITRAL_EDITOR_MCP__

#include "java/lang/Runnable.h"
#include "java/lang/Thread.h"

class XtOpenGL4SceneEditorApplication;

/**
Automation service of the editor (MCP-like JSON-RPC over TCP, see `MCP.md`
of the Java application): listens on a local TCP port and serves each
connection in its own thread with an `XtOpenGL4VitralEditorMCPProtocol`.
*/
class XtOpenGL4VitralEditorMCP : public java::Runnable {
private:
    XtOpenGL4SceneEditorApplication* parent;
    const int tcpPort;
    java::Thread networkThread;

public:
    /**
    Starts listening in its own thread, which lives as long as the process.
    @param parent application to serve (referenced)
    */
    explicit XtOpenGL4VitralEditorMCP(XtOpenGL4SceneEditorApplication* parent);

    virtual void run() override;
};

#endif
