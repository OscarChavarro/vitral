#include <cmath>
#include <cstdio>
#include <exception>
#include <stdexcept>

#include "java/io/BufferedReader.h"
#include "java/io/File.h"
#include "java/io/PrintWriter.h"
#include "java/lang/Double.h"
#include "java/lang/StringBuilder.h"
#include "java/net/Socket.h"
#include "java/util/ArrayList.txx"
#include "java/util/regex/Matcher.h"
#include "java/util/regex/Pattern.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/volume/Cone.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/light/PointLight.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleBodyGroup.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "vsdk/toolkit/gui/viewport/ViewportSet.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "application/GuiEventExecutor.h"
#include "application/XtOpenGL4ApplicationController.h"
#include "application/XtOpenGL4SceneEditorApplication.h"
#include "application/mcp/XtOpenGL4VitralEditorMCPProtocol.h"
#include "gui/xt/XtEventQueue.h"
#include "model/ApplicationModel.h"
#include "model/DrawingArea.h"
#include "model/InteractionMode.h"
#include "model/Scene.h"
#include "model/SceneLightFactory.h"
#include "model/history/EditHistory.h"
#include "model/history/SceneHistory.h"
#include "model/history/UndoQueue.h"
#include "model/history/ViewportHistory.h"
#include "model/selection/SelectionSet.h"

using java::util::regex::Matcher;
using java::util::regex::Pattern;

namespace {

/**
Collects the status message of a GUI command.
*/
class MessageCollector : public GuiEventExecutor::Presenter {
public:
    java::String message;

    virtual void showStatusMessage(const java::String& text) override
    {
        message = text;
    }
};

/**
A tool executed in the thread of the GUI.
*/
class ToolCall : public java::Runnable {
private:
    std::function<java::String()> tool;

public:
    java::String result;
    bool failed;
    java::String failure;

    explicit ToolCall(const std::function<java::String()>& tool)
        : tool(tool), failed(false) {}

    virtual void run() override
    {
        try {
            result = tool();
        }
        catch ( const std::exception& e ) {
            failed = true;
            failure = e.what();
        }
    }
};

/**
Closes the application once the response of `app.exit` was sent.
*/
class CloseApplication : public java::Runnable {
private:
    XtOpenGL4SceneEditorApplication* parent;

public:
    explicit CloseApplication(XtOpenGL4SceneEditorApplication* parent)
        : parent(parent) {}

    virtual void run() override
    {
        parent->closeApplication();
    }
};

const char* commandResultName(GuiEventExecutor::CommandResult result)
{
    switch ( result ) {
      case GuiEventExecutor::CommandResult::DONE: return "DONE";
      case GuiEventExecutor::CommandResult::FAILED: return "FAILED";
      default: return "NOT_HANDLED";
    }
}

const char* shadingTypeName(ShadingType type)
{
    switch ( type ) {
      case SHADING_NOLIGHT: return "NOLIGHT";
      case SHADING_FLAT: return "FLAT";
      case SHADING_GOURAUD: return "GOURAUD";
      case SHADING_PHONG: return "PHONG";
      default: return "COOK_TERRANCE";
    }
}

/**
As Java `ShadingType.valueOf`.
@throws std::invalid_argument if there is no type with that name
*/
ShadingType shadingTypeValueOf(const java::String& name)
{
    const ShadingType types[] = {
        SHADING_NOLIGHT, SHADING_FLAT, SHADING_GOURAUD, SHADING_PHONG,
        SHADING_COOK_TERRANCE
    };
    for ( int i = 0; i < 5; i++ ) {
        if ( name.equals(shadingTypeName(types[i])) ) {
            return types[i];
        }
    }
    throw std::invalid_argument((java::String(
        "No enum constant vsdk.toolkit.environment.material.ShadingType.") +
        name).c_str());
}

/**
@return the message of the Java `IndexOutOfBoundsException` of lists
*/
std::string indexOutOfBounds(int index, int length)
{
    return "Index " + std::to_string(index) + " out of bounds for length " +
        std::to_string(length);
}

}

XtOpenGL4VitralEditorMCPProtocol::XtOpenGL4VitralEditorMCPProtocol(
    XtOpenGL4SceneEditorApplication* parent, java::net::Socket* socket)
    : parent(parent), socket(socket)
{
}

void XtOpenGL4VitralEditorMCPProtocol::run()
{
    try {
        java::BufferedReader in(socket->getInputStream());
        java::PrintWriter out(socket->getOutputStream(), true);
        java::String line;

        while ( in.readLine(line) ) {
            out.println(handle(line));
            if ( out.checkError() ) {
                break;
            }
        }
    }
    catch ( const std::exception& e ) {
        fprintf(stderr, "Error on XtOpenGL4VitralEditorMCPProtocol!\n%s\n",
                e.what());
    }
}

