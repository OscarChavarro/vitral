#include <stdexcept>
#include <string>
#include "vsdk/toolkit/common/Cpp98Support.h"

#include "java/lang/StringBuilder.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/volume/Cone.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/light/PointLight.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleBodyGroup.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "vsdk/toolkit/gui/viewport/ViewportSet.h"
#include "application/commands/GuiEventExecutor.h"
#include "application/mcp/MCPJson.h"
#include "application/mcp/MCPSceneTools.h"
#include "model/ApplicationModel.h"
#include "model/Scene.h"
#include "model/SceneLightFactory.h"
#include "model/history/EditHistory.h"
#include "model/history/SceneHistory.h"
#include "model/history/UndoQueue.h"
#include "model/history/ViewportHistory.h"
#include "model/selection/SelectionSet.h"


/**
Collects the status message of a GUI command.
*/
class MessageCollector : public Presenter {
public:
    String message;

    virtual void showStatusMessage(const String& text)
    {
        message = text;
    }
};


MCPSceneTools::MCPSceneTools(ApplicationModel* model)
    : model(model)
{
}

Scene* MCPSceneTools::scene() const
{
    return model->getScene();
}

void MCPSceneTools::recordSceneChange(const String& tool,
                                      SceneChange::Method method,
                                      Geometry* geometry,
                                      const String& request)
{
    SceneChange change(this, method, geometry, request);
    model->getEditHistory()->getSceneHistory()->perform(tool, &change);
}

String MCPSceneTools::describeScene()
{
    Scene* scene = this->scene();
    StringBuilder sb;
    sb.append("{\"bodies\":[");
    ArrayList<SimpleBody*>& bodies = scene->scene->getSimpleBodies();
    for ( int i = 0; i < bodies.size(); i++ ) {
        if ( i > 0 ) {
            sb.append(',');
        }
        SimpleBody* body = bodies.get(i);
        Geometry* geometry = body->getGeometry();
        Vector3Dd position = body->getPosition();
        Vector3Dd scale = body->getScale();
        sb.append("{\"index\":").append(i)
            .append(",\"name\":\"").append(MCPJson::escape(body->getName())).append('"')
            .append(",\"geometry\":\"").append(geometry->getClassSimpleName()).append('"')
            .append(",\"position\":").append(MCPJson::vector(position))
            .append(",\"scale\":").append(MCPJson::vector(scale));
        Sphere* sphere = dynamic_cast<Sphere*>(geometry);
        if ( sphere != NULL ) {
            sb.append(",\"radius\":").append(sphere->getRadius());
        }
        sb.append('}');
    }
    sb.append("],\"lights\":[");
    ArrayList<Light*>& lights = scene->scene->getLights();
    for ( int i = 0; i < lights.size(); i++ ) {
        if ( i > 0 ) {
            sb.append(',');
        }
        Light* light = lights.get(i);
        sb.append("{\"index\":").append(i)
            .append(",\"type\":\"").append(light->getClassSimpleName()).append('"')
            .append(",\"position\":").append(MCPJson::vector(light->getPosition()))
            .append(",\"emission\":").append(MCPJson::color(light->getEmission()))
            .append('}');
    }
    sb.append("]}");
    return sb.toString();
}

void MCPSceneTools::clearScene()
{
    recordSceneChange("scene.clear", &MCPSceneTools::removeAllElements,
                      NULL, String());
}

void MCPSceneTools::removeAllElements(Geometry* /*unused*/,
                                      const String& /*unusedRequest*/)
{
    Scene* scene = this->scene();
    // Referenced by the history, that can put them back (see `Scene`)
    scene->scene->getSimpleBodies().clear();
    scene->scene->getLights().clear();
    scene->debugThingGroups.clear();
    scene->selectedThings->sync();
    scene->selectedLights->sync();
    scene->selectedDebugThingGroups->sync();
}

void MCPSceneTools::addPointLight(const String& request)
{
    recordSceneChange("scene.add_point_light",
        &MCPSceneTools::createPointLight, NULL, request);
}

void MCPSceneTools::createPointLight(Geometry* /*unused*/,
                                     const String& request)
{
    bool explicitPosition = MCPJson::hasProperty(request, "x") ||
        MCPJson::hasProperty(request, "y") || MCPJson::hasProperty(request, "z");
    bool explicitColor = MCPJson::hasProperty(request, "r") ||
        MCPJson::hasProperty(request, "g") || MCPJson::hasProperty(request, "b");

    if ( !explicitPosition && !explicitColor ) {
        model->addNewLight();
        return;
    }
    // Missing values follow the default policy of the first light
    ArrayList<Light*> noLights;
    PointLight* defaults = SceneLightFactory().createLight(
        noLights, model->getActiveViewportSet());
    Vector3Dd p = defaults == NULL ? Vector3Dd() : defaults->getPosition();
    delete defaults;
    double x = MCPJson::numberProperty(request, "x", p.x());
    double y = MCPJson::numberProperty(request, "y", p.y());
    double z = MCPJson::numberProperty(request, "z", p.z());
    double r = MCPJson::numberProperty(request, "r", 1.0);
    double g = MCPJson::numberProperty(request, "g", 1.0);
    double b = MCPJson::numberProperty(request, "b", 1.0);
    scene()->scene->addLight(new PointLight(Vector3Dd(x, y, z),
        ColorRgb(r, g, b)));
}

