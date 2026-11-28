//= References:                                                             =
//= [WEIL1977] Weiler, Kevin. Atherton, Peter. "Hidden surface removal      =
//=          using polygon area sorting". SIGGRAPH proceedings, 1977.       =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

#ifndef __HIDDEN_SURFACE_RENDERER__
#define __HIDDEN_SURFACE_RENDERER__

// First: gcc 2.7.2 fails if _DoubleLinkedListNode is defined before it
// (see WeilerAthertonPolygonClipper.h)
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/genericPolygonClipper/WeilerAthertonPolygonClipper.h"
#include "java/util/ArrayList.h"
#include "java/util/TreeMap.h"
#include "vsdk/toolkit/common/Entity.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector4Dd.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.h"
#include "vsdk/toolkit/render/RenderingElement.h"
class Camera;
class InfinitePlane;
class Light;
class SimpleBody;
class _Polygon2DContour;
class _PolyhedralBoundedSolidFace;

/**
C++ port note: the `double[]` arrays of the Java version (points {x, y},
projected points {x, y, depth}, edges {ax, ay, bx, by}, ...) are kept by
value in this fixed size tuple, so the lists of them need no allocation.
*/
class _WeilerAthertonTuple
{
public:
    double v[4];

    static _WeilerAthertonTuple of(double a, double b, double c = 0.0,
                                   double d = 0.0);
};

/**
An area of the x-y plane of the viewing space (a polygon with holes, maybe
with several main contours), with the measures used to sort and to clip it.

C++ port note: the polygon is not owned by the area; every object of a
solution is owned by the `_WeilerAthertonArena` of the call (see
`HiddenSurfaceRenderer`), as the Java objects are shared freely.
*/
class _WeilerAthertonArea : public RenderingElement
{
public:
    Polygon2D* polygon;
    double minX;
    double minY;
    double maxX;
    double maxY;
    /// Area enclosed (holes discounted)
    double area;
    /// Length of all the contours
    double perimeter;

    _WeilerAthertonArea(Polygon2D* polygon);

    BOOLEAN overlapsBox(const _WeilerAthertonArea* other) const;
};

/**
Original copy of an input polygon of [WEIL1977]: a front face of a solid,
transformed and clipped to the viewing space, with its plane in that space.
As noted in [WEIL1977].216, the clip template is always this copy of the
original polygon, not the pieces left of it after previous clips.
*/
class _WeilerAthertonSourcePolygon : public RenderingElement
{
public:
    /// Area of the polygon on the x-y plane of the viewing space
    _WeilerAthertonArea* templateArea;
    /// Plane of the polygon in the viewing space, as depth =
    /// depthSlopeX * x + depthSlopeY * y + depthOffset,
    /// where a smaller depth is closer to the observer
    double depthSlopeX;
    double depthSlopeY;
    double depthOffset;
    /// Key of the preliminary depth sort: the nearest depth of the polygon
    double nearestDepth;
    SimpleBody* body;
    _PolyhedralBoundedSolidFace* face;
    InfinitePlane* worldPlane;
    /// False if the near, far or window clips of the view removed part of
    /// the face
    BOOLEAN complete;
    /// Polygon of the whole view this one was clipped from by the screen
    /// subdivision (itself if it was not clipped)
    _WeilerAthertonSourcePolygon* original;

    _WeilerAthertonSourcePolygon();

    double depthAt(double x, double y) const;
};

/**
Part of a `_WeilerAthertonSourcePolygon` left by the x-y clips: the
elements of the inside and outside lists of [WEIL1977].216, figure 2.
*/
class _WeilerAthertonPiece : public RenderingElement
{
public:
    _WeilerAthertonArea* area;
    _WeilerAthertonSourcePolygon* source;

    _WeilerAthertonPiece(_WeilerAthertonArea* area,
                         _WeilerAthertonSourcePolygon* source);
};

/**
Areas of a face of a solid reached by a light (the "surface detail of a
lighter shade" of [WEIL1977].218), in world coordinates: the visible
polygons of a view from the light, transformed back to the plane of their
source face.
*/
class _WeilerAthertonLitFace : public RenderingElement
{
public:
    /// The whole face is visible from the light
    BOOLEAN whole;
    /// Otherwise, the lit regions: each one a list of contours (main
    /// contours and holes), each contour a list of points
    ArrayList<ArrayList<ArrayList<Vector3Dd> > > regions;

    _WeilerAthertonLitFace();
};