java::String XtOpenGL4VitralEditorMCPProtocol::handle(
    const java::String& request)
{
    java::String id = idProperty(request);
    java::String method = stringProperty(request, "method", "");

    try {
        if ( method.equals("initialize") ) {
            return result(id,
                "{\"protocolVersion\":\"2024-11-05\",\"serverInfo\":{\"name\":\"VitralEditorMCP\",\"version\":\"0.1\"},\"capabilities\":{\"tools\":{}}}");
        }
        if ( method.equals("tools/list") ) {
            return result(id, toolsJson());
        }
        if ( method.equals("tools/call") ) {
            java::String tool = nestedStringProperty(request, "name", "");
            return result(id, callTool(tool, request));
        }
        return error(id, -32601, java::String("Unknown method: ") + method);
    }
    catch ( const std::exception& e ) {
        return error(id, -32000, e.what());
    }
}

java::String XtOpenGL4VitralEditorMCPProtocol::callTool(
    const java::String& tool, const java::String& request)
{
    ToolCall command([this, tool, request]() {
        return executeTool(tool, request);
    });

    XtEventQueue::invokeAndWait(&command);
    if ( command.failed ) {
        return content(java::String("{\"error\":\"") +
            escape(command.failure) + "\"}");
    }
    return content(command.result);
}

java::String XtOpenGL4VitralEditorMCPProtocol::executeTool(
    const java::String& tool, const java::String& request)
{
    if ( tool.equals("scene.describe") ) {
        return describeScene();
    }
    if ( tool.equals("scene.clear") ) {
        recordSceneChange(tool, [this]() { clearScene(); });
        return "{\"ok\":true}";
    }
    if ( tool.equals("scene.add_point_light") ) {
        recordSceneChange(tool, [this, &request]() { addPointLight(request); });
        return describeScene();
    }
    if ( tool.equals("scene.add_sphere") ) {
        recordSceneChange(tool, [this, &request]() { addSphere(request); });
        return describeScene();
    }
    if ( tool.equals("scene.add_cone") ) {
        recordSceneChange(tool, [this, &request]() { addCone(request); });
        return describeScene();
    }
    if ( tool.equals("scene.add_cylinder") ) {
        recordSceneChange(tool, [this, &request]() { addCylinder(request); });
        return describeScene();
    }
    if ( tool.equals("scene.move_body") ) {
        recordSceneChange(tool, [this, &request]() { moveBody(request); });
        return describeScene();
    }
    if ( tool.equals("edit.history") ) {
        return describeEditHistory();
    }
    if ( tool.equals("gui.command") ) {
        return executeGuiCommand(request);
    }
    if ( tool.equals("scene.select_body") ) {
        selectBody(request);
        return describeScene();
    }
    if ( tool.equals("gui.set_mode") ) {
        return setInteractionMode(request);
    }
    if ( tool.equals("gui.mouse") ) {
        return injectMouse(request);
    }
    if ( tool.equals("gui.key") ) {
        return injectKey(request);
    }
    if ( tool.equals("viewport.project") ) {
        return projectSelectedBody(request);
    }
    if ( tool.equals("render.get_configuration") ) {
        return describeRendererConfigurations(request);
    }
    if ( tool.equals("render.set_configuration") ) {
        setRendererConfiguration(request);
        return describeRendererConfigurations(request);
    }
    if ( tool.equals("render.raytrace_png") ) {
        return raytracePng(request);
    }
    if ( tool.equals("viewport.export_jpg") ) {
        return viewportJpg(request);
    }
    if ( tool.equals("workspace.export_jpg") ) {
        return workspaceJpg(request);
    }
    if ( tool.equals("gui.list_languages") ) {
        return listLanguages();
    }
    if ( tool.equals("gui.set_language") ) {
        return setLanguage(request);
    }
    if ( tool.equals("app.exit") ) {
        return exitApplication();
    }
    throw std::invalid_argument(
        (java::String("Unknown tool: ") + tool).c_str());
}

java::String XtOpenGL4VitralEditorMCPProtocol::listLanguages()
{
    java::StringBuilder sb;
    java::String current = parent->getCurrentGuiLanguage();
    java::ArrayList<java::String> languages = parent->getGuiLanguages();
    bool first = true;

    sb.append("{\"current\":\"").append(escape(current)).append("\",\"languages\":[");
    for ( long i = 0; i < languages.size(); i++ ) {
        const java::String& language = languages.get(i);
        if ( !first ) {
            sb.append(',');
        }
        first = false;
        sb.append("{\"id\":\"").append(escape(language)).append('"')
            .append(",\"current\":").append(language.equals(current)).append('}');
    }
    sb.append("]}");
    return sb.toString();
}

