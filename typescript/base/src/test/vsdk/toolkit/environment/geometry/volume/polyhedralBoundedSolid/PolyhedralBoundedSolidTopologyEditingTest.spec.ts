import { describe, expect, it } from "vitest";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidEulerOperators as Euler } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
import { PolyhedralBoundedSolidTopologyEditing } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologyEditing.js";
describe("PolyhedralBoundedSolidTopologyEditingTest", () =>
    it("welds coincident vertices and compacts identifiers", () => {
        const s = new PolyhedralBoundedSolid();
        Euler.mvfs(s, new Vector3Dd(), 9, 8);
        const h = s.findFace(8)!.boundariesList[0]!.boundaryStartHalfEdge!;
        Euler.lmev(s, h, h, 20, new Vector3Dd());
        expect(PolyhedralBoundedSolidTopologyEditing.weldCoincidentVertices(s)).toBe(1);
        PolyhedralBoundedSolidTopologyEditing.compactIds(s);
        expect(s.getVerticesList()[0]!.id).toBe(1);
    }));
