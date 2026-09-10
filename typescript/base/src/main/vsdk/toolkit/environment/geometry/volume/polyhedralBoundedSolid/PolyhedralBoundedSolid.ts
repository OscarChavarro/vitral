import { Vector3Dd } from "../../../../common/linealAlgebra/Vector3Dd.js";
import { Ray } from "../../element/Ray.js";
import { RayHit } from "../../element/RayHit.js";
import { Solid } from "../Solid.js";
import { _PolyhedralBoundedSolidEdge } from "./nodes/_PolyhedralBoundedSolidEdge.js";
import { _PolyhedralBoundedSolidFace } from "./nodes/_PolyhedralBoundedSolidFace.js";
import { _PolyhedralBoundedSolidVertex } from "./nodes/_PolyhedralBoundedSolidVertex.js";
import { PolyhedralBoundedSolidPredicates } from "./PolyhedralBoundedSolidPredicates.js";

/** Five-level half-edge boundary representation from [MANT1988]. */
export class PolyhedralBoundedSolid extends Solid {
    public static readonly PLUS = 1;
    public static readonly MINUS = 0;
    private polygons: _PolyhedralBoundedSolidFace[] = [];
    private edges: _PolyhedralBoundedSolidEdge[] = [];
    private vertices: _PolyhedralBoundedSolidVertex[] = [];
    private maxVertexId = -1;
    private maxFaceId = -1;
    private modelIsValid = false;
    private visibilityActive = false;
    public findFace(id: number): _PolyhedralBoundedSolidFace | null {
        return this.polygons.find((face) => face.id === id) ?? null;
    }
    public findVertex(id: number): _PolyhedralBoundedSolidVertex | null {
        return this.vertices.find((vertex) => vertex.id === id) ?? null;
    }
    public getPolygonsList(): _PolyhedralBoundedSolidFace[] {
        return this.polygons;
    }
    public setPolygonsList(value: _PolyhedralBoundedSolidFace[]): void {
        this.polygons = value;
        this.modelIsValid = false;
    }
    public getEdgesList(): _PolyhedralBoundedSolidEdge[] {
        return this.edges;
    }
    public setEdgesList(value: _PolyhedralBoundedSolidEdge[]): void {
        this.edges = value;
        this.modelIsValid = false;
    }
    public getVerticesList(): _PolyhedralBoundedSolidVertex[] {
        return this.vertices;
    }
    public setVerticesList(value: _PolyhedralBoundedSolidVertex[]): void {
        this.vertices = value;
        this.modelIsValid = false;
    }
    public getMaxVertexId(): number {
        return this.maxVertexId;
    }
    public setMaxVertexId(value: number): void {
        this.maxVertexId = value;
    }
    public getMaxFaceId(): number {
        return this.maxFaceId;
    }
    public setMaxFaceId(value: number): void {
        this.maxFaceId = value;
    }
    public isValid(): boolean {
        return this.modelIsValid;
    }
    public setValidationState(value: boolean): void {
        this.modelIsValid = value;
    }
    public doIntersectionFirstHit(ray: Ray): Ray | null;
    public override doIntersectionFirstHit(ray: Ray, hit: RayHit): boolean;
    public doIntersectionFirstHit(ray: Ray, hit?: RayHit): Ray | null | boolean {
        let best: number | undefined;
        for (const face of this.polygons) {
            const plane = face.getContainingPlane();
            if (plane === null) continue;
            const candidate = plane.intersectRay(ray);
            if (candidate === null || candidate.getT() <= 0 || (best !== undefined && candidate.getT() >= best))
                continue;
            const point = ray.getOrigin().add(ray.getDirection().multiply(candidate.getT())),
                status = face.testPointInside(point, 1e-8, plane);
            if (status === PolyhedralBoundedSolid.INSIDE || status === PolyhedralBoundedSolid.LIMIT)
                best = candidate.getT();
        }
        if (best === undefined) return hit === undefined ? null : false;
        const result = ray.withT(best);
        if (hit === undefined) return result;
        hit.setRay(result);
        if (hit.needsAnySurfaceData()) this.doExtraInformation(ray, best, hit);
        return true;
    }
    public override doExtraInformation(ray: Ray, t: number, hit: RayHit): void {
        if (hit.needsPoint()) hit.p = ray.getOrigin().add(ray.getDirection().multiply(t));
    }
    public getMinMax(): Float64Array {
        const result = new Float64Array([Infinity, Infinity, Infinity, -Infinity, -Infinity, -Infinity]);
        for (const vertex of this.vertices) {
            const point = vertex.position;
            result[0] = Math.min(result[0]!, point.x());
            result[1] = Math.min(result[1]!, point.y());
            result[2] = Math.min(result[2]!, point.z());
            result[3] = Math.max(result[3]!, point.x());
            result[4] = Math.max(result[4]!, point.y());
            result[5] = Math.max(result[5]!, point.z());
        }
        return result;
    }
    /** Begins a read-only visibility-query batch; mutations invalidate the snapshot contract. */ public beginVisibilityQueries(): void {
        this.visibilityActive = true;
    }
    public endVisibilityQueries(): void {
        this.visibilityActive = false;
    }
    public visibilityQueriesActive(): boolean {
        return this.visibilityActive;
    }
    public override computeQuantitativeInvisibility(origin: Vector3Dd, point: Vector3Dd): number {
        return PolyhedralBoundedSolidPredicates.quantitativeInvisibility(this, origin, point);
    }
    public merge(other: PolyhedralBoundedSolid): void {
        const faceOffset = this.maxFaceId,
            vertexOffset = this.maxVertexId;
        for (const face of other.polygons) {
            face.id += faceOffset;
            this.maxFaceId = Math.max(this.maxFaceId, face.id);
            this.polygons.push(face);
        }
        for (const edge of other.edges) this.edges.push(edge);
        for (const vertex of other.vertices) {
            vertex.id += vertexOffset;
            this.maxVertexId = Math.max(this.maxVertexId, vertex.id);
            this.vertices.push(vertex);
        }
        other.polygons = [];
        other.edges = [];
        other.vertices = [];
        this.modelIsValid = false;
    }
    public static compareValue(a: number, b: number, tolerance: number): number {
        return Math.abs(a - b) < tolerance ? 0 : a > b ? 1 : -1;
    }
    public revert(): void {
        for (const face of this.polygons) face.revert();
        this.modelIsValid = false;
    }
    public override exportToPolyhedralBoundedSolid(): PolyhedralBoundedSolid {
        return this;
    }
    public override toString(): string {
        return `= POLYHEDRAL BOUNDED SOLID STRUCTURE =\nSolid with ${this.vertices.length} vertices:\nSolid with ${this.edges.length} edges:\nSolid with ${this.polygons.length} faces:\n`;
    }
}
