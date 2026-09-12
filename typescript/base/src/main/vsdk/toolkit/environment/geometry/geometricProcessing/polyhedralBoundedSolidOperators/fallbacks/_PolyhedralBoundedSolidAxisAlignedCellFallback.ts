import { Vector3Dd } from "../../../../../common/linealAlgebra/Vector3Dd.js";
import { Geometry } from "../../../Geometry.js";
import { Ray } from "../../../element/Ray.js";
import { PolyhedralBoundedSolid } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { _PolyhedralBoundedSolidEdge } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.js";
import { _PolyhedralBoundedSolidFace } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import { _PolyhedralBoundedSolidHalfEdge } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import { _PolyhedralBoundedSolidLoop } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";
import { _PolyhedralBoundedSolidVertex } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";
import { _PolyhedralBoundedSolidOperator } from "../_PolyhedralBoundedSolidOperator.js";
import { _PolyhedralBoundedSolidFallbackGeometry } from "./_PolyhedralBoundedSolidFallbackGeometry.js";

class AxisAlignedCellBooleanBuilder {
    private readonly solid = new PolyhedralBoundedSolid();
    private readonly vertices = new Map<string, _PolyhedralBoundedSolidVertex>();
    private readonly edges = new Map<string, _PolyhedralBoundedSolidEdge>();
    private nextVertexId = 1;
    private nextFaceId = 1;
    public constructor(
        private readonly xs: number[],
        private readonly ys: number[],
        private readonly zs: number[],
    ) {}
    private vertexKey(ix: number, iy: number, iz: number): string {
        return `${ix}:${iy}:${iz}`;
    }
    private vertexAt(ix: number, iy: number, iz: number): _PolyhedralBoundedSolidVertex {
        const key = this.vertexKey(ix, iy, iz);
        let vertex = this.vertices.get(key);
        if (vertex !== undefined) return vertex;
        vertex = new _PolyhedralBoundedSolidVertex(
            this.solid,
            new Vector3Dd(this.xs[ix]!, this.ys[iy]!, this.zs[iz]!),
            this.nextVertexId,
        );
        this.solid.setMaxVertexId(this.nextVertexId);
        this.nextVertexId++;
        this.vertices.set(key, vertex);
        return vertex;
    }
    private edgeKey(a: _PolyhedralBoundedSolidVertex, b: _PolyhedralBoundedSolidVertex): string {
        return a.id < b.id ? `${a.id}:${b.id}` : `${b.id}:${a.id}`;
    }
    private attachEdge(
        halfEdge: _PolyhedralBoundedSolidHalfEdge,
        a: _PolyhedralBoundedSolidVertex,
        b: _PolyhedralBoundedSolidVertex,
    ): void {
        const key = this.edgeKey(a, b);
        let edge = this.edges.get(key);
        if (edge === undefined) {
            edge = new _PolyhedralBoundedSolidEdge(this.solid);
            edge.rightHalf = halfEdge;
            this.edges.set(key, edge);
        } else if (edge.leftHalf === null) edge.leftHalf = halfEdge;
        else if (edge.rightHalf === null) edge.rightHalf = halfEdge;
        halfEdge.parentEdge = edge;
    }
    private addQuad(corners: number[][]): void {
        const face = new _PolyhedralBoundedSolidFace(this.solid, this.nextFaceId);
        this.solid.setMaxFaceId(this.nextFaceId);
        this.nextFaceId++;
        const loop = new _PolyhedralBoundedSolidLoop(face);
        const halfEdges: _PolyhedralBoundedSolidHalfEdge[] = [];
        const faceVertices: _PolyhedralBoundedSolidVertex[] = [];
        for (let i = 0; i < corners.length; i++) {
            const corner = corners[i]!;
            const vertex = this.vertexAt(corner[0]!, corner[1]!, corner[2]!);
            const halfEdge = new _PolyhedralBoundedSolidHalfEdge(vertex, loop, this.solid);
            faceVertices.push(vertex);
            halfEdges.push(halfEdge);
            loop.halfEdgesList.add(halfEdge);
            if (vertex.emanatingHalfEdge === null) vertex.emanatingHalfEdge = halfEdge;
        }
        loop.boundaryStartHalfEdge = halfEdges[0]!;
        for (let i = 0; i < halfEdges.length; i++)
            this.attachEdge(halfEdges[i]!, faceVertices[i]!, faceVertices[(i + 1) % faceVertices.length]!);
    }
    private result(): PolyhedralBoundedSolid {
        return this.solid;
    }
}

