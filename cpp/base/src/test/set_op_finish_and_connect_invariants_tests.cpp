#include <ostream>
#include <gtest/gtest.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetFinisher.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/topology/_PolyhedralBoundedSolidSetNullEdgesConnector.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/SimpleTestGeometryLibrary.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "CsgSampleCorpusFixtures.h"
#include "PolyhedralBoundedSolidTestFixtures.h"

/*
C++ counterparts of Java's `SetOpFinishInvariantsTest` and
`SetOpConnectNoLooseInvariantTest`, which had none before. Each case builds
its own operands and releases them with the result.
*/
namespace {

typedef PolyhedralBoundedSolidModeler Modeler;
typedef PolyhedralBoundedSolidTestFixtures Fixtures;

void releaseAll(java::ArrayList<PolyhedralBoundedSolid*>& pair,
    PolyhedralBoundedSolid* result)
{
    delete result;
    delete pair[0];
    delete pair[1];
}

//-------------------------------------------------------------------------
// SetOpFinishInvariantsTest
//
// Regression guard for the Finisher invariants:
// - 9.1: sanitizePairedFaces pairs null faces by the index connect gave
//   them. The old fallback to list order is gone: an unpaired set of null
//   faces throws SolidOperationFailedException, so the baseline fixtures
//   must finish without it.
// - 9.2: triangulateNonPlanarFaces must not triangulate any face in the
//   baseline fixtures that already pass validation. A non-zero count
//   indicates the loopGlue produced a non-planar face, which is a signal
//   for 9 follow-up work.
// Traceability: plan-csg-boolean-fix-stage2.md 9.1 and 9.2.

struct FinishFixture
{
    const char* label;
    int pair;
    int operation;
};

/** Test name shown by gtest and ctest (instead of the raw bytes). */
void PrintTo(const FinishFixture& v, std::ostream* os)
{
    *os << v.label;
}

java::ArrayList<PolyhedralBoundedSolid*> finishPair(int pair)
{
    if ( pair == 0 ) {
        return Fixtures::createMant1986_2Pair();
    }
    if ( pair == 1 ) {
        return Fixtures::createMant1988_15_2Pair(0);
    }
    return Fixtures::createMant1988_6_13Pair();
}

const FinishFixture BASELINE_FIXTURES[] = {
    { "MANT1986_2 UNION", 0, PolyhedralBoundedSolidModeler::UNION },
    { "MANT1986_2 INTERSECTION", 0,
        PolyhedralBoundedSolidModeler::INTERSECTION },
    { "MANT1986_2 SUBTRACT", 0, PolyhedralBoundedSolidModeler::SUBTRACT },
    { "MANT1988_15_2 UNION", 1, PolyhedralBoundedSolidModeler::UNION },
    { "MANT1988_6_13 SUBTRACT", 2, PolyhedralBoundedSolidModeler::SUBTRACT }
};

std::string fixtureName(const ::testing::TestParamInfo<FinishFixture>& info)
{
    std::string name;
    for ( const char* c = info.param.label; *c != '\0'; c++ ) {
        name += std::isalnum((unsigned char)*c) ? *c : '_';
    }
    return name;
}

class SetOpFinishInvariantsTest :
    public ::testing::TestWithParam<FinishFixture>
{
};

//-------------------------------------------------------------------------
// SetOpConnectNoLooseInvariantTest
//
// Acceptance test for the setopconnect contract from [MANT1988] 15.7,
// Program 15.14 (sgetnextnulledge / scanjoin / join / cuta / cutb): when
// the loop terminates, every null-edge endpoint has been joined into the
// active topology and no "loose" half-edges remain pending: looseA = looseB
// = 0. Every case of the matrix must close: the cases that used to leave
// loose endpoints (MANT1988_15_1 intersection and difference) close since
// connect drops isolated struts.

struct ConnectCase
{
    const char* pairName;
    const char* opName;
    int operation;
};

/** Test name shown by gtest and ctest (instead of the raw bytes). */
void PrintTo(const ConnectCase& v, std::ostream* os)
{
    *os << v.pairName << " " << v.opName;
}

const ConnectCase BASELINE_CASES[] = {
    { "MANT1988_15_1", "UNION", PolyhedralBoundedSolidModeler::UNION },
    { "STACKED_BLOCKS", "UNION", PolyhedralBoundedSolidModeler::UNION },
    { "STACKED_BLOCKS", "INTERSECTION",
        PolyhedralBoundedSolidModeler::INTERSECTION },
    { "STACKED_BLOCKS", "SUBTRACT", PolyhedralBoundedSolidModeler::SUBTRACT },
    // These two left four loose ends (an isolated strut on each solid at a
    // coplanar boundary-ray contact) until connect learned to drop isolated
    // struts (2026-11-16).
    { "MANT1988_15_1", "INTERSECTION",
        PolyhedralBoundedSolidModeler::INTERSECTION },
    { "MANT1988_15_1", "SUBTRACT", PolyhedralBoundedSolidModeler::SUBTRACT }
};

java::ArrayList<PolyhedralBoundedSolid*> connectPair(const char* name)
{
    if ( java::String(name) == "STACKED_BLOCKS" ) {
        return CsgSampleCorpusFixtures::createPair(STACKED_BLOCKS);
    }
    return SimpleTestGeometryLibrary::createTestObjectPairMANT1988_15_1();
}

class SetOpConnectNoLooseInvariantTest :
    public ::testing::TestWithParam<ConnectCase>
{
};

}

