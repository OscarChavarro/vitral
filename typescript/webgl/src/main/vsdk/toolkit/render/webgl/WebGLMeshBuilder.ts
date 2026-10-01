import { Vector3Dd } from "@vitral/base";
import { WebGLMesh } from "./WebGLMeshRenderer.js";

/**
Port of `vsdk.toolkit.render.jogl.Jogl4MeshBuilder`.

Accumulates the triangles of a tessellated surface and builds a `WebGLMesh`
from them. Tangents and binormals are derived from the normal of each vertex.
Each vertex is kept as the 14 floats Java keeps in a `float[]` (position,
normal, texture coordinates, tangent and binormal).
*/
export class WebGLMeshBuilder {
    private readonly vertices: Float32Array[] = [];
    private readonly characteristicSize: number;
    private doubleSided = false;

    public constructor(characteristicSize: number) {
        this.characteristicSize = characteristicSize;
    }

    /**
    Adds one triangle; vertices are given counterclockwise seen from the side
    the normals point to.
    */
    public addTriangle(
        p0: Vector3Dd, n0: Vector3Dd, u0: number, v0: number,
        p1: Vector3Dd, n1: Vector3Dd, u1: number, v1: number,
        p2: Vector3Dd, n2: Vector3Dd, u2: number, v2: number,
    ): void {
        this.addVertex(p0, n0, u0, v0);
        this.addVertex(p1, n1, u1, v1);
        this.addVertex(p2, n2, u2, v2);
    }

    /**
    Adds a quad as two triangles; vertices are given counterclockwise seen from
    the side the normals point to.
    */
    public addQuad(
        p0: Vector3Dd, n0: Vector3Dd, u0: number, v0: number,
        p1: Vector3Dd, n1: Vector3Dd, u1: number, v1: number,
        p2: Vector3Dd, n2: Vector3Dd, u2: number, v2: number,
        p3: Vector3Dd, n3: Vector3Dd, u3: number, v3: number,
    ): void {
        this.addTriangle(p0, n0, u0, v0, p1, n1, u1, v1, p2, n2, u2, v2);
        this.addTriangle(p0, n0, u0, v0, p2, n2, u2, v2, p3, n3, u3, v3);
    }

    /**
    Adds the side of a frustum of cone around the Z axis.

    @param z0 height of the first ring
    @param r0 radius of the first ring
    @param z1 height of the second ring
    @param r1 radius of the second ring
    @param slices number of divisions around the axis
    */
    public addFrustum(z0: number, r0: number, z1: number, r1: number, slices: number): void {
        const dz: number = z1 - z0;
        const dr: number = r0 - r1;
        const normalLength: number = Math.sqrt(dz * dz + dr * dr);

        if (normalLength < 1e-12) {
            return;
        }
        const nz: number = dr / normalLength;
        const nr: number = dz / normalLength;

        for (let i = 0; i < slices; i++) {
            const a0: number = (2 * Math.PI * i) / slices;
            const a1: number = (2 * Math.PI * (i + 1)) / slices;
            const c0: number = Math.cos(a0);
            const s0: number = Math.sin(a0);
            const c1: number = Math.cos(a1);
            const s1: number = Math.sin(a1);
            const n0 = new Vector3Dd(nr * c0, nr * s0, nz);
            const n1 = new Vector3Dd(nr * c1, nr * s1, nz);
            const u0: number = i / slices;
            const u1: number = (i + 1) / slices;

            if (r1 < 1e-12) {
                const apex = new Vector3Dd(0, 0, z1);
                const nApex = new Vector3Dd(nr * Math.cos((a0 + a1) / 2), nr * Math.sin((a0 + a1) / 2), nz);
                this.addTriangle(
                    new Vector3Dd(r0 * c0, r0 * s0, z0), n0, u0, 0,
                    new Vector3Dd(r0 * c1, r0 * s1, z0), n1, u1, 0,
                    apex, nApex, (u0 + u1) / 2, 1,
                );
            } else {
                this.addQuad(
                    new Vector3Dd(r0 * c0, r0 * s0, z0), n0, u0, 0,
                    new Vector3Dd(r0 * c1, r0 * s1, z0), n1, u1, 0,
                    new Vector3Dd(r1 * c1, r1 * s1, z1), n1, u1, 1,
                    new Vector3Dd(r1 * c0, r1 * s0, z1), n0, u0, 1,
                );
            }
        }
    }

