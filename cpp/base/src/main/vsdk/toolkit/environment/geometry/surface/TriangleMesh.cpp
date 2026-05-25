#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMeshGroup.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/elements/Vertex.h"
#include "vsdk/toolkit/environment/geometry/elements/Triangle.h"
#include "vsdk/toolkit/environment/geometry/elements/Ray.h"
#include "vsdk/toolkit/environment/geometry/elements/RayHit.h"
#include "vsdk/toolkit/environment/geometry/volume/VoxelVolume.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/media/Image.h"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include <cmath>
#include <algorithm>

/*
This class represents a "basic" triangle mesh. Its model is based in a set
of vertexes and triangles (the edges are not store explicitly, and there
can not be an edge not forming part of a triangle).

This basic triangle mesh model can be associated with one and only one
material (this could change in future), but can have multiple textures,
and each texture can be mapped to a different set of triangles.

As every model class or `Entity` in VSDK, this class only can represent
(store in memory) the mesh model. It doesn't provide persistence or rendering
functionality, as this could be found at `io` and `render` packages.
Nevertheles, this class will be highly coupled with both of those, so
making any change here will impact highly that code.

This class does not ensure nor impose data integrity, and this will be the
sole responsability of the cooperating utilities and applications.
*/

TriangleMesh::TriangleMesh() : name("default"), intersectionTriangleIndex(-1) {}
TriangleMesh::TriangleMesh(const TriangleMesh& o) = default;
TriangleMesh* TriangleMesh::clone() const { return new TriangleMesh(*this); }

std::string TriangleMesh::getName() const { return name; }
void TriangleMesh::setName(const std::string& n) { name = n; }

void TriangleMesh::getVertexAt(int i, Vertex& vertex) const
{
    vertex.position = Vector3Dd(vertexPositions[i*3], vertexPositions[i*3+1], vertexPositions[i*3+2]);
    if ((int)vertexNormals.size() >= (i+1)*3) vertex.normal = Vector3Dd(vertexNormals[i*3], vertexNormals[i*3+1], vertexNormals[i*3+2]);
    if ((int)vertexBinormals.size() >= (i+1)*3) vertex.binormal = Vector3Dd(vertexBinormals[i*3], vertexBinormals[i*3+1], vertexBinormals[i*3+2]);
    if ((int)vertexTangents.size() >= (i+1)*3) vertex.tangent = Vector3Dd(vertexTangents[i*3], vertexTangents[i*3+1], vertexTangents[i*3+2]);
    if ((int)vertexUvs.size() >= (i+1)*2) { vertex.u = vertexUvs[i*2]; vertex.v = vertexUvs[i*2+1]; }
}

void TriangleMesh::initVertexPositionsArray(int n) { vertexPositions.assign(n*3, 0.0); vertexSelections.assign(n, false); }
void TriangleMesh::initVertexNormalsArray() { vertexNormals.assign(getNumVertices()*3, 0.0); }
void TriangleMesh::initVertexBinormalsArray() { vertexBinormals.assign(getNumVertices()*3, 0.0); }
void TriangleMesh::initVertexTangentsArray() { vertexTangents.assign(getNumVertices()*3, 0.0); }
void TriangleMesh::initVertexColorsArray() { vertexColors.assign(getNumVertices()*3, 0.0); }
void TriangleMesh::initVertexUvsArray() { vertexUvs.assign(getNumVertices()*2, 0.0); }
void TriangleMesh::initIncidentTrianglesPerVertexArray() { incidentTrianglesPerVertexArray.assign(getNumVertices(), std::vector<int>()); }

void TriangleMesh::detachColors() { vertexColors.clear(); }
void TriangleMesh::detachNormals() { vertexNormals.clear(); }
void TriangleMesh::detachUvs() { vertexUvs.clear(); }

/*
This method provides a clear structured form of defining the mesh vertexes,
but it is inefficient. Its use is discouraged for applications manipulating
big meshes.
@param vertexes
@param withNormals
@param withBinormals
@param withTangents
@param withUvs
*/
void TriangleMesh::setVertexes(const std::vector<Vertex>& vertexes, bool withNormals, bool withBinormals, bool withTangents, bool withUvs)
{
    initVertexPositionsArray((int)vertexes.size());
    if (withNormals) initVertexNormalsArray();
    if (withBinormals) initVertexBinormalsArray();
    if (withTangents) initVertexTangentsArray();
    if (withUvs) initVertexUvsArray();
    for (int i = 0; i < (int)vertexes.size(); i++) setVertexAt(i, vertexes[i]);
}

void TriangleMesh::initTriangleArrays(int n)
{
    triangleIndices.assign(n*3, 0);
    triangleNormals.assign(n*3, 0.0);
}

