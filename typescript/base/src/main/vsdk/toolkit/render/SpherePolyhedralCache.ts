import type { Vector3Dd } from "../common/linealAlgebra/Vector3Dd.js";
import type { Sphere } from "../environment/geometry/volume/Sphere.js";
import type { PolyhedralBoundedSolid } from "../environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import type { _PolyhedralBoundedSolidFace } from "../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import type { _PolyhedralBoundedSolidLoop } from "../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";

/**
A sphere converted into a polyhedral bounded solid, with the attributes
of its triangles for the rasterizers: three consecutive vertices make a
triangle, counterclockwise seen from outside.

Port of the nested class `vsdk.toolkit.render.SpherePolyhedralCache.Entry`.
*/
export class SpherePolyhedralCacheEntry {
    private readonly radius: number;
    private readonly meridians: number;
    private readonly parallels: number;
    private readonly solid: PolyhedralBoundedSolid;
    private readonly positions: Float32Array;
    private readonly normals: Float32Array;
    private readonly uvs: Float32Array;
    private readonly tangents: Float32Array;
    private readonly biNormals: Float32Array;

    /** @internal */
    public constructor(
        radius: number,
        meridians: number,
        parallels: number,
        solid: PolyhedralBoundedSolid,
        positions: Float32Array,
        normals: Float32Array,
        uvs: Float32Array,
        tangents: Float32Array,
        biNormals: Float32Array,
    ) {
        this.radius = radius;
        this.meridians = meridians;
        this.parallels = parallels;
        this.solid = solid;
        this.positions = positions;
        this.normals = normals;
        this.uvs = uvs;
        this.tangents = tangents;
        this.biNormals = biNormals;
    }

    /**
    @return radius of the sphere
    */
    public getRadius(): number {
        return this.radius;
    }

    /**
    @return number of meridians of the tessellation
    */
    public getMeridians(): number {
        return this.meridians;
    }

    /**
    @return number of parallels (bands between the poles) of the
    tessellation
    */
    public getParallels(): number {
        return this.parallels;
    }

    /**
    @return the sphere as a polyhedral bounded solid, shared by every
    user of the entry: it must not be modified
    */
    public getSolid(): PolyhedralBoundedSolid {
        return this.solid;
    }

    /**
    @return number of vertices of the triangles
    */
    public getVertexCount(): number {
        return this.positions.length / 3;
    }

    /**
    @return x, y, z of each vertex of the triangles
    */
    public getPositions(): Float32Array {
        return this.positions;
    }

    /**
    @return x, y, z of the normal of the sphere at each vertex
    */
    public getNormals(): Float32Array {
        return this.normals;
    }

    /**
    @return u, v texture coordinates of each vertex
    */
    public getUvs(): Float32Array {
        return this.uvs;
    }

    /**
    @return x, y, z of the tangent of the sphere at each vertex
    */
    public getTangents(): Float32Array {
        return this.tangents;
    }

    /**
    @return x, y, z of the binormal of the sphere at each vertex
    */
    public getBiNormals(): Float32Array {
        return this.biNormals;
    }
}