/**
Uniform grid over the edges of a polygon, telling quickly whether a point is
near its border: the verification of the clips tests it at a number of
points that grows with the number of vertices.
*/
class _WeilerAthertonEdgeGrid : public RenderingElement
{
private:
    double tolerance;
    double minX;
    double minY;
    double cellWidth;
    double cellHeight;
    long int columns;
    long int rows;
    /// For each cell, the edges {ax, ay, bx, by} near it (NULL if none)
    ArrayList<ArrayList<_WeilerAthertonTuple>*> cells;

    long int column(double x) const;
    long int row(double y) const;

public:
    _WeilerAthertonEdgeGrid(Polygon2D* polygon, double tolerance);
    virtual ~_WeilerAthertonEdgeGrid();

    /**
    @return true if the point is at a distance from the border of the
    polygon not greater than the tolerance
    */
    BOOLEAN isNearBorder(double x, double y) const;
};

/**
Part of a visible area of a face, with the lights that reach it.

C++ port note: the list of lights is kept by value (the Java version
shares the same unmodified list among the parts lit by the same lights).
*/
class _WeilerAthertonShadedPart : public RenderingElement
{
public:
    _WeilerAthertonArea* area;
    ArrayList<Light*> lights;

    _WeilerAthertonShadedPart(_WeilerAthertonArea* area,
                              const ArrayList<Light*>& lights);
};

/**
C++ port note: owner of the objects created while solving a view (the Java
version leaves them to the garbage collector). The objects are shared
freely by the lists of the algorithm, so they are deleted all together
when the public call that created them ends.
*/
class _WeilerAthertonArena
{
private:
    ArrayList<Entity*> entities;
    ArrayList<RenderingElement*> elements;

public:
    _WeilerAthertonArena();
    ~_WeilerAthertonArena();

    Entity* add(Entity* entity);
    RenderingElement* add(RenderingElement* element);
    /// Gives to another arena the objects of this one that are in the given
    /// lists (as addresses, sorted in increasing order), so they outlive it
    void moveInto(_WeilerAthertonArena* target,
                  const ArrayList<unsigned long>& keptAddresses);
};

/// C++ port note: Java's Map<_PolyhedralBoundedSolidFace,
/// _WeilerAthertonLitFace> (an IdentityHashMap)
typedef TreeMap<_PolyhedralBoundedSolidFace*, _WeilerAthertonLitFace*>
    _WeilerAthertonLitFacesOfBody;
/// C++ port note: Java's Map<SimpleBody, Map<_PolyhedralBoundedSolidFace,
/// _WeilerAthertonLitFace>> (an IdentityHashMap)
typedef TreeMap<SimpleBody*, _WeilerAthertonLitFacesOfBody*>
    _WeilerAthertonLitFaces;

