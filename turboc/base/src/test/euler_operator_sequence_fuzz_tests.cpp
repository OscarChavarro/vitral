// C++ port of Java's EulerOperatorSequenceFuzzTest
// (environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators).
#include <math.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "VitralTest.h"
#include "CsgSampleCorpus.h"
#include "CsgSampleCorpusFixtures.h"
#include "PolyhedralBoundedSolidTestFixtures.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"
#include "PrismFixtures.h"
#include "java/util/ArrayDeque.h"
#include "java/util/ArrayList.txx"
#include "java/util/Random.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

typedef PolyhedralBoundedSolidEulerOperators Euler;
typedef PolyhedralBoundedSolidTopologySummary TopologySummary;
typedef PolyhedralBoundedSolidVolumeOracle Oracle;

static const int STEPS = 40;

static void
assertInvariants(const String& label, PolyhedralBoundedSolid* solid,
    const TopologySummary& before, double volume)
{
    TopologySummary now = TopologySummary::from(solid);
    BOOLEAN valid =
        PolyhedralBoundedSolidValidationEngine::validateIntermediate(solid);
    double scale = fabs(volume) > 1.0 ? fabs(volume) : 1.0;
    double current = Oracle::solidVolume(solid);

    if ( !valid ||
         now.getAdjustedEulerCharacteristic() !=
             before.getAdjustedEulerCharacteristic() ||
         now.getShellCount() != before.getShellCount() ||
         fabs(current - volume) > 1.0e-9 * scale ) {
        printf("%s\n", label.c_str());
    }
    // valid
    EXPECT_TRUE(valid);
    // Euler-Poincare characteristic
    EXPECT_EQ(before.getAdjustedEulerCharacteristic(),
        now.getAdjustedEulerCharacteristic());
    // shells
    EXPECT_EQ(before.getShellCount(), now.getShellCount());
    // volume
    EXPECT_NEAR(volume, current, 1.0e-9 * scale);
}

/** lmev and lmef append the edge they make to the edge list. */
static _PolyhedralBoundedSolidEdge*
newestEdge(PolyhedralBoundedSolid* solid)
{
    return solid->getEdgesList().get(solid->getEdgesList().size() - 1);
}

/** Splits a random edge at a point inside it; returns the new vertex id. */
static int
splitRandomEdge(PolyhedralBoundedSolid* solid, Random& random)
{
    _PolyhedralBoundedSolidEdge* edge = solid->getEdgesList().get(
        random.nextInt((int)solid->getEdgesList().size()));
    _PolyhedralBoundedSolidHalfEdge* he = edge->rightHalf;
    Vector3Dd a = he->startingVertex->position;
    Vector3Dd b = he->next()->startingVertex->position;
    double t = 0.3 + 0.4 * random.nextDouble();
    int id = solid->getMaxVertexId() + 1;

    Euler::lmev(solid, he, he->mirrorHalfEdge()->next(), id,
        a.add(b.subtract(a).multiply(t)));
    return id;
}

static double
side(const Vector3Dd& from, const Vector3Dd& to, const Vector3Dd& p,
    const Vector3Dd& normal)
{
    return to.subtract(from).crossProduct(p.subtract(from))
        .dotProduct(normal);
}

/**
True when the triangle (he, he.next, he.next.next) is an ear of the
face: its corner at he.next is strictly convex and no other vertex of
the loop lies inside it or on its border.
*/
static BOOLEAN
isEar(_PolyhedralBoundedSolidHalfEdge* he, const Vector3Dd& normal)
{
    Vector3Dd a = he->startingVertex->position;
    Vector3Dd b = he->next()->startingVertex->position;
    Vector3Dd c = he->next()->next()->startingVertex->position;
    double size = b.subtract(a).length() * c.subtract(b).length();

    if ( b.subtract(a).crossProduct(c.subtract(b)).dotProduct(normal) <=
         1.0e-6 * size ) {
        return FALSE;
    }
    _PolyhedralBoundedSolidHalfEdge* other = he->next()->next()->next();
    while ( other != he ) {
        Vector3Dd p = other->startingVertex->position;
        if ( side(a, b, p, normal) >= -1.0e-9 * size &&
             side(b, c, p, normal) >= -1.0e-9 * size &&
             side(c, a, p, normal) >= -1.0e-9 * size ) {
            return FALSE;
        }
        other = other->next();
    }
    return TRUE;
}

