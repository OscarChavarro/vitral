//= References:                                                             =
//= [FUNK2003], Funkhouser, Thomas.  Min, Patrick. Kazhdan, Michael. Chen,  =
//=     Joyce. Halderman, Alex. Dobkin, David. Jacobs, David. "A Search     =
//=     Engine for 3D Models", ACM Transactions on Graphics, Vol 22. No1.   =
//=     January 2003. Pp. 83-105                                            =

#ifndef __PROJECTED_VIEWS_DEBUGGER__
#define __PROJECTED_VIEWS_DEBUGGER__

class ApplicationModel;
class DrawingArea;
class DrawingAreaHost;
class Image;
class ProjectedViewRenderer;
class RendererConfiguration;
class Scene;
class SimpleBodyGroup;
class ZBuffer;

/**
Debugging tool that renders the projected views of the selected bodies (13
views from the faces and corners of their bounding cube) and presents them
as textured boxes in a visual debug group of the scene. Views are rendered
through a `ProjectedViewRenderer`, so this class does not depend on the
rendering technology.
*/
class ProjectedViewsDebugger {
private:
    Scene* scene;
    DrawingArea* drawingArea;
    DrawingAreaHost* host;
    RendererConfiguration* quality;
    int viewSize;
    bool isTransparent;

    Image* createProjectedView(ProjectedViewRenderer* renderer,
                               SimpleBodyGroup* referenceBodies, int cam);
    Image* createContourImage(const ZBuffer* depth) const;
    SimpleBodyGroup* addDebugProjectedView(ProjectedViewRenderer* renderer,
                                           SimpleBodyGroup* referenceBodies);

    ProjectedViewsDebugger(const ProjectedViewsDebugger& other);
    ProjectedViewsDebugger& operator=(const ProjectedViewsDebugger& other);

public:
    ProjectedViewsDebugger(ApplicationModel* model, DrawingAreaHost* host);
    virtual ~ProjectedViewsDebugger();

    /**
    Creates the debug group with the projected views of the selected bodies,
    if it was requested in the drawing area.
    @param renderer renders the projected views
    */
    void debugIfNeeded(ProjectedViewRenderer* renderer);
};

#endif
