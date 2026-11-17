#include <math.h>

#include "java/lang/Math.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector4Dd.h"
#include "vsdk/toolkit/media/Calligraphic2DBuffer.h"
#include "vsdk/toolkit/media/Calligraphic3DBuffer.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/element/Intersection.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/element/Triangle.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/Volume.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/render/hiddenLine/HiddenLineRenderer.h"
#include "vsdk/toolkit/common/Cpp98Support.h"

class AppelEdgeSegment;

/**
Orders segments by line parameter, with an `VSDK::EPSILON` tolerance (the
`compareTo` of the Java `_AppelEdgeSegment`).
*/
class AppelEdgeSegmentOrder : public Comparator<AppelEdgeSegment> {
public:
    int compare(const AppelEdgeSegment& a, const AppelEdgeSegment& b) const;
};

class AppelEdgeSegment {
public:
    double lineParameter;
    int deltaQI;

    AppelEdgeSegment() : lineParameter(0.0), deltaQI(0) {}

    BOOLEAN samePosition(const AppelEdgeSegment& other) const
    {
        return Cpp98Support::abs(lineParameter - other.lineParameter) <= VSDK::EPSILON;
    }
};

int AppelEdgeSegmentOrder::compare(const AppelEdgeSegment& a,
    const AppelEdgeSegment& b) const
{
    if (a.lineParameter < b.lineParameter - VSDK::EPSILON) return -1;
    if (a.lineParameter > b.lineParameter + VSDK::EPSILON) return 1;
    return 0;
}

class AppelEdgeCache {
public:
    enum {
        HIDDEN_LINE = 0,
        VISIBLE_LINE = 1,
        CONTOUR_LINE = 2
    };

    int edgeType;
    BOOLEAN onSequence;
    Vector3Dd start;
    Vector3Dd end;
    Vector3Dd direction;
    SimpleBody* ownerBody;
    PolyhedralBoundedSolid* ownerSolid;
    _PolyhedralBoundedSolidFace* visibleEdgeForContourLine;
    SimpleBody* visibleEdgeBody;
    _PolyhedralBoundedSolidFace* leftFace;
    _PolyhedralBoundedSolidFace* rightFace;
    int edgeIndex;

    AppelEdgeCache()
        : edgeType(VISIBLE_LINE),
          onSequence(FALSE),
          start(),
          end(),
          direction(),
          ownerBody(0),
          ownerSolid(0),
          visibleEdgeForContourLine(0),
          visibleEdgeBody(0),
          leftFace(0),
          rightFace(0),
          edgeIndex(-1)
    {
    }

    void setStart(const Vector3Dd& s) { start = Vector3Dd(s); }
    void setEnd(const Vector3Dd& e) { end = Vector3Dd(e); }
};

/// `edgeIndex` of the free lines, which are not edges of any solid
static const int FREE_LINE_EDGE_INDEX = -2;

static const double CLIP_PLANES[][4] = {
    { 1.0, 0.0, 0.0, 1.0 },
    { -1.0, 0.0, 0.0, 1.0 },
    { 0.0, 1.0, 0.0, 1.0 },
    { 0.0, -1.0, 0.0, 1.0 },
    { 0.0, 0.0, 1.0, 1.0 },
    { 0.0, 0.0, -1.0, 1.0 }
};

static double evaluateClipPlane(const double plane[4], const Vector4Dd& point)
{
    return plane[0] * point.x() + plane[1] * point.y() +
        plane[2] * point.z() + plane[3] * point.w();
}

static Vector4Dd interpolate(const Vector4Dd& start, const Vector4Dd& end, double t)
{
    return start.multiply(1.0 - t).add(end.multiply(t));
}

