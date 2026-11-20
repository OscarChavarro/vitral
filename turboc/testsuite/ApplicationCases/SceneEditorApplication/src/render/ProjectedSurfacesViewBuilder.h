//= References:                                                             =
//= [WEIL1977] Weiler, Kevin. Atherton, Peter. "Hidden surface removal      =
//=          using polygon area sorting". SIGGRAPH proceedings, 1977.       =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

#ifndef __PROJ_SURFACES_VIEW_BUILDER__
#define __PROJ_SURFACES_VIEW_BUILDER__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/render/TraceWorkspace.h"
#include "vsdk/toolkit/render/shaders/FlatShader.h"
#include "vsdk/toolkit/render/hiddenSurface/HiddenSurfaceRenderer.h"
#include "render/ProjectedPolygon.h"
#include "model/history/CameraState.h"

class BrepExportCache;
class Camera;
class Light;
class PolyhedralBoundedSolid;
class Scene;
class SimpleBody;
class Viewport;
class _PolyhedralBoundedSolidFace;

/// Polygons kept for a viewport (see `ProjectedSurfacesViewBuilder`)
class _ProjectedSurfacesCachedView
{
public:
    Viewport* viewport;
    CameraState* cameraState;
    int width;
    int height;
    long generation;
    BOOLEAN withShadows;
    ArrayList<ProjectedPolygon*> polygons;

    _ProjectedSurfacesCachedView();
    ~_ProjectedSurfacesCachedView();
};

/// A color already computed for a face, a body and a set of lights
class _ProjectedSurfacesColorKey
{
public:
    _PolyhedralBoundedSolidFace* face;
    SimpleBody* body;
    ArrayList<Light*> lights;
    ColorRgb color;
};

/**
Builds the drawing of a viewport as projected 2D polygons, computed entirely
in the processor: the visible surfaces of the solids, solved by the hidden
surface removal of Weiler and Atherton [WEIL1977] (`HiddenSurfaceRenderer`),
whose output is a set of polygons that do not overlap on the view. The
result is a list of `ProjectedPolygon`s, so this class does not depend on the
2D technology that finally draws them (see `render/xlib`), as
`ProjectedLinesViewBuilder` does for lines.

Only the bodies that are (or can be exported to) a `PolyhedralBoundedSolid`
are presented, as [WEIL1977] works on polygons. Each visible polygon is
filled with the color of its face (constant shading [FOLE1992].16.2.1, by
`FlatShader`) under the lights of the scene, evaluated at the center of the
face. With shadows (see `RendererConfiguration.isWithShadowsSet`, `F10`),
the visible polygons are divided by the shadows the solids cast, computed
by the same algorithm from each light [WEIL1977].218, and each part is
shaded only with the lights that reach it. As the polygons have no depth, the annotations of the
view (selection, lights, gizmos) are drawn over them, without depth test, as
over a raytraced image.

The polygons of a viewport are kept, and given again, while its camera, its
size, the shadows option and the content of the scene (see `DrawingArea.getContentGeneration`) do
not change: the hidden surface removal of a complex scene takes from tens to
hundreds of milliseconds, and the frame is drawn again when only overlays
change.

C++ port note: Java keeps the views in a `WeakHashMap` by viewport; here
they are kept in a list, until the builder is destroyed. The polygons given
are owned by the builder.
*/
class ProjectedSurfacesViewBuilder
{
private:
    BrepExportCache* brepCache;
    ArrayList<_ProjectedSurfacesCachedView*> cache;
    FlatShader shader;
    TraceWorkspace workspace;

    ProjectedSurfacesViewBuilder(const ProjectedSurfacesViewBuilder& other);
    ProjectedSurfacesViewBuilder& operator=(
        const ProjectedSurfacesViewBuilder& other);

    ColorRgb shadeFace(HiddenSurfaceRenderer::VisiblePolygon* polygon,
                       Scene* scene, Camera* camera,
                       ArrayList<Light*>& lights);
    static Vector3Dd faceCenter(_PolyhedralBoundedSolidFace* face,
                                SimpleBody* body);
    static double clamp(double value);
    static SimpleBody* brepBody(SimpleBody* body, PolyhedralBoundedSolid* brep);

public:
    ProjectedSurfacesViewBuilder();
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
    `DrawingArea.getContentGeneration`)
    @param withShadows true to cast the shadows of the solids
    @return the polygons to fill, which do not overlap (owned by the builder)
    */
    const ArrayList<ProjectedPolygon*>& buildView(Scene* scene,
        Viewport* viewport, int width, int height, long generation,
        BOOLEAN withShadows);
};

#endif
