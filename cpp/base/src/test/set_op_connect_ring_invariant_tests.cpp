#include <ostream>
#include <cmath>
#include <gtest/gtest.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/NonManifoldBooleanResultException.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/_PolyhedralBoundedSolidSetOperator.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "BooleanOracleFuzz.h"
#include "CsgSampleCorpusFixtures.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"

/*
Post-connect invariant (Fable5.2Analysis.md Stage C step 1): when connect
leaves no loose ends, every null face handed to finish has exactly two
loops, and no other face carries two coincident loops. The second rule is
what defect B-01 broke: a section ring strictly inside a face must stay in
that face as a hole while the disk becomes a separate face; before the fix
the hole was moved into the disk, leaving the carrier without hole and an
annular zero-area face.

Traceability: [MANT1988] Sec. 14.7 (join, laringmv), Sec. 15.6.1 (null
edges inserted as rings of the pierced face, Fig. 15.6), Sec. 15.7
(Program 15.14) and Sec. 15.8 (Program 15.15 expects two loops per null
face).
C++ counterpart of Java's `SetOpConnectRingInvariantTest`.
*/
namespace {

typedef PolyhedralBoundedSolidModeler Modeler;
typedef PolyhedralBoundedSolidVolumeOracle Oracle;
typedef BooleanOracleFuzz Fuzz;

java::String join(const java::ArrayList<java::String>& list)
{
    java::String text("[");
    for ( long i = 0; i < list.size(); i++ ) {
        if ( i > 0 ) {
            text += ", ";
        }
        text += list[i];
    }
    return text + "]";
}

void assertViolationsEmpty(const java::String& label)
{
    const java::ArrayList<java::String>* violations =
        _PolyhedralBoundedSolidSetOperator::getLastConnectInvariantViolations();
    if ( violations != nullptr ) {
        EXPECT_TRUE(violations->isEmpty()) << label.c_str() << " " <<
            join(*violations).c_str();
    }
}

struct CorpusCase
{
    CsgSampleCorpus sample;
    int operation;
};

/** Test name shown by gtest and ctest (instead of the raw bytes). */
void PrintTo(const CorpusCase& v, std::ostream* os)
{
    *os << csgSampleCorpusName(v.sample) << " op=" << v.operation;
}

java::ArrayList<CorpusCase> corpusCases()
{
    java::ArrayList<CorpusCase> rows;
    for ( int s = 0; s < CSG_SAMPLE_CORPUS_COUNT; s++ ) {
        for ( int k = 0; k < 4; k++ ) {
            CorpusCase row = { CSG_SAMPLE_CORPUS_VALUES[s],
                Fuzz::OPERATIONS[k] };
            rows.add(row);
        }
    }
    return rows;
}

const java::ArrayList<CorpusCase> CORPUS_CASES = corpusCases();

struct SectionRingCase
{
    const char* name;
    int pairIndex;
    int operation;
};

/** Test name shown by gtest and ctest (instead of the raw bytes). */
void PrintTo(const SectionRingCase& v, std::ostream* os)
{
    *os << v.name << " op=" << v.operation;
}

/** Java `sectionRingCases()` pairs, by index. */
Fuzz::BoxPair sectionRingPair(int index)
{
    if ( index == 0 ) {
        return Fuzz::BoxPair(
            Oracle::OrientedBox(2, 2, 2, Oracle::translation(0, 0, 0)),
            Oracle::OrientedBox(0.5, 0.5, 1.5,
                Oracle::translation(0.1, 0.2, 1.0)));
    }
    if ( index == 1 ) {
        return Fuzz::BoxPair(
            Oracle::OrientedBox(2, 2, 2, Oracle::translation(0, 0, 0)),
            Oracle::OrientedBox(0.5, 0.5, 3.0,
                Oracle::translation(0.1, 0.2, 0.05)
                    .multiply(Oracle::rotation(0.3, 0, 0, 1))));
    }
    return Fuzz::BoxPair(
        Oracle::OrientedBox(4, 4, 1, Oracle::translation(0.05, 0.03, 1.2)),
        Oracle::OrientedBox(1, 1, 1, Oracle::rotation(0.37, 0, 0, 1)
            .multiply(Oracle::rotation(std::acos(1.0 / std::sqrt(3.0)),
                1, -1, 0))));
}

java::ArrayList<SectionRingCase> sectionRingCases()
{
    java::ArrayList<SectionRingCase> rows;
    for ( int k = 0; k < 4; k++ ) {
        int op = Fuzz::OPERATIONS[k];
        SectionRingCase peg = { "peg-ending-inside-block", 0, op };
        SectionRingCase pegThrough = { "peg-through-block", 1, op };
        SectionRingCase corner = { "cube-corner-through-slab-face", 2, op };
        rows.add(peg);
        rows.add(pegThrough);
        rows.add(corner);
    }
    return rows;
}

const java::ArrayList<SectionRingCase> SECTION_RING_CASES =
    sectionRingCases();

class SetOpConnectRingInvariantCorpusTest :
    public ::testing::TestWithParam<CorpusCase>
{
};

class SetOpConnectRingInvariantSectionTest :
    public ::testing::TestWithParam<SectionRingCase>
{
};

}

