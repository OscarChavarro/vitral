#include <algorithm>
#include <vector>
#include <gtest/gtest.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "PolyhedralBoundedSolidTestFixtures.h"

/*
Edge cases of the low-level Euler operators that the main operator suite
(PolyhedralBoundedSolidEulerOperatorsTest) does not reach: empty-loop special
cases, inverse round trips that are continued afterwards, and back-pointer
integrity (vertex -> emanating half-edge).

Traceability: [MANT1988] Ch. 11.2.2 (addhe/delhe special cases for the
edge-less half-edge of an empty loop), Programs 11.6-11.8, Problems 11.3 and
11.4, and the lkemr text of Section 11.3.4 ("the special cases that one or
both of the resulting loops are empty"). Cases marked "hidden defect" were
found by the Fable 5.2 kernel analysis
(doc/polyhedralBoundedSolidDevelopment/Fable5.2Analysis.md, defects E-01 to
E-06).
C++ counterpart of Java's `PolyhedralBoundedSolidEulerOperatorsEdgeCasesTest`.
*/
namespace {

typedef PolyhedralBoundedSolidEulerOperators Euler;

PolyhedralBoundedSolid* createSkeletalSolid()
{
    PolyhedralBoundedSolid* solid = new PolyhedralBoundedSolid();
    Euler::mvfs(solid, Vector3Dd(0.0, 0.0, 0.0), 1, 1);
    return solid;
}

/**
Unit square lamina (faces 1 and 2, vertices 1..4) with a strut from vertex
1 to an interior vertex 5 inside face 1.
*/
PolyhedralBoundedSolid* createSquareLaminaWithStrut()
{
    PolyhedralBoundedSolid* solid = createSkeletalSolid();
    Euler::smev(solid, 1, 1, 2, Vector3Dd(1.0, 0.0, 0.0));
    Euler::smev(solid, 1, 2, 3, Vector3Dd(1.0, 1.0, 0.0));
    Euler::smev(solid, 1, 3, 4, Vector3Dd(0.0, 1.0, 0.0));
    Euler::lmef(solid, solid->findFace(1)->findHalfEdge(4),
        solid->findFace(1)->findHalfEdge(1), 2);
    Euler::lmev(solid, solid->findFace(1)->findHalfEdge(1),
        solid->findFace(1)->findHalfEdge(1), 5, Vector3Dd(0.25, 0.25, 0.0));
    return solid;
}

std::vector<int> loopSizes(_PolyhedralBoundedSolidFace* face)
{
    std::vector<int> sizes;
    int i;
    for ( i = 0; i < face->boundariesList.size(); i++ ) {
        sizes.push_back(face->boundariesList.get(i)->halfEdgesList.size());
    }
    return sizes;
}

/**
Lists every vertex whose emanating half-edge is null, does not start at the
vertex, or is no longer a member of its parent loop.
*/
java::String describeBrokenEmanatingReferences(PolyhedralBoundedSolid* solid)
{
    java::String report;
    int i;

    for ( i = 0; i < solid->getVerticesList().size(); i++ ) {
        _PolyhedralBoundedSolidVertex* vertex = solid->getVerticesList().get(i);
        _PolyhedralBoundedSolidHalfEdge* he = vertex->emanatingHalfEdge;

        if ( he == nullptr ) {
            report += java::String(" v") + java::String::valueOf(vertex->id) +
                ":null";
            continue;
        }
        if ( he->startingVertex != vertex ) {
            report += java::String(" v") + java::String::valueOf(vertex->id) +
                ":wrongStart";
        }
        if ( he->parentLoop == nullptr ||
             !he->parentLoop->halfEdgesList.locateWindowAtElem(he) ) {
            report += java::String(" v") + java::String::valueOf(vertex->id) +
                ":notInLoop";
        }
    }
    return report;
}

}

