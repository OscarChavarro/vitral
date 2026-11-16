#include <gtest/gtest.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

/*
C++ counterpart of the test of Java's `PolyhedralBoundedSolidEulerOperatorsTest`
added in the range ported here (lmekr keeps the outer loop first). The rest
of that Java suite predates the range and has no C++ counterpart yet; the
fixture below is the Java one it needs.
*/
namespace {

typedef PolyhedralBoundedSolidEulerOperators Euler;

PolyhedralBoundedSolid* createSkeletalSolid()
{
    PolyhedralBoundedSolid* solid = new PolyhedralBoundedSolid();
    Euler::mvfs(solid, Vector3Dd(0.0, 0.0, 0.0), 1, 1);
    return solid;
}

PolyhedralBoundedSolid* createPlanarFaceWithBridgeToHoleSeed()
{
    PolyhedralBoundedSolid* solid = createSkeletalSolid();
    Euler::lmev(solid,
        solid->findFace(1)->boundariesList.get(0)->boundaryStartHalfEdge,
        solid->findFace(1)->boundariesList.get(0)->boundaryStartHalfEdge,
        2, Vector3Dd(1.0, 0.0, 0.0));
    Euler::lmev(solid, solid->findVertex(2)->emanatingHalfEdge,
        solid->findVertex(2)->emanatingHalfEdge, 3, Vector3Dd(1.0, 1.0, 0.0));
    Euler::lmev(solid, solid->findVertex(3)->emanatingHalfEdge,
        solid->findVertex(3)->emanatingHalfEdge, 4, Vector3Dd(0.0, 1.0, 0.0));
    Euler::lmef(solid, solid->findFace(1)->findHalfEdge(4),
        solid->findFace(1)->findHalfEdge(1), 2);
    Euler::lmev(solid, solid->findFace(1)->findHalfEdge(1),
        solid->findFace(1)->findHalfEdge(1), 5, Vector3Dd(0.25, 0.25, 0.0));
    return solid;
}

_PolyhedralBoundedSolidLoop* loopThrough(_PolyhedralBoundedSolidFace* face,
    int vertexId)
{
    int i;
    int j;

    for ( i = 0; i < face->boundariesList.size(); i++ ) {
        _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(i);
        for ( j = 0; j < loop->halfEdgesList.size(); j++ ) {
            if ( loop->halfEdgesList.get(j)->startingVertex->id == vertexId ) {
                return loop;
            }
        }
    }
    return nullptr;
}

}

/*
lmekr removes the loop of its second half-edge. When that loop is the outer
loop of the face (the first half-edge is on a ring), the merged loop is the
outer boundary and must come first. It used to stay in the place of the
ring, so that in a face with another ring that other ring was taken for the
outer loop. The connect stage of the set operations does exactly this when
a face is crossed by several chords that pass through vertices inside the
face (plan-stage11 Phase F): every later decision about where a pending
ring belongs was then taken against the wrong loop.
*/
TEST(PolyhedralBoundedSolidEulerOperatorsTest, FaceWithTwoRingsLmekrMergesTheOuterLoopIntoARingMergedLoopIsTheOuterLoop) {
    // Arrange: a square face with two strut rings, {5,6} and {7,8}.
    PolyhedralBoundedSolid* solid = createPlanarFaceWithBridgeToHoleSeed();
    _PolyhedralBoundedSolidFace* face = solid->findFace(1);
    Euler::lmev(solid, solid->findVertex(5)->emanatingHalfEdge,
        solid->findVertex(5)->emanatingHalfEdge, 6,
        Vector3Dd(0.4, 0.3, 0.0));
    Euler::lkemr(solid, face->findHalfEdge(1, 5), face->findHalfEdge(5, 1));
    Euler::lmev(solid, face->findHalfEdge(3), face->findHalfEdge(3), 7,
        Vector3Dd(0.75, 0.75, 0.0));
    Euler::lmev(solid, solid->findVertex(7)->emanatingHalfEdge,
        solid->findVertex(7)->emanatingHalfEdge, 8,
        Vector3Dd(0.6, 0.7, 0.0));
    Euler::lkemr(solid, face->findHalfEdge(3, 7), face->findHalfEdge(7, 3));
    ASSERT_EQ(3, face->boundariesList.size());
    _PolyhedralBoundedSolidLoop* outer = face->boundariesList.get(0);
    _PolyhedralBoundedSolidLoop* firstRing = loopThrough(face, 5);
    _PolyhedralBoundedSolidLoop* secondRing = loopThrough(face, 7);
    EXPECT_NE(outer, firstRing);
    EXPECT_NE(secondRing, firstRing);

    // Action: the ring half-edge first, the outer half-edge second.
    Euler::lmekr(solid, firstRing->boundaryStartHalfEdge,
        outer->boundaryStartHalfEdge);

    // Assert
    ASSERT_EQ(2, face->boundariesList.size());
    EXPECT_EQ(firstRing, face->boundariesList.get(0));
    EXPECT_EQ(secondRing, face->boundariesList.get(1));
    EXPECT_EQ(face->boundariesList.get(0), loopThrough(face, 1));
    EXPECT_EQ(face->boundariesList.get(0), loopThrough(face, 3));
    EXPECT_TRUE(PolyhedralBoundedSolidValidationEngine::validateIntermediate(
        solid));
    delete solid;
}
