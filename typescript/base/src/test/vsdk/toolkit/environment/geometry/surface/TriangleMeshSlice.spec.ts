import { describe, expect, it } from "vitest";
import { InfinitePlane } from "../../../../../../main/vsdk/toolkit/environment/geometry/surface/InfinitePlane.js";
import { TriangleMesh } from "../../../../../../main/vsdk/toolkit/environment/geometry/surface/TriangleMesh.js";
import { Vertex } from "../../../../../../main/vsdk/toolkit/environment/geometry/element/Vertex.js";
import { Triangle } from "../../../../../../main/vsdk/toolkit/environment/geometry/element/Triangle.js";
import { Vector3Dd } from "../../../../../../main/vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";

describe("TriangleMesh.slice", () => {
    it("keeps the Java one-inside/two-outside cut ordering", () => {
        const mesh = new TriangleMesh();
        mesh.setVertexes([new Vertex(-1, 0, 0), new Vertex(1, 0, 0), new Vertex(1, 1, 0)], false);
        mesh.setTriangles([new Triangle(0, 1, 2)]);
        mesh.slice(new InfinitePlane(new Vector3Dd(1, 0, 0), new Vector3Dd(0, 0, 0)));
        expect(mesh.getNumTriangles()).toBe(1);
        expect(mesh.getNumVertices()).toBe(3);
        expect(mesh.position(0).x()).toBeCloseTo(-1);
        expect(mesh.position(1).x()).toBeCloseTo(0);
        expect(mesh.position(2).x()).toBeCloseTo(0);
    });
});
