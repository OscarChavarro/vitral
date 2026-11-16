// C++ port of Java's SetOpContractTest
// (environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators).
#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "BooleanOracleFuzz.h"
#include "CsgSampleCorpus.h"
#include "CsgSampleCorpusFixtures.h"
#include "PolyhedralBoundedSolidTestFixtures.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"
#include "PolyhedralTestFailures.h"
#include "RigidMotionInvarianceGate.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidStructuralCopy.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

typedef PolyhedralBoundedSolidVolumeOracle Oracle;
typedef PolyhedralBoundedSolidVolumeOracle::OrientedBox OrientedBox;
typedef PolyhedralBoundedSolidModeler Modeler;

static Matrix4x4d
translation(double x, double y, double z)
{
    return Oracle::translation(x, y, z);
}

static void
checkOperandsUnchanged(CsgSampleCorpus sample, int op)
{
    ArrayList<PolyhedralBoundedSolid*> pair =
        CsgSampleCorpusFixtures::createPair(sample);
    String beforeA = PolyhedralBoundedSolidTopologySummary::from(pair[0])
        .toString();
    String beforeB = PolyhedralBoundedSolidTopologySummary::from(pair[1])
        .toString();
    double volumeA = Oracle::solidVolume(pair[0]);
    double volumeB = Oracle::solidVolume(pair[1]);
    PolyhedralBoundedSolid* result = NULL;
    String failureName;
    String failureMessage;

    printf("%s op=%d\n", csgSampleCorpusName(sample), op);
    try {
        result = Modeler::setOp(pair[0], pair[1], op, FALSE, TRUE, FALSE);
    }
    // The contract holds after a failure as well.
    PBS_CATCH_OPERATION(failureName, failureMessage)
    delete result;

    EXPECT_TRUE(PolyhedralBoundedSolidTopologySummary::from(pair[0])
        .toString() == beforeA);
    EXPECT_TRUE(PolyhedralBoundedSolidTopologySummary::from(pair[1])
        .toString() == beforeB);
    EXPECT_EQ(volumeA, Oracle::solidVolume(pair[0]));
    EXPECT_EQ(volumeB, Oracle::solidVolume(pair[1]));
    PolyhedralBoundedSolidTestFixtures::dispose(pair);
}

/**
C++ port note: the Java parameterized test over (sample, op) is a loop that
prints each row as "{sample} op={op}".
*/
TEST(SetOpContract, given_referencePair_when_setOp_then_operandsAreUnchanged)
{
    static const int ops[3] = { PolyhedralBoundedSolidModeler::UNION,
        PolyhedralBoundedSolidModeler::INTERSECTION,
        PolyhedralBoundedSolidModeler::SUBTRACT };
    int sampleIndex;
    int k;

    for ( sampleIndex = 0; sampleIndex < CSG_SAMPLE_CORPUS_COUNT;
          sampleIndex++ ) {
        for ( k = 0; k < 3; k++ ) {
            checkOperandsUnchanged((CsgSampleCorpus)sampleIndex, ops[k]);
        }
    }
}

TEST(SetOpContract, given_operands_when_setOpDestructive_then_operandsAreConsumed)
{
    PolyhedralBoundedSolid* a = OrientedBox(2, 2, 2,
        translation(0, 0, 0)).toSolid();
    PolyhedralBoundedSolid* b = OrientedBox(2, 2, 2,
        translation(1, 1, 1)).toSolid();

    PolyhedralBoundedSolid* result = Modeler::setOpDestructive(a, b,
        Modeler::UNION, FALSE, TRUE, TRUE);

    EXPECT_NEAR(15.0, Oracle::solidVolume(result), 1.0e-9);
    // operand A after a destructive union
    EXPECT_GT(fabs(Oracle::solidVolume(a) - 8.0), 1.0e-9);
    delete result;
    delete a;
    delete b;
}

TEST(SetOpContract, given_sameSolidTwice_when_subtracting_then_resultIsTheEmptySolid)
{
    PolyhedralBoundedSolid* a = OrientedBox(1, 2, 3,
        translation(0.5, 0, 0)).toSolid();
    PolyhedralBoundedSolid* b = PolyhedralBoundedSolidStructuralCopy::copy(a);

    PolyhedralBoundedSolid* result = Modeler::setOp(a, b, Modeler::SUBTRACT);

    EXPECT_TRUE(Modeler::isEmptyResult(result));
    delete result;
    delete a;
    delete b;
}

