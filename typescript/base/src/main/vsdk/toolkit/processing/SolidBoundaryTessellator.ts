import { Double } from "../../../java/lang/Double.js";
import { ArrayList } from "../../../java/util/ArrayList.js";
import { Vector3Dd } from "../common/linealAlgebra/Vector3Dd.js";
import { GeometryBounds } from "../environment/geometry/element/GeometryBounds.js";
import { GeometryContainment } from "../environment/geometry/element/GeometryContainment.js";
import { Ray } from "../environment/geometry/element/Ray.js";
import { RayCrossingQuery } from "../environment/geometry/element/RayCrossingQuery.js";
import { RayHit } from "../environment/geometry/element/RayHit.js";
import { RaySolidClassification } from "../environment/geometry/element/RaySolidClassification.js";
import { RaySolidQueryStatus } from "../environment/geometry/element/RaySolidQueryStatus.js";
import { TriangleMesh } from "../environment/geometry/surface/TriangleMesh.js";
import type { SceneSolid } from "../environment/scene/SceneSolid.js";
import type { SolidBoundaryTessellationReport } from "./SolidBoundaryTessellationReport.js";

// Drawable triangle mesh of the boundary of a body's solid inside a world
// region: naive surface nets (Gibson 1998) over a grid of cubic cells whose
// side is the region's longest side divided by the resolution. It works for any
// body that classifies rays or points, such as quadrics, implicit solids,
// height fields and CSG bodies, and asks the body for everything:
// - node containment comes from one ray classification per grid line along x
//   (one point query per node when the line cannot be classified);
// - each grid edge whose ends differ crosses the boundary where the body's
//   first hit from the outside end lies, with the body's surface normal;
// - each cell holding crossings gets one vertex, the mean of its crossings,
//   moved onto the boundary by a ray along the mean normal when that hit stays
//   in the cell;
// - each crossing edge gives the quad of the four cells around it, facing out.
// The mesh is closed where the solid leaves the region. It is an approximation
// of the declared resolution, not another definition of the solid: details
// thinner than a cell may disappear or join, and a cell with several sheets
// gets one vertex. The report says how many crossings and vertices lie on the
// boundary found by the body's own ray queries.
export class SolidBoundaryTessellator {
    private static readonly OUTSIDE_NODE = 0;
    private static readonly INSIDE_NODE = 1;
    private readonly body: SceneSolid;
    private origin = new Vector3Dd();
    private cellSize: number;
    private readonly nodeCounts: number[] = [0, 0, 0];
    private readonly inside = new ArrayList<number>();
    private readonly edgeCrossings: ArrayList<number>[] = [
        new ArrayList<number>(),
        new ArrayList<number>(),
        new ArrayList<number>(),
    ];
    private readonly crossingPoints = new ArrayList<Vector3Dd>();
    private readonly crossingNormals = new ArrayList<Vector3Dd>();
    private readonly cellVertices = new ArrayList<number>();
    private readonly vertexPoints = new ArrayList<Vector3Dd>();
    private readonly vertexNormals = new ArrayList<Vector3Dd>();
    private readonly triangleIndices = new ArrayList<number>();
    private readonly report: SolidBoundaryTessellationReport;

    private constructor(body: SceneSolid, report: SolidBoundaryTessellationReport) {
        this.body = body;
        this.cellSize = 0;
        this.report = report;
        this.nodeCounts[0] = this.nodeCounts[1] = this.nodeCounts[2] = 0;
    }

    private nodeIndex(i: number, j: number, k: number): number {
        return (k * this.nodeCounts[1]! + j) * this.nodeCounts[0]! + i;
    }

    private cellIndex(i: number, j: number, k: number): number {
        return (k * (this.nodeCounts[1]! - 1) + j) * (this.nodeCounts[0]! - 1) + i;
    }