/**
Port of `vsdk.toolkit.render.SpherePolyhedralCache`.

Tessellation cache shared by the `*SphereRenderer`s of every rendering
technology (WebGL, software...), independent of all of them.

A `Sphere` is kept by its renderers as a parametric surface; to draw it, it is
converted, in code, into a `PolyhedralBoundedSolid` with the parametric
construction of `Sphere.exportToPolyhedralBoundedSolid(meridians, parallels)`,
and that solid is the cached representation: besides being drawn by the
rasterizers, it can be used by other drawing algorithms that are not
OpenGL based (hidden line removal, software renderers...), which a plain mesh
does not allow.

For the rasterizers, each entry also derives from the (triangular) faces of
the solid the per vertex attributes a smooth, textured and bump mapped sphere
needs, which the faceted solid does not carry: for each vertex, its spherical
coordinates (theta, phi) are recovered from its position, and the normal,
tangent and binormal are evaluated on the parametric surface
(`Sphere.sphereNormal`, `sphereTangent`, `sphereBinormal`), with texture
coordinates (1 - theta / (2 PI), (phi + PI / 2) / PI). Along the seam of the
parametrization (theta = 0) the texture coordinates of a triangle are made
continuous, and at the poles (where theta is undefined) the vertex takes the
mean theta of the other corners of its triangle.

Entries are kept by radius and resolution, so many spheres of a scene share
them. Java's `synchronized` has no counterpart: a JavaScript realm runs this
code on a single thread, and each Web Worker has its own cache.
*/
export class SpherePolyhedralCache {
    /// Maximum number of entries kept; when reached, the cache is emptied
    private static readonly MAX_CACHED_ENTRIES: number = 256;
    /// Relative distance to a pole under which a vertex is taken as a pole
    private static readonly POLE_TOLERANCE: number = 1.0e-9;

    private static readonly ENTRIES: Map<string, SpherePolyhedralCacheEntry> =
        new Map<string, SpherePolyhedralCacheEntry>();

    private constructor() {}

    /**
    @param sphere sphere to tessellate
    @param meridians number of meridians
    @param parallels number of bands between the poles
    @return the cached tessellation of a sphere of that radius, created the
    first time it is asked for
    */
    public static obtain(sphere: Sphere, meridians: number, parallels: number): SpherePolyhedralCacheEntry {
        const key: string = sphere.getRadius() + "/" + meridians + "/" + parallels;
        let entry: SpherePolyhedralCacheEntry | undefined = SpherePolyhedralCache.ENTRIES.get(key);

        if (entry === undefined) {
            entry = SpherePolyhedralCache.build(sphere, meridians, parallels);
            if (SpherePolyhedralCache.ENTRIES.size >= SpherePolyhedralCache.MAX_CACHED_ENTRIES) {
                SpherePolyhedralCache.ENTRIES.clear();
            }
            SpherePolyhedralCache.ENTRIES.set(key, entry);
        }
        return entry;
    }

    /**
    Forgets every entry.
    */
    public static clear(): void {
        SpherePolyhedralCache.ENTRIES.clear();
    }

    private static build(sphere: Sphere, meridians: number, parallels: number): SpherePolyhedralCacheEntry {
        const solid: PolyhedralBoundedSolid = sphere.exportToPolyhedralBoundedSolid(meridians, parallels);
        const triangles: Vector3Dd[] = SpherePolyhedralCache.collectTriangles(solid);
        const vertexCount: number = triangles.length;
        const positions: Float32Array = new Float32Array(vertexCount * 3);
        const normals: Float32Array = new Float32Array(vertexCount * 3);
        const uvs: Float32Array = new Float32Array(vertexCount * 2);
        const tangents: Float32Array = new Float32Array(vertexCount * 3);
        const biNormals: Float32Array = new Float32Array(vertexCount * 3);
        const radius: number = sphere.getRadius();

        for (let t: number = 0; t + 2 < vertexCount; t += 3) {
            const theta: number[] = [0, 0, 0];
            const phi: number[] = [0, 0, 0];
            const pole: boolean[] = [false, false, false];

            for (let k: number = 0; k < 3; k++) {
                const p: Vector3Dd = triangles[t + k]!;
                const z: number = radius > 0 ? p.z() / radius : 0;

                phi[k] = Math.asin(Math.max(-1.0, Math.min(1.0, z)));
                pole[k] = Math.abs(Math.abs(z) - 1.0) < SpherePolyhedralCache.POLE_TOLERANCE;
                theta[k] = Math.atan2(-p.y(), p.x());
                if (theta[k]! < 0) {
                    theta[k] = theta[k]! + 2 * Math.PI;
                }
            }
            SpherePolyhedralCache.makeContinuous(theta, pole);

            for (let k: number = 0; k < 3; k++) {
                const i: number = t + k;
                const p: Vector3Dd = triangles[i]!;
                const n: Vector3Dd = sphere.sphereNormal(theta[k]!, phi[k]!);
                const tangent: Vector3Dd = sphere.sphereTangent(theta[k]!, phi[k]!);
                const biNormal: Vector3Dd = sphere.sphereBinormal(theta[k]!, phi[k]!);

                SpherePolyhedralCache.put(positions, i, p);
                SpherePolyhedralCache.put(normals, i, n);
                SpherePolyhedralCache.put(tangents, i, tangent);
                SpherePolyhedralCache.put(biNormals, i, biNormal);
                uvs[2 * i] = 1.0 - theta[k]! / (2 * Math.PI);
                uvs[2 * i + 1] = (phi[k]! + Math.PI / 2) / Math.PI;
            }
        }
        return new SpherePolyhedralCacheEntry(radius, meridians, parallels, solid, positions,
            normals, uvs, tangents, biNormals);
    }

