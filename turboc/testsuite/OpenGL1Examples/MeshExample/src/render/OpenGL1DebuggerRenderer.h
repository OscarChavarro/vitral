#ifndef __OPEN_GL_1_DEBUGGER_RENDERER__
#define __OPEN_GL_1_DEBUGGER_RENDERER__

class MeshModel;
class OpenGL1LabelImageProvider;
class OpenGL1ViewportWindow;
class Viewport;
class ViewportSet;
class Camera;
class Light;
class RendererConfiguration;
class SimpleBody;
class SimpleMaterial;
class TriangleMesh;

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

/**
OpenGL 1.2 counterpart of `Jogl4DebuggerRenderer`: the fixed function state
selected by `OpenGL1RendererConfigurationStateSelector` takes the place of the
GLSL programs, and the vertices of each frame go to OpenGL as client side
vertex arrays (the mesh may change between frames). Phong and Cook-Torrance
shading types are presented as Gouraud, as OpenGL 1.2 has no per pixel
shading.
*/
class OpenGL1DebuggerRenderer {
private:
    static const float SURFACE_POLYGON_OFFSET_FACTOR;
    static const float SURFACE_POLYGON_OFFSET_UNITS;
    static const float LINE_POLYGON_OFFSET_FACTOR;
    static const float LINE_POLYGON_OFFSET_UNITS;

    MeshModel* model;
    // A single viewport covering the whole canvas, used to draw the input
    // gizmo of the ray gizmo (numeric boxes) at its lower right corner
    ViewportSet* viewportSet;
    Viewport* viewport;
    OpenGL1ViewportWindow* viewportWindow;

    void drawSimpleBody(SimpleBody* body, Camera* camera, const ArrayList<Light*>& lights, RendererConfiguration* quality);
    BOOLEAN buildFrame(TriangleMesh* mesh, ArrayList<float>& outPositions, ArrayList<float>& outNormals, ArrayList<float>& outUvs);
    static void drawArrays(int mode, ArrayList<float>& positions, ArrayList<float>* normals, ArrayList<float>* uvs);
    static SimpleMaterial defaultMaterial();

public:
    OpenGL1DebuggerRenderer(MeshModel* model,
                            OpenGL1LabelImageProvider* labelImageProvider);
    ~OpenGL1DebuggerRenderer();

    /**
    @return the viewport set where the overlays are drawn, so its element
    scaler can be configured with the resolution of the screen
    */
    ViewportSet* getViewportSet();
    BOOLEAN init();
    void display();
    void reshape(int width, int height);
    void dispose();
};

#endif