    /**
    Adds a flat ring (or disk, when the inner radius is zero) perpendicular
    to the Z axis.

    @param z height of the ring
    @param innerRadius inner radius
    @param outerRadius outer radius
    @param facingUp true if the normal is +Z, false for -Z
    @param slices number of divisions around the axis
    */
    public addDisk(z: number, innerRadius: number, outerRadius: number, facingUp: boolean, slices: number): void {
        const n = new Vector3Dd(0, 0, facingUp ? 1 : -1);

        for (let i = 0; i < slices; i++) {
            const a0: number = (2 * Math.PI * i) / slices;
            const a1: number = (2 * Math.PI * (i + 1)) / slices;
            const c0: number = Math.cos(a0);
            const s0: number = Math.sin(a0);
            const c1: number = Math.cos(a1);
            const s1: number = Math.sin(a1);
            const o0 = new Vector3Dd(outerRadius * c0, outerRadius * s0, z);
            const o1 = new Vector3Dd(outerRadius * c1, outerRadius * s1, z);
            const uo0: number = 0.5 + 0.5 * c0;
            const vo0: number = 0.5 + 0.5 * s0;
            const uo1: number = 0.5 + 0.5 * c1;
            const vo1: number = 0.5 + 0.5 * s1;

            if (innerRadius < 1e-12) {
                const center = new Vector3Dd(0, 0, z);
                if (facingUp) {
                    this.addTriangle(center, n, 0.5, 0.5, o0, n, uo0, vo0, o1, n, uo1, vo1);
                } else {
                    this.addTriangle(center, n, 0.5, 0.5, o1, n, uo1, vo1, o0, n, uo0, vo0);
                }
            } else {
                const i0 = new Vector3Dd(innerRadius * c0, innerRadius * s0, z);
                const i1 = new Vector3Dd(innerRadius * c1, innerRadius * s1, z);
                if (facingUp) {
                    this.addQuad(i0, n, 0.5, 0.5, o0, n, uo0, vo0, o1, n, uo1, vo1, i1, n, 0.5, 0.5);
                } else {
                    this.addQuad(i0, n, 0.5, 0.5, i1, n, 0.5, 0.5, o1, n, uo1, vo1, o0, n, uo0, vo0);
                }
            }
        }
    }

    private addVertex(p: Vector3Dd, n: Vector3Dd, u: number, v: number): void {
        const normal: Vector3Dd = n.normalized();
        const reference: Vector3Dd = Math.abs(normal.z()) < 0.9 ? new Vector3Dd(0, 0, 1) : new Vector3Dd(1, 0, 0);
        const tangent: Vector3Dd = reference.crossProduct(normal).normalized();
        const binormal: Vector3Dd = normal.crossProduct(tangent).normalized();

        this.vertices.push(
            new Float32Array([
                p.x(), p.y(), p.z(),
                normal.x(), normal.y(), normal.z(),
                u, v,
                tangent.x(), tangent.y(), tangent.z(),
                binormal.x(), binormal.y(), binormal.z(),
            ]),
        );
    }

    /**
    Makes the built mesh visible from both sides, as needed by open surfaces
    (i.e. a patch or a height field): each triangle gets a back side copy with
    the opposite winding and normals, so it is lit and culled as the front one
    is, with no special shader support.
    @param doubleSided true to add the back sides when building
    */
    public setDoubleSided(doubleSided: boolean): void {
        this.doubleSided = doubleSided;
    }

    public build(): WebGLMesh {
        const frontCount: number = this.vertices.length;

        if (this.doubleSided) {
            this.addBackSides(frontCount);
        }

        const count: number = this.vertices.length;
        const positions = new Float32Array(count * 3);
        const normals = new Float32Array(count * 3);
        const uvs = new Float32Array(count * 2);
        const tangents = new Float32Array(count * 3);
        const biNormals = new Float32Array(count * 3);

        for (let i = 0; i < count; i++) {
            const v: Float32Array = this.vertices[i]!;

            positions.set(v.subarray(0, 3), i * 3);
            normals.set(v.subarray(3, 6), i * 3);
            uvs.set(v.subarray(6, 8), i * 2);
            tangents.set(v.subarray(8, 11), i * 3);
            biNormals.set(v.subarray(11, 14), i * 3);
        }
        const mesh = new WebGLMesh(positions, normals, uvs, tangents, biNormals, this.characteristicSize);

        mesh.setFrontVertexCount(frontCount);
        return mesh;
    }

    /**
    Appends a back side copy of the first triangles: vertices in reverse order
    and all vectors (normal, tangent, binormal) negated.
    @param frontCount number of vertices of the front sides
    */
    private addBackSides(frontCount: number): void {
        for (let t = 0; t + 2 < frontCount; t += 3) {
            for (let k = 2; k >= 0; k--) {
                const v: Float32Array = this.vertices[t + k]!.slice();

                for (let c = 3; c < 6; c++) {
                    v[c] = -v[c]!;
                }
                for (let c = 8; c < 14; c++) {
                    v[c] = -v[c]!;
                }
                this.vertices.push(v);
            }
        }
    }
}