static void
checkEmptyByDefinition(int op)
{
    PolyhedralBoundedSolid* a;
    if ( op == Modeler::SUBTRACT ) {
        a = OrientedBox(1, 1, 1, translation(0, 0, 0)).toSolid();
    }
    else {
        a = OrientedBox(1, 1, 1, translation(5, 0, 0)).toSolid();
    }
    PolyhedralBoundedSolid* b = OrientedBox(3, 3, 3,
        translation(0, 0, 0)).toSolid();

    PolyhedralBoundedSolid* result = Modeler::setOp(a, b, op);

    printf("op=%d\n", op);
    EXPECT_TRUE(Modeler::isEmptyResult(result));
    delete result;
    delete a;
    delete b;
}

TEST(SetOpContract, given_disjointOrContainedOperands_when_emptyByDefinition_then_emptySolid)
{
    checkEmptyByDefinition(Modeler::INTERSECTION);
    checkEmptyByDefinition(Modeler::SUBTRACT);
}

/**
The rotated cross pair used to come back as an empty solid without any
report (Stage D notes). Union and intersection give the solid; the
difference leaves two bars that touch only along an edge, a non-manifold
result that is rejected (policy of B-04).
*/
static void
checkRotatedCrossPair(int op)
{
    ArrayList<PolyhedralBoundedSolid*> pair =
        CsgSampleCorpusFixtures::createPair(CROSS_PAIR);
    Modeler::applyTransformation(pair[0], rigidMotionGenericRotation());
    Modeler::applyTransformation(pair[1], rigidMotionGenericRotation());

    printf("op=%d\n", op);
    if ( op == Modeler::SUBTRACT ) {
        EXPECT_THROW(Modeler::setOp(pair[0], pair[1], op, FALSE, TRUE,
            FALSE), NonManifoldBooleanResultException);
        PolyhedralBoundedSolidTestFixtures::dispose(pair);
        return;
    }
    PolyhedralBoundedSolid* result = Modeler::setOp(pair[0], pair[1], op,
        FALSE, TRUE, FALSE);
    // silent empty result
    EXPECT_FALSE(Modeler::isEmptyResult(result));
    String message;
    EXPECT_TRUE(PolyhedralBoundedSolidValidationEngine::validateStrict(
        result, &message));
    delete result;
    PolyhedralBoundedSolidTestFixtures::dispose(pair);
}

TEST(SetOpContract, given_rotatedCrossPair_when_setOp_then_neverASilentEmptySolid)
{
    checkRotatedCrossPair(Modeler::UNION);
    checkRotatedCrossPair(Modeler::INTERSECTION);
    checkRotatedCrossPair(Modeler::SUBTRACT);
}

/**
C++ port note: the class relations are checked by conversions to the base
classes (no RTTI on the legacy compilers), and the cause of a
SolidOperationFailedException is not kept by the port (its constructor
takes the cause only for the Java signature), so `hasCauseInstanceOf` is
not checked.
*/
TEST(SetOpContract, given_typedFailures_then_theyShareTheBaseAndStayIllegalState)
{
    NonManifoldBooleanResultException nonManifold("x");
    InvalidSolidResultException invalidResult("x");
    SolidOperationFailedException failed("x", RuntimeException("cause"));
    InvalidOperandException invalidOperand("x");
    const PolyhedralBoundedSolidOperationException* base;
    const IllegalStateException* illegalState;

    base = &nonManifold;
    illegalState = &nonManifold;
    EXPECT_TRUE(base != NULL && illegalState != NULL);
    base = &invalidResult;
    EXPECT_TRUE(base != NULL);
    base = &failed;
    EXPECT_TRUE(base != NULL);
    base = &invalidOperand;
    EXPECT_TRUE(base != NULL);
}

/**
An operand that is not a valid boundary model stops the operation before
the pipeline runs: a cube with one vertex lifted has non-planar faces.
*/
TEST(SetOpContract, given_operandWithNonPlanarFaces_when_setOp_then_invalidOperand)
{
    PolyhedralBoundedSolid* warped = OrientedBox(1, 1, 1,
        translation(0, 0, 0)).toSolid();
    PolyhedralBoundedSolid* box = OrientedBox(1, 1, 1,
        translation(0.3, 0.2, 0.1)).toSolid();

    warped->getVerticesList().get(0)->position =
        warped->getVerticesList().get(0)->position.add(
            Vector3Dd(0.1, 0.2, 0.3));

    EXPECT_THROW(Modeler::setOp(warped, box, Modeler::UNION),
        InvalidOperandException);
    delete warped;
    delete box;
}

VITRAL_TEST_MAIN()