    private nodePoint(i: number, j: number, k: number): Vector3Dd {
        return new Vector3Dd(
            this.origin.x() + i * this.cellSize,
            this.origin.y() + j * this.cellSize,
            this.origin.z() + k * this.cellSize,
        );
    }

    // The outer layer of nodes lies outside the region and counts as outside, so
    // the mesh closes where the solid leaves the region.
    private isBorderNode(i: number, j: number, k: number): boolean {
        return (
            i === 0 ||
            j === 0 ||
            k === 0 ||
            i === this.nodeCounts[0]! - 1 ||
            j === this.nodeCounts[1]! - 1 ||
            k === this.nodeCounts[2]! - 1
        );
    }

    private classifyNodesByPoints(j: number, k: number): void {
        this.report.pointClassifiedLines++;
        for (let i = 1; i < this.nodeCounts[0]! - 1; i++) {
            const p = this.nodePoint(i, j, k);
            let c = this.body.classifyPoint(p, 0);
            if (c === GeometryContainment.Unsupported) c = this.body.classifyPointCertified(p, 0);
            if (c === GeometryContainment.Inside)
                this.inside.set(this.nodeIndex(i, j, k), SolidBoundaryTessellator.INSIDE_NODE);
            else if (c !== GeometryContainment.Outside && c !== GeometryContainment.Boundary)
                this.report.unknownNodes++;
        }
    }

    private classifyNodes(): boolean {
        const total = this.nodeCounts[0]! * this.nodeCounts[1]! * this.nodeCounts[2]!;
        this.inside.clear();
        for (let n = 0; n < total; n++) this.inside.add(SolidBoundaryTessellator.OUTSIDE_NODE);
        const length = (this.nodeCounts[0]! - 1) * this.cellSize;
        const classification = new RaySolidClassification();
        for (let k = 1; k < this.nodeCounts[2]! - 1; k++) {
            for (let j = 1; j < this.nodeCounts[1]! - 1; j++) {
                const line = Ray.fromAffine(this.nodePoint(0, j, k), new Vector3Dd(1, 0, 0));
                const status = this.body.classifyRay(line, new RayCrossingQuery(0, length), classification);
                if (
                    status !== RaySolidQueryStatus.Supported ||
                    classification.insideSegments.size() !== classification.boundaries.size() + 1
                ) {
                    this.classifyNodesByPoints(j, k);
                    continue;
                }
                let segment = 0;
                for (let i = 1; i < this.nodeCounts[0]! - 1; i++) {
                    const t = i * this.cellSize;
                    while (segment < classification.boundaries.size() && classification.boundaries.get(segment).t < t)
                        segment++;
                    if (classification.insideSegments.get(segment))
                        this.inside.set(this.nodeIndex(i, j, k), SolidBoundaryTessellator.INSIDE_NODE);
                }
            }
        }
        return true;
    }

