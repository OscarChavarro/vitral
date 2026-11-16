#include <ostream>
#include <gtest/gtest.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidStructuralCopy.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "CsgSampleCorpusFixtures.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"

/*
Structural copy ([MANT1988] Sec. 15.8, "scopy"): the copy has the same
topology, geometry and identifiers as the source, passes the same
validation, and shares no node with it, so changing one leaves the other
intact.
C++ counterpart of Java's `PolyhedralBoundedSolidStructuralCopyTest`.
*/
namespace {

struct CorpusSolid
{
    CsgSampleCorpus sample;
    int which;
};

/** Test name shown by gtest and ctest (instead of the raw bytes). */
void PrintTo(const CorpusSolid& v, std::ostream* os)
{
    *os << csgSampleCorpusName(v.sample) << (v.which == 0 ? "/A" : "/B");
}

java::ArrayList<CorpusSolid> corpusSolids()
{
    java::ArrayList<CorpusSolid> rows;
    for ( int s = 0; s < CSG_SAMPLE_CORPUS_COUNT; s++ ) {
        CorpusSolid a = { CSG_SAMPLE_CORPUS_VALUES[s], 0 };
        CorpusSolid b = { CSG_SAMPLE_CORPUS_VALUES[s], 1 };
        rows.add(a);
        rows.add(b);
    }
    return rows;
}

const java::ArrayList<CorpusSolid> CORPUS_SOLIDS = corpusSolids();

/** The operand `which` of the pair; the other one is released. */
PolyhedralBoundedSolid* operandOf(CsgSampleCorpus sample, int which)
{
    java::ArrayList<PolyhedralBoundedSolid*> pair =
        CsgSampleCorpusFixtures::createPair(sample);
    delete pair[1 - which];
    return pair[which];
}

class PolyhedralBoundedSolidStructuralCopyTest :
    public ::testing::TestWithParam<CorpusSolid>
{
};

}

TEST_P(PolyhedralBoundedSolidStructuralCopyTest, CorpusSolidCopiedIsEqualAndIndependent) {
    const CorpusSolid& row = GetParam();
    PolyhedralBoundedSolid* source = operandOf(row.sample, row.which);
    java::String sourceMessage;
    bool sourceStrict = PolyhedralBoundedSolidValidationEngine
        ::validateStrict(source, &sourceMessage);

    PolyhedralBoundedSolid* copy =
        PolyhedralBoundedSolidStructuralCopy::copy(source);

    EXPECT_STREQ(PolyhedralBoundedSolidTopologySummary::from(source)
            .toString().c_str(),
        PolyhedralBoundedSolidTopologySummary::from(copy).toString().c_str());
    EXPECT_EQ(PolyhedralBoundedSolidVolumeOracle::solidVolume(source),
        PolyhedralBoundedSolidVolumeOracle::solidVolume(copy));
    java::String copyMessage;
    EXPECT_EQ(sourceStrict, PolyhedralBoundedSolidValidationEngine
        ::validateStrict(copy, &copyMessage));
    EXPECT_EQ(source->getMaxVertexId(), copy->getMaxVertexId());
    EXPECT_EQ(source->getMaxFaceId(), copy->getMaxFaceId());
    int i;
    for ( i = 0; i < source->getVerticesList().size(); i++ ) {
        EXPECT_NE(source->getVerticesList().get(i),
            copy->getVerticesList().get(i));
        EXPECT_NE(&source->getVerticesList().get(i)->position,
            &copy->getVerticesList().get(i)->position);
        EXPECT_EQ(source->getVerticesList().get(i)->id,
            copy->getVerticesList().get(i)->id);
    }
    for ( i = 0; i < source->getPolygonsList().size(); i++ ) {
        EXPECT_EQ(copy, copy->getPolygonsList().get(i)->parentSolid);
    }
    delete copy;
    delete source;
}

INSTANTIATE_TEST_SUITE_P(Corpus, PolyhedralBoundedSolidStructuralCopyTest,
    ::testing::ValuesIn(CORPUS_SOLIDS.begin(), CORPUS_SOLIDS.end()),
    [](const ::testing::TestParamInfo<CorpusSolid>& info) {
        return std::string(csgSampleCorpusName(info.param.sample)) +
            (info.param.which == 0 ? "_A" : "_B");
    });

TEST(PolyhedralBoundedSolidStructuralCopyTest, CopySourceChangedCopyIsUntouched) {
    PolyhedralBoundedSolid* source = operandOf(HOLLOW_BRICK, 0);
    PolyhedralBoundedSolid* copy =
        PolyhedralBoundedSolidStructuralCopy::copy(source);
    double volume = PolyhedralBoundedSolidVolumeOracle::solidVolume(copy);
    int faces = copy->getPolygonsList().size();
    int i;

    for ( i = 0; i < source->getVerticesList().size(); i++ ) {
        source->getVerticesList().get(i)->position = source
            ->getVerticesList().get(i)->position.multiply(3.0);
    }
    _PolyhedralBoundedSolidHalfEdge* he = source->getPolygonsList().get(0)
        ->boundariesList.get(0)->halfEdgesList.get(0);
    _PolyhedralBoundedSolidVertex* startingVertex = he->startingVertex;
    he->startingVertex = nullptr;

    EXPECT_EQ(volume, PolyhedralBoundedSolidVolumeOracle::solidVolume(copy));
    EXPECT_EQ(faces, copy->getPolygonsList().size());
    java::String message;
    EXPECT_TRUE(PolyhedralBoundedSolidValidationEngine::validateStrict(
        copy, &message));
    // C++ ownership: the source is restored before it is released
    he->startingVertex = startingVertex;
    delete copy;
    delete source;
}

TEST(PolyhedralBoundedSolidStructuralCopyTest, NullCopiedIsNull) {
    EXPECT_EQ(nullptr, PolyhedralBoundedSolidStructuralCopy::copy(nullptr));
}
