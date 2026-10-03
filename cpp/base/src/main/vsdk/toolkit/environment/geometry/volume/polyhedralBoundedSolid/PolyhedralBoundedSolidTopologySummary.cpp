#include <map>
#include <set>
#include <stdexcept>
#include <string>
#include <vector>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

namespace {

_PolyhedralBoundedSolidFace* faceOf(_PolyhedralBoundedSolidHalfEdge* halfEdge)
{
    if ( halfEdge == nullptr || halfEdge->parentLoop == nullptr ) {
        return nullptr;
    }
    return halfEdge->parentLoop->parentFace;
}

void collectVertex(_PolyhedralBoundedSolidHalfEdge* halfEdge,
                   std::set<_PolyhedralBoundedSolidVertex*>& vertices)
{
    if ( halfEdge != nullptr && halfEdge->startingVertex != nullptr ) {
        vertices.insert(halfEdge->startingVertex);
    }
}

void collectFaceVertices(_PolyhedralBoundedSolidFace* face,
                         std::set<_PolyhedralBoundedSolidVertex*>& vertices)
{
    long i;
    long j;
    for ( i = 0; i < face->boundariesList.size(); i++ ) {
        for ( j = 0;
              j < face->boundariesList.get(i)->halfEdgesList.size();
              j++ ) {
            collectVertex(
                face->boundariesList.get(i)->halfEdgesList.get(j),
                vertices);
        }
    }
}

int floorMod(int value, int modulus)
{
    int result = value % modulus;
    return result < 0 ? result + modulus : result;
}

const char* booleanName(bool value)
{
    return value ? "true" : "false";
}

class DisjointSet {
private:
    std::vector<int> parent;
    std::vector<unsigned char> rank;

public:
    explicit DisjointSet(int size)
        : parent((size_t)size), rank((size_t)size, 0)
    {
        for ( int i = 0; i < size; i++ ) {
            parent[(size_t)i] = i;
        }
    }

    int find(int value)
    {
        if ( parent[(size_t)value] != value ) {
            parent[(size_t)value] = find(parent[(size_t)value]);
        }
        return parent[(size_t)value];
    }

    void unite(int first, int second)
    {
        int firstRoot = find(first);
        int secondRoot = find(second);
        if ( firstRoot == secondRoot ) {
            return;
        }
        if ( rank[(size_t)firstRoot] < rank[(size_t)secondRoot] ) {
            parent[(size_t)firstRoot] = secondRoot;
        }
        else if ( rank[(size_t)firstRoot] > rank[(size_t)secondRoot] ) {
            parent[(size_t)secondRoot] = firstRoot;
        }
        else {
            parent[(size_t)secondRoot] = firstRoot;
            rank[(size_t)firstRoot]++;
        }
    }
};

}

//= Shell =================================================================

PolyhedralBoundedSolidTopologySummary::Shell::Shell(int faceCount,
    int edgeCount, int vertexCount, int adjustedEulerCharacteristic,
    bool closed)
    : faceCount(faceCount), edgeCount(edgeCount), vertexCount(vertexCount),
      adjustedEulerCharacteristic(adjustedEulerCharacteristic),
      closed(closed),
      closedOrientableEulerCompatible(closed &&
          adjustedEulerCharacteristic <= 2 &&
          floorMod(adjustedEulerCharacteristic, 2) == 0)
{
}

int PolyhedralBoundedSolidTopologySummary::Shell::getFaceCount() const { return faceCount; }
int PolyhedralBoundedSolidTopologySummary::Shell::getEdgeCount() const { return edgeCount; }
int PolyhedralBoundedSolidTopologySummary::Shell::getVertexCount() const { return vertexCount; }
int PolyhedralBoundedSolidTopologySummary::Shell::getAdjustedEulerCharacteristic() const { return adjustedEulerCharacteristic; }
bool PolyhedralBoundedSolidTopologySummary::Shell::isClosed() const { return closed; }
bool PolyhedralBoundedSolidTopologySummary::Shell::isClosedOrientableEulerCompatible() const { return closedOrientableEulerCompatible; }

