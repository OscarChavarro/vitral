#ifndef __OPEN_GL_4_LIGHT_RENDERER__
#define __OPEN_GL_4_LIGHT_RENDERER__

class Light;
class Camera;

#include "vsdk/toolkit/gui/gizmo/LightGizmoStyle.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "java/util/ArrayList.h"

#include <utility>
#include <vector>

class OpenGL4LightRenderer {
public:
    /**
    Activates a copy of the light for the renderers that take the lights
    from the active ones instead of as a parameter (i.e.
    `OpenGL4PolyhedralBoundedSolidRenderer`). A light with the id of an
    already active one replaces it.
    @param light light to activate, or null to do nothing
    */
    static void activate(const Light* light);

    /**
    Forgets all the lights activated so far, so `getActiveLights` reports
    the default light until other lights are activated.
    */
    static void deactivateAll();

    /**
    @param outLights receives copies (owned by the caller) of the active
    lights, in activation order, or of the default light if none is active
    */
    static void getActiveLights(java::ArrayList<Light*>& outLights);

    static void draw(const Light* light);
    static void draw(const Light* light, Camera* camera);
    static void draw(const Light* light, Camera* camera, LightGizmoStyle lightGizmoStyle);
    static void draw(const Light* light, Camera* camera,
                     LightGizmoStyle lightGizmoStyle, bool selected);

    static double getScale();
    static void setScale(double newScale);

    static void dispose();

private:
    /// Copies of the active lights by id, in activation order (Java uses a
    /// `LinkedHashMap`)
    static std::vector<std::pair<int, Light*> > activeLights;
    static Light* defaultLight();

    static double scale;
    static unsigned int vao;
    static unsigned int vboPositions;
    static unsigned int vboColors;
    static unsigned int program;

    static bool initIfNeeded();
    static unsigned int compileShader(unsigned int type, const char* source);
    static double calculateHalfAxisLength(const Light* light, Camera* camera, int viewportWidth, int viewportHeight);
    static Vector3Dd mapPatternPointToWorld(
        const Vector3Dd& point,
        const Vector3Dd& center,
        const Vector3Dd& right,
        const Vector3Dd& up,
        double worldSize);
    static void drawLines(const Matrix4x4d& mvp,
                          const java::ArrayList<float>& positions,
                          const java::ArrayList<float>& colors,
                          float lineWidth);
    static void drawCross(const Light* light, Camera* camera, bool selected);
    static void drawOmniBillboard(const Light* light, Camera* camera,
                                  bool selected);
};

#endif
