// C++ port of Java's PolyhedralBoundedSolidStructuralCopyTest
// (environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators).
#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "CsgSampleCorpus.h"
#include "CsgSampleCorpusFixtures.h"
#include "PolyhedralBoundedSolidTestFixtures.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidStructuralCopy.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

typedef PolyhedralBoundedSolidVolumeOracle Oracle;
typedef PolyhedralBoundedSolidValidationEngine Validation;

static void
checkCorpusSolid(CsgSampleCorpus sample, int which)
{
    ArrayList<PolyhedralBoundedSolid*> pair =
        CsgSampleCorpusFixtures::createPair(sample);
    PolyhedralBoundedSolid* source = pair[which];
    String message;
    BOOLEAN sourceStrict = Validation::validateStrict(source, &message);

    PolyhedralBoundedSolid* copy =
        PolyhedralBoundedSolidStructuralCopy::copy(source);

    printf("%s/%c\n", csgSampleCorpusName(sample), which == 0 ? 'A' : 'B');
    EXPECT_TRUE(PolyhedralBoundedSolidTopologySummary::from(copy).toString() ==
        PolyhedralBoundedSolidTopologySummary::from(source).toString());
    EXPECT_EQ(Oracle::solidVolume(source), Oracle::solidVolume(copy));
    message = "";
    EXPECT_EQ(sourceStrict, Validation::validateStrict(copy, &message));
    EXPECT_EQ(source->getMaxVertexId(), copy->getMaxVertexId());
    EXPECT_EQ(source->getMaxFaceId(), copy->getMaxFaceId());
    long i;
    for ( i = 0; i < source->getVerticesList().size(); i++ ) {
        EXPECT_NE(source->getVerticesList().get(i),
            copy->getVerticesList().get(i));
        // C++ port note: the positions are values, so "not the same
        // object" is the distinct address of each vertex member
        EXPECT_NE(&source->getVerticesList().get(i)->position,
            &copy->getVerticesList().get(i)->position);
        EXPECT_EQ(source->getVerticesList().get(i)->id,
            copy->getVerticesList().get(i)->id);
    }
    for ( i = 0; i < source->getPolygonsList().size(); i++ ) {
        EXPECT_EQ(copy, copy->getPolygonsList().get(i)->parentSolid);
    }
    delete copy;
    PolyhedralBoundedSolidTestFixtures::dispose(pair);
}

/**
C++ port note: the Java parameterized test (one row per corpus sample and
operand, named "SAMPLE/A" or "SAMPLE/B") is a loop that prints each name.
*/
TEST(PolyhedralBoundedSolidStructuralCopy, given_corpusSolid_when_copied_then_copyIsEqualAndIndependent)
{
    int sampleIndex;

    for ( sampleIndex = 0; sampleIndex < CSG_SAMPLE_CORPUS_COUNT;
          sampleIndex++ ) {
        checkCorpusSolid((CsgSampleCorpus)sampleIndex, 0);
        checkCorpusSolid((CsgSampleCorpus)sampleIndex, 1);
    }
}

TEST(PolyhedralBoundedSolidStructuralCopy, given_copy_when_sourceIsChanged_then_copyIsUntouched)
{
    ArrayList<PolyhedralBoundedSolid*> pair =
        CsgSampleCorpusFixtures::createPair(HOLLOW_BRICK);
    PolyhedralBoundedSolid* source = pair[0];
    PolyhedralBoundedSolid* copy =
        PolyhedralBoundedSolidStructuralCopy::copy(source);
    double volume = Oracle::solidVolume(copy);
    long faces = copy->getPolygonsList().size();
    long i;

    for ( i = 0; i < source->getVerticesList().size(); i++ ) {
        source->getVerticesList().get(i)->position = source->getVerticesList()
            .get(i)->position.multiply(3.0);
    }
    _PolyhedralBoundedSolidHalfEdge* broken = source->getPolygonsList()
        .get(0)->boundariesList.get(0)->halfEdgesList.get(0);
    _PolyhedralBoundedSolidVertex* keep = broken->startingVertex;
    broken->startingVertex = NULL;

    EXPECT_EQ(volume, Oracle::solidVolume(copy));
    EXPECT_EQ(faces, copy->getPolygonsList().size());
    String message;
    EXPECT_TRUE(Validation::validateStrict(copy, &message));

    // C++ port note: the vertex is put back so the source can be deleted
    broken->startingVertex = keep;
    delete copy;
    PolyhedralBoundedSolidTestFixtures::dispose(pair);
}

TEST(PolyhedralBoundedSolidStructuralCopy, given_null_when_copied_then_null)
{
    EXPECT_TRUE(PolyhedralBoundedSolidStructuralCopy::copy(NULL) == NULL);
}

VITRAL_TEST_MAIN()
