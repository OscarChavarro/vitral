//= References:                                                             =
//= [BLOO1988] Bloomenthal, Jules. "Polygonization of implicit surfaces".   =
//=     Computer Aided Geometric Design 5(4), 1988.                         =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

import { Double } from "../../../../../../java/lang/Double.js";
import { ArrayList } from "../../../../../../java/util/ArrayList.js";
import { Vector3Dd } from "../../../../common/linealAlgebra/Vector3Dd.js";
import type { PolyhedralBoundedSolid } from "./PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidMeshConstructions } from "./PolyhedralBoundedSolidMeshConstructions.js";

/**
Polyhedral approximations of solids given by a scalar field: the solid is the
region where the field is negative. The field is sampled on a regular grid,
and its zero level is polygonized by marching tetrahedra [BLOO1988]: each
grid cell is split into six tetrahedra sharing its main diagonal (the same
split in all the cells, so neighboring cells agree on their common faces),
and the field is taken as linear inside each tetrahedron. That gives one
triangle or one planar quadrilateral per tetrahedron crossed by the surface,
and a closed 2-manifold surface: the nodes on the border of the grid are taken
as outside, so a solid that does not fit in the grid is cut by it (with flat
caps half a cell inside the border).

The polygons are given to `PolyhedralBoundedSolidMeshConstructions`, so the
boundary representation is built only with Euler operators [MANT1988], for
any genus of the surface.
*/
export class PolyhedralBoundedSolidFieldConstructions {
    /// The grid is shifted this fraction of a cell (an irrational number), so
    /// its nodes do not fall exactly on simple surfaces, which would give
    /// coincident vertices
    private static readonly GRID_SHIFT = 0.5 * (Math.sqrt(5.0) - 2.0);

    /**
    Samples a field over a box and builds the solid where it is negative.
    @param field scalar field
    @param minimum minimum corner of the box
    @param maximum maximum corner of the box
    @param divisions number of grid cells along the largest side of the box
    (cells are cubes)
    @return the polyhedral approximation of the solid inside the box, or null
    if it is empty
    */
    public static createFromScalarField(
        field: (point: Vector3Dd) => number,
        minimum: Vector3Dd,
        maximum: Vector3Dd,
        divisions: number,
    ): PolyhedralBoundedSolid | null {
        const size = maximum.subtract(minimum);
        const largest = Math.max(size.x(), Math.max(size.y(), size.z()));
        if (!(largest > 0) || divisions < 1) {
            return null;
        }
        const h = largest / divisions;
        const nodesX = Math.max(2, Math.trunc(Math.ceil(size.x() / h - 1.0e-9)) + 1);
        const nodesY = Math.max(2, Math.trunc(Math.ceil(size.y() / h - 1.0e-9)) + 1);
        const nodesZ = Math.max(2, Math.trunc(Math.ceil(size.z() / h - 1.0e-9)) + 1);
        // Grid centered on the box, slightly shifted
        const origin = new Vector3Dd(
            minimum.x() + 0.5 * (size.x() - (nodesX - 1) * h) + PolyhedralBoundedSolidFieldConstructions.GRID_SHIFT * h,
            minimum.y() + 0.5 * (size.y() - (nodesY - 1) * h) + PolyhedralBoundedSolidFieldConstructions.GRID_SHIFT * h,
            minimum.z() + 0.5 * (size.z() - (nodesZ - 1) * h) + PolyhedralBoundedSolidFieldConstructions.GRID_SHIFT * h,
        );
        const values = new Float64Array(nodesX * nodesY * nodesZ);

        for (let k = 0; k < nodesZ; k++) {
            for (let j = 0; j < nodesY; j++) {
                for (let i = 0; i < nodesX; i++) {
                    values[i + nodesX * (j + nodesY * k)] = field(
                        new Vector3Dd(origin.x() + i * h, origin.y() + j * h, origin.z() + k * h),
                    );
                }
            }
        }
        return PolyhedralBoundedSolidFieldConstructions.createFromSampledField(
            nodesX,
            nodesY,
            nodesZ,
            origin,
            new Vector3Dd(h, h, h),
            values,
        );
    }

