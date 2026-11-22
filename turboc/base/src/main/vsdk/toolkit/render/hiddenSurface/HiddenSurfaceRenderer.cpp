//= References:                                                             =
//= [WEIL1977] Weiler, Kevin. Atherton, Peter. "Hidden surface removal      =
//=          using polygon area sorting". SIGGRAPH proceedings, 1977.       =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

#include <math.h>

#include "vsdk/toolkit/render/hiddenSurface/HiddenSurfaceRenderer.h"
#include "java/lang/Double.h"
#include "java/lang/Math.h"
#include "java/util/ArrayList.txx"
#include "java/util/Collections.txx"
#include "java/util/Comparator.h"
#include "java/util/TreeMap.txx"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector4Dd.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/element/Vertex2D.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/Polygon2D.h"
#include "vsdk/toolkit/environment/geometry/surface/polygon/_Polygon2DContour.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "vsdk/toolkit/environment/light/DirectionalLight.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/common/Cpp98Support.h"

// C++ port note: the private constants of the Java class are constants of
// this file (Turbo C++ 3.0 has no static member initializers)

/// Units of the viewing space for each unit of the normalized device
/// coordinates: the tolerances of the 2D clipper are absolute (1e-4), so
/// with this scale they are around 1e-5 pixels of a usual view
static const double SCALE = 1.0e4;
/// Tolerance to compare depths in the viewing space (normalized device
/// coordinates, from -1 to 1)
static const double DEPTH_EPSILON = 1.0e-10;
/// Areas (in square units of the viewing space) under this are degenerate
static const double AREA_EPSILON = 1.0e-6;
/// Contours thinner than this (area over half perimeter, in units of the
/// viewing space) are degenerate strips
static const double SLIVER_WIDTH = 1.0e-3;
/// Distance the clip polygon is moved in the first retry of a failed
/// clip, in units of the viewing space; it grows in each retry
static const double RETRY_SHIFT = 1.0e-2;
static const int MAXIMUM_RETRIES = 6;
/// Most polygons solved together in a window of the screen subdivision
static const long int MAXIMUM_POLYGONS_PER_WINDOW = 32;
/// Deepest level of the screen subdivision (4^8 windows at most)
static const int MAXIMUM_SUBDIVISION_DEPTH = 8;
/// Edges shorter than this (in units of the viewing space) are ignored by
/// the clips by half planes
static const double MINIMUM_EDGE_LENGTH = 1.0e-6;

static const int BEHIND = 0;
static const int IN_FRONT = 1;
static const int PENETRATING = 2;

/// Relative margin of the views from the lights around the scene
static const double LIGHT_VIEW_MARGIN = 1.05;
/// Largest half angle (radians) of the scene seen from a point light
/// covered with one view; beyond it, six views of a cube are used
static const double MAXIMUM_LIGHT_HALF_ANGLE = Math::toRadians(55.0);
/// Degrees added to the field of view of the six views of a cube
static const double CUBE_VIEW_OVERLAP = 2.0;
/// Size (in pixels) of the views from the lights: only their aspect
/// ratio matters
static const int LIGHT_VIEW_SIZE = 256;

//= _WeilerAthertonTuple ================================================

_WeilerAthertonTuple
_WeilerAthertonTuple::of(double a, double b, double c, double d)
{
    _WeilerAthertonTuple tuple;
    tuple.v[0] = a;
    tuple.v[1] = b;
    tuple.v[2] = c;
    tuple.v[3] = d;
    return tuple;
}

//= _WeilerAthertonArea =================================================

_WeilerAthertonArea::_WeilerAthertonArea(Polygon2D* polygon)
{
    double box[4];

    HiddenSurfaceRenderer::boundingBox(polygon, box);
    this->polygon = polygon;
    this->minX = box[0];
    this->minY = box[1];
    this->maxX = box[2];
    this->maxY = box[3];
    this->area = HiddenSurfaceRenderer::enclosedArea(polygon);
    this->perimeter = HiddenSurfaceRenderer::perimeter(polygon);
}

BOOLEAN
_WeilerAthertonArea::overlapsBox(const _WeilerAthertonArea* other) const
{
    return maxX >= other->minX && minX <= other->maxX &&
        maxY >= other->minY && minY <= other->maxY;
}

//= _WeilerAthertonSourcePolygon ========================================

_WeilerAthertonSourcePolygon::_WeilerAthertonSourcePolygon() :
    templateArea(NULL), depthSlopeX(0.0), depthSlopeY(0.0), depthOffset(0.0), nearestDepth(0.0), body(NULL),
    face(NULL), worldPlane(NULL), complete(FALSE)
{
    original = this;
}

double
_WeilerAthertonSourcePolygon::depthAt(double x, double y) const
{
    return depthSlopeX * x + depthSlopeY * y + depthOffset;
}

//= _WeilerAthertonPiece ================================================

_WeilerAthertonPiece::_WeilerAthertonPiece(_WeilerAthertonArea* area,
    _WeilerAthertonSourcePolygon* source)
{
    this->area = area;
    this->source = source;
}

//= _WeilerAthertonLitFace ==============================================

_WeilerAthertonLitFace::_WeilerAthertonLitFace() : whole(FALSE)
{
}

//= _WeilerAthertonEdgeGrid =============================================

_WeilerAthertonEdgeGrid::_WeilerAthertonEdgeGrid(Polygon2D* polygon,
    double tolerance)
{
    double box[4];
    long int edges = 0;
    long int i;
    long int k;
    long int side;

    HiddenSurfaceRenderer::boundingBox(polygon, box);
    for ( k = 0; k < polygon->loops.size(); k++ ) {
        edges += polygon->loops[k]->vertices.size();
    }
    this->tolerance = tolerance;
    this->minX = box[0] - tolerance;
    this->minY = box[1] - tolerance;
    side = (long int)Math::sqrt((double)edges);
    if ( side < 1 ) {
        side = 1;
    }
    this->columns = side;
    this->rows = side;
    this->cellWidth = Math::max((box[2] + tolerance - minX) / columns,
        Double::MIN_NORMAL);
    this->cellHeight = Math::max((box[3] + tolerance - minY) / rows,
        Double::MIN_NORMAL);
    cells.reserve(columns * rows);
    for ( i = 0; i < columns * rows; i++ ) {
        cells.add((ArrayList<_WeilerAthertonTuple>*)NULL);
    }
    for ( k = 0; k < polygon->loops.size(); k++ ) {
        _Polygon2DContour* loop = polygon->loops[k];
        long int n = loop->vertices.size();
        for ( i = 0; i < n; i++ ) {
            const Vertex2D& a = loop->vertices[i];
            const Vertex2D& b = loop->vertices[(i + 1) % n];
            _WeilerAthertonTuple edge = _WeilerAthertonTuple::of(a.x, a.y,
                b.x, b.y);
            long int c0 = column(Math::min(a.x, b.x) - tolerance);
            long int c1 = column(Math::max(a.x, b.x) + tolerance);
            long int r0 = row(Math::min(a.y, b.y) - tolerance);
            long int r1 = row(Math::max(a.y, b.y) + tolerance);
            long int r;
            long int c;
            for ( r = r0; r <= r1; r++ ) {
                for ( c = c0; c <= c1; c++ ) {
                    ArrayList<_WeilerAthertonTuple>* cell =
                        cells[r * columns + c];
                    if ( cell == NULL ) {
                        cell = new ArrayList<_WeilerAthertonTuple>();
                        cells[r * columns + c] = cell;
                    }
                    cell->add(edge);
                }
            }
        }
    }
}

_WeilerAthertonEdgeGrid::~_WeilerAthertonEdgeGrid()
{
    long int i;

    for ( i = 0; i < cells.size(); i++ ) {
        delete cells[i];
    }
}

long int
_WeilerAthertonEdgeGrid::column(double x) const
{
    long int value = (long int)((x - minX) / cellWidth);
    if ( value > columns - 1 ) {
        value = columns - 1;
    }
    return value < 0 ? 0 : value;
}

long int
_WeilerAthertonEdgeGrid::row(double y) const
{
    long int value = (long int)((y - minY) / cellHeight);
    if ( value > rows - 1 ) {
        value = rows - 1;
    }
    return value < 0 ? 0 : value;
}

BOOLEAN
_WeilerAthertonEdgeGrid::isNearBorder(double x, double y) const
{
    ArrayList<_WeilerAthertonTuple>* cell;
    double tolerance2;
    long int i;

    if ( x < minX || y < minY || x > minX + columns * cellWidth ||
         y > minY + rows * cellHeight ) {
        return FALSE;
    }
    cell = cells[row(y) * columns + column(x)];
    if ( cell == NULL ) {
        return FALSE;
    }
    tolerance2 = tolerance * tolerance;
    for ( i = 0; i < cell->size(); i++ ) {
        const double* e = (*cell)[i].v;
        double dx = e[2] - e[0];
        double dy = e[3] - e[1];
        double length2 = dx * dx + dy * dy;
        double t = length2 > 0.0 ? Math::max(0.0, Math::min(1.0,
            ((x - e[0]) * dx + (y - e[1]) * dy) / length2)) : 0.0;
        double ex = e[0] + t * dx - x;
        double ey = e[1] + t * dy - y;
        if ( ex * ex + ey * ey <= tolerance2 ) {
            return TRUE;
        }
    }
    return FALSE;
}

//= _WeilerAthertonShadedPart ===========================================

_WeilerAthertonShadedPart::_WeilerAthertonShadedPart(
    _WeilerAthertonArea* area, const ArrayList<Light*>& lights) :
    lights(lights)
{
    this->area = area;
}

//= _WeilerAthertonArena ================================================

_WeilerAthertonArena::_WeilerAthertonArena()
{
}

_WeilerAthertonArena::~_WeilerAthertonArena()
{
    long int i;

    for ( i = elements.size() - 1; i >= 0; i-- ) {
        delete elements[i];
    }
    for ( i = entities.size() - 1; i >= 0; i-- ) {
        delete entities[i];
    }
}

Entity*
_WeilerAthertonArena::add(Entity* entity)
{
    entities.add(entity);
    return entity;
}

RenderingElement*
_WeilerAthertonArena::add(RenderingElement* element)
{
    elements.add(element);
    return element;
}

/**
@return true if a sorted list of addresses holds the given one
*/
static BOOLEAN containsAddress(const ArrayList<unsigned long>& addresses,
                               unsigned long address)
{
    long int low = 0;
    long int high = addresses.size() - 1;

    while ( low <= high ) {
        long int middle = (low + high) / 2;
        if ( addresses[middle] == address ) {
            return TRUE;
        }
        if ( addresses[middle] < address ) {
            low = middle + 1;
        }
        else {
            high = middle - 1;
        }
    }
    return FALSE;
}

void
_WeilerAthertonArena::moveInto(_WeilerAthertonArena* target,
    const ArrayList<unsigned long>& keptAddresses)
{
    ArrayList<Entity*> remainingEntities;
    ArrayList<RenderingElement*> remainingElements;
    long int i;

    for ( i = 0; i < entities.size(); i++ ) {
        if ( containsAddress(keptAddresses, (unsigned long)entities[i]) ) {
            target->add(entities[i]);
        }
        else {
            remainingEntities.add(entities[i]);
        }
    }
    for ( i = 0; i < elements.size(); i++ ) {
        if ( containsAddress(keptAddresses, (unsigned long)elements[i]) ) {
            target->add(elements[i]);
        }
        else {
            remainingElements.add(elements[i]);
        }
    }
    entities = remainingEntities;
    elements = remainingElements;
}

/**
Adds to a list of addresses kept by `_WeilerAthertonArena::moveInto` a
piece, its area and the polygon of its area.
*/
static void keepPiece(ArrayList<unsigned long>& kept,
                      _WeilerAthertonPiece* piece)
{
    kept.add((unsigned long)(RenderingElement*)piece);
    kept.add((unsigned long)(RenderingElement*)piece->area);
    kept.add((unsigned long)(Entity*)piece->area->polygon);
}