/*
This method provides a clear structured form of defining the mesh triangles,
but it is inefficient. Its use is discouraged for applications manipulating
big meshes.
@param triangles
*/
void TriangleMesh::setTriangles(const std::vector<Triangle>& triangles)
{
    initTriangleArrays((int)triangles.size());
    for (int i = 0; i < (int)triangles.size(); i++) setTriangleAt(i, triangles[i]);
}

void TriangleMesh::setTextures(const std::vector<Image*>& t) { textures = t; }
void TriangleMesh::setMaterials(const std::vector<SimpleMaterial*>& m) { materials = m; }

/*
Given a vertex structure and an `i` position, this method copies
the information from the structure in to the i-th vertex arrays position.
PRE: 0 <= i < vertexPositions.length/3
@param i
@param vertex
*/
void TriangleMesh::setVertexAt(int i, const Vertex& v)
{
    vertexPositions[i*3] = v.position.x();
    vertexPositions[i*3+1] = v.position.y();
    vertexPositions[i*3+2] = v.position.z();
    if ((int)vertexNormals.size() >= (i+1)*3) {
        vertexNormals[i*3] = v.normal.x(); vertexNormals[i*3+1] = v.normal.y(); vertexNormals[i*3+2] = v.normal.z();
    }
    if ((int)vertexBinormals.size() >= (i+1)*3) {
        vertexBinormals[i*3] = v.binormal.x(); vertexBinormals[i*3+1] = v.binormal.y(); vertexBinormals[i*3+2] = v.binormal.z();
    }
    if ((int)vertexTangents.size() >= (i+1)*3) {
        vertexTangents[i*3] = v.tangent.x(); vertexTangents[i*3+1] = v.tangent.y(); vertexTangents[i*3+2] = v.tangent.z();
    }
    if ((int)vertexUvs.size() >= (i+1)*2) {
        vertexUvs[i*2] = v.u; vertexUvs[i*2+1] = v.v;
    }
}

/*
Given a triangle structure and an `i` position, this method copies
the information from the structure in to the i-th triangle arrays position.
PRE: 0 <= i < vertexPositions.length/3
@param i
@param triangle
*/
void TriangleMesh::setTriangleAt(int i, const Triangle& t)
{
    triangleIndices[i*3] = t.p0;
    triangleIndices[i*3+1] = t.p1;
    triangleIndices[i*3+2] = t.p2;
    if ((int)triangleNormals.size() >= (i+1)*3) {
        triangleNormals[i*3] = t.normal.x();
        triangleNormals[i*3+1] = t.normal.y();
        triangleNormals[i*3+2] = t.normal.z();
    }
}

int TriangleMesh::getNumVertices() const { return (int)vertexPositions.size()/3; }
int TriangleMesh::getNumTriangles() const { return (int)triangleIndices.size()/3; }

std::vector<bool>& TriangleMesh::getVertexSelections()
{
    if ((int)vertexSelections.size() != getNumVertices()) vertexSelections.assign(getNumVertices(), false);
    return vertexSelections;
}
std::vector<double>& TriangleMesh::getVertexPositions() { return vertexPositions; }
std::vector<double>& TriangleMesh::getVertexNormals() { return vertexNormals; }
std::vector<double>& TriangleMesh::getVertexBinormals() { return vertexBinormals; }
std::vector<double>& TriangleMesh::getVertexTangents() { return vertexTangents; }
std::vector<double>& TriangleMesh::getVertexColors() { return vertexColors; }
std::vector<double>& TriangleMesh::getVertexUvs() { return vertexUvs; }
std::vector<int>& TriangleMesh::getTriangleIndexes() { return triangleIndices; }
std::vector<double>& TriangleMesh::getTriangleNormals() { return triangleNormals; }

std::vector<SimpleMaterial*>& TriangleMesh::getMaterials() { return materials; }
std::vector<Image*>& TriangleMesh::getTextures() { return textures; }
Image* TriangleMesh::getTextureAt(int index) const { return (index>=0 && index<(int)textures.size()) ? textures[index] : nullptr; }
void TriangleMesh::setTextureAt(int index, Image* image) { if (index>=0 && index<(int)textures.size()) textures[index] = image; }

std::vector< std::vector<int> >& TriangleMesh::getTextureRanges() { return textureRanges; }
/*
Note this always returns an array with two (2) integers: the first one
is an index to `triangles` array, the second one is an index to the
`textures` array.
@param spanRange
@return an integer array for textures ranges indexes
*/
std::vector<int> TriangleMesh::getTextureRangeAt(int spanRange) const { return textureRanges[spanRange]; }
void TriangleMesh::setTextureRanges(const std::vector< std::vector<int> >& r) { textureRanges = r; }