static BOOLEAN clipLineToClipVolume(
    const Vector4Dd& start,
    const Vector4Dd& end,
    Vector4Dd& outStart,
    Vector4Dd& outEnd)
{
    outStart = start;
    outEnd = end;

    for (int i = 0; i < 6; ++i) {
        double d0 = evaluateClipPlane(CLIP_PLANES[i], outStart);
        double d1 = evaluateClipPlane(CLIP_PLANES[i], outEnd);

        if (d0 < 0.0 && d1 < 0.0) {
            return FALSE;
        }
        if (d0 < 0.0 || d1 < 0.0) {
            double denominator = d0 - d1;
            if (Cpp98Support::abs(denominator) < VSDK::EPSILON) {
                return FALSE;
            }
            double t = d0 / denominator;
            Vector4Dd intersection = interpolate(outStart, outEnd, t);
            if (d0 < 0.0) {
                outStart = intersection;
            }
            else {
                outEnd = intersection;
            }
        }
    }
    return TRUE;
}

static void addProjectedLine(
    Calligraphic2DBuffer* lineSet,
    const Vector3Dd& point0,
    const Vector3Dd& point1,
    const Camera* camera)
{
    if (lineSet == 0 || camera == 0) {
        return;
    }

    Matrix4x4d projection = camera->calculateProjectionMatrix();
    Vector4Dd clip0 = projection.multiply(Vector4Dd(point0));
    Vector4Dd clip1 = projection.multiply(Vector4Dd(point1));
    Vector4Dd clipped0(0.0, 0.0, 0.0, 1.0);
    Vector4Dd clipped1(0.0, 0.0, 0.0, 1.0);
    if (!clipLineToClipVolume(clip0, clip1, clipped0, clipped1)) {
        return;
    }

    Vector4Dd ndc0 = clipped0.dividedByW();
    Vector4Dd ndc1 = clipped1.dividedByW();
    lineSet->add2DLine(ndc0.x(), ndc0.y(), ndc1.x(), ndc1.y());
}

static Vector3Dd transformToWorld(SimpleBody* body, const Vector3Dd& localPoint)
{
    if (body == 0) {
        return localPoint;
    }
    return body->getTransformationMatrix().multiply(localPoint);
}

static Vector3Dd transformToLocal(SimpleBody* body, const Vector3Dd& worldPoint)
{
    if (body == 0) {
        return worldPoint;
    }

    Vector3Dd translatedPoint = worldPoint.subtract(body->getPosition());
    Vector3Dd rotatedPoint = body->getRotationInverse().multiply(translatedPoint);
    Vector3Dd scale = body->getScale();

    double x = Cpp98Support::abs(scale.x()) > VSDK::EPSILON ? rotatedPoint.x() / scale.x() : 0.0;
    double y = Cpp98Support::abs(scale.y()) > VSDK::EPSILON ? rotatedPoint.y() / scale.y() : 0.0;
    double z = Cpp98Support::abs(scale.z()) > VSDK::EPSILON ? rotatedPoint.z() / scale.z() : 0.0;
    return Vector3Dd(x, y, z);
}

static InfinitePlane* getWorldContainingPlane(
    _PolyhedralBoundedSolidFace* face,
    SimpleBody* body)
{
    if (face == 0) {
        return 0;
    }

    InfinitePlane* localPlane = face->getContainingPlane();
    if (localPlane == 0) {
        return 0;
    }

    Vector3Dd pointOnFace;
    BOOLEAN foundPoint = FALSE;
    for (long int i = 0; i < face->boundariesList.size() && !foundPoint; ++i) {
        _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(i);
        _PolyhedralBoundedSolidHalfEdge* he =
            loop != 0 ? loop->boundaryStartHalfEdge : 0;
        if (he != 0 && he->startingVertex != 0) {
            pointOnFace = he->startingVertex->position;
            foundPoint = TRUE;
        }
    }

    if (!foundPoint) {
        delete localPlane;
        return 0;
    }

    Vector3Dd worldPoint = transformToWorld(body, pointOnFace);
    Vector3Dd localNormal = localPlane->getNormal();
    delete localPlane;

    Vector3Dd worldNormal;
    if (body == 0) {
        worldNormal = localNormal;
    }
    else {
        Vector3Dd scale = body->getScale();
        Vector3Dd scaledNormal(
            Cpp98Support::abs(scale.x()) > VSDK::EPSILON ? localNormal.x() / scale.x() : 0.0,
            Cpp98Support::abs(scale.y()) > VSDK::EPSILON ? localNormal.y() / scale.y() : 0.0,
            Cpp98Support::abs(scale.z()) > VSDK::EPSILON ? localNormal.z() / scale.z() : 0.0);
        worldNormal = body->getRotation().withoutTranslation().transformDirection(
            scaledNormal).normalized();
    }

    if (worldNormal.length() <= VSDK::EPSILON) {
        return 0;
    }

    return new InfinitePlane(worldNormal.normalized(), worldPoint);
}

