// C++ port of Java's SetOperandAlignerTest
// (environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators).
#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/_PolyhedralBoundedSolidSetOperandAligner.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"

typedef PolyhedralBoundedSolidVolumeOracle Oracle;
typedef PolyhedralBoundedSolidModeler Modeler;
typedef _PolyhedralBoundedSolidSetOperandAligner Aligner;

static const double UNIT[6] = {0, 0, 0, 1, 1, 1};

static PolyhedralBoundedSolid*
box(const double* corners)
{
    return Oracle::OrientedBox(corners[3] - corners[0],
        corners[4] - corners[1], corners[5] - corners[2],
        Oracle::translation(
            (corners[0] + corners[3]) / 2.0,
            (corners[1] + corners[4]) / 2.0,
            (corners[2] + corners[5]) / 2.0)).toSolid();
}

static PolyhedralBoundedSolid*
box(double x0, double y0, double z0, double x1, double y1, double z1)
{
    double corners[6];

    corners[0] = x0;
    corners[1] = y0;
    corners[2] = z0;
    corners[3] = x1;
    corners[4] = y1;
    corners[5] = z1;
    return box(corners);
}

/** C++ helper: Java `solid.getMinMax()[i]` (the array is owned here). */
static double
minMax(PolyhedralBoundedSolid* solid, int i)
{
    double* values = solid->getMinMax();
    double value = values[i];

    delete[] values;
    return value;
}

static double
tolerance()
{
    PolyhedralBoundedSolid* a = box(UNIT);
    PolyhedralBoundedSolid* b = box(0.4, 0.3, 1, 1.4, 1.3, 1.6);
    double value = PolyhedralBoundedSolidNumericPolicy::forSolids(a, b)
        .bigEpsilon();

    delete a;
    delete b;
    return value;
}

TEST(SetOperandAligner, given_faceInsideTheBand_when_aligned_then_bMovesByTheGap)
{
    double tolerance = ::tolerance();
    static const double gaps[] = {-1.5, -0.3, 0.4, 1.7};
    int g;

    for ( g = 0; g < 4; g++ ) {
        double gap = gaps[g];
        PolyhedralBoundedSolid* a = box(UNIT);
        PolyhedralBoundedSolid* b = box(0.4, 0.3,
            1 + gap * tolerance, 1.4, 1.3, 1.6 + gap * tolerance);
        Vector3Dd moved;

        // gap of `gap` tolerances
        EXPECT_TRUE(Aligner::align(a, b, moved));
        EXPECT_NEAR(0.0, moved.x(), 1.0e-15);
        EXPECT_NEAR(0.0, moved.y(), 1.0e-15);
        EXPECT_NEAR(-gap * tolerance, moved.z(), 1.0e-9 * tolerance);
        EXPECT_NEAR(1.0, minMax(b, 2), 1.0e-15);
        delete a;
        delete b;
    }
}

TEST(SetOperandAligner, given_faceOutsideTheBandOrInContact_when_aligned_then_nothingMoves)
{
    double tolerance = ::tolerance();
    static const double gaps[] = {-10, -3, 0, 3, 10};
    int g;

    for ( g = 0; g < 5; g++ ) {
        double gap = gaps[g];
        PolyhedralBoundedSolid* b = box(0.4, 0.3,
            1 + gap * tolerance, 1.4, 1.3, 1.6 + gap * tolerance);
        double before = minMax(b, 2);
        PolyhedralBoundedSolid* a = box(UNIT);
        Vector3Dd moved;

        // gap of `gap` tolerances
        EXPECT_FALSE(Aligner::align(a, b, moved));
        EXPECT_EQ(before, minMax(b, 2));
        delete a;
        delete b;
    }
}

/** B sits in a corner of an L of two boxes: two gaps, two directions. */
TEST(SetOperandAligner, given_gapsAlongTwoDirections_when_aligned_then_bothAreClosed)
{
    double tolerance = ::tolerance();
    PolyhedralBoundedSolid* a = box(UNIT);
    PolyhedralBoundedSolid* b = box(
        1 + 0.7 * tolerance, 0.2, 0.5 - 1.2 * tolerance,
        1.8 + 0.7 * tolerance, 0.9, 1 - 1.2 * tolerance);
    Vector3Dd moved;

    EXPECT_TRUE(Aligner::align(a, b, moved));
    EXPECT_NEAR(-0.7 * tolerance, moved.x(), 1.0e-9 * tolerance);
    EXPECT_NEAR(1.2 * tolerance, moved.z(), 1.0e-9 * tolerance);
    EXPECT_NEAR(0.0, moved.y(), 1.0e-15);
    delete a;
    delete b;
}

/**
B stands between the top of A and the bottom of a second piece of A,
a tolerance short of each on different amounts: no translation closes
both gaps, so B stays where it is.
*/
TEST(SetOperandAligner, given_twoGapsAlongOneDirection_when_aligned_then_nothingMoves)
{
    double tolerance = ::tolerance();
    PolyhedralBoundedSolid* lower = box(UNIT);
    PolyhedralBoundedSolid* upper = box(0, 0, 2, 1, 1, 3);
    PolyhedralBoundedSolid* a = Modeler::setOp(lower, upper, Modeler::UNION);
    PolyhedralBoundedSolid* b = box(0.2, 0.2,
        1 + 0.5 * tolerance, 0.8, 0.8, 2 - 1.5 * tolerance);
    double before = minMax(b, 2);
    Vector3Dd moved;

    EXPECT_FALSE(Aligner::align(a, b, moved));
    EXPECT_EQ(before, minMax(b, 2));
    delete lower;
    delete upper;
    delete a;
    delete b;
}

TEST(SetOperandAligner, given_operandsInsideTheBand_when_setOp_then_answeredAndOperandsUntouched)
{
    double tolerance = ::tolerance();
    PolyhedralBoundedSolid* a = box(UNIT);
    PolyhedralBoundedSolid* b = box(0.4, 0.3,
        1 - 0.5 * tolerance, 1.4, 1.3, 1.6 - 0.5 * tolerance);
    double bottomOfB = minMax(b, 2);

    PolyhedralBoundedSolid* result = Modeler::setOp(a, b, Modeler::UNION);

    EXPECT_NEAR(1.0 + 0.6, Oracle::solidVolume(result), 2.0 * tolerance);
    EXPECT_EQ(bottomOfB, minMax(b, 2));
    delete a;
    delete b;
    delete result;
}

/**
B inside A with one face on a face of A: A - B is A with a pocket open
to the outside (11 faces), not A with a cavity. The containment
decision used to answer it with a cavity, which strict validation
rejected.
*/
TEST(SetOperandAligner, given_boxInsideTouchingAFace_when_subtracted_then_resultIsAPocket)
{
    PolyhedralBoundedSolid* a = box(UNIT);
    PolyhedralBoundedSolid* b = box(0.2, 0.3, 0.6, 0.7, 0.8, 1);
    PolyhedralBoundedSolid* result = Modeler::setOp(a, b, Modeler::SUBTRACT);

    EXPECT_NEAR(1.0 - 0.5 * 0.5 * 0.4, Oracle::solidVolume(result), 1.0e-12);
    EXPECT_EQ(11, result->getPolygonsList().size());
    ASSERT_TRUE(Modeler::getLastSetOpResultPath() != NULL);
    EXPECT_TRUE(*Modeler::getLastSetOpResultPath() == "normal-pipeline");
    delete a;
    delete b;
    delete result;
}

VITRAL_TEST_MAIN()
