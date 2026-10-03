#ifndef __XT_OPENGL4_VITRAL_EDITOR_MCP_PROTOCOL__
#define __XT_OPENGL4_VITRAL_EDITOR_MCP_PROTOCOL__

#include "java/lang/Runnable.h"
#include "java/lang/String.h"
#include "application/mcp/MCPSceneTools.h"
#include "application/mcp/MCPViewportTools.h"
#include "application/mcp/XtOpenGL4MCPApplicationTools.h"

namespace java {
namespace net {
class Socket;
}
}
class XtOpenGL4SceneEditorApplication;

/**
Serves one connection of the automation service of the editor: reads one
JSON-RPC request per line (`initialize`, `tools/list`, `tools/call`) and
answers it in one line. Each tool is executed in the thread of the GUI,
delegated to the tool group that owns it:
- `MCPSceneTools`: the scene and the GUI commands over the model,
- `MCPViewportTools`: rendering configuration and interaction mode,
- `XtOpenGL4MCPApplicationTools`: the running application (input events,
  images, language, exit).

C++ port note: failures inside the tools are C++ exceptions (i.e.
`std::invalid_argument` where Java throws `IllegalArgumentException`), with
the same messages as the Java ones.
*/
class XtOpenGL4VitralEditorMCPProtocol : public java::Runnable {
private:
    java::net::Socket* socket;
    MCPSceneTools sceneTools;
    MCPViewportTools viewportTools;
    XtOpenGL4MCPApplicationTools applicationTools;

    java::String handle(const java::String& request);
    java::String callTool(const java::String& tool,
                          const java::String& request);
    java::String executeTool(const java::String& tool,
                             const java::String& request);
    static java::String toolsJson();

public:
    /**
    @param parent application to automate (referenced)
    @param socket connection to serve (referenced)
    */
    XtOpenGL4VitralEditorMCPProtocol(XtOpenGL4SceneEditorApplication* parent,
                                     java::net::Socket* socket);

    virtual void run() override;
};

#endif