static int isFaceVisibleFromCameraTransformed(
    _PolyhedralBoundedSolidFace* face,
    SimpleBody* body,
    const Camera* camera)
{
    if (face == 0 || camera == 0) {
        return 0;
    }

    Vector3Dd iv(1, 0, 0);
    Vector3Dd viewingVector = camera->getRotation().multiply(iv);
    InfinitePlane* plane = getWorldContainingPlane(face, body);
    if (plane == 0) {
        return 0;
    }
    Vector3Dd n = plane->getNormal().normalized();
    delete plane;

    if (camera->getProjectionMode() == Camera::PROJECTION_MODE_ORTHOGONAL) {
        viewingVector = viewingVector.normalized();
        double dot = n.dotProduct(viewingVector);
        if (dot > VSDK::EPSILON) {
            return -1;
        }
        else if (dot < -VSDK::EPSILON) {
            return 1;
        }
        return 0;
    }

    Vector3Dd cameraPosition = camera->getPosition();
    for (long int i = 0; i < face->boundariesList.size(); ++i) {
        _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(i);
        if (loop == 0 || loop->boundaryStartHalfEdge == 0) {
            continue;
        }
        _PolyhedralBoundedSolidHalfEdge* he = loop->boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge* heStart = he;
        do {
            he = he->next();
            if (he == 0 || he->startingVertex == 0) {
                break;
            }
            Vector3Dd p = transformToWorld(body, he->startingVertex->position);
            Vector3Dd t = p.subtract(cameraPosition).multiply(-1).normalized();
            if (t.dotProduct(n) > 0.0) {
                return 1;
            }
        } while (he != heStart);
    }
    return -1;
}

static void buildCache(
    ArrayList<HiddenLineQuerySolid>& solids,
    SimpleBody* body,
    PolyhedralBoundedSolid* solid,
    ArrayList<AppelEdgeCache>& cache,
    ArrayList<AppelEdgeCache*>& contourCache,
    const Camera* camera,
    BOOLEAN ownsSolid)
{
    if (body == 0 || solid == 0) {
        return;
    }

    HiddenLineQuerySolid querySolid;
    querySolid.body = body;
    querySolid.solid = solid;
    querySolid.ownsSolid = ownsSolid;
    solids.add(querySolid);

    Vector3Dd prevEnd;
    BOOLEAN hasPrevEnd = FALSE;
    CircularArrayList<_PolyhedralBoundedSolidEdge*>& edges = solid->getEdgesList();

    for (long int i = 0; i < edges.size(); ++i) {
        _PolyhedralBoundedSolidEdge* edge = edges.get(i);
        if (edge == 0 || edge->leftHalf == 0 || edge->rightHalf == 0 ||
            edge->leftHalf->startingVertex == 0 ||
            edge->rightHalf->startingVertex == 0) {
            continue;
        }

        _PolyhedralBoundedSolidFace* face1 = edge->leftHalf->parentLoop != 0 ?
            edge->leftHalf->parentLoop->parentFace : 0;
        _PolyhedralBoundedSolidFace* face2 = edge->rightHalf->parentLoop != 0 ?
            edge->rightHalf->parentLoop->parentFace : 0;
        if (face1 == 0 || face2 == 0) {
            continue;
        }

        Vector3Dd startPosition =
            transformToWorld(body, edge->leftHalf->startingVertex->position);
        Vector3Dd endPosition =
            transformToWorld(body, edge->rightHalf->startingVertex->position);
        BOOLEAN f1 = isFaceVisibleFromCameraTransformed(face1, body, camera) >= 0;
        BOOLEAN f2 = isFaceVisibleFromCameraTransformed(face2, body, camera) >= 0;

        cache.add(AppelEdgeCache());
        AppelEdgeCache& materialLine = cache[cache.size() - 1];
        materialLine.setStart(startPosition);
        materialLine.setEnd(endPosition);
        materialLine.direction = endPosition.subtract(startPosition);
        materialLine.ownerBody = body;
        materialLine.ownerSolid = solid;
        materialLine.leftFace = face1;
        materialLine.rightFace = face2;
        materialLine.edgeIndex = ((int)(i));
        materialLine.onSequence = hasPrevEnd &&
            prevEnd.subtract(startPosition).length() < VSDK::EPSILON;

        if (!f1 && !f2) {
            materialLine.edgeType = AppelEdgeCache::HIDDEN_LINE;
        }
        else if ((f1 && !f2) || (!f1 && f2)) {
            materialLine.edgeType = AppelEdgeCache::CONTOUR_LINE;
            materialLine.visibleEdgeForContourLine = f1 ? face1 : face2;
            materialLine.visibleEdgeBody = body;
            // C++ port note: the pointer to the cache entry is added to
            // `contourCache` by `executeAppelAlgorithm` once the cache is
            // complete, as growing `cache` moves its entries
        }
        else {
            materialLine.edgeType = AppelEdgeCache::VISIBLE_LINE;
        }

        prevEnd = Vector3Dd::copyOf(endPosition);
        hasPrevEnd = TRUE;
    }
}

