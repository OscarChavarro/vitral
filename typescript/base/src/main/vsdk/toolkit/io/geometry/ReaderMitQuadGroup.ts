import { ArrayList } from "../../../../java/util/ArrayList.js";
import type { Vector3Dd } from "../../common/linealAlgebra/Vector3Dd.js";
import { Vertex } from "../../environment/geometry/element/Vertex.js";
import { QuadMesh } from "../../environment/geometry/surface/QuadMesh.js";
import { SimpleMaterial } from "../../environment/material/SimpleMaterial.js";
import { SimpleBody } from "../../environment/scene/SimpleBody.js";
import type { SimpleScene } from "../../environment/scene/SimpleScene.js";
import { _CScanf } from "./_CScanf.js";

/**
One MIT quads block: `v x y z` vertices and `q i0 i1 i2 i3` faces, built as a
QuadMesh; material changes flush faces while keeping vertices.
*/
export class ReaderMitQuadGroup {
    private readonly vertices = new ArrayList<Vector3Dd>();

    private readonly quadIndices = new ArrayList<number>();

    public constructor() {}

    public reset(): void {
        this.vertices.clear();
        this.quadIndices.clear();
    }

    public addVertex(vertex: Vector3Dd): boolean {
        return this.vertices.add(vertex);
    }

    /** Reads four vertex indices; false when malformed or out of range. */
    public addQuad(indices: string): boolean {
        const q: number[][] = [[0], [0], [0], [0]];
        const extra = [""];
        if (_CScanf.sscanf(indices, "%d %d %d %d %1s", q[0]!, q[1]!, q[2]!, q[3]!, extra) !== 4) return false;
        for (let i = 0; i < 4; ++i) if (q[i]![0]! < 0 || q[i]![0]! >= this.vertices.size()) return false;
        for (let i = 0; i < 4; ++i) if (!this.quadIndices.add(q[i]![0]!)) return false;
        return true;
    }

    /** Adds the pending faces as one body and returns it, or null when none are pending. */
    public flush(scene: SimpleScene, material: SimpleMaterial): SimpleBody | null {
        if (this.quadIndices.isEmpty()) return null;
        const meshVertices = new ArrayList<Vertex>();
        for (let i = 0; i < this.vertices.size(); ++i) meshVertices.add(new Vertex(this.vertices.get(i)));
        const mesh = new QuadMesh();
        mesh.setVertexes(meshVertices.toArray());
        const quadCount = Math.trunc(this.quadIndices.size() / 4);
        mesh.initQuadArrays(quadCount);
        const target = mesh.getQuadIndices()!;
        for (let i = 0; i < this.quadIndices.size(); ++i) target[i] = this.quadIndices.get(i);
        const body = new SimpleBody();
        body.setGeometry(mesh);
        body.setMaterial(new SimpleMaterial(material));
        scene.addBody(body);
        this.quadIndices.clear();
        return body;
    }
}