/*
Defect (fixed) E-01: mvfs -> lmev (strut) -> lkev returns to the skeletal
count, but the loop's half-edge list is left with a stale head node (list
defect L-01). The next lmev on the same vertex then yields a
two-half-edge loop whose next() chain does not visit both half-edges.
*/
TEST(PolyhedralBoundedSolidEulerOperatorsEdgeCasesTest, StrutKilledBackToSkeletonNewStrutIsAddedLoopTraversalVisitsBothHalfEdges) {
    PolyhedralBoundedSolid* solid = createSkeletalSolid();
    _PolyhedralBoundedSolidLoop* loop =
        solid->findFace(1)->boundariesList.get(0);
    Euler::smev(solid, 1, 1, 2, Vector3Dd(1.0, 0.0, 0.0));
    _PolyhedralBoundedSolidHalfEdge* back = loop->halfEdgeVertices(2, 1);
    Euler::lkev(solid, back, back->mirrorHalfEdge());

    Euler::smev(solid, 1, 1, 3, Vector3Dd(0.0, 1.0, 0.0));

    ASSERT_EQ(2, loop->halfEdgesList.size());
    _PolyhedralBoundedSolidHalfEdge* first = loop->halfEdgesList.get(0);
    _PolyhedralBoundedSolidHalfEdge* second = loop->halfEdgesList.get(1);
    EXPECT_EQ(second, first->next());
    EXPECT_EQ(first, second->next());
    EXPECT_EQ(second, first->previous());
    EXPECT_EQ(first, second->previous());
    delete solid;
}

/*
Defect (fixed) E-02: the lkemr documentation (after [MANT1988] Sec.
11.3.4) says the arguments can be swapped to choose which component stays
as the outer loop. With the arguments swapped so that the outer component
collapses to a single vertex, the operator raised the fatal "Case B Should
not happen!" instead of leaving an empty (edge-less) loop.
*/
TEST(PolyhedralBoundedSolidEulerOperatorsEdgeCasesTest, StrutInsideFaceLkemrArgumentsAreSwappedOuterLoopBecomesLoneVertex) {
    PolyhedralBoundedSolid* solid = createSquareLaminaWithStrut();
    _PolyhedralBoundedSolidFace* face = solid->findFace(1);
    int edgeCount = solid->getEdgesList().size();

    EXPECT_NO_THROW(Euler::lkemr(solid, face->findHalfEdge(5, 1),
        face->findHalfEdge(1, 5)));

    EXPECT_EQ(edgeCount - 1, solid->getEdgesList().size());
    EXPECT_EQ(2, face->boundariesList.size());
    std::vector<int> sizes = loopSizes(face);
    std::sort(sizes.begin(), sizes.end());
    EXPECT_EQ(std::vector<int>({1, 4}), sizes);
    delete solid;
}

/*
Defect (fixed) E-03: lkemr leaves the vertex of a single-vertex ring with a
null emanating half-edge, and the inverse lmekr does not restore it, so
lkemr -> lmekr is not an identity on the vertex back-pointers. Any later
pass that treats "null emanating half-edge" as "orphan vertex" (e.g.
remakeEmanatingHalfedgesReferences) will delete a live vertex.
*/
TEST(PolyhedralBoundedSolidEulerOperatorsEdgeCasesTest, LoneVertexRingLmekrReconnectsItEveryVertexHasALiveEmanatingHalfEdge) {
    PolyhedralBoundedSolid* solid = createSquareLaminaWithStrut();
    _PolyhedralBoundedSolidFace* face = solid->findFace(1);
    Euler::lkemr(solid, face->findHalfEdge(1, 5), face->findHalfEdge(5, 1));

    Euler::lmekr(solid,
        face->boundariesList.get(0)->halfEdgeVertices(1, 2),
        face->boundariesList.get(1)->boundaryStartHalfEdge);

    EXPECT_EQ(1, face->boundariesList.size());
    EXPECT_STREQ("", describeBrokenEmanatingReferences(solid).c_str());
    delete solid;
}

/*
Defect (fixed) E-04: lmef(he, he) is documented (after Program 11.7) as
creating a "circular" face with one edge. The container-based port inserted
both new half-edges in the old loop and returned a face whose loop is
empty.
*/
TEST(PolyhedralBoundedSolidEulerOperatorsEdgeCasesTest, SameHalfEdgeTwiceLmefNewFaceOwnsASingleSelfLoopEdge) {
    PolyhedralBoundedSolid* solid = createSquareLaminaWithStrut();
    _PolyhedralBoundedSolidFace* face = solid->findFace(1);
    _PolyhedralBoundedSolidHalfEdge* he = face->findHalfEdge(2);
    int oldLoopSize = face->boundariesList.get(0)->halfEdgesList.size();

    _PolyhedralBoundedSolidFace* newFace = Euler::lmef(solid, he, he, 9);

    ASSERT_NE(nullptr, newFace);
    _PolyhedralBoundedSolidLoop* newLoop = newFace->boundariesList.get(0);
    EXPECT_EQ(1, newLoop->halfEdgesList.size());
    EXPECT_EQ(newLoop, newLoop->boundaryStartHalfEdge->parentLoop);
    EXPECT_EQ(oldLoopSize + 1,
        face->boundariesList.get(0)->halfEdgesList.size());
    delete solid;
}