static int computeMidpointQuantitativeInvisibility(
    ArrayList<HiddenLineQuerySolid>& solids,
    const Camera* camera,
    const Vector3Dd& midpoint)
{
    int qi = 0;
    for (long int i = 0; i < solids.size(); ++i) {
        HiddenLineQuerySolid& querySolid = solids[i];
        if (querySolid.body == 0 || querySolid.solid == 0) {
            continue;
        }
        Vector3Dd localEye =
            transformToLocal(querySolid.body, camera->getPosition());
        Vector3Dd localPoint =
            transformToLocal(querySolid.body, midpoint);
        qi += querySolid.solid->computeQuantitativeInvisibility(
            localEye, localPoint);
    }
    return qi;
}

static BOOLEAN isUnitInterval(double t)
{
    return t >= VSDK::EPSILON && t <= 1.0 - VSDK::EPSILON;
}

/**
Adds to `segments` the line parameters where an edge (or a free line) is
crossed in the image by a contour line: the piercing points of the
sweeping planes "SP2" of the contours (from the eye along each contour
line, figure [APPE1967].5) with the edge, for the contours that pierce
the sweeping plane "SP1" of the edge (the triangle `sp1a`, `sp1b`,
`sp1c`). The quantitative invisibility of a line of a solid can only
change at those points.
*/
static void addContourCrossings(
    AppelEdgeCache& inEdge,
    const Vector3Dd& sp1a,
    const Vector3Dd& sp1b,
    const Vector3Dd& sp1c,
    const Camera* inCamera,
    ArrayList<AppelEdgeCache*>& contourCache,
    ArrayList<AppelEdgeSegment>& segments)
{
    Vector3Dd sp2c = inCamera->getPosition();

    long i;
    for (i = 0; i < contourCache.size(); ++i) {
        AppelEdgeCache* cl = contourCache[i];
        if (cl == 0 || cl == &inEdge) {
            continue;
        }

        Ray ray(
            cl->start.add(cl->direction.multiply(3 * VSDK::EPSILON)),
            cl->direction);
        double t0 = ray.getDirection().length() - 6 * VSDK::EPSILON;
        ray = ray.withDirection(ray.getDirection().normalized());
        Intersection* hit = Triangle::doIntersectionWithTriangle(
            ray, sp1a, sp1b, sp1c);
        if (hit != 0 && hit->t < t0) {
            InfinitePlane plane(cl->start, cl->end, sp2c);
            Ray edgeRay(inEdge.start, inEdge.direction.normalized());
            Ray* planeHit = plane.doIntersectionFirstHit(edgeRay);
            if (planeHit != 0) {
                AppelEdgeSegment segment;
                segment.lineParameter = planeHit->getT() / inEdge.direction.length();
                if (isUnitInterval(segment.lineParameter)) {
                    segments.add(segment);
                }
                delete planeHit;
            }
        }
        delete hit;
    }
}