std::vector< std::vector<int> >& TriangleMesh::getMaterialRanges() { return materialRanges; }
/*
Note this always returns an array with two (2) integers: the first one
is an index to `triangles` array, the second one is an index to the
`materials` array.
@param spanRange
@return a integer array with material ranges indexes
*/
std::vector<int> TriangleMesh::getMaterialRangeAt(int spanRange) const { return materialRanges[spanRange]; }
void TriangleMesh::setMaterialRanges(const std::vector< std::vector<int> >& r) { materialRanges = r; }

void TriangleMesh::calculateNormals()
{
    initVertexNormalsArray();
    triangleNormals.assign(getNumTriangles()*3, 0.0);

    for (int i = 0; i < getNumTriangles(); i++) {
        int i0 = triangleIndices[i*3], i1 = triangleIndices[i*3+1], i2 = triangleIndices[i*3+2];
        Vector3Dd v0(vertexPositions[i0*3], vertexPositions[i0*3+1], vertexPositions[i0*3+2]);
        Vector3Dd v1(vertexPositions[i1*3], vertexPositions[i1*3+1], vertexPositions[i1*3+2]);
        Vector3Dd v2(vertexPositions[i2*3], vertexPositions[i2*3+1], vertexPositions[i2*3+2]);
        Vector3Dd n = v1.subtract(v0).crossProduct(v2.subtract(v0)).normalized();

        triangleNormals[i*3] = n.x(); triangleNormals[i*3+1] = n.y(); triangleNormals[i*3+2] = n.z();

        vertexNormals[i0*3] += n.x(); vertexNormals[i0*3+1] += n.y(); vertexNormals[i0*3+2] += n.z();
        vertexNormals[i1*3] += n.x(); vertexNormals[i1*3+1] += n.y(); vertexNormals[i1*3+2] += n.z();
        vertexNormals[i2*3] += n.x(); vertexNormals[i2*3+1] += n.y(); vertexNormals[i2*3+2] += n.z();
    }

    for (int i = 0; i < getNumVertices(); i++) {
        Vector3Dd n(vertexNormals[i*3], vertexNormals[i*3+1], vertexNormals[i*3+2]);
        n = n.normalized();
        vertexNormals[i*3] = n.x(); vertexNormals[i*3+1] = n.y(); vertexNormals[i*3+2] = n.z();
    }
}

void TriangleMesh::reorientateNormals()
{
    for (size_t i = 0; i < vertexNormals.size(); i++) vertexNormals[i] = -vertexNormals[i];
    for (int i = 0; i < getNumTriangles(); i++) std::swap(triangleIndices[i*3+1], triangleIndices[i*3+2]);
}

double* TriangleMesh::calculateMinMaxPositions()
{
    double* m = new double[6];
    double minX=1e308,minY=1e308,minZ=1e308,maxX=-1e308,maxY=-1e308,maxZ=-1e308;
    for (int i = 0; i < getNumVertices(); i++) {
        double x=vertexPositions[i*3], y=vertexPositions[i*3+1], z=vertexPositions[i*3+2];
        minX=std::min(minX,x); minY=std::min(minY,y); minZ=std::min(minZ,z);
        maxX=std::max(maxX,x); maxY=std::max(maxY,y); maxZ=std::max(maxZ,z);
    }
    m[0]=minX; m[1]=minY; m[2]=minZ; m[3]=maxX; m[4]=maxY; m[5]=maxZ;
    return m;
}

int TriangleMesh::doIntersectionInformation() const { return intersectionTriangleIndex; }

bool TriangleMesh::intersectTriangle(const Ray& ray, const Vector3Dd& v0, const Vector3Dd& v1, const Vector3Dd& v2, double& t, double& u, double& v)
{
    Vector3Dd e1 = v1.subtract(v0);
    Vector3Dd e2 = v2.subtract(v0);
    Vector3Dd h = ray.direction().crossProduct(e2);
    double a = e1.dotProduct(h);
    if (std::abs(a) < VSDK::EPSILON) return false;
    double f = 1.0 / a;
    Vector3Dd s = ray.origin().subtract(v0);
    u = f * s.dotProduct(h);
    if (u < 0.0 || u > 1.0) return false;
    Vector3Dd q = s.crossProduct(e1);
    v = f * ray.direction().dotProduct(q);
    if (v < 0.0 || u + v > 1.0) return false;
    t = f * e2.dotProduct(q);
    return t > VSDK::EPSILON;
}

Ray* TriangleMesh::doIntersection(const Ray& inOut_Ray)
{
    RayHit hit;
    if (doIntersection(inOut_Ray, &hit) && hit.ray() != 0) return new Ray(*hit.ray());
    return 0;
}

