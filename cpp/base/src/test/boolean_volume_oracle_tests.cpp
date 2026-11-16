#include <ostream>
#include <cmath>
#include <gtest/gtest.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/NonManifoldBooleanResultException.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"

/*
Boolean set operations checked against an independent volume oracle, on
box/box configurations that are NOT part of the fixture corpus.

Every row runs one operation (A u B, A n B, A \ B or B \ A) through the
public default entry point (strict validation on) and asserts both the
strict B-Rep postcondition and the exact expected volume.

Traceability: [MANT1988] Ch. 15 (set operations), Sec. 15.3 (8-way boundary
classification and "on" reclassification, Eq. 15.3), Sec. 15.6.1
(vertex/face classification: null edges inserted as an internal loop of the
pierced face, Fig. 15.6), Sec. 15.8 (result generation). Cases marked
"hidden defect" were found by the Fable 5.2 kernel analysis
(doc/polyhedralBoundedSolidDevelopment/Fable5.2Analysis.md, defects B-01 to
B-05).
C++ counterpart of Java's `BooleanVolumeOracleTest`, which had none before.
*/
namespace {

typedef PolyhedralBoundedSolidModeler Modeler;
typedef PolyhedralBoundedSolidVolumeOracle Oracle;
typedef Oracle::OrientedBox OrientedBox;

const int UNION = PolyhedralBoundedSolidModeler::UNION;
const int INTERSECTION = PolyhedralBoundedSolidModeler::INTERSECTION;
const int A_MINUS_B = PolyhedralBoundedSolidModeler::SUBTRACT;
const int B_MINUS_A = 4;

struct BoxPair
{
    OrientedBox boxA;
    OrientedBox boxB;
};

BoxPair pairOf(const OrientedBox& a, const OrientedBox& b)
{
    BoxPair pair;
    pair.boxA = a;
    pair.boxB = b;
    return pair;
}

// -- Case catalogue ------------------------------------------------------

/** Generic position: the section polygon only crosses edges. */
BoxPair cornerInCorner()
{
    return pairOf(OrientedBox(1.4, 1.2, 1.6, Oracle::translation(0, 0, 0)),
        OrientedBox(1.1, 1.5, 1.3, Oracle::translation(0.413, -0.287, 0.351)));
}

/**
B-01: the four peg edges pierce the interior of the block's top face, so the
section polygon is a ring strictly inside one face ([MANT1988] Fig. 15.6).
*/
BoxPair pegEndingInsideBlock()
{
    return pairOf(OrientedBox(2, 2, 2, Oracle::translation(0, 0, 0)),
        OrientedBox(0.5, 0.5, 1.5, Oracle::translation(0.1, 0.2, 1.0)));
}

/** B-01 with two interior rings (entry and exit faces), peg rotated. */
BoxPair pegThroughBlock()
{
    return pairOf(OrientedBox(2, 2, 2, Oracle::translation(0, 0, 0)),
        OrientedBox(0.5, 0.5, 3.0, Oracle::translation(0.1, 0.2, 0.05)
            .multiply(Oracle::rotation(0.3, 0, 0, 1))));
}

/**
B-01 with a triangular ring: a cube corner pokes through the interior of a
slab face (three edges pierce one face).
*/
BoxPair cubeCornerThroughSlabFace()
{
    Matrix4x4d diagonalUp = Oracle::rotation(0.37, 0, 0, 1).multiply(
        Oracle::rotation(std::acos(1.0 / std::sqrt(3.0)), 1, -1, 0));
    return pairOf(OrientedBox(4, 4, 1, Oracle::translation(0.05, 0.03, 1.2)),
        OrientedBox(1, 1, 1, diagonalUp));
}

/** B-05 control: top and bottom faces coplanar, both axis-aligned. */
BoxPair coplanarAxisAligned()
{
    return pairOf(OrientedBox(2, 2, 1, Oracle::translation(0, 0, 0)),
        OrientedBox(1, 1, 1, Oracle::translation(1.0, 0.1, 0)));
}

/** B-05 (rigid-motion invariance): coplanarAxisAligned() rotated rigidly. */
BoxPair coplanarRigidlyRotated()
{
    Matrix4x4d rigid = Oracle::rotation(0.5, 0.3, 1.0, 0.2);
    return pairOf(OrientedBox(2, 2, 1, rigid),
        OrientedBox(1, 1, 1, rigid.multiply(Oracle::translation(1.0, 0.1, 0))));
}

/** B-02: coplanar top and bottom faces, B rotated 30 degrees about z. */
BoxPair coplanarWithCrossingEdges()
{
    return pairOf(OrientedBox(2, 2, 1, Oracle::translation(0, 0, 0)),
        OrientedBox(1, 1, 1, Oracle::translation(1.0, 0.1, 0).multiply(
            Oracle::rotation(M_PI / 6.0, 0, 0, 1))));
}

/** B-02 (containment with tangent faces). */
BoxPair innerBoxWithFacesOnOuterFaces()
{
    return pairOf(OrientedBox(2, 2, 1, Oracle::translation(0, 0, 0)),
        OrientedBox(0.5, 0.5, 1, Oracle::translation(0.1, 0.1, 0).multiply(
            Oracle::rotation(M_PI / 6.0, 0, 0, 1))));
}

/** B-03: a cube rotated 45 degrees straddling A's face x = 1. */
BoxPair diamondStraddlingFace()
{
    return pairOf(OrientedBox(2, 2, 2, Oracle::translation(0, 0, 0)),
        OrientedBox(1, 1, 1, Oracle::translation(1.0, 0, 0.2).multiply(
            Oracle::rotation(M_PI / 4.0, 0, 0, 1))));
}

/** B-04: two unit cubes sharing one full face. */
BoxPair cubesSharingAFace()
{
    return pairOf(OrientedBox(1, 1, 1, Oracle::translation(0, 0, 0)),
        OrientedBox(1, 1, 1, Oracle::translation(1, 0, 0)));
}

/**
B-04: two boxes touching along an edge segment only. The kernel rejects the
union by design as non-manifold (decision 2026-11-16).
*/
BoxPair boxesTouchingAlongAnEdge()
{
    return pairOf(OrientedBox(1, 1, 2, Oracle::translation(0.5, 0.5, 1.0)),
        OrientedBox(2, 2, 2, Oracle::translation(-1.0, 2.0, 2.0)));
}

struct OracleRow
{
    const char* name;
    BoxPair (*pair)();
    int operation;
};

const char* opNameOf(int op)
{
    return op == UNION ? "UNION" : op == INTERSECTION ? "INTERSECTION" :
        op == A_MINUS_B ? "A-B" : "B-A";
}

/** Test name shown by gtest and ctest (instead of the raw bytes). */
void PrintTo(const OracleRow& v, std::ostream* os)
{
    *os << v.name << " " << opNameOf(v.operation);
}

// -- Operations that match the oracle today (regression guard) -----------
java::ArrayList<OracleRow> operationsThatMatchTheOracleToday()
{
    struct Config { const char* name; BoxPair (*pair)(); bool noUnion; };
    const Config configs[] = {
        { "corner-in-corner", cornerInCorner, false },
        // B-01 fixed 2026-11-16: all four operations.
        { "peg-ending-inside-block", pegEndingInsideBlock, false },
        { "peg-through-block", pegThroughBlock, false },
        { "cube-corner-through-slab-face", cubeCornerThroughSlabFace, false },
        { "coplanar-axis-aligned", coplanarAxisAligned, false },
        // Stage D (2026-11-16): vertex/face neighbourhood fix, witness
        // containment and face-contact union.
        { "coplanar-rigidly-rotated", coplanarRigidlyRotated, false },
        { "coplanar-with-crossing-edges", coplanarWithCrossingEdges, false },
        { "inner-box-with-faces-on-outer-faces",
            innerBoxWithFacesOnOuterFaces, false },
        { "diamond-straddling-face", diamondStraddlingFace, false },
        { "cubes-sharing-a-face", cubesSharingAFace, false },
        { "boxes-touching-along-an-edge", boxesTouchingAlongAnEdge, true }
    };
    const int ops[] = {UNION, INTERSECTION, A_MINUS_B, B_MINUS_A};
    java::ArrayList<OracleRow> rows;
    for ( const Config& c : configs ) {
        for ( int op : ops ) {
            if ( c.noUnion && op == UNION ) {
                continue;
            }
            OracleRow row = { c.name, c.pair, op };
            rows.add(row);
        }
    }
    return rows;
}

const java::ArrayList<OracleRow> ORACLE_ROWS =
    operationsThatMatchTheOracleToday();

void assertBooleanMatchesOracle(const OrientedBox& a, const OrientedBox& b,
    int op)
{
    const OrientedBox& first = (op == B_MINUS_A) ? b : a;
    const OrientedBox& second = (op == B_MINUS_A) ? a : b;
    int kernelOp = (op == B_MINUS_A) ? A_MINUS_B : op;
    double expected = Oracle::expectedVolume(first, second, kernelOp);
    double tolerance = 1.0e-6 * std::fmax(1.0, a.volume() + b.volume());
    PolyhedralBoundedSolid* firstSolid = first.toSolid();
    PolyhedralBoundedSolid* secondSolid = second.toSolid();
    PolyhedralBoundedSolid* result = Modeler::setOp(firstSolid, secondSolid,
        kernelOp);
    java::String strictReport;
    EXPECT_TRUE(PolyhedralBoundedSolidValidationEngine::validateStrict(
        result, &strictReport)) << "strict validation: " <<
        strictReport.c_str();
    EXPECT_NEAR(expected, Oracle::solidVolume(result), tolerance)
        << "result volume";
    delete result;
    delete firstSolid;
    delete secondSolid;
}

class BooleanVolumeOracleTest : public ::testing::TestWithParam<OracleRow>
{
};

class BooleanVolumeOracleNonManifoldTest : public ::testing::TestWithParam<bool>
{
};

}