    /**
    Samples a field over a box enlarged by a margin of a cell and a half on
    each side, so a solid that fits in the box is not cut by the grid.
    @param field scalar field
    @param minimum minimum corner of a box containing the solid
    @param maximum maximum corner of a box containing the solid
    @param divisions number of grid cells along the largest side of the box
    @return the polyhedral approximation of the solid, or null if it is empty
    */
    public static createFromBoundedScalarField(
        field: (point: Vector3Dd) => number,
        minimum: Vector3Dd,
        maximum: Vector3Dd,
        divisions: number,
    ): PolyhedralBoundedSolid | null {
        const size = maximum.subtract(minimum);
        const largest = Math.max(size.x(), Math.max(size.y(), size.z()));
        if (!(largest > 0) || divisions < 1) {
            return null;
        }
        const margin = (1.5 * largest) / divisions;
        const m = new Vector3Dd(margin, margin, margin);
        return PolyhedralBoundedSolidFieldConstructions.createFromScalarField(
            field,
            minimum.subtract(m),
            maximum.add(m),
            divisions + 3,
        );
    }

    /**
    Builds the solid where a sampled field is negative.
    @param nodesX number of grid nodes along X (at least 2)
    @param nodesY number of grid nodes along Y (at least 2)
    @param nodesZ number of grid nodes along Z (at least 2)
    @param origin position of node (0, 0, 0)
    @param spacing distance between nodes along each axis
    @param values field at each node (i, j, k), at index i + nodesX*(j + nodesY*k);
    nodes on the border of the grid are taken as outside
    @return the polyhedral approximation of the solid, or null if it is empty
    */
    public static createFromSampledField(
        nodesX: number,
        nodesY: number,
        nodesZ: number,
        origin: Vector3Dd,
        spacing: Vector3Dd,
        values: Float64Array,
    ): PolyhedralBoundedSolid | null {
        return new _Polygonizer(nodesX, nodesY, nodesZ, origin, spacing, values).build();
    }
}

/**
Java private static nested class
`PolyhedralBoundedSolidFieldConstructions._Polygonizer`. The private
constants that only it uses are declared here, since a TypeScript module
class can not read the private members of the outer class.
*/
class _Polygonizer {
    /// Interpolated vertices are kept this fraction of an edge away from its
    /// nodes, so polygons never degenerate to points or lines
    private static readonly MINIMUM_EDGE_FRACTION = 0.001;
    /// A quadrilateral whose fourth vertex is farther than this (relative to
    /// its size) from the plane of the other three is split in triangles
    private static readonly PLANARITY_TOLERANCE = 1.0e-9;

    /// A node whose field is closer to zero than this fraction of the largest
    /// difference with its neighbors gets this value instead (keeping its
    /// sign): the surface stays that fraction of an edge away from it, so no
    /// sliver polygons appear around nodes the surface passes close to
    private static readonly NODE_SEPARATION = 0.05;
    /// Offsets to the neighbors of a node along the edges of the tetrahedra
    private static readonly TETRAHEDRA_NEIGHBORS: readonly (readonly number[])[] = [
        [1, 0, 0],
        [0, 1, 0],
        [0, 0, 1],
        [1, 1, 0],
        [1, 0, 1],
        [0, 1, 1],
        [1, 1, 1],
        [-1, 0, 0],
        [0, -1, 0],
        [0, 0, -1],
        [-1, -1, 0],
        [-1, 0, -1],
        [0, -1, -1],
        [-1, -1, -1],
    ];

    /// The six tetrahedra of a cell, as corners of the cell (bit 0: +x, bit
    /// 1: +y, bit 2: +z): paths from corner 0 to corner 7 along the axes
    private static readonly TETRAHEDRA: readonly (readonly number[])[] = [
        [0, 1, 3, 7],
        [0, 1, 5, 7],
        [0, 2, 3, 7],
        [0, 2, 6, 7],
        [0, 4, 5, 7],
        [0, 4, 6, 7],
    ];

    private readonly nodesX: number;
    private readonly nodesY: number;
    private readonly nodesZ: number;
    private readonly origin: Vector3Dd;
    private readonly spacing: Vector3Dd;
    private readonly values: Float64Array;
    private readonly positions = new ArrayList<Vector3Dd>();
    private readonly polygons = new ArrayList<number[]>();
    /// Java `HashMap<Long, Integer>`, only looked up (never iterated), so a
    /// native `Map` keyed by the numeric value has the same behavior
    private readonly edgeVertices = new Map<number, number>();

