// C++ port of Java's PolyhedralBoundedSolidEulerOperatorsTest
// (environment/geometry/volume/polyhedralBoundedSolid).
#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "PolyhedralBoundedSolidTestFixtures.h"
#include "java/lang/IllegalStateException.h"
#include "java/util/ArrayList.txx"
#include "java/util/Comparator.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

typedef PolyhedralBoundedSolidEulerOperators Euler;
typedef PolyhedralBoundedSolidValidationEngine Validation;

static PolyhedralBoundedSolid*
createSkeletalSolid()
{
    PolyhedralBoundedSolid* solid = new PolyhedralBoundedSolid();
    Euler::mvfs(solid, Vector3Dd(0.0, 0.0, 0.0), 1, 1);
    return solid;
}

static PolyhedralBoundedSolid*
createThreeEdgeWire()
{
    PolyhedralBoundedSolid* solid = createSkeletalSolid();
    _PolyhedralBoundedSolidHalfEdge* he =
        solid->findFace(1)->boundariesList.get(0)->boundaryStartHalfEdge;

    Euler::lmev(solid, he, he, 2, Vector3Dd(1.0, 0.0, 0.0));
    he = solid->findVertex(2)->emanatingHalfEdge;
    Euler::lmev(solid, he, he, 3, Vector3Dd(1.0, 1.0, 0.0));
    he = solid->findVertex(3)->emanatingHalfEdge;
    Euler::lmev(solid, he, he, 4, Vector3Dd(0.0, 1.0, 0.0));
    return solid;
}

static PolyhedralBoundedSolid*
createPlanarFaceWithBridgeToHoleSeed()
{
    PolyhedralBoundedSolid* solid = createSkeletalSolid();
    Euler::lmev(solid,
        solid->findFace(1)->boundariesList.get(0)->boundaryStartHalfEdge,
        solid->findFace(1)->boundariesList.get(0)->boundaryStartHalfEdge,
        2, Vector3Dd(1.0, 0.0, 0.0));
    Euler::lmev(solid, solid->findVertex(2)->emanatingHalfEdge,
        solid->findVertex(2)->emanatingHalfEdge, 3,
        Vector3Dd(1.0, 1.0, 0.0));
    Euler::lmev(solid, solid->findVertex(3)->emanatingHalfEdge,
        solid->findVertex(3)->emanatingHalfEdge, 4,
        Vector3Dd(0.0, 1.0, 0.0));
    Euler::lmef(solid, solid->findFace(1)->findHalfEdge(4),
        solid->findFace(1)->findHalfEdge(1), 2);
    Euler::lmev(solid, solid->findFace(1)->findHalfEdge(1),
        solid->findFace(1)->findHalfEdge(1), 5,
        Vector3Dd(0.25, 0.25, 0.0));
    return solid;
}

/**
C++ port note: the Java array of two half-edges is returned through `pair`.
*/
static void
firstDistinctHalfEdgesStartingAtSameVertex(PolyhedralBoundedSolid* solid,
    _PolyhedralBoundedSolidHalfEdge* pair[2])
{
    long i;
    long j;

    for ( i = 0; i < solid->getPolygonsList().size(); i++ ) {
        _PolyhedralBoundedSolidHalfEdge* a =
            solid->getPolygonsList().get(i)->boundariesList.get(0)
                ->boundaryStartHalfEdge;
        do {
            for ( j = i; j < solid->getPolygonsList().size(); j++ ) {
                _PolyhedralBoundedSolidHalfEdge* b =
                    solid->getPolygonsList().get(j)->boundariesList.get(0)
                        ->boundaryStartHalfEdge;
                do {
                    if ( a != b && a->startingVertex == b->startingVertex ) {
                        pair[0] = a;
                        pair[1] = b;
                        return;
                    }
                    b = b->next();
                } while ( b != solid->getPolygonsList().get(j)
                    ->boundariesList.get(0)->boundaryStartHalfEdge );
            }
            a = a->next();
        } while ( a != solid->getPolygonsList().get(i)->boundariesList.get(0)
            ->boundaryStartHalfEdge );
    }
    throw IllegalStateException("Expected at least one split vertex.");
}