java::String XtOpenGL4VitralEditorMCPProtocol::setLanguage(
    const java::String& request)
{
    java::String language = stringProperty(request, "language", "");

    if ( !parent->setGuiLanguageById(language) ) {
        // As Java `List.toString`
        java::ArrayList<java::String> languages = parent->getGuiLanguages();
        java::StringBuilder available;
        available.append('[');
        for ( long i = 0; i < languages.size(); i++ ) {
            if ( i > 0 ) {
                available.append(", ");
            }
            available.append(languages.get(i));
        }
        available.append(']');
        throw std::invalid_argument((java::String("Unknown language \"") +
            language + "\". Available languages: " +
            available.toString()).c_str());
    }
    return java::String("{\"ok\":true,\"language\":\"") + escape(language) +
        "\"}";
}

/**
The exit is deferred a short time, so the response of this call can be
sent to the client before the process ends.
*/
java::String XtOpenGL4VitralEditorMCPProtocol::exitApplication()
{
    XtEventQueue::invokeLater(new CloseApplication(parent), 300);
    return "{\"ok\":true,\"message\":\"The application is closing\"}";
}

/**
Executes a change of the scene requested by the agent, recording it in the
scene history, as the ones of the user, so it can be undone.
*/
void XtOpenGL4VitralEditorMCPProtocol::recordSceneChange(
    const java::String& tool, const std::function<void()>& change)
{
    parent->getApplicationModel()->getEditHistory()->getSceneHistory()
        ->perform(tool, change);
    parent->getOpenGL4Controller()->repaint();
}

/**
@return the state of the scene history and of the view history of each
viewport of the active viewport set
*/
java::String XtOpenGL4VitralEditorMCPProtocol::describeEditHistory()
{
    EditHistory* history = parent->getApplicationModel()->getEditHistory();
    ViewportSet* viewportSet =
        parent->getApplicationModel()->getActiveViewportSet();
    java::StringBuilder sb;
    int i;

    sb.append("{\"scene\":")
        .append(queueJson(history->getSceneHistory()->getQueue()))
        .append(",\"viewports\":[");
    for ( i = 0; i < viewportSet->getViewportCount(); i++ ) {
        Viewport* viewport = viewportSet->getViewport(i);

        if ( i > 0 ) {
            sb.append(',');
        }
        sb.append("{\"index\":").append(i)
            .append(",\"title\":\"").append(escape(viewport->getTitle())).append('"')
            .append(",\"selected\":")
            .append(viewport == viewportSet->getSelectedViewport())
            .append(",\"history\":")
            .append(queueJson(history->getViewportHistory()->getQueue(viewport)))
            .append('}');
    }
    sb.append("]}");
    return sb.toString();
}

java::String XtOpenGL4VitralEditorMCPProtocol::queueJson(UndoQueue* queue)
{
    // The C++ queue names no operation with an empty name (Java: null)
    bool withUndo = queue->getUndoCount() > 0;
    bool withRedo = queue->getRedoCount() > 0;
    java::StringBuilder sb;

    sb.append("{\"undo\":").append(queue->getUndoCount())
        .append(",\"redo\":").append(queue->getRedoCount())
        .append(",\"nextUndo\":");
    if ( withUndo ) {
        sb.append('"').append(escape(queue->getUndoName())).append('"');
    }
    else {
        sb.append("null");
    }
    sb.append(",\"nextRedo\":");
    if ( withRedo ) {
        sb.append('"').append(escape(queue->getRedoName())).append('"');
    }
    else {
        sb.append("null");
    }
    sb.append('}');
    return sb.toString();
}

/**
Executes a command of the GUI that works only over the model (i.e. the
`IDC_CREATE_*` ones), as its menu item or button does. What it changes in
the scene is recorded in the scene history.
*/
java::String XtOpenGL4VitralEditorMCPProtocol::executeGuiCommand(
    const java::String& request)
{
    java::String command = stringProperty(request, "command", "");
    MessageCollector message;
    GuiEventExecutor executor(parent->getApplicationModel(), &message);
    GuiEventExecutor::CommandResult result = executor.execute(command);

    parent->getOpenGL4Controller()->repaint();
    return java::String("{\"command\":\"") + escape(command) +
        "\",\"result\":\"" + commandResultName(result) +
        "\",\"message\":\"" + escape(message.message) + "\"}";
}

void XtOpenGL4VitralEditorMCPProtocol::clearScene()
{
    Scene* scene = parent->getApplicationModel()->getScene();
    // Referenced by the history, that can put them back (see `Scene`)
    scene->scene->getSimpleBodies().clear();
    scene->scene->getLights().clear();
    scene->debugThingGroups.clear();
    scene->selectedThings->sync();
    scene->selectedLights->sync();
    scene->selectedDebugThingGroups->sync();
}

