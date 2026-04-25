package vsdk.toolkit.environment.geometry.polyhedralBoundedSolid;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidEulerOperators;

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
class PolyhedralBoundedSolidEulerOperatorsTest
{
    @Test
    void given_emptySolid_when_mvfs_then_createsSkeletalHalfEdgeStructure()
    {
        // Arrange
        PolyhedralBoundedSolid solid = new PolyhedralBoundedSolid();

        // Action
        PolyhedralBoundedSolidEulerOperators.mvfs(solid, new Vector3D(1.0, 2.0, 3.0), 7, 11);

        // Assert
        assertThat(solid.getPolygonsList().size()).isEqualTo(1);
        assertThat(solid.getVerticesList().size()).isEqualTo(1);
        assertThat(solid.getEdgesList().size()).isZero();
        assertThat(solid.getMaxVertexId()).isEqualTo(7);
        assertThat(solid.getMaxFaceId()).isEqualTo(11);

        _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(0);
        _PolyhedralBoundedSolidLoop loop = face.boundariesList.get(0);
        _PolyhedralBoundedSolidHalfEdge halfEdge = loop.boundaryStartHalfEdge;

        assertThat(face.parentSolid).isSameAs(solid);
        assertThat(loop.parentFace).isSameAs(face);
        assertThat(loop.halfEdgesList.size()).isEqualTo(1);
        assertThat(halfEdge.parentEdge).isNull();
        assertThat(halfEdge.parentLoop).isSameAs(loop);
        assertThat(halfEdge.startingVertex).isSameAs(solid.getVerticesList().get(0));
        assertThat(halfEdge.next()).isSameAs(halfEdge);
        assertThat(halfEdge.previous()).isSameAs(halfEdge);
    }

    @Test
    void given_skeletalSolid_when_lmevReceivesSameHalfEdge_then_createsStrut()
    {
        // Arrange
        PolyhedralBoundedSolid solid = createSkeletalSolid();
        _PolyhedralBoundedSolidHalfEdge seed =
            solid.findFace(1).boundariesList.get(0).boundaryStartHalfEdge;

        // Action
        PolyhedralBoundedSolidEulerOperators.lmev(solid, seed, seed, 2, new Vector3D(1.0, 0.0, 0.0));

        // Assert
        assertThat(solid.getPolygonsList().size()).isEqualTo(1);
        assertThat(solid.getEdgesList().size()).isEqualTo(1);
        assertThat(solid.getVerticesList().size()).isEqualTo(2);
        assertThat(solid.findFace(1).boundariesList.get(0).halfEdgesList.size())
            .isEqualTo(2);

        _PolyhedralBoundedSolidEdge edge = solid.getEdgesList().get(0);
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
        // Arrange
        PolyhedralBoundedSolid solid = createSkeletalSolid();
        _PolyhedralBoundedSolidHalfEdge seed =
            solid.findFace(1).boundariesList.get(0).boundaryStartHalfEdge;
        PolyhedralBoundedSolidEulerOperators.lmev(solid, seed, seed, 2, new Vector3D(1.0, 0.0, 0.0));
        _PolyhedralBoundedSolidEdge edge = solid.getEdgesList().get(0);

        // Action
        PolyhedralBoundedSolidEulerOperators.lkev(solid, edge.rightHalf, edge.leftHalf);

        // Assert
        assertThat(solid.getEdgesList().size()).isZero();
        assertThat(solid.getVerticesList().size()).isEqualTo(1);
        assertThat(solid.getPolygonsList().size()).isEqualTo(1);
        assertThat(solid.findFace(1).boundariesList.get(0).halfEdgesList.size())
            .isEqualTo(1);
        assertThat(solid.findFace(1).boundariesList.get(0)
            .boundaryStartHalfEdge.parentEdge).isNull();
    }

    @Test
    void given_boxVertexNeighborhood_when_lmevSplitsVertex_then_addsOneEdgeAndOneVertex()
    {
        // Arrange
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0,
                0.0, 0.0, 0.0);
        _PolyhedralBoundedSolidHalfEdge[] pair =
            firstDistinctHalfEdgesStartingAtSameVertex(solid);
        int faceCount = solid.getPolygonsList().size();
        int edgeCount = solid.getEdgesList().size();
        int vertexCount = solid.getVerticesList().size();
        int newVertexId = solid.getMaxVertexId() + 1;

