package vsdk.toolkit.io.geometry.stepCad.reader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;

/**
Traverses the flat entity map produced by `_StepTokenizer` and
reconstructs a `PolyhedralBoundedSolid` by direct half-edge
construction.

Traversal root is the single MANIFOLD_SOLID_BREP entity, from which
the reconstruction descends:

  MANIFOLD_SOLID_BREP
    -> CLOSED_SHELL -> [ADVANCED_FACE]
       -> [FACE_OUTER_BOUND | FACE_BOUND] -> EDGE_LOOP
          -> [ORIENTED_EDGE] -> EDGE_CURVE
             -> VERTEX_POINT -> CARTESIAN_POINT

The reconstruction proceeds in four passes to avoid forward-reference
issues inherent in the circular half-edge data structure:

  Pass 1 — Vertex positions: collect every reachable VERTEX_POINT
            and create one `_PolyhedralBoundedSolidVertex` per point.
  Pass 2 — Face / loop / half-edge topology: for each ADVANCED_FACE,
            create its face, loops, and half-edges in loop traversal
            order; record which EDGE_CURVE each half-edge belongs to
            and whether the oriented edge was .T. or .F.
  Pass 3 — Edge wiring: group half-edges by EDGE_CURVE id; for each
            pair create one `_PolyhedralBoundedSolidEdge`, assign the
            .T. half as rightHalf and the .F. half as leftHalf, and
            write parentEdge on both.
  Pass 4 — Emanating half-edge: for each vertex, set
            `emanatingHalfEdge` to any half-edge that starts from it.

Orientation assumptions (same as in the writer):
  - ORIENTED_EDGE .T. → the contributing vertex for the loop position
    is the EDGE_CURVE start vertex (= rightHalf in Mantyla terms).
  - ORIENTED_EDGE .F. → the contributing vertex is the EDGE_CURVE end
    vertex.

Edge geometry tolerance:
  Vertex positions are always taken from the VERTEX_POINT references
  in EDGE_CURVE params[1] and params[2], never from the geometry
  entity in params[3].  Consequently the geometry type in params[3]
  is irrelevant to topological reconstruction and is accepted without
  validation.  Supported geometry types include:
    - LINE               — straight edge (produced by StepWriter)
    - SURFACE_CURVE      — parametric wrapper; the builder unwraps the
                           3D curve reference (param[1]) for logging
                           only; topology is unaffected.
    - B_SPLINE_CURVE_WITH_KNOTS (degree 1, two control points) —
                           treated as a straight-line segment.  This is
                           valid for faces that have been planarised
                           before export (tangent directions coincide
                           with the chord between the two VERTEX_POINTs).
  Any other geometry type is tolerated silently; its edge contributes
  only the two VERTEX_POINT endpoints to the B-rep topology.

This is an internal collaborator of `StepReader`.
*/
public class _StepSolidBuilder {

    private final Map<Integer, _StepEntity> entities;

    private final Map<Integer, _PolyhedralBoundedSolidVertex> vertexByVpId;
    private final Map<Integer, Integer> ecStartVpId;
    private final Map<Integer, Integer> ecEndVpId;

    /** Per half-edge: which STEP EDGE_CURVE id it references. */
    private final Map<_PolyhedralBoundedSolidHalfEdge, Integer> heEdgeCurveId;

    /** Per half-edge: true when the ORIENTED_EDGE had orientation .T. */
    private final Map<_PolyhedralBoundedSolidHalfEdge, Boolean> heIsForward;

    /**
    Scale factor applied to every vertex coordinate to convert the file's
    length unit to metres.  Detected from the SI_UNIT length entity; defaults
    to 1.0 (metres) when the unit cannot be determined.
    */
    private double unitToMetre;

    private _StepSolidBuilder(Map<Integer, _StepEntity> entities)
    {
        this.entities = entities;
        this.vertexByVpId = new HashMap<>();
        this.ecStartVpId = new HashMap<>();
        this.ecEndVpId = new HashMap<>();
        this.heEdgeCurveId = new HashMap<>();
        this.heIsForward = new HashMap<>();
        this.unitToMetre = detectLengthUnitScale();
    }

    public static PolyhedralBoundedSolid build(Map<Integer, _StepEntity> entities)
    {
        return new _StepSolidBuilder(entities).buildInternal();
    }

    //=================================================================

