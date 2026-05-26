#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"

#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "vsdk/toolkit/environment/geometry/elements/Ray.h"
#include "vsdk/toolkit/environment/geometry/elements/RayHit.h"

#include "java/util/ArrayList.txx"

#include <cfloat>
#include <cmath>

PolyhedralBoundedSolid::PolyhedralBoundedSolid()
    : maxVertexId(-1), maxFaceId(-1), modelIsValid(false)
{
}

PolyhedralBoundedSolid::~PolyhedralBoundedSolid()
{
    for (long int i = 0; i < polygonsList.size(); ++i) delete polygonsList[i];
    for (long int i = 0; i < edgesList.size(); ++i) delete edgesList[i];
    for (long int i = 0; i < verticesList.size(); ++i) delete verticesList[i];
}

_PolyhedralBoundedSolidFace* PolyhedralBoundedSolid::findFace(int id) { for (long int i=0;i<polygonsList.size();++i) if (polygonsList[i]->id==id) return polygonsList[i]; return 0; }
_PolyhedralBoundedSolidVertex* PolyhedralBoundedSolid::findVertex(int id) { for (long int i=0;i<verticesList.size();++i) if (verticesList[i]->id==id) return verticesList[i]; return 0; }

java::ArrayList<_PolyhedralBoundedSolidFace*>& PolyhedralBoundedSolid::getPolygonsList() { return polygonsList; }
void PolyhedralBoundedSolid::setPolygonsList(java::ArrayList<_PolyhedralBoundedSolidFace*>& v) {
    polygonsList.clear();
    for (long int i = 0; i < v.size(); i++) polygonsList.add(v.get(i));
}
java::ArrayList<_PolyhedralBoundedSolidEdge*>& PolyhedralBoundedSolid::getEdgesList() { return edgesList; }
void PolyhedralBoundedSolid::setEdgesList(java::ArrayList<_PolyhedralBoundedSolidEdge*>& v) {
    edgesList.clear();
    for (long int i = 0; i < v.size(); i++) edgesList.add(v.get(i));
}
java::ArrayList<_PolyhedralBoundedSolidVertex*>& PolyhedralBoundedSolid::getVerticesList() { return verticesList; }
void PolyhedralBoundedSolid::setVerticesList(java::ArrayList<_PolyhedralBoundedSolidVertex*>& v) {
    verticesList.clear();
    for (long int i = 0; i < v.size(); i++) verticesList.add(v.get(i));
}

int PolyhedralBoundedSolid::getMaxVertexId() const { return maxVertexId; }
void PolyhedralBoundedSolid::setMaxVertexId(int v) { maxVertexId = v; }
int PolyhedralBoundedSolid::getMaxFaceId() const { return maxFaceId; }
void PolyhedralBoundedSolid::setMaxFaceId(int v) { maxFaceId = v; }

Ray* PolyhedralBoundedSolid::doIntersection(const Ray& inOutRay)
{
    RayHit hit(RayHit::DETAIL_NONE, true);
    if ( doIntersection(inOutRay, &hit) && hit.ray() != 0 ) return new Ray(*hit.ray());
    return 0;
}

bool PolyhedralBoundedSolid::doIntersection(const Ray&, RayHit*) { return false; }
void PolyhedralBoundedSolid::doExtraInformation(const Ray&, double, RayHit*) {}

double* PolyhedralBoundedSolid::getMinMax()
{
    double* minMax = new double[6];
    double minX = DBL_MAX, minY = DBL_MAX, minZ = DBL_MAX;
    double maxX = -DBL_MAX, maxY = -DBL_MAX, maxZ = -DBL_MAX;
    for (long int i = 0; i < verticesList.size(); ++i) {
        const Vector3Dd& p = verticesList[i]->position;
        if ( p.x() < minX ) minX = p.x(); if ( p.y() < minY ) minY = p.y(); if ( p.z() < minZ ) minZ = p.z();
        if ( p.x() > maxX ) maxX = p.x(); if ( p.y() > maxY ) maxY = p.y(); if ( p.z() > maxZ ) maxZ = p.z();
    }
    if ( verticesList.size() == 0 ) { minX=minY=minZ=maxX=maxY=maxZ=0; }
    minMax[0]=minX; minMax[1]=minY; minMax[2]=minZ; minMax[3]=maxX; minMax[4]=maxY; minMax[5]=maxZ;
    return minMax;
}

bool PolyhedralBoundedSolid::isValid() const { return modelIsValid; }
void PolyhedralBoundedSolid::setValidationState(bool flag) { modelIsValid = flag; }

void PolyhedralBoundedSolid::merge(PolyhedralBoundedSolid* other)
{
    if ( other == 0 ) return;
    int offsetFacesId = getMaxFaceId();
    int offsetVertexId = getMaxVertexId();

    while ( other->getPolygonsList().size() != 0 ) {
        _PolyhedralBoundedSolidFace* f = other->getPolygonsList()[0];
        f->id += offsetFacesId;
        if ( f->id > maxFaceId ) maxFaceId = f->id;
        polygonsList.add(f);
        other->getPolygonsList().remove(0L);
    }
    while ( other->getEdgesList().size() != 0 ) {
        edgesList.add(other->getEdgesList()[0]);
        other->getEdgesList().remove(0L);
    }
    while ( other->getVerticesList().size() != 0 ) {
        _PolyhedralBoundedSolidVertex* v = other->getVerticesList()[0];
        v->id += offsetVertexId;
        if ( v->id > maxVertexId ) maxVertexId = v->id;
        verticesList.add(v);
        other->getVerticesList().remove(0L);
    }
}

int PolyhedralBoundedSolid::computeQuantitativeInvisibility(const Vector3Dd&, const Vector3Dd&) { return 0; }
int PolyhedralBoundedSolid::compareValue(double a, double b, double tolerance)
{
    double delta = std::abs(a - b);
    if ( delta < tolerance ) return 0;
    return (a > b) ? 1 : -1;
}

void PolyhedralBoundedSolid::revert()
{
    for (long int i = 0; i < polygonsList.size(); ++i) {
        polygonsList[i]->revert();
    }
}

PolyhedralBoundedSolid* PolyhedralBoundedSolid::exportToPolyhedralBoundedSolid() { return this; }