static _PolyhedralBoundedSolidLoop*
loopThrough(_PolyhedralBoundedSolidFace* face, int vertexId)
{
    long i;
    long j;

    for ( i = 0; i < face->boundariesList.size(); i++ ) {
        _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(i);
        for ( j = 0; j < loop->halfEdgesList.size(); j++ ) {
            if ( loop->halfEdgesList.get(j)->startingVertex->id ==
                 vertexId ) {
                return loop;
            }
        }
    }
    return NULL;
}

/**
C++ port note: the counts are `int`, as in Java (`ArrayList<long>` is not
available: its `remove(T)` collides with `remove(long)`).
*/
class TopologicalSignature
{
public:
    long faceCount;
    long edgeCount;
    long vertexCount;
    ArrayList<int> loopsPerFace;
    ArrayList<int> loopHalfEdgeCounts;

    explicit TopologicalSignature(PolyhedralBoundedSolid* solid)
    {
        long i;
        long j;

        faceCount = solid->getPolygonsList().size();
        edgeCount = solid->getEdgesList().size();
        vertexCount = solid->getVerticesList().size();

        for ( i = 0; i < solid->getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace* face =
                solid->getPolygonsList().get(i);
            loopsPerFace.add((int)face->boundariesList.size());
            for ( j = 0; j < face->boundariesList.size(); j++ ) {
                loopHalfEdgeCounts.add((int)
                    face->boundariesList.get(j)->halfEdgesList.size());
            }
        }
        loopsPerFace.sort(NaturalOrderComparator<int>());
        loopHalfEdgeCounts.sort(NaturalOrderComparator<int>());
    }

    BOOLEAN operator==(const TopologicalSignature& other) const
    {
        return faceCount == other.faceCount &&
            edgeCount == other.edgeCount &&
            vertexCount == other.vertexCount &&
            loopsPerFace.equals(other.loopsPerFace) &&
            loopHalfEdgeCounts.equals(other.loopHalfEdgeCounts);
    }
};

TEST(PolyhedralBoundedSolidEulerOperators, given_emptySolid_when_mvfs_then_createsSkeletalHalfEdgeStructure)
{
    // Arrange
    PolyhedralBoundedSolid solid;

    // Action
    Euler::mvfs(&solid, Vector3Dd(1.0, 2.0, 3.0), 7, 11);

    // Assert
    EXPECT_EQ(1, solid.getPolygonsList().size());
    EXPECT_EQ(1, solid.getVerticesList().size());
    EXPECT_EQ(0, solid.getEdgesList().size());
    EXPECT_EQ(7, solid.getMaxVertexId());
    EXPECT_EQ(11, solid.getMaxFaceId());

    _PolyhedralBoundedSolidFace* face = solid.getPolygonsList().get(0);
    _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(0);
    _PolyhedralBoundedSolidHalfEdge* halfEdge = loop->boundaryStartHalfEdge;

    EXPECT_EQ(&solid, face->parentSolid);
    EXPECT_EQ(face, loop->parentFace);
    EXPECT_EQ(1, loop->halfEdgesList.size());
    EXPECT_TRUE(halfEdge->parentEdge == NULL);
    EXPECT_EQ(loop, halfEdge->parentLoop);
    EXPECT_EQ(solid.getVerticesList().get(0), halfEdge->startingVertex);
    EXPECT_EQ(halfEdge, halfEdge->next());
    EXPECT_EQ(halfEdge, halfEdge->previous());
}

