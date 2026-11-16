#include <ostream>
#include <gtest/gtest.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "CsgSampleCorpusFixtures.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"

/*
Splitting a cube [-1,1]^3 with planes in generic and degenerate position,
checked by volume conservation: vol(above) + vol(below) = vol(solid), every
piece strict-valid, non-empty when the plane really cuts the solid, and on
the side of the plane its list names. The input solid must come back
unchanged and must not appear in the output lists.

Traceability: [MANT1988] Ch. 14. Section 14.1 requires the algorithm to be
"general, i.e., it must work correctly for all possible geometric
configurations of solids and planes"; Sections 14.4-14.5 (splitgenerate,
ON-vertex neighborhoods, reclassification rules, Figs. 14.7-14.9) exist
precisely for vertices that lie on the splitting plane. The planes through
vertices reproduce defect S-01 of the Fable 5.2 kernel analysis
(doc/polyhedralBoundedSolidDevelopment/Fable5.2Analysis.md), fixed in
Stage E.
C++ counterpart of Java's `PolyhedralBoundedSolidSplitterDegeneratePlaneTest`.
*/
namespace {

typedef PolyhedralBoundedSolidVolumeOracle Oracle;
typedef java::ArrayList<PolyhedralBoundedSolid*> SolidList;

const double TOLERANCE = 1.0e-6;

struct CuttingPlane
{
    const char* name;
    Vector3Dd normal;
    Vector3Dd point;
    int side;
};

/** Test name shown by gtest and ctest (instead of the raw bytes). */
void PrintTo(const CuttingPlane& v, std::ostream* os)
{
    *os << v.name;
}

const CuttingPlane CUTTING_PLANES[] = {
    { "generic oblique plane",
        Vector3Dd(0.3, 0.5, 0.8), Vector3Dd(0.1, 0, 0.05), 0 },
    { "axis plane through the middle",
        Vector3Dd(0, 0, 1), Vector3Dd(0, 0, 0.1), 0 },
    { "plane containing one cube edge",
        Vector3Dd(1, -0.5, 0), Vector3Dd(1, 1, 0), 0 },
    { "diagonal plane through two opposite edges",
        Vector3Dd(1, -1, 0), Vector3Dd(0, 0, 0), 0 },
    // S-01 (Stage E): planes through vertices.
    { "corner cut through three vertices (x+y+z=1)",
        Vector3Dd(1, 1, 1), Vector3Dd(1, 1, -1), 0 },
    { "generic plane through exactly one vertex",
        Vector3Dd(1, 0.7, 0.4), Vector3Dd(1, 1, -1), 0 },
    { "corner cut with the normal reversed",
        Vector3Dd(-1, -1, -1), Vector3Dd(1, 1, -1), 0 },
    { "hexagonal section through six edge midpoints",
        Vector3Dd(1, 1, 1), Vector3Dd(0, 0, 0), 0 }
};

const CuttingPlane PLANES_THAT_DO_NOT_CUT[] = {
    { "plane missing the cube, cube below",
        Vector3Dd(0, 0, 1), Vector3Dd(0, 0, 5), -1 },
    { "plane missing the cube, cube above",
        Vector3Dd(0, 0, 1), Vector3Dd(0, 0, -5), +1 },
    { "plane touching one vertex from outside",
        Vector3Dd(1, 1, 1), Vector3Dd(1, 1, 1), -1 },
    { "plane containing the top face",
        Vector3Dd(0, 0, 1), Vector3Dd(0, 0, 1), -1 },
    { "plane containing the bottom face",
        Vector3Dd(0, 0, 1), Vector3Dd(0, 0, -1), +1 },
    { "plane touching one edge from outside",
        Vector3Dd(1, 1, 0), Vector3Dd(1, 1, 0), -1 }
};

std::string testName(const ::testing::TestParamInfo<CuttingPlane>& info)
{
    std::string name;
    for ( const char* c = info.param.name; *c != '\0'; c++ ) {
        name += std::isalnum((unsigned char)*c) ? *c : '_';
    }
    return name;
}

void assertInputUnchanged(PolyhedralBoundedSolid* solid,
    const Oracle::OrientedBox& cube, const SolidList& above,
    const SolidList& below)
{
    EXPECT_EQ(6, solid->getPolygonsList().size())
        << "input faces after split";
    EXPECT_NEAR(cube.volume(), Oracle::solidVolume(solid), TOLERANCE)
        << "input volume after split";
    for ( long i = 0; i < above.size(); i++ ) {
        EXPECT_NE(solid, above[i]) << "above holds the input object";
    }
    for ( long i = 0; i < below.size(); i++ ) {
        EXPECT_NE(solid, below[i]) << "below holds the input object";
    }
}

double assertValidPieceOnSide(PolyhedralBoundedSolid* piece,
    const InfinitePlane& plane, int side)
{
    java::String strictReport;
    int i;
    EXPECT_GE(piece->getPolygonsList().size(), 4) << "piece face count";
    EXPECT_TRUE(PolyhedralBoundedSolidValidationEngine::validateStrict(
        piece, &strictReport)) << "strict validation: " <<
        strictReport.c_str();
    for ( i = 0; i < piece->getVerticesList().size(); i++ ) {
        EXPECT_GE(side * plane.pointDistance(
            piece->getVerticesList().get(i)->position), -TOLERANCE)
            << "vertex side";
    }
    return Oracle::solidVolume(piece);
}

void release(SolidList& list)
{
    for ( long i = 0; i < list.size(); i++ ) {
        delete list[i];
    }
}

class SplitterCuttingPlaneTest : public ::testing::TestWithParam<CuttingPlane>
{
};

class SplitterPlaneThatDoesNotCutTest :
    public ::testing::TestWithParam<CuttingPlane>
{
};

}

