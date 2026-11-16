#include <cmath>
#include <gtest/gtest.h>
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
#include "CsgSampleCorpusFixtures.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"
#include "PrismFixtures.h"

/*
Random sequences of Euler operators and their inverses (plan-stage11 Phase
B). On each solid a seeded sequence of edge splits (lmev) and face splits
(lmef) is applied and then undone in reverse order (lkev, lkef).

After every single operator:
- the solid passes the intermediate validation (planar faces, every edge
  used by exactly two half-edges, closed loops);
- the Euler-Poincare characteristic is the one of the start: lmev adds one
  vertex and one edge, lmef one edge and one face;
- the volume is the one of the start: the new vertices lie on edges and the
  new edges inside faces.
After the whole sequence is undone the counts of faces, edges and vertices
are the initial ones and the solid is strict-valid.

Traceability: [MANT1988] Sec. 9.2 (Euler operators and their inverses),
Programs 11.6 to 11.10.
C++ counterpart of Java's `EulerOperatorSequenceFuzzTest`. The Java
`ArrayDeque` undo stacks are `java::ArrayList` used at their end.
*/
namespace {

typedef PolyhedralBoundedSolidEulerOperators Euler;
typedef PolyhedralBoundedSolidTopologySummary Summary;

const int STEPS = 40;

/** lmev and lmef append the edge they make to the edge list. */
_PolyhedralBoundedSolidEdge* newestEdge(PolyhedralBoundedSolid* solid)
{
    return solid->getEdgesList().get(solid->getEdgesList().size() - 1);
}

double side(const Vector3Dd& from, const Vector3Dd& to, const Vector3Dd& p,
    const Vector3Dd& normal)
{
    return to.subtract(from).crossProduct(p.subtract(from))
        .dotProduct(normal);
}

/**
True when the triangle (he, he.next, he.next.next) is an ear of the face:
its corner at he.next is strictly convex and no other vertex of the loop
lies inside it or on its border.
*/
bool isEar(_PolyhedralBoundedSolidHalfEdge* he, const Vector3Dd& normal)
{
    Vector3Dd a = he->startingVertex->position;
    Vector3Dd b = he->next()->startingVertex->position;
    Vector3Dd c = he->next()->next()->startingVertex->position;
    double size = b.subtract(a).length() * c.subtract(b).length();
    if ( b.subtract(a).crossProduct(c.subtract(b)).dotProduct(normal) <=
         1.0e-6 * size ) {
        return false;
    }
    _PolyhedralBoundedSolidHalfEdge* other = he->next()->next()->next();
    while ( other != he ) {
        Vector3Dd p = other->startingVertex->position;
        if ( side(a, b, p, normal) >= -1.0e-9 * size &&
             side(b, c, p, normal) >= -1.0e-9 * size &&
             side(c, a, p, normal) >= -1.0e-9 * size ) {
            return false;
        }
        other = other->next();
    }
    return true;
}

/** Splits a random edge at a point inside it; returns the new vertex id. */
int splitRandomEdge(PolyhedralBoundedSolid* solid, java::Random& random)
{
    _PolyhedralBoundedSolidEdge* edge = solid->getEdgesList().get(
        random.nextInt(solid->getEdgesList().size()));
    _PolyhedralBoundedSolidHalfEdge* he = edge->rightHalf;
    Vector3Dd a = he->startingVertex->position;
    Vector3Dd b = he->next()->startingVertex->position;
    double t = 0.3 + 0.4 * random.nextDouble();
    int id = solid->getMaxVertexId() + 1;
    Euler::lmev(solid, he, he->mirrorHalfEdge()->next(), id,
        a.add(b.subtract(a).multiply(t)));
    return id;
}

/**
Cuts a corner off a random face without rings: a new edge from the vertex
before a convex corner to the vertex after it. Returns the new edge, or
null when the face drawn has no corner that can be cut (a triangle, a face
with rings, or a corner whose cut would cross the boundary).
*/
_PolyhedralBoundedSolidEdge* splitRandomFace(PolyhedralBoundedSolid* solid,
    java::Random& random)
{
    _PolyhedralBoundedSolidFace* face = solid->getPolygonsList().get(
        random.nextInt(solid->getPolygonsList().size()));
    if ( face->boundariesList.size() != 1 ) {
        return nullptr;
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
        return nullptr;
    }
    Vector3Dd normal = face->containingPlaneNormal();
    int skip = random.nextInt(count);
    for ( he = start; skip > 0; skip-- ) {
        he = he->next();
    }
    for ( int tries = 0; tries < count; tries++, he = he->next() ) {
        if ( !isEar(he, normal) ) {
            continue;
        }
        int faceCount = solid->getPolygonsList().size();
        _PolyhedralBoundedSolidFace* made = Euler::lmef(solid, he,
            he->next()->next(), solid->getMaxFaceId() + 1);
        EXPECT_EQ(faceCount + 1, solid->getPolygonsList().size());
        return made->boundariesList.get(0)->boundaryStartHalfEdge
            ->parentEdge == nullptr ? nullptr : newestEdge(solid);
    }
    return nullptr;
}

void assertInvariants(const java::String& label,
    PolyhedralBoundedSolid* solid, const Summary& before, double volume)
{
    Summary now = Summary::from(solid);
    EXPECT_TRUE(PolyhedralBoundedSolidValidationEngine::validateIntermediate(
        solid)) << label.c_str() << " valid";
    EXPECT_EQ(before.getAdjustedEulerCharacteristic(),
        now.getAdjustedEulerCharacteristic())
        << label.c_str() << " Euler-Poincare characteristic";
    EXPECT_EQ(before.getShellCount(), now.getShellCount())
        << label.c_str() << " shells";
    EXPECT_NEAR(volume,
        PolyhedralBoundedSolidVolumeOracle::solidVolume(solid),
        1.0e-9 * std::fmax(1.0, std::fabs(volume)))
        << label.c_str() << " volume";
}

/** The solid is released at the end (Java: garbage collector). */
void exercise(const java::String& name, PolyhedralBoundedSolid* solid,
    java::Random& random)
{
    Summary before = Summary::from(solid);
    double volume = PolyhedralBoundedSolidVolumeOracle::solidVolume(solid);
    // Undo stack: the edge each operator made, and whether lmev or lmef
    // made it.
    java::ArrayList<_PolyhedralBoundedSolidEdge*> undo;
    java::ArrayList<int> undoVertex;
    int step;

    for ( step = 0; step < STEPS; step++ ) {
        java::String label = name + " step " + java::String::valueOf(step);
        if ( random.nextBoolean() ) {
            undoVertex.add(splitRandomEdge(solid, random));
            undo.add(newestEdge(solid));
            label += " lmev";
        }
        else {
            _PolyhedralBoundedSolidEdge* made = splitRandomFace(solid, random);
            if ( made == nullptr ) {
                continue;
            }
            undo.add(made);
            undoVertex.add(-1);
            label += " lmef";
        }
        assertInvariants(label, solid, before, volume);
    }
    while ( !undo.isEmpty() ) {
        _PolyhedralBoundedSolidEdge* edge = undo.get(undo.size() - 1);
        undo.remove(undo.size() - 1);
        int vertexId = undoVertex.get(undoVertex.size() - 1);
        undoVertex.remove(undoVertex.size() - 1);
        java::String label = name + " undo, " +
            java::String::valueOf((int)undo.size()) + " left";
        if ( vertexId >= 0 ) {
            // lkev removes the starting vertex of its first argument: the
            // vertex lmev made, along the edge lmev made.
            _PolyhedralBoundedSolidHalfEdge* he =
                edge->rightHalf->startingVertex->id == vertexId ?
                edge->rightHalf : edge->leftHalf;
            Euler::lkev(solid, he, he->mirrorHalfEdge());
            label += " lkev";
        }
        else {
            Euler::lkef(solid, edge->rightHalf, edge->leftHalf);
            label += " lkef";
        }
        assertInvariants(label, solid, before, volume);
    }
    Summary after = Summary::from(solid);
    EXPECT_EQ(before.getFaceCount(), after.getFaceCount())
        << name.c_str() << " faces";
    EXPECT_EQ(before.getEdgeCount(), after.getEdgeCount())
        << name.c_str() << " edges";
    EXPECT_EQ(before.getVertexCount(), after.getVertexCount())
        << name.c_str() << " vertices";
    java::String message;
    EXPECT_TRUE(PolyhedralBoundedSolidValidationEngine::validateStrict(
        solid, &message)) << name.c_str() << " strict: " << message.c_str();
    delete solid;
}

}

TEST(EulerOperatorSequenceFuzzTest, CorpusSolidsRandomOperatorsAreAppliedAndUndoneSolidIsRestored) {
    java::Random random(1109);
    for ( int s = 0; s < CSG_SAMPLE_CORPUS_COUNT; s++ ) {
        CsgSampleCorpus sample = CSG_SAMPLE_CORPUS_VALUES[s];
        java::ArrayList<PolyhedralBoundedSolid*> pair =
            CsgSampleCorpusFixtures::createPair(sample);
        exercise(java::String(csgSampleCorpusName(sample)) + "/A", pair[0],
            random);
        exercise(java::String(csgSampleCorpusName(sample)) + "/B", pair[1],
            random);
    }
}

TEST(EulerOperatorSequenceFuzzTest, StarPrismRandomOperatorsAreAppliedAndUndoneSolidIsRestored) {
    java::Random random(5);
    exercise("STAR7", PrismFixtures::prism(
        PrismFixtures::starPolygon(7, 1.0, 0.45), 1.0), random);
}