void TriangleMesh::interpolateTriangleData(int tri, double u, double v, RayHit* outHit, const Vector3Dd& rayDirection)
{
    if (outHit == 0) return;
    int i0 = triangleIndices[tri*3], i1 = triangleIndices[tri*3+1], i2 = triangleIndices[tri*3+2];
    double w = 1.0 - u - v;

    if (outHit->needsNormal()) {
        if ((int)vertexNormals.size() >= getNumVertices()*3 && (int)vertexUvs.size() >= getNumVertices()*2) {
            Vector3Dd n(
                w*vertexNormals[i0*3] + u*vertexNormals[i1*3] + v*vertexNormals[i2*3],
                w*vertexNormals[i0*3+1] + u*vertexNormals[i1*3+1] + v*vertexNormals[i2*3+1],
                w*vertexNormals[i0*3+2] + u*vertexNormals[i1*3+2] + v*vertexNormals[i2*3+2]);
            outHit->n = n.normalized();
        }
        else {
            Vector3Dd n(triangleNormals[tri*3], triangleNormals[tri*3+1], triangleNormals[tri*3+2]);
            outHit->n = n.normalized();
        }
        if (outHit->n.dotProduct(rayDirection) >= 0) outHit->n = outHit->n.multiply(-1);
    }

    if (outHit->needsTextureCoordinates() && (int)vertexUvs.size() >= getNumVertices()*2) {
        outHit->u = w*vertexUvs[i0*2] + u*vertexUvs[i1*2] + v*vertexUvs[i2*2];
        outHit->v = w*vertexUvs[i0*2+1] + u*vertexUvs[i1*2+1] + v*vertexUvs[i2*2+1];
    }
}

void TriangleMesh::fillMaterialAndTexture(int triangleIndex, RayHit* outHit)
{
    if (outHit == 0) return;
    if (!materials.empty()) outHit->material = materials[0];

    if (!materialRanges.empty()) {
        for (size_t i = 0; i + 1 < materialRanges.size(); i++) {
            if (materialRanges[i].size() >= 1 && materialRanges[i+1].size() >= 2 &&
                triangleIndex >= materialRanges[i][0] && triangleIndex < materialRanges[i+1][0]) {
                int idx = materialRanges[i+1][1];
                if (idx >= 0 && idx < (int)materials.size()) outHit->material = materials[idx];
                break;
            }
        }
    }

    outHit->texture = 0;
    if (!textureRanges.empty()) {
        for (size_t i = 0; i + 1 < textureRanges.size(); i++) {
            if (textureRanges[i].size() >= 1 && textureRanges[i+1].size() >= 2 &&
                triangleIndex >= textureRanges[i][0] && triangleIndex < textureRanges[i+1][0]) {
                int idx = textureRanges[i+1][1] - 1;
                if (idx >= 0 && idx < (int)textures.size()) outHit->texture = textures[idx];
                break;
            }
        }
    }
}

bool TriangleMesh::doIntersectionInternal(const Ray& inRay, RayHit* outHit, int* outTriangleIndex)
{
    if (getNumTriangles() == 0) {
        intersectionTriangleIndex = -1;
        if (outTriangleIndex != 0) *outTriangleIndex = -1;
        return false;
    }
    if ((int)triangleNormals.size() < getNumTriangles()*3) calculateNormals();

    bool found = false;
    double bestT = 1e308;
    int bestTri = -1;
    double bestU = 0, bestV = 0;

    for (int i = 0; i < getNumTriangles(); i++) {
        int i0 = triangleIndices[i*3], i1 = triangleIndices[i*3+1], i2 = triangleIndices[i*3+2];
        Vector3Dd v0(vertexPositions[i0*3], vertexPositions[i0*3+1], vertexPositions[i0*3+2]);
        Vector3Dd v1(vertexPositions[i1*3], vertexPositions[i1*3+1], vertexPositions[i1*3+2]);
        Vector3Dd v2(vertexPositions[i2*3], vertexPositions[i2*3+1], vertexPositions[i2*3+2]);
        double t,u,v;
        if (intersectTriangle(inRay, v0, v1, v2, t, u, v) && t < bestT) {
            bestT=t; bestTri=i; bestU=u; bestV=v; found=true;
        }
    }

    if (!found) {
        intersectionTriangleIndex = -1;
        if (outTriangleIndex != 0) *outTriangleIndex = -1;
        return false;
    }
    intersectionTriangleIndex = bestTri;
    if (outTriangleIndex != 0) *outTriangleIndex = bestTri;

    if (outHit != 0) {
        outHit->setRay(inRay.withT(bestT));
        outHit->p = inRay.origin().add(inRay.direction().multiply(bestT));
        outHit->n = Vector3Dd(triangleNormals[bestTri*3], triangleNormals[bestTri*3+1], triangleNormals[bestTri*3+2]).normalized();
        outHit->t = Vector3Dd();
        outHit->u = 0;
        outHit->v = 0;
        interpolateTriangleData(bestTri, bestU, bestV, outHit, inRay.direction());
        fillMaterialAndTexture(bestTri, outHit);
    }
    return true;
}

