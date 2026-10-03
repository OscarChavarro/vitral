#include <stdexcept>
#include <string>

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

namespace {

/**
Collects the status message of a GUI command.
*/
class MessageCollector : public Presenter {
public:
    java::String message;

    virtual void showStatusMessage(const java::String& text) override
    {
        message = text;
    }
};

}

MCPSceneTools::MCPSceneTools(ApplicationModel* model)
    : model(model)
{
}

Scene* MCPSceneTools::scene() const
{
    return model->getScene();
}

void MCPSceneTools::recordSceneChange(const java::String& tool,
                                      const std::function<void()>& change)
{
    model->getEditHistory()->getSceneHistory()->perform(tool, change);
}

java::String MCPSceneTools::describeScene()
{
    Scene* scene = this->scene();
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
            .append(",\"name\":\"").append(MCPJson::escape(body->getName())).append('"')
            .append(",\"geometry\":\"").append(geometry->getClassSimpleName()).append('"')
            .append(",\"position\":").append(MCPJson::vector(position))
            .append(",\"scale\":").append(MCPJson::vector(scale));
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
            .append(",\"position\":").append(MCPJson::vector(light->getPosition()))
            .append(",\"emission\":").append(MCPJson::color(light->getEmission()))
            .append('}');
    }
    sb.append("]}");
    return sb.toString();
}

void MCPSceneTools::clearScene()
{
    recordSceneChange("scene.clear", [this]() {
        Scene* scene = this->scene();
        // Referenced by the history, that can put them back (see `Scene`)
        scene->scene->getSimpleBodies().clear();
        scene->scene->getLights().clear();
        scene->debugThingGroups.clear();
        scene->selectedThings->sync();
        scene->selectedLights->sync();
        scene->selectedDebugThingGroups->sync();
    });
}

void MCPSceneTools::addPointLight(const java::String& request)
{
    recordSceneChange("scene.add_point_light",
        [this, &request]() { createPointLight(request); });
}

void MCPSceneTools::createPointLight(const java::String& request)
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
    java::ArrayList<Light*> noLights;
    PointLight* defaults = SceneLightFactory().createLight(
        noLights, model->getActiveViewportSet());
    Vector3Dd p = defaults == nullptr ? Vector3Dd() : defaults->getPosition();
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

void MCPSceneTools::addSphere(const java::String& request)
{
    double radius = MCPJson::numberProperty(request, "radius", 1.0);
    recordSceneChange("scene.add_sphere",
        [this, radius, &request]() { placeNewBody(new Sphere(radius), request); });
}

void MCPSceneTools::addCone(const java::String& request)
{
    double baseRadius = MCPJson::numberProperty(request, "baseRadius", 1.0);
    double topRadius = MCPJson::numberProperty(request, "topRadius", 0.0);
    double height = MCPJson::numberProperty(request, "height", 2.0);
    recordSceneChange("scene.add_cone",
        [this, baseRadius, topRadius, height, &request]() {
            placeNewBody(new Cone(baseRadius, topRadius, height), request);
        });
}

void MCPSceneTools::addCylinder(const java::String& request)
{
    double radius = MCPJson::numberProperty(request, "radius", 1.0);
    double height = MCPJson::numberProperty(request, "height", 2.0);
    recordSceneChange("scene.add_cylinder",
        [this, radius, height, &request]() {
            placeNewBody(new Cone(radius, radius, height), request);
        });
}

void MCPSceneTools::placeNewBody(Geometry* geometry,
                                 const java::String& request)
{
    SimpleBody* body = scene()->addThing(geometry);
    body->setPosition(Vector3Dd(
        MCPJson::numberProperty(request, "x", 0.0),
        MCPJson::numberProperty(request, "y", 0.0),
        MCPJson::numberProperty(request, "z", 0.0)));
}

void MCPSceneTools::moveBody(const java::String& request)
{
    recordSceneChange("scene.move_body", [this, &request]() {
        java::ArrayList<SimpleBody*>& bodies = scene()->scene->getSimpleBodies();
        int index = (int)MCPJson::numberProperty(request, "index",
                                                 (double)(bodies.size() - 1));

        if ( index < 0 || index >= bodies.size() ) {
            throw std::invalid_argument(
                "Body index out of range: " + std::to_string(index));
        }
        SimpleBody* body = bodies.get(index);
        Vector3Dd p = body->getPosition();

        body->setPosition(Vector3Dd(
            MCPJson::numberProperty(request, "x", p.x()),
            MCPJson::numberProperty(request, "y", p.y()),
            MCPJson::numberProperty(request, "z", p.z())));
    });
}

void MCPSceneTools::selectBody(const java::String& request)
{
    Scene* scene = this->scene();
    int index = (int)MCPJson::numberProperty(request, "index", -1);

    scene->selectedThings->unselectAll();
    if ( index >= 0 ) {
        if ( index >= scene->scene->getSimpleBodies().size() ) {
            throw std::invalid_argument(
                "Body index out of range: " + std::to_string(index));
        }
        scene->selectedThings->select(index);
    }
}

java::String MCPSceneTools::executeGuiCommand(const java::String& request)
{
    java::String command = MCPJson::stringProperty(request, "command", "");
    MessageCollector message;
    GuiEventExecutor executor(model, &message);
    CommandResult result = executor.execute(command);

    return java::String("{\"command\":\"") + MCPJson::escape(command) +
        "\",\"result\":\"" + commandResultName(result) +
        "\",\"message\":\"" + MCPJson::escape(message.message) + "\"}";
}

java::String MCPSceneTools::describeEditHistory()
{
    EditHistory* history = model->getEditHistory();
    ViewportSet* viewportSet = model->getActiveViewportSet();
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

java::String MCPSceneTools::queueJson(UndoQueue* queue)
{
    // The C++ queue names no operation with an empty name (Java: null)
    bool withUndo = queue->getUndoCount() > 0;
    bool withRedo = queue->getRedoCount() > 0;
    java::StringBuilder sb;

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