//= Comparators (the lambdas of the Java version) =======================

/**
C++ port note: increasing order of addresses (see
`_WeilerAthertonArena::moveInto`)
*/
class _WeilerAthertonAddressOrder : public Comparator<unsigned long>
{
public:
    virtual int compare(const unsigned long& a, const unsigned long& b) const
    {
        if ( a < b ) {
            return -1;
        }
        if ( a > b ) {
            return 1;
        }
        return 0;
    }
};

/**
C++ port note: `(p, q) -> Double.compare(p.nearestDepth, q.nearestDepth)`
*/
class _WeilerAthertonNearestDepthOrder :
    public Comparator<_WeilerAthertonSourcePolygon*>
{
public:
    virtual int compare(_WeilerAthertonSourcePolygon* const& p,
                        _WeilerAthertonSourcePolygon* const& q) const
    {
        return Double::compare(p->nearestDepth, q->nearestDepth);
    }
};

/**
C++ port note: the natural order of `Double` (`Collections.sort(xs)`)
*/
class _WeilerAthertonDoubleOrder : public Comparator<double>
{
public:
    virtual int compare(const double& a, const double& b) const
    {
        return Double::compare(a, b);
    }
};

/**
C++ port note: `(a, b) -> Double.compare(a[2], b[2])`
*/
class _WeilerAthertonMiddleOrder : public Comparator<_WeilerAthertonTuple>
{
public:
    virtual int compare(const _WeilerAthertonTuple& a,
                        const _WeilerAthertonTuple& b) const
    {
        return Double::compare(a.v[2], b.v[2]);
    }
};

//= VisiblePolygon ======================================================

HiddenSurfaceRenderer::VisiblePolygon::VisiblePolygon(Polygon2D* area,
    _WeilerAthertonSourcePolygon* source, const ArrayList<Light*>* lights)
{
    this->area = area;
    this->body = source->body;
    this->face = source->face;
    this->worldPlane = new InfinitePlane(*source->worldPlane);
    this->lights = NULL;
    if ( lights != NULL ) {
        this->lights = new ArrayList<Light*>(*lights);
    }
}

HiddenSurfaceRenderer::VisiblePolygon::~VisiblePolygon()
{
    delete area;
    delete worldPlane;
    if ( lights != NULL ) {
        delete lights;
    }
}

//= HiddenSurfaceRenderer ===============================================

_WeilerAthertonArena* HiddenSurfaceRenderer::arena = NULL;

HiddenSurfaceRenderer::HiddenSurfaceRenderer()
{
}

/**
C++ port note: Java's `new Polygon2D()`, owned by the arena of the call.
*/
Polygon2D*
HiddenSurfaceRenderer::newPolygon()
{
    Polygon2D* polygon = new Polygon2D();
    arena->add(polygon);
    return polygon;
}

/**
C++ port note: Java's `new Polygon2D()` followed by `loops.clear()`, NOT
owned by the arena (the caller deletes it, or gives it to the arena).
*/
Polygon2D*
HiddenSurfaceRenderer::newEmptyPolygon()
{
    Polygon2D* polygon = new Polygon2D();
    long int i;

    for ( i = 0; i < polygon->loops.size(); i++ ) {
        delete polygon->loops[i];
    }
    polygon->loops.clear();
    return polygon;
}

/**
C++ port note: Java's `new _WeilerAthertonArea(polygon)`, owned by the
arena of the call.
*/
_WeilerAthertonArea*
HiddenSurfaceRenderer::newArea(Polygon2D* polygon)
{
    _WeilerAthertonArea* area = new _WeilerAthertonArea(polygon);
    arena->add(area);
    return area;
}

/**
C++ port note: Java's `new _WeilerAthertonPiece(area, source)`, owned by
the arena of the call.
*/
_WeilerAthertonPiece*
HiddenSurfaceRenderer::newPiece(_WeilerAthertonArea* area,
                                _WeilerAthertonSourcePolygon* source)
{
    _WeilerAthertonPiece* piece = new _WeilerAthertonPiece(area, source);
    arena->add(piece);
    return piece;
}

ArrayList<HiddenSurfaceRenderer::VisiblePolygon*>
HiddenSurfaceRenderer::executeWeilerAthertonAlgorithm(
    ArrayList<SimpleBody*>& solids, Camera* camera)
{
    _WeilerAthertonArena callArena;
    _WeilerAthertonArena* previousArena = arena;
    HiddenSurfaceRenderer* renderer;
    ArrayList<VisiblePolygon*> visible;
    long int i;

    arena = &callArena;
    renderer = solve(solids, camera);
    for ( i = 0; i < renderer->output.size(); i++ ) {
        _WeilerAthertonPiece* piece = renderer->output[i];
        visible.add(new VisiblePolygon(toNormalizedDeviceCoordinates(
            piece->area->polygon), piece->source, NULL));
    }
    arena = previousArena;
    return visible;
}

ArrayList<HiddenSurfaceRenderer::VisiblePolygon*>
HiddenSurfaceRenderer::executeWeilerAthertonAlgorithm(
    ArrayList<SimpleBody*>& solids, Camera* camera,
    ArrayList<Light*>& lights)
{
    _WeilerAthertonArena callArena;
    _WeilerAthertonArena* previousArena = arena;
    HiddenSurfaceRenderer* renderer;
    ArrayList<Light*> ambientLights;
    ArrayList<Light*> castingLights;
    ArrayList<_WeilerAthertonLitFaces*> litFaces;
    double bounds[4];
    BOOLEAN withBounds;
    Matrix4x4d projection = camera->calculateProjectionMatrix();
    ArrayList<VisiblePolygon*> visible;
    long int i;
    long int j;
    long int k;

    arena = &callArena;
    renderer = solve(solids, camera);
    withBounds = boundingSphere(solids, bounds);

    // First pass: hidden surface removed views from the lights
    for ( i = 0; i < lights.size(); i++ ) {
        Light* light = lights[i];
        if ( light->isAmbient() ) {
            ambientLights.add(light);
        }
        else if ( withBounds ) {
            castingLights.add(light);
            litFaces.add(litFacesFromLight(solids, light, bounds));
        }
    }

    // Second pass: the visible areas from the observer, divided by the
    // lit areas of their faces
    for ( i = 0; i < renderer->output.size(); i++ ) {
        _WeilerAthertonPiece* piece = renderer->output[i];
        // C++ port note: the objects made to divide a visible piece are
        // released after it, as the visible polygons copy what they keep
        _WeilerAthertonArena pieceArena;
        arena = &pieceArena;
        ArrayList<_WeilerAthertonShadedPart*> parts;
        _WeilerAthertonShadedPart* first =
            new _WeilerAthertonShadedPart(piece->area, ambientLights);
        arena->add(first);
        parts.add(first);
        for ( k = 0; k < castingLights.size(); k++ ) {
            _WeilerAthertonLitFacesOfBody** faces =
                litFaces[k]->get(piece->source->body);
            _WeilerAthertonLitFace* lit = NULL;
            if ( faces != NULL ) {
                _WeilerAthertonLitFace** found =
                    (*faces)->get(piece->source->face);
                if ( found != NULL ) {
                    lit = *found;
                }
            }
            if ( lit != NULL ) {
                parts = renderer->divideByLitArea(parts, lit,
                    castingLights[k], projection);
            }
        }
        for ( j = 0; j < parts.size(); j++ ) {
            _WeilerAthertonShadedPart* part = parts[j];
            visible.add(new VisiblePolygon(toNormalizedDeviceCoordinates(
                part->area->polygon), piece->source, &part->lights));
        }
        arena = &callArena;
    }

    // C++ port note: release of the maps of the lit faces (their values
    // are owned by the arena)
    for ( k = 0; k < litFaces.size(); k++ ) {
        for ( j = 0; j < litFaces[k]->size(); j++ ) {
            delete litFaces[k]->getValue(j);
        }
        delete litFaces[k];
    }
    arena = previousArena;
    return visible;
}

/**
Runs the hidden surface removal of [WEIL1977] for a view.
@return the renderer, with the visible polygons in its output (owned by the
arena of the call)
*/
HiddenSurfaceRenderer*
HiddenSurfaceRenderer::solve(ArrayList<SimpleBody*>& solids, Camera* camera)
{
    HiddenSurfaceRenderer* renderer = new HiddenSurfaceRenderer();
    ArrayList<_WeilerAthertonSourcePolygon*> sources;

    arena->add(renderer);
    sources = renderer->buildInputPolygons(solids, camera);
    renderer->solveWindow(-SCALE, -SCALE, SCALE, SCALE, sources, 0);
    return renderer;
}

//= Screen subdivision ==================================================

/**
Screen subdivision of [WEIL1977].219: "by taking the approach of a
Warnock style screen area subdivision, the image can be divided into
areas each containing a specified maximum number of polygons", each one
processed separately by the hidden surface removal. This bounds the
exponential growth of the number of sorting steps with the number of
polygons, so the whole process becomes almost linear (the subdivision
itself grows as n log n).

A window with more than `MAXIMUM_POLYGONS_PER_WINDOW` polygons is divided
in four quadrants, and the polygons are clipped to each of them (to a
convex window, by its half planes). The division stops at
`MAXIMUM_SUBDIVISION_DEPTH` levels, and also in a quadrant where it did
not reduce the number of polygons (all of them cover it, as a floor seen
from above).
@param sources the polygons of the window, clipped to it
@param depth level of the window in the subdivision
*/
void
HiddenSurfaceRenderer::solveWindow(double minX, double minY, double maxX,
    double maxY, const ArrayList<_WeilerAthertonSourcePolygon*>& sources,
    int depth)
{
    double middleX;
    double middleY;
    double quadrants[4][4];
    int q;
    long int i;

    if ( sources.size() <= MAXIMUM_POLYGONS_PER_WINDOW ||
         depth >= MAXIMUM_SUBDIVISION_DEPTH ) {
        solveArea(sources);
        return;
    }
    middleX = (minX + maxX) / 2.0;
    middleY = (minY + maxY) / 2.0;
    quadrants[0][0] = minX;    quadrants[0][1] = minY;
    quadrants[0][2] = middleX; quadrants[0][3] = middleY;
    quadrants[1][0] = middleX; quadrants[1][1] = minY;
    quadrants[1][2] = maxX;    quadrants[1][3] = middleY;
    quadrants[2][0] = minX;    quadrants[2][1] = middleY;
    quadrants[2][2] = middleX; quadrants[2][3] = maxY;
    quadrants[3][0] = middleX; quadrants[3][1] = middleY;
    quadrants[3][2] = maxX;    quadrants[3][3] = maxY;
    for ( q = 0; q < 4; q++ ) {
        ArrayList<_WeilerAthertonSourcePolygon*> inside;
        for ( i = 0; i < sources.size(); i++ ) {
            _WeilerAthertonSourcePolygon* clipped =
                clippedToWindow(sources[i], quadrants[q][0], quadrants[q][1],
                    quadrants[q][2], quadrants[q][3]);
            if ( clipped != NULL ) {
                inside.add(clipped);
            }
        }
        if ( inside.size() < sources.size() ) {
            solveWindow(quadrants[q][0], quadrants[q][1], quadrants[q][2],
                quadrants[q][3], inside, depth + 1);
        }
        else {
            solveArea(inside);
        }
    }
}

