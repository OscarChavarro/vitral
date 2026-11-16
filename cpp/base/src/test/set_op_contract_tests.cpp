#include <ostream>
#include <gtest/gtest.h>
#include "java/lang/RuntimeException.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/InvalidOperandException.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/InvalidSolidResultException.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/NonManifoldBooleanResultException.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidOperationException.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/SolidOperationFailedException.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidStructuralCopy.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "CsgSampleCorpusFixtures.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"
#include "RigidMotionInvarianceGate.h"

/*
Public contract of the set operations (Fable5.2Analysis.md Stage F):
- F.1 setOp leaves its operands intact; setOpDestructive consumes them, as
  [MANT1988] Sec. 15.8 does.
- F.2 failures are typed, all under PolyhedralBoundedSolidOperationException
  (still an IllegalStateException).
- F.4 a right empty result is an empty solid that
  PolyhedralBoundedSolidModeler::isEmptyResult recognizes; an empty result
  the operands disprove is a SolidOperationFailedException, never a silent
  empty solid.
C++ counterpart of Java's `SetOpContractTest`.
*/
namespace {

typedef PolyhedralBoundedSolidModeler Modeler;
typedef PolyhedralBoundedSolidVolumeOracle Oracle;

struct CorpusOperation
{
    CsgSampleCorpus sample;
    int operation;
};

/** Test name shown by gtest and ctest (instead of the raw bytes). */
void PrintTo(const CorpusOperation& v, std::ostream* os)
{
    *os << csgSampleCorpusName(v.sample) << " op=" << v.operation;
}

java::ArrayList<CorpusOperation> corpusOperations()
{
    java::ArrayList<CorpusOperation> rows;
    const int ops[] = {Modeler::UNION, Modeler::INTERSECTION,
        Modeler::SUBTRACT};
    for ( int s = 0; s < CSG_SAMPLE_CORPUS_COUNT; s++ ) {
        for ( int op : ops ) {
            CorpusOperation row = { CSG_SAMPLE_CORPUS_VALUES[s], op };
            rows.add(row);
        }
    }
    return rows;
}

const java::ArrayList<CorpusOperation> CORPUS_OPERATIONS = corpusOperations();

class SetOpContractCorpusTest :
    public ::testing::TestWithParam<CorpusOperation>
{
};

class SetOpContractEmptyTest : public ::testing::TestWithParam<int>
{
};

class SetOpContractCrossTest : public ::testing::TestWithParam<int>
{
};

}

TEST_P(SetOpContractCorpusTest, ReferencePairSetOpOperandsAreUnchanged) {
    const CorpusOperation& row = GetParam();
    java::ArrayList<PolyhedralBoundedSolid*> pair =
        CsgSampleCorpusFixtures::createPair(row.sample);
    java::String beforeA = PolyhedralBoundedSolidTopologySummary::from(
        pair[0]).toString();
    java::String beforeB = PolyhedralBoundedSolidTopologySummary::from(
        pair[1]).toString();
    double volumeA = Oracle::solidVolume(pair[0]);
    double volumeB = Oracle::solidVolume(pair[1]);
    try {
        PolyhedralBoundedSolid* result = Modeler::setOp(pair[0], pair[1],
            row.operation, false, true, false);
        delete result;
    }
    catch ( const PolyhedralBoundedSolidOperationException& ) {
        // The contract holds after a failure as well.
    }
    EXPECT_STREQ(beforeA.c_str(), PolyhedralBoundedSolidTopologySummary
        ::from(pair[0]).toString().c_str());
    EXPECT_STREQ(beforeB.c_str(), PolyhedralBoundedSolidTopologySummary
        ::from(pair[1]).toString().c_str());
    EXPECT_EQ(volumeA, Oracle::solidVolume(pair[0]));
    EXPECT_EQ(volumeB, Oracle::solidVolume(pair[1]));
    delete pair[0];
    delete pair[1];
}

INSTANTIATE_TEST_SUITE_P(Corpus, SetOpContractCorpusTest,
    ::testing::ValuesIn(CORPUS_OPERATIONS.begin(), CORPUS_OPERATIONS.end()),
    [](const ::testing::TestParamInfo<CorpusOperation>& info) {
        return std::string(csgSampleCorpusName(info.param.sample)) +
            "_op" + std::to_string(info.param.operation);
    });

TEST(SetOpContractTest, OperandsSetOpDestructiveOperandsAreConsumed) {
    PolyhedralBoundedSolid* a = Oracle::OrientedBox(2, 2, 2,
        Oracle::translation(0, 0, 0)).toSolid();
    PolyhedralBoundedSolid* b = Oracle::OrientedBox(2, 2, 2,
        Oracle::translation(1, 1, 1)).toSolid();
    PolyhedralBoundedSolid* result = Modeler::setOpDestructive(a, b,
        Modeler::UNION, false, true, true);
    EXPECT_NEAR(15.0, Oracle::solidVolume(result), 1.0e-9);
    EXPECT_GT(std::fabs(Oracle::solidVolume(a) - 8.0), 1.0e-9)
        << "operand A after a destructive union";
    delete result;
    delete a;
    delete b;
}