TEST(PolyhedralBoundedSolidEulerOperators, given_skeletalSolid_when_lmevReceivesSameHalfEdge_then_createsStrut)
{
    // Arrange
    PolyhedralBoundedSolid* solid = createSkeletalSolid();
    _PolyhedralBoundedSolidHalfEdge* seed =
        solid->findFace(1)->boundariesList.get(0)->boundaryStartHalfEdge;

    // Action
    Euler::lmev(solid, seed, seed, 2, Vector3Dd(1.0, 0.0, 0.0));

    // Assert
    EXPECT_EQ(1, solid->getPolygonsList().size());
    EXPECT_EQ(1, solid->getEdgesList().size());
    EXPECT_EQ(2, solid->getVerticesList().size());
    EXPECT_EQ(2, solid->findFace(1)->boundariesList.get(0)
        ->halfEdgesList.size());

    _PolyhedralBoundedSolidEdge* edge = solid->getEdgesList().get(0);
    EXPECT_TRUE(edge->leftHalf != NULL);
    EXPECT_TRUE(edge->rightHalf != NULL);
    EXPECT_EQ(edge->rightHalf->parentLoop, edge->leftHalf->parentLoop);
    EXPECT_EQ(edge->rightHalf, edge->leftHalf->mirrorHalfEdge());
    EXPECT_EQ(edge->leftHalf, edge->rightHalf->mirrorHalfEdge());
    EXPECT_EQ(2, edge->rightHalf->startingVertex->id);
    EXPECT_EQ(1, edge->leftHalf->startingVertex->id);
    delete solid;
}

TEST(PolyhedralBoundedSolidEulerOperators, given_strut_when_lkevKillsItsEdge_then_returnsToSkeletalState)
{
    // Arrange
    PolyhedralBoundedSolid* solid = createSkeletalSolid();
    _PolyhedralBoundedSolidHalfEdge* seed =
        solid->findFace(1)->boundariesList.get(0)->boundaryStartHalfEdge;
    Euler::lmev(solid, seed, seed, 2, Vector3Dd(1.0, 0.0, 0.0));
    _PolyhedralBoundedSolidEdge* edge = solid->getEdgesList().get(0);

    // Action
    Euler::lkev(solid, edge->rightHalf, edge->leftHalf);

    // Assert
    EXPECT_EQ(0, solid->getEdgesList().size());
    EXPECT_EQ(1, solid->getVerticesList().size());
    EXPECT_EQ(1, solid->getPolygonsList().size());
    EXPECT_EQ(1, solid->findFace(1)->boundariesList.get(0)
        ->halfEdgesList.size());
    EXPECT_TRUE(solid->findFace(1)->boundariesList.get(0)
        ->boundaryStartHalfEdge->parentEdge == NULL);
    delete solid;
}

TEST(PolyhedralBoundedSolidEulerOperators, given_boxVertexNeighborhood_when_lmevSplitsVertex_then_addsOneEdgeAndOneVertex)
{
    // Arrange
    PolyhedralBoundedSolid* solid =
        PolyhedralBoundedSolidTestFixtures::createBoxSolid(1.0, 1.0, 1.0,
            0.0, 0.0, 0.0);
    _PolyhedralBoundedSolidHalfEdge* pair[2];
    firstDistinctHalfEdgesStartingAtSameVertex(solid, pair);
    long faceCount = solid->getPolygonsList().size();
    long edgeCount = solid->getEdgesList().size();
    long vertexCount = solid->getVerticesList().size();
    int newVertexId = solid->getMaxVertexId() + 1;

    // Action
    Euler::lmev(solid, pair[0], pair[1], newVertexId,
        pair[0]->startingVertex->position);

    // Assert
    EXPECT_EQ(faceCount, solid->getPolygonsList().size());
    EXPECT_EQ(edgeCount + 1, solid->getEdgesList().size());
    EXPECT_EQ(vertexCount + 1, solid->getVerticesList().size());
    EXPECT_TRUE(solid->findVertex(newVertexId) != NULL);
    EXPECT_TRUE(Validation::validateIntermediate(solid));
    delete solid;
}

TEST(PolyhedralBoundedSolidEulerOperators, given_openWire_when_lmefClosesLoop_then_addsOneEdgeAndOneFace)
{
    // Arrange
    PolyhedralBoundedSolid* solid = createThreeEdgeWire();
    _PolyhedralBoundedSolidFace* face = solid->findFace(1);
    _PolyhedralBoundedSolidHalfEdge* he1 = face->findHalfEdge(4);
    _PolyhedralBoundedSolidHalfEdge* he2 = face->findHalfEdge(1);
    long edgeCount = solid->getEdgesList().size();
    long vertexCount = solid->getVerticesList().size();

    // Action
    _PolyhedralBoundedSolidFace* newFace = Euler::lmef(solid, he1, he2, 2);

    // Assert
    ASSERT_TRUE(newFace != NULL);
    EXPECT_EQ(2, solid->getPolygonsList().size());
    EXPECT_EQ(edgeCount + 1, solid->getEdgesList().size());
    EXPECT_EQ(vertexCount, solid->getVerticesList().size());
    EXPECT_EQ(1, newFace->boundariesList.size());
    EXPECT_GE(newFace->boundariesList.get(0)->halfEdgesList.size(), 1);
    delete solid;
}