/**
@return the part of a polygon inside a window (the same polygon if it is
entirely inside), or null if no part is inside
*/
_WeilerAthertonSourcePolygon*
HiddenSurfaceRenderer::clippedToWindow(_WeilerAthertonSourcePolygon* source,
    double minX, double minY, double maxX, double maxY)
{
    _WeilerAthertonArea* templateArea = source->templateArea;
    Polygon2D* polygon;
    Polygon2D* next;
    ArrayList<_WeilerAthertonArea*> parts;
    _WeilerAthertonSourcePolygon* clipped;
    long int i;
    long int k;

    if ( templateArea->maxX <= minX || templateArea->minX >= maxX ||
         templateArea->maxY <= minY || templateArea->minY >= maxY ) {
        return NULL;
    }
    if ( templateArea->minX >= minX && templateArea->maxX <= maxX &&
         templateArea->minY >= minY && templateArea->maxY <= maxY ) {
        return source;
    }
    // C++ port note: the intermediate polygons are deleted as soon as the
    // next one is computed
    polygon = clipByHalfPlane(templateArea->polygon, 1.0, 0.0, -minX);
    if ( polygon != NULL ) {
        next = clipByHalfPlane(polygon, -1.0, 0.0, maxX);
        delete polygon;
        polygon = next;
    }
    if ( polygon != NULL ) {
        next = clipByHalfPlane(polygon, 0.0, 1.0, -minY);
        delete polygon;
        polygon = next;
    }
    if ( polygon != NULL ) {
        next = clipByHalfPlane(polygon, 0.0, -1.0, maxY);
        delete polygon;
        polygon = next;
    }
    addWithoutSlivers(parts, polygon);
    if ( polygon != NULL ) {
        delete polygon;
    }
    if ( parts.isEmpty() ) {
        return NULL;
    }

    clipped = new _WeilerAthertonSourcePolygon();
    arena->add(clipped);
    clipped->templateArea = merged(parts);
    clipped->depthSlopeX = source->depthSlopeX;
    clipped->depthSlopeY = source->depthSlopeY;
    clipped->depthOffset = source->depthOffset;
    clipped->body = source->body;
    clipped->face = source->face;
    clipped->worldPlane = source->worldPlane;
    clipped->complete = FALSE;
    clipped->original = source->original;
    clipped->nearestDepth = Double::MAX_VALUE;
    for ( k = 0; k < clipped->templateArea->polygon->loops.size(); k++ ) {
        _Polygon2DContour* loop = clipped->templateArea->polygon->loops[k];
        for ( i = 0; i < loop->vertices.size(); i++ ) {
            const Vertex2D& v = loop->vertices[i];
            clipped->nearestDepth = Math::min(clipped->nearestDepth,
                clipped->depthAt(v.x, v.y));
        }
    }
    return clipped;
}

/**
Hidden surface removal of the polygons of a window, as in
[WEIL1977].215-216.
*/
void
HiddenSurfaceRenderer::solveArea(
    const ArrayList<_WeilerAthertonSourcePolygon*>& inSources)
{
    ArrayList<_WeilerAthertonPiece*> polygonList;
    ArrayList<_WeilerAthertonSourcePolygon*> sources(inSources);
    _WeilerAthertonNearestDepthOrder nearestDepthOrder;
    long int i;

    // Step a) of [WEIL1977].215: preliminary rough depth sort, from the
    // polygons closest to the observer to those furthest away
    sources.sort(nearestDepthOrder);

    // C++ port note: the pieces and areas made while solving the window
    // are owned by an arena of the window, and only the visible pieces
    // (with their areas and polygons) are kept when it ends; the rest is
    // released, as Java's garbage collector does, which bounds the memory
    // used by a view
    _WeilerAthertonArena windowArena;
    _WeilerAthertonArena* viewArena = arena;
    _WeilerAthertonAddressOrder addressOrder;
    ArrayList<unsigned long> kept;
    long int firstOutput = output.size();

    arena = &windowArena;
    for ( i = 0; i < sources.size(); i++ ) {
        polygonList.add(newPiece(sources[i]->templateArea, sources[i]));
    }

    // [WEIL1977].216: the process is continued until the outside list
    // is exhausted
    while ( !polygonList.isEmpty() ) {
        // C++ port note: likewise, the objects of each step are released
        // at its end, but for the pieces that remain to be processed and
        // the visible ones
        _WeilerAthertonArena stepArena;
        ArrayList<unsigned long> remaining;
        long int stepOutput = output.size();

        arena = &stepArena;
        polygonList = subdivide(polygonList[0]->source, polygonList);
        for ( i = 0; i < polygonList.size(); i++ ) {
            keepPiece(remaining, polygonList[i]);
        }
        for ( i = stepOutput; i < output.size(); i++ ) {
            keepPiece(remaining, output[i]);
        }
        remaining.sort(addressOrder);
        stepArena.moveInto(&windowArena, remaining);
        arena = &windowArena;
    }

    for ( i = firstOutput; i < output.size(); i++ ) {
        keepPiece(kept, output[i]);
    }
    kept.sort(addressOrder);
    windowArena.moveInto(viewArena, kept);
    arena = viewArena;
}

//= Shadows =============================================================

/**
Shadowing pass of [WEIL1977].218 for one light: "a hidden surface removed
view of a scene from the position of the light source", whose visible
polygons are "transformed back to the original space" as the lit areas
of their source faces.
@param bounds bounding sphere of the solids, as {x, y, z, radius}
@return the lit areas of the faces reached by the light, by body and face
(the maps are owned by the caller, their lit faces by the arena)
*/
_WeilerAthertonLitFaces*
HiddenSurfaceRenderer::litFacesFromLight(ArrayList<SimpleBody*>& solids,
    Light* light, const double bounds[4])
{
    _WeilerAthertonLitFaces* lit = new _WeilerAthertonLitFaces();
    ArrayList<Camera*> cameras;
    long int c;
    long int i;
    long int j;
    long int k;

    // A light inside a solid sees only its inner side, the back of its
    // faces: it reaches nothing outside of it
    if ( !light->instanceOf(DirectionalLight::CLASS) &&
         isInsideASolid(solids, light->getPosition()) ) {
        return lit;
    }
    cameras = lightCameras(light, bounds);
    for ( c = 0; c < cameras.size(); c++ ) {
        Camera* camera = cameras[c];
        // C++ port note: the polygons of each view from the light are
        // released after the view (Java leaves them to the garbage
        // collector), as only the lit faces, owned by the arena of the
        // call, are kept; this bounds the memory used by the shadows
        _WeilerAthertonArena viewArena;
        _WeilerAthertonArena* callArena = arena;
        arena = &viewArena;
        HiddenSurfaceRenderer* renderer = solve(solids, camera);
        Matrix4x4d inverse = camera->calculateProjectionMatrix().inverse();
        TreeMap<_WeilerAthertonSourcePolygon*, double> visibleAreas;

        // The visible areas of a face, added over the windows of the
        // screen subdivision
        for ( i = 0; i < renderer->output.size(); i++ ) {
            _WeilerAthertonPiece* piece = renderer->output[i];
            double* sum = visibleAreas.get(piece->source->original);
            if ( sum == NULL ) {
                visibleAreas.put(piece->source->original, 0.0);
                sum = visibleAreas.get(piece->source->original);
            }
            *sum += piece->area->area;
        }
        for ( i = 0; i < renderer->output.size(); i++ ) {
            _WeilerAthertonPiece* piece = renderer->output[i];
            _WeilerAthertonSourcePolygon* source = piece->source;
            _WeilerAthertonLitFacesOfBody* faces;
            _WeilerAthertonLitFacesOfBody** foundFaces = lit->get(source->body);
            _WeilerAthertonLitFace* litFace;
            _WeilerAthertonLitFace** foundFace;
            _WeilerAthertonSourcePolygon* original;
            ArrayList<ArrayList<Vector3Dd> > region;

            if ( foundFaces == NULL ) {
                faces = new _WeilerAthertonLitFacesOfBody();
                lit->put(source->body, faces);
            }
            else {
                faces = *foundFaces;
            }
            foundFace = faces->get(source->face);
            if ( foundFace == NULL ) {
                litFace = new _WeilerAthertonLitFace();
                callArena->add(litFace);
                faces->put(source->face, litFace);
            }
            else {
                litFace = *foundFace;
            }
            if ( litFace->whole ) {
                continue;
            }
            // A face entirely in the view and not hidden at all is lit
            // whole: its visible pieces add up to its area (but for the
            // slivers the screen subdivision may discard)
            original = source->original;
            if ( original->complete && *visibleAreas.get(original) >=
                 original->templateArea->area * (1.0 - 1.0e-9) - AREA_EPSILON -
                 SLIVER_WIDTH * original->templateArea->perimeter ) {
                litFace->whole = TRUE;
                litFace->regions.clear();
                continue;
            }
            for ( k = 0; k < piece->area->polygon->loops.size(); k++ ) {
                _Polygon2DContour* loop = piece->area->polygon->loops[k];
                ArrayList<Vector3Dd> contour;
                for ( j = 0; j < loop->vertices.size(); j++ ) {
                    const Vertex2D& v = loop->vertices[j];
                    Vector3Dd p;
                    if ( unprojectToPlane(inverse, v.x / SCALE,
                             v.y / SCALE, source->worldPlane, p) ) {
                        contour.add(p);
                    }
                }
                if ( contour.size() >= 3 ) {
                    region.add(contour);
                }
            }
            if ( !region.isEmpty() ) {
                litFace->regions.add(region);
            }
        }
        arena = callArena;
    }
    return lit;
}

BOOLEAN
HiddenSurfaceRenderer::isInsideASolid(ArrayList<SimpleBody*>& solids,
                                      const Vector3Dd& p)
{
    long int i;

    for ( i = 0; i < solids.size(); i++ ) {
        SimpleBody* body = solids[i];
        Geometry* geometry = body->getGeometry();
        if ( geometry != NULL &&
             geometry->instanceOf(PolyhedralBoundedSolid::CLASS) &&
             ((PolyhedralBoundedSolid*)geometry)->doContainmentTest(
                 body->getTransformationMatrix().inverse().multiply(p),
                 VSDK::EPSILON) == Geometry::INSIDE ) {
            return TRUE;
        }
    }
    return FALSE;
}

/**
@param out the point of a plane seen at a point of the x-y plane of the
normalized device coordinates of a view
@return false if the line of sight is parallel to the plane (the Java
version returns null)
*/
BOOLEAN
HiddenSurfaceRenderer::unprojectToPlane(const Matrix4x4d& inverseProjection,
    double x, double y, const InfinitePlane* plane, Vector3Dd& out)
{
    Vector4Dd a = inverseProjection.multiply(
        Vector4Dd(x, y, -0.5, 1.0)).dividedByW();
    Vector4Dd b = inverseProjection.multiply(
        Vector4Dd(x, y, 0.5, 1.0)).dividedByW();
    Vector3Dd p(a.x(), a.y(), a.z());
    Vector3Dd direction(b.x() - a.x(), b.y() - a.y(), b.z() - a.z());
    Vector3Dd n = plane->getNormal();
    double denominator = n.dotProduct(direction);
    double t;

    if ( Math::abs(denominator) <= 1.0e-300 ) {
        return FALSE;
    }
    t = -(n.dotProduct(p) + plane->getD()) / denominator;
    out = p.add(direction.multiply(t));
    return TRUE;
}

