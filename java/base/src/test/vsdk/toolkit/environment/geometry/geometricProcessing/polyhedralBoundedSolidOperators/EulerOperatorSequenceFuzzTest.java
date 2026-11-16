package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayDeque;
import java.util.Random;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologySummary;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;

import static org.assertj.core.api.Assertions.assertThat;

/**
Random sequences of Euler operators and their inverses (plan-stage11 Phase
B). On each solid a seeded sequence of edge splits (lmev) and face splits
(lmef) is applied and then undone in reverse order (lkev, lkef).

<p>After every single operator:</p>
<ul>
<li>the solid passes the intermediate validation (planar faces, every edge
    used by exactly two half-edges, closed loops);</li>
<li>the Euler-Poincare characteristic is the one of the start: lmev adds
    one vertex and one edge, lmef one edge and one face;</li>
<li>the volume is the one of the start: the new vertices lie on edges and
    the new edges inside faces.</li>
</ul>
<p>After the whole sequence is undone the counts of faces, edges and
vertices are the initial ones and the solid is strict-valid.</p>

<p>Traceability: [MANT1988] Sec. 9.2 (Euler operators and their inverses),
Programs 11.6 to 11.10.</p>
 */
class EulerOperatorSequenceFuzzTest
{
    private static final int STEPS = 40;

    @Test
    void given_corpusSolids_when_randomOperatorsAreAppliedAndUndone_then_solidIsRestored()
    {
        Random random = new Random(1109);

        for ( CsgSampleCorpus sample : CsgSampleCorpus.values() ) {
            PolyhedralBoundedSolid[] pair =
                CsgSampleCorpusFixtures.createPair(sample);
            exercise(sample + "/A", pair[0], random);
            exercise(sample + "/B", pair[1], random);
        }
    }

    @Test
    void given_starPrism_when_randomOperatorsAreAppliedAndUndone_then_solidIsRestored()
    {
        exercise("STAR7", PrismFixtures.prism(
            PrismFixtures.starPolygon(7, 1.0, 0.45), 1.0), new Random(5));
    }

    private static void exercise(String name, PolyhedralBoundedSolid solid,
        Random random)
    {
        PolyhedralBoundedSolidTopologySummary before =
            PolyhedralBoundedSolidTopologySummary.from(solid);
        double volume = PolyhedralBoundedSolidVolumeOracle.solidVolume(solid);
        // Undo stack: the edge each operator made, and whether lmev or
        // lmef made it.
        ArrayDeque<_PolyhedralBoundedSolidEdge> undo = new ArrayDeque<>();
        ArrayDeque<Integer> undoVertex = new ArrayDeque<>();
        int step;

        for ( step = 0; step < STEPS; step++ ) {
            String label = name + " step " + step;
            if ( random.nextBoolean() ) {
                undoVertex.push(Integer.valueOf(
                    splitRandomEdge(solid, random)));
                undo.push(newestEdge(solid));
                label += " lmev";
            }
            else {
                _PolyhedralBoundedSolidEdge made =
                    splitRandomFace(solid, random);
                if ( made == null ) {
                    continue;
                }
                undo.push(made);
                undoVertex.push(Integer.valueOf(-1));
                label += " lmef";
            }
            assertInvariants(label, solid, before, volume);
        }
        while ( !undo.isEmpty() ) {
            _PolyhedralBoundedSolidEdge edge = undo.pop();
            int vertexId = undoVertex.pop().intValue();
            String label = name + " undo, " + undo.size() + " left";
            if ( vertexId >= 0 ) {
                // lkev removes the starting vertex of its first argument:
                // the vertex lmev made, along the edge lmev made.
                _PolyhedralBoundedSolidHalfEdge he =
                    edge.rightHalf.startingVertex.id == vertexId ?
                    edge.rightHalf : edge.leftHalf;
                PolyhedralBoundedSolidEulerOperators.lkev(solid, he,
                    he.mirrorHalfEdge());
                label += " lkev";
            }
            else {
                PolyhedralBoundedSolidEulerOperators.lkef(solid,
                    edge.rightHalf, edge.leftHalf);
                label += " lkef";
            }
            assertInvariants(label, solid, before, volume);
        }

        PolyhedralBoundedSolidTopologySummary after =
            PolyhedralBoundedSolidTopologySummary.from(solid);
        assertThat(after.getFaceCount()).as(name + " faces")
            .isEqualTo(before.getFaceCount());
        assertThat(after.getEdgeCount()).as(name + " edges")
            .isEqualTo(before.getEdgeCount());
        assertThat(after.getVertexCount()).as(name + " vertices")
            .isEqualTo(before.getVertexCount());
        StringBuilder message = new StringBuilder();
        assertThat(PolyhedralBoundedSolidValidationEngine.validateStrict(
            solid, message)).as(name + " strict: " + message).isTrue();
    }