TEST_P(SplitterCuttingPlaneTest, CuttingPlaneSplitVolumeIsConservedOnTheRightSides) {
    const CuttingPlane& row = GetParam();
    Oracle::OrientedBox cube(2, 2, 2, Oracle::translation(0, 0, 0));
    PolyhedralBoundedSolid* solid = cube.toSolid();
    InfinitePlane plane(row.normal.normalized(), row.point);
    SolidList above;
    SolidList below;
    double total = 0.0;
    PolyhedralBoundedSolidModeler::split(solid, plane, above, below);
    assertInputUnchanged(solid, cube, above, below);
    EXPECT_FALSE(above.isEmpty()) << "pieces above";
    EXPECT_FALSE(below.isEmpty()) << "pieces below";
    for ( long i = 0; i < above.size(); i++ ) {
        total += assertValidPieceOnSide(above[i], plane, +1);
    }
    for ( long i = 0; i < below.size(); i++ ) {
        total += assertValidPieceOnSide(below[i], plane, -1);
    }
    EXPECT_NEAR(cube.volume(), total, TOLERANCE);
    release(above);
    release(below);
    delete solid;
}

INSTANTIATE_TEST_SUITE_P(Planes, SplitterCuttingPlaneTest,
    ::testing::ValuesIn(CUTTING_PLANES), testName);

/*
A plane that does not cut the solid leaves it whole: a copy goes to the
list of the side it lies on, an empty solid to the other one. Before Stage
E the input object itself always went to "above".
*/
TEST_P(SplitterPlaneThatDoesNotCutTest, PlaneThatDoesNotCutSplitACopyGoesToItsSide) {
    const CuttingPlane& row = GetParam();
    Oracle::OrientedBox cube(2, 2, 2, Oracle::translation(0, 0, 0));
    PolyhedralBoundedSolid* solid = cube.toSolid();
    InfinitePlane plane(row.normal.normalized(), row.point);
    SolidList above;
    SolidList below;
    PolyhedralBoundedSolidModeler::split(solid, plane, above, below);
    assertInputUnchanged(solid, cube, above, below);
    ASSERT_EQ(1, above.size());
    ASSERT_EQ(1, below.size());
    PolyhedralBoundedSolid* whole = row.side > 0 ? above[0] : below[0];
    PolyhedralBoundedSolid* empty = row.side > 0 ? below[0] : above[0];
    EXPECT_EQ(0, empty->getPolygonsList().size());
    EXPECT_NEAR(cube.volume(), assertValidPieceOnSide(whole, plane,
        row.side), TOLERANCE);
    release(above);
    release(below);
    delete solid;
}

INSTANTIATE_TEST_SUITE_P(Planes, SplitterPlaneThatDoesNotCutTest,
    ::testing::ValuesIn(PLANES_THAT_DO_NOT_CUT), testName);

/*
A plane through the lower reflex vertex of the L-shaped hollow brick cuts a
section of two regions that touch at that vertex. Above the plane the piece
is one solid whose section has two faces sharing the vertex; below, the two
regions meet only at the vertex and come out as two solids. Every piece is
strict-valid and the volume is conserved.
*/
TEST(PolyhedralBoundedSolidSplitterDegeneratePlaneTest, PlaneThroughReflexVertexSectionIsPinchedSimpleFaces) {
    java::ArrayList<PolyhedralBoundedSolid*> pair =
        CsgSampleCorpusFixtures::createPair(HOLLOW_BRICK);
    PolyhedralBoundedSolid* brick = pair[0];
    delete pair[1];
    InfinitePlane plane(Vector3Dd(-0.85, 1.14, 0.19).normalized(),
        Vector3Dd(0.2, 0.8, 0.0));
    SolidList above;
    SolidList below;
    double total = 0.0;
    PolyhedralBoundedSolidModeler::split(brick, plane, above, below);
    EXPECT_EQ(1, above.size());
    EXPECT_EQ(2, below.size());
    for ( long i = 0; i < above.size(); i++ ) {
        java::String message;
        EXPECT_TRUE(PolyhedralBoundedSolidValidationEngine::validateStrict(
            above[i], &message));
        total += Oracle::solidVolume(above[i]);
    }
    for ( long i = 0; i < below.size(); i++ ) {
        java::String message;
        EXPECT_TRUE(PolyhedralBoundedSolidValidationEngine::validateStrict(
            below[i], &message));
        total += Oracle::solidVolume(below[i]);
    }
    EXPECT_NEAR(Oracle::solidVolume(brick), total, 1.0e-9);
    release(above);
    release(below);
    delete brick;
}