        // Action
        PolyhedralBoundedSolidEulerOperators.lmev(solid, pair[0], pair[1], newVertexId, pair[0].startingVertex.position);

        // Assert
        assertThat(solid.getPolygonsList().size()).isEqualTo(faceCount);
        assertThat(solid.getEdgesList().size()).isEqualTo(edgeCount + 1);
        assertThat(solid.getVerticesList().size()).isEqualTo(vertexCount + 1);
        assertThat(solid.findVertex(newVertexId)).isNotNull();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(solid)).isTrue();
    }

    @Test
    void given_openWire_when_lmefClosesLoop_then_addsOneEdgeAndOneFace()
    {
        // Arrange
        PolyhedralBoundedSolid solid = createThreeEdgeWire();
        _PolyhedralBoundedSolidFace face = solid.findFace(1);
        _PolyhedralBoundedSolidHalfEdge he1 = face.findHalfEdge(4);
        _PolyhedralBoundedSolidHalfEdge he2 = face.findHalfEdge(1);
        int edgeCount = solid.getEdgesList().size();
        int vertexCount = solid.getVerticesList().size();

        // Action
        _PolyhedralBoundedSolidFace newFace = PolyhedralBoundedSolidEulerOperators.lmef(solid, he1, he2, 2);

        // Assert
        assertThat(newFace).isNotNull();
        assertThat(solid.getPolygonsList().size()).isEqualTo(2);
        assertThat(solid.getEdgesList().size()).isEqualTo(edgeCount + 1);
        assertThat(solid.getVerticesList().size()).isEqualTo(vertexCount);
        assertThat(newFace.boundariesList.size()).isEqualTo(1);
        assertThat(newFace.boundariesList.get(0).halfEdgesList.size())
            .isGreaterThanOrEqualTo(1);
    }

    @Test
    void given_lmefResult_when_lkefKillsCreatedEdge_then_mergesFaces()
    {
        // Arrange
        PolyhedralBoundedSolid solid = createThreeEdgeWire();
        _PolyhedralBoundedSolidFace face = solid.findFace(1);
        _PolyhedralBoundedSolidFace newFace =
            PolyhedralBoundedSolidEulerOperators.lmef(solid, face.findHalfEdge(4), face.findHalfEdge(1), 2);
        _PolyhedralBoundedSolidEdge edgeToKill =
            newFace.boundariesList.get(0).boundaryStartHalfEdge.parentEdge;
        int edgeCount = solid.getEdgesList().size();

        // Action
        PolyhedralBoundedSolidEulerOperators.lkef(solid, edgeToKill.rightHalf, edgeToKill.leftHalf);

        // Assert
        assertThat(solid.getPolygonsList().size()).isEqualTo(1);
        assertThat(solid.getEdgesList().size()).isEqualTo(edgeCount - 1);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(solid)).isTrue();
    }

    @Test
    void given_faceWithBridgeEdge_when_lkemrKillsEdge_then_createsRing()
    {
        // Arrange
        PolyhedralBoundedSolid solid = createPlanarFaceWithBridgeToHoleSeed();
        _PolyhedralBoundedSolidFace face = solid.findFace(1);
        _PolyhedralBoundedSolidHalfEdge he1 = face.findHalfEdge(1, 5);
        _PolyhedralBoundedSolidHalfEdge he2 = face.findHalfEdge(5, 1);
        int edgeCount = solid.getEdgesList().size();
        int loopCount = face.boundariesList.size();

        // Action
        PolyhedralBoundedSolidEulerOperators.lkemr(solid, he1, he2);

        // Assert
        assertThat(solid.getEdgesList().size()).isEqualTo(edgeCount - 1);
        assertThat(face.boundariesList.size()).isEqualTo(loopCount + 1);
    }

    @Test
    void given_lkemrResult_when_lmekrConnectsLoops_then_restoresSingleLoop()
    {
        // Arrange
        PolyhedralBoundedSolid solid = createPlanarFaceWithBridgeToHoleSeed();
        _PolyhedralBoundedSolidFace face = solid.findFace(1);
        _PolyhedralBoundedSolidHalfEdge he1 = face.findHalfEdge(1, 5);
        _PolyhedralBoundedSolidHalfEdge he2 = face.findHalfEdge(5, 1);
        PolyhedralBoundedSolidEulerOperators.lkemr(solid, he1, he2);
        int edgeCount = solid.getEdgesList().size();
        _PolyhedralBoundedSolidLoop outer = face.boundariesList.get(0);
        _PolyhedralBoundedSolidLoop ring = face.boundariesList.get(1);

        // Action
        PolyhedralBoundedSolidEulerOperators.lmekr(solid, outer.boundaryStartHalfEdge, ring.boundaryStartHalfEdge);

        // Assert
        assertThat(solid.getEdgesList().size()).isEqualTo(edgeCount + 1);
        assertThat(face.boundariesList.size()).isEqualTo(1);
    }

    @Test
    void given_faceWithRing_when_lringmvReordersOuterLoop_then_keepsSameFace()
    {
        // Arrange
        PolyhedralBoundedSolid solid = createPlanarFaceWithBridgeToHoleSeed();
        _PolyhedralBoundedSolidFace face = solid.findFace(1);
        PolyhedralBoundedSolidEulerOperators.lkemr(solid, face.findHalfEdge(1, 5), face.findHalfEdge(5, 1));
        _PolyhedralBoundedSolidLoop ring = face.boundariesList.get(1);

        // Action
        boolean movedToOuter = PolyhedralBoundedSolidEulerOperators.lringmv(solid, ring, face, true);
        boolean ringBecameOuter = (face.boundariesList.get(0) == ring);
        boolean movedBackToInner = PolyhedralBoundedSolidEulerOperators.lringmv(solid, ring, face, false);

        // Assert
        assertThat(movedToOuter).isTrue();
        assertThat(ringBecameOuter).isTrue();
        assertThat(movedBackToInner).isTrue();
        assertThat(face.boundariesList.get(0)).isNotSameAs(ring);
        assertThat(ring.parentFace).isSameAs(face);
    }

    @Test
    void given_faceWithRing_when_lringmvMovesLoopToAnotherFace_then_updatesOwnership()
    {
        // Arrange
        PolyhedralBoundedSolid solid = createPlanarFaceWithBridgeToHoleSeed();
        _PolyhedralBoundedSolidFace sourceFace = solid.findFace(1);
        _PolyhedralBoundedSolidFace targetFace = solid.findFace(2);
        PolyhedralBoundedSolidEulerOperators.lkemr(solid, sourceFace.findHalfEdge(1, 5),
            sourceFace.findHalfEdge(5, 1));
        _PolyhedralBoundedSolidLoop ring = sourceFace.boundariesList.get(1);
        int sourceLoopCount = sourceFace.boundariesList.size();
        int targetLoopCount = targetFace.boundariesList.size();

        // Action
        boolean result = PolyhedralBoundedSolidEulerOperators.lringmv(solid, ring, targetFace, false);

        // Assert
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
        // Arrange
        PolyhedralBoundedSolid solid = createSkeletalSolid();

        // Action
        PolyhedralBoundedSolidEulerOperators.kvfs(solid);

        // Assert
        assertThat(solid.getPolygonsList().size()).isZero();
        assertThat(solid.getEdgesList().size()).isZero();
        assertThat(solid.getVerticesList().size()).isZero();
    }

    @Test
    void given_skeletalSolid_when_smev_then_delegatesToRobustLmev()
    {
        // Arrange
        PolyhedralBoundedSolid solid = createSkeletalSolid();

        // Action
        boolean result = PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 1, 2, new Vector3D(1.0, 0.0, 0.0));

        // Assert
        assertThat(result).isTrue();
        assertThat(solid.getPolygonsList().size()).isEqualTo(1);
        assertThat(solid.getEdgesList().size()).isEqualTo(1);
        assertThat(solid.getVerticesList().size()).isEqualTo(2);
    }

    @Test
    void given_boxVertexNeighborhood_when_mev_then_splitsVertex()
    {
        // Arrange
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0,
                0.0, 0.0, 0.0);
        _PolyhedralBoundedSolidHalfEdge[] pair =
            firstDistinctHalfEdgesStartingAtSameVertex(solid);
        int vertexCount = solid.getVerticesList().size();
        int edgeCount = solid.getEdgesList().size();
        int newVertexId = solid.getMaxVertexId() + 1;

        // Action
        boolean result = PolyhedralBoundedSolidEulerOperators.mev(solid, 
            pair[0].parentLoop.parentFace.id,
            pair[1].parentLoop.parentFace.id,
            pair[0].startingVertex.id,
            pair[0].next().startingVertex.id,
            pair[1].next().startingVertex.id,
            newVertexId,
            pair[0].startingVertex.position);

        // Assert
        assertThat(result).isTrue();
        assertThat(solid.getVerticesList().size()).isEqualTo(vertexCount + 1);
        assertThat(solid.getEdgesList().size()).isEqualTo(edgeCount + 1);
        assertThat(solid.findVertex(newVertexId)).isNotNull();
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(solid)).isTrue();
    }

    @Test
    void given_openWire_when_smef_then_closesLoop()
    {
        // Arrange
        PolyhedralBoundedSolid solid = createThreeEdgeWire();

        // Action
        boolean result = PolyhedralBoundedSolidEulerOperators.smef(solid, 1, 4, 1, 2);

        // Assert
        assertThat(result).isTrue();
        assertThat(solid.getPolygonsList().size()).isEqualTo(2);
        assertThat(solid.getEdgesList().size()).isEqualTo(4);
    }

    @Test
    void given_openWire_when_mef_then_closesLoop()
    {
        // Arrange
        PolyhedralBoundedSolid solid = createThreeEdgeWire();
        _PolyhedralBoundedSolidFace face = solid.findFace(1);
        _PolyhedralBoundedSolidHalfEdge he1 = face.findHalfEdge(4);
        _PolyhedralBoundedSolidHalfEdge he2 = face.findHalfEdge(1);

        // Action
        boolean result = PolyhedralBoundedSolidEulerOperators.mef(solid, 1, 1,
            he1.startingVertex.id, he1.next().startingVertex.id,
            he2.startingVertex.id, he2.next().startingVertex.id, 2);

        // Assert
        assertThat(result).isTrue();
        assertThat(solid.getPolygonsList().size()).isEqualTo(2);
        assertThat(solid.getEdgesList().size()).isEqualTo(4);
    }

    @Test
    void given_faceWithBridgeEdge_when_kemr_then_createsRing()
    {
        // Arrange
        PolyhedralBoundedSolid solid = createPlanarFaceWithBridgeToHoleSeed();
        _PolyhedralBoundedSolidFace face = solid.findFace(1);
        int loopCount = face.boundariesList.size();

        // Action
        boolean result = PolyhedralBoundedSolidEulerOperators.kemr(solid, 1, 1, 1, 5, 5, 1);

        // Assert
        assertThat(result).isTrue();
        assertThat(face.boundariesList.size()).isEqualTo(loopCount + 1);
    }

    @Test
    void given_twoFaces_when_kfmrh_then_secondFaceBecomesInnerLoop()
    {
        // Arrange
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0,
                0.0, 0.0, 0.0);
        _PolyhedralBoundedSolidFace face1 = solid.getPolygonsList().get(0);
        _PolyhedralBoundedSolidFace face2 = solid.getPolygonsList().get(1);
        int faceCount = solid.getPolygonsList().size();
        int loopCount = face1.boundariesList.size();

        // Action
        boolean result = PolyhedralBoundedSolidEulerOperators.kfmrh(solid, face1.id, face2.id);

        // Assert
        assertThat(result).isTrue();
        assertThat(solid.getPolygonsList().size()).isEqualTo(faceCount - 1);
        assertThat(face1.boundariesList.size())
            .isEqualTo(loopCount + face2.boundariesList.size());
        assertThat(solid.findFace(face2.id)).isNull();
    }

    @Test
    void given_boxNeighborhood_when_lmevThenLkev_then_restoresTopologicalIdentity()
    {
        // Arrange
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0,
                0.0, 0.0, 0.0);
        TopologicalSignature before = TopologicalSignature.from(solid);
        _PolyhedralBoundedSolidHalfEdge[] pair =
            firstDistinctHalfEdgesStartingAtSameVertex(solid);
        int newVertexId = solid.getMaxVertexId() + 1;

        // Action
        PolyhedralBoundedSolidEulerOperators.lmev(solid, pair[0], pair[1], newVertexId, pair[0].startingVertex.position);
        _PolyhedralBoundedSolidHalfEdge newVertexHalfEdge =
            solid.findVertex(newVertexId).emanatingHalfEdge;
        PolyhedralBoundedSolidEulerOperators.lkev(solid, newVertexHalfEdge, newVertexHalfEdge.mirrorHalfEdge());

        // Assert
        assertThat(TopologicalSignature.from(solid)).isEqualTo(before);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(solid)).isTrue();
    }

    @Test
    void given_openWire_when_lmefThenLkef_then_restoresTopologicalIdentity()
    {
        // Arrange
        PolyhedralBoundedSolid solid = createThreeEdgeWire();
        TopologicalSignature before = TopologicalSignature.from(solid);
        _PolyhedralBoundedSolidFace face = solid.findFace(1);
        _PolyhedralBoundedSolidFace newFace =
            PolyhedralBoundedSolidEulerOperators.lmef(solid, face.findHalfEdge(4), face.findHalfEdge(1), 2);
        _PolyhedralBoundedSolidEdge edgeToKill =
            newFace.boundariesList.get(0).boundaryStartHalfEdge.parentEdge;

        // Action
        PolyhedralBoundedSolidEulerOperators.lkef(solid, edgeToKill.rightHalf, edgeToKill.leftHalf);

        // Assert
        assertThat(TopologicalSignature.from(solid)).isEqualTo(before);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(solid)).isTrue();
    }

    @Test
    void given_faceWithBridge_when_lkemrThenLmekr_then_restoresTopologicalIdentity()
    {
        // Arrange
        PolyhedralBoundedSolid solid = createPlanarFaceWithBridgeToHoleSeed();
        TopologicalSignature before = TopologicalSignature.from(solid);
        _PolyhedralBoundedSolidFace face = solid.findFace(1);
        PolyhedralBoundedSolidEulerOperators.lkemr(solid, face.findHalfEdge(1, 5), face.findHalfEdge(5, 1));
        _PolyhedralBoundedSolidLoop outer = face.boundariesList.get(0);
        _PolyhedralBoundedSolidLoop ring = face.boundariesList.get(1);

        // Action
        PolyhedralBoundedSolidEulerOperators.lmekr(solid, outer.boundaryStartHalfEdge, ring.boundaryStartHalfEdge);

        // Assert
        assertThat(TopologicalSignature.from(solid)).isEqualTo(before);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(solid)).isTrue();
    }

    @Test
    void given_twoFaces_when_lkfmrhThenLmfkrh_then_restoresTopologicalIdentity()
    {
        // Arrange
        PolyhedralBoundedSolid solid =
            PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0,
                0.0, 0.0, 0.0);
        TopologicalSignature before = TopologicalSignature.from(solid);
        _PolyhedralBoundedSolidFace face1 = solid.getPolygonsList().get(0);
        _PolyhedralBoundedSolidFace face2 = solid.getPolygonsList().get(1);
        int newFaceId = solid.getMaxFaceId() + 1;

        // Action
        PolyhedralBoundedSolidEulerOperators.lkfmrh(solid, face1, face2);
        _PolyhedralBoundedSolidLoop loopToPromote =
            face1.boundariesList.get(face1.boundariesList.size() - 1);
        PolyhedralBoundedSolidEulerOperators.lmfkrh(solid, loopToPromote, newFaceId);

        // Assert
        assertThat(TopologicalSignature.from(solid)).isEqualTo(before);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(solid)).isTrue();
    }

    @Test
    void given_skeletalSolid_when_twoStrutsThenLmef_then_createsTriangularLaminaTopology()
    {
        // Arrange
        PolyhedralBoundedSolid solid = createSkeletalSolid();
        _PolyhedralBoundedSolidHalfEdge seed =
            solid.findFace(1).boundariesList.get(0).boundaryStartHalfEdge;

        // Action
        PolyhedralBoundedSolidEulerOperators.lmev(solid, seed, seed, 2, new Vector3D(1.0, 0.0, 0.0));
        PolyhedralBoundedSolidEulerOperators.lmev(solid, solid.findVertex(2).emanatingHalfEdge,
            solid.findVertex(2).emanatingHalfEdge, 3,
            new Vector3D(0.0, 1.0, 0.0));
        PolyhedralBoundedSolidEulerOperators.lmef(solid, solid.findFace(1).findHalfEdge(3),
            solid.findFace(1).findHalfEdge(1), 2);

        // Assert
        assertThat(solid.getPolygonsList().size()).isEqualTo(2);
        assertThat(solid.getEdgesList().size()).isEqualTo(3);
        assertThat(solid.getVerticesList().size()).isEqualTo(3);
        assertThat(TopologicalSignature.from(solid).loopHalfEdgeCounts)
            .containsExactly(3, 3);
        assertThat(PolyhedralBoundedSolidValidationEngine
            .validateIntermediate(solid)).isTrue();
    }

    private static PolyhedralBoundedSolid createSkeletalSolid()
    {
        PolyhedralBoundedSolid solid = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolidEulerOperators.mvfs(solid, new Vector3D(0.0, 0.0, 0.0), 1, 1);
        return solid;
    }

    private static PolyhedralBoundedSolid createThreeEdgeWire()
    {
        PolyhedralBoundedSolid solid = createSkeletalSolid();
        _PolyhedralBoundedSolidHalfEdge he =
            solid.findFace(1).boundariesList.get(0).boundaryStartHalfEdge;

        PolyhedralBoundedSolidEulerOperators.lmev(solid, he, he, 2, new Vector3D(1.0, 0.0, 0.0));
        he = solid.findVertex(2).emanatingHalfEdge;
        PolyhedralBoundedSolidEulerOperators.lmev(solid, he, he, 3, new Vector3D(1.0, 1.0, 0.0));
        he = solid.findVertex(3).emanatingHalfEdge;
        PolyhedralBoundedSolidEulerOperators.lmev(solid, he, he, 4, new Vector3D(0.0, 1.0, 0.0));
        return solid;
    }

    private static PolyhedralBoundedSolid createPlanarFaceWithBridgeToHoleSeed()
    {
        PolyhedralBoundedSolid solid = createSkeletalSolid();
        PolyhedralBoundedSolidEulerOperators.lmev(solid, solid.findFace(1).boundariesList.get(0).boundaryStartHalfEdge,
            solid.findFace(1).boundariesList.get(0).boundaryStartHalfEdge,
            2, new Vector3D(1.0, 0.0, 0.0));
        PolyhedralBoundedSolidEulerOperators.lmev(solid, solid.findVertex(2).emanatingHalfEdge,
            solid.findVertex(2).emanatingHalfEdge, 3,
            new Vector3D(1.0, 1.0, 0.0));
        PolyhedralBoundedSolidEulerOperators.lmev(solid, solid.findVertex(3).emanatingHalfEdge,
            solid.findVertex(3).emanatingHalfEdge, 4,
            new Vector3D(0.0, 1.0, 0.0));
        PolyhedralBoundedSolidEulerOperators.lmef(solid, solid.findFace(1).findHalfEdge(4),
            solid.findFace(1).findHalfEdge(1), 2);
        PolyhedralBoundedSolidEulerOperators.lmev(solid, solid.findFace(1).findHalfEdge(1),
            solid.findFace(1).findHalfEdge(1), 5,
            new Vector3D(0.25, 0.25, 0.0));
        return solid;
    }

    private static _PolyhedralBoundedSolidHalfEdge[]
    firstDistinctHalfEdgesStartingAtSameVertex(PolyhedralBoundedSolid solid)
    {
        int i;
        int j;

        for ( i = 0; i < solid.getPolygonsList().size(); i++ ) {
            _PolyhedralBoundedSolidHalfEdge a =
                solid.getPolygonsList().get(i).boundariesList.get(0)
                    .boundaryStartHalfEdge;
            do {
                for ( j = i; j < solid.getPolygonsList().size(); j++ ) {
                    _PolyhedralBoundedSolidHalfEdge b =
                        solid.getPolygonsList().get(j).boundariesList.get(0)
                            .boundaryStartHalfEdge;
                    do {
                        if ( a != b && a.startingVertex == b.startingVertex ) {
                            return new _PolyhedralBoundedSolidHalfEdge[] { a, b  };
                        }
                        b = b.next();
                    } while ( b != solid.getPolygonsList().get(j).boundariesList.get(0)
                        .boundaryStartHalfEdge );
                }
                a = a.next();
            } while ( a != solid.getPolygonsList().get(i).boundariesList.get(0)
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

            faceCount = solid.getPolygonsList().size();
            edgeCount = solid.getEdgesList().size();
            vertexCount = solid.getVerticesList().size();
            loopsPerFace = new ArrayList<>();
            loopHalfEdgeCounts = new ArrayList<>();

            for ( i = 0; i < solid.getPolygonsList().size(); i++ ) {
                _PolyhedralBoundedSolidFace face = solid.getPolygonsList().get(i);
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
            if ( !(other instanceof TopologicalSignature topologicalSignature) ) {
                return false;
            }
            return faceCount == topologicalSignature.faceCount &&
                edgeCount == topologicalSignature.edgeCount &&
                vertexCount == topologicalSignature.vertexCount &&
                loopsPerFace.equals(topologicalSignature.loopsPerFace) &&
                loopHalfEdgeCounts.equals(topologicalSignature.loopHalfEdgeCounts);
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
