#ifndef __SOLID_TEXTURE_JOGL4_DEBUGGER_RENDERER__
#define __SOLID_TEXTURE_JOGL4_DEBUGGER_RENDERER__

#include "vsdk/toolkit/render/opengl4/OpenGL4SolidTextureRenderer.h"
#include "render/Jogl4SolidTexturePlanesRenderer.h"

class SolidTextureModel;
class OpenGL4LabelImageProvider;
class OpenGL4ViewportWindow;
class Viewport;
class ViewportSet;

class Jogl4DebuggerRenderer {
private:
    SolidTextureModel* model;
    // A single viewport covering the whole canvas, used to draw the input
    // gizmo of the ray gizmo (numeric boxes) at its lower right corner
    ViewportSet* viewportSet;
    Viewport* viewport;
    OpenGL4ViewportWindow* viewportWindow;
    Jogl4SolidTexturePlanesRenderer planesRenderer;
    OpenGL4SolidTextureRenderer solidTextureRenderer;

    void acquireGizmoSnapshots();
    void drawMeshModel(class InfinitePlane* clippingPlane);
    void drawGizmos();

public:
    Jogl4DebuggerRenderer(SolidTextureModel* model,
                          OpenGL4LabelImageProvider* labelImageProvider);
    ~Jogl4DebuggerRenderer();

    /**
    @return the viewport set where the overlays are drawn, so its element
    scaler can be configured with the resolution of the screen
    */
    ViewportSet* getViewportSet();
    bool init();
    void display();
    void reshape(int width, int height);
    void dispose();
};

#endif
