import { describe, expect, it } from "vitest";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { _PolyhedralBoundedSolidFace } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import { _PolyhedralBoundedSolidHalfEdge } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import { _PolyhedralBoundedSolidLoop } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";
import { _PolyhedralBoundedSolidVertex } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";
describe("_PolyhedralBoundedSolidLoopTest", () =>
    it("finds directed half-edges in its circular connectivity", () => {
        const s = new PolyhedralBoundedSolid(),
            f = new _PolyhedralBoundedSolidFace(s, 1),
            l = new _PolyhedralBoundedSolidLoop(f),
            v = [0, 1, 2].map((id) => new _PolyhedralBoundedSolidVertex(s, new Vector3Dd(id, 0, 0), id));
        for (const x of v) l.halfEdgesList.add(new _PolyhedralBoundedSolidHalfEdge(x, l));
        l.boundaryStartHalfEdge = l.halfEdgesList.get(0)!;
        expect(l.halfEdgeVertices(0, 1)).not.toBeNull();
        expect(l.firstHalfEdgeAtVertex(2)).not.toBeNull();
    }));
