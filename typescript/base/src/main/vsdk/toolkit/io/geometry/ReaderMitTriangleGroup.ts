import { ArrayList } from "../../../../java/util/ArrayList.js";
import type { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { Triangle } from "../../environment/geometry/element/Triangle.js";
import { TriangleMesh } from "../../environment/geometry/surface/TriangleMesh.js";
import { SimpleMaterial } from "../../environment/material/SimpleMaterial.js";
import { SimpleBody } from "../../environment/scene/SimpleBody.js";
import type { SimpleScene } from "../../environment/scene/SimpleScene.js";
import { _CScanf } from "./_CScanf.js";

/**
One MIT triangles block; material changes flush faces while keeping vertices.
*/
export class ReaderMitTriangleGroup {
    private readonly vertices = new ArrayList<Vector3Dd>();

    private readonly triangles = new ArrayList<Triangle>();

    public constructor() {}

    public reset(): void {
        this.vertices.clear();
        this.triangles.clear();
    }

    public addVertex(vertex: Vector3Dd): boolean {
        return this.vertices.add(vertex);
    }

    private static isStrtolSpace(c: string): boolean {
        return c === " " || c === "\t" || c === "\n" || c === "\u000b" || c === "\f" || c === "\r";
    }

    public addFace(coordinates: string): boolean {
        const indices = new ArrayList<number>();
        let cursor = 0;
        const n = coordinates.length;
        while (cursor < n) {
            while (
                cursor < n &&
                (coordinates.charAt(cursor) === " " ||
                    coordinates.charAt(cursor) === "\t" ||
                    coordinates.charAt(cursor) === "\r" ||
                    coordinates.charAt(cursor) === "\n")
            )
                ++cursor;
            if (cursor >= n) break;
            // strtol: leading white space, optional sign and decimal digits.
            let start = cursor;
            while (start < n && ReaderMitTriangleGroup.isStrtolSpace(coordinates.charAt(start))) ++start;
            const value = [0];
            const length = _CScanf.integerPrefix(coordinates, start, value);
            if (length === 0 || value[0]! < 0 || value[0]! >= this.vertices.size()) return false;
            if (!indices.add(value[0]!)) return false;
            cursor = start + length;
        }
        if (indices.size() < 3) return false;
        for (let i = 2; i < indices.size(); ++i) {
            const a = indices.get(0);
            const b = indices.get(i - 1);
            const c = indices.get(i);
            if (a === b || b === c || a === c) continue;
            if (!this.triangles.add(new Triangle(a, b, c))) return false;
        }
        return true;
    }

    /** Adds the pending faces as one body and returns it, or null when none are pending. */
    public flush(scene: SimpleScene, material: SimpleMaterial): SimpleBody | null {
        if (this.triangles.isEmpty()) return null;
        const mesh = new TriangleMesh();
        mesh.initVertexPositionsArray(this.vertices.size());
        const positions = mesh.getVertexPositions()!;
        for (let i = 0; i < this.vertices.size(); ++i) {
            positions[i * 3] = this.vertices.get(i).x();
            positions[i * 3 + 1] = this.vertices.get(i).y();
            positions[i * 3 + 2] = this.vertices.get(i).z();
        }
        mesh.setTriangles(this.triangles.toArray());
        // Prepare shared read-only normals before parallel ray tracing starts.
        mesh.calculateNormals();
        const body = new SimpleBody();
        body.setGeometry(mesh);
        body.setMaterial(new SimpleMaterial(material));
        scene.addBody(body);
        this.triangles.clear();
        return body;
    }
}