/**
Views from a light enclosing the scene: one perspective view, or the six
views of the faces of a cube when the light is among the solids, for a
point (or spot) light, and one orthogonal view for a directional light.
@param bounds bounding sphere of the solids, as {x, y, z, radius}
@return the cameras (owned by the arena)
*/
ArrayList<Camera*>
HiddenSurfaceRenderer::lightCameras(Light* light, const double bounds[4])
{
    ArrayList<Camera*> cameras;
    Vector3Dd center(bounds[0], bounds[1], bounds[2]);
    double radius = bounds[3] * LIGHT_VIEW_MARGIN + VSDK::EPSILON;
    Vector3Dd position;
    Vector3Dd toCenter;
    double distance;
    long int i;

    if ( light->instanceOf(DirectionalLight::CLASS) ) {
        // The position of a directional light is its direction
        Vector3Dd direction = light->getPosition().normalized();
        Camera* camera = lightCamera(center.subtract(
            direction.multiply(2.0 * radius)), direction);
        camera->setProjectionMode(Camera::PROJECTION_MODE_ORTHOGONAL);
        camera->setOrthogonalZoom(1.0 / radius);
        camera->setNearPlaneDistance(0.5 * radius);
        camera->setFarPlaneDistance(3.5 * radius);
        camera->updateViewportResize(LIGHT_VIEW_SIZE, LIGHT_VIEW_SIZE);
        cameras.add(camera);
        return cameras;
    }

    position = light->getPosition();
    toCenter = center.subtract(position);
    distance = toCenter.length();
    if ( distance > radius &&
         Math::asin(radius / distance) <= MAXIMUM_LIGHT_HALF_ANGLE ) {
        Camera* camera = lightCamera(position, toCenter.normalized());
        camera->setFov(2.0 * Math::toDegrees(Math::asin(radius / distance)) *
            LIGHT_VIEW_MARGIN);
        camera->setNearPlaneDistance(0.5 * (distance - radius));
        camera->setFarPlaneDistance(2.0 * (distance + radius));
        cameras.add(camera);
    }
    else {
        Vector3Dd directions[6];
        directions[0] = Vector3Dd(1.0, 0.0, 0.0);
        directions[1] = Vector3Dd(-1.0, 0.0, 0.0);
        directions[2] = Vector3Dd(0.0, 1.0, 0.0);
        directions[3] = Vector3Dd(0.0, -1.0, 0.0);
        directions[4] = Vector3Dd(0.0, 0.0, 1.0);
        directions[5] = Vector3Dd(0.0, 0.0, -1.0);
        // The views overlap a little, so no lit area is lost on their
        // common borders (the lit regions of a face may overlap: see
        // `divideByLitArea`)
        for ( i = 0; i < 6; i++ ) {
            Camera* camera = lightCamera(position, directions[i]);
            camera->setFov(90.0 + CUBE_VIEW_OVERLAP);
            camera->setNearPlaneDistance(1.0e-3 * (distance + radius));
            camera->setFarPlaneDistance(2.0 * (distance + radius));
            cameras.add(camera);
        }
    }
    for ( i = 0; i < cameras.size(); i++ ) {
        cameras[i]->updateViewportResize(LIGHT_VIEW_SIZE, LIGHT_VIEW_SIZE);
    }
    return cameras;
}

/**
@return a camera at a position looking along a direction (owned by the
arena)
*/
Camera*
HiddenSurfaceRenderer::lightCamera(const Vector3Dd& position,
                                   const Vector3Dd& front)
{
    Camera* camera = new Camera();
    Vector3Dd upHint;
    Vector3Dd left;
    Vector3Dd up;

    arena->add(camera);
    if ( Math::abs(front.z()) < 0.9 ) {
        upHint = Vector3Dd(0.0, 0.0, 1.0);
    }
    else {
        upHint = Vector3Dd(1.0, 0.0, 0.0);
    }
    left = upHint.crossProduct(front).normalized();
    up = front.crossProduct(left).normalized();

    camera->setPosition(position);
    camera->setRotation(Matrix4x4d()
        .withVal(0, 0, front.x()).withVal(0, 1, left.x()).withVal(0, 2, up.x())
        .withVal(1, 0, front.y()).withVal(1, 1, left.y()).withVal(1, 2, up.y())
        .withVal(2, 0, front.z()).withVal(2, 1, left.z()).withVal(2, 2, up.z()));
    camera->updateViewportResize(LIGHT_VIEW_SIZE, LIGHT_VIEW_SIZE);
    return camera;
}

/**
@param bounds the bounding sphere of the vertices of the solids, as {x, y,
z, radius}
@return false if they have no vertices (the Java version returns null)
*/
BOOLEAN
HiddenSurfaceRenderer::boundingSphere(ArrayList<SimpleBody*>& solids,
                                      double bounds[4])
{
    double box[6];
    ArrayList<Vector3Dd> points;
    Vector3Dd center;
    double radius;
    long int b;
    long int i;
    long int j;

    box[0] = Double::MAX_VALUE;
    box[1] = Double::MAX_VALUE;
    box[2] = Double::MAX_VALUE;
    box[3] = -Double::MAX_VALUE;
    box[4] = -Double::MAX_VALUE;
    box[5] = -Double::MAX_VALUE;
    for ( b = 0; b < solids.size(); b++ ) {
        SimpleBody* body = solids[b];
        Geometry* geometry = body->getGeometry();
        PolyhedralBoundedSolid* brep;
        Matrix4x4d transform;
        if ( geometry == NULL ||
             !geometry->instanceOf(PolyhedralBoundedSolid::CLASS) ) {
            continue;
        }
        brep = (PolyhedralBoundedSolid*)geometry;
        transform = body->getTransformationMatrix();
        for ( i = 0; i < brep->getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace* face = brep->getPolygonsList().get(i);
            for ( j = 0; j < face->boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(j);
                _PolyhedralBoundedSolidHalfEdge* he =
                    loop != NULL ? loop->boundaryStartHalfEdge :
                    (_PolyhedralBoundedSolidHalfEdge*)NULL;
                _PolyhedralBoundedSolidHalfEdge* heStart = he;
                if ( he == NULL ) {
                    continue;
                }
                do {
                    Vector3Dd p;
                    if ( he == NULL || he->startingVertex == NULL ) {
                        break;
                    }
                    p = transform.multiply(he->startingVertex->position);
                    points.add(p);
                    box[0] = Math::min(box[0], p.x());
                    box[1] = Math::min(box[1], p.y());
                    box[2] = Math::min(box[2], p.z());
                    box[3] = Math::max(box[3], p.x());
                    box[4] = Math::max(box[4], p.y());
                    box[5] = Math::max(box[5], p.z());
                    he = he->next();
                } while ( he != heStart );
            }
        }
    }
    if ( points.isEmpty() ) {
        return FALSE;
    }
    center = Vector3Dd((box[0] + box[3]) / 2.0,
        (box[1] + box[4]) / 2.0, (box[2] + box[5]) / 2.0);
    radius = 0.0;
    for ( i = 0; i < points.size(); i++ ) {
        radius = Math::max(radius, points[i].subtract(center).length());
    }
    bounds[0] = center.x();
    bounds[1] = center.y();
    bounds[2] = center.z();
    bounds[3] = radius;
    return TRUE;
}

/**
Divides the parts of a visible area by the lit areas of its face for a
light: the parts inside them get the light. The lit areas are projected
to the viewing space of the observer and used as clip polygons.
*/
ArrayList<_WeilerAthertonShadedPart*>
HiddenSurfaceRenderer::divideByLitArea(
    const ArrayList<_WeilerAthertonShadedPart*>& parts,
    _WeilerAthertonLitFace* lit, Light* light, const Matrix4x4d& projection)
{
    ArrayList<_WeilerAthertonShadedPart*> result;
    ArrayList<_WeilerAthertonArea*> regions;
    _WeilerAthertonShadedPart* shaded;
    long int i;
    long int j;
    long int k;
    long int p;

    if ( lit->whole ) {
        for ( p = 0; p < parts.size(); p++ ) {
            shaded = new _WeilerAthertonShadedPart(parts[p]->area,
                withLight(parts[p]->lights, light));
            arena->add(shaded);
            result.add(shaded);
        }
        return result;
    }

    for ( i = 0; i < lit->regions.size(); i++ ) {
        _WeilerAthertonArea* area = projectRegion(lit->regions[i], projection);
        if ( area != NULL ) {
            regions.add(area);
        }
    }
    for ( p = 0; p < parts.size(); p++ ) {
        _WeilerAthertonShadedPart* part = parts[p];
        // Each lit region takes its part of what the previous ones left,
        // so regions that overlap (from the views of a cube) are counted
        // once
        ArrayList<_WeilerAthertonArea*> remaining;
        ArrayList<_WeilerAthertonArea*> litAreas;
        remaining.add(part->area);
        for ( k = 0; k < regions.size(); k++ ) {
            _WeilerAthertonArea* region = regions[k];
            ArrayList<_WeilerAthertonArea*> next;
            for ( j = 0; j < remaining.size(); j++ ) {
                _WeilerAthertonArea* area = remaining[j];
                if ( !area->overlapsBox(region) ) {
                    next.add(area);
                    continue;
                }
                verifiedClip(region, area, litAreas, next);
            }
            remaining = next;
        }
        if ( !litAreas.isEmpty() ) {
            ArrayList<Light*> lights = withLight(part->lights, light);
            for ( j = 0; j < litAreas.size(); j++ ) {
                shaded = new _WeilerAthertonShadedPart(litAreas[j], lights);
                arena->add(shaded);
                result.add(shaded);
            }
        }
        for ( j = 0; j < remaining.size(); j++ ) {
            shaded = new _WeilerAthertonShadedPart(remaining[j], part->lights);
            arena->add(shaded);
            result.add(shaded);
        }
    }
    return result;
}

ArrayList<Light*>
HiddenSurfaceRenderer::withLight(const ArrayList<Light*>& lights,
                                 Light* light)
{
    ArrayList<Light*> result(lights);
    result.add(light);
    return result;
}

/**
Projects a lit region (in world coordinates) to the viewing space of the
observer, clipped against its near and far planes.
@return the region, with its main contours counterclockwise and its holes
clockwise, or null if nothing of it is left
*/
_WeilerAthertonArea*
HiddenSurfaceRenderer::projectRegion(
    const ArrayList<ArrayList<Vector3Dd> >& region,
    const Matrix4x4d& projection)
{
    Polygon2D* polygon = newEmptyPolygon();
    _WeilerAthertonArea* area;
    long int i;
    long int k;

    arena->add(polygon);
    for ( k = 0; k < region.size(); k++ ) {
        const ArrayList<Vector3Dd>& contour = region[k];
        ArrayList<Vector4Dd> clipLoop;
        ArrayList<_WeilerAthertonTuple> loop;
        for ( i = 0; i < contour.size(); i++ ) {
            clipLoop.add(projection.multiply(Vector4Dd(contour[i])));
        }
        clipLoop = clipAgainstPlane(clipLoop, 1.0);
        clipLoop = clipAgainstPlane(clipLoop, -1.0);
        if ( clipLoop.size() < 3 ) {
            continue;
        }
        for ( i = 0; i < clipLoop.size(); i++ ) {
            Vector4Dd ndc = clipLoop[i].dividedByW();
            loop.add(_WeilerAthertonTuple::of(SCALE * ndc.x(),
                SCALE * ndc.y()));
        }
        if ( Math::abs(signedArea(loop)) > AREA_EPSILON ) {
            addLoop(polygon, loop);
        }
    }
    if ( polygon->loops.isEmpty() ) {
        return NULL;
    }
    // The light and the observer see the same side of the face, so the
    // orientation is kept; it is checked anyway
    if ( enclosedArea(polygon) < 0.0 ) {
        for ( k = 0; k < polygon->loops.size(); k++ ) {
            Collections<Vertex2D>::reverse(polygon->loops[k]->vertices);
        }
    }
    area = newArea(polygon);
    return area->area > AREA_EPSILON ? area : (_WeilerAthertonArea*)NULL;
}

/**
@return a polygon owned by the caller (C++ port note)
*/
Polygon2D*
HiddenSurfaceRenderer::toNormalizedDeviceCoordinates(Polygon2D* polygon)
{
    Polygon2D* ndc = newEmptyPolygon();
    long int i;
    long int k;

    for ( k = 0; k < polygon->loops.size(); k++ ) {
        _Polygon2DContour* loop = polygon->loops[k];
        ndc->nextLoop();
        for ( i = 0; i < loop->vertices.size(); i++ ) {
            const Vertex2D& v = loop->vertices[i];
            ndc->addVertex(v.x / SCALE, v.y / SCALE);
        }
    }
    return ndc;
}

//= Hidden surface algorithm ============================================

