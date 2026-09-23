#ifndef __PROJECTED_VIEW_RENDERER__
#define __PROJECTED_VIEW_RENDERER__

class Camera;
class RendererConfiguration;
class SimpleBodyGroup;
class ZBuffer;

/**
Renders the projected views used by `ProjectedViewsDebugger`, with the
rendering technology in use.
*/
class ProjectedViewRenderer {
public:
    virtual ~ProjectedViewRenderer() {}

    /**
    Renders a group of bodies alone, over an empty background.
    @param bodies bodies to render
    @param camera camera taking the view
    @param quality rendering configuration
    @param xSize width of the view, in pixels
    @param ySize height of the view, in pixels
    @return the depth buffer of the rendered view, owned by the caller
    */
    virtual ZBuffer* renderDepth(SimpleBodyGroup* bodies, Camera* camera,
                                 RendererConfiguration* quality, int xSize,
                                 int ySize) = 0;
};

#endif