/**
This class implements the hidden surface removal algorithm by polygon area
sorting of Weiler and Atherton [WEIL1977]: the hidden surface problem is
solved in polygon shaped windows of the image, taken from the input
polygons themselves, using the general 2D polygon clipper of the same paper
(`WeilerAthertonPolygonClipper`), which clips a concave polygon with holes
to the area of another concave polygon with holes.

The input are the faces of a set of `PolyhedralBoundedSolid`s. As required
by [WEIL1977].214, they are first transformed to the viewing space, where
the X and Y axes are parallel to the display surface and Z is parallel to
the line of sight: here, the normalized device coordinates of the camera
(with x and y scaled by `SCALE`), after clipping in homogeneous coordinates
[FOLE1992].6 against the near and far planes. The projective
transformation keeps planes as planes, so each face keeps a planar depth
function over its x-y area, and the depth of any point given by the 2D
clipper (that only computes x and y) is recovered from the plane of its
source face ("depth information can be preserved in all output",
[WEIL1977].217). The faces facing away from the observer are removed
beforehand (the "backplane removal" of [WEIL1977].221), and the faces are
clipped to the window of the view (the "viewbox clipping") with the same 2D
clipper.

The four steps of [WEIL1977].215 follow:
  a) A preliminary rough depth sort, by the nearest depth of each polygon.
  b) An x-y polygon area sort to the area of the currently most forward
     polygon: the remaining polygons are clipped to it, giving an inside
     list and an outside list (figure 2).
  c) A depth sort by removal of the polygons of the inside list located
     behind the current clip polygon.
  d) A conclusive depth sort by recursive subdivision when a polygon of the
     inside list is in front of the clip polygon, using it (its original
     copy) as the clip polygon and the inside list as input (figure 3). A
     stack of the polygons used as clip polygons detects the cyclic overlap
     (figure 4a), where no additional recursion is needed. Then the inside
     list is output and the process repeats with the outside list.

The output never overlaps on the x-y plane, as each visible area had all
the polygons behind it removed, so it solves the hidden lines as well as the
hidden surfaces [WEIL1977].215.

As the number of sorting steps can grow exponentially with the number of
polygons, the view is first divided by the screen subdivision of
[WEIL1977].219 in windows with a bounded number of polygons, each one solved
separately (see `solveWindow`). Visible areas of a face crossing several
windows are output in several pieces.

Two additions to [WEIL1977] make it usable with the solids of a scene:
  - Polygons that penetrate each other are not considered by [WEIL1977]
    (between two polygons in a window, one is entirely in front of the
    other). As solids of a scene may intersect, a piece of the inside list
    that lies in front of the clip polygon in part, and behind it in the
    other part, is divided by the projection of the intersection line of
    both planes (where their depth functions are equal), and only its front
    part is kept, so the steps c) and d) work as in [WEIL1977] with it.
  - The pieces have most of their vertices on the borders of previous clip
    polygons, the degenerate case of the clipper where "care is taken in
    placement of intersections where the subject and clip polygon contours
    are identical" [WEIL1977].218. Each clip is verified (the inside and
    outside areas must add up to the area of the subject, and sample points
    of the polygons must fall in the right part) and, if it fails, it is
    repeated with the clip polygon moved a distance far under the size of a
    pixel, which turns the degenerate contacts into regular ones. The strips
    thinner than `SLIVER_WIDTH` that such moves can leave are discarded. If
    every attempt fails, a convex clip polygon (the faces of boxes and the
    facets of spheres, the half planes of the penetrations) is solved by the
    half planes of its edges, as the Sutherland-Hodgman clipper that
    [WEIL1977].217 cites, and a concave one by the half planes of the convex
    trapezoids it is decomposed in.

Shadows are cast as in [WEIL1977].218 ("Shadowing"), taking advantage of the
output having the same form as the input: a hidden surface removed view of
the scene is produced from the position of each light, and its visible
polygons, transformed back to the original space, are the lit areas of their
source faces (surface detail of a lighter shade, [WEIL1977].219). Then the
hidden surface removed view from the observer is produced, and each visible
area is clipped with the same polygon clipper to the lit areas of its face,
for each light, so its parts know which lights reach them. A point light is
seen with one perspective view enclosing the scene or, if it is among the
solids, with the six views of the faces of a cube around it (slightly
overlapped); a directional light with an orthogonal view along its
direction. Ambient lights cast no
shadows.

C++ port notes: the objects created during a call are owned by a
`_WeilerAthertonArena` (see `arena`), deleted when the public call ends, so
a call is not reentrant from several threads. The returned visible polygons
are owned by the caller (see `VisiblePolygon`).
*/
class HiddenSurfaceRenderer : public RenderingElement
{
public:
    /**
    A visible area of a face of a solid, output of the algorithm.

    C++ port note: it owns its area, its copy of the plane and its list of
    lights; the body, face and lights are those of the input.
    */
    class VisiblePolygon : public RenderingElement
    {
    private:
        VisiblePolygon(const VisiblePolygon& other);
        VisiblePolygon& operator=(const VisiblePolygon& other);

    public:
        /// Visible area, on the x-y plane of the normalized device coordinates
        /// of the camera (from <-1, -1> at the lower left corner of the view
        /// to <1, 1> at its upper right corner). Main contours are
        /// counterclockwise and holes clockwise.
        Polygon2D* area;
        /// Body the face belongs to
        SimpleBody* body;
        /// Face whose visible area is given
        _PolyhedralBoundedSolidFace* face;
        /// Plane of the face in world coordinates, with an outward normal
        InfinitePlane* worldPlane;
        /// Lights reaching the area (ambient lights included), or null if
        /// shadows were not computed
        ArrayList<Light*>* lights;

        VisiblePolygon(Polygon2D* area, _WeilerAthertonSourcePolygon* source,
                       const ArrayList<Light*>* lights);
        virtual ~VisiblePolygon();
    };

private:
    WeilerAthertonPolygonClipper clipper;
    ArrayList<_WeilerAthertonSourcePolygon*> clipStack;
    /// Visible polygons, in the viewing space
    ArrayList<_WeilerAthertonPiece*> output;