/**
Sorts the breaking points of a line by their line parameter.
*/
static void sortAndMergeSegments(ArrayList<AppelEdgeSegment>& segments)
{
    long i;

    segments.sort(AppelEdgeSegmentOrder());

    for (i = 0; i + 1 < segments.size(); ) {
        if (segments[i].samePosition(segments[i + 1])) {
            segments[i + 1].deltaQI += segments[i].deltaQI;
            segments.remove(i);
        }
        else {
            ++i;
        }
    }
}

static BOOLEAN segmentOverlapsMinMax(const Vector3Dd& a, const Vector3Dd& b,
                                     const double* minmax)
{
    double tolerance = 10*VSDK::EPSILON;

    if (minmax == 0) {
        return TRUE;
    }
    return Math::max(a.x(), b.x()) >= minmax[0] - tolerance &&
           Math::min(a.x(), b.x()) <= minmax[3] + tolerance &&
           Math::max(a.y(), b.y()) >= minmax[1] - tolerance &&
           Math::min(a.y(), b.y()) <= minmax[4] + tolerance &&
           Math::max(a.z(), b.z()) >= minmax[2] - tolerance &&
           Math::min(a.z(), b.z()) <= minmax[5] + tolerance;
}

/**
Adds to `segments` the line parameters where a free line pierces the
boundary of a solid: unlike the edges of the solids, which never cross
other faces (see `buildCache`), a free line can pass through a solid (i.e.
a reference grid through the solids standing on it), and its quantitative
invisibility also changes there, where its visible part enters into the
solid.
The test is made in the coordinates of each solid, where the line
parameter is the same (it is preserved by affine transformations): the
crossing with the containing plane of each face is a piercing point if it
lies on the boundary of the solid.

C++ port note: the solids carry their boundary representations, the
Java list `breps`; the bounds and planes returned by the solids are
deleted here.
*/
static void addSolidPiercings(
    ArrayList<HiddenLineQuerySolid>& solids,
    AppelEdgeCache& inLine,
    ArrayList<AppelEdgeSegment>& segments)
{
    long i;
    long j;

    for (i = 0; i < solids.size(); i++) {
        SimpleBody* body = solids[i].body;
        PolyhedralBoundedSolid* brep = solids[i].solid;
        if (body == 0 || brep == 0) {
            continue;
        }
        Vector3Dd a = transformToLocal(body, inLine.start);
        Vector3Dd b = transformToLocal(body, inLine.end);

        double* minmax = brep->getMinMax();
        BOOLEAN overlaps = segmentOverlapsMinMax(a, b, minmax);
        delete[] minmax;
        if (!overlaps) {
            continue;
        }
        for (j = 0; j < brep->getPolygonsList().size(); j++) {
            InfinitePlane* plane =
                brep->getPolygonsList().get(j)->getContainingPlane();
            if (plane == 0) {
                continue;
            }
            double da = plane->pointDistance(a);
            double db = plane->pointDistance(b);
            delete plane;
            if (da * db >= 0.0) {
                // Both ends at the same side, or one of them on the plane
                // (then it is a breaking point of the line already)
                continue;
            }
            double t = da / (da - db);
            if (!isUnitInterval(t)) {
                continue;
            }
            Vector3Dd p = a.add(b.subtract(a).multiply(t));
            if (brep->doContainmentTest(p, 10*VSDK::EPSILON) != Geometry::LIMIT) {
                continue;
            }
            AppelEdgeSegment segment;
            segment.lineParameter = t;
            segments.add(segment);
        }
    }
}

