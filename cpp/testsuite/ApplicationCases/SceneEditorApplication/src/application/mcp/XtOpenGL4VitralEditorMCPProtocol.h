#ifndef __XT_OPENGL4_VITRAL_EDITOR_MCP_PROTOCOL__
#define __XT_OPENGL4_VITRAL_EDITOR_MCP_PROTOCOL__

#include <functional>

#include "java/lang/Runnable.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.h"

namespace java {
namespace net {
class Socket;
}
}
class UndoQueue;
class Vector3Dd;
class ColorRgb;
class Viewport;
class XtOpenGL4ApplicationController;
class XtOpenGL4SceneEditorApplication;

/**
One connection of the automation service: reads JSON-RPC requests, one per
line, executes the tools in the thread of the GUI and writes one JSON line
per response (see `MCP.md` of the Java application).

C++ port note: failures inside the tools are C++ exceptions (i.e.
`std::invalid_argument` where Java throws `IllegalArgumentException`), with
the same messages as the Java ones.
*/
class XtOpenGL4VitralEditorMCPProtocol : public java::Runnable {
private:
    XtOpenGL4SceneEditorApplication* parent;
    java::net::Socket* socket;

    java::String handle(const java::String& request);
    java::String callTool(const java::String& tool,
                          const java::String& request);
    java::String executeTool(const java::String& tool,
                             const java::String& request);
    java::String listLanguages();
    java::String setLanguage(const java::String& request);
    java::String exitApplication();
    void recordSceneChange(const java::String& tool,
                           const std::function<void()>& change);
    java::String describeEditHistory();
    static java::String queueJson(UndoQueue* queue);
    java::String executeGuiCommand(const java::String& request);
    void clearScene();
    void addPointLight(const java::String& request);
    static bool hasProperty(const java::String& json,
                            const java::String& key);
    void addSphere(const java::String& request);
    void addCone(const java::String& request);
    void addCylinder(const java::String& request);
    void moveBody(const java::String& request);
    java::String setInteractionMode(const java::String& request);
    XtOpenGL4ApplicationController* getDrawingAreaController();
    java::String injectMouse(const java::String& request);
    java::String injectKey(const java::String& request);
    java::String projectSelectedBody(const java::String& request);
    void selectBody(const java::String& request);
    java::ArrayList<Viewport*> selectedViewports(const java::String& request);
    void setRendererConfiguration(const java::String& request);
    java::String describeRendererConfigurations(const java::String& request);
    java::String raytracePng(const java::String& request);
    java::String viewportJpg(const java::String& request);
    java::String workspaceJpg(const java::String& request);
    java::String describeScene();
    static java::String toolsJson();
    static java::String tool(const char* name, const char* description);
    static java::String content(const java::String& json);
    static java::String result(const java::String& id,
                               const java::String& json);
    static java::String error(const java::String& id, int code,
                              const java::String& message);
    static java::String vector(const Vector3Dd& v);
    static java::String color(const ColorRgb& c);
    static java::String stringProperty(const java::String& json,
                                       const java::String& key,
                                       const java::String& defaultValue);
    static java::String nestedStringProperty(const java::String& json,
                                             const java::String& key,
                                             const java::String& defaultValue);
    static java::String idProperty(const java::String& json);
    static double numberProperty(const java::String& json,
                                 const java::String& key,
                                 double defaultValue);

    /**
    @param outValue the value, if present
    @return false if the property is missing (Java returns null)
    */
    static bool booleanProperty(const java::String& json,
                                const java::String& key, bool& outValue);
    static java::String escape(const java::String& in);

public:
    /**
    @param parent application to serve (referenced)
    @param socket connection to serve (referenced)
    */
    XtOpenGL4VitralEditorMCPProtocol(XtOpenGL4SceneEditorApplication* parent,
                                     java::net::Socket* socket);

    virtual void run() override;
};

#endif
