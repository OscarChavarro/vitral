#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_SURFACE_POLYGON_POLYGON2D_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_SURFACE_POLYGON_POLYGON2D_H__

#include "vsdk/toolkit/environment/geometry/surface/Surface.h"
#include <vector>

class _Polygon2DContour;
class Vertex2D;
class Ray;
class RayHit;
template <class T> class BinaryTreeNode;

class Polygon2D : public Surface
{
public:
    std::vector<_Polygon2DContour*> loops;

private:
    _Polygon2DContour* currentLoop;
    BinaryTreeNode<_Polygon2DContour*>* headNode;

public:
    Polygon2D();
    virtual ~Polygon2D();

    void addVertex(double x, double y, double r, double g, double b);
    void addVertex(double x, double y);
    void pushVertex(double x, double y);

    void nextLoop();
    void eraseLastLoop();
    void invert();

    virtual double* getMinMax();

    Ray* doIntersection(const Ray& inOut_ray);
    virtual bool doIntersection(const Ray& inRay, RayHit* outHit);
    virtual void doExtraInformation(const Ray& inRay, double intT, RayHit* outData);

    BinaryTreeNode<_Polygon2DContour*>* getHeadNode();
    void setHeadNode(BinaryTreeNode<_Polygon2DContour*>* headNode);
};

#endif