    private PolyhedralBoundedSolid buildInternal()
    {
        int closedShellId = findClosedShellId();
        List<Integer> faceIds = collectFaceIds(closedShellId);

        collectEdgeCurveEndpoints(faceIds);

        PolyhedralBoundedSolid solid = new PolyhedralBoundedSolid();

        pass1CreateVertices(solid);
        pass2CreateFacesAndHalfEdges(solid, faceIds);
        pass3WireEdges(solid);
        pass4SetEmanatingHalfEdges();

        updateMaxIds(solid);
        return solid;
    }

    //=================================================================
    //= TRAVERSAL =====================================================

    private int findClosedShellId()
    {
        for ( _StepEntity e : entities.values() ) {
            if ( "MANIFOLD_SOLID_BREP".equals(e.name) ) {
                return _StepTokenizer.parseRef(e.params.get(1));
            }
        }
        throw new IllegalStateException(
            "STEP file contains no MANIFOLD_SOLID_BREP entity.");
    }

    private List<Integer> collectFaceIds(int closedShellId)
    {
        _StepEntity shell = require(closedShellId, "CLOSED_SHELL");
        List<String> faceAgg = _StepTokenizer.parseAggregate(shell.params.get(1));
        List<Integer> ids = new ArrayList<>(faceAgg.size());
        for ( String tok : faceAgg ) {
            ids.add(_StepTokenizer.parseRef(tok));
        }
        return ids;
    }

    /**
    Pre-collects the start/end VERTEX_POINT ids for every EDGE_CURVE
    reachable from the given ADVANCED_FACE list so that Pass 2 can
    resolve vertex references without re-traversal.

    Vertex positions are taken exclusively from EDGE_CURVE params[1]
    (start VERTEX_POINT) and params[2] (end VERTEX_POINT).  The
    geometry entity in params[3] (LINE, SURFACE_CURVE,
    B_SPLINE_CURVE_WITH_KNOTS, etc.) is inspected only for diagnostic
    logging and does not affect topology reconstruction.
    */
    private void collectEdgeCurveEndpoints(List<Integer> faceIds)
    {
        for ( int faceId : faceIds ) {
            _StepEntity face = require(faceId, "ADVANCED_FACE");
            List<String> boundsAgg =
                _StepTokenizer.parseAggregate(face.params.get(1));
            for ( String boundTok : boundsAgg ) {
                int boundId = _StepTokenizer.parseRef(boundTok);
                _StepEntity bound = entities.get(boundId);
                if ( bound == null ) {
                    continue;
                }
                int loopId = _StepTokenizer.parseRef(bound.params.get(1));
                _StepEntity loop = require(loopId, "EDGE_LOOP");
                List<String> oeAgg =
                    _StepTokenizer.parseAggregate(loop.params.get(1));
                for ( String oeTok : oeAgg ) {
                    int oeId = _StepTokenizer.parseRef(oeTok);
                    _StepEntity oe = require(oeId, "ORIENTED_EDGE");
                    int ecId = _StepTokenizer.parseRef(oe.params.get(3));
                    if ( ecStartVpId.containsKey(ecId) ) {
                        continue;
                    }
                    _StepEntity ec = require(ecId, "EDGE_CURVE");
                    ecStartVpId.put(ecId, _StepTokenizer.parseRef(ec.params.get(1)));
                    ecEndVpId.put(ecId, _StepTokenizer.parseRef(ec.params.get(2)));
                    logEdgeGeometryType(ecId, ec);
                }
            }
        }
    }

