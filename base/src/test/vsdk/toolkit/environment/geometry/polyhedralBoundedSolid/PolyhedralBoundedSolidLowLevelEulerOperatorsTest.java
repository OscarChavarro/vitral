package vsdk.toolkit.environment.geometry.polyhedralBoundedSolid;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;

import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidFace;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidHalfEdge;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.nodes._PolyhedralBoundedSolidLoop;

import static org.assertj.core.api.Assertions.assertThat;

/**
Direct regression suite for the local and same-shell Euler operators.

<p>Traceability: [MANT1988] Ch. 9.2 skeletal/local/global
manipulations; Ch. 11.3 low-level Euler operators, including Programs
11.5-11.8 and the inverse-operator exercises; and Ch. 11.5 high-level
wrappers over the low-level operators.</p>
 */
class PolyhedralBoundedSolidLowLevelEulerOperatorsTest
{
    @Test
    void given_emptySolid_when_mvfs_then_createsSkeletalHalfEdgeStructure()
    {
        PolyhedralBoundedSolid solid = new PolyhedralBoundedSolid();

        solid.mvfs(new Vector3D(1.0, 2.0, 3.0), 7, 11);

        assertThat(solid.polygonsList.size()).isEqualTo(1);
        assertThat(solid.verticesList.size()).isEqualTo(1);
        assertThat(solid.edgesList.size()).isZero();
        assertThat(solid.getMaxVertexId()).isEqualTo(7);
        assertThat(solid.getMaxFaceId()).isEqualTo(11);

        _PolyhedralBoundedSolidFace face = solid.polygonsList.get(0);
        _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(0);
        _PolyhedralBoundedSolidHalfEdge halfEdge = loop.boundaryStartHalfEdge;

        assertThat(face.parentSolid).isSameAs(solid);
        assertThat(loop.parentFace).isSameAs(face);
        assertThat(loop.halfEdgesList.size()).isEqualTo(1);
        assertThat(halfEdge.parentEdge).isNull();
        assertThat(halfEdge.parentLoop).isSameAs(loop);
        assertThat(halfEdge.startingVertex).isSameAs(solid.verticesList.get(0));
        assertThat(halfEdge.next()).isSameAs(halfEdge);
        assertThat(halfEdge.previous()).isSameAs(halfEdge);
    }

    @Test
    void given_skeletalSolid_when_lmevReceivesSameHalfEdge_then_createsStrut()
    {
        PolyhedralBoundedSolid solid = createSkeletalSolid();
        _PolyhedralBoundedSolidHalfEdge seed =
            solid.findFace(1).boundariesList.get(0).boundaryStartHalfEdge;

        solid.lmev(seed, seed, 2, new Vector3D(1.0, 0.0, 0.0));

        assertThat(solid.polygonsList.size()).isEqualTo(1);
        assertThat(solid.edgesList.size()).isEqualTo(1);
        assertThat(solid.verticesList.size()).isEqualTo(2);
        assertThat(solid.findFace(1).boundariesList.get(0).halfEdgesList.size())
            .isEqualTo(2);

        _PolyhedralBoundedSolidEdge edge = solid.edgesList.get(0);
        assertThat(edge.leftHalf).isNotNull();
        assertThat(edge.rightHalf).isNotNull();
        assertThat(edge.leftHalf.parentLoop).isSameAs(edge.rightHalf.parentLoop);
        assertThat(edge.leftHalf.mirrorHalfEdge()).isSameAs(edge.rightHalf);
        assertThat(edge.rightHalf.mirrorHalfEdge()).isSameAs(edge.leftHalf);
        assertThat(edge.rightHalf.startingVertex.id).isEqualTo(2);
        assertThat(edge.leftHalf.startingVertex.id).isEqualTo(1);
    }

    @Test
    void given_strut_when_lkevKillsItsEdge_then_returnsToSkeletalState()
    {
        PolyhedralBoundedSolid solid = createSkeletalSolid();
        _PolyhedralBoundedSolidHalfEdge seed =
            solid.findFace(1).boundariesList.get(0).boundaryStartHalfEdge;
        solid.lmev(seed, seed, 2, new Vector3D(1.0, 0.0, 0.0));
        _PolyhedralBoundedSolidEdge edge = solid.edgesList.get(0);

        solid.lkev(edge.rightHalf, edge.leftHalf);

        assertThat(solid.edgesList.size()).isZero();
        assertThat(solid.verticesList.size()).isEqualTo(1);
        assertThat(solid.polygonsList.size()).isEqualTo(1);
        assertThat(solid.findFace(1).boundariesList.get(0).halfEdgesList.size())
            .isEqualTo(1);
        assertThat(solid.findFace(1).boundariesList.get(0)
            .boundaryStartHalfEdge.parentEdge).isNull();
    }

