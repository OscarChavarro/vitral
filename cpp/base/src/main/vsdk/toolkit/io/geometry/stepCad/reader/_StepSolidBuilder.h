#ifndef __STEP_SOLID_BUILDER__
#define __STEP_SOLID_BUILDER__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "java/util/HashMap.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/io/geometry/stepCad/reader/_StepEntity.h"

class PolyhedralBoundedSolid;
class _PolyhedralBoundedSolidFace;
class _PolyhedralBoundedSolidHalfEdge;
class _PolyhedralBoundedSolidVertex;

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

C++ port notes:
  - The Java maps keyed by `Integer` (`ecStartVpId`, `ecEndVpId`) are
    iterated in the order of a Java `HashMap` (`keySet`, `entrySet`), so
    their keys are also kept in insertion order and reordered with
    `java::HashMapIterationOrder`: the vertex ids and the order of the
    edges are the ones Java builds.
  - The Java maps keyed by half-edge (`heEdgeCurveId`, `heIsForward`) hash
    by object identity, so Java iterates them in an unspecified order; the
    C++ port iterates them in insertion order (the order the half-edges
    are created).
*/
class _StepSolidBuilder {
public:
    /**
    @return a new solid, owned by the caller
    */
    static PolyhedralBoundedSolid* build(const _StepEntityMap& entities);

private:
    const _StepEntityMap& entities;

    java::HashMap<int, _PolyhedralBoundedSolidVertex*> vertexByVpId;
    java::HashMap<int, int> ecStartVpId;
    java::HashMap<int, int> ecEndVpId;
    /** C++ port: keys of `ecStartVpId` (and `ecEndVpId`), in insertion order. */
    java::ArrayList<int> ecIdsInInsertionOrder;

    /** Per half-edge: which STEP EDGE_CURVE id it references. */
    java::HashMap<_PolyhedralBoundedSolidHalfEdge*, int> heEdgeCurveId;

    /** Per half-edge: true when the ORIENTED_EDGE had orientation .T. */
    java::HashMap<_PolyhedralBoundedSolidHalfEdge*, bool> heIsForward;

    /** C++ port: keys of `heEdgeCurveId` (and `heIsForward`), in insertion order. */
    java::ArrayList<_PolyhedralBoundedSolidHalfEdge*> halfEdgesInInsertionOrder;

    /**
    Scale factor applied to every vertex coordinate to convert the file's
    length unit to metres.  Detected from the SI_UNIT length entity; defaults
    to 1.0 (metres) when the unit cannot be determined.
    */
    double unitToMetre;

    explicit _StepSolidBuilder(const _StepEntityMap& entities);

    PolyhedralBoundedSolid* buildInternal();

    int findClosedShellId();
    java::ArrayList<int> collectFaceIds(int closedShellId);
    void collectEdgeCurveEndpoints(const java::ArrayList<int>& faceIds);
    void logEdgeGeometryType(int ecId, _StepEntity* ec);

    void pass1CreateVertices(PolyhedralBoundedSolid* solid);
    java::ArrayList<int> collectAllVertexPointIds();
    Vector3Dd resolveVertexPosition(int vpId);
    double detectLengthUnitScale();
    static double siPrefixToScale(const java::String& prefix);

    void pass2CreateFacesAndHalfEdges(PolyhedralBoundedSolid* solid,
                                      const java::ArrayList<int>& faceIds);
    bool parseFaceSameSense(_StepEntity* faceEntity);
    void buildLoopsForFace(_PolyhedralBoundedSolidFace* face,
                           _StepEntity* faceEntity,
                           bool faceSameSense);
    void buildLoop(_PolyhedralBoundedSolidFace* face,
                   _StepEntity* loopEntity,
                   bool faceSameSense);

    void pass3WireEdges(PolyhedralBoundedSolid* solid);
    void pass4SetEmanatingHalfEdges();

    void updateMaxIds(PolyhedralBoundedSolid* solid);
    _StepEntity* require(int id, const java::String& expectedName);

    _StepSolidBuilder(const _StepSolidBuilder&);
    _StepSolidBuilder& operator=(const _StepSolidBuilder&);
};

#endif
