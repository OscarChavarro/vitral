import { Surface } from "./Surface.js";
import { Vertex } from "../element/Vertex.js";
import { Ray } from "../element/Ray.js";
import { RayHit } from "../element/RayHit.js";
import { Triangle } from "../element/Triangle.js";
import { TriangleMesh } from "./TriangleMesh.js";
export class TriangleStripMesh extends Surface<Ray, RayHit> {
    private name = "default";
    private vertexes: Vertex[] = [];
    private strips: number[][] = [];
    public getMinMax(): Float64Array {
        const m = new Float64Array([Infinity, Infinity, Infinity, -Infinity, -Infinity, -Infinity]);
        for (const v of this.vertexes) {
            const p = v.getPosition();
            m[0] = Math.min(m[0]!, p.x());
            m[1] = Math.min(m[1]!, p.y());
            m[2] = Math.min(m[2]!, p.z());
            m[3] = Math.max(m[3]!, p.x());
            m[4] = Math.max(m[4]!, p.y());
            m[5] = Math.max(m[5]!, p.z());
        }
        return m;
    }
    public getVertexes() {
        return this.vertexes;
    }
    public getVertexAt(i: number) {
        return this.vertexes[i]!;
    }
    public setVertexes(x: Vertex[]) {
        this.vertexes = x;
    }
    public setStrips(x: number[][]) {
        this.strips = x;
    }
    public getStrips() {
        return this.strips;
    }
    /**
    The triangles of the strips, as a triangle mesh sharing the vertex
    positions: in each strip, every vertex after the first two closes a
    triangle with the two before it, alternating their order so all the
    triangles keep the orientation of the first one. Triangles with a
    repeated vertex (used to join strips) are skipped.
    @return a new mesh with positions and triangles only, or null if there
    are no vertexes or strips
    */
    public exportToTriangleMesh(): TriangleMesh | null {
        const vertexes: Vertex[] | null = this.vertexes;
        const strips: (number[] | null)[] | null = this.strips;
        if (vertexes === null || strips === null) {
            return null;
        }
        const mesh = new TriangleMesh();
        let count = 0;
        let i: number;
        let j: number;

        for (i = 0; i < strips.length; i++) {
            if (strips[i] !== null && strips[i]!.length > 2) {
                count += strips[i]!.length - 2;
            }
        }
        mesh.initVertexPositionsArray(vertexes.length);
        const positions = mesh.getVertexPositions()!;
        for (i = 0; i < vertexes.length; i++) {
            positions[3 * i] = vertexes[i]!.getPosition().x();
            positions[3 * i + 1] = vertexes[i]!.getPosition().y();
            positions[3 * i + 2] = vertexes[i]!.getPosition().z();
        }
        const all = new Int32Array(3 * count);
        count = 0;
        for (i = 0; i < strips.length; i++) {
            const strip: number[] | null = strips[i] ?? null;
            for (j = 2; strip !== null && j < strip.length; j++) {
                const p0 = j % 2 === 0 ? strip[j - 2]! : strip[j - 1]!;
                const p1 = j % 2 === 0 ? strip[j - 1]! : strip[j - 2]!;
                const p2 = strip[j]!;
                if (p0 === p1 || p1 === p2 || p0 === p2) {
                    continue;
                }
                all[3 * count] = p0;
                all[3 * count + 1] = p1;
                all[3 * count + 2] = p2;
                count++;
            }
        }
        mesh.initTriangleArrays(count);
        const indexes = mesh.getTriangleIndexes()!;
        indexes.set(all.subarray(0, 3 * count), 0);
        return mesh;
    }

    public doIntersectionFirstHit(ray: Ray, out: RayHit): boolean {
        let best: Ray | null = null;
        for (const strip of this.strips)
            for (let i = 2; i < strip.length; i++) {
                const a = this.vertexes[strip[i - 2]!]?.position,
                    b = this.vertexes[strip[i - 1]!]?.position,
                    c = this.vertexes[strip[i]!]?.position;
                if (a === undefined || b === undefined || c === undefined) continue;
                const hit = Triangle.doIntersectionWithTriangle(ray, a, b, c);
                if (hit !== null && (best === null || hit.getT() < best.getT())) {
                    best = ray.withT(hit.getT());
                    out.point = hit.getPoint();
                    out.normal = hit.getNormal();
                }
            }
        if (best === null) return false;
        out.setRay(best);
        return true;
    }
    public override doExtraInformation(_ray: Ray, _t: number, _out: RayHit): void {}
}
