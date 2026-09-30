#ifndef __SOLID_TEXTURE_JOGL4_DEBUGGER_RENDERER__
#define __SOLID_TEXTURE_JOGL4_DEBUGGER_RENDERER__

#include "vsdk/toolkit/render/opengl4/OpenGL4SolidTextureRenderer.h"
#include "render/Jogl4SolidTexturePlanesRenderer.h"

class SolidTextureModel;

class Jogl4DebuggerRenderer {
private:
    SolidTextureModel* model;
    Jogl4SolidTexturePlanesRenderer planesRenderer;
    OpenGL4SolidTextureRenderer solidTextureRenderer;

    void acquireGizmoSnapshots();
    void drawMeshModel(class InfinitePlane* clippingPlane);
    void drawGizmos();

public:
    explicit Jogl4DebuggerRenderer(SolidTextureModel* model);
    bool init();
    void display();
    void reshape(int width, int height);
    void dispose();
};

#endif