    public constructor(
        nodesX: number,
        nodesY: number,
        nodesZ: number,
        origin: Vector3Dd,
        spacing: Vector3Dd,
        values: Float64Array,
    ) {
        this.nodesX = nodesX;
        this.nodesY = nodesY;
        this.nodesZ = nodesZ;
        this.origin = origin;
        this.spacing = spacing;
        this.values = this.separateFromZero(values);
    }

    /**
    @return the values, with the ones of the nodes too close to zero
    moved away from it (see NODE_SEPARATION)
    */
    private separateFromZero(original: Float64Array): Float64Array {
        const separated = original.slice();
        for (let k = 1; k < this.nodesZ - 1; k++) {
            for (let j = 1; j < this.nodesY - 1; j++) {
                for (let i = 1; i < this.nodesX - 1; i++) {
                    const node = this.nodeIndex(i, j, k);
                    const v = original[node]!;
                    if (!Double.isFinite(v)) {
                        continue;
                    }
                    let largestDifference = 0;
                    for (const offset of _Polygonizer.TETRAHEDRA_NEIGHBORS) {
                        const w = original[this.nodeIndex(i + offset[0]!, j + offset[1]!, k + offset[2]!)]!;
                        if (Double.isFinite(w)) {
                            largestDifference = Math.max(largestDifference, Math.abs(w - v));
                        }
                    }
                    const minimum = _Polygonizer.NODE_SEPARATION * largestDifference;
                    if (Math.abs(v) < minimum) {
                        separated[node] = v < 0 ? -minimum : minimum;
                    }
                }
            }
        }
        return separated;
    }

    private nodeIndex(i: number, j: number, k: number): number {
        return i + this.nodesX * (j + this.nodesY * k);
    }

    private nodePosition(node: number): Vector3Dd {
        const i = node % this.nodesX;
        const j = Math.trunc(node / this.nodesX) % this.nodesY;
        const k = Math.trunc(node / (this.nodesX * this.nodesY));
        return new Vector3Dd(
            this.origin.x() + i * this.spacing.x(),
            this.origin.y() + j * this.spacing.y(),
            this.origin.z() + k * this.spacing.z(),
        );
    }

    private isBorder(node: number): boolean {
        const i = node % this.nodesX;
        const j = Math.trunc(node / this.nodesX) % this.nodesY;
        const k = Math.trunc(node / (this.nodesX * this.nodesY));
        return i === 0 || j === 0 || k === 0 || i === this.nodesX - 1 || j === this.nodesY - 1 || k === this.nodesZ - 1;
    }

    private isInside(node: number): boolean {
        return !this.isBorder(node) && this.values[node]! < 0;
    }

    /**
    @return index of the vertex where the zero level crosses the edge
    from an inside node to an outside node
    */
    private edgeVertex(inside: number, outside: number): number {
        const key = Math.min(inside, outside) * this.values.length + Math.max(inside, outside);
        let index = this.edgeVertices.get(key);
        if (index !== undefined) {
            return index;
        }
        const a = this.values[inside]!;
        const b = this.values[outside]!;
        let t: number;
        if (this.isBorder(outside)) {
            // Caps: half way to the border, so they are flat
            t = 0.5;
        } else if (Double.isFinite(a) && Double.isFinite(b) && b >= 0) {
            t = a / (a - b);
        } else {
            // Not a number or infinite: no reliable interpolation
            t = 0.5;
        }
        t = Math.max(_Polygonizer.MINIMUM_EDGE_FRACTION, Math.min(1.0 - _Polygonizer.MINIMUM_EDGE_FRACTION, t));
        const p = this.nodePosition(inside);
        const q = this.nodePosition(outside);
        this.positions.add(p.add(q.subtract(p).multiply(t)));
        index = this.positions.size() - 1;
        this.edgeVertices.set(key, index);
        return index;
    }

    public build(): PolyhedralBoundedSolid | null {
        const corners = new Int32Array(8);

        for (let k = 0; k < this.nodesZ - 1; k++) {
            for (let j = 0; j < this.nodesY - 1; j++) {
                for (let i = 0; i < this.nodesX - 1; i++) {
                    for (let c = 0; c < 8; c++) {
                        corners[c] = this.nodeIndex(i + (c & 1), j + ((c >> 1) & 1), k + ((c >> 2) & 1));
                    }
                    for (const tetrahedron of _Polygonizer.TETRAHEDRA) {
                        this.polygonizeTetrahedron(
                            corners[tetrahedron[0]!]!,
                            corners[tetrahedron[1]!]!,
                            corners[tetrahedron[2]!]!,
                            corners[tetrahedron[3]!]!,
                        );
                    }
                }
            }
        }
        return PolyhedralBoundedSolidMeshConstructions.createFromPolygonMesh(this.positions, this.polygons);
    }

