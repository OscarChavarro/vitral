//= References:                                                             =
//= [WEIL1977] Weiler, Kevin. Atherton, Peter. "Hidden surface removal      =
//=          using polygon area sorting". SIGGRAPH proceedings, 1977.       =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

#ifndef __PROJECTED_SURFACES_VIEW_BUILDER__
#define __PROJECTED_SURFACES_VIEW_BUILDER__

#include "java/util/ArrayList.h"
#include "render/BrepExportCache.h"
#include "render/ProjectedPolygon.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/render/TraceWorkspace.h"
#include "vsdk/toolkit/render/hiddenSurface/HiddenSurfaceRenderer.h"
#include "vsdk/toolkit/render/shaders/FlatShader.h"

class Camera;
class CameraState;
class Light;
class PolyhedralBoundedSolid;
class Scene;
class SimpleBody;
class Viewport;
class _PolyhedralBoundedSolidFace;

/**
Builds the drawing of a viewport as projected 2D polygons, computed entirely
in the processor: the visible surfaces of the solids, solved by the hidden
surface removal of Weiler and Atherton [WEIL1977] (`HiddenSurfaceRenderer`),
whose output is a set of polygons that do not overlap on the view. The
result is a list of `ProjectedPolygon`s, so this class does not depend on the
2D technology that finally draws them (see `render/framebuffer` and
`render/xlib`), as `ProjectedLinesViewBuilder` does for lines.

Only the bodies that are (or can be exported to) a `PolyhedralBoundedSolid`
are presented, as [WEIL1977] works on polygons. Each visible polygon is
filled with the color of its face (constant shading [FOLE1992].16.2.1, by
`FlatShader`) under the lights of the scene, evaluated at the center of the
face. With shadows (see `RendererConfiguration::isWithShadowsSet`, `F10`),
the visible polygons are divided by the shadows the solids cast, computed
by the same algorithm from each light [WEIL1977].218, and each part is
shaded only with the lights that reach it. As the polygons have no depth, the annotations of the
view (selection, lights, gizmos) are drawn over them, without depth test, as
over a raytraced image.

The polygons of a viewport are kept, and given again, while its camera, its
size, the shadows option and the content of the scene (see `DrawingArea::getContentGeneration`) do
not change: the hidden surface removal of a complex scene takes from tens to
hundreds of milliseconds, and the frame is drawn again when only overlays
change.
*/
class ProjectedSurfacesViewBuilder {
private:
    class CachedView {
    public:
        /// C++ port: the key of the cache (Java's weak map key)
        Viewport* viewport;
        CameraState* cameraState;
        int width;
        int height;
        long long generation;
        bool withShadows;
        java::ArrayList<ProjectedPolygon*> polygons;

        CachedView();
        ~CachedView();
    };

    /// C++ port: an entry of the colors of the faces, by face, body and
    /// lights
    class ColorEntry {
    public:
        java::ArrayList<const void*> key;
        ColorRgb color;
    };

    static const ColorRgb NO_LIGHT_COLOR;

    BrepExportCache brepCache;
    java::ArrayList<CachedView*> cache;
    FlatShader shader;
    TraceWorkspace workspace;
    /// C++ port: the bodies standing for volumes exported to boundary
    /// representations, for one view
    java::ArrayList<SimpleBody*> surrogates;

    ProjectedSurfacesViewBuilder(const ProjectedSurfacesViewBuilder& other);
    ProjectedSurfacesViewBuilder& operator=(const ProjectedSurfacesViewBuilder& other);

    ColorRgb shadeFace(HiddenSurfaceRenderer::VisiblePolygon* polygon,
                       Scene* scene, Camera* camera,
                       java::ArrayList<Light*>& lights);
    static Vector3Dd faceCenter(_PolyhedralBoundedSolidFace* face,
                                SimpleBody* body);
    static double clamp(double value);
    SimpleBody* brepBody(SimpleBody* body, PolyhedralBoundedSolid* brep);

public:
    ProjectedSurfacesViewBuilder() {}
    ~ProjectedSurfacesViewBuilder();

    /**
    Builds the visible polygons presenting the solids of the scene in a
    viewport.
    @param scene scene to present
    @param viewport viewport whose active camera is used; its camera must
    have its viewport size updated
    @param width width of the viewport, in pixels
    @param height height of the viewport, in pixels
    @param generation content generation of the scene (see
    `DrawingArea::getContentGeneration`)
    @param withShadows true to cast the shadows of the solids
    @return the polygons to fill, which do not overlap (owned by this
    builder, valid until its next call)
    */
    const java::ArrayList<ProjectedPolygon*>& buildView(Scene* scene,
        Viewport* viewport, int width, int height, long long generation,
        bool withShadows);
};

#endif