    /**
    Resolves and logs the effective 3D geometry type for the given
    EDGE_CURVE.  SURFACE_CURVE wrappers are unwrapped one level to
    expose the underlying curve (LINE or B_SPLINE_CURVE_WITH_KNOTS).
    B_SPLINE edges are accepted and treated as straight-line segments
    because the faces are assumed to be planarised.
    */
    private void logEdgeGeometryType(int ecId, _StepEntity ec)
    {
        if ( ec.params.size() < 4 ) {
            return;
        }
        String geomTok = ec.params.get(3).strip();
        if ( geomTok.isEmpty() || geomTok.charAt(0) != '#' ) {
            return;
        }
        int geomId;
        try {
            geomId = _StepTokenizer.parseRef(geomTok);
        }
        catch ( IllegalArgumentException ignored ) {
            return;
        }
        _StepEntity geomEntity = entities.get(geomId);
        if ( geomEntity == null ) {
            return;
        }
        String geomName = geomEntity.name;
        if ( "SURFACE_CURVE".equals(geomName) && geomEntity.params.size() >= 2 ) {
            String innerTok = geomEntity.params.get(1).strip();
            if ( !innerTok.isEmpty() && innerTok.charAt(0) == '#' ) {
                try {
                    int innerGeomId = _StepTokenizer.parseRef(innerTok);
                    _StepEntity innerGeom = entities.get(innerGeomId);
                    if ( innerGeom != null ) {
                        geomName = "SURFACE_CURVE->" + innerGeom.name;
                        if ( "B_SPLINE_CURVE_WITH_KNOTS".equals(innerGeom.name) ) {
                            System.err.println(
                                "[StepReader] EDGE_CURVE #" + ecId
                                + ": geometry is " + geomName
                                + " — treating as straight-line segment"
                                + " (planarised solid assumed).");
                        }
                    }
                }
                catch ( IllegalArgumentException ignored ) {
                    // inner param is not a reference; keep geomName as-is
                }
            }
        }
        else if ( "B_SPLINE_CURVE_WITH_KNOTS".equals(geomName) ) {
            System.err.println(
                "[StepReader] EDGE_CURVE #" + ecId
                + ": geometry is B_SPLINE_CURVE_WITH_KNOTS"
                + " — treating as straight-line segment"
                + " (planarised solid assumed).");
        }
    }

    //=================================================================
    //= PASS 1: VERTICES ==============================================

    private void pass1CreateVertices(PolyhedralBoundedSolid solid)
    {
        int pbsId = 1;
        for ( int vpId : collectAllVertexPointIds() ) {
            Vector3Dd pos = resolveVertexPosition(vpId);
            _PolyhedralBoundedSolidVertex v =
                new _PolyhedralBoundedSolidVertex(solid, pos, pbsId);
            vertexByVpId.put(vpId, v);
            pbsId++;
        }
    }

    private List<Integer> collectAllVertexPointIds()
    {
        List<Integer> vpIds = new ArrayList<>(ecStartVpId.size() * 2);
        for ( Map.Entry<Integer, Integer> e : ecStartVpId.entrySet() ) {
            if ( !vpIds.contains(e.getValue()) ) {
                vpIds.add(e.getValue());
            }
        }
        for ( Map.Entry<Integer, Integer> e : ecEndVpId.entrySet() ) {
            if ( !vpIds.contains(e.getValue()) ) {
                vpIds.add(e.getValue());
            }
        }
        return vpIds;
    }

    private Vector3Dd resolveVertexPosition(int vpId)
    {
        _StepEntity vp = require(vpId, "VERTEX_POINT");
        int cpId = _StepTokenizer.parseRef(vp.params.get(1));
        _StepEntity cp = require(cpId, "CARTESIAN_POINT");
        List<String> coords =
            _StepTokenizer.parseAggregate(cp.params.get(1));
        double x = _StepTokenizer.parseDouble(coords.get(0)) * unitToMetre;
        double y = _StepTokenizer.parseDouble(coords.get(1)) * unitToMetre;
        double z = _StepTokenizer.parseDouble(coords.get(2)) * unitToMetre;
        return new Vector3Dd(x, y, z);
    }

    /**
    Scans the entity map for a compound entity that declares both
    LENGTH_UNIT and SI_UNIT, extracts the SI prefix, and returns the
    corresponding scale factor to convert to metres.

    Recognised SI prefixes and their scale factors:
      .MILLI.  → 0.001    .CENTI.  → 0.01
      .DECI.   → 0.1      (none/$) → 1.0
      .DECA.   → 10       .HECTO.  → 100
      .KILO.   → 1000

    Returns 1.0 (metres assumed) when no matching entity is found or the
    prefix is unrecognised.
    */
    private double detectLengthUnitScale()
    {
        for ( _StepEntity e : entities.values() ) {
            if ( !_StepEntity.COMPLEX_NAME.equals(e.name) ) {
                continue;
            }
            String body = e.params.get(0);
            if ( !body.contains("LENGTH_UNIT") || !body.contains("SI_UNIT") ) {
                continue;
            }
            // Extract the SI_UNIT prefix from the compound body.
            // The SI_UNIT sub-entity has the form: SI_UNIT(prefix,name)
            // where prefix may be $ (none) or an enum like .MILLI.
            int siPos = body.indexOf("SI_UNIT(");
            if ( siPos < 0 ) {
                continue;
            }
            int open = siPos + "SI_UNIT(".length();
            int close = body.indexOf(')', open);
            if ( close < 0 ) {
                continue;
            }
            String siParams = body.substring(open, close).strip();
            // siParams = "prefix,name" e.g. ".MILLI.,.METRE." or "$,.METRE."
            int comma = siParams.indexOf(',');
            String prefix = comma >= 0
                ? siParams.substring(0, comma).strip()
                : siParams.strip();
            double scale = siPrefixToScale(prefix);
            if ( scale != 1.0 ) {
                System.err.println(
                    "[StepReader] Length unit prefix " + prefix
                    + " detected — scaling all vertex coordinates by "
                    + scale + " to convert to metres.");
            }
            return scale;
        }
        return 1.0;
    }