bool TriangleMesh::doIntersection(const Ray& inRay, RayHit* outHit)
{
    return doIntersectionInternal(inRay, outHit, 0);
}

bool TriangleMesh::doIntersection(const Ray& inRay, RayHit* outHit, int* outTriangleIndex)
{
    return doIntersectionInternal(inRay, outHit, outTriangleIndex);
}

void TriangleMesh::doExtraInformation(const Ray& inRay, double inT, RayHit* outData)
{
    if (outData == 0) return;
    RayHit hit;
    if (doIntersection(inRay.withT(inT), &hit)) outData->clone(hit);
}

int TriangleMesh::doContainmentTest(const Vector3Dd& p, double distanceTolerance)
{
    for (int i = 0; i < getNumTriangles(); i++) {
        Vector3Dd p0(vertexPositions[3*triangleIndices[3*i+0]+0], vertexPositions[3*triangleIndices[3*i+0]+1], vertexPositions[3*triangleIndices[3*i+0]+2]);
        Vector3Dd p1(vertexPositions[3*triangleIndices[3*i+1]+0], vertexPositions[3*triangleIndices[3*i+1]+1], vertexPositions[3*triangleIndices[3*i+1]+2]);
        Vector3Dd p2(vertexPositions[3*triangleIndices[3*i+2]+0], vertexPositions[3*triangleIndices[3*i+2]+1], vertexPositions[3*triangleIndices[3*i+2]+2]);
        int status = Triangle::containmentTest(p0, p1, p2, p, distanceTolerance);
        if (status != OUTSIDE) return LIMIT;
    }
    return OUTSIDE;
}

double* TriangleMesh::getMinMax() { return calculateMinMaxPositions(); }

void TriangleMesh::doVoxelization(VoxelVolume& vv, const Matrix4x4d& M, ProgressMonitor*)
{
    Matrix4x4d Minv = M.inverse();
    double triangleMinmax[6];

    for (int t = 0; t < getNumTriangles(); t++) {
        Vector3Dd p0Geom(vertexPositions[3*triangleIndices[3*t+0]+0], vertexPositions[3*triangleIndices[3*t+0]+1], vertexPositions[3*triangleIndices[3*t+0]+2]);
        Vector3Dd p1Geom(vertexPositions[3*triangleIndices[3*t+1]+0], vertexPositions[3*triangleIndices[3*t+1]+1], vertexPositions[3*triangleIndices[3*t+1]+2]);
        Vector3Dd p2Geom(vertexPositions[3*triangleIndices[3*t+2]+0], vertexPositions[3*triangleIndices[3*t+2]+1], vertexPositions[3*triangleIndices[3*t+2]+2]);

        Vector3Dd p0Volume = Minv.multiply(p0Geom);
        Vector3Dd p1Volume = Minv.multiply(p1Geom);
        Vector3Dd p2Volume = Minv.multiply(p2Geom);
        Triangle::minMax(p0Volume, p1Volume, p2Volume, triangleMinmax);

        int minI = vv.getNearestIFromX(triangleMinmax[0]);
        int minJ = vv.getNearestJFromY(triangleMinmax[1]);
        int minK = vv.getNearestKFromZ(triangleMinmax[2]);
        int maxI = vv.getNearestIFromX(triangleMinmax[3]);
        int maxJ = vv.getNearestJFromY(triangleMinmax[4]);
        int maxK = vv.getNearestKFromZ(triangleMinmax[5]);

        double distanceTolerance = 2.0 / (double)vv.getXSize();
        for (int i = minI; i <= maxI; i++) {
            for (int j = minJ; j <= maxJ; j++) {
                for (int k = minK; k <= maxK; k++) {
                    Vector3Dd pVolume = vv.getVoxelPosition(i, j, k);
                    int status = Triangle::containmentTest(p0Volume, p1Volume, p2Volume, pVolume, distanceTolerance);
                    if (status != OUTSIDE) vv.putVoxel(i, j, k, (char)255);
                }
            }
        }
    }
}

TriangleMeshGroup* TriangleMesh::exportToTriangleMeshGroup()
{
    TriangleMeshGroup* g = new TriangleMeshGroup();
    g->addMesh(*this);
    return g;
}