void XtOpenGL4VitralEditorMCPProtocol::addPointLight(
    const java::String& request)
{
    bool explicitPosition = hasProperty(request, "x") ||
        hasProperty(request, "y") || hasProperty(request, "z");
    bool explicitColor = hasProperty(request, "r") ||
        hasProperty(request, "g") || hasProperty(request, "b");

    if ( !explicitPosition && !explicitColor ) {
        parent->getApplicationModel()->addNewLight();
        return;
    }
    // Missing values follow the default policy of the first light
    java::ArrayList<Light*> noLights;
    PointLight* defaults = SceneLightFactory().createLight(
        noLights, parent->getApplicationModel()->getActiveViewportSet());
    Vector3Dd p = defaults == nullptr ? Vector3Dd() : defaults->getPosition();
    delete defaults;
    double x = numberProperty(request, "x", p.x());
    double y = numberProperty(request, "y", p.y());
    double z = numberProperty(request, "z", p.z());
    double r = numberProperty(request, "r", 1.0);
    double g = numberProperty(request, "g", 1.0);
    double b = numberProperty(request, "b", 1.0);
    Scene* scene = parent->getApplicationModel()->getScene();
    scene->scene->addLight(new PointLight(Vector3Dd(x, y, z),
        ColorRgb(r, g, b)));
}

bool XtOpenGL4VitralEditorMCPProtocol::hasProperty(const java::String& json,
                                                   const java::String& key)
{
    return Pattern::compile(java::String("\"") + Pattern::quote(key) +
        "\"\\s*:").matcher(json).find();
}

void XtOpenGL4VitralEditorMCPProtocol::addSphere(const java::String& request)
{
    double radius = numberProperty(request, "radius", 1.0);
    double x = numberProperty(request, "x", 0.0);
    double y = numberProperty(request, "y", 0.0);
    double z = numberProperty(request, "z", 0.0);
    SimpleBody* body = parent->getApplicationModel()->getScene()
        ->addThing(new Sphere(radius));
    body->setPosition(Vector3Dd(x, y, z));
}

namespace {
void placeNewBody(ApplicationModel* model, Geometry* geometry,
                  double x, double y, double z)
{
    SimpleBody* body = model->getScene()->addThing(geometry);
    body->setPosition(Vector3Dd(x, y, z));
}
}

void XtOpenGL4VitralEditorMCPProtocol::addCone(const java::String& request)
{
    double baseRadius = numberProperty(request, "baseRadius", 1.0);
    double topRadius = numberProperty(request, "topRadius", 0.0);
    double height = numberProperty(request, "height", 2.0);
    placeNewBody(parent->getApplicationModel(),
        new Cone(baseRadius, topRadius, height),
        numberProperty(request, "x", 0.0),
        numberProperty(request, "y", 0.0),
        numberProperty(request, "z", 0.0));
}

void XtOpenGL4VitralEditorMCPProtocol::addCylinder(const java::String& request)
{
    double radius = numberProperty(request, "radius", 1.0);
    double height = numberProperty(request, "height", 2.0);
    placeNewBody(parent->getApplicationModel(),
        new Cone(radius, radius, height),
        numberProperty(request, "x", 0.0),
        numberProperty(request, "y", 0.0),
        numberProperty(request, "z", 0.0));
}

void XtOpenGL4VitralEditorMCPProtocol::moveBody(const java::String& request)
{
    java::ArrayList<SimpleBody*>& bodies =
        parent->getApplicationModel()->getScene()->scene->getSimpleBodies();
    int index = (int)numberProperty(request, "index",
                                    (double)(bodies.size() - 1));

    if ( index < 0 || index >= bodies.size() ) {
        throw std::invalid_argument(
            "Body index out of range: " + std::to_string(index));
    }
    SimpleBody* body = bodies.get(index);
    Vector3Dd p = body->getPosition();

    body->setPosition(Vector3Dd(
        numberProperty(request, "x", p.x()),
        numberProperty(request, "y", p.y()),
        numberProperty(request, "z", p.z())));
}

java::String XtOpenGL4VitralEditorMCPProtocol::setInteractionMode(
    const java::String& request)
{
    java::String mode = stringProperty(request, "mode", "");
    InteractionMode value;

    if ( mode.equals("camera") ) value = InteractionMode::CAMERA;
    else if ( mode.equals("select") ) value = InteractionMode::SELECT;
    else if ( mode.equals("translate") ) value = InteractionMode::TRANSLATE;
    else if ( mode.equals("rotate") ) value = InteractionMode::ROTATE;
    else if ( mode.equals("scale") ) value = InteractionMode::SCALE;
    else {
        throw std::invalid_argument((java::String("Unknown mode \"") + mode +
            "\". Use camera, select, translate, rotate or scale").c_str());
    }
    parent->getApplicationModel()->getDrawingArea()->setInteractionMode(value);
    return java::String("{\"ok\":true,\"mode\":\"") + mode + "\"}";
}

/**
@return the controller of the drawing area, once its canvas was created
*/
XtOpenGL4ApplicationController*
XtOpenGL4VitralEditorMCPProtocol::getDrawingAreaController()
{
    XtOpenGL4ApplicationController* controller =
        parent->getOpenGL4Controller();

    if ( !controller->isDrawingAreaCreated() ) {
        throw std::logic_error("The drawing area has not been created");
    }
    return controller;
}