TEST(SetOpContractTest, SameSolidTwiceSubtractingResultIsTheEmptySolid) {
    PolyhedralBoundedSolid* a = Oracle::OrientedBox(1, 2, 3,
        Oracle::translation(0.5, 0, 0)).toSolid();
    PolyhedralBoundedSolid* b = PolyhedralBoundedSolidStructuralCopy::copy(a);
    PolyhedralBoundedSolid* result = Modeler::setOp(a, b, Modeler::SUBTRACT);
    EXPECT_TRUE(Modeler::isEmptyResult(result));
    delete result;
    delete a;
    delete b;
}

TEST_P(SetOpContractEmptyTest, DisjointOrContainedOperandsEmptyByDefinitionEmptySolid) {
    int op = GetParam();
    PolyhedralBoundedSolid* a = op == Modeler::SUBTRACT
        ? Oracle::OrientedBox(1, 1, 1, Oracle::translation(0, 0, 0)).toSolid()
        : Oracle::OrientedBox(1, 1, 1, Oracle::translation(5, 0, 0)).toSolid();
    PolyhedralBoundedSolid* b = Oracle::OrientedBox(3, 3, 3,
        Oracle::translation(0, 0, 0)).toSolid();
    PolyhedralBoundedSolid* result = Modeler::setOp(a, b, op);
    EXPECT_TRUE(Modeler::isEmptyResult(result));
    delete result;
    delete a;
    delete b;
}

INSTANTIATE_TEST_SUITE_P(Ops, SetOpContractEmptyTest,
    ::testing::Values((int)PolyhedralBoundedSolidModeler::INTERSECTION,
        (int)PolyhedralBoundedSolidModeler::SUBTRACT));

/*
The rotated cross pair used to come back as an empty solid without any
report (Stage D notes). Union and intersection give the solid; the
difference leaves two bars that touch only along an edge, a non-manifold
result that is rejected (policy of B-04).
*/
TEST_P(SetOpContractCrossTest, RotatedCrossPairSetOpNeverASilentEmptySolid) {
    int op = GetParam();
    java::ArrayList<PolyhedralBoundedSolid*> pair =
        CsgSampleCorpusFixtures::createPair(CROSS_PAIR);
    Modeler::applyTransformation(pair[0], rigidMotionGenericRotation());
    Modeler::applyTransformation(pair[1], rigidMotionGenericRotation());
    if ( op == Modeler::SUBTRACT ) {
        EXPECT_THROW(Modeler::setOp(pair[0], pair[1], op, false, true,
            false), NonManifoldBooleanResultException);
        delete pair[0];
        delete pair[1];
        return;
    }
    PolyhedralBoundedSolid* result = Modeler::setOp(pair[0], pair[1], op,
        false, true, false);
    EXPECT_FALSE(Modeler::isEmptyResult(result)) << "silent empty result";
    java::String message;
    EXPECT_TRUE(PolyhedralBoundedSolidValidationEngine::validateStrict(
        result, &message));
    delete result;
    delete pair[0];
    delete pair[1];
}

INSTANTIATE_TEST_SUITE_P(Ops, SetOpContractCrossTest,
    ::testing::Values((int)PolyhedralBoundedSolidModeler::UNION,
        (int)PolyhedralBoundedSolidModeler::INTERSECTION,
        (int)PolyhedralBoundedSolidModeler::SUBTRACT));

/*
C++ port note: the C++ exceptions do not keep their cause, so Java's
`hasCauseInstanceOf(RuntimeException.class)` has no counterpart.
*/
TEST(SetOpContractTest, TypedFailuresShareTheBaseAndStayIllegalState) {
    NonManifoldBooleanResultException nonManifold("x");
    InvalidSolidResultException invalidResult("x");
    SolidOperationFailedException failed("x", java::RuntimeException("y"));
    InvalidOperandException invalidOperand("x");
    const java::Throwable* t = &nonManifold;
    EXPECT_NE(nullptr,
        dynamic_cast<const PolyhedralBoundedSolidOperationException*>(t));
    EXPECT_NE(nullptr, dynamic_cast<const java::IllegalStateException*>(t));
    t = &invalidResult;
    EXPECT_NE(nullptr,
        dynamic_cast<const PolyhedralBoundedSolidOperationException*>(t));
    t = &failed;
    EXPECT_NE(nullptr,
        dynamic_cast<const PolyhedralBoundedSolidOperationException*>(t));
    t = &invalidOperand;
    EXPECT_NE(nullptr,
        dynamic_cast<const PolyhedralBoundedSolidOperationException*>(t));
}

/*
An operand that is not a valid boundary model stops the operation before
the pipeline runs: a cube with one vertex lifted has non-planar faces.
*/
TEST(SetOpContractTest, OperandWithNonPlanarFacesSetOpInvalidOperand) {
    PolyhedralBoundedSolid* warped = Oracle::OrientedBox(1, 1, 1,
        Oracle::translation(0, 0, 0)).toSolid();
    PolyhedralBoundedSolid* box = Oracle::OrientedBox(1, 1, 1,
        Oracle::translation(0.3, 0.2, 0.1)).toSolid();
    warped->getVerticesList().get(0)->position =
        warped->getVerticesList().get(0)->position.add(
            Vector3Dd(0.1, 0.2, 0.3));
    EXPECT_THROW(Modeler::setOp(warped, box, Modeler::UNION),
        InvalidOperandException);
    delete warped;
    delete box;
}
