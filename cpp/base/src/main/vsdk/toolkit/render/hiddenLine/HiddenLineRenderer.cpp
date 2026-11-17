#include "java/lang/Math.h"
#include <cmath>

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
#include "java/lang/String.h"
#include "java/lang/System.h"

int HiddenLineRenderer::debugEdgeIndex = -1;

namespace {

/// `edgeIndex` of the free lines, which are not edges of any solid
const int FREE_LINE_EDGE_INDEX = -2;

void debugSplit(int edgeIndex, const java::String& message)
{
    if ( edgeIndex == HiddenLineRenderer::debugEdgeIndex ) {
        java::System::out.println((java::String("[AppelSplit e") +
            java::String::valueOf(edgeIndex) + "] " + message).c_str());
    }
}

java::String format5(double value)
{
    return java::String::format("%.5f", value);
}

class AppelEdgeSegment {
public:
    double lineParameter;
    int deltaQI;

    AppelEdgeSegment() : lineParameter(0.0), deltaQI(0) {}

    bool samePosition(const AppelEdgeSegment& other) const
    {
        return std::abs(lineParameter - other.lineParameter) <= VSDK::EPSILON;
    }
};

class AppelEdgeCache {
public:
    static const int HIDDEN_LINE = 0;
    static const int VISIBLE_LINE = 1;
    static const int CONTOUR_LINE = 2;