void TriangleMesh::compact()
{
    vertexNormals.clear();

    int n = getNumVertices();
    std::vector<bool> count(n, false);
    for (size_t i = 0; i < triangleIndices.size(); i++) {
        int a = triangleIndices[i];
        if (a >= 0 && a < n) count[a] = true;
    }

    int j = 0;
    std::vector<int> map(n, -1);
    for (int i = 0; i < n; i++) if (count[i]) map[i] = j++;

    std::vector<double> oldPos = vertexPositions;
    std::vector<double> oldNor = vertexNormals;

    initVertexPositionsArray(j);
    if (!oldNor.empty()) initVertexNormalsArray();

    j = 0;
    for (int i = 0; i < n; i++) {
        if (count[i]) {
            vertexPositions[3*j+0] = oldPos[3*i+0];
            vertexPositions[3*j+1] = oldPos[3*i+1];
            vertexPositions[3*j+2] = oldPos[3*i+2];
            if (!oldNor.empty()) {
                vertexNormals[3*j+0] = oldNor[3*i+0];
                vertexNormals[3*j+1] = oldNor[3*i+1];
                vertexNormals[3*j+2] = oldNor[3*i+2];
            }
            j++;
        }
    }

    std::vector<int> oldTri = triangleIndices;
    j = 0;
    for (int i = 0; i < (int)oldTri.size()/3; i++) {
        int a = oldTri[3*i+0], b = oldTri[3*i+1], c = oldTri[3*i+2];
        if (!(a < 0 || a >= n || b < 0 || b >= n || c < 0 || c >= n)) j++;
    }

    initTriangleArrays(j);
    j = 0;
    for (int i = 0; i < (int)oldTri.size()/3; i++) {
        int a = oldTri[3*i+0], b = oldTri[3*i+1], c = oldTri[3*i+2];
        if (a < 0 || a >= n || b < 0 || b >= n || c < 0 || c >= n) {}
        else {
            triangleIndices[3*j+0] = map[a];
            triangleIndices[3*j+1] = map[b];
            triangleIndices[3*j+2] = map[c];
            j++;
        }
    }
}

void TriangleMesh::removeSelectedVertices()
{
    if (vertexSelections.empty()) return;

    vertexNormals.clear();
    vertexBinormals.clear();
    vertexTangents.clear();

    int n = getNumVertices();
    for (int i = 0; i < (int)triangleIndices.size()/3; i++) {
        int a = triangleIndices[3*i+0];
        int b = triangleIndices[3*i+1];
        int c = triangleIndices[3*i+2];
        if (a < 0 || a >= n || b < 0 || b >= n || c < 0 || c >= n ||
            vertexSelections[a] || vertexSelections[b] || vertexSelections[c]) {
            triangleIndices[3*i+0] = -1;
            triangleIndices[3*i+1] = -1;
            triangleIndices[3*i+2] = -1;
        }
    }

    compact();
    vertexSelections.clear();
    calculateNormals();
}

void TriangleMesh::appendVertices(const std::vector<double>& ev)
{
    vertexPositions.insert(vertexPositions.end(), ev.begin(), ev.end());
}

void TriangleMesh::appendTriangles(const std::vector<int>& et)
{
    triangleIndices.insert(triangleIndices.end(), et.begin(), et.end());
}

void TriangleMesh::simpleTriangleCut(InfinitePlane& p, std::vector<double>& extraVertices,
                                     std::vector<int>& extraTriangles, int nv, int i,
                                     const Vector3Dd& p1, const Vector3Dd& p2, const Vector3Dd& p3)
{
    Vector3Dd a = p2.subtract(p1).normalized();
    Vector3Dd b = p3.subtract(p1).normalized();

    Vector3Dd ma, mb;
    bool hasMa = false, hasMb = false;

    Ray* hitA = p.doIntersectionWithNegative(Ray(p1, a));
    if (hitA != 0) {
        RayHit gia;
        if (p.doIntersection(*hitA, &gia)) { ma = gia.p; hasMa = true; }
        delete hitA;
    }
    Ray* hitB = p.doIntersectionWithNegative(Ray(p1, b));
    if (hitB != 0) {
        RayHit gib;
        if (p.doIntersection(*hitB, &gib)) { mb = gib.p; hasMb = true; }
        delete hitB;
    }

    extraTriangles.push_back(i);
    extraTriangles.push_back((int)extraVertices.size()/3 + nv);
    if (!hasMa || !hasMb) return;

    extraVertices.push_back(ma.x()); extraVertices.push_back(ma.y()); extraVertices.push_back(ma.z());
    extraTriangles.push_back((int)extraVertices.size()/3 + nv);
    extraVertices.push_back(mb.x()); extraVertices.push_back(mb.y()); extraVertices.push_back(mb.z());
}

void TriangleMesh::halfTriangleCut(InfinitePlane& p, std::vector<double>& extraVertices,
                                   std::vector<int>& extraTriangles, int nv, int i, int j,
                                   const Vector3Dd&, const Vector3Dd& p2, const Vector3Dd& p3)
{
    Vector3Dd a = p2.subtract(p3).normalized();

    Vector3Dd ma;
    bool hasMa = false;

    Ray* hitA = p.doIntersectionWithNegative(Ray(p2, a));
    if (hitA != 0) {
        RayHit gia;
        if (p.doIntersection(*hitA, &gia)) { ma = gia.p; hasMa = true; }
        delete hitA;
    }

    extraTriangles.push_back(i);
    extraTriangles.push_back((int)extraVertices.size()/3 + nv);
    if (!hasMa) return;

    extraVertices.push_back(ma.x()); extraVertices.push_back(ma.y()); extraVertices.push_back(ma.z());
    extraTriangles.push_back(j);
}

