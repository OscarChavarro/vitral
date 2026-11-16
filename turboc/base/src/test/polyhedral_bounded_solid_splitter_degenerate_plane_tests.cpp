// C++ port of Java's PolyhedralBoundedSolidSplitterDegeneratePlaneTest
// (environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators).
#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "CsgSampleCorpus.h"
#include "CsgSampleCorpusFixtures.h"
#include "PolyhedralBoundedSolidTestFixtures.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

typedef PolyhedralBoundedSolidVolumeOracle Oracle;
typedef PolyhedralBoundedSolidVolumeOracle::OrientedBox OrientedBox;
typedef PolyhedralBoundedSolidModeler Modeler;
typedef PolyhedralBoundedSolidValidationEngine Validation;

static const double TOLERANCE = 1.0e-6;

static void
assertInputUnchanged(PolyhedralBoundedSolid* solid, const OrientedBox& cube,
    const ArrayList<PolyhedralBoundedSolid*>& above,
    const ArrayList<PolyhedralBoundedSolid*>& below)
{
    long i;

    // input faces after split
    EXPECT_EQ(6, solid->getPolygonsList().size());
    // input volume after split
    EXPECT_NEAR(cube.volume(), Oracle::solidVolume(solid), TOLERANCE);
    // above holds the input object
    for ( i = 0; i < above.size(); i++ ) {
        EXPECT_NE(solid, above[i]);
    }
    // below holds the input object
    for ( i = 0; i < below.size(); i++ ) {
        EXPECT_NE(solid, below[i]);
    }
}

static double
assertValidPieceOnSide(PolyhedralBoundedSolid* piece,
    const InfinitePlane& plane, int side)
{
    String strictReport;
    long i;

    // piece face count
    EXPECT_GE(piece->getPolygonsList().size(), 4);
    BOOLEAN strict = Validation::validateStrict(piece, &strictReport);
    if ( !strict ) {
        printf("strict validation: %s\n", strictReport.c_str());
    }
    EXPECT_TRUE(strict);
    for ( i = 0; i < piece->getVerticesList().size(); i++ ) {
        // vertex side
        EXPECT_GE(side * plane.pointDistance(
            piece->getVerticesList().get(i)->position), -TOLERANCE);
    }
    return Oracle::solidVolume(piece);
}

static void
cutAndCheck(const char* name, const Vector3Dd& normal, const Vector3Dd& point)
{
    OrientedBox cube(2, 2, 2, Oracle::translation(0, 0, 0));
    PolyhedralBoundedSolid* solid = cube.toSolid();
    InfinitePlane plane(normal.normalized(), point);
    ArrayList<PolyhedralBoundedSolid*> above;
    ArrayList<PolyhedralBoundedSolid*> below;
    double total = 0.0;
    long i;

    printf("%s\n", name);
    Modeler::split(solid, plane, above, below);

    assertInputUnchanged(solid, cube, above, below);
    // pieces above
    EXPECT_FALSE(above.isEmpty());
    // pieces below
    EXPECT_FALSE(below.isEmpty());
    for ( i = 0; i < above.size(); i++ ) {
        total += assertValidPieceOnSide(above[i], plane, +1);
    }
    for ( i = 0; i < below.size(); i++ ) {
        total += assertValidPieceOnSide(below[i], plane, -1);
    }
    EXPECT_NEAR(cube.volume(), total, TOLERANCE);
    PolyhedralBoundedSolidTestFixtures::dispose(above);
    PolyhedralBoundedSolidTestFixtures::dispose(below);
    delete solid;
}

/**
C++ port note: the parameterized tests are loops over their Java rows.
*/
TEST(PolyhedralBoundedSolidSplitterDegeneratePlane, given_cuttingPlane_when_split_then_volumeIsConservedOnTheRightSides)
{
    cutAndCheck("generic oblique plane",
        Vector3Dd(0.3, 0.5, 0.8), Vector3Dd(0.1, 0, 0.05));
    cutAndCheck("axis plane through the middle",
        Vector3Dd(0, 0, 1), Vector3Dd(0, 0, 0.1));
    cutAndCheck("plane containing one cube edge",
        Vector3Dd(1, -0.5, 0), Vector3Dd(1, 1, 0));
    cutAndCheck("diagonal plane through two opposite edges",
        Vector3Dd(1, -1, 0), Vector3Dd(0, 0, 0));
    // S-01 (Stage E): planes through vertices.
    cutAndCheck("corner cut through three vertices (x+y+z=1)",
        Vector3Dd(1, 1, 1), Vector3Dd(1, 1, -1));
    cutAndCheck("generic plane through exactly one vertex",
        Vector3Dd(1, 0.7, 0.4), Vector3Dd(1, 1, -1));
    cutAndCheck("corner cut with the normal reversed",
        Vector3Dd(-1, -1, -1), Vector3Dd(1, 1, -1));
    cutAndCheck("hexagonal section through six edge midpoints",
        Vector3Dd(1, 1, 1), Vector3Dd(0, 0, 0));
}

