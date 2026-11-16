#include <gtest/gtest.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/_PolyhedralBoundedSolidSetOperandAligner.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"

/*
The one decision on whether two faces coincide
(`_PolyhedralBoundedSolidSetOperandAligner`): a face of B parallel to a face
of A, next to it and closer than two tolerances is put on its plane by
translating B, before any stage of the set operation runs.

The cases: inside the band B moves by exactly the gap; outside the band
and at exact contact nothing moves; two gaps along two directions are both
closed; two different gaps along one direction cannot be, and nothing
moves; the caller's operands are never touched by `setOp`.
C++ counterpart of Java's `SetOperandAlignerTest`.
*/
namespace {

typedef PolyhedralBoundedSolidModeler Modeler;
typedef _PolyhedralBoundedSolidSetOperandAligner Aligner;

const double UNIT[6] = {0, 0, 0, 1, 1, 1};

PolyhedralBoundedSolid* box(const double* corners)
{
    return PolyhedralBoundedSolidVolumeOracle::OrientedBox(
        corners[3] - corners[0],
        corners[4] - corners[1], corners[5] - corners[2],
        PolyhedralBoundedSolidVolumeOracle::translation(
            (corners[0] + corners[3]) / 2.0,
            (corners[1] + corners[4]) / 2.0,
            (corners[2] + corners[5]) / 2.0)).toSolid();
}

PolyhedralBoundedSolid* box(double x0, double y0, double z0, double x1,
    double y1, double z1)
{
    double corners[6] = {x0, y0, z0, x1, y1, z1};
    return box(corners);
}

double tolerance()
{
    PolyhedralBoundedSolid* a = box(UNIT);
    PolyhedralBoundedSolid* b = box(0.4, 0.3, 1, 1.4, 1.3, 1.6);
    double value = PolyhedralBoundedSolidNumericPolicy::forSolids(a, b)
        .bigEpsilon();
    delete a;
    delete b;
    return value;
}

/** Java `solid.getMinMax()[2]`. */
double minZ(PolyhedralBoundedSolid* solid)
{
    double* minMax = solid->getMinMax();
    double z = minMax[2];
    delete[] minMax;
    return z;
}

}

TEST(SetOperandAlignerTest, FaceInsideTheBandAlignedBMovesByTheGap) {
    double tolerance_ = tolerance();
    const double gaps[] = {-1.5, -0.3, 0.4, 1.7};
    for ( double gap : gaps ) {
        PolyhedralBoundedSolid* a = box(UNIT);
        PolyhedralBoundedSolid* b = box(0.4, 0.3, 1 + gap * tolerance_,
            1.4, 1.3, 1.6 + gap * tolerance_);
        Vector3Dd* moved = Aligner::align(a, b);
        ASSERT_NE(nullptr, moved) << "gap of " << gap << " tolerances";
        EXPECT_NEAR(0.0, moved->x(), 1.0e-15);
        EXPECT_NEAR(0.0, moved->y(), 1.0e-15);
        EXPECT_NEAR(-gap * tolerance_, moved->z(), 1.0e-9 * tolerance_);
        EXPECT_NEAR(1.0, minZ(b), 1.0e-15);
        delete moved;
        delete a;
        delete b;
    }
}

TEST(SetOperandAlignerTest, FaceOutsideTheBandOrInContactAlignedNothingMoves) {
    double tolerance_ = tolerance();
    const double gaps[] = {-10, -3, 0, 3, 10};
    for ( double gap : gaps ) {
        PolyhedralBoundedSolid* b = box(0.4, 0.3, 1 + gap * tolerance_,
            1.4, 1.3, 1.6 + gap * tolerance_);
        double before = minZ(b);
        PolyhedralBoundedSolid* a = box(UNIT);
        Vector3Dd* moved = Aligner::align(a, b);
        EXPECT_EQ(nullptr, moved) << "gap of " << gap << " tolerances";
        EXPECT_EQ(before, minZ(b));
        delete moved;
        delete a;
        delete b;
    }
}