/**
Solves the hidden surface problem in the area of a clip polygon, as in
[WEIL1977].215-216 (steps b, c and d), outputting the visible polygons of
that area.
@param clipPolygon source polygon whose original copy is the clip
template
@param polygonList polygons to sort
@return the outside list: the parts of the polygons outside of the clip
polygon, still to be solved
*/
ArrayList<_WeilerAthertonPiece*>
HiddenSurfaceRenderer::subdivide(_WeilerAthertonSourcePolygon* clipPolygon,
    const ArrayList<_WeilerAthertonPiece*>& polygonList)
{
    ArrayList<_WeilerAthertonPiece*> insideList;
    ArrayList<_WeilerAthertonPiece*> outsideList;
    long int i;

    // Step b): x-y polygon area sort to the area of the clip polygon
    for ( i = 0; i < polygonList.size(); i++ ) {
        clipPiece(clipPolygon, polygonList[i], insideList, outsideList);
    }

    // Step c): removal of the polygons behind the clip polygon
    insideList = removePolygonsBehind(clipPolygon, insideList);

    // Step d): a polygon in front of the clip polygon reveals an error in
    // the initial depth sort: the region is subdivided recursively with
    // the offending polygon as clip polygon and the inside list as input
    // (figure 3). If it is on the stack, it is a cyclic overlap
    // (figure 4a), and all the material behind it was already removed.
    while ( TRUE ) {
        _WeilerAthertonPiece* offending = NULL;
        ArrayList<_WeilerAthertonPiece*> next;
        for ( i = 0; i < insideList.size(); i++ ) {
            _WeilerAthertonPiece* piece = insideList[i];
            if ( piece->source != clipPolygon &&
                 !clipStack.contains(piece->source) ) {
                offending = piece;
                break;
            }
        }
        if ( offending == NULL ) {
            break;
        }
        clipStack.add(clipPolygon);
        next = subdivide(offending->source, insideList);
        insideList = next;
        clipStack.remove(clipStack.size() - 1);
    }

    // The inside list is displayed: first the parts of the clip polygon,
    // then the parts of the cyclic overlaps, which are in front of it
    for ( i = 0; i < insideList.size(); i++ ) {
        if ( insideList[i]->source == clipPolygon ) {
            display(insideList[i]);
        }
    }
    for ( i = 0; i < insideList.size(); i++ ) {
        if ( insideList[i]->source != clipPolygon ) {
            display(insideList[i]);
        }
    }
    return outsideList;
}

/**
Clips a polygon to the area of the original copy of the clip polygon,
placing its parts on the inside and outside lists.
*/
void
HiddenSurfaceRenderer::clipPiece(_WeilerAthertonSourcePolygon* clipPolygon,
    _WeilerAthertonPiece* piece,
    ArrayList<_WeilerAthertonPiece*>& insideList,
    ArrayList<_WeilerAthertonPiece*>& outsideList)
{
    ArrayList<_WeilerAthertonArea*> inside;
    ArrayList<_WeilerAthertonArea*> outside;
    long int i;

    // The parts of a polygon are inside its own original copy
    if ( piece->source == clipPolygon ) {
        insideList.add(piece);
        return;
    }
    // Areas that do not overlap need no clip
    if ( !piece->area->overlapsBox(clipPolygon->templateArea) ) {
        outsideList.add(piece);
        return;
    }

    verifiedClip(clipPolygon->templateArea, piece->area, inside, outside);
    for ( i = 0; i < inside.size(); i++ ) {
        insideList.add(newPiece(inside[i], piece->source));
    }
    for ( i = 0; i < outside.size(); i++ ) {
        outsideList.add(newPiece(outside[i], piece->source));
    }
}

/**
Step c) of [WEIL1977].216: the polygons of the inside list located behind
the clip polygon are hidden from view. A polygon partially in front of
it (penetrating it) is divided, and only its front part is kept.
@return the new inside list
*/
ArrayList<_WeilerAthertonPiece*>
HiddenSurfaceRenderer::removePolygonsBehind(
    _WeilerAthertonSourcePolygon* clipPolygon,
    const ArrayList<_WeilerAthertonPiece*>& insideList)
{
    ArrayList<_WeilerAthertonPiece*> remaining;
    long int i;

    for ( i = 0; i < insideList.size(); i++ ) {
        _WeilerAthertonPiece* piece = insideList[i];
        if ( piece->source == clipPolygon ) {
            remaining.add(piece);
            continue;
        }
        switch ( classifyDepth(clipPolygon, piece) ) {
          case IN_FRONT:
            remaining.add(piece);
            break;
          case PENETRATING:
            addFrontPart(clipPolygon, piece, remaining);
            break;
          case BEHIND:
          default:
            break;
        }
    }
    return remaining;
}

/**
The difference between the depth of a piece and the depth of the clip
polygon is an affine function over the x-y plane (both are planes in the
viewing space), so its extremes over the piece are at its vertices.
@return BEHIND if no part of the piece is in front of the clip polygon
(including the coplanar case), IN_FRONT if no part is behind it, or
PENETRATING otherwise
*/
int
HiddenSurfaceRenderer::classifyDepth(
    _WeilerAthertonSourcePolygon* clipPolygon, _WeilerAthertonPiece* piece)
{
    BOOLEAN front = FALSE;
    BOOLEAN behind = FALSE;
    long int i;
    long int k;

    for ( k = 0; k < piece->area->polygon->loops.size(); k++ ) {
        _Polygon2DContour* loop = piece->area->polygon->loops[k];
        for ( i = 0; i < loop->vertices.size(); i++ ) {
            const Vertex2D& v = loop->vertices[i];
            double difference = piece->source->depthAt(v.x, v.y) -
                clipPolygon->depthAt(v.x, v.y);
            if ( difference < -DEPTH_EPSILON ) {
                front = TRUE;
            }
            else if ( difference > DEPTH_EPSILON ) {
                behind = TRUE;
            }
        }
    }
    if ( front && behind ) {
        return PENETRATING;
    }
    return front ? IN_FRONT : BEHIND;
}

/**
Divides a piece penetrating the clip polygon by the projection of the
intersection of their planes, where their depths are equal, keeping
only the part in front of the clip polygon. The half plane in front is
taken as a convex polygon covering the piece, to which the piece is
clipped.
*/
void
HiddenSurfaceRenderer::addFrontPart(
    _WeilerAthertonSourcePolygon* clipPolygon, _WeilerAthertonPiece* piece,
    ArrayList<_WeilerAthertonPiece*>& remaining)
{
    // Points with da*x + db*y + dc < 0 are in front of the clip polygon
    double da = piece->source->depthSlopeX - clipPolygon->depthSlopeX;
    double db = piece->source->depthSlopeY - clipPolygon->depthSlopeY;
    double dc = piece->source->depthOffset - clipPolygon->depthOffset;
    _WeilerAthertonArea* area = piece->area;
    double margin = 1.0 + 0.01 * ((area->maxX - area->minX) +
        (area->maxY - area->minY));
    double box[4][2];
    ArrayList<_WeilerAthertonTuple> halfPlane;
    Polygon2D* frontRegion;
    ArrayList<_WeilerAthertonArea*> front;
    ArrayList<_WeilerAthertonArea*> back;
    int i;
    long int k;

    box[0][0] = area->minX - margin; box[0][1] = area->minY - margin;
    box[1][0] = area->maxX + margin; box[1][1] = area->minY - margin;
    box[2][0] = area->maxX + margin; box[2][1] = area->maxY + margin;
    box[3][0] = area->minX - margin; box[3][1] = area->maxY + margin;

    // Sutherland-Hodgman clip of the (convex) box by the line
    for ( i = 0; i < 4; i++ ) {
        const double* p = box[i];
        const double* q = box[(i + 1) % 4];
        double fp = da * p[0] + db * p[1] + dc;
        double fq = da * q[0] + db * q[1] + dc;
        if ( fp <= 0.0 ) {
            halfPlane.add(_WeilerAthertonTuple::of(p[0], p[1]));
        }
        if ( (fp < 0.0 && fq > 0.0) || (fp > 0.0 && fq < 0.0) ) {
            double t = fp / (fp - fq);
            halfPlane.add(_WeilerAthertonTuple::of(p[0] + t * (q[0] - p[0]),
                                                   p[1] + t * (q[1] - p[1])));
        }
    }
    if ( halfPlane.size() < 3 ) {
        return;
    }

    frontRegion = newPolygon();
    for ( k = 0; k < halfPlane.size(); k++ ) {
        frontRegion->addVertex(halfPlane[k].v[0], halfPlane[k].v[1]);
    }
    verifiedClip(newArea(frontRegion), area, front, back);
    for ( k = 0; k < front.size(); k++ ) {
        remaining.add(newPiece(front[k], piece->source));
    }
}

void
HiddenSurfaceRenderer::display(_WeilerAthertonPiece* piece)
{
    output.add(piece);
}

//= Verified clips ======================================================

/**
Clips a subject polygon to the area of a clip polygon with
`WeilerAthertonPolygonClipper`, verifying the result (see the class
documentation): if it is not consistent, the clip is repeated with the
clip polygon slightly moved. If no attempt succeeds, the clip is solved
by the half planes of the clip polygon (`clipByConvexPolygon`), or of
the convex trapezoids of a concave one (`trapezoids`).
@param clip clip polygon
@param subject polygon to clip
@param inside receives the parts of the subject inside the clip polygon
@param outside receives the parts of the subject outside of it
*/
void
HiddenSurfaceRenderer::verifiedClip(_WeilerAthertonArea* clip,
    _WeilerAthertonArea* subject, ArrayList<_WeilerAthertonArea*>& inside,
    ArrayList<_WeilerAthertonArea*>& outside)
{
    int attempt;
    ArrayList<_WeilerAthertonArea*> remaining;
    ArrayList<Polygon2D*> pieces;
    long int i;
    long int t;

    for ( attempt = 0; attempt <= MAXIMUM_RETRIES; attempt++ ) {
        double shift = attempt * RETRY_SHIFT;
        // Golden angle: the directions of the moves do not repeat
        double angle = attempt * 2.399963229728653;
        // C++ port note: the polygons of an attempt are deleted at its end
        // (addWithoutSlivers copies the contours it keeps)
        Polygon2D* clipPolygon = attempt == 0 ? clip->polygon :
            translated(clip->polygon, shift * Math::cos(angle),
                       shift * Math::sin(angle));
        Polygon2D* insidePolygon = new Polygon2D();
        Polygon2D* outsidePolygon = new Polygon2D();
        Polygon2D* insideClean;
        Polygon2D* outsideClean;
        BOOLEAN consistent;

        clipper.clipPolygons(clipPolygon, subject->polygon, insidePolygon,
            outsidePolygon);
        if ( clipper.isLastClipAbandoned() ) {
            delete insidePolygon;
            delete outsidePolygon;
            if ( attempt != 0 ) {
                delete clipPolygon;
            }
            continue;
        }
        insideClean = withoutDegenerateContours(insidePolygon);
        outsideClean = withoutDegenerateContours(outsidePolygon);
        consistent = isConsistentClip(clip, clipPolygon, subject, insideClean,
                                      outsideClean, shift);
        if ( consistent ) {
            addWithoutSlivers(inside, insideClean);
            addWithoutSlivers(outside, outsideClean);
        }
        delete insidePolygon;
        delete outsidePolygon;
        if ( insideClean != NULL ) {
            delete insideClean;
        }
        if ( outsideClean != NULL ) {
            delete outsideClean;
        }
        if ( attempt != 0 ) {
            delete clipPolygon;
        }
        if ( consistent ) {
            return;
        }
    }

    // The clipper could not solve the contacts: a convex clip polygon
    // (the usual case) is solved exactly by half planes instead, and a
    // concave one by the half planes of its trapezoids
    if ( isConvex(clip->polygon) ) {
        clipByConvexPolygon(clip->polygon, subject, inside, outside);
        return;
    }
    remaining.add(subject);
    pieces = trapezoids(clip->polygon);
    for ( t = 0; t < pieces.size(); t++ ) {
        Polygon2D* trapezoid = pieces[t];
        ArrayList<_WeilerAthertonArea*> next;
        _WeilerAthertonArea* trapezoidArea = newArea(trapezoid);
        for ( i = 0; i < remaining.size(); i++ ) {
            _WeilerAthertonArea* area = remaining[i];
            if ( area->overlapsBox(trapezoidArea) ) {
                clipByConvexPolygon(trapezoid, area, inside, next);
            }
            else {
                next.add(area);
            }
        }
        remaining = next;
    }
    outside.addAll(remaining);
}

