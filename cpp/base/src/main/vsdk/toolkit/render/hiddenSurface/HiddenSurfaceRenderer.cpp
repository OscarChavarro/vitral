//= References:                                                             =
//= [WEIL1977] Weiler, Kevin. Atherton, Peter. "Hidden surface removal      =
//=          using polygon area sorting". SIGGRAPH proceedings, 1977.       =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics,           =
//=          principles and practice" - second edition, Addison Wesley,     =
//=          1992.                                                          =

#include <cfloat>
#include <cmath>
#include <typeinfo>

#include "java/lang/Double.h"
#include "java/lang/Math.h"
#include "java/util/ArrayList.txx"
#include "java/util/Collections.txx"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/element/Vertex2D.h"
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
#include "vsdk/toolkit/render/hiddenSurface/HiddenSurfaceRenderer.h"

constexpr double HiddenSurfaceRenderer::SCALE;
constexpr double HiddenSurfaceRenderer::DEPTH_EPSILON;
constexpr double HiddenSurfaceRenderer::AREA_EPSILON;
constexpr double HiddenSurfaceRenderer::SLIVER_WIDTH;
constexpr double HiddenSurfaceRenderer::RETRY_SHIFT;
constexpr double HiddenSurfaceRenderer::MINIMUM_EDGE_LENGTH;
constexpr double HiddenSurfaceRenderer::LIGHT_VIEW_MARGIN;
constexpr double HiddenSurfaceRenderer::MAXIMUM_LIGHT_HALF_ANGLE;
constexpr double HiddenSurfaceRenderer::CUBE_VIEW_OVERLAP;

/**
Emulation of the Java conversion of a double to an int: NaN is 0 and the
values out of range saturate.
*/
static int
hiddenSurfaceToInt(double value)
{
    if ( value != value ) {
        return 0;
    }
    if ( value >= 2147483647.0 ) {
        return 2147483647;
    }
    if ( value <= -2147483648.0 ) {
        return -2147483647 - 1;
    }
    return (int)value;
}

//= _WeilerAthertonArea ===================================================

_WeilerAthertonArea::_WeilerAthertonArea(Polygon2D* inPolygon)
    : polygon(inPolygon)
{
    double box[4];
    HiddenSurfaceRenderer::boundingBox(polygon, box);
    minX = box[0];
    minY = box[1];
    maxX = box[2];
    maxY = box[3];
    area = HiddenSurfaceRenderer::enclosedArea(polygon);
    perimeter = HiddenSurfaceRenderer::perimeter(polygon);
}

_WeilerAthertonArea::~_WeilerAthertonArea()
{
    delete polygon;
}

bool
_WeilerAthertonArea::overlapsBox(const _WeilerAthertonArea* other) const
{
    return maxX >= other->minX && minX <= other->maxX &&
        maxY >= other->minY && minY <= other->maxY;
}

//= _WeilerAthertonSourcePolygon ==========================================

_WeilerAthertonSourcePolygon::_WeilerAthertonSourcePolygon()
    : templateArea(nullptr), a(0.0), b(0.0), c(0.0), nearestDepth(0.0),
      body(nullptr), face(nullptr), worldPlane(nullptr), complete(false),
      original(this)
{
}

double
_WeilerAthertonSourcePolygon::depthAt(double x, double y) const
{
    return a * x + b * y + c;
}

//= _WeilerAthertonPiece ==================================================

_WeilerAthertonPiece::_WeilerAthertonPiece(_WeilerAthertonArea* inArea,
    _WeilerAthertonSourcePolygon* inSource)
    : area(inArea), source(inSource)
{
}

//= _WeilerAthertonLitFace ================================================

_WeilerAthertonLitFace::_WeilerAthertonLitFace() : whole(false)
{
}

//= _WeilerAthertonEdgeGrid ===============================================

_WeilerAthertonEdgeGrid::_WeilerAthertonEdgeGrid(Polygon2D* polygon,
                                                 double inTolerance)
{
    double box[4];
    int edges = 0;

    HiddenSurfaceRenderer::boundingBox(polygon, box);
    for ( long l = 0; l < polygon->loops.size(); l++ ) {
        edges += (int)polygon->loops[l]->vertices.size();
    }
    tolerance = inTolerance;
    minX = box[0] - tolerance;
    minY = box[1] - tolerance;
    int side = java::Math::max(1, hiddenSurfaceToInt(std::sqrt((double)edges)));
    columns = side;
    rows = side;
    cellWidth = java::Math::max((box[2] + tolerance - minX) / columns,
        DBL_MIN);
    cellHeight = java::Math::max((box[3] + tolerance - minY) / rows,
        DBL_MIN);
    for ( int i = 0; i < columns * rows; i++ ) {
        cells.add(java::ArrayList<Vector4Dd>());
    }
    for ( long l = 0; l < polygon->loops.size(); l++ ) {
        _Polygon2DContour* loop = polygon->loops[l];
        long n = loop->vertices.size();
        for ( long i = 0; i < n; i++ ) {
            const Vertex2D& a = loop->vertices[i];
            const Vertex2D& b = loop->vertices[(i + 1) % n];
            Vector4Dd edge(a.x, a.y, b.x, b.y);
            int c0 = column(java::Math::min(a.x, b.x) - tolerance);
            int c1 = column(java::Math::max(a.x, b.x) + tolerance);
            int r0 = row(java::Math::min(a.y, b.y) - tolerance);
            int r1 = row(java::Math::max(a.y, b.y) + tolerance);
            for ( int r = r0; r <= r1; r++ ) {
                for ( int c = c0; c <= c1; c++ ) {
                    cells[r * columns + c].add(edge);
                }
            }
        }
    }
}

int
_WeilerAthertonEdgeGrid::column(double x) const
{
    return java::Math::max(0, java::Math::min(columns - 1,
        hiddenSurfaceToInt((x - minX) / cellWidth)));
}

int
_WeilerAthertonEdgeGrid::row(double y) const
{
    return java::Math::max(0, java::Math::min(rows - 1,
        hiddenSurfaceToInt((y - minY) / cellHeight)));
}

bool
_WeilerAthertonEdgeGrid::isNearBorder(double x, double y) const
{
    if ( x < minX || y < minY || x > minX + columns * cellWidth ||
         y > minY + rows * cellHeight ) {
        return false;
    }
    const java::ArrayList<Vector4Dd>& cell = cells[row(y) * columns + column(x)];
    if ( cell.isEmpty() ) {
        return false;
    }
    double tolerance2 = tolerance * tolerance;
    for ( long i = 0; i < cell.size(); i++ ) {
        const Vector4Dd& e = cell[i];
        double dx = e.z() - e.x();
        double dy = e.w() - e.y();
        double length2 = dx * dx + dy * dy;
        double t = length2 > 0.0 ? java::Math::max(0.0, java::Math::min(1.0,
            ((x - e.x()) * dx + (y - e.y()) * dy) / length2)) : 0.0;
        double ex = e.x() + t * dx - x;
        double ey = e.y() + t * dy - y;
        if ( ex * ex + ey * ey <= tolerance2 ) {
            return true;
        }
    }
    return false;
}

//= _WeilerAthertonShadedPart =============================================

_WeilerAthertonShadedPart::_WeilerAthertonShadedPart() : area(nullptr)
{
}

_WeilerAthertonShadedPart::_WeilerAthertonShadedPart(
    _WeilerAthertonArea* inArea, const java::ArrayList<Light*>& inLights)
    : area(inArea), lights(inLights)
{
}

//= HiddenSurfaceRenderer::VisiblePolygon =================================

HiddenSurfaceRenderer::VisiblePolygon::VisiblePolygon(Polygon2D* inArea,
    _WeilerAthertonSourcePolygon* source, java::ArrayList<Light*>* inLights)
    : area(inArea), body(source->body), face(source->face),
      worldPlane(*source->worldPlane), lights(inLights)
{
}

HiddenSurfaceRenderer::VisiblePolygon::~VisiblePolygon()
{
    delete area;
    delete lights;
}

//= HiddenSurfaceRenderer =================================================

HiddenSurfaceRenderer::HiddenSurfaceRenderer()
{
}

HiddenSurfaceRenderer::~HiddenSurfaceRenderer()
{
    for ( long i = 0; i < ownedPieces.size(); i++ ) {
        delete ownedPieces[i];
    }
    for ( long i = 0; i < ownedSources.size(); i++ ) {
        delete ownedSources[i];
    }
    for ( long i = 0; i < ownedAreas.size(); i++ ) {
        delete ownedAreas[i];
    }
    for ( long i = 0; i < ownedPlanes.size(); i++ ) {
        delete ownedPlanes[i];
    }
}

_WeilerAthertonArea*
HiddenSurfaceRenderer::newArea(Polygon2D* polygon)
{
    _WeilerAthertonArea* area = new _WeilerAthertonArea(polygon);
    ownedAreas.add(area);
    return area;
}

_WeilerAthertonSourcePolygon*
HiddenSurfaceRenderer::newSource()
{
    _WeilerAthertonSourcePolygon* source = new _WeilerAthertonSourcePolygon();
    ownedSources.add(source);
    return source;
}

