#include <gtest/gtest.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "CsgSampleCorpusFixtures.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"

/*
The result path names which code produced a boolean result: the [MANT1988]
Ch. 15 pipeline ("normal-pipeline") or one of the preflights in front of it
(contact, identity, containment). Tests can then tell a pipeline answer
from a replacement (Fable5.2Analysis.md Stage A step 4).
C++ counterpart of Java's `SetOpResultPathTest`.
*/
namespace {

typedef PolyhedralBoundedSolidModeler Modeler;
typedef PolyhedralBoundedSolidVolumeOracle Oracle;

/** Java `setOp(a.toSolid(), b.toSolid(), ...)`; everything released. */
void runSetOp(const Oracle::OrientedBox& a, const Oracle::OrientedBox& b,
    int op, bool strict)
{
    PolyhedralBoundedSolid* solidA = a.toSolid();
    PolyhedralBoundedSolid* solidB = b.toSolid();
    PolyhedralBoundedSolid* result = Modeler::setOp(solidA, solidB, op,
        false, true, strict);
    delete result;
    delete solidA;
    delete solidB;
}

}

TEST(SetOpResultPathTest, BoxesInGeneralPositionSetOpPathIsNormalPipeline) {
    Oracle::OrientedBox a(1.4, 1.2, 1.6, Oracle::translation(0, 0, 0));
    Oracle::OrientedBox b(1.1, 1.5, 1.3,
        Oracle::translation(0.413, -0.287, 0.351));
    runSetOp(a, b, Modeler::UNION, true);
    const java::String* path = Modeler::getLastSetOpResultPath();
    ASSERT_NE(nullptr, path);
    EXPECT_STREQ("normal-pipeline", path->c_str());
}

TEST(SetOpResultPathTest, DisjointBoxesSetOpPathNamesTheNonPipelineAnswer) {
    Oracle::OrientedBox a(1, 1, 1, Oracle::translation(0, 0, 0));
    Oracle::OrientedBox b(1, 1, 1, Oracle::translation(5, 0, 0));
    runSetOp(a, b, Modeler::INTERSECTION, false);
    const java::String* path = Modeler::getLastSetOpResultPath();
    ASSERT_NE(nullptr, path);
    EXPECT_STRNE("normal-pipeline", path->c_str());
}

/*
Two blocks that share part of a face (plan-stage11 Phase E). Until then a
preflight written for this case answered the intersection and the two
differences. Now generate and classify run, find no null edge, and the
"no intersections found" case of [MANT1988] 15.4 answers from the operands
as they were before generate: the differences are the operands, with their
own 8 vertices, and the intersection is the shared part of the face as a
lamina.
*/
TEST(SetOpResultPathTest, StackedBlocksSetOpGenerateAndClassifyRunAndFindNoIntersection) {
    const int ops[] = { Modeler::INTERSECTION, Modeler::SUBTRACT };
    for ( int op : ops ) {
        java::ArrayList<PolyhedralBoundedSolid*> pair =
            CsgSampleCorpusFixtures::createPair(STACKED_BLOCKS);
        PolyhedralBoundedSolid* result = Modeler::setOp(pair[0], pair[1], op);
        const java::String* path = Modeler::getLastSetOpResultPath();
        ASSERT_NE(nullptr, path);
        EXPECT_STREQ("no-intersection-after-classify", path->c_str());
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
        delete pair[0];
        delete pair[1];
    }
}
