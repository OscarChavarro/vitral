#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_VOLUME_POLYHEDRALBOUNDEDSOLID_POLYHEDRALBOUNDEDSOLID_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_VOLUME_POLYHEDRALBOUNDEDSOLID_POLYHEDRALBOUNDEDSOLID_H__

#include "vsdk/toolkit/environment/geometry/volume/Solid.h"

#include "java/util/ArrayList.h"

class Ray;
class RayHit;
class Vector3Dd;
class _PolyhedralBoundedSolidFace;
class _PolyhedralBoundedSolidEdge;
class _PolyhedralBoundedSolidVertex;

class PolyhedralBoundedSolid : public Solid {
public:
    static const int PLUS = 1;
    static const int MINUS = 0;

private:
    java::ArrayList<_PolyhedralBoundedSolidFace*> polygonsList;
    java::ArrayList<_PolyhedralBoundedSolidEdge*> edgesList;
    java::ArrayList<_PolyhedralBoundedSolidVertex*> verticesList;
    int maxVertexId;
    int maxFaceId;
    bool modelIsValid;

public:
    PolyhedralBoundedSolid();
    virtual ~PolyhedralBoundedSolid() override;

    _PolyhedralBoundedSolidFace* findFace(int id);
    _PolyhedralBoundedSolidVertex* findVertex(int id);

    java::ArrayList<_PolyhedralBoundedSolidFace*>& getPolygonsList();
    void setPolygonsList(java::ArrayList<_PolyhedralBoundedSolidFace*>& polygonsList);
    java::ArrayList<_PolyhedralBoundedSolidEdge*>& getEdgesList();
    void setEdgesList(java::ArrayList<_PolyhedralBoundedSolidEdge*>& edgesList);
    java::ArrayList<_PolyhedralBoundedSolidVertex*>& getVerticesList();
    void setVerticesList(java::ArrayList<_PolyhedralBoundedSolidVertex*>& verticesList);

    int getMaxVertexId() const;
    void setMaxVertexId(int maxVertexId);
    int getMaxFaceId() const;
    void setMaxFaceId(int maxFaceId);

    Ray* doIntersection(const Ray& inOutRay);
    virtual bool doIntersection(const Ray& inRay, RayHit* outHit) override;
    virtual void doExtraInformation(const Ray& inRay, double inT, RayHit* outData) override;
    virtual double* getMinMax() override;

    bool isValid() const;
    void setValidationState(bool flag);

    void merge(PolyhedralBoundedSolid* other);
    virtual int computeQuantitativeInvisibility(const Vector3Dd& origin, const Vector3Dd& p) override;
    static int compareValue(double a, double b, double tolerance);
    void revert();

    virtual PolyhedralBoundedSolid* exportToPolyhedralBoundedSolid() override;
};

#endif