void TriangleMesh::doubleTriangleCut(InfinitePlane& p, std::vector<double>& extraVertices,
                                     std::vector<int>& extraTriangles, int nv, int i, int j,
                                     const Vector3Dd& p1, const Vector3Dd& p2, const Vector3Dd& p3)
{
    Vector3Dd a = p1.subtract(p3).normalized();
    Vector3Dd b = p2.subtract(p3).normalized();

    Vector3Dd ma, mb;
    bool hasMa = false, hasMb = false;

    Ray* hitA = p.doIntersectionWithNegative(Ray(p3, a));
    if (hitA != 0) {
        RayHit gia;
        if (p.doIntersection(*hitA, &gia)) { ma = gia.p; hasMa = true; }
        delete hitA;
    }
    Ray* hitB = p.doIntersectionWithNegative(Ray(p3, b));
    if (hitB != 0) {
        RayHit gib;
        if (p.doIntersection(*hitB, &gib)) { mb = gib.p; hasMb = true; }
        delete hitB;
    }

    extraTriangles.push_back(i);
    extraTriangles.push_back((int)extraVertices.size()/3 + nv);
    if (!hasMa || !hasMb) return;

    extraVertices.push_back(ma.x()); extraVertices.push_back(ma.y()); extraVertices.push_back(ma.z());
    extraTriangles.push_back((int)extraVertices.size()/3 + nv);
    extraTriangles.push_back(i);
    extraTriangles.push_back((int)extraVertices.size()/3 + nv);
    extraTriangles.push_back(j);
    extraVertices.push_back(mb.x()); extraVertices.push_back(mb.y()); extraVertices.push_back(mb.z());
}

void TriangleMesh::slice(InfinitePlane& p)
{
    std::vector<int> extraTriangles;
    std::vector<double> extraVertices;

    int nv = getNumVertices();

    for (int i = 0; i < (int)triangleIndices.size()/3; i++) {
        int ia = triangleIndices[3*i+0];
        int ib = triangleIndices[3*i+1];
        int ic = triangleIndices[3*i+2];
        if (ia < 0 || ib < 0 || ic < 0 || ia >= getNumVertices() || ib >= getNumVertices() || ic >= getNumVertices()) continue;

        Vector3Dd p1(vertexPositions[3*ia+0], vertexPositions[3*ia+1], vertexPositions[3*ia+2]);
        Vector3Dd p2(vertexPositions[3*ib+0], vertexPositions[3*ib+1], vertexPositions[3*ib+2]);
        Vector3Dd p3(vertexPositions[3*ic+0], vertexPositions[3*ic+1], vertexPositions[3*ic+2]);

        int t1 = p.doContainmentTestHalfSpace(p1, VSDK::EPSILON);
        int t2 = p.doContainmentTestHalfSpace(p2, VSDK::EPSILON);
        int t3 = p.doContainmentTestHalfSpace(p3, VSDK::EPSILON);

        if ((t1 == OUTSIDE && t2 == OUTSIDE && t3 == OUTSIDE) ||
            (t1 == LIMIT && t2 == OUTSIDE && t3 == OUTSIDE) ||
            (t1 == OUTSIDE && t2 == LIMIT && t3 == OUTSIDE) ||
            (t1 == OUTSIDE && t2 == OUTSIDE && t3 == LIMIT) ||
            (t1 == LIMIT && t2 == LIMIT && t3 == OUTSIDE) ||
            (t1 == LIMIT && t2 == OUTSIDE && t3 == LIMIT) ||
            (t1 == OUTSIDE && t2 == LIMIT && t3 == LIMIT)) {
            triangleIndices[3*i+0] = triangleIndices[3*i+1] = triangleIndices[3*i+2] = -1;
        }
        else if (t1 == LIMIT && t2 == LIMIT && t3 == LIMIT) {
            Vector3Dd n = p2.subtract(p1).crossProduct(p3.subtract(p1));
            if (n.dotProduct(p.getNormal()) > 0) triangleIndices[3*i+0] = triangleIndices[3*i+1] = triangleIndices[3*i+2] = -1;
        }
        else if ((t1 == LIMIT && t2 == INSIDE && t3 == INSIDE) ||
                 (t1 == INSIDE && t2 == LIMIT && t3 == INSIDE) ||
                 (t1 == INSIDE && t2 == INSIDE && t3 == LIMIT) ||
                 (t1 == LIMIT && t2 == LIMIT && t3 == INSIDE) ||
                 (t1 == LIMIT && t2 == INSIDE && t3 == LIMIT) ||
                 (t1 == INSIDE && t2 == LIMIT && t3 == LIMIT) ||
                 (t1 == INSIDE && t2 == INSIDE && t3 == INSIDE)) {
        }
        else {
            if (t1 == INSIDE && t2 == OUTSIDE && t3 == OUTSIDE) simpleTriangleCut(p, extraVertices, extraTriangles, nv, ia, p1, p2, p3);
            else if (t2 == INSIDE && t1 == OUTSIDE && t3 == OUTSIDE) simpleTriangleCut(p, extraVertices, extraTriangles, nv, ia, p2, p1, p3);
            else if (t3 == INSIDE && t1 == OUTSIDE && t2 == OUTSIDE) simpleTriangleCut(p, extraVertices, extraTriangles, nv, ia, p3, p1, p2);
            else if (t1 == INSIDE && t2 == INSIDE && t3 == OUTSIDE) doubleTriangleCut(p, extraVertices, extraTriangles, nv, ia, ib, p1, p2, p3);
            else if (t1 == INSIDE && t3 == INSIDE && t2 == OUTSIDE) doubleTriangleCut(p, extraVertices, extraTriangles, nv, ia, ic, p1, p3, p2);
            else if (t2 == INSIDE && t3 == INSIDE && t1 == OUTSIDE) doubleTriangleCut(p, extraVertices, extraTriangles, nv, ib, ic, p2, p3, p1);
            else if (t1 == INSIDE && t2 == LIMIT && t3 == OUTSIDE) halfTriangleCut(p, extraVertices, extraTriangles, nv, ia, ib, p2, p1, p3);
            else if (t2 == INSIDE && t1 == LIMIT && t3 == OUTSIDE) halfTriangleCut(p, extraVertices, extraTriangles, nv, ia, ia, p1, p2, p3);
            else if (t2 == INSIDE && t3 == LIMIT && t1 == OUTSIDE) halfTriangleCut(p, extraVertices, extraTriangles, nv, ia, ic, p3, p2, p1);
            else if (t3 == INSIDE && t2 == LIMIT && t1 == OUTSIDE) halfTriangleCut(p, extraVertices, extraTriangles, nv, ia, ib, p2, p3, p1);
            else if (t3 == INSIDE && t1 == LIMIT && t2 == OUTSIDE) halfTriangleCut(p, extraVertices, extraTriangles, nv, ia, ia, p1, p3, p2);
            else if (t1 == INSIDE && t3 == LIMIT && t2 == OUTSIDE) halfTriangleCut(p, extraVertices, extraTriangles, nv, ia, ic, p3, p1, p2);
            else Logger::reportMessage("TriangleMesh", Logger::WARNING, "slice", "Unhandled slice case");
        }
    }

    appendVertices(extraVertices);
    appendTriangles(extraTriangles);
    compact();
    calculateNormals();
}