void MCPSceneTools::addSphere(const String& request)
{
    double radius = MCPJson::numberProperty(request, "radius", 1.0);
    recordSceneChange("scene.add_sphere", &MCPSceneTools::placeNewBody,
        new Sphere(radius), request);
}

void MCPSceneTools::addCone(const String& request)
{
    double baseRadius = MCPJson::numberProperty(request, "baseRadius", 1.0);
    double topRadius = MCPJson::numberProperty(request, "topRadius", 0.0);
    double height = MCPJson::numberProperty(request, "height", 2.0);
    recordSceneChange("scene.add_cone", &MCPSceneTools::placeNewBody,
        new Cone(baseRadius, topRadius, height), request);
}

void MCPSceneTools::addCylinder(const String& request)
{
    double radius = MCPJson::numberProperty(request, "radius", 1.0);
    double height = MCPJson::numberProperty(request, "height", 2.0);
    recordSceneChange("scene.add_cylinder", &MCPSceneTools::placeNewBody,
        new Cone(radius, radius, height), request);
}

void MCPSceneTools::placeNewBody(Geometry* geometry,
                                 const String& request)
{
    SimpleBody* body = scene()->addThing(geometry);
    body->setPosition(Vector3Dd(
        MCPJson::numberProperty(request, "x", 0.0),
        MCPJson::numberProperty(request, "y", 0.0),
        MCPJson::numberProperty(request, "z", 0.0)));
}

void MCPSceneTools::moveBody(const String& request)
{
    recordSceneChange("scene.move_body", &MCPSceneTools::setBodyPosition,
                      NULL, request);
}

void MCPSceneTools::setBodyPosition(Geometry* /*unused*/,
                                    const String& request)
{
    ArrayList<SimpleBody*>& bodies = scene()->scene->getSimpleBodies();
    int index = (int)MCPJson::numberProperty(request, "index",
                                             (double)(bodies.size() - 1));

    if ( index < 0 || index >= bodies.size() ) {
        throw std::invalid_argument(
            "Body index out of range: " + Cpp98Support::toString(index));
    }
    SimpleBody* body = bodies.get(index);
    Vector3Dd p = body->getPosition();

    body->setPosition(Vector3Dd(
        MCPJson::numberProperty(request, "x", p.x()),
        MCPJson::numberProperty(request, "y", p.y()),
        MCPJson::numberProperty(request, "z", p.z())));
}

void MCPSceneTools::selectBody(const String& request)
{
    Scene* scene = this->scene();
    int index = (int)MCPJson::numberProperty(request, "index", -1);

    scene->selectedThings->unselectAll();
    if ( index >= 0 ) {
        if ( index >= scene->scene->getSimpleBodies().size() ) {
            throw std::invalid_argument(
                "Body index out of range: " + Cpp98Support::toString(index));
        }
        scene->selectedThings->select(index);
    }
}

String MCPSceneTools::executeGuiCommand(const String& request)
{
    String command = MCPJson::stringProperty(request, "command", "");
    MessageCollector message;
    GuiEventExecutor executor(model, &message);
    CommandResult result = executor.execute(command);

    return String("{\"command\":\"") + MCPJson::escape(command) +
        "\",\"result\":\"" + commandResultName(result) +
        "\",\"message\":\"" + MCPJson::escape(message.message) + "\"}";
}

String MCPSceneTools::describeEditHistory()
{
    EditHistory* history = model->getEditHistory();
    ViewportSet* viewportSet = model->getActiveViewportSet();
    StringBuilder sb;
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
            .append(",\"title\":\"").append(MCPJson::escape(viewport->getTitle())).append('"')
            .append(",\"selected\":")
            .append(viewport == viewportSet->getSelectedViewport())
            .append(",\"history\":")
            .append(queueJson(history->getViewportHistory()->getQueue(viewport)))
            .append('}');
    }
    sb.append("]}");
    return sb.toString();
}

String MCPSceneTools::queueJson(UndoQueue* queue)
{
    // The C++ queue names no operation with an empty name (Java: null)
    bool withUndo = queue->getUndoCount() > 0;
    bool withRedo = queue->getRedoCount() > 0;
    StringBuilder sb;

    sb.append("{\"undo\":").append(queue->getUndoCount())
        .append(",\"redo\":").append(queue->getRedoCount())
        .append(",\"nextUndo\":");
    if ( withUndo ) {
        sb.append('"').append(MCPJson::escape(queue->getUndoName())).append('"');
    }
    else {
        sb.append("null");
    }
    sb.append(",\"nextRedo\":");
    if ( withRedo ) {
        sb.append('"').append(MCPJson::escape(queue->getRedoName())).append('"');
    }
    else {
        sb.append("null");
    }
    sb.append('}');
    return sb.toString();
}
