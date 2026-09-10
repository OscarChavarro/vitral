import { describe, expect, it } from "vitest";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Ray } from "vsdk/toolkit/environment/geometry/element/Ray.js";
import { RayHit } from "vsdk/toolkit/environment/geometry/element/RayHit.js";
import { SurfaceRayIntersection } from "vsdk/toolkit/environment/geometry/geometricProcessing/SurfaceRayIntersection.js";
import { GeometryTriangulator } from "vsdk/toolkit/environment/geometry/geometricProcessing/GeometryTriangulator.js";
import { TriangleMeshVoxelization } from "vsdk/toolkit/environment/geometry/geometricProcessing/TriangleMeshVoxelization.js";
import { Triangle } from "vsdk/toolkit/environment/geometry/element/Triangle.js";
import { TriangleMesh } from "vsdk/toolkit/environment/geometry/surface/TriangleMesh.js";
import { TriangleMeshGroup } from "vsdk/toolkit/environment/geometry/surface/TriangleMeshGroup.js";
import { Voxelization } from "vsdk/toolkit/environment/geometry/geometricProcessing/Voxelization.js";
import { Sphere } from "vsdk/toolkit/environment/geometry/volume/Sphere.js";
import { VoxelVolume } from "vsdk/toolkit/environment/geometry/volume/VoxelVolume.js";

function unitTriangleMesh(): TriangleMesh {
    const mesh = new TriangleMesh();
    mesh.initVertexPositionsArray(3);
    const positions = mesh.getVertexPositions()!;
    positions.set([-1, -1, 0, 1, -1, 0, -1, 1, 0]);
    mesh.setTriangles([new Triangle(0, 1, 2)]);
    return mesh;
}

describe("Voxelization", () => {
    it("marks contained voxels through the generic containment path", () => {
        const volume = new VoxelVolume();
        volume.init(7, 7, 7);
        Voxelization.doVoxelization(new Sphere(1), volume, new Matrix4x4d(), null);
        expect(volume.getVoxel(3, 3, 3)).toBe(255);
        expect(volume.getVoxel(0, 0, 0)).toBe(0);
    });

    it("rasterizes a triangle mesh in voxel coordinates", () => {
        const volume = new VoxelVolume();
        volume.init(9, 9, 9);
        TriangleMeshVoxelization.doVoxelization(unitTriangleMesh(), volume, new Matrix4x4d(), null);
        expect(volume.getVoxel(1, 1, 4)).toBe(255);
        // The legacy Triangle.containmentTest independently clamps both
        // barycentric coordinates, so the voxelizer fills this bounding-box
        // corner as well; retain Java's exact rasterization semantics.
        expect(volume.getVoxel(8, 8, 4)).toBe(255);
        expect(volume.getVoxel(1, 1, 0)).toBe(0);
    });

    it("dispatches mesh geometry and preserves triangulator identity rules", () => {
        const mesh = unitTriangleMesh();
        const volume = new VoxelVolume();
        volume.init(9, 9, 9);
        Voxelization.doVoxelization(mesh, volume, new Matrix4x4d(), null);
        expect(volume.getVoxel(1, 1, 4)).toBe(255);
        expect(GeometryTriangulator.exportToTriangleMeshGroup(mesh).getMeshAt(0)).toBe(mesh);
        const group = new TriangleMeshGroup([mesh]);
        expect(GeometryTriangulator.exportToTriangleMeshGroup(group)).toBe(group);
        expect(GeometryTriangulator.exportToTriangleMeshGroup(new Sphere(1))).toBeNull();
    });

    it("uses the surface bounding-box fast rejection without changing a valid mesh hit", () => {
        const mesh = unitTriangleMesh();
        const hit = new RayHit();
        expect(
            SurfaceRayIntersection.doIntersectionFirstHit(
                mesh,
                new Ray(new Vector3Dd(0, 0, 2), new Vector3Dd(0, 0, -1)),
                hit,
            ),
        ).toBe(true);
        expect(hit.hitDistance()).toBeCloseTo(2);
        expect(
            SurfaceRayIntersection.doIntersectionFirstHit(
                mesh,
                new Ray(new Vector3Dd(3, 3, 2), new Vector3Dd(0, 0, -1)),
                new RayHit(),
            ),
        ).toBe(false);
    });
});
