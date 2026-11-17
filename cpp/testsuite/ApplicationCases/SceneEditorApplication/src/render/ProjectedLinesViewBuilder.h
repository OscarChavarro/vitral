//= References:                                                             =
//= [APPE1967] Appel, Arthur. "The notion of quantitative invisibility and  =
//=          the machine rendering of solids". Proceedings, ACM National    =
//=          Meeting 1967.                                                  =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

#ifndef __PROJECTED_LINES_VIEW_BUILDER__
#define __PROJECTED_LINES_VIEW_BUILDER__

#include "java/util/ArrayList.h"
#include "render/BrepExportCache.h"
#include "render/ProjectedLineLayer.h"

class BodyEditFeedbackProvider;
class Calligraphic3DBuffer;
class Camera;
class ClipSpaceLineProjector;
class Geometry;
class Matrix4x4d;
class PolyhedralBoundedSolid;
class RendererConfiguration;
class Scene;
class SimpleBody;
class SimpleMaterial;
class Viewport;

/**
Builds the drawing of a viewport as projected 2D lines, computed entirely
in the processor: the "calligraphic" approach of vector displays and
plotters [FOLE1992].1, where a 3D scene is presented without any 3D graphics
API. The result is a list of `ProjectedLineLayer`s, so this class does not
depend on the 2D technology that finally draws them (see `render/xlib`).
Port of the Java `render.ProjectedLinesViewBuilder`.

The bodies follow the `RendererConfiguration` of the viewport:
  - With surfaces, the bodies that are (or can be exported to) a
    `PolyhedralBoundedSolid` are drawn with hidden line removal, by the
    quantitative invisibility algorithm of [APPE1967] (`HiddenLineRenderer`):
    contour lines are thicker than the other visible lines, and hidden lines
    are dashed if wires are also asked for. The occlusion is computed among
    all those solids.
  - Curves and surfaces are exported to 3D lines
    (`GeometryCalligraphicExporter`: the surfaces through a mesh, whose
    edges are the lines). With surfaces they are "free lines" of the hidden
    line algorithm: they are hidden by the solids (dashed if wires are asked
    for), but do not hide anything. The reference grid is also drawn as free
    lines. An infinite plane is presented as a square of it, centered at the
    point of the plane viewed by the camera.
  - Without surfaces, every body is a plain wireframe with no hidden line
    removal (whether wires are asked for or not): the edges of its boundary
    representation, or its lines as above.
  - Bodies with none of them are represented by their bounding box.
  - Points, bounding volumes and the selection corners of selected bodies
    are added over them.

The light gizmos, the visual debug entities and the feedback of the body
editor are also presented as lines, over the bodies.
*/
class ProjectedLinesViewBuilder {
private:
    /**
    Lines in world coordinates that are not edges of the solids (the
    reference grid, the curves), drawn with one color.
    */
    struct FreeLineSet;

    BrepExportCache brepCache;
    /// Bodies standing for volumes exported to boundary representations
    java::ArrayList<SimpleBody*> surrogates;

    ProjectedLinesViewBuilder(const ProjectedLinesViewBuilder& other);
    ProjectedLinesViewBuilder& operator=(const ProjectedLinesViewBuilder& other);

    void addGrid(java::ArrayList<FreeLineSet*>& freeLines, Camera* camera);
    void addBodies(java::ArrayList<ProjectedLineLayer*>& layers,
                   java::ArrayList<FreeLineSet*>& freeLines,
                   const ClipSpaceLineProjector& projector, Scene* scene,
                   Camera* camera, RendererConfiguration* quality);
    static void addFreeLines(java::ArrayList<FreeLineSet*>& freeLines,
                             Calligraphic3DBuffer* local, SimpleBody* body);
    static void addFreeLinesWithoutHiding(
        java::ArrayList<ProjectedLineLayer*>& layers, long position,
        java::ArrayList<FreeLineSet*>& freeLines,
        const ClipSpaceLineProjector& projector);
    void addHiddenLineDrawing(java::ArrayList<ProjectedLineLayer*>& layers,
                              java::ArrayList<SimpleBody*>& solids,
                              java::ArrayList<FreeLineSet*>& freeLines,
                              Camera* camera, bool withHiddenLines);
    SimpleBody* brepBody(SimpleBody* body, PolyhedralBoundedSolid* brep);
    static Calligraphic3DBuffer* exportToLines(Geometry* geometry,
        const Matrix4x4d& transform, Camera* camera);
    void addWireframe(ProjectedLineLayer* layer,
                      const ClipSpaceLineProjector& projector, Camera* camera,
                      Geometry* geometry, PolyhedralBoundedSolid* brep,
                      const Matrix4x4d& transform);
    void addPoints(ProjectedLineLayer* layer,
                   const ClipSpaceLineProjector& projector,
                   Geometry* geometry, PolyhedralBoundedSolid* brep,
                   const Matrix4x4d& transform);
    void addAnnotations(java::ArrayList<ProjectedLineLayer*>& layers,
                        const ClipSpaceLineProjector& projector,
                        Camera* camera, Scene* scene,
                        RendererConfiguration* quality,
                        BodyEditFeedbackProvider* editor);
    static void addSelectionCorners(ProjectedLineLayer* layer,
                                    const ClipSpaceLineProjector& projector,
                                    const double* minmax,
                                    const Matrix4x4d& transform);
    static void addBox(ProjectedLineLayer* layer,
                       const ClipSpaceLineProjector& projector,
                       const double* minmax, const Matrix4x4d& transform);
    void addDebugEntities(java::ArrayList<ProjectedLineLayer*>& layers,
                          const ClipSpaceLineProjector& projector,
                          Camera* camera, Scene* scene);
    static void addLights(java::ArrayList<ProjectedLineLayer*>& layers,
                          const ClipSpaceLineProjector& projector,
                          Scene* scene);
    static void addIfNotEmpty(java::ArrayList<ProjectedLineLayer*>& layers,
                              ProjectedLineLayer* layer);

public:
    ProjectedLinesViewBuilder() {}
    ~ProjectedLinesViewBuilder();

    /**
    Builds the lines presenting the scene in a viewport.
    @param scene scene to present
    @param viewport viewport whose active camera, rendering configuration
    and grid are used; its camera must have its viewport size updated
    @param editor editor of the selected body, whose feedback is presented,
    or null if there is none
    @param withBodies true to present the bodies and the grid (hidden lines
    render mode), false to present only the annotations that go over an
    image of the scene computed by other means (raytracing render mode)
    @param outLayers receives the layers, in drawing order (owned by the
    caller: see `ProjectedLineLayer::deleteAll`)
    */
    void buildView(Scene* scene, Viewport* viewport,
                   BodyEditFeedbackProvider* editor, bool withBodies,
                   java::ArrayList<ProjectedLineLayer*>& outLayers);
};

#endif