_WeilerAthertonPiece*
HiddenSurfaceRenderer::newPiece(_WeilerAthertonArea* area,
                                _WeilerAthertonSourcePolygon* source)
{
    _WeilerAthertonPiece* piece = new _WeilerAthertonPiece(area, source);
    ownedPieces.add(piece);
    return piece;
}

java::ArrayList<HiddenSurfaceRenderer::VisiblePolygon*>
HiddenSurfaceRenderer::executeWeilerAthertonAlgorithm(
    java::ArrayList<SimpleBody*>& solids, const Camera& camera)
{
    HiddenSurfaceRenderer* renderer = solve(solids, camera);
    java::ArrayList<VisiblePolygon*> visible;

    for ( long i = 0; i < renderer->output.size(); i++ ) {
        _WeilerAthertonPiece* piece = renderer->output[i];
        visible.add(new VisiblePolygon(toNormalizedDeviceCoordinates(
            piece->area->polygon), piece->source, nullptr));
    }
    delete renderer;
    return visible;
}

java::ArrayList<HiddenSurfaceRenderer::VisiblePolygon*>
HiddenSurfaceRenderer::executeWeilerAthertonAlgorithm(
    java::ArrayList<SimpleBody*>& solids, const Camera& camera,
    const java::ArrayList<Light*>& lights)
{
    HiddenSurfaceRenderer* renderer = solve(solids, camera);
    java::ArrayList<Light*> ambientLights;
    java::ArrayList<Light*> castingLights;
    java::ArrayList<LitFaces*> litFaces;
    double bounds[4];
    bool withBounds = boundingSphere(solids, bounds);
    Matrix4x4d projection = camera.calculateProjectionMatrix();
    java::ArrayList<VisiblePolygon*> visible;

    // First pass: hidden surface removed views from the lights
    for ( long i = 0; i < lights.size(); i++ ) {
        Light* light = lights[i];
        if ( light->isAmbient() ) {
            ambientLights.add(light);
        }
        else if ( withBounds ) {
            castingLights.add(light);
            LitFaces* lit = new LitFaces();
            litFacesFromLight(solids, light, bounds, *lit);
            litFaces.add(lit);
        }
    }

    // Second pass: the visible areas from the observer, divided by the
    // lit areas of their faces
    for ( long i = 0; i < renderer->output.size(); i++ ) {
        _WeilerAthertonPiece* piece = renderer->output[i];
        java::ArrayList<_WeilerAthertonShadedPart> parts;
        parts.add(_WeilerAthertonShadedPart(piece->area, ambientLights));
        for ( long k = 0; k < castingLights.size(); k++ ) {
            LitFacesOfBody* faces = litFaces[k]->get(piece->source->body);
            _WeilerAthertonLitFace* lit = faces != nullptr ?
                faces->get(piece->source->face) : nullptr;
            if ( lit != nullptr ) {
                parts = renderer->divideByLitArea(parts, lit,
                    castingLights[k], projection);
            }
        }
        for ( long j = 0; j < parts.size(); j++ ) {
            const _WeilerAthertonShadedPart& part = parts[j];
            visible.add(new VisiblePolygon(toNormalizedDeviceCoordinates(
                part.area->polygon), piece->source,
                new java::ArrayList<Light*>(part.lights)));
        }
    }
    for ( long i = 0; i < litFaces.size(); i++ ) {
        delete litFaces[i];
    }
    delete renderer;
    return visible;
}

/**
Runs the hidden surface removal of [WEIL1977] for a view.
@return the renderer, with the visible polygons in its output (C++ port:
owned by the caller)
*/
HiddenSurfaceRenderer*
HiddenSurfaceRenderer::solve(java::ArrayList<SimpleBody*>& solids,
                             const Camera& camera)
{
    HiddenSurfaceRenderer* renderer = new HiddenSurfaceRenderer();

    renderer->solveWindow(-SCALE, -SCALE, SCALE, SCALE,
        renderer->buildInputPolygons(solids, camera), 0);
    return renderer;
}

