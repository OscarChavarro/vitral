import { Surface } from "./Surface.js";
import type { Vertex } from "../element/Vertex.js";
import type { Ray } from "../element/Ray.js";
import type { RayHit } from "../element/RayHit.js";
import { TriangleMesh } from "./TriangleMesh.js";
import { Double } from "../../../../../java/lang/Double.js";
import { ArrayIndexOutOfBoundsException } from "../../../../../java/lang/ArrayIndexOutOfBoundsException.js";
import { NullPointerException } from "../../../../../java/lang/NullPointerException.js";

export class TriangleStripMesh extends Surface<Ray, RayHit> {
    //= Class attributes ========================================================

    // Basic mesh data model
    private name: string = "default";
    private vertexes: Vertex[] | null = null;
    private strips: (number[] | null)[] | null = null;

    //= Basic class management methods ==========================================

    public constructor() {
        super();
    }

    /** Needed for supplying the Geometry.getMinMax operation */
    private calculateMinMaxPositions(): number[] {
        const minMax: number[] = [0, 0, 0, 0, 0, 0];
        let minX = Double.MAX_VALUE;
        let minY = Double.MAX_VALUE;
        let minZ = Double.MAX_VALUE;
        let maxX = -Double.MAX_VALUE;
        let maxY = -Double.MAX_VALUE;
        let maxZ = -Double.MAX_VALUE;
        let i: number;

        const vertexes = TriangleStripMesh.nonNull(this.vertexes);
        for (i = 0; i < vertexes.length; i++) {
            const position = TriangleStripMesh.nonNull(vertexes[i] ?? null).getPosition();
            const x = position.x();
            const y = position.y();
            const z = position.z();

            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (z < minZ) minZ = z;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
            if (z > maxZ) maxZ = z;
        }
        minMax[0] = minX;
        minMax[1] = minY;
        minMax[2] = minZ;
        minMax[3] = maxX;
        minMax[4] = maxY;
        minMax[5] = maxZ;
        return minMax;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.getMinMax.
    @return a new 6 valued double array containing the coordinates of a min-max
    bounding box for current geometry.
    */
    public override getMinMax(): number[] {
        return this.calculateMinMaxPositions();
    }

    public getVertexes(): Vertex[] | null {
        return this.vertexes;
    }

    public getVertexAt(index: number): Vertex {
        const vertexes = TriangleStripMesh.nonNull(this.vertexes);
        if (index < 0 || index >= vertexes.length) {
            throw new ArrayIndexOutOfBoundsException("Index " + index + " out of bounds for length " + vertexes.length);
        }
        return vertexes[index]!;
    }

    public setVertexes(vertexes: Vertex[] | null): void {
        this.vertexes = vertexes;
    }

    public setStrips(indexes: (number[] | null)[] | null): void {
        this.strips = indexes;
    }

    public getStrips(): (number[] | null)[] | null {
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
        const vertexes = this.vertexes;
        const strips = this.strips;
        if (vertexes === null || strips === null) {
            return null;
        }
        const mesh = new TriangleMesh();
        let count = 0;
        let i: number;
        let j: number;

        for (i = 0; i < strips.length; i++) {
            const strip = strips[i] ?? null;
            if (strip !== null && strip.length > 2) {
                count += strip.length - 2;
            }
        }
        mesh.initVertexPositionsArray(vertexes.length);
        const positions = mesh.getVertexPositions()!;
        for (i = 0; i < vertexes.length; i++) {
            const position = TriangleStripMesh.nonNull(vertexes[i] ?? null).getPosition();
            positions[3 * i] = position.x();
            positions[3 * i + 1] = position.y();
            positions[3 * i + 2] = position.z();
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

    /**
    Check the general interface contract in superclass method
    Geometry.doIntersectionFirstHit.
    \todo  Method not implemented!
    Java overload `doIntersectionFirstHit(Ray inOut_Ray)` returns null; the
    two-argument form returns false.
    */
    public doIntersectionFirstHit(inOutRay: Ray): Ray | null;
    public override doIntersectionFirstHit(inRay: Ray, outHit: RayHit): boolean;
    public override doIntersectionFirstHit(inRay: Ray, outHit: RayHit | null): boolean;
    public override doIntersectionFirstHit(_inRay: Ray, outHit?: RayHit | null): Ray | null | boolean {
        if (outHit === undefined) {
            return null;
        }
        return false;
    }

    /**
    Check the general interface contract in superclass method
    Geometry.doExtraInformation.
    \todo  Method not implemented!
    */
    public override doExtraInformation(_inRay: Ray, _inT: number, _outData: RayHit): void {}

    private static nonNull<T>(value: T | null): T {
        if (value === null) {
            throw new NullPointerException();
        }
        return value;
    }
}