/**
Draws a free line: a line that is not an edge of any of the solids (i.e.
a curve or a reference grid), so it is hidden by the solids but does not
hide anything, nor is a contour. As the lines of the solids, it is broken
where its quantitative invisibility can change (where contour lines cross
it in the image, and where it pierces a solid), and each sub-segment is
classified by the quantitative invisibility of its midpoint.
*/
static void processFreeLineToBeDrawn(
    ArrayList<HiddenLineQuerySolid>& solids,
    AppelEdgeCache& inLine,
    const Camera* inCamera,
    Calligraphic2DBuffer* outVisibleLineSet,
    Calligraphic2DBuffer* outHiddenLineSet,
    ArrayList<AppelEdgeCache*>& contourCache)
{
    ArrayList<AppelEdgeSegment> segments;
    AppelEdgeSegment segment;
    long i;

    if (inLine.direction.length() < VSDK::EPSILON) {
        return;
    }

    segment.lineParameter = 0;
    segments.add(segment);
    addContourCrossings(inLine, inLine.start, inLine.end,
        inCamera->getPosition(), inCamera, contourCache, segments);
    addSolidPiercings(solids, inLine, segments);
    segment = AppelEdgeSegment();
    segment.lineParameter = 1;
    segments.add(segment);
    sortAndMergeSegments(segments);

    for (i = 0; i < segments.size()-1; i++) {
        double val1 = segments[i].lineParameter;
        double val2 = segments[i+1].lineParameter;
        Vector3Dd pos1 = inLine.start.add(inLine.direction.multiply(val1));
        Vector3Dd pos2 = inLine.start.add(inLine.direction.multiply(val2));
        Vector3Dd posx = inLine.start.add(inLine.direction.multiply((val1+val2)/2));

        if (computeMidpointQuantitativeInvisibility(solids, inCamera,
                posx) == 0) {
            addProjectedLine(outVisibleLineSet, pos1, pos2, inCamera);
        }
        else {
            addProjectedLine(outHiddenLineSet, pos1, pos2, inCamera);
        }
    }
}

static void processLineToBeDrawn(
    ArrayList<HiddenLineQuerySolid>& solids,
    AppelEdgeCache& inEdge,
    const Camera* inCamera,
    Calligraphic2DBuffer* outVisibleContourLineSet,
    Calligraphic2DBuffer* outVisibleNonContourLineSet,
    Calligraphic2DBuffer* outHiddenLineSet,
    ArrayList<AppelEdgeCache*>& contourCache)
{
    //- 1. Compute the sweep plane triangle ---------------------------
    // Defines plane "SP1" on figure [APPE1967].5.
    Vector3Dd sp1a = inEdge.start;
    Vector3Dd sp1b = inEdge.end;
    Vector3Dd sp1c = inCamera->getPosition();

    //- 2. Break current edge into segments ---------------------------
    ArrayList<AppelEdgeSegment> segments;
    long i;

    segments.add(AppelEdgeSegment());
    addContourCrossings(inEdge, sp1a, sp1b, sp1c, inCamera, contourCache,
        segments);
    AppelEdgeSegment endSegment;
    endSegment.lineParameter = 1.0;
    segments.add(endSegment);

    //- 3. Sort segment set -------------------------------------------
    sortAndMergeSegments(segments);

    for (i = 0; i + 1 < segments.size(); ++i) {
        double t1 = segments[i].lineParameter;
        double t2 = segments[i + 1].lineParameter;
        Vector3Dd pos1 = inEdge.start.add(inEdge.direction.multiply(t1));
        Vector3Dd pos2 = inEdge.start.add(inEdge.direction.multiply(t2));
        Vector3Dd midpoint = inEdge.start.add(
            inEdge.direction.multiply((t1 + t2) * 0.5));

        int midpointQi =
            computeMidpointQuantitativeInvisibility(solids, inCamera, midpoint);

        if (midpointQi == 0) {
            if (inEdge.edgeType == AppelEdgeCache::CONTOUR_LINE) {
                addProjectedLine(outVisibleContourLineSet, pos1, pos2, inCamera);
            }
            else {
                addProjectedLine(outVisibleNonContourLineSet, pos1, pos2, inCamera);
            }
        }
        else {
            addProjectedLine(outHiddenLineSet, pos1, pos2, inCamera);
        }
    }
}