/**
Decomposes a polygon (with holes, filled by the even-odd rule) in convex
trapezoids that do not overlap: between each pair of consecutive x
coordinates of its vertices, the edges crossing that vertical slab,
sorted from bottom to top, bound the polygon by pairs.
@return the trapezoids, counterclockwise and without repeated vertices
(owned by the arena)
*/
ArrayList<Polygon2D*>
HiddenSurfaceRenderer::trapezoids(Polygon2D* polygon)
{
    ArrayList<Polygon2D*> result;
    ArrayList<_WeilerAthertonTuple> edges;
    ArrayList<double> xs;
    _WeilerAthertonDoubleOrder doubleOrder;
    _WeilerAthertonMiddleOrder middleOrder;
    long int i;
    long int j;
    long int k;

    for ( k = 0; k < polygon->loops.size(); k++ ) {
        _Polygon2DContour* loop = polygon->loops[k];
        long int n = loop->vertices.size();
        for ( i = 0; i < n; i++ ) {
            const Vertex2D& p = loop->vertices[i];
            const Vertex2D& q = loop->vertices[(i + 1) % n];
            xs.add(p.x);
            if ( p.x != q.x ) {
                if ( p.x < q.x ) {
                    edges.add(_WeilerAthertonTuple::of(p.x, p.y, q.x, q.y));
                }
                else {
                    edges.add(_WeilerAthertonTuple::of(q.x, q.y, p.x, p.y));
                }
            }
        }
    }
    xs.sort(doubleOrder);
    for ( i = 0; i + 1 < xs.size(); i++ ) {
        double x0 = xs[i];
        double x1 = xs[i + 1];
        double xm;
        ArrayList<_WeilerAthertonTuple> crossing;
        if ( x1 <= x0 ) {
            continue;
        }
        xm = (x0 + x1) / 2.0;
        for ( k = 0; k < edges.size(); k++ ) {
            const double* e = edges[k].v;
            if ( e[0] <= x0 && e[2] >= x1 ) {
                double slope = (e[3] - e[1]) / (e[2] - e[0]);
                crossing.add(_WeilerAthertonTuple::of(e[1] + slope * (x0 - e[0]),
                    e[1] + slope * (x1 - e[0]), e[1] + slope * (xm - e[0])));
            }
        }
        crossing.sort(middleOrder);
        for ( j = 0; j + 1 < crossing.size(); j += 2 ) {
            _WeilerAthertonTuple low = crossing[j];
            _WeilerAthertonTuple high = crossing[j + 1];
            ArrayList<_WeilerAthertonTuple> corners;
            addDistinct(corners, x0, low.v[0]);
            addDistinct(corners, x1, low.v[1]);
            addDistinct(corners, x1, high.v[1]);
            addDistinct(corners, x0, high.v[0]);
            if ( corners.size() >= 3 &&
                 Math::abs(signedArea(corners)) > AREA_EPSILON ) {
                Polygon2D* trapezoid = newEmptyPolygon();
                arena->add(trapezoid);
                addLoop(trapezoid, corners);
                result.add(trapezoid);
            }
        }
    }
    return result;
}

void
HiddenSurfaceRenderer::addDistinct(ArrayList<_WeilerAthertonTuple>& points,
                                   double x, double y)
{
    if ( !points.isEmpty() ) {
        const double* last = points[points.size() - 1].v;
        const double* first = points[0].v;
        if ( Math::hypot(last[0] - x, last[1] - y) <= MINIMUM_EDGE_LENGTH ||
             Math::hypot(first[0] - x, first[1] - y) <= MINIMUM_EDGE_LENGTH ) {
            return;
        }
    }
    points.add(_WeilerAthertonTuple::of(x, y));
}

/**
Clips a polygon to a convex one, by the half planes of its edges
(Sutherland-Hodgman, [FOLE1992].3.14.1, for each contour): the inside part
is the subject within all the half planes, and the outside part is the
union of the disjoint parts outside of each edge and inside of the
previous ones. The parts may have degenerate edges along the clip edges,
harmless for their areas.
@param clip a convex counterclockwise polygon (see `isConvex`)
*/
void
HiddenSurfaceRenderer::clipByConvexPolygon(Polygon2D* clip,
    _WeilerAthertonArea* subject, ArrayList<_WeilerAthertonArea*>& inside,
    ArrayList<_WeilerAthertonArea*>& outside)
{
    const ArrayList<Vertex2D>& vertices = clip->loops[0]->vertices;
    Polygon2D* remaining = subject->polygon;
    long int n = vertices.size();
    long int i;

    for ( i = 0; i < n && remaining != NULL; i++ ) {
        const Vertex2D& p = vertices[i];
        const Vertex2D& q = vertices[(i + 1) % n];
        double a;
        double b;
        double c;
        Polygon2D* out;
        Polygon2D* next;
        // An edge of (almost) no length gives no direction: its half
        // plane would cut at random
        if ( Math::hypot(q.x - p.x, q.y - p.y) <= MINIMUM_EDGE_LENGTH ) {
            continue;
        }
        // Inside is at the left of the edge: a*x + b*y + c >= 0
        a = -(q.y - p.y);
        b = q.x - p.x;
        c = -(a * p.x + b * p.y);
        out = clipByHalfPlane(remaining, -a, -b, -c);
        if ( out != NULL ) {
            addWithoutSlivers(outside, out);
            delete out;
        }
        next = clipByHalfPlane(remaining, a, b, c);
        // C++ port note: the intermediate polygons are deleted (not the
        // polygon of the subject)
        if ( remaining != subject->polygon ) {
            delete remaining;
        }
        remaining = next;
    }
    if ( remaining != NULL ) {
        addWithoutSlivers(inside, remaining);
        if ( remaining != subject->polygon ) {
            delete remaining;
        }
    }
}

/**
@return the part of a polygon where a*x + b*y + c >= 0 (each contour
clipped by Sutherland-Hodgman), or null if nothing is left (C++ port note:
owned by the caller)
*/
Polygon2D*
HiddenSurfaceRenderer::clipByHalfPlane(Polygon2D* polygon, double a,
                                       double b, double c)
{
    Polygon2D* result = newEmptyPolygon();
    Polygon2D* clean;
    long int i;
    long int k;

    for ( k = 0; k < polygon->loops.size(); k++ ) {
        _Polygon2DContour* loop = polygon->loops[k];
        ArrayList<_WeilerAthertonTuple> clipped;
        long int n = loop->vertices.size();
        for ( i = 0; i < n; i++ ) {
            const Vertex2D& p = loop->vertices[i];
            const Vertex2D& q = loop->vertices[(i + 1) % n];
            double fp = a * p.x + b * p.y + c;
            double fq = a * q.x + b * q.y + c;
            if ( fp >= 0.0 ) {
                clipped.add(_WeilerAthertonTuple::of(p.x, p.y));
            }
            if ( (fp >= 0.0) != (fq >= 0.0) ) {
                double t = fp / (fp - fq);
                clipped.add(_WeilerAthertonTuple::of(p.x + t * (q.x - p.x),
                                                     p.y + t * (q.y - p.y)));
            }
        }
        if ( clipped.size() >= 3 ) {
            addLoop(result, clipped);
        }
    }
    clean = withoutDegenerateContours(result);
    delete result;
    return clean;
}

/**
@return true if the polygon has one contour, counterclockwise and convex
*/
BOOLEAN
HiddenSurfaceRenderer::isConvex(Polygon2D* polygon)
{
    long int n;
    long int i;

    if ( polygon->loops.size() != 1 ) {
        return FALSE;
    }
    const ArrayList<Vertex2D>& vertices = polygon->loops[0]->vertices;
    n = vertices.size();
    if ( n < 3 || signedArea(polygon->loops[0]) <= 0.0 ) {
        return FALSE;
    }
    for ( i = 0; i < n; i++ ) {
        const Vertex2D& p = vertices[i];
        const Vertex2D& q = vertices[(i + 1) % n];
        const Vertex2D& r = vertices[(i + 2) % n];
        double cross = (q.x - p.x) * (r.y - q.y) - (q.y - p.y) * (r.x - q.x);
        double scale = Math::hypot(q.x - p.x, q.y - p.y) *
            Math::hypot(r.x - q.x, r.y - q.y);
        if ( cross < -1.0e-9 * scale ) {
            return FALSE;
        }
    }
    return TRUE;
}

/**
@return true if the inside and outside parts of a clip add up to the
subject and are within the expected bounds
*/
BOOLEAN
HiddenSurfaceRenderer::isConsistentClip(_WeilerAthertonArea* clip,
    Polygon2D* clipPolygon, _WeilerAthertonArea* subject, Polygon2D* inside,
    Polygon2D* outside, double shift)
{
    double insideArea = inside != NULL ? enclosedArea(inside) : 0.0;
    double outsideArea = outside != NULL ? enclosedArea(outside) : 0.0;
    double length = shift + SLIVER_WIDTH;
    double tolerance = 1.0e-7 * subject->area + AREA_EPSILON +
        4.0 * length * (subject->perimeter + 2.0 *
            ((subject->maxX - subject->minX) + (subject->maxY - subject->minY)));
    double slack;
    ArrayList<_WeilerAthertonTuple> samples;
    _WeilerAthertonEdgeGrid* clipBorder;
    _WeilerAthertonEdgeGrid* subjectBorder;
    _WeilerAthertonEdgeGrid* insideBorder;
    _WeilerAthertonEdgeGrid* outsideBorder;
    BOOLEAN consistent;
    long int i;

    if ( insideArea < -tolerance || outsideArea < -tolerance ) {
        return FALSE;
    }
    if ( Math::abs(insideArea + outsideArea - subject->area) > tolerance ) {
        return FALSE;
    }
    if ( insideArea > Math::min(subject->area, clip->area) + tolerance ) {
        return FALSE;
    }
    if ( inside != NULL ) {
        double box[4];
        boundingBox(inside, box);
        slack = 2.0 * length;
        if ( box[0] < Math::max(subject->minX, clip->minX) - slack ||
             box[1] < Math::max(subject->minY, clip->minY) - slack ||
             box[2] > Math::min(subject->maxX, clip->maxX) + slack ||
             box[3] > Math::min(subject->maxY, clip->maxY) + slack ) {
            return FALSE;
        }
    }
    if ( outside != NULL ) {
        double box[4];
        boundingBox(outside, box);
        slack = 2.0 * length;
        if ( box[0] < subject->minX - slack || box[1] < subject->minY - slack ||
             box[2] > subject->maxX + slack || box[3] > subject->maxY + slack ) {
            return FALSE;
        }
    }
    // At sample points of the four polygons, far from all their
    // borders, a point is in the inside part if and only if it is in the
    // subject and in the clip polygon, and it is in the outside part if
    // and only if it is in the subject and not in the clip polygon. It
    // fails when the clipper misses contacts of the contours: i.e. a
    // piece inscribed in the clip polygon (all its vertices on its
    // border), or a contour of the subject identical to the clip polygon
    slack = 2.0 * length;
    samplePoints(clipPolygon, samples);
    samplePoints(subject->polygon, samples);
    if ( inside != NULL ) {
        samplePoints(inside, samples);
    }
    if ( outside != NULL ) {
        samplePoints(outside, samples);
    }
    clipBorder = new _WeilerAthertonEdgeGrid(clipPolygon, slack);
    subjectBorder = new _WeilerAthertonEdgeGrid(subject->polygon, slack);
    insideBorder = NULL;
    if ( inside != NULL ) {
        insideBorder = new _WeilerAthertonEdgeGrid(inside, slack);
    }
    outsideBorder = NULL;
    if ( outside != NULL ) {
        outsideBorder = new _WeilerAthertonEdgeGrid(outside, slack);
    }
    consistent = TRUE;
    for ( i = 0; i < samples.size(); i++ ) {
        double x = samples[i].v[0];
        double y = samples[i].v[1];
        BOOLEAN inSubject;
        BOOLEAN inClip;
        if ( clipBorder->isNearBorder(x, y) ||
             subjectBorder->isNearBorder(x, y) ) {
            continue;
        }
        inSubject = contains(subject->polygon, x, y);
        inClip = contains(clipPolygon, x, y);
        if ( !isOnExpectedSide(inside, insideBorder, x, y,
                               inSubject && inClip) ||
             !isOnExpectedSide(outside, outsideBorder, x, y,
                               inSubject && !inClip) ) {
            consistent = FALSE;
            break;
        }
    }
    delete clipBorder;
    delete subjectBorder;
    if ( insideBorder != NULL ) {
        delete insideBorder;
    }
    if ( outsideBorder != NULL ) {
        delete outsideBorder;
    }
    return consistent;
}