TEST(PolyhedralBoundedSolidEulerOperators, given_lmefResult_when_lkefKillsCreatedEdge_then_mergesFaces)
{
    // Arrange
    PolyhedralBoundedSolid* solid = createThreeEdgeWire();
    _PolyhedralBoundedSolidFace* face = solid->findFace(1);
    _PolyhedralBoundedSolidFace* newFace = Euler::lmef(solid,
        face->findHalfEdge(4), face->findHalfEdge(1), 2);
    _PolyhedralBoundedSolidEdge* edgeToKill =
        newFace->boundariesList.get(0)->boundaryStartHalfEdge->parentEdge;
    long edgeCount = solid->getEdgesList().size();

    // Action
    Euler::lkef(solid, edgeToKill->rightHalf, edgeToKill->leftHalf);

    // Assert
    EXPECT_EQ(1, solid->getPolygonsList().size());
    EXPECT_EQ(edgeCount - 1, solid->getEdgesList().size());
    EXPECT_TRUE(Validation::validateIntermediate(solid));
    delete solid;
}

TEST(PolyhedralBoundedSolidEulerOperators, given_faceWithBridgeEdge_when_lkemrKillsEdge_then_createsRing)
{
    // Arrange
    PolyhedralBoundedSolid* solid = createPlanarFaceWithBridgeToHoleSeed();
    _PolyhedralBoundedSolidFace* face = solid->findFace(1);
    _PolyhedralBoundedSolidHalfEdge* he1 = face->findHalfEdge(1, 5);
    _PolyhedralBoundedSolidHalfEdge* he2 = face->findHalfEdge(5, 1);
    long edgeCount = solid->getEdgesList().size();
    long loopCount = face->boundariesList.size();

    // Action
    Euler::lkemr(solid, he1, he2);

    // Assert
    EXPECT_EQ(edgeCount - 1, solid->getEdgesList().size());
    EXPECT_EQ(loopCount + 1, face->boundariesList.size());
    delete solid;
}

TEST(PolyhedralBoundedSolidEulerOperators, given_lkemrResult_when_lmekrConnectsLoops_then_restoresSingleLoop)
{
    // Arrange
    PolyhedralBoundedSolid* solid = createPlanarFaceWithBridgeToHoleSeed();
    _PolyhedralBoundedSolidFace* face = solid->findFace(1);
    _PolyhedralBoundedSolidHalfEdge* he1 = face->findHalfEdge(1, 5);
    _PolyhedralBoundedSolidHalfEdge* he2 = face->findHalfEdge(5, 1);
    Euler::lkemr(solid, he1, he2);
    long edgeCount = solid->getEdgesList().size();
    _PolyhedralBoundedSolidLoop* outer = face->boundariesList.get(0);
    _PolyhedralBoundedSolidLoop* ring = face->boundariesList.get(1);

    // Action
    Euler::lmekr(solid, outer->boundaryStartHalfEdge,
        ring->boundaryStartHalfEdge);

    // Assert
    EXPECT_EQ(edgeCount + 1, solid->getEdgesList().size());
    EXPECT_EQ(1, face->boundariesList.size());
    delete solid;
}

