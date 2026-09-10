import { describe, expect, it } from "vitest";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { _PolyhedralBoundedSolidVertex } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";
describe("_PolyhedralBoundedSolidVertexTest", () =>
    it("registers itself in its owner", () => {
        const vertices: _PolyhedralBoundedSolidVertex[] = [];
        const owner = { getVerticesList: () => vertices };
        const vertex = new _PolyhedralBoundedSolidVertex(owner, new Vector3Dd(1, 2, 3), 7);
        expect(vertices).toEqual([vertex]);
        expect(vertex.position).toEqual(new Vector3Dd(1, 2, 3));
    }));