/** B sits in a corner of an L of two boxes: two gaps, two directions. */
TEST(SetOperandAlignerTest, GapsAlongTwoDirectionsAlignedBothAreClosed) {
    double tolerance_ = tolerance();
    PolyhedralBoundedSolid* a = box(UNIT);
    PolyhedralBoundedSolid* b = box(
        1 + 0.7 * tolerance_, 0.2, 0.5 - 1.2 * tolerance_,
        1.8 + 0.7 * tolerance_, 0.9, 1 - 1.2 * tolerance_);
    Vector3Dd* moved = Aligner::align(a, b);
    ASSERT_NE(nullptr, moved);
    EXPECT_NEAR(-0.7 * tolerance_, moved->x(), 1.0e-9 * tolerance_);
    EXPECT_NEAR(1.2 * tolerance_, moved->z(), 1.0e-9 * tolerance_);
    EXPECT_NEAR(0.0, moved->y(), 1.0e-15);
    delete moved;
    delete a;
    delete b;
}

/*
B stands between the top of A and the bottom of a second piece of A,
a tolerance short of each on different amounts: no translation closes
both gaps, so B stays where it is.
*/
TEST(SetOperandAlignerTest, TwoGapsAlongOneDirectionAlignedNothingMoves) {
    double tolerance_ = tolerance();
    PolyhedralBoundedSolid* lower = box(UNIT);
    PolyhedralBoundedSolid* upper = box(0, 0, 2, 1, 1, 3);
    PolyhedralBoundedSolid* a = Modeler::setOp(lower, upper, Modeler::UNION);
    delete lower;
    delete upper;
    PolyhedralBoundedSolid* b = box(0.2, 0.2, 1 + 0.5 * tolerance_,
        0.8, 0.8, 2 - 1.5 * tolerance_);
    double before = minZ(b);
    Vector3Dd* moved = Aligner::align(a, b);
    EXPECT_EQ(nullptr, moved);
    EXPECT_EQ(before, minZ(b));
    delete moved;
    delete a;
    delete b;
}

TEST(SetOperandAlignerTest, OperandsInsideTheBandSetOpAnsweredAndOperandsUntouched) {
    double tolerance_ = tolerance();
    PolyhedralBoundedSolid* a = box(UNIT);
    PolyhedralBoundedSolid* b = box(0.4, 0.3, 1 - 0.5 * tolerance_,
        1.4, 1.3, 1.6 - 0.5 * tolerance_);
    double bottomOfB = minZ(b);
    PolyhedralBoundedSolid* unionSolid = Modeler::setOp(a, b,
        Modeler::UNION);
    EXPECT_NEAR(1.0 + 0.6,
        PolyhedralBoundedSolidVolumeOracle::solidVolume(unionSolid),
        2.0 * tolerance_);
    EXPECT_EQ(bottomOfB, minZ(b));
    delete unionSolid;
    delete a;
    delete b;
}

/*
B inside A with one face on a face of A: A - B is A with a pocket open
to the outside (11 faces), not A with a cavity. The containment
decision used to answer it with a cavity, which strict validation
rejected.
*/
TEST(SetOperandAlignerTest, BoxInsideTouchingAFaceSubtractedResultIsAPocket) {
    PolyhedralBoundedSolid* a = box(UNIT);
    PolyhedralBoundedSolid* b = box(0.2, 0.3, 0.6, 0.7, 0.8, 1);
    PolyhedralBoundedSolid* result = Modeler::setOp(a, b, Modeler::SUBTRACT);
    EXPECT_NEAR(1.0 - 0.5 * 0.5 * 0.4,
        PolyhedralBoundedSolidVolumeOracle::solidVolume(result), 1.0e-12);
    EXPECT_EQ(11, result->getPolygonsList().size());
    const java::String* path = Modeler::getLastSetOpResultPath();
    ASSERT_NE(nullptr, path);
    EXPECT_STREQ("normal-pipeline", path->c_str());
    delete result;
    delete a;
    delete b;
}