    @Test
    void given_boxVertexNeighborhood_when_lmevSplitsVertex_then_addsOneEdgeAndOneVertex()
    {
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0,
                0.0, 0.0, 0.0);
        _PolyhedralBoundedSolidHalfEdge[] pair =
            firstDistinctHalfEdgesStartingAtSameVertex(solid);
        int faceCount = solid.polygonsList.size();
        int edgeCount = solid.edgesList.size();
        int vertexCount = solid.verticesList.size();
        int newVertexId = solid.getMaxVertexId() + 1;

        solid.lmev(pair[0], pair[1], newVertexId, pair[0].startingVertex.position);

        assertThat(solid.polygonsList.size()).isEqualTo(faceCount);
        assertThat(solid.edgesList.size()).isEqualTo(edgeCount + 1);
        assertThat(solid.verticesList.size()).isEqualTo(vertexCount + 1);
        assertThat(solid.findVertex(newVertexId)).isNotNull();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(solid)).isTrue();
    }

    @Test
    void given_openWire_when_lmefClosesLoop_then_addsOneEdgeAndOneFace()
    {
        PolyhedralBoundedSolid solid = createThreeEdgeWire();
        _PolyhedralBoundedSolidFace face = solid.findFace(1);
        _PolyhedralBoundedSolidHalfEdge he1 = face.findHalfEdge(4);
        _PolyhedralBoundedSolidHalfEdge he2 = face.findHalfEdge(1);
        int edgeCount = solid.edgesList.size();
        int vertexCount = solid.verticesList.size();

        _PolyhedralBoundedSolidFace newFace = solid.lmef(he1, he2, 2);

        assertThat(newFace).isNotNull();
        assertThat(solid.polygonsList.size()).isEqualTo(2);
        assertThat(solid.edgesList.size()).isEqualTo(edgeCount + 1);
        assertThat(solid.verticesList.size()).isEqualTo(vertexCount);
        assertThat(newFace.boundariesList.size()).isEqualTo(1);
        assertThat(newFace.boundariesList.get(0).halfEdgesList.size())
            .isGreaterThanOrEqualTo(1);
    }

    @Test
    void given_lmefResult_when_lkefKillsCreatedEdge_then_mergesFaces()
    {
        PolyhedralBoundedSolid solid = createThreeEdgeWire();
        _PolyhedralBoundedSolidFace face = solid.findFace(1);
        _PolyhedralBoundedSolidFace newFace =
            solid.lmef(face.findHalfEdge(4), face.findHalfEdge(1), 2);
        _PolyhedralBoundedSolidEdge edgeToKill =
            newFace.boundariesList.get(0).boundaryStartHalfEdge.parentEdge;
        int edgeCount = solid.edgesList.size();

        solid.lkef(edgeToKill.rightHalf, edgeToKill.leftHalf);

        assertThat(solid.polygonsList.size()).isEqualTo(1);
        assertThat(solid.edgesList.size()).isEqualTo(edgeCount - 1);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(solid)).isTrue();
    }

    @Test
    void given_faceWithBridgeEdge_when_lkemrKillsEdge_then_createsRing()
    {
        PolyhedralBoundedSolid solid = createPlanarFaceWithBridgeToHoleSeed();
        _PolyhedralBoundedSolidFace face = solid.findFace(1);
        _PolyhedralBoundedSolidHalfEdge he1 = face.findHalfEdge(1, 5);
        _PolyhedralBoundedSolidHalfEdge he2 = face.findHalfEdge(5, 1);
        int edgeCount = solid.edgesList.size();
        int loopCount = face.boundariesList.size();

        solid.lkemr(he1, he2);

        assertThat(solid.edgesList.size()).isEqualTo(edgeCount - 1);
        assertThat(face.boundariesList.size()).isEqualTo(loopCount + 1);
    }

    @Test
    void given_lkemrResult_when_lmekrConnectsLoops_then_restoresSingleLoop()
    {
        PolyhedralBoundedSolid solid = createPlanarFaceWithBridgeToHoleSeed();
        _PolyhedralBoundedSolidFace face = solid.findFace(1);
        _PolyhedralBoundedSolidHalfEdge he1 = face.findHalfEdge(1, 5);
        _PolyhedralBoundedSolidHalfEdge he2 = face.findHalfEdge(5, 1);
        solid.lkemr(he1, he2);
        int edgeCount = solid.edgesList.size();
        _PolyhedralBoundedSolidLoop outer = face.boundariesList.get(0);
        _PolyhedralBoundedSolidLoop ring = face.boundariesList.get(1);

        solid.lmekr(outer.boundaryStartHalfEdge, ring.boundaryStartHalfEdge);

        assertThat(solid.edgesList.size()).isEqualTo(edgeCount + 1);
        assertThat(face.boundariesList.size()).isEqualTo(1);
    }

    @Test
    void given_faceWithRing_when_lringmvReordersOuterLoop_then_keepsSameFace()
    {
        PolyhedralBoundedSolid solid = createPlanarFaceWithBridgeToHoleSeed();
        _PolyhedralBoundedSolidFace face = solid.findFace(1);
        solid.lkemr(face.findHalfEdge(1, 5), face.findHalfEdge(5, 1));
        _PolyhedralBoundedSolidLoop ring = face.boundariesList.get(1);

        boolean movedToOuter = solid.lringmv(ring, face, true);
        boolean ringBecameOuter = (face.boundariesList.get(0) == ring);
        boolean movedBackToInner = solid.lringmv(ring, face, false);

        assertThat(movedToOuter).isTrue();
        assertThat(ringBecameOuter).isTrue();
        assertThat(movedBackToInner).isTrue();
        assertThat(face.boundariesList.get(0)).isNotSameAs(ring);
        assertThat(ring.parentFace).isSameAs(face);
    }

    @Test
    void given_faceWithRing_when_lringmvMovesLoopToAnotherFace_then_updatesOwnership()
    {
        PolyhedralBoundedSolid solid = createPlanarFaceWithBridgeToHoleSeed();
        _PolyhedralBoundedSolidFace sourceFace = solid.findFace(1);
        _PolyhedralBoundedSolidFace targetFace = solid.findFace(2);
        solid.lkemr(sourceFace.findHalfEdge(1, 5),
            sourceFace.findHalfEdge(5, 1));
        _PolyhedralBoundedSolidLoop ring = sourceFace.boundariesList.get(1);
        int sourceLoopCount = sourceFace.boundariesList.size();
        int targetLoopCount = targetFace.boundariesList.size();

        boolean result = solid.lringmv(ring, targetFace, false);

        assertThat(result).isTrue();
        assertThat(sourceFace.boundariesList.size())
            .isEqualTo(sourceLoopCount - 1);
        assertThat(targetFace.boundariesList.size())
            .isEqualTo(targetLoopCount + 1);
        assertThat(ring.parentFace).isSameAs(targetFace);
        assertThat(targetFace.boundariesList.get(0)).isNotSameAs(ring);
    }

    @Test
    void given_skeletalSolid_when_kvfs_then_clearsSolid()
    {
        PolyhedralBoundedSolid solid = createSkeletalSolid();

        solid.kvfs();

        assertThat(solid.polygonsList.size()).isZero();
        assertThat(solid.edgesList.size()).isZero();
        assertThat(solid.verticesList.size()).isZero();
    }

    @Test
    void given_skeletalSolid_when_smev_then_delegatesToRobustLmev()
    {
        PolyhedralBoundedSolid solid = createSkeletalSolid();

        boolean result = solid.smev(1, 1, 2, new Vector3D(1.0, 0.0, 0.0));

        assertThat(result).isTrue();
        assertThat(solid.polygonsList.size()).isEqualTo(1);
        assertThat(solid.edgesList.size()).isEqualTo(1);
        assertThat(solid.verticesList.size()).isEqualTo(2);
    }

    @Test
    void given_boxVertexNeighborhood_when_mev_then_splitsVertex()
    {
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0,
                0.0, 0.0, 0.0);
        _PolyhedralBoundedSolidHalfEdge[] pair =
            firstDistinctHalfEdgesStartingAtSameVertex(solid);
        int vertexCount = solid.verticesList.size();
        int edgeCount = solid.edgesList.size();
        int newVertexId = solid.getMaxVertexId() + 1;

        boolean result = solid.mev(
            pair[0].parentLoop.parentFace.id,
            pair[1].parentLoop.parentFace.id,
            pair[0].startingVertex.id,
            pair[0].next().startingVertex.id,
            pair[1].next().startingVertex.id,
            newVertexId,
            pair[0].startingVertex.position);

        assertThat(result).isTrue();
        assertThat(solid.verticesList.size()).isEqualTo(vertexCount + 1);
        assertThat(solid.edgesList.size()).isEqualTo(edgeCount + 1);
        assertThat(solid.findVertex(newVertexId)).isNotNull();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(solid)).isTrue();
    }

    @Test
    void given_openWire_when_smef_then_closesLoop()
    {
        PolyhedralBoundedSolid solid = createThreeEdgeWire();

        boolean result = solid.smef(1, 4, 1, 2);

        assertThat(result).isTrue();
        assertThat(solid.polygonsList.size()).isEqualTo(2);
        assertThat(solid.edgesList.size()).isEqualTo(4);
    }

    @Test
    void given_openWire_when_mef_then_closesLoop()
    {
        PolyhedralBoundedSolid solid = createThreeEdgeWire();
        _PolyhedralBoundedSolidFace face = solid.findFace(1);
        _PolyhedralBoundedSolidHalfEdge he1 = face.findHalfEdge(4);
        _PolyhedralBoundedSolidHalfEdge he2 = face.findHalfEdge(1);

        boolean result = solid.mef(1, 1,
            he1.startingVertex.id, he1.next().startingVertex.id,
            he2.startingVertex.id, he2.next().startingVertex.id, 2);

        assertThat(result).isTrue();
        assertThat(solid.polygonsList.size()).isEqualTo(2);
        assertThat(solid.edgesList.size()).isEqualTo(4);
    }

    @Test
    void given_faceWithBridgeEdge_when_kemr_then_createsRing()
    {
        PolyhedralBoundedSolid solid = createPlanarFaceWithBridgeToHoleSeed();
        _PolyhedralBoundedSolidFace face = solid.findFace(1);
        int loopCount = face.boundariesList.size();

        boolean result = solid.kemr(1, 1, 1, 5, 5, 1);

        assertThat(result).isTrue();
        assertThat(face.boundariesList.size()).isEqualTo(loopCount + 1);
    }

    @Test
    void given_twoFaces_when_kfmrh_then_secondFaceBecomesInnerLoop()
    {
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0,
                0.0, 0.0, 0.0);
        _PolyhedralBoundedSolidFace face1 = solid.polygonsList.get(0);
        _PolyhedralBoundedSolidFace face2 = solid.polygonsList.get(1);
        int faceCount = solid.polygonsList.size();
        int loopCount = face1.boundariesList.size();

        boolean result = solid.kfmrh(face1.id, face2.id);

        assertThat(result).isTrue();
        assertThat(solid.polygonsList.size()).isEqualTo(faceCount - 1);
        assertThat(face1.boundariesList.size())
            .isEqualTo(loopCount + face2.boundariesList.size());
        assertThat(solid.findFace(face2.id)).isNull();
    }

    @Test
    void given_boxNeighborhood_when_lmevThenLkev_then_restoresTopologicalIdentity()
    {
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0,
                0.0, 0.0, 0.0);
        TopologicalSignature before = TopologicalSignature.from(solid);
        _PolyhedralBoundedSolidHalfEdge[] pair =
            firstDistinctHalfEdgesStartingAtSameVertex(solid);
        int newVertexId = solid.getMaxVertexId() + 1;

        solid.lmev(pair[0], pair[1], newVertexId, pair[0].startingVertex.position);
        _PolyhedralBoundedSolidHalfEdge newVertexHalfEdge =
            solid.findVertex(newVertexId).emanatingHalfEdge;
        solid.lkev(newVertexHalfEdge, newVertexHalfEdge.mirrorHalfEdge());

        assertThat(TopologicalSignature.from(solid)).isEqualTo(before);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(solid)).isTrue();
    }

    @Test
    void given_openWire_when_lmefThenLkef_then_restoresTopologicalIdentity()
    {
        PolyhedralBoundedSolid solid = createThreeEdgeWire();
        TopologicalSignature before = TopologicalSignature.from(solid);
        _PolyhedralBoundedSolidFace face = solid.findFace(1);
        _PolyhedralBoundedSolidFace newFace =
            solid.lmef(face.findHalfEdge(4), face.findHalfEdge(1), 2);
        _PolyhedralBoundedSolidEdge edgeToKill =
            newFace.boundariesList.get(0).boundaryStartHalfEdge.parentEdge;

        solid.lkef(edgeToKill.rightHalf, edgeToKill.leftHalf);

        assertThat(TopologicalSignature.from(solid)).isEqualTo(before);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(solid)).isTrue();
    }

    @Test
    void given_faceWithBridge_when_lkemrThenLmekr_then_restoresTopologicalIdentity()
    {
        PolyhedralBoundedSolid solid = createPlanarFaceWithBridgeToHoleSeed();
        TopologicalSignature before = TopologicalSignature.from(solid);
        _PolyhedralBoundedSolidFace face = solid.findFace(1);
        solid.lkemr(face.findHalfEdge(1, 5), face.findHalfEdge(5, 1));
        _PolyhedralBoundedSolidLoop outer = face.boundariesList.get(0);
        _PolyhedralBoundedSolidLoop ring = face.boundariesList.get(1);

        solid.lmekr(outer.boundaryStartHalfEdge, ring.boundaryStartHalfEdge);

        assertThat(TopologicalSignature.from(solid)).isEqualTo(before);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(solid)).isTrue();
    }

    @Test
    void given_twoFaces_when_lkfmrhThenLmfkrh_then_restoresTopologicalIdentity()
    {
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0,
                0.0, 0.0, 0.0);
        TopologicalSignature before = TopologicalSignature.from(solid);
        _PolyhedralBoundedSolidFace face1 = solid.polygonsList.get(0);
        _PolyhedralBoundedSolidFace face2 = solid.polygonsList.get(1);
        int newFaceId = solid.getMaxFaceId() + 1;

        solid.lkfmrh(face1, face2);
        _PolyhedralBoundedSolidLoop loopToPromote =
            face1.boundariesList.get(face1.boundariesList.size() - 1);
        solid.lmfkrh(loopToPromote, newFaceId);

        assertThat(TopologicalSignature.from(solid)).isEqualTo(before);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(solid)).isTrue();
    }

    @Test
    void given_skeletalSolid_when_twoStrutsThenLmef_then_createsTriangularLaminaTopology()
    {
        PolyhedralBoundedSolid solid = createSkeletalSolid();
        _PolyhedralBoundedSolidHalfEdge seed =
            solid.findFace(1).boundariesList.get(0).boundaryStartHalfEdge;

        solid.lmev(seed, seed, 2, new Vector3D(1.0, 0.0, 0.0));
        solid.lmev(solid.findVertex(2).emanatingHalfEdge,
            solid.findVertex(2).emanatingHalfEdge, 3,
            new Vector3D(0.0, 1.0, 0.0));
        solid.lmef(solid.findFace(1).findHalfEdge(3),
            solid.findFace(1).findHalfEdge(1), 2);

        assertThat(solid.polygonsList.size()).isEqualTo(2);
        assertThat(solid.edgesList.size()).isEqualTo(3);
        assertThat(solid.verticesList.size()).isEqualTo(3);
        assertThat(TopologicalSignature.from(solid).loopHalfEdgeCounts)
            .containsExactly(3, 3);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(solid)).isTrue();
    }

    private static PolyhedralBoundedSolid createSkeletalSolid()
    {
        PolyhedralBoundedSolid solid = new PolyhedralBoundedSolid();
        solid.mvfs(new Vector3D(0.0, 0.0, 0.0), 1, 1);
        return solid;
    }

    private static PolyhedralBoundedSolid createThreeEdgeWire()
    {
        PolyhedralBoundedSolid solid = createSkeletalSolid();
        _PolyhedralBoundedSolidHalfEdge he =
            solid.findFace(1).boundariesList.get(0).boundaryStartHalfEdge;

        solid.lmev(he, he, 2, new Vector3D(1.0, 0.0, 0.0));
        he = solid.findVertex(2).emanatingHalfEdge;
        solid.lmev(he, he, 3, new Vector3D(1.0, 1.0, 0.0));
        he = solid.findVertex(3).emanatingHalfEdge;
        solid.lmev(he, he, 4, new Vector3D(0.0, 1.0, 0.0));
        return solid;
    }

    private static PolyhedralBoundedSolid createPlanarFaceWithBridgeToHoleSeed()
    {
        PolyhedralBoundedSolid solid = createSkeletalSolid();
        solid.lmev(solid.findFace(1).boundariesList.get(0).boundaryStartHalfEdge,
            solid.findFace(1).boundariesList.get(0).boundaryStartHalfEdge,
            2, new Vector3D(1.0, 0.0, 0.0));
        solid.lmev(solid.findVertex(2).emanatingHalfEdge,
            solid.findVertex(2).emanatingHalfEdge, 3,
            new Vector3D(1.0, 1.0, 0.0));
        solid.lmev(solid.findVertex(3).emanatingHalfEdge,
            solid.findVertex(3).emanatingHalfEdge, 4,
            new Vector3D(0.0, 1.0, 0.0));
        solid.lmef(solid.findFace(1).findHalfEdge(4),
            solid.findFace(1).findHalfEdge(1), 2);
        solid.lmev(solid.findFace(1).findHalfEdge(1),
            solid.findFace(1).findHalfEdge(1), 5,
            new Vector3D(0.25, 0.25, 0.0));
        return solid;
    }

    private static _PolyhedralBoundedSolidHalfEdge[]
    firstDistinctHalfEdgesStartingAtSameVertex(PolyhedralBoundedSolid solid)
    {
        int i;
        int j;

        for ( i = 0; i < solid.polygonsList.size(); i++ ) {
            _PolyhedralBoundedSolidHalfEdge a =
                solid.polygonsList.get(i).boundariesList.get(0)
                    .boundaryStartHalfEdge;
            do {
                for ( j = i; j < solid.polygonsList.size(); j++ ) {
                    _PolyhedralBoundedSolidHalfEdge b =
                        solid.polygonsList.get(j).boundariesList.get(0)
                            .boundaryStartHalfEdge;
                    do {
                        if ( a != b && a.startingVertex == b.startingVertex ) {
                            return new _PolyhedralBoundedSolidHalfEdge[] { a, b };
                        }
                        b = b.next();
                    } while ( b != solid.polygonsList.get(j).boundariesList.get(0)
                        .boundaryStartHalfEdge );
                }
                a = a.next();
            } while ( a != solid.polygonsList.get(i).boundariesList.get(0)
                .boundaryStartHalfEdge );
        }
        throw new IllegalStateException("Expected at least one split vertex.");
    }

    private static final class TopologicalSignature
    {
        private final int faceCount;
        private final int edgeCount;
        private final int vertexCount;
        private final ArrayList<Integer> loopsPerFace;
        private final ArrayList<Integer> loopHalfEdgeCounts;

        private TopologicalSignature(PolyhedralBoundedSolid solid)
        {
            int i;
            int j;

            faceCount = solid.polygonsList.size();
            edgeCount = solid.edgesList.size();
            vertexCount = solid.verticesList.size();
            loopsPerFace = new ArrayList<Integer>();
            loopHalfEdgeCounts = new ArrayList<Integer>();

            for ( i = 0; i < solid.polygonsList.size(); i++ ) {
                _PolyhedralBoundedSolidFace face = solid.polygonsList.get(i);
                loopsPerFace.add(face.boundariesList.size());
                for ( j = 0; j < face.boundariesList.size(); j++ ) {
                    loopHalfEdgeCounts.add(
                        face.boundariesList.get(j).halfEdgesList.size());
                }
            }
            Collections.sort(loopsPerFace);
            Collections.sort(loopHalfEdgeCounts);
        }

        static TopologicalSignature from(PolyhedralBoundedSolid solid)
        {
            return new TopologicalSignature(solid);
        }

        @Override
        public boolean equals(Object other)
        {
            if ( this == other ) {
                return true;
            }
            if ( !(other instanceof TopologicalSignature) ) {
                return false;
            }
            TopologicalSignature that = (TopologicalSignature)other;
            return faceCount == that.faceCount &&
                edgeCount == that.edgeCount &&
                vertexCount == that.vertexCount &&
                loopsPerFace.equals(that.loopsPerFace) &&
                loopHalfEdgeCounts.equals(that.loopHalfEdgeCounts);
        }

        @Override
        public int hashCode()
        {
            int result;

            result = faceCount;
            result = 31 * result + edgeCount;
            result = 31 * result + vertexCount;
            result = 31 * result + loopsPerFace.hashCode();
            result = 31 * result + loopHalfEdgeCounts.hashCode();
            return result;
        }

        @Override
        public String toString()
        {
            return "TopologicalSignature{" +
                "faces=" + faceCount +
                ", edges=" + edgeCount +
                ", vertices=" + vertexCount +
                ", loopsPerFace=" + loopsPerFace +
                ", loopHalfEdgeCounts=" + loopHalfEdgeCounts +
                '}';
        }
    }
}
