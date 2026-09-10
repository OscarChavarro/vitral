import { describe, expect, it } from "vitest";
import { _PolyhedralBoundedSolidEdge } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.js";
describe("_PolyhedralBoundedSolidEdgeTest", () =>
    it("registers itself in its owner", () => {
        const edges: _PolyhedralBoundedSolidEdge[] = [];
        const edge = new _PolyhedralBoundedSolidEdge({ getEdgesList: () => edges });
        expect(edges).toEqual([edge]);
        expect(edge.getStartingVertexId()).toBe(-1);
    }));
