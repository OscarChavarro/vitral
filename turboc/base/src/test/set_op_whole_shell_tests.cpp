// C++ port of Java's SetOpWholeShellTest
// (environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators).
#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "BooleanOracleFuzz.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.h"

typedef PolyhedralBoundedSolidVolumeOracle Oracle;
typedef PolyhedralBoundedSolidVolumeOracle::OrientedBox OrientedBox;
typedef PolyhedralBoundedSolidModeler Modeler;
typedef PolyhedralBoundedSolidTopologySummary TopologySummary;

static const double BOX = 1.0;

static OrientedBox
box(double x0, double y0, double z0, double x1, double y1, double z1)
{
    return OrientedBox(x1 - x0, y1 - y0, z1 - z0,
        Oracle::translation((x0 + x1) / 2.0, (y0 + y1) / 2.0,
            (z0 + z1) / 2.0));
}

static OrientedBox
place(const OrientedBox& box, BOOLEAN rotated)
{
    if ( !rotated ) {
        return box;
    }
    return OrientedBox(box.size[0], box.size[1], box.size[2],
        Oracle::translation(0.3, -0.2, 0.5)
            .multiply(Oracle::rotation(0.83, 1, 2, -1.5))
            .multiply(box.transform));
}

/** C++ helper: Java `setOp(a.toSolid(), b.toSolid(), op)`. */
static PolyhedralBoundedSolid*
setOpOnBoxes(const OrientedBox& a, const OrientedBox& b, int op)
{
    PolyhedralBoundedSolid* solidA = a.toSolid();
    PolyhedralBoundedSolid* solidB = b.toSolid();
    PolyhedralBoundedSolid* result = Modeler::setOp(solidA, solidB, op);

    delete solidA;
    delete solidB;
    return result;
}

static PolyhedralBoundedSolid*
twoCubes(BOOLEAN rotated)
{
    PolyhedralBoundedSolid* both = setOpOnBoxes(
        place(box(0, 0, 0, 1, 1, 1), rotated),
        place(box(5, 0, 0, 6, 1, 1), rotated), Modeler::UNION);

    EXPECT_EQ(2, TopologySummary::from(both).getShellCount());
    return both;
}

static void
assertVolume(BOOLEAN rotated, int op, BOOLEAN swapped, double expected,
    int shells)
{
    PolyhedralBoundedSolid* a = twoCubes(rotated);
    PolyhedralBoundedSolid* b = place(box(0.2, 0.2, 0.7, 0.8, 0.8, 1.3),
        rotated).toSolid();
    PolyhedralBoundedSolid* result;
    if ( swapped ) {
        result = Modeler::setOp(b, a, op);
    }
    else {
        result = Modeler::setOp(a, b, op);
    }
    String label = rotated ? "rotated " : "";
    label = label + BooleanOracleFuzz::opName(
        swapped ? (int)BooleanOracleFuzz::B_MINUS_A : op);

    printf("%s\n", label.c_str());
    // volume
    EXPECT_NEAR(expected, Oracle::solidVolume(result), 1.0e-9);
    // shells
    EXPECT_EQ(shells, TopologySummary::from(result).getShellCount());
    delete a;
    delete b;
    delete result;
}

/** B sticks out of P: half of it (0.108 of 0.216) is inside. */
static void
check(BOOLEAN rotated)
{
    double inside = 0.6 * 0.6 * 0.3;
    double b = 0.6 * 0.6 * 0.6;

    assertVolume(rotated, Modeler::UNION, FALSE, 2 * BOX + b - inside, 2);
    assertVolume(rotated, Modeler::INTERSECTION, FALSE, inside, 1);
    assertVolume(rotated, Modeler::SUBTRACT, FALSE, 2 * BOX - inside, 2);
    assertVolume(rotated, Modeler::SUBTRACT, TRUE, b - inside, 1);
}

TEST(SetOpWholeShell, given_twoPieceSolidCrossedInOnePiece_when_setOp_then_theOtherPieceIsKept)
{
    check(FALSE);
    check(TRUE);
}

/**
The void left by a box removed from inside P is a shell of its own. A
tool that crosses only the outside of P must not lose it.
*/
TEST(SetOpWholeShell, given_solidWithACavity_when_toolCrossesTheOuterShellOnly_then_theCavityIsKept)
{
    PolyhedralBoundedSolid* hollow = setOpOnBoxes(
        box(0, 0, 0, 2, 2, 2), box(0.5, 0.5, 0.5, 1.5, 1.5, 1.5),
        Modeler::SUBTRACT);
    ASSERT_EQ(2, TopologySummary::from(hollow).getShellCount());
    PolyhedralBoundedSolid* tool = box(1.7, 0.8, 0.8, 2.3, 1.2, 1.2)
        .toSolid();

    PolyhedralBoundedSolid* cut = Modeler::setOp(hollow, tool,
        Modeler::SUBTRACT);

    EXPECT_NEAR(8.0 - 1.0 - 0.3 * 0.4 * 0.4, Oracle::solidVolume(cut),
        1.0e-9);
    EXPECT_EQ(2, TopologySummary::from(cut).getShellCount());
    delete hollow;
    delete tool;
    delete cut;
}

VITRAL_TEST_MAIN()