    private static double siPrefixToScale(String prefix)
    {
        switch ( prefix ) {
            case ".ATTO.":   return 1e-18;
            case ".FEMTO.":  return 1e-15;
            case ".PICO.":   return 1e-12;
            case ".NANO.":   return 1e-9;
            case ".MICRO.":  return 1e-6;
            case ".MILLI.":  return 1e-3;
            case ".CENTI.":  return 1e-2;
            case ".DECI.":   return 1e-1;
            case ".DECA.":   return 1e1;
            case ".HECTO.":  return 1e2;
            case ".KILO.":   return 1e3;
            case ".MEGA.":   return 1e6;
            case ".GIGA.":   return 1e9;
            case ".TERA.":   return 1e12;
            default:         return 1.0;   // $ or unrecognised = metres
        }
    }

    //=================================================================
    //= PASS 2: FACES, LOOPS, HALF-EDGES ==============================

    private void pass2CreateFacesAndHalfEdges(PolyhedralBoundedSolid solid,
                                              List<Integer> faceIds)
    {
        int faceSeqId = 1;
        for ( int faceId : faceIds ) {
            _StepEntity faceEntity = require(faceId, "ADVANCED_FACE");
            _PolyhedralBoundedSolidFace face =
                new _PolyhedralBoundedSolidFace(solid, faceSeqId);
            faceSeqId++;
            boolean faceSameSense = parseFaceSameSense(faceEntity);
            buildLoopsForFace(face, faceEntity, faceSameSense);
        }
    }

    /**
    Reads ADVANCED_FACE param[3] (same_sense flag).
    Returns true when the face normal agrees with the underlying surface
    normal (.T.), false when it is reversed (.F.).  Any parse failure
    defaults to true (no flip).
    */
    private boolean parseFaceSameSense(_StepEntity faceEntity)
    {
        if ( faceEntity.params.size() < 4 ) {
            return true;
        }
        return !".F.".equals(faceEntity.params.get(3).strip());
    }

    private void buildLoopsForFace(_PolyhedralBoundedSolidFace face,
                                   _StepEntity faceEntity,
                                   boolean faceSameSense)
    {
        List<String> boundsAgg =
            _StepTokenizer.parseAggregate(faceEntity.params.get(1));
        for ( String boundTok : boundsAgg ) {
            int boundId = _StepTokenizer.parseRef(boundTok);
            _StepEntity bound = entities.get(boundId);
            if ( bound == null ) {
                continue;
            }
            int loopId = _StepTokenizer.parseRef(bound.params.get(1));
            _StepEntity loop = require(loopId, "EDGE_LOOP");
            buildLoop(face, loop, faceSameSense);
        }
    }

