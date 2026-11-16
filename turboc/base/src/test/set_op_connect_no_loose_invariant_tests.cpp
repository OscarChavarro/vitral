// C++ port of Java's SetOpConnectNoLooseInvariantTest
// (environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators).
#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "CsgSampleCorpus.h"
#include "CsgSampleCorpusFixtures.h"
#include "PolyhedralBoundedSolidTestFixtures.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetNullEdgesConnector.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/SimpleTestGeometryLibrary.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"

typedef PolyhedralBoundedSolidModeler Modeler;
typedef _PolyhedralBoundedSolidSetNullEdgesConnector Connector;

static ArrayList<PolyhedralBoundedSolid*>
createPair(const String& name)
{
    if ( name == "STACKED_BLOCKS" ) {
        return CsgSampleCorpusFixtures::createPair(STACKED_BLOCKS);
    }
    return SimpleTestGeometryLibrary::createTestObjectPairMANT1988_15_1();
}

static void
checkBaselinePair(const String& pairName, const String& opName, int op)
{
    ArrayList<PolyhedralBoundedSolid*> pair = createPair(pairName);
    PolyhedralBoundedSolid* result = Modeler::setOp(pair[0], pair[1], op,
        FALSE);

    int looseA = Connector::getLastLooseACount();
    int looseB = Connector::getLastLooseBCount();

    // looseA and looseB after Connect must be 0 per [MANT1988] Program 15.14
    printf("[%s + %s]\n", pairName.c_str(), opName.c_str());
    EXPECT_EQ(0, looseA);
    EXPECT_EQ(0, looseB);
    delete result;
    PolyhedralBoundedSolidTestFixtures::dispose(pair);
}

/**
Cases where the current connector already satisfies Program 15.14:
looseA == looseB == 0 after Connect. Locked in as regression guard; do not
move to "pending" without an investigation.
C++ port note: the Java parameterized test is a loop over its rows.
*/
TEST(SetOpConnectNoLooseInvariant, given_baselinePair_when_setopRuns_then_connectLeavesNoLooseEndpoints)
{
    checkBaselinePair("MANT1988_15_1", "UNION", Modeler::UNION);
    checkBaselinePair("STACKED_BLOCKS", "UNION", Modeler::UNION);
    checkBaselinePair("STACKED_BLOCKS", "INTERSECTION", Modeler::INTERSECTION);
    checkBaselinePair("STACKED_BLOCKS", "SUBTRACT", Modeler::SUBTRACT);
    // These two left four loose ends (an isolated strut on each
    // solid at a coplanar boundary-ray contact) until connect
    // learned to drop isolated struts (2026-11-16).
    checkBaselinePair("MANT1988_15_1", "INTERSECTION", Modeler::INTERSECTION);
    checkBaselinePair("MANT1988_15_1", "SUBTRACT", Modeler::SUBTRACT);
}

VITRAL_TEST_MAIN()
