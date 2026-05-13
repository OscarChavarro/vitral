package vsdk.toolkit.processing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.volume.Cone;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class CsgMoonCylinderDifferenceDegeneracyTest
{
    private static final double RADIUS = 0.15;
    private static final double HEIGHT = 0.5;
    private static final double TOLERANCE = 1.0e-9;
    private static final Vector3D MOON_OFFSET =
        new Vector3D(0.11, 0.0, 0.06);

    @ParameterizedTest(name = "{0}")
    @MethodSource("moonCylinderResolutionVariants")
    void given_offsetCylindersWithResolutionVariants_when_subtracting_then_moonHasNoDegenerateTopology(
        String label,
        int aRadialDivisions,
        int aHeightDivisions,
        int bRadialDivisions,
        int bHeightDivisions)
    {
        PolyhedralBoundedSolid moon;

        try {
            moon = createMoon(aRadialDivisions, aHeightDivisions,
                bRadialDivisions, bHeightDivisions);
        }
        catch ( StackOverflowError error ) {
            fail(label + " should build the moon without overflowing the " +
                "boolean pipeline stack", error);
            return;
        }
        catch ( RuntimeException error ) {
            fail(label + " should build the moon without runtime failures",
                error);
            return;
        }

        assertThat(auditDegenerateTopology(moon))
            .as(label)
            .isEmpty();
    }

    private static Stream<Arguments> moonCylinderResolutionVariants()
    {
        return Stream.of(
            Arguments.of("same radial and height: A(16,1) - B(16,1)",
                16, 1, 16, 1),
            Arguments.of("kurlander baseline: A(30,1) - B(30,1)",
                30, 1, 30, 1),
            Arguments.of("same radial and height: A(24,8) - B(24,8)",
                24, 8, 24, 8),
            Arguments.of("same radial and height: A(30,8) - B(30,8)",
                30, 8, 30, 8),
            Arguments.of("same radial, B taller discretization: A(16,1) - B(16,8)",
                16, 1, 16, 8),
            Arguments.of("same radial, A taller discretization: A(16,8) - B(16,1)",
                16, 8, 16, 1),
            Arguments.of("same radial, B taller discretization: A(30,1) - B(30,8)",
                30, 1, 30, 8),
            Arguments.of("same radial, A taller discretization: A(30,8) - B(30,1)",
                30, 8, 30, 1),
            Arguments.of("different radial, same height: A(24,1) - B(16,1)",
                24, 1, 16, 1),
            Arguments.of("different radial, same height: A(16,1) - B(24,1)",
                16, 1, 24, 1),
            Arguments.of("different radial, same height: A(30,1) - B(24,1)",
                30, 1, 24, 1),
            Arguments.of("different radial, same height: A(24,1) - B(30,1)",
                24, 1, 30, 1),
            Arguments.of("different radial, same height: A(36,1) - B(30,1)",
                36, 1, 30, 1),
            Arguments.of("different radial, same height: A(30,1) - B(36,1)",
                30, 1, 36, 1),
            Arguments.of("different radial and height: A(12,4) - B(16,8)",
                12, 4, 16, 8),
            Arguments.of("different radial and height: A(16,8) - B(12,4)",
                16, 8, 12, 4),
            Arguments.of("different radial and height: A(30,8) - B(24,4)",
                30, 8, 24, 4),
            Arguments.of("different radial and height: A(24,4) - B(30,8)",
                24, 4, 30, 8),
            Arguments.of("different radial and height: A(8,2) - B(12,3)",
                8, 2, 12, 3),
            Arguments.of("different radial and height: A(12,3) - B(8,2)",
                12, 3, 8, 2)
        );
    }

    private static PolyhedralBoundedSolid createMoon(
        int aRadialDivisions,
        int aHeightDivisions,
        int bRadialDivisions,
        int bHeightDivisions)
    {
        PolyhedralBoundedSolid a = createCylinder(aRadialDivisions,
            aHeightDivisions);
        PolyhedralBoundedSolid b = createCylinder(bRadialDivisions,
            bHeightDivisions);
        Matrix4x4 translation = new Matrix4x4();

        translation = translation.translation(MOON_OFFSET);
        PolyhedralBoundedSolidModeler.applyTransformation(b, translation);
        return PolyhedralBoundedSolidModeler.setOp(
            a, b, PolyhedralBoundedSolidModeler.SUBTRACT, false);
    }

    private static PolyhedralBoundedSolid createCylinder(
        int radialDivisions,
        int heightDivisions)
    {
        PolyhedralBoundedSolid cylinder = new Cone(RADIUS, RADIUS, HEIGHT)
            .exportToPolyhedralBoundedSolid(radialDivisions, heightDivisions);

        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(cylinder)).isTrue();
        return cylinder;
    }

    private static List<String> auditDegenerateTopology(
        PolyhedralBoundedSolid solid)
    {
        List<String> failures = new ArrayList<String>();

        if ( solid == null ) {
            failures.add("moon result is null");
            return failures;
        }

        if ( solid.getVerticesList().size() <= 0 ) {
            failures.add("moon result has no vertices");
        }
        if ( solid.getEdgesList().size() <= 0 ) {
            failures.add("moon result has no edges");
        }
        if ( solid.getPolygonsList().size() <= 0 ) {
            failures.add("moon result has no faces");
        }
        if ( !PolyhedralBoundedSolidValidationEngine
                 .validateIntermediate(solid) ) {
            failures.add("validateIntermediate rejected the moon result");
        }
        if ( !PolyhedralBoundedSolidValidationEngine.validateStrict(solid) ) {
            failures.add("validateStrict rejected the moon result");
        }

        auditVertices(solid, failures);
        auditEdges(solid, failures);
        auditVertexValence(solid, failures);
        auditFacesAndLoops(solid, failures);
        return failures;
    }

    private static void auditVertices(PolyhedralBoundedSolid solid,
                                      List<String> failures)
    {
        Set<Integer> vertexIds = new HashSet<Integer>();
        int i;
        int j;

        for ( i = 0; i < solid.getVerticesList().size(); i++ ) {
            _PolyhedralBoundedSolidVertex vertex =
                solid.getVerticesList().get(i);

            if ( vertex == null ) {
                failures.add("vertex list contains a null vertex at index " +
                    i);
                continue;
            }
            if ( !vertexIds.add(Integer.valueOf(vertex.id)) ) {
                failures.add("vertex id " + vertex.id + " is repeated");
            }
            if ( vertex.position == null ) {
                failures.add("vertex " + vertex.id + " has null position");
            }
            if ( vertex.emanatingHalfEdge == null ) {
                failures.add("vertex " + vertex.id +
                    " has no emanating half-edge");
            }
            else if ( vertex.emanatingHalfEdge.startingVertex == null ) {
                failures.add("vertex " + vertex.id +
                    " points to an emanating half-edge with no start vertex");
            }
            else if ( vertex.emanatingHalfEdge.startingVertex != vertex ) {
                failures.add("vertex " + vertex.id +
                    " points to an emanating half-edge that starts at vertex " +
                    vertex.emanatingHalfEdge.startingVertex.id);
            }
        }

        for ( i = 0; i < solid.getVerticesList().size(); i++ ) {
            _PolyhedralBoundedSolidVertex first =
                solid.getVerticesList().get(i);
            if ( first == null || first.position == null ) {
                continue;
            }
            for ( j = i + 1; j < solid.getVerticesList().size(); j++ ) {
                _PolyhedralBoundedSolidVertex second =
                    solid.getVerticesList().get(j);
                if ( second == null || second.position == null ) {
                    continue;
                }
                if ( samePosition(first.position, second.position) ) {
                    failures.add("vertices " + first.id + " and " +
                        second.id + " share the same position " +
                        first.position);
                }
            }
        }
    }

    private static void auditEdges(PolyhedralBoundedSolid solid,
                                   List<String> failures)
    {
        Set<Integer> edgeIds = new HashSet<Integer>();
        int i;

        for ( i = 0; i < solid.getEdgesList().size(); i++ ) {
            _PolyhedralBoundedSolidEdge edge = solid.getEdgesList().get(i);

            if ( edge == null ) {
                failures.add("edge list contains a null edge at index " + i);
                continue;
            }
            if ( !edgeIds.add(Integer.valueOf(edge.id)) ) {
                failures.add("edge id " + edge.id + " is repeated");
            }
            if ( edge.rightHalf == null ) {
                failures.add("edge " + edge.id + " has null right half");
            }
            if ( edge.leftHalf == null ) {
                failures.add("edge " + edge.id + " has null left half");
            }
            if ( edge.rightHalf == null || edge.leftHalf == null ) {
                continue;
            }
            if ( edge.rightHalf.parentEdge != edge ) {
                failures.add("edge " + edge.id +
                    " right half does not point back to the edge");
            }
            if ( edge.leftHalf.parentEdge != edge ) {
                failures.add("edge " + edge.id +
                    " left half does not point back to the edge");
            }
            if ( edge.rightHalf.startingVertex == null ||
                 edge.leftHalf.startingVertex == null ) {
                failures.add("edge " + edge.id +
                    " has a half-edge without starting vertex");
                continue;
            }
            if ( samePosition(edge.rightHalf.startingVertex.position,
                    edge.leftHalf.startingVertex.position) ) {
                failures.add("edge " + edge.id +
                    " is geometrically zero-length between vertex " +
                    edge.rightHalf.startingVertex.id + " and vertex " +
                    edge.leftHalf.startingVertex.id);
            }
        }
    }

    private static void auditVertexValence(PolyhedralBoundedSolid solid,
                                           List<String> failures)
    {
        Map<_PolyhedralBoundedSolidVertex, Integer> edgeValence =
            new HashMap<_PolyhedralBoundedSolidVertex, Integer>();
        Map<_PolyhedralBoundedSolidVertex, Integer> outgoingHalfEdges =
            new HashMap<_PolyhedralBoundedSolidVertex, Integer>();
        int i;
        int j;

        for ( i = 0; i < solid.getVerticesList().size(); i++ ) {
            _PolyhedralBoundedSolidVertex vertex =
                solid.getVerticesList().get(i);
            if ( vertex != null ) {
                edgeValence.put(vertex, Integer.valueOf(0));
                outgoingHalfEdges.put(vertex, Integer.valueOf(0));
            }
        }

        for ( i = 0; i < solid.getEdgesList().size(); i++ ) {
            _PolyhedralBoundedSolidEdge edge = solid.getEdgesList().get(i);
            if ( edge == null ) {
                continue;
            }
            countEdgeEndpoint(edgeValence, edge.rightHalf);
            countEdgeEndpoint(edgeValence, edge.leftHalf);
        }

        for ( i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);
            if ( face == null || face.boundariesList == null ) {
                continue;
            }
            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                countLoopOutgoingHalfEdges(outgoingHalfEdges,
                    face.boundariesList.get(j));
            }
        }

        for ( i = 0; i < solid.getVerticesList().size(); i++ ) {
            _PolyhedralBoundedSolidVertex vertex =
                solid.getVerticesList().get(i);
            if ( vertex == null ) {
                continue;
            }
            if ( edgeValence.get(vertex).intValue() < 3 ) {
                failures.add("vertex " + vertex.id +
                    " has edge valence " + edgeValence.get(vertex));
            }
            if ( outgoingHalfEdges.get(vertex).intValue() < 3 ) {
                failures.add("vertex " + vertex.id +
                    " has only " + outgoingHalfEdges.get(vertex) +
                    " outgoing half-edges");
            }
            if ( !edgeValence.get(vertex).equals(outgoingHalfEdges.get(vertex)) ) {
                failures.add("vertex " + vertex.id +
                    " has edge valence " + edgeValence.get(vertex) +
                    " but " + outgoingHalfEdges.get(vertex) +
                    " outgoing half-edges");
            }
        }
    }

    private static void countEdgeEndpoint(
        Map<_PolyhedralBoundedSolidVertex, Integer> edgeValence,
        _PolyhedralBoundedSolidHalfEdge halfEdge)
    {
        if ( halfEdge == null || halfEdge.startingVertex == null ||
             !edgeValence.containsKey(halfEdge.startingVertex) ) {
            return;
        }
        edgeValence.put(halfEdge.startingVertex,
            Integer.valueOf(edgeValence.get(halfEdge.startingVertex)
                .intValue() + 1));
    }

    private static void countLoopOutgoingHalfEdges(
        Map<_PolyhedralBoundedSolidVertex, Integer> outgoingHalfEdges,
        _PolyhedralBoundedSolidLoop loop)
    {
        int i;

        if ( loop == null || loop.halfEdgesList == null ) {
            return;
        }
        for ( i = 0; i < loop.halfEdgesList.size(); i++ ) {
            _PolyhedralBoundedSolidHalfEdge halfEdge =
                loop.halfEdgesList.get(i);
            if ( halfEdge == null || halfEdge.startingVertex == null ||
                 !outgoingHalfEdges.containsKey(halfEdge.startingVertex) ) {
                continue;
            }
            outgoingHalfEdges.put(halfEdge.startingVertex,
                Integer.valueOf(outgoingHalfEdges.get(halfEdge.startingVertex)
                    .intValue() + 1));
        }
    }

    private static void auditFacesAndLoops(PolyhedralBoundedSolid solid,
                                           List<String> failures)
    {
        Set<Integer> faceIds = new HashSet<Integer>();
        int i;
        int j;

        for ( i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);

            if ( face == null ) {
                failures.add("face list contains a null face at index " + i);
                continue;
            }
            if ( !faceIds.add(Integer.valueOf(face.id)) ) {
                failures.add("face id " + face.id + " is repeated");
            }
            if ( face.boundariesList == null ||
                 face.boundariesList.size() <= 0 ) {
                failures.add("face " + face.id + " has no boundary loops");
                continue;
            }

            for ( j = 0; j < face.boundariesList.size(); j++ ) {
                auditLoop(face, face.boundariesList.get(j), j, failures);
            }
            auditRepeatedVerticesAcrossFace(face, failures);
        }
    }

    private static void auditLoop(_PolyhedralBoundedSolidFace face,
                                  _PolyhedralBoundedSolidLoop loop,
                                  int loopIndex,
                                  List<String> failures)
    {
        List<_PolyhedralBoundedSolidHalfEdge> halfEdges =
            new ArrayList<_PolyhedralBoundedSolidHalfEdge>();
        Set<Integer> vertexIds = new HashSet<Integer>();
        int i;
        int j;

        if ( loop == null ) {
            failures.add("face " + face.id + " has null loop " + loopIndex);
            return;
        }
        if ( loop.parentFace != face ) {
            failures.add("face " + face.id + " loop " + loopIndex +
                " does not point back to the face");
        }
        if ( loop.boundaryStartHalfEdge == null ) {
            failures.add("face " + face.id + " loop " + loopIndex +
                " has null boundary start");
            return;
        }
        if ( loop.halfEdgesList == null ) {
            failures.add("face " + face.id + " loop " + loopIndex +
                " has null half-edge list");
            return;
        }
        if ( loop.halfEdgesList.size() < 3 ) {
            failures.add("face " + face.id + " loop " + loopIndex +
                " has fewer than three half-edges");
        }

        for ( i = 0; i < loop.halfEdgesList.size(); i++ ) {
            _PolyhedralBoundedSolidHalfEdge halfEdge =
                loop.halfEdgesList.get(i);
            halfEdges.add(halfEdge);
            auditHalfEdge(face, loopIndex, i, loop, halfEdge, failures);
            if ( halfEdge == null || halfEdge.startingVertex == null ) {
                continue;
            }
            if ( !vertexIds.add(
                    Integer.valueOf(halfEdge.startingVertex.id)) ) {
                failures.add("face " + face.id + " loop " + loopIndex +
                    " repeats vertex " + halfEdge.startingVertex.id);
            }
        }

        for ( i = 0; i < halfEdges.size(); i++ ) {
            _PolyhedralBoundedSolidHalfEdge first = halfEdges.get(i);
            if ( first == null || first.startingVertex == null ||
                 first.startingVertex.position == null ) {
                continue;
            }
            for ( j = i + 1; j < halfEdges.size(); j++ ) {
                _PolyhedralBoundedSolidHalfEdge second = halfEdges.get(j);
                if ( second == null || second.startingVertex == null ||
                     second.startingVertex.position == null ) {
                    continue;
                }
                if ( samePosition(first.startingVertex.position,
                        second.startingVertex.position) ) {
                    failures.add("face " + face.id + " loop " + loopIndex +
                        " repeats position " +
                        first.startingVertex.position +
                        " at vertices " + first.startingVertex.id +
                        " and " + second.startingVertex.id);
                }
            }
        }

        if ( loopAreaMagnitude(loop) <= TOLERANCE ) {
            failures.add("face " + face.id + " loop " + loopIndex +
                " has zero area");
        }
    }

    private static void auditHalfEdge(
        _PolyhedralBoundedSolidFace face,
        int loopIndex,
        int halfEdgeIndex,
        _PolyhedralBoundedSolidLoop loop,
        _PolyhedralBoundedSolidHalfEdge halfEdge,
        List<String> failures)
    {
        _PolyhedralBoundedSolidHalfEdge next;
        _PolyhedralBoundedSolidHalfEdge previous;
        _PolyhedralBoundedSolidHalfEdge mirror;

        if ( halfEdge == null ) {
            failures.add("face " + face.id + " loop " + loopIndex +
                " has null half-edge at index " + halfEdgeIndex);
            return;
        }
        if ( halfEdge.parentLoop != loop ) {
            failures.add("half-edge " + halfEdge.id +
                " does not point back to face " + face.id + " loop " +
                loopIndex);
        }
        if ( halfEdge.parentEdge == null ) {
            failures.add("half-edge " + halfEdge.id +
                " has null parent edge");
        }
        if ( halfEdge.startingVertex == null ) {
            failures.add("half-edge " + halfEdge.id +
                " has null starting vertex");
        }

        next = safeNext(halfEdge);
        previous = safePrevious(halfEdge);
        mirror = safeMirror(halfEdge);

        if ( next == null ) {
            failures.add("half-edge " + halfEdge.id + " has null next");
        }
        if ( previous == null ) {
            failures.add("half-edge " + halfEdge.id + " has null previous");
        }
        if ( mirror == null ) {
            failures.add("half-edge " + halfEdge.id + " has null mirror");
        }
        else if ( safeMirror(mirror) != halfEdge ) {
            failures.add("half-edge " + halfEdge.id +
                " mirror does not mirror back");
        }

        if ( next != null && next.startingVertex != null &&
             halfEdge.startingVertex != null &&
             samePosition(halfEdge.startingVertex.position,
                 next.startingVertex.position) ) {
            failures.add("half-edge " + halfEdge.id +
                " is geometrically zero-length from vertex " +
                halfEdge.startingVertex.id + " to vertex " +
                next.startingVertex.id);
        }
    }

    private static void auditRepeatedVerticesAcrossFace(
        _PolyhedralBoundedSolidFace face,
        List<String> failures)
    {
        List<_PolyhedralBoundedSolidVertex> vertices =
            new ArrayList<_PolyhedralBoundedSolidVertex>();
        int i;
        int j;

        for ( i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
            if ( loop == null || loop.halfEdgesList == null ) {
                continue;
            }
            for ( j = 0; j < loop.halfEdgesList.size(); j++ ) {
                _PolyhedralBoundedSolidHalfEdge halfEdge =
                    loop.halfEdgesList.get(j);
                if ( halfEdge != null ) {
                    vertices.add(halfEdge.startingVertex);
                }
            }
        }

        for ( i = 0; i < vertices.size(); i++ ) {
            _PolyhedralBoundedSolidVertex first = vertices.get(i);
            if ( first == null || first.position == null ) {
                continue;
            }
            for ( j = i + 1; j < vertices.size(); j++ ) {
                _PolyhedralBoundedSolidVertex second = vertices.get(j);
                if ( second == null || second.position == null ) {
                    continue;
                }
                if ( first == second ) {
                    failures.add("face " + face.id +
                        " references vertex " + first.id +
                        " more than once");
                }
                else if ( samePosition(first.position, second.position) ) {
                    failures.add("face " + face.id +
                        " references coincident vertices " + first.id +
                        " and " + second.id);
                }
            }
        }
    }

    private static _PolyhedralBoundedSolidHalfEdge safeNext(
        _PolyhedralBoundedSolidHalfEdge halfEdge)
    {
        try {
            return halfEdge.next();
        }
        catch ( RuntimeException error ) {
            return null;
        }
    }

    private static _PolyhedralBoundedSolidHalfEdge safePrevious(
        _PolyhedralBoundedSolidHalfEdge halfEdge)
    {
        try {
            return halfEdge.previous();
        }
        catch ( RuntimeException error ) {
            return null;
        }
    }

    private static _PolyhedralBoundedSolidHalfEdge safeMirror(
        _PolyhedralBoundedSolidHalfEdge halfEdge)
    {
        try {
            return halfEdge.mirrorHalfEdge();
        }
        catch ( RuntimeException error ) {
            return null;
        }
    }

    private static double loopAreaMagnitude(_PolyhedralBoundedSolidLoop loop)
    {
        Vector3D normalAccumulator = new Vector3D();
        int i;

        if ( loop == null || loop.halfEdgesList == null ||
             loop.halfEdgesList.size() < 3 ) {
            return 0.0;
        }

        for ( i = 0; i < loop.halfEdgesList.size(); i++ ) {
            _PolyhedralBoundedSolidHalfEdge halfEdge =
                loop.halfEdgesList.get(i);
            _PolyhedralBoundedSolidHalfEdge next = safeNext(halfEdge);
            if ( halfEdge == null || next == null ||
                 halfEdge.startingVertex == null ||
                 next.startingVertex == null ||
                 halfEdge.startingVertex.position == null ||
                 next.startingVertex.position == null ) {
                return 0.0;
            }

            Vector3D p = halfEdge.startingVertex.position;
            Vector3D q = next.startingVertex.position;

            normalAccumulator = normalAccumulator.add(new Vector3D(
                (p.y() - q.y()) * (p.z() + q.z()),
                (p.z() - q.z()) * (p.x() + q.x()),
                (p.x() - q.x()) * (p.y() + q.y())));
        }
        return normalAccumulator.length();
    }

    private static boolean samePosition(Vector3D first, Vector3D second)
    {
        if ( first == null || second == null ) {
            return false;
        }
        return first.subtract(second).length() <= TOLERANCE;
    }
}