/**
Cuts a corner off a random face without rings: a new edge from the
vertex before a convex corner to the vertex after it. Returns the new
edge, or null when the face drawn has no corner that can be cut (a
triangle, a face with rings, or a corner whose cut would cross the
boundary).
*/
static _PolyhedralBoundedSolidEdge*
splitRandomFace(PolyhedralBoundedSolid* solid, Random& random)
{
    _PolyhedralBoundedSolidFace* face = solid->getPolygonsList().get(
        random.nextInt((int)solid->getPolygonsList().size()));
    if ( face->boundariesList.size() != 1 ) {
        return NULL;
    }
    _PolyhedralBoundedSolidHalfEdge* start =
        face->boundariesList.get(0)->boundaryStartHalfEdge;
    int count = 0;
    _PolyhedralBoundedSolidHalfEdge* he = start;
    do {
        count++;
        he = he->next();
    } while ( he != start );
    if ( count < 4 ) {
        return NULL;
    }
    // C++ port note: getContainingPlane() returns a new plane, owned here
    InfinitePlane* plane = face->getContainingPlane();
    Vector3Dd normal = plane->getNormal();
    delete plane;
    int skip = random.nextInt(count);
    for ( he = start; skip > 0; skip-- ) {
        he = he->next();
    }
    int tries;
    for ( tries = 0; tries < count; tries++, he = he->next() ) {
        if ( !isEar(he, normal) ) {
            continue;
        }
        long faceCount = solid->getPolygonsList().size();
        _PolyhedralBoundedSolidFace* made = Euler::lmef(solid, he,
            he->next()->next(), solid->getMaxFaceId() + 1);
        EXPECT_EQ(faceCount + 1, solid->getPolygonsList().size());
        if ( made->boundariesList.get(0)->boundaryStartHalfEdge->parentEdge ==
             NULL ) {
            return NULL;
        }
        return newestEdge(solid);
    }
    return NULL;
}

static void
exercise(const String& name, PolyhedralBoundedSolid* solid, Random& random)
{
    TopologySummary before = TopologySummary::from(solid);
    double volume = Oracle::solidVolume(solid);
    // Undo stack: the edge each operator made, and whether lmev or
    // lmef made it.
    ArrayDeque<_PolyhedralBoundedSolidEdge*> undo;
    ArrayDeque<int> undoVertex;
    int step;
    char number[32];

    for ( step = 0; step < STEPS; step++ ) {
        sprintf(number, "%d", step);
        String label = name + " step " + number;
        if ( random.nextBoolean() ) {
            undoVertex.push(splitRandomEdge(solid, random));
            undo.push(newestEdge(solid));
            label = label + " lmev";
        }
        else {
            _PolyhedralBoundedSolidEdge* made =
                splitRandomFace(solid, random);
            if ( made == NULL ) {
                continue;
            }
            undo.push(made);
            undoVertex.push(-1);
            label = label + " lmef";
        }
        assertInvariants(label, solid, before, volume);
    }
    while ( !undo.isEmpty() ) {
        _PolyhedralBoundedSolidEdge* edge = undo.pop();
        int vertexId = undoVertex.pop();
        sprintf(number, "%ld", undo.size());
        String label = name + " undo, " + number + " left";
        if ( vertexId >= 0 ) {
            // lkev removes the starting vertex of its first argument:
            // the vertex lmev made, along the edge lmev made.
            _PolyhedralBoundedSolidHalfEdge* he =
                edge->rightHalf->startingVertex->id == vertexId ?
                edge->rightHalf : edge->leftHalf;
            Euler::lkev(solid, he, he->mirrorHalfEdge());
            label = label + " lkev";
        }
        else {
            Euler::lkef(solid, edge->rightHalf, edge->leftHalf);
            label = label + " lkef";
        }
        assertInvariants(label, solid, before, volume);
    }

    TopologySummary after = TopologySummary::from(solid);
    printf("%s\n", name.c_str());
    // faces
    EXPECT_EQ(before.getFaceCount(), after.getFaceCount());
    // edges
    EXPECT_EQ(before.getEdgeCount(), after.getEdgeCount());
    // vertices
    EXPECT_EQ(before.getVertexCount(), after.getVertexCount());
    String message;
    BOOLEAN strict =
        PolyhedralBoundedSolidValidationEngine::validateStrict(solid,
            &message);
    if ( !strict ) {
        printf("%s strict: %s\n", name.c_str(), message.c_str());
    }
    EXPECT_TRUE(strict);
}

TEST(EulerOperatorSequenceFuzz, given_corpusSolids_when_randomOperatorsAreAppliedAndUndone_then_solidIsRestored)
{
    Random random(1109);
    int sampleIndex;

    for ( sampleIndex = 0; sampleIndex < CSG_SAMPLE_CORPUS_COUNT;
          sampleIndex++ ) {
        CsgSampleCorpus sample = (CsgSampleCorpus)sampleIndex;
        ArrayList<PolyhedralBoundedSolid*> pair =
            CsgSampleCorpusFixtures::createPair(sample);
        exercise(String(csgSampleCorpusName(sample)) + "/A", pair[0],
            random);
        exercise(String(csgSampleCorpusName(sample)) + "/B", pair[1],
            random);
        PolyhedralBoundedSolidTestFixtures::dispose(pair);
    }
}

TEST(EulerOperatorSequenceFuzz, given_starPrism_when_randomOperatorsAreAppliedAndUndone_then_solidIsRestored)
{
    Random random(5);
    PolyhedralBoundedSolid* solid = PrismFixtures::prism(
        PrismFixtures::starPolygon(7, 1.0, 0.45), 1.0);

    exercise("STAR7", solid, random);
    delete solid;
}

VITRAL_TEST_MAIN()