int HiddenLineRenderer::isFaceVisibleFromCamera(
    _PolyhedralBoundedSolidFace* face,
    const Camera* camera)
{
    if (face == 0 || camera == 0) {
        return 0;
    }

    Vector3Dd iv(1, 0, 0);
    Vector3Dd viewingVector = camera->getRotation().multiply(iv);
    InfinitePlane* plane = face->getContainingPlane();
    if (plane == 0) {
        return 0;
    }
    Vector3Dd n = plane->getNormal().normalized();
    delete plane;

    if (camera->getProjectionMode() == Camera::PROJECTION_MODE_ORTHOGONAL) {
        viewingVector = viewingVector.normalized();
        double dot = n.dotProduct(viewingVector);
        if (dot > VSDK::EPSILON) {
            return -1;
        }
        else if (dot < -VSDK::EPSILON) {
            return 1;
        }
        return 0;
    }

    Vector3Dd cameraPosition = camera->getPosition();
    for (long int i = 0; i < face->boundariesList.size(); ++i) {
        _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(i);
        if (loop == 0 || loop->boundaryStartHalfEdge == 0) {
            continue;
        }
        _PolyhedralBoundedSolidHalfEdge* he = loop->boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge* heStart = he;
        do {
            he = he->next();
            if (he == 0 || he->startingVertex == 0) {
                break;
            }
            Vector3Dd p = he->startingVertex->position;
            Vector3Dd t = p.subtract(cameraPosition).multiply(-1).normalized();
            if (t.dotProduct(n) > 0.0) {
                return 1;
            }
        } while (he != heStart);
    }
    return -1;
}

void HiddenLineRenderer::executeAppelAlgorithm(
    ArrayList<SimpleBody*>& inSimpleBodyArray,
    const Camera* inCamera,
    Calligraphic2DBuffer* outVisibleContourLineSet,
    Calligraphic2DBuffer* outVisibleNonContourLineSet,
    Calligraphic2DBuffer* outHiddenLineSet)
{
    runAppelAlgorithm(inSimpleBodyArray, inCamera, outVisibleContourLineSet,
        outVisibleNonContourLineSet, outHiddenLineSet, 0, 0, 0);
}

void HiddenLineRenderer::executeAppelAlgorithm(
    ArrayList<SimpleBody*>& inSimpleBodyArray,
    const Camera* inCamera,
    Calligraphic2DBuffer* outVisibleContourLineSet,
    Calligraphic2DBuffer* outVisibleNonContourLineSet,
    Calligraphic2DBuffer* outHiddenLineSet,
    ArrayList<Calligraphic3DBuffer*>& inFreeLineSets,
    ArrayList<Calligraphic2DBuffer*>& outVisibleFreeLineSets,
    ArrayList<Calligraphic2DBuffer*>& outHiddenFreeLineSets)
{
    runAppelAlgorithm(inSimpleBodyArray, inCamera, outVisibleContourLineSet,
        outVisibleNonContourLineSet, outHiddenLineSet, &inFreeLineSets,
        &outVisibleFreeLineSets, &outHiddenFreeLineSets);
}

