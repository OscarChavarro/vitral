#include <gtest/gtest.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.h"
#include "BooleanOracleFuzz.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"

/*
Operands with several shells, one of which the other operand does not cross
(plan-stage11 Phase F). The finish stage moves to the result the parts that
are connected to a cut; a shell without any cut was left behind without an
error. The composite oracle found it in a chain: (A n B) u C lost a piece
of A n B that C did not touch.

A is two separate unit cubes, P at the origin and Q five units away. B
crosses P only (or lies inside it). The expected volumes follow from the
boxes; the rotated run repeats them away from the axes.
C++ counterpart of Java's `SetOpWholeShellTest`.
*/
namespace {

typedef PolyhedralBoundedSolidModeler Modeler;
typedef PolyhedralBoundedSolidVolumeOracle Oracle;

const double BOX = 1.0;

Oracle::OrientedBox box(double x0, double y0, double z0, double x1,
    double y1, double z1)
{
    return Oracle::OrientedBox(x1 - x0, y1 - y0, z1 - z0,
        Oracle::translation((x0 + x1) / 2.0, (y0 + y1) / 2.0,
            (z0 + z1) / 2.0));
}

Oracle::OrientedBox place(const Oracle::OrientedBox& box, bool rotated)
{
    if ( !rotated ) {
        return box;
    }
    return Oracle::OrientedBox(box.size[0], box.size[1], box.size[2],
        Oracle::translation(0.3, -0.2, 0.5)
            .multiply(Oracle::rotation(0.83, 1, 2, -1.5))
            .multiply(box.transform));
}

/** Java `setOp(a.toSolid(), b.toSolid(), op)`; operands released. */
PolyhedralBoundedSolid* setOp(const Oracle::OrientedBox& a,
    const Oracle::OrientedBox& b, int op)
{
    PolyhedralBoundedSolid* solidA = a.toSolid();
    PolyhedralBoundedSolid* solidB = b.toSolid();
    PolyhedralBoundedSolid* result = Modeler::setOp(solidA, solidB, op);
    delete solidA;
    delete solidB;
    return result;
}

PolyhedralBoundedSolid* twoCubes(bool rotated)
{
    PolyhedralBoundedSolid* both = setOp(place(box(0, 0, 0, 1, 1, 1),
        rotated), place(box(5, 0, 0, 6, 1, 1), rotated), Modeler::UNION);
    EXPECT_EQ(2, PolyhedralBoundedSolidTopologySummary::from(both)
        .getShellCount());
    return both;
}

void assertVolume(bool rotated, int op, bool swapped, double expected,
    int shells)
{
    PolyhedralBoundedSolid* a = twoCubes(rotated);
    PolyhedralBoundedSolid* b = place(box(0.2, 0.2, 0.7, 0.8, 0.8, 1.3),
        rotated).toSolid();
    PolyhedralBoundedSolid* result = swapped ?
        Modeler::setOp(b, a, op) : Modeler::setOp(a, b, op);
    java::String label = java::String(rotated ? "rotated " : "") +
        BooleanOracleFuzz::opName(swapped ? BooleanOracleFuzz::B_MINUS_A :
            op);
    EXPECT_NEAR(expected, Oracle::solidVolume(result), 1.0e-9)
        << label.c_str() << " volume";
    EXPECT_EQ(shells, PolyhedralBoundedSolidTopologySummary::from(result)
        .getShellCount()) << label.c_str() << " shells";
    delete result;
    delete a;
    delete b;
}

/** B sticks out of P: half of it (0.108 of 0.216) is inside. */
void check(bool rotated)
{
    double inside = 0.6 * 0.6 * 0.3;
    double b = 0.6 * 0.6 * 0.6;
    assertVolume(rotated, Modeler::UNION, false, 2 * BOX + b - inside, 2);
    assertVolume(rotated, Modeler::INTERSECTION, false, inside, 1);
    assertVolume(rotated, Modeler::SUBTRACT, false, 2 * BOX - inside, 2);
    assertVolume(rotated, Modeler::SUBTRACT, true, b - inside, 1);
}

}

TEST(SetOpWholeShellTest, TwoPieceSolidCrossedInOnePieceSetOpTheOtherPieceIsKept) {
    check(false);
    check(true);
}

/*
The void left by a box removed from inside P is a shell of its own. A tool
that crosses only the outside of P must not lose it.
*/
TEST(SetOpWholeShellTest, SolidWithACavityToolCrossesTheOuterShellOnlyTheCavityIsKept) {
    PolyhedralBoundedSolid* hollow = setOp(box(0, 0, 0, 2, 2, 2),
        box(0.5, 0.5, 0.5, 1.5, 1.5, 1.5), Modeler::SUBTRACT);
    EXPECT_EQ(2, PolyhedralBoundedSolidTopologySummary::from(hollow)
        .getShellCount());
    PolyhedralBoundedSolid* tool = box(1.7, 0.8, 0.8, 2.3, 1.2, 1.2)
        .toSolid();
    PolyhedralBoundedSolid* cut = Modeler::setOp(hollow, tool,
        Modeler::SUBTRACT);
    EXPECT_NEAR(8.0 - 1.0 - 0.3 * 0.4 * 0.4, Oracle::solidVolume(cut),
        1.0e-9);
    EXPECT_EQ(2, PolyhedralBoundedSolidTopologySummary::from(cut)
        .getShellCount());
    delete cut;
    delete tool;
    delete hollow;
}