    // `point` and `normal` are one-element output holders.
    private firstHit(
        from: Vector3Dd,
        direction: Vector3Dd,
        reach: number,
        point: Vector3Dd[],
        normal: Vector3Dd[],
    ): boolean {
        const ray = new Ray(from, direction);
        const candidate = new RayHit();
        candidate.setStoreRay(false);
        candidate.resetForDistanceOnly();
        if (!this.body.doIntersectionFirstHit(ray, candidate)) return false;
        const distance = candidate.getHitDistance();
        if (!(distance >= 0) || distance > reach) return false;
        const hitRay = ray.withT(distance);
        const hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL);
        hit.setRay(hitRay);
        this.body.doExtraInformation(hitRay, distance, hit);
        point[0] = from.add(ray.getDirection().multiply(distance));
        normal[0] = hit.normal;
        const size = normal[0].length();
        if (!(size > 0) || !Double.isFinite(size)) return false;
        normal[0] = normal[0].multiply(1.0 / size);
        return true;
    }

    // Crossing on the edge from node (i, j, k) to its neighbour along the axis.
    private crossing(axis: number, i: number, j: number, k: number): number {
        const edge = this.nodeIndex(i, j, k);
        if (this.edgeCrossings[axis]!.get(edge) >= 0) return this.edgeCrossings[axis]!.get(edge);
        let ni = i;
        let nj = j;
        let nk = k;
        if (axis === 0) ni++;
        else if (axis === 1) nj++;
        else nk++;
        const firstInside = this.inside.get(edge) === SolidBoundaryTessellator.INSIDE_NODE;
        const a = this.nodePoint(i, j, k);
        const b = this.nodePoint(ni, nj, nk);
        const outer = firstInside ? b : a;
        const inner = firstInside ? a : b;
        const outerIsBorder = firstInside ? this.isBorderNode(ni, nj, nk) : this.isBorderNode(i, j, k);
        const inward = inner.subtract(outer).multiply(1.0 / this.cellSize);
        const point = [new Vector3Dd()];
        const normal = [new Vector3Dd()];
        if (this.firstHit(outer, inward, this.cellSize * (1 + 1e-9), point, normal)) {
            if (normal[0]!.dotProduct(inward) > 0) normal[0] = normal[0]!.multiply(-1);
            this.report.exactCrossings++;
        } else if (outerIsBorder) {
            // The solid continues beyond the region: close it at the inner node.
            point[0] = inner;
            normal[0] = inward.multiply(-1);
            this.report.regionCrossings++;
        } else {
            point[0] = a.add(b).multiply(0.5);
            normal[0] = inward.multiply(-1);
            this.report.midpointCrossings++;
        }
        const index = this.crossingPoints.size();
        this.crossingPoints.add(point[0]!);
        this.crossingNormals.add(normal[0]!);
        this.edgeCrossings[axis]!.set(edge, index);
        return index;
    }

    private cellVertex(i: number, j: number, k: number): number {
        const cell = this.cellIndex(i, j, k);
        if (this.cellVertices.get(cell) >= 0) return this.cellVertices.get(cell);
        let sum = new Vector3Dd(0, 0, 0);
        let normalSum = new Vector3Dd(0, 0, 0);
        let count = 0;
        for (let axis = 0; axis < 3; axis++) {
            for (let a = 0; a < 2; a++) {
                for (let b = 0; b < 2; b++) {
                    let ei = i;
                    let ej = j;
                    let ek = k;
                    if (axis === 0) {
                        ej += a;
                        ek += b;
                    } else if (axis === 1) {
                        ek += a;
                        ei += b;
                    } else {
                        ei += a;
                        ej += b;
                    }
                    let fi = ei;
                    let fj = ej;
                    let fk = ek;
                    if (axis === 0) fi++;
                    else if (axis === 1) fj++;
                    else fk++;
                    if (this.inside.get(this.nodeIndex(ei, ej, ek)) === this.inside.get(this.nodeIndex(fi, fj, fk)))
                        continue;
                    const c = this.crossing(axis, ei, ej, ek);
                    sum = sum.add(this.crossingPoints.get(c));
                    normalSum = normalSum.add(this.crossingNormals.get(c));
                    count++;
                }
            }
        }
        let point = sum.multiply(1.0 / count);
        let normal = normalSum;
        const size = normal.length();
        let projected = false;
        if (size > 1e-12) {
            normal = normal.multiply(1.0 / size);
            const reach = this.cellSize * Math.sqrt(3.0);
            const from = point.add(normal.multiply(reach / 2));
            const hitPoint = [new Vector3Dd()];
            const hitNormal = [new Vector3Dd()];
            if (this.firstHit(from, normal.multiply(-1), reach, hitPoint, hitNormal)) {
                const low = this.nodePoint(i, j, k);
                const margin = 1e-9 * this.cellSize;
                const h = hitPoint[0]!;
                if (
                    h.x() >= low.x() - margin &&
                    h.x() <= low.x() + this.cellSize + margin &&
                    h.y() >= low.y() - margin &&
                    h.y() <= low.y() + this.cellSize + margin &&
                    h.z() >= low.z() - margin &&
                    h.z() <= low.z() + this.cellSize + margin
                ) {
                    if (hitNormal[0]!.dotProduct(normal) < 0) hitNormal[0] = hitNormal[0]!.multiply(-1);
                    point = h;
                    normal = hitNormal[0]!;
                    projected = true;
                }
            }
        } else normal = new Vector3Dd(0, 0, 1);
        if (projected) this.report.boundaryVertices++;
        else this.report.cellVertices++;
        const index = this.vertexPoints.size();
        this.vertexPoints.add(point);
        this.vertexNormals.add(normal);
        this.cellVertices.set(cell, index);
        return index;
    }

    // Every edge along the axis whose ends differ gives the quad of the four cells
    // around it, wound to face from the inside end to the outside end.
    private emitQuads(axis: number): void {
        const b = (axis + 1) % 3;
        const c = (axis + 2) % 3;
        const index = [0, 0, 0];
        for (index[2] = 0; index[2] < this.nodeCounts[2]!; index[2]++) {
            for (index[1] = 0; index[1] < this.nodeCounts[1]!; index[1]++) {
                for (index[0] = 0; index[0] < this.nodeCounts[0]!; index[0]++) {
                    if (index[axis]! + 1 >= this.nodeCounts[axis]!) continue;
                    const next = [index[0], index[1], index[2]];
                    next[axis]!++;
                    const here =
                        this.inside.get(this.nodeIndex(index[0], index[1], index[2])) ===
                        SolidBoundaryTessellator.INSIDE_NODE;
                    const there =
                        this.inside.get(this.nodeIndex(next[0]!, next[1]!, next[2]!)) ===
                        SolidBoundaryTessellator.INSIDE_NODE;
                    if (here === there) continue;
                    // Border nodes are outside, so both ends are interior across b and c.
                    const quad = [0, 0, 0, 0];
                    const offsets = [
                        [-1, -1],
                        [0, -1],
                        [0, 0],
                        [-1, 0],
                    ];
                    for (let q = 0; q < 4; q++) {
                        const cell = [index[0], index[1], index[2]];
                        cell[b]! += offsets[q]![0]!;
                        cell[c]! += offsets[q]![1]!;
                        quad[q] = this.cellVertex(cell[0]!, cell[1]!, cell[2]!);
                    }
                    // The (b, c) cycle above turns around +axis.
                    if (here) {
                        this.triangleIndices.add(quad[0]!);
                        this.triangleIndices.add(quad[1]!);
                        this.triangleIndices.add(quad[2]!);
                        this.triangleIndices.add(quad[0]!);
                        this.triangleIndices.add(quad[2]!);
                        this.triangleIndices.add(quad[3]!);
                    } else {
                        this.triangleIndices.add(quad[0]!);
                        this.triangleIndices.add(quad[2]!);
                        this.triangleIndices.add(quad[1]!);
                        this.triangleIndices.add(quad[0]!);
                        this.triangleIndices.add(quad[3]!);
                        this.triangleIndices.add(quad[2]!);
                    }
                }
            }
        }
    }

    private buildMesh(): TriangleMesh {
        const mesh = new TriangleMesh();
        const vertexCount = this.vertexPoints.size();
        const triangleCount = Math.trunc(this.triangleIndices.size() / 3);
        mesh.initVertexPositionsArray(vertexCount);
        mesh.initVertexNormalsArray();
        const positions = mesh.getVertexPositions()!;
        const normals = mesh.getVertexNormals()!;
        for (let v = 0; v < vertexCount; v++) {
            positions[v * 3] = this.vertexPoints.get(v).x();
            positions[v * 3 + 1] = this.vertexPoints.get(v).y();
            positions[v * 3 + 2] = this.vertexPoints.get(v).z();
            normals[v * 3] = this.vertexNormals.get(v).x();
            normals[v * 3 + 1] = this.vertexNormals.get(v).y();
            normals[v * 3 + 2] = this.vertexNormals.get(v).z();
        }
        mesh.initTriangleArrays(triangleCount);
        const indices = mesh.getTriangleIndexes()!;
        const faceNormals = mesh.getTriangleNormals()!;
        for (let t = 0; t < triangleCount; t++) {
            const v0 = this.triangleIndices.get(t * 3);
            const v1 = this.triangleIndices.get(t * 3 + 1);
            const v2 = this.triangleIndices.get(t * 3 + 2);
            indices[t * 3] = v0;
            indices[t * 3 + 1] = v1;
            indices[t * 3 + 2] = v2;
            let n = this.vertexPoints
                .get(v1)
                .subtract(this.vertexPoints.get(v0))
                .crossProduct(this.vertexPoints.get(v2).subtract(this.vertexPoints.get(v0)));
            const size = n.length();
            n = size > 0 ? n.multiply(1.0 / size) : this.vertexNormals.get(v0);
            faceNormals[t * 3] = n.x();
            faceNormals[t * 3 + 1] = n.y();
            faceNormals[t * 3 + 2] = n.z();
        }
        return mesh;
    }

    // Largest accepted resolution.
    public static readonly MAXIMUM_RESOLUTION = 512;

    // Returns a new mesh in world coordinates, or null when the region is not
    // finite and nonempty or the resolution is outside 1..MAXIMUM_RESOLUTION.
    // An empty mesh means no boundary was found.
    public static tessellate(
        body: SceneSolid,
        region: GeometryBounds,
        resolution: number,
        report: SolidBoundaryTessellationReport,
    ): TriangleMesh | null {
        report.clear();
        if (region.getKind() !== GeometryBounds.Kind.Finite) return null;
        if (resolution < 1 || resolution > SolidBoundaryTessellator.MAXIMUM_RESOLUTION) return null;
        const low = region.getMinimum();
        const high = region.getMaximum();
        const extent = [high.x() - low.x(), high.y() - low.y(), high.z() - low.z()];
        let longest = 0;
        for (let a = 0; a < 3; a++) {
            if (!Double.isFinite(extent[a]!) || extent[a]! < 0) return null;
            if (extent[a]! > longest) longest = extent[a]!;
        }
        if (!(longest > 0)) return null;

        const tessellator = new SolidBoundaryTessellator(body, report);
        tessellator.cellSize = longest / resolution;
        // Cells covering the region, plus one layer of outside nodes on each side.
        for (let a = 0; a < 3; a++) {
            let cells = Math.ceil(extent[a]! / tessellator.cellSize - 1e-9);
            if (cells < 1) cells = 1;
            tessellator.nodeCounts[a] = cells + 3;
            report.nodeCounts[a] = tessellator.nodeCounts[a]!;
        }
        report.cellSize = tessellator.cellSize;
        tessellator.origin = new Vector3Dd(
            low.x() - tessellator.cellSize,
            low.y() - tessellator.cellSize,
            low.z() - tessellator.cellSize,
        );
        tessellator.classifyNodes();

        const nodes = tessellator.nodeCounts[0]! * tessellator.nodeCounts[1]! * tessellator.nodeCounts[2]!;
        for (let a = 0; a < 3; a++) {
            for (let n = 0; n < nodes; n++) tessellator.edgeCrossings[a]!.add(-1);
        }
        const cells =
            (tessellator.nodeCounts[0]! - 1) * (tessellator.nodeCounts[1]! - 1) * (tessellator.nodeCounts[2]! - 1);
        for (let c = 0; c < cells; c++) tessellator.cellVertices.add(-1);
        for (let a = 0; a < 3; a++) tessellator.emitQuads(a);
        report.triangles = Math.trunc(tessellator.triangleIndices.size() / 3);
        return tessellator.buildMesh();
    }
}
