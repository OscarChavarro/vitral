package vsdk.toolkit.io.geometry.stepCad.writer;

import static vsdk.toolkit.io.geometry.stepCad.writer._StepEntityBuffer.escape;
import static vsdk.toolkit.io.geometry.stepCad.writer._StepEntityBuffer.refList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;
import vsdk.toolkit.io.geometry.stepCad.StepLengthUnit;

/**
Maps the Mantyla half-edge data structure of [MANT1988].10.2.1 onto the
ISO 10303-21 boundary representation entities:

  - _PolyhedralBoundedSolidVertex   -> CARTESIAN_POINT + VERTEX_POINT
  - _PolyhedralBoundedSolidEdge     -> EDGE_CURVE (LINE + VECTOR + DIRECTION)
  - _PolyhedralBoundedSolidHalfEdge -> ORIENTED_EDGE
  - _PolyhedralBoundedSolidLoop     -> EDGE_LOOP
  - _PolyhedralBoundedSolidFace     -> ADVANCED_FACE on PLANE, with one
                                       FACE_OUTER_BOUND and zero or more
                                       FACE_BOUND for inner rings
  - PolyhedralBoundedSolid          -> CLOSED_SHELL -> MANIFOLD_SOLID_BREP

Orientation invariants:
  - Canonical EDGE_CURVE direction goes from the starting vertex of the
    edge's right half-edge to the starting vertex of its left half-edge.
  - ORIENTED_EDGE on the right half-edge gets .T.; on the left half-edge,
    .F.
  - ADVANCED_FACE is emitted with same_sense .T. because the face plane
    normal is computed from the same loop using Newell's method, hence
    plane and face normals agree by construction.

This is an internal collaborator of `StepWriter`.
*/
public class _StepTopologyEmitter {

    private final _StepEntityBuffer buffer;
    private final _StepGeometryEmitter geometry;
    private final double scale;

    private final Map<Integer, Integer> vertexPointByVertexId;
    private final Map<Integer, Integer> cartesianPointByVertexId;
    private final Map<Integer, Integer> edgeCurveByEdgeId;

    public _StepTopologyEmitter(_StepEntityBuffer buffer,
                                _StepGeometryEmitter geometry,
                                StepLengthUnit lengthUnit)
    {
        this.buffer = buffer;
        this.geometry = geometry;
        this.scale = lengthUnit.metreScale;
        this.vertexPointByVertexId = new HashMap<>();
        this.cartesianPointByVertexId = new HashMap<>();
        this.edgeCurveByEdgeId = new HashMap<>();
    }

    /**
    Emits the full B-Rep topology of the solid and returns the
    MANIFOLD_SOLID_BREP entity id.
    */
    public int emit(PolyhedralBoundedSolid solid, String productName)
    {
        emitVertices(solid);
        emitEdges(solid);
        List<Integer> faceIds = emitFaces(solid);
        int closedShellId = emitClosedShell(faceIds);
        return emitManifoldSolidBrep(productName, closedShellId);
    }

    //=================================================================

    private void emitVertices(PolyhedralBoundedSolid solid)
    {
        int i;
        for ( i = 0; i < solid.getVerticesList().size(); i++ ) {
            _PolyhedralBoundedSolidVertex v = solid.getVerticesList().get(i);
            int cpId = geometry.emitCartesianPoint(
                v.position.x() * scale,
                v.position.y() * scale,
                v.position.z() * scale);
            int vpId = geometry.emitVertexPoint(cpId);
            cartesianPointByVertexId.put(v.id, cpId);
            vertexPointByVertexId.put(v.id, vpId);
        }
    }

    private void emitEdges(PolyhedralBoundedSolid solid)
    {
        int i;
        for ( i = 0; i < solid.getEdgesList().size(); i++ ) {
            _PolyhedralBoundedSolidEdge edge = solid.getEdgesList().get(i);
            edgeCurveByEdgeId.put(edge.id, emitEdgeCurve(edge));
        }
    }

    private int emitEdgeCurve(_PolyhedralBoundedSolidEdge edge)
    {
        _PolyhedralBoundedSolidHalfEdge rightHe = edge.rightHalf;
        _PolyhedralBoundedSolidHalfEdge leftHe = edge.leftHalf;
        if ( rightHe == null || leftHe == null ) {
            throw new IllegalStateException(
                "Edge " + edge.id + " has a missing half-edge; "
                + "solid topology is not manifold.");
        }

        _PolyhedralBoundedSolidVertex startVertex = rightHe.startingVertex;
        _PolyhedralBoundedSolidVertex endVertex = leftHe.startingVertex;
        Vector3Dd start = startVertex.position;
        Vector3Dd end = endVertex.position;
        Vector3Dd delta = end.subtract(start);
        double length = delta.length();
        if ( length <= 0.0 ) {
            throw new IllegalStateException(
                "Edge " + edge.id + " has zero length between vertices "
                + startVertex.id + " and " + endVertex.id + ".");
        }
        Vector3Dd direction = delta.multiply(1.0 / length);

        int directionId = geometry.emitDirection(
            direction.x(), direction.y(), direction.z());
        int vectorId = geometry.emitVector(directionId, length * scale);
        int lineId = geometry.emitLine(
            cartesianPointByVertexId.get(startVertex.id), vectorId);

        int vpStart = vertexPointByVertexId.get(startVertex.id);
        int vpEnd = vertexPointByVertexId.get(endVertex.id);

        int id = buffer.nextId();
        buffer.appendEntity(id,
            "EDGE_CURVE('',#" + vpStart + ",#" + vpEnd
            + ",#" + lineId + ",.T.)");
        return id;
    }