//= Screen subdivision ====================================================

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
    double maxY, const java::ArrayList<_WeilerAthertonSourcePolygon*>& sources,
    int depth)
{
    if ( sources.size() <= MAXIMUM_POLYGONS_PER_WINDOW ||
         depth >= MAXIMUM_SUBDIVISION_DEPTH ) {
        solveArea(sources);
        return;
    }
    double middleX = (minX + maxX) / 2.0;
    double middleY = (minY + maxY) / 2.0;
    double quadrants[4][4] = {
        {minX, minY, middleX, middleY}, {middleX, minY, maxX, middleY},
        {minX, middleY, middleX, maxY}, {middleX, middleY, maxX, maxY}
    };
    for ( int k = 0; k < 4; k++ ) {
        const double* q = quadrants[k];
        java::ArrayList<_WeilerAthertonSourcePolygon*> inside;
        for ( long i = 0; i < sources.size(); i++ ) {
            _WeilerAthertonSourcePolygon* clipped =
                clippedToWindow(sources[i], q[0], q[1], q[2], q[3]);
            if ( clipped != nullptr ) {
                inside.add(clipped);
            }
        }
        if ( inside.size() < sources.size() ) {
            solveWindow(q[0], q[1], q[2], q[3], inside, depth + 1);
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

    if ( templateArea->maxX <= minX || templateArea->minX >= maxX ||
         templateArea->maxY <= minY || templateArea->minY >= maxY ) {
        return nullptr;
    }
    if ( templateArea->minX >= minX && templateArea->maxX <= maxX &&
         templateArea->minY >= minY && templateArea->maxY <= maxY ) {
        return source;
    }
    Polygon2D* polygon = clipByHalfPlane(templateArea->polygon, 1.0, 0.0, -minX);
    Polygon2D* next;
    if ( polygon != nullptr ) {
        next = clipByHalfPlane(polygon, -1.0, 0.0, maxX);
        delete polygon;
        polygon = next;
    }
    if ( polygon != nullptr ) {
        next = clipByHalfPlane(polygon, 0.0, 1.0, -minY);
        delete polygon;
        polygon = next;
    }
    if ( polygon != nullptr ) {
        next = clipByHalfPlane(polygon, 0.0, -1.0, maxY);
        delete polygon;
        polygon = next;
    }
    java::ArrayList<_WeilerAthertonArea*> parts;
    addWithoutSlivers(parts, polygon);
    delete polygon;
    if ( parts.isEmpty() ) {
        return nullptr;
    }

    _WeilerAthertonSourcePolygon* clipped = newSource();
    clipped->templateArea = merged(parts);
    clipped->a = source->a;
    clipped->b = source->b;
    clipped->c = source->c;
    clipped->body = source->body;
    clipped->face = source->face;
    clipped->worldPlane = source->worldPlane;
    clipped->complete = false;
    clipped->original = source->original;
    clipped->nearestDepth = java::Double::MAX_VALUE;
    Polygon2D* clippedPolygon = clipped->templateArea->polygon;
    for ( long l = 0; l < clippedPolygon->loops.size(); l++ ) {
        _Polygon2DContour* loop = clippedPolygon->loops[l];
        for ( long i = 0; i < loop->vertices.size(); i++ ) {
            const Vertex2D& v = loop->vertices[i];
            clipped->nearestDepth = java::Math::min(clipped->nearestDepth,
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
    const java::ArrayList<_WeilerAthertonSourcePolygon*>& inSources)
{
    java::ArrayList<_WeilerAthertonPiece*> polygonList;
    long i;

    java::ArrayList<_WeilerAthertonSourcePolygon*> sources(inSources);

    // Step a) of [WEIL1977].215: preliminary rough depth sort, from the
    // polygons closest to the observer to those furthest away
    java::Collections::sort(sources, [](_WeilerAthertonSourcePolygon* const& p,
                                        _WeilerAthertonSourcePolygon* const& q) -> int {
        return java::Double::compare(p->nearestDepth, q->nearestDepth);
    });

    for ( i = 0; i < sources.size(); i++ ) {
        polygonList.add(newPiece(sources[i]->templateArea, sources[i]));
    }

    // [WEIL1977].216: the process is continued until the outside list
    // is exhausted
    while ( !polygonList.isEmpty() ) {
        polygonList = subdivide(polygonList[0]->source, polygonList);
    }
}

//= Shadows ===============================================================

/**
Shadowing pass of [WEIL1977].218 for one light: "a hidden surface removed
view of a scene from the position of the light source", whose visible
polygons are "transformed back to the original space" as the lit areas
of their source faces.
@param bounds bounding sphere of the solids, as {x, y, z, radius}
@param lit receives the lit areas of the faces reached by the light, by body
and face (C++ port: an output parameter instead of the return value)
*/
void
HiddenSurfaceRenderer::litFacesFromLight(java::ArrayList<SimpleBody*>& solids,
    Light* light, const double bounds[4], LitFaces& lit)
{
    // A light inside a solid sees only its inner side, the back of its
    // faces: it reaches nothing outside of it
    if ( dynamic_cast<DirectionalLight*>(light) == nullptr &&
         isInsideASolid(solids, light->getPosition()) ) {
        return;
    }
    java::ArrayList<Camera> cameras = lightCameras(light, bounds);
    for ( long ci = 0; ci < cameras.size(); ci++ ) {
        const Camera& camera = cameras[ci];
        HiddenSurfaceRenderer* renderer = solve(solids, camera);
        Matrix4x4d inverse = camera.calculateProjectionMatrix().inverse();
        java::HashMap<_WeilerAthertonSourcePolygon*, double> visibleAreas;

        // The visible areas of a face, added over the windows of the
        // screen subdivision
        for ( long i = 0; i < renderer->output.size(); i++ ) {
            _WeilerAthertonPiece* piece = renderer->output[i];
            double* sum = visibleAreas.get(piece->source->original);
            if ( sum == nullptr ) {
                visibleAreas.put(piece->source->original, 0.0);
                sum = visibleAreas.get(piece->source->original);
            }
            *sum += piece->area->area;
        }
        for ( long i = 0; i < renderer->output.size(); i++ ) {
            _WeilerAthertonPiece* piece = renderer->output[i];
            _WeilerAthertonSourcePolygon* source = piece->source;
            LitFacesOfBody* faces = lit.get(source->body);
            if ( faces == nullptr ) {
                lit.put(source->body, LitFacesOfBody());
                faces = lit.get(source->body);
            }
            _WeilerAthertonLitFace* litFace = faces->get(source->face);
            if ( litFace == nullptr ) {
                faces->put(source->face, _WeilerAthertonLitFace());
                litFace = faces->get(source->face);
            }
            if ( litFace->whole ) {
                continue;
            }
            // A face entirely in the view and not hidden at all is lit
            // whole: its visible pieces add up to its area (but for the
            // slivers the screen subdivision may discard)
            _WeilerAthertonSourcePolygon* original = source->original;
            if ( original->complete && *visibleAreas.get(original) >=
                 original->templateArea->area * (1.0 - 1.0e-9) - AREA_EPSILON -
                 SLIVER_WIDTH * original->templateArea->perimeter ) {
                litFace->whole = true;
                litFace->regions.clear();
                continue;
            }
            java::ArrayList<java::ArrayList<Vector3Dd> > region;
            Polygon2D* polygon = piece->area->polygon;
            for ( long l = 0; l < polygon->loops.size(); l++ ) {
                _Polygon2DContour* loop = polygon->loops[l];
                java::ArrayList<Vector3Dd> contour;
                for ( long j = 0; j < loop->vertices.size(); j++ ) {
                    const Vertex2D& v = loop->vertices[j];
                    Vector3Dd p;
                    if ( unprojectToPlane(inverse, v.x / SCALE,
                                          v.y / SCALE, *source->worldPlane, p) ) {
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
        delete renderer;
    }
}

bool
HiddenSurfaceRenderer::isInsideASolid(java::ArrayList<SimpleBody*>& solids,
                                      const Vector3Dd& p)
{
    for ( long i = 0; i < solids.size(); i++ ) {
        SimpleBody* body = solids[i];
        PolyhedralBoundedSolid* brep =
            dynamic_cast<PolyhedralBoundedSolid*>(body->getGeometry());
        if ( brep != nullptr &&
             brep->doContainmentTest(body->getTransformationMatrix().inverse()
                 .multiply(p), VSDK::EPSILON) == Geometry::INSIDE ) {
            return true;
        }
    }
    return false;
}

/**
@param out receives the point of a plane seen at a point of the x-y plane of
the normalized device coordinates of a view
@return false if the line of sight is parallel to the plane (C++ port: where
Java returns null)
*/
bool
HiddenSurfaceRenderer::unprojectToPlane(const Matrix4x4d& inverseProjection,
    double x, double y, const InfinitePlane& plane, Vector3Dd& out)
{
    Vector4Dd a = inverseProjection.multiply(
        Vector4Dd(x, y, -0.5, 1.0)).dividedByW();
    Vector4Dd b = inverseProjection.multiply(
        Vector4Dd(x, y, 0.5, 1.0)).dividedByW();
    Vector3Dd p(a.x(), a.y(), a.z());
    Vector3Dd direction(b.x() - a.x(), b.y() - a.y(), b.z() - a.z());
    Vector3Dd n = plane.getNormal();
    double denominator = n.dotProduct(direction);

    if ( std::fabs(denominator) <= 1.0e-300 ) {
        return false;
    }
    double t = -(n.dotProduct(p) + plane.getD()) / denominator;
    out = p.add(direction.multiply(t));
    return true;
}

/**
Views from a light enclosing the scene: one perspective view, or the six
views of the faces of a cube when the light is among the solids, for a
point (or spot) light, and one orthogonal view for a directional light.
@param bounds bounding sphere of the solids, as {x, y, z, radius}
*/
java::ArrayList<Camera>
HiddenSurfaceRenderer::lightCameras(Light* light, const double bounds[4])
{
    java::ArrayList<Camera> cameras;
    Vector3Dd center(bounds[0], bounds[1], bounds[2]);
    double radius = bounds[3] * LIGHT_VIEW_MARGIN + VSDK::EPSILON;

    if ( dynamic_cast<DirectionalLight*>(light) != nullptr ) {
        // The position of a directional light is its direction
        Vector3Dd direction = light->getPosition().normalized();
        Camera camera = lightCamera(center.subtract(
            direction.multiply(2.0 * radius)), direction);
        camera.setProjectionMode(Camera::PROJECTION_MODE_ORTHOGONAL);
        camera.setOrthogonalZoom(1.0 / radius);
        camera.setNearPlaneDistance(0.5 * radius);
        camera.setFarPlaneDistance(3.5 * radius);
        camera.updateViewportResize(LIGHT_VIEW_SIZE, LIGHT_VIEW_SIZE);
        cameras.add(camera);
        return cameras;
    }

    Vector3Dd position = light->getPosition();
    Vector3Dd toCenter = center.subtract(position);
    double distance = toCenter.length();
    if ( distance > radius &&
         java::Math::asin(radius / distance) <= MAXIMUM_LIGHT_HALF_ANGLE ) {
        Camera camera = lightCamera(position, toCenter.normalized());
        camera.setFov(2.0 * java::Math::toDegrees(java::Math::asin(radius / distance)) *
            LIGHT_VIEW_MARGIN);
        camera.setNearPlaneDistance(0.5 * (distance - radius));
        camera.setFarPlaneDistance(2.0 * (distance + radius));
        cameras.add(camera);
    }
    else {
        Vector3Dd directions[6] = {
            Vector3Dd(1.0, 0.0, 0.0), Vector3Dd(-1.0, 0.0, 0.0),
            Vector3Dd(0.0, 1.0, 0.0), Vector3Dd(0.0, -1.0, 0.0),
            Vector3Dd(0.0, 0.0, 1.0), Vector3Dd(0.0, 0.0, -1.0)
        };
        // The views overlap a little, so no lit area is lost on their
        // common borders (the lit regions of a face may overlap: see
        // `divideByLitArea`)
        for ( int i = 0; i < 6; i++ ) {
            Camera camera = lightCamera(position, directions[i]);
            camera.setFov(90.0 + CUBE_VIEW_OVERLAP);
            camera.setNearPlaneDistance(1.0e-3 * (distance + radius));
            camera.setFarPlaneDistance(2.0 * (distance + radius));
            cameras.add(camera);
        }
    }
    for ( long i = 0; i < cameras.size(); i++ ) {
        cameras[i].updateViewportResize(LIGHT_VIEW_SIZE, LIGHT_VIEW_SIZE);
    }
    return cameras;
}

/**
@return a camera at a position looking along a direction
*/
Camera
HiddenSurfaceRenderer::lightCamera(const Vector3Dd& position,
                                   const Vector3Dd& front)
{
    Camera camera;
    Vector3Dd upHint = std::fabs(front.z()) < 0.9 ?
        Vector3Dd(0.0, 0.0, 1.0) : Vector3Dd(1.0, 0.0, 0.0);
    Vector3Dd left = upHint.crossProduct(front).normalized();
    Vector3Dd up = front.crossProduct(left).normalized();

    camera.setPosition(position);
    camera.setRotation(Matrix4x4d()
        .withVal(0, 0, front.x()).withVal(0, 1, left.x()).withVal(0, 2, up.x())
        .withVal(1, 0, front.y()).withVal(1, 1, left.y()).withVal(1, 2, up.y())
        .withVal(2, 0, front.z()).withVal(2, 1, left.z()).withVal(2, 2, up.z()));
    camera.updateViewportResize(LIGHT_VIEW_SIZE, LIGHT_VIEW_SIZE);
    return camera;
}

/**
@param bounds receives the bounding sphere of the vertices of the solids, as
{x, y, z, radius}
@return false if they have no vertices (C++ port: where Java returns null)
*/
bool
HiddenSurfaceRenderer::boundingSphere(java::ArrayList<SimpleBody*>& solids,
                                      double bounds[4])
{
    double box[6] = {java::Double::MAX_VALUE, java::Double::MAX_VALUE,
                     java::Double::MAX_VALUE, -java::Double::MAX_VALUE,
                     -java::Double::MAX_VALUE, -java::Double::MAX_VALUE};
    java::ArrayList<Vector3Dd> points;

    for ( long b = 0; b < solids.size(); b++ ) {
        SimpleBody* body = solids[b];
        PolyhedralBoundedSolid* brep =
            dynamic_cast<PolyhedralBoundedSolid*>(body->getGeometry());
        if ( brep == nullptr ) {
            continue;
        }
        Matrix4x4d transform = body->getTransformationMatrix();
        for ( long i = 0; i < brep->getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace* face = brep->getPolygonsList().get(i);
            for ( long j = 0; j < face->boundariesList.size(); j++ ) {
                _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(j);
                _PolyhedralBoundedSolidHalfEdge* he =
                    loop != nullptr ? loop->boundaryStartHalfEdge : nullptr;
                _PolyhedralBoundedSolidHalfEdge* heStart = he;
                if ( he == nullptr ) {
                    continue;
                }
                do {
                    if ( he == nullptr || he->startingVertex == nullptr ) {
                        break;
                    }
                    Vector3Dd p = transform.multiply(he->startingVertex->position);
                    points.add(p);
                    box[0] = java::Math::min(box[0], p.x());
                    box[1] = java::Math::min(box[1], p.y());
                    box[2] = java::Math::min(box[2], p.z());
                    box[3] = java::Math::max(box[3], p.x());
                    box[4] = java::Math::max(box[4], p.y());
                    box[5] = java::Math::max(box[5], p.z());
                    he = he->next();
                } while ( he != heStart );
            }
        }
    }
    if ( points.isEmpty() ) {
        return false;
    }
    Vector3Dd center((box[0] + box[3]) / 2.0,
        (box[1] + box[4]) / 2.0, (box[2] + box[5]) / 2.0);
    double radius = 0.0;
    for ( long i = 0; i < points.size(); i++ ) {
        radius = java::Math::max(radius, points[i].subtract(center).length());
    }
    bounds[0] = center.x();
    bounds[1] = center.y();
    bounds[2] = center.z();
    bounds[3] = radius;
    return true;
}

/**
Divides the parts of a visible area by the lit areas of its face for a
light: the parts inside them get the light. The lit areas are projected
to the viewing space of the observer and used as clip polygons.
*/
java::ArrayList<_WeilerAthertonShadedPart>
HiddenSurfaceRenderer::divideByLitArea(
    const java::ArrayList<_WeilerAthertonShadedPart>& parts,
    _WeilerAthertonLitFace* lit, Light* light, const Matrix4x4d& projection)
{
    java::ArrayList<_WeilerAthertonShadedPart> result;

    if ( lit->whole ) {
        for ( long i = 0; i < parts.size(); i++ ) {
            const _WeilerAthertonShadedPart& part = parts[i];
            result.add(_WeilerAthertonShadedPart(part.area,
                withLight(part.lights, light)));
        }
        return result;
    }

    java::ArrayList<_WeilerAthertonArea*> regions;
    for ( long i = 0; i < lit->regions.size(); i++ ) {
        _WeilerAthertonArea* area = projectRegion(lit->regions[i], projection);
        if ( area != nullptr ) {
            regions.add(area);
        }
    }
    for ( long p = 0; p < parts.size(); p++ ) {
        const _WeilerAthertonShadedPart& part = parts[p];
        // Each lit region takes its part of what the previous ones left,
        // so regions that overlap (from the views of a cube) are counted
        // once
        java::ArrayList<_WeilerAthertonArea*> remaining;
        java::ArrayList<_WeilerAthertonArea*> litAreas;
        remaining.add(part.area);
        for ( long r = 0; r < regions.size(); r++ ) {
            _WeilerAthertonArea* region = regions[r];
            java::ArrayList<_WeilerAthertonArea*> next;
            for ( long i = 0; i < remaining.size(); i++ ) {
                _WeilerAthertonArea* area = remaining[i];
                if ( !area->overlapsBox(region) ) {
                    next.add(area);
                    continue;
                }
                verifiedClip(region, area, litAreas, next);
            }
            remaining = next;
        }
        if ( !litAreas.isEmpty() ) {
            java::ArrayList<Light*> lights = withLight(part.lights, light);
            for ( long i = 0; i < litAreas.size(); i++ ) {
                result.add(_WeilerAthertonShadedPart(litAreas[i], lights));
            }
        }
        for ( long i = 0; i < remaining.size(); i++ ) {
            result.add(_WeilerAthertonShadedPart(remaining[i], part.lights));
        }
    }
    return result;
}

java::ArrayList<Light*>
HiddenSurfaceRenderer::withLight(const java::ArrayList<Light*>& lights,
                                 Light* light)
{
    java::ArrayList<Light*> result(lights);
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
    const java::ArrayList<java::ArrayList<Vector3Dd> >& region,
    const Matrix4x4d& projection)
{
    Polygon2D* polygon = emptyPolygon();

    for ( long r = 0; r < region.size(); r++ ) {
        const java::ArrayList<Vector3Dd>& contour = region[r];
        java::ArrayList<Vector4Dd> clipLoop;
        for ( long i = 0; i < contour.size(); i++ ) {
            clipLoop.add(projection.multiply(Vector4Dd(contour[i])));
        }
        clipLoop = clipAgainstPlane(clipLoop, 1.0);
        clipLoop = clipAgainstPlane(clipLoop, -1.0);
        if ( clipLoop.size() < 3 ) {
            continue;
        }
        java::ArrayList<Vector3Dd> loop;
        for ( long i = 0; i < clipLoop.size(); i++ ) {
            Vector4Dd ndc = clipLoop[i].dividedByW();
            loop.add(Vector3Dd(SCALE * ndc.x(), SCALE * ndc.y(), 0.0));
        }
        if ( std::fabs(signedArea(loop)) > AREA_EPSILON ) {
            addLoop(polygon, loop);
        }
    }
    if ( polygon->loops.isEmpty() ) {
        delete polygon;
        return nullptr;
    }
    // The light and the observer see the same side of the face, so the
    // orientation is kept; it is checked anyway
    if ( enclosedArea(polygon) < 0.0 ) {
        for ( long l = 0; l < polygon->loops.size(); l++ ) {
            java::Collections::reverse(polygon->loops[l]->vertices);
        }
    }
    _WeilerAthertonArea* area = newArea(polygon);
    return area->area > AREA_EPSILON ? area : nullptr;
}

Polygon2D*
HiddenSurfaceRenderer::toNormalizedDeviceCoordinates(Polygon2D* polygon)
{
    Polygon2D* ndc = emptyPolygon();
    for ( long l = 0; l < polygon->loops.size(); l++ ) {
        _Polygon2DContour* loop = polygon->loops[l];
        ndc->nextLoop();
        for ( long i = 0; i < loop->vertices.size(); i++ ) {
            const Vertex2D& v = loop->vertices[i];
            ndc->addVertex(v.x / SCALE, v.y / SCALE);
        }
    }
    return ndc;
}

//= Hidden surface algorithm ==============================================

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
java::ArrayList<_WeilerAthertonPiece*>
HiddenSurfaceRenderer::subdivide(_WeilerAthertonSourcePolygon* clipPolygon,
    const java::ArrayList<_WeilerAthertonPiece*>& polygonList)
{
    java::ArrayList<_WeilerAthertonPiece*> insideList;
    java::ArrayList<_WeilerAthertonPiece*> outsideList;
    long i;

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
    while ( true ) {
        _WeilerAthertonPiece* offending = nullptr;
        for ( i = 0; i < insideList.size(); i++ ) {
            _WeilerAthertonPiece* piece = insideList[i];
            if ( piece->source != clipPolygon &&
                 !clipStack.contains(piece->source) ) {
                offending = piece;
                break;
            }
        }
        if ( offending == nullptr ) {
            break;
        }
        clipStack.add(clipPolygon);
        insideList = subdivide(offending->source, insideList);
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
    java::ArrayList<_WeilerAthertonPiece*>& insideList,
    java::ArrayList<_WeilerAthertonPiece*>& outsideList)
{
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

    java::ArrayList<_WeilerAthertonArea*> inside;
    java::ArrayList<_WeilerAthertonArea*> outside;
    verifiedClip(clipPolygon->templateArea, piece->area, inside, outside);
    for ( long i = 0; i < inside.size(); i++ ) {
        insideList.add(newPiece(inside[i], piece->source));
    }
    for ( long i = 0; i < outside.size(); i++ ) {
        outsideList.add(newPiece(outside[i], piece->source));
    }
}

/**
Step c) of [WEIL1977].216: the polygons of the inside list located behind
the clip polygon are hidden from view. A polygon partially in front of
it (penetrating it) is divided, and only its front part is kept.
@return the new inside list
*/
java::ArrayList<_WeilerAthertonPiece*>
HiddenSurfaceRenderer::removePolygonsBehind(
    _WeilerAthertonSourcePolygon* clipPolygon,
    const java::ArrayList<_WeilerAthertonPiece*>& insideList)
{
    java::ArrayList<_WeilerAthertonPiece*> remaining;
    long i;

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
HiddenSurfaceRenderer::classifyDepth(_WeilerAthertonSourcePolygon* clipPolygon,
                                     _WeilerAthertonPiece* piece)
{
    bool front = false;
    bool behind = false;
    Polygon2D* polygon = piece->area->polygon;

    for ( long l = 0; l < polygon->loops.size(); l++ ) {
        _Polygon2DContour* loop = polygon->loops[l];
        for ( long i = 0; i < loop->vertices.size(); i++ ) {
            const Vertex2D& v = loop->vertices[i];
            double difference = piece->source->depthAt(v.x, v.y) -
                clipPolygon->depthAt(v.x, v.y);
            if ( difference < -DEPTH_EPSILON ) {
                front = true;
            }
            else if ( difference > DEPTH_EPSILON ) {
                behind = true;
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
HiddenSurfaceRenderer::addFrontPart(_WeilerAthertonSourcePolygon* clipPolygon,
    _WeilerAthertonPiece* piece,
    java::ArrayList<_WeilerAthertonPiece*>& remaining)
{
    // Points with da*x + db*y + dc < 0 are in front of the clip polygon
    double da = piece->source->a - clipPolygon->a;
    double db = piece->source->b - clipPolygon->b;
    double dc = piece->source->c - clipPolygon->c;
    _WeilerAthertonArea* area = piece->area;
    double margin = 1.0 + 0.01 * ((area->maxX - area->minX) +
        (area->maxY - area->minY));
    double box[4][2] = {
        {area->minX - margin, area->minY - margin},
        {area->maxX + margin, area->minY - margin},
        {area->maxX + margin, area->maxY + margin},
        {area->minX - margin, area->maxY + margin}
    };
    java::ArrayList<Vector3Dd> halfPlane;
    int i;

    // Sutherland-Hodgman clip of the (convex) box by the line
    for ( i = 0; i < 4; i++ ) {
        const double* p = box[i];
        const double* q = box[(i + 1) % 4];
        double fp = da * p[0] + db * p[1] + dc;
        double fq = da * q[0] + db * q[1] + dc;
        if ( fp <= 0.0 ) {
            halfPlane.add(Vector3Dd(p[0], p[1], 0.0));
        }
        if ( (fp < 0.0 && fq > 0.0) || (fp > 0.0 && fq < 0.0) ) {
            double t = fp / (fp - fq);
            halfPlane.add(Vector3Dd(p[0] + t * (q[0] - p[0]),
                                    p[1] + t * (q[1] - p[1]), 0.0));
        }
    }
    if ( halfPlane.size() < 3 ) {
        return;
    }

    Polygon2D* frontRegion = new Polygon2D();
    for ( long j = 0; j < halfPlane.size(); j++ ) {
        frontRegion->addVertex(halfPlane[j].x(), halfPlane[j].y());
    }
    java::ArrayList<_WeilerAthertonArea*> front;
    java::ArrayList<_WeilerAthertonArea*> back;
    verifiedClip(newArea(frontRegion), area, front, back);
    for ( long j = 0; j < front.size(); j++ ) {
        remaining.add(newPiece(front[j], piece->source));
    }
}

void
HiddenSurfaceRenderer::display(_WeilerAthertonPiece* piece)
{
    output.add(piece);
}

//= Verified clips ========================================================

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
    _WeilerAthertonArea* subject,
    java::ArrayList<_WeilerAthertonArea*>& inside,
    java::ArrayList<_WeilerAthertonArea*>& outside)
{
    int attempt;

    for ( attempt = 0; attempt <= MAXIMUM_RETRIES; attempt++ ) {
        double shift = attempt * RETRY_SHIFT;
        // Golden angle: the directions of the moves do not repeat
        double angle = attempt * 2.399963229728653;
        Polygon2D* clipPolygon = attempt == 0 ? clip->polygon :
            translated(clip->polygon, shift * std::cos(angle),
                       shift * std::sin(angle));
        Polygon2D insidePolygon;
        Polygon2D outsidePolygon;

        clipper.clipPolygons(clipPolygon, subject->polygon, &insidePolygon,
            &outsidePolygon);
        if ( clipper.isLastClipAbandoned() ) {
            if ( attempt != 0 ) {
                delete clipPolygon;
            }
            continue;
        }
        Polygon2D* insideClean = withoutDegenerateContours(&insidePolygon);
        Polygon2D* outsideClean = withoutDegenerateContours(&outsidePolygon);
        bool consistent = isConsistentClip(clip, clipPolygon, subject,
            insideClean, outsideClean, shift);
        if ( consistent ) {
            addWithoutSlivers(inside, insideClean);
            addWithoutSlivers(outside, outsideClean);
        }
        delete insideClean;
        delete outsideClean;
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
    java::ArrayList<_WeilerAthertonArea*> remaining;
    remaining.add(subject);
    java::ArrayList<Polygon2D*> pieces = trapezoids(clip->polygon);
    for ( long t = 0; t < pieces.size(); t++ ) {
        Polygon2D* trapezoid = pieces[t];
        java::ArrayList<_WeilerAthertonArea*> next;
        // C++ port: the area owns its polygon, so the trapezoid itself is
        // the one clipped by
        _WeilerAthertonArea* trapezoidArea = newArea(trapezoid);
        for ( long i = 0; i < remaining.size(); i++ ) {
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
(C++ port: owned by the caller)
*/
java::ArrayList<Polygon2D*>
HiddenSurfaceRenderer::trapezoids(Polygon2D* polygon)
{
    java::ArrayList<Polygon2D*> result;
    java::ArrayList<Vector4Dd> edges;
    java::ArrayList<double> xs;

    for ( long l = 0; l < polygon->loops.size(); l++ ) {
        _Polygon2DContour* loop = polygon->loops[l];
        long n = loop->vertices.size();
        for ( long i = 0; i < n; i++ ) {
            const Vertex2D& p = loop->vertices[i];
            const Vertex2D& q = loop->vertices[(i + 1) % n];
            xs.add(p.x);
            if ( p.x != q.x ) {
                edges.add(p.x < q.x ? Vector4Dd(p.x, p.y, q.x, q.y) :
                                      Vector4Dd(q.x, q.y, p.x, p.y));
            }
        }
    }
    java::Collections::sort(xs, [](const double& a, const double& b) -> int {
        return java::Double::compare(a, b);
    });
    for ( long i = 0; i + 1 < xs.size(); i++ ) {
        double x0 = xs[i];
        double x1 = xs[i + 1];
        if ( x1 <= x0 ) {
            continue;
        }
        double xm = (x0 + x1) / 2.0;
        java::ArrayList<Vector3Dd> crossing;
        for ( long k = 0; k < edges.size(); k++ ) {
            const Vector4Dd& e = edges[k];
            if ( e.x() <= x0 && e.z() >= x1 ) {
                double slope = (e.w() - e.y()) / (e.z() - e.x());
                crossing.add(Vector3Dd(e.y() + slope * (x0 - e.x()),
                    e.y() + slope * (x1 - e.x()), e.y() + slope * (xm - e.x())));
            }
        }
        crossing.sort([](const Vector3Dd& a, const Vector3Dd& b) -> int {
            return java::Double::compare(a.z(), b.z());
        });
        for ( long j = 0; j + 1 < crossing.size(); j += 2 ) {
            const Vector3Dd& low = crossing[j];
            const Vector3Dd& high = crossing[j + 1];
            java::ArrayList<Vector3Dd> corners;
            addDistinct(corners, x0, low.x());
            addDistinct(corners, x1, low.y());
            addDistinct(corners, x1, high.y());
            addDistinct(corners, x0, high.x());
            if ( corners.size() >= 3 &&
                 std::fabs(signedArea(corners)) > AREA_EPSILON ) {
                Polygon2D* trapezoid = emptyPolygon();
                addLoop(trapezoid, corners);
                result.add(trapezoid);
            }
        }
    }
    return result;
}

void
HiddenSurfaceRenderer::addDistinct(java::ArrayList<Vector3Dd>& points,
                                   double x, double y)
{
    if ( !points.isEmpty() ) {
        const Vector3Dd& last = points[points.size() - 1];
        const Vector3Dd& first = points[0];
        if ( std::hypot(last.x() - x, last.y() - y) <= MINIMUM_EDGE_LENGTH ||
             std::hypot(first.x() - x, first.y() - y) <= MINIMUM_EDGE_LENGTH ) {
            return;
        }
    }
    points.add(Vector3Dd(x, y, 0.0));
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
    _WeilerAthertonArea* subject,
    java::ArrayList<_WeilerAthertonArea*>& inside,
    java::ArrayList<_WeilerAthertonArea*>& outside)
{
    java::ArrayList<Vertex2D>& vertices = clip->loops[0]->vertices;
    Polygon2D* remaining = subject->polygon;
    // C++ port: the intermediate polygons are freed here
    bool ownsRemaining = false;
    long n = vertices.size();

    for ( long i = 0; i < n && remaining != nullptr; i++ ) {
        const Vertex2D& p = vertices[i];
        const Vertex2D& q = vertices[(i + 1) % n];
        // An edge of (almost) no length gives no direction: its half
        // plane would cut at random
        if ( std::hypot(q.x - p.x, q.y - p.y) <= MINIMUM_EDGE_LENGTH ) {
            continue;
        }
        // Inside is at the left of the edge: a*x + b*y + c >= 0
        double a = -(q.y - p.y);
        double b = q.x - p.x;
        double c = -(a * p.x + b * p.y);
        Polygon2D* out = clipByHalfPlane(remaining, -a, -b, -c);
        if ( out != nullptr ) {
            addWithoutSlivers(outside, out);
            delete out;
        }
        Polygon2D* next = clipByHalfPlane(remaining, a, b, c);
        if ( ownsRemaining ) {
            delete remaining;
        }
        remaining = next;
        ownsRemaining = true;
    }
    if ( remaining != nullptr ) {
        addWithoutSlivers(inside, remaining);
        if ( ownsRemaining ) {
            delete remaining;
        }
    }
}

/**
@return the part of a polygon where a*x + b*y + c >= 0 (each contour
clipped by Sutherland-Hodgman), or null if nothing is left (C++ port: owned
by the caller)
*/
Polygon2D*
HiddenSurfaceRenderer::clipByHalfPlane(Polygon2D* polygon, double a,
                                       double b, double c)
{
    Polygon2D* result = emptyPolygon();
    for ( long l = 0; l < polygon->loops.size(); l++ ) {
        _Polygon2DContour* loop = polygon->loops[l];
        java::ArrayList<Vector3Dd> clipped;
        long n = loop->vertices.size();
        for ( long i = 0; i < n; i++ ) {
            const Vertex2D& p = loop->vertices[i];
            const Vertex2D& q = loop->vertices[(i + 1) % n];
            double fp = a * p.x + b * p.y + c;
            double fq = a * q.x + b * q.y + c;
            if ( fp >= 0.0 ) {
                clipped.add(Vector3Dd(p.x, p.y, 0.0));
            }
            if ( (fp >= 0.0) != (fq >= 0.0) ) {
                double t = fp / (fp - fq);
                clipped.add(Vector3Dd(p.x + t * (q.x - p.x),
                                      p.y + t * (q.y - p.y), 0.0));
            }
        }
        if ( clipped.size() >= 3 ) {
            addLoop(result, clipped);
        }
    }
    Polygon2D* clean = withoutDegenerateContours(result);
    delete result;
    return clean;
}

/**
@return true if the polygon has one contour, counterclockwise and convex
*/
bool
HiddenSurfaceRenderer::isConvex(Polygon2D* polygon)
{
    if ( polygon->loops.size() != 1 ) {
        return false;
    }
    java::ArrayList<Vertex2D>& vertices = polygon->loops[0]->vertices;
    long n = vertices.size();
    if ( n < 3 || signedArea(polygon->loops[0]) <= 0.0 ) {
        return false;
    }
    for ( long i = 0; i < n; i++ ) {
        const Vertex2D& p = vertices[i];
        const Vertex2D& q = vertices[(i + 1) % n];
        const Vertex2D& r = vertices[(i + 2) % n];
        double cross = (q.x - p.x) * (r.y - q.y) - (q.y - p.y) * (r.x - q.x);
        double scale = std::hypot(q.x - p.x, q.y - p.y) *
            std::hypot(r.x - q.x, r.y - q.y);
        if ( cross < -1.0e-9 * scale ) {
            return false;
        }
    }
    return true;
}

/**
@return true if the inside and outside parts of a clip add up to the
subject and are within the expected bounds
*/
bool
HiddenSurfaceRenderer::isConsistentClip(_WeilerAthertonArea* clip,
    Polygon2D* clipPolygon, _WeilerAthertonArea* subject, Polygon2D* inside,
    Polygon2D* outside, double shift)
{
    double insideArea = inside != nullptr ? enclosedArea(inside) : 0.0;
    double outsideArea = outside != nullptr ? enclosedArea(outside) : 0.0;
    double length = shift + SLIVER_WIDTH;
    double tolerance = 1.0e-7 * subject->area + AREA_EPSILON +
        4.0 * length * (subject->perimeter + 2.0 *
            ((subject->maxX - subject->minX) + (subject->maxY - subject->minY)));

    if ( insideArea < -tolerance || outsideArea < -tolerance ) {
        return false;
    }
    if ( std::fabs(insideArea + outsideArea - subject->area) > tolerance ) {
        return false;
    }
    if ( insideArea > java::Math::min(subject->area, clip->area) + tolerance ) {
        return false;
    }
    if ( inside != nullptr ) {
        double box[4];
        boundingBox(inside, box);
        double slack = 2.0 * length;
        if ( box[0] < java::Math::max(subject->minX, clip->minX) - slack ||
             box[1] < java::Math::max(subject->minY, clip->minY) - slack ||
             box[2] > java::Math::min(subject->maxX, clip->maxX) + slack ||
             box[3] > java::Math::min(subject->maxY, clip->maxY) + slack ) {
            return false;
        }
    }
    if ( outside != nullptr ) {
        double box[4];
        boundingBox(outside, box);
        double slack = 2.0 * length;
        if ( box[0] < subject->minX - slack || box[1] < subject->minY - slack ||
             box[2] > subject->maxX + slack || box[3] > subject->maxY + slack ) {
            return false;
        }
    }
    // At sample points of the four polygons, far from all their
    // borders, a point is in the inside part if and only if it is in the
    // subject and in the clip polygon, and it is in the outside part if
    // and only if it is in the subject and not in the clip polygon. It
    // fails when the clipper misses contacts of the contours: i.e. a
    // piece inscribed in the clip polygon (all its vertices on its
    // border), or a contour of the subject identical to the clip polygon
    double slack = 2.0 * length;
    java::ArrayList<Vector3Dd> samples = samplePoints(clipPolygon);
    samples.addAll(samplePoints(subject->polygon));
    if ( inside != nullptr ) {
        samples.addAll(samplePoints(inside));
    }
    if ( outside != nullptr ) {
        samples.addAll(samplePoints(outside));
    }
    _WeilerAthertonEdgeGrid clipBorder(clipPolygon, slack);
    _WeilerAthertonEdgeGrid subjectBorder(subject->polygon, slack);
    _WeilerAthertonEdgeGrid* insideBorder = inside != nullptr ?
        new _WeilerAthertonEdgeGrid(inside, slack) : nullptr;
    _WeilerAthertonEdgeGrid* outsideBorder = outside != nullptr ?
        new _WeilerAthertonEdgeGrid(outside, slack) : nullptr;
    bool consistent = true;
    for ( long i = 0; i < samples.size(); i++ ) {
        double x = samples[i].x();
        double y = samples[i].y();
        if ( clipBorder.isNearBorder(x, y) ||
             subjectBorder.isNearBorder(x, y) ) {
            continue;
        }
        bool inSubject = contains(subject->polygon, x, y);
        bool inClip = contains(clipPolygon, x, y);
        if ( !isOnExpectedSide(inside, insideBorder, x, y,
                               inSubject && inClip) ||
             !isOnExpectedSide(outside, outsideBorder, x, y,
                               inSubject && !inClip) ) {
            consistent = false;
            break;
        }
    }
    delete insideBorder;
    delete outsideBorder;
    return consistent;
}

/**
@param part a part of the clip result (null if empty)
@param border index of the edges of the part
@param expected true if the point must be in the part
@return true if the point is in the part as expected, or it is too close
to its border to tell
*/
bool
HiddenSurfaceRenderer::isOnExpectedSide(Polygon2D* part,
    const _WeilerAthertonEdgeGrid* border, double x, double y, bool expected)
{
    if ( part == nullptr ) {
        return !expected;
    }
    return contains(part, x, y) == expected || border->isNearBorder(x, y);
}

/**
@return sample points of a polygon, as {x, y} pairs: its vertices, the
middle points of its edges, and the centroids of the triangles at its
vertices (inside the polygon at its ears)
*/
java::ArrayList<Vector3Dd>
HiddenSurfaceRenderer::samplePoints(Polygon2D* polygon)
{
    java::ArrayList<Vector3Dd> points;
    for ( long l = 0; l < polygon->loops.size(); l++ ) {
        _Polygon2DContour* loop = polygon->loops[l];
        long n = loop->vertices.size();
        for ( long i = 0; i < n; i++ ) {
            const Vertex2D& p = loop->vertices[(i + n - 1) % n];
            const Vertex2D& q = loop->vertices[i];
            const Vertex2D& r = loop->vertices[(i + 1) % n];
            points.add(Vector3Dd(q.x, q.y, 0.0));
            points.add(Vector3Dd((q.x + r.x) / 2.0, (q.y + r.y) / 2.0, 0.0));
            points.add(Vector3Dd((p.x + q.x + r.x) / 3.0,
                                 (p.y + q.y + r.y) / 3.0, 0.0));
        }
    }
    return points;
}

/**
Adds the parts of a clip result to a list, discarding the contours that
are degenerate strips (see `SLIVER_WIDTH`). C++ port: the polygon is
copied, so it stays owned by the caller.
*/
void
HiddenSurfaceRenderer::addWithoutSlivers(
    java::ArrayList<_WeilerAthertonArea*>& list, Polygon2D* polygon)
{
    if ( polygon == nullptr ) {
        return;
    }
    Polygon2D* clean = emptyPolygon();
    bool empty = true;
    for ( long l = 0; l < polygon->loops.size(); l++ ) {
        _Polygon2DContour* loop = polygon->loops[l];
        double area = std::fabs(signedArea(loop));
        double halfPerimeter = perimeter(loop) / 2.0;
        if ( halfPerimeter <= 0.0 || area / halfPerimeter < SLIVER_WIDTH ) {
            continue;
        }
        copyLoop(clean, loop);
        empty = false;
    }
    if ( !empty ) {
        _WeilerAthertonArea* area = newArea(clean);
        if ( area->area > AREA_EPSILON ) {
            list.add(area);
        }
    }
    else {
        delete clean;
    }
}

//= Input polygons ========================================================

/**
Transforms the faces of the solids to the viewing space, removing the
faces facing away from the observer and clipping them to the view.
*/
java::ArrayList<_WeilerAthertonSourcePolygon*>
HiddenSurfaceRenderer::buildInputPolygons(java::ArrayList<SimpleBody*>& solids,
                                          const Camera& camera)
{
    java::ArrayList<_WeilerAthertonSourcePolygon*> sources;
    Matrix4x4d projection = camera.calculateProjectionMatrix();
    double sign = depthSign(camera, projection);
    Polygon2D* windowPolygon = new Polygon2D();
    long i;

    windowPolygon->addVertex(-SCALE, -SCALE);
    windowPolygon->addVertex(SCALE, -SCALE);
    windowPolygon->addVertex(SCALE, SCALE);
    windowPolygon->addVertex(-SCALE, SCALE);
    _WeilerAthertonArea* window = newArea(windowPolygon);

    for ( long b = 0; b < solids.size(); b++ ) {
        SimpleBody* body = solids[b];
        Geometry* geometry = body->getGeometry();
        PolyhedralBoundedSolid* brep = dynamic_cast<PolyhedralBoundedSolid*>(geometry);
        if ( brep == nullptr ) {
            continue;
        }
        Matrix4x4d transform = body->getTransformationMatrix();
        for ( i = 0; i < brep->getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace* face = brep->getPolygonsList().get(i);
            _WeilerAthertonSourcePolygon* source = buildInputPolygon(face,
                body, transform, camera, projection, sign, window);
            if ( source != nullptr ) {
                sources.add(source);
            }
        }
    }
    return sources;
}

_WeilerAthertonSourcePolygon*
HiddenSurfaceRenderer::buildInputPolygon(_PolyhedralBoundedSolidFace* face,
    SimpleBody* body, const Matrix4x4d& transform, const Camera& camera,
    const Matrix4x4d& projection, double depthSign,
    _WeilerAthertonArea* window)
{
    InfinitePlane* worldPlane = worldContainingPlane(face, body, transform);
    if ( worldPlane == nullptr ) {
        return nullptr;
    }
    ownedPlanes.add(worldPlane);
    if ( !isFrontFace(*worldPlane, camera) ) {
        return nullptr;
    }

    // Projection of each loop, clipped in homogeneous coordinates against
    // the near and far planes
    java::ArrayList<java::ArrayList<Vector3Dd> > loops;
    bool complete = true;
    for ( long i = 0; i < face->boundariesList.size(); i++ ) {
        _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(i);
        java::ArrayList<Vector4Dd> clipLoop;
        if ( loop == nullptr || loop->boundaryStartHalfEdge == nullptr ) {
            continue;
        }
        _PolyhedralBoundedSolidHalfEdge* he = loop->boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge* heStart = he;
        do {
            if ( he == nullptr || he->startingVertex == nullptr ) {
                break;
            }
            clipLoop.add(projection.multiply(Vector4Dd(
                transform.multiply(he->startingVertex->position))));
            he = he->next();
        } while ( he != heStart );

        for ( long j = 0; j < clipLoop.size(); j++ ) {
            const Vector4Dd& p = clipLoop[j];
            if ( p.z() + p.w() < 0.0 || p.w() - p.z() < 0.0 ) {
                complete = false;
            }
        }
        clipLoop = clipAgainstPlane(clipLoop, 1.0);
        clipLoop = clipAgainstPlane(clipLoop, -1.0);
        if ( clipLoop.size() < 3 ) {
            complete = false;
            continue;
        }
        java::ArrayList<Vector3Dd> projectedLoop;
        for ( long j = 0; j < clipLoop.size(); j++ ) {
            Vector4Dd ndc = clipLoop[j].dividedByW();
            projectedLoop.add(Vector3Dd(SCALE * ndc.x(), SCALE * ndc.y(),
                                        depthSign * ndc.z()));
        }
        if ( std::fabs(signedArea(projectedLoop)) > AREA_EPSILON ) {
            loops.add(projectedLoop);
        }
    }
    if ( loops.isEmpty() ) {
        return nullptr;
    }

    // The polygon clipper follows main contours counterclockwise and
    // holes clockwise: the main contour is the one of largest area
    long outer = 0;
    for ( long i = 1; i < loops.size(); i++ ) {
        if ( std::fabs(signedArea(loops[i])) >
             std::fabs(signedArea(loops[outer])) ) {
            outer = i;
        }
    }
    for ( long i = 0; i < loops.size(); i++ ) {
        double area = signedArea(loops[i]);
        if ( (i == outer && area < 0.0) || (i != outer && area > 0.0) ) {
            java::Collections::reverse(loops[i]);
        }
    }

    _WeilerAthertonSourcePolygon* source = newSource();
    if ( !fitDepthPlane(loops[outer], source) ) {
        // Seen edge-on
        return nullptr;
    }

    Polygon2D* projected = emptyPolygon();
    addLoop(projected, loops[outer]);
    for ( long i = 0; i < loops.size(); i++ ) {
        if ( i != outer ) {
            addLoop(projected, loops[i]);
        }
    }

    // Clip to the window of the view
    _WeilerAthertonArea* templateArea = newArea(projected);
    if ( !templateArea->overlapsBox(window) ) {
        return nullptr;
    }
    if ( templateArea->minX < window->minX || templateArea->maxX > window->maxX ||
         templateArea->minY < window->minY || templateArea->maxY > window->maxY ) {
        java::ArrayList<_WeilerAthertonArea*> inside;
        java::ArrayList<_WeilerAthertonArea*> outside;
        verifiedClip(window, templateArea, inside, outside);
        if ( inside.isEmpty() ) {
            return nullptr;
        }
        templateArea = merged(inside);
        complete = false;
    }

    source->templateArea = templateArea;
    source->nearestDepth = java::Double::MAX_VALUE;
    Polygon2D* polygon = templateArea->polygon;
    for ( long l = 0; l < polygon->loops.size(); l++ ) {
        _Polygon2DContour* loop = polygon->loops[l];
        for ( long i = 0; i < loop->vertices.size(); i++ ) {
            const Vertex2D& v = loop->vertices[i];
            source->nearestDepth = java::Math::min(source->nearestDepth,
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
java::ArrayList<Vector4Dd>
HiddenSurfaceRenderer::clipAgainstPlane(const java::ArrayList<Vector4Dd>& loop,
                                        double side)
{
    java::ArrayList<Vector4Dd> result;
    long n = loop.size();
    long i;

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
bool
HiddenSurfaceRenderer::fitDepthPlane(const java::ArrayList<Vector3Dd>& loop,
                                     _WeilerAthertonSourcePolygon* source)
{
    double nx = 0.0;
    double ny = 0.0;
    double nz = 0.0;
    double cx = 0.0;
    double cy = 0.0;
    double cz = 0.0;
    long n = loop.size();
    long i;

    for ( i = 0; i < n; i++ ) {
        const Vector3Dd& p = loop[i];
        const Vector3Dd& q = loop[(i + 1) % n];
        nx += (p.y() - q.y()) * (p.z() + q.z());
        ny += (p.z() - q.z()) * (p.x() + q.x());
        nz += (p.x() - q.x()) * (p.y() + q.y());
        cx += p.x();
        cy += p.y();
        cz += p.z();
    }
    double length = std::sqrt(nx * nx + ny * ny + nz * nz);
    if ( length <= VSDK::EPSILON * VSDK::EPSILON ||
         std::fabs(nz) <= 1.0e-12 * length ) {
        return false;
    }
    cx /= n;
    cy /= n;
    cz /= n;
    source->a = -nx / nz;
    source->b = -ny / nz;
    source->c = cz - source->a * cx - source->b * cy;
    return true;
}

/**
@return 1 if the depth of the normalized device coordinates grows away
from the observer, -1 otherwise, so the depths used by the algorithm
are always smaller closer to the observer
*/
double
HiddenSurfaceRenderer::depthSign(const Camera& camera,
                                 const Matrix4x4d& projection)
{
    Vector3Dd front = camera.getFront().normalized();
    double nearDistance = camera.getNearPlaneDistance();
    double farDistance = camera.getFarPlaneDistance();
    Vector3Dd p0 = camera.getPosition().add(front.multiply(
        nearDistance + 0.25 * (farDistance - nearDistance)));
    Vector3Dd p1 = camera.getPosition().add(front.multiply(
        nearDistance + 0.75 * (farDistance - nearDistance)));
    double z0 = projection.multiply(Vector4Dd(p0)).dividedByW().z();
    double z1 = projection.multiply(Vector4Dd(p1)).dividedByW().z();
    return z1 >= z0 ? 1.0 : -1.0;
}

/**
Backplane removal: a face of a closed solid is seen only from the side
its outward normal points to.
*/
bool
HiddenSurfaceRenderer::isFrontFace(const InfinitePlane& worldPlane,
                                   const Camera& camera)
{
    Vector3Dd n = worldPlane.getNormal();
    if ( camera.getProjectionMode() == Camera::PROJECTION_MODE_ORTHOGONAL ) {
        return n.dotProduct(camera.getFront()) < 0.0;
    }
    // Signed distance from the plane to the eye
    double distance = n.x() * camera.getPosition().x() +
        n.y() * camera.getPosition().y() +
        n.z() * camera.getPosition().z() + worldPlane.getD();
    return distance > 0.0;
}

/**
@return the plane of a face in world coordinates, with its outward
normal (from the maintained plane of the face, a Newell fit, not from
three vertices that could be at a reflex corner), or null if undefined
(C++ port: owned by the caller)
*/
InfinitePlane*
HiddenSurfaceRenderer::worldContainingPlane(_PolyhedralBoundedSolidFace* face,
    SimpleBody* body, const Matrix4x4d& transform)
{
    InfinitePlane* localPlane = face->getContainingPlane();
    Vector3Dd pointOnFace;
    bool foundPoint = false;

    if ( localPlane == nullptr ) {
        return nullptr;
    }
    for ( long i = 0; i < face->boundariesList.size() && !foundPoint; i++ ) {
        _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(i);
        _PolyhedralBoundedSolidHalfEdge* he =
            loop != nullptr ? loop->boundaryStartHalfEdge : nullptr;
        if ( he != nullptr && he->startingVertex != nullptr ) {
            pointOnFace = he->startingVertex->position;
            foundPoint = true;
        }
    }
    if ( !foundPoint ) {
        delete localPlane;
        return nullptr;
    }
    Vector3Dd worldNormal = body->transformNormalToWorld(localPlane->getNormal());
    delete localPlane;
    if ( worldNormal.length() <= VSDK::EPSILON ) {
        return nullptr;
    }
    return new InfinitePlane(worldNormal.normalized(),
        transform.multiply(pointOnFace));
}

//= 2D polygon utilities ==================================================

/**
C++ port: Java's `new Polygon2D()` followed by `loops.clear()`, freeing the
initial contour.
*/
Polygon2D*
HiddenSurfaceRenderer::emptyPolygon()
{
    Polygon2D* polygon = new Polygon2D();
    for ( long i = 0; i < polygon->loops.size(); i++ ) {
        delete polygon->loops[i];
    }
    polygon->loops.clear();
    return polygon;
}

void
HiddenSurfaceRenderer::addLoop(Polygon2D* polygon,
                               const java::ArrayList<Vector3Dd>& loop)
{
    polygon->nextLoop();
    for ( long i = 0; i < loop.size(); i++ ) {
        polygon->addVertex(loop[i].x(), loop[i].y());
    }
}

void
HiddenSurfaceRenderer::copyLoop(Polygon2D* polygon, _Polygon2DContour* loop)
{
    polygon->nextLoop();
    for ( long i = 0; i < loop->vertices.size(); i++ ) {
        const Vertex2D& v = loop->vertices[i];
        polygon->addVertex(v.x, v.y);
    }
}

/**
C++ port: the result is owned by the caller.
*/
Polygon2D*
HiddenSurfaceRenderer::translated(Polygon2D* polygon, double dx, double dy)
{
    Polygon2D* result = emptyPolygon();
    for ( long l = 0; l < polygon->loops.size(); l++ ) {
        _Polygon2DContour* loop = polygon->loops[l];
        result->nextLoop();
        for ( long i = 0; i < loop->vertices.size(); i++ ) {
            const Vertex2D& v = loop->vertices[i];
            result->addVertex(v.x + dx, v.y + dy);
        }
    }
    return result;
}

/**
@return one area with all the contours of several areas (that do not
overlap)
*/
_WeilerAthertonArea*
HiddenSurfaceRenderer::merged(const java::ArrayList<_WeilerAthertonArea*>& areas)
{
    if ( areas.size() == 1 ) {
        return areas[0];
    }
    Polygon2D* result = emptyPolygon();
    for ( long a = 0; a < areas.size(); a++ ) {
        Polygon2D* polygon = areas[a]->polygon;
        for ( long l = 0; l < polygon->loops.size(); l++ ) {
            copyLoop(result, polygon->loops[l]);
        }
    }
    return newArea(result);
}

double
HiddenSurfaceRenderer::signedArea(const java::ArrayList<Vector3Dd>& loop)
{
    double area = 0.0;
    long n = loop.size();
    for ( long i = 0; i < n; i++ ) {
        const Vector3Dd& p = loop[i];
        const Vector3Dd& q = loop[(i + 1) % n];
        area += p.x() * q.y() - q.x() * p.y();
    }
    return area / 2.0;
}

double
HiddenSurfaceRenderer::signedArea(_Polygon2DContour* loop)
{
    double area = 0.0;
    long n = loop->vertices.size();
    for ( long i = 0; i < n; i++ ) {
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
    long n = loop->vertices.size();
    for ( long i = 0; i < n; i++ ) {
        const Vertex2D& p = loop->vertices[i];
        const Vertex2D& q = loop->vertices[(i + 1) % n];
        length += std::hypot(q.x - p.x, q.y - p.y);
    }
    return length;
}

double
HiddenSurfaceRenderer::perimeter(Polygon2D* polygon)
{
    double length = 0.0;
    for ( long l = 0; l < polygon->loops.size(); l++ ) {
        length += perimeter(polygon->loops[l]);
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
    for ( long l = 0; l < polygon->loops.size(); l++ ) {
        area += signedArea(polygon->loops[l]);
    }
    return area;
}

/**
@return a copy of a clip result without its degenerate contours (with
less than three vertices or with no area), or null if no contour is left
(C++ port: owned by the caller)
*/
Polygon2D*
HiddenSurfaceRenderer::withoutDegenerateContours(Polygon2D* polygon)
{
    Polygon2D* clean = emptyPolygon();
    bool empty = true;

    for ( long l = 0; l < polygon->loops.size(); l++ ) {
        _Polygon2DContour* loop = polygon->loops[l];
        if ( loop->vertices.size() < 3 ||
             std::fabs(signedArea(loop)) <= AREA_EPSILON ) {
            continue;
        }
        copyLoop(clean, loop);
        empty = false;
    }
    if ( empty ) {
        delete clean;
        return nullptr;
    }
    return clean;
}

/**
@return true if the point is inside the polygon, for the even-odd rule
*/
bool
HiddenSurfaceRenderer::contains(Polygon2D* polygon, double x, double y)
{
    bool inside = false;
    for ( long l = 0; l < polygon->loops.size(); l++ ) {
        _Polygon2DContour* loop = polygon->loops[l];
        if ( loop->vertices.size() >= 3 && containsInLoop(loop, x, y) ) {
            inside = !inside;
        }
    }
    return inside;
}

bool
HiddenSurfaceRenderer::containsInLoop(_Polygon2DContour* loop, double x,
                                      double y)
{
    bool inside = false;
    long n = loop->vertices.size();
    for ( long i = 0, j = n - 1; i < n; j = i++ ) {
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
(C++ port: an output parameter instead of the returned array)
*/
void
HiddenSurfaceRenderer::boundingBox(Polygon2D* polygon, double box[4])
{
    box[0] = java::Double::MAX_VALUE;
    box[1] = java::Double::MAX_VALUE;
    box[2] = -java::Double::MAX_VALUE;
    box[3] = -java::Double::MAX_VALUE;
    for ( long l = 0; l < polygon->loops.size(); l++ ) {
        _Polygon2DContour* loop = polygon->loops[l];
        for ( long i = 0; i < loop->vertices.size(); i++ ) {
            const Vertex2D& v = loop->vertices[i];
            box[0] = java::Math::min(box[0], v.x);
            box[1] = java::Math::min(box[1], v.y);
            box[2] = java::Math::max(box[2], v.x);
            box[3] = java::Math::max(box[3], v.y);
        }
    }
}