    /// C++ port note: owner of the objects of the current public call
    static _WeilerAthertonArena* arena;

    HiddenSurfaceRenderer();

    static Polygon2D* newPolygon();
    static Polygon2D* newEmptyPolygon();
    static _WeilerAthertonArea* newArea(Polygon2D* polygon);
    static _WeilerAthertonPiece* newPiece(_WeilerAthertonArea* area,
        _WeilerAthertonSourcePolygon* source);

    static HiddenSurfaceRenderer* solve(ArrayList<SimpleBody*>& solids,
                                        Camera* camera);

    //= Screen subdivision ================================================
    void solveWindow(double minX, double minY, double maxX, double maxY,
                     const ArrayList<_WeilerAthertonSourcePolygon*>& sources,
                     int depth);
    static _WeilerAthertonSourcePolygon* clippedToWindow(
        _WeilerAthertonSourcePolygon* source, double minX, double minY,
        double maxX, double maxY);
    void solveArea(const ArrayList<_WeilerAthertonSourcePolygon*>& inSources);

    //= Shadows ===========================================================
    static _WeilerAthertonLitFaces* litFacesFromLight(
        ArrayList<SimpleBody*>& solids, Light* light, const double bounds[4]);
    static BOOLEAN isInsideASolid(ArrayList<SimpleBody*>& solids,
                                  const Vector3Dd& p);
    static BOOLEAN unprojectToPlane(const Matrix4x4d& inverseProjection,
                                    double x, double y,
                                    const InfinitePlane* plane,
                                    Vector3Dd& out);
    static ArrayList<Camera*> lightCameras(Light* light,
                                           const double bounds[4]);
    static Camera* lightCamera(const Vector3Dd& position,
                               const Vector3Dd& front);
    static BOOLEAN boundingSphere(ArrayList<SimpleBody*>& solids,
                                  double bounds[4]);
    ArrayList<_WeilerAthertonShadedPart*> divideByLitArea(
        const ArrayList<_WeilerAthertonShadedPart*>& parts,
        _WeilerAthertonLitFace* lit, Light* light,
        const Matrix4x4d& projection);
    static ArrayList<Light*> withLight(const ArrayList<Light*>& lights,
                                       Light* light);
    static _WeilerAthertonArea* projectRegion(
        const ArrayList<ArrayList<Vector3Dd> >& region,
        const Matrix4x4d& projection);
    static Polygon2D* toNormalizedDeviceCoordinates(Polygon2D* polygon);

    //= Hidden surface algorithm ==========================================
    ArrayList<_WeilerAthertonPiece*> subdivide(
        _WeilerAthertonSourcePolygon* clipPolygon,
        const ArrayList<_WeilerAthertonPiece*>& polygonList);
    void clipPiece(_WeilerAthertonSourcePolygon* clipPolygon,
                   _WeilerAthertonPiece* piece,
                   ArrayList<_WeilerAthertonPiece*>& insideList,
                   ArrayList<_WeilerAthertonPiece*>& outsideList);
    ArrayList<_WeilerAthertonPiece*> removePolygonsBehind(
        _WeilerAthertonSourcePolygon* clipPolygon,
        const ArrayList<_WeilerAthertonPiece*>& insideList);
    static int classifyDepth(_WeilerAthertonSourcePolygon* clipPolygon,
                             _WeilerAthertonPiece* piece);
    void addFrontPart(_WeilerAthertonSourcePolygon* clipPolygon,
                      _WeilerAthertonPiece* piece,
                      ArrayList<_WeilerAthertonPiece*>& remaining);
    void display(_WeilerAthertonPiece* piece);

