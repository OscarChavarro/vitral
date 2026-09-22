package render;

import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.material.RendererConfiguration;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.media.ZBuffer;

/**
Renders the projected views used by `ProjectedViewsDebugger`, with the
rendering technology in use.
*/
public interface ProjectedViewRenderer
{
    /**
    Renders a group of bodies alone, over an empty background.
    @param bodies bodies to render
    @param camera camera taking the view
    @param quality rendering configuration
    @param xSize width of the view, in pixels
    @param ySize height of the view, in pixels
    @return the depth buffer of the rendered view
    */
    ZBuffer renderDepth(SimpleBodyGroup bodies, Camera camera,
                        RendererConfiguration quality, int xSize, int ySize);
}