    private void buildLoop(_PolyhedralBoundedSolidFace face,
                           _StepEntity loopEntity,
                           boolean faceSameSense)
    {
        _PolyhedralBoundedSolidLoop loop =
            new _PolyhedralBoundedSolidLoop(face);

        List<String> oeAgg =
            _StepTokenizer.parseAggregate(loopEntity.params.get(1));

        List<_PolyhedralBoundedSolidHalfEdge> halfEdges =
            new ArrayList<>(oeAgg.size());

        for ( String oeTok : oeAgg ) {
            int oeId = _StepTokenizer.parseRef(oeTok);
            _StepEntity oe = require(oeId, "ORIENTED_EDGE");
            int ecId = _StepTokenizer.parseRef(oe.params.get(3));
            // When same_sense = .F. the face normal is flipped, which means
            // the loop traversal direction is also reversed.  XOR-flipping
            // isForward restores the canonical half-edge direction so that
            // pass3 always sees exactly one .T. (rightHalf) and one .F.
            // (leftHalf) per EDGE_CURVE.
            boolean isForward = ".T.".equals(oe.params.get(4).strip());
            if ( !faceSameSense ) {
                isForward = !isForward;
            }

            int vpId = isForward
                ? ecStartVpId.get(ecId)
                : ecEndVpId.get(ecId);
            _PolyhedralBoundedSolidVertex v = vertexByVpId.get(vpId);
            if ( v == null ) {
                throw new IllegalStateException(
                    "Vertex not found for VERTEX_POINT id " + vpId);
            }

            _PolyhedralBoundedSolidHalfEdge he =
                new _PolyhedralBoundedSolidHalfEdge(
                    v, loop, face.parentSolid);
            loop.halfEdgesList.add(he);
            heEdgeCurveId.put(he, ecId);
            heIsForward.put(he, isForward);
            halfEdges.add(he);
        }

        if ( halfEdges.isEmpty() ) {
            return;
        }

        // When same_sense = .F., the STEP loop is wound in the opposite
        // direction (CW from outside).  The isForward flip above corrects
        // the rightHalf/leftHalf assignment for pass3, but the list order
        // still reflects the CW traversal.  Reversing the list here restores
        // CCW winding for rendering without affecting the per-edge direction
        // flags already recorded in heIsForward.
        if ( !faceSameSense ) {
            loop.halfEdgesList.reverse();
        }

        loop.boundaryStartHalfEdge = loop.halfEdgesList.get(0);
    }

    //=================================================================
    //= PASS 3: EDGE WIRING ===========================================

    private void pass3WireEdges(PolyhedralBoundedSolid solid)
    {
        Map<Integer, _PolyhedralBoundedSolidHalfEdge> forwardHe =
            new HashMap<>();
        Map<Integer, _PolyhedralBoundedSolidHalfEdge> reverseHe =
            new HashMap<>();

        for ( Map.Entry<_PolyhedralBoundedSolidHalfEdge, Integer> entry
              : heEdgeCurveId.entrySet() ) {
            _PolyhedralBoundedSolidHalfEdge he = entry.getKey();
            int ecId = entry.getValue();
            boolean forward = heIsForward.get(he);
            if ( forward ) {
                forwardHe.put(ecId, he);
            }
            else {
                reverseHe.put(ecId, he);
            }
        }

        for ( int ecId : ecStartVpId.keySet() ) {
            _PolyhedralBoundedSolidHalfEdge right = forwardHe.get(ecId);
            _PolyhedralBoundedSolidHalfEdge left = reverseHe.get(ecId);
            if ( right == null || left == null ) {
                throw new IllegalStateException(
                    "EDGE_CURVE #" + ecId
                    + " is not referenced by exactly two ORIENTED_EDGEs "
                    + "with opposite orientations.");
            }
            _PolyhedralBoundedSolidEdge edge =
                new _PolyhedralBoundedSolidEdge(solid);
            edge.rightHalf = right;
            edge.leftHalf = left;
            right.parentEdge = edge;
            left.parentEdge = edge;
        }
    }

    //=================================================================
    //= PASS 4: EMANATING HALF-EDGE ===================================

    private void pass4SetEmanatingHalfEdges()
    {
        for ( _PolyhedralBoundedSolidHalfEdge he : heEdgeCurveId.keySet() ) {
            _PolyhedralBoundedSolidVertex v = he.startingVertex;
            if ( v.emanatingHalfEdge == null ) {
                v.emanatingHalfEdge = he;
            }
        }
    }

    //=================================================================
    //= UTILITIES =====================================================

    private void updateMaxIds(PolyhedralBoundedSolid solid)
    {
        int maxV = 0;
        int i;
        for ( i = 0; i < solid.getVerticesList().size(); i++ ) {
            int vid = solid.getVerticesList().get(i).id;
            if ( vid > maxV ) {
                maxV = vid;
            }
        }
        int maxF = 0;
        for ( i = 0; i < solid.getPolygonsList().size(); i++ ) {
            int fid = solid.getPolygonsList().get(i).id;
            if ( fid > maxF ) {
                maxF = fid;
            }
        }
        solid.setMaxVertexId(maxV);
        solid.setMaxFaceId(maxF);
    }

    private _StepEntity require(int id, String expectedName)
    {
        _StepEntity e = entities.get(id);
        if ( e == null ) {
            throw new IllegalStateException(
                "Expected entity #" + id
                + " (" + expectedName + ") but it was not found.");
        }
        if ( !expectedName.equals(e.name) ) {
            throw new IllegalStateException(
                "Entity #" + id + " expected to be " + expectedName
                + " but is " + e.name + ".");
        }
        return e;
    }
}
