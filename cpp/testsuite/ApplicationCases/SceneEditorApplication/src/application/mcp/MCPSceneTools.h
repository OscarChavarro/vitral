#ifndef __MCP_SCENE_TOOLS__
#define __MCP_SCENE_TOOLS__

#include <functional>

#include "java/lang/String.h"

class ApplicationModel;
class Geometry;
class Scene;
class UndoQueue;

/**
Tools of the automation service of the editor that query and edit the scene
of the application model: description, bodies, lights, selection, edit history
and the GUI commands that only work over the model. The changes are recorded
in the scene history (named after the tool), so they can be undone. It does not
depend on any GUI or rendering technology: repainting after a change is up to
the caller.

C++ port note: failures are C++ exceptions (i.e. `std::invalid_argument`
where Java throws `IllegalArgumentException`), with the same messages as the
Java ones.
*/
class MCPSceneTools {
private:
    ApplicationModel* model;

    Scene* scene() const;
    void recordSceneChange(const java::String& tool,
                           const std::function<void()>& change);
    void createPointLight(const java::String& request);
    void placeNewBody(Geometry* geometry, const java::String& request);
    static java::String queueJson(UndoQueue* queue);

public:
    /**
    @param model application model the tools work over (referenced)
    */
    explicit MCPSceneTools(ApplicationModel* model);

    /**
    @return the bodies and lights of the scene as JSON
    */
    java::String describeScene();

    /**
    Removes all bodies, lights and debug groups (tool `scene.clear`).
    */
    void clearScene();

    /**
    Adds a point light (tool `scene.add_point_light`).
    @param request request with the optional arguments x,y,z,r,g,b
    */
    void addPointLight(const java::String& request);

    /**
    Adds a sphere (tool `scene.add_sphere`).
    @param request request with the arguments radius,x,y,z
    */
    void addSphere(const java::String& request);

    /**
    Adds a cone (tool `scene.add_cone`).
    @param request request with the arguments baseRadius,topRadius,height,x,y,z
    */
    void addCone(const java::String& request);

    /**
    Adds a cylinder (tool `scene.add_cylinder`).
    @param request request with the arguments radius,height,x,y,z
    */
    void addCylinder(const java::String& request);

    /**
    Sets the position of a body (tool `scene.move_body`).
    @param request request with the arguments index,x,y,z
    */
    void moveBody(const java::String& request);

    /**
    Selects one body, or none (tool `scene.select_body`).
    @param request request with the argument index
    */
    void selectBody(const java::String& request);

    /**
    Executes a GUI command that only works over the model (tool
    `gui.command`).
    @param request request with the argument command
    @return the result of the command and its status message as JSON
    */
    java::String executeGuiCommand(const java::String& request);

    /**
    @return the undo/redo state of the scene history and of the view history
    of each viewport as JSON (tool `edit.history`)
    */
    java::String describeEditHistory();
};

#endif