/** Literal axis-aligned cell decomposition fallback from the Java boolean kernel. */
export class _PolyhedralBoundedSolidAxisAlignedCellFallback extends _PolyhedralBoundedSolidOperator {
    private static isAxisAlignedEdge(edge: _PolyhedralBoundedSolidEdge | null): boolean {
        if (edge === null || edge.rightHalf === null || edge.leftHalf === null) return false;
        const a = edge.rightHalf.startingVertex.position;
        const b = edge.leftHalf.startingVertex.position;
        let changingAxes = 0;
        if (!_PolyhedralBoundedSolidFallbackGeometry.sameCoordinate(a.x(), b.x())) changingAxes++;
        if (!_PolyhedralBoundedSolidFallbackGeometry.sameCoordinate(a.y(), b.y())) changingAxes++;
        if (!_PolyhedralBoundedSolidFallbackGeometry.sameCoordinate(a.z(), b.z())) changingAxes++;
        return changingAxes <= 1;
    }
    private static isAxisAlignedSolid(solid: PolyhedralBoundedSolid | null): boolean {
        if (solid === null || solid.getEdgesList().size() <= 0) return false;
        for (let i = 0; i < solid.getEdgesList().size(); i++)
            if (!this.isAxisAlignedEdge(solid.getEdgesList().get(i)!)) return false;
        return true;
    }
    private static classifyPointForAxisAlignedFallback(solid: PolyhedralBoundedSolid | null, point: Vector3Dd): number {
        if (solid === null || solid.getPolygonsList().size() <= 0) return Geometry.OUTSIDE;
        const epsilon = this.numericContext.bigEpsilon();
        const ray = new Ray(point, new Vector3Dd(1, 0.371, 0.137));
        const distances: number[] = [];
        let hits = 0;
        for (let i = 0; i < solid.getPolygonsList().size(); i++) {
            const face = solid.getPolygonsList().get(i)!;
            const plane = face.getContainingPlane();
            if (plane === null) continue;
            const hit = plane.doIntersectionFirstHit(new Ray(ray));
            if (hit === null || hit.getT() <= epsilon) continue;
            const p = hit.getOrigin().add(hit.getDirection().multiply(hit.getT()));
            if (face.testPointInside(p, epsilon) !== Geometry.INSIDE) continue;
            let duplicate = false;
            for (let j = 0; j < distances.length; j++)
                if (Math.abs(distances[j]! - hit.getT()) <= epsilon) {
                    duplicate = true;
                    break;
                }
            if (!duplicate) {
                distances.push(hit.getT());
                hits++;
            }
        }
        return hits % 2 === 1 ? Geometry.INSIDE : Geometry.OUTSIDE;
    }
    public static axisAlignedCellSelected(insideA: boolean, insideB: boolean, operation: number): boolean {
        if (operation === this.UNION) return insideA || insideB;
        if (operation === this.INTERSECTION) return insideA && insideB;
        return insideA && !insideB;
    }
    private static addAxisAlignedBoundaryQuad(
        builder: AxisAlignedCellBooleanBuilder,
        axis: number,
        positiveSide: boolean,
        ix: number,
        iy: number,
        iz: number,
    ): void {
        if (axis === 0 && !positiveSide)
            builder["addQuad"]([
                [ix, iy, iz],
                [ix, iy, iz + 1],
                [ix, iy + 1, iz + 1],
                [ix, iy + 1, iz],
            ]);
        else if (axis === 0)
            builder["addQuad"]([
                [ix + 1, iy, iz],
                [ix + 1, iy + 1, iz],
                [ix + 1, iy + 1, iz + 1],
                [ix + 1, iy, iz + 1],
            ]);
        else if (axis === 1 && !positiveSide)
            builder["addQuad"]([
                [ix, iy, iz],
                [ix + 1, iy, iz],
                [ix + 1, iy, iz + 1],
                [ix, iy, iz + 1],
            ]);
        else if (axis === 1)
            builder["addQuad"]([
                [ix, iy + 1, iz],
                [ix, iy + 1, iz + 1],
                [ix + 1, iy + 1, iz + 1],
                [ix + 1, iy + 1, iz],
            ]);
        else if (axis === 2 && !positiveSide)
            builder["addQuad"]([
                [ix, iy, iz],
                [ix, iy + 1, iz],
                [ix + 1, iy + 1, iz],
                [ix + 1, iy, iz],
            ]);
        else
            builder["addQuad"]([
                [ix, iy, iz + 1],
                [ix + 1, iy, iz + 1],
                [ix + 1, iy + 1, iz + 1],
                [ix, iy + 1, iz + 1],
            ]);
    }
    public static buildAxisAlignedCellBooleanFallback(
        solidA: PolyhedralBoundedSolid,
        solidB: PolyhedralBoundedSolid,
        operation: number,
    ): PolyhedralBoundedSolid | null {
        if (!this.isAxisAlignedSolid(solidA) || !this.isAxisAlignedSolid(solidB)) return null;
        const xs = _PolyhedralBoundedSolidFallbackGeometry.uniqueVertexCoordinates(solidA, 0);
        const ys = _PolyhedralBoundedSolidFallbackGeometry.uniqueVertexCoordinates(solidA, 1);
        const zs = _PolyhedralBoundedSolidFallbackGeometry.uniqueVertexCoordinates(solidA, 2);
        for (let i = 0; i < solidB.getVerticesList().size(); i++) {
            const p = solidB.getVerticesList().get(i)!.position;
            _PolyhedralBoundedSolidFallbackGeometry.addUniqueCoordinate(xs, p.x());
            _PolyhedralBoundedSolidFallbackGeometry.addUniqueCoordinate(ys, p.y());
            _PolyhedralBoundedSolidFallbackGeometry.addUniqueCoordinate(zs, p.z());
        }
        if (xs.length < 2 || ys.length < 2 || zs.length < 2 || xs.length > 16 || ys.length > 16 || zs.length > 16)
            return null;
        const occupied = Array.from({ length: xs.length - 1 }, () =>
            Array.from({ length: ys.length - 1 }, () => Array<boolean>(zs.length - 1).fill(false)),
        );
        for (let ix = 0; ix < xs.length - 1; ix++)
            for (let iy = 0; iy < ys.length - 1; iy++)
                for (let iz = 0; iz < zs.length - 1; iz++) {
                    const sample = new Vector3Dd(
                        (xs[ix]! + xs[ix + 1]!) * 0.5,
                        (ys[iy]! + ys[iy + 1]!) * 0.5,
                        (zs[iz]! + zs[iz + 1]!) * 0.5,
                    );
                    const insideA = this.classifyPointForAxisAlignedFallback(solidA, sample) === Geometry.INSIDE;
                    const insideB = this.classifyPointForAxisAlignedFallback(solidB, sample) === Geometry.INSIDE;
                    occupied[ix]![iy]![iz] = this.axisAlignedCellSelected(insideA, insideB, operation);
                }
        const builder = new AxisAlignedCellBooleanBuilder(xs, ys, zs);
        for (let ix = 0; ix < xs.length - 1; ix++)
            for (let iy = 0; iy < ys.length - 1; iy++)
                for (let iz = 0; iz < zs.length - 1; iz++) {
                    if (!occupied[ix]![iy]![iz]) continue;
                    if (ix === 0 || !occupied[ix - 1]![iy]![iz])
                        this.addAxisAlignedBoundaryQuad(builder, 0, false, ix, iy, iz);
                    if (ix === xs.length - 2 || !occupied[ix + 1]![iy]![iz])
                        this.addAxisAlignedBoundaryQuad(builder, 0, true, ix, iy, iz);
                    if (iy === 0 || !occupied[ix]![iy - 1]![iz])
                        this.addAxisAlignedBoundaryQuad(builder, 1, false, ix, iy, iz);
                    if (iy === ys.length - 2 || !occupied[ix]![iy + 1]![iz])
                        this.addAxisAlignedBoundaryQuad(builder, 1, true, ix, iy, iz);
                    if (iz === 0 || !occupied[ix]![iy]![iz - 1])
                        this.addAxisAlignedBoundaryQuad(builder, 2, false, ix, iy, iz);
                    if (iz === zs.length - 2 || !occupied[ix]![iy]![iz + 1])
                        this.addAxisAlignedBoundaryQuad(builder, 2, true, ix, iy, iz);
                }
        return builder["result"]();
    }
    private static uniformCoordinates(min: number, max: number, divisions: number): number[] {
        const coordinates: number[] = [];
        for (let i = 0; i <= divisions; i++) coordinates.push(min + ((max - min) * i) / divisions);
        return coordinates;
    }
    private static buildUniformSampledCellBooleanFallback(
        solidA: PolyhedralBoundedSolid | null,
        solidB: PolyhedralBoundedSolid | null,
        operation: number,
    ): PolyhedralBoundedSolid | null {
        const divisions = 12;
        if (
            solidA === null ||
            solidB === null ||
            solidA.getVerticesList().size() <= 0 ||
            solidB.getVerticesList().size() <= 0
        )
            return null;
        const bounds = solidA.getMinMax();
        if (operation === this.UNION) {
            const boundsB = solidB.getMinMax();
            bounds[0] = Math.min(bounds[0]!, boundsB[0]!);
            bounds[1] = Math.min(bounds[1]!, boundsB[1]!);
            bounds[2] = Math.min(bounds[2]!, boundsB[2]!);
            bounds[3] = Math.max(bounds[3]!, boundsB[3]!);
            bounds[4] = Math.max(bounds[4]!, boundsB[4]!);
            bounds[5] = Math.max(bounds[5]!, boundsB[5]!);
        }
        const xs = this.uniformCoordinates(bounds[0]!, bounds[3]!, divisions);
        const ys = this.uniformCoordinates(bounds[1]!, bounds[4]!, divisions);
        const zs = this.uniformCoordinates(bounds[2]!, bounds[5]!, divisions);
        const occupied = Array.from({ length: divisions }, () =>
            Array.from({ length: divisions }, () => Array<boolean>(divisions).fill(false)),
        );
        let anyOccupied = false;
        for (let ix = 0; ix < divisions; ix++)
            for (let iy = 0; iy < divisions; iy++)
                for (let iz = 0; iz < divisions; iz++) {
                    const sample = new Vector3Dd(
                        (xs[ix]! + xs[ix + 1]!) * 0.5,
                        (ys[iy]! + ys[iy + 1]!) * 0.5,
                        (zs[iz]! + zs[iz + 1]!) * 0.5,
                    );
                    const insideA = this.classifyPointForAxisAlignedFallback(solidA, sample) === Geometry.INSIDE;
                    const insideB = this.classifyPointForAxisAlignedFallback(solidB, sample) === Geometry.INSIDE;
                    occupied[ix]![iy]![iz] = this.axisAlignedCellSelected(insideA, insideB, operation);
                    anyOccupied ||= occupied[ix]![iy]![iz]!;
                }
        if (!anyOccupied) return null;
        const builder = new AxisAlignedCellBooleanBuilder(xs, ys, zs);
        for (let ix = 0; ix < divisions; ix++)
            for (let iy = 0; iy < divisions; iy++)
                for (let iz = 0; iz < divisions; iz++) {
                    if (!occupied[ix]![iy]![iz]) continue;
                    if (ix === 0 || !occupied[ix - 1]![iy]![iz])
                        this.addAxisAlignedBoundaryQuad(builder, 0, false, ix, iy, iz);
                    if (ix === divisions - 1 || !occupied[ix + 1]![iy]![iz])
                        this.addAxisAlignedBoundaryQuad(builder, 0, true, ix, iy, iz);
                    if (iy === 0 || !occupied[ix]![iy - 1]![iz])
                        this.addAxisAlignedBoundaryQuad(builder, 1, false, ix, iy, iz);
                    if (iy === divisions - 1 || !occupied[ix]![iy + 1]![iz])
                        this.addAxisAlignedBoundaryQuad(builder, 1, true, ix, iy, iz);
                    if (iz === 0 || !occupied[ix]![iy]![iz - 1])
                        this.addAxisAlignedBoundaryQuad(builder, 2, false, ix, iy, iz);
                    if (iz === divisions - 1 || !occupied[ix]![iy]![iz + 1])
                        this.addAxisAlignedBoundaryQuad(builder, 2, true, ix, iy, iz);
                }
        return builder["result"]();
    }
}