/**
lmekr removes the loop of its second half-edge. When that loop is the
outer loop of the face (the first half-edge is on a ring), the merged
loop is the outer boundary and must come first. It used to stay in the
place of the ring, so that in a face with another ring that other ring
was taken for the outer loop. The connect stage of the set operations
does exactly this when a face is crossed by several chords that pass
through vertices inside the face (plan-stage11 Phase F): every later
decision about where a pending ring belongs was then taken against the
wrong loop.
*/
TEST(PolyhedralBoundedSolidEulerOperators, given_faceWithTwoRings_when_lmekrMergesTheOuterLoopIntoARing_then_mergedLoopIsTheOuterLoop)
{
    // Arrange: a square face with two strut rings, {5,6} and {7,8}.
    PolyhedralBoundedSolid* solid = createPlanarFaceWithBridgeToHoleSeed();
    _PolyhedralBoundedSolidFace* face = solid->findFace(1);
    Euler::lmev(solid,
        solid->findVertex(5)->emanatingHalfEdge,
        solid->findVertex(5)->emanatingHalfEdge, 6,
        Vector3Dd(0.4, 0.3, 0.0));
    Euler::lkemr(solid,
        face->findHalfEdge(1, 5), face->findHalfEdge(5, 1));
    Euler::lmev(solid,
        face->findHalfEdge(3), face->findHalfEdge(3), 7,
        Vector3Dd(0.75, 0.75, 0.0));
    Euler::lmev(solid,
        solid->findVertex(7)->emanatingHalfEdge,
        solid->findVertex(7)->emanatingHalfEdge, 8,
        Vector3Dd(0.6, 0.7, 0.0));
    Euler::lkemr(solid,
        face->findHalfEdge(3, 7), face->findHalfEdge(7, 3));
    ASSERT_EQ(3, face->boundariesList.size());
    _PolyhedralBoundedSolidLoop* outer = face->boundariesList.get(0);
    _PolyhedralBoundedSolidLoop* firstRing = loopThrough(face, 5);
    _PolyhedralBoundedSolidLoop* secondRing = loopThrough(face, 7);
    ASSERT_NE(outer, firstRing);
    ASSERT_NE(secondRing, firstRing);

    // Action: the ring half-edge first, the outer half-edge second.
    Euler::lmekr(solid,
        firstRing->boundaryStartHalfEdge, outer->boundaryStartHalfEdge);

    // Assert
    EXPECT_EQ(2, face->boundariesList.size());
    EXPECT_EQ(firstRing, face->boundariesList.get(0));
    EXPECT_EQ(secondRing, face->boundariesList.get(1));
    EXPECT_EQ(face->boundariesList.get(0), loopThrough(face, 1));
    EXPECT_EQ(face->boundariesList.get(0), loopThrough(face, 3));
    EXPECT_TRUE(Validation::validateIntermediate(solid));
    delete solid;
}

TEST(PolyhedralBoundedSolidEulerOperators, given_faceWithRing_when_lringmvReordersOuterLoop_then_keepsSameFace)
{
    // Arrange
    PolyhedralBoundedSolid* solid = createPlanarFaceWithBridgeToHoleSeed();
    _PolyhedralBoundedSolidFace* face = solid->findFace(1);
    Euler::lkemr(solid, face->findHalfEdge(1, 5), face->findHalfEdge(5, 1));
    _PolyhedralBoundedSolidLoop* ring = face->boundariesList.get(1);

    // Action
    BOOLEAN movedToOuter = Euler::lringmv(solid, ring, face, TRUE);
    BOOLEAN ringBecameOuter = (face->boundariesList.get(0) == ring);
    BOOLEAN movedBackToInner = Euler::lringmv(solid, ring, face, FALSE);

    // Assert
    EXPECT_TRUE(movedToOuter);
    EXPECT_TRUE(ringBecameOuter);
    EXPECT_TRUE(movedBackToInner);
    EXPECT_NE(ring, face->boundariesList.get(0));
    EXPECT_EQ(face, ring->parentFace);
    delete solid;
}

