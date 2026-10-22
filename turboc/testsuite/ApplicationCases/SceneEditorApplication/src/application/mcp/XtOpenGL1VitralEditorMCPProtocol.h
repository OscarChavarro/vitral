#ifndef __XT_OPENGL1_VITRAL_EDITOR_MCP_PROTOCOL__
#define __XT_OPENGL1_VITRAL_EDITOR_MCP_PROTOCOL__

#include "java/lang/Runnable.h"
#include "java/lang/String.h"
#include "application/mcp/MCPSceneTools.h"
#include "application/mcp/MCPViewportTools.h"
#include "application/mcp/XtOpenGL1MCPApplicationTools.h"
#include "java/lang/IllegalArgumentException.h"

class Socket;
class XtOpenGL1SceneEditorApplication;

/**
Serves one connection of the automation service of the editor: reads one
JSON-RPC request per line (`initialize`, `tools/list`, `tools/call`) and
answers it in one line. Each tool is executed in the thread of the GUI,
delegated to the tool group that owns it:
- `MCPSceneTools`: the scene and the GUI commands over the model,
- `MCPViewportTools`: rendering configuration and interaction mode,
- `XtOpenGL1MCPApplicationTools`: the running application (input events,
  images, language, exit).

C++ port note: failures inside the tools are C++ exceptions (i.e.
`IllegalArgumentException` where Java throws `IllegalArgumentException`), with
the same messages as the Java ones.
*/
class XtOpenGL1VitralEditorMCPProtocol : public Runnable {
    friend class ToolCall;

private:
    Socket* socket;
    MCPSceneTools sceneTools;
    MCPViewportTools viewportTools;
    XtOpenGL1MCPApplicationTools applicationTools;

    String handle(const String& request);
    String callTool(const String& tool,
                          const String& request);
    String executeTool(const String& tool,
                             const String& request);
    static String toolsJson();

public:
    /**
    @param parent application to automate (referenced)
    @param socket connection to serve (referenced)
    */
    XtOpenGL1VitralEditorMCPProtocol(XtOpenGL1SceneEditorApplication* parent,
                                     Socket* socket);

    virtual void run();
};

#endif
