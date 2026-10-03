#include <string>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

// The descriptions of the nodes, with the same texts as the Java `toString`
// methods (used by the debugging traces of the operators)

java::String _PolyhedralBoundedSolidEdge::toString() const
{
    std::string msg = "Edge id " + std::to_string(id) + ". Half1: ";
    if ( leftHalf == nullptr ) {
        msg += "null. ";
    }
    else {
        msg += "vertex " + std::to_string(leftHalf->startingVertex->id);
    }

    msg += " / Half2: ";

    if ( rightHalf == nullptr ) {
        msg += "null. ";
    }
    else {
        msg += "vertex " + std::to_string(rightHalf->startingVertex->id);
    }
    return java::String(msg.c_str());
}

java::String _PolyhedralBoundedSolidFace::toString() const
{
    std::string msg = "Face id [" + std::to_string(id) + "], " +
        std::to_string(boundariesList.size()) + " loops.";
    return java::String(msg.c_str());
}

java::String _PolyhedralBoundedSolidHalfEdge::toString()
{
    std::string msg = "HalfEdge id " + std::to_string(id) + ". ";

    msg += "From vertex [" + std::to_string(startingVertex->id) + "] ";
    msg += "to vertex [" + std::to_string(next()->startingVertex->id) + "]. ";

    msg += "Parent face [" + std::to_string(parentLoop->parentFace->id) + "]. ";
    if ( parentEdge == nullptr ) {
        msg += "<without parent edge>. ";
    }
    else {
        msg += "Parent edge " + std::to_string(parentEdge->id) + " ";
        if ( this == parentEdge->leftHalf ) {
            msg += "(left)";
        }
        else if ( this == parentEdge->rightHalf ) {
            msg += "(right)";
        }
        else {
            msg += "(INCONSISTENT!)";
        }
        msg += ". ";
    }
    msg += "Next halfedge: " + std::to_string(next()->id) + ".";
    return java::String(msg.c_str());
}

java::String _PolyhedralBoundedSolidLoop::toString() const
{
    std::string msg = "Loop, parent face " + std::to_string(parentFace->id);
    return java::String(msg.c_str());
}

java::String _PolyhedralBoundedSolidVertex::toString() const
{
    java::String* p = position.toString();
    std::string msg = "vertex id " + std::to_string(id) + ". Position " +
        std::string(p->c_str()) + ". ";
    delete p;
    if ( emanatingHalfEdge == nullptr ) {
        msg += "No associated half-edge.";
    }
    else {
        msg += "H.E. " + std::to_string(emanatingHalfEdge->id);
    }
    return java::String(msg.c_str());
}