TEST_P(BooleanVolumeOracleTest, BoxPairSetOpResultIsStrictValidAndVolumeMatchesOracle) {
    const OracleRow& row = GetParam();
    BoxPair pair = row.pair();
    assertBooleanMatchesOracle(pair.boxA, pair.boxB, row.operation);
}

INSTANTIATE_TEST_SUITE_P(Rows, BooleanVolumeOracleTest,
    ::testing::ValuesIn(ORACLE_ROWS.begin(), ORACLE_ROWS.end()),
    [](const ::testing::TestParamInfo<OracleRow>& info) {
        std::string name = std::string(info.param.name) + "_" +
            opNameOf(info.param.operation);
        for ( char& c : name ) {
            if ( !std::isalnum((unsigned char)c) ) {
                c = '_';
            }
        }
        return name;
    });

/*
-- Non-manifold results are rejected (B-04) --
The union of two solids that share only an edge (or a vertex) is two shells
meeting at a non-manifold boundary. setOp must refuse it with the typed
exception, with or without the strict postcondition, and record the
rejection path.
*/
TEST_P(BooleanVolumeOracleNonManifoldTest, BoxesTouchingAlongAnEdgeUnionRejectedAsNonManifold) {
    bool doStrictValidation = GetParam();
    BoxPair pair = boxesTouchingAlongAnEdge();
    PolyhedralBoundedSolid* a = pair.boxA.toSolid();
    PolyhedralBoundedSolid* b = pair.boxB.toSolid();
    EXPECT_THROW(Modeler::setOp(a, b, UNION, false, true,
        doStrictValidation), NonManifoldBooleanResultException);
    const java::String* path = Modeler::getLastSetOpResultPath();
    ASSERT_NE(nullptr, path);
    EXPECT_STREQ("rejected-non-manifold-union", path->c_str());
    delete a;
    delete b;
}

INSTANTIATE_TEST_SUITE_P(Strict, BooleanVolumeOracleNonManifoldTest,
    ::testing::Values(true, false));

/*
Contract C-01 (fixed in Stage F): setOp no longer consumes its operands.
The book's algorithm does ([MANT1988] Sec. 15.8); setOp now runs on
structural copies, and the consuming form is setOpDestructive.
*/
TEST(BooleanVolumeOracleTest, OperandsSetOpOperandsAreStillTheSameValidSolids) {
    BoxPair pair = cornerInCorner();
    PolyhedralBoundedSolid* a = pair.boxA.toSolid();
    PolyhedralBoundedSolid* b = pair.boxB.toSolid();
    PolyhedralBoundedSolid* result = Modeler::setOp(a, b, UNION);
    EXPECT_NEAR(pair.boxA.volume(), Oracle::solidVolume(a), 1e-9);
    EXPECT_NEAR(pair.boxB.volume(), Oracle::solidVolume(b), 1e-9);
    EXPECT_EQ(6, a->getPolygonsList().size());
    EXPECT_EQ(6, b->getPolygonsList().size());
    delete result;
    delete a;
    delete b;
}