java::String XtOpenGL4VitralEditorMCPProtocol::injectMouse(
    const java::String& request)
{
    XtOpenGL4ApplicationController* drawingArea = getDrawingAreaController();
    java::String type = stringProperty(request, "type", "move");
    int x = (int)std::round(numberProperty(request, "x", 0));
    int y = (int)std::round(numberProperty(request, "y", 0));
    int button = (int)numberProperty(request, "button", 1);

    drawingArea->injectMouseEvent(type, x, y, button);
    return describeScene();
}

java::String XtOpenGL4VitralEditorMCPProtocol::injectKey(
    const java::String& request)
{
    XtOpenGL4ApplicationController* drawingArea = getDrawingAreaController();
    java::String key = stringProperty(request, "key", "");
    bool shift = false;
    bool ctrl = false;

    booleanProperty(request, "shift", shift);
    booleanProperty(request, "ctrl", ctrl);
    drawingArea->injectKeyEvent(key, shift, ctrl);
    return describeScene();
}

/**
Reports the canvas pixel of the first selected body and of the tips of its
three axes (one unit long), as seen by a viewport.
*/
java::String XtOpenGL4VitralEditorMCPProtocol::projectSelectedBody(
    const java::String& request)
{
    XtOpenGL4ApplicationController* drawingArea = getDrawingAreaController();
    Scene* scene = parent->getApplicationModel()->getScene();
    ViewportSet* set = parent->getApplicationModel()->getActiveViewportSet();
    int viewportIndex = (int)numberProperty(request, "viewport", 0);
    int selected = scene->selectedThings->firstSelected();

    if ( selected < 0 ) {
        throw std::logic_error("No body is selected");
    }
    if ( viewportIndex < 0 || viewportIndex >= set->getViewportCount() ) {
        throw std::out_of_range(indexOutOfBounds(viewportIndex,
                                                 set->getViewportCount()));
    }
    Viewport* viewport = set->getViewport(viewportIndex);
    Vector3Dd p = scene->scene->getSimpleBodies().get(selected)->getPosition();
    const char* names[] = {"origin", "x", "y", "z"};
    Vector3Dd points[] = {
        p,
        p.add(Vector3Dd(1, 0, 0)),
        p.add(Vector3Dd(0, 1, 0)),
        p.add(Vector3Dd(0, 0, 1))
    };
    java::StringBuilder sb;

    sb.append("{\"viewport\":\"").append(escape(viewport->getTitle())).append('"');
    for ( int i = 0; i < 4; i++ ) {
        double pixel[2];

        sb.append(",\"").append(names[i]).append("\":");
        if ( drawingArea->projectToCanvas(viewport, points[i], pixel) ) {
            sb.append('[').append(pixel[0]).append(',').append(pixel[1])
                .append(']');
        }
        else {
            sb.append("null");
        }
    }
    sb.append('}');
    return sb.toString();
}

void XtOpenGL4VitralEditorMCPProtocol::selectBody(const java::String& request)
{
    Scene* scene = parent->getApplicationModel()->getScene();
    int index = (int)numberProperty(request, "index", -1);

    scene->selectedThings->unselectAll();
    if ( index >= 0 ) {
        if ( index >= scene->scene->getSimpleBodies().size() ) {
            throw std::invalid_argument(
                "Body index out of range: " + std::to_string(index));
        }
        scene->selectedThings->select(index);
    }
}

/**
@return the viewports selected by the "viewport" argument: an index, or
all of them when it is missing
*/
java::ArrayList<Viewport*>
XtOpenGL4VitralEditorMCPProtocol::selectedViewports(
    const java::String& request)
{
    ViewportSet* set = parent->getApplicationModel()->getActiveViewportSet();
    java::ArrayList<Viewport*> out;
    double index = numberProperty(request, "viewport", -1);

    if ( index < 0 ) {
        for ( int i = 0; i < set->getViewportCount(); i++ ) {
            out.add(set->getViewport(i));
        }
    }
    else if ( index < set->getViewportCount() ) {
        out.add(set->getViewport((int)index));
    }
    else {
        throw std::invalid_argument(
            "Viewport index out of range: " + std::to_string((int)index));
    }
    return out;
}