    //= Verified clips ====================================================
    void verifiedClip(_WeilerAthertonArea* clip,
                      _WeilerAthertonArea* subject,
                      ArrayList<_WeilerAthertonArea*>& inside,
                      ArrayList<_WeilerAthertonArea*>& outside);
    static ArrayList<Polygon2D*> trapezoids(Polygon2D* polygon);
    static void addDistinct(ArrayList<_WeilerAthertonTuple>& points,
                            double x, double y);
    static void clipByConvexPolygon(Polygon2D* clip,
                                    _WeilerAthertonArea* subject,
                                    ArrayList<_WeilerAthertonArea*>& inside,
                                    ArrayList<_WeilerAthertonArea*>& outside);
    static Polygon2D* clipByHalfPlane(Polygon2D* polygon, double a,
                                      double b, double c);
    static BOOLEAN isConvex(Polygon2D* polygon);
    static BOOLEAN isConsistentClip(_WeilerAthertonArea* clip,
                                    Polygon2D* clipPolygon,
                                    _WeilerAthertonArea* subject,
                                    Polygon2D* inside,
                                    Polygon2D* outside,
                                    double shift);
    static BOOLEAN isOnExpectedSide(Polygon2D* part,
                                    const _WeilerAthertonEdgeGrid* border,
                                    double x, double y,
                                    BOOLEAN expected);
    static void samplePoints(Polygon2D* polygon,
                             ArrayList<_WeilerAthertonTuple>& points);
    static void addWithoutSlivers(ArrayList<_WeilerAthertonArea*>& list,
                                  Polygon2D* polygon);

    //= Input polygons ====================================================
    ArrayList<_WeilerAthertonSourcePolygon*> buildInputPolygons(
        ArrayList<SimpleBody*>& solids, Camera* camera);
    _WeilerAthertonSourcePolygon* buildInputPolygon(
        _PolyhedralBoundedSolidFace* face, SimpleBody* body,
        const Matrix4x4d& transform, Camera* camera,
        const Matrix4x4d& projection, double depthSign,
        _WeilerAthertonArea* window);
    static ArrayList<Vector4Dd> clipAgainstPlane(
        const ArrayList<Vector4Dd>& loop, double side);
    static BOOLEAN fitDepthPlane(const ArrayList<_WeilerAthertonTuple>& loop,
                                 _WeilerAthertonSourcePolygon* source);
    static double depthSign(Camera* camera, const Matrix4x4d& projection);
    static BOOLEAN isFrontFace(const InfinitePlane* worldPlane,
                               Camera* camera);
    static InfinitePlane* worldContainingPlane(
        _PolyhedralBoundedSolidFace* face, SimpleBody* body,
        const Matrix4x4d& transform);

    //= 2D polygon utilities ==============================================
    static void addLoop(Polygon2D* polygon,
                        const ArrayList<_WeilerAthertonTuple>& loop);
    static void copyLoop(Polygon2D* polygon, _Polygon2DContour* loop);
    static Polygon2D* translated(Polygon2D* polygon, double dx, double dy);
    static _WeilerAthertonArea* merged(
        const ArrayList<_WeilerAthertonArea*>& areas);
    static double signedArea(const ArrayList<_WeilerAthertonTuple>& loop);
    static BOOLEAN containsInLoop(_Polygon2DContour* loop, double x,
                                  double y);

public:
    // C++ port note: package-private in the Java version (used by the
    // classes of this file)
    static double signedArea(_Polygon2DContour* loop);
    static double perimeter(_Polygon2DContour* loop);
    static double perimeter(Polygon2D* polygon);
    static double enclosedArea(Polygon2D* polygon);
    static Polygon2D* withoutDegenerateContours(Polygon2D* polygon);
    static BOOLEAN contains(Polygon2D* polygon, double x, double y);
    static void boundingBox(Polygon2D* polygon, double box[4]);

    /**
    Solves the visible surfaces of a set of solids seen from a camera.
    @param solids bodies whose geometry is a `PolyhedralBoundedSolid` (other
    bodies are ignored)
    @param camera camera of the view; its viewport size must be already
    updated (it gives the aspect ratio)
    @return the visible areas of the faces of the solids, which do not
    overlap each other on the view (owned by the caller)
    */
    static ArrayList<VisiblePolygon*> executeWeilerAthertonAlgorithm(
        ArrayList<SimpleBody*>& solids, Camera* camera);

    /**
    Solves the visible surfaces of a set of solids seen from a camera, with
    the shadows cast by a set of lights, as in [WEIL1977].218 (see the class
    documentation).
    @param solids bodies whose geometry is a `PolyhedralBoundedSolid` (other
    bodies are ignored, and cast no shadows)
    @param camera camera of the view; its viewport size must be already
    updated (it gives the aspect ratio)
    @param lights lights of the scene
    @return the visible areas of the faces of the solids, which do not
    overlap each other on the view, each one with the lights that reach it
    (owned by the caller)
    */
    static ArrayList<VisiblePolygon*> executeWeilerAthertonAlgorithm(
        ArrayList<SimpleBody*>& solids, Camera* camera,
        ArrayList<Light*>& lights);
};

#endif