TEST(PolyhedralBoundedSolidEulerOperators, given_faceWithRing_when_lringmvMovesLoopToAnotherFace_then_updatesOwnership)
{
    // Arrange
    PolyhedralBoundedSolid* solid = createPlanarFaceWithBridgeToHoleSeed();
    _PolyhedralBoundedSolidFace* sourceFace = solid->findFace(1);
    _PolyhedralBoundedSolidFace* targetFace = solid->findFace(2);
    Euler::lkemr(solid, sourceFace->findHalfEdge(1, 5),
        sourceFace->findHalfEdge(5, 1));
    _PolyhedralBoundedSolidLoop* ring = sourceFace->boundariesList.get(1);
    long sourceLoopCount = sourceFace->boundariesList.size();
    long targetLoopCount = targetFace->boundariesList.size();

    // Action
    BOOLEAN result = Euler::lringmv(solid, ring, targetFace, FALSE);

    // Assert
    EXPECT_TRUE(result);
    EXPECT_EQ(sourceLoopCount - 1, sourceFace->boundariesList.size());
    EXPECT_EQ(targetLoopCount + 1, targetFace->boundariesList.size());
    EXPECT_EQ(targetFace, ring->parentFace);
    EXPECT_NE(ring, targetFace->boundariesList.get(0));
    delete solid;
}

TEST(PolyhedralBoundedSolidEulerOperators, given_skeletalSolid_when_kvfs_then_clearsSolid)
{
    // Arrange
    PolyhedralBoundedSolid* solid = createSkeletalSolid();

    // Action
    Euler::kvfs(solid);

    // Assert
    EXPECT_EQ(0, solid->getPolygonsList().size());
    EXPECT_EQ(0, solid->getEdgesList().size());
    EXPECT_EQ(0, solid->getVerticesList().size());
    delete solid;
}

TEST(PolyhedralBoundedSolidEulerOperators, given_skeletalSolid_when_smev_then_delegatesToRobustLmev)
{
    // Arrange
    PolyhedralBoundedSolid* solid = createSkeletalSolid();

    // Action
    BOOLEAN result = Euler::smev(solid, 1, 1, 2, Vector3Dd(1.0, 0.0, 0.0));

    // Assert
    EXPECT_TRUE(result);
    EXPECT_EQ(1, solid->getPolygonsList().size());
    EXPECT_EQ(1, solid->getEdgesList().size());
    EXPECT_EQ(2, solid->getVerticesList().size());
    delete solid;
}

TEST(PolyhedralBoundedSolidEulerOperators, given_boxVertexNeighborhood_when_mev_then_splitsVertex)
{
    // Arrange
    PolyhedralBoundedSolid* solid =
        PolyhedralBoundedSolidTestFixtures::createBoxSolid(1.0, 1.0, 1.0,
            0.0, 0.0, 0.0);
    _PolyhedralBoundedSolidHalfEdge* pair[2];
    firstDistinctHalfEdgesStartingAtSameVertex(solid, pair);
    long vertexCount = solid->getVerticesList().size();
    long edgeCount = solid->getEdgesList().size();
    int newVertexId = solid->getMaxVertexId() + 1;

    // Action
    BOOLEAN result = Euler::mev(solid,
        pair[0]->parentLoop->parentFace->id,
        pair[1]->parentLoop->parentFace->id,
        pair[0]->startingVertex->id,
        pair[0]->next()->startingVertex->id,
        pair[1]->next()->startingVertex->id,
        newVertexId,
        pair[0]->startingVertex->position);

    // Assert
    EXPECT_TRUE(result);
    EXPECT_EQ(vertexCount + 1, solid->getVerticesList().size());
    EXPECT_EQ(edgeCount + 1, solid->getEdgesList().size());
    EXPECT_TRUE(solid->findVertex(newVertexId) != NULL);
    EXPECT_TRUE(Validation::validateIntermediate(solid));
    delete solid;
}

TEST(PolyhedralBoundedSolidEulerOperators, given_openWire_when_smef_then_closesLoop)
{
    // Arrange
    PolyhedralBoundedSolid* solid = createThreeEdgeWire();

    // Action
    BOOLEAN result = Euler::smef(solid, 1, 4, 1, 2);

    // Assert
    EXPECT_TRUE(result);
    EXPECT_EQ(2, solid->getPolygonsList().size());
    EXPECT_EQ(4, solid->getEdgesList().size());
    delete solid;
}

