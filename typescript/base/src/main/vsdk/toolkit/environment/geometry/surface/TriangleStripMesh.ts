import { Surface } from "./Surface.js";
import { Vertex } from "../element/Vertex.js";
import { Ray } from "../element/Ray.js";
import { RayHit } from "../element/RayHit.js";
import { Triangle } from "../element/Triangle.js";
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
                    out.p = hit.getPoint();
                    out.n = hit.getNormal();
                }
            }
        if (best === null) return false;
        out.setRay(best);
        return true;
    }
    public override doExtraInformation(_ray: Ray, _t: number, _out: RayHit): void {}
}