void XtOpenGL4VitralEditorMCPProtocol::setRendererConfiguration(
    const java::String& request)
{
    java::ArrayList<Viewport*> viewports = selectedViewports(request);

    for ( long i = 0; i < viewports.size(); i++ ) {
        Viewport* viewport = viewports.get(i);
        RendererConfiguration* q = viewport->getRendererConfiguration();
        bool value;

        if ( booleanProperty(request, "points", value) ) q->setPoints(value);
        if ( booleanProperty(request, "wires", value) ) q->setWires(value);
        if ( booleanProperty(request, "surfaces", value) ) q->setSurfaces(value);
        if ( booleanProperty(request, "texture", value) ) q->setTexture(value);
        if ( booleanProperty(request, "bumpMap", value) ) q->setBumpMap(value);
        if ( booleanProperty(request, "boundingVolume", value) ) q->setBoundingVolume(value);
        if ( booleanProperty(request, "normals", value) ) q->setNormals(value);
        if ( booleanProperty(request, "trianglesNormals", value) ) q->setTrianglesNormals(value);
        if ( booleanProperty(request, "selectionCorners", value) ) q->setSelectionCorners(value);

        java::String shading = stringProperty(request, "shading", "");
        if ( !shading.isEmpty() ) {
            q->setShadingType(shadingTypeValueOf(shading.toUpperCase()));
        }

        if ( booleanProperty(request, "grid", value) ) viewport->setShowGrid(value);
        java::String renderMode = stringProperty(request, "renderMode", "");
        if ( renderMode.equalsIgnoreCase("gpu") ) {
            viewport->setRenderMode(Viewport::RENDER_MODE_Z_BUFFER);
        }
        else if ( renderMode.equalsIgnoreCase("cpu") ) {
            viewport->setRenderMode(Viewport::RENDER_MODE_RAYTRACING);
        }
        else if ( !renderMode.isEmpty() ) {
            throw std::invalid_argument("renderMode must be gpu or cpu");
        }
    }
}

java::String XtOpenGL4VitralEditorMCPProtocol::describeRendererConfigurations(
    const java::String& request)
{
    java::StringBuilder sb;
    ViewportSet* set = parent->getApplicationModel()->getActiveViewportSet();
    java::ArrayList<Viewport*> viewports = selectedViewports(request);
    bool first = true;

    sb.append("{\"viewports\":[");
    for ( long i = 0; i < viewports.size(); i++ ) {
        Viewport* viewport = viewports.get(i);
        RendererConfiguration* q = viewport->getRendererConfiguration();
        int index = -1;

        for ( int j = 0; j < set->getViewportCount(); j++ ) {
            if ( set->getViewport(j) == viewport ) {
                index = j;
                break;
            }
        }
        if ( !first ) {
            sb.append(',');
        }
        first = false;
        sb.append("{\"index\":").append(index)
            .append(",\"title\":\"").append(escape(viewport->getTitle())).append('"')
            .append(",\"points\":").append(q->isPointsSet())
            .append(",\"wires\":").append(q->isWiresSet())
            .append(",\"surfaces\":").append(q->isSurfacesSet())
            .append(",\"texture\":").append(q->isTextureSet())
            .append(",\"bumpMap\":").append(q->isBumpMapSet())
            .append(",\"boundingVolume\":").append(q->isBoundingVolumeSet())
            .append(",\"normals\":").append(q->isNormalsSet())
            .append(",\"trianglesNormals\":").append(q->isTrianglesNormalsSet())
            .append(",\"selectionCorners\":").append(q->isSelectionCornersSet())
            .append(",\"shading\":\"").append(shadingTypeName(q->getShadingTypeEnum())).append('"')
            .append(",\"grid\":").append(viewport->isShowGrid())
            .append(",\"renderMode\":\"")
            .append(viewport->getRenderMode() == Viewport::RENDER_MODE_RAYTRACING ? "cpu" : "gpu")
            .append("\"}");
    }
    sb.append("]}");
    return sb.toString();
}

java::String XtOpenGL4VitralEditorMCPProtocol::raytracePng(
    const java::String& request)
{
    java::String path = stringProperty(request, "path", "./mcp-raytrace.png");
    int width = (int)numberProperty(request, "width", 640);
    int height = (int)numberProperty(request, "height", 480);
    parent->getApplicationModel()->setRaytracedImageWidth(width);
    parent->getApplicationModel()->setRaytracedImageHeight(height);
    parent->doRaytracingImage();
    java::File out(path);
    if ( !ImagePersistence::exportPNG(out,
             parent->getApplicationModel()->getRaytracedImage()) ) {
        throw std::runtime_error((java::String("Can not write ") +
            out.getAbsolutePath()).c_str());
    }
    return java::String("{\"ok\":true,\"path\":\"") +
        escape(out.getAbsolutePath()) + "\"}";
}

java::String XtOpenGL4VitralEditorMCPProtocol::viewportJpg(
    const java::String& request)
{
    java::String path = stringProperty(request, "path",
                                       "./outputSelectedViewport.jpg");
    XtOpenGL4ApplicationController* drawingArea = getDrawingAreaController();
    java::File out(path);
    drawingArea->exportViewportJpg(out);
    return java::String("{\"ok\":true,\"path\":\"") +
        escape(out.getAbsolutePath()) + "\"}";
}

java::String XtOpenGL4VitralEditorMCPProtocol::workspaceJpg(
    const java::String& request)
{
    java::String path = stringProperty(request, "path", "./outputViewport.jpg");
    XtOpenGL4ApplicationController* drawingArea = getDrawingAreaController();
    java::File out(path);
    drawingArea->exportWorkspaceJpg(out);
    return java::String("{\"ok\":true,\"path\":\"") +
        escape(out.getAbsolutePath()) + "\"}";
}