TEST(PolyhedralBoundedSolidEulerOperators, given_openWire_when_mef_then_closesLoop)
{
    // Arrange
    PolyhedralBoundedSolid* solid = createThreeEdgeWire();
    _PolyhedralBoundedSolidFace* face = solid->findFace(1);
    _PolyhedralBoundedSolidHalfEdge* he1 = face->findHalfEdge(4);
    _PolyhedralBoundedSolidHalfEdge* he2 = face->findHalfEdge(1);

    // Action
    BOOLEAN result = Euler::mef(solid, 1, 1,
        he1->startingVertex->id, he1->next()->startingVertex->id,
        he2->startingVertex->id, he2->next()->startingVertex->id, 2);

    // Assert
    EXPECT_TRUE(result);
    EXPECT_EQ(2, solid->getPolygonsList().size());
    EXPECT_EQ(4, solid->getEdgesList().size());
    delete solid;
}

TEST(PolyhedralBoundedSolidEulerOperators, given_faceWithBridgeEdge_when_kemr_then_createsRing)
{
    // Arrange
    PolyhedralBoundedSolid* solid = createPlanarFaceWithBridgeToHoleSeed();
    _PolyhedralBoundedSolidFace* face = solid->findFace(1);
    long loopCount = face->boundariesList.size();

    // Action
    BOOLEAN result = Euler::kemr(solid, 1, 1, 1, 5, 5, 1);

    // Assert
    EXPECT_TRUE(result);
    EXPECT_EQ(loopCount + 1, face->boundariesList.size());
    delete solid;
}

TEST(PolyhedralBoundedSolidEulerOperators, given_twoFaces_when_kfmrh_then_secondFaceBecomesInnerLoop)
{
    // Arrange
    PolyhedralBoundedSolid* solid =
        PolyhedralBoundedSolidTestFixtures::createBoxSolid(1.0, 1.0, 1.0,
            0.0, 0.0, 0.0);
    _PolyhedralBoundedSolidFace* face1 = solid->getPolygonsList().get(0);
    _PolyhedralBoundedSolidFace* face2 = solid->getPolygonsList().get(1);
    long faceCount = solid->getPolygonsList().size();
    long loopCount = face1->boundariesList.size();
    // C++ port note: kfmrh deletes face2, so its id and loop count are read
    // first (Java reads them from the unlisted face afterwards).
    int face2Id = face2->id;
    long face2LoopCount = face2->boundariesList.size();

    // Action
    BOOLEAN result = Euler::kfmrh(solid, face1->id, face2Id);

    // Assert
    EXPECT_TRUE(result);
    EXPECT_EQ(faceCount - 1, solid->getPolygonsList().size());
    EXPECT_EQ(loopCount + face2LoopCount, face1->boundariesList.size());
    EXPECT_TRUE(solid->findFace(face2Id) == NULL);
    delete solid;
}

TEST(PolyhedralBoundedSolidEulerOperators, given_boxNeighborhood_when_lmevThenLkev_then_restoresTopologicalIdentity)
{
    // Arrange
    PolyhedralBoundedSolid* solid =
        PolyhedralBoundedSolidTestFixtures::createBoxSolid(1.0, 1.0, 1.0,
            0.0, 0.0, 0.0);
    TopologicalSignature before(solid);
    _PolyhedralBoundedSolidHalfEdge* pair[2];
    firstDistinctHalfEdgesStartingAtSameVertex(solid, pair);
    int newVertexId = solid->getMaxVertexId() + 1;

    // Action
    Euler::lmev(solid, pair[0], pair[1], newVertexId,
        pair[0]->startingVertex->position);
    _PolyhedralBoundedSolidHalfEdge* newVertexHalfEdge =
        solid->findVertex(newVertexId)->emanatingHalfEdge;
    Euler::lkev(solid, newVertexHalfEdge,
        newVertexHalfEdge->mirrorHalfEdge());

    // Assert
    EXPECT_TRUE(TopologicalSignature(solid) == before);
    EXPECT_TRUE(Validation::validateIntermediate(solid));
    delete solid;
}