java::String PolyhedralBoundedSolidTopologySummary::Shell::toString() const
{
    std::string msg = "Shell{faces=" + std::to_string(faceCount) +
        ", edges=" + std::to_string(edgeCount) +
        ", vertices=" + std::to_string(vertexCount) +
        ", adjustedEuler=" + std::to_string(adjustedEulerCharacteristic) +
        ", closed=" + booleanName(closed) +
        ", closedOrientableEulerCompatible=" +
        booleanName(closedOrientableEulerCompatible) + "}";
    return java::String(msg.c_str());
}

//= Summary ===============================================================

PolyhedralBoundedSolidTopologySummary::PolyhedralBoundedSolidTopologySummary(
    PolyhedralBoundedSolid* solid)
{
    faceCount = (int)solid->getPolygonsList().size();
    edgeCount = (int)solid->getEdgesList().size();
    vertexCount = (int)solid->getVerticesList().size();

    std::map<_PolyhedralBoundedSolidFace*, int> faceIndexes;
    int i;
    for ( i = 0; i < faceCount; i++ ) {
        faceIndexes[solid->getPolygonsList().get(i)] = i;
    }

    DisjointSet components(faceCount);
    int invalidAdjacencies = 0;
    for ( i = 0; i < edgeCount; i++ ) {
        _PolyhedralBoundedSolidEdge* edge = solid->getEdgesList().get(i);
        std::map<_PolyhedralBoundedSolidFace*, int>::const_iterator leftIndex =
            faceIndexes.find(faceOf(edge->leftHalf));
        std::map<_PolyhedralBoundedSolidFace*, int>::const_iterator rightIndex =
            faceIndexes.find(faceOf(edge->rightHalf));
        if ( leftIndex == faceIndexes.end() ||
             rightIndex == faceIndexes.end() ) {
            invalidAdjacencies++;
            continue;
        }
        components.unite(leftIndex->second, rightIndex->second);
    }
    invalidEdgeAdjacencyCount = invalidAdjacencies;

    // Ordered by component root, as the Java `TreeMap`
    std::map<int, std::vector<_PolyhedralBoundedSolidFace*> > componentFaces;
    int reachedFaces = 0;
    for ( i = 0; i < faceCount; i++ ) {
        int root = components.find(i);
        componentFaces[root].push_back(solid->getPolygonsList().get(i));
        reachedFaces++;
    }
    int componentFaceTotal = 0;
    std::map<int, std::vector<_PolyhedralBoundedSolidFace*> >::const_iterator c;
    for ( c = componentFaces.begin(); c != componentFaces.end(); ++c ) {
        componentFaceTotal += (int)c->second.size();
    }
    everyFaceReachedExactlyOnce =
        (int)faceIndexes.size() == faceCount &&
        reachedFaces == faceCount && componentFaceTotal == faceCount;

    int totalAdjustedFaceTerm = 0;
    for ( c = componentFaces.begin(); c != componentFaces.end(); ++c ) {
        const std::vector<_PolyhedralBoundedSolidFace*>& faces = c->second;
        std::set<_PolyhedralBoundedSolidFace*> faceSet(faces.begin(), faces.end());
        std::set<_PolyhedralBoundedSolidEdge*> shellEdges;
        std::set<_PolyhedralBoundedSolidVertex*> shellVertices;
        int adjustedFaceTerm = 0;
        bool closed = true;

        for ( size_t f = 0; f < faces.size(); f++ ) {
            adjustedFaceTerm += 2 - (int)faces[f]->boundariesList.size();
            collectFaceVertices(faces[f], shellVertices);
        }
        totalAdjustedFaceTerm += adjustedFaceTerm;

        for ( i = 0; i < (int)solid->getEdgesList().size(); i++ ) {
            _PolyhedralBoundedSolidEdge* edge = solid->getEdgesList().get(i);
            _PolyhedralBoundedSolidFace* leftFace = faceOf(edge->leftHalf);
            _PolyhedralBoundedSolidFace* rightFace = faceOf(edge->rightHalf);
            bool touchesShell = faceSet.count(leftFace) > 0 ||
                faceSet.count(rightFace) > 0;
            if ( !touchesShell ) {
                continue;
            }
            shellEdges.insert(edge);
            if ( leftFace == nullptr || rightFace == nullptr ||
                 faceSet.count(leftFace) == 0 ||
                 faceSet.count(rightFace) == 0 ) {
                closed = false;
            }
            collectVertex(edge->leftHalf, shellVertices);
            collectVertex(edge->rightHalf, shellVertices);
        }

        int chi = (int)shellVertices.size() - (int)shellEdges.size() +
            adjustedFaceTerm;
        shells.push_back(Shell((int)faces.size(), (int)shellEdges.size(),
            (int)shellVertices.size(), chi, closed));
    }
    adjustedEulerCharacteristic =
        vertexCount - edgeCount + totalAdjustedFaceTerm;
}