java::String XtOpenGL4VitralEditorMCPProtocol::describeScene()
{
    Scene* scene = parent->getApplicationModel()->getScene();
    java::StringBuilder sb;
    sb.append("{\"bodies\":[");
    java::ArrayList<SimpleBody*>& bodies = scene->scene->getSimpleBodies();
    for ( int i = 0; i < bodies.size(); i++ ) {
        if ( i > 0 ) {
            sb.append(',');
        }
        SimpleBody* body = bodies.get(i);
        Geometry* geometry = body->getGeometry();
        Vector3Dd position = body->getPosition();
        Vector3Dd scale = body->getScale();
        sb.append("{\"index\":").append(i)
            .append(",\"name\":\"").append(escape(body->getName())).append('"')
            .append(",\"geometry\":\"").append(geometry->getClassSimpleName()).append('"')
            .append(",\"position\":").append(vector(position))
            .append(",\"scale\":").append(vector(scale));
        Sphere* sphere = dynamic_cast<Sphere*>(geometry);
        if ( sphere != nullptr ) {
            sb.append(",\"radius\":").append(sphere->getRadius());
        }
        sb.append('}');
    }
    sb.append("],\"lights\":[");
    java::ArrayList<Light*>& lights = scene->scene->getLights();
    for ( int i = 0; i < lights.size(); i++ ) {
        if ( i > 0 ) {
            sb.append(',');
        }
        Light* light = lights.get(i);
        sb.append("{\"index\":").append(i)
            .append(",\"type\":\"").append(light->getClassSimpleName()).append('"')
            .append(",\"position\":").append(vector(light->getPosition()))
            .append(",\"emission\":").append(color(light->getEmission()))
            .append('}');
    }
    sb.append("]}");
    return sb.toString();
}

java::String XtOpenGL4VitralEditorMCPProtocol::toolsJson()
{
    return java::String("{\"tools\":[")
        + tool("scene.describe", "Return the bodies (index, name, geometry, position, scale, radius of spheres) and lights (index, type, position, emission) as JSON.")
        + "," + tool("scene.clear", "Remove all bodies, lights and debug groups (undoable).")
        + "," + tool("scene.add_point_light", "Create a point light inside the view of a viewport (first light white, the rest random light colors and positions). Optional arguments: x,y,z,r,g,b override the automatic values.")
        + "," + tool("scene.add_sphere", "Create a sphere. Arguments: radius,x,y,z.")
        + "," + tool("scene.add_cone", "Create a cone (or truncated cone). Arguments: baseRadius,topRadius,height,x,y,z.")
        + "," + tool("scene.add_cylinder", "Create a cylinder. Arguments: radius,height,x,y,z.")
        + "," + tool("scene.move_body", "Set the position of a body (default: the last one; undoable). Arguments: index,x,y,z (missing coordinates are kept).")
        + "," + tool("scene.select_body", "Select one body (a negative index clears the selection). Arguments: index.")
        + "," + tool("gui.set_mode", "Set the interaction mode. Arguments: mode (camera|select|translate|rotate|scale).")
        + "," + tool("gui.mouse", "Send a mouse event to the drawing area. Arguments: type (move|press|drag|release), x, y (logical pixels of the drawing area, as given by viewport.project), button (1 left, 2 middle, 3 right; default 1). Returns the scene state.")
        + "," + tool("gui.key", "Send a key press to the drawing area. Arguments: key (a single character, or tab|enter|backspace|delete|escape|left|right|up|down|pageup|pagedown), shift (default false), ctrl (default false; i.e. key z with ctrl is undo, y with ctrl is redo, and with shift too they work over the view of the selected viewport). Returns the scene state.")
        + "," + tool("gui.command", "Execute a command of the GUI that works only over the model, as its menu item or button does (i.e. IDC_CREATE_SPHERE, IDC_CREATE_FUNCTIONALEXPLICITSURFACE, IDC_CREATE_OMNILIGHT, IDC_OTHERS_CYCLE_BACKGROUND). Arguments: command. Returns result (DONE, FAILED or NOT_HANDLED for commands that need the GUI, i.e. file dialogs) and the status message.")
        + "," + tool("edit.history", "Return the undo/redo state of the scene history and of the view history of each viewport: operations to undo and redo, and the names of the next ones.")
        + "," + tool("viewport.project", "Drawing area pixels (as used by gui.mouse) of the first selected body origin and its x, y, z unit-axis tips in a viewport. Arguments: viewport (index, default 0).")
        + "," + tool("render.get_configuration", "Return the rendering configuration of the viewports. Arguments: viewport (index; default all).")
        + "," + tool("render.set_configuration", "Set the rendering configuration of the viewports, only in the given values. Arguments: viewport (index; default all), and any of the booleans points,wires,surfaces,texture,bumpMap,boundingVolume,normals,trianglesNormals,selectionCorners,grid, shading (nolight|flat|gouraud|phong|cook_terrance) and renderMode (gpu|cpu).")
        + "," + tool("render.raytrace_png", "Raytrace the scene from the camera of the last drawn viewport and export a PNG (it also writes ./output.jpg). Arguments: path, width (default 640), height (default 480).")
        + "," + tool("viewport.export_jpg", "Export the selected viewport, as drawn, to a JPG. Arguments: path.")
        + "," + tool("workspace.export_jpg", "Export the whole drawing area, with all its viewports, to a JPG. Arguments: path.")
        + "," + tool("gui.list_languages", "List the languages available for the GUI (I18N files in etc/gui), marking the current one.")
        + "," + tool("gui.set_language", "Change the GUI language, rebuilding the GUI. Arguments: language (an id given by gui.list_languages).")
        + "," + tool("app.exit", "Close the application (after answering this call).")
        + "]}";
}