static void
missAndCheck(const char* name, const Vector3Dd& normal,
    const Vector3Dd& point, int side)
{
    OrientedBox cube(2, 2, 2, Oracle::translation(0, 0, 0));
    PolyhedralBoundedSolid* solid = cube.toSolid();
    InfinitePlane plane(normal.normalized(), point);
    ArrayList<PolyhedralBoundedSolid*> above;
    ArrayList<PolyhedralBoundedSolid*> below;

    printf("%s\n", name);
    Modeler::split(solid, plane, above, below);

    assertInputUnchanged(solid, cube, above, below);
    EXPECT_EQ(1, above.size());
    EXPECT_EQ(1, below.size());
    if ( above.size() == 1 && below.size() == 1 ) {
        PolyhedralBoundedSolid* whole = side > 0 ? above[0] : below[0];
        PolyhedralBoundedSolid* empty = side > 0 ? below[0] : above[0];
        EXPECT_EQ(0, empty->getPolygonsList().size());
        EXPECT_NEAR(cube.volume(), assertValidPieceOnSide(whole, plane, side),
            TOLERANCE);
    }
    PolyhedralBoundedSolidTestFixtures::dispose(above);
    PolyhedralBoundedSolidTestFixtures::dispose(below);
    delete solid;
}

/**
A plane that does not cut the solid leaves it whole: a copy goes to the
list of the side it lies on, an empty solid to the other one. Before
Stage E the input object itself always went to "above".
*/
TEST(PolyhedralBoundedSolidSplitterDegeneratePlane, given_planeThatDoesNotCut_when_split_then_aCopyGoesToItsSide)
{
    missAndCheck("plane missing the cube, cube below",
        Vector3Dd(0, 0, 1), Vector3Dd(0, 0, 5), -1);
    missAndCheck("plane missing the cube, cube above",
        Vector3Dd(0, 0, 1), Vector3Dd(0, 0, -5), +1);
    missAndCheck("plane touching one vertex from outside",
        Vector3Dd(1, 1, 1), Vector3Dd(1, 1, 1), -1);
    missAndCheck("plane containing the top face",
        Vector3Dd(0, 0, 1), Vector3Dd(0, 0, 1), -1);
    missAndCheck("plane containing the bottom face",
        Vector3Dd(0, 0, 1), Vector3Dd(0, 0, -1), +1);
    missAndCheck("plane touching one edge from outside",
        Vector3Dd(1, 1, 0), Vector3Dd(1, 1, 0), -1);
}

/**
A plane through the lower reflex vertex of the L-shaped hollow brick
cuts a section of two regions that touch at that vertex. Above the
plane the piece is one solid whose section has two faces sharing the
vertex; below, the two regions meet only at the vertex and come out as
two solids. Every piece is strict-valid and the volume is conserved.
*/
TEST(PolyhedralBoundedSolidSplitterDegeneratePlane, given_planeThroughReflexVertex_when_sectionIsPinched_then_simpleFaces)
{
    ArrayList<PolyhedralBoundedSolid*> pair =
        CsgSampleCorpusFixtures::createPair(HOLLOW_BRICK);
    PolyhedralBoundedSolid* brick = pair[0];
    InfinitePlane plane(Vector3Dd(-0.85, 1.14, 0.19).normalized(),
        Vector3Dd(0.2, 0.8, 0.0));
    ArrayList<PolyhedralBoundedSolid*> above;
    ArrayList<PolyhedralBoundedSolid*> below;
    double total = 0.0;
    String message;
    long i;

    Modeler::split(brick, plane, above, below);

    EXPECT_EQ(1, above.size());
    EXPECT_EQ(2, below.size());
    for ( i = 0; i < above.size(); i++ ) {
        message = "";
        EXPECT_TRUE(Validation::validateStrict(above[i], &message));
        total += Oracle::solidVolume(above[i]);
    }
    for ( i = 0; i < below.size(); i++ ) {
        message = "";
        EXPECT_TRUE(Validation::validateStrict(below[i], &message));
        total += Oracle::solidVolume(below[i]);
    }
    EXPECT_NEAR(Oracle::solidVolume(brick), total, 1.0e-9);
    PolyhedralBoundedSolidTestFixtures::dispose(above);
    PolyhedralBoundedSolidTestFixtures::dispose(below);
    PolyhedralBoundedSolidTestFixtures::dispose(pair);
}

VITRAL_TEST_MAIN()