    private List<Integer> emitFaces(PolyhedralBoundedSolid solid)
    {
        List<Integer> faceIds = new ArrayList<>(solid.getPolygonsList().size());
        int i;
        for ( i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);
            faceIds.add(emitAdvancedFace(face));
        }
        return faceIds;
    }

    private int emitAdvancedFace(_PolyhedralBoundedSolidFace face)
    {
        InfinitePlane plane = face.getContainingPlane();
        if ( plane == null ) {
            throw new IllegalStateException(
                "Face " + face.id + " has no containing plane; "
                + "geometry is degenerate.");
        }
        Vector3Dd normal = plane.getNormal().normalized();
        Vector3Dd origin = chooseFaceAnchor(face);
        Vector3Dd refX = chooseReferenceX(normal);

        int originCpId = geometry.emitCartesianPoint(
            origin.x() * scale, origin.y() * scale, origin.z() * scale);
        int normalDirId = geometry.emitDirection(
            normal.x(), normal.y(), normal.z());
        int refXDirId = geometry.emitDirection(
            refX.x(), refX.y(), refX.z());
        int axisId = geometry.emitAxis2Placement3D(
            originCpId, normalDirId, refXDirId);
        int planeId = geometry.emitPlane(axisId);

        List<Integer> boundIds = new ArrayList<>(face.boundariesList.size());
        int i;
        for ( i = 0; i < face.boundariesList.size(); i++ ) {
            _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(i);
            int edgeLoopId = emitEdgeLoop(loop);
            int boundId = buffer.nextId();
            if ( i == 0 ) {
                buffer.appendEntity(boundId,
                    "FACE_OUTER_BOUND('',#" + edgeLoopId + ",.T.)");
            }
            else {
                buffer.appendEntity(boundId,
                    "FACE_BOUND('',#" + edgeLoopId + ",.T.)");
            }
            boundIds.add(boundId);
        }

        int afId = buffer.nextId();
        buffer.appendEntity(afId,
            "ADVANCED_FACE('',(" + refList(boundIds) + "),#"
            + planeId + ",.T.)");
        return afId;
    }

    private int emitEdgeLoop(_PolyhedralBoundedSolidLoop loop)
    {
        if ( loop.boundaryStartHalfEdge == null ) {
            throw new IllegalStateException(
                "Loop without starting half-edge in face "
                + loop.parentFace.id);
        }
        List<Integer> orientedEdgeIds = new ArrayList<>();
        _PolyhedralBoundedSolidHalfEdge start = loop.boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge he = start;
        do {
            orientedEdgeIds.add(emitOrientedEdge(he));
            he = he.next();
            if ( he == null ) {
                throw new IllegalStateException(
                    "Open loop detected in face " + loop.parentFace.id);
            }
        } while ( he != start );

        int id = buffer.nextId();
        buffer.appendEntity(id,
            "EDGE_LOOP('',(" + refList(orientedEdgeIds) + "))");
        return id;
    }

    private int emitOrientedEdge(_PolyhedralBoundedSolidHalfEdge he)
    {
        if ( he.parentEdge == null ) {
            throw new IllegalStateException(
                "Half-edge " + he.id + " has no parent edge.");
        }
        Integer ecId = edgeCurveByEdgeId.get(he.parentEdge.id);
        if ( ecId == null ) {
            throw new IllegalStateException(
                "Edge " + he.parentEdge.id + " was not emitted as EDGE_CURVE.");
        }
        String orientation = (he == he.parentEdge.rightHalf) ? ".T." : ".F.";
        int id = buffer.nextId();
        buffer.appendEntity(id,
            "ORIENTED_EDGE('',*,*,#" + ecId + "," + orientation + ")");
        return id;
    }

    private int emitClosedShell(List<Integer> faceIds)
    {
        int id = buffer.nextId();
        buffer.appendEntity(id,
            "CLOSED_SHELL('',(" + refList(faceIds) + "))");
        return id;
    }

    private int emitManifoldSolidBrep(String productName, int closedShellId)
    {
        int id = buffer.nextId();
        buffer.appendEntity(id,
            "MANIFOLD_SOLID_BREP('" + escape(productName) + "',#"
            + closedShellId + ")");
        return id;
    }

    //=================================================================

    private static Vector3Dd chooseFaceAnchor(_PolyhedralBoundedSolidFace face)
    {
        _PolyhedralBoundedSolidLoop outer = face.boundariesList.get(0);
        return outer.boundaryStartHalfEdge.startingVertex.position;
    }

    private static Vector3Dd chooseReferenceX(Vector3Dd normal)
    {
        Vector3Dd candidate;
        if ( Math.abs(normal.x()) <= Math.abs(normal.y()) &&
             Math.abs(normal.x()) <= Math.abs(normal.z()) ) {
            candidate = new Vector3Dd(1.0, 0.0, 0.0);
        }
        else if ( Math.abs(normal.y()) <= Math.abs(normal.z()) ) {
            candidate = new Vector3Dd(0.0, 1.0, 0.0);
        }
        else {
            candidate = new Vector3Dd(0.0, 0.0, 1.0);
        }
        Vector3Dd projection = normal.multiply(candidate.dotProduct(normal));
        Vector3Dd refX = candidate.subtract(projection);
        double len = refX.length();
        if ( len <= 0.0 ) {
            return new Vector3Dd(1.0, 0.0, 0.0);
        }
        return refX.multiply(1.0 / len);
    }
}
