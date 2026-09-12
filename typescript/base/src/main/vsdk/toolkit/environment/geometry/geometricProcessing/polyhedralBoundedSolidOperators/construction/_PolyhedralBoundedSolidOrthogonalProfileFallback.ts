import { Vector3Dd } from "../../../../../common/linealAlgebra/Vector3Dd.js";
import { PolyhedralBoundedSolid } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { _PolyhedralBoundedSolidEdge } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.js";
import { _PolyhedralBoundedSolidFace } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import { _PolyhedralBoundedSolidHalfEdge } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import { _PolyhedralBoundedSolidLoop } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";
import { _PolyhedralBoundedSolidVertex } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";
import { _PolyhedralBoundedSolidOperator } from "../_PolyhedralBoundedSolidOperator.js";
import { _PolyhedralBoundedSolidAxisAlignedCellFallback } from "../fallbacks/_PolyhedralBoundedSolidAxisAlignedCellFallback.js";
import { _PolyhedralBoundedSolidFallbackGeometry } from "../fallbacks/_PolyhedralBoundedSolidFallbackGeometry.js";

class OrthogonalProfileOperandSpec {
    public constructor(
        private readonly type: number,
        private readonly bounds: number[],
        private readonly yzProfile: Vector3Dd[] | null,
        private readonly rightBoundaryZ: number[] | null,
        private readonly rightBoundaryX: number[] | null,
    ) {}
    private contains(x: number, y: number, z: number): boolean {
        const epsilon = _PolyhedralBoundedSolidOrthogonalProfileFallback["numericContext"].bigEpsilon();
        if (this.type === _PolyhedralBoundedSolidOrthogonalProfileFallback["PROFILE_X_EXTRUDED_YZ"])
            return (
                x >= this.bounds[0]! - epsilon &&
                x <= this.bounds[3]! + epsilon &&
                _PolyhedralBoundedSolidOrthogonalProfileFallback["pointInsideYZProfile"](this.yzProfile, y, z)
            );
        return (
            y >= this.bounds[1]! - epsilon &&
            y <= this.bounds[4]! + epsilon &&
            z >= this.bounds[2]! - epsilon &&
            z <= this.bounds[5]! + epsilon &&
            x >= this.bounds[0]! - epsilon &&
            x <= this.rightXAtZ(z) + epsilon
        );
    }
    private rightXAtZ(z: number): number {
        const epsilon = _PolyhedralBoundedSolidOrthogonalProfileFallback["numericContext"].bigEpsilon();
        if (this.rightBoundaryZ === null || this.rightBoundaryZ.length === 0) return this.bounds[3]!;
        if (z <= this.rightBoundaryZ[0]! + epsilon) return this.rightBoundaryX![0]!;
        for (let i = 0; i < this.rightBoundaryZ.length - 1; i++) {
            const z0 = this.rightBoundaryZ[i]!,
                z1 = this.rightBoundaryZ[i + 1]!;
            const x0 = this.rightBoundaryX![i]!,
                x1 = this.rightBoundaryX![i + 1]!;
            if (z <= z1 + epsilon) {
                if (_PolyhedralBoundedSolidFallbackGeometry.sameCoordinate(z0, z1)) return x0;
                const t = (z - z0) / (z1 - z0);
                return x0 + (x1 - x0) * t;
            }
        }
        return this.rightBoundaryX![this.rightBoundaryX!.length - 1]!;
    }
}

class OrthogonalProfileBooleanFallbackSpec {
    public constructor(
        private readonly operandA: OrthogonalProfileOperandSpec,
        private readonly operandB: OrthogonalProfileOperandSpec,
        private readonly yExtruded: OrthogonalProfileOperandSpec,
        private readonly xMin: number,
        private readonly xMax: number,
        private readonly ys: number[],
        private readonly zs: number[],
    ) {}
    private xAtBoundary(boundary: number, z: number): number {
        if (boundary === 0) return this.xMin;
        if (boundary === 1) return this.yExtruded["rightXAtZ"](z);
        return this.xMax;
    }
    private point(boundary: number, iy: number, iz: number): Vector3Dd {
        const z = this.zs[iz]!;
        return new Vector3Dd(this.xAtBoundary(boundary, z), this.ys[iy]!, z);
    }
}