    /**
    Makes the angles of the corners of a triangle continuous across the seam
    (a triangle whose angles span more than half a turn crosses it, so the
    small ones are taken one turn further), and gives the poles the mean
    angle of the other corners.
    */
    private static makeContinuous(theta: number[], pole: boolean[]): void {
        let min: number = Number.MAX_VALUE;
        let max: number = -Number.MAX_VALUE;

        for (let k: number = 0; k < 3; k++) {
            if (!pole[k]) {
                min = Math.min(min, theta[k]!);
                max = Math.max(max, theta[k]!);
            }
        }
        if (max - min > Math.PI) {
            for (let k: number = 0; k < 3; k++) {
                if (!pole[k] && theta[k]! < Math.PI) {
                    theta[k] = theta[k]! + 2 * Math.PI;
                }
            }
        }

        let sum: number = 0;
        let count: number = 0;

        for (let k: number = 0; k < 3; k++) {
            if (!pole[k]) {
                sum += theta[k]!;
                count++;
            }
        }
        for (let k: number = 0; k < 3; k++) {
            if (pole[k]) {
                theta[k] = count > 0 ? sum / count : 0;
            }
        }
    }

    /**
    @return the vertices of the faces of the solid as triangles (a fan per
    face, from its outer loop), three consecutive per triangle, each one
    counterclockwise seen from outside the sphere
    */
    private static collectTriangles(solid: PolyhedralBoundedSolid): Vector3Dd[] {
        const triangles: Vector3Dd[] = [];

        for (let f: number = 0; f < solid.getPolygonsList().size(); f++) {
            const face: _PolyhedralBoundedSolidFace | null = solid.getPolygonsList().get(f) ?? null;

            if (face === null || face.boundariesList.size() < 1) {
                continue;
            }
            const loop: _PolyhedralBoundedSolidLoop | null = face.boundariesList.get(0) ?? null;
            const start: _PolyhedralBoundedSolidHalfEdge | null = loop !== null ? loop.boundaryStartHalfEdge : null;
            const polygon: Vector3Dd[] = [];

            if (start === null) {
                continue;
            }
            let he: _PolyhedralBoundedSolidHalfEdge | null = start;
            do {
                polygon.push(he.startingVertex.position);
                he = he.next();
            } while (he !== start && he !== null);

            for (let i: number = 1; i + 1 < polygon.length; i++) {
                const a: Vector3Dd = polygon[0]!;
                const b: Vector3Dd = polygon[i]!;
                const c: Vector3Dd = polygon[i + 1]!;
                const normal: Vector3Dd = b.subtract(a).crossProduct(c.subtract(a));
                const center: Vector3Dd = a.add(b).add(c);

                if (normal.dotProduct(center) >= 0) {
                    triangles.push(a, b, c);
                }
                else {
                    triangles.push(a, c, b);
                }
            }
        }
        return triangles;
    }

    private static put(array: Float32Array, index: number, v: Vector3Dd): void {
        array[3 * index] = v.x();
        array[3 * index + 1] = v.y();
        array[3 * index + 2] = v.z();
    }
}