PolyhedralBoundedSolidTopologySummary PolyhedralBoundedSolidTopologySummary::from(
    PolyhedralBoundedSolid* solid)
{
    if ( solid == nullptr ) {
        throw std::invalid_argument("solid must not be null");
    }
    return PolyhedralBoundedSolidTopologySummary(solid);
}

int PolyhedralBoundedSolidTopologySummary::getFaceCount() const { return faceCount; }
int PolyhedralBoundedSolidTopologySummary::getEdgeCount() const { return edgeCount; }
int PolyhedralBoundedSolidTopologySummary::getVertexCount() const { return vertexCount; }
int PolyhedralBoundedSolidTopologySummary::getShellCount() const { return (int)shells.size(); }
int PolyhedralBoundedSolidTopologySummary::getAdjustedEulerCharacteristic() const { return adjustedEulerCharacteristic; }
bool PolyhedralBoundedSolidTopologySummary::isEveryFaceReachedExactlyOnce() const { return everyFaceReachedExactlyOnce; }
int PolyhedralBoundedSolidTopologySummary::getInvalidEdgeAdjacencyCount() const { return invalidEdgeAdjacencyCount; }

const std::vector<PolyhedralBoundedSolidTopologySummary::Shell>&
PolyhedralBoundedSolidTopologySummary::getShells() const
{
    return shells;
}

bool PolyhedralBoundedSolidTopologySummary::hasUniversalContradiction() const
{
    if ( !everyFaceReachedExactlyOnce || invalidEdgeAdjacencyCount > 0 ) {
        return true;
    }
    for ( size_t i = 0; i < shells.size(); i++ ) {
        if ( !shells[i].isClosedOrientableEulerCompatible() ) {
            return true;
        }
    }
    return false;
}

java::String PolyhedralBoundedSolidTopologySummary::toString() const
{
    std::string perShell = "[";
    for ( size_t i = 0; i < shells.size(); i++ ) {
        if ( i > 0 ) {
            perShell += ", ";
        }
        perShell += shells[i].toString().c_str();
    }
    perShell += "]";
    std::string msg = "TopologySummary{faces=" + std::to_string(faceCount) +
        ", edges=" + std::to_string(edgeCount) +
        ", vertices=" + std::to_string(vertexCount) +
        ", shells=" + std::to_string(shells.size()) +
        ", adjustedEuler=" + std::to_string(adjustedEulerCharacteristic) +
        ", everyFaceReachedExactlyOnce=" +
        booleanName(everyFaceReachedExactlyOnce) +
        ", invalidEdgeAdjacencies=" + std::to_string(invalidEdgeAdjacencyCount) +
        ", perShell=" + perShell + "}";
    return java::String(msg.c_str());
}
