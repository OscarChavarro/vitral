// Port of cpp/base/src/test/triangle_mesh_contract_tests.cpp
import { describe, expect, it } from "vitest";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { GeometryBounds } from "vsdk/toolkit/environment/geometry/element/GeometryBounds.js";
import { Ray } from "vsdk/toolkit/environment/geometry/element/Ray.js";
import { RayBoundary } from "vsdk/toolkit/environment/geometry/element/RayBoundary.js";
import { RayCrossingQuery } from "vsdk/toolkit/environment/geometry/element/RayCrossingQuery.js";
import { RayCrossingSupport } from "vsdk/toolkit/environment/geometry/element/RayCrossingSupport.js";
import { RayFirstHitStatus } from "vsdk/toolkit/environment/geometry/element/RayFirstHitStatus.js";
import { RayHit } from "vsdk/toolkit/environment/geometry/element/RayHit.js";
import { Triangle } from "vsdk/toolkit/environment/geometry/element/Triangle.js";
import { TriangleMesh } from "vsdk/toolkit/environment/geometry/surface/TriangleMesh.js";
import { SimpleMaterial } from "vsdk/toolkit/environment/material/SimpleMaterial.js";
import { RayCrossingTestCollector } from "../../../../../RayCrossingTestCollector.js";

describe("TriangleMeshContract", () => {
    it("OrderedCrossingsCarryFaceAndFirstHitDetails", () => {
        const mesh = new TriangleMesh();
        mesh.initVertexPositionsArray(6);
        const positions = mesh.getVertexPositions()!;
        const values = [0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 2, 1, 0, 2, 0, 1, 2];
        for (let i = 0; i < 18; ++i) positions[i] = values[i]!;
        mesh.initTriangleArrays(2);
        mesh.setTriangleAt(0, new Triangle(0, 1, 2));
        mesh.setTriangleAt(1, new Triangle(3, 4, 5));
        mesh.initVertexUvsArray();
        const uv = mesh.getVertexUvs()!;
        uv[6] = 0;
        uv[7] = 0;
        uv[8] = 1;
        uv[9] = 0;
        uv[10] = 0;
        uv[11] = 1;
        const ray = Ray.fromAffine(new Vector3Dd(0.25, 0.25, 4), new Vector3Dd(0, 0, -1));
        const query = new RayCrossingQuery(2, 4);
        const events = new RayCrossingTestCollector();
        expect(mesh.emitRayCrossings(ray, query, events)).toBe(RayCrossingSupport.Supported);
        expect(events.roots.size()).toBe(2);
        expect(events.roots.get(0)).toBe(2);
        expect(events.roots.get(1)).toBe(4);
        expect(events.surfaceIndices.get(0)).toBe(1);
        expect(events.surfaceIndices.get(1)).toBe(0);
        expect(events.geometries.get(0)).toBe(mesh);
        const hit = new RayHit(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL | RayHit.DETAIL_UV, false);
        const boundary = new RayBoundary();
        expect(mesh.queryFirstHit(ray, query, hit, boundary)).toBe(RayFirstHitStatus.Hit);
        expect(hit.getHitDistance()).toBe(2);
        expect(hit.point.z()).toBe(2);
        expect(hit.normal.z()).toBe(1);
        expect(hit.u).toBe(0.25);
        expect(hit.v).toBe(0.25);
        expect(boundary.surfaceIndex).toBe(1);
        expect(boundary.geometry).toBe(mesh);
        expect(hit.getAvailableDetailMask()).toBe(RayHit.DETAIL_POINT | RayHit.DETAIL_NORMAL | RayHit.DETAIL_UV);
        const copied = new RayBoundary(boundary);
        expect(copied.surfaceIndex).toBe(1);
        const assigned = new RayBoundary();
        assigned.assign(copied);
        expect(assigned.surfaceIndex).toBe(1);
        const deferred = new RayHit(RayHit.DETAIL_NORMAL | RayHit.DETAIL_UV, false);
        expect(mesh.querySurfaceDetail(ray, boundary, deferred)).toBe(RayFirstHitStatus.Hit);
        expect(deferred.normal.z()).toBe(1);
        expect(deferred.u).toBe(0.25);
        expect(deferred.v).toBe(0.25);
        boundary.normalReversed = true;
        expect(mesh.querySurfaceDetail(ray, boundary, deferred)).toBe(RayFirstHitStatus.Hit);
        expect(deferred.normal.z()).toBe(-1);
        boundary.surfaceIndex = 99;
        expect(mesh.querySurfaceDetail(ray, boundary, deferred)).toBe(RayFirstHitStatus.InvalidInput);
        expect(deferred.hasHitDistance()).toBe(false);
        expect(mesh.queryFirstHit(ray, new RayCrossingQuery(2.1, 3.9), hit, boundary)).toBe(RayFirstHitStatus.Miss);
        expect(hit.hasHitDistance()).toBe(false);
        expect(boundary.surfaceIndex).toBe(-1);
    });

    it("CoplanarityInvalidMeshAndEmptyAreExplicit", () => {
        const mesh = new TriangleMesh();
        mesh.initVertexPositionsArray(6);
        const positions = mesh.getVertexPositions()!;
        const values = [0, 0, 0, 1, 0, 0, 0, 1, 0, 0.25, 0, 0, 0.25, 1, 0, 0.25, 0, 1];
        for (let i = 0; i < 18; ++i) positions[i] = values[i]!;
        mesh.initTriangleArrays(2);
        mesh.setTriangleAt(0, new Triangle(0, 1, 2));
        mesh.setTriangleAt(1, new Triangle(3, 4, 5));
        const ray = Ray.fromAffine(new Vector3Dd(0.25, 0.25, 2), new Vector3Dd(0, 0, -1));
        const events = new RayCrossingTestCollector();
        expect(mesh.emitRayCrossings(ray, new RayCrossingQuery(0, 4), events)).toBe(RayCrossingSupport.Indeterminate);
        expect(events.roots.isEmpty()).toBe(true);
        expect(mesh.queryFirstHit(ray, new RayCrossingQuery(0, 4), null)).toBe(RayFirstHitStatus.Indeterminate);
        mesh.getTriangleIndexes()![4] = 999;
        expect(mesh.emitRayCrossings(ray, new RayCrossingQuery(0, 4), events)).toBe(RayCrossingSupport.Indeterminate);
        const bounds = new GeometryBounds();
        mesh.getBounds(bounds);
        expect(bounds.getKind()).toBe(GeometryBounds.Kind.Indeterminate);
        const empty = new TriangleMesh();
        expect(empty.emitRayCrossings(ray, new RayCrossingQuery(0, 4), events)).toBe(RayCrossingSupport.Supported);
        expect(
            empty.queryFirstHit(Ray.fromAffine(new Vector3Dd(), new Vector3Dd()), new RayCrossingQuery(0, 4), null),
        ).toBe(RayFirstHitStatus.InvalidInput);
        expect(empty.queryFirstHit(ray, new RayCrossingQuery(0, 4, 0.01), null)).toBe(RayFirstHitStatus.Unsupported);
    });

    // setOwnsMaterials and the scoped destruction of the original are C++
    // lifetime management with no TypeScript counterpart.
    it("MathematicalCopyDoesNotBorrowOwnedAppearance", () => {
        let copied: TriangleMesh | null = null;
        {
            const original = new TriangleMesh();
            original.initVertexPositionsArray(3);
            const positions = original.getVertexPositions()!;
            const values = [0, 0, 0, 1, 0, 0, 0, 1, 0];
            for (let i = 0; i < 9; ++i) positions[i] = values[i]!;
            original.initTriangleArrays(1);
            original.setTriangleAt(0, new Triangle(0, 1, 2));
            const materials: SimpleMaterial[] = [];
            materials.push(new SimpleMaterial());
            original.setMaterials(materials);
            const copy = original.copyGeometry();
            copied = copy instanceof TriangleMesh ? copy : null;
            expect(copied).not.toBeNull();
            expect(copied!.getMaterials()!.length).toBe(0);
        }
        const boundary = new RayBoundary();
        expect(
            copied!.queryFirstHit(
                Ray.fromAffine(new Vector3Dd(0.25, 0.25, 1), new Vector3Dd(0, 0, -1)),
                new RayCrossingQuery(0, 2),
                null,
                boundary,
            ),
        ).toBe(RayFirstHitStatus.Hit);
        expect(boundary.geometry).toBe(copied);
    });
});
