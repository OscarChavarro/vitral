#include "vsdk/toolkit/render/HiddenLineRenderer.h"

#include <algorithm>
#include <deque>
#include <vector>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector4Dd.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
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
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/media/Calligraphic2DBuffer.h"

namespace {

class AppelEdgeSegment {
public:
    double t;
    int deltaQI;

    AppelEdgeSegment() : t(0.0), deltaQI(0) {}

    bool samePosition(const AppelEdgeSegment& other) const
    {
        return std::abs(t - other.t) <= VSDK::EPSILON;
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
    Vector3Dd d;
    _PolyhedralBoundedSolidFace* visibleEdgeForContourLine;

    AppelEdgeCache()
        : edgeType(VISIBLE_LINE),
          onSequence(false),
          start(),
          end(),
          d(),
          visibleEdgeForContourLine(0)
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

    for ( int i = 0; i < 6; i++ ) {
        double d0 = evaluateClipPlane(CLIP_PLANES[i], outStart);
        double d1 = evaluateClipPlane(CLIP_PLANES[i], outEnd);

        if ( d0 < 0.0 && d1 < 0.0 ) {
            return false;
        }
        if ( d0 < 0.0 || d1 < 0.0 ) {
            double denominator = d0 - d1;
            if ( std::abs(denominator) < VSDK::EPSILON ) {
                return false;
            }
            double t = d0 / denominator;
            Vector4Dd intersection = interpolate(outStart, outEnd, t);
            if ( d0 < 0.0 ) {
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
    if ( lineSet == 0 || camera == 0 ) {
        return;
    }

    Matrix4x4d projection = camera->calculateProjectionMatrix();
    Vector4Dd clip0 = projection.multiply(Vector4Dd(point0));
    Vector4Dd clip1 = projection.multiply(Vector4Dd(point1));
    Vector4Dd clipped0(0.0, 0.0, 0.0, 1.0);
    Vector4Dd clipped1(0.0, 0.0, 0.0, 1.0);
    if ( !clipLineToClipVolume(clip0, clip1, clipped0, clipped1) ) {
        return;
    }

    Vector4Dd ndc0 = clipped0.dividedByW();
    Vector4Dd ndc1 = clipped1.dividedByW();
    lineSet->add2DLine(ndc0.x(), ndc0.y(), ndc1.x(), ndc1.y());
}

int computeQuantitativeInvisibility(
    java::ArrayList<SimpleBody*>& solids,
    const Camera* camera,
    const AppelEdgeCache& edge)
{
    int qi = 0;
    for ( long int i = 0; i < solids.size(); i++ ) {
        if ( solids[i] != 0 ) {
            qi += solids[i]->computeQuantitativeInvisibility(
                camera->getPosition(),
                edge.start.add(edge.d.multiply(10 * VSDK::EPSILON)));
        }
    }
    return qi;
}

void buildCache(
    java::ArrayList<SimpleBody*>& solids,
    SimpleBody* body,
    std::deque<AppelEdgeCache>& cache,
    std::vector<AppelEdgeCache*>& contourCache,
    const Camera* camera)
{
    if ( body == 0 || body->getGeometry() == 0 ) {
        return;
    }

    Volume* volume = dynamic_cast<Volume*>(body->getGeometry());
    if ( volume == 0 ) {
        return;
    }

    PolyhedralBoundedSolid* solid = volume->exportToPolyhedralBoundedSolid();
    if ( solid == 0 ) {
        return;
    }

    solids.add(body);

    Vector3Dd prevEnd;
    bool hasPrevEnd = false;
    java::ArrayList<_PolyhedralBoundedSolidEdge*>& edges = solid->getEdgesList();

    for ( long int i = 0; i < edges.size(); i++ ) {
        _PolyhedralBoundedSolidEdge* edge = edges.get(i);
        if ( edge == 0 || edge->leftHalf == 0 || edge->rightHalf == 0 ||
             edge->leftHalf->startingVertex == 0 ||
             edge->rightHalf->startingVertex == 0 ) {
            continue;
        }

        _PolyhedralBoundedSolidFace* face1 = edge->leftHalf->parentLoop != 0 ?
            edge->leftHalf->parentLoop->parentFace : 0;
        _PolyhedralBoundedSolidFace* face2 = edge->rightHalf->parentLoop != 0 ?
            edge->rightHalf->parentLoop->parentFace : 0;
        if ( face1 == 0 || face2 == 0 ) {
            continue;
        }

        Vector3Dd startPosition = edge->leftHalf->startingVertex->position;
        Vector3Dd endPosition = edge->rightHalf->startingVertex->position;
        bool f1 = HiddenLineRenderer::isFaceVisibleFromCamera(face1, camera) >= 0;
        bool f2 = HiddenLineRenderer::isFaceVisibleFromCamera(face2, camera) >= 0;

        cache.push_back(AppelEdgeCache());
        AppelEdgeCache& materialLine = cache.back();
        materialLine.setStart(startPosition);
        materialLine.setEnd(endPosition);
        materialLine.d = endPosition.subtract(startPosition);
        materialLine.onSequence = hasPrevEnd &&
            prevEnd.subtract(startPosition).length() < VSDK::EPSILON;

        if ( !f1 && !f2 ) {
            materialLine.edgeType = AppelEdgeCache::HIDDEN_LINE;
        }
        else if ( (f1 && !f2) || (!f1 && f2) ) {
            materialLine.edgeType = AppelEdgeCache::CONTOUR_LINE;
            materialLine.visibleEdgeForContourLine = f1 ? face1 : face2;
            contourCache.push_back(&materialLine);
        }
        else {
            materialLine.edgeType = AppelEdgeCache::VISIBLE_LINE;
        }

        prevEnd = Vector3Dd::copyOf(endPosition);
        hasPrevEnd = true;
    }
}

void processLineToBeDrawn(
    java::ArrayList<SimpleBody*>& solids,
    AppelEdgeCache& inEdge,
    const Camera* inCamera,
    Calligraphic2DBuffer* outVisibleContourLineSet,
    Calligraphic2DBuffer* outVisibleNonContourLineSet,
    Calligraphic2DBuffer* outHiddenLineSet,
    std::vector<AppelEdgeCache*>& contourCache)
{
    Vector3Dd sp1a = inEdge.start;
    Vector3Dd sp1b = inEdge.end;
    Vector3Dd sp1c = inCamera->getPosition();
    Vector3Dd sp2c = inCamera->getPosition();

    std::vector<AppelEdgeSegment> segments;
    segments.push_back(AppelEdgeSegment());

    for ( size_t i = 0; i < contourCache.size(); i++ ) {
        AppelEdgeCache* cl = contourCache[i];
        if ( cl == 0 || cl == &inEdge ) {
            continue;
        }

        Ray ray(
            cl->start.add(cl->d.multiply(3 * VSDK::EPSILON)),
            cl->d);
        double t0 = ray.direction().length() - 6 * VSDK::EPSILON;
        ray = ray.withDirection(ray.direction().normalized());
        Intersection* hit = Triangle::doIntersectionWithTriangle(
            ray, sp1a, sp1b, sp1c);
        if ( hit != 0 && hit->t < t0 ) {
            InfinitePlane plane(cl->start, cl->end, sp2c);
            Ray edgeRay(inEdge.start, inEdge.d.normalized());
            Ray* planeHit = plane.doIntersection(edgeRay);
            if ( planeHit != 0 ) {
                AppelEdgeSegment segment;
                segment.t = planeHit->t() / inEdge.d.length();

                Vector3Dd K = inEdge.start.add(
                    inEdge.d.multiply(segment.t - 2 * VSDK::EPSILON));
                Ray projectionRay(K, sp2c.subtract(K).normalized());
                InfinitePlane* contourPlane =
                    cl->visibleEdgeForContourLine != 0 ?
                    cl->visibleEdgeForContourLine->getContainingPlane() : 0;
                if ( contourPlane != 0 ) {
                    Ray* contourHit = contourPlane->doIntersection(projectionRay);
                    if ( contourHit != 0 ) {
                        Vector3Dd J = contourHit->origin().add(
                            contourHit->direction().multiply(contourHit->t()));
                        int pos = cl->visibleEdgeForContourLine->testPointInside(
                            J, VSDK::EPSILON);
                        segment.deltaQI =
                            (pos == Geometry::INSIDE || pos == Geometry::LIMIT) ?
                            1 : -1;
                        segments.push_back(segment);
                        delete contourHit;
                    }
                    delete contourPlane;
                }
                delete planeHit;
            }
        }
        delete hit;
    }

    AppelEdgeSegment endSegment;
    endSegment.t = 1.0;
    segments.push_back(endSegment);

    std::sort(segments.begin(), segments.end(),
        [](const AppelEdgeSegment& a, const AppelEdgeSegment& b) {
            if ( a.t < b.t - VSDK::EPSILON ) return true;
            if ( a.t > b.t + VSDK::EPSILON ) return false;
            return false;
        });

    for ( size_t i = 0; i + 1 < segments.size(); ) {
        if ( segments[i].samePosition(segments[i + 1]) ) {
            segments.erase(segments.begin() + static_cast<long>(i));
        }
        else {
            i++;
        }
    }

    int qi = computeQuantitativeInvisibility(solids, inCamera, inEdge);
    for ( size_t i = 0; i + 1 < segments.size(); i++ ) {
        AppelEdgeSegment& segment1 = segments[i];
        Vector3Dd pos1 = inEdge.start.add(inEdge.d.multiply(segment1.t));
        qi += segment1.deltaQI;

        AppelEdgeSegment& segment2 = segments[i + 1];
        Vector3Dd pos2 = inEdge.start.add(inEdge.d.multiply(segment2.t));

        Vector3Dd posx = inEdge.start.add(
            inEdge.d.multiply((segment1.t + segment2.t) / 2.0));
        qi = 0;
        for ( long int solidIndex = 0; solidIndex < solids.size(); solidIndex++ ) {
            if ( solids[solidIndex] != 0 ) {
                qi += solids[solidIndex]->computeQuantitativeInvisibility(
                    inCamera->getPosition(), posx);
            }
        }

        if ( qi == 0 ) {
            if ( inEdge.edgeType == AppelEdgeCache::CONTOUR_LINE ) {
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

} // namespace

int HiddenLineRenderer::isFaceVisibleFromCamera(
    _PolyhedralBoundedSolidFace* face,
    const Camera* camera)
{
    if ( face == 0 || camera == 0 ) {
        return 0;
    }

    Vector3Dd iv(1, 0, 0);
    Vector3Dd viewingVector = camera->getRotation().multiply(iv);
    InfinitePlane* plane = face->getContainingPlane();
    if ( plane == 0 ) {
        return 0;
    }
    Vector3Dd n = plane->getNormal().normalized();
    delete plane;

    if ( camera->getProjectionMode() == Camera::PROJECTION_MODE_ORTHOGONAL ) {
        viewingVector = viewingVector.normalized();
        double dot = n.dotProduct(viewingVector);
        if ( dot > VSDK::EPSILON ) {
            return -1;
        }
        else if ( dot > VSDK::EPSILON ) {
            return 1;
        }
        return 0;
    }

    Vector3Dd cameraPosition = camera->getPosition();
    for ( long int i = 0; i < face->boundariesList.size(); i++ ) {
        _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(i);
        if ( loop == 0 || loop->boundaryStartHalfEdge == 0 ) {
            continue;
        }
        _PolyhedralBoundedSolidHalfEdge* he = loop->boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge* heStart = he;
        do {
            he = he->next();
            if ( he == 0 || he->startingVertex == 0 ) {
                break;
            }
            Vector3Dd p = he->startingVertex->position;
            Vector3Dd t = p.subtract(cameraPosition).multiply(-1).normalized();
            if ( t.dotProduct(n) > 0.0 ) {
                return 1;
            }
        } while ( he != heStart );
    }
    return -1;
}

void HiddenLineRenderer::executeAppelAlgorithm(
    java::ArrayList<SimpleBody*>& inSimpleBodyArray,
    const Camera* inCamera,
    Calligraphic2DBuffer* outVisibleContourLineSet,
    Calligraphic2DBuffer* outVisibleNonContourLineSet,
    Calligraphic2DBuffer* outHiddenLineSet)
{
    if ( inCamera == 0 || outVisibleContourLineSet == 0 ||
         outVisibleNonContourLineSet == 0 || outHiddenLineSet == 0 ) {
        return;
    }

    std::deque<AppelEdgeCache> cache;
    std::vector<AppelEdgeCache*> contourCache;
    java::ArrayList<SimpleBody*> solids;

    for ( long int i = 0; i < inSimpleBodyArray.size(); i++ ) {
        buildCache(solids, inSimpleBodyArray.get(i), cache, contourCache,
            inCamera);
    }

    for ( size_t i = 0; i < cache.size(); i++ ) {
        AppelEdgeCache& edge = cache[i];
        switch ( edge.edgeType ) {
          case AppelEdgeCache::HIDDEN_LINE:
            addProjectedLine(outHiddenLineSet, edge.start, edge.end, inCamera);
            break;
          case AppelEdgeCache::CONTOUR_LINE:
          case AppelEdgeCache::VISIBLE_LINE:
            processLineToBeDrawn(solids, edge, inCamera,
                outVisibleContourLineSet, outVisibleNonContourLineSet,
                outHiddenLineSet, contourCache);
            break;
          default:
            break;
        }
    }
}
