package vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidVertex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
Edge cases of the low-level Euler operators that the main operator suite
(PolyhedralBoundedSolidEulerOperatorsTest) does not reach: empty-loop
special cases, inverse round trips that are continued afterwards, and
back-pointer integrity (vertex -> emanating half-edge).

<p>Traceability: [MANT1988] Ch. 11.2.2 (addhe/delhe special cases for the
edge-less half-edge of an empty loop), Programs 11.6-11.8, Problems 11.3
and 11.4, and the lkemr text of Section 11.3.4 ("the special cases that one
or both of the resulting loops are empty"). Cases marked "hidden defect"
were found by the Fable 5.2 kernel analysis
(doc/polyhedralBoundedSolidDevelopment/Fable5.2Analysis.md, defects E-01 to
E-06) and stay disabled until the production fix lands.</p>
 */
class PolyhedralBoundedSolidEulerOperatorsEdgeCasesTest
{
    /**
    Defect (fixed) E-01: mvfs -> lmev (strut) -> lkev returns to the skeletal
    count, but the loop's half-edge list is left with a stale head node
    (list defect L-01). The next lmev on the same vertex then yields a
    two-half-edge loop whose next() chain does not visit both half-edges.
    */
    @Test
    void given_strutKilledBackToSkeleton_when_newStrutIsAdded_then_loopTraversalVisitsBothHalfEdges()
    {
        PolyhedralBoundedSolid solid = createSkeletalSolid();
        _PolyhedralBoundedSolidLoop loop =
            solid.findFace(1).boundariesList.get(0);
        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 1, 2,
            new Vector3Dd(1.0, 0.0, 0.0));
        _PolyhedralBoundedSolidHalfEdge back = loop.halfEdgeVertices(2, 1);
        PolyhedralBoundedSolidEulerOperators.lkev(solid, back,
            back.mirrorHalfEdge());

        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 1, 3,
            new Vector3Dd(0.0, 1.0, 0.0));

        assertThat(loop.halfEdgesList.size()).isEqualTo(2);
        _PolyhedralBoundedSolidHalfEdge first = loop.halfEdgesList.get(0);
        _PolyhedralBoundedSolidHalfEdge second = loop.halfEdgesList.get(1);
        assertThat(first.next()).isSameAs(second);
        assertThat(second.next()).isSameAs(first);
        assertThat(first.previous()).isSameAs(second);
        assertThat(second.previous()).isSameAs(first);
    }

    /**
    Defect (fixed) E-02: the lkemr documentation (after [MANT1988] Sec.
    11.3.4) says the arguments can be swapped to choose which component
    stays as the outer loop. With the arguments swapped so that the outer
    component collapses to a single vertex, the operator raises the fatal
    "Case B Should not happen!" instead of leaving an empty (edge-less) loop.
    */
    @Test
    void given_strutInsideFace_when_lkemrArgumentsAreSwapped_then_outerLoopBecomesLoneVertex()
    {
        PolyhedralBoundedSolid solid = createSquareLaminaWithStrut();
        _PolyhedralBoundedSolidFace face = solid.findFace(1);
        int edgeCount = solid.getEdgesList().size();

        assertThatCode(() -> PolyhedralBoundedSolidEulerOperators.lkemr(
            solid, face.findHalfEdge(5, 1), face.findHalfEdge(1, 5)))
            .doesNotThrowAnyException();

        assertThat(solid.getEdgesList().size()).isEqualTo(edgeCount - 1);
        assertThat(face.boundariesList.size()).isEqualTo(2);
        assertThat(loopSizes(face)).containsExactlyInAnyOrder(1, 4);
    }

    /**
    Defect (fixed) E-03: lkemr leaves the vertex of a single-vertex ring with
    a null emanating half-edge, and the inverse lmekr does not restore it,
    so lkemr -> lmekr is not an identity on the vertex back-pointers. Any
    later pass that treats "null emanating half-edge" as "orphan vertex"
    (e.g. remakeEmanatingHalfedgesReferences) will delete a live vertex.
    */
    @Test
    void given_loneVertexRing_when_lmekrReconnectsIt_then_everyVertexHasALiveEmanatingHalfEdge()
    {
        PolyhedralBoundedSolid solid = createSquareLaminaWithStrut();
        _PolyhedralBoundedSolidFace face = solid.findFace(1);
        PolyhedralBoundedSolidEulerOperators.lkemr(solid,
            face.findHalfEdge(1, 5), face.findHalfEdge(5, 1));

        PolyhedralBoundedSolidEulerOperators.lmekr(solid,
            face.boundariesList.get(0).halfEdgeVertices(1, 2),
            face.boundariesList.get(1).boundaryStartHalfEdge);

        assertThat(face.boundariesList.size()).isEqualTo(1);
        assertThat(describeBrokenEmanatingReferences(solid)).isEmpty();
    }

    /**
    Defect (fixed) E-04: lmef(he, he) is documented (after Program 11.7) as
    creating a "circular" face with one edge. The container-based port
    inserts both new half-edges in the old loop and returns a face whose
    loop is empty.
    */
    @Test
    void given_sameHalfEdgeTwice_when_lmef_then_newFaceOwnsASingleSelfLoopEdge()
    {
        PolyhedralBoundedSolid solid = createSquareLaminaWithStrut();
        _PolyhedralBoundedSolidFace face = solid.findFace(1);
        _PolyhedralBoundedSolidHalfEdge he = face.findHalfEdge(2);
        int oldLoopSize = face.boundariesList.get(0).halfEdgesList.size();

        _PolyhedralBoundedSolidFace newFace =
            PolyhedralBoundedSolidEulerOperators.lmef(solid, he, he, 9);

        assertThat(newFace).isNotNull();
        _PolyhedralBoundedSolidLoop newLoop = newFace.boundariesList.get(0);
        assertThat(newLoop.halfEdgesList.size()).isEqualTo(1);
        assertThat(newLoop.boundaryStartHalfEdge.parentLoop)
            .isSameAs(newLoop);
        assertThat(face.boundariesList.get(0).halfEdgesList.size())
            .isEqualTo(oldLoopSize + 1);
    }

    /**
    Defect (fixed) E-05: after lkev removes a strut from a box face, the
    surviving vertex keeps an emanating half-edge that was unlisted from
    its loop (dangling back-pointer). validateIntermediate still reports
    the solid as valid, so the validator has a matching gap.
    */
    @Test
    void given_boxWithStrut_when_lkevRemovesStrut_then_everyVertexHasALiveEmanatingHalfEdge()
    {
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0,
                0.0, 0.0, 0.0);
        _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(0);
        _PolyhedralBoundedSolidHalfEdge he =
            face.boundariesList.get(0).boundaryStartHalfEdge;
        int baseVertexId = he.startingVertex.id;
        PolyhedralBoundedSolidEulerOperators.lmev(solid, he, he, 99,
            new Vector3Dd(0.0, 0.0, -0.5));
        assertThat(describeBrokenEmanatingReferences(solid)).isEmpty();

        PolyhedralBoundedSolidEulerOperators.lkev(solid,
            face.findHalfEdge(99, baseVertexId),
            face.findHalfEdge(baseVertexId, 99));

        assertThat(solid.getVerticesList().size()).isEqualTo(8);
        assertThat(solid.getEdgesList().size()).isEqualTo(12);
        assertThat(describeBrokenEmanatingReferences(solid)).isEmpty();
    }

    /**
    Defect (fixed) E-06: lmef does not check its precondition
    he1.parentLoop == he2.parentLoop ([MANT1988] Sec. 11.3.3). With
    half-edges from two different faces it silently migrates a whole loop,
    returns a non-null face and leaves the solid invalid. A rejected call
    must leave the solid untouched.
    */
    @Test
    void given_halfEdgesFromDifferentLoops_when_lmef_then_operationIsRejectedWithoutSideEffects()
    {
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0,
                0.0, 0.0, 0.0);
        _PolyhedralBoundedSolidHalfEdge he1 = solid.getPolygonsList().get(0)
            .boundariesList.get(0).boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge he2 = solid.getPolygonsList().get(1)
            .boundariesList.get(0).boundaryStartHalfEdge;

        _PolyhedralBoundedSolidFace newFace =
            PolyhedralBoundedSolidEulerOperators.lmef(solid, he1, he2, 50);

        assertThat(newFace).isNull();
        assertThat(solid.getPolygonsList().size()).isEqualTo(6);
        assertThat(solid.getEdgesList().size()).isEqualTo(12);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(solid)).isTrue();
    }

    /**
    Control case (passes today): the documented default orientation of
    lkemr, where the ring collapses to a single vertex.
    */
    @Test
    void given_strutInsideFace_when_lkemr_then_ringIsALoneVertexWithoutEdge()
    {
        PolyhedralBoundedSolid solid = createSquareLaminaWithStrut();
        _PolyhedralBoundedSolidFace face = solid.findFace(1);

        PolyhedralBoundedSolidEulerOperators.lkemr(solid,
            face.findHalfEdge(1, 5), face.findHalfEdge(5, 1));

        assertThat(face.boundariesList.size()).isEqualTo(2);
        _PolyhedralBoundedSolidLoop ring = face.boundariesList.get(1);
        assertThat(ring.halfEdgesList.size()).isEqualTo(1);
        assertThat(ring.boundaryStartHalfEdge.parentEdge).isNull();
        assertThat(ring.boundaryStartHalfEdge.startingVertex.id).isEqualTo(5);
    }

    private static PolyhedralBoundedSolid createSkeletalSolid()
    {
        PolyhedralBoundedSolid solid = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolidEulerOperators.mvfs(solid,
            new Vector3Dd(0.0, 0.0, 0.0), 1, 1);
        return solid;
    }

    /**
    Unit square lamina (faces 1 and 2, vertices 1..4) with a strut from
    vertex 1 to an interior vertex 5 inside face 1.
    */
    private static PolyhedralBoundedSolid createSquareLaminaWithStrut()
    {
        PolyhedralBoundedSolid solid = createSkeletalSolid();
        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 1, 2,
            new Vector3Dd(1.0, 0.0, 0.0));
        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 2, 3,
            new Vector3Dd(1.0, 1.0, 0.0));
        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 3, 4,
            new Vector3Dd(0.0, 1.0, 0.0));
        PolyhedralBoundedSolidEulerOperators.lmef(solid,
            solid.findFace(1).findHalfEdge(4),
            solid.findFace(1).findHalfEdge(1), 2);
        PolyhedralBoundedSolidEulerOperators.lmev(solid,
            solid.findFace(1).findHalfEdge(1),
            solid.findFace(1).findHalfEdge(1), 5,
            new Vector3Dd(0.25, 0.25, 0.0));
        return solid;
    }

    private static java.util.ArrayList<Integer> loopSizes(
        _PolyhedralBoundedSolidFace face)
    {
        java.util.ArrayList<Integer> sizes = new java.util.ArrayList<>();
        int i;

        for ( i = 0; i < face.boundariesList.size(); i++ ) {
            sizes.add(face.boundariesList.get(i).halfEdgesList.size());
        }
        return sizes;
    }

    /**
    Lists every vertex whose emanating half-edge is null, does not start at
    the vertex, or is no longer a member of its parent loop.
    */
    static String describeBrokenEmanatingReferences(
        PolyhedralBoundedSolid solid)
    {
        StringBuilder report = new StringBuilder();
        int i;

        for ( i = 0; i < solid.getVerticesList().size(); i++ ) {
            _PolyhedralBoundedSolidVertex vertex =
                solid.getVerticesList().get(i);
            _PolyhedralBoundedSolidHalfEdge he = vertex.emanatingHalfEdge;

            if ( he == null ) {
                report.append(" v").append(vertex.id).append(":null");
                continue;
            }
            if ( he.startingVertex != vertex ) {
                report.append(" v").append(vertex.id).append(":wrongStart");
            }
            if ( he.parentLoop == null ||
                 !he.parentLoop.halfEdgesList.locateWindowAtElem(he) ) {
                report.append(" v").append(vertex.id).append(":notInLoop");
            }
        }
        return report.toString();
    }
}
