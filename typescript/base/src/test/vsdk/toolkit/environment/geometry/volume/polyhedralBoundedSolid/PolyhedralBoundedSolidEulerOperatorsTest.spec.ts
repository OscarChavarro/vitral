import { describe, expect, it } from "vitest";

import { IllegalStateException } from "java/lang/IllegalStateException.js";
import { ArrayList } from "java/util/ArrayList.js";
import { Collections } from "java/util/Collections.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidEulerOperators } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import { PolyhedralBoundedSolidTestFixtures } from "./PolyhedralBoundedSolidTestFixtures.js";

/**
Direct regression suite for the local and same-shell Euler operators.

<p>Traceability: [MANT1988] Ch. 9.2 skeletal/local/global
manipulations; Ch. 11.3 low-level Euler operators, including Programs
11.5-11.8 and the inverse-operator exercises; and Ch. 11.5 high-level
wrappers over the low-level operators.</p>
 */
describe("PolyhedralBoundedSolidEulerOperatorsTest", () => {
    class TopologicalSignature {
        public readonly faceCount: number;
        public readonly edgeCount: number;
        public readonly vertexCount: number;
        public readonly loopsPerFace: ArrayList<number>;
        public readonly loopHalfEdgeCounts: ArrayList<number>;

        private constructor(solid: PolyhedralBoundedSolid) {
            let i: number;
            let j: number;

            this.faceCount = solid.getPolygonsList().size();
            this.edgeCount = solid.getEdgesList().size();
            this.vertexCount = solid.getVerticesList().size();
            this.loopsPerFace = new ArrayList<number>();
            this.loopHalfEdgeCounts = new ArrayList<number>();

            for (i = 0; i < solid.getPolygonsList().size(); i++) {
                const face = solid.getPolygonsList().get(i)!;
                this.loopsPerFace.add(face.boundariesList.size());
                for (j = 0; j < face.boundariesList.size(); j++) {
                    this.loopHalfEdgeCounts.add(face.boundariesList.get(j)!.halfEdgesList.size());
                }
            }
            Collections.sort(this.loopsPerFace);
            Collections.sort(this.loopHalfEdgeCounts);
        }

        public static from(solid: PolyhedralBoundedSolid): TopologicalSignature {
            return new TopologicalSignature(solid);
        }

        public equals(other: TopologicalSignature): boolean {
            if (this === other) {
                return true;
            }
            return (
                this.faceCount === other.faceCount &&
                this.edgeCount === other.edgeCount &&
                this.vertexCount === other.vertexCount &&
                JSON.stringify(this.loopsPerFace.toArray()) === JSON.stringify(other.loopsPerFace.toArray()) &&
                JSON.stringify(this.loopHalfEdgeCounts.toArray()) === JSON.stringify(other.loopHalfEdgeCounts.toArray())
            );
        }

        public toString(): string {
            return (
                "TopologicalSignature{" +
                "faces=" +
                this.faceCount +
                ", edges=" +
                this.edgeCount +
                ", vertices=" +
                this.vertexCount +
                ", loopsPerFace=" +
                this.loopsPerFace +
                ", loopHalfEdgeCounts=" +
                this.loopHalfEdgeCounts +
                "}"
            );
        }
    }

    function createSkeletalSolid(): PolyhedralBoundedSolid {
        const solid = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolidEulerOperators.mvfs(solid, new Vector3Dd(0.0, 0.0, 0.0), 1, 1);
        return solid;
    }

    function createThreeEdgeWire(): PolyhedralBoundedSolid {
        const solid = createSkeletalSolid();
        let he = solid.findFace(1)!.boundariesList.get(0)!.boundaryStartHalfEdge!;

        PolyhedralBoundedSolidEulerOperators.lmev(solid, he, he, 2, new Vector3Dd(1.0, 0.0, 0.0));
        he = solid.findVertex(2)!.emanatingHalfEdge!;
        PolyhedralBoundedSolidEulerOperators.lmev(solid, he, he, 3, new Vector3Dd(1.0, 1.0, 0.0));
        he = solid.findVertex(3)!.emanatingHalfEdge!;
        PolyhedralBoundedSolidEulerOperators.lmev(solid, he, he, 4, new Vector3Dd(0.0, 1.0, 0.0));
        return solid;
    }

    function createPlanarFaceWithBridgeToHoleSeed(): PolyhedralBoundedSolid {
        const solid = createSkeletalSolid();
        PolyhedralBoundedSolidEulerOperators.lmev(
            solid,
            solid.findFace(1)!.boundariesList.get(0)!.boundaryStartHalfEdge,
            solid.findFace(1)!.boundariesList.get(0)!.boundaryStartHalfEdge,
            2,
            new Vector3Dd(1.0, 0.0, 0.0),
        );
        PolyhedralBoundedSolidEulerOperators.lmev(
            solid,
            solid.findVertex(2)!.emanatingHalfEdge,
            solid.findVertex(2)!.emanatingHalfEdge,
            3,
            new Vector3Dd(1.0, 1.0, 0.0),
        );
        PolyhedralBoundedSolidEulerOperators.lmev(
            solid,
            solid.findVertex(3)!.emanatingHalfEdge,
            solid.findVertex(3)!.emanatingHalfEdge,
            4,
            new Vector3Dd(0.0, 1.0, 0.0),
        );
        PolyhedralBoundedSolidEulerOperators.lmef(
            solid,
            solid.findFace(1)!.findHalfEdge(4)!,
            solid.findFace(1)!.findHalfEdge(1)!,
            2,
        );
        PolyhedralBoundedSolidEulerOperators.lmev(
            solid,
            solid.findFace(1)!.findHalfEdge(1),
            solid.findFace(1)!.findHalfEdge(1),
            5,
            new Vector3Dd(0.25, 0.25, 0.0),
        );
        return solid;
    }

    function firstDistinctHalfEdgesStartingAtSameVertex(
        solid: PolyhedralBoundedSolid,
    ): _PolyhedralBoundedSolidHalfEdge[] {
        let i: number;
        let j: number;

        for (i = 0; i < solid.getPolygonsList().size(); i++) {
            let a = solid.getPolygonsList().get(i)!.boundariesList.get(0)!.boundaryStartHalfEdge!;
            do {
                for (j = i; j < solid.getPolygonsList().size(); j++) {
                    let b = solid.getPolygonsList().get(j)!.boundariesList.get(0)!.boundaryStartHalfEdge!;
                    do {
                        if (a !== b && a.startingVertex === b.startingVertex) {
                            return [a, b];
                        }
                        b = b.next()!;
                    } while (b !== solid.getPolygonsList().get(j)!.boundariesList.get(0)!.boundaryStartHalfEdge);
                }
                a = a.next()!;
            } while (a !== solid.getPolygonsList().get(i)!.boundariesList.get(0)!.boundaryStartHalfEdge);
        }
        throw new IllegalStateException("Expected at least one split vertex.");
    }

    it("given_emptySolid_when_mvfs_then_createsSkeletalHalfEdgeStructure", () => {
        // Arrange
        const solid = new PolyhedralBoundedSolid();

        // Action
        PolyhedralBoundedSolidEulerOperators.mvfs(solid, new Vector3Dd(1.0, 2.0, 3.0), 7, 11);

        // Assert
        expect(solid.getPolygonsList().size()).toBe(1);
        expect(solid.getVerticesList().size()).toBe(1);
        expect(solid.getEdgesList().size()).toBe(0);
        expect(solid.getMaxVertexId()).toBe(7);
        expect(solid.getMaxFaceId()).toBe(11);

        const face = solid.getPolygonsList().get(0)!;
        const loop = face.boundariesList.get(0)!;
        const halfEdge = loop.boundaryStartHalfEdge!;

        expect(face.parentSolid).toBe(solid);
        expect(loop.parentFace).toBe(face);
        expect(loop.halfEdgesList.size()).toBe(1);
        expect(halfEdge.parentEdge).toBeNull();
        expect(halfEdge.parentLoop).toBe(loop);
        expect(halfEdge.startingVertex).toBe(solid.getVerticesList().get(0));
        expect(halfEdge.next()).toBe(halfEdge);
        expect(halfEdge.previous()).toBe(halfEdge);
    });

    it("given_skeletalSolid_when_lmevReceivesSameHalfEdge_then_createsStrut", () => {
        // Arrange
        const solid = createSkeletalSolid();
        const seed = solid.findFace(1)!.boundariesList.get(0)!.boundaryStartHalfEdge!;

        // Action
        PolyhedralBoundedSolidEulerOperators.lmev(solid, seed, seed, 2, new Vector3Dd(1.0, 0.0, 0.0));

        // Assert
        expect(solid.getPolygonsList().size()).toBe(1);
        expect(solid.getEdgesList().size()).toBe(1);
        expect(solid.getVerticesList().size()).toBe(2);
        expect(solid.findFace(1)!.boundariesList.get(0)!.halfEdgesList.size()).toBe(2);

        const edge = solid.getEdgesList().get(0)!;
        expect(edge.leftHalf).not.toBeNull();
        expect(edge.rightHalf).not.toBeNull();
        expect(edge.leftHalf!.parentLoop).toBe(edge.rightHalf!.parentLoop);
        expect(edge.leftHalf!.mirrorHalfEdge()).toBe(edge.rightHalf);
        expect(edge.rightHalf!.mirrorHalfEdge()).toBe(edge.leftHalf);
        expect(edge.rightHalf!.startingVertex.id).toBe(2);
        expect(edge.leftHalf!.startingVertex.id).toBe(1);
    });

    it("given_strut_when_lkevKillsItsEdge_then_returnsToSkeletalState", () => {
        // Arrange
        const solid = createSkeletalSolid();
        const seed = solid.findFace(1)!.boundariesList.get(0)!.boundaryStartHalfEdge!;
        PolyhedralBoundedSolidEulerOperators.lmev(solid, seed, seed, 2, new Vector3Dd(1.0, 0.0, 0.0));
        const edge = solid.getEdgesList().get(0)!;

        // Action
        PolyhedralBoundedSolidEulerOperators.lkev(solid, edge.rightHalf!, edge.leftHalf!);

        // Assert
        expect(solid.getEdgesList().size()).toBe(0);
        expect(solid.getVerticesList().size()).toBe(1);
        expect(solid.getPolygonsList().size()).toBe(1);
        expect(solid.findFace(1)!.boundariesList.get(0)!.halfEdgesList.size()).toBe(1);
        expect(solid.findFace(1)!.boundariesList.get(0)!.boundaryStartHalfEdge!.parentEdge).toBeNull();
    });

    it("given_boxVertexNeighborhood_when_lmevSplitsVertex_then_addsOneEdgeAndOneVertex", () => {
        // Arrange
        const solid = PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        const pair = firstDistinctHalfEdgesStartingAtSameVertex(solid);
        const faceCount = solid.getPolygonsList().size();
        const edgeCount = solid.getEdgesList().size();
        const vertexCount = solid.getVerticesList().size();
        const newVertexId = solid.getMaxVertexId() + 1;

        // Action
        PolyhedralBoundedSolidEulerOperators.lmev(
            solid,
            pair[0]!,
            pair[1]!,
            newVertexId,
            pair[0]!.startingVertex.position,
        );

        // Assert
        expect(solid.getPolygonsList().size()).toBe(faceCount);
        expect(solid.getEdgesList().size()).toBe(edgeCount + 1);
        expect(solid.getVerticesList().size()).toBe(vertexCount + 1);
        expect(solid.findVertex(newVertexId)).not.toBeNull();
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid)).toBe(true);
    });

    it("given_openWire_when_lmefClosesLoop_then_addsOneEdgeAndOneFace", () => {
        // Arrange
        const solid = createThreeEdgeWire();
        const face = solid.findFace(1)!;
        const he1 = face.findHalfEdge(4)!;
        const he2 = face.findHalfEdge(1)!;
        const edgeCount = solid.getEdgesList().size();
        const vertexCount = solid.getVerticesList().size();

        // Action
        const newFace = PolyhedralBoundedSolidEulerOperators.lmef(solid, he1, he2, 2);

        // Assert
        expect(newFace).not.toBeNull();
        expect(solid.getPolygonsList().size()).toBe(2);
        expect(solid.getEdgesList().size()).toBe(edgeCount + 1);
        expect(solid.getVerticesList().size()).toBe(vertexCount);
        expect(newFace!.boundariesList.size()).toBe(1);
        expect(newFace!.boundariesList.get(0)!.halfEdgesList.size()).toBeGreaterThanOrEqual(1);
    });

    it("given_lmefResult_when_lkefKillsCreatedEdge_then_mergesFaces", () => {
        // Arrange
        const solid = createThreeEdgeWire();
        const face = solid.findFace(1)!;
        const newFace = PolyhedralBoundedSolidEulerOperators.lmef(
            solid,
            face.findHalfEdge(4)!,
            face.findHalfEdge(1)!,
            2,
        )!;
        const edgeToKill = newFace.boundariesList.get(0)!.boundaryStartHalfEdge!.parentEdge!;
        const edgeCount = solid.getEdgesList().size();

        // Action
        PolyhedralBoundedSolidEulerOperators.lkef(solid, edgeToKill.rightHalf!, edgeToKill.leftHalf!);

        // Assert
        expect(solid.getPolygonsList().size()).toBe(1);
        expect(solid.getEdgesList().size()).toBe(edgeCount - 1);
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid)).toBe(true);
    });

    it("given_faceWithBridgeEdge_when_lkemrKillsEdge_then_createsRing", () => {
        // Arrange
        const solid = createPlanarFaceWithBridgeToHoleSeed();
        const face = solid.findFace(1)!;
        const he1 = face.findHalfEdge(1, 5)!;
        const he2 = face.findHalfEdge(5, 1)!;
        const edgeCount = solid.getEdgesList().size();
        const loopCount = face.boundariesList.size();

        // Action
        PolyhedralBoundedSolidEulerOperators.lkemr(solid, he1, he2);

        // Assert
        expect(solid.getEdgesList().size()).toBe(edgeCount - 1);
        expect(face.boundariesList.size()).toBe(loopCount + 1);
    });

    it("given_lkemrResult_when_lmekrConnectsLoops_then_restoresSingleLoop", () => {
        // Arrange
        const solid = createPlanarFaceWithBridgeToHoleSeed();
        const face = solid.findFace(1)!;
        const he1 = face.findHalfEdge(1, 5)!;
        const he2 = face.findHalfEdge(5, 1)!;
        PolyhedralBoundedSolidEulerOperators.lkemr(solid, he1, he2);
        const edgeCount = solid.getEdgesList().size();
        const outer = face.boundariesList.get(0)!;
        const ring = face.boundariesList.get(1)!;

        // Action
        PolyhedralBoundedSolidEulerOperators.lmekr(solid, outer.boundaryStartHalfEdge!, ring.boundaryStartHalfEdge!);

        // Assert
        expect(solid.getEdgesList().size()).toBe(edgeCount + 1);
        expect(face.boundariesList.size()).toBe(1);
    });

    it("given_faceWithRing_when_lringmvReordersOuterLoop_then_keepsSameFace", () => {
        // Arrange
        const solid = createPlanarFaceWithBridgeToHoleSeed();
        const face = solid.findFace(1)!;
        PolyhedralBoundedSolidEulerOperators.lkemr(solid, face.findHalfEdge(1, 5)!, face.findHalfEdge(5, 1)!);
        const ring = face.boundariesList.get(1)!;

        // Action
        const movedToOuter = PolyhedralBoundedSolidEulerOperators.lringmv(solid, ring, face, true);
        const ringBecameOuter = face.boundariesList.get(0) === ring;
        const movedBackToInner = PolyhedralBoundedSolidEulerOperators.lringmv(solid, ring, face, false);

        // Assert
        expect(movedToOuter).toBe(true);
        expect(ringBecameOuter).toBe(true);
        expect(movedBackToInner).toBe(true);
        expect(face.boundariesList.get(0)).not.toBe(ring);
        expect(ring.parentFace).toBe(face);
    });

    it("given_faceWithRing_when_lringmvMovesLoopToAnotherFace_then_updatesOwnership", () => {
        // Arrange
        const solid = createPlanarFaceWithBridgeToHoleSeed();
        const sourceFace = solid.findFace(1)!;
        const targetFace = solid.findFace(2)!;
        PolyhedralBoundedSolidEulerOperators.lkemr(
            solid,
            sourceFace.findHalfEdge(1, 5)!,
            sourceFace.findHalfEdge(5, 1)!,
        );
        const ring = sourceFace.boundariesList.get(1)!;
        const sourceLoopCount = sourceFace.boundariesList.size();
        const targetLoopCount = targetFace.boundariesList.size();

        // Action
        const result = PolyhedralBoundedSolidEulerOperators.lringmv(solid, ring, targetFace, false);

        // Assert
        expect(result).toBe(true);
        expect(sourceFace.boundariesList.size()).toBe(sourceLoopCount - 1);
        expect(targetFace.boundariesList.size()).toBe(targetLoopCount + 1);
        expect(ring.parentFace).toBe(targetFace);
        expect(targetFace.boundariesList.get(0)).not.toBe(ring);
    });

    it("given_skeletalSolid_when_kvfs_then_clearsSolid", () => {
        // Arrange
        const solid = createSkeletalSolid();

        // Action
        PolyhedralBoundedSolidEulerOperators.kvfs(solid);

        // Assert
        expect(solid.getPolygonsList().size()).toBe(0);
        expect(solid.getEdgesList().size()).toBe(0);
        expect(solid.getVerticesList().size()).toBe(0);
    });

    it("given_skeletalSolid_when_smev_then_delegatesToRobustLmev", () => {
        // Arrange
        const solid = createSkeletalSolid();

        // Action
        const result = PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 1, 2, new Vector3Dd(1.0, 0.0, 0.0));

        // Assert
        expect(result).toBe(true);
        expect(solid.getPolygonsList().size()).toBe(1);
        expect(solid.getEdgesList().size()).toBe(1);
        expect(solid.getVerticesList().size()).toBe(2);
    });

    it("given_boxVertexNeighborhood_when_mev_then_splitsVertex", () => {
        // Arrange
        const solid = PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        const pair = firstDistinctHalfEdgesStartingAtSameVertex(solid);
        const vertexCount = solid.getVerticesList().size();
        const edgeCount = solid.getEdgesList().size();
        const newVertexId = solid.getMaxVertexId() + 1;

        // Action
        const result = PolyhedralBoundedSolidEulerOperators.mev(
            solid,
            pair[0]!.parentLoop.parentFace.id,
            pair[1]!.parentLoop.parentFace.id,
            pair[0]!.startingVertex.id,
            pair[0]!.next()!.startingVertex.id,
            pair[1]!.next()!.startingVertex.id,
            newVertexId,
            pair[0]!.startingVertex.position,
        );

        // Assert
        expect(result).toBe(true);
        expect(solid.getVerticesList().size()).toBe(vertexCount + 1);
        expect(solid.getEdgesList().size()).toBe(edgeCount + 1);
        expect(solid.findVertex(newVertexId)).not.toBeNull();
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid)).toBe(true);
    });

    it("given_openWire_when_smef_then_closesLoop", () => {
        // Arrange
        const solid = createThreeEdgeWire();

        // Action
        const result = PolyhedralBoundedSolidEulerOperators.smef(solid, 1, 4, 1, 2);

        // Assert
        expect(result).toBe(true);
        expect(solid.getPolygonsList().size()).toBe(2);
        expect(solid.getEdgesList().size()).toBe(4);
    });

    it("given_openWire_when_mef_then_closesLoop", () => {
        // Arrange
        const solid = createThreeEdgeWire();
        const face = solid.findFace(1)!;
        const he1 = face.findHalfEdge(4)!;
        const he2 = face.findHalfEdge(1)!;

        // Action
        const result = PolyhedralBoundedSolidEulerOperators.mef(
            solid,
            1,
            1,
            he1.startingVertex.id,
            he1.next()!.startingVertex.id,
            he2.startingVertex.id,
            he2.next()!.startingVertex.id,
            2,
        );

        // Assert
        expect(result).toBe(true);
        expect(solid.getPolygonsList().size()).toBe(2);
        expect(solid.getEdgesList().size()).toBe(4);
    });

    it("given_faceWithBridgeEdge_when_kemr_then_createsRing", () => {
        // Arrange
        const solid = createPlanarFaceWithBridgeToHoleSeed();
        const face = solid.findFace(1)!;
        const loopCount = face.boundariesList.size();

        // Action
        const result = PolyhedralBoundedSolidEulerOperators.kemr(solid, 1, 1, 1, 5, 5, 1);

        // Assert
        expect(result).toBe(true);
        expect(face.boundariesList.size()).toBe(loopCount + 1);
    });

    it("given_twoFaces_when_kfmrh_then_secondFaceBecomesInnerLoop", () => {
        // Arrange
        const solid = PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        const face1 = solid.getPolygonsList().get(0)!;
        const face2 = solid.getPolygonsList().get(1)!;
        const faceCount = solid.getPolygonsList().size();
        const loopCount = face1.boundariesList.size();

        // Action
        const result = PolyhedralBoundedSolidEulerOperators.kfmrh(solid, face1.id, face2.id);

        // Assert
        expect(result).toBe(true);
        expect(solid.getPolygonsList().size()).toBe(faceCount - 1);
        expect(face1.boundariesList.size()).toBe(loopCount + face2.boundariesList.size());
        expect(solid.findFace(face2.id)).toBeNull();
    });

    it("given_boxNeighborhood_when_lmevThenLkev_then_restoresTopologicalIdentity", () => {
        // Arrange
        const solid = PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        const before = TopologicalSignature.from(solid);
        const pair = firstDistinctHalfEdgesStartingAtSameVertex(solid);
        const newVertexId = solid.getMaxVertexId() + 1;

        // Action
        PolyhedralBoundedSolidEulerOperators.lmev(
            solid,
            pair[0]!,
            pair[1]!,
            newVertexId,
            pair[0]!.startingVertex.position,
        );
        const newVertexHalfEdge = solid.findVertex(newVertexId)!.emanatingHalfEdge!;
        PolyhedralBoundedSolidEulerOperators.lkev(solid, newVertexHalfEdge, newVertexHalfEdge.mirrorHalfEdge()!);

        // Assert
        expect(TopologicalSignature.from(solid).equals(before)).toBe(true);
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid)).toBe(true);
    });

    it("given_openWire_when_lmefThenLkef_then_restoresTopologicalIdentity", () => {
        // Arrange
        const solid = createThreeEdgeWire();
        const before = TopologicalSignature.from(solid);
        const face = solid.findFace(1)!;
        const newFace = PolyhedralBoundedSolidEulerOperators.lmef(
            solid,
            face.findHalfEdge(4)!,
            face.findHalfEdge(1)!,
            2,
        )!;
        const edgeToKill = newFace.boundariesList.get(0)!.boundaryStartHalfEdge!.parentEdge!;

        // Action
        PolyhedralBoundedSolidEulerOperators.lkef(solid, edgeToKill.rightHalf!, edgeToKill.leftHalf!);

        // Assert
        expect(TopologicalSignature.from(solid).equals(before)).toBe(true);
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid)).toBe(true);
    });

    it("given_faceWithBridge_when_lkemrThenLmekr_then_restoresTopologicalIdentity", () => {
        // Arrange
        const solid = createPlanarFaceWithBridgeToHoleSeed();
        const before = TopologicalSignature.from(solid);
        const face = solid.findFace(1)!;
        PolyhedralBoundedSolidEulerOperators.lkemr(solid, face.findHalfEdge(1, 5)!, face.findHalfEdge(5, 1)!);
        const outer = face.boundariesList.get(0)!;
        const ring = face.boundariesList.get(1)!;

        // Action
        PolyhedralBoundedSolidEulerOperators.lmekr(solid, outer.boundaryStartHalfEdge!, ring.boundaryStartHalfEdge!);

        // Assert
        expect(TopologicalSignature.from(solid).equals(before)).toBe(true);
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid)).toBe(true);
    });

    it("given_twoFaces_when_lkfmrhThenLmfkrh_then_restoresTopologicalIdentity", () => {
        // Arrange
        const solid = PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        const before = TopologicalSignature.from(solid);
        const face1 = solid.getPolygonsList().get(0)!;
        const face2 = solid.getPolygonsList().get(1)!;
        const newFaceId = solid.getMaxFaceId() + 1;

        // Action
        PolyhedralBoundedSolidEulerOperators.lkfmrh(solid, face1, face2);
        const loopToPromote = face1.boundariesList.get(face1.boundariesList.size() - 1)!;
        PolyhedralBoundedSolidEulerOperators.lmfkrh(solid, loopToPromote, newFaceId);

        // Assert
        expect(TopologicalSignature.from(solid).equals(before)).toBe(true);
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid)).toBe(true);
    });

    it("given_skeletalSolid_when_twoStrutsThenLmef_then_createsTriangularLaminaTopology", () => {
        // Arrange
        const solid = createSkeletalSolid();
        const seed = solid.findFace(1)!.boundariesList.get(0)!.boundaryStartHalfEdge!;

        // Action
        PolyhedralBoundedSolidEulerOperators.lmev(solid, seed, seed, 2, new Vector3Dd(1.0, 0.0, 0.0));
        PolyhedralBoundedSolidEulerOperators.lmev(
            solid,
            solid.findVertex(2)!.emanatingHalfEdge,
            solid.findVertex(2)!.emanatingHalfEdge,
            3,
            new Vector3Dd(0.0, 1.0, 0.0),
        );
        PolyhedralBoundedSolidEulerOperators.lmef(
            solid,
            solid.findFace(1)!.findHalfEdge(3)!,
            solid.findFace(1)!.findHalfEdge(1)!,
            2,
        );

        // Assert
        expect(solid.getPolygonsList().size()).toBe(2);
        expect(solid.getEdgesList().size()).toBe(3);
        expect(solid.getVerticesList().size()).toBe(3);
        expect(TopologicalSignature.from(solid).loopHalfEdgeCounts.toArray()).toEqual([3, 3]);
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid)).toBe(true);
    });
});