std::string TriangleMesh::toString() const
{
    std::string msg;
    msg += "- TriangleMesh ------------------------------------------------------------\n";
    msg += "  - Number of triangles:" + std::to_string(getNumTriangles()) + "\n";
    msg += "  - Number of vertexes:" + std::to_string(getNumVertices()) + "\n";

    TriangleMesh* self = const_cast<TriangleMesh*>(this);
    double* mm = self->getMinMax();
    msg += "  - MINMAX: (" + std::to_string(mm[0]) + ", " + std::to_string(mm[1]) + ", " + std::to_string(mm[2]) + ") - (" + std::to_string(mm[3]) + ", " + std::to_string(mm[4]) + ", " + std::to_string(mm[5]) + ")\n";
    delete [] mm;

    if (materials.empty()) msg += "  - No materials available!\n";
    else msg += "  - " + std::to_string(materials.size()) + " materials\n";

    if (materialRanges.empty()) msg += "  - No material ranges association table available!\n";
    else {
        msg += "  - " + std::to_string(materialRanges.size()) + " material spans:\n";
        for (size_t i = 0; i < materialRanges.size(); i++) {
            if (materialRanges[i].size() >= 2) msg += "    . " + std::to_string(materialRanges[i][0]) + " -> " + std::to_string(materialRanges[i][1]) + "\n";
        }
    }

    if (textures.empty()) msg += "  - No textures available!\n";
    else msg += "  - " + std::to_string(textures.size()) + " textures\n";

    if (textureRanges.empty()) msg += "  - No texture ranges association table available!\n";
    else {
        msg += "  - " + std::to_string(textureRanges.size()) + " texture spans:\n";
        for (size_t i = 0; i < textureRanges.size(); i++) {
            if (textureRanges[i].size() >= 2) msg += "    . " + std::to_string(textureRanges[i][0]) + " -> " + std::to_string(textureRanges[i][1]) + "\n";
        }
    }

    msg += "---------------------------------------------------------------------------\n";
    return msg;
}
