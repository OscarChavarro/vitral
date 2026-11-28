// Port of java/base/src/test/vsdk/toolkit/environment/geometry/volume/VoxelVolumeTest.java
import { describe, expect, it } from "vitest";

import { Random } from "java/util/Random.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Ray } from "vsdk/toolkit/environment/geometry/element/Ray.js";
import { RayHit } from "vsdk/toolkit/environment/geometry/element/RayHit.js";
import { VoxelVolume } from "vsdk/toolkit/environment/geometry/volume/VoxelVolume.js";

/** AssertJ `isCloseTo(expected, offset(tolerance))`. */
function expectCloseTo(actual: number, expected: number, tolerance: number, message?: string): void {
    expect(Math.abs(actual - expected), message).toBeLessThanOrEqual(tolerance);
}

/**
Exercises the intersection of rays with the filled voxels of a volume (the
cube <-1, -1, -1>-<1, 1, 1>), traversed voxel by voxel.
 */
describe("VoxelVolumeTest", () => {
    const EPS = 1.0e-9;
    const FILLED = -1; // Java (byte)-1

    function createVolume(n: number): VoxelVolume {
        const volume = new VoxelVolume();
        volume.init(n, n, n);
        return volume;
    }

    it("given_oneFilledVoxel_when_rayEntersItsTopFace_then_hitsTheFaceWithItsNormal", () => {
        // Arrange: in a 4^3 volume the voxel (1, 1, 1) spans [-0.5, 0]^3
        const volume = createVolume(4);
        volume.putVoxel(1, 1, 1, FILLED);
        const ray = new Ray(new Vector3Dd(-0.25, -0.25, 5), new Vector3Dd(0, 0, -1));
        const hit = new RayHit(RayHit.DETAIL_ALL, true);

        // Act
        const intersects = volume.doIntersectionFirstHit(ray, hit);

        // Assert
        expect(intersects).toBe(true);
        expectCloseTo(hit.getRay()!.getT(), 5.0, EPS);
        expectCloseTo(hit.normal.x(), 0.0, EPS);
        expectCloseTo(hit.normal.y(), 0.0, EPS);
        expectCloseTo(hit.normal.z(), 1.0, EPS);
        expectCloseTo(hit.point.z(), 0.0, EPS);
    });

    it("given_oneFilledVoxel_when_rayPassesBesideIt_then_missesTheVolume", () => {
        // Arrange
        const volume = createVolume(4);
        volume.putVoxel(1, 1, 1, FILLED);
        const ray = new Ray(new Vector3Dd(0.25, -0.25, 5), new Vector3Dd(0, 0, -1));

        // Act & Assert
        expect(volume.doIntersectionFirstHit(ray, new RayHit())).toBe(false);
    });

    it("given_fullVolume_when_rayComesFromOutside_then_hitsTheFaceOfTheCube", () => {
        // Arrange
        const volume = createVolume(8);
        fill(volume);
        const ray = new Ray(new Vector3Dd(-5, 0.3, -0.2), new Vector3Dd(1, 0, 0));
        const hit = new RayHit(RayHit.DETAIL_ALL, true);

        // Act
        const intersects = volume.doIntersectionFirstHit(ray, hit);

        // Assert
        expect(intersects).toBe(true);
        expectCloseTo(hit.getRay()!.getT(), 4.0, EPS);
        expectCloseTo(hit.normal.x(), -1.0, EPS);
    });

    it("given_rayStartingInsideFilledVoxels_when_intersected_then_onlyEnteredVoxelsAreHit", () => {
        // Arrange: two filled voxels of the row y = z = 0 of a 4^3 volume,
        // x = 0 spans [-1, -0.5] and x = 3 spans [0.5, 1]
        const volume = createVolume(4);
        volume.putVoxel(0, 0, 0, FILLED);
        volume.putVoxel(3, 0, 0, FILLED);
        const ray = new Ray(new Vector3Dd(-0.75, -0.75, -0.75), new Vector3Dd(1, 0, 0));
        const hit = new RayHit(RayHit.DETAIL_ALL, true);

        // Act
        const intersects = volume.doIntersectionFirstHit(ray, hit);

        // Assert
        expect(intersects).toBe(true);
        expectCloseTo(hit.getRay()!.getT(), 1.25, EPS);
        expectCloseTo(hit.normal.x(), -1.0, EPS);
    });

    it("given_volumeBelowThreshold_when_intersected_then_missesTheVolume", () => {
        // Arrange
        const volume = createVolume(4);
        volume.putVoxel(1, 1, 1, 100);
        const ray = new Ray(new Vector3Dd(-0.25, -0.25, 5), new Vector3Dd(0, 0, -1));

        // Act & Assert
        expect(volume.doIntersectionFirstHit(ray, new RayHit())).toBe(false);
        volume.setThreshold(50);
        expect(volume.doIntersectionFirstHit(ray, new RayHit())).toBe(true);
    });

    it("given_randomVolumeAndRays_when_traversed_then_firstHitMatchesTheNearestFilledVoxel", () => {
        // Arrange
        const random = new Random(1234);
        const n = 7;
        const volume = createVolume(n);
        for (let z = 0; z < n; z++) {
            for (let y = 0; y < n; y++) {
                for (let x = 0; x < n; x++) {
                    if (random.nextDouble() < 0.08) {
                        volume.putVoxel(x, y, z, FILLED);
                    }
                }
            }
        }

        for (let i = 0; i < 2000; i++) {
            // Rays from outside the volume cube, aimed at a point inside it
            const origin = randomDirection(random).multiply(4.0);
            const target = new Vector3Dd(
                random.nextDouble() * 2 - 1,
                random.nextDouble() * 2 - 1,
                random.nextDouble() * 2 - 1,
            );
            const direction = target.subtract(origin).normalized();
            const ray = new Ray(origin, direction);
            const hit = new RayHit(RayHit.DETAIL_NONE, true);

            // Act
            const intersects = volume.doIntersectionFirstHit(ray, hit);
            const expected = bruteForceFirstHit(volume, origin, direction);

            // Assert
            if (!Number.isFinite(expected)) {
                expect(intersects, "ray " + i).toBe(false);
            } else {
                expect(intersects, "ray " + i).toBe(true);
                expectCloseTo(hit.getRay()!.getT(), expected, 1.0e-7, "ray " + i);
            }
        }
    });

    function fill(volume: VoxelVolume): void {
        for (let z = 0; z < volume.getZSize(); z++) {
            for (let y = 0; y < volume.getYSize(); y++) {
                for (let x = 0; x < volume.getXSize(); x++) {
                    volume.putVoxel(x, y, z, FILLED);
                }
            }
        }
    }

    function randomDirection(random: Random): Vector3Dd {
        let v: Vector3Dd;
        do {
            v = new Vector3Dd(random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1);
        } while (v.length() < 0.1 || v.length() > 1);
        return v.normalized();
    }

    /**
    @return the smallest entry distance of the ray into the box of a filled
    voxel, or infinity
    */
    function bruteForceFirstHit(volume: VoxelVolume, origin: Vector3Dd, direction: Vector3Dd): number {
        let best = Number.POSITIVE_INFINITY;
        const n = volume.getXSize();
        const cell = 2.0 / n;
        const o = [origin.x(), origin.y(), origin.z()];
        const d = [direction.x(), direction.y(), direction.z()];

        for (let z = 0; z < n; z++) {
            for (let y = 0; y < n; y++) {
                for (let x = 0; x < n; x++) {
                    if (!volume.isFilled(x, y, z)) {
                        continue;
                    }
                    const g = [x, y, z];
                    let tNear = Number.NEGATIVE_INFINITY;
                    let tFar = Number.POSITIVE_INFINITY;
                    for (let a = 0; a < 3; a++) {
                        const lo = -1 + g[a]! * cell;
                        const hi = lo + cell;
                        if (Math.abs(d[a]!) < 1e-15) {
                            if (o[a]! < lo || o[a]! > hi) {
                                tNear = Number.POSITIVE_INFINITY;
                            }
                            continue;
                        }
                        const t1 = (lo - o[a]!) / d[a]!;
                        const t2 = (hi - o[a]!) / d[a]!;
                        tNear = Math.max(tNear, Math.min(t1, t2));
                        tFar = Math.min(tFar, Math.max(t1, t2));
                    }
                    if (tNear <= tFar && tNear >= 0 && tNear < best) {
                        best = tNear;
                    }
                }
            }
        }
        return best;
    }
});
