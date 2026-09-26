#ifndef __OPEN_GL_1_TRANSLATE_GIZMO_RENDERER__
#define __OPEN_GL_1_TRANSLATE_GIZMO_RENDERER__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class Box;
class Camera;
class Cone;
class SimpleBody;
class TranslateGizmo;

/**
Renders a `TranslateGizmo` with the OpenGL 1.2 fixed function pipeline (see
`OpenGL1ColoredPrimitiveRenderer`). The geometric model of the gizmo (arrows,
cylinders, boxes) is only used to compute the interaction with rays; it is not
drawn directly:

- The lines of the gizmo (axis shafts and plane handle segments) are drawn as
  colored triangle strips facing the camera, so they can have the width given
  by `TranslateGizmo::getLineWidth()`.
- The heads of the axes are drawn as cones, tessellated in world space by
  `GizmoSolidTessellator`, with a darker base.
- The plane handle selected is drawn as a translucent quad.

Every element is generated in world space and drawn with the projection
matrix of the camera that views the gizmo.

Usage (render thread, once per frame, after the transformation matrix of the
gizmo has been set):
<pre>
    OpenGL1TranslateGizmoRenderer::draw(gizmo, camera);
</pre>
*/
class OpenGL1TranslateGizmoRenderer {
private:
    static const double CONE_BASE_SHADE;

    static void drawLines(TranslateGizmo* gizmo, const Matrix4x4d& mvp);

    /**
    Draws a cone whose base is at the position of the element, pointing to the
    local +Z direction of the element.
    */
    static void drawCone(const Matrix4x4d& mvp, SimpleBody* element, Cone* cone);

    /**
    Draws the translucent square that shows a selected plane handle, centered
    at the position of the element and over its local XY plane.
    */
    static void drawPlaneHandle(const Matrix4x4d& mvp, SimpleBody* element, Box* box);

    static void drawStrip(const Matrix4x4d& mvp,
                          const java::ArrayList<Vector3Dd>& points,
                          const ColorRgb& c, double alpha,
                          unsigned int primitiveType);

public:
    /**
    Draws the gizmo over the current contents of the surface. The blending and
    depth mask states changed are restored.

    @param gizmo gizmo to draw; its transformation matrix must be set
    @param camera camera that views the gizmo
    */
    static void draw(TranslateGizmo* gizmo, Camera* camera);
};

#endif
