#ifndef __OPEN_GL_4_ROTATE_GIZMO_RENDERER__
#define __OPEN_GL_4_ROTATE_GIZMO_RENDERER__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class Camera;
class RotateGizmo;

/**
Renders a `RotateGizmo` with the GL4 core pipeline (see
`OpenGL4ColoredPrimitiveRenderer`).

Each of the three rings around the local axes is drawn as a colored triangle
strip facing the camera (see `RotateGizmo::buildRingStrips(int)`), with the
color of its axis (or yellow if the ring is under the cursor or was chosen).
Only the half of each ring closer to the camera is drawn, unless it is seen
almost face on, when it is drawn whole.

A fourth ring, gray (or yellow if under the cursor or chosen), always whole,
is drawn around the front vector of the camera (see
`RotateGizmo::buildCameraRingStrip()`).

While a ring is being dragged, the arc the rotation has swept is drawn over
them as a translucent sector with the color of the axis of the ring (never
yellow, see `RotateGizmo::buildArcFan()`).

Usage (render thread, once per frame, after the transformation matrix of the
gizmo has been set):
<pre>
    OpenGL4RotateGizmoRenderer::draw(gizmo, camera);
</pre>
*/
class OpenGL4RotateGizmoRenderer {
private:
    static const float ARC_OPACITY;

    static void drawArc(const Matrix4x4d& mvp,
                        const java::ArrayList<Vector3Dd>& fan,
                        const ColorRgb& c);
    static void drawRing(const Matrix4x4d& mvp,
                         const java::ArrayList<Vector3Dd>& strip,
                         const ColorRgb& c);

public:
    /**
    Draws the gizmo over the current contents of the surface.

    @param gizmo gizmo to draw; its transformation matrix must be set
    @param camera camera that views the gizmo
    */
    static void draw(RotateGizmo* gizmo, Camera* camera);
};

#endif
