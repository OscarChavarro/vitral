//= References:                                                             =
//= [APPE1967] Appel, Arthur. "The notion of quantitative invisibility and  =
//=          the machine rendering of solids". Proceedings, ACM National    =
//=          meeting 1967.                                                  =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,          =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

#ifndef __CMR_CTRL_GZM_2D_PAINTER__
#define __CMR_CTRL_GZM_2D_PAINTER__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/media/Calligraphic2DBuffer.h"

class CameraControlGizmo;
class Gizmo2DDrawList;
class SimpleBody;
class ViewportElementScaler;

/**
Presents a `CameraControlGizmo` (a ViewCube) as 2D primitives of a
`Gizmo2DDrawList`, for drawing surfaces without a 3D API, in the HUD area of
the gizmo:
  - The edges of its 27 parts (26 boxes and the compass tube), as boundary
    representations, with the hidden lines removed by the algorithm of Appel
    [APPE1967] (`HiddenLineRenderer::executeAppelAlgorithm`), seen from the
    HUD camera of the gizmo. Only the visible lines are presented (contours
    thicker), as overlay lines, so they are exact.
  - Under them, the faces of the parts that face the viewer, filled with the
    color of each part (the highlight of the region under the cursor, and
    the translucency of an inactive gizmo), sorted with the painter's
    algorithm [FOLE1992].15.5.
  - Over everything, the labels of the faces of the cube that face the
    viewer.

The boundary representations are exported once, and the lines are kept
while the orientation and size of the gizmo do not change, as the hidden
line removal is the expensive part.

Use one painter (and one draw list) for each gizmo, and draw that list after
the one of the other gizmos, as the HUD is over everything.
*/
class CameraControlGizmo2DPainter {
private:
    /// Java's COMPASS_SLICES is `CameraControlGizmo::COMPASS_SLICES`
    enum { BASE_LABEL_FONT_SIZE = 9 };
    static const double MIN_LABEL_VISIBILITY;

    const CameraControlGizmo* cachedGizmo;
    /// Bodies with the boundary representations of the parts of the gizmo
    /// (owned, with their geometries)
    ArrayList<SimpleBody*> brepParts;
    BOOLEAN hasCachedRotation;
    Matrix4x4d cachedRotation;
    int cachedSize;
    Calligraphic2DBuffer contourLines;
    Calligraphic2DBuffer visibleLines;

    void clearBrepParts();

    //= Caches ==========================================================
    void prepareBreps(const CameraControlGizmo* gizmo);

    /**
    [APPE1967] over the parts, only when the orientation or size changed.
    */
    void updateHiddenLines(const CameraControlGizmo* gizmo);

    //= Primitives ======================================================
    static void addLines(Gizmo2DDrawList* list, const CameraControlGizmo* gizmo,
        const Calligraphic2DBuffer& lines, const ColorRgb& color,
        double width);

    /**
    A little of shading, so the sides of the cube seen at once differ: the
    color is dimmed by how much the face turns away from the viewer.
    */
    static double lambert(const Vector3Dd& normal,
        const Vector3Dd& viewDirection);

    /**
    Adds the polygon of a face if it faces the viewer.
    */
    static void addPolygon(Gizmo2DDrawList* list,
        const CameraControlGizmo* gizmo, const Matrix4x4d& projection,
        const Vector3Dd& viewDirection, const Vector3Dd* vertices, int n,
        const Vector3Dd& normal, const ColorRgb& color, double opacity);

    static void addBrepFaces(Gizmo2DDrawList* list,
        const CameraControlGizmo* gizmo, SimpleBody* body,
        const Matrix4x4d& projection, const Vector3Dd& viewDirection,
        const ColorRgb& color, double opacity);

    /**
    The compass is filled with quads computed from its parameters, as its
    caps (annuli) are not simple polygons.
    */
    static void addCompassFaces(Gizmo2DDrawList* list,
        const CameraControlGizmo* gizmo, const Matrix4x4d& projection,
        const Vector3Dd& viewDirection, const ColorRgb& color,
        double opacity);

    // Not copied: owns the boundary representations
    CameraControlGizmo2DPainter(const CameraControlGizmo2DPainter&);
    CameraControlGizmo2DPainter& operator=(const CameraControlGizmo2DPainter&);

public:
    CameraControlGizmo2DPainter();
    virtual ~CameraControlGizmo2DPainter();

    /**
    Adds the primitives that present the gizmo, in pixels of its viewport.
    @param list list receiving the primitives
    @param gizmo gizmo to present, already placed (see
    `CameraControlGizmo::placeInViewport`) and with its HUD camera updated
    @param scaler scaler of the viewport set (sizes of lines and labels), or
    null for base sizes
    */
    void paint(Gizmo2DDrawList* list, const CameraControlGizmo* gizmo,
        const ViewportElementScaler* scaler);
};

#endif