TEST(PolyhedralBoundedSolidEulerOperators, given_openWire_when_lmefThenLkef_then_restoresTopologicalIdentity)
{
    // Arrange
    PolyhedralBoundedSolid* solid = createThreeEdgeWire();
    TopologicalSignature before(solid);
    _PolyhedralBoundedSolidFace* face = solid->findFace(1);
    _PolyhedralBoundedSolidFace* newFace = Euler::lmef(solid,
        face->findHalfEdge(4), face->findHalfEdge(1), 2);
    _PolyhedralBoundedSolidEdge* edgeToKill =
        newFace->boundariesList.get(0)->boundaryStartHalfEdge->parentEdge;

    // Action
    Euler::lkef(solid, edgeToKill->rightHalf, edgeToKill->leftHalf);

    // Assert
    EXPECT_TRUE(TopologicalSignature(solid) == before);
    EXPECT_TRUE(Validation::validateIntermediate(solid));
    delete solid;
}

TEST(PolyhedralBoundedSolidEulerOperators, given_faceWithBridge_when_lkemrThenLmekr_then_restoresTopologicalIdentity)
{
    // Arrange
    PolyhedralBoundedSolid* solid = createPlanarFaceWithBridgeToHoleSeed();
    TopologicalSignature before(solid);
    _PolyhedralBoundedSolidFace* face = solid->findFace(1);
    Euler::lkemr(solid, face->findHalfEdge(1, 5), face->findHalfEdge(5, 1));
    _PolyhedralBoundedSolidLoop* outer = face->boundariesList.get(0);
    _PolyhedralBoundedSolidLoop* ring = face->boundariesList.get(1);

    // Action
    Euler::lmekr(solid, outer->boundaryStartHalfEdge,
        ring->boundaryStartHalfEdge);

    // Assert
    EXPECT_TRUE(TopologicalSignature(solid) == before);
    EXPECT_TRUE(Validation::validateIntermediate(solid));
    delete solid;
}

TEST(PolyhedralBoundedSolidEulerOperators, given_twoFaces_when_lkfmrhThenLmfkrh_then_restoresTopologicalIdentity)
{
    // Arrange
    PolyhedralBoundedSolid* solid =
        PolyhedralBoundedSolidTestFixtures::createBoxSolid(1.0, 1.0, 1.0,
            0.0, 0.0, 0.0);
    TopologicalSignature before(solid);
    _PolyhedralBoundedSolidFace* face1 = solid->getPolygonsList().get(0);
    _PolyhedralBoundedSolidFace* face2 = solid->getPolygonsList().get(1);
    int newFaceId = solid->getMaxFaceId() + 1;

    // Action
    Euler::lkfmrh(solid, face1, face2);
    _PolyhedralBoundedSolidLoop* loopToPromote =
        face1->boundariesList.get(face1->boundariesList.size() - 1);
    Euler::lmfkrh(solid, loopToPromote, newFaceId);

    // Assert
    EXPECT_TRUE(TopologicalSignature(solid) == before);
    EXPECT_TRUE(Validation::validateIntermediate(solid));
    delete solid;
}

TEST(PolyhedralBoundedSolidEulerOperators, given_skeletalSolid_when_twoStrutsThenLmef_then_createsTriangularLaminaTopology)
{
    // Arrange
    PolyhedralBoundedSolid* solid = createSkeletalSolid();
    _PolyhedralBoundedSolidHalfEdge* seed =
        solid->findFace(1)->boundariesList.get(0)->boundaryStartHalfEdge;

    // Action
    Euler::lmev(solid, seed, seed, 2, Vector3Dd(1.0, 0.0, 0.0));
    Euler::lmev(solid, solid->findVertex(2)->emanatingHalfEdge,
        solid->findVertex(2)->emanatingHalfEdge, 3,
        Vector3Dd(0.0, 1.0, 0.0));
    Euler::lmef(solid, solid->findFace(1)->findHalfEdge(3),
        solid->findFace(1)->findHalfEdge(1), 2);

    // Assert
    EXPECT_EQ(2, solid->getPolygonsList().size());
    EXPECT_EQ(3, solid->getEdgesList().size());
    EXPECT_EQ(3, solid->getVerticesList().size());
    TopologicalSignature signature(solid);
    ASSERT_EQ(2, signature.loopHalfEdgeCounts.size());
    EXPECT_EQ(3, signature.loopHalfEdgeCounts[0]);
    EXPECT_EQ(3, signature.loopHalfEdgeCounts[1]);
    EXPECT_TRUE(Validation::validateIntermediate(solid));
    delete solid;
}

VITRAL_TEST_MAIN()