static void processFreeLineSets(
    ArrayList<HiddenLineQuerySolid>& solids,
    const Camera* inCamera,
    ArrayList<AppelEdgeCache*>& contourCache,
    ArrayList<Calligraphic3DBuffer*>& inFreeLineSets,
    ArrayList<Calligraphic2DBuffer*>& outVisibleFreeLineSets,
    ArrayList<Calligraphic2DBuffer*>& outHiddenFreeLineSets)
{
    long i;
    int j;

    for (i = 0; i < inFreeLineSets.size(); i++) {
        Calligraphic3DBuffer* lines = inFreeLineSets[i];
        for (j = 0; j < lines->getNumLines(); j++) {
            Vector3Dd ends[2];
            lines->get3DLine(j, ends);
            AppelEdgeCache freeLine;
            freeLine.setStart(ends[0]);
            freeLine.setEnd(ends[1]);
            freeLine.direction = freeLine.end.subtract(freeLine.start);
            freeLine.edgeType = AppelEdgeCache::VISIBLE_LINE;
            // Not an edge of a solid (nor selectable by debugEdgeIndex)
            freeLine.edgeIndex = FREE_LINE_EDGE_INDEX;
            processFreeLineToBeDrawn(solids, freeLine, inCamera,
                outVisibleFreeLineSets[i], outHiddenFreeLineSets[i],
                contourCache);
        }
    }
}

/**
C++ port note: the free line lists are null when there are no free lines.
*/
void HiddenLineRenderer::runAppelAlgorithm(
    ArrayList<SimpleBody*>& inSimpleBodyArray,
    const Camera* inCamera,
    Calligraphic2DBuffer* outVisibleContourLineSet,
    Calligraphic2DBuffer* outVisibleNonContourLineSet,
    Calligraphic2DBuffer* outHiddenLineSet,
    ArrayList<Calligraphic3DBuffer*>* inFreeLineSets,
    ArrayList<Calligraphic2DBuffer*>* outVisibleFreeLineSets,
    ArrayList<Calligraphic2DBuffer*>* outHiddenFreeLineSets)
{
    if (inCamera == 0 || outVisibleContourLineSet == 0 ||
        outVisibleNonContourLineSet == 0 || outHiddenLineSet == 0) {
        return;
    }

    ArrayList<HiddenLineQuerySolid> querySolids;
    ArrayList<AppelEdgeCache> cache;
    ArrayList<AppelEdgeCache*> contourCache;

    long int i;
    for (i = 0; i < inSimpleBodyArray.size(); ++i) {
        SimpleBody* body = inSimpleBodyArray.get(i);
        if (body == 0 || body->getGeometry() == 0) {
            continue;
        }
        Volume* volume = (body->getGeometry() != NULL && body->getGeometry()->instanceOf(Volume::CLASS) ? (Volume*)body->getGeometry() : NULL);
        if (volume == 0) {
            continue;
        }

        PolyhedralBoundedSolid* solid = volume->exportToPolyhedralBoundedSolid();
        if (solid == 0) {
            continue;
        }

        BOOLEAN ownsSolid = ((Geometry*)(solid)) != body->getGeometry();
        buildCache(
            querySolids,
            body,
            solid,
            cache,
            contourCache,
            inCamera,
            ownsSolid);
    }

    for (i = 0; i < cache.size(); ++i) {
        if (cache[i].edgeType == AppelEdgeCache::CONTOUR_LINE) {
            contourCache.add(&cache[i]);
        }
    }

    for (i = 0; i < querySolids.size(); ++i) {
        if (querySolids[i].solid != 0) {
            querySolids[i].solid->beginVisibilityQueries();
        }
    }

    for (i = 0; i < cache.size(); ++i) {
        AppelEdgeCache& edge = cache[i];
        switch (edge.edgeType) {
          case AppelEdgeCache::HIDDEN_LINE:
          case AppelEdgeCache::CONTOUR_LINE:
          case AppelEdgeCache::VISIBLE_LINE:
            processLineToBeDrawn(querySolids, edge, inCamera,
                outVisibleContourLineSet, outVisibleNonContourLineSet,
                outHiddenLineSet, contourCache);
            break;
          default:
            break;
        }
    }
    if (inFreeLineSets != 0) {
        processFreeLineSets(querySolids, inCamera, contourCache,
            *inFreeLineSets, *outVisibleFreeLineSets, *outHiddenFreeLineSets);
    }

    for (i = 0; i < querySolids.size(); ++i) {
        if (querySolids[i].solid != 0) {
            querySolids[i].solid->endVisibilityQueries();
        }
        if (querySolids[i].ownsSolid) {
            delete querySolids[i].solid;
            querySolids[i].solid = 0;
        }
    }
}