    private polygonizeTetrahedron(n0: number, n1: number, n2: number, n3: number): void {
        const nodes = [n0, n1, n2, n3];
        const inNodes = new Int32Array(4);
        const out = new Int32Array(4);
        let insideCount = 0;
        let outsideCount = 0;

        for (const node of nodes) {
            if (this.isInside(node)) {
                inNodes[insideCount++] = node;
            } else {
                out[outsideCount++] = node;
            }
        }
        if (insideCount === 0 || outsideCount === 0) {
            return;
        }

        let polygon: number[];
        if (insideCount === 1) {
            polygon = [
                this.edgeVertex(inNodes[0]!, out[0]!),
                this.edgeVertex(inNodes[0]!, out[1]!),
                this.edgeVertex(inNodes[0]!, out[2]!),
            ];
        } else if (insideCount === 3) {
            polygon = [
                this.edgeVertex(inNodes[0]!, out[0]!),
                this.edgeVertex(inNodes[1]!, out[0]!),
                this.edgeVertex(inNodes[2]!, out[0]!),
            ];
        } else {
            // Consecutive vertices share a node of the tetrahedron
            polygon = [
                this.edgeVertex(inNodes[0]!, out[0]!),
                this.edgeVertex(inNodes[0]!, out[1]!),
                this.edgeVertex(inNodes[1]!, out[1]!),
                this.edgeVertex(inNodes[1]!, out[0]!),
            ];
        }

        // Counterclockwise as seen from outside: the normal points from
        // the inside nodes to the outside ones
        const towardsOutside = this.centroid(out, outsideCount).subtract(this.centroid(inNodes, insideCount));
        if (this.newellNormal(polygon).dotProduct(towardsOutside) < 0) {
            _Polygonizer.reverse(polygon);
        }

        if (polygon.length === 4 && !this.isPlanar(polygon)) {
            this.polygons.add([polygon[0]!, polygon[1]!, polygon[2]!]);
            this.polygons.add([polygon[0]!, polygon[2]!, polygon[3]!]);
        } else {
            this.polygons.add(polygon);
        }
    }

    private centroid(nodes: Int32Array, count: number): Vector3Dd {
        let sum = new Vector3Dd(0, 0, 0);
        for (let i = 0; i < count; i++) {
            sum = sum.add(this.nodePosition(nodes[i]!));
        }
        return sum.multiply(1.0 / count);
    }

    private newellNormal(polygon: number[]): Vector3Dd {
        let x = 0;
        let y = 0;
        let z = 0;
        for (let i = 0; i < polygon.length; i++) {
            const p = this.positions.get(polygon[i]!);
            const q = this.positions.get(polygon[(i + 1) % polygon.length]!);
            x += (p.y() - q.y()) * (p.z() + q.z());
            y += (p.z() - q.z()) * (p.x() + q.x());
            z += (p.x() - q.x()) * (p.y() + q.y());
        }
        return new Vector3Dd(x, y, z);
    }

    private isPlanar(quad: number[]): boolean {
        const p0 = this.positions.get(quad[0]!);
        const p1 = this.positions.get(quad[1]!);
        const p2 = this.positions.get(quad[2]!);
        const p3 = this.positions.get(quad[3]!);
        const n = p1.subtract(p0).crossProduct(p2.subtract(p0));
        const size = Math.max(p2.subtract(p0).length(), p3.subtract(p1).length());
        const length = n.length();
        if (!(length > 0) || !(size > 0)) {
            return false;
        }
        return Math.abs(p3.subtract(p0).dotProduct(n)) / length <= _Polygonizer.PLANARITY_TOLERANCE * size;
    }

    private static reverse(polygon: number[]): void {
        for (let i = 0, j = polygon.length - 1; i < j; i++, j--) {
            const tmp = polygon[i]!;
            polygon[i] = polygon[j]!;
            polygon[j] = tmp;
        }
    }
}