TEST_P(SetOpConnectRingInvariantCorpusTest, ReferencePairSetOpReachesConnectRingInvariantHolds) {
    const CorpusCase& row = GetParam();
    java::ArrayList<PolyhedralBoundedSolid*> p =
        CsgSampleCorpusFixtures::createPair(row.sample);
    bool bMinusA = row.operation == Fuzz::B_MINUS_A;
    try {
        PolyhedralBoundedSolid* result = Modeler::setOp(
            bMinusA ? p[1] : p[0], bMinusA ? p[0] : p[1],
            bMinusA ? (int)Modeler::SUBTRACT : row.operation, false, true, false);
        delete result;
    }
    catch ( const NonManifoldBooleanResultException& ) {
        // Rejected after finish (CROSS_PAIR A-B: two bars touching
        // along an edge); connect ran and its invariant still applies.
    }
    assertViolationsEmpty(java::String(csgSampleCorpusName(row.sample)) +
        " " + Fuzz::opName(row.operation));
    delete p[0];
    delete p[1];
}

INSTANTIATE_TEST_SUITE_P(Corpus, SetOpConnectRingInvariantCorpusTest,
    ::testing::ValuesIn(CORPUS_CASES.begin(), CORPUS_CASES.end()),
    [](const ::testing::TestParamInfo<CorpusCase>& info) {
        return std::string(csgSampleCorpusName(info.param.sample)) + "_op" +
            std::to_string(info.param.operation);
    });

TEST_P(SetOpConnectRingInvariantSectionTest, SectionRingInsideAFaceSetOpRingInvariantHolds) {
    const SectionRingCase& row = GetParam();
    Fuzz::CaseResult result = Fuzz::run(Fuzz::B_ROTATED_ABOUT_RANDOM_AXIS,
        0, sectionRingPair(row.pairIndex), row.operation);
    EXPECT_NE(nullptr, _PolyhedralBoundedSolidSetOperator
        ::getLastConnectInvariantViolations())
        << row.name << " reached connect";
    assertViolationsEmpty(java::String(row.name) + " " +
        Fuzz::opName(row.operation));
    EXPECT_EQ(Fuzz::OK, result.outcome) << result.toString().c_str();
}

INSTANTIATE_TEST_SUITE_P(SectionRings, SetOpConnectRingInvariantSectionTest,
    ::testing::ValuesIn(SECTION_RING_CASES.begin(), SECTION_RING_CASES.end()),
    [](const ::testing::TestParamInfo<SectionRingCase>& info) {
        std::string name(info.param.name);
        for ( char& c : name ) {
            if ( c == '-' ) {
                c = '_';
            }
        }
        return name + "_op" + std::to_string(info.param.operation);
    });

TEST(SetOpConnectRingInvariantTest, NonCoplanarFuzzFamiliesSetOpRingInvariantHolds) {
    java::ArrayList<java::String> report;
    for ( int f = 0; f < Fuzz::FAMILY_COUNT; f++ ) {
        Fuzz::Family family = Fuzz::FAMILIES[f];
        if ( family == Fuzz::INTEGER_GRID ||
             family == Fuzz::COPLANAR_TOP_BOTTOM_ROTATED_45 ) {
            // Coplanar contacts: Stage D (defects B-02, B-05).
            continue;
        }
        java::ArrayList<Fuzz::BoxPair> pairs = Fuzz::pairs(family);
        int i;
        for ( i = 0; i < pairs.size(); i++ ) {
            for ( int k = 0; k < 4; k++ ) {
                int op = Fuzz::OPERATIONS[k];
                Fuzz::run(family, i, pairs.get(i), op);
                const java::ArrayList<java::String>* v =
                    _PolyhedralBoundedSolidSetOperator
                        ::getLastConnectInvariantViolations();
                if ( v != nullptr && !v->isEmpty() ) {
                    report.add(java::String(Fuzz::familyName(family)) + "#" +
                        java::String::valueOf(i) + " " + Fuzz::opName(op) +
                        " " + join(*v));
                }
            }
        }
    }
    EXPECT_TRUE(report.isEmpty()) << join(report).c_str();
}