    private static void assertInvariants(String label,
        PolyhedralBoundedSolid solid,
        PolyhedralBoundedSolidTopologySummary before, double volume)
    {
        PolyhedralBoundedSolidTopologySummary now =
            PolyhedralBoundedSolidTopologySummary.from(solid);

        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(solid)).as(label + " valid").isTrue();
        assertThat(now.getAdjustedEulerCharacteristic())
            .as(label + " Euler-Poincare characteristic")
            .isEqualTo(before.getAdjustedEulerCharacteristic());
        assertThat(now.getShellCount()).as(label + " shells")
            .isEqualTo(before.getShellCount());
        assertThat(PolyhedralBoundedSolidVolumeOracle.solidVolume(solid))
            .as(label + " volume")
            .isCloseTo(volume, org.assertj.core.api.Assertions.offset(
                1.0e-9 * Math.max(1.0, Math.abs(volume))));
    }

    /** Splits a random edge at a point inside it; returns the new vertex id. */
    private static int splitRandomEdge(PolyhedralBoundedSolid solid,
        Random random)
    {
        _PolyhedralBoundedSolidEdge edge = solid.getEdgesList().get(
            random.nextInt(solid.getEdgesList().size()));
        _PolyhedralBoundedSolidHalfEdge he = edge.rightHalf;
        Vector3Dd a = he.startingVertex.position;
        Vector3Dd b = he.next().startingVertex.position;
        double t = 0.3 + 0.4 * random.nextDouble();
        int id = solid.getMaxVertexId() + 1;

        PolyhedralBoundedSolidEulerOperators.lmev(solid, he,
            he.mirrorHalfEdge().next(), id,
            a.add(b.subtract(a).multiply(t)));
        return id;
    }

    /**
    Cuts a corner off a random face without rings: a new edge from the
    vertex before a convex corner to the vertex after it. Returns the new
    edge, or null when the face drawn has no corner that can be cut (a
    triangle, a face with rings, or a corner whose cut would cross the
    boundary).
    */
    private static _PolyhedralBoundedSolidEdge splitRandomFace(
        PolyhedralBoundedSolid solid, Random random)
    {
        _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(
            random.nextInt(solid.getPolygonsList().size()));
        if ( face.boundariesList.size() != 1 ) {
            return null;
        }
        _PolyhedralBoundedSolidHalfEdge start =
            face.boundariesList.get(0).boundaryStartHalfEdge;
        int count = 0;
        _PolyhedralBoundedSolidHalfEdge he = start;
        do {
            count++;
            he = he.next();
        } while ( he != start );
        if ( count < 4 ) {
            return null;
        }
        Vector3Dd normal = face.getContainingPlane().getNormal();
        int skip = random.nextInt(count);
        for ( he = start; skip > 0; skip-- ) {
            he = he.next();
        }
        for ( int tries = 0; tries < count; tries++, he = he.next() ) {
            if ( !isEar(he, normal) ) {
                continue;
            }
            int faceCount = solid.getPolygonsList().size();
            _PolyhedralBoundedSolidFace made =
                PolyhedralBoundedSolidEulerOperators.lmef(solid, he,
                    he.next().next(), solid.getMaxFaceId() + 1);
            assertThat(solid.getPolygonsList().size())
                .isEqualTo(faceCount + 1);
            return made.boundariesList.get(0).boundaryStartHalfEdge
                .parentEdge == null ? null : newestEdge(solid);
        }
        return null;
    }

    /** lmev and lmef append the edge they make to the edge list. */
    private static _PolyhedralBoundedSolidEdge newestEdge(
        PolyhedralBoundedSolid solid)
    {
        return solid.getEdgesList().get(solid.getEdgesList().size() - 1);
    }

    /**
    True when the triangle (he, he.next, he.next.next) is an ear of the
    face: its corner at he.next is strictly convex and no other vertex of
    the loop lies inside it or on its border.
    */
    private static boolean isEar(_PolyhedralBoundedSolidHalfEdge he,
        Vector3Dd normal)
    {
        Vector3Dd a = he.startingVertex.position;
        Vector3Dd b = he.next().startingVertex.position;
        Vector3Dd c = he.next().next().startingVertex.position;
        double size = b.subtract(a).length() * c.subtract(b).length();

        if ( b.subtract(a).crossProduct(c.subtract(b)).dotProduct(normal) <=
             1.0e-6 * size ) {
            return false;
        }
        _PolyhedralBoundedSolidHalfEdge other = he.next().next().next();
        while ( other != he ) {
            Vector3Dd p = other.startingVertex.position;
            if ( side(a, b, p, normal) >= -1.0e-9 * size &&
                 side(b, c, p, normal) >= -1.0e-9 * size &&
                 side(c, a, p, normal) >= -1.0e-9 * size ) {
                return false;
            }
            other = other.next();
        }
        return true;
    }

    private static double side(Vector3Dd from, Vector3Dd to, Vector3Dd p,
        Vector3Dd normal)
    {
        return to.subtract(from).crossProduct(p.subtract(from))
            .dotProduct(normal);
    }
}
