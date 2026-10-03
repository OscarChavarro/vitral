#ifndef __MCP_VIEWPORT_TOOLS__
#define __MCP_VIEWPORT_TOOLS__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"

class ApplicationModel;
class Viewport;

/**
Tools of the automation service of the editor that query and change how the
viewports of the application model are shown and used: the rendering
configuration of each viewport and the interaction mode of the drawing area.
It does not depend on any GUI or rendering technology.

C++ port note: failures are C++ exceptions (i.e. `std::invalid_argument`
where Java throws `IllegalArgumentException`), with the same messages as the
Java ones.
*/
class MCPViewportTools {
private:
    ApplicationModel* model;

    java::ArrayList<Viewport*> selectedViewports(const java::String& request);

public:
    /**
    @param model application model the tools work over (referenced)
    */
    explicit MCPViewportTools(ApplicationModel* model);

    /**
    Sets the interaction mode of the drawing area (tool `gui.set_mode`).
    @param request request with the argument mode
    @return the mode set as JSON
    */
    java::String setInteractionMode(const java::String& request);

    /**
    Sets, only in the given values, the rendering configuration of one
    viewport or of all of them (tool `render.set_configuration`).
    @param request request with the argument viewport, and the values to set
    */
    void setRendererConfiguration(const java::String& request);

    /**
    @param request request with the argument viewport (index; default all)
    @return the rendering configuration of the viewports as JSON (tool
    `render.get_configuration`)
    */
    java::String describeRendererConfigurations(const java::String& request);
};

#endif