/**
@param part a part of the clip result (null if empty)
@param border index of the edges of the part
@param expected true if the point must be in the part
@return true if the point is in the part as expected, or it is too close
to its border to tell
*/
BOOLEAN
HiddenSurfaceRenderer::isOnExpectedSide(Polygon2D* part,
    const _WeilerAthertonEdgeGrid* border, double x, double y,
    BOOLEAN expected)
{
    if ( part == NULL ) {
        return !expected;
    }
    return (contains(part, x, y) ? TRUE : FALSE) == (expected ? TRUE : FALSE) ||
        border->isNearBorder(x, y);
}

/**
Appends the sample points of a polygon, as {x, y} pairs: its vertices, the
middle points of its edges, and the centroids of the triangles at its
vertices (inside the polygon at its ears). C++ port note: the Java version
returns them in a new list.
*/
void
HiddenSurfaceRenderer::samplePoints(Polygon2D* polygon,
                                    ArrayList<_WeilerAthertonTuple>& points)
{
    long int i;
    long int k;

    for ( k = 0; k < polygon->loops.size(); k++ ) {
        _Polygon2DContour* loop = polygon->loops[k];
        long int n = loop->vertices.size();
        for ( i = 0; i < n; i++ ) {
            const Vertex2D& p = loop->vertices[(i + n - 1) % n];
            const Vertex2D& q = loop->vertices[i];
            const Vertex2D& r = loop->vertices[(i + 1) % n];
            points.add(_WeilerAthertonTuple::of(q.x, q.y));
            points.add(_WeilerAthertonTuple::of((q.x + r.x) / 2.0,
                (q.y + r.y) / 2.0));
            points.add(_WeilerAthertonTuple::of((p.x + q.x + r.x) / 3.0,
                (p.y + q.y + r.y) / 3.0));
        }
    }
}

/**
Adds the parts of a clip result to a list, discarding the contours that
are degenerate strips (see `SLIVER_WIDTH`). C++ port note: the contours
kept are copied, so the polygon stays owned by the caller.
*/
void
HiddenSurfaceRenderer::addWithoutSlivers(ArrayList<_WeilerAthertonArea*>& list,
                                         Polygon2D* polygon)
{
    Polygon2D* clean;
    BOOLEAN empty = TRUE;
    long int k;

    if ( polygon == NULL ) {
        return;
    }
    clean = newEmptyPolygon();
    for ( k = 0; k < polygon->loops.size(); k++ ) {
        _Polygon2DContour* loop = polygon->loops[k];
        double area = Math::abs(signedArea(loop));
        double halfPerimeter = perimeter(loop) / 2.0;
        if ( halfPerimeter <= 0.0 || area / halfPerimeter < SLIVER_WIDTH ) {
            continue;
        }
        copyLoop(clean, loop);
        empty = FALSE;
    }
    if ( !empty ) {
        _WeilerAthertonArea* area;
        arena->add(clean);
        area = newArea(clean);
        if ( area->area > AREA_EPSILON ) {
            list.add(area);
        }
    }
    else {
        delete clean;
    }
}

//= Input polygons ======================================================

/**
Transforms the faces of the solids to the viewing space, removing the
faces facing away from the observer and clipping them to the view.
*/
ArrayList<_WeilerAthertonSourcePolygon*>
HiddenSurfaceRenderer::buildInputPolygons(ArrayList<SimpleBody*>& solids,
                                          Camera* camera)
{
    ArrayList<_WeilerAthertonSourcePolygon*> sources;
    Matrix4x4d projection = camera->calculateProjectionMatrix();
    // C++ port note: qualified, as the local variable hides the method
    double depthSign = HiddenSurfaceRenderer::depthSign(camera, projection);
    Polygon2D* windowPolygon = newPolygon();
    _WeilerAthertonArea* window;
    long int b;
    long int i;

    windowPolygon->addVertex(-SCALE, -SCALE);
    windowPolygon->addVertex(SCALE, -SCALE);
    windowPolygon->addVertex(SCALE, SCALE);
    windowPolygon->addVertex(-SCALE, SCALE);
    window = newArea(windowPolygon);

    for ( b = 0; b < solids.size(); b++ ) {
        SimpleBody* body = solids[b];
        Geometry* geometry = body->getGeometry();
        PolyhedralBoundedSolid* brep;
        Matrix4x4d transform;
        if ( geometry == NULL ||
             !geometry->instanceOf(PolyhedralBoundedSolid::CLASS) ) {
            continue;
        }
        brep = (PolyhedralBoundedSolid*)geometry;
        transform = body->getTransformationMatrix();
        for ( i = 0; i < brep->getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace* face = brep->getPolygonsList().get(i);
            _WeilerAthertonSourcePolygon* source = buildInputPolygon(face,
                body, transform, camera, projection, depthSign, window);
            if ( source != NULL ) {
                sources.add(source);
            }
        }
    }
    return sources;
}

_WeilerAthertonSourcePolygon*
HiddenSurfaceRenderer::buildInputPolygon(_PolyhedralBoundedSolidFace* face,
    SimpleBody* body, const Matrix4x4d& transform, Camera* camera,
    const Matrix4x4d& projection, double depthSign,
    _WeilerAthertonArea* window)
{
    InfinitePlane* worldPlane = worldContainingPlane(face, body, transform);
    ArrayList<ArrayList<_WeilerAthertonTuple> > loops;
    BOOLEAN complete = TRUE;
    long int outer;
    _WeilerAthertonSourcePolygon* source;
    Polygon2D* projected;
    _WeilerAthertonArea* templateArea;
    long int i;
    long int j;
    long int k;

    if ( worldPlane == NULL || !isFrontFace(worldPlane, camera) ) {
        return NULL;
    }

    // Projection of each loop, clipped in homogeneous coordinates against
    // the near and far planes
    for ( i = 0; i < face->boundariesList.size(); i++ ) {
        _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(i);
        ArrayList<Vector4Dd> clipLoop;
        ArrayList<_WeilerAthertonTuple> projectedLoop;
        _PolyhedralBoundedSolidHalfEdge* he;
        _PolyhedralBoundedSolidHalfEdge* heStart;
        if ( loop == NULL || loop->boundaryStartHalfEdge == NULL ) {
            continue;
        }
        he = loop->boundaryStartHalfEdge;
        heStart = he;
        do {
            if ( he == NULL || he->startingVertex == NULL ) {
                break;
            }
            clipLoop.add(projection.multiply(Vector4Dd(
                transform.multiply(he->startingVertex->position))));
            he = he->next();
        } while ( he != heStart );

        for ( j = 0; j < clipLoop.size(); j++ ) {
            const Vector4Dd& p = clipLoop[j];
            if ( p.z() + p.w() < 0.0 || p.w() - p.z() < 0.0 ) {
                complete = FALSE;
            }
        }
        clipLoop = clipAgainstPlane(clipLoop, 1.0);
        clipLoop = clipAgainstPlane(clipLoop, -1.0);
        if ( clipLoop.size() < 3 ) {
            complete = FALSE;
            continue;
        }
        for ( j = 0; j < clipLoop.size(); j++ ) {
            Vector4Dd ndc = clipLoop[j].dividedByW();
            projectedLoop.add(_WeilerAthertonTuple::of(SCALE * ndc.x(),
                SCALE * ndc.y(), depthSign * ndc.z()));
        }
        if ( Math::abs(signedArea(projectedLoop)) > AREA_EPSILON ) {
            loops.add(projectedLoop);
        }
    }
    if ( loops.isEmpty() ) {
        return NULL;
    }

    // The polygon clipper follows main contours counterclockwise and
    // holes clockwise: the main contour is the one of largest area
    outer = 0;
    for ( i = 1; i < loops.size(); i++ ) {
        if ( Math::abs(signedArea(loops[i])) >
             Math::abs(signedArea(loops[outer])) ) {
            outer = i;
        }
    }
    for ( i = 0; i < loops.size(); i++ ) {
        double area = signedArea(loops[i]);
        if ( (i == outer && area < 0.0) || (i != outer && area > 0.0) ) {
            Collections<_WeilerAthertonTuple>::reverse(loops[i]);
        }
    }

    source = new _WeilerAthertonSourcePolygon();
    arena->add(source);
    if ( !fitDepthPlane(loops[outer], source) ) {
        // Seen edge-on
        return NULL;
    }

    projected = newEmptyPolygon();
    arena->add(projected);
    addLoop(projected, loops[outer]);
    for ( i = 0; i < loops.size(); i++ ) {
        if ( i != outer ) {
            addLoop(projected, loops[i]);
        }
    }

    // Clip to the window of the view
    templateArea = newArea(projected);
    if ( !templateArea->overlapsBox(window) ) {
        return NULL;
    }
    if ( templateArea->minX < window->minX || templateArea->maxX > window->maxX ||
         templateArea->minY < window->minY || templateArea->maxY > window->maxY ) {
        ArrayList<_WeilerAthertonArea*> inside;
        ArrayList<_WeilerAthertonArea*> outside;
        verifiedClip(window, templateArea, inside, outside);
        if ( inside.isEmpty() ) {
            return NULL;
        }
        templateArea = merged(inside);
        complete = FALSE;
    }

    source->templateArea = templateArea;
    source->nearestDepth = Double::MAX_VALUE;
    for ( k = 0; k < templateArea->polygon->loops.size(); k++ ) {
        _Polygon2DContour* loop = templateArea->polygon->loops[k];
        for ( j = 0; j < loop->vertices.size(); j++ ) {
            const Vertex2D& v = loop->vertices[j];
            source->nearestDepth = Math::min(source->nearestDepth,
                source->depthAt(v.x, v.y));
        }
    }
    source->body = body;
    source->face = face;
    source->worldPlane = worldPlane;
    source->complete = complete;
    return source;
}

/**
Sutherland-Hodgman clip of a loop in homogeneous coordinates against the
near (side = 1: z >= -w) or far (side = -1: z <= w) plane.
*/
ArrayList<Vector4Dd>
HiddenSurfaceRenderer::clipAgainstPlane(const ArrayList<Vector4Dd>& loop,
                                        double side)
{
    ArrayList<Vector4Dd> result;
    long int n = loop.size();
    long int i;

    for ( i = 0; i < n; i++ ) {
        const Vector4Dd& p = loop[i];
        const Vector4Dd& q = loop[(i + 1) % n];
        double dp = side * p.z() + p.w();
        double dq = side * q.z() + q.w();
        if ( dp >= 0.0 ) {
            result.add(p);
        }
        if ( (dp >= 0.0) != (dq >= 0.0) ) {
            double t = dp / (dp - dq);
            result.add(p.multiply(1.0 - t).add(q.multiply(t)));
        }
    }
    return result;
}