    int edgeType;
    bool onSequence;
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
          onSequence(false),
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

const double CLIP_PLANES[][4] = {
    { 1.0, 0.0, 0.0, 1.0 },
    { -1.0, 0.0, 0.0, 1.0 },
    { 0.0, 1.0, 0.0, 1.0 },
    { 0.0, -1.0, 0.0, 1.0 },
    { 0.0, 0.0, 1.0, 1.0 },
    { 0.0, 0.0, -1.0, 1.0 }
};

double evaluateClipPlane(const double plane[4], const Vector4Dd& point)
{
    return plane[0] * point.x() + plane[1] * point.y() +
        plane[2] * point.z() + plane[3] * point.w();
}

Vector4Dd interpolate(const Vector4Dd& start, const Vector4Dd& end, double t)
{
    return start.multiply(1.0 - t).add(end.multiply(t));
}

bool clipLineToClipVolume(
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
            return false;
        }
        if (d0 < 0.0 || d1 < 0.0) {
            double denominator = d0 - d1;
            if (std::abs(denominator) < VSDK::EPSILON) {
                return false;
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
    return true;
}

void addProjectedLine(
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

Vector3Dd transformToWorld(SimpleBody* body, const Vector3Dd& localPoint)
{
    if (body == 0) {
        return localPoint;
    }
    return body->getTransformationMatrix().multiply(localPoint);
}

Vector3Dd transformToLocal(SimpleBody* body, const Vector3Dd& worldPoint)
{
    if (body == 0) {
        return worldPoint;
    }

    Vector3Dd translatedPoint = worldPoint.subtract(body->getPosition());
    Vector3Dd rotatedPoint = body->getRotationInverse().multiply(translatedPoint);
    Vector3Dd scale = body->getScale();

    double x = std::abs(scale.x()) > VSDK::EPSILON ? rotatedPoint.x() / scale.x() : 0.0;
    double y = std::abs(scale.y()) > VSDK::EPSILON ? rotatedPoint.y() / scale.y() : 0.0;
    double z = std::abs(scale.z()) > VSDK::EPSILON ? rotatedPoint.z() / scale.z() : 0.0;
    return Vector3Dd(x, y, z);
}

InfinitePlane* getWorldContainingPlane(
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
    bool foundPoint = false;
    for (long int i = 0; i < face->boundariesList.size() && !foundPoint; ++i) {
        _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(i);
        _PolyhedralBoundedSolidHalfEdge* he =
            loop != 0 ? loop->boundaryStartHalfEdge : 0;
        if (he != 0 && he->startingVertex != 0) {
            pointOnFace = he->startingVertex->position;
            foundPoint = true;
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
            std::abs(scale.x()) > VSDK::EPSILON ? localNormal.x() / scale.x() : 0.0,
            std::abs(scale.y()) > VSDK::EPSILON ? localNormal.y() / scale.y() : 0.0,
            std::abs(scale.z()) > VSDK::EPSILON ? localNormal.z() / scale.z() : 0.0);
        worldNormal = body->getRotation().withoutTranslation().transformDirection(
            scaledNormal).normalized();
    }

    if (worldNormal.length() <= VSDK::EPSILON) {
        return 0;
    }

    return new InfinitePlane(worldNormal.normalized(), worldPoint);
}

int isFaceVisibleFromCameraTransformed(
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

void buildCache(
    java::ArrayList<HiddenLineQuerySolid>& solids,
    SimpleBody* body,
    PolyhedralBoundedSolid* solid,
    java::ArrayList<AppelEdgeCache*>& cache,
    java::ArrayList<AppelEdgeCache*>& contourCache,
    const Camera* camera,
    bool ownsSolid)
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
    bool hasPrevEnd = false;
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
        bool f1 = isFaceVisibleFromCameraTransformed(face1, body, camera) >= 0;
        bool f2 = isFaceVisibleFromCameraTransformed(face2, body, camera) >= 0;

        // On the heap, as the references of the Java version: contourCache
        // keeps pointers to them while cache grows.
        cache.add(new AppelEdgeCache());
        AppelEdgeCache& materialLine = *cache[cache.size() - 1];
        materialLine.setStart(startPosition);
        materialLine.setEnd(endPosition);
        materialLine.direction = endPosition.subtract(startPosition);
        materialLine.ownerBody = body;
        materialLine.ownerSolid = solid;
        materialLine.leftFace = face1;
        materialLine.rightFace = face2;
        materialLine.edgeIndex = static_cast<int>(i);
        materialLine.onSequence = hasPrevEnd &&
            prevEnd.subtract(startPosition).length() < VSDK::EPSILON;

        if (!f1 && !f2) {
            materialLine.edgeType = AppelEdgeCache::HIDDEN_LINE;
        }
        else if ((f1 && !f2) || (!f1 && f2)) {
            materialLine.edgeType = AppelEdgeCache::CONTOUR_LINE;
            materialLine.visibleEdgeForContourLine = f1 ? face1 : face2;
            materialLine.visibleEdgeBody = body;
            contourCache.add(&materialLine);
        }
        else {
            materialLine.edgeType = AppelEdgeCache::VISIBLE_LINE;
        }

        prevEnd = Vector3Dd::copyOf(endPosition);
        hasPrevEnd = true;
    }
}

int computeMidpointQuantitativeInvisibility(
    java::ArrayList<HiddenLineQuerySolid>& solids,
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

bool isUnitInterval(double t)
{
    return t >= VSDK::EPSILON && t <= 1.0 - VSDK::EPSILON;
}

HiddenLineRenderer::AppelEdgeDump* createEdgeDump(const AppelEdgeCache& edge)
{
    HiddenLineRenderer::AppelEdgeDump* dump =
        new HiddenLineRenderer::AppelEdgeDump();
    dump->edgeIndex = edge.edgeIndex;
    dump->edgeType = edge.edgeType;
    dump->edgeTypeName = HiddenLineRenderer::edgeTypeName(edge.edgeType);
    dump->face1Id = edge.leftFace != 0 ? edge.leftFace->id : -1;
    dump->face2Id = edge.rightFace != 0 ? edge.rightFace->id : -1;
    dump->start = Vector3Dd(edge.start);
    dump->end = Vector3Dd(edge.end);
    return dump;
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
void addContourCrossings(
    AppelEdgeCache& inEdge,
    const Vector3Dd& sp1a,
    const Vector3Dd& sp1b,
    const Vector3Dd& sp1c,
    const Camera* inCamera,
    java::ArrayList<AppelEdgeCache*>& contourCache,
    java::ArrayList<AppelEdgeSegment>& segments)
{
    Vector3Dd sp2c = inCamera->getPosition();

    for (long int i = 0; i < contourCache.size(); ++i) {
        AppelEdgeCache* cl = contourCache[i];
        if (cl == 0 || cl == &inEdge) {
            continue;
        }

        // As Java, `withDirection` keeps the contour direction unnormalized
        // (the constructors normalize it), so `t0` is the contour length
        Ray ray = Ray()
            .withOrigin(cl->start.add(cl->direction.multiply(3 * VSDK::EPSILON)))
            .withDirection(cl->direction);
        double t0 = ray.getDirection().length() - 6 * VSDK::EPSILON;
        ray = ray.withDirection(ray.getDirection().normalized());
        Intersection* hit = Triangle::doIntersectionWithTriangle(
            ray, sp1a, sp1b, sp1c);
        if (inEdge.edgeIndex == HiddenLineRenderer::debugEdgeIndex &&
            hit != 0) {
            debugSplit(inEdge.edgeIndex, java::String("contour cl.edgeIndex=") +
                java::String::valueOf(cl->edgeIndex) + " sweepHit t=" +
                format5(hit->t) + " t0=" + format5(t0) + " accepted=" +
                (hit->t < t0 ? "true" : "false"));
        }
        if (hit != 0 && hit->t < t0) {
            InfinitePlane plane(cl->start, cl->end, sp2c);
            Ray edgeRay(inEdge.start, inEdge.direction.normalized());
            Ray* planeHit = plane.doIntersectionFirstHit(edgeRay);
            if (planeHit == 0) {
                debugSplit(inEdge.edgeIndex, java::String("  cl=") +
                    java::String::valueOf(cl->edgeIndex) +
                    " DISCARDED: edge/SP2 plane intersection null");
            }
            else {
                AppelEdgeSegment segment;
                segment.lineParameter = planeHit->getT() / inEdge.direction.length();
                if (!isUnitInterval(segment.lineParameter)) {
                    debugSplit(inEdge.edgeIndex, java::String("  cl=") +
                        java::String::valueOf(cl->edgeIndex) +
                        " DISCARDED: split t=" + format5(segment.lineParameter) +
                        " outside unit interval");
                }
                else {
                    debugSplit(inEdge.edgeIndex, java::String("  cl=") +
                        java::String::valueOf(cl->edgeIndex) +
                        " ADDED split t=" + format5(segment.lineParameter));
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
void sortAndMergeSegments(java::ArrayList<AppelEdgeSegment>& segments)
{
    segments.sort([](const AppelEdgeSegment& a, const AppelEdgeSegment& b) {
            if (a.lineParameter < b.lineParameter - VSDK::EPSILON) return true;
            if (a.lineParameter > b.lineParameter + VSDK::EPSILON) return false;
            return false;
        });

    for (size_t i = 0; i + 1 < segments.size(); ) {
        if (segments[i].samePosition(segments[i + 1])) {
            segments[i + 1].deltaQI += segments[i].deltaQI;
            segments.remove(static_cast<long>(i));
        }
        else {
            ++i;
        }
    }
}

bool segmentOverlapsMinMax(const Vector3Dd& a, const Vector3Dd& b,
                           const double* minmax)
{
    double tolerance = 10*VSDK::EPSILON;

    if ( minmax == 0 ) {
        return true;
    }
    return java::Math::max(a.x(), b.x()) >= minmax[0] - tolerance &&
           java::Math::min(a.x(), b.x()) <= minmax[3] + tolerance &&
           java::Math::max(a.y(), b.y()) >= minmax[1] - tolerance &&
           java::Math::min(a.y(), b.y()) <= minmax[4] + tolerance &&
           java::Math::max(a.z(), b.z()) >= minmax[2] - tolerance &&
           java::Math::min(a.z(), b.z()) <= minmax[5] + tolerance;
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
(C++ port: the boundary representation of each solid is the one kept with
its body in `solids`.)
*/
void addSolidPiercings(
    java::ArrayList<HiddenLineQuerySolid>& solids,
    AppelEdgeCache& inLine,
    java::ArrayList<AppelEdgeSegment>& segments)
{
    long int i;
    int j;

    for ( i = 0; i < solids.size(); i++ ) {
        SimpleBody* body = solids[i].body;
        PolyhedralBoundedSolid* brep = solids[i].solid;
        Vector3Dd a = transformToLocal(body, inLine.start);
        Vector3Dd b = transformToLocal(body, inLine.end);

        double* minmax = brep->getMinMax();
        bool overlaps = segmentOverlapsMinMax(a, b, minmax);
        delete[] minmax;
        if ( !overlaps ) {
            continue;
        }
        for ( j = 0; j < brep->getPolygonsList().size(); j++ ) {
            InfinitePlane* plane =
                brep->getPolygonsList().get(j)->getContainingPlane();
            if ( plane == 0 ) {
                continue;
            }
            double da = plane->pointDistance(a);
            double db = plane->pointDistance(b);
            delete plane;
            if ( da * db >= 0.0 ) {
                // Both ends at the same side, or one of them on the plane
                // (then it is a breaking point of the line already)
                continue;
            }
            double t = da / (da - db);
            if ( !isUnitInterval(t) ) {
                continue;
            }
            Vector3Dd p = a.add(b.subtract(a).multiply(t));
            if ( brep->doContainmentTest(p, 10*VSDK::EPSILON) != Geometry::LIMIT ) {
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
void processFreeLineToBeDrawn(
    java::ArrayList<HiddenLineQuerySolid>& solids,
    AppelEdgeCache& inLine,
    const Camera* inCamera,
    Calligraphic2DBuffer* outVisibleLineSet,
    Calligraphic2DBuffer* outHiddenLineSet,
    java::ArrayList<AppelEdgeCache*>& contourCache)
{
    if ( inLine.direction.length() < VSDK::EPSILON ) {
        return;
    }

    java::ArrayList<AppelEdgeSegment> segments;
    segments.add(AppelEdgeSegment());
    addContourCrossings(inLine, inLine.start, inLine.end,
        inCamera->getPosition(), inCamera, contourCache, segments);
    addSolidPiercings(solids, inLine, segments);
    AppelEdgeSegment endSegment;
    endSegment.lineParameter = 1.0;
    segments.add(endSegment);
    sortAndMergeSegments(segments);

    for (long int i = 0; i + 1 < segments.size(); ++i) {
        double val1 = segments[i].lineParameter;
        double val2 = segments[i + 1].lineParameter;
        Vector3Dd pos1 = inLine.start.add(inLine.direction.multiply(val1));
        Vector3Dd pos2 = inLine.start.add(inLine.direction.multiply(val2));
        Vector3Dd posx = inLine.start.add(
            inLine.direction.multiply((val1 + val2) / 2));

        if ( computeMidpointQuantitativeInvisibility(solids, inCamera,
                 posx) == 0 ) {
            addProjectedLine(outVisibleLineSet, pos1, pos2, inCamera);
        }
        else {
            addProjectedLine(outHiddenLineSet, pos1, pos2, inCamera);
        }
    }
}

void processLineToBeDrawn(
    java::ArrayList<HiddenLineQuerySolid>& solids,
    AppelEdgeCache& inEdge,
    const Camera* inCamera,
    Calligraphic2DBuffer* outVisibleContourLineSet,
    Calligraphic2DBuffer* outVisibleNonContourLineSet,
    Calligraphic2DBuffer* outHiddenLineSet,
    java::ArrayList<AppelEdgeCache*>& contourCache,
    HiddenLineRenderer::AppelEdgeDump* edgeDump)
{
    //- 1. Compute the sweep plane triangle ---------------------------
    // Defines plane "SP1" on figure [APPE1967].5.
    Vector3Dd sp1a = inEdge.start;
    Vector3Dd sp1b = inEdge.end;
    Vector3Dd sp1c = inCamera->getPosition();

    //- 2. Break current edge into segments ---------------------------
    java::ArrayList<AppelEdgeSegment> segments;
    segments.add(AppelEdgeSegment());
    addContourCrossings(inEdge, sp1a, sp1b, sp1c, inCamera, contourCache,
        segments);
    AppelEdgeSegment endSegment;
    endSegment.lineParameter = 1.0;
    segments.add(endSegment);

    //- 3. Sort segment set -------------------------------------------
    sortAndMergeSegments(segments);

    for (size_t i = 0; i + 1 < segments.size(); ++i) {
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

        if (edgeDump != 0) {
            HiddenLineRenderer::AppelSegmentDump segmentDump;
            segmentDump.tStart = t1;
            segmentDump.tEnd = t2;
            segmentDump.start = Vector3Dd(pos1);
            segmentDump.end = Vector3Dd(pos2);
            segmentDump.midpoint = Vector3Dd(midpoint);
            segmentDump.midpointQuantitativeInvisibility = midpointQi;
            segmentDump.classification = midpointQi == 0 ? "visible" : "hidden";
            edgeDump->segments.add(segmentDump);
        }
    }
}

void processFreeLineSets(
    java::ArrayList<HiddenLineQuerySolid>& solids,
    const Camera* inCamera,
    java::ArrayList<AppelEdgeCache*>& contourCache,
    const java::ArrayList<Calligraphic3DBuffer*>& inFreeLineSets,
    java::ArrayList<Calligraphic2DBuffer*>& outVisibleFreeLineSets,
    java::ArrayList<Calligraphic2DBuffer*>& outHiddenFreeLineSets)
{
    long int i;
    int j;

    for ( i = 0; i < inFreeLineSets.size(); i++ ) {
        Calligraphic3DBuffer* lines = inFreeLineSets[i];
        for ( j = 0; j < lines->getNumLines(); j++ ) {
            Vector3Dd end0;
            Vector3Dd end1;
            lines->get3DLine(j, end0, end1);
            AppelEdgeCache freeLine;
            freeLine.setStart(end0);
            freeLine.setEnd(end1);
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

} // namespace

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

java::String HiddenLineRenderer::edgeTypeName(int edgeType)
{
    switch ( edgeType ) {
      case AppelEdgeCache::HIDDEN_LINE: return "hidden";
      case AppelEdgeCache::CONTOUR_LINE: return "contour";
      case AppelEdgeCache::VISIBLE_LINE: return "visible";
      default: return "unknown";
    }
}

/**
Given a viewing camera and a set of bodies, this method generates three
sets of lines for visible/hidden line rendering, as described in
paper [APPE1967] and section [FOLE1992].15.3.2. The resulting line sets
are projected to 2D calligraphic buffers and separated into visible
contour, visible non-contour and hidden segments.
*/
void HiddenLineRenderer::executeAppelAlgorithm(
    java::ArrayList<SimpleBody*>& inSimpleBodyArray,
    const Camera* inCamera,
    Calligraphic2DBuffer* outVisibleContourLineSet,
    Calligraphic2DBuffer* outVisibleNonContourLineSet,
    Calligraphic2DBuffer* outHiddenLineSet)
{
    // Rendering path: do not collect the per-edge diagnostic dump
    AppelAlgorithmDump unusedDump;
    runAppelAlgorithm(inSimpleBodyArray, inCamera, outVisibleContourLineSet,
        outVisibleNonContourLineSet, outHiddenLineSet, false, unusedDump,
        0, 0, 0);
}

/**
As `executeAppelAlgorithm`, also presenting sets of free lines: lines
that are not edges of the solids (i.e. curves, or a reference grid), given
in world coordinates, which are hidden by the solids but do not hide
anything. The visible and hidden parts of the lines of
`inFreeLineSets[i]` are reported in `outVisibleFreeLineSets[i]` and
`outHiddenFreeLineSets[i]`, so each set can be drawn with its own style.
*/
void HiddenLineRenderer::executeAppelAlgorithm(
    java::ArrayList<SimpleBody*>& inSimpleBodyArray,
    const Camera* inCamera,
    Calligraphic2DBuffer* outVisibleContourLineSet,
    Calligraphic2DBuffer* outVisibleNonContourLineSet,
    Calligraphic2DBuffer* outHiddenLineSet,
    const java::ArrayList<Calligraphic3DBuffer*>& inFreeLineSets,
    java::ArrayList<Calligraphic2DBuffer*>& outVisibleFreeLineSets,
    java::ArrayList<Calligraphic2DBuffer*>& outHiddenFreeLineSets)
{
    AppelAlgorithmDump unusedDump;
    runAppelAlgorithm(inSimpleBodyArray, inCamera, outVisibleContourLineSet,
        outVisibleNonContourLineSet, outHiddenLineSet, false, unusedDump,
        &inFreeLineSets, &outVisibleFreeLineSets, &outHiddenFreeLineSets);
}

void HiddenLineRenderer::executeAppelAlgorithmWithDiagnostics(
    java::ArrayList<SimpleBody*>& inSimpleBodyArray,
    const Camera* inCamera,
    Calligraphic2DBuffer* outVisibleContourLineSet,
    Calligraphic2DBuffer* outVisibleNonContourLineSet,
    Calligraphic2DBuffer* outHiddenLineSet,
    AppelAlgorithmDump& outDump)
{
    runAppelAlgorithm(inSimpleBodyArray, inCamera, outVisibleContourLineSet,
        outVisibleNonContourLineSet, outHiddenLineSet, true, outDump,
        0, 0, 0);
}

void HiddenLineRenderer::runAppelAlgorithm(
    java::ArrayList<SimpleBody*>& inSimpleBodyArray,
    const Camera* inCamera,
    Calligraphic2DBuffer* outVisibleContourLineSet,
    Calligraphic2DBuffer* outVisibleNonContourLineSet,
    Calligraphic2DBuffer* outHiddenLineSet,
    bool collectDiagnostics,
    AppelAlgorithmDump& outDump,
    const java::ArrayList<Calligraphic3DBuffer*>* inFreeLineSets,
    java::ArrayList<Calligraphic2DBuffer*>* outVisibleFreeLineSets,
    java::ArrayList<Calligraphic2DBuffer*>* outHiddenFreeLineSets)
{
    if (inCamera == 0 || outVisibleContourLineSet == 0 ||
        outVisibleNonContourLineSet == 0 || outHiddenLineSet == 0) {
        return;
    }

    java::ArrayList<HiddenLineQuerySolid> querySolids;
    java::ArrayList<AppelEdgeCache*> cache;
    java::ArrayList<AppelEdgeCache*> contourCache;

    for (long int i = 0; i < inSimpleBodyArray.size(); ++i) {
        SimpleBody* body = inSimpleBodyArray.get(i);
        if (body == 0 || body->getGeometry() == 0) {
            continue;
        }
        Volume* volume = dynamic_cast<Volume*>(body->getGeometry());
        if (volume == 0) {
            continue;
        }

        PolyhedralBoundedSolid* solid = volume->exportToPolyhedralBoundedSolid();
        if (solid == 0) {
            continue;
        }

        bool ownsSolid = static_cast<Geometry*>(solid) != body->getGeometry();
        buildCache(
            querySolids,
            body,
            solid,
            cache,
            contourCache,
            inCamera,
            ownsSolid);
    }

    for (long int i = 0; i < querySolids.size(); ++i) {
        if (querySolids[i].solid != 0) {
            querySolids[i].solid->beginVisibilityQueries();
        }
    }

    for (long int i = 0; i < cache.size(); ++i) {
        AppelEdgeCache& edge = *cache[i];
        AppelEdgeDump* edgeDump =
            collectDiagnostics ? createEdgeDump(edge) : 0;
        switch (edge.edgeType) {
          case AppelEdgeCache::HIDDEN_LINE:
          case AppelEdgeCache::CONTOUR_LINE:
          case AppelEdgeCache::VISIBLE_LINE:
            processLineToBeDrawn(querySolids, edge, inCamera,
                outVisibleContourLineSet, outVisibleNonContourLineSet,
                outHiddenLineSet, contourCache, edgeDump);
            break;
          default:
            break;
        }
        if (edgeDump != 0) {
            outDump.edges.add(*edgeDump);
            delete edgeDump;
        }
    }
    if ( inFreeLineSets != 0 ) {
        processFreeLineSets(querySolids, inCamera, contourCache,
            *inFreeLineSets, *outVisibleFreeLineSets, *outHiddenFreeLineSets);
    }

    for (long int i = 0; i < querySolids.size(); ++i) {
        if (querySolids[i].solid != 0) {
            querySolids[i].solid->endVisibilityQueries();
        }
        if (querySolids[i].ownsSolid) {
            delete querySolids[i].solid;
            querySolids[i].solid = 0;
        }
    }
    for (long int i = 0; i < cache.size(); ++i) {
        delete cache[i];
    }
}