/*
Defect (fixed) E-05: after lkev removes a strut from a box face, the
surviving vertex kept an emanating half-edge that was unlisted from its
loop (dangling back-pointer). validateIntermediate still reported the solid
as valid, so the validator had a matching gap.
*/
TEST(PolyhedralBoundedSolidEulerOperatorsEdgeCasesTest, BoxWithStrutLkevRemovesStrutEveryVertexHasALiveEmanatingHalfEdge) {
    PolyhedralBoundedSolid* solid = PolyhedralBoundedSolidTestFixtures
        ::createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
    _PolyhedralBoundedSolidFace* face = solid->getPolygonsList().get(0);
    _PolyhedralBoundedSolidHalfEdge* he =
        face->boundariesList.get(0)->boundaryStartHalfEdge;
    int baseVertexId = he->startingVertex->id;
    Euler::lmev(solid, he, he, 99, Vector3Dd(0.0, 0.0, -0.5));
    EXPECT_STREQ("", describeBrokenEmanatingReferences(solid).c_str());

    Euler::lkev(solid, face->findHalfEdge(99, baseVertexId),
        face->findHalfEdge(baseVertexId, 99));

    EXPECT_EQ(8, solid->getVerticesList().size());
    EXPECT_EQ(12, solid->getEdgesList().size());
    EXPECT_STREQ("", describeBrokenEmanatingReferences(solid).c_str());
    delete solid;
}

/*
Defect (fixed) E-06: lmef did not check its precondition he1.parentLoop ==
he2.parentLoop ([MANT1988] Sec. 11.3.3). With half-edges from two different
faces it silently migrated a whole loop, returned a non-null face and left
the solid invalid. A rejected call must leave the solid untouched.
*/
TEST(PolyhedralBoundedSolidEulerOperatorsEdgeCasesTest, HalfEdgesFromDifferentLoopsLmefOperationIsRejectedWithoutSideEffects) {
    PolyhedralBoundedSolid* solid = PolyhedralBoundedSolidTestFixtures
        ::createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
    _PolyhedralBoundedSolidHalfEdge* he1 = solid->getPolygonsList().get(0)
        ->boundariesList.get(0)->boundaryStartHalfEdge;
    _PolyhedralBoundedSolidHalfEdge* he2 = solid->getPolygonsList().get(1)
        ->boundariesList.get(0)->boundaryStartHalfEdge;

    _PolyhedralBoundedSolidFace* newFace = Euler::lmef(solid, he1, he2, 50);

    EXPECT_EQ(nullptr, newFace);
    EXPECT_EQ(6, solid->getPolygonsList().size());
    EXPECT_EQ(12, solid->getEdgesList().size());
    EXPECT_TRUE(PolyhedralBoundedSolidValidationEngine::validateIntermediate(
        solid));
    delete solid;
}

/*
Control case: the documented default orientation of lkemr, where the ring
collapses to a single vertex.
*/
TEST(PolyhedralBoundedSolidEulerOperatorsEdgeCasesTest, StrutInsideFaceLkemrRingIsALoneVertexWithoutEdge) {
    PolyhedralBoundedSolid* solid = createSquareLaminaWithStrut();
    _PolyhedralBoundedSolidFace* face = solid->findFace(1);

    Euler::lkemr(solid, face->findHalfEdge(1, 5), face->findHalfEdge(5, 1));

    ASSERT_EQ(2, face->boundariesList.size());
    _PolyhedralBoundedSolidLoop* ring = face->boundariesList.get(1);
    EXPECT_EQ(1, ring->halfEdgesList.size());
    EXPECT_EQ(nullptr, ring->boundaryStartHalfEdge->parentEdge);
    EXPECT_EQ(5, ring->boundaryStartHalfEdge->startingVertex->id);
    delete solid;
}