/**
Fits the depth plane of a projected loop with Newell's method (robust for
concave loops), as depth = a*x + b*y + c.
@return false if the plane is parallel to the line of sight
*/
BOOLEAN
HiddenSurfaceRenderer::fitDepthPlane(
    const ArrayList<_WeilerAthertonTuple>& loop,
    _WeilerAthertonSourcePolygon* source)
{
    double nx = 0.0;
    double ny = 0.0;
    double nz = 0.0;
    double cx = 0.0;
    double cy = 0.0;
    double cz = 0.0;
    long int n = loop.size();
    long int i;
    double length;

    for ( i = 0; i < n; i++ ) {
        const double* p = loop[i].v;
        const double* q = loop[(i + 1) % n].v;
        nx += (p[1] - q[1]) * (p[2] + q[2]);
        ny += (p[2] - q[2]) * (p[0] + q[0]);
        nz += (p[0] - q[0]) * (p[1] + q[1]);
        cx += p[0];
        cy += p[1];
        cz += p[2];
    }
    length = Math::sqrt(nx * nx + ny * ny + nz * nz);
    if ( length <= VSDK::EPSILON * VSDK::EPSILON ||
         Math::abs(nz) <= 1.0e-12 * length ) {
        return FALSE;
    }
    cx /= n;
    cy /= n;
    cz /= n;
    source->depthSlopeX = -nx / nz;
    source->depthSlopeY = -ny / nz;
    source->depthOffset = cz - source->depthSlopeX * cx - source->depthSlopeY * cy;
    return TRUE;
}

/**
@return 1 if the depth of the normalized device coordinates grows away
from the observer, -1 otherwise, so the depths used by the algorithm
are always smaller closer to the observer
*/
double
HiddenSurfaceRenderer::depthSign(Camera* camera, const Matrix4x4d& projection)
{
    Vector3Dd front = camera->getFront().normalized();
    // C++ port note: `near` and `far` are keywords of Turbo C++
    double nearDistance = camera->getNearPlaneDistance();
    double farDistance = camera->getFarPlaneDistance();
    Vector3Dd p0 = camera->getPosition().add(front.multiply(
        nearDistance + 0.25 * (farDistance - nearDistance)));
    Vector3Dd p1 = camera->getPosition().add(front.multiply(
        nearDistance + 0.75 * (farDistance - nearDistance)));
    double z0 = projection.multiply(Vector4Dd(p0)).dividedByW().z();
    double z1 = projection.multiply(Vector4Dd(p1)).dividedByW().z();
    return z1 >= z0 ? 1.0 : -1.0;
}

/**
Backplane removal: a face of a closed solid is seen only from the side
its outward normal points to.
*/
BOOLEAN
HiddenSurfaceRenderer::isFrontFace(const InfinitePlane* worldPlane,
                                   Camera* camera)
{
    Vector3Dd n = worldPlane->getNormal();
    double distance;

    if ( camera->getProjectionMode() == Camera::PROJECTION_MODE_ORTHOGONAL ) {
        return n.dotProduct(camera->getFront()) < 0.0;
    }
    // Signed distance from the plane to the eye
    distance = n.x() * camera->getPosition().x() +
        n.y() * camera->getPosition().y() +
        n.z() * camera->getPosition().z() + worldPlane->getD();
    return distance > 0.0;
}

/**
@return the plane of a face in world coordinates, with its outward
normal (from the maintained plane of the face, a Newell fit, not from
three vertices that could be at a reflex corner), or null if undefined
(owned by the arena)
*/
InfinitePlane*
HiddenSurfaceRenderer::worldContainingPlane(_PolyhedralBoundedSolidFace* face,
    SimpleBody* body, const Matrix4x4d& transform)
{
    InfinitePlane* localPlane = face->getContainingPlane();
    Vector3Dd pointOnFace;
    BOOLEAN withPoint = FALSE;
    Vector3Dd worldNormal;
    InfinitePlane* plane;
    long int i;

    if ( localPlane == NULL ) {
        return NULL;
    }
    for ( i = 0; i < face->boundariesList.size() && !withPoint; i++ ) {
        _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(i);
        _PolyhedralBoundedSolidHalfEdge* he =
            loop != NULL ? loop->boundaryStartHalfEdge :
            (_PolyhedralBoundedSolidHalfEdge*)NULL;
        if ( he != NULL && he->startingVertex != NULL ) {
            pointOnFace = he->startingVertex->position;
            withPoint = TRUE;
        }
    }
    if ( !withPoint ) {
        delete localPlane;
        return NULL;
    }
    worldNormal = body->transformNormalToWorld(localPlane->getNormal());
    delete localPlane;
    if ( worldNormal.length() <= VSDK::EPSILON ) {
        return NULL;
    }
    plane = new InfinitePlane(worldNormal.normalized(),
        transform.multiply(pointOnFace));
    arena->add(plane);
    return plane;
}

//= 2D polygon utilities ================================================

void
HiddenSurfaceRenderer::addLoop(Polygon2D* polygon,
                               const ArrayList<_WeilerAthertonTuple>& loop)
{
    long int i;

    polygon->nextLoop();
    for ( i = 0; i < loop.size(); i++ ) {
        polygon->addVertex(loop[i].v[0], loop[i].v[1]);
    }
}

void
HiddenSurfaceRenderer::copyLoop(Polygon2D* polygon, _Polygon2DContour* loop)
{
    long int i;

    polygon->nextLoop();
    for ( i = 0; i < loop->vertices.size(); i++ ) {
        polygon->addVertex(loop->vertices[i].x, loop->vertices[i].y);
    }
}

/**
@return a moved copy of a polygon (C++ port note: owned by the caller)
*/
Polygon2D*
HiddenSurfaceRenderer::translated(Polygon2D* polygon, double dx, double dy)
{
    Polygon2D* result = newEmptyPolygon();
    long int i;
    long int k;

    for ( k = 0; k < polygon->loops.size(); k++ ) {
        _Polygon2DContour* loop = polygon->loops[k];
        result->nextLoop();
        for ( i = 0; i < loop->vertices.size(); i++ ) {
            result->addVertex(loop->vertices[i].x + dx,
                              loop->vertices[i].y + dy);
        }
    }
    return result;
}

/**
@return one area with all the contours of several areas (that do not
overlap)
*/
_WeilerAthertonArea*
HiddenSurfaceRenderer::merged(const ArrayList<_WeilerAthertonArea*>& areas)
{
    Polygon2D* result;
    long int i;
    long int k;

    if ( areas.size() == 1 ) {
        return areas[0];
    }
    result = newEmptyPolygon();
    arena->add(result);
    for ( i = 0; i < areas.size(); i++ ) {
        Polygon2D* polygon = areas[i]->polygon;
        for ( k = 0; k < polygon->loops.size(); k++ ) {
            copyLoop(result, polygon->loops[k]);
        }
    }
    return newArea(result);
}

double
HiddenSurfaceRenderer::signedArea(const ArrayList<_WeilerAthertonTuple>& loop)
{
    double area = 0.0;
    long int n = loop.size();
    long int i;

    for ( i = 0; i < n; i++ ) {
        const double* p = loop[i].v;
        const double* q = loop[(i + 1) % n].v;
        area += p[0] * q[1] - q[0] * p[1];
    }
    return area / 2.0;
}

double
HiddenSurfaceRenderer::signedArea(_Polygon2DContour* loop)
{
    double area = 0.0;
    long int n = loop->vertices.size();
    long int i;

    for ( i = 0; i < n; i++ ) {
        const Vertex2D& p = loop->vertices[i];
        const Vertex2D& q = loop->vertices[(i + 1) % n];
        area += p.x * q.y - q.x * p.y;
    }
    return area / 2.0;
}

double
HiddenSurfaceRenderer::perimeter(_Polygon2DContour* loop)
{
    double length = 0.0;
    long int n = loop->vertices.size();
    long int i;

    for ( i = 0; i < n; i++ ) {
        const Vertex2D& p = loop->vertices[i];
        const Vertex2D& q = loop->vertices[(i + 1) % n];
        length += Math::hypot(q.x - p.x, q.y - p.y);
    }
    return length;
}

double
HiddenSurfaceRenderer::perimeter(Polygon2D* polygon)
{
    double length = 0.0;
    long int k;

    for ( k = 0; k < polygon->loops.size(); k++ ) {
        length += perimeter(polygon->loops[k]);
    }
    return length;
}

/**
@return the area enclosed by the contours of a polygon: its main contours
are counterclockwise (positive area) and its holes clockwise (negative
area), as the clipper keeps them
*/
double
HiddenSurfaceRenderer::enclosedArea(Polygon2D* polygon)
{
    double area = 0.0;
    long int k;

    for ( k = 0; k < polygon->loops.size(); k++ ) {
        area += signedArea(polygon->loops[k]);
    }
    return area;
}

/**
@return a copy of a clip result without its degenerate contours (with
less than three vertices or with no area), or null if no contour is left
(C++ port note: owned by the caller)
*/
Polygon2D*
HiddenSurfaceRenderer::withoutDegenerateContours(Polygon2D* polygon)
{
    Polygon2D* clean = newEmptyPolygon();
    BOOLEAN empty = TRUE;
    long int k;

    for ( k = 0; k < polygon->loops.size(); k++ ) {
        _Polygon2DContour* loop = polygon->loops[k];
        if ( loop->vertices.size() < 3 ||
             Math::abs(signedArea(loop)) <= AREA_EPSILON ) {
            continue;
        }
        copyLoop(clean, loop);
        empty = FALSE;
    }
    if ( empty ) {
        delete clean;
        return NULL;
    }
    return clean;
}

/**
@return true if the point is inside the polygon, for the even-odd rule
*/
BOOLEAN
HiddenSurfaceRenderer::contains(Polygon2D* polygon, double x, double y)
{
    BOOLEAN inside = FALSE;
    long int k;

    for ( k = 0; k < polygon->loops.size(); k++ ) {
        _Polygon2DContour* loop = polygon->loops[k];
        if ( loop->vertices.size() >= 3 && containsInLoop(loop, x, y) ) {
            inside = !inside;
        }
    }
    return inside;
}

BOOLEAN
HiddenSurfaceRenderer::containsInLoop(_Polygon2DContour* loop, double x,
                                      double y)
{
    BOOLEAN inside = FALSE;
    long int n = loop->vertices.size();
    long int i;
    long int j;

    for ( i = 0, j = n - 1; i < n; j = i++ ) {
        const Vertex2D& a = loop->vertices[i];
        const Vertex2D& b = loop->vertices[j];
        if ( (a.y > y) != (b.y > y) &&
             x < (b.x - a.x) * (y - a.y) / (b.y - a.y) + a.x ) {
            inside = !inside;
        }
    }
    return inside;
}

/**
@param box receives {minX, minY, maxX, maxY} of the vertices of a polygon
*/
void
HiddenSurfaceRenderer::boundingBox(Polygon2D* polygon, double box[4])
{
    long int i;
    long int k;

    box[0] = Double::MAX_VALUE;
    box[1] = Double::MAX_VALUE;
    box[2] = -Double::MAX_VALUE;
    box[3] = -Double::MAX_VALUE;
    for ( k = 0; k < polygon->loops.size(); k++ ) {
        _Polygon2DContour* loop = polygon->loops[k];
        for ( i = 0; i < loop->vertices.size(); i++ ) {
            const Vertex2D& v = loop->vertices[i];
            box[0] = Math::min(box[0], v.x);
            box[1] = Math::min(box[1], v.y);
            box[2] = Math::max(box[2], v.x);
            box[3] = Math::max(box[3], v.y);
        }
    }
}
