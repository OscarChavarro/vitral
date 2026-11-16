// C++ port of Java's SetOpResultPathTest
// (environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators).
#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "CsgSampleCorpus.h"
#include "CsgSampleCorpusFixtures.h"
#include "PolyhedralBoundedSolidTestFixtures.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"

typedef PolyhedralBoundedSolidVolumeOracle Oracle;
typedef PolyhedralBoundedSolidVolumeOracle::OrientedBox OrientedBox;
typedef PolyhedralBoundedSolidModeler Modeler;

/**
C++ helper: Java `setOp(a.toSolid(), b.toSolid(), ...)` with the operands
and the result deleted afterwards.
*/
static void
setOpOnBoxes(const OrientedBox& a, const OrientedBox& b, int op,
    BOOLEAN strict)
{
    PolyhedralBoundedSolid* solidA = a.toSolid();
    PolyhedralBoundedSolid* solidB = b.toSolid();
    PolyhedralBoundedSolid* result;

    if ( strict ) {
        result = Modeler::setOp(solidA, solidB, op);
    }
    else {
        result = Modeler::setOp(solidA, solidB, op, FALSE, TRUE, FALSE);
    }
    delete result;
    delete solidA;
    delete solidB;
}

TEST(SetOpResultPath, given_boxesInGeneralPosition_when_setOp_then_pathIsNormalPipeline)
{
    OrientedBox a(1.4, 1.2, 1.6, Oracle::translation(0, 0, 0));
    OrientedBox b(1.1, 1.5, 1.3, Oracle::translation(0.413, -0.287, 0.351));

    setOpOnBoxes(a, b, Modeler::UNION, TRUE);

    ASSERT_TRUE(Modeler::getLastSetOpResultPath() != NULL);
    EXPECT_TRUE(*Modeler::getLastSetOpResultPath() == "normal-pipeline");
}

TEST(SetOpResultPath, given_disjointBoxes_when_setOp_then_pathNamesTheNonPipelineAnswer)
{
    OrientedBox a(1, 1, 1, Oracle::translation(0, 0, 0));
    OrientedBox b(1, 1, 1, Oracle::translation(5, 0, 0));

    setOpOnBoxes(a, b, Modeler::INTERSECTION, FALSE);

    ASSERT_TRUE(Modeler::getLastSetOpResultPath() != NULL);
    EXPECT_FALSE(*Modeler::getLastSetOpResultPath() == "normal-pipeline");
}

/**
Two blocks that share part of a face (plan-stage11 Phase E). Until then
a preflight written for this case answered the intersection and the two
differences. Now generate and classify run, find no null edge, and the
"no intersections found" case of [MANT1988] 15.4 answers from the
operands as they were before generate: the differences are the
operands, with their own 8 vertices, and the intersection is the
shared part of the face as a lamina.
*/
TEST(SetOpResultPath, given_stackedBlocks_when_setOp_then_generateAndClassifyRunAndFindNoIntersection)
{
    static const int ops[2] = { PolyhedralBoundedSolidModeler::INTERSECTION,
        PolyhedralBoundedSolidModeler::SUBTRACT };
    int k;

    for ( k = 0; k < 2; k++ ) {
        int op = ops[k];
        ArrayList<PolyhedralBoundedSolid*> pair =
            CsgSampleCorpusFixtures::createPair(STACKED_BLOCKS);
        PolyhedralBoundedSolid* result = Modeler::setOp(pair[0], pair[1], op);

        EXPECT_TRUE(Modeler::getLastSetOpResultPath() != NULL &&
            *Modeler::getLastSetOpResultPath() ==
                "no-intersection-after-classify");
        if ( op == Modeler::SUBTRACT ) {
            EXPECT_EQ(8, result->getVerticesList().size());
            EXPECT_EQ(Oracle::solidVolume(pair[0]),
                Oracle::solidVolume(result));
        }
        else {
            EXPECT_EQ(2, result->getPolygonsList().size());
            EXPECT_EQ(0.0, Oracle::solidVolume(result));
        }
        delete result;
        PolyhedralBoundedSolidTestFixtures::dispose(pair);
    }
}

VITRAL_TEST_MAIN()