class ProfileCellBooleanBuilder {
    private readonly solid = new PolyhedralBoundedSolid();
    private readonly vertices = new Map<string, _PolyhedralBoundedSolidVertex>();
    private readonly edges = new Map<string, _PolyhedralBoundedSolidEdge>();
    private nextVertexId = 1;
    private nextFaceId = 1;
    private coordinateKey(value: number): number {
        return Math.round(value * 1000000000000);
    }
    private vertexKey(point: Vector3Dd): string {
        return `${this.coordinateKey(point.x())}:${this.coordinateKey(point.y())}:${this.coordinateKey(point.z())}`;
    }
    private vertexAt(point: Vector3Dd): _PolyhedralBoundedSolidVertex {
        const key = this.vertexKey(point);
        let vertex = this.vertices.get(key);
        if (vertex !== undefined) return vertex;
        vertex = new _PolyhedralBoundedSolidVertex(this.solid, point, this.nextVertexId);
        this.solid.setMaxVertexId(this.nextVertexId++);
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
    private degenerateQuad(corners: Vector3Dd[]): boolean {
        for (let i = 0; i < corners.length; i++)
            if (
                _PolyhedralBoundedSolidFallbackGeometry.sameProfilePoint(
                    corners[i]!,
                    corners[(i + 1) % corners.length]!,
                )
            )
                return true;
        return false;
    }
    private addQuad(corners: Vector3Dd[]): void {
        if (corners === null || corners.length < 3 || this.degenerateQuad(corners)) return;
        const face = new _PolyhedralBoundedSolidFace(this.solid, this.nextFaceId);
        this.solid.setMaxFaceId(this.nextFaceId++);
        const loop = new _PolyhedralBoundedSolidLoop(face);
        const halfEdges: _PolyhedralBoundedSolidHalfEdge[] = [],
            faceVertices: _PolyhedralBoundedSolidVertex[] = [];
        for (let i = 0; i < corners.length; i++) {
            const vertex = this.vertexAt(corners[i]!);
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

/** Literal cell reconstruction fallback for orthogonal extruded profiles. */
export class _PolyhedralBoundedSolidOrthogonalProfileFallback extends _PolyhedralBoundedSolidOperator {
    private static readonly PROFILE_X_EXTRUDED_YZ = 0;
    private static readonly PROFILE_Y_EXTRUDED_XZ = 1;
    private static pointInsideYZProfile(profile: Vector3Dd[] | null, y: number, z: number): boolean {
        if (profile === null || profile.length < 3) return false;
        let inside = false,
            j = profile.length - 1;
        for (let i = 0; i < profile.length; i++) {
            const yi = profile[i]!.y(),
                zi = profile[i]!.z(),
                yj = profile[j]!.y(),
                zj = profile[j]!.z();
            if (zi > z !== zj > z && y < ((yj - yi) * (z - zi)) / (zj - zi) + yi) inside = !inside;
            j = i;
        }
        return inside;
    }
    private static createXExtrudedYZSpec(
        solid: PolyhedralBoundedSolid,
        xs: number[],
        ys: number[],
        zs: number[],
    ): OrthogonalProfileOperandSpec | null {
        if (xs.length !== 2 || ys.length !== 4 || zs.length !== 3 || solid.getVerticesList().size() !== 16) return null;
        const bounds = Array.from(solid.getMinMax());
        let profile = _PolyhedralBoundedSolidFallbackGeometry.extractProfileAtX(solid, bounds[0]!);
        if (profile === null || profile.length < 3)
            profile = _PolyhedralBoundedSolidFallbackGeometry.extractProfileAtX(solid, bounds[3]!);
        return profile === null || profile.length < 3
            ? null
            : new OrthogonalProfileOperandSpec(this.PROFILE_X_EXTRUDED_YZ, bounds, profile, null, null);
    }
    private static createYExtrudedXZSpec(
        solid: PolyhedralBoundedSolid,
        xs: number[],
        ys: number[],
        zs: number[],
    ): OrthogonalProfileOperandSpec | null {
        if (xs.length !== 3 || ys.length !== 2 || zs.length !== 3 || solid.getVerticesList().size() !== 10) return null;
        const bounds = Array.from(solid.getMinMax()),
            rightZ: number[] = [],
            rightX: number[] = [];
        for (let i = 0; i < zs.length; i++) {
            const z = zs[i]!;
            let maxX = -Number.MAX_VALUE;
            for (let j = 0; j < solid.getVerticesList().size(); j++) {
                const p = solid.getVerticesList().get(j)!.position;
                if (_PolyhedralBoundedSolidFallbackGeometry.sameCoordinate(p.z(), z) && p.x() > maxX) maxX = p.x();
            }
            if (maxX <= -Number.MAX_VALUE / 2) return null;
            rightZ.push(z);
            rightX.push(maxX);
        }
        return new OrthogonalProfileOperandSpec(this.PROFILE_Y_EXTRUDED_XZ, bounds, null, rightZ, rightX);
    }
    private static createOrthogonalProfileSpec(solid: PolyhedralBoundedSolid): OrthogonalProfileOperandSpec | null {
        const xs = _PolyhedralBoundedSolidFallbackGeometry.uniqueVertexCoordinates(solid, 0);
        const ys = _PolyhedralBoundedSolidFallbackGeometry.uniqueVertexCoordinates(solid, 1);
        const zs = _PolyhedralBoundedSolidFallbackGeometry.uniqueVertexCoordinates(solid, 2);
        const spec = this.createYExtrudedXZSpec(solid, xs, ys, zs);
        return spec ?? this.createXExtrudedYZSpec(solid, xs, ys, zs);
    }
    private static prepareOrthogonalProfileBooleanFallbackSpec(
        solidA: PolyhedralBoundedSolid,
        solidB: PolyhedralBoundedSolid,
    ): OrthogonalProfileBooleanFallbackSpec | null {
        const specA = this.createOrthogonalProfileSpec(solidA),
            specB = this.createOrthogonalProfileSpec(solidB);
        if (specA === null || specB === null || specA["type"] === specB["type"]) return null;
        const yExtruded = specA["type"] === this.PROFILE_Y_EXTRUDED_XZ ? specA : specB;
        const xExtruded = specA["type"] === this.PROFILE_X_EXTRUDED_YZ ? specA : specB;
        const yb = yExtruded["bounds"],
            xb = xExtruded["bounds"];
        if (
            !_PolyhedralBoundedSolidFallbackGeometry.sameCoordinate(yb[0]!, xb[0]!) ||
            !_PolyhedralBoundedSolidFallbackGeometry.sameCoordinate(yb[1]!, xb[1]!) ||
            !_PolyhedralBoundedSolidFallbackGeometry.sameCoordinate(yb[2]!, xb[2]!) ||
            !_PolyhedralBoundedSolidFallbackGeometry.sameCoordinate(yb[4]!, xb[4]!) ||
            !_PolyhedralBoundedSolidFallbackGeometry.sameCoordinate(yb[5]!, xb[5]!) ||
            yb[3]! >= xb[3]! - this.numericContext.bigEpsilon()
        )
            return null;
        const ys = _PolyhedralBoundedSolidFallbackGeometry.uniqueVertexCoordinates(solidA, 1),
            zs = _PolyhedralBoundedSolidFallbackGeometry.uniqueVertexCoordinates(solidA, 2);
        for (let i = 0; i < solidB.getVerticesList().size(); i++) {
            const p = solidB.getVerticesList().get(i)!.position;
            _PolyhedralBoundedSolidFallbackGeometry.addUniqueCoordinate(ys, p.y());
            _PolyhedralBoundedSolidFallbackGeometry.addUniqueCoordinate(zs, p.z());
        }
        if (ys.length < 2 || zs.length < 2 || ys.length > 8 || zs.length > 8) return null;
        return new OrthogonalProfileBooleanFallbackSpec(specA, specB, yExtruded, xb[0]!, xb[3]!, ys, zs);
    }
    private static profileCellSelected(
        spec: OrthogonalProfileBooleanFallbackSpec,
        operation: number,
        zone: number,
        iy: number,
        iz: number,
    ): boolean {
        const ys = spec["ys"],
            zs = spec["zs"];
        const y = (ys[iy]! + ys[iy + 1]!) * 0.5,
            z = (zs[iz]! + zs[iz + 1]!) * 0.5;
        const x0 = spec["xAtBoundary"](zone, z),
            x1 = spec["xAtBoundary"](zone + 1, z);
        if (x1 <= x0 + this.numericContext.bigEpsilon()) return false;
        const x = (x0 + x1) * 0.5;
        return _PolyhedralBoundedSolidAxisAlignedCellFallback.axisAlignedCellSelected(
            spec["operandA"]["contains"](x, y, z),
            spec["operandB"]["contains"](x, y, z),
            operation,
        );
    }
    private static addProfileBoundaryQuad(
        builder: ProfileCellBooleanBuilder,
        spec: OrthogonalProfileBooleanFallbackSpec,
        zone: number,
        iy: number,
        iz: number,
        side: number,
    ): void {
        const left = zone,
            right = zone + 1,
            p = (b: number, y: number, z: number) => spec["point"](b, y, z);
        if (side === 0)
            builder["addQuad"]([p(left, iy, iz), p(left, iy, iz + 1), p(left, iy + 1, iz + 1), p(left, iy + 1, iz)]);
        else if (side === 1)
            builder["addQuad"]([
                p(right, iy, iz),
                p(right, iy + 1, iz),
                p(right, iy + 1, iz + 1),
                p(right, iy, iz + 1),
            ]);
        else if (side === 2)
            builder["addQuad"]([p(left, iy, iz), p(right, iy, iz), p(right, iy, iz + 1), p(left, iy, iz + 1)]);
        else if (side === 3)
            builder["addQuad"]([
                p(left, iy + 1, iz),
                p(left, iy + 1, iz + 1),
                p(right, iy + 1, iz + 1),
                p(right, iy + 1, iz),
            ]);
        else if (side === 4)
            builder["addQuad"]([p(left, iy, iz), p(left, iy + 1, iz), p(right, iy + 1, iz), p(right, iy, iz)]);
        else
            builder["addQuad"]([
                p(left, iy, iz + 1),
                p(right, iy, iz + 1),
                p(right, iy + 1, iz + 1),
                p(left, iy + 1, iz + 1),
            ]);
    }
    public static buildOrthogonalProfileBooleanFallback(
        solidA: PolyhedralBoundedSolid,
        solidB: PolyhedralBoundedSolid,
        operation: number,
    ): PolyhedralBoundedSolid | null {
        const spec = this.prepareOrthogonalProfileBooleanFallbackSpec(solidA, solidB);
        if (spec === null) return null;
        const ys = spec["ys"],
            zs = spec["zs"];
        const occupied = Array.from({ length: 2 }, () =>
            Array.from({ length: ys.length - 1 }, () => Array<boolean>(zs.length - 1).fill(false)),
        );
        for (let zone = 0; zone < 2; zone++)
            for (let iy = 0; iy < ys.length - 1; iy++)
                for (let iz = 0; iz < zs.length - 1; iz++)
                    occupied[zone]![iy]![iz] = this.profileCellSelected(spec, operation, zone, iy, iz);
        const builder = new ProfileCellBooleanBuilder();
        for (let zone = 0; zone < 2; zone++)
            for (let iy = 0; iy < ys.length - 1; iy++)
                for (let iz = 0; iz < zs.length - 1; iz++) {
                    if (!occupied[zone]![iy]![iz]) continue;
                    if (zone === 0 || !occupied[zone - 1]![iy]![iz])
                        this.addProfileBoundaryQuad(builder, spec, zone, iy, iz, 0);
                    if (zone === 1 || !occupied[zone + 1]![iy]![iz])
                        this.addProfileBoundaryQuad(builder, spec, zone, iy, iz, 1);
                    if (iy === 0 || !occupied[zone]![iy - 1]![iz])
                        this.addProfileBoundaryQuad(builder, spec, zone, iy, iz, 2);
                    if (iy === ys.length - 2 || !occupied[zone]![iy + 1]![iz])
                        this.addProfileBoundaryQuad(builder, spec, zone, iy, iz, 3);
                    if (iz === 0 || !occupied[zone]![iy]![iz - 1])
                        this.addProfileBoundaryQuad(builder, spec, zone, iy, iz, 4);
                    if (iz === zs.length - 2 || !occupied[zone]![iy]![iz + 1])
                        this.addProfileBoundaryQuad(builder, spec, zone, iy, iz, 5);
                }
        return builder["result"]();
    }
}