java::String XtOpenGL4VitralEditorMCPProtocol::tool(const char* name,
                                                    const char* description)
{
    return java::String("{\"name\":\"") + name + "\",\"description\":\""
        + escape(description)
        + "\",\"inputSchema\":{\"type\":\"object\",\"additionalProperties\":true}}";
}

java::String XtOpenGL4VitralEditorMCPProtocol::content(const java::String& json)
{
    return java::String("{\"content\":[{\"type\":\"text\",\"text\":\"")
        + escape(json) + "\"}],\"isError\":false}";
}

java::String XtOpenGL4VitralEditorMCPProtocol::result(const java::String& id,
                                                      const java::String& json)
{
    return java::String("{\"jsonrpc\":\"2.0\",\"id\":") + id + ",\"result\":" +
        json + "}";
}

java::String XtOpenGL4VitralEditorMCPProtocol::error(
    const java::String& id, int code, const java::String& message)
{
    return java::String("{\"jsonrpc\":\"2.0\",\"id\":") + id
        + ",\"error\":{\"code\":" + java::String::valueOf(code) +
        ",\"message\":\"" + escape(message) + "\"}}";
}

java::String XtOpenGL4VitralEditorMCPProtocol::vector(const Vector3Dd& v)
{
    java::StringBuilder sb;
    sb.append("{\"x\":").append(v.x()).append(",\"y\":").append(v.y())
        .append(",\"z\":").append(v.z()).append('}');
    return sb.toString();
}

java::String XtOpenGL4VitralEditorMCPProtocol::color(const ColorRgb& c)
{
    java::StringBuilder sb;
    sb.append("{\"r\":").append(c.r()).append(",\"g\":").append(c.g())
        .append(",\"b\":").append(c.b()).append('}');
    return sb.toString();
}

java::String XtOpenGL4VitralEditorMCPProtocol::stringProperty(
    const java::String& json, const java::String& key,
    const java::String& defaultValue)
{
    Pattern pattern = Pattern::compile(java::String("\"") + Pattern::quote(key)
        + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
    Matcher matcher = pattern.matcher(json);
    if ( !matcher.find() ) {
        return defaultValue;
    }
    return matcher.group(1);
}

java::String XtOpenGL4VitralEditorMCPProtocol::nestedStringProperty(
    const java::String& json, const java::String& key,
    const java::String& defaultValue)
{
    return stringProperty(json, key, defaultValue);
}

java::String XtOpenGL4VitralEditorMCPProtocol::idProperty(
    const java::String& json)
{
    Pattern pattern = Pattern::compile(
        "\"id\"\\s*:\\s*(\"((?:\\\\.|[^\"])*)\"|[-0-9]+|null)");
    Matcher matcher = pattern.matcher(json);
    if ( !matcher.find() ) {
        return "null";
    }
    return matcher.group(1);
}

double XtOpenGL4VitralEditorMCPProtocol::numberProperty(
    const java::String& json, const java::String& key, double defaultValue)
{
    Pattern pattern = Pattern::compile(java::String("\"") + Pattern::quote(key)
        + "\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)");
    Matcher matcher = pattern.matcher(json);
    if ( !matcher.find() ) {
        return defaultValue;
    }
    return java::Double::parseDouble(matcher.group(1));
}

bool XtOpenGL4VitralEditorMCPProtocol::booleanProperty(
    const java::String& json, const java::String& key, bool& outValue)
{
    Pattern pattern = Pattern::compile(java::String("\"") + Pattern::quote(key)
        + "\"\\s*:\\s*(true|false)");
    Matcher matcher = pattern.matcher(json);
    if ( !matcher.find() ) {
        return false;
    }
    outValue = matcher.group(1).equals("true");
    return true;
}

java::String XtOpenGL4VitralEditorMCPProtocol::escape(const java::String& in)
{
    return in.replace("\\", "\\\\").replace("\"", "\\\"");
}
