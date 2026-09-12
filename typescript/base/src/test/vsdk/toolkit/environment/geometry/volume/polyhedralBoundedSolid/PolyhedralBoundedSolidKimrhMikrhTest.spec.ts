import { describe, expect, it } from "vitest";

import { PolyhedralBoundedSolidEulerOperators } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
import type { _PolyhedralBoundedSolidFace } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidLoop } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";
import { PolyhedralBoundedSolidTestFixtures } from "./PolyhedralBoundedSolidTestFixtures.js";

/**
Covers same-shell face/loop movement operators that convert faces to
interior rings and back.

<p>Traceability: [MANT1988] Ch. 9.2.4 global manipulations and
Ch. 11.5 high-level Euler operator family, especially the KFMRH/MFKRH
same-shell case.</p>
 */
describe("PolyhedralBoundedSolidKimrhMikrhTest", () => {
    function faceContainsLoop(face: _PolyhedralBoundedSolidFace, loop: _PolyhedralBoundedSolidLoop): boolean {
        let i: number;
        for (i = 0; i < face.boundariesList.size(); i++) {
            if (face.boundariesList.get(i) === loop) {
                return true;
            }
        }
        return false;
    }

    it("given_boxSolid_when_lkimrh_then_faceIsRemovedAndItsLoopBecomesInnerLoop", () => {
        // Arrange
        const solid = PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        const face1 = solid.getPolygonsList().get(0)!;
        const face2 = solid.getPolygonsList().get(1)!;
        const migratedLoop = face2.boundariesList.get(0)!;
        const migratedHalfEdge = migratedLoop.boundaryStartHalfEdge!;
        const faceCountBefore = solid.getPolygonsList().size();
        const face1LoopsBefore = face1.boundariesList.size();
        const face2LoopsBefore = face2.boundariesList.size();
        const removedFaceId = face2.id;

        // Action
        PolyhedralBoundedSolidEulerOperators.lkimrh(solid, face1, face2);

        // Assert
        expect(solid.getPolygonsList().size()).toBe(faceCountBefore - 1);
        expect(solid.findFace(removedFaceId)).toBeNull();
        expect(face1.boundariesList.size()).toBe(face1LoopsBefore + face2LoopsBefore);
        expect(migratedHalfEdge.parentLoop.parentFace).toBe(face1);
        expect(migratedHalfEdge.parentLoop).not.toBe(migratedLoop);
        expect(faceContainsLoop(face1, migratedHalfEdge.parentLoop)).toBe(true);
    });

    it("given_faceWithInnerLoop_when_lmikrh_then_innerLoopBecomesNewFaceOuterLoop", () => {
        // Arrange
        const solid = PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        const face1 = solid.getPolygonsList().get(0)!;
        const face2 = solid.getPolygonsList().get(1)!;
        const loopToPromote = face2.boundariesList.get(0)!;

        PolyhedralBoundedSolidEulerOperators.lkimrh(solid, face1, face2);
        const facesAfterLkimrh = solid.getPolygonsList().size();
        const newFaceId = solid.getMaxFaceId() + 1;

        // Action
        const newFace = PolyhedralBoundedSolidEulerOperators.lmikrh(solid, loopToPromote, newFaceId);

        // Assert
        expect(newFace).not.toBeNull();
        expect(newFace!.id).toBe(newFaceId);
        expect(solid.getPolygonsList().size()).toBe(facesAfterLkimrh + 1);
        expect(loopToPromote.parentFace).toBe(newFace);
        expect(newFace!.boundariesList.size()).toBe(1);
        expect(newFace!.boundariesList.get(0)).toBe(loopToPromote);
        expect(faceContainsLoop(face1, loopToPromote)).toBe(false);
    });
});