TEST_P(SetOpFinishInvariantsTest, BaselineFixtureSetopRunsNullFacesArePairedByIndex) {
    const FinishFixture& row = GetParam();
    java::ArrayList<PolyhedralBoundedSolid*> pair = finishPair(row.pair);
    PolyhedralBoundedSolid* result = nullptr;
    EXPECT_NO_THROW(result = Modeler::setOp(pair[0], pair[1], row.operation, false,
        true, false))
        << "[" << row.label << "] finish must pair every null face by index";
    releaseAll(pair, result);
}

TEST_P(SetOpFinishInvariantsTest, BaselineFixtureSetopRunsNoNonPlanarFaceTriangulated) {
    const FinishFixture& row = GetParam();
    java::ArrayList<PolyhedralBoundedSolid*> pair = finishPair(row.pair);
    PolyhedralBoundedSolid* result = Modeler::setOp(pair[0], pair[1], row.operation,
        false, true, false);
    EXPECT_EQ(0, _PolyhedralBoundedSolidSetFinisher
        ::getLastTriangulatedFaceCount())
        << "[" << row.label << "] triangulateNonPlanarFaces must split 0 "
        << "faces in clean baseline";
    releaseAll(pair, result);
}

INSTANTIATE_TEST_SUITE_P(Baseline, SetOpFinishInvariantsTest,
    ::testing::ValuesIn(BASELINE_FIXTURES), fixtureName);

TEST_P(SetOpConnectNoLooseInvariantTest, BaselinePairSetopRunsConnectLeavesNoLooseEndpoints) {
    const ConnectCase& row = GetParam();
    java::ArrayList<PolyhedralBoundedSolid*> pair = connectPair(row.pairName);
    PolyhedralBoundedSolid* result = Modeler::setOp(pair[0], pair[1], row.operation,
        false);
    int looseA = _PolyhedralBoundedSolidSetNullEdgesConnector
        ::getLastLooseACount();
    int looseB = _PolyhedralBoundedSolidSetNullEdgesConnector
        ::getLastLooseBCount();
    EXPECT_EQ(0, looseA) << "[" << row.pairName << " + " << row.opName
        << "] looseA after Connect must be 0 per [MANT1988] Program 15.14";
    EXPECT_EQ(0, looseB) << "[" << row.pairName << " + " << row.opName
        << "] looseB after Connect must be 0 per [MANT1988] Program 15.14";
    releaseAll(pair, result);
}

INSTANTIATE_TEST_SUITE_P(Baseline, SetOpConnectNoLooseInvariantTest,
    ::testing::ValuesIn(BASELINE_CASES),
    [](const ::testing::TestParamInfo<ConnectCase>& info) {
        return std::string(info.param.pairName) + "_" + info.param.opName;
    });
