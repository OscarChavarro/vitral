import { describe, expect, it } from "vitest";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";
import { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { _PolyhedralBoundedSolidFace } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import { _PolyhedralBoundedSolidVertex } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";
describe("PolyhedralBoundedSolidTest", () => {
    it("owns and finds topology", () => {
        const s = new PolyhedralBoundedSolid(),
            v = new _PolyhedralBoundedSolidVertex(s, new Vector3Dd(1, 2, 3), 4),
            f = new _PolyhedralBoundedSolidFace(s, 5);
        expect(s.findVertex(4)).toBe(v);
        expect(s.findFace(5)).toBe(f);
        expect(s.getMinMax()).toEqual(new Float64Array([1, 2, 3, 1, 2, 3]));
    });
    it("tracks the visibility-query lifecycle", () => {
        const s = new Box(2, 2, 2).exportToPolyhedralBoundedSolid();
        s.beginVisibilityQueries();
        expect(s.visibilityQueriesActive()).toBe(true);
        s.endVisibilityQueries();
        expect(s.visibilityQueriesActive()).toBe(false);
    });
});
