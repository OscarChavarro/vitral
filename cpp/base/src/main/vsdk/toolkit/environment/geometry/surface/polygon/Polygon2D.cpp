#include "Polygon2D.h"
#include "_Polygon2DContour.h"
#include "vsdk/toolkit/environment/geometry/elements/Vertex2D.h"
#include "vsdk/toolkit/environment/geometry/elements/Ray.h"
#include "vsdk/toolkit/environment/geometry/elements/RayHit.h"
#include "vsdk/toolkit/common/dataStructures/BinaryTreeNode.h"
#include "vsdk/toolkit/common/VSDK.h"
#include <algorithm>

Polygon2D::Polygon2D() : currentLoop(nullptr), headNode(nullptr) { nextLoop(); }

Polygon2D::~Polygon2D()
{
    for (size_t i = 0; i < loops.size(); i++) delete loops[i];
}

void Polygon2D::addVertex(double x, double y, double r, double g, double b) { currentLoop->addVertex(x,y,r,g,b); }
void Polygon2D::addVertex(double x, double y) { currentLoop->addVertex(x,y); }
void Polygon2D::pushVertex(double x, double y) { currentLoop->pushVertex(x,y); }

void Polygon2D::nextLoop()
{
    currentLoop = new _Polygon2DContour();
    loops.push_back(currentLoop);
}

void Polygon2D::eraseLastLoop()
{
    if (loops.size() <= 1) return;
    _Polygon2DContour* last = loops.back();
    for (size_t i = 0; i < loops.size(); ) {
        if (loops[i]->getExteriorContour() == last) {
            delete loops[i];
            loops.erase(loops.begin() + i);
        }
        else i++;
    }
    if (loops.size() > 1) {
        delete loops.back();
        loops.pop_back();
        currentLoop = loops.back();
    }
}

void Polygon2D::invert()
{
    for (size_t i = 0; i < loops.size(); i++) {
        std::reverse(loops[i]->vertices.begin(), loops[i]->vertices.end());
    }
}

double* Polygon2D::getMinMax()
{
    double* minMax = new double[6];
    double minX = 1e308, minY = 1e308;
    double maxX = -1e308, maxY = -1e308;

    for (size_t i = 0; i < loops.size(); i++) {
        for (size_t j = 0; j + 1 < loops[i]->vertices.size(); j++) {
            const Vertex2D& v = loops[i]->vertices[j];
            if (v.x > maxX) maxX = v.x;
            if (v.x < minX) minX = v.x;
            if (v.y > maxY) maxY = v.y;
            if (v.y < minY) minY = v.y;
        }
    }

    minMax[0]=minX; minMax[1]=minY; minMax[2]=0;
    minMax[3]=maxX; minMax[4]=maxY; minMax[5]=0;
    return minMax;
}

static bool pointInLoop(const _Polygon2DContour* loop, double x, double y)
{
    bool inside = false;
    const std::vector<Vertex2D>& v = loop->vertices;
    size_t n = v.size();
    if (n < 3) return false;
    for (size_t i = 0, j = n - 1; i < n; j = i++) {
        bool inter = ((v[i].y > y) != (v[j].y > y)) &&
                     (x < (v[j].x - v[i].x) * (y - v[i].y) / ((v[j].y - v[i].y) + 1e-30) + v[i].x);
        if (inter) inside = !inside;
    }
    return inside;
}

Ray* Polygon2D::doIntersection(const Ray& inOut_ray)
{
    RayHit hit;
    if (doIntersection(inOut_ray, &hit) && hit.ray() != nullptr) {
        return new Ray(*hit.ray());
    }
    return nullptr;
}

bool Polygon2D::doIntersection(const Ray& inRay, RayHit* outHit)
{
    if (std::abs(inRay.direction().z()) < VSDK::EPSILON) return false;
    double t = -inRay.origin().z() / inRay.direction().z();
    if (t < 0) return false;

    Vector3Dd p = inRay.origin().add(inRay.direction().multiply(t));
    bool inside = false;
    for (size_t i = 0; i < loops.size(); i++) {
        if (pointInLoop(loops[i], p.x(), p.y())) {
            if (loops[i]->getExteriorContour() == nullptr) inside = true;
            else inside = false;
        }
    }
    if (!inside) return false;

    if (outHit != nullptr) {
        if (outHit->shouldStoreRay() || outHit->needsAnySurfaceData()) outHit->setRay(inRay.withT(t));
        else outHit->setHitDistance(t);
        if (outHit->needsPoint()) outHit->p = p;
        if (outHit->needsNormal()) outHit->n = Vector3Dd(0,0,1);
    }
    return true;
}

void Polygon2D::doExtraInformation(const Ray& inRay, double intT, RayHit* outData)
{
    if (outData == nullptr) return;
    outData->p = inRay.origin().add(inRay.direction().multiply(intT));
    outData->n = Vector3Dd(0,0,1);
}

BinaryTreeNode<_Polygon2DContour*>* Polygon2D::getHeadNode() { return headNode; }
void Polygon2D::setHeadNode(BinaryTreeNode<_Polygon2DContour*>* node) { headNode = node; }
